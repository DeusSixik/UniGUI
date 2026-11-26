package dev.sixik.unigui.backend.minecraft_impl;

import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.sixik.unigui.api.math.MutableMat4;
import dev.sixik.unigui.api.math.MutableQuat;
import dev.sixik.unigui.api.math.MutableVec3;
import dev.sixik.unigui.api.scene.Camera3D;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Передаёт ванильным моделям мировые матрицы узлов и использует ту же глубину, что и меши.
 */
final class SceneVanillaFeatures implements AutoCloseable {
    /**
     * Версионный сборщик и проход ванильных render state.
     */
    private final SceneFeaturePass pass = new SceneFeaturePass();
    /**
     * Выход PoseStack для extract/submit.
     */
    private final PoseStack pose = new PoseStack();
    /**
     * Переиспользуемый выход модели предмета.
     */
    private final ItemStackRenderState item = new ItemStackRenderState();
    /**
     * Камера для билбордов и feature-рендереров.
     */
    private final CameraRenderState vanillaCamera = new CameraRenderState();
    /**
     * Промежуточные позиция и поворот камеры в координатах сцены.
     */
    private final MutableVec3 eye = new MutableVec3();
    /**
     * Вращение локальных осей камеры в мировые.
     */
    private final MutableQuat rotation = new MutableQuat();
    /**
     * Промежуточная матрица для передачи JOML.
     */
    private final Matrix4f matrix = new Matrix4f();
    /**
     * Перевод из мировых координат в координаты камеры для сортировки ванильных команд.
     */
    private final Matrix4f view = new Matrix4f();
    /**
     * Проекция в часть общей цели, с направлением глубины текущей версии.
     */
    private final Matrix4f projection = new Matrix4f();
    /**
     * Проекция камеры из координат вида в clip space сцены.
     */
    private final MutableMat4 cameraProjection = new MutableMat4();
    /**
     * Значения матрицы в порядке столбцов.
     */
    private final float[] entries = new float[16];
    /**
     * Независимый от мира uniform-блок Projection.
     */
    private final ProjectionMatrixBuffer projectionBuffer = new ProjectionMatrixBuffer("UniGUI vanilla scene");
    /**
     * Uniform-блок отключённого тумана мира.
     */
    private final FogRenderer fog = new FogRenderer();
    /**
     * Есть ли команды, записанные в текущий кадр.
     */
    private boolean submitted;
    /**
     * Дробная часть тика для анимации при извлечении состояния.
     */
    private float partialTick;

    /**
     * Создаёт проход при первом ванильном объекте.
     */
    SceneVanillaFeatures() {
    }

    /**
     * Переносит позу камеры в render state ванильного пайплайна.
     *
     * @param camera      камера сцены.
     * @param cameraView  перевод в координаты камеры.
     * @param partialTick дробная часть тика.
     */
    void begin(Camera3D camera, MutableMat4 cameraView, float partialTick) {
        this.partialTick = partialTick;
        cameraView.copyColumnMajor(entries, 0);
        view.set(entries);
        camera.projection(cameraProjection);
        camera.position(eye);
        camera.orientation(rotation);
        vanillaCamera.initialized = true;
        vanillaCamera.pos = new Vec3(eye.x(), eye.y(), eye.z());
        vanillaCamera.blockPos = BlockPos.containing(vanillaCamera.pos);
        vanillaCamera.orientation.set(rotation.x(), rotation.y(), rotation.z(), rotation.w());
    }

    /**
     * Записывает модель сущности с позой текущего кадра.
     *
     * @param node  узел сущности.
     * @param world мировое преобразование узла.
     */
    void submit(EntityNode node, MutableMat4 world) {
        var dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        EntityRenderState state = node.fixedState();
        if (state == null) {
            state = dispatcher.extractEntity(node.entity(), partialTick);
            state.lightCoords = LightCoordsUtil.FULL_BRIGHT;
            state.outlineColor = EntityRenderState.NO_OUTLINE;
            state.shadowPieces.clear();
            state.nameTag = null;
            state.scoreText = null;
            state.leashStates = null;
        }
        setPose(world);
        submitted = true;
        dispatcher.submit(state, vanillaCamera, 0, 0, 0, pose, pass.storage());
    }

