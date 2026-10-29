package dev.sixik.unigui.widgets.dnd;

import dev.sixik.unigui.api.core.UIContext;
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
public final class DragController {
    private static final float DEFAULT_THRESHOLD = 4.0f;
    private static final float DEFAULT_GHOST_OPACITY = 0.75f;
    private static final float DEFAULT_SOURCE_OPACITY = 0.45f;
    private static final float DEFAULT_AUTO_SCROLL_EDGE = 36.0f;
    private static final float DEFAULT_AUTO_SCROLL_SPEED = 700.0f;

    /**
     * Итог жеста для {@link #onDragEnd(Consumer)}.
     */
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
    public static DragController attach(Widget handle) {
        return new DragController(handle);
    }

    // -- настройка -------------------------------------------------------------------------------

    /**
     * Что логически перемещается: по умолчанию сам {@code handle}. Например, вся секция при драге за заголовок.
     */
    public DragController dragged(Widget dragged) {
        this.dragged = dragged;
        return this;
    }

    /**
     * Фиксированный payload.
     */
    public DragController payload(DragPayload payload) {
        Objects.requireNonNull(payload, "payload");
        this.payload = () -> payload;
        return this;
    }

    /**
     * Payload, создаваемый в момент старта жеста. Возврат {@code null} отменяет drag.
     */
    public DragController payload(Supplier<DragPayload> payload) {
        this.payload = payload;
        return this;
    }

    /**
     * Порог движения в UI-пикселях до начала drag (по умолчанию 4).
     */
    public DragController threshold(float threshold) {
        this.threshold = Float.isFinite(threshold) ? Math.max(0.0f, threshold) : DEFAULT_THRESHOLD;
        return this;
    }

    public DragController enabled(boolean enabled) {
        if (!enabled) cancel();
        this.enabled = enabled;
        return this;
    }

    /**
     * Дополнительный фильтр нажатий: {@code false} не даёт начать жест.
     */
    public DragController filter(Predicate<PointerPressedEvent> filter) {
        this.filter = filter;
        return this;
    }

    /**
     * Собственный ghost.
     */
    public DragController ghost(DragGhost ghost) {
        this.ghost = ghost;
        this.ghostWidget = null;
        return this;
    }

    /**
     * Ghost, повторяющий живой виджет (например, заголовок или вся секция).
     */
    public DragController ghost(Widget widget) {
        this.ghostWidget = widget;
        this.ghost = null;
        return this;
    }

    /**
     * Прозрачность widget-ghost'а {@code [0, 1]} (по умолчанию 0.75).
     */
    public DragController ghostOpacity(float opacity) {
        this.ghostOpacity = Float.isFinite(opacity) ? Math.max(0.0f, Math.min(1.0f, opacity)) : DEFAULT_GHOST_OPACITY;
        return this;
    }

    /**
     * Блокирует ось движения ghost'а: {@link Orientation#VERTICAL} двигает только по вертикали
     * (типично для списка), {@code null} разрешает свободное движение.
     */
    public DragController lockGhostAxis(Orientation axis) {
        this.ghostAxis = axis;
        return this;
    }

    /**
     * Прозрачность исходного виджета во время драга; {@code 1} отключает затемнение (по умолчанию 0.45).
     */
    public DragController sourceOpacity(float opacity) {
        this.sourceOpacity = Float.isFinite(opacity) ? Math.max(0.0f, Math.min(1.0f, opacity)) : DEFAULT_SOURCE_OPACITY;
        return this;
    }

    /**
     * Включает или выключает автоскролл у края {@code ScrollView} (по умолчанию включён).
     */
    public DragController autoScroll(boolean autoScroll) {
        this.autoScroll = autoScroll;
        return this;
    }

    /**
     * Настраивает автоскролл.
     *
     * @param edge            ширина зоны у края viewport'а в пикселях
     * @param pixelsPerSecond максимальная скорость прокрутки
     */
    public DragController autoScroll(float edge, float pixelsPerSecond) {
        this.autoScrollEdge = Float.isFinite(edge) ? Math.max(0.0f, edge) : DEFAULT_AUTO_SCROLL_EDGE;
        this.autoScrollSpeed = Float.isFinite(pixelsPerSecond) ? Math.max(0.0f, pixelsPerSecond) : DEFAULT_AUTO_SCROLL_SPEED;
        return this;
    }

    public DragController onDragStart(Consumer<DragSession> listener) {
        startListeners.add(Objects.requireNonNull(listener, "listener"));
        return this;
    }

    public DragController onDragMove(Consumer<DragSession> listener) {
        moveListeners.add(Objects.requireNonNull(listener, "listener"));
        return this;
    }

    public DragController onDragEnd(Consumer<DragEnd> listener) {
        endListeners.add(Objects.requireNonNull(listener, "listener"));
        return this;
    }

    // -- состояние -------------------------------------------------------------------------------

    /**
     * @return виджет, за который тянут
     */
    public Widget handle() {
        return handle;
    }

    /**
     * @return {@code true}, пока идёт drag (порог пройден)
     */
    public boolean dragging() {
        return state == State.DRAGGING;
    }

    /**
     * @return активная сессия или {@code null}
     */
    public DragSession session() {
        return session;
    }

    /**
     * Отменяет текущий жест, если он идёт.
     */
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
