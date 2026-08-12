package dev.sixik.unigui.widgets;

import dev.sixik.unigui.api.core.FrameContext;
import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.core.UIContext;
import dev.sixik.unigui.api.event.DockLayoutChangedEvent;
import dev.sixik.unigui.api.event.DockLayoutRestoredEvent;
import dev.sixik.unigui.api.event.DockDragEndedEvent;
import dev.sixik.unigui.api.event.DockDragMovedEvent;
import dev.sixik.unigui.api.event.DockDragStartedEvent;
import dev.sixik.unigui.api.event.DockDropPreviewChangedEvent;
import dev.sixik.unigui.api.event.Event;
import dev.sixik.unigui.api.event.EventListener;
import dev.sixik.unigui.api.event.EventPhase;
import dev.sixik.unigui.api.event.EventSubscription;
import dev.sixik.unigui.api.event.KeyPressedEvent;
import dev.sixik.unigui.api.event.PointerMovedEvent;
import dev.sixik.unigui.api.event.PointerExitedEvent;
import dev.sixik.unigui.api.event.PointerPressedEvent;
import dev.sixik.unigui.api.event.PointerReleasedEvent;
import dev.sixik.unigui.api.event.ScrollEvent;
import dev.sixik.unigui.api.input.KeyCodes;
import dev.sixik.unigui.api.input.KeyModifiers;
import dev.sixik.unigui.api.input.MouseCursor;
import dev.sixik.unigui.api.input.PointerButton;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.layout.LayoutSize;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.api.widget.skin.WidgetsRender;
import dev.sixik.unigui.impl.widget.WidgetBase;
import dev.sixik.unigui.widgets.render.DockDropPreviewRenderer;
import dev.sixik.unigui.widgets.render.DockDropPreviewState;
import dev.sixik.unigui.widgets.render.DockPaneRenderer;
import dev.sixik.unigui.widgets.render.DockPaneState;
import dev.sixik.unigui.widgets.render.DockSplitHandleRenderer;
import dev.sixik.unigui.widgets.render.DockSplitHandleState;
import dev.sixik.unigui.widgets.render.DockTabState;
import dev.sixik.unigui.widgets.render.DockingRootRenderer;
import dev.sixik.unigui.widgets.render.DockingRootState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Visual root host for the IDE-style docking system.
 *
 * <p>DockingRoot owns widget-facing state such as pane chrome, tab strips,
 * split handles, drop previews, floating-window redock integration and input
 * handling. Persistent tree mutation and restore operations are delegated to
 * {@link DockingManager}.</p>
 *
 * <p>Use {@link DockPanel} for simple edge-based child layout. DockingRoot is
 * the docking system root; DockPanel is only a layout container.</p>
 */
public final class DockingRoot extends Box {
    private static final float DEFAULT_WIDTH = 320.0f;
    private static final float DEFAULT_HEIGHT = 200.0f;
    private static final float DEFAULT_TOOL_PANE_FRACTION = 0.28f;
    private static final float MIN_TAB_WIDTH = 56.0f;
    private static final float MAX_TAB_WIDTH = 132.0f;
    private static final float OVERFLOW_BUTTON_WIDTH = 24.0f;
    private static final float OVERFLOW_MENU_WIDTH = 168.0f;
    private static final float OVERFLOW_MENU_ROW_HEIGHT = 20.0f;
    private static final int OVERFLOW_MENU_MAX_VISIBLE_ROWS = 8;
    private static final MutableColor OVERFLOW_MENU_BACKGROUND =
            new MutableColor(0.045f, 0.052f, 0.070f, 0.98f);
    private static final MutableColor OVERFLOW_MENU_BORDER =
            new MutableColor(0.25f, 0.78f, 1.0f, 0.85f);
    private static final MutableColor OVERFLOW_MENU_SELECTED =
            new MutableColor(0.12f, 0.16f, 0.23f, 0.98f);
    private static final MutableColor OVERFLOW_MENU_HOVER =
            new MutableColor(0.18f, 0.24f, 0.34f, 0.96f);
    private static final MutableColor OVERFLOW_MENU_TEXT =
            new MutableColor(0.88f, 0.93f, 1.0f, 1.0f);

    private final DockingManager manager = new DockingManager(this);
    private final DockDragController dragController = new DockDragController(this);
    private final Set<Widget> registeredContents = Collections.newSetFromMap(new IdentityHashMap<>());
    private DockingRootRenderer rootRenderer;
    private DockPaneRenderer paneRenderer;
    private DockSplitHandleRenderer splitHandleRenderer;
    private DockDropPreviewRenderer dropPreviewRenderer;
    private WindowWidget lastFloatingWindow;
    private final List<WindowWidget> floatingDockWindows = new ArrayList<>();
    private String hoveredPaneId = "";
    private String pressedPaneId = "";
    private String openOverflowNodeId = "";
    private int hoveredOverflowMenuIndex = -1;
    private final Map<String, Integer> tabScrollOffsets = new HashMap<>();
    private final Map<String, Integer> overflowMenuScrollOffsets = new HashMap<>();
    private DockDropIntent floatingDropPreviewIntent = DockDropIntent.none();
    private float tabHeight = 22.0f;
    private float splitHandleThickness = 5.0f;
    private boolean hasArrangedBounds;
    private boolean allowFloatingOutsideHost;
    private boolean floatingWindowsRedockLocked;

    // Split-drag state
    private String hoveredSplitNodeId = "";
    private String pressedSplitNodeId = "";
    private int splitDragPointerId = -1;
    private float splitDragStartX;
    private float splitDragStartY;
    private float splitDragStartRatio;
    private DockSplitOrientation splitDragOrientation;
    private float splitDragAvailable;  // total track length (px) at drag start

    public DockingRoot() {
        backgroundVisible(true);
        borderVisible(true);
        radius(4.0f);
        background().set(0.025f, 0.030f, 0.042f, 0.98f);
        borderColor().set(0.18f, 0.22f, 0.30f, 0.95f);
        borderWidth(1.0f);
        focusable(true);
    }

    public DockingManager dockingManager() {
        return manager;
    }

    public DockingManager manager() {
        return manager;
    }

    public DockNode rootNode() {
        return manager.rootNode();
    }

    public DockDragController dragController() {
        return dragController;
    }

    public DockDropIntent dockDropPreview() {
        return dragController.previewIntent();
    }

    public boolean allowFloatingOutsideHost() {
        return allowFloatingOutsideHost;
    }

    public DockingRoot allowFloatingOutsideHost(boolean allowFloatingOutsideHost) {
        if (this.allowFloatingOutsideHost == allowFloatingOutsideHost) return this;
        this.allowFloatingOutsideHost = allowFloatingOutsideHost;
        return this;
    }

    public boolean floatingWindowsRedockLocked() {
        return floatingWindowsRedockLocked;
    }

    public DockingRoot floatingWindowsRedockLocked(boolean floatingWindowsRedockLocked) {
        this.floatingWindowsRedockLocked = floatingWindowsRedockLocked;
        syncFloatingDockWindowRedockLocks();
        return this;
    }

    public boolean dockDragging() {
        return dragController.dragging();
    }

    public WindowWidget lastFloatingWindow() {
        return lastFloatingWindow;
    }

    public DockingRoot addPane(DockPane pane) {
        manager.addPane(pane);
        return this;
    }

    public DockingRoot splitPane(String targetPaneId, DockArea area, DockPane pane) {
        manager.splitPane(targetPaneId, area, pane);
        return this;
    }

    public DockingRoot tabPane(String targetPaneId, DockPane pane) {
        manager.tabPane(targetPaneId, pane);
        return this;
    }

    public DockingRoot selectPane(String paneId) {
        manager.selectPane(paneId);
        return this;
    }

    public DockingRoot restoreLayout(DockLayoutSnapshot snapshot, Map<String, DockPane> paneRegistry) {
        manager.restore(snapshot, paneRegistry);
        return this;
    }

    public DockingRoot addDocument(String id, String title, Widget content) {
        DockPane pane = DockPane.document(id, title, content);
        DockPane target = firstDocumentPane();
        if (target == null) {
            manager.addPane(pane);
        } else {
            manager.tabPane(target.id(), pane);
        }
        return this;
    }

