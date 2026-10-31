package dev.sixik.unigui.widgets.dnd;

import dev.sixik.unigui.api.core.UIContext;
import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiLink;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;
import dev.sixik.unigui.api.input.HitTestResult;
import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.impl.widget.WidgetBase;
import dev.sixik.unigui.widgets.containers.ScrollView;
import dev.sixik.unigui.widgets.core.Orientation;
import dev.sixik.unigui.widgets.feedback.OverlayLayer;

import java.util.Objects;

/**
 * Живое состояние одного drag-жеста: payload, позиция указателя, активная drop-зона,
 * индикатор вставки и ghost.
 *
 * <p>Сессию создаёт {@link DragController} в момент, когда указатель прошёл порог. Drop-зоны
 * ({@link DropController}, {@link ReorderController}) читают её из событий и могут показать
 * индикатор через {@link #showIndicator(float, float, float, float, ColorView)}.</p>
 *
 * <p>Каждый кадр и каждое движение указателя сессия:</p>
 * <ol>
 *   <li>прокручивает ближайший {@link ScrollView}, если указатель у его края;</li>
 *   <li>зондирует виджет под указателем событием {@link DragOverEvent} и находит зону-приёмник;</li>
 *   <li>отправляет {@link DragLeaveEvent} зоне, которую указатель покинул.</li>
 * </ol>
 */
@AiDoc(
        description = "Live state and controller of an active drag gesture, managing pointer coordinates, active drop target, ghost positioning, insertion indicators, and autoscrolling."
)
@WikiPage(
        title = "DragSession",
        slug = "drag-session",
        category = "<key:dnd.category>",
        summary = "<key:drag.session.summary>"
)
@WikiSection(
        title = "<key:wiki.section.overview.title>",
        markdown = "<key:drag.session.section.overview.body>",
        order = 10
)
@WikiExample(
        title = "<key:drag.session.example.indicator.title>",
        code = "session.showIndicator(x, y, width, 2.0f, MutableColor.rgba(0.2f, 0.6f, 1.0f, 1.0f));"
)
@WikiLink(
        label = "DragController",
        target = "page:drag-controller",
        description = "<key:drag.controller.summary>"
)
@WikiLink(
        label = "DropController",
        target = "page:drop-controller",
        description = "<key:drop.controller.summary>"
)
public final class DragSession {
    private static final float MIN_AUTO_SCROLL_RAMP = 0.12f;

    private final DragController controller;
    private final Widget handle;
    private final Widget dragged;
    private final DragPayload payload;
    private final DragGhost ghost;
    private final UIContext context;
    private final Widget root;
    private final float pressX;
    private final float pressY;
    private final MutableColor indicatorColor = new MutableColor(0.30f, 0.60f, 1.0f, 1.0f);

    private DragOverlay overlay;
    private OverlayLayer overlayHost;
    private Widget dropTarget;
    private Widget probedTarget;
    private Widget lastHit;
    private float pointerX;
    private float pointerY;
    private boolean active;
    private boolean indicatorVisible;
    private float indicatorX;
    private float indicatorY;
    private float indicatorWidth;
    private float indicatorHeight;
    private float restoredOpacity = 1.0f;
    private boolean dimmed;
    private Widget ghostWidget;
    private float initialGhostX;
    private float initialGhostY;

    DragSession(DragController controller, Widget handle, Widget dragged, DragPayload payload,
                DragGhost ghost, float pressX, float pressY) {
        this.controller = Objects.requireNonNull(controller, "controller");
        this.handle = Objects.requireNonNull(handle, "handle");
        this.dragged = Objects.requireNonNull(dragged, "dragged");
        this.payload = Objects.requireNonNull(payload, "payload");
        this.ghost = Objects.requireNonNull(ghost, "ghost");
        this.context = handle.uiContext();
        this.root = topmost(handle);
        this.pressX = pressX;
        this.pressY = pressY;
        this.pointerX = pressX;
        this.pointerY = pressY;
    }

    /**
     * @return payload перетаскиваемого объекта
     */
    @AiDoc(description = "Returns the payload associated with this drag session.")
    @WikiMethod(description = "<key:drag.session.method.payload>")
    @WikiReturn("<key:drag.session.return.payload>")
    public DragPayload payload() {
        return payload;
    }

