package dev.sixik.unigui.api.style;

/**
 * Описывает, каким способом должен рендериться именованный стиль.
 *
 * <p>Декларативный backend хранит только {@link Style}-свойства и может быть полностью отредактирован
 * как данные. Custom backend хранит те же свойства, но дополнительно ссылается на Java renderer по id.
 * Такой escape hatch нужен для эффектов, которые пока нельзя выразить через RenderPlan/StylePack.</p>
 */
public sealed interface StyleBackend permits StyleBackend.Declarative, StyleBackend.Custom {
    /**
     * @return набор style-свойств, доступный и декларативному, и custom renderer path
     */
    Style style();

    /**
     * Создаёт декларативный backend.
     *
     * @param style свойства стиля
     * @return backend без custom renderer'а
     */
    static StyleBackend declarative(Style style) {
        return new Declarative(style);
    }

    /**
     * Создаёт backend со ссылкой на custom renderer.
     *
     * @param rendererId id renderer'а в WidgetRendererRegistry
     * @param style свойства стиля
     * @return backend с custom renderer path
     */
    static StyleBackend custom(String rendererId, Style style) {
        return new Custom(rendererId, style);
    }

    /**
     * @return {@code true}, если backend должен использовать custom renderer id
     */
    default boolean customRenderer() {
        return false;
    }

    /**
     * @return id custom renderer'а или пустая строка
     */
    default String rendererId() {
        return "";
    }

    /** Декларативный backend, который строится только из style-свойств. */
    record Declarative(Style style) implements StyleBackend {
        /** Нормализует отсутствующий стиль в {@link Style#EMPTY}. */
        public Declarative {
            style = style == null ? Style.EMPTY : style;
        }
    }

    /** Backend, который добавляет к style-свойствам ссылку на Java renderer. */
    record Custom(String rendererId, Style style) implements StyleBackend {
        /** Нормализует renderer id и отсутствующий стиль. */
        public Custom {
            rendererId = normalize(rendererId);
            style = style == null ? Style.EMPTY : style;
        }

        @Override
        public boolean customRenderer() {
            return !rendererId.isEmpty();
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}