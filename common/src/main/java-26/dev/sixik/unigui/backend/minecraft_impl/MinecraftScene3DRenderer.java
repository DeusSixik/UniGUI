package dev.sixik.unigui.backend.minecraft_impl;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import dev.sixik.unigui.api.scene.BlockNode;
import dev.sixik.unigui.api.scene.EntityNode;
import dev.sixik.unigui.api.scene.ItemNode;
import dev.sixik.unigui.api.scene.StructureNode;
import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import dev.sixik.unigui.api.backend.UniGuiLogger;
import dev.sixik.unigui.api.backend.UniGuiRuntime;
import dev.sixik.unigui.api.math.Aabb;
import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.math.Frustum3D;
import dev.sixik.unigui.api.math.Mat4View;
import dev.sixik.unigui.api.math.MutableMat4;
import dev.sixik.unigui.api.math.MutableVec3;
import dev.sixik.unigui.api.render.RenderTarget;
import dev.sixik.unigui.api.render.RenderTargetOptions;
import dev.sixik.unigui.api.render.Scene3DRenderer;
import dev.sixik.unigui.api.scene.Camera3D;
import dev.sixik.unigui.api.scene.DirectionalShadows;
import dev.sixik.unigui.api.scene.FogVolume;
import dev.sixik.unigui.api.scene.Material;
import dev.sixik.unigui.api.scene.Mesh3D;
import dev.sixik.unigui.api.scene.Scene;
import dev.sixik.unigui.api.scene.SceneMaterialEffect;
import dev.sixik.unigui.api.scene.SceneNode;
import dev.sixik.unigui.api.scene.SceneWireframe;
import dev.sixik.unigui.impl.render.SceneMeshData;
import dev.sixik.unigui.impl.render.SceneShadowProjection;
import it.unimi.dsi.fastutil.objects.ObjectArrays;
import net.minecraft.core.BlockPos;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Рендер индексированных мешей в цвет и глубину offscreen-цели Minecraft.
 */
final class MinecraftScene3DRenderer implements Scene3DRenderer, AutoCloseable {
    /**
     * Журнал рендер-бэкенда.
     */
    private static final UniGuiLogger LOGGER = UniGuiRuntime.logger(MinecraftScene3DRenderer.class);
    /**
     * Имя прохода, передаваемое GPU API.
     */
    private static final Supplier<String> PASS_NAME = () -> "UniGUI 3D scene";
    /**
     * GPU debug label of the directional light depth pass.
     */
    private static final Supplier<String> SHADOW_PASS_NAME = () -> "UniGUI scene shadow map";
    /**
     * Offscreen attachments for the directional map. Only depth is sampled.
     */
    private static final RenderTargetOptions SHADOW_TARGET = new RenderTargetOptions(true, false, "scene_shadow");
    /**
     * Unused color attachment starts transparent.
     */
    private static final Vector4f SHADOW_CLEAR = new Vector4f();
    /**
     * Мягкий предел памяти буферов вне активной сцены, в байтах.
     */
    private static final long CACHE_BYTES = 64L * 1024 * 1024;
    /**
     * Мягкий предел числа закэшированных мешей.
     */
    private static final int CACHE_MESHES = 1024;
    /**
     * Слои идут в порядке solid, cutout, translucent. Прозрачные меши идут от дальних к ближним.
     */
    private static final Comparator<Draw> DRAW_ORDER = Comparator
            .comparingInt((Draw draw) -> draw.material.alphaMode().ordinal())
            .thenComparingDouble(draw -> draw.depth)
            .thenComparingInt(draw -> draw.order);

    /**
     * Переиспользует существующий кэш разрешения текстур бэкенда.
     */
    private final UiTextureBinding.Cache textures;
    /**
     * Local lights and their GPU resources, allocated only on a supporting backend.
     */
    private final SceneLocalLights localLights;
    /**
     * Optional atmospheric pass, with GPU storage allocated when a volume is visible.
     */
    private final SceneFogPass fog;
    /**
     * Буферы неизменяемых мешей, ключом служит идентичность экземпляра Mesh3D.
     */
    private final LinkedHashMap<Mesh3D, GpuMesh> meshes = new LinkedHashMap<>(32, 0.75f, true);
    /**
     * Local native model poses shared between the light and color passes of one frame.
     */
    private final IdentityHashMap<SceneNode, List<SceneBlockMeshes.Surface>> nativeSurfaces = new IdentityHashMap<>();
    /**
     * Retires replaced native geometry through the same fence as ordinary mesh evictions.
     */
    private final Consumer<Mesh3D> retireNativeMesh = this::retireMesh;
    /**
     * Кэш ванильных моделей и запечённых секций.
     */
    private final SceneBlockMeshes blockMeshes = new SceneBlockMeshes();
    /**
     * Alternate bake cache with neutral cardinal shading, created on demand.
     */
    private SceneBlockMeshes relitBlocks;
    /**
     * Block cache selected for both passes of the current scene.
     */
    private SceneBlockMeshes activeBlocks;
    /**
     * Reused projection builder and world-to-light clip matrix.
     */
    private final SceneShadowProjection shadowProjection = new SceneShadowProjection();
    /**
     * Matrix used for light-space culling and receiver depth.
     */
    private final MutableMat4 shadowClip = new MutableMat4();
    /**
     * Cached GPU attachments owned by this renderer.
     */
    private MinecraftRenderTarget shadowTarget;
    /**
     * Shadow map settings for the current scene, or null without a populated map.
     */
    private DirectionalShadows frameShadows;
    /**
     * Whether collection and drawing are for the light's depth pass.
     */
    private boolean drawingShadows;
    /**
     * Обходчик, выделяемый один раз на бэкенд.
     */
    private final Consumer<SceneNode> collect = this::collectNode;
    /**
     * Объём видимости в мировых координатах.
     */
    private final Frustum3D frustum = new Frustum3D();
    /**
     * Матрица вида, используемая для сортировки прозрачных мешей.
     */
    private final MutableMat4 view = new MutableMat4();
    /**
     * Матрица из мира в clip space.
     */
    private final MutableMat4 viewProjection = new MutableMat4();
    /**
     * Color view-projection matrices for the positive and negative native layering steps.
     */
    private final MutableMat4[] layeredProjection = {new MutableMat4(), new MutableMat4()};
    /**
     * Mask of layered projections computed for the active render.
     */
    private int preparedLayers;
    /**
     * Temporary camera-space transform used to reproduce vanilla layer depth offsets.
     */
    private final Matrix4f vanillaLayerTransform = new Matrix4f();
    /**
     * Views the native column-first indexed matrix in UniGUI's row-first accessor convention.
     */
    private final Mat4View layerTransform = (row, column) -> vanillaLayerTransform.get(column, row);
    /**
     * Camera-space layering step of depth-tested wireframe edges: perspective scales camera space by
     * {@code 1 - step / 4096}, orthographic moves it {@code step / 512} toward the eye, so edges win the depth
     * test against their own surface on both OpenGL and Vulkan without a rasterizer depth bias.
     */
    private static final int WIRE_DEPTH_STEP = 4;
    /**
     * Wireframe settings of the active render, or null when the scene draws plain surfaces.
     */
    private SceneWireframe wire;
    /**
     * World-to-clip projection of depth-tested edges, layered toward the eye by {@link #WIRE_DEPTH_STEP}.
     */
    private final MutableMat4 wireProjection = new MutableMat4();
    /**
     * Whether {@link #wireProjection} matches the active camera.
     */
    private boolean wireProjectionReady;
    /**
     * Flat unlit edge materials by ARGB color, reused between frames.
     */
    private final HashMap<Integer, Material> wireMaterials = new HashMap<>();
    /**
     * Рабочая матрица clip-преобразования текущего меша.
     */
    private final MutableMat4 clip = new MutableMat4();
    /**
     * Мировая матрица, общая для поверхностей текущего узла.
     */
    private final MutableMat4 nodeWorld = new MutableMat4();
    /**
     * Положение перспективной камеры для выбора дальних моделей.
     */
    private final MutableVec3 eye = new MutableVec3();
    /**
     * Цвет очистки с premultiplied alpha.
     */
    private final Vector4f clear = new Vector4f();
    /**
     * Варианты пайплайна по текстуре, прозрачности и инстансированию.
     */
    private final RenderPipeline[] pipelines = new RenderPipeline[6];

