package dev.sixik.unigui.xaml.schema.builtin;

import dev.sixik.unigui.api.widget.CheckboxState;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.widgets.display.Separator;
import dev.sixik.unigui.widgets.feedback.ContextMenu;
import dev.sixik.unigui.widgets.feedback.LoadingIndicator;
import dev.sixik.unigui.widgets.feedback.NotificationView;
import dev.sixik.unigui.widgets.feedback.OverlayLayer;
import dev.sixik.unigui.widgets.feedback.Popup;
import dev.sixik.unigui.widgets.feedback.ProgressBar;
import dev.sixik.unigui.widgets.feedback.Spinner;
import dev.sixik.unigui.widgets.feedback.Toast;
import dev.sixik.unigui.widgets.feedback.Tooltip;
import dev.sixik.unigui.widgets.feedback.WindowWidget;
import dev.sixik.unigui.widgets.interaction.Button;
import dev.sixik.unigui.widgets.interaction.Checkbox;
import dev.sixik.unigui.widgets.interaction.CodeEditor;
import dev.sixik.unigui.widgets.interaction.ColorPicker;
import dev.sixik.unigui.widgets.interaction.ComboBox;
import dev.sixik.unigui.widgets.interaction.DatePicker;
import dev.sixik.unigui.widgets.interaction.DropDownBox;
import dev.sixik.unigui.widgets.interaction.IconButton;
import dev.sixik.unigui.widgets.interaction.NumberField;
import dev.sixik.unigui.widgets.interaction.PasswordField;
import dev.sixik.unigui.widgets.interaction.RadioButton;
import dev.sixik.unigui.widgets.interaction.SearchField;
import dev.sixik.unigui.widgets.interaction.Slider;
import dev.sixik.unigui.widgets.interaction.TextArea;
import dev.sixik.unigui.widgets.interaction.TextField;
import dev.sixik.unigui.widgets.interaction.TextInput;
import dev.sixik.unigui.widgets.interaction.TimeSpanField;
import dev.sixik.unigui.widgets.interaction.ToggleButton;
import dev.sixik.unigui.widgets.interaction.ToggleSwitch;
import dev.sixik.unigui.widgets.interaction.ToggleToolButton;
import dev.sixik.unigui.widgets.interaction.ToolButton;
import dev.sixik.unigui.widgets.interaction.TreeListPicker;
import dev.sixik.unigui.widgets.interaction.XmlCodeEditor;
import dev.sixik.unigui.widgets.navigation.MenuBar;
import dev.sixik.unigui.widgets.navigation.ToolBar;
import dev.sixik.unigui.xaml.schema.ChildSink;
import dev.sixik.unigui.xaml.schema.Converters;
import dev.sixik.unigui.xaml.schema.LoadContext;
import dev.sixik.unigui.xaml.schema.Meta;
import dev.sixik.unigui.xaml.schema.Schema;
import dev.sixik.unigui.xaml.schema.SlotInfo;
import dev.sixik.unigui.xaml.schema.TypeInfo;
import dev.sixik.unigui.xaml.schema.ValueKind;

import static dev.sixik.unigui.xaml.schema.builtin.Types.singleContent;
import static dev.sixik.unigui.xaml.schema.builtin.Types.type;

/** Интерактивные элементы и оверлеи: кнопки, поля ввода, переключатели, слайдеры, окна, всплывающие слои. */
final class ControlTypes {
    private ControlTypes() {
    }

