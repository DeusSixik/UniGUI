package dev.sixik.unigui.widgets.editor;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.core.UIContext;
import dev.sixik.unigui.api.event.Event;
import dev.sixik.unigui.api.event.EventPhase;
import dev.sixik.unigui.api.event.EventSubscription;
import dev.sixik.unigui.api.event.PointerEvent;
import dev.sixik.unigui.api.event.PointerMovedEvent;
import dev.sixik.unigui.api.event.PointerPressedEvent;
import dev.sixik.unigui.api.event.PointerReleasedEvent;
import dev.sixik.unigui.api.input.PointerButton;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.xml.XmlAttribute;
import dev.sixik.unigui.api.xml.XmlWidgetName;
import dev.sixik.unigui.widgets.containers.Box;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/** Editor drag source helper that exposes XML-configurable payload id/type and preview metadata. */
@XmlWidgetName("DragSource")
public class DragSource extends Box {
    private final List<Consumer<DragEvent>> listeners = new ArrayList<>();
    private String payloadId = "";
    private String payloadType = "generic";
    private String dragPreview = "";
    private boolean dragEnabled = true;
    private float dragThreshold = 4.0f;
    private boolean pointerActive;
    private boolean dragging;
    private int dragPointerId = -1;
    private float pressRootX;
    private float pressRootY;
    private float currentRootX;
    private float currentRootY;
    private DropTarget.DropResult lastDropResult = DropTarget.DropResult.IGNORED;

    public String payloadId() {
        return payloadId;
    }

