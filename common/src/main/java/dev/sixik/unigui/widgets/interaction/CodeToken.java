package dev.sixik.unigui.widgets.interaction;

/** Syntax token range in UTF-16 text indexes. Boundaries must be code-point safe. */
public record CodeToken(int startIndex, int endIndex, TokenStyle style) {
    public CodeToken {
        int start = Math.max(0, startIndex);
        int end = Math.max(start, endIndex);
        startIndex = start;
        endIndex = end;
        style = style == null ? TokenStyle.NONE : style;
    }

    public boolean isEmpty() {
        return endIndex <= startIndex;
    }
}