package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.text.RichText;

import java.util.Arrays;

public record TextInputState(
        TextInputRenderType type,
        float x,
        float y,
        float width,
        float height,
        boolean backgroundVisible,
        ColorView backgroundColor,
        float radius,
        boolean borderVisible,
        ColorView borderColor,
        float borderWidth,
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
        boolean clearButtonHovered,
        float clearButtonX,
        float clearButtonY,
        float clearButtonWidth,
        float clearButtonHeight
) {
    private static final MutableColor TRANSPARENT = new MutableColor(0.0f, 0.0f, 0.0f, 0.0f);

    public TextInputState {
        backgroundColor = backgroundColor == null ? TRANSPARENT : backgroundColor;
        borderColor = borderColor == null ? TRANSPARENT : borderColor;
        radius = Math.max(0.0f, radius);
        borderWidth = Math.max(0.0f, borderWidth);
        prefixWidths = prefixWidths == null ? new float[]{0.0f} : Arrays.copyOf(prefixWidths, prefixWidths.length);
    }

    /**
     * Совместимый конструктор старого формата без состояния hover clear-зоны.
     * Сторонние renderers, создающие state вручную, сохраняют прежнее поведение.
     */
    public TextInputState(
            TextInputRenderType type,
            float x, float y, float width, float height,
            boolean backgroundVisible, ColorView backgroundColor, float radius,
            boolean borderVisible, ColorView borderColor, float borderWidth,
            float viewportX, float viewportY, float viewportWidth, float viewportHeight,
            float textY, float textHeight, float horizontalScrollPixels, float measuredTextWidth,
            String visibleText, RichText richText,
            boolean focused, boolean showingPlaceholder, boolean hasSelection,
            int selectionStart, int selectionEnd, int cursorIndex,
            ColorView textColor, ColorView placeholderColor, ColorView caretColor,
            float[] prefixWidths, boolean clearButtonVisible,
            float clearButtonX, float clearButtonY, float clearButtonWidth, float clearButtonHeight) {
        this(type, x, y, width, height, backgroundVisible, backgroundColor, radius,
                borderVisible, borderColor, borderWidth, viewportX, viewportY,
                viewportWidth, viewportHeight, textY, textHeight, horizontalScrollPixels,
                measuredTextWidth, visibleText, richText, focused, showingPlaceholder,
                hasSelection, selectionStart, selectionEnd, cursorIndex, textColor,
                placeholderColor, caretColor, prefixWidths, clearButtonVisible, false,
                clearButtonX, clearButtonY, clearButtonWidth, clearButtonHeight);
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
