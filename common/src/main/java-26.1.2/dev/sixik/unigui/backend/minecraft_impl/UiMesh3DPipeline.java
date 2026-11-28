package dev.sixik.unigui.backend.minecraft_impl;

import java.nio.ByteBuffer;

import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.platform.PolygonMode;
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
     * This backend uses the original scene mesh pipeline.
     */
    static final boolean SUPPORTS_SURFACE_EFFECTS = false;

    /**
     * This backend has no surface cache to trim.
     */
    static void trimSurfaces() {
    }

    /**
     * @param textured      whether the material uses a base texture
     * @param translucent   whether the draw blends with the background
     * @param shadowPass    whether this is a shadow depth draw
     * @param receiveShadow whether the scene shadow texture must be bound
     * @param style         optional native layer state
     * @param localLights   whether to bind local lighting
     * @param masks         number of projected mask samplers
     * @param spotShadows   number of perspective shadow samplers
     * @param source        custom GLSL surface function, or null
     * @return null because this backend has no surface pipeline
     */
    static RenderPipeline getSurface(boolean textured, boolean translucent, boolean shadowPass,
                                     boolean receiveShadow, SceneNativeLayer.Style style, boolean localLights,
                                     int masks, int spotShadows, String source) {
        return null;
    }

    /**
     * @return zero because this backend has no scene glint pass
     */
    static float glintStrength() {
        return 0;
    }

    /**
     * @param out   destination for native glint uniforms
     * @param glint selected vanilla glint animation
     */
    static void writeGlintUv(ByteBuffer out, SceneNativeLayer.Glint glint) {
    }

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

    /**
     * Shared GLSL of both wireframe pipelines.
     */
    private static final Identifier WIRE_SHADER = Identifier.fromNamespaceAndPath("unigui", "scene_mesh_wire");
    /**
     * Supplies the plain mesh vertex stage and the tint-only fragment stage of the wireframe.
     */
    private static final ShaderSource WIRE_SOURCE = wireSource();
    /**
     * Depth-tested wireframe edges.
     */
    private static final RenderPipeline WIRE_TESTED = buildWire(true);
    /**
     * Wireframe edges drawn over the whole scene.
     */
    private static final RenderPipeline WIRE_OVERLAY = buildWire(false);

    private UiMesh3DPipeline() {
    }

    /**
     * @return null because this backend has no atmospheric pass
     */
    static SceneFogPass createFogPass() {
        return null;
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
     * Compiles or reuses the edge pipeline of the scene wireframe: polygons rasterized as lines, colored by the
     * material tint alone and blended with premultiplied alpha, without writing depth.
     *
     * @param depthTested whether surfaces in front hide the edges; false draws every edge over the scene
     * @return wireframe pipeline
     * @throws IllegalStateException if compilation failed
     */
    static RenderPipeline getWire(boolean depthTested) {
        RenderPipeline pipeline = depthTested ? WIRE_TESTED : WIRE_OVERLAY;
        if (!RenderSystem.getDevice().precompilePipeline(pipeline, WIRE_SOURCE).isValid()) {
            throw new IllegalStateException("Could not compile UniGUI wireframe pipeline " + pipeline.getLocation());
        }
        return pipeline;
    }

    /**
     * @return source of the wireframe shader identifier, or null for any other shader
     */
    private static ShaderSource wireSource() {
        String vertex = UiMesh3DShaders.vertex(330, SceneFeaturePass.REVERSED_DEPTH, false);
        String fragment = UiMesh3DShaders.wireFragment(330);
        return (id, type) -> WIRE_SHADER.equals(id) ? (type == ShaderType.VERTEX ? vertex : fragment) : null;
    }

    /**
     * Describes a wireframe pass. This version uses forward depth, so a nearer edge passes
     * {@code LESS_THAN_OR_EQUAL}.
     *
     * @param depthTested whether to compare against scene depth
     * @return pipeline description
     */
    private static RenderPipeline buildWire(boolean depthTested) {
        return RenderPipeline.builder()
                .withLocation(Identifier.fromNamespaceAndPath("unigui",
                        "pipeline/scene_mesh_wire" + (depthTested ? "" : "_overlay")))
                .withVertexShader(WIRE_SHADER)
                .withFragmentShader(WIRE_SHADER)
                .withUniform("SceneMesh", UniformType.UNIFORM_BUFFER)
                .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL, VertexFormat.Mode.TRIANGLES)
                .withPolygonMode(PolygonMode.WIREFRAME)
                .withColorTargetState(new ColorTargetState(MinecraftUiBlend.PREMULTIPLIED.function()))
                .withDepthStencilState(new DepthStencilState(
                        depthTested ? CompareOp.LESS_THAN_OR_EQUAL : CompareOp.ALWAYS_PASS, false))
                .withCull(false)
                .build();
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
