package dev.sixik.unigui.api.render;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.*;

/**
 * Упорядоченная группа текстурных слоёв, рисуемых в общих базовых bounds.
 * Настройки каждого слоя задают собственные fit, UV, tint, nine-slice и отступы.
 */
public final class TextureGroup {
    private final List<TextureLayer> layers;

    /**
     * Создаёт группу из слоёв в порядке отрисовки.
     */
    public static TextureGroup create(TextureLayer... layers) {
        return new TextureGroup(Arrays.asList(layers));
    }

    /**
     * Создаёт группу из слоёв в порядке отрисовки.
     */
    public TextureGroup(List<TextureLayer> layers) {
        Objects.requireNonNull(layers, "layers");
        List<TextureLayer> copy = new ObjectArrayList<>(layers.size());
        for (TextureLayer layer : layers) copy.add(Objects.requireNonNull(layer, "layer"));
        this.layers = Collections.unmodifiableList(copy);
    }

    /**
     * Возвращает неизменяемый список слоёв в порядке отрисовки.
     */
    public List<TextureLayer> layers() {
        return layers;
    }

    /**
     * Создаёт построитель группы.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Строитель упорядоченной группы текстур.
     */
    public static final class Builder {
        private final List<TextureLayer> layers = new ObjectArrayList<>();

        /**
         * Добавляет настроенный текстурный слой.
         */
        public Builder add(TextureLayer layer) {
            layers.add(Objects.requireNonNull(layer, "layer"));
            return this;
        }

        /**
         * Добавляет текстуру с параметрами слоя по умолчанию.
         */
        public Builder add(TextureHandle texture) {
            return add(new TextureLayer(Objects.requireNonNull(texture, "texture")));
        }

        /**
         * Создаёт группу из добавленных слоёв.
         */
        public TextureGroup build() {
            return new TextureGroup(layers);
        }
    }
}
