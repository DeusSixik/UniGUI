package dev.sixik.unigui.widgets;

import dev.sixik.unigui.api.core.FrameContext;
import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.core.UIContext;
import dev.sixik.unigui.api.event.Event;
import dev.sixik.unigui.api.event.EventListener;
import dev.sixik.unigui.api.event.EventPhase;
import dev.sixik.unigui.api.event.EventSubscription;
import dev.sixik.unigui.api.event.PointerEvent;
import dev.sixik.unigui.api.event.PointerMovedEvent;
import dev.sixik.unigui.api.event.PointerPressedEvent;
import dev.sixik.unigui.api.event.PointerReleasedEvent;
import dev.sixik.unigui.api.event.RoutableWidgetEvent;
import dev.sixik.unigui.api.event.ScrollEvent;
import dev.sixik.unigui.api.event.ViewportChangedEvent;
import dev.sixik.unigui.api.input.KeyModifiers;
import dev.sixik.unigui.api.input.PointerButton;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.layout.LayoutSize;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.viewport.Viewport2D;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.impl.widget.WidgetBase;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Generic zoomable 2D world viewport with screen-space projected anchors.
 */
public class WorldCanvas extends WidgetBase {
    private static final float DEFAULT_WIDTH = 320.0f;
    private static final float DEFAULT_HEIGHT = 240.0f;

    private Viewport2D viewport = new Viewport2D();
    private final AnchorLayer anchorLayer = new AnchorLayer(this);
    private final ObjectArrayList<WorldLayer> worldLayers = new ObjectArrayList<>();
    private final List<WorldLayer> worldLayersView = Collections.unmodifiableList(worldLayers);
    private final ObjectArrayList<Widget> children = new ObjectArrayList<>();
    private final List<Widget> childrenView = Collections.unmodifiableList(children);

    private Widget[] childSnapshot = new Widget[0];
    private boolean childSnapshotDirty = true;
    private WorldLayer[] worldLayerSnapshot = new WorldLayer[0];
    private boolean worldLayerSnapshotDirty = true;

    private boolean clippingEnabled = true;
    private boolean panningEnabled = true;
    private boolean zoomEnabled = true;
    private boolean wheelPanningEnabled = true;
    private boolean consumeWheelWhileHovered = true;
    private float wheelPanStep = 32.0f;
    private float zoomStep = 1.1f;
    private PointerButton panButton = PointerButton.PRIMARY;
    private PanDragState panDrag;

    public Viewport2D viewport() {
        return viewport;
    }

    public WorldCanvas viewport(Viewport2D viewport) {
        Viewport2D next = viewport == null ? new Viewport2D() : viewport;
        if (this.viewport == next) return this;
        float oldX = this.viewport.x();
        float oldY = this.viewport.y();
        float oldZoom = this.viewport.zoom();
        this.viewport = next;
        afterViewportChanged(oldX, oldY, oldZoom);
        return this;
    }

    public WorldCanvas viewport(float x, float y) {
        return viewport(x, y, viewport.zoom());
    }

    public WorldCanvas viewport(float x, float y, float zoom) {
        float oldX = viewport.x();
        float oldY = viewport.y();
        float oldZoom = viewport.zoom();
        if (!viewport.set(x, y, zoom)) return this;
        afterViewportChanged(oldX, oldY, oldZoom);
        return this;
    }

    public WorldCanvas zoomRange(float minZoom, float maxZoom) {
        float oldX = viewport.x();
        float oldY = viewport.y();
        float oldZoom = viewport.zoom();
        viewport.zoomRange(minZoom, maxZoom);
        if (oldX != viewport.x() || oldY != viewport.y() || oldZoom != viewport.zoom()) {
            afterViewportChanged(oldX, oldY, oldZoom);
        }
        return this;
    }

