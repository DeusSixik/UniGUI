package dev.sixik.unigui.backend.minecraft_impl;

import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.BindGroupLayout;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.pipeline.CompareOp;
import com.mojang.renderpearl.api.pipeline.PolygonMode;
import com.mojang.renderpearl.api.pipeline.UniformType;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.TextureTransform;
import net.minecraft.resources.Identifier;

import java.nio.ByteBuffer;
import java.util.LinkedHashMap;

/**
 * Пайплайны мешей Minecraft 26.3. В прозрачном проходе тест глубины остаётся включён.
 */
final class UiMesh3DPipeline {
    /**
     * Minecraft 26.3 compiles the advanced surface shaders through RenderPearl.
     */
    static final boolean SUPPORTS_SURFACE_EFFECTS = true;
    /**
     * Maximum number of compiled advanced shader variants kept between frames.
     */
    private static final int SURFACE_CACHE_SIZE = 64;
    /**
     * LRU cache of source variants, used exclusively on the render thread.
     */
    private static final LinkedHashMap<SurfaceKey, UiCustomPipeline> SURFACES =
            new LinkedHashMap<>(16, 0.75f, true);
    /**
     * Unique suffix for advanced shader and pipeline names, even after a cache eviction.
     */
    private static long nextSurfaceId;
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
    private static final BindGroupLayout SAMPLER = BindGroupLayout.builder()
            .withUniform("Sampler0", UniformType.COMBINED_IMAGE_SAMPLER).build();
    /**
     * Second sampler used by native items and armor with integrated glint.
     */
    private static final BindGroupLayout GLINT_SAMPLER = BindGroupLayout.builder()
            .withUniform("GlintSampler", UniformType.COMBINED_IMAGE_SAMPLER).build();
    /**
     * World, shadow and user parameters plus the shadow sampler for receivers.
     */
    private static final BindGroupLayout SURFACE_RECEIVER = BindGroupLayout.builder()
            .withUniform("SceneSurface", UniformType.UNIFORM_BUFFER)
            .withUniform("SceneShadowMap", UniformType.COMBINED_IMAGE_SAMPLER).build();
    /**
     * Surface parameters for materials that do not sample the shadow map.
     */
    private static final BindGroupLayout SURFACE = BindGroupLayout.builder()
            .withUniform("SceneSurface", UniformType.UNIFORM_BUFFER).build();
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
     * @return lazy atmospheric pass
     */
    static SceneFogPass createFogPass() {
        return new GpuSceneFogPass();
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
     * Compiles or reuses the surface variant for a color or shadow draw.
     *
     * @param textured      whether to bind the base material texture
     * @param translucent   whether to blend the output without writing depth
     * @param shadowPass    whether to write shadow depth
     * @param receiveShadow whether to bind a shadow map in the color pass
     * @param style         native depth and glint behavior, or null for ordinary scene geometry
     * @param localLights   whether to bind the shared local lights block
     * @param masks         number of spotlight mask samplers
     * @param spotShadows   number of spotlight shadow samplers
     * @param source        GLSL surface function, or null to use the default surface
     * @return pipeline, or null if GLSL compilation failed
     */
    static RenderPipeline getSurface(boolean textured, boolean translucent, boolean shadowPass,
                                     boolean receiveShadow, SceneNativeLayer.Style style, boolean localLights,
                                     int masks, int spotShadows, String source) {
        boolean integrated = !shadowPass && textured && style != null && style.integratedGlint();
        boolean overlay = !shadowPass && textured && style != null && style.glintOverlay();
        boolean equal = !shadowPass && style != null && style.depthEqual;
        SurfaceKey key = new SurfaceKey(textured, shadowPass, receiveShadow, style != null, integrated, overlay, equal,
                localLights, masks, spotShadows, source);
        UiCustomPipeline variant = SURFACES.computeIfAbsent(key, UiMesh3DPipeline::createSurface);
        return variant.get(translucent ? MinecraftUiBlend.PREMULTIPLIED : MinecraftUiBlend.REPLACE);
    }

    /**
     * @return enchantment glint intensity in [0, 1] from the current Minecraft frame
     */
    static float glintStrength() {
        return (float) Minecraft.getInstance().gameRenderer.gameRenderState().optionsRenderState.glintStrength;
    }

    /**
     * Packs the two affine rows of the native texture matrix as std140 vec4s.
     * For UV {@code (u, v)} each row evaluates {@code m0*u + m1*v + translation}.
     *
     * @param out   destination with 32 writable bytes
     * @param glint selects the Minecraft UV matrix and animation scale, or NONE for an unused block
     */
    static void writeGlintUv(ByteBuffer out, SceneNativeLayer.Glint glint) {
        var transform = switch (glint) {
            case ITEM -> TextureTransform.GLINT_TEXTURING;
            case ENTITY -> TextureTransform.ENTITY_GLINT_TEXTURING;
            case ARMOR -> TextureTransform.ARMOR_ENTITY_GLINT_TEXTURING;
            case NONE -> null;
        };
        if (transform == null) {
            for (int i = 0; i < 4; i++) out.putLong(0);
            return;
        }
        var matrix = transform.createMatrix();
        out.putFloat(matrix.m00()).putFloat(matrix.m10()).putFloat(matrix.m30()).putFloat(0);
        out.putFloat(matrix.m01()).putFloat(matrix.m11()).putFloat(matrix.m31()).putFloat(0);
    }

    /**
     * Trims the surface cache after the current scene has submitted all draws that reference it.
     */
    static void trimSurfaces() {
        while (SURFACES.size() > SURFACE_CACHE_SIZE) {
            SURFACES.pollFirstEntry().getValue().releaseAfterFrame();
        }
    }

    /**
     * Assembles a surface GLSL variant with the matching texture and uniform bindings.
     *
     * @param key shader options
     * @return cache entry for opaque and translucent modes
     */
    private static UiCustomPipeline createSurface(SurfaceKey key) {
        String name = "scene_surface_" + nextSurfaceId++;
        Identifier shader = Identifier.fromNamespaceAndPath("unigui", name);
        BindGroupLayout local = key.localLights ? localLightBindings(key.masks, key.spotShadows) : null;
        return new UiCustomPipeline(name, shader, UiMesh3DShaders.surfaceVertex(),
                UiMesh3DShaders.surfaceFragment(key.textured, key.shadowPass, key.receiveShadow, key.nativeCutoff,
                        key.integratedGlint, key.glintOverlay, key.localLights, key.masks, key.spotShadows, key.source),
                blend -> {
                    var builder = builder(name, shader, key.textured, false, blend)
                            .withBindGroupLayout(key.receiveShadow ? SURFACE_RECEIVER : SURFACE);
                    if (key.integratedGlint) builder.withBindGroupLayout(GLINT_SAMPLER);
                    if (local != null) builder.withBindGroupLayout(local);
                    if (key.depthEqual) builder.withDepthStencilState(new DepthStencilState(CompareOp.EQUAL, false));
                    if (key.glintOverlay) builder.withColorTargetState(new ColorTargetState(BlendFunction.GLINT));
                    return builder.build();
                });
    }

    /**
     * Builds the layout for the shared local lights block and its texture slots.
     *
     * @param masks   number of projected mask textures
     * @param shadows number of perspective shadow maps
     * @return complete local-light bindings for the color pipeline
     */
    private static BindGroupLayout localLightBindings(int masks, int shadows) {
        var builder = BindGroupLayout.builder().withUniform("SceneLocalLights", UniformType.UNIFORM_BUFFER);
        for (int i = 0; i < masks; i++) {
            builder.withUniform(UiMesh3DShaders.LOCAL_MASK_NAMES[i], UniformType.COMBINED_IMAGE_SAMPLER);
        }
        for (int i = 0; i < shadows; i++) {
            builder.withUniform(UiMesh3DShaders.LOCAL_SHADOW_NAMES[i], UniformType.COMBINED_IMAGE_SAMPLER);
        }
        return builder.build();
    }

    /**
     * Identifies a surface program independently of shader parameters and blending.
     *
     * @param textured        whether GLSL samples the base texture
     * @param shadowPass      whether the fragment stage only contributes shadow depth
     * @param receiveShadow   whether GLSL samples directional shadows
     * @param nativeCutoff    whether translucent fragments have a native alpha cutoff
     * @param integratedGlint whether GLSL samples a separate foil texture
     * @param glintOverlay    whether the only color output is animated foil
     * @param depthEqual      whether a matching base surface must have written depth
     * @param localLights     whether to accumulate point and spot lights
     * @param masks           number of projected texture samplers
     * @param spotShadows     number of perspective depth samplers
     * @param source          optional GLSL function definition
     */
    private record SurfaceKey(boolean textured, boolean shadowPass, boolean receiveShadow, boolean nativeCutoff,
                              boolean integratedGlint, boolean glintOverlay, boolean depthEqual,
                              boolean localLights, int masks, int spotShadows, String source) {
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
                blend -> builder(name, shader, textured, instanced, blend).build());
    }

    /**
     * Uses Minecraft's reversed depth, where nearer fragments have larger values.
     *
     * @param name      variant name
     * @param shader    GLSL source identifier
     * @param textured  whether a texture sampler is required
     * @param instanced whether an instance matrix block is required
     * @param blend     blend state
     * @return pipeline builder
     */
    private static RenderPipeline.Builder builder(String name, Identifier shader, boolean textured,
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
        return pipeline;
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
