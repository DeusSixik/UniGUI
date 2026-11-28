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
import dev.sixik.unigui.api.math.Mat4View;
import dev.sixik.unigui.api.math.Vec3View;
import dev.sixik.unigui.api.scene.Camera3D;
import dev.sixik.unigui.impl.render.SceneMeshData;
import it.unimi.dsi.fastutil.objects.ObjectArrays;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.Identifier;
import org.joml.Quaternionfc;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Comparator;
import java.util.function.Supplier;

/**
 * Batches native atlas sprites for premultiplied rendering with sampled scene depth.
 */
final class GpuSceneSoftParticles implements AutoCloseable {
    /**
     * Debug label for the color-only pass.
     */
    private static final Supplier<String> PASS = () -> "UniGUI soft scene particles";
    /**
     * More negative camera-space Z comes first for alpha compositing.
     */
    private static final Comparator<Sprite> BACK_TO_FRONT = Comparator.comparingDouble(sprite -> sprite.z);
    /**
     * Program shared by all offscreen scenes.
     */
    private static final UiCustomPipeline PIPELINE = createPipeline();
    /**
     * Active sprites occupy the prefix of the array. Entries are reused across frames.
     */
    private Sprite[] sprites = new Sprite[32];
    /**
     * Uploaded blocks, one per instanced draw.
     */
    private GpuBufferSlice[] chunks = new GpuBufferSlice[4];
    /**
     * Reusable staging memory for the larger of the two uniform blocks.
     */
    private final ByteBuffer bytes = ByteBuffer.allocateDirect(SceneSoftParticleShaders.BATCH_BYTES)
            .order(ByteOrder.nativeOrder());
    /**
     * Number of sprites collected for this frame.
     */
    private int count;
    /**
     * Positions of the six vertices shared by all billboard instances.
     */
    private GpuBuffer quad;

    /**
     * @return true if the soft shader compiles for the active renderer
     */
    static boolean available() {
        return PIPELINE.get(MinecraftUiBlend.PREMULTIPLIED) != null;
    }

    /**
     * Adds one billboard with UVs and color from the native sample and shape from the emitter.
     *
     * @param center   camera-space center in scene blocks
     * @param rotation native camera-facing orientation
     * @param size     native half-extent in scene blocks
     * @param frame    native sprite UV rectangle and tint
     * @param settings soft intersection and billboard properties
     */
    void add(Vec3View center, Quaternionfc rotation, float size, SceneParticles.Frame frame,
             MinecraftParticleEmitter.SoftParticles settings) {
        float halfWidth = size * settings.widthScale();
        float halfHeight = size * settings.heightScale();
        if (!Float.isFinite(halfWidth) || !Float.isFinite(halfHeight)) return;
        if (count == sprites.length) sprites = Arrays.copyOf(sprites, sprites.length * 2);
        Sprite sprite = sprites[count];
        if (sprite == null) sprites[count] = sprite = new Sprite();
        sprite.z = (float) center.z();
        sprite.values[0] = (float) center.x();
        sprite.values[1] = (float) center.y();
        sprite.values[2] = sprite.z;
        sprite.values[3] = settings.fadeDistance();
        sprite.values[4] = halfWidth;
        sprite.values[5] = halfHeight;
        sprite.values[8] = rotation.x();
        sprite.values[9] = rotation.y();
        sprite.values[10] = rotation.z();
        sprite.values[11] = rotation.w();
        sprite.values[12] = frame.u0();
        sprite.values[13] = frame.v0();
        sprite.values[14] = frame.u1();
        sprite.values[15] = frame.v1();
        int color = frame.color();
        sprite.values[16] = (color >> 16 & 255) / 255f;
        sprite.values[17] = (color >> 8 & 255) / 255f;
        sprite.values[18] = (color & 255) / 255f;
        sprite.values[19] = (color >>> 24) / 255f * settings.opacity();
        count++;
    }

