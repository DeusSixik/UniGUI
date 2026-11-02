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
import dev.sixik.unigui.api.event.EventPhase;
import dev.sixik.unigui.api.event.EventSubscription;
import dev.sixik.unigui.api.event.PointerMovedEvent;
import dev.sixik.unigui.api.event.PointerPressedEvent;
import dev.sixik.unigui.api.event.PointerReleasedEvent;
import dev.sixik.unigui.api.input.PointerButton;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.widgets.core.Orientation;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Drag-жест, который вешается на любой виджет композицией, без наследования.
 *
 * <pre>{@code
 * DragController.attach(header)
 *         .dragged(section)                         // что логически переносим
 *         .payload(() -> DragPayload.of(id, "unity.component", title, section))
 *         .threshold(4.0f)                          // клик не ломается: drag стартует после 4 px
 *         .onDragStart(session -> ...)
 *         .onDragEnd(end -> ...);
 * }</pre>
 *
 * <h2>Жест</h2>
 * <ol>
 *   <li>Нажатие ЛКМ на виджет (или потомка, если потомок не поглотил событие) запоминает точку
 *       и захватывает указатель за виджетом: дальнейшие события приходят даже вне его границ.</li>
 *   <li>Пока смещение меньше {@link #threshold(float)}, это обычный клик: события не гасятся.</li>
 *   <li>После порога создаётся {@link DragSession}: overlay с ghost'ом, индикатор вставки,
 *       автоскролл. Отпускание гасится, чтобы не сработал клик по заголовку.</li>
 * </ol>
 *
 * <p>Интерактивные потомки (кнопки, чекбоксы) гасят нажатие сами, поэтому до контроллера оно не
 * доходит и клики по ним не ломаются. Для дополнительной фильтрации есть {@link #filter}.</p>
 *
 * <p>Для ghost'а и индикатора дерево должно содержать {@code OverlayLayer}: без него жест работает,
 * но без ghost'а и без покадрового автоскролла.</p>
 */
@AiDoc(summary = "Drag gesture controller attachable to any widget via composition to initiate drag-and-drop operations with ghost previews, threshold handling, and autoscrolling."
)
@WikiPage(
        title = "DragController",
        slug = "drag-controller",
        category = "<key:dnd.category>",
        summary = "<key:drag.controller.summary>"
)
@WikiSection(
        title = "<key:wiki.section.overview.title>",
        markdown = "<key:drag.controller.section.overview.body>",
        order = 10
)
@WikiExample(
        title = "<key:drag.controller.example.attach.title>",
        code = "DragController.attach(header)\n"
                + "        .dragged(card)\n"
                + "        .payload(() -> DragPayload.of(\"card-1\", \"card\", \"Card #1\", data))\n"
                + "        .threshold(4.0f)\n"
                + "        .onDragEnd(end -> System.out.println(\"Outcome: \" + end.outcome()));"
)
@WikiLink(
        label = "DropController",
        target = "page:drop-controller",
        description = "<key:drop.controller.summary>"
)
@WikiLink(
        label = "DragSession",
        target = "page:drag-session",
        description = "<key:drag.session.summary>"
)
public final class DragController {
    private static final float DEFAULT_THRESHOLD = 4.0f;
    private static final float DEFAULT_GHOST_OPACITY = 0.75f;
    private static final float DEFAULT_SOURCE_OPACITY = 0.45f;
    private static final float DEFAULT_AUTO_SCROLL_EDGE = 36.0f;
    private static final float DEFAULT_AUTO_SCROLL_SPEED = 700.0f;

    /**
     * Итог жеста для {@link #onDragEnd(Consumer)}.
     */
    @AiDoc(summary = "Record carrying the finished drag session and the final drag outcome.")
    public record DragEnd(DragSession session, DragOutcome outcome) {
    }

    private enum State {
        IDLE,
        PRESSED,
        DRAGGING
    }

    private final Widget handle;
    private final List<EventSubscription> subscriptions = new ArrayList<>(3);
    private final List<Consumer<DragSession>> startListeners = new ArrayList<>();
    private final List<Consumer<DragSession>> moveListeners = new ArrayList<>();
    private final List<Consumer<DragEnd>> endListeners = new ArrayList<>();

    private Widget dragged;
    private Supplier<DragPayload> payload;
    private DragGhost ghost;
    private Widget ghostWidget;
    private float ghostOpacity = DEFAULT_GHOST_OPACITY;
    private Orientation ghostAxis;
    private float sourceOpacity = DEFAULT_SOURCE_OPACITY;
    private float threshold = DEFAULT_THRESHOLD;
    private boolean enabled = true;
    private Predicate<PointerPressedEvent> filter;
    private boolean autoScroll = true;
    private float autoScrollEdge = DEFAULT_AUTO_SCROLL_EDGE;
    private float autoScrollSpeed = DEFAULT_AUTO_SCROLL_SPEED;

    private State state = State.IDLE;
    private int pointerId = -1;
    private float pressX;
    private float pressY;
    private DragSession session;

    private DragController(Widget handle) {
        this.handle = Objects.requireNonNull(handle, "handle");
        subscriptions.add(handle.on(PointerPressedEvent.TYPE, this::onPressed));
        subscriptions.add(handle.on(PointerMovedEvent.TYPE, this::onMoved));
        subscriptions.add(handle.on(PointerReleasedEvent.TYPE, true, this::onReleased));
    }

    /**
     * Подключает drag-жест к виджету.
     *
     * @param handle виджет, за который тянут (например, заголовок секции)
     * @return контроллер для fluent-настройки
     */
    @AiDoc(summary = "Attaches a new DragController to the specified handle widget.")
    @WikiMethod(summary = "<key:drag.controller.method.attach>")
    @WikiReturn("<key:drag.controller.return.controller>")
    public static DragController attach(
            @WikiParameter(description = "<key:drag.controller.param.handle>")
            Widget handle
    ) {
        return new DragController(handle);
    }

    // -- настройка -------------------------------------------------------------------------------

    /**
     * Что логически перемещается: по умолчанию сам {@code handle}. Например, вся секция при драге за заголовок.
     */
    @AiDoc(summary = "Specifies the logical widget being dragged (e.g. the entire card when dragged by its header).")
    @WikiMethod(summary = "<key:drag.controller.method.dragged>")
    @WikiReturn("<key:drag.controller.return.this>")
    public DragController dragged(
            @WikiParameter(description = "<key:drag.controller.param.dragged>")
            Widget dragged
    ) {
        this.dragged = dragged;
        return this;
    }

    /**
     * Фиксированный payload.
     */
    @AiDoc(summary = "Configures a static constant payload for the drag gesture.")
    @WikiMethod(summary = "<key:drag.controller.method.payloadConstant>")
    @WikiReturn("<key:drag.controller.return.this>")
    public DragController payload(
            @WikiParameter(description = "<key:drag.controller.param.payload>")
            DragPayload payload
    ) {
        Objects.requireNonNull(payload, "payload");
        this.payload = () -> payload;
        return this;
    }

    /**
     * Payload, создаваемый в момент старта жеста. Возврат {@code null} отменяет drag.
     */
    @AiDoc(summary = "Configures a lazy payload supplier invoked when the drag threshold is exceeded.")
    @WikiMethod(summary = "<key:drag.controller.method.payloadSupplier>")
    @WikiReturn("<key:drag.controller.return.this>")
    public DragController payload(
            @WikiParameter(description = "<key:drag.controller.param.payloadSupplier>")
            Supplier<DragPayload> payload
    ) {
        this.payload = payload;
        return this;
    }

    /**
     * Порог движения в UI-пикселях до начала drag (по умолчанию 4).
     */
    @AiDoc(summary = "Sets the minimum movement threshold in pixels before the drag session starts.")
    @WikiMethod(summary = "<key:drag.controller.method.threshold>")
    @WikiReturn("<key:drag.controller.return.this>")
    public DragController threshold(
            @WikiParameter(description = "<key:drag.controller.param.threshold>")
            float threshold
    ) {
        this.threshold = Float.isFinite(threshold) ? Math.max(0.0f, threshold) : DEFAULT_THRESHOLD;
        return this;
    }

    @AiDoc(summary = "Enables or disables this drag controller.")
    @WikiMethod(summary = "<key:drag.controller.method.enabled>")
    @WikiReturn("<key:drag.controller.return.this>")
    public DragController enabled(
            @WikiParameter(description = "<key:drag.controller.param.enabled>")
            boolean enabled
    ) {
        if (!enabled) cancel();
        this.enabled = enabled;
        return this;
    }

    /**
     * Дополнительный фильтр нажатий: {@code false} не даёт начать жест.
     */
    @AiDoc(summary = "Sets a predicate filter to selectively reject pointer pressed events.")
    @WikiMethod(summary = "<key:drag.controller.method.filter>")
    @WikiReturn("<key:drag.controller.return.this>")
    public DragController filter(
            @WikiParameter(description = "<key:drag.controller.param.filter>")
            Predicate<PointerPressedEvent> filter
    ) {
        this.filter = filter;
        return this;
    }

    /**
     * Собственный ghost.
     */
    @AiDoc(summary = "Configures a custom drag ghost renderer.")
    @WikiMethod(summary = "<key:drag.controller.method.ghostCustom>")
    @WikiReturn("<key:drag.controller.return.this>")
    public DragController ghost(
            @WikiParameter(description = "<key:drag.controller.param.ghost>")
            DragGhost ghost
    ) {
        this.ghost = ghost;
        this.ghostWidget = null;
        return this;
    }

    /**
     * Ghost, повторяющий живой виджет (например, заголовок или вся секция).
     */
    @AiDoc(summary = "Configures a ghost that duplicates the visual appearance of the specified widget.")
    @WikiMethod(summary = "<key:drag.controller.method.ghostWidget>")
    @WikiReturn("<key:drag.controller.return.this>")
    public DragController ghost(
            @WikiParameter(description = "<key:drag.controller.param.widget>")
            Widget widget
    ) {
        this.ghostWidget = widget;
        this.ghost = null;
        return this;
    }

    /**
     * Прозрачность widget-ghost'а {@code [0, 1]} (по умолчанию 0.75).
     */
    @AiDoc(summary = "Sets the opacity level for the widget ghost preview.")
    @WikiMethod(summary = "<key:drag.controller.method.ghostOpacity>")
    @WikiReturn("<key:drag.controller.return.this>")
    public DragController ghostOpacity(
            @WikiParameter(description = "<key:drag.controller.param.opacity>")
            float opacity
    ) {
        this.ghostOpacity = Float.isFinite(opacity) ? Math.max(0.0f, Math.min(1.0f, opacity)) : DEFAULT_GHOST_OPACITY;
        return this;
    }

    /**
     * Блокирует ось движения ghost'а: {@link Orientation#VERTICAL} двигает только по вертикали
     * (типично для списка), {@code null} разрешает свободное движение.
     */
    @AiDoc(summary = "Locks ghost movement to a single axis (e.g. VERTICAL for lists) or null for free movement.")
    @WikiMethod(summary = "<key:drag.controller.method.lockGhostAxis>")
    @WikiReturn("<key:drag.controller.return.this>")
    public DragController lockGhostAxis(
            @WikiParameter(description = "<key:drag.controller.param.axis>")
            Orientation axis
    ) {
        this.ghostAxis = axis;
        return this;
    }

    /**
     * Прозрачность исходного виджета во время драга; {@code 1} отключает затемнение (по умолчанию 0.45).
     */
    @AiDoc(summary = "Sets the opacity of the source widget while dragging (1.0 disables dimming).")
    @WikiMethod(summary = "<key:drag.controller.method.sourceOpacity>")
    @WikiReturn("<key:drag.controller.return.this>")
    public DragController sourceOpacity(
            @WikiParameter(description = "<key:drag.controller.param.opacity>")
            float opacity
    ) {
        this.sourceOpacity = Float.isFinite(opacity) ? Math.max(0.0f, Math.min(1.0f, opacity)) : DEFAULT_SOURCE_OPACITY;
        return this;
    }

    /**
     * Включает или выключает автоскролл у края {@code ScrollView} (по умолчанию включён).
     */
    @AiDoc(summary = "Enables or disables automatic scrolling near edges of parent ScrollViews.")
    @WikiMethod(summary = "<key:drag.controller.method.autoScrollEnabled>")
    @WikiReturn("<key:drag.controller.return.this>")
    public DragController autoScroll(
            @WikiParameter(description = "<key:drag.controller.param.autoScroll>")
            boolean autoScroll
    ) {
        this.autoScroll = autoScroll;
        return this;
    }

    /**
     * Настраивает автоскролл.
     *
     * @param edge            ширина зоны у края viewport'а в пикселях
     * @param pixelsPerSecond максимальная скорость прокрутки
     */
    @AiDoc(summary = "Configures autoscroll edge margin and maximum scroll speed in pixels per second.")
    @WikiMethod(summary = "<key:drag.controller.method.autoScrollConfig>")
    @WikiReturn("<key:drag.controller.return.this>")
    public DragController autoScroll(
            @WikiParameter(description = "<key:drag.controller.param.edge>")
            float edge,
            @WikiParameter(description = "<key:drag.controller.param.pixelsPerSecond>")
            float pixelsPerSecond
    ) {
        this.autoScrollEdge = Float.isFinite(edge) ? Math.max(0.0f, edge) : DEFAULT_AUTO_SCROLL_EDGE;
        this.autoScrollSpeed = Float.isFinite(pixelsPerSecond) ? Math.max(0.0f, pixelsPerSecond) : DEFAULT_AUTO_SCROLL_SPEED;
        return this;
    }

    @AiDoc(summary = "Registers a listener invoked when a drag gesture starts.")
    @WikiMethod(summary = "<key:drag.controller.method.onDragStart>")
    @WikiReturn("<key:drag.controller.return.this>")
    public DragController onDragStart(
            @WikiParameter(description = "<key:drag.controller.param.startListener>")
            Consumer<DragSession> listener
    ) {
        startListeners.add(Objects.requireNonNull(listener, "listener"));
        return this;
    }

    @AiDoc(summary = "Registers a listener invoked on each pointer movement during drag.")
    @WikiMethod(summary = "<key:drag.controller.method.onDragMove>")
    @WikiReturn("<key:drag.controller.return.this>")
    public DragController onDragMove(
            @WikiParameter(description = "<key:drag.controller.param.moveListener>")
            Consumer<DragSession> listener
    ) {
        moveListeners.add(Objects.requireNonNull(listener, "listener"));
        return this;
    }

    @AiDoc(summary = "Registers a listener invoked when a drag gesture completes or is cancelled.")
    @WikiMethod(summary = "<key:drag.controller.method.onDragEnd>")
    @WikiReturn("<key:drag.controller.return.this>")
    public DragController onDragEnd(
            @WikiParameter(description = "<key:drag.controller.param.endListener>")
            Consumer<DragEnd> listener
    ) {
        endListeners.add(Objects.requireNonNull(listener, "listener"));
        return this;
    }

    // -- состояние -------------------------------------------------------------------------------

    /**
     * @return виджет, за который тянут
     */
    @AiDoc(summary = "Returns the handle widget.")
    @WikiMethod(summary = "<key:drag.controller.method.handle>")
    @WikiReturn("<key:drag.controller.return.handle>")
    public Widget handle() {
        return handle;
    }

    /**
     * @return {@code true}, пока идёт drag (порог пройден)
     */
    @AiDoc(summary = "Returns true if currently in an active dragging state past the threshold.")
    @WikiMethod(summary = "<key:drag.controller.method.dragging>")
    @WikiReturn("<key:drag.controller.return.dragging>")
    public boolean dragging() {
        return state == State.DRAGGING;
    }

    /**
     * @return активная сессия или {@code null}
     */
    @AiDoc(summary = "Returns the active DragSession, or null if idle.")
    @WikiMethod(summary = "<key:drag.controller.method.session>")
    @WikiReturn("<key:drag.controller.return.session>")
    public DragSession session() {
        return session;
    }

    /**
     * Отменяет текущий жест, если он идёт.
     */
    @AiDoc(summary = "Cancels the active gesture if one is in progress.")
    @WikiMethod(summary = "<key:drag.controller.method.cancel>")
    public void cancel() {
        if (state == State.IDLE) return;
        DragSession current = session;
        boolean wasDragging = state == State.DRAGGING;
        releaseCapture();
        resetState();
        if (wasDragging && current != null) {
            current.abort();
            fireEnd(current, DragOutcome.CANCELLED);
        }
    }

    /**
     * Снимает контроллер с виджета и отменяет жест.
     */
    @AiDoc(summary = "Detaches event listeners from the handle widget and terminates active gestures.")
    @WikiMethod(summary = "<key:drag.controller.method.detach>")
    public void detach() {
        cancel();
        for (EventSubscription subscription : subscriptions) subscription.unsubscribe();
        subscriptions.clear();
    }

    // -- параметры для DragSession ---------------------------------------------------------------

    Orientation ghostAxis() {
        return ghostAxis;
    }

    float sourceOpacity() {
        return sourceOpacity;
    }

    boolean autoScroll() {
        return autoScroll;
    }

    float autoScrollEdge() {
        return autoScrollEdge;
    }

    float autoScrollSpeed() {
        return autoScrollSpeed;
    }

    void notifyMove(DragSession session) {
        for (Consumer<DragSession> listener : List.copyOf(moveListeners)) listener.accept(session);
    }

    // -- обработка указателя ---------------------------------------------------------------------

    private void onPressed(PointerPressedEvent event) {
        if (event.phase() == EventPhase.CAPTURE || event.isCancelled()) return;
        if (!enabled || state != State.IDLE || event.button() != PointerButton.PRIMARY) return;
        if (filter != null && !filter.test(event)) return;

        state = State.PRESSED;
        pointerId = event.pointerId();
        pressX = event.rootX();
        pressY = event.rootY();
        UIContext context = handle.uiContext();
        if (context != null) context.capturePointer(pointerId, handle);
    }

    private void onMoved(PointerMovedEvent event) {
        if (event.phase() == EventPhase.CAPTURE || state == State.IDLE || event.pointerId() != pointerId) return;
        if (state == State.PRESSED) {
            float dx = event.rootX() - pressX;
            float dy = event.rootY() - pressY;
            if (dx * dx + dy * dy < threshold * threshold) return;
            if (!beginDrag(event.rootX(), event.rootY())) return;
        } else if (session != null) {
            session.movePointer(event.rootX(), event.rootY());
        }
        event.cancel();
    }

    private void onReleased(PointerReleasedEvent event) {
        if (event.phase() == EventPhase.CAPTURE || state == State.IDLE) return;
        if (event.pointerId() != pointerId || event.button() != PointerButton.PRIMARY) return;

        DragSession current = session;
        boolean wasDragging = state == State.DRAGGING;
        releaseCapture();
        resetState();
        if (!wasDragging || current == null) return;

        DragOutcome outcome = current.finish(event.rootX(), event.rootY());
        fireEnd(current, outcome);
        // Жест состоялся: гасим отпускание, чтобы заголовок не воспринял его как клик.
        event.cancel();
    }

    private boolean beginDrag(float x, float y) {
        DragPayload resolved = payload != null ? payload.get() : defaultPayload();
        if (resolved == null) {
            releaseCapture();
            resetState();
            return false;
        }

        Widget moving = dragged != null ? dragged : handle;
        Widget ghostSource = ghostWidget != null ? ghostWidget : (ghost == null ? handle : null);
        DragGhost resolvedGhost = ghost != null ? ghost : DragGhost.widget(ghostSource, ghostOpacity);

        session = new DragSession(this, handle, moving, resolved, resolvedGhost, pressX, pressY);
        state = State.DRAGGING;
        session.begin(x, y, ghostSource);
        for (Consumer<DragSession> listener : List.copyOf(startListeners)) listener.accept(session);
        return true;
    }

    private DragPayload defaultPayload() {
        Widget moving = dragged != null ? dragged : handle;
        return DragPayload.of(moving.id(), "generic", moving.id(), moving);
    }

    private void fireEnd(DragSession finished, DragOutcome outcome) {
        DragEnd end = new DragEnd(finished, outcome);
        for (Consumer<DragEnd> listener : List.copyOf(endListeners)) listener.accept(end);
    }

    private void releaseCapture() {
        UIContext context = handle.uiContext();
        if (context != null && pointerId >= 0) context.releasePointer(pointerId, handle);
    }

    private void resetState() {
        state = State.IDLE;
        pointerId = -1;
        session = null;
    }
}