    public DockingRoot addToolPane(String id, String title, Widget content, DockArea area) {
        DockPane pane = DockPane.tool(id, title, content);
        DockArea dockArea = area == null ? DockArea.LEFT : area;
        DockPane target = firstDocumentPane();
        if (target == null) target = manager.selectedPane();
        if (target == null) {
            manager.addPane(pane);
        } else if (dockArea == DockArea.CENTER || dockArea == DockArea.TAB) {
            manager.tabPane(target.id(), pane);
        } else {
            manager.splitPane(target.id(), dockArea, pane, toolPaneSplitRatio(dockArea));
        }
        return this;
    }

    private static float toolPaneSplitRatio(DockArea area) {
        return area != null && area.insertsBeforeTarget()
                ? DEFAULT_TOOL_PANE_FRACTION
                : 1.0f - DEFAULT_TOOL_PANE_FRACTION;
    }

    public boolean closeActivePane() {
        return manager.closeActivePane();
    }

    public boolean selectNextTab() {
        return manager.selectNextTab();
    }

    public boolean selectPreviousTab() {
        return manager.selectPreviousTab();
    }

    public float tabHeight() {
        return tabHeight;
    }

    public DockingRoot tabHeight(float tabHeight) {
        float normalized = Float.isFinite(tabHeight) ? Math.max(14.0f, tabHeight) : 22.0f;
        if (this.tabHeight == normalized) return this;
        this.tabHeight = normalized;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public float splitHandleThickness() {
        return splitHandleThickness;
    }

    public DockingRoot splitHandleThickness(float splitHandleThickness) {
        float normalized = Float.isFinite(splitHandleThickness) ? Math.max(1.0f, splitHandleThickness) : 5.0f;
        if (this.splitHandleThickness == normalized) return this;
        this.splitHandleThickness = normalized;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public DockingRootRenderer rootRenderer() {
        return rootRenderer;
    }

    public DockingRoot rootRenderer(DockingRootRenderer rootRenderer) {
        if (this.rootRenderer == rootRenderer) return this;
        this.rootRenderer = rootRenderer;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public DockPaneRenderer paneRenderer() {
        return paneRenderer;
    }

    public DockingRoot paneRenderer(DockPaneRenderer paneRenderer) {
        if (this.paneRenderer == paneRenderer) return this;
        this.paneRenderer = paneRenderer;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public DockSplitHandleRenderer splitHandleRenderer() {
        return splitHandleRenderer;
    }

    public DockingRoot splitHandleRenderer(DockSplitHandleRenderer splitHandleRenderer) {
        if (this.splitHandleRenderer == splitHandleRenderer) return this;
        this.splitHandleRenderer = splitHandleRenderer;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public DockDropPreviewRenderer dropPreviewRenderer() {
        return dropPreviewRenderer;
    }

    public DockingRoot dropPreviewRenderer(DockDropPreviewRenderer dropPreviewRenderer) {
        if (this.dropPreviewRenderer == dropPreviewRenderer) return this;
        this.dropPreviewRenderer = dropPreviewRenderer;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public DockingRoot useDefaultDockingRenderers() {
        rootRenderer = null;
        paneRenderer = null;
        splitHandleRenderer = null;
        dropPreviewRenderer = null;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public EventSubscription onLayoutChanged(EventListener<? super DockLayoutChangedEvent> listener) {
        return on(DockLayoutChangedEvent.TYPE, listener);
    }

    public EventSubscription onLayoutRestored(EventListener<? super DockLayoutRestoredEvent> listener) {
        return on(DockLayoutRestoredEvent.TYPE, listener);
    }

    public EventSubscription onDragStarted(EventListener<? super DockDragStartedEvent> listener) {
        return on(DockDragStartedEvent.TYPE, listener);
    }

    public EventSubscription onDragMoved(EventListener<? super DockDragMovedEvent> listener) {
        return on(DockDragMovedEvent.TYPE, listener);
    }

    public EventSubscription onDragEnded(EventListener<? super DockDragEndedEvent> listener) {
        return on(DockDragEndedEvent.TYPE, listener);
    }

    public EventSubscription onDropPreviewChanged(EventListener<? super DockDropPreviewChangedEvent> listener) {
        return on(DockDropPreviewChangedEvent.TYPE, listener);
    }

    @Override
    public void measure(LayoutContext context) {
        if (visibility() == Visibility.COLLAPSED) {
            setDesiredSize(LayoutSize.ZERO);
            return;
        }
        syncPaneChildren();
        applyQueuedMutations();
        float width = finiteOr(context == null ? Float.NaN : context.availableWidth(), DEFAULT_WIDTH);
        float height = finiteOr(context == null ? Float.NaN : context.availableHeight(), DEFAULT_HEIGHT);
        measureSelectedContents(rootNode(), new LayoutContext(width, Math.max(0.0f, height - tabHeight)));
        setDesiredSize(resolveDesiredSize(context, width, height));
    }

    @Override
    public void arrange(RectView bounds) {
        mutableLayoutBounds().set(bounds);
        hasArrangedBounds = bounds != null && bounds.width() > 0.0f && bounds.height() > 0.0f;
        if (visibility() == Visibility.COLLAPSED) return;
        syncPaneChildren();
        applyQueuedMutations();
        Set<Widget> arranged = Collections.newSetFromMap(new IdentityHashMap<>());
        arrangeNode(rootNode(), bounds, arranged);
        for (DockPane pane : manager.panes()) {
            Widget content = pane.content();
            if (!arranged.contains(content)) {
                content.arrange(new MutableRect(bounds.x(), bounds.y(), 0.0f, 0.0f));
            }
        }
    }

    @Override
    public void render(RenderContext context) {
        if (visibility() != Visibility.VISIBLE) return;
        syncPaneChildren();
        applyQueuedMutations();
        pushOpacity(context);
        try {
            effectiveRootRenderer().render(new DrawScope(context, transform()), rootState());
            renderSelectedContents(context, rootNode(), layoutBounds());
            renderChrome(context);
            renderOverflowMenu(context);
        } finally {
            popOpacity(context);
        }
    }

    @Override
    public void tick(FrameContext frame) {
        if (visibility() != Visibility.VISIBLE) return;
        super.tick(frame);
    }

    @Override
    public MouseCursor mouseCursorAt(float localX, float localY) {
        if (!enabled() || visibility() != Visibility.VISIBLE) return MouseCursor.DEFAULT;
        // Show resize cursor when hovering or dragging a split handle
        if (!pressedSplitNodeId.isEmpty()) {
            return splitDragOrientation == DockSplitOrientation.HORIZONTAL
                    ? MouseCursor.RESIZE_HORIZONTAL : MouseCursor.RESIZE_VERTICAL;
        }
        if (!hoveredSplitNodeId.isEmpty()) {
            SplitNodeHit hit = splitHandleAt(rootNode(), layoutBounds(),
                    layoutBounds().x() + localX, layoutBounds().y() + localY);
            if (hit != null) {
                return hit.orientation() == DockSplitOrientation.HORIZONTAL
                        ? MouseCursor.RESIZE_HORIZONTAL : MouseCursor.RESIZE_VERTICAL;
            }
        }
        return super.mouseCursorAt(localX, localY);
    }

    @Override
    public void handle(Event event) {
        if (visibility() != Visibility.VISIBLE || !enabled()) return;
        if (handleOverflowMenuEventBeforeChildren(event)) return;
        super.handle(event);
        if (event.isCancelled()) return;

        if (event instanceof KeyPressedEvent key && key.phase() == EventPhase.TARGET) {
            if (handleDockShortcut(key)) {
                event.cancel();
            }
            return;
        }

        // -- Split drag: move --------------------------------------------------
        if (event instanceof PointerMovedEvent pointer && !pressedSplitNodeId.isEmpty()
                && pointer.pointerId() == splitDragPointerId) {
            DockNode node = findSplitNodeById(rootNode(), pressedSplitNodeId);
            if (node != null) {
                float newRatio = computeNewSplitRatio(
                        pointer.rootX(), pointer.rootY());
                node.splitRatio(newRatio);
                invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
            }
            event.cancel();
            return;
        }

        // -- Split drag: end ---------------------------------------------------
        if (event instanceof PointerReleasedEvent pointer
                && pointer.button() == PointerButton.PRIMARY
                && !pressedSplitNodeId.isEmpty()
                && pointer.pointerId() == splitDragPointerId) {
            UIContext ctx = uiContext();
            if (ctx != null) ctx.releasePointer(pointer.pointerId(), this);
            pressedSplitNodeId = "";
            splitDragPointerId = -1;
            invalidate(InvalidationFlags.VISUAL);
            event.cancel();
            return;
        }

        // -- Tab drag: move ----------------------------------------------------
        if (event instanceof PointerMovedEvent pointer && dragController.active()) {
            if (dragController.move(pointer.pointerId(), pointer.rootX(), pointer.rootY())) {
                event.cancel();
            }
            return;
        }

        if (event instanceof ScrollEvent scroll && scroll.phase() == EventPhase.TARGET) {
            if (scrollTabsAt(scroll.rootX(), scroll.rootY(), scroll.deltaX(), scroll.deltaY())) {
                event.cancel();
            }
            return;
        }

        // -- Hover: split handle & pane tab ------------------------------------
        if (event instanceof PointerMovedEvent pointer && pointer.phase() == EventPhase.TARGET) {
            SplitNodeHit splitHit = splitHandleAt(rootNode(), layoutBounds(), pointer.rootX(), pointer.rootY());
            setHoveredSplitNode(splitHit == null ? "" : splitHit.node().id());
            if (splitHit == null) {
                DockPane pane = tabAt(rootNode(), layoutBounds(), pointer.rootX(), pointer.rootY());
                setHoveredPane(pane == null ? "" : pane.id());
            } else {
                setHoveredPane("");
            }
        }

        if (event instanceof PointerExitedEvent exited && exited.phase() == EventPhase.TARGET) {
            setHoveredPane("");
            setHoveredSplitNode("");
        }

        // -- Tab drag: end -----------------------------------------------------
        if (event instanceof PointerReleasedEvent pointer
                && pointer.button() == PointerButton.PRIMARY
                && dragController.active()) {
            int pointerId = pointer.pointerId();
            if (dragController.end(pointerId, pointer.rootX(), pointer.rootY())) {
                UIContext context = uiContext();
                if (context != null) {
                    context.releasePointer(pointerId, this);
                }
                setPressedPane("");
                event.cancel();
            }
            return;
        }

        if (event instanceof PointerReleasedEvent pointer && pointer.button() == PointerButton.PRIMARY) {
            setPressedPane("");
        }

        // -- Press: split handle or tab ----------------------------------------
        if (event instanceof PointerPressedEvent pointer
                && pointer.phase() == EventPhase.TARGET
                && pointer.button() == PointerButton.PRIMARY) {

            // Split handle has priority over overflow button and tabs
            SplitNodeHit splitHit = splitHandleAt(rootNode(), layoutBounds(), pointer.rootX(), pointer.rootY());
            if (splitHit != null) {
                pressedSplitNodeId = splitHit.node().id();
                splitDragPointerId = pointer.pointerId();
                splitDragStartX = pointer.rootX();
                splitDragStartY = pointer.rootY();
                splitDragStartRatio = splitHit.node().splitRatio();
                splitDragOrientation = splitHit.orientation();
                // Compute total track length for ratio math
                RectView nb = splitHit.nodeBounds();
                float thickness = Math.min(splitHandleThickness,
                        Math.max(0.0f, splitDragOrientation == DockSplitOrientation.HORIZONTAL
                                ? nb.width() : nb.height()));
                splitDragAvailable = Math.max(1.0f,
                        (splitDragOrientation == DockSplitOrientation.HORIZONTAL
                                ? nb.width() : nb.height()) - thickness);
                UIContext ctx = uiContext();
                if (ctx != null) ctx.capturePointer(pointer.pointerId(), this);
                openOverflowNodeId = "";
                invalidate(InvalidationFlags.VISUAL);
                event.cancel();
                return;
            }

            LeafHit overflowButton = overflowButtonAt(rootNode(), layoutBounds(), pointer.rootX(), pointer.rootY());
            if (overflowButton != null) {
                openOverflowNodeId = overflowButton.node().id().equals(openOverflowNodeId) ? "" : overflowButton.node().id();
                if (!openOverflowNodeId.isEmpty()) {
                    revealOverflowMenuSelection(overflowButton.node());
                }
                invalidate(InvalidationFlags.VISUAL);
                event.cancel();
                return;
            }
            DockPane pane = tabAt(rootNode(), layoutBounds(), pointer.rootX(), pointer.rootY());
            if (pane != null) {
                manager.selectPane(pane.id());
                openOverflowNodeId = "";
                setPressedPane(pane.id());
                if (dragController.begin(pane, pointer.pointerId(), pointer.rootX(), pointer.rootY())) {
                    UIContext context = uiContext();
                    if (context != null) {
                        context.capturePointer(pointer.pointerId(), this);
                    }
                }
                event.cancel();
            } else if (!openOverflowNodeId.isEmpty()) {
                closeOverflowMenu();
            }
        }
    }

    private boolean handleOverflowMenuEventBeforeChildren(Event event) {
        if (event instanceof ScrollEvent scroll
                && scroll.phase() != EventPhase.BUBBLE
                && scrollOverflowMenuAt(scroll.rootX(), scroll.rootY(), scroll.deltaX(), scroll.deltaY())) {
            event.cancel();
            return true;
        }

        if (event instanceof PointerMovedEvent pointer && pointer.phase() != EventPhase.BUBBLE) {
            if (!openOverflowNodeId.isEmpty()) {
                OverflowMenuItem hovered = overflowMenuItemAt(pointer.rootX(), pointer.rootY());
                setHoveredOverflowMenuIndex(hovered == null ? -1 : hovered.index());
                if (hovered != null) {
                    setHoveredPane("");
                    event.cancel();
                    return true;
                }
            }
        }

        if (event instanceof PointerExitedEvent exited && exited.phase() != EventPhase.BUBBLE) {
            setHoveredOverflowMenuIndex(-1);
        }

        if (event instanceof PointerPressedEvent pointer
                && pointer.phase() != EventPhase.BUBBLE
                && pointer.button() == PointerButton.PRIMARY
                && !openOverflowNodeId.isEmpty()) {
            OverflowMenuItem menuItem = overflowMenuItemAt(pointer.rootX(), pointer.rootY());
            if (menuItem != null) {
                int tabScrollOffset = normalizedTabScrollOffset(menuItem.node(), layoutBounds());
                manager.selectPane(menuItem.pane().id());
                tabScrollOffsets.put(menuItem.node().id(), tabScrollOffset);
                closeOverflowMenu();
                event.cancel();
                return true;
            }

            LeafHit overflowButton = overflowButtonAt(rootNode(), layoutBounds(), pointer.rootX(), pointer.rootY());
            if (overflowButton != null) {
                openOverflowNodeId = overflowButton.node().id().equals(openOverflowNodeId) ? "" : overflowButton.node().id();
                if (!openOverflowNodeId.isEmpty()) {
                    revealOverflowMenuSelection(overflowButton.node());
                }
                setHoveredOverflowMenuIndex(-1);
                invalidate(InvalidationFlags.VISUAL);
                event.cancel();
                return true;
            }

            closeOverflowMenu();
            event.cancel();
            return true;
        }

        return false;
    }

    private void setHoveredPane(String paneId) {
        String normalized = paneId == null ? "" : paneId;
        if (hoveredPaneId.equals(normalized)) return;
        hoveredPaneId = normalized;
        invalidate(InvalidationFlags.VISUAL);
    }

    private void setPressedPane(String paneId) {
        String normalized = paneId == null ? "" : paneId;
        if (pressedPaneId.equals(normalized)) return;
        pressedPaneId = normalized;
        invalidate(InvalidationFlags.VISUAL);
    }

    private void setHoveredSplitNode(String nodeId) {
        String normalized = nodeId == null ? "" : nodeId;
        if (hoveredSplitNodeId.equals(normalized)) return;
        hoveredSplitNodeId = normalized;
        invalidate(InvalidationFlags.VISUAL);
    }

    private void setHoveredOverflowMenuIndex(int index) {
        int normalized = Math.max(-1, index);
        if (hoveredOverflowMenuIndex == normalized) return;
        hoveredOverflowMenuIndex = normalized;
        invalidate(InvalidationFlags.VISUAL);
    }

    private void closeOverflowMenu() {
        if (openOverflowNodeId.isEmpty() && hoveredOverflowMenuIndex < 0) return;
        openOverflowNodeId = "";
        hoveredOverflowMenuIndex = -1;
        invalidate(InvalidationFlags.VISUAL);
    }

    private boolean handleDockShortcut(KeyPressedEvent key) {
        if (!KeyModifiers.has(key.modifiers(), KeyModifiers.CONTROL)) return false;
        if (key.keyCode() == KeyCodes.W) {
            return closeActivePane();
        }
        if (key.keyCode() == KeyCodes.TAB) {
            return KeyModifiers.has(key.modifiers(), KeyModifiers.SHIFT)
                    ? selectPreviousTab()
                    : selectNextTab();
        }
        return false;
    }

    void unregisterPaneContent(DockPane pane) {
        if (pane == null) return;
        Widget content = pane.content();
        if (registeredContents.remove(content)) {
            super.removeChild(content);
            applyQueuedMutations();
        }
    }

    void onDockLayoutChanged(String operation, String paneId, String targetPaneId) {
        if (hasArrangedBounds || isSelectionOperation(operation)) {
            revealPane(paneId);
        }
        syncPaneChildren();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        DockLayoutChangedEvent event = new DockLayoutChangedEvent(this, operation, paneId, targetPaneId);
        dispatchDockEvent(event);
    }

    private static boolean isSelectionOperation(String operation) {
        return "select".equals(operation)
                || "select_next".equals(operation)
                || "select_previous".equals(operation);
    }

    void onDockLayoutRestored(DockLayoutSnapshot snapshot, int restoredPaneCount, int missingPaneCount) {
        revealPane(activePaneId());
        syncPaneChildren();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        dispatchDockEvent(new DockLayoutRestoredEvent(this, snapshot, restoredPaneCount, missingPaneCount));
    }

    void dispatchDockDragStarted(String paneId, float rootX, float rootY) {
        dispatchDockEvent(new DockDragStartedEvent(this, paneId, rootX, rootY));
    }

    void dispatchDockDragMoved(String paneId, float rootX, float rootY, DockDropIntent intent) {
        dispatchDockEvent(new DockDragMovedEvent(this, paneId, rootX, rootY, intent));
    }

    void dispatchDockDragEnded(String paneId, float rootX, float rootY, DockDropIntent intent, boolean dropped) {
        dispatchDockEvent(new DockDragEndedEvent(this, paneId, rootX, rootY, intent, dropped));
    }

    void dispatchDockDropPreviewChanged(String paneId, DockDropIntent oldIntent, DockDropIntent newIntent) {
        invalidate(InvalidationFlags.VISUAL);
        dispatchDockEvent(new DockDropPreviewChangedEvent(this, paneId, oldIntent, newIntent));
    }

    DockDropIntent resolveDockDropIntent(String sourcePaneId, float rootX, float rootY) {
        RectView rootBounds = layoutBounds();
        if (!contains(rootBounds, rootX, rootY)) {
            RectView hostBounds = floatingHostBounds();
            if (allowFloatingOutsideHost || contains(hostBounds, rootX, rootY)) {
                return DockDropIntent.floating(sourcePaneId, rootX, rootY);
            }
            return DockDropIntent.none();
        }
        LeafHit hit = leafAt(rootNode(), rootBounds, rootX, rootY);
        if (hit == null || hit.node().panes().isEmpty()) {
            return DockDropIntent.none();
        }
        DockPane target = hit.node().selectedPane();
        if (target == null) target = hit.node().panes().get(0);
        DockArea area = dropArea(hit.bounds(), rootX, rootY);
        if (sourcePaneId != null && sourcePaneId.equals(target.id()) && hit.node().panes().size() == 1) {
            return DockDropIntent.none();
        }
        MutableRect preview = previewBounds(hit.bounds(), area);
        return DockDropIntent.of(sourcePaneId, target.id(), area, preview, rootX, rootY);
    }

    boolean applyDockDropIntent(String paneId, DockDropIntent intent) {
        if (intent == null || !intent.valid()) return false;
        if (intent.floating()) {
            DockPane pane = manager.detachPaneForFloating(paneId);
            lastFloatingWindow = pane == null ? null : createFloatingPaneWindow(pane, intent);
            if (lastFloatingWindow != null) {
                return true;
            }
            return false;
        }
        lastFloatingWindow = null;
        return manager.dockPane(paneId, intent.targetPaneId(), intent.area());
    }

    private WindowWidget createFloatingPaneWindow(DockPane pane, DockDropIntent intent) {
        WindowWidget window = new WindowWidget(pane.richTitle(), pane.content());
        float width = Math.max(180.0f, intent.width());
        float height = Math.max(112.0f, intent.height());
        window.layout(style -> style.size(width, height));
        window.minWindowSize(120.0f, 64.0f);
        window.closeButtonVisible(false);
        window.closeOnOutsideClick(false);
        window.constrainToHost(true);
        window.resizable(true);
        window.dockRedockLocked(floatingWindowsRedockLocked);

        OverlayLayer overlay = nearestOverlayLayer(this);
        RectView hostBounds = overlay == null ? floatingHostBounds() : overlay.layoutBounds();
        float hostWidth = Math.max(0.0f, hostBounds.width());
        float hostHeight = Math.max(0.0f, hostBounds.height());
        float relativeX = intent.x() - hostBounds.x();
        float relativeY = intent.y() - hostBounds.y();
        if (!allowFloatingOutsideHost) {
            relativeX = clamp(relativeX, 0.0f, Math.max(0.0f, hostWidth - width));
            relativeY = clamp(relativeY, 0.0f, Math.max(0.0f, hostHeight - height));
        }
        window.position(relativeX, relativeY);

        if (overlay != null) {
            configureFloatingPaneRedock(window, pane, overlay);
            overlay.addOverlay(window);
        }
        window.open();
        registerFloatingDockWindow(window);
        return window;
    }

    private void registerFloatingDockWindow(WindowWidget window) {
        if (window == null) return;
        floatingDockWindows.removeIf(existing -> existing == null || !existing.opened());
        if (!floatingDockWindows.contains(window)) {
            floatingDockWindows.add(window);
        }
        window.dockRedockLocked(floatingWindowsRedockLocked);
    }

    private void syncFloatingDockWindowRedockLocks() {
        floatingDockWindows.removeIf(window -> window == null || !window.opened());
        for (WindowWidget window : floatingDockWindows) {
            window.dockRedockLocked(floatingWindowsRedockLocked);
        }
    }

    private void configureFloatingPaneRedock(WindowWidget window, DockPane pane, OverlayLayer overlay) {
        final DockingRoot[] previewTarget = {null};
        window.onMoved(event -> {
            if (event.phase() != EventPhase.TARGET) return;
            if (window.dockRedockLocked()) {
                if (previewTarget[0] != null) {
                    previewTarget[0].floatingDropPreview(DockDropIntent.none());
                    previewTarget[0] = null;
                }
                return;
            }
            float dropX = floatingWindowDropX(window, overlay, event.newX());
            float dropY = floatingWindowDropY(window, overlay, event.newY());
            DockingRoot target = dockingRootAt(overlay, window, dropX, dropY);
            if (previewTarget[0] != null && previewTarget[0] != target) {
                previewTarget[0].floatingDropPreview(DockDropIntent.none());
            }
            previewTarget[0] = target;
            if (target == null) return;
            target.floatingDropPreview(target.resolveDockDropIntent(pane.id(), dropX, dropY));
        });
        window.onMoveEnded(event -> {
            if (event.phase() != EventPhase.TARGET) return;
            if (window.dockRedockLocked()) {
                if (previewTarget[0] != null) {
                    previewTarget[0].floatingDropPreview(DockDropIntent.none());
                    previewTarget[0] = null;
                }
                return;
            }
            float dropX = floatingWindowDropX(window, overlay, event.x());
            float dropY = floatingWindowDropY(window, overlay, event.y());
            DockingRoot target = dockingRootAt(overlay, window, dropX, dropY);
            if (previewTarget[0] != null) {
                previewTarget[0].floatingDropPreview(DockDropIntent.none());
                previewTarget[0] = null;
            }
            if (target == null) return;
            redockFloatingPane(window, pane, overlay, target, dropX, dropY);
            event.cancel();
        });
    }

    private static float floatingWindowDropX(WindowWidget window, OverlayLayer overlay, float windowX) {
        return overlay.layoutBounds().x() + windowX
                + Math.min(Math.max(16.0f, window.layoutBounds().width() * 0.5f), 96.0f);
    }

    private static float floatingWindowDropY(WindowWidget window, OverlayLayer overlay, float windowY) {
        return overlay.layoutBounds().y() + windowY + Math.max(6.0f, window.headerHeight() * 0.5f);
    }

    private void redockFloatingPane(WindowWidget window, DockPane pane, OverlayLayer overlay,
                                    DockingRoot target, float rootX, float rootY) {
        window.content(null);
        window.close();
        overlay.removeOverlay(window);
        target.floatingDropPreview(DockDropIntent.none());

        LeafHit hit = target.leafAt(target.rootNode(), target.layoutBounds(), rootX, rootY);
        if (hit == null || hit.node().panes().isEmpty()) {
            target.manager().addPane(pane);
        } else {
            DockPane targetPane = hit.node().selectedPane();
            if (targetPane == null) targetPane = hit.node().panes().get(0);
            DockArea area = target.dropArea(hit.bounds(), rootX, rootY);
            if (area == DockArea.CENTER || area == DockArea.TAB) {
                target.manager().tabPane(targetPane.id(), pane);
            } else {
                target.manager().splitPane(targetPane.id(), area, pane);
            }
        }
        if (lastFloatingWindow == window) {
            lastFloatingWindow = null;
        }
        floatingDockWindows.remove(window);
    }

    private RectView floatingHostBounds() {
        OverlayLayer overlay = nearestOverlayLayer(this);
        if (overlay != null && (overlay.layoutBounds().width() > 0.0f || overlay.layoutBounds().height() > 0.0f)) {
            return overlay.layoutBounds();
        }
        Widget current = this;
        Widget top = this;
        while (current != null) {
            top = current;
            current = current.parent();
        }
        RectView bounds = top.layoutBounds();
        return bounds == null ? layoutBounds() : bounds;
    }

    private static OverlayLayer nearestOverlayLayer(Widget widget) {
        Widget current = widget;
        while (current != null) {
            if (current instanceof OverlayLayer overlay) return overlay;
            current = current.parent();
        }
        return null;
    }

    private static DockingRoot dockingRootAt(Widget root, Widget ignored, float rootX, float rootY) {
        if (root == null || root == ignored || root.visibility() != Visibility.VISIBLE) return null;
        if (!contains(root.layoutBounds(), rootX, rootY)) return null;
        List<Widget> children = List.copyOf(root.children());
        for (int i = children.size() - 1; i >= 0; i--) {
            DockingRoot childHit = dockingRootAt(children.get(i), ignored, rootX, rootY);
            if (childHit != null) return childHit;
        }
        return root instanceof DockingRoot dockingRoot ? dockingRoot : null;
    }

    private void dispatchDockEvent(dev.sixik.unigui.api.event.WidgetEvent event) {
        UIContext context = uiContext();
        if (context == null) {
            emit(event);
        } else {
            context.routedEvents().dispatch(event);
        }
    }

    private LeafHit leafAt(DockNode node, RectView bounds, float rootX, float rootY) {
        if (node == null || !contains(bounds, rootX, rootY)) return null;
        if (node.isLeaf()) {
            return new LeafHit(node, new MutableRect(bounds.x(), bounds.y(), bounds.width(), bounds.height()));
        }
        SplitRects split = splitRects(bounds, node.orientation(), node.splitRatio());
        LeafHit first = leafAt(node.first(), split.first(), rootX, rootY);
        return first != null ? first : leafAt(node.second(), split.second(), rootX, rootY);
    }

    private DockArea dropArea(RectView bounds, float rootX, float rootY) {
        float width = Math.max(1.0f, bounds.width());
        float height = Math.max(1.0f, bounds.height());
        float relativeX = rootX - bounds.x();
        float relativeY = rootY - bounds.y();
        float edgeWidth = Math.min(width * 0.25f, 48.0f);
        float edgeHeight = Math.min(height * 0.25f, 48.0f);
        if (relativeX <= edgeWidth) return DockArea.LEFT;
        if (relativeX >= width - edgeWidth) return DockArea.RIGHT;
        if (relativeY <= edgeHeight && relativeY > Math.min(tabHeight, height)) return DockArea.TOP;
        if (relativeY >= height - edgeHeight) return DockArea.BOTTOM;
        return DockArea.CENTER;
    }

    private MutableRect previewBounds(RectView bounds, DockArea area) {
        float thirdWidth = Math.max(0.0f, bounds.width() * 0.33f);
        float thirdHeight = Math.max(0.0f, bounds.height() * 0.33f);
        return switch (area == null ? DockArea.CENTER : area) {
            case LEFT -> new MutableRect(bounds.x(), bounds.y(), thirdWidth, bounds.height());
            case RIGHT -> new MutableRect(bounds.x() + Math.max(0.0f, bounds.width() - thirdWidth), bounds.y(),
                    thirdWidth, bounds.height());
            case TOP -> new MutableRect(bounds.x(), bounds.y(), bounds.width(), thirdHeight);
            case BOTTOM -> new MutableRect(bounds.x(), bounds.y() + Math.max(0.0f, bounds.height() - thirdHeight),
                    bounds.width(), thirdHeight);
            case CENTER, TAB -> new MutableRect(bounds.x() + 3.0f, bounds.y() + 3.0f,
                    Math.max(0.0f, bounds.width() - 6.0f), Math.max(0.0f, bounds.height() - 6.0f));
            case FLOAT -> new MutableRect(bounds.x(), bounds.y(), bounds.width(), bounds.height());
        };
    }

    private static boolean contains(RectView bounds, float rootX, float rootY) {
        return bounds != null
                && rootX >= bounds.x()
                && rootX <= bounds.x() + bounds.width()
                && rootY >= bounds.y()
                && rootY <= bounds.y() + bounds.height();
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private void syncPaneChildren() {
        Set<Widget> desired = Collections.newSetFromMap(new IdentityHashMap<>());
        for (DockPane pane : manager.panes()) {
            desired.add(pane.content());
            if (registeredContents.add(pane.content())) {
                super.addChild(pane.content());
            }
        }
        List<Widget> stale = new ArrayList<>();
        for (Widget content : registeredContents) {
            if (!desired.contains(content)) {
                stale.add(content);
            }
        }
        for (Widget content : stale) {
            registeredContents.remove(content);
            super.removeChild(content);
        }
    }

    private DockPane firstDocumentPane() {
        for (DockPane pane : manager.panes()) {
            if (pane.document()) return pane;
        }
        return null;
    }

    private void measureSelectedContents(DockNode node, LayoutContext context) {
        if (node == null) return;
        if (node.isLeaf()) {
            DockPane selected = node.selectedPane();
            if (selected != null && selected.content().visibility() != Visibility.COLLAPSED) {
                selected.content().measure(context);
            }
            return;
        }
        measureSelectedContents(node.first(), context);
        measureSelectedContents(node.second(), context);
    }

    private void arrangeNode(DockNode node, RectView bounds, Set<Widget> arranged) {
        if (node == null) return;
        if (node.isLeaf()) {
            DockPane selected = node.selectedPane();
            if (selected == null) return;
            MutableRect contentBounds = contentBounds(bounds);
            selected.content().arrange(contentBounds);
            arranged.add(selected.content());
            return;
        }
        SplitRects split = splitRects(bounds, node.orientation(), node.splitRatio());
        arrangeNode(node.first(), split.first(), arranged);
        arrangeNode(node.second(), split.second(), arranged);
    }

    private void renderSelectedContents(RenderContext context, DockNode node, RectView bounds) {
        if (node == null) return;
        if (node.isLeaf()) {
            DockPane selected = node.selectedPane();
            if (selected != null && selected.content().visibility() == Visibility.VISIBLE) {
                MutableRect clip = contentBounds(bounds);
                context.pushClip(clip.x(), clip.y(), clip.width(), clip.height());
                try {
                    selected.content().render(context);
                } finally {
                    context.popClip();
                }
            }
            return;
        }
        SplitRects split = splitRects(bounds, node.orientation(), node.splitRatio());
        renderSelectedContents(context, node.first(), split.first());
        renderSelectedContents(context, node.second(), split.second());
    }

    private MutableRect contentBounds(RectView bounds) {
        float tab = Math.min(tabHeight, bounds.height());
        float contentY = bounds.y() + tab;
        float contentHeight = Math.max(0.0f, bounds.height() - tab);
        return new MutableRect(
                bounds.x() + 1.0f,
                contentY,
                Math.max(0.0f, bounds.width() - 2.0f),
                Math.max(0.0f, contentHeight - 1.0f));
    }

    private void renderChrome(RenderContext context) {
        DrawScope draw = new DrawScope(context, transform());
        for (DockPaneState pane : paneStates(rootNode(), layoutBounds())) {
            effectivePaneRenderer().render(draw, pane);
        }
        for (DockSplitHandleState handle : splitStates(rootNode(), layoutBounds())) {
            effectiveSplitHandleRenderer().render(draw, handle);
        }
        DockDropIntent preview = dragController.previewIntent().valid()
                ? dragController.previewIntent()
                : floatingDropPreviewIntent;
        if (preview.valid()) {
            effectiveDropPreviewRenderer().render(draw, new DockDropPreviewState(
                    true,
                    preview.sourcePaneId(),
                    preview.targetPaneId(),
                    preview.area(),
                    preview.x(),
                    preview.y(),
                    preview.width(),
                    preview.height()));
        }
    }

    private void floatingDropPreview(DockDropIntent intent) {
        DockDropIntent normalized = intent == null ? DockDropIntent.none() : intent;
        if (floatingDropPreviewIntent.equals(normalized)) return;
        floatingDropPreviewIntent = normalized;
        invalidate(InvalidationFlags.VISUAL);
    }

    private void renderOverflowMenu(RenderContext context) {
        if (openOverflowNodeId.isEmpty()) return;
        LeafHit hit = leafById(rootNode(), layoutBounds(), openOverflowNodeId);
        if (hit == null || hit.node().panes().isEmpty() || !tabsOverflow(hit.node(), hit.bounds())) {
            openOverflowNodeId = "";
            return;
        }
        DrawScope draw = new DrawScope(context, transform());
        MutableRect menu = overflowMenuBounds(hit);
        float x = menu.x();
        float y = menu.y();
        float menuWidth = menu.width();
        float rowHeight = Math.max(16.0f, OVERFLOW_MENU_ROW_HEIGHT);
        draw.roundedRect(menu.x(), menu.y(), menu.width(), menu.height(), 4.0f, Paint.fill(OVERFLOW_MENU_BACKGROUND));
        draw.rect(menu.x(), menu.y(), menu.width(), menu.height(), Paint.stroke(OVERFLOW_MENU_BORDER, 1.0f));
        int first = normalizedOverflowMenuScrollOffset(hit.node());
        int visibleRows = overflowMenuVisibleRows(hit.node());
        int lastExclusive = Math.min(hit.node().panes().size(), first + visibleRows);
        draw.pushClip(menu.x(), menu.y(), menu.width(), menu.height());
        try {
        for (int i = first; i < lastExclusive; i++) {
            DockPane pane = hit.node().panes().get(i);
            float rowY = y + (i - first) * rowHeight;
            if (i == hoveredOverflowMenuIndex) {
                draw.rect(x + 1.0f, rowY + 1.0f, Math.max(0.0f, menuWidth - 2.0f), Math.max(0.0f, rowHeight - 2.0f),
                        Paint.fill(OVERFLOW_MENU_HOVER));
            } else if (i == hit.node().selectedIndex()) {
                draw.rect(x + 1.0f, rowY + 1.0f, Math.max(0.0f, menuWidth - 2.0f), Math.max(0.0f, rowHeight - 2.0f),
                        Paint.fill(OVERFLOW_MENU_SELECTED));
            }
            draw.pushClip(x + 6.0f, rowY, Math.max(0.0f, menuWidth - 12.0f), rowHeight);
            try {
                draw.text(pane.richTitle(), x + 6.0f, rowY + 4.0f,
                        Math.max(0.0f, menuWidth - 12.0f), Math.max(0.0f, rowHeight - 6.0f),
                        Paint.fill(OVERFLOW_MENU_TEXT));
            } finally {
                draw.popClip();
            }
        }
        } finally {
            draw.popClip();
        }
        if (hit.node().panes().size() > visibleRows) {
            float trackX = x + Math.max(0.0f, menuWidth - 5.0f);
            float trackHeight = Math.max(0.0f, menu.height() - 4.0f);
            float thumbHeight = Math.max(10.0f, trackHeight * visibleRows / Math.max(1, hit.node().panes().size()));
            float maxScroll = Math.max(1, hit.node().panes().size() - visibleRows);
            float thumbY = y + 2.0f + (trackHeight - thumbHeight) * (first / maxScroll);
            draw.rect(trackX, y + 2.0f, 2.0f, trackHeight, Paint.fill(OVERFLOW_MENU_SELECTED));
            draw.rect(trackX, thumbY, 2.0f, thumbHeight, Paint.fill(OVERFLOW_MENU_BORDER));
        }
    }

    private DockPane tabAt(DockNode node, RectView bounds, float rootX, float rootY) {
        if (node == null) return null;
        if (node.isLeaf()) {
            if (rootY < bounds.y() || rootY > bounds.y() + Math.min(tabHeight, bounds.height())) return null;
            List<DockTabState> tabs = tabStates(node, bounds);
            for (int i = 0; i < tabs.size(); i++) {
                DockTabState tab = tabs.get(i);
                if (rootX >= tab.x() && rootX <= tab.x() + tab.width()
                        && rootY >= tab.y() && rootY <= tab.y() + tab.height()) {
                    return node.panes().get(i);
                }
            }
            return null;
        }
        SplitRects split = splitRects(bounds, node.orientation(), node.splitRatio());
        DockPane first = tabAt(node.first(), split.first(), rootX, rootY);
        return first != null ? first : tabAt(node.second(), split.second(), rootX, rootY);
    }

    private List<DockPaneState> paneStates(DockNode node, RectView bounds) {
        List<DockPaneState> result = new ArrayList<>();
        collectPaneStates(node, bounds, result);
        return result;
    }

    private void collectPaneStates(DockNode node, RectView bounds, List<DockPaneState> result) {
        if (node == null) return;
        if (node.isLeaf()) {
            result.add(new DockPaneState(
                    bounds.x(), bounds.y(), bounds.width(), bounds.height(),
                    Math.min(tabHeight, bounds.height()), tabStates(node, bounds), node.selectedIndex(),
                    firstVisibleTab(node, bounds), lastVisibleTab(node, bounds), tabsOverflow(node, bounds),
                    overflowButtonBounds(bounds).x(), overflowButtonBounds(bounds).y(),
                    overflowButtonBounds(bounds).width(), overflowButtonBounds(bounds).height(),
                    node.id().equals(openOverflowNodeId)));
            return;
        }
        SplitRects split = splitRects(bounds, node.orientation(), node.splitRatio());
        collectPaneStates(node.first(), split.first(), result);
        collectPaneStates(node.second(), split.second(), result);
    }

    private List<DockSplitHandleState> splitStates(DockNode node, RectView bounds) {
        List<DockSplitHandleState> result = new ArrayList<>();
        collectSplitStates(node, bounds, result);
        return result;
    }

    private void collectSplitStates(DockNode node, RectView bounds, List<DockSplitHandleState> result) {
        if (node == null || node.isLeaf()) return;
        SplitRects split = splitRects(bounds, node.orientation(), node.splitRatio());
        boolean hovered = node.id().equals(hoveredSplitNodeId);
        boolean pressed = node.id().equals(pressedSplitNodeId);
        result.add(new DockSplitHandleState(
                split.handle().x(), split.handle().y(), split.handle().width(), split.handle().height(),
                node.orientation(), hovered, pressed));
        collectSplitStates(node.first(), split.first(), result);
        collectSplitStates(node.second(), split.second(), result);
    }

    private List<DockTabState> tabStates(DockNode node, RectView bounds) {
        List<DockTabState> tabs = new ArrayList<>();
        if (node == null || !node.isLeaf() || node.panes().isEmpty()) return tabs;
        float y = bounds.y();
        float height = Math.min(tabHeight, bounds.height());
        boolean overflow = tabsOverflow(node, bounds);
        float availableWidth = Math.max(0.0f, bounds.width() - (overflow ? OVERFLOW_BUTTON_WIDTH : 0.0f));
        int visibleCapacity = visibleTabCapacity(node, bounds);
        int firstVisible = overflow ? normalizedTabScrollOffset(node, bounds) : 0;
        int lastVisible = overflow ? Math.min(node.panes().size() - 1, firstVisible + visibleCapacity - 1) : node.panes().size() - 1;
        float maxTabWidth = overflow
                ? Math.max(MIN_TAB_WIDTH, Math.min(MAX_TAB_WIDTH, availableWidth / Math.max(1, visibleCapacity)))
                : Math.max(MIN_TAB_WIDTH, Math.min(MAX_TAB_WIDTH, bounds.width() / Math.max(1, node.panes().size())));
        float x = bounds.x();
        for (int i = 0; i < node.panes().size(); i++) {
            DockPane pane = node.panes().get(i);
            boolean visible = !overflow || (i >= firstVisible && i <= lastVisible);
            float remaining = Math.max(0.0f, bounds.x() + bounds.width() - x);
            float width = visible ? Math.min(maxTabWidth, remaining) : 0.0f;
            if (!visible || remaining <= 0.0f || width < 12.0f) {
                width = 0.0f;
            }
            tabs.add(new DockTabState(
                    pane.id(), pane.richTitle(),
                    x, y, Math.max(0.0f, width), height,
                    i == node.selectedIndex(), pane.closable(),
                    pane.kind(), pane.dirty(), pane.pinned(), pane.autoHide(),
                    pane.id().equals(activePaneId()),
                    pane.id().equals(hoveredPaneId),
                    pane.id().equals(pressedPaneId),
                    pane.id().equals(dragController.paneId()) && dragController.dragging()));
            if (visible) x += width;
        }
        return tabs;
    }

    private int firstVisibleTab(DockNode node, RectView bounds) {
        if (node == null || !node.isLeaf() || node.panes().isEmpty()) return 0;
        return tabsOverflow(node, bounds) ? normalizedTabScrollOffset(node, bounds) : 0;
    }

    private int lastVisibleTab(DockNode node, RectView bounds) {
        if (node == null || !node.isLeaf() || node.panes().isEmpty()) return -1;
        if (!tabsOverflow(node, bounds)) return node.panes().size() - 1;
        return Math.min(node.panes().size() - 1, normalizedTabScrollOffset(node, bounds) + visibleTabCapacity(node, bounds) - 1);
    }

    private boolean tabsOverflow(DockNode node, RectView bounds) {
        return node != null
                && node.isLeaf()
                && node.panes().size() > visibleTabCapacity(node, bounds);
    }

    private int visibleTabCapacity(DockNode node, RectView bounds) {
        if (node == null || !node.isLeaf() || node.panes().isEmpty() || bounds == null) return 0;
        float availableWidth = Math.max(0.0f, bounds.width());
        int fullCapacity = Math.max(1, (int) Math.floor(availableWidth / MIN_TAB_WIDTH));
        if (node.panes().size() <= fullCapacity) return node.panes().size();
        float overflowAvailableWidth = Math.max(0.0f, availableWidth - OVERFLOW_BUTTON_WIDTH);
        return Math.max(1, (int) Math.floor(overflowAvailableWidth / MIN_TAB_WIDTH));
    }

    private int normalizedTabScrollOffset(DockNode node, RectView bounds) {
        int max = maxTabScrollOffset(node, bounds);
        int current = tabScrollOffsets.getOrDefault(node.id(), 0);
        int normalized = Math.max(0, Math.min(max, current));
        if (normalized != current) {
            tabScrollOffsets.put(node.id(), normalized);
        }
        return normalized;
    }

    private int maxTabScrollOffset(DockNode node, RectView bounds) {
        if (node == null || !node.isLeaf()) return 0;
        return Math.max(0, node.panes().size() - visibleTabCapacity(node, bounds));
    }

    private MutableRect overflowButtonBounds(RectView bounds) {
        float height = Math.min(tabHeight, bounds == null ? 0.0f : bounds.height());
        float width = Math.min(OVERFLOW_BUTTON_WIDTH, bounds == null ? 0.0f : bounds.width());
        return new MutableRect(
                bounds == null ? 0.0f : bounds.x() + Math.max(0.0f, bounds.width() - width),
                bounds == null ? 0.0f : bounds.y(),
                Math.max(0.0f, width),
                Math.max(0.0f, height));
    }

    private boolean scrollTabsAt(float rootX, float rootY, float deltaX, float deltaY) {
        LeafHit hit = tabStripAt(rootNode(), layoutBounds(), rootX, rootY);
        if (hit == null || !tabsOverflow(hit.node(), hit.bounds())) return false;
        float delta = Math.abs(deltaY) >= Math.abs(deltaX) ? deltaY : deltaX;
        if (delta == 0.0f) return false;
        int direction = delta < 0.0f ? 1 : -1;
        int current = normalizedTabScrollOffset(hit.node(), hit.bounds());
        int next = Math.max(0, Math.min(maxTabScrollOffset(hit.node(), hit.bounds()), current + direction));
        if (next == current) return true;
        tabScrollOffsets.put(hit.node().id(), next);
        openOverflowNodeId = "";
        setHoveredPane("");
        invalidate(InvalidationFlags.VISUAL);
        return true;
    }

    private boolean scrollOverflowMenuAt(float rootX, float rootY, float deltaX, float deltaY) {
        if (openOverflowNodeId.isEmpty()) return false;
        LeafHit hit = leafById(rootNode(), layoutBounds(), openOverflowNodeId);
        if (hit == null || !tabsOverflow(hit.node(), hit.bounds())) return false;
        MutableRect menu = overflowMenuBounds(hit);
        if (!contains(menu, rootX, rootY)) return false;
        float delta = Math.abs(deltaY) >= Math.abs(deltaX) ? deltaY : deltaX;
        if (delta == 0.0f) return true;
        int direction = delta < 0.0f ? 1 : -1;
        int current = normalizedOverflowMenuScrollOffset(hit.node());
        int next = Math.max(0, Math.min(maxOverflowMenuScrollOffset(hit.node()), current + direction));
        if (next != current) {
            overflowMenuScrollOffsets.put(hit.node().id(), next);
            invalidate(InvalidationFlags.VISUAL);
        }
        return true;
    }

    private void revealPane(String paneId) {
        LeafHit ref = leafContainingPane(rootNode(), layoutBounds(), paneId);
        if (ref != null) revealPane(ref.node(), paneId);
    }

    private void revealPane(DockNode node, String paneId) {
        if (node == null || paneId == null || paneId.isEmpty()) return;
        int index = -1;
        for (int i = 0; i < node.panes().size(); i++) {
            if (node.panes().get(i).id().equals(paneId)) {
                index = i;
                break;
            }
        }
        if (index < 0) return;
        int capacity = visibleTabCapacity(node, layoutBounds());
        if (capacity <= 0 || node.panes().size() <= capacity) {
            tabScrollOffsets.put(node.id(), 0);
            return;
        }
        int first = normalizedTabScrollOffset(node, layoutBounds());
        int last = first + capacity - 1;
        if (index < first) {
            tabScrollOffsets.put(node.id(), index);
        } else if (index > last) {
            tabScrollOffsets.put(node.id(), Math.max(0, index - capacity + 1));
        }
    }

    private LeafHit overflowButtonAt(DockNode node, RectView bounds, float rootX, float rootY) {
        if (node == null || bounds == null || !contains(bounds, rootX, rootY)) return null;
        if (node.isLeaf()) {
            if (!tabsOverflow(node, bounds)) return null;
            MutableRect button = overflowButtonBounds(bounds);
            return contains(button, rootX, rootY) ? new LeafHit(node, button) : null;
        }
        SplitRects split = splitRects(bounds, node.orientation(), node.splitRatio());
        LeafHit first = overflowButtonAt(node.first(), split.first(), rootX, rootY);
        return first != null ? first : overflowButtonAt(node.second(), split.second(), rootX, rootY);
    }

    private LeafHit tabStripAt(DockNode node, RectView bounds, float rootX, float rootY) {
        if (node == null || bounds == null || !contains(bounds, rootX, rootY)) return null;
        if (node.isLeaf()) {
            float tabStripHeight = Math.min(tabHeight, bounds.height());
            if (rootY < bounds.y() || rootY > bounds.y() + tabStripHeight) return null;
            return new LeafHit(node, new MutableRect(bounds.x(), bounds.y(), bounds.width(), tabStripHeight));
        }
        SplitRects split = splitRects(bounds, node.orientation(), node.splitRatio());
        LeafHit first = tabStripAt(node.first(), split.first(), rootX, rootY);
        return first != null ? first : tabStripAt(node.second(), split.second(), rootX, rootY);
    }

    private LeafHit leafById(DockNode node, RectView bounds, String nodeId) {
        if (node == null || bounds == null || nodeId == null || nodeId.isEmpty()) return null;
        if (node.id().equals(nodeId)) {
            return new LeafHit(node, new MutableRect(bounds.x(), bounds.y(), bounds.width(), bounds.height()));
        }
        if (node.isLeaf()) return null;
        SplitRects split = splitRects(bounds, node.orientation(), node.splitRatio());
        LeafHit first = leafById(node.first(), split.first(), nodeId);
        return first != null ? first : leafById(node.second(), split.second(), nodeId);
    }

    private LeafHit leafContainingPane(DockNode node, RectView bounds, String paneId) {
        if (node == null || bounds == null || paneId == null || paneId.isEmpty()) return null;
        if (node.isLeaf()) {
            return node.containsPane(paneId)
                    ? new LeafHit(node, new MutableRect(bounds.x(), bounds.y(), bounds.width(), bounds.height()))
                    : null;
        }
        SplitRects split = splitRects(bounds, node.orientation(), node.splitRatio());
        LeafHit first = leafContainingPane(node.first(), split.first(), paneId);
        return first != null ? first : leafContainingPane(node.second(), split.second(), paneId);
    }

    private OverflowMenuItem overflowMenuItemAt(float rootX, float rootY) {
        if (openOverflowNodeId.isEmpty()) return null;
        LeafHit hit = leafById(rootNode(), layoutBounds(), openOverflowNodeId);
        if (hit == null || !tabsOverflow(hit.node(), hit.bounds())) return null;
        MutableRect menu = overflowMenuBounds(hit);
        if (!contains(menu, rootX, rootY)) return null;
        int index = normalizedOverflowMenuScrollOffset(hit.node())
                + (int) Math.floor((rootY - menu.y()) / Math.max(16.0f, OVERFLOW_MENU_ROW_HEIGHT));
        if (index < 0 || index >= hit.node().panes().size()) return null;
        return new OverflowMenuItem(hit.node(), hit.node().panes().get(index), index);
    }

    private MutableRect overflowMenuBounds(LeafHit hit) {
        MutableRect button = overflowButtonBounds(hit.bounds());
        float rowHeight = Math.max(16.0f, OVERFLOW_MENU_ROW_HEIGHT);
        float menuWidth = Math.min(OVERFLOW_MENU_WIDTH, Math.max(72.0f, layoutBounds().width()));
        float menuHeight = rowHeight * overflowMenuVisibleRows(hit.node());
        float x = Math.min(button.x(), layoutBounds().x() + Math.max(0.0f, layoutBounds().width() - menuWidth));
        float y = Math.min(button.y() + button.height(),
                layoutBounds().y() + Math.max(0.0f, layoutBounds().height() - menuHeight));
        return new MutableRect(x, y, menuWidth, menuHeight);
    }

    private int overflowMenuVisibleRows(DockNode node) {
        if (node == null || !node.isLeaf() || node.panes().isEmpty()) return 0;
        return Math.max(1, Math.min(OVERFLOW_MENU_MAX_VISIBLE_ROWS, node.panes().size()));
    }

    private int normalizedOverflowMenuScrollOffset(DockNode node) {
        if (node == null) return 0;
        int max = maxOverflowMenuScrollOffset(node);
        int current = overflowMenuScrollOffsets.getOrDefault(node.id(), 0);
        int normalized = Math.max(0, Math.min(max, current));
        if (normalized != current) {
            overflowMenuScrollOffsets.put(node.id(), normalized);
        }
        return normalized;
    }

    private int maxOverflowMenuScrollOffset(DockNode node) {
        if (node == null || !node.isLeaf()) return 0;
        return Math.max(0, node.panes().size() - overflowMenuVisibleRows(node));
    }

    private void revealOverflowMenuSelection(DockNode node) {
        if (node == null || !node.isLeaf()) return;
        int selected = Math.max(0, node.selectedIndex());
        int first = normalizedOverflowMenuScrollOffset(node);
        int visibleRows = overflowMenuVisibleRows(node);
        int last = first + visibleRows - 1;
        if (selected < first) {
            overflowMenuScrollOffsets.put(node.id(), selected);
        } else if (selected > last) {
            overflowMenuScrollOffsets.put(node.id(), Math.max(0, selected - visibleRows + 1));
        }
    }

    private SplitRects splitRects(RectView bounds, DockSplitOrientation orientation, float ratio) {
        float thickness = Math.min(splitHandleThickness,
                Math.max(0.0f, orientation == DockSplitOrientation.HORIZONTAL ? bounds.width() : bounds.height()));
        float available = Math.max(0.0f,
                (orientation == DockSplitOrientation.HORIZONTAL ? bounds.width() : bounds.height()) - thickness);
        float firstSize = available * Math.max(0.0f, Math.min(1.0f, ratio));
        float secondSize = Math.max(0.0f, available - firstSize);
        if (orientation == DockSplitOrientation.HORIZONTAL) {
            return new SplitRects(
                    new MutableRect(bounds.x(), bounds.y(), firstSize, bounds.height()),
                    new MutableRect(bounds.x() + firstSize, bounds.y(), thickness, bounds.height()),
                    new MutableRect(bounds.x() + firstSize + thickness, bounds.y(), secondSize, bounds.height()));
        }
        return new SplitRects(
                new MutableRect(bounds.x(), bounds.y(), bounds.width(), firstSize),
                new MutableRect(bounds.x(), bounds.y() + firstSize, bounds.width(), thickness),
                new MutableRect(bounds.x(), bounds.y() + firstSize + thickness, bounds.width(), secondSize));
    }

    private DockingRootState rootState() {
        return new DockingRootState(
                layoutBounds().x(), layoutBounds().y(), layoutBounds().width(), layoutBounds().height(),
                manager.empty(),
                backgroundVisible(), background().copy(), radius(),
                borderVisible(), borderColor().copy(), borderWidth(),
                dragController.dragging(), dragController.previewIntent().valid());
    }

    private DockingRootRenderer effectiveRootRenderer() {
        return rootRenderer == null ? styleRenderer(DockingRootRenderer.class, WidgetsRender.dockingRoot()) : rootRenderer;
    }

    private DockPaneRenderer effectivePaneRenderer() {
        return paneRenderer == null ? WidgetsRender.dockPane() : paneRenderer;
    }

    private DockSplitHandleRenderer effectiveSplitHandleRenderer() {
        return splitHandleRenderer == null ? WidgetsRender.dockSplitHandle() : splitHandleRenderer;
    }

    private DockDropPreviewRenderer effectiveDropPreviewRenderer() {
        return dropPreviewRenderer == null ? WidgetsRender.dockDropPreview() : dropPreviewRenderer;
    }

    private String activePaneId() {
        DockPane active = manager.selectedPane();
        return active == null ? "" : active.id();
    }

    private static float finiteOr(float value, float fallback) {
        return Float.isFinite(value) && value > 0.0f ? value : fallback;
    }

    private record SplitRects(MutableRect first, MutableRect handle, MutableRect second) {
    }

    private record LeafHit(DockNode node, MutableRect bounds) {
    }

    private record OverflowMenuItem(DockNode node, DockPane pane, int index) {
    }

    private record SplitNodeHit(DockNode node, MutableRect nodeBounds, MutableRect handleBounds,
                                DockSplitOrientation orientation) {
    }

    // -- Split-drag helpers ----------------------------------------------------

    /**
     * Returns the split node whose handle intersects the given root-space point,
     * or {@code null} if no handle is hit.
     */
    private SplitNodeHit splitHandleAt(DockNode node, RectView bounds, float rootX, float rootY) {
        if (node == null || node.isLeaf() || bounds == null) return null;
        if (!contains(bounds, rootX, rootY)) return null;
        SplitRects split = splitRects(bounds, node.orientation(), node.splitRatio());
        if (contains(split.handle(), rootX, rootY)) {
            return new SplitNodeHit(node,
                    new MutableRect(bounds.x(), bounds.y(), bounds.width(), bounds.height()),
                    new MutableRect(split.handle().x(), split.handle().y(),
                            split.handle().width(), split.handle().height()),
                    node.orientation());
        }
        SplitNodeHit first = splitHandleAt(node.first(), split.first(), rootX, rootY);
        return first != null ? first : splitHandleAt(node.second(), split.second(), rootX, rootY);
    }

    /**
     * Finds the split node with the given id anywhere in the tree.
     */
    private DockNode findSplitNodeById(DockNode node, String id) {
        if (node == null || id == null || id.isEmpty()) return null;
        if (node.id().equals(id) && node.isSplit()) return node;
        if (node.isLeaf()) return null;
        DockNode found = findSplitNodeById(node.first(), id);
        return found != null ? found : findSplitNodeById(node.second(), id);
    }

    /**
     * Computes the new split ratio during a drag, clamped to [0.05, 0.95].
     */
    private float computeNewSplitRatio(float currentRootX, float currentRootY) {
        float delta = splitDragOrientation == DockSplitOrientation.HORIZONTAL
                ? currentRootX - splitDragStartX
                : currentRootY - splitDragStartY;
        float newRatio = splitDragStartRatio + delta / splitDragAvailable;
        return Math.max(0.05f, Math.min(0.95f, newRatio));
    }
}
