package dev.sixik.unigui.api.style;

import dev.sixik.unigui.api.math.ColorView;

import java.util.Objects;

/**
 * Неизменяемая группа цветовых токенов темы.
 *
 * <p>Цвета хранятся как снимки компонентов {@link ColorView}; переданный
 * изменяемый цвет не может поменять уже созданную группу.</p>
 */
public final class ThemeColors {
    private final ColorView surface;
    private final ColorView surfaceHover;
    private final ColorView surfacePressed;
    private final ColorView surfaceSelected;
    private final ColorView surfaceDisabled;
    private final ColorView border;
    private final ColorView borderStrong;
    private final ColorView borderDisabled;
    private final ColorView textPrimary;
    private final ColorView textSecondary;
    private final ColorView textMuted;
    private final ColorView textOnAccent;
    private final ColorView accent;
    private final ColorView accentHover;
    private final ColorView accentDim;
    private final ColorView success;

    private ThemeColors(Builder builder) {
        surface = copy(builder.surface);
        surfaceHover = copy(builder.surfaceHover);
        surfacePressed = copy(builder.surfacePressed);
        surfaceSelected = copy(builder.surfaceSelected);
        surfaceDisabled = copy(builder.surfaceDisabled);
        border = copy(builder.border);
        borderStrong = copy(builder.borderStrong);
        borderDisabled = copy(builder.borderDisabled);
        textPrimary = copy(builder.textPrimary);
        textSecondary = copy(builder.textSecondary);
        textMuted = copy(builder.textMuted);
        textOnAccent = copy(builder.textOnAccent);
        accent = copy(builder.accent);
        accentHover = copy(builder.accentHover);
        accentDim = copy(builder.accentDim);
        success = copy(builder.success);
    }

    /**
     * @return builder с нейтральными тёмными значениями по умолчанию
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * @return builder, инициализированный значениями группы
     */
    public Builder toBuilder() {
        return new Builder(this);
    }

    /**
     * @return цвет базовой поверхности
     */
    public ColorView surface() {
        return surface;
    }

    /**
     * @return цвет поверхности при наведении
     */
    public ColorView surfaceHover() {
        return surfaceHover;
    }

    /**
     * @return цвет поверхности в нажатом состоянии
     */
    public ColorView surfacePressed() {
        return surfacePressed;
    }

    /**
     * @return цвет выбранной поверхности
     */
    public ColorView surfaceSelected() {
        return surfaceSelected;
    }

    /**
     * @return цвет отключённой поверхности
     */
    public ColorView surfaceDisabled() {
        return surfaceDisabled;
    }

    /**
     * @return обычный цвет границы
     */
    public ColorView border() {
        return border;
    }

    /**
     * @return усиленный цвет границы
     */
    public ColorView borderStrong() {
        return borderStrong;
    }

    /**
     * @return цвет границы отключённого элемента
     */
    public ColorView borderDisabled() {
        return borderDisabled;
    }

    /**
     * @return основной цвет текста
     */
    public ColorView textPrimary() {
        return textPrimary;
    }

    /**
     * @return вторичный цвет текста
     */
    public ColorView textSecondary() {
        return textSecondary;
    }

    /**
     * @return приглушённый цвет текста
     */
    public ColorView textMuted() {
        return textMuted;
    }

    /**
     * @return цвет текста на акцентной поверхности
     */
    public ColorView textOnAccent() {
        return textOnAccent;
    }

    /**
     * @return основной акцентный цвет
     */
    public ColorView accent() {
        return accent;
    }

    /**
     * @return акцентный цвет при наведении
     */
    public ColorView accentHover() {
        return accentHover;
    }

    /**
     * @return приглушённый акцентный цвет
     */
    public ColorView accentDim() {
        return accentDim;
    }

    /**
     * @return цвет успешного состояния
     */
    public ColorView success() {
        return success;
    }

    private static ColorView copy(ColorView value) {
        Objects.requireNonNull(value, "color token");
        return new ImmutableColor(value.r(), value.g(), value.b(), value.a());
    }

