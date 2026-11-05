package dev.sixik.unigui.api.animation;

/** Primitive-интерполятор для int без boxing на каждом кадре. */
@FunctionalInterface
public interface IntInterpolator {
    /** Линейная интерполяция с округлением до ближайшего целого. */
    IntInterpolator LINEAR = IntInterpolator::linear;

    /** Интерполирует два целых значения по прогрессу 0..1. */
    int interpolate(int start, int end, float progress);

    private static int linear(int start, int end, float progress) {
        long delta = (long) end - start;
        return (int) (start + Math.round(delta * (double) progress));
    }
}
