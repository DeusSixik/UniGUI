package dev.sixik.unigui.widgets.interaction;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.xml.XmlAttribute;
import dev.sixik.unigui.api.xml.XmlWidgetName;
import dev.sixik.unigui.api.style.StyleAnimationIds;
import dev.sixik.unigui.api.style.StyleIds;
import dev.sixik.unigui.api.style.Style;
import dev.sixik.unigui.widgets.WidgetStyles;

@XmlWidgetName("PasswordField")
public class PasswordField extends TextInput {
    public static final String STYLE_TYPE = StyleIds.Widget.PASSWORD_FIELD;

    private char mask = '\u2022';

    public PasswordField() {
        applyComponentStyleDefaults(WidgetStyles.TextInput.DEFAULT_CHROME);
    }

    /**
     * Возвращает defaults chrome для семейства стандартных текстовых полей.
     *
     * @return стандартный style preset оформления поля
     */
    @Override
    protected Style defaultStyle() {
        return WidgetStyles.TextInput.DEFAULT_CHROME;
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
    @XmlAttribute(value = "textPaddingX", category = "Layout", defaultValue = "12", description = "Symmetric inset between both edges and editable text.")
    public PasswordField textPaddingX(float textPaddingX) {
        super.textPaddingX(textPaddingX);
        return this;
    }

    @Override
    @XmlAttribute(value = "textPaddingLeft", category = "Layout", defaultValue = "12", description = "Inset between the left edge and editable text.")
    public PasswordField textPaddingLeft(float textPaddingLeft) {
        super.textPaddingLeft(textPaddingLeft);
        return this;
    }

    @Override
    @XmlAttribute(value = "textPaddingRight", category = "Layout", defaultValue = "12", description = "Inset between editable text and the right edge.")
    public PasswordField textPaddingRight(float textPaddingRight) {
        super.textPaddingRight(textPaddingRight);
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
}
