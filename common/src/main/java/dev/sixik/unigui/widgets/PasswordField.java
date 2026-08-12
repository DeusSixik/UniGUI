package dev.sixik.unigui.widgets;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.widget.skin.WidgetsRender;
import dev.sixik.unigui.widgets.render.TextInputRenderer;
import dev.sixik.unigui.widgets.render.TextInputRenderType;

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

    @Override
    protected void copySelection() {
        // Never expose the real password through the system clipboard.
    }

    @Override
    protected void cutSelection() {
        if (editorModel().deleteSelectionIfNeeded()) {
            invalidate(InvalidationFlags.VISUAL);
        }
    }

    @Override
    protected TextInputRenderer effectiveRenderer() {
        return renderer() == null ? styleRenderer(TextInputRenderer.class, WidgetsRender.passwordField()) : renderer();
    }

    @Override
    protected TextInputRenderType renderType() {
        return TextInputRenderType.PASSWORD_FIELD;
    }
}
