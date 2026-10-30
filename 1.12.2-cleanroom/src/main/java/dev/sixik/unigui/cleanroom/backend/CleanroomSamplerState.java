package dev.sixik.unigui.cleanroom.backend;

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
 * Applies {@link TextureOptions} (filters, wrap) to the texture bound on unit 0 for one draw and takes them back.
 *
 * <p>With OpenGL 3.3 the options live in cached sampler objects, one per distinct option set: binding a sampler
 * overrides the texture's own parameters without touching them, and unbinding gives the texture back to vanilla.
 * Without sampler objects the parameters are written to the texture and the previous values restored afterwards.
 * Default options ({@link TextureOptions#isDefault()}) change nothing, as in the 1.20 backend, so vanilla textures draw
 * exactly as vanilla draws them.</p>
 *
 * <p>Render thread only. One {@link #apply} must be followed by one {@link #restore}.</p>
 */
final class CleanroomSamplerState {
    /** {@link TextureOptions#packed()} uses ten bits. */
    private static final int KEY_COUNT = 1 << 10;

    private final int[] samplers = new int[KEY_COUNT];
    private boolean samplerObjectsChecked;
    private boolean samplerObjects;
    private boolean applied;
    private boolean appliedAsSampler;
    private int savedMin;
    private int savedMag;
    private int savedWrapS;
    private int savedWrapT;

    /**
     * Applies the options to the texture currently bound on unit 0.
     *
     * @param options sampling options; {@code null} or default does nothing
     * @return {@code true} if something was changed and {@link #restore()} must be called
     */
    boolean apply(TextureOptions options) {
        if (options == null || options.isDefault() || applied) return false;
        if (supportsSamplerObjects()) {
            GL33.glBindSampler(0, sampler(options));
            appliedAsSampler = true;
        } else {
            savedMin = GL11.glGetTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER);
            savedMag = GL11.glGetTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER);
            savedWrapS = GL11.glGetTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S);
            savedWrapT = GL11.glGetTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T);
            applyToBoundTexture(options);
            appliedAsSampler = false;
        }
        applied = true;
        return true;
    }

    /** Undoes the last {@link #apply}; does nothing if it changed nothing. */
    void restore() {
        if (!applied) return;
        applied = false;
        if (appliedAsSampler) {
            GL33.glBindSampler(0, 0);
        } else {
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, savedMin);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, savedMag);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, savedWrapS);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, savedWrapT);
        }
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

    /** Generates the mipmap chain of the texture bound on unit 0. */
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
