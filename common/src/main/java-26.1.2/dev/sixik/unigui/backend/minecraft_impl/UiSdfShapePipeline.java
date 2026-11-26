package dev.sixik.unigui.backend.minecraft_impl;

import java.nio.FloatBuffer;

/**
 * SDF-фигуры через ванильный пайплайн. В Minecraft 26.1 этот путь не используется: бэкенд всегда OpenGL, и
 * фигуры рисует прямой GL-путь {@link MinecraftSdfShapeRenderer}. Реализация есть только для 26.2.
 */
final class UiSdfShapePipeline {
    private UiSdfShapePipeline() {
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
    static boolean draw(MinecraftUiBlend blend, FloatBuffer instances, int count) {
        return false;
    }
}
