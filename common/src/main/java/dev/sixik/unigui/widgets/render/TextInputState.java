package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.text.RichText;

import java.util.Arrays;

public record TextInputState(
        TextInputRenderType type,
        float x,
        float y,
        float width,
        float height,
        float viewportX,
        float viewportY,
        float viewportWidth,
        float viewportHeight,
        float textY,
        float textHeight,
        float horizontalScrollPixels,
        float measuredTextWidth,
        String visibleText,
        RichText richText,
        boolean focused,
        boolean showingPlaceholder,
        boolean hasSelection,
        int selectionStart,
        int selectionEnd,
        int cursorIndex,
        ColorView textColor,
        ColorView placeholderColor,
        ColorView caretColor,
        float[] prefixWidths,
        boolean clearButtonVisible,
        float clearButtonX,
        float clearButtonY,
        float clearButtonWidth,
        float clearButtonHeight
) {
    public TextInputState {
        prefixWidths = prefixWidths == null ? new float[]{0.0f} : Arrays.copyOf(prefixWidths, prefixWidths.length);
    }

    public boolean hasVisibleText() {
        return richText != null && !richText.isEmpty();
    }

    public float prefixWidth(int index) {
        if (prefixWidths.length == 0) return 0.0f;
        int clamped = Math.max(0, Math.min(index, prefixWidths.length - 1));
        return prefixWidths[clamped];
    }
}
