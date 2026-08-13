package dev.sixik.unigui.api.text;

import dev.sixik.unigui.api.math.ColorView;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
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
        List<TextRun> normalized = new ObjectArrayList<>();
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
        List<TextRun> combined = new ObjectArrayList<>(runs.size() + other.runs.size());
        combined.addAll(runs);
        combined.addAll(other.runs);
        return new RichText(combined);
    }

    public RichText slice(int startInclusive, int endExclusive) {
        int start = Math.max(0, Math.min(startInclusive, plainText.length()));
        int end = Math.max(start, Math.min(endExclusive, plainText.length()));
        if (start == 0 && end == plainText.length()) return this;
        if (start == end) return RichText.plain("");

        List<TextRun> sliced = new ObjectArrayList<>();
        int runStart = 0;
        for (TextRun run : runs) {
            String value = run.text();
            int runEnd = runStart + value.length();
            int overlapStart = Math.max(start, runStart);
            int overlapEnd = Math.min(end, runEnd);
            if (overlapStart < overlapEnd) {
                sliced.add(new TextRun(
                        value.substring(overlapStart - runStart, overlapEnd - runStart),
                        run.font(),
                        run.pixelSize(),
                        run.color(),
                        run.tracking(),
                        run.transform()));
            }
            runStart = runEnd;
            if (runStart >= end) break;
        }
        return new RichText(sliced);
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
        private final List<TextRun> runs = new ObjectArrayList<>();
        private FontFace font;
        private float pixelSize = TextRun.DEFAULT_PIXEL_SIZE;
        private ColorView color;
        private float tracking;
        private TextTransform transform = TextTransform.NONE;

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

        public Builder tracking(float tracking) {
            this.tracking = Float.isFinite(tracking) ? Math.max(0.0f, tracking) : 0.0f;
            return this;
        }

        public Builder transform(TextTransform transform) {
            this.transform = transform == null ? TextTransform.NONE : transform;
            return this;
        }

        public Builder uppercase() {
            return transform(TextTransform.UPPERCASE);
        }

        public Builder append(String text) {
            TextRun run = new TextRun(text, font, pixelSize, color, tracking, transform);
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
