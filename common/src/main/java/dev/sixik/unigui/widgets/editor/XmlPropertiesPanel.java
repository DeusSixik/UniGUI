package dev.sixik.unigui.widgets.editor;

import dev.sixik.unigui.api.event.EventSubscription;
import dev.sixik.unigui.api.layout.LayoutConstraints;
import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.xml.XmlAttribute;
import dev.sixik.unigui.api.xml.XmlAttributeDescriptor;
import dev.sixik.unigui.api.xml.XmlAttributeValueType;
import dev.sixik.unigui.api.xml.XmlWidgetAssetCatalog;
import dev.sixik.unigui.api.xml.XmlWidgetAssetKind;
import dev.sixik.unigui.api.xml.XmlWidgetAttribute;
import dev.sixik.unigui.api.xml.XmlWidgetDocumentEdits;
import dev.sixik.unigui.api.xml.XmlWidgetInspector;
import dev.sixik.unigui.api.xml.XmlWidgetName;
import dev.sixik.unigui.api.xml.XmlWidgetNodePath;
import dev.sixik.unigui.api.xml.XmlWidgetRegistry;
import dev.sixik.unigui.api.xml.editor.XmlEditorSession;
import dev.sixik.unigui.widgets.containers.LinearBox;
import dev.sixik.unigui.widgets.core.Orientation;
import dev.sixik.unigui.widgets.interaction.ColorPicker;
import dev.sixik.unigui.widgets.interaction.TreeListPicker;
import dev.sixik.unigui.widgets.minecraft.MinecraftTexturePickerWidget;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/** Session-aware XML inspector pane backed by {@link XmlWidgetInspector}. */
@XmlWidgetName("XmlPropertiesPanel")
public final class XmlPropertiesPanel extends LinearBox {
    public static final String STYLE_TYPE = dev.sixik.unigui.api.style.StyleIds.Widget.XML_PROPERTIES_PANEL;

    private final PropertyGrid grid = new PropertyGrid();
    private final AssetBrowserPanel assetPicker = new AssetBrowserPanel();
    private final MinecraftTexturePickerWidget texturePicker = new MinecraftTexturePickerWidget(false);
    private final TreeListPicker<String> itemPicker = new TreeListPicker<>();
    private final ColorPicker colorPicker = new ColorPicker();

    private XmlEditorSession session;
    private EventSubscription sessionSubscription;
    private boolean syncingGrid;
    private boolean syncingPicker;
    private ObjectPickerKind activePicker = ObjectPickerKind.NONE;
    private List<String> itemPickerValues = List.of();
    private XmlWidgetNodePath pickerTargetPath;
    private String pickerAttributeName = "";
    private String lastRejectedAttribute = "";
    private String lastValidationMessage = "";

    public XmlPropertiesPanel() {
        super(Orientation.VERTICAL);
        spacing(3.0f);
        layout(style -> style.size(LayoutConstraints.AUTO, LayoutConstraints.AUTO).flexGrow(1.0f).flexShrink(1.0f));
        grid.layout(style -> style.size(LayoutConstraints.AUTO, LayoutConstraints.AUTO).flexGrow(1.0f).flexShrink(1.0f));
        grid.onAttributeChanged(this::applyGridChange);
        configureObjectPickers();
        addChild(grid);
        addChild(assetPicker);
        addChild(texturePicker);
        addChild(itemPicker);
        addChild(colorPicker);
        applyQueuedMutations();
    }

    public XmlEditorSession session() {
        return session;
    }

    public XmlPropertiesPanel session(XmlEditorSession session) {
        if (this.session == session) return this;
        closeSessionSubscription();
        this.session = session;
        if (session != null) {
            grid.registry(session.registry());
            sessionSubscription = session.onChanged(change -> refreshFromSession());
        }
        refreshFromSession();
        return this;
    }

    public PropertyGrid grid() {
        return grid;
    }

    public AssetBrowserPanel assetPicker() {
        return assetPicker;
    }

    public MinecraftTexturePickerWidget texturePicker() {
        return texturePicker;
    }

    public TreeListPicker<String> itemPicker() {
        return itemPicker;
    }

    public ColorPicker colorPicker() {
        return colorPicker;
    }

    public XmlWidgetAssetCatalog assetCatalog() {
        return assetPicker.catalog();
    }

    public XmlPropertiesPanel assetCatalog(XmlWidgetAssetCatalog catalog) {
        assetPicker.catalog(catalog);
        return this;
    }

