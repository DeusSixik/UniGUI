package dev.sixik.unigui.api.animation;

/**
 * Именованные pivot-позиции для transform-анимаций виджета.
 *
 * <p>Origin определяет точку, вокруг которой применяются scale и rotation. Значения
 * {@link #relativeX()} и {@link #relativeY()} задаются относительно layout bounds виджета:
 * {@code 0.0} — левая/верхняя сторона, {@code 0.5} — центр, {@code 1.0} — правая/нижняя сторона.</p>
 *
 * <p>{@link #CUSTOM} не пересчитывает pivot автоматически. Он нужен, когда код вручную задаёт
 * или анимирует pivot в пикселях через {@link AnimatedProperty#PIVOT_X} и
 * {@link AnimatedProperty#PIVOT_Y}.</p>
 *
 * <pre>{@code
 * button.transformOrigin(TransformOrigin.CENTER);
 * button.animateScale(1.08f, 1.08f, TransitionSpec.of(0.16f));
 * }</pre>
 *
 * @see AnimatedProperty#PIVOT_X
 * @see AnimatedProperty#PIVOT_Y
 */
public enum TransformOrigin {
    /** Не менять текущий pivot: координаты управляются кодом или отдельной анимацией. */
    CUSTOM(Float.NaN, Float.NaN),
    /** Левый верхний угол. */
    LEFT_TOP(0.0f, 0.0f),
    /** Центр верхней стороны. */
    TOP(0.5f, 0.0f),
    /** Правый верхний угол. */
    RIGHT_TOP(1.0f, 0.0f),
    /** Центр левой стороны. */
    LEFT_CENTER(0.0f, 0.5f),
    /** Центр виджета. */
    CENTER(0.5f, 0.5f),
    /** Центр правой стороны. */
    RIGHT_CENTER(1.0f, 0.5f),
    /** Левый нижний угол. */
    LEFT_BOTTOM(0.0f, 1.0f),
    /** Центр нижней стороны. */
    BOTTOM(0.5f, 1.0f),
    /** Правый нижний угол. */
    RIGHT_BOTTOM(1.0f, 1.0f);

    private final float relativeX;
    private final float relativeY;

    TransformOrigin(float relativeX, float relativeY) {
        this.relativeX = relativeX;
        this.relativeY = relativeY;
    }

    /**
     * Возвращает относительную X-позицию pivot.
     *
     * @return 0..1 для предустановленных origin'ов или {@link Float#NaN} для {@link #CUSTOM}
     */
    public float relativeX() {
        return relativeX;
    }

    /**
     * Возвращает относительную Y-позицию pivot.
     *
     * @return 0..1 для предустановленных origin'ов или {@link Float#NaN} для {@link #CUSTOM}
     */
    public float relativeY() {
        return relativeY;
    }

    /**
     * Проверяет, оставляет ли origin текущий custom pivot без автоматического пересчёта.
     *
     * @return {@code true} только для {@link #CUSTOM}
     */
    public boolean custom() {
        return this == CUSTOM;
    }
}