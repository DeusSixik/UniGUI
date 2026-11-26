package dev.sixik.unigui.backend.minecraft_impl;

import dev.sixik.unigui.api.scene.BlockNode;
import dev.sixik.unigui.api.scene.EntityNode;
import dev.sixik.unigui.api.scene.ItemNode;
import dev.sixik.unigui.api.scene.StructureNode;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.sixik.unigui.api.math.MutableMat4;
import dev.sixik.unigui.api.scene.Camera3D;
import dev.sixik.unigui.api.scene.SceneNode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Рисует ванильные предметы, сущности и block entity в общей GL-цели.
 */
final class LegacySceneFeatures implements MultiBufferSource {
    /**
     * Общий для последовательных UI-проходов пул ванильных нативных буферов клиента.
     */
    private static final ArrayDeque<SceneFeatureBuffer> POOL = new ArrayDeque<>();
    /**
     * Повторно используемые слои, допускающие объединение примитивов.
     */
    private final Map<RenderType, Layer> groupedLayers = new LinkedHashMap<>();
    /**
     * Все порции геометрии в порядке первого использования.
     */
    private final List<Layer> layers = new ArrayList<>();
    /**
     * Поза, которую меняют ванильные рендереры.
     */
    private final PoseStack pose = new PoseStack();
    /**
     * Матрица вида сцены.
     */
    private final Matrix4f view = new Matrix4f();
    /**
     * Направления ванильного света в координатах камеры.
     */
    private final Vector3f light0 = new Vector3f(), light1 = new Vector3f();
    /**
     * Проекция камеры в видимый прямоугольник RTT.
     */
    private final Matrix4f projection = new Matrix4f();
    /**
     * Матрица для передачи преобразования узла в PoseStack.
     */
    private final Matrix4f transform = new Matrix4f();
    /**
     * Временная проекция в координатах камеры.
     */
    private final MutableMat4 cameraProjection = new MutableMat4();
    /**
     * Рабочая мировая матрица узла.
     */
    private final MutableMat4 world = new MutableMat4();
    /**
     * Компоненты переносимой матрицы, по столбцам.
     */
    private final float[] entries = new float[16];
    /**
     * Вращение билбордов при уже применённой сценовой матрице вида.
     */
    private final Quaternionf billboard = new Quaternionf();

