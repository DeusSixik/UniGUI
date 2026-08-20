package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.math.MutableColor;

import java.util.List;

public record TextAreaState(
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
        float horizontalScrollPixels,
        float verticalScrollPixels,
        float lineHeight,
        float textHeight,
        float measuredTextWidth,
        int totalLineCount,
        List<TextAreaLineState> lines,
        boolean focused,
        boolean showingPlaceholder,
        boolean hasSelection,
        int selectionStart,
        int selectionEnd,
        int cursorIndex,
        ColorView textColor,
        ColorView placeholderColor,
        ColorView caretColor,
        float approximateCharWidth
) {
    private static final MutableColor TRANSPARENT = new MutableColor(0.0f, 0.0f, 0.0f, 0.0f);

    public TextAreaState {
        backgroundColor = backgroundColor == null ? TRANSPARENT : backgroundColor;
        borderColor = borderColor == null ? TRANSPARENT : borderColor;
        radius = Math.max(0.0f, radius);
        borderWidth = Math.max(0.0f, borderWidth);
        lines = lines == null ? List.of() : List.copyOf(lines);
    }

    public TextAreaLineState cursorLine() {
        if (lines.isEmpty()) return null;
        for (TextAreaLineState line : lines) {
            if (cursorIndex >= line.startIndex() && cursorIndex <= line.endIndex()) {
                return line;
            }
        }
        return cursorIndex < lines.get(0).startIndex() ? lines.get(0) : lines.get(lines.size() - 1);
    }
}