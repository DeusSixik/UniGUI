package dev.sixik.unigui.widgets.docking;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiLink;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;

@AiDoc(
        description = "Handles drag-and-drop interactions for docking tabs within DockingRoot, computing drop previews, live tear-offs, and docking intents."
)
@WikiPage(
        title = "DockDragController",
        slug = "dock-drag-controller",
        category = "<key:docking.category>",
        summary = "<key:dock.drag.controller.summary>"
)
@WikiSection(
        title = "<key:wiki.section.overview.title>",
        markdown = "<key:dock.drag.controller.section.overview.body>",
        order = 10
)
@WikiLink(
        label = "DockingRoot",
        target = "page:docking-root",
        description = "<key:docking.root.summary>"
)
@WikiLink(
        label = "DockDropIntent",
        target = "page:dock-drop-intent",
        description = "<key:dock.drop.intent.summary>"
)
public final class DockDragController {
    private static final float DEFAULT_DRAG_THRESHOLD = 4.0f;

    private final DockingRoot owner;
    private float dragThreshold = DEFAULT_DRAG_THRESHOLD;
    private String paneId = "";
    private int pointerId = -1;
    private float startRootX;
    private float startRootY;
    private float lastRootX;
    private float lastRootY;
    private boolean active;
    private boolean dragging;
    private DockDropIntent previewIntent = DockDropIntent.none();

    DockDragController(DockingRoot owner) {
        this.owner = owner;
    }

    @AiDoc(description = "Returns true if a drag gesture is currently active or pressed.")
    @WikiMethod(description = "<key:dock.drag.controller.method.active>")
    @WikiReturn("<key:dock.drag.controller.return.active>")
    public boolean active() {
        return active;
    }

    @AiDoc(description = "Returns true if the drag gesture has exceeded the threshold and is actively dragging.")
    @WikiMethod(description = "<key:dock.drag.controller.method.dragging>")
    @WikiReturn("<key:dock.drag.controller.return.dragging>")
    public boolean dragging() {
        return dragging;
    }

    @AiDoc(description = "Returns the identifier of the dock pane currently being dragged.")
    @WikiMethod(description = "<key:dock.drag.controller.method.paneId>")
    @WikiReturn("<key:dock.drag.controller.return.paneId>")
    public String paneId() {
        return paneId;
    }

    @AiDoc(description = "Returns the active pointer identifier.")
    @WikiMethod(description = "<key:dock.drag.controller.method.pointerId>")
    @WikiReturn("<key:dock.drag.controller.return.pointerId>")
    public int pointerId() {
        return pointerId;
    }

    @AiDoc(description = "Returns the drag activation threshold in pixels.")
    @WikiMethod(description = "<key:dock.drag.controller.method.dragThreshold>")
    @WikiReturn("<key:dock.drag.controller.return.dragThreshold>")
    public float dragThreshold() {
        return dragThreshold;
    }

    @AiDoc(description = "Sets the minimum movement threshold in pixels before drag initiates.")
    @WikiMethod(description = "<key:dock.drag.controller.method.setDragThreshold>")
    @WikiReturn("<key:dock.drag.controller.return.this>")
    public DockDragController dragThreshold(
            @WikiParameter(description = "<key:dock.drag.controller.param.threshold>")
            float dragThreshold
    ) {
        this.dragThreshold = Float.isFinite(dragThreshold) ? Math.max(0.0f, dragThreshold) : DEFAULT_DRAG_THRESHOLD;
        return this;
    }

    @AiDoc(description = "Returns the current drop preview intent.")
    @WikiMethod(description = "<key:dock.drag.controller.method.previewIntent>")
    @WikiReturn("<key:dock.drag.controller.return.previewIntent>")
    public DockDropIntent previewIntent() {
        return previewIntent;
    }

    /** Последняя позиция указателя в root-координатах; нужна рендеру зон. */
    float lastRootX() {
        return lastRootX;
    }

