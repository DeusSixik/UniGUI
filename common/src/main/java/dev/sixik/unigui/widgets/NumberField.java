package dev.sixik.unigui.widgets;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.event.Event;
import dev.sixik.unigui.api.event.EventListener;
import dev.sixik.unigui.api.event.EventSubscription;
import dev.sixik.unigui.api.event.KeyPressedEvent;
import dev.sixik.unigui.api.event.NumberValueChangedEvent;
import dev.sixik.unigui.api.event.TextInputEvent;
import dev.sixik.unigui.api.input.KeyCodes;

import java.util.Locale;

public class NumberField extends TextField {
    private double min = -Double.MAX_VALUE;
    private double max = Double.MAX_VALUE;
    private double value;
    private double step = 1.0d;

    public NumberField() {
        text("0");
    }

    public double value() {
        return value;
    }

    public NumberField value(double value) {
        setNumberValue(value, true);
        return this;
    }

    public NumberField silentValue(double value) {
        setNumberValue(value, false);
        return this;
    }

    public NumberField range(double min, double max) {
        if (max < min) {
            double swap = min;
            min = max;
            max = swap;
        }
        this.min = min;
        this.max = max;
        setNumberValue(value, false);
        return this;
    }

    public NumberField step(double step) {
        this.step = Math.max(0.0d, step);
        return this;
    }

    public EventSubscription onValueChanged(EventListener<? super NumberValueChangedEvent> listener) {
        return on(NumberValueChangedEvent.TYPE, listener);
    }

    @Override
    protected String sanitizeTextInput(String text) {
        StringBuilder builder = new StringBuilder(text.length());
        text.codePoints()
                .filter(NumberField::isAllowed)
                .forEach(builder::appendCodePoint);
        return builder.toString();
    }

    @Override
    public void handle(Event event) {
        if (event instanceof TextInputEvent input && !isAllowed(input.codePoint())) {
            event.cancel();
            return;
        }

        super.handle(event);
        if (event.isCancelled()) {
            syncValueFromText();
            return;
        }

        if (event instanceof KeyPressedEvent key && focused()) {
            if (key.keyCode() == KeyCodes.UP) {
                setNumberValue(value + stepOrDefault(), true);
                event.cancel();
            } else if (key.keyCode() == KeyCodes.DOWN) {
                setNumberValue(value - stepOrDefault(), true);
                event.cancel();
            }
        }
    }

    private void syncValueFromText() {
        String text = text();
        if (text.isEmpty() || text.equals("-") || text.equals(".") || text.equals("-.")) return;
        try {
            setNumberValue(Double.parseDouble(text), true);
        } catch (NumberFormatException ignored) {
            // Keep editing text until it becomes a parseable number.
        }
    }

    private void setNumberValue(double value, boolean emitChange) {
        double normalized = clamp(value, min, max);
        if (Double.compare(this.value, normalized) == 0) {
            syncTextToValue(normalized);
            return;
        }
        double oldValue = this.value;
        this.value = normalized;
        syncTextToValue(normalized);
        invalidate(InvalidationFlags.VISUAL);
        if (emitChange) {
            emit(new NumberValueChangedEvent(this, oldValue, normalized));
        }
    }

    private void syncTextToValue(double value) {
        String rendered = renderNumber(value);
        if (!text().equals(rendered)) {
            super.text(rendered);
            cursorIndex(text().length());
        }
    }

    private double stepOrDefault() {
        return step > 0.0d ? step : 1.0d;
    }

    private static boolean isAllowed(int codePoint) {
        return (codePoint >= '0' && codePoint <= '9') || codePoint == '-' || codePoint == '.';
    }

    private static String renderNumber(double value) {
        if (value == Math.rint(value)) {
            return Long.toString((long) value);
        }
        return String.format(Locale.ROOT, "%s", value);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