    /**
     * Рабочие элементы текущего прохода. Массив растёт только при увеличении числа видимых мешей.
     */
    private Draw[] draws = new Draw[16];
    /**
     * Число заполненных элементов.
     */
    private int drawCount;
    /**
     * Число групп после объединения соседних одинаковых мешей.
     */
    private int drawCalls;
    /**
     * Нужна ли сортировка слоёв или прозрачных мешей.
     */
    private boolean needsSort;
    /**
     * Буфер подготовки всех блоков мешей перед открытием прохода.
     */
    private ByteBuffer uniformBytes;
    /**
     * Суммарный размер загруженных вершин и индексов в байтах.
     */
    private long meshBytes;
    /**
     * Возрастающий номер прохода для отметки активных мешей.
     */
    private long passSequence;
    /**
     * Число загрузок новых мешей для диагностики захвата кадра.
     */
    private long uploads;
    /**
     * Выводилось ли сообщение о первом успешном проходе.
     */
    private boolean announced;
    /**
     * Ванильный проход, создаётся при отрисовке первой модели сущности, предмета или block entity.
     */
    private SceneVanillaFeatures vanilla;
    /**
     * Камера текущего прохода для инициализации ванильной стадии.
     */
    private Camera3D activeCamera;
    /**
     * Доля тика текущего прохода.
     */
    private float activePartialTick;
    /**
     * Успела ли ванильная стадия получить камеру текущего прохода.
     */
    private boolean vanillaBegun;
    /**
     * Whether the scene asks for its own lights on Minecraft content this frame. Items and entities then go through
     * the lit surface pipeline even without local lights, so a scene with only a dim ambient also darkens them
     * instead of keeping vanilla lighting.
     */
    private boolean relightNative;

