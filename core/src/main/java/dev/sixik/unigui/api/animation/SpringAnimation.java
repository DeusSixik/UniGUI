package dev.sixik.unigui.api.animation;

/**
 * Пружинная анимация одного float-значения.
 *
 * <p>Переход к новой цели сохраняет текущую скорость. Аналитическое решение не зависит от
 * размера шага и не создаёт временных объектов на кадре.</p>
 */
public final class SpringAnimation implements PlayableAnimation {
    private static final float POSITION_EPSILON = 0.001f;
    private static final float VELOCITY_EPSILON = 0.001f;

    private FloatValueWriter writer;
    private final float stiffness;
    private final float damping;
    private float value;
    private float target;
    private float velocity;
    private boolean settled;
    private boolean cancelled;

    /** Создаёт пружину. */
    public SpringAnimation(float start, float target, float stiffness, float damping,
                           FloatValueWriter writer) {
        this.value = sanitize(start);
        this.target = sanitize(target);
        this.stiffness = positive(stiffness, 170.0f);
        this.damping = positive(damping, 24.0f);
        this.writer = writer;
        this.settled = nearlyEqual(value, this.target);
        write();
    }

    /** @return текущее значение */
    public float value() { return value; }

    /** @return текущая цель */
    public float target() { return target; }

    /** @return текущая скорость */
    public float velocity() { return velocity; }

    /** @return жёсткость этой пружины */
    public float stiffness() { return stiffness; }

    /** @return затухание этой пружины */
    public float damping() { return damping; }

    /** Проверяет, подходят ли параметры для переиспользования объекта. */
    public boolean matches(float requestedStiffness, float requestedDamping) {
        return stiffness == positive(requestedStiffness, 170.0f)
                && damping == positive(requestedDamping, 24.0f);
    }

    /** Меняет цель с сохранением текущей позиции и скорости. */
    public SpringAnimation retarget(float newTarget) {
        target = sanitize(newTarget);
        cancelled = false;
        settled = false;
        return this;
    }

    /** Меняет цель и writer при переиспользовании spring для другого владельца значения. */
    public SpringAnimation retarget(float newTarget, FloatValueWriter newWriter) {
        writer = newWriter;
        return retarget(newTarget);
    }

    @Override
    public void update(float deltaSeconds) {
        if (cancelled || settled) return;
        float delta = AnimationClock.sanitizeDelta(deltaSeconds);
        if (delta <= 0.0f) return;

        float displacement = value - target;
        float frequency = (float) Math.sqrt(stiffness);
        float ratio = damping / (2.0f * frequency);
        if (ratio < 1.0f) {
            float damped = frequency * (float) Math.sqrt(1.0f - ratio * ratio);
            float angle = damped * delta;
            float exponential = (float) Math.exp(-ratio * frequency * delta);
            float sine = (float) Math.sin(angle);
            float cosine = (float) Math.cos(angle);
            float coefficient = (velocity + ratio * frequency * displacement) / damped;
            value = target + exponential * (displacement * cosine + coefficient * sine);
            velocity = exponential * (velocity * (cosine - ratio * frequency / damped * sine)
                    - displacement * (frequency * frequency / damped) * sine);
        } else if (Math.abs(ratio - 1.0f) < 0.0001f) {
            float exponential = (float) Math.exp(-frequency * delta);
            float coefficient = velocity + frequency * displacement;
            value = target + exponential * (displacement + coefficient * delta);
            velocity = exponential * (velocity - frequency * coefficient * delta);
        } else {
            float root = (float) Math.sqrt(ratio * ratio - 1.0f);
            float rootA = -frequency * (ratio - root);
            float rootB = -frequency * (ratio + root);
            float coefficientB = (velocity - rootA * displacement) / (rootB - rootA);
            float coefficientA = displacement - coefficientB;
            float exponentialA = (float) Math.exp(rootA * delta);
            float exponentialB = (float) Math.exp(rootB * delta);
            value = target + coefficientA * exponentialA + coefficientB * exponentialB;
            velocity = coefficientA * rootA * exponentialA + coefficientB * rootB * exponentialB;
        }
        if (!Float.isFinite(value) || !Float.isFinite(velocity)) {
            value = target;
            velocity = 0.0f;
        }
        if (Math.abs(value - target) <= POSITION_EPSILON && Math.abs(velocity) <= VELOCITY_EPSILON) {
            value = target;
            velocity = 0.0f;
            settled = true;
        }
        write();
    }

    @Override
    public boolean isFinished() { return cancelled || settled; }

    @Override
    public void cancel() { cancelled = true; }

    private void write() { if (writer != null) writer.set(value); }
    private static boolean nearlyEqual(float a, float b) { return Math.abs(a - b) <= POSITION_EPSILON; }
    private static float sanitize(float value) { return Float.isFinite(value) ? value : 0.0f; }
    private static float positive(float value, float fallback) {
        return Float.isFinite(value) && value > 0.0f ? value : fallback;
    }
}
