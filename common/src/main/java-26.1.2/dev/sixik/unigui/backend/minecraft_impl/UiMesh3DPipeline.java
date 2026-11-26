package dev.sixik.unigui.backend.minecraft_impl;

import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.shaders.ShaderSource;
import com.mojang.blaze3d.shaders.ShaderType;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.resources.Identifier;

/**
 * Пайплайны мешей Minecraft 26.1.2 со старым описанием формата и ресурсов.
 */
final class UiMesh3DPipeline {
    /**
     * Проходы без текстуры.
     */
    private static final Variants SOLID = new Variants(false, false);
    /**
     * Проходы с UV и сэмплером.
     */
    private static final Variants TEXTURED = new Variants(true, false);
    /**
     * Инстансированный пайплайн без текстуры.
     */
    private static final Variants INSTANCED_SOLID = new Variants(false, true);
    /**
     * Инстансированный пайплайн с текстурой.
     */
    private static final Variants INSTANCED_TEXTURED = new Variants(true, true);

    private UiMesh3DPipeline() {
    }

    /**
     * Подготавливает нужный вариант с учётом возможной перезагрузки шейдеров Minecraft.
     *
     * @param textured    нужен ли сэмплер.
     * @param translucent включить ли смешивание с выключенной записью глубины.
     * @param instanced   читать ли матрицы нескольких экземпляров.
     * @return пайплайн выбранного прохода.
     * @throws IllegalStateException если шейдеры не скомпилировались.
     */
    static RenderPipeline get(boolean textured, boolean translucent, boolean instanced) {
        Variants variants = textured ? (instanced ? INSTANCED_TEXTURED : TEXTURED)
                : (instanced ? INSTANCED_SOLID : SOLID);
        RenderPipeline pipeline = translucent ? variants.translucent : variants.opaque;
        if (!RenderSystem.getDevice().precompilePipeline(pipeline, variants.source).isValid()) {
            throw new IllegalStateException("Could not compile UniGUI 3D mesh pipeline " + pipeline.getLocation());
        }
        return pipeline;
    }

    /**
     * Описывает проход с ранним тестом глубины.
     *
     * @param shader      идентификатор GLSL источника.
     * @param textured    нужен ли сэмплер.
     * @param translucent нужно ли смешивание.
     * @param instanced   нужен ли блок матриц экземпляров.
     * @return описание прохода.
     */
    private static RenderPipeline build(Identifier shader, boolean textured, boolean translucent, boolean instanced) {
        RenderPipeline.Builder pipeline = RenderPipeline.builder()
                .withLocation(Identifier.fromNamespaceAndPath("unigui", "pipeline/" + shader.getPath()
                        + (translucent ? "_blend" : "")))
                .withVertexShader(shader)
                .withFragmentShader(shader)
                .withUniform("SceneMesh", UniformType.UNIFORM_BUFFER)
                .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL, VertexFormat.Mode.TRIANGLES)
                .withColorTargetState(translucent ? new ColorTargetState(MinecraftUiBlend.PREMULTIPLIED.function())
                        : ColorTargetState.DEFAULT)
                .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, !translucent))
                .withCull(false);
        if (textured) pipeline.withSampler("Sampler0");
        if (instanced) pipeline.withUniform("SceneInstances", UniformType.UNIFORM_BUFFER);
        return pipeline.build();
    }

    /**
     * Описания двух режимов отрисовки, с одним общим источником шейдеров.
     */
    private static final class Variants {
        /**
         * Передаёт только GLSL, относящийся к этому варианту.
         */
        private final ShaderSource source;
        /**
         * Записывает глубину непрозрачных и прошедших alpha cutout фрагментов.
         */
        private final RenderPipeline opaque;
        /**
         * Читает глубину и смешивает цвет с уже нарисованной сценой.
         */
        private final RenderPipeline translucent;

        /**
         * Создаёт описания пайплайнов.
         *
         * @param textured  нужен ли сэмплер.
         * @param instanced читать ли матрицы нескольких экземпляров.
         */
        private Variants(boolean textured, boolean instanced) {
            String name = "scene_mesh" + (textured ? "_textured" : "") + (instanced ? "_instanced" : "");
            Identifier shader = Identifier.fromNamespaceAndPath("unigui", name);
            String vertex = UiMesh3DShaders.vertex(330, SceneFeaturePass.REVERSED_DEPTH, instanced);
            String fragment = UiMesh3DShaders.fragment(330, textured);
            source = (id, type) -> shader.equals(id) ? (type == ShaderType.VERTEX ? vertex : fragment) : null;
            opaque = build(shader, textured, false, instanced);
            translucent = build(shader, textured, true, instanced);
        }
    }
}
