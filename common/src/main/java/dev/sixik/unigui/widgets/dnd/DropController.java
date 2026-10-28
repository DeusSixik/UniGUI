package dev.sixik.unigui.widgets.dnd;

import dev.sixik.unigui.api.event.EventPhase;
import dev.sixik.unigui.api.event.EventSubscription;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.widgets.editor.DragPayload;

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
    public static DropController attach(Widget zone) {
        return new DropController(zone);
    }

    /**
     * Ограничивает принимаемые типы payload.
     *
     * @param types типы через запятую, {@code |} или пробел; пусто или {@code *} принимает всё
     */
    public DropController accepts(String types) {
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
    public DropController accepts(Predicate<DragPayload> validator) {
        validators.add(Objects.requireNonNull(validator, "validator"));
        return this;
    }

    public DropController enabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    /**
     * Указатель с подходящим payload вошёл в зону.
     */
    public DropController onDragEnter(Consumer<DragOverEvent> listener) {
        enterListeners.add(Objects.requireNonNull(listener, "listener"));
        return this;
    }

    /**
     * Указатель движется над зоной (каждое движение и каждый тик).
     */
    public DropController onDragOver(Consumer<DragOverEvent> listener) {
        overListeners.add(Objects.requireNonNull(listener, "listener"));
        return this;
    }

    /**
     * Указатель покинул зону или жест отменён.
     */
    public DropController onDragLeave(Consumer<DragLeaveEvent> listener) {
        leaveListeners.add(Objects.requireNonNull(listener, "listener"));
        return this;
    }

    /**
     * Payload отпущен над зоной.
     */
    public DropController onDrop(Consumer<DragDropEvent> listener) {
        dropListeners.add(Objects.requireNonNull(listener, "listener"));
        return this;
    }

    /**
     * @return виджет зоны
     */
    public Widget zone() {
        return zone;
    }

    /**
     * @return {@code true}, пока над зоной находится подходящий payload
     */
    public boolean hovering() {
        return hovering;
    }

    /**
     * Проверяет, принимает ли зона payload.
     */
    public boolean accepts(DragPayload payload) {
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
