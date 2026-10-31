package dev.sixik.unigui.gtnh.backend;

import dev.sixik.unigui.api.render.TextureFilter;
import dev.sixik.unigui.api.render.TextureOptions;
import dev.sixik.unigui.api.render.TextureWrap;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL33;

/**
 * Applies {@link TextureOptions} (filters, wrap) to textures for the duration of a draw and takes them back.
 *
 * <p>With OpenGL 3.3 the options live in cached sampler objects, one per distinct option set: binding a sampler
 * overrides the texture's own parameters without touching them, and unbinding gives the texture back to vanilla.
 * Without sampler objects the parameters are written to the texture bound on the unit and the previous values
 * restored afterwards. Default options ({@link TextureOptions#isDefault()}) change nothing, as in the 1.20 backend,
 * so vanilla textures draw exactly as vanilla draws them.</p>
 *
 * <p>Applications nest: every {@link #apply} that returns {@code true} is undone by one {@link #restore()}, last
 * first, or all at once with {@link #restoreTo(int)}. Render thread only.</p>
 */
final class GtnhSamplerState {
    /**
     * {@link TextureOptions#packed()} uses ten bits.
     */
    private static final int KEY_COUNT = 1 << 10;

    private final int[] samplers = new int[KEY_COUNT];
    private boolean samplerObjectsChecked;
    private boolean samplerObjects;
    private int depth;
    private int[] units = new int[4];
    private boolean[] asSampler = new boolean[4];
    private int[] saved = new int[16];

    /**
     * Applies the options to the texture bound on unit 0.
     *
     * @param options sampling options; {@code null} or default does nothing
     * @return {@code true} if something was changed and {@link #restore()} must be called
     */
    boolean apply(TextureOptions options) {
        return apply(0, options);
    }

