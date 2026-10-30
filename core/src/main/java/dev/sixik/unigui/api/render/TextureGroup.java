package dev.sixik.unigui.api.render;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.*;

/**
 * Упорядоченная группа текстурных слоёв, рисуемых в общих базовых bounds.
 * Настройки каждого слоя задают собственные fit, UV, tint, nine-slice и отступы.
 */
@AiDoc(
        summary = "Ordered composite group of TextureLayer instances rendered sequentially within shared base bounds.",
        purpose = "Allows combining multiple texture layers (e.g. background plate, decorative trim, corner accents) into a single composite asset.",
        useWhen = {
                "Compose complex multi-layered widget surfaces using `TextureGroup.create(layers)` or `TextureGroup.builder()`.",
                "Build multi-textured UI cards with decoupled background, border, and highlight layers."
        },
        criticalRules = {
                "Layers are rendered strictly in list order from index 0 upwards.",
                "The returned layers list is unmodifiable."
        },
        related = {"TextureLayer", "TextureHandle", "Surface"},
        examples = {
                @AiDoc.Example(
                        title = "Build composite card surface",
                        order = 10,
                        code = """
                                TextureGroup group = TextureGroup.builder()
                                        .add(bgTexture)
                                        .add(new TextureLayer(borderTexture).tint(ColorView.rgb(255, 215, 0)))
                                        .build();
                                """)
        })
@WikiPage(
        title = "<key:texture.group.title>",
        slug = "texture-group",
        category = "<key:texture.group.category>",
        summary = "<key:texture.group.summary>",
        order = 60)
@WikiSection(
        title = "<key:texture.group.section.overview.title>",
        markdown = "<key:texture.group.section.overview.body>",
        order = 10)
@WikiSection(
        title = "<key:texture.group.section.builder.title>",
        markdown = "<key:texture.group.section.builder.body>",
        order = 20)
@WikiExample(
        title = "<key:texture.group.example.composite.title>",
        code = "<key:texture.group.example.composite.code>",
        order = 10)
public final class TextureGroup {
    private final List<TextureLayer> layers;

    /**
     * Создаёт группу из слоёв в порядке отрисовки.
     */
    @WikiMethod(
            title = "<key:texture.group.method.create.title>",
            summary = "<key:texture.group.method.create.summary>",
            section = "<key:texture.group.api.section.factories>",
            sectionOrder = 10,
            order = 10)
    @WikiReturn("<key:texture.group.method.create.return>")
    public static TextureGroup create(@WikiParameter(name = "layers", description = "<key:texture.group.param.layers>") TextureLayer... layers) {
        return new TextureGroup(Arrays.asList(layers));
    }

    /**
     * Создаёт группу из слоёв в порядке отрисовки.
     */
    @WikiMethod(
            title = "<key:texture.group.method.constructor.title>",
            summary = "<key:texture.group.method.constructor.summary>",
            section = "<key:texture.group.api.section.factories>",
            sectionOrder = 10,
            order = 20)
    public TextureGroup(@WikiParameter(name = "layers", description = "<key:texture.group.param.layers>") List<TextureLayer> layers) {
        Objects.requireNonNull(layers, "layers");
        List<TextureLayer> copy = new ObjectArrayList<>(layers.size());
        for (TextureLayer layer : layers) copy.add(Objects.requireNonNull(layer, "layer"));
        this.layers = Collections.unmodifiableList(copy);
    }

    /**
     * Возвращает неизменяемый список слоёв в порядке отрисовки.
     */
    @WikiMethod(
            title = "<key:texture.group.method.layers.title>",
            summary = "<key:texture.group.method.layers.summary>",
            section = "<key:texture.group.api.section.access>",
            sectionOrder = 20,
            order = 10)
    @WikiReturn("<key:texture.group.method.layers.return>")
    public List<TextureLayer> layers() {
        return layers;
    }

    /**
     * Создаёт построитель группы.
     */
    @WikiMethod(
            title = "<key:texture.group.method.builder.title>",
            summary = "<key:texture.group.method.builder.summary>",
            section = "<key:texture.group.api.section.factories>",
            sectionOrder = 10,
            order = 30)
    @WikiReturn("<key:texture.group.method.builder.return>")
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
