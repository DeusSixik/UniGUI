package dev.sixik.unigui.editor;

import dev.sixik.unigui.api.event.EventSubscription;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Шина событий редактора: панели общаются через неё, а не друг с другом напрямую.
 *
 * <p>Событием может быть любой объект. Слушатель подписывается на класс (или интерфейс) и получает события этого
 * класса и его наследников. Доставка синхронная, в потоке интерфейса; слушатели вызываются в порядке подписки.</p>
 */
public final class EditorEvents {
    private final Map<Class<?>, List<Consumer<Object>>> listeners = new LinkedHashMap<>();

    /**
     * Подписывается на события типа.
     *
     * @param type     класс или интерфейс события
     * @param listener получатель
     * @param <T>      тип события
     * @return подписка
     */
    @SuppressWarnings("unchecked")
    public <T> EventSubscription on(Class<T> type, Consumer<? super T> listener) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(listener, "listener");
        Consumer<Object> erased = (Consumer<Object>) listener;
        listeners.computeIfAbsent(type, key -> new ArrayList<>()).add(erased);
        return () -> {
            List<Consumer<Object>> list = listeners.get(type);
            if (list != null) list.remove(erased);
        };
    }

    /**
     * Рассылает событие.
     *
     * @param event событие
     */
    public void post(Object event) {
        Objects.requireNonNull(event, "event");
        // Снимок, чтобы слушатель мог подписываться и отписываться прямо во время доставки.
        List<Consumer<Object>> targets = new ArrayList<>();
        for (Map.Entry<Class<?>, List<Consumer<Object>>> entry : listeners.entrySet()) {
            if (entry.getKey().isInstance(event)) targets.addAll(entry.getValue());
        }
        for (Consumer<Object> target : targets) target.accept(event);
    }
}
