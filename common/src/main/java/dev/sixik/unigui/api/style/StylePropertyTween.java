package dev.sixik.unigui.api.style;

import dev.sixik.unigui.api.animation.TransitionSpec;

/**
 * Декларативный шаг анимации одного свойства стиля или виджета.
 *
 * <p>Описывает, какое свойство нужно изменить, от какого значения идти, к какому значению прийти
 * и каким переходом пользоваться. Значения хранятся строками, потому что свойства стиля имеют разные
 * типы: число, цвет, enum, id текстуры или будущий пользовательский формат. Runtime-слой выбирает
 * подходящий адаптер свойства и уже там преобразует строку в нужный тип.</p>
 *
 * @param propertyName id свойства, например {@link StyleAnimationIds.Property#SCALE}
 * @param fromValue стартовое значение или {@link #CURRENT}, если нужно взять текущее runtime-значение
 * @param toValue конечное значение; пустая строка допустима для адаптеров, где это имеет смысл
 * @param transition настройки длительности, easing и повторов
 */
public record StylePropertyTween(String propertyName,
                                 String fromValue,
                                 String toValue,
                                 TransitionSpec transition) {
    /** Маркер значения, которое берётся из текущего runtime-состояния свойства. */
    public static final String CURRENT = "current";

    /**
     * Создаёт tween от текущего runtime-значения к указанному значению.
     *
     * @param propertyName id свойства
     * @param toValue конечное значение
     * @param transition настройки перехода
     */
    public StylePropertyTween(String propertyName, String toValue, TransitionSpec transition) {
        this(propertyName, CURRENT, toValue, transition);
    }

    /** Нормализует имена и подставляет безопасные значения по умолчанию. */
    public StylePropertyTween {
        propertyName = normalizeRequired(propertyName, "propertyName");
        fromValue = normalizeOptional(fromValue, CURRENT);
        toValue = normalizeOptional(toValue, "");
        transition = transition == null ? TransitionSpec.DEFAULT : transition;
    }

    /**
     * Создаёт tween от текущего runtime-значения к указанному значению.
     *
     * @param propertyName id свойства
     * @param toValue конечное значение
     * @param transition настройки перехода
     * @return новый tween с {@link #CURRENT} в качестве стартовой точки
     */
    public static StylePropertyTween currentTo(String propertyName, String toValue, TransitionSpec transition) {
        return new StylePropertyTween(propertyName, CURRENT, toValue, transition);
    }

    private static String normalizeRequired(String value, String name) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(name + " cannot be empty");
        }
        return normalized;
    }

    private static String normalizeOptional(String value, String fallback) {
        String normalized = value == null ? "" : value.trim();
        return normalized.isEmpty() ? fallback : normalized;
    }
}
