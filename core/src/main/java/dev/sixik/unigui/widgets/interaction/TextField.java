package dev.sixik.unigui.widgets.interaction;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiLink;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;
import dev.sixik.unigui.api.input.TextEditorModel;
import dev.sixik.unigui.api.style.Style;
import dev.sixik.unigui.widgets.WidgetStyles;
import dev.sixik.unigui.api.xml.XmlAttribute;
import dev.sixik.unigui.api.xml.XmlWidgetName;
import dev.sixik.unigui.api.style.StyleAnimationIds;
import dev.sixik.unigui.api.style.StyleIds;

/**
 * Styled single-line text field specialization.
 *
 * <p>TextField keeps the TextInput editor behavior but enables the default field
 * chrome and uses the TEXT_FIELD renderer type. Prefer TextField for normal
 * user-facing form controls; use TextInput when a raw/editor-base control is needed.</p>
 */
@AiDoc(
        summary = "Standard user-facing single-line text input field.",
        purpose = "Default component for textual user input in forms and dialogs.",
        useWhen = {"Collecting usernames, emails, names, and general text strings."},
        related = {"TextInput", "TextArea", "NumberField"}
)
@WikiPage(
        title = "<key:text.field.title>",
        slug = "text-field",
        category = "<key:core.widgets.interaction.category>",
        summary = "<key:text.field.summary>",
        order = 120
)
@WikiSection(
        title = "<key:text.field.section.overview.title>",
        markdown = "<key:text.field.section.overview.body>",
        order = 10
)
@WikiExample(
        title = "<key:text.field.example.basic.title>",
        code = "<key:text.field.example.basic.code>",
        order = 10
)
@WikiLink(label = "TextInput", target = "page:text-input")
@WikiLink(label = "TextArea", target = "page:text-area")
@XmlWidgetName("TextField")
public class TextField extends TextInput {
    public static final String STYLE_TYPE = StyleIds.Widget.TEXT_FIELD;

    /** Defaults used when no global or local style provides text-field chrome. */
    public TextField() {
        applyComponentStyleDefaults(WidgetStyles.TextInput.DEFAULT_CHROME);
    }

    @Override
    protected Style defaultStyle() {
        return WidgetStyles.TextInput.DEFAULT_CHROME;
    }

    public TextField(String text) {
        this();
        editorModel().silentText(TextEditorModel.sanitizePrintable(text));
        editorModel().cursorIndex(text().length());
    }

    @Override
    @XmlAttribute(value = "text", category = "Content", defaultValue = "", description = "Editable text value.")
    public TextField text(String text) {
        super.text(text);
        return this;
    }

    @Override
    @XmlAttribute(value = "placeholder", category = "Content", defaultValue = "", description = "Placeholder text shown while the value is empty.")
    public TextField placeholder(String placeholder) {
        super.placeholder(placeholder);
        return this;
    }

    @Override
    @XmlAttribute(value = "cursorIndex", category = "Behavior", defaultValue = "0", description = "Initial cursor position in the editable text.")
    public TextField cursorIndex(int cursorIndex) {
        super.cursorIndex(cursorIndex);
        return this;
    }

    @Override
    public TextField select(int start, int end) {
        super.select(start, end);
        return this;
    }

    @Override
    public TextField selectAll() {
        super.selectAll();
        return this;
    }

    @Override
    public TextField clearSelection() {
        super.clearSelection();
        return this;
    }

    @Override
    @XmlAttribute(value = "textPaddingX", category = "Layout", defaultValue = "12", description = "Symmetric inset between both edges and editable text.")
    public TextField textPaddingX(float textPaddingX) {
        super.textPaddingX(textPaddingX);
        return this;
    }

    @Override
    @XmlAttribute(value = "textPaddingLeft", category = "Layout", defaultValue = "12", description = "Inset between the left edge and editable text.")
    public TextField textPaddingLeft(float textPaddingLeft) {
        super.textPaddingLeft(textPaddingLeft);
        return this;
    }

    @Override
    @XmlAttribute(value = "textPaddingRight", category = "Layout", defaultValue = "12", description = "Inset between editable text and the right edge.")
    public TextField textPaddingRight(float textPaddingRight) {
        super.textPaddingRight(textPaddingRight);
        return this;
    }

    @Override
    @XmlAttribute(value = "maxLength", category = "Behavior", defaultValue = "0", description = "Maximum editable character count; 0 means unlimited.")
    public TextField maxLength(int maxLength) {
        super.maxLength(maxLength);
        return this;
    }
}
