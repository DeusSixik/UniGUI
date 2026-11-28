package dev.sixik.unigui.backend.minecraft_impl;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;
import dev.sixik.unigui.api.docs.WikiThrows;
import dev.sixik.unigui.api.scene.SceneNode;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.Objects;

/**
 * Emits native Minecraft sprites in the local coordinates of a scene node.
 */
@WikiPage(title = "MinecraftParticleEmitter", slug = "scene-particle-emitter", category = "<key:scene.category>",
        summary = "<key:minecraft.particle.emitter.summary>")
@WikiSection(title = "<key:minecraft.particle.emitter.section.playback.title>", markdown = "<key:minecraft.particle.emitter.section.playback.body>")
@WikiSection(title = "<key:minecraft.particle.emitter.section.cleanroom.title>", order = 20,
        markdown = "<key:minecraft.particle.emitter.section.cleanroom.body>")
@WikiExample(title = "<key:minecraft.particle.emitter.example.furnace.title>", code = """
        var smoke = new MinecraftParticleEmitter(ParticleTypes.SMOKE, 4, 6, 12);
        smoke.setMotion(new MinecraftParticleEmitter.Motion(0, 0.02f, 0.03f, 0.01f, 0.08f, 0.7f));
        smoke.setPosition(0.5f, 0.45f, 1.04f);
        furnace.addChild(smoke);
        timeline.onSample(smoke::setTime);
        """)
@WikiExample(title = "<key:minecraft.particle.emitter.example.floor.title>", code = """
        var smoke = new MinecraftParticleEmitter(ParticleTypes.CAMPFIRE_COSY_SMOKE, 4, 7, 9)
                .setMotion(new MinecraftParticleEmitter.Motion(0, 0.001f, 0.02f, 0.008f, 0.12f, 0.9f))
                .setSoftParticles(new MinecraftParticleEmitter.SoftParticles(0.34f, 2.15f, 0.62f, 0.42f));
        smoke.setPosition(0.5f, 0.15f, 1.1f);
        furnace.addChild(smoke);
        timeline.onSample(smoke::setTime);
        """)
@AiDoc(summary = "A SceneNode for seekable vanilla sprite particles on Minecraft 1.20.1, 1.21.1 and 26.3.",
        criticalRules = {
                "Drive setTime with absolute seconds. Use SceneTimeline.onSample(emitter::setTime) for replay and seeking.",
                "The emission interval includes its start and excludes its end. Infinity keeps emitting.",
                "Use supports(ParticleOptions) to check the vanilla sprite types accepted by the emitter.",
                "Particle positions are local to the emitter. Motion controls provider velocity, spread and sprite size.",
                "Set SoftParticles for depth fading and a custom billboard aspect ratio and opacity."
        }, related = {"Scene3D", "SceneNode", "SceneTimeline", "CleanroomParticleEmitter"})
public final class MinecraftParticleEmitter extends SceneNode {
    /**
     * Maximum number of recent births read for one emitter in a frame.
     */
    static final int MAX_PARTICLES = SceneParticleSchedule.MAX_PARTICLES;

    /**
     * Vanilla options passed to the particle provider.
     */
    private final ParticleOptions particle;
    /**
     * Uniform birth schedule in absolute scene time.
     */
    private final SceneParticleSchedule schedule;
    /**
     * Initial conditions of the native particle paths.
     */
    private Motion motion = new Motion(0, 0, 0, 0, 0, 1);
    /**
     * Optional depth fade and billboard shape for the soft particle pass.
     */
    private SoftParticles softParticles;
    /**
     * Current time from the scene's caller, in seconds.
     */
    private double seconds;

