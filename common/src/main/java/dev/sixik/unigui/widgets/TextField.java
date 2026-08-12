package dev.sixik.unigui.widgets;

import dev.sixik.unigui.api.input.TextEditorModel;
import dev.sixik.unigui.api.widget.skin.WidgetsRender;
import dev.sixik.unigui.widgets.render.TextInputRenderer;
import dev.sixik.unigui.widgets.render.TextInputRenderType;

/**
 * Styled single-line text field specialization.
 *
 * <p>TextField keeps the TextInput editor behavior but enables the default field
 * chrome and uses the TEXT_FIELD renderer type. Prefer TextField for normal
 * user-facing form controls; use TextInput when a raw/editor-base control is needed.</p>
 */
public class TextField extends TextInput {
    public TextField() {
        enableDefaultTextInputChrome();
    }

    public TextField(String text) {
        this();
        editorModel().silentText(TextEditorModel.sanitizePrintable(text));
        editorModel().cursorIndex(text().length());
    }

    @Override
    public TextField text(String text) {
        super.text(text);
        return this;
    }

    @Override
    public TextField placeholder(String placeholder) {
        super.placeholder(placeholder);
        return this;
    }

    @Override
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
    public TextField maxLength(int maxLength) {
        super.maxLength(maxLength);
        return this;
    }

    @Override
    protected TextInputRenderer effectiveRenderer() {
        return renderer() == null ? styleRenderer(TextInputRenderer.class, WidgetsRender.textField()) : renderer();
    }

    @Override
    protected TextInputRenderType renderType() {
        return TextInputRenderType.TEXT_FIELD;
    }
}
