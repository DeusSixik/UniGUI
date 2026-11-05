package dev.sixik.unigui.cleanroom.backend;

import dev.sixik.unigui.api.render.ManagedRenderTarget;
import dev.sixik.unigui.api.render.RenderTargetOptions;
import dev.sixik.unigui.api.render.TextureHandle;
import dev.sixik.unigui.api.render.TextureOptions;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.shader.Framebuffer;
import org.lwjgl.opengl.GL11;

import java.nio.IntBuffer;
import java.util.Objects;

/**
 * Offscreen render target backed by a vanilla {@link Framebuffer} (RGBA8 color texture, optional depth buffer).
 *
 * <p>UI drawn into the target ends up with premultiplied color, so {@link #colorTexture()} is a
 * premultiplied-alpha texture that is sampled with {@code GL_ONE} as the source factor, and stored upside down in GL
 * terms, so it is drawn with its V flipped ({@link ColorTextureHandle#flipY()}).</p>
 *
 * <p>Vanilla's {@code Framebuffer} create/resize/delete methods rebind the default framebuffer and enable depth
 * testing as a side effect; every call here goes through {@link Guard} so the caller's state survives. Render thread
 * only.</p>
 */
public final class CleanroomRenderTarget implements ManagedRenderTarget {
    private final RenderTargetOptions options;
    private final ColorTextureHandle colorTexture = new ColorTextureHandle(this);
    private Framebuffer framebuffer;
    private int width;
    private int height;
    private boolean disposed;

    /**
     * @param width   width in pixels, at least 1
     * @param height  height in pixels, at least 1
     * @param options target options, {@code null} means {@link RenderTargetOptions#COLOR}
     * @throws UnsupportedOperationException if framebuffer objects are disabled
     */
    public CleanroomRenderTarget(int width, int height, RenderTargetOptions options) {
        if (!OpenGlHelper.isFramebufferEnabled()) {
            throw new UnsupportedOperationException("Framebuffer objects are disabled; render targets need them");
        }
        this.options = options == null ? RenderTargetOptions.COLOR : options;
        this.width = Math.max(1, width);
        this.height = Math.max(1, height);
        Guard guard = Guard.capture();
        try {
            framebuffer = new Framebuffer(this.width, this.height, this.options.useDepth());
            framebuffer.setFramebufferColor(0.0f, 0.0f, 0.0f, 0.0f);
        } finally {
            guard.restore();
        }
    }

    @Override
    public int width() {
        return width;
    }

    @Override
    public int height() {
        return height;
    }

    @Override
    public TextureHandle colorTexture() {
        return colorTexture;
    }

    @Override
    public void resize(int newWidth, int newHeight) {
        ensureAlive();
        int w = Math.max(1, newWidth);
        int h = Math.max(1, newHeight);
        if (w == width && h == height) return;
        Guard guard = Guard.capture();
        try {
            framebuffer.createBindFramebuffer(w, h);
            framebuffer.setFramebufferColor(0.0f, 0.0f, 0.0f, 0.0f);
        } finally {
            guard.restore();
        }
        width = w;
        height = h;
    }

    @Override
    public boolean isDisposed() {
        return disposed;
    }

    @Override
    public void close() {
        if (disposed) return;
        disposed = true;
        Guard guard = Guard.capture();
        try {
            framebuffer.deleteFramebuffer();
        } finally {
            guard.restore();
        }
    }

    RenderTargetOptions options() {
        return options;
    }

    /**
     * @return the GL name of the color texture
     */
    int textureId() {
        ensureAlive();
        return framebuffer.framebufferTexture;
    }

    /**
     * Binds the framebuffer for drawing, sets the viewport to the full target and clears it when the options ask for
     * it. The caller must restore the previous framebuffer and viewport (see {@link Guard}).
     */
    void bindWrite() {
        ensureAlive();
        framebuffer.bindFramebuffer(true);
        if (options.clearBeforeRender()) {
            GlStateManager.clearColor(0.0f, 0.0f, 0.0f, 0.0f);
            GlStateManager.clear(options.useDepth()
                    ? GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT
                    : GL11.GL_COLOR_BUFFER_BIT);
        }
    }

    private void ensureAlive() {
        if (disposed) throw new IllegalStateException("RenderTarget is already disposed");
    }

    /**
     * The state a framebuffer operation may disturb: the bound framebuffer, the viewport and depth testing.
     */
    static final class Guard {
        private final int framebuffer;
        private final int[] viewport = new int[4];
        private final boolean depth;

        private Guard(int framebuffer, IntBuffer viewport, boolean depth) {
            this.framebuffer = framebuffer;
            for (int i = 0; i < 4; i++) this.viewport[i] = viewport.get(i);
            this.depth = depth;
        }

        /**
         * @return a guard holding the current state
         */
        static Guard capture() {
            IntBuffer viewport = org.lwjgl.BufferUtils.createIntBuffer(16);
            GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
            int binding = GL11.glGetInteger(org.lwjgl.opengl.GL30.GL_FRAMEBUFFER_BINDING);
            return new Guard(binding, viewport, GL11.glIsEnabled(GL11.GL_DEPTH_TEST));
        }

        /**
         * Puts the captured state back.
         */
        void restore() {
            OpenGlHelper.glBindFramebuffer(OpenGlHelper.GL_FRAMEBUFFER, framebuffer);
            GlStateManager.viewport(viewport[0], viewport[1], viewport[2], viewport[3]);
            if (depth) GlStateManager.enableDepth();
            else GlStateManager.disableDepth();
        }
    }

    /**
     * Handle to the color texture of a {@link CleanroomRenderTarget}. Draw it like any other texture; the backend
     * binds the GL name directly and flips V.
     */
    public static final class ColorTextureHandle implements TextureHandle {
        private static final TextureOptions OPTIONS = TextureOptions.defaults().premultipliedAlpha(true);

        private final CleanroomRenderTarget owner;

        private ColorTextureHandle(CleanroomRenderTarget owner) {
            this.owner = Objects.requireNonNull(owner, "owner");
        }

        @Override
        public String id() {
            // The GL name is part of the id: batching and caches key on it, and two targets may share a debug name.
            int gl = owner.disposed ? -1 : owner.framebuffer.framebufferTexture;
            return "unigui:fbo/" + owner.options.debugName() + "/" + gl;
        }

        @Override
        public int width() {
            return owner.width;
        }

        @Override
        public int height() {
            return owner.height;
        }

        @Override
        public TextureOptions options() {
            return OPTIONS;
        }

        /**
         * @return the GL texture name
         */
        public int textureId() {
            return owner.textureId();
        }

        /**
         * @return {@code true} once the target was closed; drawing a disposed target is skipped
         */
        public boolean isDisposed() {
            return owner.disposed;
        }

        /**
         * @return {@code true}: framebuffer rows run bottom to top, so V must be flipped when drawing
         */
        public boolean flipY() {
            return true;
        }

        @Override
        public Object nativeHandle() {
            return this;
        }
    }
}
