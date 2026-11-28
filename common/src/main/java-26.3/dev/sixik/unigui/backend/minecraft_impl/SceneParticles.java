package dev.sixik.unigui.backend.minecraft_impl;

import dev.sixik.unigui.api.math.MutableMat4;
import dev.sixik.unigui.api.math.MutableVec3;
import dev.sixik.unigui.api.math.Mat4View;
import dev.sixik.unigui.api.scene.Camera3D;
import dev.sixik.unigui.tests.mixin.SceneParticleEngineAccess;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SingleQuadParticle.Layer;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.AtlasIds;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.WeakHashMap;

/**
 * Samples local vanilla particles for either the native feature pass or the soft particle shader.
 */
final class SceneParticles implements AutoCloseable {
    /**
     * Number of vanilla simulation ticks per second.
     */
    static final int TICKS_PER_SECOND = 20;
    /**
     * Maximum stored ticks, including the longest vanilla signal smoke particle.
     */
    private static final int MAX_TICKS = 330;
    /**
     * Distinct native paths used in emission order.
     */
    private static final int VARIANTS = 8;

    /**
     * The particle atlas's missing sprite changes with each atlas reload.
     */
    private TextureAtlasSprite atlasGeneration;
    /**
     * Emitter paths can be collected after the emitter leaves its scene.
     */
    private final WeakHashMap<MinecraftParticleEmitter, Paths> cache = new WeakHashMap<>();
    /**
     * Vanilla camera at the origin and with identity rotation for sampling local quads.
     */
    private final Camera origin = new Camera();
    /**
     * Reused capture of the last quad extracted by vanilla.
     */
    private final Capture capture = new Capture();
    /**
     * Native sprite layers submitted at the current scene time.
     */
    private final QuadParticleRenderState batch = new QuadParticleRenderState();
    /**
     * Optional soft atlas sprites sampled for the current scene.
     */
    private GpuSceneSoftParticles soft;
    /**
     * Transforms local particle centers into view space.
     */
    private final MutableMat4 world = new MutableMat4();
    /**
     * View from particle-local coordinates, computed once per emitter per frame.
     */
    private final MutableMat4 modelView = new MutableMat4();
    /**
     * Reusable transformed center.
     */
    private final MutableVec3 center = new MutableVec3();
    /**
     * Reusable interpolated camera-space rotation.
     */
    private final Quaternionf rotation = new Quaternionf();
    /**
     * Reusable interpolation endpoint.
     */
    private final Quaternionf nextRotation = new Quaternionf();

    /**
     * Adds the emitter's currently alive sprites in camera space.
     *
     * @param emitter visible emitter
     * @param view    scene-world to camera-space transform
     * @return true if this emitter contributes to the native feature pass
     */
    boolean collect(MinecraftParticleEmitter emitter, MutableMat4 view) {
        int limit = emitter.emissionLimit();
        if (limit == 0 || emitter.firstEmission(limit, (double) MAX_TICKS / TICKS_PER_SECOND) == limit) return false;
        var settings = emitter.softParticles();
        if (settings != null && settings.opacity() == 0) return false;
        Minecraft client = Minecraft.getInstance();
        var atlas = client.getAtlasManager().getAtlasOrThrow(AtlasIds.PARTICLES);
        var generation = atlas.missingSprite();
        if (atlasGeneration != generation) {
            cache.clear();
            atlasGeneration = generation;
        }
        Paths paths = cache.get(emitter);
        if (paths == null || !paths.motion.equals(emitter.motion())) {
            paths = bake(client, emitter);
            cache.put(emitter, paths);
        }
        emitter.worldMatrix(world);
        modelView.setProduct(view, world);
        if (settings != null && !GpuSceneSoftParticles.available()) settings = null;
        boolean drawn = false;
        for (int index = emitter.firstEmission(limit, (double) paths.lifetime / TICKS_PER_SECOND);
             index < limit; index++) {
            Frame[] frames = paths.variants[index % VARIANTS];
            double age = emitter.age(index) * TICKS_PER_SECOND;
            int tick = (int) age;
            if (tick < frames.length - 1) {
                drawn |= append(frames[tick], frames[tick + 1], (float) (age - tick), settings);
            }
        }
        return drawn;
    }

