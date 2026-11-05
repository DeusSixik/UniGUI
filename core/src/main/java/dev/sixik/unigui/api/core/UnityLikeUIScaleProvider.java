package dev.sixik.unigui.api.core;

/**
 * Scale provider в стиле Unity Canvas Scaler.
 *
 * <p>Provider сравнивает текущий viewport с reference resolution и рассчитывает непрерывный scale.
 * Параметр {@link #match(float)} выбирает, что важнее: ширина, высота или баланс между ними.
 * Такой подход полезен для UI, который должен выглядеть одинаково на 1920x1080, 2560x1440 и других
 * разрешениях без ручной подгонки размеров текста, scissor и spacing.</p>
 *
 * <pre>{@code
 * UnityLikeUIScaleProvider scale = new UnityLikeUIScaleProvider()
 *         .referenceResolution(1920.0f, 1080.0f)
 *         .matchHeight()
 *         .scaleRange(0.75f, 2.0f);
 * scale.viewportSize(framebufferWidth, framebufferHeight);
 * }</pre>
 *
 * @see UIScaleProvider
 */
public final class UnityLikeUIScaleProvider implements UIScaleProvider {
    /** Стандартная reference width для full-HD UI макетов. */
    public static final float DEFAULT_REFERENCE_WIDTH = 1920.0f;

    /** Стандартная reference height для full-HD UI макетов. */
    public static final float DEFAULT_REFERENCE_HEIGHT = 1080.0f;

    private float referenceWidth = DEFAULT_REFERENCE_WIDTH;
    private float referenceHeight = DEFAULT_REFERENCE_HEIGHT;

    private float viewportWidth = DEFAULT_REFERENCE_WIDTH;
    private float viewportHeight = DEFAULT_REFERENCE_HEIGHT;

    /**
     * 0.0 - match width, 1.0 - match height, 0.5 - balanced.
     */
    private float match = 1.0f;

    private float userScale = 1.0f;
    private float minScale = 0.75f;
    private float maxScale = 2.5f;

    /**
     * Рассчитывает scale относительно reference resolution.
     *
     * @return scale, ограниченный диапазоном {@link #minScale()}..{@link #maxScale()}
     */
    @Override
    public float scale() {
        float scaleX = viewportWidth / referenceWidth;
        float scaleY = viewportHeight / referenceHeight;

        float logX = (float) (Math.log(scaleX) / Math.log(2.0));
        float logY = (float) (Math.log(scaleY) / Math.log(2.0));
        float matchedScale = (float) Math.pow(2.0, lerp(logX, logY, match));

        return clamp(matchedScale * userScale, minScale, maxScale);
    }

    /**
     * Задаёт текущий размер viewport'а.
     *
     * @param width ширина viewport'а в backend pixels
     * @param height высота viewport'а в backend pixels
     * @return этот provider для fluent-настройки
     */
    public UnityLikeUIScaleProvider viewport(float width, float height) {
        this.viewportWidth = sanitizeSize(width, referenceWidth);
        this.viewportHeight = sanitizeSize(height, referenceHeight);
        return this;
    }

    /**
     * Задаёт reference resolution, под которое проектировался UI.
     *
     * @param width reference width в backend pixels
     * @param height reference height в backend pixels
     * @return этот provider для fluent-настройки
     */
    public UnityLikeUIScaleProvider referenceResolution(float width, float height) {
        this.referenceWidth = sanitizeSize(width, DEFAULT_REFERENCE_WIDTH);
        this.referenceHeight = sanitizeSize(height, DEFAULT_REFERENCE_HEIGHT);
        return this;
    }

    /**
     * Задаёт баланс между width-scale и height-scale.
     *
     * @param match {@code 0.0f} - ширина, {@code 1.0f} - высота, {@code 0.5f} - баланс
     * @return этот provider для fluent-настройки
     */
    public UnityLikeUIScaleProvider match(float match) {
        this.match = clamp(match, 0.0f, 1.0f);
        return this;
    }

    /**
     * Использует ширину как главный ориентир scale.
     *
     * @return этот provider для fluent-настройки
     */
    public UnityLikeUIScaleProvider matchWidth() {
        return match(0.0f);
    }

    /**
     * Балансирует scale между шириной и высотой.
     *
     * @return этот provider для fluent-настройки
     */
    public UnityLikeUIScaleProvider matchBalanced() {
        return match(0.5f);
    }

    /**
     * Использует высоту как главный ориентир scale.
     *
     * @return этот provider для fluent-настройки
     */
    public UnityLikeUIScaleProvider matchHeight() {
        return match(1.0f);
    }

    /**
     * Добавляет пользовательский множитель поверх рассчитанного scale.
     *
     * @param userScale множитель scale, например из настроек UI
     * @return этот provider для fluent-настройки
     */
    public UnityLikeUIScaleProvider userScale(float userScale) {
        this.userScale = Math.max(0.01f, finiteOr(userScale, 1.0f));
        return this;
    }

    /**
     * Ограничивает итоговый scale диапазоном.
     *
     * @param minScale минимальный допустимый scale
     * @param maxScale максимальный допустимый scale
     * @return этот provider для fluent-настройки
     */
    public UnityLikeUIScaleProvider scaleRange(float minScale, float maxScale) {
        float min = Math.max(0.01f, finiteOr(minScale, 0.75f));
        float max = Math.max(min, finiteOr(maxScale, 2.5f));
        this.minScale = min;
        this.maxScale = max;
        return this;
    }

    /** @return reference width в backend pixels */
    public float referenceWidth() {
        return referenceWidth;
    }

    /** @return reference height в backend pixels */
    public float referenceHeight() {
        return referenceHeight;
    }

    /** @return текущая ширина viewport'а в backend pixels */
    public float viewportWidth() {
        return viewportWidth;
    }

    /** @return текущая высота viewport'а в backend pixels */
    public float viewportHeight() {
        return viewportHeight;
    }

    /** @return баланс между width-scale и height-scale */
    public float match() {
        return match;
    }

    /** @return пользовательский множитель scale */
    public float userScale() {
        return userScale;
    }

    /** @return минимальный итоговый scale */
    public float minScale() {
        return minScale;
    }

    /** @return максимальный итоговый scale */
    public float maxScale() {
        return maxScale;
    }

    /**
     * Обновляет viewport через общий контракт {@link UIScaleProvider}.
     *
     * @param width ширина viewport'а в backend pixels
     * @param height высота viewport'а в backend pixels
     */
    @Override
    public void viewportSize(float width, float height) {
        viewport(width, height);
    }

    private static float sanitizeSize(float value, float fallback) {
        return Math.max(1.0f, finiteOr(value, fallback));
    }

    private static float finiteOr(float value, float fallback) {
        return Float.isFinite(value) ? value : fallback;
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}