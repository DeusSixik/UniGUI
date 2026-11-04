package dev.sixik.unigui.editor.ui;

import dev.sixik.unigui.api.event.PointerPressedEvent;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.editor.EditorContext;
import dev.sixik.unigui.editor.PanelSlot;
import dev.sixik.unigui.widgets.containers.StackPanel;
import dev.sixik.unigui.widgets.display.CanvasWidget;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.xaml.edit.EditorSession;
import dev.sixik.unigui.xaml.edit.SessionChange;
import dev.sixik.unigui.xaml.load.LoadOptions;
import dev.sixik.unigui.xaml.preview.Preview;

/**
 * Живой предпросмотр документа. Загрузку, «последнее удачное дерево» и соответствие узел↔виджет ведёт
 * {@link Preview} из ядра; панель только показывает его, рисует рамку вокруг выбранного узла и превращает щелчок
 * в выбор узла.
 */
final class ViewportPanel extends SessionPanel {
    private static final MutableColor OUTLINE = MutableColor.rgba255(90, 160, 255, 255);
    private static final MutableColor ERROR = MutableColor.rgba255(235, 100, 100, 255);

    private final LoadOptions previewOptions;
    private final StackPanel holder = new StackPanel();
    private final Label status = new Label("");
    private Preview preview;
    private Widget shown;

    ViewportPanel(LoadOptions previewOptions) {
        this.previewOptions = previewOptions;
    }

    @Override
    public String id() {
        return UiEditor.VIEWPORT;
    }

    @Override
    public String title() {
        return "Viewport";
    }

    @Override
    public PanelSlot slot() {
        return PanelSlot.CENTER;
    }

    @Override
    protected Widget build(EditorContext context) {
        StackPanel root = new StackPanel();
        root.layout(style -> style.sizePercent(100.0f, 100.0f));
        holder.layout(style -> style.sizePercent(100.0f, 100.0f));

        CanvasWidget outline = new CanvasWidget();
        outline.layout(style -> style.sizePercent(100.0f, 100.0f));
        // Слой выбора лежит над предпросмотром и принимает все щелчки: предпросмотр остаётся «чертежом», а выбор
        // узла идёт по месту щелчка.
        outline.focusable(true); // щелчок по предпросмотру даёт окну редактора фокус, и горячие клавиши работают
        outline.on(PointerPressedEvent.TYPE, event -> pick(event.rootX(), event.rootY()));
        outline.onDraw(draw -> {
            Widget target = selectedWidget();
            if (target == null) return;
            RectView bounds = target.layoutBounds();
            draw.addRect(bounds.x(), bounds.y(), bounds.width(), bounds.height(), OUTLINE, 2.0f);
        });

        status.color(ERROR);
        status.layout(style -> style.flexGrow(0.0f));
        // Порядок важен для щелчков: самым верхним должен быть слой выбора, иначе пустая подпись статуса, лежащая
        // на всю область, перехватывала бы их.
        root.addChild(holder);
        root.addChild(status);
        root.addChild(outline);
        root.applyQueuedMutations();
        return root;
    }

    @Override
    protected void bound(EditorSession next) {
        if (preview != null) preview.close();
        preview = next == null ? null : new Preview(next.schema(), previewOptions);
        shown = null;
        holder.clearChildren();
        holder.applyQueuedMutations();
        reload();
    }

    @Override
    protected void changed(EditorSession current, SessionChange change) {
        switch (change.kind()) {
            case DOCUMENT_CHANGED, REVERTED -> reload();
            default -> { }
        }
    }

    /** Выбирает узел под точкой щелчка. */
    void pick(float x, float y) {
        EditorSession session = session().orElse(null);
        if (session == null || preview == null) return;
        preview.pick(session.document(), x, y).ifPresent(session::select);
    }

    /** @return виджет предпросмотра, созданный из выбранного узла, или {@code null} */
    Widget selectedWidget() {
        if (preview == null) return null;
        return session().flatMap(EditorSession::selected).flatMap(preview::widgetOf).orElse(null);
    }

    /** @return текущий корень предпросмотра или {@code null} */
    Widget previewRoot() {
        return preview == null ? null : preview.root().orElse(null);
    }

    private void reload() {
        EditorSession session = session().orElse(null);
        if (session == null || preview == null) {
            status.text("");
            return;
        }
        Preview.Result result = preview.show(session.document());
        status.text(result.shown() ? "" : "Preview: " + result.message());
        Widget root = preview.root().orElse(null);
        if (root != shown) {
            holder.clearChildren();
            if (root != null) holder.addChild(root);
            holder.applyQueuedMutations();
            shown = root;
        }
    }
}
