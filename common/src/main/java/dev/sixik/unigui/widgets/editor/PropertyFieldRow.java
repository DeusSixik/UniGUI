package dev.sixik.unigui.widgets.editor;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.event.EventSubscription;
import dev.sixik.unigui.api.layout.LayoutConstraints;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.api.xml.XmlAttributeDescriptor;
import dev.sixik.unigui.api.xml.XmlAttributeValueType;
import dev.sixik.unigui.api.xml.XmlWidgetName;
import dev.sixik.unigui.impl.widget.WidgetBase;
import dev.sixik.unigui.widgets.containers.LinearBox;
import dev.sixik.unigui.widgets.core.Orientation;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.widgets.interaction.Button;
import dev.sixik.unigui.widgets.interaction.Checkbox;
import dev.sixik.unigui.widgets.interaction.ComboBox;
import dev.sixik.unigui.widgets.interaction.NumberField;
import dev.sixik.unigui.widgets.interaction.TextField;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;

@XmlWidgetName("PropertyFieldRow")
public class PropertyFieldRow extends LinearBox {
    private static final float DEFAULT_LABEL_WIDTH = 112.0f;
    private static final float ROW_HEIGHT = 22.0f;

    private final XmlAttributeDescriptor descriptor;
    private final String attributeName;
    private final Label label = new Label();
    private final Button resetButton = new Button("R");
    private final Button removeButton = new Button("X");
    private final Widget editor;

    private FieldKind fieldKind;
    private ValidationState validationState = ValidationState.VALID;
    private String validationMessage = "";
    private String value;
    private boolean present;
    private float labelWidth = DEFAULT_LABEL_WIDTH;
    private boolean syncingEditor;
    private Consumer<ValueChange> valueChanged = change -> {
    };
    private Runnable resetRequested = () -> {
    };
    private Runnable removeRequested = () -> {
    };

    public PropertyFieldRow(XmlAttributeDescriptor descriptor, String value, boolean present) {
        this(descriptor, descriptor == null ? "attribute" : descriptor.name(), value, present);
    }

    public PropertyFieldRow(XmlAttributeDescriptor descriptor, String attributeName, String value, boolean present) {
        super(Orientation.HORIZONTAL);
        this.descriptor = descriptor == null ? XmlAttributeDescriptor.of("attribute") : descriptor;
        this.attributeName = normalize(attributeName).isBlank() ? this.descriptor.name() : attributeName.trim();
        this.value = normalize(value);
        this.present = present;
        this.fieldKind = inferKind(this.descriptor, this.value);
        this.editor = createEditor();

        spacing(4.0f);
        layout(style -> style.height(ROW_HEIGHT).flexGrow(0.0f).flexShrink(0.0f));

        label.text(this.descriptor.displayName());
        label.layout(style -> style.size(labelWidth, ROW_HEIGHT).flexGrow(0.0f).flexShrink(0.0f));
        if (editor != null) {
            label.focusTarget(editor);
            if (editor instanceof WidgetBase base) {
                base.layout(style -> style.size(LayoutConstraints.AUTO, ROW_HEIGHT).flexGrow(1.0f).flexShrink(1.0f));
            }
        }
        resetButton.layout(style -> style.size(20.0f, ROW_HEIGHT).flexGrow(0.0f).flexShrink(0.0f));
        removeButton.layout(style -> style.size(20.0f, ROW_HEIGHT).flexGrow(0.0f).flexShrink(0.0f));
        resetButton.enabled(!Objects.equals(this.value, this.descriptor.defaultValue()));
        removeButton.enabled(present);
        resetButton.onClick(event -> resetRequested.run());
        removeButton.onClick(event -> removeRequested.run());

        addChild(label);
        if (editor != null) addChild(editor);
        addChild(resetButton);
        addChild(removeButton);
        applyQueuedMutations();
        validate();
    }

    public XmlAttributeDescriptor descriptor() {
        return descriptor;
    }

