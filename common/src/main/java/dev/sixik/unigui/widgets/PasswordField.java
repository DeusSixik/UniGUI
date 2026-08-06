package dev.sixik.unigui.widgets;

import dev.sixik.unigui.api.core.InvalidationFlags;

public class PasswordField extends TextInput {
    private char mask = '\u2022';

    public PasswordField() {
        enableDefaultTextInputChrome();
    }

    public PasswordField(String text) {
        this();
        editorModel().silentText(text);
        editorModel().cursorIndex(text().length());
    }

    public char mask() {
        return mask;
    }

    public PasswordField mask(char mask) {
        this.mask = mask == 0 ? '\u2022' : mask;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    @Override
    protected String displayText() {
        if (text().isEmpty()) {
            return super.displayText();
        }
        return String.valueOf(mask).repeat(text().codePointCount(0, text().length()));
    }
}