    public List<String> itemPickerValues() {
        return itemPickerValues;
    }

    public XmlPropertiesPanel itemPickerValues(List<String> itemIds) {
        itemPickerValues = normalizeResourceIds(itemIds);
        itemPicker.values(itemPickerValues);
        if (activePicker == ObjectPickerKind.ITEM) {
            syncPicker(() -> configureItemPicker(currentAttributeValue(pickerAttributeName)));
        }
        return this;
    }

    public ObjectPickerKind activePicker() {
        return activePicker;
    }

    public String pickerAttributeName() {
        return pickerAttributeName;
    }

    public XmlWidgetRegistry registry() {
        return grid.registry();
    }

    public XmlPropertiesPanel registry(XmlWidgetRegistry registry) {
        grid.registry(registry);
        return this;
    }

    public XmlWidgetInspector.Inspection inspection() {
        return grid.inspection();
    }

    public XmlPropertiesPanel inspection(XmlWidgetInspector.Inspection inspection) {
        session(null);
        syncGrid(() -> grid.inspection(inspection));
        return this;
    }

    public boolean showUnsetAttributes() {
        return grid.showUnsetAttributes();
    }

    @XmlAttribute(value = "showUnsetAttributes", category = "Behavior", defaultValue = "true", description = "Whether descriptor attributes missing from the selected XML element are shown with defaults.")
    public XmlPropertiesPanel showUnsetAttributes(boolean showUnsetAttributes) {
        grid.showUnsetAttributes(showUnsetAttributes);
        return this;
    }

    public boolean showPropertyChildren() {
        return grid.showPropertyChildren();
    }

    @XmlAttribute(value = "showPropertyChildren", category = "Behavior", defaultValue = "true", description = "Whether property-child slots are shown below attribute rows.")
    public XmlPropertiesPanel showPropertyChildren(boolean showPropertyChildren) {
        grid.showPropertyChildren(showPropertyChildren);
        return this;
    }

    public float labelWidth() {
        return grid.labelWidth();
    }

    @XmlAttribute(value = "labelWidth", category = "Layout", defaultValue = "112", description = "Width of property labels in UI pixels.")
    public XmlPropertiesPanel labelWidth(float labelWidth) {
        grid.labelWidth(labelWidth);
        return this;
    }

    public int rowCount() {
        return grid.rowCount();
    }

    public Optional<PropertyFieldRow> row(String attributeName) {
        return grid.row(attributeName);
    }

    public String lastRejectedAttribute() {
        return lastRejectedAttribute;
    }

    public String lastValidationMessage() {
        return lastValidationMessage;
    }

    public Optional<XmlWidgetNodePath> selectedPath() {
        return session == null ? Optional.empty() : session.selectedPath();
    }

    public XmlPropertiesPanel refreshFromSession() {
        syncGrid(() -> {
            if (session == null) {
                grid.inspection(null);
            } else {
                session.selectedInspection().ifPresentOrElse(grid::inspection, () -> grid.inspection(null));
            }
        });
        refreshActivePicker();
        return this;
    }

    public boolean openObjectPicker(String name) {
        String normalizedName = normalizeName(name);
        ObjectPickerKind pickerKind = pickerKindFor(normalizedName);
        if (pickerKind == ObjectPickerKind.NONE) {
            closeObjectPicker();
            return false;
        }

        activePicker = pickerKind;
        pickerAttributeName = normalizedName;
        pickerTargetPath = selectedPath().orElse(null);
        syncPicker(() -> {
            switch (pickerKind) {
                case ASSET -> configureAssetPicker(normalizedName, currentAttributeValue(normalizedName));
                case TEXTURE -> configureTexturePicker(currentAttributeValue(normalizedName));
                case ITEM -> configureItemPicker(currentAttributeValue(normalizedName));
                case COLOR -> configureColorPicker(currentAttributeValue(normalizedName));
                case NONE -> {
                    // Unreachable because NONE returns above.
                }
            }
            syncPickerVisibility();
        });
        return true;
    }

    public XmlPropertiesPanel closeObjectPicker() {
        activePicker = ObjectPickerKind.NONE;
        pickerAttributeName = "";
        pickerTargetPath = null;
        syncPickerVisibility();
        return this;
    }

    public boolean applySelectedAsset() {
        return activePicker == ObjectPickerKind.ASSET && assetPicker.requestApplySelected();
    }

    public boolean applySelectedTexture() {
        ResourceLocation id = texturePicker.selectedId();
        return id != null && applyPickerValue(ObjectPickerKind.TEXTURE, id.toString());
    }