    /**
     * @return виджет, за который тянут (на нём висит {@link DragController})
     */
    @AiDoc(description = "Returns the handle widget initiating the drag.")
    @WikiMethod(description = "<key:drag.session.method.handle>")
    @WikiReturn("<key:drag.session.return.handle>")
    public Widget handle() {
        return handle;
    }

    /**
     * @return виджет, который логически перемещается (по умолчанию совпадает с {@link #handle()})
     */
    @AiDoc(description = "Returns the widget that is logically moved.")
    @WikiMethod(description = "<key:drag.session.method.dragged>")
    @WikiReturn("<key:drag.session.return.dragged>")
    public Widget dragged() {
        return dragged;
    }

    /**
     * @return корень дерева, в котором ищутся drop-зоны
     */
    @AiDoc(description = "Returns the topmost root widget of the hierarchy.")
    @WikiMethod(description = "<key:drag.session.method.root>")
    @WikiReturn("<key:drag.session.return.root>")
    public Widget root() {
        return root;
    }

    /**
     * @return UI context сессии
     */
    @AiDoc(description = "Returns the UI context for this session.")
    @WikiMethod(description = "<key:drag.session.method.uiContext>")
    @WikiReturn("<key:drag.session.return.uiContext>")
    public UIContext uiContext() {
        return context;
    }

    @AiDoc(description = "Returns the current pointer X coordinate in root space.")
    @WikiMethod(description = "<key:drag.session.method.pointerX>")
    @WikiReturn("<key:drag.session.return.pointerX>")
    public float pointerX() {
        return pointerX;
    }

    @AiDoc(description = "Returns the current pointer Y coordinate in root space.")
    @WikiMethod(description = "<key:drag.session.method.pointerY>")
    @WikiReturn("<key:drag.session.return.pointerY>")
    public float pointerY() {
        return pointerY;
    }

    /**
     * @return X нажатия, с которого начался жест
     */
    @AiDoc(description = "Returns the initial press X coordinate.")
    @WikiMethod(description = "<key:drag.session.method.pressX>")
    @WikiReturn("<key:drag.session.return.pressX>")
    public float pressX() {
        return pressX;
    }

    /**
     * @return Y нажатия, с которого начался жест
     */
    @AiDoc(description = "Returns the initial press Y coordinate.")
    @WikiMethod(description = "<key:drag.session.method.pressY>")
    @WikiReturn("<key:drag.session.return.pressY>")
    public float pressY() {
        return pressY;
    }

    /**
     * @return сдвиг указателя от точки нажатия по X
     */
    @AiDoc(description = "Returns the pointer movement delta along the X axis.")
    @WikiMethod(description = "<key:drag.session.method.deltaX>")
    @WikiReturn("<key:drag.session.return.deltaX>")
    public float deltaX() {
        return pointerX - pressX;
    }

    /**
     * @return сдвиг указателя от точки нажатия по Y
     */
    @AiDoc(description = "Returns the pointer movement delta along the Y axis.")
    @WikiMethod(description = "<key:drag.session.method.deltaY>")
    @WikiReturn("<key:drag.session.return.deltaY>")
    public float deltaY() {
        return pointerY - pressY;
    }

    /**
     * @return сдвиг ghost'а по X с учётом блокировки оси и компенсации прокрутки контейнера
     */
    @AiDoc(description = "Returns the ghost X offset accounting for axis locking and container scroll compensation.")
    @WikiMethod(description = "<key:drag.session.method.ghostOffsetX>")
    @WikiReturn("<key:drag.session.return.ghostOffsetX>")
    public float ghostOffsetX() {
        return ghostOffsetX(ghostWidget != null ? ghostWidget : handle, initialGhostX);
    }

    /**
     * @return сдвиг ghost'а по Y с учётом блокировки оси и компенсации прокрутки контейнера
     */
    @AiDoc(description = "Returns the ghost Y offset accounting for axis locking and container scroll compensation.")
    @WikiMethod(description = "<key:drag.session.method.ghostOffsetY>")
    @WikiReturn("<key:drag.session.return.ghostOffsetY>")
    public float ghostOffsetY() {
        return ghostOffsetY(ghostWidget != null ? ghostWidget : handle, initialGhostY);
    }

