package dev.sixik.unigui.api.style;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Изменяемая реализация {@link Style}.
 *
 * <p>Класс используется в Java builders, XML loader'е и editor tooling, где стиль нужно собирать
 * постепенно: добавлять свойства, state override'ы и renderer override. Все значения проверяются
 * по типу через {@link StyleKey#type()}, поэтому ошибка в значении обнаруживается в момент записи,
 * а не во время рендера виджета.</p>
 *
 * @see StyleKey
 * @see WidgetState
 */
public final class MutableStyle implements Style {
    private final EnumMap<WidgetState, Map<StyleKey<?>, Object>> values = new EnumMap<>(WidgetState.class);
    private long version;

    /**
     * Записывает значение свойства для обычного состояния {@link WidgetState#NORMAL}.
     *
     * @param key типизированный ключ свойства
     * @param value значение свойства или {@code null}
     * @return этот стиль для fluent-настройки
     * @param <T> Java-тип свойства
     */
    public <T> MutableStyle put(StyleKey<T> key, T value) {
        return put(key, WidgetState.NORMAL, value);
    }

    /**
     * Записывает значение свойства для конкретного состояния виджета.
     *
     * @param key типизированный ключ свойства
     * @param state состояние, для которого действует значение
     * @param value значение свойства или {@code null}
     * @return этот стиль для fluent-настройки
     * @param <T> Java-тип свойства
     */
    public <T> MutableStyle put(StyleKey<T> key, WidgetState state, T value) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(state, "state");
        if (value != null && !key.type().isInstance(value)) {
            throw new IllegalArgumentException("Style value for " + key.id() + " must be " + key.type().getName());
        }
        values.computeIfAbsent(state, ignored -> new HashMap<>()).put(key, value);
        version++;
        return this;
    }

    /**
     * Удаляет значение свойства из обычного состояния.
     *
     * @param key ключ свойства
     * @return этот стиль для fluent-настройки
     */
    public MutableStyle remove(StyleKey<?> key) {
        return remove(key, WidgetState.NORMAL);
    }

    /**
     * Удаляет значение свойства из конкретного состояния.
     *
     * @param key ключ свойства
     * @param state состояние, из которого нужно удалить значение
     * @return этот стиль для fluent-настройки
     */
    public MutableStyle remove(StyleKey<?> key, WidgetState state) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(state, "state");
        Map<StyleKey<?>, Object> stateValues = values.get(state);
        if (stateValues == null || !stateValues.containsKey(key)) return this;
        stateValues.remove(key);
        if (stateValues.isEmpty()) {
            values.remove(state);
        }
        version++;
        return this;
    }

    /**
     * Задаёт renderer override для виджетов, использующих этот стиль.
     *
     * <p>Конкретный виджет приводит значение к своему renderer-интерфейсу. Локальный renderer,
     * назначенный прямо на instance виджета, остаётся приоритетнее этого style-значения.</p>
     *
     * @param renderer renderer-объект, совместимый с конкретным типом виджета
     * @return этот стиль для fluent-настройки
     */
    public MutableStyle renderer(Object renderer) {
        return put(StyleKeys.RENDERER, renderer);
    }

    /**
     * Задаёт renderer override по id из {@link dev.sixik.unigui.api.widget.render.WidgetRendererRegistry}.
     *
     * <p>Этот вариант нужен для декларативных StylePack/XML стилей, где нельзя хранить Java-объект,
     * но можно сослаться на renderer по стабильной строке.</p>
     *
     * @param rendererId id renderer'а в registry
     * @return этот стиль для fluent-настройки
     */
    public MutableStyle rendererId(String rendererId) {
        return put(StyleKeys.RENDERER, rendererId == null ? "" : rendererId.trim());
    }

    @Override
    public long version() {
        return version;
    }

    @Override
    public <T> T get(StyleKey<T> key, WidgetState state, T fallback) {
        Objects.requireNonNull(key, "key");
        WidgetState normalizedState = state == null ? WidgetState.NORMAL : state;
        T value = lookup(key, normalizedState);
        if (value != null || normalizedState == WidgetState.NORMAL) {
            return value == null ? fallback : value;
        }
        value = lookup(key, WidgetState.NORMAL);
        return value == null ? fallback : value;
    }

    @Override
    public Map<WidgetState, Map<StyleKey<?>, Object>> values() {
        if (values.isEmpty()) return Map.of();
        EnumMap<WidgetState, Map<StyleKey<?>, Object>> snapshot = new EnumMap<>(WidgetState.class);
        for (Map.Entry<WidgetState, Map<StyleKey<?>, Object>> entry : values.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                snapshot.put(entry.getKey(), Map.copyOf(entry.getValue()));
            }
        }
        return Collections.unmodifiableMap(snapshot);
    }

    private <T> T lookup(StyleKey<T> key, WidgetState state) {
        Map<StyleKey<?>, Object> stateValues = values.get(state);
        if (stateValues == null) return null;
        Object value = stateValues.get(key);
        if (value == null) return null;
        return key.type().cast(value);
    }
}