    /**
     * Создаёт рендерер без GPU-аллокаций. Буферы выделяются по мере использования.
     *
     * @param textures кэш разрешённых текстур владельца.
     */
    MinecraftScene3DRenderer(UiTextureBinding.Cache textures) {
        this.textures = textures;
        localLights = UiMesh3DPipeline.SUPPORTS_SURFACE_EFFECTS ? new SceneLocalLights(textures) : null;
        fog = UiMesh3DPipeline.createFogPass();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void render(RenderTarget target, Scene scene, Camera3D camera, float viewportWidth, float viewportHeight,
                       ColorView clearColor, float partialTick) {
        render(target, scene, camera, viewportWidth, viewportHeight, clearColor, partialTick, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void render(RenderTarget target, Scene scene, Camera3D camera, float viewportWidth, float viewportHeight,
                       ColorView clearColor, float partialTick, Highlight highlight) {
        if (!(target instanceof MinecraftRenderTarget destination) || !destination.options().useDepth()
                || destination.options().clearBeforeRender()) {
            throw new IllegalArgumentException("UniGUI scene requires a Minecraft depth target with no 2D auto-clear");
        }
        UiGpu.Target output = destination.gpuTarget();
        if (output.depth() == null) throw new IllegalStateException("UniGUI scene target is missing its depth texture");

        passSequence++;
        preparedLayers = 0;
        wire = scene.wireframe();
        wireProjectionReady = false;
        activeCamera = camera;
        activePartialTick = partialTick;
        activeBlocks = blockMeshes;
        relightNative = UiMesh3DPipeline.SUPPORTS_SURFACE_EFFECTS && scene.minecraftBlockLighting();
        if (relightNative) {
            if (relitBlocks == null) relitBlocks = new SceneBlockMeshes(true);
            activeBlocks = relitBlocks;
        }
        camera.position(eye);
        camera.view(view);
        float alpha = clampColor(clearColor.a());
        clear.set(clampColor(clearColor.r()) * alpha, clampColor(clearColor.g()) * alpha,
                clampColor(clearColor.b()) * alpha, alpha);
        try {
            if (localLights != null) {
                localLights.begin(scene, output);
                renderDirectionalShadows(scene);
                for (int i = 0; i < localLights.count(); i++) {
                    var settings = localLights.shadowSettings(i);
                    if (settings != null && renderShadowMap(scene, localLights.shadowClip(i), settings.resolution(), true)) {
                        localLights.markShadowRendered(i);
                    }
                }
                localLights.upload();
            }
            frustum.set(camera.viewProjection(viewProjection));
            scene.visitVisible(frustum, collect);
            if (highlight != null)
                collectMesh(highlight.mesh(), highlight.material(), nodeWorld.set(highlight.world()), null);
            if (needsSort) ObjectArrays.quickSort(draws, 0, drawCount, DRAW_ORDER);
            int wireStart = wire == null ? drawCount : appendWireDraws();
            int alignment = Math.max(16, MinecraftGpuCompat.uniformAlignment());
            GpuBufferSlice uniforms = drawCount == 0 ? null
                    : prepare(scene, output, viewportWidth / output.width(), viewportHeight / output.height(), alignment);
            int pixelsWide = Math.clamp((int) Math.ceil(viewportWidth), 1, output.width());
            int pixelsHigh = Math.clamp((int) Math.ceil(viewportHeight), 1, output.height());
            boolean nativeModels = vanillaBegun && vanilla.hasSubmissions();
            int opaqueEnd = wireStart;
            if (nativeModels) {
                while (opaqueEnd > 0 && draws[opaqueEnd - 1].translucent) opaqueEnd--;
            }
            try (RenderPass pass = MinecraftGpuCompat.openScenePass(PASS_NAME, output, clear)) {
                clipToViewport(pass, output, pixelsWide, pixelsHigh);
                if (opaqueEnd > 0) draw(pass, uniforms, 0, opaqueEnd);
            }
            if (nativeModels) {
                vanilla.render(output, viewportWidth, viewportHeight, camera.orthographic());
                if (opaqueEnd < wireStart) {
                    try (RenderPass pass = MinecraftGpuCompat.openPass(PASS_NAME, output.color(), output.depth())) {
                        clipToViewport(pass, output, pixelsWide, pixelsHigh);
                        draw(pass, uniforms, opaqueEnd, wireStart);
                    }
                }
            }
            if (MinecraftClientCompat.NATIVE_SCENE_PARTICLES && vanillaBegun) {
                vanilla.renderSoftParticles(output, camera, viewportWidth, viewportHeight);
            }
            if (fog != null && !edgesOnly()) fog.render(output, viewProjection, camera, scene.materialTime(),
                    localLights, viewportWidth, viewportHeight);
            if (wireStart < drawCount) {
                try (RenderPass pass = MinecraftGpuCompat.openPass(PASS_NAME, output.color(), output.depth())) {
                    clipToViewport(pass, output, pixelsWide, pixelsHigh);
                    draw(pass, uniforms, wireStart, drawCount);
                }
            }
            if (!announced && drawCount > 0) {
                LOGGER.info("UniGUI scene mesh RenderPipeline drew {} meshes", drawCount);
                announced = true;
            }
            UiDebugCapture.onSceneRendered(drawCount, drawCalls, uploads, meshes.size());
        } finally {
            clearDraws();
            nativeSurfaces.clear();
            drawCalls = 0;
            if (vanillaBegun) vanilla.reset();
            vanillaBegun = false;
            activeCamera = null;
            activeBlocks = null;
            wire = null;
            frameShadows = null;
            if (localLights != null) localLights.reset();
            if (fog != null) fog.reset();
            Arrays.fill(pipelines, null);
            if (UiMesh3DPipeline.SUPPORTS_SURFACE_EFFECTS) UiMesh3DPipeline.trimSurfaces();
        }
        trimMeshes();
    }

    /**
     * Produces the directional map when a scene has an enabled directional light and shadow volume.
     *
     * @param scene scene owning the directional light and candidate geometry
     */
    private void renderDirectionalShadows(Scene scene) {
        DirectionalShadows settings = scene.directionalShadows();
        var light = scene.directionalLight();
        if (settings == null || settings.strength() == 0 || light == null || light.intensity() == 0) return;
        shadowProjection.update(light, settings, shadowClip);
        if (renderShadowMap(scene, shadowClip, settings.resolution(), false)) frameShadows = settings;
    }

    /**
     * Collects opaque and cutout casters in a light volume, including casters outside the view camera.
     *
     * @param scene      scene containing the candidate casters
     * @param lightClip  world-to-light clip transform in canonical depth
     * @param resolution side of the square map in texels
     * @param spot       true for the next spotlight slot, false for the directional map
     * @return whether a map was populated
     */
    private boolean renderShadowMap(Scene scene, MutableMat4 lightClip, int resolution, boolean spot) {
        frustum.set(lightClip);
        viewProjection.set(lightClip);
        drawingShadows = true;
        try {
            scene.visitVisible(frustum, collect);
            if (drawCount == 0) return false;
            UiGpu.Target target = (spot ? localLights.shadowTarget(resolution) : directionalTarget(resolution)).gpuTarget();
            int alignment = Math.max(16, MinecraftGpuCompat.uniformAlignment());
            GpuBufferSlice uniforms = prepare(scene, target, 1, 1, alignment);
            try (RenderPass pass = MinecraftGpuCompat.openScenePass(SHADOW_PASS_NAME, target, SHADOW_CLEAR)) {
                draw(pass, uniforms, 0, drawCount);
            }
            return true;
        } finally {
            drawingShadows = false;
            clearDraws();
        }
    }

    /**
     * Resolves the persistent directional shadow target.
     *
     * @param resolution side of the square map in texels
     * @return target with a depth attachment
     */
    private MinecraftRenderTarget directionalTarget(int resolution) {
        if (shadowTarget == null || shadowTarget.width() != resolution) {
            var previous = shadowTarget;
            shadowTarget = new MinecraftRenderTarget(resolution, resolution, SHADOW_TARGET);
            if (previous != null) RenderSystem.queueFencedTask(previous::close);
        }
        return shadowTarget;
    }

    /**
     * Releases references held by reusable draw commands between the depth and color passes.
     */
    private void clearDraws() {
        for (int i = 0; i < drawCount; i++) draws[i].releaseReferences();
        drawCount = 0;
        needsSort = false;
    }

    /**
     * Добавляет меш видимого узла в рабочий список.
     *
     * @param node узел с мировым преобразованием и материалом.
     */
    private void collectNode(SceneNode node) {
        if (fog != null && !drawingShadows && !edgesOnly() && node instanceof FogVolume volume) {
            fog.collect(volume, frustum);
        }
        if (MinecraftClientCompat.NATIVE_SCENE_PARTICLES && !drawingShadows && !edgesOnly()
                && node instanceof MinecraftParticleEmitter emitter) vanilla().submit(emitter, view);
        if (drawingShadows && !node.castsShadow()) return;
        int first = drawCount;
        boolean needBounds = UiMesh3DPipeline.SUPPORTS_SURFACE_EFFECTS && node.materialEffect() != null;
        Aabb bounds = collectNodeGeometry(node, needBounds);
        if (bounds != null) {
            for (int i = first; i < drawCount; i++) draws[i].effectBounds = bounds;
        }
    }

    /**
     * Decides whether native item or entity geometry must be converted to scene meshes for this pass.
     *
     * <p>Native conversion is required for a custom material effect and for both passes of a directional
     * shadow render. The latter keeps a model on the scene surface pipeline after its effect ends or when
     * the node uses its ordinary material, so its color pass can sample the scene shadow map.</p>
     *
     * @param surfaceEffects whether this Minecraft backend supports native surface conversion
     * @param hasEffect      whether the node has a custom material effect
     * @param shadowPass     whether the current collection writes the directional shadow map
     * @param shadowsActive  whether the node participates in directional shadows in the color pass
     * @return true when native layers must be captured as indexed scene geometry
     */
    static boolean shouldCaptureNative(boolean surfaceEffects, boolean hasEffect,
                                       boolean shadowPass, boolean shadowsActive) {
        return surfaceEffects && (hasEffect || shadowPass || shadowsActive);
    }

    /**
     * Collects a node's geometry and computes effect bounds independently of per-pass frustum and alpha tests.
     *
     * @param node       visible scene node
     * @param needBounds whether the shader will read node-local bounds
     * @return local effect bounds, or null when no effect or geometry supplies bounds
     */
    private Aabb collectNodeGeometry(SceneNode node, boolean needBounds) {
        Mesh3D mesh = node.mesh();
        Aabb bounds = needBounds && mesh != null ? mesh.bounds() : null;
        if (mesh != null) collectMesh(mesh, node.material(), node.worldMatrix(nodeWorld), node);
        if (node instanceof BlockNode block) {
            BlockState state = MinecraftScene.state(block);
            if (state == null) return bounds;
            block.worldMatrix(nodeWorld);
            var surfaces = activeBlocks.single(state);
            collectSurfaces(surfaces, nodeWorld, node);
            if (!drawingShadows) {
                BlockEntity blockEntity = MinecraftScene.blockEntity(block);
                if (blockEntity != null) vanilla().submit(blockEntity, BlockPos.ZERO, nodeWorld);
            }
            return needBounds ? SceneBlockMeshes.surfaceBounds(surfaces, bounds) : null;
        } else if (node instanceof StructureNode structure && structure.platform() == MinecraftScene.PLATFORM) {
            bounds = needBounds ? structure.bounds() : null;
            structure.worldMatrix(nodeWorld);
            StructureNode.DistantMesh distant = drawingShadows ? null
                    : structure.distantMesh(activeCamera.orthographic() ? null : eye, nodeWorld);
            if (distant != null) {
                collectMesh(distant.mesh(), distant.material(), nodeWorld, node);
                return bounds;
            }
            int topY = structure.topVisibleY();
            for (StructureNode.Section section : structure.sections()) {
                int bottom = section.minBlockY();
                if (bottom > topY) continue;
                collectSurfaces(activeBlocks.section(structure, section), nodeWorld, node);
                if (drawingShadows || section.blockEntityCount() == 0) continue;
                for (int index = 0; index < StructureNode.Section.VOLUME; index++) {
                    if (section.blockEntity(index) instanceof BlockEntity blockEntity
                            && bottom + StructureNode.Section.localY(index) <= topY) {
                        vanilla().submit(blockEntity, blockEntity.getBlockPos(), nodeWorld);
                    }
                }
            }
        } else if (node instanceof EntityNode || node instanceof ItemNode) {
            return collectNative(node, bounds, needBounds);
        }
        return bounds;
    }

    /**
     * Routes a native model with material effects or local lighting through the scene mesh pipeline.
     * Unsupported render submissions use the complete vanilla model in the color pass.
     *
     * @param node       item or entity to render
     * @param bounds     optional bounds of a custom mesh attached to the node
     * @param needBounds whether the shader will read the model's local bounds
     * @return bound of all captured layers and custom geometry, or the custom mesh bound alone
     */
    private Aabb collectNative(SceneNode node, Aabb bounds, boolean needBounds) {
        boolean usesShadows = frameShadows != null && (node.castsShadow() || node.receivesShadow());
        boolean usesLocalLight = localLights != null && localLights.count() > 0;
        if (shouldCaptureNative(UiMesh3DPipeline.SUPPORTS_SURFACE_EFFECTS, needBounds, drawingShadows,
                usesShadows || usesLocalLight || relightNative || wire != null)) {
            List<SceneBlockMeshes.Surface> surfaces = nativeSurfaces.get(node);
            if (surfaces == null) {
                surfaces = vanilla().bake(node, retireNativeMesh);
                nativeSurfaces.put(node, surfaces);
            }
            if (!surfaces.isEmpty()) {
                collectSurfaces(surfaces, node.worldMatrix(nodeWorld), node);
                return needBounds ? SceneBlockMeshes.surfaceBounds(surfaces, bounds) : bounds;
            }
        }
        // Edges-only mode hides a model whose layers could be captured; one without edges stays visible.
        if (drawingShadows || edgesOnly() && UiMesh3DPipeline.SUPPORTS_SURFACE_EFFECTS) return bounds;
        node.worldMatrix(nodeWorld);
        if (node instanceof EntityNode entity) vanilla().submit(entity, nodeWorld);
        else vanilla().submit((ItemNode) node, nodeWorld);
        return bounds;
    }

    /**
     * Drops the GPU entry for a replaced native mesh after its last submitted pass finishes.
     *
     * @param mesh replaced geometry, which may have been evicted already
     */
    private void retireMesh(Mesh3D mesh) {
        GpuMesh gpu = meshes.remove(mesh);
        if (gpu == null) return;
        meshBytes -= gpu.bytes;
        RenderSystem.queueFencedTask(gpu::close);
    }

    /**
     * Создаёт ванильный проход только для сцен с соответствующими узлами.
     *
     * @return ванильный проход с камерой текущего кадра.
     */
    private SceneVanillaFeatures vanilla() {
        if (vanilla == null) vanilla = new SceneVanillaFeatures();
        if (!vanillaBegun) {
            vanilla.begin(activeCamera, view, activePartialTick);
            vanillaBegun = true;
        }
        return vanilla;
    }

    /**
     * Устанавливает отсечение для неполной области цели.
     *
     * @param pass   текущий проход.
     * @param output цель сцены.
     * @param width  ширина области в пикселях.
     * @param height высота области в пикселях.
     */
    private static void clipToViewport(RenderPass pass, UiGpu.Target output, int width, int height) {
        if (width != output.width() || height != output.height()) {
            pass.enableScissor(0, output.height() - height, width, height);
        }
    }

    /**
     * Отбирает запечённые поверхности по собственным границам.
     *
     * @param surfaces слои ванильной геометрии.
     * @param world    мировое преобразование их узла.
     * @param node     owner of the block surfaces and their scene effect settings
     */
    private void collectSurfaces(List<SceneBlockMeshes.Surface> surfaces, MutableMat4 world, SceneNode node) {
        for (int i = 0; i < surfaces.size(); i++) {
            SceneBlockMeshes.Surface surface = surfaces.get(i);
            SceneNativeLayer layer = surface.nativeLayer();
            if ((!drawingShadows || (surface.material().alphaMode() != Material.AlphaMode.TRANSLUCENT
                    && (layer == null || !layer.style().depthEqual)))
                    && frustum.intersects(surface.mesh().bounds(), world)) {
                collectMesh(surface.mesh(), surface.material(), world, node);
                Draw draw = draws[drawCount - 1];
                draw.nativeLayer = layer;
                if (layer != null && !layer.style().lit) draw.shadowReceiver = false;
            }
        }
    }

    /**
     * Записывает параметры видимого меша для отрисовки.
     *
     * @param mesh     локальная геометрия.
     * @param material поверхность меша.
     * @param world    мировое преобразование меша.
     * @param node     source of surface settings, or null for the editor highlight
     */
    private void collectMesh(Mesh3D mesh, Material material, MutableMat4 world, SceneNode node) {
        boolean translucent = material.alphaMode() == Material.AlphaMode.TRANSLUCENT;
        if (drawingShadows && translucent) return;
        Draw draw = nextDraw();
        draw.node = node;
        draw.wire = false;
        draw.mesh = mesh;
        draw.material = material;
        draw.world.set(world);
        draw.translucent = translucent;
        draw.effect = UiMesh3DPipeline.SUPPORTS_SURFACE_EFFECTS && node != null ? node.materialEffect() : null;
        draw.effectNode = draw.effect == null ? null : node;
        draw.receiveSpotShadows = !drawingShadows && node != null && node.receivesShadow();
        draw.shadowReceiver = frameShadows != null && draw.receiveSpotShadows;
        draw.localLighting = !drawingShadows && localLights != null && localLights.count() > 0
                && material.shading() == Material.Shading.LIT;
        draw.depth = draw.translucent ? viewDepth(mesh.bounds(), draw.world) : 0;
        needsSort |= material.alphaMode() != Material.AlphaMode.OPAQUE;
    }

    /**
     * Reserves the next reusable draw command and numbers it in tree order.
     *
     * @return command to fill
     */
    private Draw nextDraw() {
        if (drawCount == draws.length) draws = Arrays.copyOf(draws, draws.length * 2);
        Draw draw = draws[drawCount];
        if (draw == null) draws[drawCount] = draw = new Draw();
        draw.order = drawCount++;
        return draw;
    }

    /**
     * @return whether the active wireframe replaces surfaces with edges alone
     */
    private boolean edgesOnly() {
        return wire != null && !wire.shaded();
    }

    /**
     * Appends one edge draw per collected node surface after the sorted surfaces. Glint overlays and depth-equal
     * native layers repeat a base surface, so they add no edges. In edges-only mode the node surfaces are then
     * dropped and only the editor highlight keeps its filled surface.
     *
     * @return index of the first edge draw
     */
    private int appendWireDraws() {
        int surfaces = drawCount;
        for (int i = 0; i < surfaces; i++) {
            Draw source = draws[i];
            SceneNativeLayer layer = source.nativeLayer;
            if (source.node == null || layer != null && (layer.style().depthEqual || layer.style().glintOverlay())) {
                continue;
            }
            Draw edge = nextDraw();
            source = draws[i];
            edge.node = source.node;
            edge.wire = true;
            edge.mesh = source.mesh;
            edge.world.set(source.world);
            edge.material = wireMaterials.computeIfAbsent(wire.colorFor(source.node), argb -> new Material(argb,
                    null, Material.Shading.UNLIT, Material.AlphaMode.OPAQUE, 0, Material.Culling.NONE));
            edge.translucent = false;
            edge.effect = null;
            edge.effectNode = null;
            edge.effectBounds = null;
            edge.receiveSpotShadows = false;
            edge.shadowReceiver = false;
            edge.localLighting = false;
            edge.nativeLayer = null;
            edge.depth = 0;
        }
        if (wireMaterials.size() > 64) wireMaterials.clear();
        if (wire.shaded()) return surfaces;
        int kept = 0;
        int highlights = 0;
        for (int i = 0; i < drawCount; i++) {
            Draw draw = draws[i];
            if (draw.node != null && !draw.wire) continue;
            if (!draw.wire) highlights++;
            draws[i] = draws[kept];
            draws[kept++] = draw;
        }
        for (int i = kept; i < drawCount; i++) draws[i].releaseReferences();
        drawCount = kept;
        return highlights;
    }

    /**
     * Pulls depth-tested edges toward the eye with Minecraft's camera-space layering, see {@link #WIRE_DEPTH_STEP}.
     *
     * @return world-to-clip projection of the edge pass
     */
    private MutableMat4 wireProjection() {
        if (!wireProjectionReady) {
            ProjectionType kind = activeCamera.orthographic() ? ProjectionType.ORTHOGRAPHIC : ProjectionType.PERSPECTIVE;
            kind.applyLayeringTransform(vanillaLayerTransform.identity(), WIRE_DEPTH_STEP);
            activeCamera.projection(wireProjection);
            wireProjection.setProduct(wireProjection, layerTransform).setProduct(wireProjection, view);
            wireProjectionReady = true;
        }
        return wireProjection;
    }

    /**
     * Считает глубину центра локального AABB в системе камеры. Более отрицательное значение дальше.
     *
     * @param bounds локальные границы меша.
     * @param world  преобразование меша.
     * @return координата Z центра в системе камеры.
     */
    private double viewDepth(Aabb bounds, MutableMat4 world) {
        double x = ((double) bounds.minX() + bounds.maxX()) * 0.5;
        double y = ((double) bounds.minY() + bounds.maxY()) * 0.5;
        double z = ((double) bounds.minZ() + bounds.maxZ()) * 0.5;
        return view.transformComponent(2, world.transformComponent(0, x, y, z, 1),
                world.transformComponent(1, x, y, z, 1), world.transformComponent(2, x, y, z, 1), 1);
    }

    /**
     * Разрешает буферы, текстуры и пайплайны. Загружает все uniform-блоки одной записью
     * перед открытием прохода, сохраняя выравнивание каждого среза.
     *
     * @param scene     источник света.
     * @param output    цель, которую в этом проходе нельзя семплировать.
     * @param viewportX доля ширины, занимаемая viewport.
     * @param viewportY доля высоты, занимаемая viewport.
     * @param alignment требуемое выравнивание uniform-срезов в байтах.
     * @return срез со всеми uniform-блоками сцены.
     */
    private GpuBufferSlice prepare(Scene scene, UiGpu.Target output, float viewportX, float viewportY, int alignment) {
        int size = layoutUniforms(alignment);
        if (uniformBytes == null || uniformBytes.capacity() < size) {
            int capacity = uniformBytes == null ? 4096 : uniformBytes.capacity();
            int next = (int) Math.min(Integer.MAX_VALUE, Math.max(size, 2L * capacity));
            uniformBytes = uniformBytes == null ? MemoryUtil.memAlloc(next) : MemoryUtil.memRealloc(uniformBytes, next);
        }
        uniformBytes.clear();
        uniformBytes.limit(size);
        boolean zeroToOne = MinecraftGpuCompat.zZeroToOne();
        for (int i = 0; i < drawCount; i += draws[i].instances) {
            Draw draw = draws[i];
            draw.texture = draw.nativeLayer == null ? textures.resolve(draw.material.texture()) : draw.nativeLayer.texture();
            if (draw.texture != null && draw.texture.view().texture() == output.color().texture()) draw.texture = null;
            boolean instanced = draw.instances > 1;
            draw.pipeline = preparePipeline(draw);
            draw.gpu = mesh(draw.mesh);

            clip.setProduct(projectionFor(draw), draw.world);
            uniformBytes.position(draw.uniformOffset);
            SceneMeshData.writeUniform(uniformBytes, clip, draw.world, draw.material, scene.ambientLight(),
                    scene.directionalLight(), viewportX, viewportY, zeroToOne,
                    draw.texture != null && draw.texture.flipY(), draw.texture != null && draw.texture.premultipliedAlpha());
            if (draw.surfacePipeline) {
                float seconds = scene.materialTime();
                if (draw.effectNode != null) seconds = draw.effectNode.materialEffectTime(seconds);
                writeSurfaceUniform(draw, seconds);
            }
            if (instanced) {
                uniformBytes.position(draw.instanceOffset);
                for (int index = i; index < i + draw.instances; index++) {
                    MutableMat4 world = draws[index].world;
                    SceneMeshData.writeClipMatrix(uniformBytes, clip.setProduct(viewProjection, world));
                }
            }
        }
        uniformBytes.position(0);
        return UiGpu.uploadUniform(uniformBytes);
    }

    /**
     * Reproduces Minecraft's view layering in the color pass using {@code P * L * V}, with projection P,
     * world-to-view V and camera-space transform L. Orthographic layering translates L along Z by
     * step / 512 scene units. Perspective layering scales XYZ uniformly by 1 - step / 4096.
     *
     * @param draw command with a native layering step, if any
     * @return world-to-clip projection for this layer
     */
    private MutableMat4 projectionFor(Draw draw) {
        if (draw.wire) return wire.shaded() ? wireProjection() : viewProjection;
        if (drawingShadows || draw.nativeLayer == null) return viewProjection;
        int step = draw.nativeLayer.style().depthLayer;
        if (step == 0) return viewProjection;
        int index = step > 0 ? 0 : 1;
        int mask = 1 << index;
        MutableMat4 projection = layeredProjection[index];
        if ((preparedLayers & mask) == 0) {
            ProjectionType kind = activeCamera.orthographic() ? ProjectionType.ORTHOGRAPHIC : ProjectionType.PERSPECTIVE;
            kind.applyLayeringTransform(vanillaLayerTransform.identity(), step);
            activeCamera.projection(projection);
            projection.setProduct(projection, layerTransform).setProduct(projection, view);
            preparedLayers |= mask;
        }
        return projection;
    }

    /**
     * Selects a shader. A compilation failure in user GLSL falls back to the corresponding built-in surface.
     *
     * @param draw command with resolved base texture and uniform layout
     * @return compiled pipeline descriptor
     */
    private RenderPipeline preparePipeline(Draw draw) {
        boolean textured = draw.texture != null;
        draw.surfacePipeline = false;
        if (draw.wire) return UiMesh3DPipeline.getWire(wire.shaded());
        if (draw.surfaceOffset >= 0) {
            String source = draw.effect == null ? null : draw.effect.shader().fragmentSource();
            SceneNativeLayer.Style style = draw.nativeLayer == null ? null : draw.nativeLayer.style();
            int masks = draw.localLighting ? localLights.maskCount() : 0;
            int spotMaps = draw.localLighting ? localLights.shadowCount() : 0;
            RenderPipeline surface = UiMesh3DPipeline.getSurface(textured, draw.translucent, drawingShadows,
                    draw.shadowReceiver, style, draw.localLighting, masks, spotMaps, source);
            if (surface == null && source != null) {
                surface = UiMesh3DPipeline.getSurface(textured, draw.translucent, drawingShadows,
                        draw.shadowReceiver, style, draw.localLighting, masks, spotMaps, null);
            }
            if (surface != null) {
                draw.surfacePipeline = true;
                return surface;
            }
        }
        boolean instanced = draw.instances > 1;
        int variant = (textured ? 1 : 0) + (instanced ? 4 : draw.translucent ? 2 : 0);
        if (pipelines[variant] == null)
            pipelines[variant] = UiMesh3DPipeline.get(textured, draw.translucent, instanced);
        return pipelines[variant];
    }

    /**
     * Writes the world and light-clip transforms, normalized comparison settings, time and user data.
     * The local-to-light transform is {@code lightClip * world}, using the light and node transforms of this draw.
     *
     * @param draw    command owning the allocated std140 slice
     * @param seconds timeline time in seconds
     */
    private void writeSurfaceUniform(Draw draw, float seconds) {
        uniformBytes.position(draw.surfaceOffset);
        SceneMeshData.writeClipMatrix(uniformBytes, draw.world);
        SceneMeshData.writeClipMatrix(uniformBytes,
                draw.shadowReceiver ? clip.setProduct(shadowClip, draw.world) : draw.world);
        SceneNativeLayer.Glint glint = drawingShadows || draw.nativeLayer == null
                ? SceneNativeLayer.Glint.NONE : draw.nativeLayer.style().glint;
        uniformBytes.putFloat(draw.shadowReceiver ? frameShadows.bias() : 0)
                .putFloat(draw.shadowReceiver ? frameShadows.strength() : 0).putFloat(seconds)
                .putFloat(glint == SceneNativeLayer.Glint.NONE ? 0 : UiMesh3DPipeline.glintStrength());
        SceneMaterialEffect effect = draw.effect;
        if (effect == null) {
            uniformBytes.putFloat(0).putFloat(0).putFloat(0).putFloat(0);
        } else {
            uniformBytes.putFloat(effect.x()).putFloat(effect.y()).putFloat(effect.z()).putFloat(effect.w());
        }
        UiMesh3DPipeline.writeGlintUv(uniformBytes, glint);
        SceneMeshData.writeEffectUniform(uniformBytes,
                draw.effectBounds == null ? draw.mesh.bounds() : draw.effectBounds, effect, seconds, draw.receiveSpotShadows);
    }

    /**
     * Объединяет соседние команды одного меша и материала без динамического освещения и с записью глубины. Сохраняет порядок
     * равных глубин и порядок прозрачных слоёв. Неиспользованные элементы uniform-массива могут
     * перекрывать следующий блок в общем буфере. Шейдер читает только отправленные экземпляры.
     *
     * @param alignment требуемое выравнивание начала uniform-блока в байтах.
     * @return размер буфера, включающий хвост последнего uniform-массива.
     */
    private int layoutUniforms(int alignment) {
        int end = 0;
        int size = 0;
        for (int i = 0; i < drawCount; ) {
            Draw draw = draws[i];
            // Native color layers still need the surface pipeline for glint and depth-equal overlays.
            boolean surface = draw.localLighting || draw.shadowReceiver || draw.effect != null
                    || (!drawingShadows && draw.nativeLayer != null);
            int instances = 1;
            if (!surface && !draw.wire && !draw.translucent && draw.material.shading() == Material.Shading.UNLIT) {
                int limit = Math.min(drawCount - i, UiMesh3DShaders.MAX_INSTANCES);
                while (instances < limit) {
                    Draw next = draws[i + instances];
                    if (next.mesh != draw.mesh || next.material != draw.material || next.wire
                            || next.effect != null || next.shadowReceiver || next.localLighting
                            || (!drawingShadows && next.nativeLayer != null)) break;
                    instances++;
                }
            }
            draw.instances = instances;
            draw.uniformOffset = align(end, alignment);
            end = Math.addExact(draw.uniformOffset, SceneMeshData.UNIFORM_BYTES);
            draw.surfaceOffset = -1;
            if (surface) {
                draw.surfaceOffset = align(end, alignment);
                end = Math.addExact(draw.surfaceOffset, UiMesh3DShaders.SURFACE_BYTES);
            }
            size = Math.max(end, size);
            if (instances > 1) {
                draw.instanceOffset = align(end, alignment);
                end = Math.addExact(draw.instanceOffset, instances * SceneMeshData.CLIP_BYTES);
                size = Math.max(size, Math.addExact(draw.instanceOffset, UiMesh3DShaders.INSTANCE_BYTES));
            }
            drawCalls++;
            i += instances;
        }
        return size;
    }

    /**
     * Округляет смещение вверх до кратного выравниванию.
     *
     * @param offset    исходное смещение в байтах.
     * @param alignment выравнивание в байтах, больше нуля.
     * @return выровненное смещение в байтах.
     */
    private static int align(int offset, int alignment) {
        return Math.addExact(offset, alignment - 1) / alignment * alignment;
    }

    /**
     * Выполняет подготовленные команды без записи данных в середине GPU-прохода.
     *
     * @param pass     текущий проход.
     * @param uniforms срез со всеми uniform-блоками.
     * @param start    индекс первой команды слоя.
     * @param end      индекс за последней командой слоя.
     */
    private void draw(RenderPass pass, GpuBufferSlice uniforms, int start, int end) {
        RenderPipeline pipeline = null;
        UiTextureBinding texture = null;
        UiTextureBinding foilTexture = null;
        for (int i = start; i < end; i += draws[i].instances) {
            Draw draw = draws[i];
            if (pipeline != draw.pipeline) {
                pipeline = draw.pipeline;
                MinecraftGpuCompat.setPipeline(pass, pipeline);
                texture = null;
                foilTexture = null;
                if (draw.surfacePipeline && draw.shadowReceiver) {
                    MinecraftGpuCompat.bindTexture(pass, "SceneShadowMap", shadowTarget.nativeTarget().getDepthTextureView(),
                            RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
                }
                if (draw.surfacePipeline && draw.localLighting) localLights.bind(pass);
            }
            if (texture != draw.texture) {
                texture = draw.texture;
                if (texture != null)
                    MinecraftGpuCompat.bindTexture(pass, "Sampler0", texture.view(), texture.sampler());
            }
            if (draw.surfacePipeline && !drawingShadows && draw.nativeLayer != null
                    && draw.nativeLayer.foil() != null && foilTexture != draw.nativeLayer.foil() && texture != null) {
                foilTexture = draw.nativeLayer.foil();
                MinecraftGpuCompat.bindTexture(pass, "GlintSampler", foilTexture.view(), foilTexture.sampler());
            }
            pass.setUniform("SceneMesh", uniforms.buffer().slice(uniforms.offset() + draw.uniformOffset,
                    SceneMeshData.UNIFORM_BYTES));
            if (draw.surfacePipeline) {
                pass.setUniform("SceneSurface", uniforms.buffer().slice(uniforms.offset() + draw.surfaceOffset,
                        UiMesh3DShaders.SURFACE_BYTES));
            }
            if (draw.instances > 1) {
                pass.setUniform("SceneInstances", uniforms.buffer().slice(uniforms.offset() + draw.instanceOffset,
                        UiMesh3DShaders.INSTANCE_BYTES));
            }
            MinecraftGpuCompat.drawSceneMesh(pass, draw.gpu.vertexSlice, draw.gpu.indices, draw.mesh.indexCount(),
                    draw.instances);
        }
    }

    /**
     * Получает буферы по идентичности неизменяемого меша или загружает его.
     *
     * @param mesh геометрия, разделяемая между узлами сцены.
     * @return живая пара GPU-буферов.
     */
    private GpuMesh mesh(Mesh3D mesh) {
        GpuMesh gpu = meshes.get(mesh);
        if (gpu == null) {
            gpu = upload(mesh);
            meshes.put(mesh, gpu);
            meshBytes += gpu.bytes;
            uploads++;
        }
        gpu.lastUse = passSequence;
        return gpu;
    }

    /**
     * Выделяет статические буферы и записывает данные один раз.
     *
     * @param mesh исходные вершины и индексы.
     * @return буферы, освобождаемые вместе с кэшем.
     */
    private static GpuMesh upload(Mesh3D mesh) {
        if (DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL.getVertexSize() != SceneMeshData.VERTEX_BYTES) {
            throw new IllegalStateException("Minecraft scene vertex format does not match UniGUI packing");
        }
        int vertexSize = Math.multiplyExact(SceneMeshData.VERTEX_BYTES, mesh.vertexCount());
        int indexSize = Math.multiplyExact(Integer.BYTES, mesh.indexCount());
        ByteBuffer bytes = MemoryUtil.memAlloc(Math.max(vertexSize, indexSize));
        GpuBuffer vertices = null;
        GpuBuffer indices = null;
        try {
            SceneMeshData.writeVertices(mesh, bytes);
            bytes.flip();
            vertices = RenderSystem.getDevice().createBuffer(() -> "UniGUI scene vertices", GpuBuffer.USAGE_VERTEX, bytes);
            bytes.clear();
            SceneMeshData.writeIndices(mesh, bytes);
            bytes.flip();
            indices = RenderSystem.getDevice().createBuffer(() -> "UniGUI scene indices", GpuBuffer.USAGE_INDEX, bytes);
            return new GpuMesh(vertices, indices, (long) vertexSize + indexSize);
        } catch (RuntimeException | Error failure) {
            if (vertices != null) vertices.close();
            if (indices != null) indices.close();
            throw failure;
        } finally {
            MemoryUtil.memFree(bytes);
        }
    }

    /**
     * Убирает старые буферы при превышении бюджета. Меши текущего прохода остаются в кэше.
     */
    private void trimMeshes() {
        if (meshBytes <= CACHE_BYTES && meshes.size() <= CACHE_MESHES) return;
        var entries = meshes.values().iterator();
        while (entries.hasNext() && (meshBytes > CACHE_BYTES || meshes.size() > CACHE_MESHES)) {
            GpuMesh oldest = entries.next();
            if (oldest.lastUse == passSequence) break;
            entries.remove();
            meshBytes -= oldest.bytes;
            RenderSystem.queueFencedTask(oldest::close);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        if (fog != null) fog.close();
        if (localLights != null) localLights.close();
        if (shadowTarget != null) {
            shadowTarget.close();
            shadowTarget = null;
        }
        if (vanilla != null) {
            vanilla.close();
            vanilla = null;
        }
        for (GpuMesh mesh : meshes.values()) mesh.close();
        meshes.clear();
        meshBytes = 0;
        if (uniformBytes != null) {
            MemoryUtil.memFree(uniformBytes);
            uniformBytes = null;
        }
    }

    /**
     * Ограничивает компоненту диапазоном цвета framebuffer.
     *
     * @param value входная компонента цвета.
     * @return конечное значение от 0 до 1.
     */
    private static float clampColor(float value) {
        return value > 0 ? Math.min(value, 1) : 0;
    }

    /**
     * Переиспользуемая команда рисования одного видимого меша.
     */
    private static final class Draw {
        /**
         * Мировое преобразование узла.
         */
        private final MutableMat4 world = new MutableMat4();
        /**
         * Геометрия текущего узла.
         */
        private Mesh3D mesh;
        /**
         * Материал текущего узла.
         */
        private Material material;
        /**
         * Custom shader function and parameters for this draw, or null.
         */
        private SceneMaterialEffect effect;
        /**
         * Owner of the active effect, or null to use scene time.
         */
        private SceneNode effectNode;
        /**
         * Local AABB shared by all of this node's effect draws, or null for default mesh bounds.
         */
        private Aabb effectBounds;
        /**
         * Whether the color shader should sample the directional shadow map.
         */
        private boolean shadowReceiver;
        /**
         * Whether this material is lit by the scene's shared point and spot lights.
         */
        private boolean localLighting;
        /**
         * Whether perspective shadow maps affect this node in the color pass.
         */
        private boolean receiveSpotShadows;
        /**
         * Whether the selected program binds the advanced surface uniform block.
         */
        private boolean surfacePipeline;
        /**
         * Читает глубину без записи и смешивает цвет с фоном.
         */
        private boolean translucent;
        /**
         * Z центра меша в камере для сортировки.
         */
        private double depth;
        /**
         * Номер команды в порядке дерева.
         */
        private int order;
        /**
         * Число экземпляров подряд, начиная с этой команды.
         */
        private int instances;
        /**
         * Смещение общего блока материала в байтах.
         */
        private int uniformOffset;
        /**
         * Surface uniform offset in bytes, or -1 when the draw uses standard uniforms alone.
         */
        private int surfaceOffset;
        /**
         * Смещение блока экземпляров в байтах, используется только при instances > 1.
         */
        private int instanceOffset;
        /**
         * Подготовленный вариант пайплайна.
         */
        private RenderPipeline pipeline;
        /**
         * Разрешённая текстура или null.
         */
        private UiTextureBinding texture;
        /**
         * Texture binding of a transient native mesh, or null for cached scene geometry.
         */
        private SceneNativeLayer nativeLayer;
        /**
         * Привязанные статические буферы.
         */
        private GpuMesh gpu;
        /**
         * Scene node that produced this draw, or null for the editor highlight.
         */
        private SceneNode node;
        /**
         * Whether this command draws wireframe edges of an earlier surface draw.
         */
        private boolean wire;

        /**
         * Освобождает ссылки на текущую сцену перед следующим проходом.
         */
        private void releaseReferences() {
            mesh = null;
            material = null;
            effect = null;
            effectNode = null;
            effectBounds = null;
            pipeline = null;
            texture = null;
            nativeLayer = null;
            gpu = null;
            node = null;
        }
    }

    /**
     * Владеет двумя неизменяемыми GPU-буферами меша.
     */
    private static final class GpuMesh {
        /**
         * Буфер вершин.
         */
        private final GpuBuffer vertices;
        /**
         * Срез с полным набором вершин.
         */
        private final GpuBufferSlice vertexSlice;
        /**
         * Буфер индексов.
         */
        private final GpuBuffer indices;
        /**
         * Суммарный размер данных в байтах.
         */
        private final long bytes;
        /**
         * Номер последнего прохода, использовавшего эти буферы.
         */
        private long lastUse;

        /**
         * Принимает во владение буферы.
         *
         * @param vertices буфер вершин.
         * @param indices  буфер индексов.
         * @param bytes    суммарный размер в байтах.
         */
        private GpuMesh(GpuBuffer vertices, GpuBuffer indices, long bytes) {
            this.vertices = vertices;
            this.vertexSlice = vertices.slice();
            this.indices = indices;
            this.bytes = bytes;
        }

        /**
         * Освобождает выделенные GPU-ресурсы.
         */
        private void close() {
            vertices.close();
            indices.close();
        }
    }
}
