package dev.sixik.unigui.impl.xml;

import dev.sixik.unigui.api.widget.CheckboxState;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.api.xml.XmlAttributeDescriptor;
import dev.sixik.unigui.api.xml.XmlPropertyChildDescriptor;
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

final class BuiltInControlXml {
    private BuiltInControlXml() {
    }

    static void register(WidgetXmlRegistry registry) {
        button(registry.register("Button", Button::new));
        toggleButton(registry.register("ToggleButton", ToggleButton::new));
        toggleSwitch(registry.register("ToggleSwitch", ToggleSwitch::new));
        checkbox(registry.register("Checkbox", Checkbox::new));
        radioButton(registry.register("RadioButton", RadioButton::new));
        comboBox(registry.register("ComboBox", ComboBox::new));
        dropDownBox(registry.register("DropDownBox", DropDownBox::new));
        treeListPicker(registry.register("TreeListPicker", TreeListPicker::new));
        datePicker(registry.register("DatePicker", DatePicker::new));
        colorPicker(registry.register("ColorPicker", ColorPicker::new));
        slider(registry.register("Slider", Slider::new));
        progressBar(registry.register("ProgressBar", ProgressBar::new));
        BuiltInWidgetXmlSupport.box(registry.register("Spinner", Spinner::new), Spinner.class);
        loadingIndicator(registry.register("LoadingIndicator", LoadingIndicator::new));
        toast(registry.register("Toast", Toast::new));
        notificationView(registry.register("NotificationView", NotificationView::new));
        contextMenu(registry.register("ContextMenu", ContextMenu::new));
        overlayLayer(registry.register("OverlayLayer", OverlayLayer::new));
        popup(registry.register("Popup", Popup::new));
        tooltip(registry.register("Tooltip", Tooltip::new));
        windowWidget(registry.register("WindowWidget", WindowWidget::new));
        textInput(registry.register("TextInput", TextInput::new)
                .describe("Text Input", "Controls", "Single-line editable text input."), TextInput.class);
        textInput(registry.register("TextField", TextField::new), TextField.class);
        textInput(registry.register("NumberField", NumberField::new)
                .describe("Number Field", "Controls", "Numeric text field with range and step support."), NumberField.class);
        textInput(registry.register("SearchField", SearchField::new)
                .describe("Search Field", "Controls", "Single-line search input with submit and debounced change events."), SearchField.class);
        textInput(registry.register("PasswordField", PasswordField::new)
                .describe("Password Field", "Controls", "Masked single-line password input."), PasswordField.class);
        textInput(registry.register("TimeSpanField", TimeSpanField::new)
                .describe("Time Span Field", "Controls", "HH:MM:SS duration input."), TimeSpanField.class);
        BuiltInWidgetXmlSupport.box(registry.register("TextArea", TextArea::new)
                .describe("Text Area", "Controls", "Multiline editable text control."), TextArea.class);
        BuiltInWidgetXmlSupport.box(registry.register("CodeEditor", CodeEditor::new)
                .describe("Code Editor", "Controls", "Line-numbered source editor."), CodeEditor.class);
        BuiltInWidgetXmlSupport.box(registry.register("XmlCodeEditor", XmlCodeEditor::new)
                .describe("XML Code Editor", "Controls", "XML-aware source editor."), XmlCodeEditor.class);
        BuiltInWidgetXmlSupport.commonWidget(registry.register("MenuBar", MenuBar::new), MenuBar.class)
                .describe("Menu Bar", "Navigation", "Horizontal application menu bar backed by editor commands.");
        BuiltInWidgetXmlSupport.commonWidget(registry.register("ToolBar", ToolBar::new), ToolBar.class)
                .describe("Tool Bar", "Controls", "Compact grouped command toolbar.");
        toolButton(registry.register("ToolButton", ToolButton::new)
                .describe("Tool Button", "Controls", "Compact toolbar button with icon/text display modes."), ToolButton.class);
        toolButton(registry.register("IconButton", IconButton::new)
                .describe("Icon Button", "Controls", "Icon-only toolbar button."), IconButton.class);
        toolButton(registry.register("ToggleToolButton", ToggleToolButton::new)
                .describe("Toggle Tool Button", "Controls", "Toolbar button with checked active state."), ToggleToolButton.class);
    }