    /**
     * Рисует модели в цель со своим видом и проекцией. Сортировка квадов видит координаты камеры.
     *
     * @param models      видимые узлы с ванильным содержимым.
     * @param modelCount  количество узлов.
     * @param target      активная offscreen-цель.
     * @param camera      камера сцены.
     * @param viewMatrix  матрица из координат сцены в координаты камеры.
     * @param scaleX      доля ширины цели, занимаемая областью отрисовки.
     * @param scaleY      доля высоты цели, занимаемая областью отрисовки.
     * @param partialTick дробная часть текущего тика.
     */
    void render(SceneNode[] models, int modelCount, MinecraftRenderTarget target, Camera3D camera,
                MutableMat4 viewMatrix, float scaleX, float scaleY, float partialTick) {
        Minecraft client = Minecraft.getInstance();
        viewMatrix.copyColumnMajor(entries, 0);
        view.set(entries);
        camera.projection(cameraProjection).copyColumnMajor(entries, 0);
        // x' = scaleX * x + (scaleX - 1) * w; y' = scaleY * y + (1 - scaleY) * w.
        projection.translation(scaleX - 1, 1 - scaleY, 0).scale(scaleX, scaleY, 1).mul(transform.set(entries));
        var oldProjection = RenderSystem.getProjectionMatrix();
        var oldSorting = RenderSystem.getVertexSorting();
        var modelView = RenderSystem.getModelViewStack();
        var dispatcher = client.getEntityRenderDispatcher();
        var oldOrientation = dispatcher.cameraOrientation();
        var oldShader = RenderSystem.getShader();
        var oldTextureMatrix = RenderSystem.getTextureMatrix();
        float oldFogStart = RenderSystem.getShaderFogStart();
        float[] color = RenderSystem.getShaderColor();
        float red = color[0], green = color[1], blue = color[2], alpha = color[3];
        int texture0 = RenderSystem.getShaderTexture(0);
        int texture1 = RenderSystem.getShaderTexture(1);
        int texture2 = RenderSystem.getShaderTexture(2);
        VertexSorting sorting = camera.orthographic() ? VertexSorting.ORTHOGRAPHIC_Z : VertexSorting.DISTANCE_TO_ORIGIN;
        MinecraftFastItemCompat.pushModelView(modelView);
        try {
            MinecraftFastItemCompat.resetModelView(modelView);
            RenderSystem.applyModelViewMatrix();
            RenderSystem.setProjectionMatrix(projection, sorting);
            RenderSystem.setShaderColor(1, 1, 1, 1);
            FogRenderer.setupNoFog();
            // Ванильный свет мира, закреплённый в сцене, как у блоков с запечённым затенением граней.
            MinecraftScene.lightsInView(view, light0, light1);
            RenderSystem.setShaderLights(light0, light1);
            dispatcher.overrideCameraOrientation(billboard);
            for (int i = 0; i < modelCount; i++) {
                submit(client, models[i], partialTick);
            }
            BufferUploader.invalidate();
            for (Layer layer : layers) if (!layer.translucent) layer.draw(target, sorting);
            for (Layer layer : layers) if (layer.translucent) layer.draw(target, sorting);
        } finally {
            for (Layer layer : layers) {
                layer.buffer.discard();
                POOL.addFirst(layer.buffer);
            }
            layers.clear();
            groupedLayers.clear();
            dispatcher.overrideCameraOrientation(oldOrientation);
            MinecraftFastItemCompat.popModelView(modelView);
            RenderSystem.applyModelViewMatrix();
            RenderSystem.setProjectionMatrix(oldProjection, oldSorting);
            RenderSystem.setShader(() -> oldShader);
            RenderSystem.setTextureMatrix(oldTextureMatrix);
            RenderSystem.setShaderFogStart(oldFogStart);
            RenderSystem.setShaderColor(red, green, blue, alpha);
            RenderSystem.setShaderTexture(0, texture0);
            RenderSystem.setShaderTexture(1, texture1);
            RenderSystem.setShaderTexture(2, texture2);
            target.bindWritePreserveContents();
            BufferUploader.invalidate();
        }
    }

    /**
     * Набирает геометрию модели с данным преобразованием из координат узла.
     *
     * @param client      текущий клиент.
     * @param node        узел с ванильным содержимым.
     * @param partialTick дробная часть тика.
     */
    private void submit(Minecraft client, SceneNode node, float partialTick) {
        node.worldMatrix(world).copyColumnMajor(entries, 0);
        transform.set(entries).mulLocal(view);
        if (node instanceof BlockNode block) {
            submitBlockEntity(client, MinecraftScene.blockEntity(block), BlockPos.ZERO, partialTick);
        } else if (node instanceof StructureNode structure) {
            int top = structure.topVisibleY();
            for (StructureNode.Section section : structure.sections()) {
                int bottom = section.minBlockY();
                if (bottom > top || section.blockEntityCount() == 0) continue;
                for (int index = 0; index < StructureNode.Section.VOLUME; index++) {
                    if (section.blockEntity(index) instanceof BlockEntity blockEntity
                            && bottom + StructureNode.Section.localY(index) <= top) {
                        submitBlockEntity(client, blockEntity, blockEntity.getBlockPos(), partialTick);
                    }
                }
            }
        } else if (node instanceof ItemNode item && !MinecraftScene.renderStack(item).isEmpty()) {
            resetPose();
            client.getItemRenderer().renderStatic(MinecraftScene.renderStack(item), MinecraftScene.context(item.display()), LightTexture.FULL_BRIGHT,
                    OverlayTexture.NO_OVERLAY, pose, this, client.level, 0);
        } else if (node instanceof EntityNode entity) {
            Entity model = MinecraftScene.entity(entity);
            if (model == null) return;
            if (entity.hasHead()) applyHead(model, entity);
            resetPose();
            var renderer = client.getEntityRenderDispatcher().getRenderer(model);
            var offset = renderer.getRenderOffset(model, partialTick);
            pose.translate(offset.x, offset.y, offset.z);
            renderer.render(model, Mth.rotLerp(partialTick, model.yRotO, model.getYRot()),
                    partialTick, pose, this, LightTexture.FULL_BRIGHT);
        }
    }

