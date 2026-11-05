package dev.sixik.unigui.api.style;

/**
 * Форма метки внутри включённого {@code Checkbox}.
 *
 * <p>Задаётся ключом {@link StyleKeys#CHECKBOX_MARK_SHAPE}; цвет метки задаёт
 * {@link StyleKeys#CHECKBOX_MARK_COLOR}. Неопределённое состояние всегда рисуется чёрточкой.</p>
 */
public enum CheckboxMarkShape {
    /**
     * Круглая точка по центру; так флажок рисовался всегда до появления ключа.
     */
    DOT,
    /**
     * Квадрат по центру.
     */
    SQUARE,
    /**
     * Галочка из двух отрезков.
     */
    CHECK,
    /**
     * Крестик из двух диагоналей.
     */
    CROSS
}