    /**
     * Возвращает сдвиг ghost'а по X для указанного виджета с учётом блокировки оси
     * и компенсации смещения самого виджета (например, при скролле).
     */
    @AiDoc(description = "Calculates the ghost X offset for a specific widget with initial coordinate compensation.")
    @WikiMethod(description = "<key:drag.session.method.ghostOffsetXWidget>")
    @WikiReturn("<key:drag.session.return.ghostOffsetX>")
    public float ghostOffsetX(
            @WikiParameter(description = "<key:drag.session.param.widget>")
            Widget widget,
            @WikiParameter(description = "<key:drag.session.param.initialX>")
            float initialX
    ) {
        float raw = controller.ghostAxis() == Orientation.VERTICAL ? 0.0f : deltaX();
        return widget == null ? raw : raw + (initialX - widget.layoutBounds().x());
    }

    /**
     * Возвращает сдвиг ghost'а по Y для указанного виджета с учётом блокировки оси
     * и компенсации смещения самого виджета (например, при скролле).
     */
    @AiDoc(description = "Calculates the ghost Y offset for a specific widget with initial coordinate compensation.")
    @WikiMethod(description = "<key:drag.session.method.ghostOffsetYWidget>")
    @WikiReturn("<key:drag.session.return.ghostOffsetY>")
    public float ghostOffsetY(
            @WikiParameter(description = "<key:drag.session.param.widget>")
            Widget widget,
            @WikiParameter(description = "<key:drag.session.param.initialY>")
            float initialY
    ) {
        float raw = controller.ghostAxis() == Orientation.HORIZONTAL ? 0.0f : deltaY();
        return widget == null ? raw : raw + (initialY - widget.layoutBounds().y());
    }

    /**
     * @return {@code true}, пока жест идёт
     */
    @AiDoc(description = "Returns true while the drag session is actively ongoing.")
    @WikiMethod(description = "<key:drag.session.method.active>")
    @WikiReturn("<key:drag.session.return.active>")
    public boolean active() {
        return active;
    }

    /**
     * @return текущая drop-зона, которая приняла payload, или {@code null}
     */
    @AiDoc(description = "Returns the currently hovered drop zone that accepted the payload, or null.")
    @WikiMethod(description = "<key:drag.session.method.dropTarget>")
    @WikiReturn("<key:drag.session.return.dropTarget>")
    public Widget dropTarget() {
        return dropTarget;
    }

    /**
     * @return ghost сессии
     */
    @AiDoc(description = "Returns the visual drag ghost used in this session.")
    @WikiMethod(description = "<key:drag.session.method.ghost>")
    @WikiReturn("<key:drag.session.return.ghost>")
    public DragGhost ghost() {
        return ghost;
    }

    /**
     * Отменяет жест: зона получает {@link DragLeaveEvent}, drop не происходит.
     */
    @AiDoc(description = "Cancels the active drag gesture.")
    @WikiMethod(description = "<key:drag.session.method.cancel>")
    public void cancel() {
        controller.cancel();
    }

    // -- индикатор вставки --------------------------------------------------------------------

    /**
     * Показывает индикатор вставки в overlay.
     *
     * @param x      левая граница в координатах корня
     * @param y      верхняя граница
     * @param width  ширина
     * @param height высота
     * @param color  цвет индикатора
     */
    @AiDoc(description = "Displays a drop insertion indicator rectangle in the overlay layer.")
    @WikiMethod(description = "<key:drag.session.method.showIndicator>")
    public void showIndicator(
            @WikiParameter(description = "<key:drag.session.param.indicatorX>")
            float x,
            @WikiParameter(description = "<key:drag.session.param.indicatorY>")
            float y,
            @WikiParameter(description = "<key:drag.session.param.indicatorWidth>")
            float width,
            @WikiParameter(description = "<key:drag.session.param.indicatorHeight>")
            float height,
            @WikiParameter(description = "<key:drag.session.param.indicatorColor>")
            ColorView color
    ) {
        indicatorVisible = true;
        indicatorX = x;
        indicatorY = y;
        indicatorWidth = width;
        indicatorHeight = height;
        if (color != null) indicatorColor.set(color.r(), color.g(), color.b(), color.a());
    }

    /**
     * Скрывает индикатор вставки.
     */
    @AiDoc(description = "Hides the drop insertion indicator.")
    @WikiMethod(description = "<key:drag.session.method.clearIndicator>")
    public void clearIndicator() {
        indicatorVisible = false;
    }

    /**
     * @return {@code true}, если индикатор вставки сейчас показан
     */
    @AiDoc(description = "Returns true if the insertion indicator is currently visible.")
    @WikiMethod(description = "<key:drag.session.method.indicatorVisible>")
    @WikiReturn("<key:drag.session.return.indicatorVisible>")
    public boolean indicatorVisible() {
        return indicatorVisible;
    }