    /**
     * Creates an emitter with a fixed emission interval and a uniform birth rate.
     *
     * @param particle           supported vanilla particle options
     * @param startSeconds       inclusive start time, finite and nonnegative
     * @param endSeconds         exclusive end time, or positive infinity
     * @param particlesPerSecond finite rate in (0, 128]
     * @throws NullPointerException     if particle is null
     * @throws IllegalArgumentException if the particle, interval or rate is unsupported
     */
    @WikiMethod(summary = "<key:minecraft.particle.emitter.api.constructor.summary>")
    @WikiThrows(type = NullPointerException.class, description = "<key:minecraft.particle.emitter.api.constructor.throws.NullPointerException>")
    @WikiThrows(type = IllegalArgumentException.class, description = "<key:minecraft.particle.emitter.api.constructor.throws.IllegalArgumentException>")
    @AiDoc(summary = "Creates an emitter. An infinite end time enables continuous emission, with a rate in (0, 128].")
    public MinecraftParticleEmitter(
            @WikiParameter(description = "<key:minecraft.particle.emitter.api.constructor.param.particle>") ParticleOptions particle,
            @WikiParameter(description = "<key:minecraft.particle.emitter.api.constructor.param.start.seconds>") double startSeconds,
            @WikiParameter(description = "<key:minecraft.particle.emitter.api.constructor.param.end.seconds>") double endSeconds,
            @WikiParameter(description = "<key:minecraft.particle.emitter.api.constructor.param.rate>") double particlesPerSecond) {
        this.particle = Objects.requireNonNull(particle, "particle");
        if (!supports(particle))
            throw new IllegalArgumentException("Unsupported scene particle: " + particle.getType());
        schedule = new SceneParticleSchedule(startSeconds, endSeconds, particlesPerSecond);
        setCastShadow(false).setReceiveShadow(false);
    }

    /**
     * Checks whether the vanilla provider's sprite animation can be simulated without a level.
     *
     * @param particle native particle options
     * @return true for one of the supported vanilla types
     */
    @WikiMethod(summary = "<key:minecraft.particle.emitter.api.supports.summary>")
    @WikiReturn("<key:scene.api.return.supported>")
    @AiDoc(summary = "Checks the particle type. Rendering with this emitter is available in Minecraft 1.20.1, 1.21.1 and 26.3.")
    public static boolean supports(
            @WikiParameter(description = "<key:minecraft.particle.emitter.api.supports.param.particle>") ParticleOptions particle) {
        if (particle == null) return false;
        var id = BuiltInRegistries.PARTICLE_TYPE.getKey(particle.getType());
        return id != null && id.getNamespace().equals("minecraft") && switch (id.getPath()) {
            case "flame", "small_flame", "soul_fire_flame", "smoke", "large_smoke", "white_smoke",
                 "campfire_cosy_smoke", "campfire_signal_smoke", "ash", "white_ash", "dust",
                 "dust_color_transition" -> true;
            default -> false;
        };
    }

    /**
     * @return the options supplied to the Minecraft particle provider
     */
    public ParticleOptions particle() {
        return particle;
    }

    /**
     * @return the current sample time in seconds
     */
    public double time() {
        return seconds;
    }

    /**
     * Advances or seeks the emitter and invalidates on-change viewports.
     *
     * @param seconds finite absolute time; negative values are before emission
     * @return this emitter
     * @throws IllegalArgumentException if seconds is nonfinite
     */
    @WikiMethod(summary = "<key:minecraft.particle.emitter.api.set.time.summary>")
    @WikiReturn("<key:scene.api.return.emitter>")
    @WikiThrows(type = IllegalArgumentException.class, description = "<key:scene.api.throws.nonfinite.time>")
    @AiDoc(summary = "Advances or seeks the emitter in seconds. Negative values precede emission.")
    public MinecraftParticleEmitter setTime(
            @WikiParameter(description = "<key:minecraft.particle.emitter.api.set.time.param.seconds>") double seconds) {
        if (!Double.isFinite(seconds)) throw new IllegalArgumentException("Particle time must be finite");
        if (this.seconds != seconds) {
            this.seconds = seconds;
            invalidateContent();
        }
        return this;
    }

    /**
     * @return the initial position, velocity variation and size settings
     */
    public Motion motion() {
        return motion;
    }

    /**
     * Updates the particle paths and sprite size.
     *
     * @param motion new settings
     * @return this emitter
     * @throws NullPointerException if motion is null
     */
    @WikiMethod(summary = "<key:minecraft.particle.emitter.api.set.motion.summary>")
    @WikiReturn("<key:scene.api.return.emitter>")
    @WikiThrows(type = NullPointerException.class, description = "<key:minecraft.particle.emitter.api.set.motion.throws.NullPointerException>")
    @AiDoc(summary = "Sets initial motion and size. Velocities are in blocks per tick and radius is in blocks.")
    public MinecraftParticleEmitter setMotion(
            @WikiParameter(description = "<key:minecraft.particle.emitter.api.set.motion.param.motion>") Motion motion) {
        if (!this.motion.equals(Objects.requireNonNull(motion, "motion"))) {
            this.motion = motion;
            invalidateContent();
        }
        return this;
    }

