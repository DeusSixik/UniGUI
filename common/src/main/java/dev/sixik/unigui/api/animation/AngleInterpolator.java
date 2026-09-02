package dev.sixik.unigui.api.animation;

/** Primitive-интерполятор углов по кратчайшему пути. */
public final class AngleInterpolator implements FloatInterpolator {
    /** Общий экземпляр. */
    public static final AngleInterpolator SHORTEST_PATH = new AngleInterpolator();
    private AngleInterpolator() { }
    @Override
    public float interpolate(float start, float end, float progress) {
        return start + wrapDegrees(end - start) * Easing.clamp01(progress);
    }
    /** Приводит угол к диапазону [-180, 180). */
    public static float wrapDegrees(float degrees) {
        if (!Float.isFinite(degrees)) return 0.0f;
        float wrapped = degrees % 360.0f;
        if (wrapped >= 180.0f) wrapped -= 360.0f;
        if (wrapped < -180.0f) wrapped += 360.0f;
        return wrapped;
    }
}
