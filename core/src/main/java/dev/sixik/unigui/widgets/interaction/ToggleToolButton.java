package dev.sixik.unigui.widgets.interaction;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiLink;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;
import dev.sixik.unigui.api.event.ButtonClickEvent;
import dev.sixik.unigui.api.event.CheckedChangedEvent;
import dev.sixik.unigui.api.event.EventListener;
import dev.sixik.unigui.api.event.EventSubscription;
import dev.sixik.unigui.api.xml.XmlWidgetName;

/** Toolbar button that toggles its checked state when clicked. */
@AiDoc(
        summary = "Toolbar button that toggles and stays highlighted while checked.",
        purpose = "Provides two-state button behavior formatted specifically for toolbars (e.g. Bold, Italic, Pin).",
        useWhen = {"Toolbar modes and active editing states."},
        related = {"ToolButton", "ToggleButton"}
)
@WikiPage(
        title = "<key:toggle.tool.button.title>",
        slug = "toggle-tool-button",
        category = "<key:core.widgets.interaction.category>",
        summary = "<key:toggle.tool.button.summary>",
        order = 60
)
@WikiSection(
        title = "<key:toggle.tool.button.section.overview.title>",
        markdown = "<key:toggle.tool.button.section.overview.body>",
        order = 10
)
@WikiExample(
        title = "<key:toggle.tool.button.example.basic.title>",
        code = "<key:toggle.tool.button.example.basic.code>",
        order = 10
)
@WikiLink(label = "ToolButton", target = "page:tool-button")
@WikiLink(label = "ToggleButton", target = "page:toggle-button")
@XmlWidgetName("ToggleToolButton")
public final class ToggleToolButton extends ToolButton {
    public static final String STYLE_TYPE = dev.sixik.unigui.api.style.StyleIds.Widget.TOGGLE_TOOL_BUTTON;

    public ToggleToolButton() {
        onClick(this::toggleOnClick);
    }

    public ToggleToolButton(String label) {
        this();
        label(label);
    }

    @Override
    public ToggleToolButton checked(boolean checked) {
        boolean oldValue = checked();
        super.checked(checked);
        if (oldValue != checked) {
            emit(new CheckedChangedEvent(this, oldValue, checked));
        }
        return this;
    }

    @Override
    public ToggleToolButton icon(String icon) {
        super.icon(icon);
        return this;
    }

    @Override
    public ToggleToolButton label(String label) {
        super.label(label);
        return this;
    }

    @Override
    public ToggleToolButton displayMode(DisplayMode displayMode) {
        super.displayMode(displayMode);
        return this;
    }

    public EventSubscription onCheckedChanged(EventListener<? super CheckedChangedEvent> listener) {
        return on(CheckedChangedEvent.TYPE, listener);
    }

    private void toggleOnClick(ButtonClickEvent ignored) {
        if (enabled()) checked(!checked());
    }
}
