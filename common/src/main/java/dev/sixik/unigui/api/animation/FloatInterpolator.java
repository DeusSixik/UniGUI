package dev.sixik.unigui.api.animation;

/** Primitive-интерполятор для float без boxing на кадре. */
@FunctionalInterface
public interface FloatInterpolator {
    /** Линейная интерполяция. */
    FloatInterpolator LINEAR = (start, end, progress) -> start + (end - start) * progress;
    /** Интерполирует два числа. */
    float interpolate(float start, float end, float progress);
}
