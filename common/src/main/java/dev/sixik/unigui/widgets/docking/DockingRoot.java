package dev.sixik.unigui.widgets.docking;

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
import dev.sixik.unigui.api.xml.XmlAttribute;
import dev.sixik.unigui.api.xml.XmlWidgetName;
import dev.sixik.unigui.impl.widget.WidgetBase;
import dev.sixik.unigui.impl.text.TextEngine;
import dev.sixik.unigui.api.widget.render.WidgetRender;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import dev.sixik.unigui.widgets.containers.Box;
import dev.sixik.unigui.widgets.containers.DockPanel;
import dev.sixik.unigui.widgets.feedback.OverlayLayer;
import dev.sixik.unigui.widgets.feedback.WindowWidget;

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
@XmlWidgetName("DockingRoot")
public final class DockingRoot extends Box {
    public static final String STYLE_TYPE = dev.sixik.unigui.api.style.StyleIds.Widget.DOCKING_ROOT;

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
    private static final MutableColor PANE_BORDER = new MutableColor(0.24f, 0.28f, 0.36f, 0.95f);
    private static final MutableColor TAB_BACKGROUND = new MutableColor(0.075f, 0.085f, 0.115f, 0.96f);
    private static final MutableColor TAB_SELECTED = new MutableColor(0.12f, 0.16f, 0.23f, 0.98f);
    private static final MutableColor TAB_DOCUMENT = new MutableColor(0.10f, 0.14f, 0.21f, 0.98f);
    private static final MutableColor TAB_TOOL = new MutableColor(0.085f, 0.095f, 0.120f, 0.98f);
    private static final MutableColor TAB_BORDER = new MutableColor(0.26f, 0.32f, 0.42f, 0.95f);
    private static final MutableColor TAB_TEXT = new MutableColor(0.88f, 0.93f, 1.0f, 1.0f);
    private static final MutableColor TAB_TEXT_MUTED = new MutableColor(0.62f, 0.68f, 0.78f, 0.88f);
    private static final MutableColor TAB_DIRTY = new MutableColor(1.0f, 0.66f, 0.24f, 1.0f);
    private static final MutableColor TAB_OVERFLOW = new MutableColor(0.28f, 0.78f, 1.0f, 0.85f);
    private static final MutableColor TAB_HOVER = new MutableColor(0.18f, 0.24f, 0.34f, 0.96f);
    private static final MutableColor TAB_PRESSED = new MutableColor(0.08f, 0.11f, 0.17f, 0.98f);
    private static final MutableColor TAB_DRAGGING = new MutableColor(0.25f, 0.78f, 1.0f, 0.92f);
    private static final MutableColor SPLIT_TRACK = new MutableColor(0.055f, 0.062f, 0.080f, 0.98f);
    private static final MutableColor SPLIT_TRACK_HOVER = new MutableColor(0.10f, 0.13f, 0.18f, 0.98f);
    private static final MutableColor SPLIT_TRACK_PRESS = new MutableColor(0.14f, 0.18f, 0.26f, 0.98f);
    private static final MutableColor SPLIT_LINE = new MutableColor(0.25f, 0.78f, 1.0f, 0.42f);
    private static final MutableColor SPLIT_LINE_HOVER = new MutableColor(0.35f, 0.88f, 1.0f, 0.75f);
    private static final MutableColor SPLIT_LINE_PRESS = new MutableColor(0.45f, 0.95f, 1.0f, 1.00f);
    private static final MutableColor DROP_FILL = new MutableColor(0.18f, 0.56f, 1.0f, 0.22f);
    private static final MutableColor DROP_STROKE = new MutableColor(0.28f, 0.78f, 1.0f, 0.92f);
    /** Зазор между ячейками креста drop-зон. */
    private static final float DROP_ZONE_GAP = 6.0f;
    private static final MutableColor ZONE_FILL = new MutableColor(0.20f, 0.32f, 0.52f, 0.55f);
    private static final MutableColor ZONE_FILL_ACTIVE = new MutableColor(0.25f, 0.55f, 1.0f, 0.80f);
    private static final MutableColor ZONE_STROKE = new MutableColor(0.35f, 0.55f, 0.85f, 0.70f);
    private static final MutableColor EMPTY_TEXT = new MutableColor(0.62f, 0.68f, 0.78f, 0.8f);
    private static final MutableColor SEAM = new MutableColor(0.22f, 0.26f, 0.36f, 0.70f);
    private static final MutableColor SEAM_HOVER = new MutableColor(0.35f, 0.88f, 1.0f, 0.90f);
    private static final MutableColor SEAM_PRESS = new MutableColor(0.50f, 0.98f, 1.0f, 1.00f);
    private static final MutableColor SEAM_GLOW = new MutableColor(0.25f, 0.78f, 1.0f, 0.18f);
    private static final MutableColor SEAM_GLOW_PRESS = new MutableColor(0.30f, 0.85f, 1.0f, 0.30f);

    /** Визуальный стиль разделителя панелей. */
    public enum SplitHandleStyle {
        /** Блок с центральной линией. */
        BLOCK,
        /** Общая кромка панелей: тонкий шов без фонового блока. */
        SEAM
    }

    private final DockingManager manager = new DockingManager(this);
    private final DockDragController dragController = new DockDragController(this);
    private final Set<Widget> registeredContents = Collections.newSetFromMap(new IdentityHashMap<>());
    private WidgetRender paneRenderer;
    private WidgetRender splitHandleRenderer;
    private WidgetRender dropPreviewRenderer;
    private SplitHandleStyle splitHandleStyle = SplitHandleStyle.BLOCK;
    private WindowWidget lastFloatingWindow;
    private final List<WindowWidget> floatingDockWindows = new ObjectArrayList<>();
    private String hoveredPaneId = "";
    private String pressedPaneId = "";
    private String openOverflowNodeId = "";
    private int hoveredOverflowMenuIndex = -1;
    private final Map<String, Integer> tabScrollOffsets = new HashMap<>();
    private final Map<String, Integer> overflowMenuScrollOffsets = new HashMap<>();
    private DockDropIntent floatingDropPreviewIntent = DockDropIntent.none();
    /** Последний размер floating-окна по id панели: tear-off возвращает его, а не размер листа. */
    private final Map<String, float[]> floatingWindowSizes = new HashMap<>();
    private static final Map<WindowWidget, DockingRoot> FOREIGN_WINDOW_PREVIEW_TARGETS =
            Collections.synchronizedMap(new WeakHashMap<>());
    private float tabHeight = 22.0f;
    private float splitHandleThickness = 5.0f;
    private boolean hasArrangedBounds;
    private boolean allowFloatingOutsideHost;
    private boolean tearOffOnDragOut = true;
    private boolean floatingWindowsRedockLocked;
    private WindowWidget windowHost;

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

    /**
     * Окно-хост, внутрь которого встроен этот root, или {@code null} для
     * обычных корневых docking area.
     */
    public WindowWidget windowHost() {
        return windowHost;
    }

