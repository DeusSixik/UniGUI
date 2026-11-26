package dev.sixik.unigui.backend.minecraft_impl;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.ScissorState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeStorage;

/**
 * Версионная отправка ванильной геометрии в offscreen-цель сцены.
 */
final class SceneFeaturePass implements AutoCloseable {
    /**
     * Ванильные пайплайны этой версии используют обратное направление глубины.
     */
    static final boolean REVERSED_DEPTH = true;
    /**
     * Сохранённое отсечение ванильных RenderType.
     */
    private final ScissorState savedScissor = new ScissorState();
    /**
     * Команды сцены, изолированные от команд основного мира.
     */
    private final SubmitNodeStorage storage = new SubmitNodeStorage();

    /**
     * Создаёт сборщик команд без прохода в GPU.
     */
    SceneFeaturePass() {
    }

    /**
     * Читает сборщик для текущего прохода.
     *
     * @return очередь ванильных объектов сцены.
     */
    SubmitNodeStorage storage() {
        return storage;
    }

    /**
     * Запускает ванильные стадии отрисовки в общие цвет и глубину.
     *
     * @param target цель сцены.
     * @param width  ширина области в пикселях.
     * @param height высота области в пикселях.
     */
    void render(UiGpu.Target target, int width, int height) {
        var client = Minecraft.getInstance();
        var previousColor = RenderSystem.outputColorTextureOverride;
        var previousDepth = RenderSystem.outputDepthTextureOverride;
        var scissor = RenderSystem.getScissorStateForRenderTypeDraws();
        savedScissor.setFrom(scissor);
        RenderSystem.outputColorTextureOverride = target.color();
        RenderSystem.outputDepthTextureOverride = target.depth();
        scissor.enable(0, target.height() - height, width, height);
        try {
            client.gameRenderer.featureRenderDispatcher().renderAllFeatures(storage);
        } finally {
            RenderSystem.outputColorTextureOverride = previousColor;
            RenderSystem.outputDepthTextureOverride = previousDepth;
            scissor.setFrom(savedScissor);
        }
    }

    /**
     * Сбрасывает команды кадра, включая остатки при исключении в рендерере объекта.
     */
    void reset() {
        storage.getSubmitsPerOrder().clear();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        reset();
    }
}
