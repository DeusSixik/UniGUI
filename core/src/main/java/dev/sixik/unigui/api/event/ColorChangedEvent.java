package dev.sixik.unigui.api.event;

import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.widget.Widget;

import java.util.Objects;

public final class ColorChangedEvent extends BaseEvent implements RoutableWidgetEvent {
    public static final EventType<ColorChangedEvent> TYPE = EventType.create("color_picker.color_changed");

    private final Widget target;
    private final Widget currentTarget;
    private final EventPhase phase;
    private final MutableColor oldColor;
    private final MutableColor newColor;

    public ColorChangedEvent(Widget target, ColorView oldColor, ColorView newColor) {
        this(target, target, EventPhase.TARGET, oldColor, newColor);
    }

    public ColorChangedEvent(Widget target, Widget currentTarget, EventPhase phase,
                             ColorView oldColor, ColorView newColor) {
        this.target = Objects.requireNonNull(target, "target");
        this.currentTarget = Objects.requireNonNull(currentTarget, "currentTarget");
        this.phase = Objects.requireNonNull(phase, "phase");
        this.oldColor = copy(oldColor);
        this.newColor = copy(newColor);
    }

    @Override
    public EventType<ColorChangedEvent> type() {
        return TYPE;
    }

    @Override
    public Widget target() {
        return target;
    }

    @Override
    public Widget currentTarget() {
        return currentTarget;
    }

    @Override
    public EventPhase phase() {
        return phase;
    }

    public ColorView oldColor() {
        return oldColor;
    }

    public ColorView newColor() {
        return newColor;
    }

    public int oldArgb() {
        return argb(oldColor);
    }

    public int newArgb() {
        return argb(newColor);
    }

    @Override
    public ColorChangedEvent routeTo(Widget currentTarget, EventPhase phase) {
        ColorChangedEvent event = new ColorChangedEvent(target, currentTarget, phase, oldColor, newColor);
        if (isCancelled()) {
            event.cancel();
        }
        return event;
    }

    private static MutableColor copy(ColorView color) {
        if (color == null) return new MutableColor(0.0f, 0.0f, 0.0f, 0.0f);
        return new MutableColor(color.r(), color.g(), color.b(), color.a());
    }

    private static int argb(ColorView color) {
        return (channel(color.a()) << 24)
                | (channel(color.r()) << 16)
                | (channel(color.g()) << 8)
                | channel(color.b());
    }

    private static int channel(float value) {
        return Math.max(0, Math.min(255, Math.round(value * 255.0f)));
    }
}