    public boolean applySelectedItem() {
        String id = itemPicker.selectedValue();
        return id != null && !id.isBlank() && applyPickerValue(ObjectPickerKind.ITEM, id);
    }

    public boolean applySelectedColor() {
        return applyPickerValue(ObjectPickerKind.COLOR, colorToXml(colorPicker.color()));
    }

    public boolean setAttributeValue(String name, String value) {
        String normalizedName = normalizeName(name);
        String normalizedValue = value == null ? "" : value;
        if (!validateAttribute(normalizedName, normalizedValue)) return false;
        if (session == null) {
            grid.setAttributeValue(normalizedName, normalizedValue);
            return true;
        }
        return selectedPath()
                .map(path -> session.applyEdit(XmlWidgetDocumentEdits.setAttribute(path, normalizedName, normalizedValue)))
                .orElse(false);
    }

    public boolean addAvailableAttribute(String name) {
        String normalizedName = normalizeName(name);
        String defaultValue = descriptorFor(normalizedName)
                .map(XmlAttributeDescriptor::defaultValue)
                .orElse("");
        return setAttributeValue(normalizedName, defaultValue);
    }

    public boolean resetAttribute(String name) {
        String normalizedName = normalizeName(name);
        String defaultValue = descriptorFor(normalizedName)
                .map(XmlAttributeDescriptor::defaultValue)
                .orElse("");
        return setAttributeValue(normalizedName, defaultValue);
    }

    public boolean removeAttribute(String name) {
        String normalizedName = normalizeName(name);
        if (session == null) {
            grid.removeAttribute(normalizedName);
            return true;
        }
        return selectedPath()
                .map(path -> session.applyEdit(XmlWidgetDocumentEdits.removeAttribute(path, normalizedName)))
                .orElse(false);
    }

    private void applyGridChange(PropertyGrid.AttributeChange change) {
        if (syncingGrid || change == null || session == null) return;
        restoreSelectedAttribute(change.attributeName(), change.oldValue());
        if (change.kind() != PropertyGrid.ChangeKind.REMOVE
                && !validateExistingGridRow(change.attributeName())) {
            refreshFromSession();
            return;
        }

        boolean applied = switch (change.kind()) {
            case SET, RESET -> setAttributeValue(change.attributeName(), change.newValue());
            case REMOVE -> removeAttribute(change.attributeName());
        };
        if (!applied) {
            lastRejectedAttribute = change.attributeName();
            lastValidationMessage = "Could not apply XML property edit.";
            refreshFromSession();
        }
    }

    private void restoreSelectedAttribute(String name, String oldValue) {
        if (session == null) return;
        session.selectedPath()
                .flatMap(path -> path.resolveElement(session.document()))
                .ifPresent(element -> {
                    if (oldValue == null || oldValue.isEmpty()) {
                        element.removeAttribute(name);
                    } else {
                        element.setAttribute(name, oldValue);
                    }
                });
    }

    private boolean validateExistingGridRow(String name) {
        Optional<PropertyFieldRow> row = grid.row(name);
        if (row.isPresent() && row.get().validationState() == PropertyFieldRow.ValidationState.ERROR) {
            lastRejectedAttribute = name;
            lastValidationMessage = row.get().validationMessage();
            return false;
        }
        lastRejectedAttribute = "";
        lastValidationMessage = "";
        return true;
    }

    private boolean validateAttribute(String name, String value) {
        PropertyFieldRow probe = new PropertyFieldRow(
                descriptorFor(name).orElseGet(() -> XmlAttributeDescriptor.of(name)),
                name,
                value,
                true);
        if (probe.validationState() == PropertyFieldRow.ValidationState.ERROR) {
            lastRejectedAttribute = name;
            lastValidationMessage = probe.validationMessage();
            return false;
        }
        lastRejectedAttribute = "";
        lastValidationMessage = "";
        return true;
    }

    private Optional<XmlAttributeDescriptor> descriptorFor(String name) {
        XmlWidgetInspector.Inspection inspection = grid.inspection();
        if (inspection == null) return Optional.empty();
        Optional<XmlAttributeDescriptor> existing = inspection.attribute(name).flatMap(XmlWidgetInspector.Attribute::descriptor);
        if (existing.isPresent()) return existing;
        return inspection.availableAttributes().stream()
                .filter(descriptor -> descriptor.name().equals(name))
                .findFirst();
    }

