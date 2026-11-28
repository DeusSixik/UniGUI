package dev.sixik.unigui.backend.minecraft_impl;

import dev.sixik.unigui.api.math.MutableMat4;
import dev.sixik.unigui.api.scene.*;
import dev.sixik.unigui.impl.render.SceneShadowProjection;
import org.lwjgl.opengl.*;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.function.Consumer;

/**
 * Collects scene lights, renders their depth targets and binds the shared light block.
 */
final class GlSceneLighting implements AutoCloseable {
    /**
     * Uniform binding used by the local light block.
     */
    static final int BINDING = 3;
    /**
     * Texture unit for the directional shadow map.
     */
    private static final int DIRECTIONAL_UNIT = 1;
    /**
     * First unit reserved for projected masks.
     */
    private static final int MASK_UNIT = 2;
    /**
     * First unit for spot shadow maps.
     */
    private static final int SPOT_UNIT = MASK_UNIT + UiMesh3DShaders.MAX_LOCAL_LIGHTS;
    /**
     * Header and eight std140 light entries, in bytes.
     */
    private static final int BYTES = 16 + UiMesh3DShaders.MAX_LOCAL_LIGHTS * SceneLightData.BYTES;

    /**
     * Shared texture resolver.
     */
    private final GlScenePlatform platform;
    /**
     * State operations tracked by the client.
     */
    private final GlScenePlatform.State gl;
    /**
     * Selected lights in scene order.
     */
    private final SceneLightData[] lights = new SceneLightData[UiMesh3DShaders.MAX_LOCAL_LIGHTS];
    /**
     * Texture bindings packed in light order.
     */
    private final GlScenePlatform.Texture[] masks = new GlScenePlatform.Texture[lights.length];
    /**
     * Texture IDs resolved while collecting the masks.
     */
    private final int[] maskTextures = new int[lights.length];
    /**
     * Persistent directional and spotlight depth targets.
     */
    private final GlSceneTarget[] maps = new GlSceneTarget[1 + UiMesh3DShaders.LOCAL_SHADOW_NAMES.length];
    /**
     * Previous texture binding by unit, saved on first use each frame.
     */
    private final int[] previousTextures = new int[SPOT_UNIT + maps.length - 1];
    /**
     * Light projection staging.
     */
    private final SceneShadowProjection projection = new SceneShadowProjection();
    /**
     * Directional world-to-clip matrix.
     */
    private final MutableMat4 directionalClip = new MutableMat4();
    /**
     * Native-order staging for the local-light UBO.
     */
    private final ByteBuffer bytes = ByteBuffer.allocateDirect(BYTES).order(ByteOrder.nativeOrder());
    /**
     * Reusable scene traversal callback.
     */
    private final Consumer<SceneNode> collector = this::collect;
    /**
     * Directional settings, present after a successful depth pass.
     */
    private DirectionalShadows directional;
    /**
     * Index-zero ID denotes no allocated UBO.
     */
    private int buffer;
    /**
     * Color attachment excluded from mask sampling.
     */
    private int outputTexture;
    /**
     * Active light, projected mask and spotlight depth counts.
     */
    private int count, maskCount, spotCount;
    /**
     * Sampler restore stack depth recorded before binding any scene light textures.
     */
    private int samplerDepth;

    /**
     * @param platform resolves projected textures and tracks OpenGL state
     */
    GlSceneLighting(GlScenePlatform platform) {
        this.platform = platform;
        this.gl = platform.state();
        Arrays.setAll(lights, i -> new SceneLightData());
        Arrays.fill(previousTextures, -1);
    }