    /**
     * @return soft particle settings or null for vanilla rendering
     */
    public SoftParticles softParticles() {
        return softParticles;
    }

    /**
     * Updates how the particles blend against scene geometry.
     *
     * @param settings depth fade, aspect and opacity, or null for vanilla rendering
     * @return this emitter
     */
    @WikiMethod(summary = "<key:minecraft.particle.emitter.api.set.soft.particles.summary>")
    @WikiReturn("<key:scene.api.return.emitter>")
    @AiDoc(summary = "Enables soft particles with a configurable distance in blocks. Pass null for vanilla rendering.")
    public MinecraftParticleEmitter setSoftParticles(
            @WikiParameter(description = "<key:minecraft.particle.emitter.api.set.soft.particles.param.settings>") SoftParticles settings) {
        if (!Objects.equals(softParticles, settings)) {
            softParticles = settings;
            invalidateContent();
        }
        return this;
    }

    /**
     * Configures soft, camera-facing particle billboards. If the opaque surface is at view distance
     * {@code s} and the sprite is at {@code p}, alpha is multiplied by
     * {@code smoothstep(0, fadeDistance, s - p) * opacity}. Clear depth leaves alpha at its configured opacity.
     *
     * @param fadeDistance positive separation in scene blocks along the view axis
     * @param widthScale   positive multiplier for the sprite's native width
     * @param heightScale  positive multiplier for the sprite's native height
     * @param opacity      alpha multiplier, between zero and one inclusive
     */
    public record SoftParticles(float fadeDistance, float widthScale, float heightScale, float opacity) {
        /**
         * Validates the depth transition and billboard settings.
         *
         * @throws IllegalArgumentException if an argument is nonfinite or outside its range
         */
        public SoftParticles {
            if (!Float.isFinite(fadeDistance) || fadeDistance <= 0 || !Float.isFinite(widthScale) || widthScale <= 0
                    || !Float.isFinite(heightScale) || heightScale <= 0 || !(opacity >= 0 && opacity <= 1)) {
                throw new IllegalArgumentException("Invalid soft particle settings");
            }
        }
    }

    /**
     * @return exclusive upper index of births at the current sample
     */
    int emissionLimit() {
        return schedule.limit(seconds);
    }

    /**
     * Finds the first of the recent births that can still be alive.
     *
     * @param limit    exclusive upper index at the current sample
     * @param lifetime maximum particle lifetime in seconds
     * @return inclusive lower index of candidates
     */
    int firstEmission(int limit, double lifetime) {
        return schedule.first(seconds, limit, lifetime);
    }

    /**
     * Computes age as {@code seconds - birth(index)}, where seconds is the emitter's absolute sample time.
     *
     * @param index zero-based birth index
     * @return age in seconds
     */
    double age(int index) {
        return seconds - schedule.birth(index);
    }

    /**
     * Initial conditions passed to the vanilla provider. Providers may apply their own variation.
     *
     * @param x      local X velocity in blocks per tick
     * @param y      local Y velocity in blocks per tick
     * @param z      local Z velocity in blocks per tick
     * @param spread maximum additive velocity variation per axis in blocks per tick
     * @param radius half-size of the spawn box along each local axis, in blocks
     * @param scale  positive multiplier for the native sprite size
     */
    public record Motion(float x, float y, float z, float spread, float radius, float scale) {
        /**
         * Validates the initial conditions.
         *
         * @throws IllegalArgumentException if a component is nonfinite, spread or radius is negative, or scale is nonpositive
         */
        public Motion {
            if (!Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(z)
                    || !(spread >= 0 && Float.isFinite(spread)) || !(radius >= 0 && Float.isFinite(radius))
                    || !(scale > 0 && Float.isFinite(scale))) {
                throw new IllegalArgumentException("Invalid scene particle motion");
            }
        }
    }
}
