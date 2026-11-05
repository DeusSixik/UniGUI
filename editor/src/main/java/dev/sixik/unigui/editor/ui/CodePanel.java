package dev.sixik.unigui.editor.ui;

import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.editor.EditorContext;
import dev.sixik.unigui.editor.PanelSlot;
import dev.sixik.unigui.widgets.interaction.XmlCodeEditor;
import dev.sixik.unigui.xaml.edit.EditorSession;
import dev.sixik.unigui.xaml.edit.SessionChange;

/**
 * Текст документа. Набранный текст разбирается сессией: пока он корректен, документ и все остальные панели
 * следуют за ним; если нет, ошибка попадает в диагностику, а документ остаётся прежним.
 */
final class CodePanel extends SessionPanel {
    private final XmlCodeEditor editor = new XmlCodeEditor();
    private boolean syncing;

    @Override
    public String id() {
        return UiEditor.CODE;
    }

    @Override
    public String title() {
        return "XAML";
    }

    @Override
    public dev.sixik.unigui.api.render.UiIcon icon() {
        return dev.sixik.unigui.editor.style.EditorIcons.CODE;
    }

    @Override
    public PanelSlot slot() {
        return PanelSlot.CENTER;
    }

    @Override
    protected Widget build(EditorContext context) {
        editor.layout(style -> style.sizePercent(100.0f, 100.0f));
        // Поле рисует свой фон поверх номеров строк и подсветки ошибок; подложкой служит фон панели.
        editor.themeEnabled(false);
        editor.borderVisible(false);
        editor.background().set(dev.sixik.unigui.api.math.MutableColor.rgba255(0, 0, 0, 0));
        editor.borderColor().set(dev.sixik.unigui.api.math.MutableColor.rgba255(0, 0, 0, 0));
        editor.textColor().set(dev.sixik.unigui.editor.style.EditorStyles.TEXT_PRIMARY);
        editor.gutterColor().set(dev.sixik.unigui.editor.style.EditorStyles.BG_PANEL);
        editor.lineNumberColor().set(dev.sixik.unigui.editor.style.EditorStyles.TEXT_LABEL);
        editor.onTextChanged(event -> {
            if (syncing) return;
            session().ifPresent(session -> session.replaceText(editor.text()));
        });
        return editor;
    }

    @Override
    protected void bound(EditorSession next) {
        if (next != null) editor.schema(next.schema());
        pull(next);
    }

    @Override
    protected void changed(EditorSession current, SessionChange change) {
        switch (change.kind()) {
            case DOCUMENT_CHANGED, REVERTED -> pull(current);
            default -> { }
        }
    }

    /** Копирует текст сессии в редактор, если он отличается (правка пришла из инспектора, палитры или отмены). */
    private void pull(EditorSession session) {
        String text = session == null ? "" : session.text();
        if (editor.text().equals(text)) return;
        syncing = true;
        try {
            editor.text(text);
        } finally {
            syncing = false;
        }
    }
}
