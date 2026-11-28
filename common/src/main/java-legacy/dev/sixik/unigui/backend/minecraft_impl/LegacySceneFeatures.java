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
import dev.sixik.unigui.api.scene.Material;
import dev.sixik.unigui.api.scene.Mesh3D;
import dev.sixik.unigui.api.render.SimpleTextureHandle;
import dev.sixik.unigui.api.render.TextureOptions;
import dev.sixik.unigui.backend.minecraft_impl.custom_renders.MinecraftRendererPlatformHook;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

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
    /** Local meshes shared by all depth and color passes of one frame. Null values select vanilla fallback. */
    private final Map<SceneNode, List<GlScenePlatform.Surface>> captures = new IdentityHashMap<>();
    /** Quad consumers keyed by native layer during a local capture. */
    private final Map<RenderType, SceneBlockVertexConsumer> capturedLayers = new LinkedHashMap<>();
    /** Whether buffers should collect local indexed meshes. */
    private boolean capturing;
    /** Frame-relative scene pose override for the vanilla entity renderer. */
    private final LegacySceneAnimation animation = new LegacySceneAnimation();
    /** Whether a native layer requires the whole node to use vanilla rendering. */
    private boolean unsupported;
    /** Consumer used to discard unsupported primitives before falling back to vanilla rendering. */
    private final SceneBlockVertexConsumer skipped = new SceneBlockVertexConsumer();

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
     * Captures a vanilla model in local coordinates for material, light and depth passes.
     *
     * @param node native model node
     * @param partialTick fractional client tick
     * @return quad meshes, or null if the node needs a native special pass
     */
    List<GlScenePlatform.Surface> capture(SceneNode node, float partialTick) {
        if (captures.containsKey(node)) return captures.get(node);
        Minecraft client = Minecraft.getInstance();
        if (node instanceof ItemNode item) {
            var stack = MinecraftScene.renderStack(item);
            if (MinecraftRendererPlatformHook.hasCustomItemRendererImpl(stack)
                    || client.getItemRenderer().getModel(stack, client.level, null, 0).isCustomRenderer()) {
                captures.put(node, null);
                return null;
            }
        }
        var dispatcher = client.getEntityRenderDispatcher();
        var original = dispatcher.cameraOrientation();
        transform.identity();
        capturing = true;
        try {
            dispatcher.overrideCameraOrientation(billboard);
            submitModel(client, node, partialTick);
            var result = unsupported ? null : bakeLayers();
            captures.put(node, result);
            return result;
        } finally {
            dispatcher.overrideCameraOrientation(original);
            capturing = unsupported = false;
            capturedLayers.clear();
            skipped.clear();
        }
    }

    /**
     * Converts the layer's normal, texture and alpha information to a scene surface. The render type
     * resolves the same atlas or texture used by the vanilla model, including resource pack overrides.
     *
     * @return indexed surfaces, or null when any nonempty layer cannot be captured
     */
    private List<GlScenePlatform.Surface> bakeLayers() {
        var result = new ArrayList<GlScenePlatform.Surface>(capturedLayers.size());
        var shader = RenderSystem.getShader();
        int texture = RenderSystem.getShaderTexture(0);
        int overlay = RenderSystem.getShaderTexture(1);
        int lightmap = RenderSystem.getShaderTexture(2);
        var textureMatrix = RenderSystem.getTextureMatrix();
        boolean depthWrite = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        boolean cullEnabled = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        try {
            for (var entry : capturedLayers.entrySet()) {
                var mesh = entry.getValue().build();
                if (mesh == null) continue;
                var type = entry.getKey();
                RenderSystem.setShaderTexture(0, 0);
                // Vanilla's default write mask and cull shards assume these states are already enabled.
                RenderSystem.depthMask(true);
                RenderSystem.enableCull();
                type.setupRenderState();
                try {
                    var active = RenderSystem.getShader();
                    int id = RenderSystem.getShaderTexture(0);
                    if (active == null || id == 0) return null;
                    String name = active.getName();
                    boolean foil = name.contains("glint");
                    var matrix = RenderSystem.getTextureMatrix();
                    var glint = foil ? new GlScenePlatform.Glint(
                            matrix.m00(), matrix.m10(), matrix.m30(), matrix.m01(), matrix.m11(), matrix.m31()) : null;
                    boolean blend = foil || ScenePoseCompat.sortsOnUpload(type)
                            || name.contains("translucent") || name.contains("eyes");
                    var alpha = blend ? Material.AlphaMode.TRANSLUCENT : Material.AlphaMode.CUTOUT;
                    var base = new SimpleTextureHandle("unigui:scene_native/" + id, 1, 1, id)
                            .withOptions(TextureOptions.nearest());
                    var shading = foil || name.contains("emissive") || name.contains("eyes")
                            ? Material.Shading.UNLIT : Material.Shading.LIT;
                    var cull = GL11.glIsEnabled(GL11.GL_CULL_FACE) ? Material.Culling.BACK : Material.Culling.NONE;
                    var material = new Material(0xffffffff, base, shading, alpha, 0.1f, cull);
                    boolean translucentCutout = blend && usesTranslucentCutout(name);
                    boolean translucentDepthWrite = blend && !foil && GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
                    result.add(new GlScenePlatform.Surface(mesh, material, glint, translucentDepthWrite, translucentCutout));
                } finally {
                    type.clearRenderState();
                }
            }
            return result;
        } finally {
            RenderSystem.setShaderTexture(0, texture);
            RenderSystem.setShaderTexture(1, overlay);
            RenderSystem.setShaderTexture(2, lightmap);
            RenderSystem.setTextureMatrix(textureMatrix);
            RenderSystem.setShader(() -> shader);
            RenderSystem.depthMask(depthWrite);
            if (cullEnabled) RenderSystem.enableCull();
            else RenderSystem.disableCull();
        }
    }

    /**
     * Identifies vanilla blended model shaders with a 0.1 alpha cutoff. The names match both
     * namespaced and plain shader IDs used by the legacy Minecraft loaders.
     *
     * @param shaderName name of the vanilla shader selected by the render type
     * @return whether to apply the material cutoff before writing blended color and depth
     */
    static boolean usesTranslucentCutout(String shaderName) {
        int separator = shaderName.indexOf(':');
        String path = separator == -1 ? shaderName : shaderName.substring(separator + 1);
        return switch (path) {
            case "rendertype_item_entity_translucent_cull",
                 "rendertype_entity_translucent_cull",
                 "rendertype_entity_translucent",
                 "rendertype_entity_translucent_emissive" -> true;
            default -> false;
        };
    }

    /**
     * Retires meshes unique to this frame after every render pass consumes them.
     *
     * @param retire removes each temporary mesh from the renderer's GPU cache
     */
    void endFrame(Consumer<Mesh3D> retire) {
        for (var surfaces : captures.values()) {
            if (surfaces == null) continue;
            for (var surface : surfaces) retire.accept(surface.mesh());
        }
        captures.clear();
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
        submitModel(client, node, partialTick);
    }

    /**
     * Submits the model in the configured coordinate space.
     *
     * @param client current client
     * @param node model node
     * @param partialTick fractional client tick
     */
    private void submitModel(Minecraft client, SceneNode node, float partialTick) {
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
            var model = MinecraftScene.entity(entity);
            if (model == null) return;
            boolean animated = entity.hasAnimation() || entity.hasHead() || entity.hasNativeAnimation() || entity.hasAction();
            float sampledTick = animated ? animation.apply(model, entity, partialTick) : partialTick;
            try {
                resetPose();
                var renderer = client.getEntityRenderDispatcher().getRenderer(model);
                var offset = renderer.getRenderOffset(model, sampledTick);
                pose.translate(offset.x, offset.y, offset.z);
                renderer.render(model, Mth.rotLerp(sampledTick, model.yRotO, model.getYRot()),
                        sampledTick, pose, this, LightTexture.FULL_BRIGHT);
            } finally {
                if (animated) animation.restore(model);
            }
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
        if (capturing) {
            if (type.mode() == VertexFormat.Mode.QUADS) {
                return capturedLayers.computeIfAbsent(type, ignored -> new SceneBlockVertexConsumer());
            }
            unsupported = true;
            return skipped;
        }
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
