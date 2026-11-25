package dev.sixik.unigui.backend.minecraft_impl;

import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;

import java.nio.FloatBuffer;

/**
 * SDF-текст через ванильный пайплайн. В Minecraft 26.1 этот путь не используется: бэкенд всегда OpenGL, и текст
 * рисует прямой GL-путь {@link MinecraftSdfTextRenderer}. Реализация есть только для 26.2.
 */
final class UiSdfTextPipeline {
    private UiSdfTextPipeline() {
    }

    /**
     * @return всегда {@code false}
     */
    static boolean available() {
        return false;
    }

    /**
     * @return всегда {@code false}
     */
    static boolean draw(MinecraftUiBlend blend, GpuTextureView atlas, GpuSampler sampler,
                        FloatBuffer instances, int count) {
        return false;
    }
}
