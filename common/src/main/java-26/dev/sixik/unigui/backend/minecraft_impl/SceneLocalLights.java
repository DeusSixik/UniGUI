package dev.sixik.unigui.backend.minecraft_impl;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import dev.sixik.unigui.api.math.MutableMat4;
import dev.sixik.unigui.api.render.RenderTargetOptions;
import dev.sixik.unigui.api.scene.LightNode;
import dev.sixik.unigui.api.scene.LocalLight;
import dev.sixik.unigui.api.scene.Scene;
import dev.sixik.unigui.api.scene.SceneNode;
import dev.sixik.unigui.api.scene.SpotLight;
import dev.sixik.unigui.api.scene.SpotShadows;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.function.Consumer;

/**
 * Collects and packs the local lights once per scene render, sharing one GPU block across color draws.
 */
final class SceneLocalLights implements AutoCloseable {
    /**
     * Number of spotlight depth maps that can be bound alongside the directional map.
     */
    static final int MAX_SHADOWS = 2;
    /**
     * Depth-bearing target reused for a spotlight shadow pass.
     */
    private static final RenderTargetOptions SHADOW_OPTIONS = new RenderTargetOptions(true, false, "scene_spot_shadow");
    /**
     * std140 header plus six vec4s and one mat4 per light.
     */
    private static final int UNIFORM_BYTES = 16 + UiMesh3DShaders.MAX_LOCAL_LIGHTS * SceneLightData.BYTES;
    /**
     * Resolver shared with scene materials.
     */
    private final UiTextureBinding.Cache textures;
    /**
     * Selected lights in traversal order.
     */
    private final SceneLightData[] entries = new SceneLightData[UiMesh3DShaders.MAX_LOCAL_LIGHTS];
    /**
     * One binding per projected texture used this frame.
     */
    private final UiTextureBinding[] masks = new UiTextureBinding[UiMesh3DShaders.MAX_LOCAL_LIGHTS];
    /**
     * Persistent shadow targets by sampler slot.
     */
    private final MinecraftRenderTarget[] shadows = new MinecraftRenderTarget[MAX_SHADOWS];
    /**
     * Reusable native-order staging buffer.
     */
    private final ByteBuffer bytes = ByteBuffer.allocateDirect(UNIFORM_BYTES).order(ByteOrder.nativeOrder());
    /**
     * Traversal callback reused across frames.
     */
    private final Consumer<SceneNode> collect = this::collect;
    /**
     * Device uniform range for the current color pass, or null before upload.
     */
    private GpuBufferSlice uniforms;
    /**
     * Active destination checked to prevent texture feedback.
     */
    private UiGpu.Target output;
    /**
     * Number of active local lights.
     */
    private int count;
    /**
     * Number of projected textures to bind.
     */
    private int maskCount;
    /**
     * Number of completed spotlight shadow maps to bind.
     */
    private int shadowCount;

    /**
     * @param textures resolver for projected textures
     */
    SceneLocalLights(UiTextureBinding.Cache textures) {
        this.textures = textures;
        Arrays.setAll(entries, i -> new SceneLightData());
    }

    /**
     * Collects lights from all visible branches, including lights outside the viewport.
     *
     * @param scene  scene whose visible lights to collect
     * @param output color pass target
     */
    void begin(Scene scene, UiGpu.Target output) {
        this.output = output;
        scene.visitVisible(null, collect);
    }

    /**
     * Resolves one light and its texture. An unavailable projection is skipped until the texture can be bound.
     *
     * @param node the next visible scene node
     */
    private void collect(SceneNode node) {
        if (count == entries.length || !(node instanceof LightNode source)) return;
        LocalLight light = source.light();
        if (light.intensity() == 0 || light.colorRgb() == 0) return;
        SceneLightData entry = entries[count];
        if (!entry.transform.update(source)) return;
        if (light instanceof SpotLight spot && spot.mask() != null) {
            var mask = textures.resolve(spot.mask().base());
            if (mask == null || mask.view().texture() == output.color().texture()) return;
            entry.maskIndex = ++maskCount;
            masks[maskCount - 1] = mask;
            entry.premultiplied = mask.premultipliedAlpha();
            if (!spot.mask().resolveUv(entry.uv)) entry.uv.set(0, 0, 1, 1);
            if (mask.flipY()) entry.uv.set(entry.uv.x(), 1 - entry.uv.y(), entry.uv.width(), -entry.uv.height());
        }
        entry.light = light;
        count++;
    }

    /**
     * @return number of active lights for the current color pass
     */
    int count() {
        return count;
    }

    /**
     * @return number of mask samplers required for the current color pass
     */
    int maskCount() {
        return maskCount;
    }

