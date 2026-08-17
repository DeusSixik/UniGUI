package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.math.ColorView;

import java.util.List;

public record TextAreaState(
        float x,
        float y,
        float width,
        float height,
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
    public TextAreaState {
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