    /**
     * Привязывает root к окну-хосту. Вызывается из {@link WindowWidget}
     * при включении внутренней docking area.
     */
    public void windowHost(WindowWidget windowHost) {
        this.windowHost = windowHost;
    }

    /**
     * Принимает ли root dock drops прямо сейчас.
     *
     * <p>Корневые area принимают всегда. Root внутри окна принимает, только
     * пока у окна включён флаг {@code dockingEnabled}: после выключения
     * панели остаются на месте, но новые дропы блокируются.</p>
     */
    public boolean acceptsDockDrops() {
        return windowHost == null || windowHost.dockingEnabled();
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

    @XmlAttribute(value = "allowFloatingOutsideHost", category = "Behavior", defaultValue = "false", description = "Whether floating dock windows may move outside the root host bounds.")
    public DockingRoot allowFloatingOutsideHost(boolean allowFloatingOutsideHost) {
        if (this.allowFloatingOutsideHost == allowFloatingOutsideHost) return this;
        this.allowFloatingOutsideHost = allowFloatingOutsideHost;
        return this;
    }

    /**
     * Возвращает, вырывается ли панель плавающим окном при дропе мимо зон.
     *
     * @return {@code true} для tear-off поведения (default)
     */
    public boolean tearOffOnDragOut() {
        return tearOffOnDragOut;
    }

    /**
     * Включает или выключает tear-off: дроп перетаскиваемой панели мимо всех
     * зон превращает её в плавающее окно вместо snap-back на место.
     *
     * <p>Срабатывает только после реального драга (порог пройден) и только если
     * отпустили вне исходного листа. Окно при создании клампится в хост, так
     * что вынос за край даёт окно у края, а не потерю панели.</p>
     *
     * @param tearOffOnDragOut {@code true}, чтобы разрешить отрыв панелей
     * @return этот root для fluent-настройки
     */
    @XmlAttribute(value = "tearOffOnDragOut", category = "Behavior", defaultValue = "true", description = "Whether dropping a dragged pane outside all zones tears it off into a floating window.")
    public DockingRoot tearOffOnDragOut(boolean tearOffOnDragOut) {
        if (this.tearOffOnDragOut == tearOffOnDragOut) return this;
        this.tearOffOnDragOut = tearOffOnDragOut;
        return this;
    }

    public boolean floatingWindowsRedockLocked() {
        return floatingWindowsRedockLocked;
    }

    @XmlAttribute(value = "floatingWindowsRedockLocked", category = "Behavior", defaultValue = "false", description = "Whether floating dock windows are prevented from redocking.")
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

    @XmlAttribute(value = "tabHeight", category = "Layout", defaultValue = "22", description = "Dock tab strip height in UI pixels.")
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

    @XmlAttribute(value = "splitHandleThickness", category = "Layout", defaultValue = "5", description = "Dock split handle thickness in UI pixels.")
    public DockingRoot splitHandleThickness(float splitHandleThickness) {
        float normalized = Float.isFinite(splitHandleThickness) ? Math.max(1.0f, splitHandleThickness) : 5.0f;
        if (this.splitHandleThickness == normalized) return this;
        this.splitHandleThickness = normalized;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public SplitHandleStyle splitHandleStyle() {
        return splitHandleStyle;
    }

    @XmlAttribute(value = "splitHandleStyle", category = "Appearance", defaultValue = "block", description = "Split handle visual style: block with a centre line or shared seam line.")
    public DockingRoot splitHandleStyle(SplitHandleStyle splitHandleStyle) {
        SplitHandleStyle normalized = splitHandleStyle == null ? SplitHandleStyle.BLOCK : splitHandleStyle;
        if (this.splitHandleStyle == normalized) return this;
        this.splitHandleStyle = normalized;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public WidgetRender paneRenderer() {
        return paneRenderer;
    }

    public DockingRoot paneRenderer(WidgetRender paneRenderer) {
        if (this.paneRenderer == paneRenderer) return this;
        this.paneRenderer = paneRenderer;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public WidgetRender splitHandleRenderer() {
        return splitHandleRenderer;
    }

    public DockingRoot splitHandleRenderer(WidgetRender splitHandleRenderer) {
        if (this.splitHandleRenderer == splitHandleRenderer) return this;
        this.splitHandleRenderer = splitHandleRenderer;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public WidgetRender dropPreviewRenderer() {
        return dropPreviewRenderer;
    }

    public DockingRoot dropPreviewRenderer(WidgetRender dropPreviewRenderer) {
        if (this.dropPreviewRenderer == dropPreviewRenderer) return this;
        this.dropPreviewRenderer = dropPreviewRenderer;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public DockingRoot useDefaultDockingRenderers() {
        renderer(null);
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
            DrawScope draw = new DrawScope(context, transform(), layoutBounds());
            if (!renderCustomVisual(draw)) {
                renderDefaultRoot(draw);
            }
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
            if (dragController.dragging()) {
                reorderTabAtPointer(pointer);
                tearOffTabAtPointer(pointer);
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

    /**
     * Live-reorder табов: наведение на соседний таб того же листа ставит
     * тащимую панель на его место.
     *
     * <p>Работает только внутри домашнего листа (таб под курсором и тащимая
     * панель в одном листе): кросс-листовые перемещения — через обычный дроп.
     * Повторные вызовы на том же табе — no-op, layout не дёргается.</p>
     *
     * @param pointer событие движения указателя
     */
    private void reorderTabAtPointer(PointerMovedEvent pointer) {
        String draggedId = dragController.paneId();
        DockPane hoveredTab = tabAt(rootNode(), layoutBounds(), pointer.rootX(), pointer.rootY());
        if (hoveredTab == null || hoveredTab.id().equals(draggedId)) return;
        LeafHit hoveredLeaf = leafContainingPane(rootNode(), layoutBounds(), hoveredTab.id());
        LeafHit draggedLeaf = leafContainingPane(rootNode(), layoutBounds(), draggedId);
        if (hoveredLeaf == null || draggedLeaf == null || hoveredLeaf.node() != draggedLeaf.node()) return;
        manager.reorderPane(draggedId, hoveredTab.id());
    }

    /**
     * Live tear-off таба: выход указателя из таб-стрипов отрывает панель
     * плавающим окном прямо посреди жеста.
     *
     * <p>Окно создаётся под курсором (хедером под указатель), драг
     * продолжается им: capture и drag-состояние передаются окну, дальше жест
     * идёт штатным window-путём (триггеры, превью, дроп). Уважается
     * {@code tearOffOnDragOut}; везёт только пока панель ещё в дереве.
     * Выход на чужой стрип — тоже отрыв: кросс-листовый дроп идёт окном
     * через зону. Единственная панель не вырывается заживо: пустой root
     * вместо дока никому не нужен, release-путь вернёт её snap-back'ом.</p>
     *
     * @param pointer событие движения указателя
     */
    private void tearOffTabAtPointer(PointerMovedEvent pointer) {
        if (!tearOffOnDragOut) return;
        if (manager.paneCount() <= 1) return;
        String paneId = dragController.paneId();
        int pointerId = pointer.pointerId();
        float rootX = pointer.rootX();
        float rootY = pointer.rootY();
        LeafHit source = leafContainingPane(rootNode(), layoutBounds(), paneId);
        if (source == null) return;
        // Выход из ЛЮБОГО таб-стрипа, не только домашнего: переход на чужой
        // стрип — это тоже отрыв (кросс-листовый дроп идёт окном через зону).
        if (tabStripAt(rootNode(), layoutBounds(), rootX, rootY) != null) return;
        DockPane detached = manager.detachPaneForFloating(paneId);
        if (detached == null) return;
        float[] remembered = rememberedFloatingSize(paneId);
        float winWidth = remembered != null
                ? remembered[0]
                : clamp(source.bounds().width(), 180.0f, 640.0f);
        float winHeight = remembered != null
                ? remembered[1]
                : clamp(source.bounds().height(), 112.0f, 480.0f);
        float grabDX = Math.min(Math.max(16.0f, winWidth * 0.5f), 96.0f);
        WindowWidget window = createFloatingPaneWindow(detached, DockDropIntent.of(paneId, "", DockArea.FLOAT,
                new MutableRect(rootX - grabDX, rootY - 11.0f, winWidth, winHeight), rootX, rootY));
        lastFloatingWindow = window;
        dragController.handoff(pointerId, rootX, rootY);
        OverlayLayer overlay = nearestOverlayLayer(this);
        if (overlay != null) {
            overlay.applyQueuedMutations();
        }
        RectView host = overlay != null ? overlay.layoutBounds() : floatingHostBounds();
        window.arrangeInHost(host);
        UIContext context = uiContext();
        if (context != null) {
            context.releasePointer(pointerId, this);
            context.capturePointer(pointerId, window);
        }
        window.takeoverDrag(pointerId, rootX, rootY);
        setPressedPane("");
        setHoveredPane("");
        invalidate(InvalidationFlags.VISUAL);
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
        // Крест зон следует за курсором даже внутри одной зоны: инвалидируем
        // каждый move, иначе подсветка отстаёт от указателя.
        invalidate(InvalidationFlags.VISUAL);
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
        TriggerHit trigger = triggerAt(rootX, rootY);
        if (!trigger.valid()) {
            return DockDropIntent.none();
        }
        return leafIntent(sourcePaneId, trigger.leaf(), trigger.area(), rootX, rootY);
    }

    /**
     * Строит intent дропа на лист: таб в центр, иначе сплит.
     *
     * @param sourcePaneId id перетаскиваемой панели
     * @param hit лист под курсором
     * @param area зона внутри листа
     * @param rootX X курсора
     * @param rootY Y курсора
     * @return intent или {@code none} для дропа панели самой на себя
     */
    private DockDropIntent leafIntent(String sourcePaneId, LeafHit hit, DockArea area, float rootX, float rootY) {
        DockPane target = hit.node().selectedPane();
        if (target == null) target = hit.node().panes().get(0);
        if (sourcePaneId != null && sourcePaneId.equals(target.id()) && hit.node().panes().size() == 1) {
            return DockDropIntent.none();
        }
        return DockDropIntent.of(sourcePaneId, target.id(), area,
                previewBounds(hit.bounds(), area), rootX, rootY);
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

    /**
     * Отрывает панель плавающим окном при дропе мимо всех зон (tear-off).
     *
     * <p>Вызывается из {@code DockDragController.end} как fallback, когда
     * intent пуст: панель уже убрана с места драгом, возвращать её молча —
     * значит терять жест пользователя. Условия: флаг включён, root принимает
     * дропы, панель ещё в дереве и отпустили вне её исходного листа.
     * Позиция окна клампится в хост через {@code createFloatingPaneWindow},
     * поэтому {@code allowFloatingOutsideHost} продолжает управлять только
     * свободным движением, но не самим отрывом.</p>
     *
     * @param paneId id перетаскиваемой панели
     * @param rootX X отпускания
     * @param rootY Y отпускания
     * @return {@code true}, если панель стала плавающим окном
     */
    boolean tryTearOff(String paneId, float rootX, float rootY) {
        if (!tearOffOnDragOut || !acceptsDockDrops() || paneId == null || paneId.isEmpty()) return false;
        LeafHit source = leafContainingPane(rootNode(), layoutBounds(), paneId);
        if (source == null) return false;
        if (contains(source.bounds(), rootX, rootY)) return false;
        DockPane detached = manager.detachPaneForFloating(paneId);
        if (detached == null) return false;
        lastFloatingWindow = createFloatingPaneWindow(detached, DockDropIntent.floating(paneId, rootX, rootY));
        return lastFloatingWindow != null;
    }

    private WindowWidget createFloatingPaneWindow(DockPane pane, DockDropIntent intent) {
        WindowWidget window = new WindowWidget(pane.richTitle(), pane.content());
        float[] remembered = rememberedFloatingSize(pane.id());
        float width = remembered != null ? remembered[0] : Math.max(180.0f, intent.width());
        float height = remembered != null ? remembered[1] : Math.max(112.0f, intent.height());
        window.layout(style -> style.size(width, height));
        window.minWindowSize(120.0f, 64.0f);
        window.closeButtonVisible(false);
        window.closeOnOutsideClick(false);
        window.constrainToHost(true);
        window.resizable(true);
        // Floating-панель — dockable-источник своего же id: после tear-off
        // окно докируется обратно штатным foreign-путём (превью на move,
        // merge на release). Лок redock уважается там же.
        window.dockable(true);
        window.dockPaneId(pane.id());
        window.dockKind(pane.kind());
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

    /**
     * Возвращает запомненный размер floating-окна панели или {@code null}.
     *
     * <p>Размер пишется в момент дока ({@code rememberFloatingSize}): повторный
     * tear-off возвращает окну прошлый размер, а не размер листа, из которого
     * его вытянули.</p>
     *
     * @param paneId id панели
     * @return массив {@code [width, height]} или {@code null}, если панель ещё не плавала
     */
    private float[] rememberedFloatingSize(String paneId) {
        float[] size = paneId == null ? null : floatingWindowSizes.get(paneId);
        if (size == null) return null;
        float width = clamp(size[0], 120.0f, 640.0f);
        float height = clamp(size[1], 64.0f, 480.0f);
        if (!Float.isFinite(width) || !Float.isFinite(height)) return null;
        return new float[]{width, height};
    }

    /**
     * Запоминает текущий размер floating-окна за id панели.
     *
     * @param paneId id панели
     * @param window окно, чей размер запоминаем
     */
    private void rememberFloatingSize(String paneId, WindowWidget window) {
        if (paneId == null || paneId.isEmpty() || window == null) return;
        float width = window.layoutBounds().width();
        float height = window.layoutBounds().height();
        if (!Float.isFinite(width) || !Float.isFinite(height) || width <= 0.0f || height <= 0.0f) return;
        floatingWindowSizes.put(paneId, new float[]{width, height});
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
            // Превью строго по мыши, а не по прямоугольнику окна: иначе оффер
            // и дроп расходятся, когда окно отстоит от курсора на offset захвата.
            float dropX = window.lastDragRootX();
            float dropY = window.lastDragRootY();
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
            float dropX = window.lastDragRootX();
            float dropY = window.lastDragRootY();
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

    private void redockFloatingPane(WindowWidget window, DockPane pane, OverlayLayer overlay,
            DockingRoot target, float rootX, float rootY) {
        rememberFloatingSize(pane.id(), window);
        target.floatingDropPreview(DockDropIntent.none());
        // Вне триггеров окно остаётся плавать: релиз мимо креста/стрипа
        // не должен прибивать панель табом в чужой лист (иначе вытянутое
        // окно при отпускании тут же прилипает обратно). Пустой root
        // принимает дроп anywhere — целиться там не во что.
        if (!target.manager().empty() && !target.triggerAt(rootX, rootY).valid()) {
            return;
        }
        window.content(null);
        window.applyQueuedMutations();
        window.close();
        overlay.removeOverlay(window);

        // Резолв строго по мыши (rootX/rootY — указатель на момент релиза),
        // тем же trigger-пайплайном, что и превью: дроп совпадает с оффером.
        // Триггер уже проверен выше, fallback ниже — только страховка от
        // гонки layout между проверкой и вставкой: панель нельзя потерять.
        boolean merged = target.insertMergedPane(pane, target.anchorLeafForMerge(rootX, rootY), rootX, rootY);
        if (!merged) {
            target.manager().addPane(pane);
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

    private static Widget rootOf(Widget widget) {
        Widget current = widget;
        Widget top = widget;
        while (current != null) {
            top = current;
            current = current.parent();
        }
        return top;
    }

    /**
     * Показывает drop preview для чужого dockable-окна, которое тащат над docking root.
     *
     * <p>Вызывается из {@link WindowWidget} во время drag, когда у окна включён
     * флаг {@code dockable}. Окно не обязано принадлежать docking-дереву:
     * достаточно общего overlay-предка. При выключенном флаге, локе или
     * отсутствии цели preview молча гасится.</p>
     *
     * @param window окно-источник в состоянии drag
     * @param rootX X указателя в root-координатах
     * @param rootY Y указателя в root-координатах
     */
    public static void previewForeignWindowDrop(WindowWidget window, float rootX, float rootY) {
        if (window == null || !window.dockable() || window.dockRedockLocked()) {
            clearForeignWindowDropPreview(window);
            return;
        }
        OverlayLayer overlay = nearestOverlayLayer(window);
        Widget searchRoot = overlay != null ? overlay : rootOf(window);
        WindowWidget hostWindow = windowHostAt(searchRoot, window, rootX, rootY);
        DockingRoot target = hostWindow != null && hostWindow.dockingRoot() != null
                ? hostWindow.dockingRoot()
                : dockingRootAt(searchRoot, window, rootX, rootY);
        if (target != null && !target.acceptsDockDrops()) target = null;
        DockingRoot previous = FOREIGN_WINDOW_PREVIEW_TARGETS.get(window);
        if (previous != null && previous != target) {
            previous.floatingDropPreview(DockDropIntent.none());
        }
        if (target == null) {
            FOREIGN_WINDOW_PREVIEW_TARGETS.remove(window);
            return;
        }
        FOREIGN_WINDOW_PREVIEW_TARGETS.put(window, target);
        String sourcePaneId = window.dockingRoot() != null && !window.dockingRoot().manager().panes().isEmpty()
                ? window.dockingRoot().manager().panes().get(0).id()
                : window.dockSourceId();
        target.floatingDropPreview(target.resolveDockDropIntent(sourcePaneId, rootX, rootY));
    }

    /**
     * Завершает drop чужого dockable-окна: гасит preview и докирует контент.
     *
     * <p>Если точка дропа попадает на другое окно с включённой docking area,
     * панели источника мержатся в окно-хост, а источник закрывается. Иначе
     * контент докируется в найденный {@link DockingRoot} как одна панель.</p>
     *
     * @param window окно-источник в конце drag
     * @param rootX X указателя в root-координатах
     * @param rootY Y указателя в root-координатах
     * @return {@code true}, если контент окна стал dock-панелью
     */
    public static boolean finishForeignWindowDrop(WindowWidget window, float rootX, float rootY) {
        DockingRoot previous = FOREIGN_WINDOW_PREVIEW_TARGETS.remove(window);
        if (previous != null) {
            previous.floatingDropPreview(DockDropIntent.none());
        }
        if (window == null || !window.dockable() || window.dockRedockLocked()) return false;
        OverlayLayer overlay = nearestOverlayLayer(window);
        Widget searchRoot = overlay != null ? overlay : rootOf(window);
        WindowWidget hostWindow = windowHostAt(searchRoot, window, rootX, rootY);
        if (hostWindow != null && hostWindow.mergeDockingFrom(window)) {
            detachForeignWindow(window);
            return true;
        }
        DockingRoot target = dockingRootAt(searchRoot, window, rootX, rootY);
        if (target == null) return false;
        return target.dockForeignWindow(window, rootX, rootY);
    }

    /**
     * Гасит drop preview чужого окна, если оно сейчас подсвечено на каком-либо root.
     *
     * @param window окно-источник или {@code null}
     */
    public static void clearForeignWindowDropPreview(WindowWidget window) {
        if (window == null) return;
        DockingRoot previous = FOREIGN_WINDOW_PREVIEW_TARGETS.remove(window);
        if (previous != null) {
            previous.floatingDropPreview(DockDropIntent.none());
        }
    }

    /**
     * Докирует контент чужого {@link WindowWidget} как новую панель этого root.
     *
     * <p>Если источник — окно-хост с панелями, все его панели мержатся в этот
     * root табом, а источник закрывается. Иначе контент окна отсоединяется как
     * одна панель с id {@code dockSourceId()} и вставляется по drop-зоне под
     * точкой {@code rootX/rootY}: центр — табом, края — сплитом.</p>
     *
     * @param window окно-источник с включённым флагом {@code dockable}
     * @param rootX X дропа в root-координатах
     * @param rootY Y дропа в root-координатах
     * @return {@code true}, если панель добавлена в дерево
     */
    public boolean dockForeignWindow(WindowWidget window, float rootX, float rootY) {
        if (window == null || !window.dockable() || window.dockRedockLocked()) return false;
        if (!acceptsDockDrops()) return false;
        floatingDropPreview(DockDropIntent.none());
        // Вне триггеров окно остаётся плавать (симметрично redockFloatingPane):
        // иначе релиз мимо креста/стрипа молча табал бы контент в preferred
        // leaf. Пустой root принимает дроп anywhere.
        if (!manager.empty() && !triggerAt(rootX, rootY).valid()) {
            return false;
        }

        DockingRoot sourceRoot = window.dockingRoot();
        if (sourceRoot != null) {
            java.util.List<DockPane> panes = java.util.List.copyOf(sourceRoot.manager().panes());
            if (!panes.isEmpty()) {
                LeafHit anchorHit = anchorLeafForMerge(rootX, rootY);
                String firstId = "";
                for (DockPane pane : panes) {
                    rememberFloatingSize(pane.id(), window);
                    DockPane detached = sourceRoot.manager().detachPaneForFloating(pane.id());
                    if (detached == null) continue;
                    if (firstId.isEmpty()) {
                        if (!insertMergedPane(detached, anchorHit, rootX, rootY)) {
                            manager.addPane(detached);
                        }
                        firstId = detached.id();
                    } else {
                        // Последующие панели мержатся табом к первой перенесённой.
                        manager.tabPane(firstId, detached);
                    }
                }
                detachForeignWindow(window);
                if (lastFloatingWindow == window) {
                    lastFloatingWindow = null;
                }
                return true;
            }
        }

        Widget content = window.content();
        if (content == null || content == window.dockingRoot()) {
            // Окно-хост без панелей: нечего докировать.
            if (sourceRoot != null) detachForeignWindow(window);
            return sourceRoot != null;
        }
        String paneId = window.dockSourceId();
        rememberFloatingSize(paneId, window);
        DockPane pane = window.dockKind() == DockPaneKind.DOCUMENT
                ? DockPane.document(paneId, window.richTitle(), content)
                : DockPane.tool(paneId, window.richTitle(), content);
        pane.closable(window.closeButtonVisible());

        window.content(null);
        window.applyQueuedMutations();
        detachForeignWindow(window);
        if (!insertMergedPane(pane, anchorLeafForMerge(rootX, rootY), rootX, rootY)) {
            manager.addPane(pane);
        }
        if (lastFloatingWindow == window) {
            lastFloatingWindow = null;
        }
        return true;
    }

    private LeafHit anchorLeafForMerge(float rootX, float rootY) {
        return leafAt(rootNode(), layoutBounds(), rootX, rootY);
    }

    private boolean insertMergedPane(DockPane pane, LeafHit hit, float rootX, float rootY) {
        if (hit == null || hit.node().panes().isEmpty()) {
            manager.addPane(pane);
            return true;
        }
        TriggerHit trigger = triggerAt(rootX, rootY);
        if (!trigger.valid()) {
            return false;
        }
        DockPane targetPane = trigger.leaf().node().selectedPane();
        if (targetPane == null) targetPane = trigger.leaf().node().panes().get(0);
        DockArea area = trigger.area();
        if (area == DockArea.CENTER || area == DockArea.TAB) {
            manager.tabPane(targetPane.id(), pane);
        } else {
            manager.splitPane(targetPane.id(), area, pane);
        }
        return true;
    }

    private static void detachForeignWindow(WindowWidget window) {
        if (window == null) return;
        window.content(null);
        window.applyQueuedMutations();
        OverlayLayer overlay = nearestOverlayLayer(window);
        if (overlay != null) {
            overlay.removeOverlay(window);
        }
        window.close();
    }

    private static DockingRoot dockingRootAt(Widget root, Widget ignored, float rootX, float rootY) {
        if (root == null) return null;
        WindowWidget hostWindow = windowHostAt(root, ignored, rootX, rootY);
        if (hostWindow != null && hostWindow.dockingRoot() != null
                && hostWindow.dockingRoot().acceptsDockDrops()) {
            return hostWindow.dockingRoot();
        }
        if (root == ignored || root.visibility() != Visibility.VISIBLE) return null;
        if (!contains(root.layoutBounds(), rootX, rootY)) return null;
        List<Widget> children = root.children();
        for (int i = children.size() - 1; i >= 0; i--) {
            DockingRoot childHit = dockingRootAt(children.get(i), ignored, rootX, rootY);
            if (childHit != null) return childHit;
        }
        DockingRoot self = root instanceof DockingRoot dockingRoot ? dockingRoot : null;
        return self != null && self.acceptsDockDrops() ? self : null;
    }

    /**
     * Ищет окно с включённой docking area под точкой дропа.
     *
     * <p>Окно-источник исключается: окно нельзя дропнуть само на себя.
     * Хост с выключенным флагом или локом redock не подходит.</p>
     */
    private static WindowWidget windowHostAt(Widget root, Widget ignored, float rootX, float rootY) {
        if (root == null || root.visibility() != Visibility.VISIBLE) return null;
        List<Widget> children = root.children();
        for (int i = children.size() - 1; i >= 0; i--) {
            Widget child = children.get(i);
            if (child == ignored) continue;
            WindowWidget nested = windowHostAt(child, ignored, rootX, rootY);
            if (nested != null) return nested;
        }
        if (root == ignored) return null;
        if (root instanceof WindowWidget window
                && window.dockingEnabled()
                && !window.dockRedockLocked()
                && window.opened()
                && contains(window.layoutBounds(), rootX, rootY)) {
            return window;
        }
        return null;
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

    /**
     * Результат trigger-резолва: видимый триггер под курсором.
     *
     * @param leaf лист триггера
     * @param area зона триггера
     */
    private record TriggerHit(LeafHit leaf, DockArea area) {
        private static TriggerHit none() {
            return new TriggerHit(null, null);
        }

        private boolean valid() {
            return area != null;
        }
    }

    private static final DockArea[] CROSS_AREAS = {
            DockArea.CENTER, DockArea.TOP, DockArea.BOTTOM, DockArea.LEFT, DockArea.RIGHT
    };

    /**
     * Находит видимый триггер под курсором мыши.
     *
     * <p>Единственная точка истины для всех drop-путей (intent, merge, redock):
     * таб-стрип → ячейка креста листа. Квадрантной fallback-геометрии нет:
     * оффер существует ровно там, где нарисован триггер. Вне триггеров —
     * мимо зон (tear-off/float/addPane по контексту).</p>
     *
     * @param rootX X курсора в root-координатах
     * @param rootY Y курсора в root-координатах
     * @return триггер или {@code none()}
     */
    private TriggerHit triggerAt(float rootX, float rootY) {
        RectView rootBounds = layoutBounds();
        LeafHit tabHit = tabStripAt(rootNode(), rootBounds, rootX, rootY);
        if (tabHit != null && !tabHit.node().panes().isEmpty()) {
            return new TriggerHit(tabHit, DockArea.CENTER);
        }
        LeafHit hit = leafAt(rootNode(), rootBounds, rootX, rootY);
        if (hit != null && !hit.node().panes().isEmpty()) {
            for (DockArea area : CROSS_AREAS) {
                if (contains(crossCellRect(hit.bounds(), area), rootX, rootY)) {
                    return new TriggerHit(hit, area);
                }
            }
        }
        return TriggerHit.none();
    }

    /**
     * Возвращает rect ячейки креста drop-зон для листа.
     *
     * <p>Та же геометрия используется рендером adornments и хит-тестом:
     * нарисованная ячейка и есть зона дропа — расхождений «показали одно,
     * приняли другое» быть не может. Ячейки не пересекаются.</p>
     *
     * @param leafBounds границы листа
     * @param area зона: TOP/BOTTOM/LEFT/RIGHT — стороны креста, CENTER/TAB — центр
     * @return rect ячейки; для прочих зон — пустой rect
     */
    public static MutableRect crossCellRect(RectView leafBounds, DockArea area) {
        if (leafBounds == null) return new MutableRect();
        float cell = dropZoneCell(leafBounds);
        float gap = DROP_ZONE_GAP;
        float centerX = leafBounds.x() + leafBounds.width() * 0.5f;
        float centerY = leafBounds.y() + leafBounds.height() * 0.5f;
        float half = cell * 0.5f;
        if (area == DockArea.TOP) {
            return new MutableRect(centerX - half, centerY - half - gap - cell, cell, cell);
        }
        if (area == DockArea.BOTTOM) {
            return new MutableRect(centerX - half, centerY + half + gap, cell, cell);
        }
        if (area == DockArea.LEFT) {
            return new MutableRect(centerX - half - gap - cell, centerY - half, cell, cell);
        }
        if (area == DockArea.RIGHT) {
            return new MutableRect(centerX + half + gap, centerY - half, cell, cell);
        }
        if (area == DockArea.CENTER || area == DockArea.TAB) {
            return new MutableRect(centerX - half, centerY - half, cell, cell);
        }
        return new MutableRect();
    }

    /**
     * Возвращает размер ячейки креста drop-зон для листа.
     *
     * <p>Та же геометрия используется хит-тестом ({@code triggerAt}) и рендером
     * ({@code renderDropCross}): что нарисовано, туда и дропается.</p>
     *
     * @param bounds границы листа
     * @return сторона квадратной ячейки креста
     */
    private static float dropZoneCell(RectView bounds) {
        float minSide = Math.min(Math.max(1.0f, bounds.width()), Math.max(1.0f, bounds.height()));
        return Math.max(40.0f, Math.min(64.0f, minSide * 0.25f));
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
        List<Widget> stale = new ObjectArrayList<>();
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
                    renderChildWithInheritedTransform(context, selected.content());
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
        DrawScope draw = new DrawScope(context, transform(), layoutBounds());
        WidgetRender pane = effectivePaneRenderer();
        if (pane != null) {
            pane.render(draw, this);
        } else {
            renderDefaultPanes(draw, rootNode(), layoutBounds());
        }
        WidgetRender split = effectiveSplitHandleRenderer();
        if (split != null) {
            split.render(draw, this);
        } else {
            renderDefaultSplits(draw, rootNode(), layoutBounds());
        }
        // Custom preview renderers are only invoked for a valid preview,
        // matching the old conditional call contract.
        DockDropIntent preview = dropPreviewIntent();
        if (preview.valid()) {
            WidgetRender drop = effectiveDropPreviewRenderer();
            if (drop != null) {
                drop.render(draw, this);
            } else {
                renderDefaultDropPreview(draw, preview);
            }
        }
        renderDefaultDropZones(draw);
    }

    private void renderDefaultRoot(DrawScope draw) {
        float x = layoutBounds().x();
        float y = layoutBounds().y();
        float width = layoutBounds().width();
        float height = layoutBounds().height();
        if (width <= 0.0f || height <= 0.0f) return;
        if (backgroundVisible()) {
            draw.roundedRect(x, y, width, height, radius(),
                    Paint.fill(background()));
        }
        if (borderVisible() && borderWidth() > 0.0f) {
            draw.roundedRect(x, y, width, height, radius(),
                    Paint.stroke(borderColor(), borderWidth()));
        }
        if (dragController.dragging() || dropPreviewIntent().valid()) {
            draw.roundedRect(x + 1.0f, y + 1.0f,
                    Math.max(0.0f, width - 2.0f), Math.max(0.0f, height - 2.0f),
                    Math.max(0.0f, radius() - 1.0f), Paint.stroke(EMPTY_TEXT, 1.0f));
        }
        if (manager.empty()) {
            draw.text("Drop or add a DockPane", x + 8.0f, y + 8.0f,
                    Math.max(0.0f, width - 16.0f), 14.0f, Paint.fill(EMPTY_TEXT));
        }
    }

    private void renderDefaultPanes(DrawScope draw, DockNode node, RectView bounds) {
        if (node == null || bounds == null) return;
        if (node.isLeaf()) {
            renderDefaultPane(draw, node, bounds);
            return;
        }
        SplitRects split = splitRects(bounds, node.orientation(), node.splitRatio());
        renderDefaultPanes(draw, node.first(), split.first());
        renderDefaultPanes(draw, node.second(), split.second());
    }

    private void renderDefaultPane(DrawScope draw, DockNode node, RectView bounds) {
        float width = bounds.width();
        float height = bounds.height();
        if (width <= 0.0f || height <= 0.0f) return;
        float stripHeight = Math.min(tabHeight, height);
        draw.rect(bounds.x(), bounds.y(), width, height, Paint.stroke(PANE_BORDER, 1.0f));
        draw.rect(bounds.x(), bounds.y(), width, stripHeight, Paint.fill(TAB_BACKGROUND));

        int selectedIndex = node.selectedIndex();
        String activeId = activePaneId();
        for (TabLayout tab : tabLayouts(node, bounds)) {
            if (tab.width() <= 0.0f || tab.height() <= 0.0f) continue;
            DockPane pane = tab.pane();
            boolean selected = tab.index() == selectedIndex;
            boolean hovered = pane.id().equals(hoveredPaneId);
            boolean pressed = pane.id().equals(pressedPaneId);
            boolean dragging = pane.id().equals(dragController.paneId()) && dragController.dragging();
            boolean active = pane.id().equals(activeId);
            MutableColor base = pressed
                    ? TAB_PRESSED
                    : hovered
                    ? TAB_HOVER
                    : selected
                    ? TAB_SELECTED
                    : pane.kind() == DockPaneKind.DOCUMENT ? TAB_DOCUMENT : TAB_TOOL;
            draw.roundedRect(tab.x(), tab.y(), tab.width(), tab.height() + 2.0f, 3.0f,
                    Paint.fill(base));
            draw.line(tab.x(), tab.y() + tab.height(),
                    tab.x() + tab.width(), tab.y() + tab.height(),
                    Paint.stroke(dragging ? TAB_DRAGGING : active ? TAB_TEXT : TAB_BORDER,
                            dragging ? 2.0f : active ? 1.5f : 1.0f));
            if (pane.dirty()) {
                draw.circle(tab.x() + Math.max(7.0f, tab.width() - 12.0f),
                        tab.y() + Math.max(6.0f, tab.height() * 0.5f - 2.0f),
                        4.0f, 4.0f, Paint.fill(TAB_DIRTY));
            }
            if (pane.autoHide()) {
                draw.line(tab.x() + 4.0f, tab.y() + 3.0f,
                        tab.x() + 4.0f, tab.y() + Math.max(4.0f, tab.height() - 4.0f),
                        Paint.stroke(TAB_OVERFLOW, 1.0f));
            }
            draw.pushTextClip(tab.x() + 6.0f, tab.y(), Math.max(0.0f, tab.width() - 12.0f), tab.height());
            try {
                TextEngine.drawInline(draw, pane.richTitle(), tab.x() + 6.0f, tab.y() + 4.0f,
                        Math.max(0.0f, tab.width() - 12.0f), Math.max(0.0f, tab.height() - 6.0f),
                        Paint.fill(selected ? TAB_TEXT : TAB_TEXT_MUTED));
            } finally {
                draw.popClip();
            }
        }
        if (tabsOverflow(node, bounds)) {
            MutableRect button = overflowButtonBounds(bounds);
            float bx = button.x();
            float by = button.y();
            float bw = button.width();
            float bh = button.height();
            boolean menuOpen = node.id().equals(openOverflowNodeId);
            draw.roundedRect(bx + 2.0f, by + 2.0f, Math.max(0.0f, bw - 4.0f), Math.max(0.0f, bh - 4.0f), 3.0f,
                    Paint.fill(menuOpen ? TAB_HOVER : TAB_BACKGROUND));
            draw.rect(bx + 2.0f, by + 2.0f, Math.max(0.0f, bw - 4.0f), Math.max(0.0f, bh - 4.0f),
                    Paint.stroke(TAB_OVERFLOW, 1.0f));
            float cx = bx + bw * 0.5f;
            float cy = by + bh * 0.5f;
            draw.circle(cx - 5.0f, cy, 1.6f, 1.6f, Paint.fill(TAB_OVERFLOW));
            draw.circle(cx, cy, 1.6f, 1.6f, Paint.fill(TAB_OVERFLOW));
            draw.circle(cx + 5.0f, cy, 1.6f, 1.6f, Paint.fill(TAB_OVERFLOW));
        }
    }

    private void renderDefaultSplits(DrawScope draw, DockNode node, RectView bounds) {
        if (node == null || node.isLeaf() || bounds == null) return;
        SplitRects split = splitRects(bounds, node.orientation(), node.splitRatio());
        renderDefaultSplitHandle(draw,
                split.handle().x(), split.handle().y(), split.handle().width(), split.handle().height(),
                node.orientation(),
                node.id().equals(hoveredSplitNodeId),
                node.id().equals(pressedSplitNodeId));
        renderDefaultSplits(draw, node.first(), split.first());
        renderDefaultSplits(draw, node.second(), split.second());
    }

    private void renderDefaultSplitHandle(DrawScope draw, float x, float y, float width, float height,
                                                DockSplitOrientation orientation, boolean hovered, boolean pressed) {
        if (width <= 0.0f || height <= 0.0f) return;
        if (splitHandleStyle == SplitHandleStyle.SEAM) {
            renderSeamSplitHandle(draw, x, y, width, height, orientation, hovered, pressed);
            return;
        }
        MutableColor track = pressed ? SPLIT_TRACK_PRESS : hovered ? SPLIT_TRACK_HOVER : SPLIT_TRACK;
        MutableColor line = pressed ? SPLIT_LINE_PRESS : hovered ? SPLIT_LINE_HOVER : SPLIT_LINE;
        draw.rect(x, y, width, height, Paint.fill(track));
        if (orientation == DockSplitOrientation.HORIZONTAL) {
            float cx = x + width * 0.5f;
            draw.line(cx, y + 4.0f, cx, y + Math.max(4.0f, height - 4.0f),
                    Paint.stroke(line, pressed ? 2.0f : 1.0f));
        } else {
            float cy = y + height * 0.5f;
            draw.line(x + 4.0f, cy, x + Math.max(4.0f, width - 4.0f), cy,
                    Paint.stroke(line, pressed ? 2.0f : 1.0f));
        }
    }

    /**
     * Рисует разделитель общей кромкой: тонкий шов без фонового блока.
     *
     * <p>При наведении поверх зоны handle рисуется полупрозрачная подсветка,
     * во время drag шов превращается в яркую линию.</p>
     */
    private static void renderSeamSplitHandle(DrawScope draw, float x, float y, float width, float height,
                                              DockSplitOrientation orientation, boolean hovered, boolean pressed) {
        if (hovered || pressed) {
            draw.rect(x, y, width, height, Paint.fill(pressed ? SEAM_GLOW_PRESS : SEAM_GLOW));
        }
        MutableColor seam = pressed ? SEAM_PRESS : hovered ? SEAM_HOVER : SEAM;
        float strokeWidth = pressed ? 2.0f : 1.0f;
        if (orientation == DockSplitOrientation.HORIZONTAL) {
            float cx = x + width * 0.5f;
            draw.line(cx, y, cx, y + height, Paint.stroke(seam, strokeWidth));
        } else {
            float cy = y + height * 0.5f;
            draw.line(x, cy, x + width, cy, Paint.stroke(seam, strokeWidth));
        }
    }

    private static void renderDefaultDropPreview(DrawScope draw, DockDropIntent preview) {
        if (preview == null || !preview.valid() || preview.width() <= 0.0f || preview.height() <= 0.0f) return;
        draw.roundedRect(preview.x(), preview.y(), preview.width(), preview.height(), 4.0f, Paint.fill(DROP_FILL));
        draw.roundedRect(preview.x(), preview.y(), preview.width(), preview.height(), 4.0f, Paint.stroke(DROP_STROKE, 1.5f));
    }

    /**
     * Рисует drop-зоны во время драга: крест над целевым листом.
     *
     * <p>Геометрия 1-в-1 с хит-тестом ({@code crossCellRect}): что нарисовано,
     * туда и дропается. Активная зона подсвечена акцентом по текущему intent,
     * остальные тусклые. Крест якорится на целевой лист intent'а (или лист
     * под курсором, если intent пуст).</p>
     *
     * @param draw draw scope
     */
    private void renderDefaultDropZones(DrawScope draw) {
        boolean dragging = dragController.dragging();
        DockDropIntent intent = dropPreviewIntent();
        if (!dragging && !intent.valid()) return;
        RectView rootBounds = layoutBounds();
        if (rootBounds.width() <= 0.0f || rootBounds.height() <= 0.0f) return;

        DockNode anchorNode = null;
        RectView anchorBounds = null;
        if (!intent.targetPaneId().isEmpty()) {
            LeafHit hit = leafContainingPane(rootNode(), rootBounds, intent.targetPaneId());
            if (hit != null && !hit.node().panes().isEmpty()) {
                anchorNode = hit.node();
                anchorBounds = hit.bounds();
            }
        } else if (dragging) {
            LeafHit hit = leafAt(rootNode(), rootBounds,
                    dragController.lastRootX(), dragController.lastRootY());
            if (hit != null && !hit.node().panes().isEmpty()) {
                anchorNode = hit.node();
                anchorBounds = hit.bounds();
            }
        } else if (intent.valid()) {
            // Floating-окно нависло над root, но триггер не задет: крест
            // якорится на лист под указателем по координатам intent.
            LeafHit hit = leafAt(rootNode(), rootBounds, intent.pointerX(), intent.pointerY());
            if (hit != null && !hit.node().panes().isEmpty()) {
                anchorNode = hit.node();
                anchorBounds = hit.bounds();
            }
        }
        if (anchorBounds != null) {
            DockArea active = intent.valid()
                    && !intent.targetPaneId().isEmpty()
                    && anchorContainsPane(anchorNode, intent.targetPaneId())
                    ? intent.area()
                    : null;
            renderDropCross(draw, anchorBounds, active);
        }
    }

    private static boolean anchorContainsPane(DockNode node, String paneId) {
        if (node == null || paneId == null || paneId.isEmpty()) return false;
        for (DockPane pane : node.panes()) {
            if (pane != null && paneId.equals(pane.id())) return true;
        }
        return false;
    }

    private static void renderDropCross(DrawScope draw, RectView bounds, DockArea active) {
        float cell = dropZoneCell(bounds);
        MutableRect center = crossCellRect(bounds, DockArea.CENTER);
        MutableRect top = crossCellRect(bounds, DockArea.TOP);
        renderZoneCell(draw, center.x(), center.y(), cell, active == DockArea.CENTER || active == DockArea.TAB);
        renderZoneCell(draw, top.x(), top.y(), cell, active == DockArea.TOP);
        MutableRect bottom = crossCellRect(bounds, DockArea.BOTTOM);
        renderZoneCell(draw, bottom.x(), bottom.y(), cell, active == DockArea.BOTTOM);
        MutableRect left = crossCellRect(bounds, DockArea.LEFT);
        renderZoneCell(draw, left.x(), left.y(), cell, active == DockArea.LEFT);
        MutableRect right = crossCellRect(bounds, DockArea.RIGHT);
        renderZoneCell(draw, right.x(), right.y(), cell, active == DockArea.RIGHT);
        // Метафоры: верхняя ячейка — таб-стрип (плашка сверху), центр — слои,
        // боковые ячейки — стрелки к краям сплита.
        float centerX = bounds.x() + bounds.width() * 0.5f;
        float centerY = bounds.y() + bounds.height() * 0.5f;
        float half = cell * 0.5f;
        float gap = DROP_ZONE_GAP;
        float barMargin = Math.max(6.0f, cell * 0.18f);
        draw.roundedRectFilled(centerX - half + barMargin, centerY - half - gap - cell + barMargin,
                Math.max(0.0f, cell - barMargin * 2.0f), Math.max(2.0f, cell * 0.16f), 2.0f, TAB_TEXT);
        for (int line = -1; line <= 1; line++) {
            float ly = centerY + line * Math.max(4.0f, cell * 0.14f);
            float lx = Math.max(4.0f, cell * 0.22f);
            draw.line(centerX - lx, ly, centerX + lx, ly, Paint.stroke(TAB_TEXT, 1.5f));
        }
        float stride = cell + gap;
        float arrowLen = Math.max(6.0f, cell * 0.26f);
        float arrowHalf = Math.max(3.0f, cell * 0.11f);
        renderZoneArrow(draw, centerX, centerY - stride, 0.0f, -1.0f, arrowLen, arrowHalf);
        renderZoneArrow(draw, centerX, centerY + stride, 0.0f, 1.0f, arrowLen, arrowHalf);
        renderZoneArrow(draw, centerX - stride, centerY, -1.0f, 0.0f, arrowLen, arrowHalf);
        renderZoneArrow(draw, centerX + stride, centerY, 1.0f, 0.0f, arrowLen, arrowHalf);
    }

    private static void renderZoneArrow(DrawScope draw, float cx, float cy,
            float dirX, float dirY, float length, float halfWidth) {
        float tipX = cx + dirX * length;
        float tipY = cy + dirY * length;
        float baseX = cx - dirX * length * 0.4f;
        float baseY = cy - dirY * length * 0.4f;
        float perpX = -dirY;
        float perpY = dirX;
        Paint paint = Paint.stroke(TAB_TEXT, 1.5f);
        draw.line(baseX, baseY, tipX, tipY, paint);
        draw.line(tipX, tipY,
                tipX - dirX * halfWidth * 1.6f + perpX * halfWidth, tipY - dirY * halfWidth * 1.6f + perpY * halfWidth,
                paint);
        draw.line(tipX, tipY,
                tipX - dirX * halfWidth * 1.6f - perpX * halfWidth, tipY - dirY * halfWidth * 1.6f - perpY * halfWidth,
                paint);
    }

    private static void renderZoneCell(DrawScope draw, float x, float y, float cell, boolean active) {
        draw.roundedRectFilled(x, y, cell, cell, 8.0f, active ? ZONE_FILL_ACTIVE : ZONE_FILL);
        draw.roundedRectStroke(x, y, cell, cell, 8.0f, active ? DROP_STROKE : ZONE_STROKE, active ? 2.0f : 1.0f);
    }

    private DockDropIntent dropPreviewIntent() {
        DockDropIntent preview = dragController.previewIntent().valid()
                ? dragController.previewIntent()
                : floatingDropPreviewIntent;
        return preview == null ? DockDropIntent.none() : preview;
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
        DrawScope draw = new DrawScope(context, transform(), layoutBounds());
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
            draw.pushTextClip(x + 6.0f, rowY, Math.max(0.0f, menuWidth - 12.0f), rowHeight);
            try {
                TextEngine.drawInline(draw, pane.richTitle(), x + 6.0f, rowY + 4.0f,
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
            for (TabLayout tab : tabLayouts(node, bounds)) {
                if (rootX >= tab.x() && rootX <= tab.x() + tab.width()
                        && rootY >= tab.y() && rootY <= tab.y() + tab.height()) {
                    return tab.pane();
                }
            }
            return null;
        }
        SplitRects split = splitRects(bounds, node.orientation(), node.splitRatio());
        DockPane first = tabAt(node.first(), split.first(), rootX, rootY);
        return first != null ? first : tabAt(node.second(), split.second(), rootX, rootY);
    }

    private List<TabLayout> tabLayouts(DockNode node, RectView bounds) {
        List<TabLayout> tabs = new ObjectArrayList<>();
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
            tabs.add(new TabLayout(i, pane, x, y, Math.max(0.0f, width), height));
            if (visible) x += width;
        }
        return tabs;
    }

    private record TabLayout(int index, DockPane pane, float x, float y, float width, float height) {
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

    private WidgetRender effectivePaneRenderer() {
        return paneRenderer;
    }

    private WidgetRender effectiveSplitHandleRenderer() {
        return splitHandleRenderer;
    }

    private WidgetRender effectiveDropPreviewRenderer() {
        return dropPreviewRenderer;
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
