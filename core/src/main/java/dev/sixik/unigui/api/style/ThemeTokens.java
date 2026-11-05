package dev.sixik.unigui.api.style;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * Неизменяемый снимок визуальных токенов темы.
 *
 * <p>Токены являются последним style-fallback: правило {@link StylePack} и локальный стиль
 * имеют приоритет. Снимок безопасно переиспользовать в нескольких UI-деревьях.</p>
 */
@AiDoc(
        summary = "Immutable design-token snapshot for colors, shapes, typography, and spacing.",
        purpose = "Supplies consistent theme defaults as the lowest-priority active layer of a `Theme` or `StylePack`.",
        useWhen = {
                "Create a coherent palette and geometry baseline shared by many widget styles.",
                "Read token groups directly for custom widget or renderer behavior."
        },
        criticalRules = {
                "Tokens are fallback values: StylePack rules and local widget styles take priority.",
                "`EMPTY` is inactive and contributes no values; `defaults()` creates an active neutral dark token set.",
                "Built snapshots are immutable; derive a new group with `toBuilder` and build a new `ThemeTokens` instance.",
                "Only keys mapped by `ThemeTokens` are supplied through `asStyle`; unknown keys preserve the caller fallback."
        },
        related = {"Theme", "StylePack", "ThemeColors", "ThemeShapes", "ThemeTypography", "ThemeSpacing"},
        examples = @AiDoc.Example(
                title = "Create custom theme tokens",
                code = """
                        ThemeTokens tokens = ThemeTokens.builder()
                                .colors(colors -> colors.accent(accentColor))
                                .shapes(shapes -> shapes.radiusMd(6.0f))
                                .spacing(spacing -> spacing.controlGap(6.0f))
                                .build();
                        """))
@WikiPage(
        title = "ThemeTokens",
        slug = "theme-tokens",
        category = "<key:style.category>",
        summary = "<key:theme.tokens.summary>",
        order = 50)
@WikiSection(title = "<key:theme.tokens.section.overview.title>",
        markdown = "<key:theme.tokens.section.overview.body>", order = 10)
@WikiSection(title = "<key:theme.tokens.section.priority.title>",
        markdown = "<key:theme.tokens.section.priority.body>", order = 20)
@WikiExample(title = "<key:theme.tokens.example.custom.title>",
        code = "<key:theme.tokens.example.custom.code>", order = 10)
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
    @AiDoc(section = "Creation", sectionOrder = 10, order = 10,
            summary = "Starts a builder for an active immutable token snapshot.")
    @WikiMethod(title = "builder()", summary = "<key:theme.tokens.method.builder.summary>",
            section = "<key:theme.tokens.api.section.creation>", sectionOrder = 10, order = 10)
    @WikiReturn("<key:theme.tokens.method.builder.return>")
    public static Builder builder() {
        return new Builder();
    }

    /**
     * @return стандартный активный снимок с нейтральной тёмной палитрой
     */
    @AiDoc(section = "Creation", sectionOrder = 10, order = 20,
            summary = "Creates the active neutral dark default token set.")
    @WikiMethod(title = "defaults()", summary = "<key:theme.tokens.method.defaults.summary>",
            section = "<key:theme.tokens.api.section.creation>", sectionOrder = 10, order = 20)
    @WikiReturn("<key:theme.tokens.method.defaults.return>")
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
    @AiDoc(section = "Integration", sectionOrder = 20, order = 10,
            summary = "Exposes mapped tokens as a read-only style layer for cascade composition.")
    @WikiMethod(title = "asStyle()", summary = "<key:theme.tokens.method.as_style.summary>",
            section = "<key:theme.tokens.api.section.integration>", sectionOrder = 20, order = 10)
    @WikiReturn("<key:theme.tokens.method.as_style.return>")
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
