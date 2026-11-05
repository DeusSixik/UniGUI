package dev.sixik.unigui.api.core;

/**
 * Фазы одного UI-кадра.
 *
 * <p>Фазы описывают порядок, в котором runtime обычно применяет отложенные изменения, input,
 * layout, animation и render. Они полезны для профайлера, debug overlay и систем, которым важно
 * понимать, в какой момент кадра выполняется код.</p>
 *
 * @see FrameContext#phase()
 */
public enum FramePhase {
    /** Начало кадра, подготовка счетчиков и временных snapshot'ов. */
    BEGIN_FRAME,

    /** Применение отложенных изменений дерева или состояния UI. */
    APPLY_MUTATIONS,

    /** Обработка mouse/keyboard/gamepad событий. */
    INPUT,

    /** Измерение и размещение виджетов. */
    LAYOUT,

    /** Обновление transition'ов, tweens и style-анимаций. */
    ANIMATION,

    /** Сбор draw-команд из дерева виджетов. */
    BUILD_DRAW_LIST,

    /** Группировка draw-команд для backend renderer'а. */
    BATCH,

    /** Передача подготовленных команд в render backend. */
    RENDER,

    /** Завершение кадра и сброс временного состояния. */
    END_FRAME
}