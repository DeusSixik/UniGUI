package dev.sixik.unigui.backend.minecraft_impl;

import dev.sixik.unigui.api.math.MutableMat4;
import dev.sixik.unigui.api.math.Mat4View;
import dev.sixik.unigui.api.scene.Camera3D;
import dev.sixik.unigui.api.scene.Mesh3D;
import dev.sixik.unigui.api.scene.SceneNode;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.ScissorState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;

import java.util.List;
import java.util.function.Consumer;

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
     * @param emitter source of native sprites
     * @param view world-to-camera transform
     * @return false because this version has no offscreen quad particle feature
     */
    boolean collectParticles(MinecraftParticleEmitter emitter, MutableMat4 view) {
        return false;
    }

    /**
     * @param target scene color and depth
     * @param projection canonical camera projection
     * @param camera camera supplying clip planes
     * @param width active viewport width in pixels
     * @param height active viewport height in pixels
     */
    void renderSoftParticles(UiGpu.Target target, Mat4View projection, Camera3D camera, float width, float height) {
        // Soft particles use the 26.3 shader pipeline.
    }

    /**
     * @param node source of the native model
     * @param submit writes the native commands for the node
     * @param retire releases evicted mesh buffers
     * @return empty because native surface shaders are only available on 26.3
     */
    List<SceneBlockMeshes.Surface> bake(SceneNode node, Consumer<SubmitNodeStorage> submit, Consumer<Mesh3D> retire) {
        return List.of();
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