    /**
     * Поворачивает голову относительно тела. Minecraft хранит абсолютное рыскание головы в градусах; значения
     * прошлого тика тоже меняются, чтобы интерполяция не раскачивала голову.
     *
     * @param model сущность.
     * @param node  узел с углами в радианах.
     */
    private static void applyHead(Entity model, EntityNode node) {
        float pitch = (float) Math.toDegrees(node.headPitch());
        model.setXRot(pitch);
        model.xRotO = pitch;
        if (model instanceof net.minecraft.world.entity.LivingEntity living) {
            float yaw = living.yBodyRot + (float) Math.toDegrees(node.headYaw());
            living.yHeadRot = yaw;
            living.yHeadRotO = yaw;
            living.yBodyRotO = living.yBodyRot;
        }
    }

    /**
     * Записывает геометрию блока через его renderer, в том числе вне загруженного мира.
     *
     * @param client      текущий клиент.
     * @param entity      объект блока либо null.
     * @param pos         локальная позиция блока.
     * @param partialTick дробная часть тика.
     */
    private void submitBlockEntity(Minecraft client, BlockEntity entity, BlockPos pos, float partialTick) {
        if (entity == null) return;
        var renderer = client.getBlockEntityRenderDispatcher().getRenderer(entity);
        if (renderer == null) return;
        resetPose();
        pose.translate(pos.getX(), pos.getY(), pos.getZ());
        renderer.render(entity, partialTick, pose, this, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
    }

    /**
     * Восстанавливает матрицу вида, умноженную на мировую матрицу узла.
     */
    private void resetPose() {
        pose.setIdentity();
        ScenePoseCompat.multiply(pose, transform);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public VertexConsumer getBuffer(RenderType type) {
        boolean grouped = type.canConsolidateConsecutiveGeometry();
        Layer layer = grouped ? groupedLayers.get(type) : null;
        if (layer == null) {
            SceneFeatureBuffer buffer = POOL.pollFirst();
            if (buffer == null) buffer = new SceneFeatureBuffer();
            layer = new Layer(type, buffer, buffer.begin(type));
            layers.add(layer);
            if (grouped) groupedLayers.put(type, layer);
        }
        return layer.vertices;
    }

    /**
     * Геометрия одного RenderType, отложенная до прохода с общей глубиной.
     */
    private static final class Layer {
        /**
         * Набор настроек шейдера и текстур.
         */
        final RenderType type;
        /**
         * Буфер, которому принадлежат вершины.
         */
        final SceneFeatureBuffer buffer;
        /**
         * Потребитель, в который пишет ванильный рендерер.
         */
        final VertexConsumer vertices;
        /**
         * Ванильный слой требует сортировки прозрачных квадов.
         */
        final boolean translucent;

        /**
         * Регистрирует новый слой текущего кадра.
         *
         * @param type     тип геометрии.
         * @param buffer   рабочий буфер.
         * @param vertices потребитель вершин.
         */
        Layer(RenderType type, SceneFeatureBuffer buffer, VertexConsumer vertices) {
            this.type = type;
            this.buffer = buffer;
            this.vertices = vertices;
            translucent = ScenePoseCompat.sortsOnUpload(type);
        }

        /**
         * Устанавливает RenderType и направляет его вывод в цель сцены.
         *
         * @param target  общая цель сцены.
         * @param sorting сортировка квадов прозрачного слоя.
         */
        void draw(MinecraftRenderTarget target, VertexSorting sorting) {
            type.setupRenderState();
            try {
                // Некоторые RenderType выбирают свой framebuffer при включённой графике Fabulous.
                target.bindWritePreserveContents();
                if (GL11.glIsEnabled(GL11.GL_BLEND) && GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB) == GL11.GL_SRC_ALPHA
                        && GL11.glGetInteger(GL14.GL_BLEND_DST_RGB) == GL11.GL_ONE_MINUS_SRC_ALPHA) {
                    MinecraftUiBlend.applyStraightAlpha(true);
                }
                buffer.draw(translucent && type.mode() == VertexFormat.Mode.QUADS ? sorting : null);
            } finally {
                type.clearRenderState();
            }
        }
    }
}
