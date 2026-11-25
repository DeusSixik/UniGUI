package dev.sixik.unigui.backend.minecraft_impl;

import dev.sixik.unigui.impl.render.shaders.GlslUniformBlocks;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.Map;

/**
 * Пользовательские шейдеры через ванильный пайплайн. В Minecraft 26.1 этот путь не используется: бэкенд всегда
 * OpenGL, и шейдеры рисует прямой GL-путь {@link MinecraftShaderQuadRenderer}. Реализация есть только для 26.2.
 */
final class UiUserShaderPipeline {
    private UiUserShaderPipeline() {
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
    static boolean draw(int key, String id, GlslUniformBlocks.Result shader, MinecraftUiBlend blend,
                        @Nullable ByteBuffer uniformData, Map<String, UiTextureBinding> textures) {
        return false;
    }
}