    /**
     * Draws the sorted soft sprites over scene color. Depth is sampled with a nearest sampler
     * and omitted from the pass attachments, so the sprite can fade against each opaque pixel.
     *
     * @param target     scene color and depth
     * @param projection canonical camera projection
     * @param camera     mode and clipping distances
     * @param width      active viewport width in pixels
     * @param height     active viewport height in pixels
     */
    void render(UiGpu.Target target, Mat4View projection, Camera3D camera, float width, float height) {
        if (count == 0) return;
        RenderPipeline pipeline = PIPELINE.get(MinecraftUiBlend.PREMULTIPLIED);
        if (pipeline == null) return;
        ObjectArrays.quickSort(sprites, 0, count, BACK_TO_FRONT);
        GpuBufferSlice cameraBlock = uploadCamera(projection, camera, width / target.width(), height / target.height());
        int batchSize = SceneSoftParticleShaders.BATCH_SIZE;
        int batches = Math.ceilDiv(count, batchSize);
        if (chunks.length < batches) chunks = Arrays.copyOf(chunks, batches);
        for (int i = 0; i < batches; i++) {
            chunks[i] = uploadSprites(i * batchSize, Math.min((i + 1) * batchSize, count));
        }
        if (quad == null || quad.isClosed()) {
            quad = UiCustomPipeline.staticVertices("UniGUI soft particle quad",
                    -1, -1, 0, 1, -1, 0, 1, 1, 0, -1, -1, 0, 1, 1, 0, -1, 1, 0);
        }
        var atlas = Minecraft.getInstance().getTextureManager().getTexture(TextureAtlas.LOCATION_PARTICLES);
        int pixelsWide = Math.clamp((int) Math.ceil(width), 1, target.width());
        int pixelsHigh = Math.clamp((int) Math.ceil(height), 1, target.height());
        try (RenderPass pass = MinecraftGpuCompat.openPass(PASS, target.color(), null)) {
            MinecraftGpuCompat.setPipeline(pass, pipeline);
            pass.enableScissor(0, target.height() - pixelsHigh, pixelsWide, pixelsHigh);
            pass.setUniform("SoftCamera", cameraBlock);
            pass.setVertexBuffer(0, quad.slice());
            var samplers = RenderSystem.getSamplerCache();
            MinecraftGpuCompat.bindTexture(pass, "SoftAtlas", atlas.getTextureView(), samplers.getClampToEdge(FilterMode.LINEAR));
            MinecraftGpuCompat.bindTexture(pass, "SoftSceneDepth", target.depth(), samplers.getClampToEdge(FilterMode.NEAREST));
            for (int i = 0; i < batches; i++) {
                pass.setUniform("SoftSprites", chunks[i]);
                pass.draw(6, Math.min(batchSize, count - i * batchSize), 0, 0);
            }
        } finally {
            Arrays.fill(chunks, 0, batches, null);
        }
        PIPELINE.drawn();
    }

    /**
     * Packs camera values in the std140 order used by the shader.
     *
     * @param projection canonical perspective or orthographic projection
     * @param camera     camera supplying clip planes and projection mode
     * @param viewportX  viewport width divided by backing texture width
     * @param viewportY  viewport height divided by backing texture height
     * @return the uploaded camera uniform slice
     */
    private GpuBufferSlice uploadCamera(Mat4View projection, Camera3D camera, float viewportX, float viewportY) {
        bytes.clear();
        SceneMeshData.writeClipMatrix(bytes, projection);
        bytes.putFloat(viewportX).putFloat(viewportY).putFloat(camera.near()).putFloat(camera.far());
        bytes.putFloat(camera.orthographic() ? 1 : 0).putFloat(MinecraftGpuCompat.zZeroToOne() ? 1 : 0)
                .putFloat(0).putFloat(0);
        bytes.flip();
        return UiGpu.uploadUniform(bytes);
    }

    /**
     * Uploads up to one shader block of sorted sprites.
     *
     * @param start first sprite index, inclusive
     * @param end   last sprite index, exclusive
     * @return uploaded sprite slice with the declared block size
     */
    private GpuBufferSlice uploadSprites(int start, int end) {
        bytes.clear();
        for (int i = start; i < end; i++) {
            for (float value : sprites[i].values) bytes.putFloat(value);
        }
        bytes.position(0);
        return UiGpu.uploadUniform(bytes);
    }

    /**
     * Clears the active prefix after drawing or aborting a frame.
     */
    void reset() {
        count = 0;
    }

    /**
     * Releases the billboard mesh. Uniform buffers are owned by the shared stream.
     */
    @Override
    public void close() {
        if (quad != null) quad.close();
        reset();
    }

    /**
     * @return the custom shader with its vertex layout and depth/atlas bindings
     */
    private static UiCustomPipeline createPipeline() {
        String name = "scene_soft_particles";
        Identifier shader = Identifier.fromNamespaceAndPath("unigui", name);
        BindGroupLayout bindings = BindGroupLayout.builder()
                .withUniform("SoftCamera", UniformType.UNIFORM_BUFFER)
                .withUniform("SoftSprites", UniformType.UNIFORM_BUFFER)
                .withUniform("SoftAtlas", UniformType.COMBINED_IMAGE_SAMPLER)
                .withUniform("SoftSceneDepth", UniformType.COMBINED_IMAGE_SAMPLER).build();
        return new UiCustomPipeline(name, shader, SceneSoftParticleShaders.VERTEX, SceneSoftParticleShaders.FRAGMENT,
                blend -> RenderPipeline.builder()
                        .withLocation(Identifier.fromNamespaceAndPath("unigui", "pipeline/" + name))
                        .withVertexShader(shader).withFragmentShader(shader).withBindGroupLayout(bindings)
                        .withPrimitiveTopology(PrimitiveTopology.TRIANGLES).withVertexBinding(0, DefaultVertexFormat.POSITION)
                        .withCull(false).withColorTargetState(new ColorTargetState(blend.function())).build());
    }

    /**
     * Reused sprite staging data in shader field order.
     */
    private static final class Sprite {
        /**
         * Packed center with fade distance, half-extents, quaternion, UV rectangle and tint.
         */
        private final float[] values = new float[20];
        /**
         * Camera-space depth in scene blocks for sorted blending.
         */
        private float z;
    }
}