    private void configureObjectPickers() {
        assetPicker.layout(style -> style.size(LayoutConstraints.AUTO, LayoutConstraints.AUTO).flexGrow(0.0f).flexShrink(0.0f));
        texturePicker.layout(style -> style.size(LayoutConstraints.AUTO, LayoutConstraints.AUTO).flexGrow(0.0f).flexShrink(0.0f));
        itemPicker.layout(style -> style.size(LayoutConstraints.AUTO, LayoutConstraints.AUTO).flexGrow(0.0f).flexShrink(0.0f));
        colorPicker.layout(style -> style.size(LayoutConstraints.AUTO, 22.0f).flexGrow(0.0f).flexShrink(0.0f));

        assetPicker.visibility(Visibility.COLLAPSED);
        texturePicker.visibility(Visibility.COLLAPSED);
        itemPicker.visibility(Visibility.COLLAPSED);
        colorPicker.visibility(Visibility.COLLAPSED);

        assetPicker.onAssetApplied(selection -> {
            if (syncingPicker || activePicker != ObjectPickerKind.ASSET) return;
            applyPickerValue(ObjectPickerKind.ASSET, selection.asset().id());
        });
        texturePicker.onSelectionChanged(event -> {
            if (!syncingPicker) applySelectedTexture();
        });
        itemPicker.onSelectionChanged(event -> {
            if (!syncingPicker) applySelectedItem();
        });
        colorPicker.onColorChanged(event -> {
            if (!syncingPicker) applyPickerValue(ObjectPickerKind.COLOR, colorToXml(event.newColor()));
        });
        syncPickerVisibility();
    }

    private void refreshActivePicker() {
        if (activePicker == ObjectPickerKind.NONE || pickerAttributeName.isEmpty()) return;
        if (grid.row(pickerAttributeName).isEmpty() && descriptorFor(pickerAttributeName).isEmpty()) {
            closeObjectPicker();
            return;
        }
        if (pickerKindFor(pickerAttributeName) == ObjectPickerKind.NONE) {
            closeObjectPicker();
        } else {
            openObjectPicker(pickerAttributeName);
        }
    }

    private boolean applyPickerValue(ObjectPickerKind pickerKind, String value) {
        if (activePicker != pickerKind || pickerAttributeName.isEmpty()) return false;
        if (!validateAttribute(pickerAttributeName, value)) return false;
        if (session == null || pickerTargetPath == null) {
            grid.setAttributeValue(pickerAttributeName, value);
            return true;
        }
        return session.applyEdit(XmlWidgetDocumentEdits.setAttribute(pickerTargetPath, pickerAttributeName, value));
    }

    private void configureAssetPicker(String attributeName, String currentValue) {
        assetPicker.title("Asset: " + attributeName)
                .targetAttribute(attributeName)
                .kind(assetKindFor(attributeName))
                .selectedAsset(currentValue);
    }

    private void configureTexturePicker(String currentValue) {
        ResourceLocation id = parseResourceLocation(currentValue);
        if (id == null) {
            texturePicker.clearSelection();
        } else {
            texturePicker.selectId(id);
        }
    }

    private void configureItemPicker(String currentValue) {
        ResourceLocation id = parseResourceLocation(currentValue);
        int index = id == null ? -1 : itemPickerValues.indexOf(id.toString());
        if (index < 0) {
            clearItemPickerSelection();
        } else {
            itemPicker.silentSelectedIndex(index);
        }
    }

    private void clearItemPickerSelection() {
        itemPicker.values(List.of());
        itemPicker.values(itemPickerValues);
    }

    private void configureColorPicker(String currentValue) {
        if (currentValue == null || currentValue.isBlank()) return;
        try {
            colorPicker.color(MutableColor.fromHex(currentValue));
        } catch (IllegalArgumentException ignored) {
            // Invalid color strings stay in the text field until validation rejects or the user picks a color.
        }
    }

    private void syncPickerVisibility() {
        assetPicker.visibility(activePicker == ObjectPickerKind.ASSET ? Visibility.VISIBLE : Visibility.COLLAPSED);
        texturePicker.visibility(activePicker == ObjectPickerKind.TEXTURE ? Visibility.VISIBLE : Visibility.COLLAPSED);
        itemPicker.visibility(activePicker == ObjectPickerKind.ITEM ? Visibility.VISIBLE : Visibility.COLLAPSED);
        colorPicker.visibility(activePicker == ObjectPickerKind.COLOR ? Visibility.VISIBLE : Visibility.COLLAPSED);
    }

