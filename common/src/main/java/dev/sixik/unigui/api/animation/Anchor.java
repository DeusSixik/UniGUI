package dev.sixik.unigui.api.animation;

import dev.sixik.unigui.api.math.MutableVec2;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.math.Vec2View;

/**
 * Именованная точка внутри прямоугольных границ.
 *
 * <p>Координаты anchor задаются относительно bounds: {@code 0.0} соответствует левой или верхней
 * стороне, {@code 0.5} — центру, {@code 1.0} — правой или нижней стороне. В отличие от
 * {@link TransformOrigin}, этот тип не меняет состояние виджета и подходит для расчёта pivot,
 * точки привязки popup или начальной точки эффекта.</p>
 *
 * <p>Для разового расчёта используйте {@link #resolve(RectView)}. В горячем пути передавайте
 * заранее созданный {@link MutableVec2} в {@link #resolve(RectView, MutableVec2)}, чтобы избежать
 * аллокации.</p>
 */
public enum Anchor {
    /** Левый верхний угол. */
    LEFT_TOP(0.0f, 0.0f),
    /** Центр верхней стороны. */
    TOP(0.5f, 0.0f),
    /** Правый верхний угол. */
    RIGHT_TOP(1.0f, 0.0f),
    /** Центр левой стороны. */
    LEFT_CENTER(0.0f, 0.5f),
    /** Центр прямоугольника. */
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

    Anchor(float relativeX, float relativeY) {
        this.relativeX = relativeX;
        this.relativeY = relativeY;
    }

    /** @return относительная координата по X в диапазоне 0..1 */
    public float relativeX() { return relativeX; }

    /** @return относительная координата по Y в диапазоне 0..1 */
    public float relativeY() { return relativeY; }

    /**
     * Рассчитывает абсолютную точку внутри bounds.
     *
     * @param bounds прямоугольные границы
     * @return новая точка с абсолютными координатами
     */
    public Vec2View resolve(RectView bounds) {
        return resolve(bounds, new MutableVec2());
    }

    /**
     * Рассчитывает абсолютную точку без создания нового объекта.
     *
     * @param bounds прямоугольные границы
     * @param result объект, в который будет записан результат
     * @return переданный {@code result}
     */
    public MutableVec2 resolve(RectView bounds, MutableVec2 result) {
        if (bounds == null) throw new IllegalArgumentException("bounds must not be null");
        if (result == null) throw new IllegalArgumentException("result must not be null");
        return result.set(
                bounds.x() + bounds.width() * relativeX,
                bounds.y() + bounds.height() * relativeY);
    }
}