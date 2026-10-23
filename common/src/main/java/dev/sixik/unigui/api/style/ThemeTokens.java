package dev.sixik.unigui.api.style;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * Неизменяемый снимок визуальных токенов темы.
 *
 * <p>Токены являются последним style-fallback: правило {@link StylePack} и локальный стиль
 * имеют приоритет. Снимок безопасно переиспользовать в нескольких UI-деревьях.</p>
 */
public final class ThemeTokens {
    private static final AtomicLong NEXT_VERSION = new AtomicLong();

    /**
     * Пустой снимок, который не подставляет значения поверх переданного fallback.
     */
    public static final ThemeTokens EMPTY = new ThemeTokens(
            ThemeColors.builder().build(),
            ThemeShapes.builder().build(),
            ThemeTypography.builder().build(),
            ThemeSpacing.builder().build(),
            0L,
            false);

    private final ThemeColors colors;
    private final ThemeShapes shapes;
    private final ThemeTypography typography;
    private final ThemeSpacing spacing;
    private final long version;
    private final boolean active;
    private final Style style;

    private ThemeTokens(ThemeColors colors,
                        ThemeShapes shapes,
                        ThemeTypography typography,
                        ThemeSpacing spacing,
                        long version,
                        boolean active) {
        this.colors = Objects.requireNonNull(colors, "colors");
        this.shapes = Objects.requireNonNull(shapes, "shapes");
        this.typography = Objects.requireNonNull(typography, "typography");
        this.spacing = Objects.requireNonNull(spacing, "spacing");
        this.version = version;
        this.active = active;
        this.style = active ? new TokenStyle(this) : Style.EMPTY;
    }

    /**
     * @return builder нового активного снимка токенов
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * @return стандартный активный снимок с нейтральной тёмной палитрой
     */
    public static ThemeTokens defaults() {
        return builder().build();
    }

    /**
     * @return группа цветовых токенов
     */
    public ThemeColors colors() {
        return colors;
    }

    /**
     * @return группа токенов форм
     */
    public ThemeShapes shapes() {
        return shapes;
    }

    /**
     * @return группа типографических токенов
     */
    public ThemeTypography typography() {
        return typography;
    }

    /**
     * @return группа токенов отступов
     */
    public ThemeSpacing spacing() {
        return spacing;
    }

    /**
     * @return версия снимка для invalidation и кэшей
     */
    public long version() {
        return version;
    }

    /**
     * Возвращает token-слой в формате {@link Style} для объединения с существующим каскадом.
     *
     * @return неизменяемый стиль токенов или {@link Style#EMPTY} для пустого снимка
     */
    public Style asStyle() {
        return style;
    }

    /**
     * Возвращает токен как значение style-свойства.
     *
     * @param key      ключ свойства
     * @param state    состояние виджета
     * @param fallback значение предыдущего слоя
     * @param <T>      тип свойства
     * @return токен или переданный fallback
     */
    @SuppressWarnings("unchecked")
    public <T> T get(StyleKey<T> key, WidgetState state, T fallback) {
        Objects.requireNonNull(key, "key");
        if (!active) return fallback;
        WidgetState normalized = state == null ? WidgetState.NORMAL : state;
        Object value = valueFor(key, normalized);
        if (value == null || !key.type().isInstance(value)) return fallback;
        return (T) value;
    }