    /**
     * Resolves the native provider with the loaded sprite bindings and samples a set of reusable paths.
     *
     * @param client  client with loaded resources
     * @param emitter source of particle options and motion
     * @return cached paths detached from the native particles and level
     */
    @SuppressWarnings("unchecked") // The map is keyed by the registry ID of the same options' type.
    private Paths bake(Minecraft client, MinecraftParticleEmitter emitter) {
        var resources = ((SceneParticleEngineAccess) client.particleEngine).unigui$particleResources();
        ParticleOptions options = emitter.particle();
        var provider = (ParticleProvider<ParticleOptions>) resources.getProviders()
                .get(BuiltInRegistries.PARTICLE_TYPE.getId(options.getType()));
        var motion = emitter.motion();
        Frame[][] variants = new Frame[VARIANTS][];
        RandomSource random = RandomSource.create(0);
        int longest = 0;
        for (int i = 0; i < variants.length; i++) {
            Frame[] frames = new Frame[0];
            if (provider != null && provider.getClass().getPackageName().equals("net.minecraft.client.particle")) {
                var particle = provider.createParticle(options, null,
                        offset(random, motion.radius()), offset(random, motion.radius()), offset(random, motion.radius()),
                        motion.x() + offset(random, motion.spread()), motion.y() + offset(random, motion.spread()),
                        motion.z() + offset(random, motion.spread()), random);
                if (particle instanceof SingleQuadParticle quad) {
                    quad.scale(motion.scale());
                    frames = sample(quad, origin, capture);
                }
            }
            variants[i] = frames;
            longest = Math.max(longest, frames.length - 1);
        }
        return new Paths(motion, variants, longest);
    }

    /**
     * Samples the pose at the start and end of each native simulation tick. The endpoint is stored after
     * advancing once and reading the native particle at partial tick one, where it uses its new position.
     *
     * @param particle isolated vanilla sprite
     * @param origin   identity camera in emitter coordinates
     * @param capture  receives the extracted quad
     * @return keyframes at integer ticks, including an endpoint for the last live interval
     */
    static Frame[] sample(SingleQuadParticle particle, Camera origin, Capture capture) {
        int duration = Math.min(MAX_TICKS, particle.getLifetime());
        var frames = new ArrayList<Frame>(Math.max(0, duration) + 1);
        capture.frame = null;
        particle.extract(capture, origin, 0);
        if (capture.frame == null || duration <= 0) return new Frame[0];
        frames.add(capture.frame);
        for (int tick = 0; tick < duration; tick++) {
            particle.tick();
            capture.frame = null;
            if (particle.isAlive()) particle.extract(capture, origin, 1);
            if (capture.frame == null) {
                frames.add(frames.getLast());
                break;
            }
            frames.add(capture.frame);
        }
        return frames.toArray(Frame[]::new);
    }

    /**
     * Samples a uniform signed offset in the interval [-amplitude, amplitude).
     *
     * @param random    source of a uniform scalar in [0, 1)
     * @param amplitude maximum absolute offset
     * @return initial position or velocity offset for one axis
     */
    private static double offset(RandomSource random, float amplitude) {
        return (2.0 * random.nextFloat() - 1) * amplitude;
    }

