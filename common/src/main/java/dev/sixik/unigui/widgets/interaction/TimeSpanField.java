package dev.sixik.unigui.widgets.interaction;

import dev.sixik.unigui.api.event.Event;
import dev.sixik.unigui.api.event.FocusLostEvent;
import dev.sixik.unigui.api.event.TextInputEvent;

import java.time.Duration;
import java.util.Locale;

public final class TimeSpanField extends TextField {
    private Duration value = Duration.ZERO;

    public TimeSpanField() {
        placeholder("HH:MM:SS");
        text("00:00:00");
    }

    public Duration value() {
        return value;
    }

    public TimeSpanField value(Duration value) {
        this.value = value == null ? Duration.ZERO : value;
        super.text(format(this.value));
        return this;
    }

    @Override
    public void handle(Event event) {
        if (event instanceof TextInputEvent input && !isAllowed(input.codePoint())) {
            event.cancel();
            return;
        }
        super.handle(event);
        if (event instanceof FocusLostEvent) {
            syncFromText();
        }
    }

    @Override
    protected String sanitizeTextInput(String text) {
        StringBuilder builder = new StringBuilder(text.length());
        text.codePoints()
                .filter(TimeSpanField::isAllowed)
                .forEach(builder::appendCodePoint);
        return builder.toString();
    }

    private void syncFromText() {
        value = parse(text());
        super.text(format(value));
    }

    private static Duration parse(String text) {
        if (text == null || text.isBlank()) return Duration.ZERO;
        String[] parts = text.trim().split(":");
        try {
            long seconds = 0L;
            for (String part : parts) {
                seconds = seconds * 60L + Long.parseLong(part.trim());
            }
            return Duration.ofSeconds(Math.max(0L, seconds));
        } catch (NumberFormatException ignored) {
            return Duration.ZERO;
        }
    }

    private static String format(Duration duration) {
        long seconds = Math.max(0L, duration == null ? 0L : duration.getSeconds());
        long hours = seconds / 3600L;
        long minutes = (seconds % 3600L) / 60L;
        long secs = seconds % 60L;
        return String.format(Locale.ROOT, "%02d:%02d:%02d", hours, minutes, secs);
    }

    private static boolean isAllowed(int codePoint) {
        return (codePoint >= '0' && codePoint <= '9') || codePoint == ':';
    }
}
