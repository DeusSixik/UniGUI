package dev.sixik.unigui.cleanroom.backend;

import dev.sixik.unigui.api.math.Mat4View;
import dev.sixik.unigui.api.render.SimpleTextureHandle;
import dev.sixik.unigui.api.scene.SceneNode;
import dev.sixik.unigui.backend.minecraft_impl.GlScenePlatform;
import dev.sixik.unigui.backend.minecraft_impl.GlSceneSprites;
import dev.sixik.unigui.cleanroom.mixin.SceneParticleAccess;
import dev.sixik.unigui.cleanroom.mixin.SceneParticleFactories;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.IParticleFactory;
import net.minecraft.client.particle.Particle;
import net.minecraft.util.EnumParticleTypes;

import java.util.LinkedHashMap;
import java.util.Random;

/**
 * Bakes isolated vanilla particles and submits their sprites to the shared depth-fade pass.
 */
final class CleanroomSceneSprites extends GlSceneSprites {
    /**
     * Several different vanilla paths are reused cyclically for different births.
     */
    private static final int VARIANTS = 8;
    /**
     * Maximum number of ticks sampled per path.
     */
    private static final int MAX_TICKS = 512;
    /**
     * Vanilla simulations advance at 20 ticks per second.
     */
    private static final double TICKS_PER_SECOND = 20;
    /**
     * Legacy 16-by-16 grid of particle sprites.
     */
    private static final SimpleTextureHandle ATLAS = new SimpleTextureHandle("minecraft:textures/particle/particles.png", 0, 0);
    /**
     * Recently used motion paths shared by emitters with identical parameters.
     */
    private final LinkedHashMap<PathKey, Paths> paths = new LinkedHashMap<>(16, 0.75f, true);

    /**
     * @param gl client GL state adapter
     */
    CleanroomSceneSprites(GlScenePlatform.State gl) {
        super(gl, true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected int atlas() {
        return CleanroomTextures.sceneTexture(ATLAS).id();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void collect(SceneNode node, Mat4View view) {
        if (node instanceof CleanroomCampfireSmoke smoke) {
            collectCampfireSmoke(smoke, view);
            return;
        }
        if (!(node instanceof CleanroomParticleEmitter emitter)) return;
        int limit = emitter.limit();
        if (limit == 0) return;
        var manager = Minecraft.getMinecraft().effectRenderer;
        if (manager == null) return;
        var factory = ((SceneParticleFactories) manager).unigui$factories().get(emitter.particle().getParticleID());
        if (factory == null || factory.getClass().getPackage() != Particle.class.getPackage()) return;
        var key = new PathKey(emitter.particle(), emitter.motion(), factory);
        var baked = paths.get(key);
        if (baked == null) {
            baked = bake(key);
            paths.put(key, baked);
            if (paths.size() > 32) paths.remove(paths.keySet().iterator().next());
        }
        if (baked.lifetime == 0) return;
        transform(emitter, view);
        var soft = emitter.softParticles();
        int tint = emitter.tintRgb();
        int start = emitter.first(limit, baked.lifetime / TICKS_PER_SECOND);
        for (int i = start; i < limit; i++) {
            var frames = baked.variants[i % VARIANTS];
            double ticks = emitter.age(i) * TICKS_PER_SECOND;
            int tick = (int) ticks;
            if (ticks >= 0 && tick < frames.length - 1) {
                append(frames[tick], frames[tick + 1], (float) (ticks - tick),
                        soft == null ? 0 : soft.fadeDistance(), soft == null ? 1 : soft.widthScale(),
                        soft == null ? 1 : soft.heightScale(), soft == null ? 1 : soft.opacity(), tint);
            }
        }
    }

    /**
     * Runs the native flame and smoke simulation in emitter-local space, with no world and with collision disabled.
     *
     * @param key factory and its initial conditions
     * @return bounded paths with a final sample for interpolation
     */
    private static Paths bake(PathKey key) {
        var random = new Random(0x5CE9E);
        var motion = key.motion;
        Frame[][] frames = new Frame[VARIANTS][];
        int lifetime = 0;
        for (int i = 0; i < frames.length; i++) {
            var particle = key.factory.createParticle(key.type.getParticleID(), null, offset(random, motion.radius()),
                    offset(random, motion.radius()), offset(random, motion.radius()),
                    motion.x() + offset(random, motion.spread()), motion.y() + offset(random, motion.spread()),
                    motion.z() + offset(random, motion.spread()));
            if (particle == null) {
                frames[i] = new Frame[0];
                continue;
            }
            var access = (SceneParticleAccess) particle;
            access.unigui$setCollision(false);
            int ticks = Math.min(MAX_TICKS, access.unigui$lifetime());
            if (!(ticks > 0)) {
                frames[i] = new Frame[0];
                continue;
            }
            frames[i] = new Frame[ticks + 1];
            lifetime = Math.max(lifetime, ticks);
            for (int tick = 0; tick <= ticks; tick++) {
                float u = access.unigui$column() / 16f, v = access.unigui$row() / 16f;
                float halfSize = size(key.type, access.unigui$scale(), tick / (float) access.unigui$lifetime()) * motion.scale();
                frames[i][tick] = new Frame((float) access.unigui$x(), (float) access.unigui$y(),
                        (float) access.unigui$z(), halfSize, access.unigui$angle(),
                        u, v, u + 0.0624375f, v + 0.0624375f, particle.getRedColorF(),
                        particle.getGreenColorF(), particle.getBlueColorF(), access.unigui$alpha());
                if (tick < ticks) particle.onUpdate();
            }
        }
        return new Paths(frames, lifetime);
    }

    /**
     * Flame shrinks quadratically as {@code 1 - age² / 2}; smoke ramps up to full size
     * at {@code 1 / 32} of its lifetime. Minecraft's base billboard half-size is a tenth of its scale.
     *
     * @param type  native flame or smoke type
     * @param scale unanimated particle scale
     * @param age   normalized age
     * @return half-size of the animated sprite in blocks
     */
    static float size(EnumParticleTypes type, float scale, float age) {
        float factor = type == EnumParticleTypes.FLAME ? 1 - age * age * 0.5f : Math.min(1, age * 32);
        return 0.1f * scale * factor;
    }

    /**
     * @param random deterministic emitter variation @param amount maximum offset @return signed local offset
     */
    private static double offset(Random random, float amount) {
        return (2 * random.nextDouble() - 1) * amount;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        super.close();
        paths.clear();
    }

    /**
     * Native particle factory and its local initial conditions.
     */
    private record PathKey(EnumParticleTypes type, CleanroomParticleEmitter.Motion motion, IParticleFactory factory) {
    }

    /**
     * Tick samples for each variant and the longest lifespan in ticks.
     */
    private record Paths(Frame[][] variants, int lifetime) {
    }
}
