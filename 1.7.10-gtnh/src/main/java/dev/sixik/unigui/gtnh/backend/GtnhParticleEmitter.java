package dev.sixik.unigui.gtnh.backend;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiSection;
import dev.sixik.unigui.api.scene.SceneNode;
import dev.sixik.unigui.backend.minecraft_impl.SceneParticleSchedule;

import java.util.Objects;

/**
 * Emits vanilla flame or smoke sprites in the scene on GTNH 1.7.10.
 */
@WikiPage(title = "GtnhParticleEmitter", slug = "gtnh-particle-emitter", category = "<key:scene.category>",
    summary = "Seekable vanilla flame and smoke in GTNH 1.7.10 scenes.")
@WikiSection(title = "Playback", markdown = """
    Attach the emitter to a scene node and register `timeline.onSample(emitter::setTime)`.
    Particle births and motion use the emitter's local coordinates. Supported types are
    `FLAME`, `SMOKE_NORMAL` and `SMOKE_LARGE`. Set `SoftParticles` to fade sprite intersections against scene depth.
    Use `setTintRgb` to override the vanilla particle color while retaining the atlas and opacity.
    """)
@AiDoc(summary = "Seekable 1.7.10 vanilla flame and smoke using the Gtnh scene renderer.",
    criticalRules = {
        "Sample absolute seconds through SceneTimeline.onSample(emitter::setTime).",
        "Use GtnhParticleEmitter.Type.FLAME, SMOKE_NORMAL or SMOKE_LARGE.",
        "Motion velocity and spread are in blocks per tick. Spawn radius and depth fade are in blocks."
    }, related = {"SceneNode", "SceneTimeline"})
public final class GtnhParticleEmitter extends SceneNode {
    /**
     * Vanilla particle variants available for isolated scene playback.
     */
    public enum Type {
        /**
         * Flame sprite used by torches and furnaces.
         */
        FLAME,
        /**
         * Standard smoke sprite.
         */
        SMOKE_NORMAL,
        /**
         * Smoke with vanilla's large-particle scale.
         */
        SMOKE_LARGE
    }

    /**
     * Vanilla particle type passed to the factory.
     */
    private final Type type;
    /**
     * Uniform particle births in absolute seconds.
     */
    private final SceneParticleSchedule schedule;
    /**
     * Initial position and velocity offsets and size multiplier.
     */
    private Motion motion = new Motion(0, 0, 0, 0, 0, 1);
    /**
     * Optional settings for soft intersections.
     */
    private SoftParticles softParticles;
    /**
     * Packed RGB override in [0, 0xFFFFFF], or -1 for the vanilla particle color.
     */
    private int tintRgb = -1;
    /**
     * Current time in seconds.
     */
    private double seconds;

    /**
     * @param type  supported vanilla particle type
     * @param start inclusive start in seconds
     * @param end   exclusive end in seconds, or positive infinity
     * @param rate  births per second in (0, 128]
     * @throws IllegalArgumentException if the type, interval or rate is unsupported
     */
    public GtnhParticleEmitter(Type type, double start, double end, double rate) {
        if (!supports(type)) throw new IllegalArgumentException("Unsupported GTNH scene particle: " + type);
        this.type = type;
        schedule = new SceneParticleSchedule(start, end, rate);
        setCastShadow(false).setReceiveShadow(false);
    }

    /**
     * @param type native particle constant @return whether it can be simulated without a world
     */
    public static boolean supports(Type type) {
        return type == Type.FLAME || type == Type.SMOKE_NORMAL || type == Type.SMOKE_LARGE;
    }

    /**
     * @param time absolute sample in seconds, negative before emission
     * @return this emitter
     * @throws IllegalArgumentException if the time is nonfinite
     */
    public GtnhParticleEmitter setTime(double time) {
        if (!Double.isFinite(time)) throw new IllegalArgumentException("Particle time must be finite");
        if (seconds != time) {
            seconds = time;
            invalidateContent();
        }
        return this;
    }

