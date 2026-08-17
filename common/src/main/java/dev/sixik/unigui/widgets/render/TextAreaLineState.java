package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.text.RichText;

import java.util.Arrays;

public record TextAreaLineState(
        int lineIndex,
        int startIndex,
        int endIndex,
        float y,
        float height,
        float measuredWidth,
        String visibleText,
        RichText richText,
        float[] prefixWidths
) {
    public TextAreaLineState {
        visibleText = visibleText == null ? "" : visibleText;
        prefixWidths = prefixWidths == null ? new float[]{0.0f} : Arrays.copyOf(prefixWidths, prefixWidths.length);
    }

    public boolean hasVisibleText() {
        return richText != null && !richText.isEmpty();
    }

    public int length() {
        return Math.max(0, endIndex - startIndex);
    }

    public float prefixWidth(int localIndex) {
        if (prefixWidths.length == 0) return 0.0f;
        int clamped = Math.max(0, Math.min(localIndex, prefixWidths.length - 1));
        return prefixWidths[clamped];
    }
}