    /**
     * Interpolates center, native size and local billboard rotation. Sprite UVs and tint follow native ticks.
     *
     * @param current  frame at the beginning of the tick
     * @param next     frame at the end of the tick
     * @param partial  elapsed fraction of the tick
     * @param settings optional soft rendering configuration
     * @return whether the sprite was queued for the native feature pass
     */
    private boolean append(Frame current, Frame next, float partial, MinecraftParticleEmitter.SoftParticles settings) {
        float size = Mth.lerp(partial, current.size, next.size);
        if (!(size > 0 && Float.isFinite(size))) return false;
        center.set(Mth.lerp(partial, current.x, next.x), Mth.lerp(partial, current.y, next.y),
                Mth.lerp(partial, current.z, next.z));
        if (!modelView.transformPoint(center, center)) return false;
        rotation.set(current.qx, current.qy, current.qz, current.qw)
                .nlerp(nextRotation.set(next.qx, next.qy, next.qz, next.qw), partial);
        if (settings != null && TextureAtlas.LOCATION_PARTICLES.equals(current.layer.textureAtlasLocation())) {
            if (soft == null) soft = new GpuSceneSoftParticles();
            soft.add(center, rotation, size, current, settings);
            return false;
        }
        batch.add(current.layer, (float) center.x(), (float) center.y(), (float) center.z(),
                rotation.x, rotation.y, rotation.z, rotation.w,
                size, current.u0, current.u1, current.v0, current.v1,
                current.color, LightCoordsUtil.FULL_BRIGHT);
        return true;
    }

    /**
     * Adds the collected quads to the offscreen native feature pass.
     *
     * @param storage commands for the offscreen scene only
     */
    void submit(SubmitNodeStorage storage) {
        if (!batch.isEmpty()) storage.submitQuadParticleGroup(batch);
    }

    /**
     * Composites the soft sprites over the scene using its completed opaque depth.
     *
     * @param target     offscreen scene color and depth
     * @param projection canonical camera projection
     * @param camera     camera supplying clip planes and projection mode
     * @param width      active viewport width in pixels
     * @param height     active viewport height in pixels
     */
    void renderSoft(UiGpu.Target target, Mat4View projection, Camera3D camera, float width, float height) {
        if (soft != null) soft.render(target, projection, camera, width, height);
    }

    /**
     * Drops quads after both passes consume them.
     */
    void reset() {
        batch.clear();
        if (soft != null) soft.reset();
    }

    /**
     * Releases soft particle rendering resources.
     */
    @Override
    public void close() {
        if (soft != null) soft.close();
    }

    /**
     * Baked path data, independent of the display camera and game world.
     *
     * @param motion   initial conditions used when baking
     * @param variants arrays of quad keyframes in order of reuse
     * @param lifetime longest path duration in ticks
     */
    private record Paths(MinecraftParticleEmitter.Motion motion, Frame[][] variants, int lifetime) {
    }

    /**
     * One vanilla billboard keyframe in emitter-local coordinates.
     *
     * @param layer native atlas and render pipeline
     * @param x     local X coordinate
     * @param y     local Y coordinate
     * @param z     local Z coordinate
     * @param qx    rotation quaternion X
     * @param qy    rotation quaternion Y
     * @param qz    rotation quaternion Z
     * @param qw    rotation quaternion W
     * @param size  native billboard half-size
     * @param u0    first horizontal atlas coordinate
     * @param u1    second horizontal atlas coordinate
     * @param v0    first vertical atlas coordinate
     * @param v1    second vertical atlas coordinate
     * @param color packed ARGB tint
     */
    record Frame(Layer layer, float x, float y, float z, float qx, float qy, float qz, float qw,
                 float size, float u0, float u1, float v0, float v1, int color) {
    }

    /**
     * Captures native billboard data without adding the original quad to a world or scene batch.
     */
    static final class Capture extends QuadParticleRenderState {
        /**
         * Last quad emitted by the sampled particle.
         */
        private Frame frame;

        /**
         * {@inheritDoc}
         */
        @Override
        public void add(Layer layer, float x, float y, float z, float qx, float qy, float qz, float qw,
                        float size, float u0, float u1, float v0, float v1, int color, int light) {
            frame = new Frame(layer, x, y, z, qx, qy, qz, qw, size, u0, u1, v0, v1, color);
        }
    }
}