    /**
     * Разрешает и записывает 3D-модель стека для рамки.
     *
     * @param node  узел предмета.
     * @param world мировое преобразование предмета.
     */
    void submit(ItemNode node, MutableMat4 world) {
        if (node.renderStack().isEmpty()) return;
        SceneItemModel.resolve(Minecraft.getInstance(), node.renderStack(), item);
        if (item.isEmpty()) return;
        setPose(world);
        submitted = true;
        item.submit(pose, pass.storage(), LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);
    }

    /**
     * Записывает модель block entity через её рендерер даже вне загруженного мира.
     *
     * @param entity   объект блока или null.
     * @param position дополнительный локальный перенос в структуре.
     * @param world    мировое преобразование узла с блоками.
     */
    void submit(BlockEntity entity, BlockPos position, MutableMat4 world) {
        if (entity == null) return;
        var dispatcher = Minecraft.getInstance().getBlockEntityRenderDispatcher();
        BlockEntityRenderer<BlockEntity, BlockEntityRenderState> renderer = dispatcher.getRenderer(entity);
        if (renderer == null) return;
        BlockEntityRenderState state = renderer.createRenderState();
        renderer.extractRenderState(entity, state, partialTick, vanillaCamera.pos, null);
        state.lightCoords = LightCoordsUtil.FULL_BRIGHT;
        setPose(world);
        pose.translate(position.getX(), position.getY(), position.getZ());
        submitted = true;
        renderer.submit(state, pose, pass.storage(), vanillaCamera);
    }

    /**
     * Проверяет, есть ли ванильная стадия между проходами геометрии сцены.
     *
     * @return true, если как минимум один рендерер передал команды.
     */
    boolean hasSubmissions() {
        return submitted;
    }

    /**
     * Рисует накопленные команды с проекцией сцены и восстанавливает uniforms и ModelView 2D-бэкенда.
     *
     * @param target         offscreen-цель с уже нарисованными непрозрачными мешами.
     * @param viewportWidth  ширина видимой области в физических пикселях.
     * @param viewportHeight высота видимой области в физических пикселях.
     * @param orthographic   режим камеры для сортировки ванильной геометрии.
     */
    void render(UiGpu.Target target, float viewportWidth, float viewportHeight, boolean orthographic) {
        if (!submitted) return;
        float x = viewportWidth / target.width();
        float y = viewportHeight / target.height();
        boolean zeroToOne = MinecraftGpuCompat.zZeroToOne();
        float zSign = SceneFeaturePass.REVERSED_DEPTH ? -1 : 1;
        cameraProjection.copyColumnMajor(entries, 0);
        projection.translation(x - 1, 1 - y, zeroToOne ? 0.5f : 0)
                .scale(x, y, zeroToOne ? zSign * 0.5f : zSign).mul(matrix.set(entries));
        var previousProjection = RenderSystem.getProjectionMatrixBuffer();
        var previousType = RenderSystem.getProjectionType();
        var previousLights = RenderSystem.getShaderLights();
        var previousFog = RenderSystem.getShaderFog();
        var modelView = RenderSystem.getModelViewStack();
        modelView.pushMatrix().identity();
        try {
            RenderSystem.setProjectionMatrix(projectionBuffer.getBuffer(projection),
                    orthographic ? ProjectionType.ORTHOGRAPHIC : ProjectionType.PERSPECTIVE);
            RenderSystem.setShaderFog(fog.getBuffer(FogRenderer.FogMode.NONE));
            SceneFeaturePass.setupLighting();
            int width = Math.clamp((int) Math.ceil(viewportWidth), 1, target.width());
            int height = Math.clamp((int) Math.ceil(viewportHeight), 1, target.height());
            pass.render(target, width, height);
        } finally {
            modelView.popMatrix();
            RenderSystem.setShaderFog(previousFog);
            RenderSystem.setShaderLights(previousLights);
            RenderSystem.setProjectionMatrix(previousProjection, previousType);
        }
    }

    /**
     * Освобождает накопленные команды после кадра или исключения.
     */
    void reset() {
        if (submitted) pass.reset();
        submitted = false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        pass.close();
        projectionBuffer.close();
        fog.close();
    }

    /**
     * Помещает в PoseStack произведение {@code view * world}. Ванильная сортировка
     * получает вершины относительно камеры, а глубина совпадает с глубиной собственных мешей.
     *
     * @param world матрица из локальных координат узла в мировые.
     */
    private void setPose(MutableMat4 world) {
        world.copyColumnMajor(entries, 0);
        pose.setIdentity();
        pose.mulPose(matrix.set(entries).mulLocal(view));
    }
}