    /**
     * @return number of shadow samplers required for the current color pass
     */
    int shadowCount() {
        return shadowCount;
    }

    /**
     * @return index of the first active spotlight in scene order, or -1
     */
    int fogSpotlight() {
        for (int i = 0; i < count; i++) {
            if (entries[i].light instanceof SpotLight) return i;
        }
        return -1;
    }

    /**
     * Encodes texture bindings for a light's fog shader variant.
     *
     * @param index spotlight index, or -1 for ambient fog
     * @return bit zero for a spotlight, bit one for a mask, bit two for a depth map
     */
    int fogVariant(int index) {
        if (index < 0) return 0;
        SceneLightData entry = entries[index];
        return 1 | (entry.maskIndex > 0 ? 2 : 0) | (entry.shadowIndex > 0 ? 4 : 0);
    }

    /**
     * Binds the shared light block and the spotlight's existing textures for volume sampling.
     *
     * @param pass  atmospheric integration pass
     * @param index selected active spotlight index
     */
    void bindFogSpotlight(RenderPass pass, int index) {
        SceneLightData entry = entries[index];
        pass.setUniform("SceneLocalLights", uniforms);
        if (entry.maskIndex > 0) {
            var mask = masks[entry.maskIndex - 1];
            MinecraftGpuCompat.bindTexture(pass, "FogSpotMask", mask.view(), mask.sampler());
        }
        if (entry.shadowIndex > 0) {
            MinecraftGpuCompat.bindTexture(pass, "FogSpotDepth",
                    shadows[entry.shadowIndex - 1].nativeTarget().getDepthTextureView(),
                    RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
        }
    }

    /**
     * Retrieves the shadow settings for an active light if a map slot is available.
     *
     * @param index zero-based active light index
     * @return settings, or null if shadows are disabled for this light or the slots are full
     */
    SpotShadows shadowSettings(int index) {
        if (shadowCount == shadows.length || !(entries[index].light instanceof SpotLight spot)) return null;
        var settings = spot.shadows();
        return settings != null && settings.strength() > 0 ? settings : null;
    }

    /**
     * Resolves the storage for the next shadow map.
     *
     * @param resolution square shadow texture side in texels
     * @return depth-bearing target for the next spotlight
     */
    MinecraftRenderTarget shadowTarget(int resolution) {
        var target = shadows[shadowCount];
        if (target == null || target.width() != resolution) {
            shadows[shadowCount] = new MinecraftRenderTarget(resolution, resolution, SHADOW_OPTIONS);
            if (target != null) RenderSystem.queueFencedTask(target::close);
        }
        return shadows[shadowCount];
    }

    /**
     * Reads the projection shared by a spotlight map and its mask.
     *
     * @param index zero-based active spotlight index
     * @return borrowed world-to-clip transform
     */
    MutableMat4 shadowClip(int index) {
        return entries[index].transform.clip();
    }

    /**
     * Marks the current shadow slot as populated.
     *
     * @param index zero-based active spotlight index
     */
    void markShadowRendered(int index) {
        entries[index].shadowIndex = ++shadowCount;
    }

    /**
     * Uploads the camera-independent local light block once before opening the color pass.
     */
    void upload() {
        if (count == 0) return;
        bytes.clear();
        bytes.putInt(count).putInt(0).putLong(0);
        for (int i = 0; i < count; i++) entries[i].write(bytes);
        bytes.position(0);
        uniforms = UiGpu.uploadUniform(bytes);
    }

    /**
     * Binds the shared scene block and the textures declared by the active shader variant.
     *
     * @param pass current color pass
     */
    void bind(RenderPass pass) {
        pass.setUniform("SceneLocalLights", uniforms);
        for (int i = 0; i < maskCount; i++) {
            var mask = masks[i];
            MinecraftGpuCompat.bindTexture(pass, UiMesh3DShaders.LOCAL_MASK_NAMES[i], mask.view(), mask.sampler());
        }
        for (int i = 0; i < shadowCount; i++) {
            MinecraftGpuCompat.bindTexture(pass, UiMesh3DShaders.LOCAL_SHADOW_NAMES[i],
                    shadows[i].nativeTarget().getDepthTextureView(),
                    RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
        }
    }

    /**
     * Releases per-frame references while keeping the reusable storage.
     */
    void reset() {
        Arrays.fill(masks, 0, maskCount, null);
        for (int i = 0; i < count; i++) entries[i].reset();
        output = null;
        uniforms = null;
        count = maskCount = shadowCount = 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        reset();
        for (var target : shadows) {
            if (target != null) target.close();
        }
        Arrays.fill(shadows, null);
    }

}