    private Object valueFor(StyleKey<?> key, WidgetState state) {
        if (key == StyleKeys.BACKGROUND_COLOR) {
            return switch (state) {
                case HOVERED -> colors.surfaceHover();
                case PRESSED -> colors.surfacePressed();
                case CHECKED, SELECTED -> colors.surfaceSelected();
                case DISABLED -> colors.surfaceDisabled();
                default -> colors.surface();
            };
        }
        if (key == StyleKeys.BORDER_COLOR) {
            return switch (state) {
                case HOVERED, PRESSED, FOCUSED, CHECKED, SELECTED -> colors.accent();
                case DISABLED -> colors.borderDisabled();
                default -> colors.border();
            };
        }
        if (key == StyleKeys.TEXT_COLOR) {
            return switch (state) {
                case PRESSED, CHECKED, SELECTED -> colors.textOnAccent();
                case DISABLED -> colors.textMuted();
                default -> colors.textPrimary();
            };
        }
        if (key == StyleKeys.PLACEHOLDER_COLOR) return colors.textMuted();
        if (key == StyleKeys.ACCENT_COLOR) {
            return switch (state) {
                case HOVERED, FOCUSED -> colors.accentHover();
                case DISABLED -> colors.accentDim();
                default -> colors.accent();
            };
        }
        if (key == StyleKeys.TRACK_COLOR) return colors.surface();
        if (key == StyleKeys.THUMB_COLOR) {
            return switch (state) {
                case HOVERED -> colors.textSecondary();
                case PRESSED, CHECKED, SELECTED -> colors.accent();
                case DISABLED -> colors.borderDisabled();
                default -> colors.borderStrong();
            };
        }
        if (key == StyleKeys.BORDER_WIDTH) return shapes.borderWidth();
        if (key == StyleKeys.RADIUS) return shapes.radiusMd();
        return null;
    }

    /**
     * Сборщик групп токенов до публикации неизменяемого снимка.
     */
    public static final class Builder {
        private ThemeColors colors = ThemeColors.builder().build();
        private ThemeShapes shapes = ThemeShapes.builder().build();
        private ThemeTypography typography = ThemeTypography.builder().build();
        private ThemeSpacing spacing = ThemeSpacing.builder().build();

        private Builder() {
        }

        /**
         * Настраивает цвета через builder-группу.
         */
        public Builder colors(Consumer<ThemeColors.Builder> consumer) {
            ThemeColors.Builder builder = colors.toBuilder();
            Objects.requireNonNull(consumer, "consumer").accept(builder);
            colors = builder.build();
            return this;
        }

        /**
         * Устанавливает готовую неизменяемую группу цветов.
         */
        public Builder colors(ThemeColors value) {
            colors = Objects.requireNonNull(value, "colors");
            return this;
        }

        /**
         * Настраивает формы через builder-группу.
         */
        public Builder shapes(Consumer<ThemeShapes.Builder> consumer) {
            ThemeShapes.Builder builder = shapes.toBuilder();
            Objects.requireNonNull(consumer, "consumer").accept(builder);
            shapes = builder.build();
            return this;
        }

        /**
         * Устанавливает готовую неизменяемую группу форм.
         */
        public Builder shapes(ThemeShapes value) {
            shapes = Objects.requireNonNull(value, "shapes");
            return this;
        }

        /**
         * Настраивает типографику через builder-группу.
         */
        public Builder typography(Consumer<ThemeTypography.Builder> consumer) {
            ThemeTypography.Builder builder = typography.toBuilder();
            Objects.requireNonNull(consumer, "consumer").accept(builder);
            typography = builder.build();
            return this;
        }

        /**
         * Устанавливает готовую неизменяемую группу типографики.
         */
        public Builder typography(ThemeTypography value) {
            typography = Objects.requireNonNull(value, "typography");
            return this;
        }

        /**
         * Настраивает отступы через builder-группу.
         */
        public Builder spacing(Consumer<ThemeSpacing.Builder> consumer) {
            ThemeSpacing.Builder builder = spacing.toBuilder();
            Objects.requireNonNull(consumer, "consumer").accept(builder);
            spacing = builder.build();
            return this;
        }

        /**
         * Устанавливает готовую неизменяемую группу отступов.
         */
        public Builder spacing(ThemeSpacing value) {
            spacing = Objects.requireNonNull(value, "spacing");
            return this;
        }

        /**
         * @return новый неизменяемый и активный снимок токенов
         */
        public ThemeTokens build() {
            return new ThemeTokens(colors, shapes, typography, spacing, NEXT_VERSION.incrementAndGet(), true);
        }
    }

    private static final class TokenStyle implements Style {
        private final ThemeTokens owner;

        private TokenStyle(ThemeTokens owner) {
            this.owner = owner;
        }

        @Override
        public long version() {
            return owner.version;
        }

        @Override
        public <T> T get(StyleKey<T> key, WidgetState state, T fallback) {
            return owner.get(key, state, fallback);
        }
    }
}
