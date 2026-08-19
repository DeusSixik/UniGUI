package dev.sixik.unigui.api.widget;

/**
 * Режим участия виджета в layout, render, input и focus pipeline.
 *
 * <p>Видимость здесь шире, чем простой boolean. Иногда контрол нужно скрыть визуально,
 * но оставить занимаемое место в layout, а иногда полностью убрать его из потока. Этот enum
 * фиксирует оба сценария и позволяет layout engine и input system принимать одинаковое решение.</p>
 *
 * @see Widget#visibility()
 * @see Widget#visible()
 */
public enum Visibility {
    /**
     * Виджет измеряется, размещается, рисуется и может получать input/focus.
     */
    VISIBLE,

    /**
     * Виджет сохраняет место в layout, но не рисуется и не принимает input/focus.
     */
    HIDDEN,

    /**
     * Виджет исключается из layout, не рисуется и не принимает input/focus.
     */
    COLLAPSED
}