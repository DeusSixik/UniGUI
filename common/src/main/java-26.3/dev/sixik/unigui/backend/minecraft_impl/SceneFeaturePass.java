package dev.sixik.unigui.backend.minecraft_impl;

import dev.sixik.unigui.api.math.MutableMat4;
import dev.sixik.unigui.api.math.Mat4View;
import dev.sixik.unigui.api.scene.Camera3D;
import dev.sixik.unigui.api.scene.Mesh3D;
import dev.sixik.unigui.api.scene.SceneNode;

import com.mojang.renderpearl.api.commands.RenderPass;
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
     * Ванильные пайплайны этой версии используют обратное направление глубины.
     */
    static final boolean REVERSED_DEPTH = true;
    /**
     * Команды сцены, изолированные от команд основного мира.
     */
    private final SubmitNodeStorage storage = new SubmitNodeStorage();
    /**
     * Isolated native model commands for conversion to indexed scene geometry.
     */
    private SceneNativeMeshes nativeMeshes;
    /**
     * Particle samples and frame batch, allocated when a scene uses its first emitter.
     */
    private SceneParticles particles;

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
     * Collects sprites from a visible vanilla emitter.
     *
     * @param emitter source of native sprites
     * @param view    world-to-camera transform
     * @return whether the emitter contributed to the batch
     */
    boolean collectParticles(MinecraftParticleEmitter emitter, MutableMat4 view) {
        if (particles == null) particles = new SceneParticles();
        return particles.collect(emitter, view);
    }

    /**
     * Converts supported commands into surfaces in the node's local coordinates.
     *
     * @param node   source of the native model
     * @param submit emits the native commands using a node-local pose
     * @param retire releases evicted mesh buffers
     * @return converted surfaces, or an empty list for unsupported or empty geometry
     */
    List<SceneBlockMeshes.Surface> bake(SceneNode node, Consumer<SubmitNodeStorage> submit, Consumer<Mesh3D> retire) {
        Minecraft client = Minecraft.getInstance();
        if (nativeMeshes == null) nativeMeshes = new SceneNativeMeshes(retire);
        nativeMeshes.updateResources(SceneItemModel.generation(client), client.level);
        return nativeMeshes.bake(node, submit);
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
        if (particles != null) particles.submit(storage);
        try (var prepared = client.gameRenderer.featureRenderDispatcher().prepareFrame(storage);
             RenderPass pass = MinecraftGpuCompat.openPass(() -> "UniGUI vanilla scene", target.color(), target.depth())) {
            if (width != target.width() || height != target.height()) {
                pass.enableScissor(0, target.height() - height, width, height);
            }
            FeatureRenderDispatcher.renderAllFeatures(pass, prepared);
        }
    }

    /**
     * Composites sprites that sample scene depth into scene color.
     *
     * @param target     scene color and depth
     * @param projection canonical camera projection
     * @param camera     camera supplying clip planes
     * @param width      active viewport width in pixels
     * @param height     active viewport height in pixels
     */
    void renderSoftParticles(UiGpu.Target target, Mat4View projection, Camera3D camera, float width, float height) {
        if (particles != null) particles.renderSoft(target, projection, camera, width, height);
    }

    /**
     * Сбрасывает команды кадра, включая остатки при исключении в рендерере объекта.
     */
    void reset() {
        storage.getSubmitsPerOrder().clear();
        if (particles != null) particles.reset();
        if (nativeMeshes != null) nativeMeshes.trim();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        nativeMeshes = null;
        if (particles != null) particles.close();
        particles = null;
        reset();
    }
}
