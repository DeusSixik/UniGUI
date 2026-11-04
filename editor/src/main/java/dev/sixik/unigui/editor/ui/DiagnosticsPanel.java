package dev.sixik.unigui.editor.ui;

import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.editor.EditorContext;
import dev.sixik.unigui.editor.PanelSlot;
import dev.sixik.unigui.widgets.containers.ScrollView;
import dev.sixik.unigui.widgets.containers.VBox;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.xaml.UiDiagnostic;
import dev.sixik.unigui.xaml.edit.EditorSession;
import dev.sixik.unigui.xaml.edit.SessionChange;

/**
 * Проблемы документа: ошибки разбора, проверки по схеме и отклонённых правок, каждая с позицией.
 */
final class DiagnosticsPanel extends SessionPanel {
    private static final MutableColor PROBLEM = MutableColor.rgba255(235, 100, 100, 255);

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
    public PanelSlot slot() {
        return PanelSlot.BOTTOM;
    }

    @Override
    public float share() {
        return 0.25f;
    }

    @Override
    protected Widget build(EditorContext context) {
        lines.spacing(1.0f);
        ScrollView scroll = new ScrollView(lines);
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
            lines.addChild(new Label(session == null ? "No document" : "No problems"));
        } else {
            for (UiDiagnostic diagnostic : session.allDiagnostics()) {
                Label line = new Label(diagnostic.toString());
                line.color(PROBLEM);
                lines.addChild(line);
            }
        }
        lines.applyQueuedMutations();
    }
}
