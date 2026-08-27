package dev.sixik.unigui.backend.minecraft_impl;

import com.mojang.blaze3d.pipeline.TextureTarget;
import dev.sixik.unigui.api.render.ManagedRenderTarget;
import dev.sixik.unigui.api.render.RenderTargetOptions;
import dev.sixik.unigui.api.render.TextureHandle;
import net.minecraft.client.Minecraft;

import java.util.Objects;

public final class MinecraftRenderTarget implements ManagedRenderTarget {
    private final RenderTargetOptions options;
    private final ColorTextureHandle colorTexture = new ColorTextureHandle(this);
    private com.mojang.blaze3d.pipeline.RenderTarget nativeTarget;
    private boolean disposed;

    public MinecraftRenderTarget(int width, int height, RenderTargetOptions options) {
        this.options = options == null ? RenderTargetOptions.COLOR : options;
        this.nativeTarget = new TextureTarget(Math.max(1, width), Math.max(1, height), this.options.useDepth(), Minecraft.ON_OSX);
        this.nativeTarget.setClearColor(0.0f, 0.0f, 0.0f, 0.0f);
    }

    @Override
    public int width() {
        return nativeTarget.width;
    }

    @Override
    public int height() {
        return nativeTarget.height;
    }

    @Override
    public TextureHandle colorTexture() {
        return colorTexture;
    }

    @Override
    public void resize(int width, int height) {
        ensureAlive();
        nativeTarget.resize(Math.max(1, width), Math.max(1, height), Minecraft.ON_OSX);
    }

    @Override
    public boolean isDisposed() {
        return disposed;
    }

    @Override
    public void close() {
        if (disposed) {
            return;
        }

        nativeTarget.destroyBuffers();
        disposed = true;
    }

    RenderTargetOptions options() {
        return options;
    }

    com.mojang.blaze3d.pipeline.RenderTarget nativeTarget() {
        ensureAlive();
        return nativeTarget;
    }

    void bindWrite() {
        ensureAlive();
        nativeTarget.bindWrite(true);
        if (options.clearBeforeRender()) {
            nativeTarget.setClearColor(0.0f, 0.0f, 0.0f, 0.0f);
            nativeTarget.clear(Minecraft.ON_OSX);
            nativeTarget.bindWrite(true);
        }
    }

    void bindWritePreserveContents() {
        ensureAlive();
        nativeTarget.bindWrite(true);
    }

    void unbindWrite() {
        if (!disposed) {
            nativeTarget.unbindWrite();
        }
    }

    private void ensureAlive() {
        if (disposed) {
            throw new IllegalStateException("RenderTarget is already disposed");
        }
    }

    public static final class ColorTextureHandle implements TextureHandle {
        private final MinecraftRenderTarget owner;

        private ColorTextureHandle(MinecraftRenderTarget owner) {
            this.owner = Objects.requireNonNull(owner, "owner");
        }

        @Override
        public String id() {
            return "unigui:fbo/" + owner.options().debugName() + "/" + textureId();
        }

        @Override
        public int width() {
            return owner.width();
        }

        @Override
        public int height() {
            return owner.height();
        }

        public int textureId() {
            return owner.nativeTarget().getColorTextureId();
        }

        public boolean flipY() {
            return true;
        }

        @Override
        public Object nativeHandle() {
            return this;
        }
    }
}