    static void register(Schema schema) {
        schema.define("Button", () -> button(type("Button", Button.class, Button::new)).build());
        schema.define("ToggleButton", () -> toggleButton(type("ToggleButton", ToggleButton.class, ToggleButton::new)).build());
        schema.define("ToggleSwitch", () -> toggleButton(type("ToggleSwitch", ToggleSwitch.class, ToggleSwitch::new))
                .describe("Toggle Switch", "Controls", "Switch-style checked/unchecked control.").build());
        schema.define("Checkbox", ControlTypes::checkbox);
        schema.define("RadioButton", () -> button(type("RadioButton", RadioButton.class, RadioButton::new))
                .describe("Radio Button", "Controls",
                        "Single radio option; group membership is currently wired by Java hosts.").build());
        schema.define("ComboBox", () -> type("ComboBox", ComboBox.class, ComboBox::new)
                .describe("Combo Box", "Controls",
                        "Selectable dropdown list. Use the items attribute for simple string options.").build());
        schema.define("DropDownBox", ControlTypes::dropDownBox);
        schema.define("TreeListPicker", () -> type("TreeListPicker", TreeListPicker.class, TreeListPicker::new)
                .describe("Tree List Picker", "Controls",
                        "Combo-box picker for path-like tree values. Use the items attribute for simple XML lists.").build());
        schema.define("DatePicker", () -> type("DatePicker", DatePicker.class, DatePicker::new)
                .describe("Date Picker", "Controls",
                        "ISO-8601 date input with previous/next buttons and popup calendar.").build());
        schema.define("ColorPicker", () -> type("ColorPicker", ColorPicker.class, ColorPicker::new)
                .describe("Color Picker", "Controls",
                        "Color selection control with HSV and ARGB editing modes.").build());
        schema.define("Slider", ControlTypes::slider);
        schema.define("ProgressBar", ControlTypes::progressBar);
        schema.define("Spinner", () -> type("Spinner", Spinner.class, Spinner::new).build());
        schema.define("LoadingIndicator", () -> type("LoadingIndicator", LoadingIndicator.class, LoadingIndicator::new)
                .describe("Loading Indicator", "Feedback",
                        "Animated activity indicator for spinner, dots and bar states.").build());
        schema.define("Toast", ControlTypes::toast);
        schema.define("NotificationView", ControlTypes::notificationView);
        schema.define("ContextMenu", ControlTypes::contextMenu);
        schema.define("OverlayLayer", ControlTypes::overlayLayer);
        schema.define("Popup", ControlTypes::popup);
        schema.define("Tooltip", () -> type("Tooltip", Tooltip.class, Tooltip::new)
                .describe("Tooltip", "Feedback", "Text tooltip overlay; anchor is wired by code.").build());
        schema.define("WindowWidget", ControlTypes::windowWidget);

        schema.define("TextInput", () -> textInput(type("TextInput", TextInput.class, TextInput::new)
                .describe("Text Input", "Controls", "Single-line editable text input.")).build());
        schema.define("TextField", () -> textInput(type("TextField", TextField.class, TextField::new)).build());
        schema.define("NumberField", () -> textInput(type("NumberField", NumberField.class, NumberField::new)
                .describe("Number Field", "Controls", "Numeric text field with range and step support.")).build());
        schema.define("SearchField", () -> textInput(type("SearchField", SearchField.class, SearchField::new)
                .describe("Search Field", "Controls",
                        "Single-line search input with submit and debounced change events.")).build());
        schema.define("PasswordField", () -> textInput(type("PasswordField", PasswordField.class, PasswordField::new)
                .describe("Password Field", "Controls", "Masked single-line password input.")).build());
        schema.define("TimeSpanField", () -> textInput(type("TimeSpanField", TimeSpanField.class, TimeSpanField::new)
                .describe("Time Span Field", "Controls", "HH:MM:SS duration input.")).build());
        schema.define("TextArea", () -> type("TextArea", TextArea.class, TextArea::new)
                .describe("Text Area", "Controls", "Multiline editable text control.").build());
        schema.define("CodeEditor", () -> type("CodeEditor", CodeEditor.class, CodeEditor::new)
                .describe("Code Editor", "Controls", "Line-numbered source editor.").build());
        schema.define("XmlCodeEditor", () -> type("XmlCodeEditor", XmlCodeEditor.class, XmlCodeEditor::new)
                .describe("XML Code Editor", "Controls", "XML-aware source editor.").build());

        schema.define("MenuBar", () -> type("MenuBar", MenuBar.class, MenuBar::new)
                .describe("Menu Bar", "Navigation", "Horizontal application menu bar backed by editor commands.").build());
        schema.define("ToolBar", () -> type("ToolBar", ToolBar.class, ToolBar::new)
                .describe("Tool Bar", "Controls", "Compact grouped command toolbar.").build());
        schema.define("ToolButton", () -> toolButton(type("ToolButton", ToolButton.class, ToolButton::new)
                .describe("Tool Button", "Controls", "Compact toolbar button with icon/text display modes.")).build());
        schema.define("IconButton", () -> toolButton(type("IconButton", IconButton.class, IconButton::new)
                .describe("Icon Button", "Controls", "Icon-only toolbar button.")).build());
        schema.define("ToggleToolButton", () -> toolButton(type("ToggleToolButton", ToggleToolButton.class, ToggleToolButton::new)
                .describe("Toggle Tool Button", "Controls", "Toolbar button with checked active state.")).build());
    }

