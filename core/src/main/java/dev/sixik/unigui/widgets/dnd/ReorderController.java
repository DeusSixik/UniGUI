package dev.sixik.unigui.widgets.dnd;

import dev.sixik.unigui.api.event.EventPhase;
import dev.sixik.unigui.api.event.EventSubscription;
import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.widgets.containers.PanelWidget;
import dev.sixik.unigui.widgets.core.Orientation;
import dev.sixik.unigui.widgets.editor.DragPayload;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Сортировка детей контейнера перетаскиванием.
 *
 * <pre>{@code
 * ReorderController.attach(componentList)
 *         .accepts("unity.component")
 *         .orientation(Orientation.VERTICAL)
 *         .onReorder((from, to) -> model.move(from, to));
 * }</pre>
 *
 * <h2>Как считается позиция вставки</h2>
 * <p>Для вертикального списка указатель сравнивается с серединой каждого ребёнка:
 * {@code raw = индекс первого ребёнка, у которого pointerY < centerY}, а если такого нет, то индекс
 * после последнего. Для внутреннего переноса итоговая позиция после удаления элемента:
 * {@code to = raw > from ? raw - 1 : raw}. Если {@code to == from}, ничего не меняется и индикатор
 * скрывается.</p>
 *
 * <h2>Индикатор</h2>
 * <p>Линия вставки рисуется в overlay между соседями (посередине зазора), поэтому не зависит от
 * clip'ов и layout контейнера. Перестановка выполняется при отпускании: во время драга список
 * не прыгает.</p>
 *
 * <p>Если контейнер является {@link PanelWidget}, а перетаскиваемый элемент его прямой потомок
 * (или лежит в нём глубже), перестановка применяется автоматически ({@link #autoApply(boolean)}).
 * Для внешнего payload индекс вставки только сообщается в событии.</p>
 */
public final class ReorderController {
    private static final float DEFAULT_THICKNESS = 2.0f;

    /**
     * Результат перестановки.
     *
     * @param container контейнер
     * @param payload   payload жеста
     * @param item      перемещаемый ребёнок контейнера или {@code null} для внешнего payload
     * @param fromIndex прежний индекс или {@code -1} для внешнего payload
     * @param toIndex   новый индекс среди детей контейнера (после удаления для внутреннего переноса)
     */
    public record ReorderEvent(Widget container, DragPayload payload, Widget item, int fromIndex, int toIndex) {
        /**
         * @return {@code true}, если переносили ребёнка этого же контейнера
         */
        public boolean internal() {
            return fromIndex >= 0;
        }
    }

    private final Widget container;
    private final List<EventSubscription> subscriptions = new ArrayList<>(3);
    private final Set<String> acceptedTypes = new LinkedHashSet<>();
    private final List<Predicate<DragPayload>> validators = new ArrayList<>();
    private final List<Consumer<ReorderEvent>> listeners = new ArrayList<>();
    private final MutableColor indicatorColor = new MutableColor(0.30f, 0.60f, 1.0f, 1.0f);
    private Orientation orientation = Orientation.VERTICAL;
    private Predicate<Widget> itemFilter = widget -> widget.visibility() != Visibility.COLLAPSED;
    private boolean acceptAll = true;
    private boolean internalOnly;
    private boolean autoApply = true;
    private boolean enabled = true;
    private float indicatorThickness = DEFAULT_THICKNESS;
    private float indicatorInset;

    private ReorderController(Widget container) {
        this.container = Objects.requireNonNull(container, "container");
        subscriptions.add(container.on(DragOverEvent.TYPE, this::onOver));
        subscriptions.add(container.on(DragDropEvent.TYPE, this::onDrop));
    }

    /**
     * Подключает сортировку к контейнеру.
     *
     * @param container контейнер, дети которого переупорядочиваются
     * @return контроллер для fluent-настройки
     */
    public static ReorderController attach(Widget container) {
        return new ReorderController(container);
    }

    /**
     * Направление списка (по умолчанию вертикальное).
     */
    public ReorderController orientation(Orientation orientation) {
        this.orientation = orientation == null ? Orientation.VERTICAL : orientation;
        return this;
    }

    /**
     * Принимаемые типы payload через запятую, {@code |} или пробел; {@code *} принимает всё.
     */
    public ReorderController accepts(String types) {
        acceptedTypes.clear();
        String normalized = types == null ? "" : types.trim();
        acceptAll = normalized.isEmpty() || normalized.equals("*");
        if (!acceptAll) {
            for (String token : normalized.split("[,|\\s]+")) {
                if (!token.isBlank()) acceptedTypes.add(token.trim());
            }
            acceptAll = acceptedTypes.isEmpty();
        }
        return this;
    }

    /**
     * Дополнительная проверка payload.
     */
    public ReorderController accepts(Predicate<DragPayload> validator) {
        validators.add(Objects.requireNonNull(validator, "validator"));
        return this;
    }

    /**
     * Принимать только элементы этого контейнера (без внешних payload).
     */
    public ReorderController internalOnly(boolean internalOnly) {
        this.internalOnly = internalOnly;
        return this;
    }

    /**
     * Применять перестановку к {@link PanelWidget} автоматически (по умолчанию да).
     */
    public ReorderController autoApply(boolean autoApply) {
        this.autoApply = autoApply;
        return this;
    }

    /**
     * Какие дети считаются элементами списка (по умолчанию все, кроме COLLAPSED).
     */
    public ReorderController itemFilter(Predicate<Widget> itemFilter) {
        this.itemFilter = Objects.requireNonNull(itemFilter, "itemFilter");
        return this;
    }

    /**
     * Внешний вид линии вставки.
     *
     * @param color     цвет линии
     * @param thickness толщина в пикселях
     * @param inset     отступ линии от краёв контейнера
     */
    public ReorderController indicator(ColorView color, float thickness, float inset) {
        if (color != null) indicatorColor.set(color.r(), color.g(), color.b(), color.a());
        this.indicatorThickness = Float.isFinite(thickness) ? Math.max(1.0f, thickness) : DEFAULT_THICKNESS;
        this.indicatorInset = Float.isFinite(inset) ? Math.max(0.0f, inset) : 0.0f;
        return this;
    }

    public ReorderController enabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    /**
     * Слушатель перестановки с полным событием.
     */
    public ReorderController onReorder(Consumer<ReorderEvent> listener) {
        listeners.add(Objects.requireNonNull(listener, "listener"));
        return this;
    }

    /**
     * Слушатель перестановки: {@code (fromIndex, toIndex)}. Для внешнего payload {@code fromIndex = -1}.
     */
    public ReorderController onReorder(BiConsumer<Integer, Integer> listener) {
        Objects.requireNonNull(listener, "listener");
        listeners.add(event -> listener.accept(event.fromIndex(), event.toIndex()));
        return this;
    }

    /**
     * @return контейнер
     */
    public Widget container() {
        return container;
    }

    /**
     * Снимает контроллер с контейнера.
     */
    public void detach() {
        for (EventSubscription subscription : subscriptions) subscription.unsubscribe();
        subscriptions.clear();
    }

    // -- обработка -----------------------------------------------------------------------------

    private void onOver(DragOverEvent event) {
        if (event.phase() == EventPhase.CAPTURE || event.isCancelled()) return;
        DragSession session = event.session();
        if (!accepts(session)) return;

        Insertion insertion = insertion(session);
        if (!insertion.noop()) {
            showIndicator(session, insertion.raw());
        }
        event.accept(container);
    }

    private void onDrop(DragDropEvent event) {
        if (event.phase() == EventPhase.CAPTURE) return;
        DragSession session = event.session();
        if (!accepts(session)) return;

        Insertion insertion = insertion(session);
        if (insertion.noop()) return;

        if (insertion.item() != null && autoApply && container instanceof PanelWidget panel) {
            panel.removeChild(insertion.item());
            panel.insertChild(insertion.to(), insertion.item());
            panel.applyQueuedMutations();
        }
        ReorderEvent reorder = new ReorderEvent(container, session.payload(), insertion.item(),
                insertion.from(), insertion.to());
        for (Consumer<ReorderEvent> listener : List.copyOf(listeners)) listener.accept(reorder);
    }

    private boolean accepts(DragSession session) {
        if (!enabled || !container.enabled() || session == null) return false;
        DragPayload payload = session.payload();
        if (!acceptAll && !acceptedTypes.contains(payload.type())) return false;
        for (Predicate<DragPayload> validator : validators) {
            if (!validator.test(payload)) return false;
        }
        return !internalOnly || itemOf(session.dragged()) != null;
    }

    private record Insertion(Widget item, int from, int raw, int to) {
        boolean noop() {
            return from >= 0 && to == from;
        }
    }

    private Insertion insertion(DragSession session) {
        Widget item = itemOf(session.dragged());
        int from = item == null ? -1 : container.children().indexOf(item);
        int raw = rawInsertionIndex(session.pointerX(), session.pointerY());
        int to = from >= 0 && raw > from ? raw - 1 : raw;
        return new Insertion(from >= 0 ? item : null, from, raw, to);
    }

    /**
     * Ребёнок контейнера, внутри которого лежит перетаскиваемый виджет, или {@code null}.
     */
    private Widget itemOf(Widget dragged) {
        for (Widget widget = dragged; widget != null; widget = widget.parent()) {
            if (widget.parent() == container) return widget;
        }
        return null;
    }

    private int rawInsertionIndex(float pointerX, float pointerY) {
        boolean vertical = orientation == Orientation.VERTICAL;
        float position = vertical ? pointerY : pointerX;
        List<Widget> children = container.children();
        int lastItem = -1;
        for (int i = 0; i < children.size(); i++) {
            Widget child = children.get(i);
            if (!itemFilter.test(child)) continue;
            RectView bounds = child.layoutBounds();
            float center = vertical
                    ? bounds.y() + bounds.height() * 0.5f
                    : bounds.x() + bounds.width() * 0.5f;
            if (position < center) return i;
            lastItem = i;
        }
        return lastItem + 1;
    }

    /**
     * Линия вставки посередине зазора между соседями (или у края списка).
     */
    private void showIndicator(DragSession session, int raw) {
        boolean vertical = orientation == Orientation.VERTICAL;
        List<Widget> children = container.children();
        Widget previous = null;
        for (int i = Math.min(raw, children.size()) - 1; i >= 0; i--) {
            if (itemFilter.test(children.get(i))) {
                previous = children.get(i);
                break;
            }
        }
        Widget next = null;
        for (int i = Math.max(raw, 0); i < children.size(); i++) {
            if (itemFilter.test(children.get(i))) {
                next = children.get(i);
                break;
            }
        }

        RectView box = container.layoutBounds();
        float position;
        if (previous != null && next != null) {
            position = (end(previous, vertical) + start(next, vertical)) * 0.5f;
        } else if (next != null) {
            position = start(next, vertical);
        } else if (previous != null) {
            position = end(previous, vertical);
        } else {
            position = vertical ? box.y() : box.x();
        }

        float half = indicatorThickness * 0.5f;
        if (vertical) {
            session.showIndicator(box.x() + indicatorInset, position - half,
                    Math.max(1.0f, box.width() - indicatorInset * 2.0f), indicatorThickness, indicatorColor);
        } else {
            session.showIndicator(position - half, box.y() + indicatorInset,
                    indicatorThickness, Math.max(1.0f, box.height() - indicatorInset * 2.0f), indicatorColor);
        }
    }

    private static float start(Widget widget, boolean vertical) {
        RectView bounds = widget.layoutBounds();
        return vertical ? bounds.y() : bounds.x();
    }

    private static float end(Widget widget, boolean vertical) {
        RectView bounds = widget.layoutBounds();
        return vertical ? bounds.y() + bounds.height() : bounds.x() + bounds.width();
    }
}