    /**
     * Applies the options to the texture bound on a unit. Without sampler objects that unit must be the active one.
     *
     * @param unit    texture unit index, 0 for {@code GL_TEXTURE0}
     * @param options sampling options; {@code null} or default does nothing
     * @return {@code true} if something was changed and {@link #restore()} must be called
     */
    boolean apply(int unit, TextureOptions options) {
        if (options == null || options.isDefault()) return false;
        int entry = depth;
        if (entry == units.length) grow();
        units[entry] = unit;
        if (supportsSamplerObjects()) {
            GL33.glBindSampler(unit, sampler(options));
            asSampler[entry] = true;
        } else {
            int base = entry * 4;
            saved[base] = GL11.glGetTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER);
            saved[base + 1] = GL11.glGetTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER);
            saved[base + 2] = GL11.glGetTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S);
            saved[base + 3] = GL11.glGetTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T);
            applyToBoundTexture(options);
            asSampler[entry] = false;
        }
        depth++;
        return true;
    }

    /**
     * @return the number of applications waiting for {@link #restore()}; pass it to {@link #restoreTo(int)}
     */
    int depth() {
        return depth;
    }

    /**
     * Undoes the last {@link #apply} that changed something; does nothing if none is pending.
     */
    void restore() {
        if (depth == 0) return;
        int entry = --depth;
        if (asSampler[entry]) {
            GL33.glBindSampler(units[entry], 0);
        } else {
            int base = entry * 4;
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, saved[base]);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, saved[base + 1]);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, saved[base + 2]);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, saved[base + 3]);
        }
    }

    /**
     * Undoes every application made after {@code targetDepth} was read from {@link #depth()}.
     *
     * @param targetDepth depth to return to
     */
    void restoreTo(int targetDepth) {
        while (depth > Math.max(0, targetDepth)) restore();
    }

    private void grow() {
        units = java.util.Arrays.copyOf(units, units.length * 2);
        asSampler = java.util.Arrays.copyOf(asSampler, asSampler.length * 2);
        saved = java.util.Arrays.copyOf(saved, saved.length * 2);
    }

    /**
     * Writes the options into the parameters of the texture bound on unit 0. Used for textures UniGUI owns, whose
     * defaults (nearest, clamp to edge) must also hold when no sampler is bound.
     *
     * @param options sampling options, {@code null} means defaults
     */
    static void applyToBoundTexture(TextureOptions options) {
        TextureOptions normalized = options == null ? TextureOptions.defaults() : options;
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, minFilter(normalized));
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, magFilter(normalized.magFilter()));
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, wrap(normalized.wrapS()));
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, wrap(normalized.wrapT()));
    }

    /**
     * Generates the mipmap chain of the texture bound on unit 0.
     */
    static void generateMipmaps() {
        GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D);
    }

    private int sampler(TextureOptions options) {
        int key = options.packed();
        int existing = samplers[key];
        if (existing != 0) return existing;
        int sampler = GL33.glGenSamplers();
        GL33.glSamplerParameteri(sampler, GL11.GL_TEXTURE_MIN_FILTER, minFilter(options));
        GL33.glSamplerParameteri(sampler, GL11.GL_TEXTURE_MAG_FILTER, magFilter(options.magFilter()));
        GL33.glSamplerParameteri(sampler, GL11.GL_TEXTURE_WRAP_S, wrap(options.wrapS()));
        GL33.glSamplerParameteri(sampler, GL11.GL_TEXTURE_WRAP_T, wrap(options.wrapT()));
        samplers[key] = sampler;
        return sampler;
    }

    private boolean supportsSamplerObjects() {
        if (!samplerObjectsChecked) {
            try {
                samplerObjects = GL.getCapabilities().OpenGL33;
            } catch (Throwable ignored) {
                samplerObjects = false;
            }
            samplerObjectsChecked = true;
        }
        return samplerObjects;
    }

    /**
     * GL minification filter. A mipmap filter on a texture without mipmaps makes it incomplete (black), so it is
     * only used when the options ask for mipmaps; otherwise the base filter of the same kind is used.
     *
     * @param options sampling options
     * @return {@code GL_*} filter constant
     */
    static int minFilter(TextureOptions options) {
        TextureFilter filter = options.minFilter() == null ? TextureFilter.NEAREST : options.minFilter();
        if (!options.mipmaps()) {
            return switch (filter) {
                case NEAREST, NEAREST_MIPMAP_NEAREST -> GL11.GL_NEAREST;
                case LINEAR, LINEAR_MIPMAP_LINEAR -> GL11.GL_LINEAR;
            };
        }
        return switch (filter) {
            case NEAREST -> GL11.GL_NEAREST;
            case LINEAR -> GL11.GL_LINEAR;
            case NEAREST_MIPMAP_NEAREST -> GL11.GL_NEAREST_MIPMAP_NEAREST;
            case LINEAR_MIPMAP_LINEAR -> GL11.GL_LINEAR_MIPMAP_LINEAR;
        };
    }

    /**
     * @param filter magnification filter; mipmap variants are meaningless here and map to their base filter
     * @return {@code GL_*} filter constant
     */
    static int magFilter(TextureFilter filter) {
        return switch (filter == null ? TextureFilter.NEAREST : filter) {
            case NEAREST, NEAREST_MIPMAP_NEAREST -> GL11.GL_NEAREST;
            case LINEAR, LINEAR_MIPMAP_LINEAR -> GL11.GL_LINEAR;
        };
    }

    /**
     * @param wrap wrap mode, {@code null} means clamp to edge
     * @return {@code GL_*} wrap constant
     */
    static int wrap(TextureWrap wrap) {
        return switch (wrap == null ? TextureWrap.CLAMP_TO_EDGE : wrap) {
            case CLAMP_TO_EDGE -> GL12.GL_CLAMP_TO_EDGE;
            case REPEAT -> GL11.GL_REPEAT;
            case MIRRORED_REPEAT -> GL14.GL_MIRRORED_REPEAT;
        };
    }

}
