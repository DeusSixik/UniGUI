package dev.sixik.unigui.widgets.interaction;

import dev.sixik.unigui.api.event.ButtonClickEvent;
import dev.sixik.unigui.api.event.CheckedChangedEvent;
import dev.sixik.unigui.api.event.EventListener;
import dev.sixik.unigui.api.event.EventSubscription;
import dev.sixik.unigui.api.xml.XmlWidgetName;

/** Toolbar button that toggles its checked state when clicked. */
@XmlWidgetName("ToggleToolButton")
public final class ToggleToolButton extends ToolButton {
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
