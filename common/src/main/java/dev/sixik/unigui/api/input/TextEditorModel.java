package dev.sixik.unigui.api.input;

import java.util.Objects;

public final class TextEditorModel {
    private String text = "";
    private int cursorIndex;
    private int selectionAnchor = -1;
    private int selectionFocus = -1;
    private int maxLength = Integer.MAX_VALUE;
    private ChangeListener changeListener = ChangeListener.NOOP;

    public String text() {
        return text;
    }

    public boolean text(String text) {
        return setText(normalize(text), true);
    }

    public boolean silentText(String text) {
        return setText(normalize(text), false);
    }

    public int cursorIndex() {
        return cursorIndex;
    }

    public boolean cursorIndex(int cursorIndex) {
        int clamped = clamp(cursorIndex, 0, text.length());
        boolean changed = this.cursorIndex != clamped;
        this.cursorIndex = clamped;
        return clearSelection() || changed;
    }

    public int selectionStart() {
        return hasSelection() ? Math.min(selectionAnchor, selectionFocus) : cursorIndex;
    }

    public int selectionEnd() {
        return hasSelection() ? Math.max(selectionAnchor, selectionFocus) : cursorIndex;
    }

    public boolean hasSelection() {
        return selectionAnchor >= 0 && selectionFocus >= 0 && selectionAnchor != selectionFocus;
    }

    public String selectedText() {
        return hasSelection() ? text.substring(selectionStart(), selectionEnd()) : "";
    }

    public boolean select(int start, int end) {
        int normalizedAnchor = clamp(start, 0, text.length());
        int normalizedFocus = clamp(end, 0, text.length());
        int normalizedCursor = Math.max(normalizedAnchor, normalizedFocus);
        boolean changed = selectionAnchor != normalizedAnchor || selectionFocus != normalizedFocus || cursorIndex != normalizedCursor;
        selectionAnchor = normalizedAnchor;
        selectionFocus = normalizedFocus;
        cursorIndex = normalizedCursor;
        return changed;
    }

    public boolean selectAll() {
        return select(0, text.length());
    }

    public boolean clearSelection() {
        if (selectionAnchor < 0 && selectionFocus < 0) return false;
        selectionAnchor = -1;
        selectionFocus = -1;
        return true;
    }

    public int maxLength() {
        return maxLength;
    }

    public boolean maxLength(int maxLength) {
        int normalized = Math.max(0, maxLength);
        if (this.maxLength == normalized) return false;
        this.maxLength = normalized;
        boolean changed = false;
        if (text.length() > normalized) {
            changed = setText(text.substring(0, normalized), true);
        }
        cursorIndex = clamp(cursorIndex, 0, text.length());
        if (selectionAnchor > text.length() || selectionFocus > text.length()) {
            changed = clearSelection() || changed;
        }
        return changed;
    }

    public boolean insertCodePoint(int codePoint) {
        if (!isPrintable(codePoint)) return false;
        return insertText(new String(Character.toChars(codePoint)));
    }

    public boolean insertText(String text) {
        String inserted = normalize(text);
        int start = selectionStart();
        int end = selectionEnd();
        int baseLength = this.text.length() - Math.max(0, end - start);
        int remaining = Math.max(0, maxLength - baseLength);
        if (inserted.length() > remaining) {
            inserted = inserted.substring(0, remaining);
        }
        if (inserted.isEmpty() && !hasSelection()) return false;

        String nextText = this.text.substring(0, start) + inserted + this.text.substring(end);
        boolean changed = setText(nextText, true);
        cursorIndex = start + inserted.length();
        selectionAnchor = -1;
        selectionFocus = -1;
        return changed;
    }

    public boolean backspace() {
        if (deleteSelectionIfNeeded()) return true;
        if (cursorIndex <= 0 || text.isEmpty()) return false;
        int previous = text.offsetByCodePoints(cursorIndex, -1);
        String nextText = text.substring(0, previous) + text.substring(cursorIndex);
        boolean changed = setText(nextText, true);
        cursorIndex = previous;
        return changed;
    }

    public boolean delete() {
        if (deleteSelectionIfNeeded()) return true;
        if (cursorIndex >= text.length() || text.isEmpty()) return false;
        int next = text.offsetByCodePoints(cursorIndex, 1);
        return setText(text.substring(0, cursorIndex) + text.substring(next), true);
    }

    public boolean deleteSelectionIfNeeded() {
        if (!hasSelection()) return false;
        return insertText("");
    }

    public TextEditorModel onChanged(ChangeListener changeListener) {
        this.changeListener = changeListener == null ? ChangeListener.NOOP : changeListener;
        return this;
    }

    public static boolean isPrintable(int codePoint) {
        return codePoint >= 32 && !Character.isISOControl(codePoint);
    }

    public static String sanitizePrintable(String text) {
        String normalized = normalize(text);
        StringBuilder builder = new StringBuilder(normalized.length());
        normalized.codePoints()
                .filter(TextEditorModel::isPrintable)
                .forEach(builder::appendCodePoint);
        return builder.toString();
    }

    private boolean setText(String text, boolean emitChange) {
        String normalized = trimToMax(normalize(text));
        boolean changed = !Objects.equals(this.text, normalized);
        if (!changed) {
            cursorIndex = clamp(cursorIndex, 0, this.text.length());
            if (selectionAnchor > this.text.length() || selectionFocus > this.text.length()) {
                clearSelection();
            }
            return false;
        }

        String oldText = this.text;
        this.text = normalized;
        cursorIndex = clamp(cursorIndex, 0, this.text.length());
        if (selectionAnchor > this.text.length() || selectionFocus > this.text.length()) {
            clearSelection();
        }
        if (emitChange) {
            changeListener.onTextChanged(oldText, this.text);
        }
        return true;
    }

    private String trimToMax(String value) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private static String normalize(String text) {
        return text == null ? "" : text;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    @FunctionalInterface
    public interface ChangeListener {
        ChangeListener NOOP = (oldText, newText) -> {
        };

        void onTextChanged(String oldText, String newText);
    }
}