    public WorldCanvas worldBounds(float x, float y, float width, float height) {
        viewport.worldBounds(x, y, width, height);
        clampViewport();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public WorldCanvas clearWorldBounds() {
        viewport.clearWorldBounds();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public WorldCanvas clampToWorldBounds(boolean clampToWorldBounds) {
        viewport.clampToWorldBounds(clampToWorldBounds);
        clampViewport();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public AnchorLayer anchorLayer() {
        return anchorLayer;
    }

    public List<WorldLayer> worldLayers() {
        return worldLayersView;
    }

    public WorldCanvas addWorldLayer(WorldLayer layer) {
        if (layer == null) return this;
        worldLayers.add(layer);
        worldLayerSnapshotDirty = true;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public WorldCanvas removeWorldLayer(WorldLayer layer) {
        if (layer == null || !worldLayers.remove(layer)) return this;
        worldLayerSnapshotDirty = true;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public WorldCanvas clearWorldLayers() {
        if (worldLayers.isEmpty()) return this;
        worldLayers.clear();
        worldLayerSnapshotDirty = true;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public boolean clippingEnabled() {
        return clippingEnabled;
    }

    public WorldCanvas clippingEnabled(boolean clippingEnabled) {
        if (this.clippingEnabled == clippingEnabled) return this;
        this.clippingEnabled = clippingEnabled;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public boolean panningEnabled() {
        return panningEnabled;
    }

    public WorldCanvas panningEnabled(boolean panningEnabled) {
        this.panningEnabled = panningEnabled;
        return this;
    }

    public boolean zoomEnabled() {
        return zoomEnabled;
    }

    public WorldCanvas zoomEnabled(boolean zoomEnabled) {
        this.zoomEnabled = zoomEnabled;
        return this;
    }

    public boolean wheelPanningEnabled() {
        return wheelPanningEnabled;
    }

    public WorldCanvas wheelPanningEnabled(boolean wheelPanningEnabled) {
        this.wheelPanningEnabled = wheelPanningEnabled;
        return this;
    }

    public boolean consumeWheelWhileHovered() {
        return consumeWheelWhileHovered;
    }

    public WorldCanvas consumeWheelWhileHovered(boolean consumeWheelWhileHovered) {
        this.consumeWheelWhileHovered = consumeWheelWhileHovered;
        return this;
    }

    public float wheelPanStep() {
        return wheelPanStep;
    }

    public WorldCanvas wheelPanStep(float wheelPanStep) {
        this.wheelPanStep = Float.isFinite(wheelPanStep) ? Math.max(0.0f, wheelPanStep) : 32.0f;
        return this;
    }

    public float zoomStep() {
        return zoomStep;
    }

    public WorldCanvas zoomStep(float zoomStep) {
        this.zoomStep = Float.isFinite(zoomStep) && zoomStep > 0.0f ? zoomStep : 1.1f;
        return this;
    }

    public PointerButton panButton() {
        return panButton;
    }

    public WorldCanvas panButton(PointerButton panButton) {
        this.panButton = panButton == null ? PointerButton.PRIMARY : panButton;
        return this;
    }

    public float worldToRootX(float worldX) {
        return layoutBounds().x() + viewport.worldToScreenX(worldX);
    }

    public float worldToRootY(float worldY) {
        return layoutBounds().y() + viewport.worldToScreenY(worldY);
    }

    public float rootToWorldX(float rootX) {
        return viewport.screenToWorldX(rootX - layoutBounds().x());
    }

    public float rootToWorldY(float rootY) {
        return viewport.screenToWorldY(rootY - layoutBounds().y());
    }

    public float worldToLocalX(float worldX) {
        return viewport.worldToScreenX(worldX);
    }

    public float worldToLocalY(float worldY) {
        return viewport.worldToScreenY(worldY);
    }

    public float localToWorldX(float localX) {
        return viewport.screenToWorldX(localX);
    }

    public float localToWorldY(float localY) {
        return viewport.screenToWorldY(localY);
    }

    public EventSubscription onViewportChanged(EventListener<? super ViewportChangedEvent> listener) {
        return on(ViewportChangedEvent.TYPE, listener);
    }

    @Override
    public List<Widget> children() {
        return childrenView;
    }

    @Override
    public void setUiContextInternal(UIContext uiContext) {
        super.setUiContextInternal(uiContext);
        Object[] raw = children.elements();
        for (int i = 0, size = children.size(); i < size; i++) {
            Widget child = (Widget) raw[i];
            if (child instanceof WidgetBase base) {
                base.setUiContextInternal(uiContext);
            }
        }
    }

    @Override
    public void measure(LayoutContext context) {
        if (visibility() == Visibility.COLLAPSED) {
            setDesiredSize(LayoutSize.ZERO);
            return;
        }
        LayoutContext childContext = new LayoutContext(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY);
        AnchorWidget[] anchors = anchorLayer.snapshot();
        for (AnchorWidget anchor : anchors) {
            Widget child = anchor.widget();
            if (child.visibility() != Visibility.COLLAPSED) {
                child.measure(childContext);
            }
        }
        setDesiredSize(resolveDesiredSize(context, DEFAULT_WIDTH, DEFAULT_HEIGHT));
    }

    @Override
    public void arrange(RectView bounds) {
        super.arrange(bounds);
        if (visibility() == Visibility.COLLAPSED) return;
        clampViewport();
        arrangeAnchors();
    }

    @Override
    public void tick(FrameContext frame) {
        if (visibility() != Visibility.VISIBLE) return;
        super.tick(frame);
        Widget[] snapshot = childSnapshot();
        for (Widget child : snapshot) {
            if (child.visibility() == Visibility.VISIBLE) {
                child.tick(frame);
            }
        }
    }

    @Override
    public void render(RenderContext context) {
        if (visibility() != Visibility.VISIBLE || context == null) return;
        if (layoutBounds().width() <= 0.0f || layoutBounds().height() <= 0.0f) return;

        pushOpacity(context);
        boolean pushedClip = false;
        try {
            DrawScope draw = new DrawScope(context, transform(), layoutBounds());
            if (clippingEnabled) {
                draw.pushClip(layoutBounds().x(), layoutBounds().y(), layoutBounds().width(), layoutBounds().height());
                pushedClip = true;
            }

            WorldLayer[] layers = worldLayerSnapshot();
            for (WorldLayer layer : layers) {
                layer.render(this, draw);
            }

            AnchorWidget[] anchors = anchorLayer.snapshot();
            for (AnchorWidget anchor : anchors) {
                if (!anchor.arrangedVisible()) continue;
                renderChildWithInheritedTransform(context, anchor.widget());
            }
        } finally {
            if (pushedClip) {
                context.popClip();
            }
            popOpacity(context);
        }
    }

    @Override
    public void handle(Event event) {
        if (visibility() != Visibility.VISIBLE || !enabled()) return;
        super.handle(event);
        if (event.isCancelled()) return;
        if (event instanceof PointerEvent pointer && pointer.phase() == EventPhase.CAPTURE) return;

        if (event instanceof PointerPressedEvent pointer) {
            handlePointerPressed(pointer, event);
        } else if (event instanceof PointerMovedEvent pointer) {
            handlePointerMoved(pointer);
        } else if (event instanceof PointerReleasedEvent pointer) {
            handlePointerReleased(pointer, event);
        } else if (event instanceof ScrollEvent scroll) {
            handleScroll(scroll, event);
        }
    }

    @Override
    public void dispose() {
        anchorLayer.clear();
        clearWorldLayers();
    }

    void attachAnchor(AnchorWidget anchor) {
        Widget child = anchor.widget();
        if (children.contains(child)) {
            throw new IllegalArgumentException("Anchor widget is already attached to this WorldCanvas");
        }
        children.add(child);
        childSnapshotDirty = true;
        if (child instanceof WidgetBase base) {
            base.setParentInternal(this);
            base.setUiContextInternal(uiContext());
        }
    }

    void detachAnchor(AnchorWidget anchor) {
        Widget child = anchor.widget();
        if (!children.remove(child)) return;
        childSnapshotDirty = true;
        if (child instanceof WidgetBase base) {
            base.setParentInternal(null);
            base.setUiContextInternal(null);
        }
    }

    private void handlePointerPressed(PointerPressedEvent pointer, Event sourceEvent) {
        if (!panningEnabled || pointer.button() != panButton) return;
        if (pointer.target() != this) return;

        panDrag = new PanDragState(pointer.pointerId(), pointer.rootX(), pointer.rootY(), viewport.x(), viewport.y());
        UIContext context = uiContext();
        if (context != null) {
            context.capturePointer(pointer.pointerId(), this);
        }
        sourceEvent.cancel();
    }

    private void handlePointerMoved(PointerMovedEvent pointer) {
        if (panDrag == null || panDrag.pointerId != pointer.pointerId()) return;

        float oldX = viewport.x();
        float oldY = viewport.y();
        float oldZoom = viewport.zoom();
        float nextX = panDrag.startViewportX + pointer.rootX() - panDrag.startRootX;
        float nextY = panDrag.startViewportY + pointer.rootY() - panDrag.startRootY;
        if (!viewport.setPosition(nextX, nextY)) return;
        afterViewportChanged(oldX, oldY, oldZoom);
    }

    private void handlePointerReleased(PointerReleasedEvent pointer, Event sourceEvent) {
        if (panDrag == null || panDrag.pointerId != pointer.pointerId()) return;
        UIContext context = uiContext();
        if (context != null) {
            context.releasePointer(pointer.pointerId(), this);
        }
        panDrag = null;
        sourceEvent.cancel();
    }

    private void handleScroll(ScrollEvent scroll, Event sourceEvent) {
        if (scroll.phase() == EventPhase.CAPTURE) return;

        boolean handled = false;
        boolean ctrlWheel = KeyModifiers.has(scroll.modifiers(), KeyModifiers.CONTROL);
        if (ctrlWheel) {
            if (zoomEnabled && scroll.deltaY() != 0.0f) {
                float factor = (float) Math.pow(zoomStep, scroll.deltaY());
                handled = zoomAt(scroll.rootX(), scroll.rootY(), factor);
            } else {
                handled = true;
            }
        } else if (wheelPanningEnabled && (scroll.deltaX() != 0.0f || scroll.deltaY() != 0.0f)) {
            float deltaX = scroll.deltaX();
            float deltaY = scroll.deltaY();
            if (KeyModifiers.has(scroll.modifiers(), KeyModifiers.SHIFT) && deltaY != 0.0f) {
                deltaX = deltaY;
                deltaY = 0.0f;
            }

            float oldX = viewport.x();
            float oldY = viewport.y();
            float oldZoom = viewport.zoom();
            if (viewport.panBy(deltaX * wheelPanStep, deltaY * wheelPanStep)) {
                afterViewportChanged(oldX, oldY, oldZoom);
                handled = true;
            }
        }

        if (handled || consumeWheelWhileHovered) {
            sourceEvent.cancel();
        }
    }

    private boolean zoomAt(float rootX, float rootY, float factor) {
        float oldX = viewport.x();
        float oldY = viewport.y();
        float oldZoom = viewport.zoom();

        float localX = rootX - layoutBounds().x();
        float localY = rootY - layoutBounds().y();
        if (!viewport.zoomAt(localX, localY, factor)) return false;
        afterViewportChanged(oldX, oldY, oldZoom);
        return true;
    }

    private void clampViewport() {
        if (viewport.clamp(layoutBounds().width(), layoutBounds().height())) {
            arrangeAnchors();
            invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        }
    }

    private void afterViewportChanged(float oldX, float oldY, float oldZoom) {
        arrangeAnchors();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        dispatch(new ViewportChangedEvent(this, oldX, oldY, oldZoom, viewport.x(), viewport.y(), viewport.zoom()));
    }

    private void arrangeAnchors() {
        AnchorWidget[] anchors = anchorLayer.snapshot();
        if (anchors.length == 0) return;

        LayoutContext childContext = new LayoutContext(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY);
        for (AnchorWidget anchor : anchors) {
            Widget child = anchor.widget();
            if (child.visibility() == Visibility.COLLAPSED) {
                anchor.arrangedVisible(false);
                continue;
            }

            child.measure(childContext);
            float width = anchor.screenWidth() >= 0.0f ? anchor.screenWidth() : child.desiredSize().width();
            float height = anchor.screenHeight() >= 0.0f ? anchor.screenHeight() : child.desiredSize().height();
            width = sanitizeSize(width);
            height = sanitizeSize(height);

            float rootX = worldToRootX(anchor.worldX());
            float rootY = worldToRootY(anchor.worldY());
            float x = rootX - width * anchor.pivotX();
            float y = rootY - height * anchor.pivotY();

            boolean visible = anchor.visibleAtZoom(viewport.zoom())
                    && (!anchor.cullOutsideViewport()
                    || rectsIntersect(x, y, width, height,
                    layoutBounds().x(), layoutBounds().y(), layoutBounds().width(), layoutBounds().height()));
            anchor.arrangedVisible(visible);
            anchor.projectedRoot(rootX, rootY);

            if (visible) {
                child.arrange(new MutableRect(x, y, width, height));
            } else {
                child.arrange(new MutableRect(rootX, rootY, 0.0f, 0.0f));
            }
        }
    }

    private Widget[] childSnapshot() {
        if (childSnapshotDirty) {
            childSnapshot = children.toArray(new Widget[children.size()]);
            childSnapshotDirty = false;
        }
        return childSnapshot;
    }

    private WorldLayer[] worldLayerSnapshot() {
        if (worldLayerSnapshotDirty) {
            worldLayerSnapshot = worldLayers.toArray(new WorldLayer[worldLayers.size()]);
            worldLayerSnapshotDirty = false;
        }
        return worldLayerSnapshot;
    }

    private <T extends Event> T dispatch(T event) {
        UIContext context = uiContext();
        if (context != null && event instanceof RoutableWidgetEvent widgetEvent) {
            context.routedEvents().dispatch(widgetEvent);
        } else {
            emit(event);
        }
        return event;
    }

    private static float sanitizeSize(float value) {
        return Float.isFinite(value) ? Math.max(0.0f, value) : 0.0f;
    }

    private static boolean rectsIntersect(float ax, float ay, float aw, float ah,
                                          float bx, float by, float bw, float bh) {
        return ax + aw >= bx && bx + bw >= ax && ay + ah >= by && by + bh >= ay;
    }

    private record PanDragState(
            int pointerId,
            float startRootX,
            float startRootY,
            float startViewportX,
            float startViewportY
    ) {
    }
}