    @AiDoc(description = "Returns the insertion indicator X coordinate.")
    @WikiMethod(description = "<key:drag.session.method.indicatorX>")
    @WikiReturn("<key:drag.session.return.indicatorX>")
    public float indicatorX() {
        return indicatorX;
    }

    @AiDoc(description = "Returns the insertion indicator Y coordinate.")
    @WikiMethod(description = "<key:drag.session.method.indicatorY>")
    @WikiReturn("<key:drag.session.return.indicatorY>")
    public float indicatorY() {
        return indicatorY;
    }

    @AiDoc(description = "Returns the insertion indicator width.")
    @WikiMethod(description = "<key:drag.session.method.indicatorWidth>")
    @WikiReturn("<key:drag.session.return.indicatorWidth>")
    public float indicatorWidth() {
        return indicatorWidth;
    }

    @AiDoc(description = "Returns the insertion indicator height.")
    @WikiMethod(description = "<key:drag.session.method.indicatorHeight>")
    @WikiReturn("<key:drag.session.return.indicatorHeight>")
    public float indicatorHeight() {
        return indicatorHeight;
    }

    @AiDoc(description = "Returns the insertion indicator color.")
    @WikiMethod(description = "<key:drag.session.method.indicatorColor>")
    @WikiReturn("<key:drag.session.return.indicatorColor>")
    public ColorView indicatorColor() {
        return indicatorColor;
    }

    // -- жизненный цикл (вызывается контроллером и overlay) ------------------------------------

    /**
     * Запускает сессию: подключает overlay, затемняет источник и делает первое зондирование.
     */
    void begin(float x, float y, Widget ghostWidget) {
        active = true;
        pointerX = x;
        pointerY = y;
        this.ghostWidget = ghostWidget;
        if (ghostWidget != null) {
            this.initialGhostX = ghostWidget.layoutBounds().x();
            this.initialGhostY = ghostWidget.layoutBounds().y();
        } else {
            this.initialGhostX = handle.layoutBounds().x();
            this.initialGhostY = handle.layoutBounds().y();
        }
        dimSource(ghostWidget);
        attachOverlay();
        update(0.0f, false);
    }

    void movePointer(float x, float y) {
        pointerX = x;
        pointerY = y;
        update(0.0f, true);
    }

    /**
     * Покадровое обновление: автоскролл и повторное зондирование (контент мог сдвинуться).
     */
    void tick(float deltaSeconds) {
        if (active) update(deltaSeconds, true);
    }

    /**
     * Завершает жест отпусканием.
     *
     * @return итог жеста
     */
    DragOutcome finish(float x, float y) {
        if (!active) return DragOutcome.CANCELLED;
        pointerX = x;
        pointerY = y;
        update(0.0f, false);

        Widget target = dropTarget;
        DragOutcome outcome;
        if (target != null) {
            dropTarget = null;
            target.handle(new DragDropEvent(target, this));
            outcome = DragOutcome.DROPPED;
        } else {
            outcome = DragOutcome.NO_TARGET;
        }
        shutdown();
        return outcome;
    }

    /**
     * Отменяет жест без drop.
     */
    void abort() {
        if (!active) return;
        shutdown();
    }

    void acceptDropTarget(Widget zone) {
        probedTarget = zone;
    }

    private void shutdown() {
        if (dropTarget != null) {
            Widget previous = dropTarget;
            dropTarget = null;
            previous.handle(new DragLeaveEvent(previous, this));
        }
        active = false;
        indicatorVisible = false;
        restoreSource();
        detachOverlay();
    }

    private void update(float deltaSeconds, boolean notify) {
        if (!active) return;
        autoScroll(deltaSeconds);

        indicatorVisible = false;
        probedTarget = null;
        lastHit = null;
        if (context != null) {
            HitTestResult hit = context.hitTester().hitTest(root, pointerX, pointerY).orElse(null);
            if (hit != null) {
                lastHit = hit.widget();
                context.routedEvents().dispatch(new DragOverEvent(hit.widget(), this));
            }
        }

        Widget next = probedTarget;
        probedTarget = null;
        if (next != dropTarget) {
            Widget previous = dropTarget;
            dropTarget = next;
            if (previous != null) previous.handle(new DragLeaveEvent(previous, this));
        }
        if (notify) controller.notifyMove(this);
        if (overlay != null) overlay.invalidate(dev.sixik.unigui.api.core.InvalidationFlags.VISUAL);
    }

