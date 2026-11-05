package dev.sixik.unigui.api.widget.render;

/**
 * Семантическая роль визуального виджета.
 *
 * <p>Роль описывает назначение виджета, а не форму его поверхности. Например, checkbox
 * может использовать прямоугольный фон, но от этого не становится обычной кнопкой.</p>
 */
public enum WidgetRole {
    /** Совместимый renderer без заявленной semantic role. */
    UNSPECIFIED,
    BUTTON,
    CHECKBOX,
    RADIO_BUTTON,
    TOGGLE_BUTTON,
    TOGGLE_SWITCH,
    TOOL_BUTTON,
    ICON_BUTTON,
    HOLD_BUTTON,
    SLIDER,
    TEXT_INPUT,
    PANEL,
    POPUP,
    CANVAS,
    DISPLAY;

    /**
     * Проверяет, совместима ли зарегистрированная роль с ожидаемой ролью.
     * Legacy renderer с {@link #UNSPECIFIED} принимается для переходной совместимости.
     */
    public boolean accepts(WidgetRole registeredRole) {
        return this == UNSPECIFIED
                || registeredRole == null
                || registeredRole == UNSPECIFIED
                || this == registeredRole;
    }
}
