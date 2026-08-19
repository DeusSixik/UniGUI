package dev.sixik.unigui.widgets.interaction;

/** Immutable input passed to a {@link CompletionProvider}. */
public record CompletionContext(String text,
                                String languageId,
                                int cursorIndex,
                                int selectionStart,
                                int selectionEnd) {
    public CompletionContext {
        text = text == null ? "" : text;
        languageId = languageId == null ? "" : languageId.trim();
        cursorIndex = clamp(cursorIndex, 0, text.length());
        selectionStart = clamp(selectionStart, 0, text.length());
        selectionEnd = clamp(selectionEnd, 0, text.length());
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}