package dev.sixik.unigui.backend.minecraft_impl;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.pipeline.BindGroupLayout;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.pipeline.UniformType;
import com.mojang.renderpearl.api.textures.FilterMode;
import dev.sixik.unigui.api.math.Frustum3D;
import dev.sixik.unigui.api.math.Mat4View;
import dev.sixik.unigui.api.render.RenderTargetOptions;
import dev.sixik.unigui.api.scene.Camera3D;
import dev.sixik.unigui.api.scene.FogVolume;
import net.minecraft.resources.Identifier;

import java.util.function.Supplier;

/**
 * Integrates at half spatial resolution, then blends with depth-aware reconstruction.
 */
final class GpuSceneFogPass implements SceneFogPass {
    /**
     * Debug label for the half-resolution draw.
     */
    private static final Supplier<String> INTEGRATE = () -> "UniGUI fog integration";
    /**
     * Debug label for blending fog into scene color.
     */
    private static final Supplier<String> COMPOSITE = () -> "UniGUI fog composite";
    /**
     * Half-resolution color scratch, overwritten in its entirety by integration.
     */
    private static final RenderTargetOptions FOG_TARGET = new RenderTargetOptions(false, false, "scene_fog");
    /**
     * Integration programs by spotlight resource flags.
     */
    private static final UiCustomPipeline[] variants = new UiCustomPipeline[8];
    /**
     * Program for premultiplied blending into an existing scene.
     */
    private static final UiCustomPipeline composite = pipeline(true, 0);

    /**
     * Shared fog selection and uniform staging.
     */
    private final SceneFogData data = new SceneFogData();
    /**
     * Static positions for a clip-space triangle, allocated on the first fog render.
     */
    private GpuBuffer triangle;
    /**
     * Reusable half-resolution medium color and transmittance.
     */
    private MinecraftRenderTarget scratch;

    /**
     * {@inheritDoc}
     */
    @Override
    public void collect(FogVolume volume, Frustum3D frustum) {
        data.collect(volume, frustum);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void render(UiGpu.Target output, Mat4View viewProjection, Camera3D camera, float seconds,
                       SceneLocalLights lights, float width, float height) {
        if (!data.prepare(viewProjection)) return;
        int spot = data.scattering() && lights != null ? lights.fogSpotlight() : -1;
        int flags = spot < 0 ? 0 : lights.fogVariant(spot);
        UiCustomPipeline integration = variants[flags];
        if (integration == null) variants[flags] = integration = pipeline(false, flags);
        RenderPipeline march = integration.get(MinecraftUiBlend.REPLACE);
        RenderPipeline blend = composite.get(MinecraftUiBlend.PREMULTIPLIED);
        if (march == null || blend == null) return;

        int pixelsWide = Math.clamp((int) Math.ceil(width), 1, output.width());
        int pixelsHigh = Math.clamp((int) Math.ceil(height), 1, output.height());
        UiGpu.Target reduced = scratch((pixelsWide + 1) / 2, (pixelsHigh + 1) / 2);
        GpuBufferSlice fogUniforms = UiGpu.uploadUniform(
                data.write(width / output.width(), height / output.height(), camera, seconds, spot));
        if (triangle == null || triangle.isClosed()) {
            triangle = UiCustomPipeline.staticVertices("UniGUI fog triangle", -1, -1, 0, 3, -1, 0, -1, 3, 0);
        }
        try (RenderPass pass = MinecraftGpuCompat.openPass(INTEGRATE, reduced.color(), null)) {
            bind(pass, march, output, fogUniforms);
            if (spot >= 0) lights.bindFogSpotlight(pass, spot);
            pass.draw(3, 1, 0, 0);
        }
        try (RenderPass pass = MinecraftGpuCompat.openPass(COMPOSITE, output.color(), null)) {
            bind(pass, blend, output, fogUniforms);
            pass.enableScissor(0, output.height() - pixelsHigh, pixelsWide, pixelsHigh);
            MinecraftGpuCompat.bindTexture(pass, "FogIntegrated", reduced.color(),
                    RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
            pass.draw(3, 1, 0, 0);
        }
        integration.drawn();
    }

    /**
     * Binds source depth as a sampler. Neither fog pass attaches that depth texture as a render target.
     *
     * @param pass        open fog pass
     * @param pipeline    compiled pass state
     * @param output      original scene target with opaque depth
     * @param fogUniforms frame uniform slice
     */
    private void bind(RenderPass pass, RenderPipeline pipeline, UiGpu.Target output, GpuBufferSlice fogUniforms) {
        MinecraftGpuCompat.setPipeline(pass, pipeline);
        pass.setVertexBuffer(0, triangle.slice());
        pass.setUniform("SceneFog", fogUniforms);
        MinecraftGpuCompat.bindTexture(pass, "FogSceneDepth", output.depth(),
                RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
    }

    /**
     * Gets scratch storage at the current integration resolution.
     *
     * @param width  half-resolution width in pixels
     * @param height half-resolution height in pixels
     * @return color-only target
     */
    private UiGpu.Target scratch(int width, int height) {
        if (scratch == null || scratch.width() != width || scratch.height() != height) {
            var previous = scratch;
            scratch = new MinecraftRenderTarget(width, height, FOG_TARGET);
            if (previous != null) RenderSystem.queueFencedTask(previous::close);
        }
        return scratch.gpuTarget();
    }

    /**
     * Assembles a RenderPearl program that binds only the resources used by the shader.
     *
     * @param blend whether this is the compositing pass
     * @param flags texture bindings for the integration pass
     * @return backed shader variant
     */
    private static UiCustomPipeline pipeline(boolean blend, int flags) {
        String name = blend ? "scene_fog_composite" : "scene_fog_integrate_" + flags;
        Identifier shader = Identifier.fromNamespaceAndPath("unigui", name);
        var layout = BindGroupLayout.builder().withUniform("SceneFog", UniformType.UNIFORM_BUFFER)
                .withUniform("FogSceneDepth", UniformType.COMBINED_IMAGE_SAMPLER);
        if (blend) layout.withUniform("FogIntegrated", UniformType.COMBINED_IMAGE_SAMPLER);
        if ((flags & SceneFogShaders.SPOT) != 0) layout.withUniform("SceneLocalLights", UniformType.UNIFORM_BUFFER);
        if ((flags & SceneFogShaders.MASK) != 0) layout.withUniform("FogSpotMask", UniformType.COMBINED_IMAGE_SAMPLER);
        if ((flags & SceneFogShaders.SHADOW) != 0)
            layout.withUniform("FogSpotDepth", UniformType.COMBINED_IMAGE_SAMPLER);
        var bindings = layout.build();
        return new UiCustomPipeline(name, shader, SceneFogShaders.vertex(blend),
                blend ? SceneFogShaders.COMPOSITE : SceneFogShaders.integrate(flags),
                mode -> RenderPipeline.builder()
                        .withLocation(Identifier.fromNamespaceAndPath("unigui", "pipeline/" + name))
                        .withVertexShader(shader).withFragmentShader(shader)
                        .withBindGroupLayout(bindings).withCull(false)
                        .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
                        .withVertexBinding(0, DefaultVertexFormat.POSITION)
                        .withColorTargetState(blend ? new ColorTargetState(mode.function()) : ColorTargetState.DEFAULT)
                        .build());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void reset() {
        data.reset();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        if (triangle != null) triangle.close();
        if (scratch != null) scratch.close();
        reset();
    }
}
