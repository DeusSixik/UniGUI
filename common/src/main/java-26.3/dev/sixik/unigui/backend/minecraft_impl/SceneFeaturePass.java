package dev.sixik.unigui.backend.minecraft_impl;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.renderpearl.api.commands.RenderPass;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;

/**
 * Версионная отправка ванильной геометрии в offscreen-цель сцены.
 */
final class SceneFeaturePass implements AutoCloseable {
    /**
     * Ванильные пайплайны этой версии используют обратное направление глубины.
     */
    static final boolean REVERSED_DEPTH = true;
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
     * Привязывает ванильный свет для моделей, показанных внутри UI.
     */
    static void setupLighting() {
        Minecraft.getInstance().gameRenderer.lighting().setupFor(Lighting.Entry.ENTITY_IN_UI);
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
        try (var prepared = client.gameRenderer.featureRenderDispatcher().prepareFrame(storage);
             RenderPass pass = MinecraftGpuCompat.openPass(() -> "UniGUI vanilla scene", target.color(), target.depth())) {
            if (width != target.width() || height != target.height()) {
                pass.enableScissor(0, target.height() - height, width, height);
            }
            FeatureRenderDispatcher.renderAllFeatures(pass, prepared);
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
