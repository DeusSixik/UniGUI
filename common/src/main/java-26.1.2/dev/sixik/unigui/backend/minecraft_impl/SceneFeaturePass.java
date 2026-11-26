package dev.sixik.unigui.backend.minecraft_impl;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.ScissorState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;

/**
 * Версионная отправка ванильной геометрии в offscreen-цель сцены.
 */
final class SceneFeaturePass implements AutoCloseable {
    /**
     * Ванильные пайплайны этой версии используют прямое направление глубины.
     */
    static final boolean REVERSED_DEPTH = false;
    /**
     * Сохранённое отсечение ванильных RenderType.
     */
    private final ScissorState savedScissor = new ScissorState();
    /**
     * Команды сцены, изолированные от команд основного мира.
     */
    private final SubmitNodeStorage storage = new SubmitNodeStorage();
    /**
     * Экземпляр dispatcher с собственным SubmitNodeStorage.
     */
    private FeatureRenderDispatcher features;

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
     * Привязывает ванильный свет для моделей, показанных внутри UI.
     */
    static void setupLighting() {
        Minecraft.getInstance().gameRenderer.getLighting().setupFor(Lighting.Entry.ENTITY_IN_UI);
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
        if (features == null) {
            var buffers = client.renderBuffers();
            features = new FeatureRenderDispatcher(storage, client.getModelManager(), buffers.bufferSource(),
                    client.getAtlasManager(), buffers.outlineBufferSource(), buffers.crumblingBufferSource(),
                    client.font, client.gameRenderer.getGameRenderState());
        }
        var previousColor = RenderSystem.outputColorTextureOverride;
        var previousDepth = RenderSystem.outputDepthTextureOverride;
        var scissor = RenderSystem.getScissorStateForRenderTypeDraws();
        if (scissor.enabled()) savedScissor.enable(scissor.x(), scissor.y(), scissor.width(), scissor.height());
        else savedScissor.disable();
        RenderSystem.outputColorTextureOverride = target.color();
        RenderSystem.outputDepthTextureOverride = target.depth();
        scissor.enable(0, target.height() - height, width, height);
        try {
            features.renderAllFeatures();
            client.renderBuffers().bufferSource().endBatch();
        } finally {
            RenderSystem.outputColorTextureOverride = previousColor;
            RenderSystem.outputDepthTextureOverride = previousDepth;
            if (savedScissor.enabled())
                scissor.enable(savedScissor.x(), savedScissor.y(), savedScissor.width(), savedScissor.height());
            else scissor.disable();
        }
    }

    /**
     * Сбрасывает команды кадра, включая остатки при исключении в рендерере объекта.
     */
    void reset() {
        storage.clear();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        reset();
        if (features != null) features.close();
    }
}