    private static <T extends Button> WidgetXmlType<T> button(WidgetXmlType<T> type) {
        return BuiltInWidgetXmlSupport.box(type)
                .attribute("text", XmlValueParsers.STRING, Button::text)
                .attribute("color", XmlValueParsers.COLOR, (widget, color) -> widget.textColor().set(color))
                .attribute("textColor", XmlValueParsers.COLOR, (widget, color) -> widget.textColor().set(color))
                .attribute("textPaddingX", XmlValueParsers.FLOAT, Button::textPaddingX)
                .attribute("textPaddingY", XmlValueParsers.FLOAT, Button::textPaddingY)
                .attribute("interactionTransitions", XmlValueParsers.BOOLEAN, Button::interactionTransitions)
                .attribute("onClick", XmlValueParsers.STRING, BuiltInControlXml::bindClickCommand);
    }

    private static void bindClickCommand(Button button, String commandName) {
        String normalized = commandName == null ? "" : commandName.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Button onClick command name must not be blank");
        }
        var command = XmlValueParsers.commandRegistry()
                .command(normalized)
                .orElseThrow(() -> new IllegalArgumentException("Unknown XML command '" + normalized + "'"));
        button.onClick(event -> command.execute(button, event));
    }

    private static <T extends ToggleButton> WidgetXmlType<T> toggleButton(WidgetXmlType<T> type) {
        return button(type)
                .attribute("checked", XmlValueParsers.BOOLEAN, ToggleButton::silentChecked)
                .attribute("checkedBackground", XmlValueParsers.COLOR, (widget, color) -> widget.checkedBackground().set(color))
                .attribute("uncheckedBackground", XmlValueParsers.COLOR, (widget, color) -> widget.uncheckedBackground().set(color));
    }

    private static WidgetXmlType<ToggleSwitch> toggleSwitch(WidgetXmlType<ToggleSwitch> type) {
        return BuiltInWidgetXmlSupport.annotated(toggleButton(type), ToggleSwitch.class)
                .describe("Toggle Switch", "Controls", "Switch-style checked/unchecked control.");
    }

    private static <T extends TextInput> WidgetXmlType<T> textInput(WidgetXmlType<T> type, Class<?> widgetType) {
        return BuiltInWidgetXmlSupport.annotated(BuiltInWidgetXmlSupport.commonWidget(type)
                .attribute("text", XmlValueParsers.STRING, TextInput::text,
                        XmlAttributeDescriptor.of("text")
                                .category("Content")
                                .defaultValue("")
                                .description("Editable text value."))
                .attribute("placeholder", XmlValueParsers.STRING, TextInput::placeholder,
                        XmlAttributeDescriptor.of("placeholder")
                                .category("Content")
                                .defaultValue("")
                                .description("Placeholder text shown while the value is empty."))
                .attribute("maxLength", XmlValueParsers.INT, TextInput::maxLength,
                        XmlAttributeDescriptor.of("maxLength")
                                .category("Behavior")
                                .defaultValue("0")
                                .description("Maximum editable character count; 0 means unlimited."))
                .attribute("visualOnlyTextChanges", XmlValueParsers.BOOLEAN, TextInput::visualOnlyTextChanges,
                        XmlAttributeDescriptor.of("visualOnlyTextChanges")
                                .category("Behavior")
                                .defaultValue("false")
                                .description("Whether text changes invalidate visuals only instead of layout."))
                .attribute("cursorIndex", XmlValueParsers.INT, TextInput::cursorIndex,
                        XmlAttributeDescriptor.of("cursorIndex")
                                .category("Behavior")
                                .defaultValue("0")
                                .description("Initial cursor position in the editable text.")),
                widgetType);
    }

    private static <T extends ToolButton> WidgetXmlType<T> toolButton(WidgetXmlType<T> type, Class<?> widgetType) {
        return BuiltInWidgetXmlSupport.annotated(button(type), widgetType)
                .attribute("icon", XmlValueParsers.STRING, ToolButton::icon)
                .attribute("label", XmlValueParsers.STRING, ToolButton::label)
                .attribute("tooltip", XmlValueParsers.STRING, ToolButton::tooltip)
                .attribute("command", XmlValueParsers.STRING, ToolButton::commandId)
                .attribute("displayMode", XmlValueParsers.enumValue(ToolButton.DisplayMode.class), ToolButton::displayMode)
                .attribute("checked", XmlValueParsers.BOOLEAN, ToolButton::checked);
    }

    private static WidgetXmlType<RadioButton> radioButton(WidgetXmlType<RadioButton> type) {
        return BuiltInWidgetXmlSupport.annotated(button(type), RadioButton.class)
                .describe("Radio Button", "Controls", "Single radio option; group membership is currently wired by Java hosts.");
    }

    private static WidgetXmlType<ComboBox> comboBox(WidgetXmlType<ComboBox> type) {
        return BuiltInWidgetXmlSupport.annotated(BuiltInWidgetXmlSupport.commonWidget(type), ComboBox.class)
                .describe("Combo Box", "Controls", "Selectable dropdown list. Use the items attribute for simple string options.");
    }

    private static WidgetXmlType<DropDownBox> dropDownBox(WidgetXmlType<DropDownBox> type) {
        return BuiltInWidgetXmlSupport.annotated(BuiltInWidgetXmlSupport.commonWidget(type), DropDownBox.class)
                .describe("Drop Down Box", "Controls", "Dropdown host for arbitrary content widgets.")
                .childPolicy((widget, child) -> widget.content(child))
                .propertyChild("Content", (widget, child) -> widget.content(child),
                        XmlPropertyChildDescriptor.of("Content")
                                .category("Content")
                                .description("Single widget shown inside the dropdown content area.")
                                .singleChildOnly());
    }

    private static WidgetXmlType<TreeListPicker> treeListPicker(WidgetXmlType<TreeListPicker> type) {
        return BuiltInWidgetXmlSupport.commonWidget(type, TreeListPicker.class)
                .describe("Tree List Picker", "Controls", "Combo-box picker for path-like tree values. Use the items attribute for simple XML lists.");
    }

    private static WidgetXmlType<DatePicker> datePicker(WidgetXmlType<DatePicker> type) {
        return BuiltInWidgetXmlSupport.commonWidget(type, DatePicker.class)
                .describe("Date Picker", "Controls", "ISO-8601 date input with previous/next buttons and popup calendar.");
    }

    private static WidgetXmlType<ColorPicker> colorPicker(WidgetXmlType<ColorPicker> type) {
        return BuiltInWidgetXmlSupport.commonWidget(type, ColorPicker.class)
                .describe("Color Picker", "Controls", "Color selection control with HSV and ARGB editing modes.");
    }

    private static WidgetXmlType<LoadingIndicator> loadingIndicator(WidgetXmlType<LoadingIndicator> type) {
        return BuiltInWidgetXmlSupport.box(type, LoadingIndicator.class)
                .describe("Loading Indicator", "Feedback", "Animated activity indicator for spinner, dots and bar states.");
    }

    private static WidgetXmlType<Toast> toast(WidgetXmlType<Toast> type) {
        return BuiltInWidgetXmlSupport.box(type, Toast.class)
                .describe("Toast", "Feedback", "Transient message card for overlay surfaces.")
                .attribute("open", XmlValueParsers.BOOLEAN, BuiltInControlXml::openToast,
                        XmlAttributeDescriptor.of("open")
                                .category("Behavior")
                                .defaultValue("false")
                                .description("Whether the toast starts visible after XML load."));
    }

    private static WidgetXmlType<NotificationView> notificationView(WidgetXmlType<NotificationView> type) {
        return BuiltInWidgetXmlSupport.box(type, NotificationView.class)
                .describe("Notification View", "Feedback", "Overlay host for stacked transient notification cards.")
                .attribute("open", XmlValueParsers.BOOLEAN, BuiltInControlXml::openNotificationView,
                        XmlAttributeDescriptor.of("open")
                                .category("Behavior")
                                .defaultValue("false")
                                .description("Whether the pending notification text is shown after XML load."));
    }

    private static void openToast(Toast toast, boolean open) {
        if (open) {
            toast.show();
        } else {
            toast.hide();
        }
    }

    private static void openNotificationView(NotificationView view, boolean open) {
        if (open) {
            view.show();
        } else {
            view.hide();
        }
    }

    private static WidgetXmlType<ContextMenu> contextMenu(WidgetXmlType<ContextMenu> type) {
        XmlChildPolicy<ContextMenu> items = BuiltInControlXml::addContextMenuItem;
        return BuiltInWidgetXmlSupport.box(type, ContextMenu.class)
                .describe("Context Menu", "Feedback", "Floating menu with button items and separators.")
                .childPolicy(items)
                .propertyChild("Items", items,
                        XmlPropertyChildDescriptor.of("Items")
                                .category("Content")
                                .description("Menu item widgets. Button children become actions; Separator children become menu dividers."));
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

    private static WidgetXmlType<OverlayLayer> overlayLayer(WidgetXmlType<OverlayLayer> type) {
        return BuiltInWidgetXmlSupport.commonWidget(type, OverlayLayer.class)
                .describe("Overlay Layer", "Feedback", "Root host for normal content plus popups, tooltips and windows.")
                .attribute("modalScrimColor", XmlValueParsers.COLOR, (widget, color) -> widget.modalScrimColor().set(color),
                        XmlAttributeDescriptor.of("modalScrimColor")
                                .category("Appearance")
                                .defaultValue("#0000007A")
                                .description("Color drawn behind the top modal window."))
                .childPolicy(BuiltInControlXml::addOverlayLayerChild)
                .propertyChild("Content", BuiltInControlXml::setOverlayLayerContent,
                        XmlPropertyChildDescriptor.of("Content")
                                .category("Content")
                                .description("Single base content widget rendered below overlays.")
                                .singleChildOnly())
                .propertyChild("Overlays", BuiltInControlXml::addOverlayLayerOverlay,
                        XmlPropertyChildDescriptor.of("Overlays")
                                .category("Content")
                                .description("Overlay widgets rendered above the base content."));
    }

    private static void addOverlayLayerChild(OverlayLayer layer, Widget child) {
        if (layer.content() == null) {
            layer.content(child);
        } else {
            layer.addOverlay(child);
        }
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

    private static WidgetXmlType<Popup> popup(WidgetXmlType<Popup> type) {
        return BuiltInWidgetXmlSupport.box(type, Popup.class)
                .describe("Popup", "Feedback", "Overlay popup host for arbitrary content; anchor is wired by code.")
                .childPolicy(BuiltInControlXml::setPopupContent)
                .propertyChild("Content", BuiltInControlXml::setPopupContent,
                        XmlPropertyChildDescriptor.of("Content")
                                .category("Content")
                                .description("Single widget shown inside the popup.")
                                .singleChildOnly());
    }

    private static void setPopupContent(Popup popup, Widget child) {
        if (popup.content() != null && popup.content() != child) {
            throw new IllegalArgumentException("Widget Popup can contain only one content child.");
        }
        popup.content(child);
        popup.applyQueuedMutations();
    }

    private static WidgetXmlType<Tooltip> tooltip(WidgetXmlType<Tooltip> type) {
        return BuiltInWidgetXmlSupport.box(type, Tooltip.class)
                .describe("Tooltip", "Feedback", "Text tooltip overlay; anchor is wired by code.");
    }

    private static WidgetXmlType<WindowWidget> windowWidget(WidgetXmlType<WindowWidget> type) {
        return BuiltInWidgetXmlSupport.box(type, WindowWidget.class)
                .describe("Window", "Feedback", "Floating draggable and resizable overlay window.")
                .attribute("windowX", XmlValueParsers.FLOAT, (widget, value) -> widget.position(value, widget.windowY()),
                        XmlAttributeDescriptor.of("windowX")
                                .category("Layout")
                                .defaultValue("32")
                                .description("Window position inside its overlay host on the X axis."))
                .attribute("windowY", XmlValueParsers.FLOAT, (widget, value) -> widget.position(widget.windowX(), value),
                        XmlAttributeDescriptor.of("windowY")
                                .category("Layout")
                                .defaultValue("28")
                                .description("Window position inside its overlay host on the Y axis."))
                .childPolicy(BuiltInControlXml::setWindowContent)
                .propertyChild("Content", BuiltInControlXml::setWindowContent,
                        XmlPropertyChildDescriptor.of("Content")
                                .category("Content")
                                .description("Single widget shown inside the window body.")
                                .singleChildOnly());
    }

    private static void setWindowContent(WindowWidget window, Widget child) {
        if (window.content() != null && window.content() != child) {
            throw new IllegalArgumentException("Widget WindowWidget can contain only one content child.");
        }
        window.content(child);
        window.applyQueuedMutations();
    }

    private static WidgetXmlType<Checkbox> checkbox(WidgetXmlType<Checkbox> type) {
        return toggleButton(type)
                .attribute("state", XmlValueParsers.enumValue(CheckboxState.class), Checkbox::silentState)
                .attribute("checked", XmlValueParsers.BOOLEAN, Checkbox::silentChecked)
                .attribute("triState", XmlValueParsers.BOOLEAN, Checkbox::triState)
                .attribute("boxSize", XmlValueParsers.FLOAT, Checkbox::boxSize)
                .attribute("checkSize", XmlValueParsers.FLOAT, Checkbox::checkSize)
                .attribute("textGap", XmlValueParsers.FLOAT, Checkbox::textGap)
                .attribute("labelLeft", XmlValueParsers.BOOLEAN, Checkbox::labelLeft);
    }

    private static WidgetXmlType<Slider> slider(WidgetXmlType<Slider> type) {
        return BuiltInWidgetXmlSupport.box(type)
                .attribute("min", XmlValueParsers.FLOAT, (widget, value) -> widget.range(value, widget.max()))
                .attribute("max", XmlValueParsers.FLOAT, (widget, value) -> widget.range(widget.min(), value))
                .attribute("value", XmlValueParsers.FLOAT, Slider::value)
                .attribute("step", XmlValueParsers.FLOAT, Slider::step)
                .attribute("preferredWidth", XmlValueParsers.FLOAT, Slider::preferredWidth)
                .attribute("preferredHeight", XmlValueParsers.FLOAT, Slider::preferredHeight)
                .attribute("trackColor", XmlValueParsers.COLOR, (widget, color) -> widget.trackColor().set(color))
                .attribute("fillColor", XmlValueParsers.COLOR, (widget, color) -> widget.fillColor().set(color))
                .attribute("knobColor", XmlValueParsers.COLOR, (widget, color) -> widget.knobColor().set(color));
    }

    private static WidgetXmlType<ProgressBar> progressBar(WidgetXmlType<ProgressBar> type) {
        return BuiltInWidgetXmlSupport.box(type)
                .attribute("min", XmlValueParsers.FLOAT, (widget, value) -> widget.range(value, widget.max()))
                .attribute("max", XmlValueParsers.FLOAT, (widget, value) -> widget.range(widget.min(), value))
                .attribute("value", XmlValueParsers.FLOAT, ProgressBar::value)
                .attribute("indeterminate", XmlValueParsers.BOOLEAN, ProgressBar::indeterminate)
                .attribute("indeterminateSpeed", XmlValueParsers.FLOAT, ProgressBar::indeterminateSpeed)
                .attribute("preferredWidth", XmlValueParsers.FLOAT, ProgressBar::preferredWidth)
                .attribute("preferredHeight", XmlValueParsers.FLOAT, ProgressBar::preferredHeight)
                .attribute("trackColor", XmlValueParsers.COLOR, (widget, color) -> widget.trackColor().set(color))
                .attribute("fillColor", XmlValueParsers.COLOR, (widget, color) -> widget.fillColor().set(color));
    }
}
