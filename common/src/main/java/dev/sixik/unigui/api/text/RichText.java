package dev.sixik.unigui.api.text;

import dev.sixik.unigui.api.math.ColorView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Immutable rich text made of runs that may use different font faces, sizes
 * and colors. The value contains no Minecraft or renderer-specific objects.
 */
public final class RichText {
    private final List<TextRun> runs;
    private final String plainText;

    public RichText(List<TextRun> runs) {
        List<TextRun> normalized = new ArrayList<>();
        if (runs != null) {
            for (TextRun run : runs) {
                if (run != null && !run.isEmpty()) normalized.add(run);
            }
        }
        this.runs = Collections.unmodifiableList(normalized);
        StringBuilder text = new StringBuilder();
        for (TextRun run : normalized) text.append(run.text());
        this.plainText = text.toString();
    }

    public static RichText plain(String text) {
        return new RichText(List.of(new TextRun(text, null, TextRun.DEFAULT_PIXEL_SIZE)));
    }

    public static RichText of(String text, FontFace font, float pixelSize) {
        return new RichText(List.of(new TextRun(text, font, pixelSize)));
    }

    public static RichText of(String text, FontFace font, float pixelSize, ColorView color) {
        return new RichText(List.of(new TextRun(text, font, pixelSize, color)));
    }

    public static Builder builder() {
        return new Builder();
    }

    public List<TextRun> runs() {
        return runs;
    }

    public String plainText() {
        return plainText;
    }

    public boolean isEmpty() {
        return runs.isEmpty() || plainText.isEmpty();
    }

    public RichText append(RichText other) {
        if (other == null || other.isEmpty()) return this;
        List<TextRun> combined = new ArrayList<>(runs.size() + other.runs.size());
        combined.addAll(runs);
        combined.addAll(other.runs);
        return new RichText(combined);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        return other instanceof RichText richText && runs.equals(richText.runs);
    }

    @Override
    public int hashCode() {
        return runs.hashCode();
    }

    @Override
    public String toString() {
        return plainText;
    }

    public static final class Builder {
        private final List<TextRun> runs = new ArrayList<>();
        private FontFace font;
        private float pixelSize = TextRun.DEFAULT_PIXEL_SIZE;
        private ColorView color;

        public Builder font(FontFace font) {
            this.font = font;
            return this;
        }

        public Builder size(float pixelSize) {
            this.pixelSize = pixelSize;
            return this;
        }

        public Builder color(ColorView color) {
            this.color = color;
            return this;
        }

        public Builder append(String text) {
            TextRun run = new TextRun(text, font, pixelSize, color);
            if (!run.isEmpty()) runs.add(run);
            return this;
        }

        public Builder append(RichText text) {
            if (text != null) runs.addAll(text.runs);
            return this;
        }

        public RichText build() {
            return new RichText(runs);
        }
    }
}
