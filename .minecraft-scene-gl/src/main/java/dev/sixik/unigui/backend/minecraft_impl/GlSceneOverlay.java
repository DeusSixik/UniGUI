package dev.sixik.unigui.backend.minecraft_impl;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

/**
 * A color-only framebuffer view so post passes can sample the scene's depth attachment.
 */
final class GlSceneOverlay implements AutoCloseable {
    /**
     * Texture unit reserved for reading scene depth.
     */
    static final int DEPTH = 12;
    /**
     * Texture unit reserved for a post pass's color source.
     */
    static final int COLOR = 13;
    /**
     * Framebuffer for the destination's color texture.
     */
    private int framebuffer;
    /**
     * Original OpenGL texture names on the two post-pass units.
     */
    private int oldDepth, oldColor;
    /**
     * Whether depth/color were captured before drawing.
     */
    private boolean active;
    /**
     * State operations tracked by the client.
     */
    private final GlScenePlatform.State gl;

    /**
     * @param gl client GL state adapter
     */
    GlSceneOverlay(GlScenePlatform.State gl) {
        this.gl = gl;
    }

    /**
     * Opens the post passes on the upper-left rectangle of the scene's color attachment.
     *
     * @param output scene's color and sampled depth
     * @param width  active region width rounded up to pixels
     * @param height active region height rounded up to pixels
     */
    void begin(GlScenePlatform.Target output, int width, int height) {
        if (framebuffer == 0) framebuffer = GL30.glGenFramebuffers();
        oldColor = exchangeTexture(COLOR, 0);
        oldDepth = exchangeTexture(DEPTH, output.depth());
        active = true;
        bindOutput(output, width, height);
        gl.depthTest(false);
        gl.depthMask(false);
        gl.cull(false);
    }

    /**
     * Selects the original color attachment after writing to a scratch target.
     *
     * @param output scene owning the color texture
     * @param width  active region width in pixels
     * @param height active region height in pixels
     */
    void bindOutput(GlScenePlatform.Target output, int width, int height) {
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, framebuffer);
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0,
                GL11.GL_TEXTURE_2D, output.color(), 0);
        gl.viewport(0, 0, output.width(), output.height());
        gl.scissor(0, output.height() - height, width, height);
    }

    /**
     * Binds an OpenGL texture on one of the reserved units.
     *
     * @param unit zero-based texture unit
     * @param id   texture name
     */
    static void bindTexture(int unit, int id) {
        exchangeTexture(unit, id);
    }

    /**
     * Temporarily selects a reserved shader unit without updating the game's texture state.
     *
     * @param unit reserved OpenGL texture unit
     * @param id   replacement texture name
     * @return previously bound texture name
     */
    static int exchangeTexture(int unit, int id) {
        int active = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        GL13.glActiveTexture(GL13.GL_TEXTURE0 + unit);
        int previous = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, id);
        GL13.glActiveTexture(active);
        return previous;
    }

    /**
     * Assigns a linked sampler to its texture unit.
     *
     * @param program currently bound shader program
     * @param sampler uniform identifier in GLSL
     * @param unit    zero-based texture unit
     */
    static void sampler(int program, String sampler, int unit) {
        GL20.glUniform1i(GL20.glGetUniformLocation(program, sampler), unit);
    }

    /**
     * Releases the attachment and restores texture bindings after a post pass.
     */
    void end() {
        if (!active) return;
        active = false;
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, framebuffer);
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, 0, 0);
        bindTexture(COLOR, oldColor);
        bindTexture(DEPTH, oldDepth);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        if (framebuffer != 0) GL30.glDeleteFramebuffers(framebuffer);
        framebuffer = 0;
    }
}
