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
        int clamped = clampToCodePointBoundary(text, cursorIndex);
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

    public int selectionAnchor() {
        return selectionAnchor;
    }

    public int selectionFocus() {
        return selectionFocus;
    }

    public boolean hasSelection() {
        return selectionAnchor >= 0 && selectionFocus >= 0 && selectionAnchor != selectionFocus;
    }

    public String selectedText() {
        return hasSelection() ? text.substring(selectionStart(), selectionEnd()) : "";
    }

    public boolean select(int start, int end) {
        int normalizedAnchor = clampToCodePointBoundary(text, start);
        int normalizedFocus = clampToCodePointBoundary(text, end);
        int normalizedCursor = normalizedFocus;
        boolean changed = selectionAnchor != normalizedAnchor || selectionFocus != normalizedFocus || cursorIndex != normalizedCursor;
        selectionAnchor = normalizedAnchor;
        selectionFocus = normalizedFocus;
        cursorIndex = normalizedCursor;
        return changed;
    }

    public boolean moveCursor(int cursorIndex, boolean extendSelection) {
        int clamped = clampToCodePointBoundary(text, cursorIndex);
        if (extendSelection) {
            int anchor = selectionAnchor >= 0 ? selectionAnchor : this.cursorIndex;
            return select(anchor, clamped);
        }

        boolean changed = this.cursorIndex != clamped;
        this.cursorIndex = clamped;
        return clearSelection() || changed;
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
            changed = setText(text.substring(0, clampToCodePointBoundary(text, normalized)), true);
        }
        cursorIndex = clampToCodePointBoundary(text, cursorIndex);
        normalizeSelectionBounds();
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
            inserted = inserted.substring(0, clampToCodePointBoundary(inserted, remaining));
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

    public boolean backspaceWord() {
        if (deleteSelectionIfNeeded()) return true;
        if (cursorIndex <= 0 || text.isEmpty()) return false;
        int start = previousWordBoundary(cursorIndex);
        if (start == cursorIndex) return false;
        String nextText = text.substring(0, start) + text.substring(cursorIndex);
        boolean changed = setText(nextText, true);
        cursorIndex = start;
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

    public static int clampToCodePointBoundary(String text, int index) {
        String value = normalize(text);
        int clamped = clamp(index, 0, value.length());
        if (clamped > 0
                && clamped < value.length()
                && Character.isLowSurrogate(value.charAt(clamped))
                && Character.isHighSurrogate(value.charAt(clamped - 1))) {
            return clamped - 1;
        }
        return clamped;
    }

    private boolean setText(String text, boolean emitChange) {
        String normalized = trimToMax(normalize(text));
        boolean changed = !Objects.equals(this.text, normalized);
        if (!changed) {
            cursorIndex = clampToCodePointBoundary(this.text, cursorIndex);
            normalizeSelectionBounds();
            return false;
        }

        String oldText = this.text;
        this.text = normalized;
        cursorIndex = clampToCodePointBoundary(this.text, cursorIndex);
        normalizeSelectionBounds();
        if (emitChange) {
            changeListener.onTextChanged(oldText, this.text);
        }
        return true;
    }

    private int previousWordBoundary(int index) {
        int position = clampToCodePointBoundary(text, index);
        while (position > 0) {
            int previous = text.offsetByCodePoints(position, -1);
            int codePoint = text.codePointAt(previous);
            if (!Character.isWhitespace(codePoint)) break;
            position = previous;
        }
        if (position <= 0) return 0;

        int previous = text.offsetByCodePoints(position, -1);
        boolean word = isWordCodePoint(text.codePointAt(previous));
        while (position > 0) {
            previous = text.offsetByCodePoints(position, -1);
            int codePoint = text.codePointAt(previous);
            if (Character.isWhitespace(codePoint)) break;
            if (isWordCodePoint(codePoint) != word) break;
            position = previous;
        }
        return position;
    }

    private static boolean isWordCodePoint(int codePoint) {
        return Character.isLetterOrDigit(codePoint) || codePoint == '_';
    }

    private String trimToMax(String value) {
        return value.length() <= maxLength ? value : value.substring(0, clampToCodePointBoundary(value, maxLength));
    }

    private void normalizeSelectionBounds() {
        if (selectionAnchor >= 0) selectionAnchor = clampToCodePointBoundary(text, selectionAnchor);
        if (selectionFocus >= 0) selectionFocus = clampToCodePointBoundary(text, selectionFocus);
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
