package dev.sixik.unigui.backend.minecraft_impl;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import dev.sixik.unigui.api.scene.EntityNode;
import dev.sixik.unigui.api.scene.ItemNode;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.sixik.unigui.api.math.MutableMat4;
import dev.sixik.unigui.api.math.MutableQuat;
import dev.sixik.unigui.api.math.MutableVec3;
import dev.sixik.unigui.api.scene.Camera3D;
import dev.sixik.unigui.api.scene.Mesh3D;
import dev.sixik.unigui.api.scene.SceneNode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;
import net.minecraft.client.renderer.SubmitNodeStorage;
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
import org.joml.Vector3f;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import java.util.function.Consumer;

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
     * Направления ванильного света в координатах камеры.
     */
    private final Vector3f light0 = new Vector3f(), light1 = new Vector3f();
    /**
     * Блок света в layout std140: два vec3, каждый выровнен до 16 байт.
     */
    private final ByteBuffer lightBytes = ByteBuffer.allocateDirect(Lighting.UBO_SIZE).order(ByteOrder.nativeOrder());
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
     * Collects sprites using the scene camera and the emitter's absolute playback time.
     *
     * @param emitter visible emitter
     * @param cameraView world-to-camera transform
     */
    void submit(MinecraftParticleEmitter emitter, MutableMat4 cameraView) {
        submitted |= pass.collectParticles(emitter, cameraView);
    }

    /**
     * Записывает модель сущности с позой текущего кадра.
     *
     * @param node  узел сущности.
     * @param world мировое преобразование узла.
     */
    void submit(EntityNode node, MutableMat4 world) {
        setPose(world);
        submitted |= submitEntity(node, pass.storage());
    }

    /**
     * Emits the entity's model commands using the active pose.
     * @param node source of the model and head rotation
     * @param storage receives vanilla feature submissions
     * @return whether render state was available and submitted
     */
    private boolean submitEntity(EntityNode node, SubmitNodeStorage storage) {
        SceneEntity handle = MinecraftScene.handle(node);
        if (handle == null) return false;
        var dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        Entity model = handle.entity();
        EntityRenderState state;
        if (model == null) {
            state = handle.fixedState(node.hasAnimation() || node.hasNativeAnimation() || node.hasAction() || node.hasHead());
            if (state == null) return false;
        } else {
            state = dispatcher.extractEntity(model, partialTick);
            state.lightCoords = LightCoordsUtil.FULL_BRIGHT;
            state.outlineColor = EntityRenderState.NO_OUTLINE;
            state.shadowPieces.clear();
            state.nameTag = null;
            state.scoreText = null;
            state.leashStates = null;
        }
        if (!handle.hasSuppliedState()) SceneEntity.sampleAnimation(node, state);
        if (node.hasHead() && state instanceof LivingEntityRenderState living) {
            // Состояние хранит поворот головы относительно тела в градусах.
            living.yRot = (float) Math.toDegrees(node.headYaw());
            living.xRot = (float) Math.toDegrees(node.headPitch());
        }
        dispatcher.submit(state, vanillaCamera, 0, 0, 0, pose, storage);
        return true;
    }

    /**
     * Разрешает и записывает 3D-модель стека для рамки.
     *
     * @param node  узел предмета.
     * @param world мировое преобразование предмета.
     */
    void submit(ItemNode node, MutableMat4 world) {
        setPose(world);
        submitted |= submitItem(node, pass.storage());
    }

    /**
     * Emits the item layers using the active pose.
     * @param node source of the stack and display context
     * @param storage receives vanilla feature submissions
     * @return whether the resolved item contributed a model
     */
    private boolean submitItem(ItemNode node, SubmitNodeStorage storage) {
        ItemStack stack = MinecraftScene.renderStack(node);
        if (stack.isEmpty()) return false;
        SceneItemModel.resolve(Minecraft.getInstance(), stack, MinecraftScene.context(node.display()), item);
        if (item.isEmpty()) return false;
        item.submit(pose, storage, LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);
        return true;
    }

    /**
     * Captures a native item or entity in node-local coordinates for both scene passes.
     * @param node source item or entity participating in scene lighting or using a material effect
     * @param retire drops GPU buffers when cached geometry is replaced
     * @return local surfaces, or an empty list when vanilla should handle the node
     */
    List<SceneBlockMeshes.Surface> bake(SceneNode node, Consumer<Mesh3D> retire) {
        pose.setIdentity();
        return pass.bake(node, storage -> {
            if (node instanceof EntityNode entity) submitEntity(entity, storage);
            else if (node instanceof ItemNode sceneItem) submitItem(sceneItem, storage);
        }, retire);
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
     * Draws soft sprites using the same projection as the native feature pass.
     *
     * @param target scene color and depth
     * @param camera camera supplying clip planes and projection mode
     * @param width active viewport width in pixels
     * @param height active viewport height in pixels
     */
    void renderSoftParticles(UiGpu.Target target, Camera3D camera, float width, float height) {
        pass.renderSoftParticles(target, cameraProjection, camera, width, height);
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
            RenderSystem.setShaderLights(lights());
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
     * Загружает ванильный свет мира, закреплённый в сцене, как у блоков с запечённым затенением граней.
     *
     * @return срез uniform-буфера в формате {@link Lighting}.
     */
    private com.mojang.blaze3d.buffers.GpuBufferSlice lights() {
        MinecraftScene.lightsInView(view, light0, light1);
        lightBytes.clear();
        light0.get(0, lightBytes);
        light1.get(16, lightBytes);
        lightBytes.limit(Lighting.UBO_SIZE);
        return UiGpu.uploadUniform(lightBytes);
    }

    /**
     * Освобождает накопленные команды после кадра или исключения.
     */
    void reset() {
        pass.reset();
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
