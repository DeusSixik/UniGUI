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
     * Константа {@code AUTO}, используемая как общее значение по умолчанию или служебный предел.
     */
    public static final float AUTO = Float.NaN;
    /**
     * Константа {@code DEFAULT}, используемая как общее значение по умолчанию или служебный предел.
     */
    public static final LayoutConstraints DEFAULT = new LayoutConstraints(
            AUTO, AUTO,
            0.0f, 0.0f,
            Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY,
            EdgeInsets.ZERO,
            Alignment.STRETCH, Alignment.STRETCH,
            0.0f);

    /**
     * Хранит текстовое или идентификационное значение {@code preferredWidth}.
     */
    private final float preferredWidth;
    /**
     * Хранит числовой параметр {@code preferredHeight}, используемый в расчётах, вводе или отрисовке.
     */
    private final float preferredHeight;
    /**
     * Хранит текстовое или идентификационное значение {@code minWidth}.
     */
    private final float minWidth;
    /**
     * Хранит числовой параметр {@code minHeight}, используемый в расчётах, вводе или отрисовке.
     */
    private final float minHeight;
    /**
     * Хранит текстовое или идентификационное значение {@code maxWidth}.
     */
    private final float maxWidth;
    /**
     * Хранит числовой параметр {@code maxHeight}, используемый в расчётах, вводе или отрисовке.
     */
    private final float maxHeight;
    /**
     * Хранит состояние или настройку {@code margin}, используемую логикой объекта.
     */
    private final EdgeInsets margin;
    /**
     * Хранит числовой параметр {@code horizontalAlignment}, используемый в расчётах, вводе или отрисовке.
     */
    private final Alignment horizontalAlignment;
    /**
     * Хранит состояние или настройку {@code verticalAlignment}, используемую логикой объекта.
     */
    private final Alignment verticalAlignment;
    /**
     * Хранит числовой параметр {@code grow}, используемый в расчётах, вводе или отрисовке.
     */
    private final float grow;

    /**
     * Создаёт экземпляр {@code LayoutConstraints} и подготавливает его начальное состояние.
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
     * Возвращает текущее значение или выполняет операцию {@code preferredWidth}.
     */
    public float preferredWidth() {
        return preferredWidth;
    }

    /**
     * Возвращает текущее значение или выполняет операцию {@code preferredHeight}.
     */
    public float preferredHeight() {
        return preferredHeight;
    }

    /**
     * Возвращает текущее значение или выполняет операцию {@code minWidth}.
     */
    public float minWidth() {
        return minWidth;
    }

    /**
     * Возвращает текущее значение или выполняет операцию {@code minHeight}.
     */
    public float minHeight() {
        return minHeight;
    }

    /**
     * Возвращает текущее значение или выполняет операцию {@code maxWidth}.
     */
    public float maxWidth() {
        return maxWidth;
    }

    /**
     * Возвращает текущее значение или выполняет операцию {@code maxHeight}.
     */
    public float maxHeight() {
        return maxHeight;
    }

    /**
     * Возвращает текущее значение или выполняет операцию {@code margin}.
     */
    public EdgeInsets margin() {
        return margin;
    }

    /**
     * Возвращает текущее значение или выполняет операцию {@code horizontalAlignment}.
     */
    public Alignment horizontalAlignment() {
        return horizontalAlignment;
    }

    /**
     * Возвращает текущее значение или выполняет операцию {@code verticalAlignment}.
     */
    public Alignment verticalAlignment() {
        return verticalAlignment;
    }

    /**
     * Возвращает текущее значение или выполняет операцию {@code grow}.
     */
    public float grow() {
        return grow;
    }

    /**
     * Выполняет операцию {@code preferredSize} с переданными параметрами.
     */
    public LayoutConstraints preferredSize(float width, float height) {
        return new LayoutConstraints(width, height, minWidth, minHeight, maxWidth, maxHeight, margin, horizontalAlignment, verticalAlignment, grow);
    }

    /**
     * Выполняет операцию {@code minSize} с переданными параметрами.
     */
    public LayoutConstraints minSize(float width, float height) {
        return new LayoutConstraints(preferredWidth, preferredHeight, width, height, maxWidth, maxHeight, margin, horizontalAlignment, verticalAlignment, grow);
    }

    /**
     * Выполняет операцию {@code maxSize} с переданными параметрами.
     */
    public LayoutConstraints maxSize(float width, float height) {
        return new LayoutConstraints(preferredWidth, preferredHeight, minWidth, minHeight, width, height, margin, horizontalAlignment, verticalAlignment, grow);
    }

    /**
     * Выполняет операцию {@code margin} с переданными параметрами.
     */
    public LayoutConstraints margin(EdgeInsets margin) {
        return new LayoutConstraints(preferredWidth, preferredHeight, minWidth, minHeight, maxWidth, maxHeight, margin, horizontalAlignment, verticalAlignment, grow);
    }

    /**
     * Выполняет операцию {@code align} с переданными параметрами.
     */
    public LayoutConstraints align(Alignment horizontal, Alignment vertical) {
        return new LayoutConstraints(preferredWidth, preferredHeight, minWidth, minHeight, maxWidth, maxHeight, margin, horizontal, vertical, grow);
    }

    /**
     * Выполняет операцию {@code grow} с переданными параметрами.
     */
    public LayoutConstraints grow(float grow) {
        return new LayoutConstraints(preferredWidth, preferredHeight, minWidth, minHeight, maxWidth, maxHeight, margin, horizontalAlignment, verticalAlignment, grow);
    }

    /**
     * Проверяет состояние или возможность, связанную с {@code isAuto}.
     */
    public static boolean isAuto(float value) {
        return Float.isNaN(value);
    }

    /**
     * Приводит входное значение к безопасному или допустимому диапазону.
     */
    private static float sanitizeAuto(float value) {
        if (Float.isNaN(value)) return AUTO;
        return Float.isFinite(value) ? Math.max(0.0f, value) : AUTO;
    }

    /**
     * Приводит входное значение к безопасному или допустимому диапазону.
     */
    private static float sanitizeMin(float value) {
        return Float.isFinite(value) ? Math.max(0.0f, value) : 0.0f;
    }

    /**
     * Приводит входное значение к безопасному или допустимому диапазону.
     */
    private static float sanitizeMax(float value, float min) {
        if (!Float.isFinite(value)) return Float.POSITIVE_INFINITY;
        return Math.max(min, value);
    }
}
