package dev.sixik.unigui.widgets.interaction;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.widget.skin.WidgetsRender;
import dev.sixik.unigui.api.xml.XmlAttribute;
import dev.sixik.unigui.api.xml.XmlWidgetName;
import dev.sixik.unigui.widgets.render.TextInputRenderer;
import dev.sixik.unigui.widgets.render.TextInputRenderType;
import dev.sixik.unigui.api.style.StyleAnimationIds;
import dev.sixik.unigui.api.style.StyleIds;

@XmlWidgetName("PasswordField")
public class PasswordField extends TextInput {
    public static final String STYLE_TYPE = StyleIds.Widget.PASSWORD_FIELD;

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

    @XmlAttribute(value = "mask", category = "Appearance", defaultValue = "\u2022", description = "Single character used to hide the password text.")
    public PasswordField mask(String mask) {
        if (mask == null || mask.isEmpty()) return mask('\u2022');
        return mask(mask.charAt(0));
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
    protected TextInputRenderer defaultRenderer() {
        return WidgetsRender.passwordField();
    }

    @Override
    protected TextInputRenderer effectiveRenderer() {
        return renderer() == null ? styleRenderer(TextInputRenderer.class, defaultRenderer()) : renderer();
    }

    @Override
    protected TextInputRenderType renderType() {
        return TextInputRenderType.PASSWORD_FIELD;
    }
}
