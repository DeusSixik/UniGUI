package dev.sixik.unigui.backend.minecraft_impl;

import dev.sixik.unigui.api.math.Mat4View;
import dev.sixik.unigui.api.scene.SceneNode;
import dev.sixik.unigui.tests.mixin.LegacySceneParticleAccess;
import dev.sixik.unigui.tests.mixin.LegacySceneParticleEngineAccess;
import dev.sixik.unigui.tests.mixin.LegacySceneSpriteAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.util.RandomSource;

import java.util.Arrays;
import java.util.LinkedHashMap;

 /** Supplies native paths and atlas sprites on Minecraft 1.20.1 and 1.21.1. */
final class LegacySceneParticles extends GlSceneSprites {
    /** Number of native path variants per emitter configuration. */
    private static final int VARIANTS = 8;
    /** Maximum sampled lifetime in vanilla ticks. */
    private static final int MAX_TICKS = 512;
    /** Native updates per second used when baking and interpolating paths. */
    private static final int TICKS_PER_SECOND = 20;
    /** Recently used native paths, shared by emitters with the same options and motion. */
    private final LinkedHashMap<PathKey, Paths> paths = new LinkedHashMap<>(16, 0.75f, true);
    /** Particle atlas associated with the latest model resource generation. */
    private Object generation;

    /** @param gl client GL state adapter */
    LegacySceneParticles(GlScenePlatform.State gl) {
        super(gl);
    }

    /** {@inheritDoc} */
    @Override
    protected int atlas() {
        return Minecraft.getInstance().getTextureManager().getTexture(TextureAtlas.LOCATION_PARTICLES).getId();
    }

    /**
     * Queues all particles alive at the emitter's sampled time.
     *
     * @param node visible scene node
     * @param view canonical world-to-view camera matrix
     */
    @Override
    public void collect(SceneNode node, Mat4View view) {
        if (!(node instanceof MinecraftParticleEmitter emitter)) return;
        int limit = emitter.emissionLimit();
        if (limit == 0) return;
        Minecraft client = Minecraft.getInstance();
        Object current = SceneItemModel.generation(client);
        if (current != generation) {
            generation = current;
            paths.clear();
        }
        if (current == null) return;
        var key = new PathKey(emitter.particle(), emitter.motion());
        var baked = paths.get(key);
        if (baked == null) {
            baked = bake(client, key);
            paths.put(key, baked);
            if (paths.size() > 32) paths.remove(paths.keySet().iterator().next());
        }
        if (baked.lifetime == 0) return;
        transform(emitter, view);
        int start = emitter.firstEmission(limit, baked.lifetime / (double) TICKS_PER_SECOND);
        for (int i = start; i < limit; i++) {
            var frames = baked.variants[i % VARIANTS];
            double ticks = emitter.age(i) * TICKS_PER_SECOND;
            int tick = (int) ticks;
            if (ticks >= 0 && tick < frames.length - 1) {
                var soft = emitter.softParticles();
                append(frames[tick], frames[tick + 1], (float) (ticks - tick),
                        soft == null ? 0 : soft.fadeDistance(), soft == null ? 1 : soft.widthScale(),
                        soft == null ? 1 : soft.heightScale(), soft == null ? 1 : soft.opacity());
            }
        }
    }

    /**
     * Bakes a bounded set of real vanilla paths with collisions disabled and native sprite selection.
     *
     * @param client client with the current resource pack's providers
     * @param key vanilla particle options and initial motion
     * @return cached paths, with a zero lifetime if the provider is unavailable
     */
    private static Paths bake(Minecraft client, PathKey key) {
        var access = (LegacySceneParticleEngineAccess) client.particleEngine;
        Frame[][] variants = new Frame[VARIANTS][];
        var rng = RandomSource.create(0x5CE9E);
        var motion = key.motion;
        int lifetime = 0;
        for (int i = 0; i < VARIANTS; i++) {
            var particle = access.unigui$makeParticle(key.options,
                    offset(rng, motion.radius()), offset(rng, motion.radius()), offset(rng, motion.radius()),
                    motion.x() + offset(rng, motion.spread()), motion.y() + offset(rng, motion.spread()),
                    motion.z() + offset(rng, motion.spread()));
            if (particle instanceof SingleQuadParticle nativeQuad
                    && nativeQuad.getClass().getPackage() == SingleQuadParticle.class.getPackage()) {
                nativeQuad.scale(motion.scale());
                variants[i] = sample(nativeQuad);
            } else {
                variants[i] = new Frame[0];
            }
            lifetime = Math.max(lifetime, variants[i].length - 1);
        }
        return new Paths(variants, lifetime);
    }

    /**
     * Samples the native sprite before every tick, including a closing frame for interpolation.
     *
     * @param sprite isolated native vanilla particle
     * @return frames in ascending tick order
     */
    private static Frame[] sample(SingleQuadParticle sprite) {
        var access = (LegacySceneParticleAccess) sprite;
        var uv = (LegacySceneSpriteAccess) sprite;
        access.unigui$setPhysics(false);
        int duration = Math.min(MAX_TICKS, sprite.getLifetime());
        if (duration <= 0 || !sprite.isAlive()) return new Frame[0];
        Frame[] frames = new Frame[duration + 1];
        int tick = 0;
        do {
            frames[tick] = new Frame((float) access.unigui$getX(), (float) access.unigui$getY(), (float) access.unigui$getZ(),
                    sprite.getQuadSize(0), access.unigui$getRoll(), uv.unigui$getU0(), uv.unigui$getV0(),
                    uv.unigui$getU1(), uv.unigui$getV1(), access.unigui$getRed(), access.unigui$getGreen(),
                    access.unigui$getBlue(), access.unigui$getAlpha());
            if (tick == duration) return frames;
            tick++;
            sprite.tick();
        } while (sprite.isAlive());
        frames[tick] = frames[tick - 1];
        return tick == duration ? frames : Arrays.copyOf(frames, tick + 1);
    }

    /**
     * Maps a random value in [0, 1) to a signed offset in [-amplitude, amplitude).
     *
     * @param random sample source
     * @param amplitude maximum absolute offset in blocks or blocks per tick
     * @return signed offset
     */
    private static double offset(RandomSource random, float amplitude) {
        return (2.0 * random.nextFloat() - 1) * amplitude;
    }


    /** {@inheritDoc} */
    @Override
    public void close() {
        super.close();
        paths.clear();
        generation = null;
    }

    /**
     * @param options options passed to the vanilla particle provider
     * @param motion initial position, velocity and size settings
     */
    private record PathKey(ParticleOptions options, MinecraftParticleEmitter.Motion motion) {
    }

    /**
     * @param variants native sprite samples at whole ticks
     * @param lifetime longest path duration in ticks
     */
    private record Paths(Frame[][] variants, int lifetime) {
    }

}
