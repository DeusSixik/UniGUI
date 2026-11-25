package dev.sixik.unigui.backend.minecraft_impl;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import dev.sixik.unigui.api.render.ManagedRenderTarget;
import dev.sixik.unigui.api.render.RenderTargetOptions;
import dev.sixik.unigui.api.render.TextureHandle;

import java.util.Objects;

/**
 * Offscreen-цель UniGUI для Minecraft 26.1 на основе {@link TextureTarget}.
 *
 * <p>В 1.20/1.21 цель привязывалась как текущий framebuffer ({@code bindWrite}). В 26.1 текущего framebuffer нет:
 * цель передаётся в {@link UiGpu#pushTarget}, а каждая отрисовка явно указывает её текстуры.</p>
 */
public final class MinecraftRenderTarget implements ManagedRenderTarget {
    private static int nextId;

    private final RenderTargetOptions options;
    private final ColorTextureHandle colorTexture = new ColorTextureHandle(this);
    private final RenderTarget nativeTarget;
    private boolean disposed;

    public MinecraftRenderTarget(int width, int height, RenderTargetOptions options) {
        this.options = options == null ? RenderTargetOptions.COLOR : options;
        this.nativeTarget = new TextureTarget("UniGUI " + this.options.debugName() + " #" + (nextId++),
                Math.max(1, width), Math.max(1, height), this.options.useDepth());
        clear();
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
        int safeWidth = Math.max(1, width);
        int safeHeight = Math.max(1, height);
        if (safeWidth == nativeTarget.width && safeHeight == nativeTarget.height) return;
        nativeTarget.resize(safeWidth, safeHeight);
        clear();
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

    RenderTarget nativeTarget() {
        ensureAlive();
        return nativeTarget;
    }

    /**
     * Цель для {@link UiGpu}. Если опции требуют очистки перед рендером, цель очищается.
     *
     * @return цель отрисовки
     */
    UiGpu.Target beginWrite() {
        ensureAlive();
        if (options.clearBeforeRender()) clear();
        return gpuTarget();
    }

    /**
     * Цель для {@link UiGpu} без очистки содержимого.
     */
    UiGpu.Target gpuTarget() {
        ensureAlive();
        return new UiGpu.Target(nativeTarget.getColorTextureView(), nativeTarget.getDepthTextureView(),
                nativeTarget.width, nativeTarget.height, false);
    }

    /**
     * Очищает цвет прозрачным, глубину единицей.
     */
    void clear() {
        GpuTexture color = nativeTarget.getColorTexture();
        if (color == null) return;
        GpuTexture depth = nativeTarget.getDepthTexture();
        if (depth != null) {
            RenderSystem.getDevice().createCommandEncoder().clearColorAndDepthTextures(color, 0, depth, 1.0);
        } else {
            RenderSystem.getDevice().createCommandEncoder().clearColorTexture(color, 0);
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
            return "unigui:fbo/" + owner.options().debugName() + "/" + System.identityHashCode(owner);
        }

        @Override
        public int width() {
            return owner.width();
        }

        @Override
        public int height() {
            return owner.height();
        }

        /**
         * @return вид цветовой текстуры или {@code null}, если цель освобождена
         */
        public GpuTextureView view() {
            return owner.isDisposed() ? null : owner.nativeTarget.getColorTextureView();
        }

        /**
         * @return сэмплер по умолчанию (nearest, clamp)
         */
        public GpuSampler defaultSampler() {
            return RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST);
        }

        /**
         * @return {@code true}, если владеющий target уже освобождён и текстуру нельзя привязывать
         */
        public boolean isDisposed() {
            return owner.isDisposed();
        }

        /**
         * Цель отрисовки хранит изображение снизу вверх, поэтому при выборке V отражается.
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
