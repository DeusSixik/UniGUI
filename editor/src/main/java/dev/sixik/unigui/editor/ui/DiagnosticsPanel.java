package dev.sixik.unigui.editor.ui;

import dev.sixik.unigui.api.layout.Align;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.render.UiIcon;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.editor.EditorContext;
import dev.sixik.unigui.editor.PanelSlot;
import dev.sixik.unigui.editor.style.EditorIcons;
import dev.sixik.unigui.editor.style.EditorStyles;
import dev.sixik.unigui.widgets.containers.HBox;
import dev.sixik.unigui.widgets.containers.ScrollView;
import dev.sixik.unigui.widgets.containers.VBox;
import dev.sixik.unigui.widgets.display.CanvasWidget;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.xaml.UiDiagnostic;
import dev.sixik.unigui.xaml.edit.EditorSession;
import dev.sixik.unigui.xaml.edit.SessionChange;

/**
 * Проблемы документа: ошибки разбора, проверки по схеме и отклонённых правок, каждая с позицией.
 */
final class DiagnosticsPanel extends SessionPanel {
    private final VBox lines = new VBox();

    @Override
    public String id() {
        return UiEditor.DIAGNOSTICS;
    }

    @Override
    public String title() {
        return "Problems";
    }

    @Override
    public dev.sixik.unigui.api.render.UiIcon icon() {
        return dev.sixik.unigui.editor.style.EditorIcons.PROBLEMS;
    }

    @Override
    public PanelSlot slot() {
        return PanelSlot.BOTTOM;
    }

    @Override
    public float share() {
        return 0.25f;
    }

    @Override
    protected Widget build(EditorContext context) {
        lines.spacing(2.0f);
        lines.layout(style -> style.widthPercent(100.0f).padding(8.0f, 6.0f));
        ScrollView scroll = new ScrollView(lines);
        EditorStyles.styleScroll(scroll);
        scroll.layout(style -> style.sizePercent(100.0f, 100.0f));
        return scroll;
    }

    @Override
    protected void bound(EditorSession next) {
        refresh();
    }

    @Override
    protected void changed(EditorSession current, SessionChange change) {
        switch (change.kind()) {
            case DIAGNOSTICS_CHANGED, DOCUMENT_CHANGED, REVERTED -> refresh();
            default -> { }
        }
    }

    private void refresh() {
        lines.clearChildren();
        EditorSession session = session().orElse(null);
        if (session == null || session.allDiagnostics().isEmpty()) {
            lines.addChild(row(EditorIcons.CHECK, EditorStyles.PLAY_GREEN, session == null ? "No document" : "No problems",
                    EditorStyles.TEXT_MUTED));
        } else {
            for (UiDiagnostic diagnostic : session.allDiagnostics()) {
                lines.addChild(row(EditorIcons.ERROR, EditorStyles.ERROR_RED, diagnostic.toString(),
                        EditorStyles.TEXT_PRIMARY));
            }
        }
        lines.applyQueuedMutations();
    }

    private static Widget row(UiIcon icon, MutableColor tint, String text, MutableColor color) {
        HBox row = new HBox();
        row.spacing(6.0f);
        row.layout(style -> style.widthPercent(100.0f).flexShrink(0.0f).alignItems(Align.CENTER));
        CanvasWidget mark = new CanvasWidget();
        mark.layout(style -> style.size(11.0f, 11.0f).flexShrink(0.0f));
        mark.onDraw(draw -> icon.draw(EditorStyles.scope(draw, mark), mark.layoutBounds().x(), mark.layoutBounds().y(), 11.0f, tint));
        Label label = new Label(text);
        label.color(color);
        label.layout(style -> style.flexGrow(1.0f).flexShrink(1.0f).minWidth(0.0f));
        row.addChild(mark);
        row.addChild(label);
        row.applyQueuedMutations();
        return row;
    }
}
