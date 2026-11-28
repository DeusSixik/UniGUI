package dev.sixik.unigui.backend.minecraft_impl;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.BindGroupLayout;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.platform.PolygonMode;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.resources.Identifier;

import java.nio.ByteBuffer;


/**
 * Пайплайны мешей Minecraft 26.2. В прозрачном проходе тест глубины остаётся включён.
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
     * std140 блок матриц, света и материала.
     */
    private static final BindGroupLayout SCENE = BindGroupLayout.builder()
            .withUniform("SceneMesh", UniformType.UNIFORM_BUFFER).build();
    /**
     * Матрицы экземпляров, общие для одного вызова GPU.
     */
    private static final BindGroupLayout INSTANCES = BindGroupLayout.builder()
            .withUniform("SceneInstances", UniformType.UNIFORM_BUFFER).build();
    /**
     * Привязка текстуры в варианте с UV.
     */
    private static final BindGroupLayout SAMPLER = BindGroupLayouts.SAMPLER0;
    /**
     * Пайплайн мешей без текстуры.
     */
    private static final UiCustomPipeline SOLID = create(false, false);
    /**
     * Пайплайн мешей с текстурой.
     */
    private static final UiCustomPipeline TEXTURED = create(true, false);
    /**
     * Инстансированный пайплайн без текстуры.
     */
    private static final UiCustomPipeline INSTANCED_SOLID = create(false, true);
    /**
     * Инстансированный пайплайн с текстурой.
     */
    private static final UiCustomPipeline INSTANCED_TEXTURED = create(true, true);

    /**
     * Depth-tested wireframe edges.
     */
    private static final UiCustomPipeline WIRE_TESTED = createWire(true);
    /**
     * Wireframe edges drawn over the whole scene.
     */
    private static final UiCustomPipeline WIRE_OVERLAY = createWire(false);

    private UiMesh3DPipeline() {
    }

    /**
     * @return null because this backend has no atmospheric pass
     */
    static SceneFogPass createFogPass() {
        return null;
    }

    /**
     * Компилирует или достаёт из кэша нужный вариант.
     *
     * @param textured    нужен ли сэмплер.
     * @param translucent включить ли смешивание с выключенной записью глубины.
     * @param instanced   читать ли матрицы нескольких экземпляров.
     * @return пайплайн выбранного прохода.
     * @throws IllegalStateException если компиляция не удалась.
     */
    static RenderPipeline get(boolean textured, boolean translucent, boolean instanced) {
        UiCustomPipeline variant = textured ? (instanced ? INSTANCED_TEXTURED : TEXTURED)
                : (instanced ? INSTANCED_SOLID : SOLID);
        RenderPipeline pipeline = variant.get(translucent ? MinecraftUiBlend.PREMULTIPLIED : MinecraftUiBlend.REPLACE);
        if (pipeline == null) throw new IllegalStateException("Could not compile UniGUI 3D mesh pipeline");
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
        RenderPipeline pipeline = (depthTested ? WIRE_TESTED : WIRE_OVERLAY).get(MinecraftUiBlend.PREMULTIPLIED);
        if (pipeline == null) throw new IllegalStateException("Could not compile UniGUI wireframe pipeline");
        return pipeline;
    }

    /**
     * Создаёт кэш пайплайна с собственным источником GLSL.
     *
     * @param textured  нужен ли сэмплер.
     * @param instanced нужен ли блок матриц экземпляров.
     * @return компилятор для непрозрачного и прозрачного проходов.
     */
    private static UiCustomPipeline create(boolean textured, boolean instanced) {
        String name = "scene_mesh" + (textured ? "_textured" : "") + (instanced ? "_instanced" : "");
        Identifier shader = Identifier.fromNamespaceAndPath("unigui", name);
        return new UiCustomPipeline(name, shader, UiMesh3DShaders.vertex(330, SceneFeaturePass.REVERSED_DEPTH, instanced),
                UiMesh3DShaders.fragment(330, textured),
                blend -> build(name, shader, textured, instanced, blend));
    }

    /**
     * Использует обратную глубину Minecraft, ближние фрагменты имеют большие значения.
     *
     * @param name      имя варианта.
     * @param shader    идентификатор GLSL источника.
     * @param textured  нужен ли сэмплер.
     * @param instanced нужен ли блок матриц экземпляров.
     * @param blend     состояние смешивания.
     * @return описание пайплайна.
     */
    private static RenderPipeline build(String name, Identifier shader, boolean textured,
                                        boolean instanced, MinecraftUiBlend blend) {
        boolean opaque = blend == MinecraftUiBlend.REPLACE;
        RenderPipeline.Builder pipeline = RenderPipeline.builder()
                .withLocation(Identifier.fromNamespaceAndPath("unigui", "pipeline/" + name + (opaque ? "" : "_blend")))
                .withVertexShader(shader)
                .withFragmentShader(shader)
                .withBindGroupLayout(SCENE)
                .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL)
                .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
                .withColorTargetState(opaque ? ColorTargetState.DEFAULT : new ColorTargetState(blend.function()))
                .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, opaque))
                .withCull(false);
        if (textured) pipeline.withBindGroupLayout(SAMPLER);
        if (instanced) pipeline.withBindGroupLayout(INSTANCES);
        return pipeline.build();
    }

    /**
     * Builds the wireframe pipeline with the plain mesh vertex stage and a tint-only fragment stage.
     * Lines use Minecraft's reversed depth, so a nearer edge passes {@code GREATER_THAN_OR_EQUAL}.
     *
     * @param depthTested whether to compare against scene depth
     * @return pipeline cache for the wireframe pass
     */
    private static UiCustomPipeline createWire(boolean depthTested) {
        String name = "scene_mesh_wire" + (depthTested ? "" : "_overlay");
        Identifier shader = Identifier.fromNamespaceAndPath("unigui", name);
        return new UiCustomPipeline(name, shader, UiMesh3DShaders.vertex(330, SceneFeaturePass.REVERSED_DEPTH, false),
                UiMesh3DShaders.wireFragment(330),
                blend -> RenderPipeline.builder()
                        .withLocation(Identifier.fromNamespaceAndPath("unigui", "pipeline/" + name))
                        .withVertexShader(shader)
                        .withFragmentShader(shader)
                        .withBindGroupLayout(SCENE)
                        .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL)
                        .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
                        .withPolygonMode(PolygonMode.WIREFRAME)
                        .withColorTargetState(new ColorTargetState(blend.function()))
                        .withDepthStencilState(new DepthStencilState(
                                depthTested ? CompareOp.GREATER_THAN_OR_EQUAL : CompareOp.ALWAYS_PASS, false))
                        .withCull(false)
                        .build());
    }
}