    public String name() {
        return attributeName;
    }

    public String displayName() {
        return descriptor.displayName();
    }

    public String category() {
        return descriptor.category();
    }

    public String description() {
        return descriptor.description();
    }

    public String defaultValue() {
        return descriptor.defaultValue();
    }

    public String value() {
        return value;
    }

    public boolean present() {
        return present;
    }

    public FieldKind fieldKind() {
        return fieldKind;
    }

    public ValidationState validationState() {
        return validationState;
    }

    public String validationMessage() {
        return validationMessage;
    }

    public Widget editor() {
        return editor;
    }

    public Label label() {
        return label;
    }

    public Button resetButton() {
        return resetButton;
    }

    public Button removeButton() {
        return removeButton;
    }

    public PropertyFieldRow labelWidth(float labelWidth) {
        float normalized = Float.isFinite(labelWidth) && labelWidth > 0.0f ? labelWidth : DEFAULT_LABEL_WIDTH;
        if (this.labelWidth == normalized) return this;
        this.labelWidth = normalized;
        label.layout(style -> style.size(this.labelWidth, ROW_HEIGHT).flexGrow(0.0f).flexShrink(0.0f));
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public EventSubscription onValueChanged(Consumer<ValueChange> listener) {
        valueChanged = listener == null ? change -> {
        } : listener;
        return () -> valueChanged = change -> {
        };
    }

    public PropertyFieldRow onResetRequested(Runnable listener) {
        resetRequested = listener == null ? () -> {
        } : listener;
        return this;
    }

    public PropertyFieldRow onRemoveRequested(Runnable listener) {
        removeRequested = listener == null ? () -> {
        } : listener;
        return this;
    }

    public PropertyFieldRow value(String value) {
        setValue(value, false);
        return this;
    }

    public PropertyFieldRow present(boolean present) {
        if (this.present == present) return this;
        this.present = present;
        removeButton.enabled(present);
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public PropertyFieldRow validation(ValidationState state, String message) {
        ValidationState normalized = state == null ? ValidationState.VALID : state;
        String normalizedMessage = normalize(message);
        if (validationState == normalized && Objects.equals(validationMessage, normalizedMessage)) return this;
        validationState = normalized;
        validationMessage = normalizedMessage;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public PropertyFieldRow validate() {
        return validation(validationStateFor(fieldKind, value), validationMessageFor(fieldKind, value));
    }

    private void setValue(String nextValue, boolean emitChange) {
        String normalized = normalize(nextValue);
        if (Objects.equals(value, normalized) && present) return;
        String oldValue = value;
        value = normalized;
        present = true;
        syncEditorValue();
        resetButton.enabled(!Objects.equals(value, descriptor.defaultValue()));
        removeButton.enabled(true);
        validate();
        if (emitChange) {
            valueChanged.accept(new ValueChange(this, attributeName, oldValue, value));
        }
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
    }

    private Widget createEditor() {
        return switch (fieldKind) {
            case BOOLEAN -> booleanEditor();
            case NUMBER -> numberEditor();
            case ENUM -> enumEditor();
            default -> textEditor();
        };
    }

    private TextField textEditor() {
        TextField field = new TextField(value);
        field.placeholder(descriptor.defaultValue());
        field.onTextChanged(event -> {
            if (!syncingEditor) setValue(event.newText(), true);
        });
        return field;
    }

    private NumberField numberEditor() {
        NumberField field = new NumberField();
        field.text(value.isBlank() ? descriptor.defaultValue() : value);
        field.onTextChanged(event -> {
            if (!syncingEditor) setValue(event.newText(), true);
        });
        return field;
    }

    private Checkbox booleanEditor() {
        Checkbox checkbox = new Checkbox();
        checkbox.silentChecked(parseBoolean(value, parseBoolean(descriptor.defaultValue(), false)));
        checkbox.onCheckedChanged(event -> {
            if (!syncingEditor) setValue(Boolean.toString(event.newValue()), true);
        });
        return checkbox;
    }

    private Widget enumEditor() {
        List<String> options = enumOptions(descriptor.name());
        if (options.isEmpty()) return textEditor();
        ComboBox comboBox = new ComboBox()
                .items(options)
                .dropDownMode(ComboBox.DropDownMode.INLINE)
                .placeholder(descriptor.defaultValue());
        int selected = options.indexOf(value);
        if (selected < 0) selected = options.indexOf(descriptor.defaultValue());
        comboBox.silentSelectedIndex(selected);
        comboBox.onSelectionChanged(event -> {
            if (!syncingEditor) setValue(comboBox.selectedItem(), true);
        });
        return comboBox;
    }

    private void syncEditorValue() {
        syncingEditor = true;
        try {
            if (editor instanceof TextField field) {
                field.text(value);
            } else if (editor instanceof NumberField numberField) {
                numberField.text(value);
            } else if (editor instanceof Checkbox checkbox) {
                checkbox.silentChecked(parseBoolean(value, false));
            } else if (editor instanceof ComboBox comboBox) {
                int selected = comboBox.items().indexOf(value);
                comboBox.silentSelectedIndex(selected);
            }
        } finally {
            syncingEditor = false;
        }
    }

    private static FieldKind inferKind(XmlAttributeDescriptor descriptor, String value) {
        FieldKind descriptorKind = fieldKindFor(descriptor.valueType());
        if (descriptorKind != null) return descriptorKind;

        String name = descriptor.name().toLowerCase(Locale.ROOT);
        String defaultValue = descriptor.defaultValue();
        String candidate = value.isBlank() ? defaultValue : value;
        if (isBooleanLiteral(candidate) || name.startsWith("is") || name.startsWith("has")
                || name.startsWith("allow") || name.startsWith("show") || name.endsWith("enabled")
                || name.endsWith("visible") || name.endsWith("checked") || name.equals("open") || name.equals("opened")) {
            return FieldKind.BOOLEAN;
        }
        if (name.contains("color") || name.contains("tint") || looksLikeColor(candidate)) return FieldKind.COLOR;
        if (name.contains("padding") || name.contains("margin") || name.contains("inset")) return FieldKind.INSETS;
        if (name.contains("texture") || name.contains("resource") || name.equals("item")
                || name.equals("block") || name.equals("entity") || name.endsWith("id")) {
            return FieldKind.RESOURCE_ID;
        }
        if (name.contains("binding") || name.contains("action") || name.contains("command") || name.startsWith("on")) {
            return FieldKind.BINDING_OR_ACTION;
        }
        if (isEnumName(name)) return FieldKind.ENUM;
        if (isSizeName(name)) return FieldKind.SIZE_VALUE;
        if (isNumericLiteral(candidate) || isNumericName(name)) return FieldKind.NUMBER;
        return FieldKind.STRING;
    }

    private static ValidationState validationStateFor(FieldKind kind, String value) {
        if (value == null || value.isBlank()) return ValidationState.VALID;
        return switch (kind) {
            case BOOLEAN -> isBooleanLiteral(value) ? ValidationState.VALID : ValidationState.ERROR;
            case NUMBER -> isNumericLiteral(value) ? ValidationState.VALID : ValidationState.ERROR;
            case COLOR -> looksLikeColor(value) ? ValidationState.VALID : ValidationState.WARNING;
            default -> ValidationState.VALID;
        };
    }

    private static String validationMessageFor(FieldKind kind, String value) {
        if (validationStateFor(kind, value) == ValidationState.VALID) return "";
        return switch (kind) {
            case BOOLEAN -> "Expected true or false.";
            case NUMBER -> "Expected a number.";
            case COLOR -> "Expected a color such as #RRGGBB or #AARRGGBB.";
            default -> "";
        };
    }

    private static boolean isEnumName(String name) {
        return name.endsWith("mode") || name.endsWith("state") || name.endsWith("alignment")
                || name.endsWith("orientation") || name.endsWith("direction") || name.equals("selectionmode")
                || name.equals("overflow") || name.equals("overflowx") || name.equals("overflowy")
                || name.equals("position") || name.equals("flexdirection") || name.equals("flexwrap")
                || name.equals("align") || name.equals("alignitems") || name.equals("alignself")
                || name.equals("justifycontent") || name.equals("dockarea");
    }

    private static boolean isSizeName(String name) {
        return name.equals("width") || name.equals("height") || name.equals("minwidth") || name.equals("minheight")
                || name.equals("maxwidth") || name.equals("maxheight") || name.equals("left") || name.equals("top")
                || name.equals("right") || name.equals("bottom") || name.endsWith("width") || name.endsWith("height")
                || name.endsWith("size");
    }

    private static FieldKind fieldKindFor(XmlAttributeValueType valueType) {
        if (valueType == null) return null;
        return switch (valueType) {
            case STRING -> FieldKind.STRING;
            case NUMBER -> FieldKind.NUMBER;
            case BOOLEAN -> FieldKind.BOOLEAN;
            case ENUM -> FieldKind.ENUM;
            case COLOR -> FieldKind.COLOR;
            case INSETS -> FieldKind.INSETS;
            case SIZE_VALUE -> FieldKind.SIZE_VALUE;
            case RESOURCE_ID -> FieldKind.RESOURCE_ID;
            case BINDING_OR_ACTION -> FieldKind.BINDING_OR_ACTION;
        };
    }

    private static boolean isNumericName(String name) {
        return name.equals("x") || name.equals("y") || name.endsWith("index") || name.endsWith("count")
                || name.contains("gap") || name.contains("radius") || name.contains("opacity")
                || name.contains("speed") || name.contains("step") || name.contains("seconds") || name.contains("lines");
    }

    private static List<String> enumOptions(String name) {
        String normalized = name.toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "orientation" -> List.of("horizontal", "vertical");
            case "selectionmode" -> List.of("single", "multiple", "none");
            case "overflow", "overflowx", "overflowy" -> List.of("visible", "hidden", "auto", "scroll");
            case "position" -> List.of("relative", "absolute");
            case "flexdirection" -> List.of("row", "column", "row_reverse", "column_reverse");
            case "flexwrap" -> List.of("nowrap", "wrap", "wrap_reverse");
            case "align", "alignitems", "alignself" -> List.of("start", "center", "end", "stretch");
            case "justifycontent" -> List.of("start", "center", "end", "space_between", "space_around", "space_evenly");
            case "targetoptions" -> List.of("color", "colorDepth");
            default -> List.of();
        };
    }

    private static boolean isBooleanLiteral(String value) {
        return "true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value);
    }

    private static boolean parseBoolean(String value, boolean fallback) {
        if ("true".equalsIgnoreCase(value)) return true;
        if ("false".equalsIgnoreCase(value)) return false;
        return fallback;
    }

    private static boolean isNumericLiteral(String value) {
        if (value == null || value.isBlank()) return false;
        try {
            Double.parseDouble(value.trim());
            return true;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    private static boolean looksLikeColor(String value) {
        if (value == null) return false;
        String normalized = value.trim();
        if (normalized.matches("#[0-9a-fA-F]{6}([0-9a-fA-F]{2})?")) return true;
        return normalized.matches("#[0-9a-fA-F]{8}");
    }

    private static String normalize(String value) {
        return value == null ? "" : value;
    }

    public enum FieldKind {
        STRING,
        NUMBER,
        BOOLEAN,
        ENUM,
        COLOR,
        INSETS,
        SIZE_VALUE,
        RESOURCE_ID,
        BINDING_OR_ACTION
    }

    public enum ValidationState {
        VALID,
        WARNING,
        ERROR
    }

    public record ValueChange(PropertyFieldRow row, String attributeName, String oldValue, String newValue) {
    }
}
