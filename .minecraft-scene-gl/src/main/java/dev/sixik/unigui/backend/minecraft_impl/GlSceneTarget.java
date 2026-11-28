package dev.sixik.unigui.backend.minecraft_impl;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL21;
import org.lwjgl.opengl.GL30;

import java.nio.ByteBuffer;

/**
 * Offscreen storage for either a light depth texture or the fog integration color texture.
 */
final class GlSceneTarget implements GlScenePlatform.Target, AutoCloseable {
    /**
     * Framebuffer name.
     */
    private final int framebuffer;
    /**
     * Texture name for the sole attachment.
     */
    private final int texture;
    /**
     * Size of the attachment in pixels.
     */
    private final int width, height;
    /**
     * Whether the attachment stores depth values.
     */
    private final boolean depth;

    /**
     * Allocates one framebuffer attachment and restores the caller's bindings.
     *
     * @param width  width in pixels
     * @param height height in pixels
     * @param depth  true for depth-only storage, false for RGBA8 storage
     * @throws IllegalStateException if the driver could not create the target
     */
    GlSceneTarget(int width, int height, boolean depth) {
        this.width = width;
        this.height = height;
        this.depth = depth;
        int oldDraw = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        int oldRead = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        int oldTexture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        int unpack = GL11.glGetInteger(GL21.GL_PIXEL_UNPACK_BUFFER_BINDING);
        framebuffer = GL30.glGenFramebuffers();
        texture = GL11.glGenTextures();
        try {
            GL15.glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER, 0);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, depth ? GL14.GL_DEPTH_COMPONENT24 : GL11.GL_RGBA8, width, height,
                    0, depth ? GL11.GL_DEPTH_COMPONENT : GL11.GL_RGBA,
                    depth ? GL11.GL_UNSIGNED_INT : GL11.GL_UNSIGNED_BYTE, (ByteBuffer) null);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, framebuffer);
            GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, depth ? GL30.GL_DEPTH_ATTACHMENT : GL30.GL_COLOR_ATTACHMENT0,
                    GL11.GL_TEXTURE_2D, texture, 0);
            if (depth) {
                GL11.glDrawBuffer(GL11.GL_NONE);
                GL11.glReadBuffer(GL11.GL_NONE);
            }
            if (GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER) != GL30.GL_FRAMEBUFFER_COMPLETE) {
                throw new IllegalStateException("Incomplete UniGUI scene framebuffer");
            }
        } catch (RuntimeException | Error failure) {
            close();
            throw failure;
        } finally {
            GL15.glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER, unpack);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, oldTexture);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, oldDraw);
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, oldRead);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int width() {
        return width;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int height() {
        return height;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int color() {
        return depth ? 0 : texture;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int depth() {
        return depth ? texture : 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void bind() {
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, framebuffer);
        GL11.glViewport(0, 0, width, height);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        GL30.glDeleteFramebuffers(framebuffer);
        GL11.glDeleteTextures(texture);
    }
}
