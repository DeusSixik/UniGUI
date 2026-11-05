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
import dev.sixik.unigui.api.widget.Widget;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Drop-зона, которая вешается на любой виджет композицией.
 *
 * <pre>{@code
 * DropController.attach(panel)
 *         .accepts("unity.component")
 *         .onDragEnter(event -> panel.background(HIGHLIGHT))
 *         .onDragLeave(event -> panel.background(NORMAL))
 *         .onDrop(event -> addComponent(event.payload().data(Component.class)));
 * }</pre>
 *
 * <p>Зона реагирует на {@link DragOverEvent}: если payload подходит, она объявляет себя целью и
 * останавливает всплытие, поэтому из вложенных зон выигрывает самая близкая к указателю. Типы
 * принимаются списком через запятую, {@code |} или пробел; {@code *} принимает всё.</p>
 */
@AiDoc(summary = "Drop target controller attachable to any widget via composition to filter payloads and handle drag enter, over, leave, and drop events."
)
@WikiPage(
        title = "DropController",
        slug = "drop-controller",
        category = "<key:dnd.category>",
        summary = "<key:drop.controller.summary>"
)
@WikiSection(
        title = "<key:wiki.section.overview.title>",
        markdown = "<key:drop.controller.section.overview.body>",
        order = 10
)
@WikiExample(
        title = "<key:drop.controller.example.attach.title>",
        code = "DropController.attach(targetPanel)\n"
                + "        .accepts(\"card\")\n"
                + "        .onDrop(event -> System.out.println(\"Dropped: \" + event.payload().id()));"
)
@WikiLink(
        label = "DragController",
        target = "page:drag-controller",
        description = "<key:drag.controller.summary>"
)
@WikiLink(
        label = "DragSession",
        target = "page:drag-session",
        description = "<key:drag.session.summary>"
)
public final class DropController {
    private final Widget zone;
    private final List<EventSubscription> subscriptions = new ArrayList<>(3);
    private final Set<String> acceptedTypes = new LinkedHashSet<>();
    private final List<Predicate<DragPayload>> validators = new ArrayList<>();
    private final List<Consumer<DragOverEvent>> enterListeners = new ArrayList<>();
    private final List<Consumer<DragOverEvent>> overListeners = new ArrayList<>();
    private final List<Consumer<DragLeaveEvent>> leaveListeners = new ArrayList<>();
    private final List<Consumer<DragDropEvent>> dropListeners = new ArrayList<>();
    private boolean acceptAll = true;
    private boolean enabled = true;
    private boolean hovering;

    private DropController(Widget zone) {
        this.zone = Objects.requireNonNull(zone, "zone");
        subscriptions.add(zone.on(DragOverEvent.TYPE, this::onOver));
        subscriptions.add(zone.on(DragLeaveEvent.TYPE, this::onLeave));
        subscriptions.add(zone.on(DragDropEvent.TYPE, this::onDrop));
    }

    /**
     * Подключает drop-зону к виджету.
     *
     * @param zone виджет, который принимает payload
     * @return контроллер для fluent-настройки
     */
    @AiDoc(summary = "Attaches a DropController to the specified widget zone.")
    @WikiMethod(summary = "<key:drop.controller.method.attach>")
    @WikiReturn("<key:drop.controller.return.controller>")
    public static DropController attach(
            @WikiParameter(description = "<key:drop.controller.param.zone>")
            Widget zone
    ) {
        return new DropController(zone);
    }

