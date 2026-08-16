package dev.sixik.unigui.impl.xml;

import dev.sixik.unigui.api.widget.CheckboxState;
import dev.sixik.unigui.widgets.feedback.ProgressBar;
import dev.sixik.unigui.widgets.interaction.Button;
import dev.sixik.unigui.widgets.interaction.Checkbox;
import dev.sixik.unigui.widgets.interaction.Slider;
import dev.sixik.unigui.widgets.interaction.ToggleButton;

final class BuiltInControlXml {
    private BuiltInControlXml() {
    }

    static void register(WidgetXmlRegistry registry) {
        button(registry.register("Button", Button::new));
        toggleButton(registry.register("ToggleButton", ToggleButton::new));
        checkbox(registry.register("Checkbox", Checkbox::new));
        slider(registry.register("Slider", Slider::new));
        progressBar(registry.register("ProgressBar", ProgressBar::new));
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