    // -- автоскролл -----------------------------------------------------------------------------

    private void autoScroll(float deltaSeconds) {
        if (!controller.autoScroll() || deltaSeconds <= 0.0f) return;
        if (!scrollAncestors(lastHit, deltaSeconds)) {
            scrollAncestors(dragged, deltaSeconds);
        }
    }

    private boolean scrollAncestors(Widget start, float deltaSeconds) {
        for (Widget widget = start; widget != null; widget = widget.parent()) {
            if (widget instanceof ScrollView scrollView && scrollNearEdge(scrollView, deltaSeconds)) {
                return true;
            }
        }
        return false;
    }

    private boolean scrollNearEdge(ScrollView view, float deltaSeconds) {
        RectView bounds = view.layoutBounds();
        float edge = Math.min(controller.autoScrollEdge(), Math.min(bounds.width(), bounds.height()) / 3.0f);
        float velocityX = edgeVelocity(pointerX - bounds.x(), bounds.right() - pointerX, edge,
                view.scrollX(), view.maxScrollX());
        float velocityY = edgeVelocity(pointerY - bounds.y(), bounds.bottom() - pointerY, edge,
                view.scrollY(), view.maxScrollY());
        if (velocityX == 0.0f && velocityY == 0.0f) return false;

        float beforeX = view.scrollX();
        float beforeY = view.scrollY();
        view.scrollBy(velocityX * deltaSeconds, velocityY * deltaSeconds);
        return view.scrollX() != beforeX || view.scrollY() != beforeY;
    }

    /**
     * Скорость автоскролла вдоль оси в пикселях в секунду.
     *
     * <p>{@code v = ±maxSpeed * clamp((edge - d) / edge, ramp, 1)}, где {@code d} — расстояние от
     * указателя до края viewport'а. Чем глубже указатель в зоне края, тем быстрее прокрутка;
     * за пределами viewport'а скорость максимальна. Скролл идёт только если есть куда
     * прокручивать.</p>
     */
    private float edgeVelocity(float distanceToStart, float distanceToEnd, float edge, float scroll, float maxScroll) {
        if (edge <= 0.0f) return 0.0f;
        if (distanceToStart < edge && scroll > 0.0f) {
            return -controller.autoScrollSpeed() * ramp(distanceToStart, edge);
        }
        if (distanceToEnd < edge && scroll < maxScroll) {
            return controller.autoScrollSpeed() * ramp(distanceToEnd, edge);
        }
        return 0.0f;
    }

    private static float ramp(float distance, float edge) {
        float depth = (edge - distance) / edge;
        return Math.max(MIN_AUTO_SCROLL_RAMP, Math.min(1.0f, depth));
    }

    // -- overlay и затемнение источника --------------------------------------------------------

    private void attachOverlay() {
        OverlayLayer host = null;
        for (Widget widget = handle; widget != null; widget = widget.parent()) {
            if (widget instanceof OverlayLayer layer) host = layer;
        }
        if (host == null) return;
        overlayHost = host;
        overlay = new DragOverlay(this);
        host.addOverlay(overlay);
    }

    private void detachOverlay() {
        if (overlayHost != null && overlay != null) {
            overlayHost.removeOverlay(overlay);
        }
        overlay = null;
        overlayHost = null;
    }

    private void dimSource(Widget ghostWidget) {
        float opacity = controller.sourceOpacity();
        if (opacity >= 1.0f || !(dragged instanceof WidgetBase base)) return;
        // Если ghost повторяет сам источник (или его потомка), затемнение источника затемнило бы и ghost.
        if (ghostWidget != null && isSameOrDescendant(dragged, ghostWidget)) return;
        restoredOpacity = base.opacity();
        base.opacity(Math.min(restoredOpacity, opacity));
        dimmed = true;
    }

    private void restoreSource() {
        if (dimmed && dragged instanceof WidgetBase base) {
            base.opacity(restoredOpacity);
        }
        dimmed = false;
    }

    private static boolean isSameOrDescendant(Widget widget, Widget ancestor) {
        for (Widget current = widget; current != null; current = current.parent()) {
            if (current == ancestor) return true;
        }
        return false;
    }

    private static Widget topmost(Widget widget) {
        Widget current = widget;
        while (current.parent() != null) current = current.parent();
        return current;
    }
}