    /**
     * Изменяемый сборщик цветовых токенов до публикации снимка.
     */
    public static final class Builder {
        private ColorView surface = color(0x1B, 0x1F, 0x26, 235);
        private ColorView surfaceHover = color(0x2A, 0x30, 0x3B, 255);
        private ColorView surfacePressed = color(0x6B, 0x4A, 0x2C, 255);
        private ColorView surfaceSelected = color(0xC9, 0x8A, 0x4B, 255);
        private ColorView surfaceDisabled = color(0x17, 0x1A, 0x20, 185);
        private ColorView border = color(0x33, 0x3A, 0x48, 255);
        private ColorView borderStrong = color(0x45, 0x4E, 0x60, 255);
        private ColorView borderDisabled = color(0x25, 0x2A, 0x34, 205);
        private ColorView textPrimary = color(0xE8, 0xEA, 0xEE, 255);
        private ColorView textSecondary = color(0x9A, 0xA2, 0xB1, 255);
        private ColorView textMuted = color(0x5C, 0x65, 0x77, 225);
        private ColorView textOnAccent = color(0x20, 0x12, 0x06, 255);
        private ColorView accent = color(0xC9, 0x8A, 0x4B, 255);
        private ColorView accentHover = color(0xE0, 0xA6, 0x68, 255);
        private ColorView accentDim = color(0x6B, 0x4A, 0x2C, 255);
        private ColorView success = color(0x5F, 0xAE, 0x7A, 255);

        private Builder() {
        }

        private Builder(ThemeColors source) {
            surface = source.surface;
            surfaceHover = source.surfaceHover;
            surfacePressed = source.surfacePressed;
            surfaceSelected = source.surfaceSelected;
            surfaceDisabled = source.surfaceDisabled;
            border = source.border;
            borderStrong = source.borderStrong;
            borderDisabled = source.borderDisabled;
            textPrimary = source.textPrimary;
            textSecondary = source.textSecondary;
            textMuted = source.textMuted;
            textOnAccent = source.textOnAccent;
            accent = source.accent;
            accentHover = source.accentHover;
            accentDim = source.accentDim;
            success = source.success;
        }

        /**
         * Задаёт базовый цвет поверхности.
         */
        public Builder surface(ColorView value) {
            surface = require(value);
            return this;
        }

        /**
         * Задаёт цвет поверхности при наведении.
         */
        public Builder surfaceHover(ColorView value) {
            surfaceHover = require(value);
            return this;
        }

        /**
         * Задаёт цвет поверхности в нажатом состоянии.
         */
        public Builder surfacePressed(ColorView value) {
            surfacePressed = require(value);
            return this;
        }

        /**
         * Задаёт цвет выбранной поверхности.
         */
        public Builder surfaceSelected(ColorView value) {
            surfaceSelected = require(value);
            return this;
        }

        /**
         * Задаёт цвет отключённой поверхности.
         */
        public Builder surfaceDisabled(ColorView value) {
            surfaceDisabled = require(value);
            return this;
        }

        /**
         * Задаёт обычный цвет границы.
         */
        public Builder border(ColorView value) {
            border = require(value);
            return this;
        }

        /**
         * Задаёт усиленный цвет границы.
         */
        public Builder borderStrong(ColorView value) {
            borderStrong = require(value);
            return this;
        }

        /**
         * Задаёт цвет отключённой границы.
         */
        public Builder borderDisabled(ColorView value) {
            borderDisabled = require(value);
            return this;
        }

        /**
         * Задаёт основной цвет текста.
         */
        public Builder textPrimary(ColorView value) {
            textPrimary = require(value);
            return this;
        }

        /**
         * Задаёт вторичный цвет текста.
         */
        public Builder textSecondary(ColorView value) {
            textSecondary = require(value);
            return this;
        }

        /**
         * Задаёт приглушённый цвет текста.
         */
        public Builder textMuted(ColorView value) {
            textMuted = require(value);
            return this;
        }

        /**
         * Задаёт цвет текста на акцентной поверхности.
         */
        public Builder textOnAccent(ColorView value) {
            textOnAccent = require(value);
            return this;
        }

        /**
         * Задаёт основной акцентный цвет.
         */
        public Builder accent(ColorView value) {
            accent = require(value);
            return this;
        }

        /**
         * Задаёт акцентный цвет при наведении.
         */
        public Builder accentHover(ColorView value) {
            accentHover = require(value);
            return this;
        }

        /**
         * Задаёт приглушённый акцентный цвет.
         */
        public Builder accentDim(ColorView value) {
            accentDim = require(value);
            return this;
        }

        /**
         * Задаёт цвет успешного состояния.
         */
        public Builder success(ColorView value) {
            success = require(value);
            return this;
        }

        /**
         * @return неизменяемая группа цветов
         */
        public ThemeColors build() {
            return new ThemeColors(this);
        }

        private static ColorView require(ColorView value) {
            return Objects.requireNonNull(value, "color token");
        }

        private static ColorView color(int r, int g, int b, int a) {
            return new ImmutableColor(r / 255.0f, g / 255.0f, b / 255.0f, a / 255.0f);
        }
    }

    private record ImmutableColor(float r, float g, float b, float a) implements ColorView {
    }
}
