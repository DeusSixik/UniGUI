package dev.sixik.unigui.widgets.docking;



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

    public boolean active() {
        return active;
    }

    public boolean dragging() {
        return dragging;
    }

    public String paneId() {
        return paneId;
    }

    public int pointerId() {
        return pointerId;
    }

    public float dragThreshold() {
        return dragThreshold;
    }

    public DockDragController dragThreshold(float dragThreshold) {
        this.dragThreshold = Float.isFinite(dragThreshold) ? Math.max(0.0f, dragThreshold) : DEFAULT_DRAG_THRESHOLD;
        return this;
    }

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