    /**
     * Ограничивает принимаемые типы payload.
     *
     * @param types типы через запятую, {@code |} или пробел; пусто или {@code *} принимает всё
     */
    @AiDoc(summary = "Restricts accepted payload types by comma/pipe/whitespace delimited string (* accepts all).")
    @WikiMethod(summary = "<key:drop.controller.method.acceptsTypes>")
    @WikiReturn("<key:drop.controller.return.this>")
    public DropController accepts(
            @WikiParameter(description = "<key:drop.controller.param.types>")
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
     * Добавляет дополнительную проверку payload: зона принимает, только если все проверки истинны.
     */
    @AiDoc(summary = "Adds a custom predicate validation rule for candidate payloads.")
    @WikiMethod(summary = "<key:drop.controller.method.acceptsValidator>")
    @WikiReturn("<key:drop.controller.return.this>")
    public DropController accepts(
            @WikiParameter(description = "<key:drop.controller.param.validator>")
            Predicate<DragPayload> validator
    ) {
        validators.add(Objects.requireNonNull(validator, "validator"));
        return this;
    }

    @AiDoc(summary = "Enables or disables this drop controller.")
    @WikiMethod(summary = "<key:drop.controller.method.enabled>")
    @WikiReturn("<key:drop.controller.return.this>")
    public DropController enabled(
            @WikiParameter(description = "<key:drop.controller.param.enabled>")
            boolean enabled
    ) {
        this.enabled = enabled;
        return this;
    }

    /**
     * Указатель с подходящим payload вошёл в зону.
     */
    @AiDoc(summary = "Registers a listener called when an accepted payload enters the drop zone.")
    @WikiMethod(summary = "<key:drop.controller.method.onDragEnter>")
    @WikiReturn("<key:drop.controller.return.this>")
    public DropController onDragEnter(
            @WikiParameter(description = "<key:drop.controller.param.enterListener>")
            Consumer<DragOverEvent> listener
    ) {
        enterListeners.add(Objects.requireNonNull(listener, "listener"));
        return this;
    }

    /**
     * Указатель движется над зоной (каждое движение и каждый тик).
     */
    @AiDoc(summary = "Registers a listener called on each movement and frame tick over the drop zone.")
    @WikiMethod(summary = "<key:drop.controller.method.onDragOver>")
    @WikiReturn("<key:drop.controller.return.this>")
    public DropController onDragOver(
            @WikiParameter(description = "<key:drop.controller.param.overListener>")
            Consumer<DragOverEvent> listener
    ) {
        overListeners.add(Objects.requireNonNull(listener, "listener"));
        return this;
    }

    /**
     * Указатель покинул зону или жест отменён.
     */
    @AiDoc(summary = "Registers a listener called when the drag pointer leaves the drop zone or is cancelled.")
    @WikiMethod(summary = "<key:drop.controller.method.onDragLeave>")
    @WikiReturn("<key:drop.controller.return.this>")
    public DropController onDragLeave(
            @WikiParameter(description = "<key:drop.controller.param.leaveListener>")
            Consumer<DragLeaveEvent> listener
    ) {
        leaveListeners.add(Objects.requireNonNull(listener, "listener"));
        return this;
    }

    /**
     * Payload отпущен над зоной.
     */
    @AiDoc(summary = "Registers a listener called when an accepted payload is dropped over the zone.")
    @WikiMethod(summary = "<key:drop.controller.method.onDrop>")
    @WikiReturn("<key:drop.controller.return.this>")
    public DropController onDrop(
            @WikiParameter(description = "<key:drop.controller.param.dropListener>")
            Consumer<DragDropEvent> listener
    ) {
        dropListeners.add(Objects.requireNonNull(listener, "listener"));
        return this;
    }

    /**
     * @return виджет зоны
     */
    @AiDoc(summary = "Returns the host drop zone widget.")
    @WikiMethod(summary = "<key:drop.controller.method.zone>")
    @WikiReturn("<key:drop.controller.return.zone>")
    public Widget zone() {
        return zone;
    }

    /**
     * @return {@code true}, пока над зоной находится подходящий payload
     */
    @AiDoc(summary = "Returns true while an accepted drag payload is hovering over the zone.")
    @WikiMethod(summary = "<key:drop.controller.method.hovering>")
    @WikiReturn("<key:drop.controller.return.hovering>")
    public boolean hovering() {
        return hovering;
    }

    /**
     * Проверяет, принимает ли зона payload.
     */
    @AiDoc(summary = "Tests whether the specified payload matches type and validator requirements.")
    @WikiMethod(summary = "<key:drop.controller.method.acceptsPayload>")
    @WikiReturn("<key:drop.controller.return.acceptsPayload>")
    public boolean accepts(
            @WikiParameter(description = "<key:drop.controller.param.payload>")
            DragPayload payload
    ) {
        if (!enabled || payload == null || !zone.enabled()) return false;
        if (!acceptAll && !acceptedTypes.contains(payload.type())) return false;
        for (Predicate<DragPayload> validator : validators) {
            if (!validator.test(payload)) return false;
        }
        return true;
    }

    /**
     * Снимает контроллер с виджета.
     */
    @AiDoc(summary = "Detaches event listeners from the host drop zone widget.")
    @WikiMethod(summary = "<key:drop.controller.method.detach>")
    public void detach() {
        for (EventSubscription subscription : subscriptions) subscription.unsubscribe();
        subscriptions.clear();
        hovering = false;
    }

    private void onOver(DragOverEvent event) {
        if (event.phase() == EventPhase.CAPTURE || event.isCancelled()) return;
        if (!accepts(event.payload())) return;
        if (!hovering) {
            hovering = true;
            for (Consumer<DragOverEvent> listener : List.copyOf(enterListeners)) listener.accept(event);
        }
        for (Consumer<DragOverEvent> listener : List.copyOf(overListeners)) listener.accept(event);
        event.accept(zone);
    }

    private void onLeave(DragLeaveEvent event) {
        if (event.phase() == EventPhase.CAPTURE || !hovering) return;
        hovering = false;
        for (Consumer<DragLeaveEvent> listener : List.copyOf(leaveListeners)) listener.accept(event);
    }

    private void onDrop(DragDropEvent event) {
        if (event.phase() == EventPhase.CAPTURE) return;
        if (hovering) {
            // Перед drop зона «выходит из hover»: слушатели leave снимают подсветку.
            hovering = false;
            DragLeaveEvent leave = new DragLeaveEvent(zone, event.session());
            for (Consumer<DragLeaveEvent> listener : List.copyOf(leaveListeners)) listener.accept(leave);
        }
        if (!accepts(event.payload())) return;
        for (Consumer<DragDropEvent> listener : List.copyOf(dropListeners)) listener.accept(event);
    }
}
