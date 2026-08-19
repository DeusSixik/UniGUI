package dev.sixik.unigui.widgets.interaction;

import dev.sixik.unigui.api.math.ColorView;

/** Visual style applied to a tokenizer range in {@link CodeEditor}. */
public record TokenStyle(ColorView color) {
    public static final TokenStyle NONE = new TokenStyle(null);

    public TokenStyle {
        color = color == null ? null : new SnapshotColor(color.r(), color.g(), color.b(), color.a());
    }

    public static TokenStyle color(ColorView color) {
        return new TokenStyle(color);
    }

    public boolean hasColor() {
        return color != null && color.a() > 0.0f;
    }

    private record SnapshotColor(float r, float g, float b, float a) implements ColorView {
    }
}