    /**
     * Resolves lights in scene order and fills each enabled shadow map.
     *
     * @param scene  scene containing visible lights and casters
     * @param output destination used to reject render target feedback
     * @param pass   shadow geometry renderer
     */
    void prepare(Scene scene, GlScenePlatform.Target output, ShadowPass pass) {
        samplerDepth = gl.samplerDepth();
        outputTexture = output.color();
        scene.visitVisible(null, collector);
        var light = scene.directionalLight();
        var settings = scene.directionalShadows();
        if (light != null && light.intensity() > 0 && settings != null && settings.strength() > 0) {
            projection.update(light, settings, directionalClip);
            if (pass.draw(target(0, settings.resolution()), directionalClip)) directional = settings;
        }
        for (int i = 0; i < count && spotCount < maps.length - 1; i++) {
            var entry = lights[i];
            SpotShadows shadow = entry.light instanceof SpotLight spot ? spot.shadows() : null;
            if (shadow != null && shadow.strength() > 0
                    && pass.draw(target(spotCount + 1, shadow.resolution()), entry.transform.clip())) {
                entry.shadowIndex = ++spotCount;
            }
        }
        if (count == 0) return;
        bytes.clear().putInt(count).putInt(0).putLong(0);
        for (int i = 0; i < count; i++) lights[i].write(bytes);
        if (buffer == 0) {
            buffer = GL15.glGenBuffers();
            GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, buffer);
            GL15.glBufferData(GL31.GL_UNIFORM_BUFFER, BYTES, GL15.GL_STREAM_DRAW);
        }
        GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, buffer);
        GL15.glBufferSubData(GL31.GL_UNIFORM_BUFFER, 0, bytes.flip());
    }

    /**
     * @param node visible candidate, including nodes outside the viewport
     */
    private void collect(SceneNode node) {
        if (count == lights.length || !(node instanceof LightNode source)) return;
        var light = source.light();
        var entry = lights[count];
        if (light.intensity() == 0 || light.colorRgb() == 0 || !entry.transform.update(source)) return;
        if (light instanceof SpotLight spot && spot.mask() != null) {
            var mask = platform.texture(spot.mask().base());
            if (mask == null) return;
            int id = mask.id();
            if (id == outputTexture || id == 0) return;
            masks[maskCount] = mask;
            maskTextures[maskCount] = id;
            entry.maskIndex = ++maskCount;
            entry.premultiplied = mask.premultipliedAlpha();
            if (!spot.mask().resolveUv(entry.uv)) entry.uv.set(0, 0, 1, 1);
            if (mask.flipY()) entry.uv.set(entry.uv.x(), 1 - entry.uv.y(), entry.uv.width(), -entry.uv.height());
        }
        entry.light = light;
        count++;
    }

    /**
     * Resolves persistent depth storage by light slot.
     *
     * @param index zero for directional, one-based index for a spotlight
     * @param size  square texture size in pixels
     * @return depth-bearing framebuffer
     */
    private GlSceneTarget target(int index, int size) {
        var target = maps[index];
        if (target == null || target.width() != size) {
            maps[index] = new GlSceneTarget(size, size, true);
            if (target != null) target.close();
            target = maps[index];
        }
        return target;
    }

    /**
     * @return true when a point or spot light illuminates the frame
     */
    boolean hasLights() {
        return count > 0;
    }

    /**
     * @return active mask count for shader specialization
     */
    int masks() {
        return maskCount;
    }

    /**
     * @return active spot shadow count for shader specialization
     */
    int spots() {
        return spotCount;
    }

    /**
     * @return light settings, or null when there is no directional shadow map
     */
    DirectionalShadows directional() {
        return directional;
    }

    /**
     * @return borrowed canonical directional world-to-clip matrix
     */
    MutableMat4 directionalClip() {
        return directionalClip;
    }

    /**
     * Binds light textures once before drawing the color pass.
     */
    void bind() {
        samplerDepth = gl.samplerDepth();
        if (directional != null) bindTexture(DIRECTIONAL_UNIT, maps[0].depth());
        if (count > 0) GL30.glBindBufferBase(GL31.GL_UNIFORM_BUFFER, BINDING, buffer);
        for (int i = 0; i < maskCount; i++) {
            bindTexture(MASK_UNIT + i, maskTextures[i]);
            gl.applySampler(MASK_UNIT + i, masks[i].options());
        }
        for (int i = 0; i < spotCount; i++) bindTexture(SPOT_UNIT + i, maps[i + 1].depth());
        gl.activeTexture(GL13.GL_TEXTURE0);
    }

    /**
     * @return index of the first contributing spotlight, or -1
     */
    int fogSpotlight() {
        for (int i = 0; i < count; i++) {
            if (lights[i].light instanceof SpotLight) return i;
        }
        return -1;
    }

    /**
     * @param index selected spotlight index or -1
     * @return shader flags for active spotlight resources
     */
    int fogVariant(int index) {
        if (index < 0) return 0;
        var spot = lights[index];
        return SceneFogShaders.SPOT | (spot.maskIndex > 0 ? SceneFogShaders.MASK : 0)
                | (spot.shadowIndex > 0 ? SceneFogShaders.SHADOW : 0);
    }

    /**
     * Points the fog shader at the units already bound by the surface pass.
     *
     * @param program current fog integration program
     * @param index   selected spotlight index or -1
     */
    void linkFogSamplers(int program, int index) {
        if (index < 0) return;
        var spot = lights[index];
        if (spot.maskIndex > 0) GlSceneOverlay.sampler(program, "FogSpotMask", MASK_UNIT + spot.maskIndex - 1);
        if (spot.shadowIndex > 0) GlSceneOverlay.sampler(program, "FogSpotDepth", SPOT_UNIT + spot.shadowIndex - 1);
    }

    /**
     * Registers the sampler units declared by a linked surface program.
     *
     * @param program active OpenGL program
     * @param masks   number of projected masks the variant reads
     * @param spots   number of spotlight maps the variant reads
     */
    static void linkSamplers(int program, int masks, int spots) {
        GL20.glUniform1i(GL20.glGetUniformLocation(program, "SceneShadowMap"), DIRECTIONAL_UNIT);
        for (int i = 0; i < masks; i++)
            GL20.glUniform1i(GL20.glGetUniformLocation(program, UiMesh3DShaders.LOCAL_MASK_NAMES[i]), MASK_UNIT + i);
        for (int i = 0; i < spots; i++)
            GL20.glUniform1i(GL20.glGetUniformLocation(program, UiMesh3DShaders.LOCAL_SHADOW_NAMES[i]), SPOT_UNIT + i);
    }

    /**
     * Saves the previous binding on first use of a unit.
     *
     * @param unit    texture unit index
     * @param texture OpenGL texture name
     */
    private void bindTexture(int unit, int texture) {
        int previous = GlSceneOverlay.exchangeTexture(unit, texture);
        if (previousTextures[unit] < 0) previousTextures[unit] = previous;
    }

    /**
     * Restores the game's texture units before running a native draw.
     */
    void unbind() {
        gl.restoreSamplers(samplerDepth);
        for (int i = 1; i < previousTextures.length; i++) {
            if (previousTextures[i] < 0) continue;
            GlSceneOverlay.exchangeTexture(i, previousTextures[i]);
            previousTextures[i] = -1;
        }
    }

    /**
     * Restores bindings and drops scene references after rendering.
     */
    void reset() {
        unbind();
        Arrays.fill(masks, 0, maskCount, null);
        for (int i = 0; i < count; i++) lights[i].reset();
        count = maskCount = spotCount = 0;
        directional = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        for (var map : maps) if (map != null) map.close();
        Arrays.fill(maps, null);
        if (buffer != 0) GL15.glDeleteBuffers(buffer);
        buffer = 0;
    }

    /**
     * Renders the opaque and cutout geometry for a light.
     */
    @FunctionalInterface
    interface ShadowPass {
        /**
         * @param target light depth framebuffer
         * @param clip   canonical world-to-light clip transform
         * @return whether a depth map was populated
         */
        boolean draw(GlSceneTarget target, MutableMat4 clip);
    }
}