    @XmlAttribute(value = "payloadId", category = "State", defaultValue = "", description = "Stable payload id emitted by this drag source.")
    public DragSource payloadId(String payloadId) {
        this.payloadId = normalize(payloadId, "");
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public String payloadType() {
        return payloadType;
    }

    @XmlAttribute(value = "payloadType", category = "Behavior", defaultValue = "generic", description = "Payload type used by drop target validation.")
    public DragSource payloadType(String payloadType) {
        this.payloadType = normalize(payloadType, "generic");
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public String dragPreview() {
        return dragPreview;
    }

    @XmlAttribute(value = "dragPreview", category = "Content", defaultValue = "", description = "Textual preview label used by host drag overlays.")
    public DragSource dragPreview(String dragPreview) {
        this.dragPreview = normalize(dragPreview, "");
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public boolean dragEnabled() {
        return dragEnabled;
    }

    @XmlAttribute(value = "dragEnabled", category = "Behavior", defaultValue = "true", description = "Whether this source can start drag operations.")
    public DragSource dragEnabled(boolean dragEnabled) {
        if (this.dragEnabled == dragEnabled) return this;
        this.dragEnabled = dragEnabled;
        if (!dragEnabled) cancelDrag();
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public float dragThreshold() {
        return dragThreshold;
    }

    @XmlAttribute(value = "dragThreshold", category = "Behavior", defaultValue = "4", description = "Pointer movement in UI pixels required before drag start.")
    public DragSource dragThreshold(float dragThreshold) {
        this.dragThreshold = Float.isFinite(dragThreshold) ? Math.max(0.0f, dragThreshold) : 4.0f;
        return this;
    }

    public boolean dragging() {
        return dragging;
    }

    public boolean pointerActive() {
        return pointerActive;
    }

    public DropTarget.DropResult lastDropResult() {
        return lastDropResult;
    }

    public DragPayload payload() {
        return new DragPayload(payloadId, payloadType, dragPreview);
    }

    public boolean startDrag(float rootX, float rootY) {
        if (!canDrag()) return false;
        pointerActive = true;
        dragging = true;
        currentRootX = rootX;
        currentRootY = rootY;
        lastDropResult = DropTarget.DropResult.IGNORED;
        emit(Action.STARTED, rootX, rootY, lastDropResult);
        invalidate(InvalidationFlags.VISUAL);
        return true;
    }

    public boolean moveDrag(float rootX, float rootY) {
        if (!dragging) return false;
        currentRootX = rootX;
        currentRootY = rootY;
        emit(Action.MOVED, rootX, rootY, lastDropResult);
        invalidate(InvalidationFlags.VISUAL);
        return true;
    }

    public DropTarget.DropResult dropOn(DropTarget target, float rootX, float rootY) {
        if (!dragging && !startDrag(rootX, rootY)) return DropTarget.DropResult.IGNORED;
        DropTarget.DropResult result = target == null
                ? DropTarget.DropResult.IGNORED
                : target.requestDrop(payload(), rootX, rootY);
        finishDrag(rootX, rootY, result);
        return result;
    }

    public boolean finishDrag(float rootX, float rootY, DropTarget.DropResult result) {
        if (!pointerActive && !dragging) return false;
        DropTarget.DropResult normalized = result == null ? DropTarget.DropResult.IGNORED : result;
        currentRootX = rootX;
        currentRootY = rootY;
        lastDropResult = normalized;
        emit(Action.ENDED, rootX, rootY, normalized);
        releasePointerCapture();
        resetPointerState(true);
        invalidate(InvalidationFlags.VISUAL);
        return true;
    }

    public boolean cancelDrag() {
        if (!pointerActive && !dragging) return false;
        emit(Action.CANCELLED, currentRootX, currentRootY, DropTarget.DropResult.IGNORED);
        releasePointerCapture();
        resetPointerState(false);
        invalidate(InvalidationFlags.VISUAL);
        return true;
    }

    public EventSubscription onDrag(Consumer<DragEvent> listener) {
        Objects.requireNonNull(listener, "listener");
        listeners.add(listener);
        return () -> listeners.remove(listener);
    }

    public EventSubscription onDragStarted(Consumer<DragEvent> listener) {
        return onFiltered(Action.STARTED, listener);
    }

    public EventSubscription onDragMoved(Consumer<DragEvent> listener) {
        return onFiltered(Action.MOVED, listener);
    }

    public EventSubscription onDragEnded(Consumer<DragEvent> listener) {
        return onFiltered(Action.ENDED, listener);
    }

    @Override
    public void handle(Event event) {
        if (visibility() != Visibility.VISIBLE || !enabled()) return;
        super.handle(event);
        if (event.isCancelled()) return;
        if (event instanceof PointerEvent pointer && pointer.phase() == EventPhase.CAPTURE) return;

        if (event instanceof PointerPressedEvent pointer && pointer.button() == PointerButton.PRIMARY && canDrag()) {
            beginPointerDrag(pointer);
            event.cancel();
        } else if (event instanceof PointerMovedEvent pointer && pointerActive && pointer.pointerId() == dragPointerId) {
            updatePointerDrag(pointer);
            event.cancel();
        } else if (event instanceof PointerReleasedEvent pointer
                && pointer.button() == PointerButton.PRIMARY
                && pointerActive
                && pointer.pointerId() == dragPointerId) {
            if (dragging) {
                finishDrag(pointer.rootX(), pointer.rootY(), DropTarget.DropResult.IGNORED);
            } else {
                cancelDrag();
            }
            event.cancel();
        }
    }

    private void beginPointerDrag(PointerPressedEvent pointer) {
        pointerActive = true;
        dragging = false;
        dragPointerId = pointer.pointerId();
        pressRootX = pointer.rootX();
        pressRootY = pointer.rootY();
        currentRootX = pressRootX;
        currentRootY = pressRootY;
        lastDropResult = DropTarget.DropResult.IGNORED;
        UIContext context = uiContext();
        if (context != null) {
            context.capturePointer(dragPointerId, this);
        }
        invalidate(InvalidationFlags.VISUAL);
    }

    private void updatePointerDrag(PointerMovedEvent pointer) {
        currentRootX = pointer.rootX();
        currentRootY = pointer.rootY();
        if (!dragging && distanceFromPress(pointer.rootX(), pointer.rootY()) >= dragThreshold) {
            startDrag(pointer.rootX(), pointer.rootY());
        } else if (dragging) {
            moveDrag(pointer.rootX(), pointer.rootY());
        }
    }

    private float distanceFromPress(float rootX, float rootY) {
        float dx = rootX - pressRootX;
        float dy = rootY - pressRootY;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    private boolean canDrag() {
        return enabled() && dragEnabled;
    }

    private EventSubscription onFiltered(Action action, Consumer<DragEvent> listener) {
        Objects.requireNonNull(listener, "listener");
        Consumer<DragEvent> wrapper = event -> {
            if (event.action() == action) listener.accept(event);
        };
        listeners.add(wrapper);
        return () -> listeners.remove(wrapper);
    }

    private void emit(Action action, float rootX, float rootY, DropTarget.DropResult result) {
        DragEvent event = new DragEvent(this, payload(), action, rootX, rootY, result);
        for (Consumer<DragEvent> listener : List.copyOf(listeners)) {
            listener.accept(event);
        }
    }

    private void releasePointerCapture() {
        UIContext context = uiContext();
        if (context != null && dragPointerId >= 0) {
            context.releasePointer(dragPointerId, this);
        }
    }

    private void resetPointerState(boolean keepLastResult) {
        pointerActive = false;
        dragging = false;
        dragPointerId = -1;
        pressRootX = 0.0f;
        pressRootY = 0.0f;
        currentRootX = 0.0f;
        currentRootY = 0.0f;
        if (!keepLastResult) {
            lastDropResult = DropTarget.DropResult.IGNORED;
        }
    }

    private static String normalize(String value, String fallback) {
        String normalized = value == null ? "" : value.trim();
        return normalized.isEmpty() ? fallback : normalized;
    }

    public enum Action {
        STARTED,
        MOVED,
        ENDED,
        CANCELLED
    }

    public record DragEvent(DragSource source,
                            DragPayload payload,
                            Action action,
                            float rootX,
                            float rootY,
                            DropTarget.DropResult dropResult) {
        public DragEvent {
            Objects.requireNonNull(source, "source");
            payload = payload == null ? source.payload() : payload;
            action = action == null ? Action.MOVED : action;
            dropResult = dropResult == null ? DropTarget.DropResult.IGNORED : dropResult;
        }
    }
}
