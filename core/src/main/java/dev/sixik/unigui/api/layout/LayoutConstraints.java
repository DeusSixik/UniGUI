package dev.sixik.unigui.api.layout;

/**
 * Неизменяемый совместимый контракт компоновки, используемый исходными вспомогательными методами виджетов.
 *
 * <p>Он остаётся публичным и поддерживается для интеграций, которые создают ограничения
 * напрямую. Новый расширенный код компоновки должен предпочитать {@link LayoutStyle} через
 * {@code WidgetBase.layout(style -> ...)}.</p>
 */
public final class LayoutConstraints {
    /**
     * Маркер {@code AUTO}: размер по содержимому или ограничениям родителя.
     */
    public static final float AUTO = Float.NaN;
    /**
     * Ограничения по умолчанию: размеры {@code auto}, отступы нулевые, выравнивание {@code STRETCH}.
     */
    public static final LayoutConstraints DEFAULT = new LayoutConstraints(
            AUTO, AUTO,
            0.0f, 0.0f,
            Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY,
            EdgeInsets.ZERO,
            Alignment.STRETCH, Alignment.STRETCH,
            0.0f);

    /**
     * Предпочтительная ширина; {@code AUTO} означает размер по содержимому.
     */
    private final float preferredWidth;
    /**
     * Предпочтительная высота; {@code AUTO} означает размер по содержимому.
     */
    private final float preferredHeight;
    /**
     * Минимальная ширина, не ниже нуля.
     */
    private final float minWidth;
    /**
     * Минимальная высота, не ниже нуля.
     */
    private final float minHeight;
    /**
     * Максимальная ширина; бесконечность означает отсутствие предела.
     */
    private final float maxWidth;
    /**
     * Максимальная высота; бесконечность означает отсутствие предела.
     */
    private final float maxHeight;
    /**
     * Внешний отступ вокруг виджета.
     */
    private final EdgeInsets margin;
    /**
     * Горизонтальное выравнивание виджета.
     */
    private final Alignment horizontalAlignment;
    /**
     * Вертикальное выравнивание виджета.
     */
    private final Alignment verticalAlignment;
    /**
     * Вес расширения виджета на главной оси родителя.
     */
    private final float grow;

    /**
     * Создаёт ограничения с заданными размерами, отступами, выравниванием и grow-весом.
     */
    public LayoutConstraints(float preferredWidth, float preferredHeight,
                             float minWidth, float minHeight,
                             float maxWidth, float maxHeight,
                             EdgeInsets margin,
                             Alignment horizontalAlignment, Alignment verticalAlignment,
                             float grow) {
        this.preferredWidth = sanitizeAuto(preferredWidth);
        this.preferredHeight = sanitizeAuto(preferredHeight);
        this.minWidth = sanitizeMin(minWidth);
        this.minHeight = sanitizeMin(minHeight);
        this.maxWidth = sanitizeMax(maxWidth, this.minWidth);
        this.maxHeight = sanitizeMax(maxHeight, this.minHeight);
        this.margin = margin == null ? EdgeInsets.ZERO : margin;
        this.horizontalAlignment = horizontalAlignment == null ? Alignment.STRETCH : horizontalAlignment;
        this.verticalAlignment = verticalAlignment == null ? Alignment.STRETCH : verticalAlignment;
        this.grow = Float.isFinite(grow) ? Math.max(0.0f, grow) : 0.0f;
    }

    /**
     * Возвращает предпочтительную ширину.
     */
    public float preferredWidth() {
        return preferredWidth;
    }

    /**
     * Возвращает предпочтительную высоту.
     */
    public float preferredHeight() {
        return preferredHeight;
    }

    /**
     * Возвращает минимальную ширину.
     */
    public float minWidth() {
        return minWidth;
    }

    /**
     * Возвращает минимальную высоту.
     */
    public float minHeight() {
        return minHeight;
    }

    /**
     * Возвращает максимальную ширину.
     */
    public float maxWidth() {
        return maxWidth;
    }

    /**
     * Возвращает максимальную высоту.
     */
    public float maxHeight() {
        return maxHeight;
    }

    /**
     * Возвращает внешний отступ.
     */
    public EdgeInsets margin() {
        return margin;
    }

    /**
     * Возвращает горизонтальное выравнивание.
     */
    public Alignment horizontalAlignment() {
        return horizontalAlignment;
    }

    /**
     * Возвращает вертикальное выравнивание.
     */
    public Alignment verticalAlignment() {
        return verticalAlignment;
    }

    /**
     * Возвращает grow-вес расширения.
     */
    public float grow() {
        return grow;
    }

    /**
     * Возвращает копию с новым предпочтительным размером.
     */
    public LayoutConstraints preferredSize(float width, float height) {
        return new LayoutConstraints(width, height, minWidth, minHeight, maxWidth, maxHeight, margin, horizontalAlignment, verticalAlignment, grow);
    }

    /**
     * Возвращает копию с новым минимальным размером.
     */
    public LayoutConstraints minSize(float width, float height) {
        return new LayoutConstraints(preferredWidth, preferredHeight, width, height, maxWidth, maxHeight, margin, horizontalAlignment, verticalAlignment, grow);
    }

    /**
     * Возвращает копию с новым максимальным размером.
     */
    public LayoutConstraints maxSize(float width, float height) {
        return new LayoutConstraints(preferredWidth, preferredHeight, minWidth, minHeight, width, height, margin, horizontalAlignment, verticalAlignment, grow);
    }

    /**
     * Возвращает копию с новым внешним отступом.
     */
    public LayoutConstraints margin(EdgeInsets margin) {
        return new LayoutConstraints(preferredWidth, preferredHeight, minWidth, minHeight, maxWidth, maxHeight, margin, horizontalAlignment, verticalAlignment, grow);
    }

    /**
     * Возвращает копию с новым выравниванием по обеим осям.
     */
    public LayoutConstraints align(Alignment horizontal, Alignment vertical) {
        return new LayoutConstraints(preferredWidth, preferredHeight, minWidth, minHeight, maxWidth, maxHeight, margin, horizontal, vertical, grow);
    }

    /**
     * Возвращает копию с новым grow-весом.
     */
    public LayoutConstraints grow(float grow) {
        return new LayoutConstraints(preferredWidth, preferredHeight, minWidth, minHeight, maxWidth, maxHeight, margin, horizontalAlignment, verticalAlignment, grow);
    }

    /**
     * Проверяет, означает ли значение {@code AUTO} (размер по содержимому).
     */
    public static boolean isAuto(float value) {
        return Float.isNaN(value);
    }

    /**
     * Нормализует предпочтительный размер: бесконечность и мусор в {@code AUTO}, отрицательные в ноль.
     */
    private static float sanitizeAuto(float value) {
        if (Float.isNaN(value)) return AUTO;
        return Float.isFinite(value) ? Math.max(0.0f, value) : AUTO;
    }

    /**
     * Нормализует минимальный размер: отрицательные и мусор в ноль.
     */
    private static float sanitizeMin(float value) {
        return Float.isFinite(value) ? Math.max(0.0f, value) : 0.0f;
    }

    /**
     * Нормализует максимальный размер: бесконечность означает отсутствие предела, ниже минимума не опускается.
     */
    private static float sanitizeMax(float value, float min) {
        if (!Float.isFinite(value)) return Float.POSITIVE_INFINITY;
        return Math.max(min, value);
    }
}
