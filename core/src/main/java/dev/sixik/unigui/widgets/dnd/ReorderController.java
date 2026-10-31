package dev.sixik.unigui.widgets.dnd;

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
import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.widgets.containers.PanelWidget;
import dev.sixik.unigui.widgets.core.Orientation;

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
@AiDoc(
        description = "Drag reordering controller attachable to container widgets (e.g. VBox, HBox, PanelWidget) supporting insertion lines and automated DOM reordering."
)
@WikiPage(
        title = "ReorderController",
        slug = "reorder-controller",
        category = "<key:dnd.category>",
        summary = "<key:reorder.controller.summary>"
)
@WikiSection(
        title = "<key:wiki.section.overview.title>",
        markdown = "<key:reorder.controller.section.overview.body>",
        order = 10
)
@WikiExample(
        title = "<key:reorder.controller.example.attach.title>",
        code = "ReorderController.attach(vbox)\n"
                + "        .orientation(Orientation.VERTICAL)\n"
                + "        .accepts(\"card\")\n"
                + "        .onReorder((from, to) -> dataList.add(to, dataList.remove((int) from)));"
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
@WikiLink(
        label = "DragSession",
        target = "page:drag-session",
        description = "<key:drag.session.summary>"
)
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
    @AiDoc(description = "Event record describing reorder source, destination indices, and dragged item.")
    public record ReorderEvent(Widget container, DragPayload payload, Widget item, int fromIndex, int toIndex) {
        /**
         * @return {@code true}, если переносили ребёнка этого же контейнера
         */
        @AiDoc(description = "Returns true if the dragged item was an internal child of this container.")
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
    @AiDoc(description = "Attaches a ReorderController to the specified container widget.")
    @WikiMethod(description = "<key:reorder.controller.method.attach>")
    @WikiReturn("<key:reorder.controller.return.controller>")
    public static ReorderController attach(
            @WikiParameter(description = "<key:reorder.controller.param.container>")
            Widget container
    ) {
        return new ReorderController(container);
    }

    /**
     * Направление списка (по умолчанию вертикальное).
     */
    @AiDoc(description = "Configures orientation (VERTICAL or HORIZONTAL) for reordering calculations.")
    @WikiMethod(description = "<key:reorder.controller.method.orientation>")
    @WikiReturn("<key:reorder.controller.return.this>")
    public ReorderController orientation(
            @WikiParameter(description = "<key:reorder.controller.param.orientation>")
            Orientation orientation
    ) {
        this.orientation = orientation == null ? Orientation.VERTICAL : orientation;
        return this;
    }

    /**
     * Принимаемые типы payload через запятую, {@code |} или пробел; {@code *} принимает всё.
     */
    @AiDoc(description = "Restricts accepted payload types by comma/pipe/whitespace delimited string (* accepts all).")
    @WikiMethod(description = "<key:reorder.controller.method.acceptsTypes>")
    @WikiReturn("<key:reorder.controller.return.this>")
    public ReorderController accepts(
            @WikiParameter(description = "<key:reorder.controller.param.types>")
            String types
    ) {
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
    @AiDoc(description = "Adds custom predicate validation rule for reorder payloads.")
    @WikiMethod(description = "<key:reorder.controller.method.acceptsValidator>")
    @WikiReturn("<key:reorder.controller.return.this>")
    public ReorderController accepts(
            @WikiParameter(description = "<key:reorder.controller.param.validator>")
            Predicate<DragPayload> validator
    ) {
        validators.add(Objects.requireNonNull(validator, "validator"));
        return this;
    }

    /**
     * Принимать только элементы этого контейнера (без внешних payload).
     */
    @AiDoc(description = "Restricts reordering to internal children of this container only.")
    @WikiMethod(description = "<key:reorder.controller.method.internalOnly>")
    @WikiReturn("<key:reorder.controller.return.this>")
    public ReorderController internalOnly(
            @WikiParameter(description = "<key:reorder.controller.param.internalOnly>")
            boolean internalOnly
    ) {
        this.internalOnly = internalOnly;
        return this;
    }

    /**
     * Применять перестановку к {@link PanelWidget} автоматически (по умолчанию да).
     */
    @AiDoc(description = "Enables or disables automatic repositioning of children when container is a PanelWidget.")
    @WikiMethod(description = "<key:reorder.controller.method.autoApply>")
    @WikiReturn("<key:reorder.controller.return.this>")
    public ReorderController autoApply(
            @WikiParameter(description = "<key:reorder.controller.param.autoApply>")
            boolean autoApply
    ) {
        this.autoApply = autoApply;
        return this;
    }

    /**
     * Какие дети считаются элементами списка (по умолчанию все, кроме COLLAPSED).
     */
    @AiDoc(description = "Configures a predicate filter selecting which child widgets participate in reordering.")
    @WikiMethod(description = "<key:reorder.controller.method.itemFilter>")
    @WikiReturn("<key:reorder.controller.return.this>")
    public ReorderController itemFilter(
            @WikiParameter(description = "<key:reorder.controller.param.itemFilter>")
            Predicate<Widget> itemFilter
    ) {
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
    @AiDoc(description = "Customizes appearance of the insertion line indicator.")
    @WikiMethod(description = "<key:reorder.controller.method.indicator>")
    @WikiReturn("<key:reorder.controller.return.this>")
    public ReorderController indicator(
            @WikiParameter(description = "<key:reorder.controller.param.color>")
            ColorView color,
            @WikiParameter(description = "<key:reorder.controller.param.thickness>")
            float thickness,
            @WikiParameter(description = "<key:reorder.controller.param.inset>")
            float inset
    ) {
        if (color != null) indicatorColor.set(color.r(), color.g(), color.b(), color.a());
        this.indicatorThickness = Float.isFinite(thickness) ? Math.max(1.0f, thickness) : DEFAULT_THICKNESS;
        this.indicatorInset = Float.isFinite(inset) ? Math.max(0.0f, inset) : 0.0f;
        return this;
    }

    @AiDoc(description = "Enables or disables this reorder controller.")
    @WikiMethod(description = "<key:reorder.controller.method.enabled>")
    @WikiReturn("<key:reorder.controller.return.this>")
    public ReorderController enabled(
            @WikiParameter(description = "<key:reorder.controller.param.enabled>")
            boolean enabled
    ) {
        this.enabled = enabled;
        return this;
    }

    /**
     * Слушатель перестановки с полным событием.
     */
    @AiDoc(description = "Registers a listener receiving full ReorderEvent details.")
    @WikiMethod(description = "<key:reorder.controller.method.onReorderEvent>")
    @WikiReturn("<key:reorder.controller.return.this>")
    public ReorderController onReorder(
            @WikiParameter(description = "<key:reorder.controller.param.eventListener>")
            Consumer<ReorderEvent> listener
    ) {
        listeners.add(Objects.requireNonNull(listener, "listener"));
        return this;
    }

    /**
     * Слушатель перестановки: {@code (fromIndex, toIndex)}. Для внешнего payload {@code fromIndex = -1}.
     */
    @AiDoc(description = "Registers a listener receiving indices: fromIndex and toIndex.")
    @WikiMethod(description = "<key:reorder.controller.method.onReorderIndices>")
    @WikiReturn("<key:reorder.controller.return.this>")
    public ReorderController onReorder(
            @WikiParameter(description = "<key:reorder.controller.param.indicesListener>")
            BiConsumer<Integer, Integer> listener
    ) {
        Objects.requireNonNull(listener, "listener");
        listeners.add(event -> listener.accept(event.fromIndex(), event.toIndex()));
        return this;
    }

    /**
     * @return контейнер
     */
    @AiDoc(description = "Returns the container widget managed by this controller.")
    @WikiMethod(description = "<key:reorder.controller.method.container>")
    @WikiReturn("<key:reorder.controller.return.container>")
    public Widget container() {
        return container;
    }

    /**
     * Снимает контроллер с контейнера.
     */
    @AiDoc(description = "Detaches event listeners from the container.")
    @WikiMethod(description = "<key:reorder.controller.method.detach>")
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

    private Widget itemOf(Widget descendant) {
        for (Widget current = descendant; current != null; current = current.parent()) {
            if (current.parent() == container) return current;
        }
        return null;
    }

    private int rawInsertionIndex(float pointerX, float pointerY) {
        List<Widget> children = container.children();
        int eligibleIndex = 0;
        for (int i = 0; i < children.size(); i++) {
            Widget child = children.get(i);
            if (!itemFilter.test(child)) continue;
            RectView bounds = child.layoutBounds();
            if (orientation == Orientation.VERTICAL) {
                float midY = bounds.y() + bounds.height() * 0.5f;
                if (pointerY < midY) return i;
            } else {
                float midX = bounds.x() + bounds.width() * 0.5f;
                if (pointerX < midX) return i;
            }
            eligibleIndex = i + 1;
        }
        return eligibleIndex;
    }

    private void showIndicator(DragSession session, int rawIndex) {
        RectView containerBounds = container.layoutBounds();
        List<Widget> children = container.children();
        List<Widget> eligible = new ArrayList<>();
        for (Widget child : children) {
            if (itemFilter.test(child)) eligible.add(child);
        }

        if (orientation == Orientation.VERTICAL) {
            float lineY;
            if (eligible.isEmpty()) {
                lineY = containerBounds.y() + containerBounds.height() * 0.5f;
            } else if (rawIndex <= 0) {
                lineY = eligible.get(0).layoutBounds().y();
            } else if (rawIndex >= eligible.size()) {
                lineY = eligible.get(eligible.size() - 1).layoutBounds().bottom();
            } else {
                RectView prev = eligible.get(rawIndex - 1).layoutBounds();
                RectView next = eligible.get(rawIndex).layoutBounds();
                lineY = (prev.bottom() + next.y()) * 0.5f;
            }
            float x = containerBounds.x() + indicatorInset;
            float width = Math.max(0.0f, containerBounds.width() - indicatorInset * 2.0f);
            session.showIndicator(x, lineY - indicatorThickness * 0.5f, width, indicatorThickness, indicatorColor);
        } else {
            float lineX;
            if (eligible.isEmpty()) {
                lineX = containerBounds.x() + containerBounds.width() * 0.5f;
            } else if (rawIndex <= 0) {
                lineX = eligible.get(0).layoutBounds().x();
            } else if (rawIndex >= eligible.size()) {
                lineX = eligible.get(eligible.size() - 1).layoutBounds().right();
            } else {
                RectView prev = eligible.get(rawIndex - 1).layoutBounds();
                RectView next = eligible.get(rawIndex).layoutBounds();
                lineX = (prev.right() + next.x()) * 0.5f;
            }
            float y = containerBounds.y() + indicatorInset;
            float height = Math.max(0.0f, containerBounds.height() - indicatorInset * 2.0f);
            session.showIndicator(lineX - indicatorThickness * 0.5f, y, indicatorThickness, height, indicatorColor);
        }
    }
}