    // ------------------------------------------------------------------ кнопки

    private static <T extends Button> TypeInfo.Builder<T> button(TypeInfo.Builder<T> builder) {
        return builder
                .property("text", Converters.STRING, Button::text)
                .property("color", Converters.COLOR, (widget, color) -> widget.textColor().set(color))
                .property("textColor", Converters.COLOR, (widget, color) -> widget.textColor().set(color))
                .property("textPaddingX", Converters.FLOAT, Button::textPaddingX)
                .property("textPaddingY", Converters.FLOAT, Button::textPaddingY)
                .property("interactionTransitions", Converters.BOOLEAN, Button::interactionTransitions)
                .property("hoverBackground", Converters.COLOR, Button::hoverBackground)
                .property("pressedBackground", Converters.COLOR, Button::pressedBackground)
                .property("hoverTexture", Converters.TEXTURE, Button::hoverTexture)
                .property("pressedTexture", Converters.TEXTURE, Button::pressedTexture)
                .propertyWithContext("onClick", Converters.STRING, ControlTypes::bindClickCommand,
                        Meta.category("Behavior").kind(ValueKind.BINDING_OR_ACTION)
                                .description("Named command/action id invoked when the button is clicked."))
                .bindable("text");
    }

    private static void bindClickCommand(Button button, String commandName, LoadContext context) {
        String normalized = commandName == null ? "" : commandName.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Button onClick command name must not be blank");
        }
        var command = context.commands()
                .command(normalized)
                .orElseThrow(() -> new IllegalArgumentException("Unknown XML command '" + normalized + "'"));
        button.onClick(event -> command.execute(button, event));
    }

    private static <T extends ToggleButton> TypeInfo.Builder<T> toggleButton(TypeInfo.Builder<T> builder) {
        return button(builder)
                .property("checked", Converters.BOOLEAN, ToggleButton::silentChecked)
                .property("checkedBackground", Converters.COLOR, (widget, color) -> widget.checkedBackground().set(color))
                .property("uncheckedBackground", Converters.COLOR, (widget, color) -> widget.uncheckedBackground().set(color))
                .bindable("checked");
    }

    private static TypeInfo<Checkbox> checkbox() {
        return toggleButton(type("Checkbox", Checkbox.class, Checkbox::new))
                .property("state", Converters.enumOf(CheckboxState.class), Checkbox::silentState)
                .property("checked", Converters.BOOLEAN, Checkbox::silentChecked)
                .property("triState", Converters.BOOLEAN, Checkbox::triState)
                .property("boxSize", Converters.FLOAT, Checkbox::boxSize)
                .property("checkSize", Converters.FLOAT, Checkbox::checkSize)
                .property("textGap", Converters.FLOAT, Checkbox::textGap)
                .property("labelLeft", Converters.BOOLEAN, Checkbox::labelLeft)
                .build();
    }

    private static <T extends ToolButton> TypeInfo.Builder<T> toolButton(TypeInfo.Builder<T> builder) {
        return button(builder)
                .property("icon", Converters.STRING, ToolButton::icon)
                .property("label", Converters.STRING, ToolButton::label)
                .property("tooltip", Converters.STRING, ToolButton::tooltip)
                .property("command", Converters.STRING, ToolButton::commandId)
                .property("displayMode", Converters.enumOf(ToolButton.DisplayMode.class), ToolButton::displayMode)
                .property("checked", Converters.BOOLEAN, ToolButton::checked);
    }

    // ------------------------------------------------------------------ ввод

    private static <T extends TextInput> TypeInfo.Builder<T> textInput(TypeInfo.Builder<T> builder) {
        return builder
                .property("text", Converters.STRING, TextInput::text,
                        Meta.category("Content").description("Editable text value."))
                .property("placeholder", Converters.STRING, TextInput::placeholder,
                        Meta.category("Content").description("Placeholder text shown while the value is empty."))
                .property("maxLength", Converters.INT, TextInput::maxLength,
                        Meta.category("Behavior").defaultValue("0")
                                .description("Maximum editable character count; 0 means unlimited."))
                .property("visualOnlyTextChanges", Converters.BOOLEAN, TextInput::visualOnlyTextChanges,
                        Meta.category("Behavior").defaultValue("false")
                                .description("Whether text changes invalidate visuals only instead of layout."))
                .property("cursorIndex", Converters.INT, TextInput::cursorIndex,
                        Meta.category("Behavior").defaultValue("0")
                                .description("Initial cursor position in the editable text."))
                .bindable("text");
    }

    private static TypeInfo<DropDownBox> dropDownBox() {
        ChildSink<DropDownBox> content = (widget, child) -> widget.content(child);
        return singleContent(type("DropDownBox", DropDownBox.class, DropDownBox::new)
                        .describe("Drop Down Box", "Controls", "Dropdown host for arbitrary content widgets."),
                content, "Single widget shown inside the dropdown content area.").build();
    }

    // ------------------------------------------------------------------ значения

    private static TypeInfo<Slider> slider() {
        return type("Slider", Slider.class, Slider::new)
                .property("min", Converters.FLOAT, (widget, value) -> widget.range(value, widget.max()))
                .property("max", Converters.FLOAT, (widget, value) -> widget.range(widget.min(), value))
                .property("value", Converters.FLOAT, Slider::value)
                .property("step", Converters.FLOAT, Slider::step)
                .property("preferredWidth", Converters.FLOAT, Slider::preferredWidth)
                .property("preferredHeight", Converters.FLOAT, Slider::preferredHeight)
                .property("trackColor", Converters.COLOR, (widget, color) -> widget.trackColor().set(color))
                .property("fillColor", Converters.COLOR, (widget, color) -> widget.fillColor().set(color))
                .property("knobColor", Converters.COLOR, (widget, color) -> widget.knobColor().set(color))
                .bindable("value")
                .build();
    }

    private static TypeInfo<ProgressBar> progressBar() {
        return type("ProgressBar", ProgressBar.class, ProgressBar::new)
                .property("min", Converters.FLOAT, (widget, value) -> widget.range(value, widget.max()))
                .property("max", Converters.FLOAT, (widget, value) -> widget.range(widget.min(), value))
                .property("value", Converters.FLOAT, ProgressBar::value)
                .property("indeterminate", Converters.BOOLEAN, ProgressBar::indeterminate)
                .property("indeterminateSpeed", Converters.FLOAT, ProgressBar::indeterminateSpeed)
                .property("preferredWidth", Converters.FLOAT, ProgressBar::preferredWidth)
                .property("preferredHeight", Converters.FLOAT, ProgressBar::preferredHeight)
                .property("trackColor", Converters.COLOR, (widget, color) -> widget.trackColor().set(color))
                .property("fillColor", Converters.COLOR, (widget, color) -> widget.fillColor().set(color))
                .bindable("value")
                .build();
    }

    // ------------------------------------------------------------------ оверлеи

    private static TypeInfo<Toast> toast() {
        return type("Toast", Toast.class, Toast::new)
                .describe("Toast", "Feedback", "Transient message card for overlay surfaces.")
                .property("open", Converters.BOOLEAN, ControlTypes::openToast,
                        Meta.category("Behavior").defaultValue("false")
                                .description("Whether the toast starts visible after XML load."))
                .build();
    }

    private static TypeInfo<NotificationView> notificationView() {
        return type("NotificationView", NotificationView.class, NotificationView::new)
                .describe("Notification View", "Feedback", "Overlay host for stacked transient notification cards.")
                .property("open", Converters.BOOLEAN, ControlTypes::openNotificationView,
                        Meta.category("Behavior").defaultValue("false")
                                .description("Whether the pending notification text is shown after XML load."))
                .build();
    }

    private static void openToast(Toast toast, boolean open) {
        if (open) toast.show();
        else toast.hide();
    }

    private static void openNotificationView(NotificationView view, boolean open) {
        if (open) view.show();
        else view.hide();
    }

    private static TypeInfo<ContextMenu> contextMenu() {
        ChildSink<ContextMenu> items = ControlTypes::addContextMenuItem;
        return type("ContextMenu", ContextMenu.class, ContextMenu::new)
                .describe("Context Menu", "Feedback", "Floating menu with button items and separators.")
                .content(items)
                .slot(SlotInfo.many("Items", items,
                        "Menu item widgets. Button children become actions; Separator children become menu dividers."))
                .build();
    }

    private static void addContextMenuItem(ContextMenu menu, Widget child) {
        if (child instanceof Button button) {
            menu.item(button);
        } else if (child instanceof Separator separator) {
            menu.separator(separator);
        } else {
            throw new IllegalArgumentException("ContextMenu items must be Button or Separator widgets.");
        }
    }

    private static TypeInfo<OverlayLayer> overlayLayer() {
        return type("OverlayLayer", OverlayLayer.class, OverlayLayer::new)
                .describe("Overlay Layer", "Feedback", "Root host for normal content plus popups, tooltips and windows.")
                .property("modalScrimColor", Converters.COLOR, (widget, color) -> widget.modalScrimColor().set(color),
                        Meta.category("Appearance").defaultValue("#0000007A")
                                .description("Color drawn behind the top modal window."))
                .content(ControlTypes::addOverlayLayerChild)
                .slot(SlotInfo.single("Content", ControlTypes::setOverlayLayerContent,
                        "Single base content widget rendered below overlays."))
                .slot(SlotInfo.many("Overlays", ControlTypes::addOverlayLayerOverlay,
                        "Overlay widgets rendered above the base content."))
                .build();
    }

    private static void addOverlayLayerChild(OverlayLayer layer, Widget child) {
        if (layer.content() == null) layer.content(child);
        else layer.addOverlay(child);
        layer.applyQueuedMutations();
    }

    private static void setOverlayLayerContent(OverlayLayer layer, Widget child) {
        if (layer.content() != null && layer.content() != child) {
            throw new IllegalArgumentException("Widget OverlayLayer can contain only one content child.");
        }
        layer.content(child);
        layer.applyQueuedMutations();
    }

    private static void addOverlayLayerOverlay(OverlayLayer layer, Widget child) {
        layer.addOverlay(child);
        layer.applyQueuedMutations();
    }

    private static TypeInfo<Popup> popup() {
        return singleContent(type("Popup", Popup.class, Popup::new)
                        .describe("Popup", "Feedback", "Overlay popup host for arbitrary content; anchor is wired by code."),
                ControlTypes::setPopupContent, "Single widget shown inside the popup.").build();
    }

    private static void setPopupContent(Popup popup, Widget child) {
        if (popup.content() != null && popup.content() != child) {
            throw new IllegalArgumentException("Widget Popup can contain only one content child.");
        }
        popup.content(child);
        popup.applyQueuedMutations();
    }

    private static TypeInfo<WindowWidget> windowWidget() {
        return singleContent(type("WindowWidget", WindowWidget.class, WindowWidget::new)
                        .describe("Window", "Feedback", "Floating draggable and resizable overlay window.")
                        .property("windowX", Converters.FLOAT, (widget, value) -> widget.position(value, widget.windowY()),
                                Meta.category("Layout").defaultValue("32")
                                        .description("Window position inside its overlay host on the X axis."))
                        .property("windowY", Converters.FLOAT, (widget, value) -> widget.position(widget.windowX(), value),
                                Meta.category("Layout").defaultValue("28")
                                        .description("Window position inside its overlay host on the Y axis.")),
                ControlTypes::setWindowContent, "Single widget shown inside the window body.").build();
    }

    private static void setWindowContent(WindowWidget window, Widget child) {
        if (window.content() != null && window.content() != child) {
            throw new IllegalArgumentException("Widget WindowWidget can contain only one content child.");
        }
        window.content(child);
        window.applyQueuedMutations();
    }
}