    /**
     * @param motion new initial conditions @return this emitter
     */
    public GtnhParticleEmitter setMotion(Motion motion) {
        if (!this.motion.equals(Objects.requireNonNull(motion, "motion"))) {
            this.motion = motion;
            invalidateContent();
        }
        return this;
    }

    /**
     * @param settings depth fade and billboard shape, or null for a hard depth test @return this emitter
     */
    public GtnhParticleEmitter setSoftParticles(SoftParticles settings) {
        if (!Objects.equals(softParticles, settings)) {
            softParticles = settings;
            invalidateContent();
        }
        return this;
    }

    /**
     * Overrides the tint supplied by the particle factory. Sprite alpha and texture still come from vanilla.
     *
     * @param rgb packed 24-bit RGB, or -1 to use the vanilla tint
     * @return this emitter
     * @throws IllegalArgumentException if the value is outside the RGB range and is not -1
     */
    public GtnhParticleEmitter setTintRgb(int rgb) {
        if (rgb < -1 || rgb > 0xFFFFFF) throw new IllegalArgumentException("Expected 24-bit particle RGB or -1");
        if (tintRgb != rgb) {
            tintRgb = rgb;
            invalidateContent();
        }
        return this;
    }

    /**
     * @return the vanilla particle type
     */
    public Type particle() {
        return type;
    }

    /**
     * @return the current sample time in seconds
     */
    public double time() {
        return seconds;
    }

    /**
     * @return initial conditions used to bake the native motion
     */
    public Motion motion() {
        return motion;
    }

    /**
     * @return soft particle options, or null for a hard intersection
     */
    public SoftParticles softParticles() {
        return softParticles;
    }

    /**
     * @return the 24-bit RGB override, or -1 for the vanilla particle color
     */
    public int tintRgb() {
        return tintRgb;
    }

    /**
     * @return exclusive birth index at the latest sample
     */
    int limit() {
        return schedule.limit(seconds);
    }

    /**
     * @param limit exclusive upper birth index @param lifetime lifespan in seconds @return first eligible birth
     */
    int first(int limit, double lifetime) {
        return schedule.first(seconds, limit, lifetime);
    }

    /**
     * @param index zero-based birth index @return sample time minus birth time, in seconds
     */
    double age(int index) {
        return seconds - schedule.birth(index);
    }

    /**
     * @param x      local X velocity in blocks per tick
     * @param y      local Y velocity in blocks per tick
     * @param z      local Z velocity in blocks per tick
     * @param spread maximum additive velocity variation per axis in blocks per tick
     * @param radius half-size of the spawn box in blocks
     * @param scale  positive multiplier on vanilla sprite size
     */
    public record Motion(float x, float y, float z, float spread, float radius, float scale) {
        /**
         * @throws IllegalArgumentException if any component is nonfinite or out of range
         */
        public Motion {
            if (!Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(z)
                || !Float.isFinite(spread) || spread < 0 || !Float.isFinite(radius) || radius < 0
                || !Float.isFinite(scale) || scale <= 0) {
                throw new IllegalArgumentException("Invalid GTNH scene particle motion");
            }
        }
    }

    /**
     * @param fadeDistance positive scene distance over which depth intersections become fully opaque
     * @param widthScale   multiplier on vanilla width
     * @param heightScale  multiplier on vanilla height
     * @param opacity      alpha multiplier in [0,1]
     */
    public record SoftParticles(float fadeDistance, float widthScale, float heightScale, float opacity) {
        /**
         * @throws IllegalArgumentException if any component is nonfinite or out of range
         */
        public SoftParticles {
            if (!Float.isFinite(fadeDistance) || fadeDistance <= 0 || !Float.isFinite(widthScale) || widthScale <= 0
                || !Float.isFinite(heightScale) || heightScale <= 0 || !(opacity >= 0 && opacity <= 1)) {
                throw new IllegalArgumentException("Invalid Gtnh soft particle settings");
            }
        }
    }
}
