package dev.sixik.unigui.api.style;

import java.util.Map;

/**
 * Неизменяемое представление набора style-свойств виджета.
 *
 * <p>{@code Style} хранит значения по типизированным ключам {@link StyleKey} и состояниям
 * {@link WidgetState}. Один и тот же ключ может иметь обычное значение для {@link WidgetState#NORMAL}
 * и override для состояний вроде {@link WidgetState#HOVERED} или {@link WidgetState#PRESSED}.</p>
 *
 * <p>Виджет обычно не создаёт {@code Style} напрямую. Он получает его из {@link Theme},
 * {@link StylePack} или локального style-scope, а затем читает нужные значения через
 * {@link #get(StyleKey, WidgetState, Object)}.</p>
 *
 * <pre>{@code
 * ColorView color = style.get(StyleKeys.TEXT_COLOR, WidgetState.HOVERED, fallbackColor);
 * }</pre>
 *
 * @see MutableStyle
 * @see ResolvedStyle
 * @see StylePack
 */
public interface Style {
    /** Пустой стиль, который всегда возвращает переданный fallback. */
    Style EMPTY = new Style() {
    };

    /**
     * Возвращает версию стиля для кэшей и invalidation.
     *
     * <p>Версия должна меняться при любом изменении значений. Неизменяемые стили могут
     * возвращать {@code 0}.</p>
     *
     * @return монотонный или агрегированный номер версии
     */
    default long version() {
        return 0L;
    }

    /**
     * Возвращает значение свойства для указанного состояния.
     *
     * <p>Реализация может сама делать fallback на {@link WidgetState#NORMAL}. Если значение
     * не найдено, возвращается {@code fallback}.</p>
     *
     * @param key типизированный ключ свойства
     * @param state состояние виджета; {@code null} трактуется как {@link WidgetState#NORMAL}
     * @param fallback значение по умолчанию
     * @return найденное значение или {@code fallback}
     * @param <T> Java-тип свойства
     */
    default <T> T get(StyleKey<T> key, WidgetState state, T fallback) {
        return fallback;
    }

    /**
     * Возвращает снимок всех значений, сгруппированных по состояниям.
     *
     * <p>Метод нужен редактору, сериализации и отладочным инспекторам. Возвращаемые карты
     * должны рассматриваться как read-only снимок.</p>
     *
     * @return значения стиля по состояниям
     */
    default Map<WidgetState, Map<StyleKey<?>, Object>> values() {
        return Map.of();
    }

    /**
     * Возвращает значения только для одного состояния.
     *
     * @param state состояние виджета; {@code null} трактуется как {@link WidgetState#NORMAL}
     * @return read-only карта значений или пустая карта
     */
    default Map<StyleKey<?>, Object> values(WidgetState state) {
        WidgetState normalized = state == null ? WidgetState.NORMAL : state;
        Map<StyleKey<?>, Object> values = values().get(normalized);
        return values == null ? Map.of() : values;
    }
}