    private ObjectPickerKind pickerKindFor(String attributeName) {
        PropertyFieldRow.FieldKind fieldKind = fieldKindFor(attributeName);
        if (fieldKind == PropertyFieldRow.FieldKind.COLOR) return ObjectPickerKind.COLOR;
        if (fieldKind != PropertyFieldRow.FieldKind.RESOURCE_ID) return ObjectPickerKind.NONE;
        if (isTextureAttribute(attributeName)) return ObjectPickerKind.TEXTURE;
        if (isItemAttribute(attributeName)) return ObjectPickerKind.ITEM;
        return ObjectPickerKind.ASSET;
    }

    private PropertyFieldRow.FieldKind fieldKindFor(String attributeName) {
        Optional<PropertyFieldRow> row = grid.row(attributeName);
        if (row.isPresent()) return row.get().fieldKind();
        return fieldKindFor(descriptorFor(attributeName)
                .orElseGet(() -> XmlAttributeDescriptor.of(attributeName))
                .valueType());
    }

    private String currentAttributeValue(String attributeName) {
        return grid.row(attributeName)
                .map(PropertyFieldRow::value)
                .orElseGet(() -> descriptorFor(attributeName)
                        .map(XmlAttributeDescriptor::defaultValue)
                        .orElse(""));
    }

    private XmlWidgetAssetKind assetKindFor(String attributeName) {
        String lower = attributeName.toLowerCase(Locale.ROOT);
        if (lower.contains("font")) return XmlWidgetAssetKind.FONT;
        if (lower.contains("shader")) return XmlWidgetAssetKind.SHADER;
        return XmlWidgetAssetKind.TEXTURE;
    }

    private static boolean isTextureAttribute(String attributeName) {
        String lower = attributeName.toLowerCase(Locale.ROOT);
        return lower.contains("texture") || lower.equals("textures");
    }

    private static boolean isItemAttribute(String attributeName) {
        String lower = attributeName.toLowerCase(Locale.ROOT);
        return lower.equals("item") || lower.equals("items") || lower.equals("block") || lower.equals("blocks");
    }

    private static PropertyFieldRow.FieldKind fieldKindFor(XmlAttributeValueType valueType) {
        if (valueType == null) return PropertyFieldRow.FieldKind.STRING;
        return switch (valueType) {
            case STRING -> PropertyFieldRow.FieldKind.STRING;
            case NUMBER -> PropertyFieldRow.FieldKind.NUMBER;
            case BOOLEAN -> PropertyFieldRow.FieldKind.BOOLEAN;
            case ENUM -> PropertyFieldRow.FieldKind.ENUM;
            case COLOR -> PropertyFieldRow.FieldKind.COLOR;
            case INSETS -> PropertyFieldRow.FieldKind.INSETS;
            case SIZE_VALUE -> PropertyFieldRow.FieldKind.SIZE_VALUE;
            case RESOURCE_ID -> PropertyFieldRow.FieldKind.RESOURCE_ID;
            case BINDING_OR_ACTION -> PropertyFieldRow.FieldKind.BINDING_OR_ACTION;
        };
    }

    private void syncPicker(Runnable action) {
        syncingPicker = true;
        try {
            action.run();
        } finally {
            syncingPicker = false;
        }
    }

    private void syncGrid(Runnable action) {
        Objects.requireNonNull(action, "action");
        syncingGrid = true;
        try {
            action.run();
        } finally {
            syncingGrid = false;
        }
    }

    private void closeSessionSubscription() {
        if (sessionSubscription == null) return;
        try {
            sessionSubscription.close();
        } catch (Exception ignored) {
            // EventSubscription implementations in UniGUI are local and non-throwing.
        }
        sessionSubscription = null;
    }

    private static String normalizeName(String name) {
        return new XmlWidgetAttribute(name, "").name();
    }

    private static ResourceLocation parseResourceLocation(String value) {
        String normalized = value == null ? "" : value.trim();
        return normalized.isEmpty() ? null : ResourceLocation.tryParse(normalized);
    }

    private static List<String> normalizeResourceIds(List<String> values) {
        if (values == null || values.isEmpty()) return List.of();
        return values.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .distinct()
                .toList();
    }

    private static String colorToXml(ColorView color) {
        return color == null ? "#FFFFFFFF" : new MutableColor(color.r(), color.g(), color.b(), color.a()).toHexString(true);
    }

    public enum ObjectPickerKind {
        NONE,
        ASSET,
        TEXTURE,
        ITEM,
        COLOR
    }
}