    /** Последняя позиция указателя в root-координатах; нужна рендеру зон. */
    float lastRootY() {
        return lastRootY;
    }

    boolean begin(DockPane pane, int pointerId, float rootX, float rootY) {
        if (pane == null) return false;
        cancel();
        this.paneId = pane.id();
        this.pointerId = pointerId;
        this.startRootX = rootX;
        this.startRootY = rootY;
        this.active = true;
        this.dragging = false;
        this.previewIntent = DockDropIntent.none();
        return true;
    }

    boolean move(int pointerId, float rootX, float rootY) {
        if (!active || this.pointerId != pointerId) return false;
        lastRootX = rootX;
        lastRootY = rootY;
        if (!dragging) {
            float dx = rootX - startRootX;
            float dy = rootY - startRootY;
            if (dx * dx + dy * dy < dragThreshold * dragThreshold) {
                return true;
            }
            dragging = true;
            owner.dispatchDockDragStarted(paneId, startRootX, startRootY);
        }

        DockDropIntent next = owner.resolveDockDropIntent(paneId, rootX, rootY);
        if (!next.equals(previewIntent)) {
            DockDropIntent previous = previewIntent;
            previewIntent = next;
            owner.dispatchDockDropPreviewChanged(paneId, previous, next);
        }
        owner.dispatchDockDragMoved(paneId, rootX, rootY, previewIntent);
        return true;
    }

    boolean end(int pointerId, float rootX, float rootY) {
        if (!active || this.pointerId != pointerId) return false;
        boolean wasDragging = dragging;
        DockDropIntent finalIntent = wasDragging ? previewIntent : DockDropIntent.none();
        boolean dropped = wasDragging && finalIntent.valid() && owner.applyDockDropIntent(paneId, finalIntent);
        if (wasDragging && !dropped) {
            // Мимо зон — не snap-back, а tear-off: панель вырывается плавающим окном.
            dropped = owner.tryTearOff(paneId, rootX, rootY);
        }
        if (wasDragging) {
            if (!previewIntent.equals(DockDropIntent.none())) {
                owner.dispatchDockDropPreviewChanged(paneId, previewIntent, DockDropIntent.none());
            }
            owner.dispatchDockDragEnded(paneId, rootX, rootY, finalIntent, dropped);
        }
        clear();
        return true;
    }

    /**
     * Передаёт активный жест плавающему окну при live tear-off.
     *
     * <p>В отличие от {@link #end}, ничего не дропает и не отрывает повторно:
     * панель уже извлечена вызывающей стороной. Гасит preview, шлёт
     * {@code drag-ended(dropped=true)} как завершение таб-фазы жеста и
     * очищает состояние, чтобы release указателя не попал обратно в root.</p>
     *
     * @param pointerId id указателя жеста
     * @param rootX X указателя в момент handoff
     * @param rootY Y указателя в момент handoff
     * @return {@code true}, если был активный жест этого указателя
     */
    boolean handoff(int pointerId, float rootX, float rootY) {
        if (!active || this.pointerId != pointerId) return false;
        boolean wasDragging = dragging;
        if (wasDragging) {
            if (!previewIntent.equals(DockDropIntent.none())) {
                owner.dispatchDockDropPreviewChanged(paneId, previewIntent, DockDropIntent.none());
            }
            owner.dispatchDockDragEnded(paneId, rootX, rootY, DockDropIntent.none(), true);
        }
        clear();
        return true;
    }

    void cancel() {
        if (active && dragging && !previewIntent.equals(DockDropIntent.none())) {
            owner.dispatchDockDropPreviewChanged(paneId, previewIntent, DockDropIntent.none());
        }
        clear();
    }

    private void clear() {
        paneId = "";
        pointerId = -1;
        startRootX = 0.0f;
        startRootY = 0.0f;
        active = false;
        dragging = false;
        previewIntent = DockDropIntent.none();
    }
}
