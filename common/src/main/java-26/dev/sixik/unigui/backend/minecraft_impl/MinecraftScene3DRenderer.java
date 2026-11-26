package dev.sixik.unigui.backend.minecraft_impl;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import dev.sixik.unigui.api.scene.BlockNode;
import dev.sixik.unigui.api.scene.EntityNode;
import dev.sixik.unigui.api.scene.ItemNode;
import dev.sixik.unigui.api.scene.StructureNode;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import dev.sixik.unigui.api.backend.UniGuiLogger;
import dev.sixik.unigui.api.backend.UniGuiRuntime;
import dev.sixik.unigui.api.math.Aabb;
import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.math.Frustum3D;
import dev.sixik.unigui.api.math.MutableMat4;
import dev.sixik.unigui.api.math.MutableVec3;
import dev.sixik.unigui.api.render.RenderTarget;
import dev.sixik.unigui.api.render.Scene3DRenderer;
import dev.sixik.unigui.api.scene.Camera3D;
import dev.sixik.unigui.api.scene.Material;
import dev.sixik.unigui.api.scene.Mesh3D;
import dev.sixik.unigui.api.scene.Scene;
import dev.sixik.unigui.api.scene.SceneNode;
import dev.sixik.unigui.impl.render.SceneMeshData;
import it.unimi.dsi.fastutil.objects.ObjectArrays;
import net.minecraft.core.BlockPos;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Comparator;
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
     * Буферы неизменяемых мешей, ключом служит идентичность экземпляра Mesh3D.
     */
    private final LinkedHashMap<Mesh3D, GpuMesh> meshes = new LinkedHashMap<>(32, 0.75f, true);
    /**
     * Кэш ванильных моделей и запечённых секций.
     */
    private final SceneBlockMeshes blockMeshes = new SceneBlockMeshes();
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
     * Создаёт рендерер без GPU-аллокаций. Буферы выделяются по мере использования.
     *
     * @param textures кэш разрешённых текстур владельца.
     */
    MinecraftScene3DRenderer(UiTextureBinding.Cache textures) {
        this.textures = textures;
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
        activeCamera = camera;
        activePartialTick = partialTick;
        camera.position(eye);
        camera.view(view);
        frustum.set(camera.viewProjection(viewProjection));
        float alpha = clampColor(clearColor.a());
        clear.set(clampColor(clearColor.r()) * alpha, clampColor(clearColor.g()) * alpha,
                clampColor(clearColor.b()) * alpha, alpha);
        try {
            scene.visitVisible(frustum, collect);
            if (highlight != null)
                collectMesh(highlight.mesh(), highlight.material(), nodeWorld.set(highlight.world()));
            if (needsSort) ObjectArrays.quickSort(draws, 0, drawCount, DRAW_ORDER);
            int alignment = Math.max(16, MinecraftGpuCompat.uniformAlignment());
            GpuBufferSlice uniforms = drawCount == 0 ? null
                    : prepare(scene, output, viewportWidth / output.width(), viewportHeight / output.height(), alignment);
            int pixelsWide = Math.clamp((int) Math.ceil(viewportWidth), 1, output.width());
            int pixelsHigh = Math.clamp((int) Math.ceil(viewportHeight), 1, output.height());
            boolean nativeModels = vanillaBegun && vanilla.hasSubmissions();
            int opaqueEnd = drawCount;
            if (nativeModels) {
                while (opaqueEnd > 0 && draws[opaqueEnd - 1].translucent) opaqueEnd--;
            }
            try (RenderPass pass = MinecraftGpuCompat.openScenePass(PASS_NAME, output, clear)) {
                clipToViewport(pass, output, pixelsWide, pixelsHigh);
                if (opaqueEnd > 0) draw(pass, uniforms, 0, opaqueEnd);
            }
            if (nativeModels) {
                vanilla.render(output, viewportWidth, viewportHeight, camera.orthographic());
                if (opaqueEnd < drawCount) {
                    try (RenderPass pass = MinecraftGpuCompat.openPass(PASS_NAME, output.color(), output.depth())) {
                        clipToViewport(pass, output, pixelsWide, pixelsHigh);
                        draw(pass, uniforms, opaqueEnd, drawCount);
                    }
                }
            }
            if (!announced && drawCount > 0) {
                LOGGER.info("UniGUI scene mesh RenderPipeline drew {} meshes", drawCount);
                announced = true;
            }
            UiDebugCapture.onSceneRendered(drawCount, drawCalls, uploads, meshes.size());
        } finally {
            for (int i = 0; i < drawCount; i++) draws[i].releaseReferences();
            drawCount = 0;
            drawCalls = 0;
            needsSort = false;
            if (vanillaBegun) vanilla.reset();
            vanillaBegun = false;
            activeCamera = null;
            Arrays.fill(pipelines, null);
        }
        trimMeshes();
    }

    /**
     * Добавляет меш видимого узла в рабочий список.
     *
     * @param node узел с мировым преобразованием и материалом.
     */
    private void collectNode(SceneNode node) {
        Mesh3D mesh = node.mesh();
        if (mesh != null) collectMesh(mesh, node.material(), node.worldMatrix(nodeWorld));
        if (node instanceof BlockNode block) {
            BlockState state = MinecraftScene.state(block);
            if (state == null) return;
            block.worldMatrix(nodeWorld);
            collectSurfaces(blockMeshes.single(state), nodeWorld);
            BlockEntity blockEntity = MinecraftScene.blockEntity(block);
            if (blockEntity != null) vanilla().submit(blockEntity, BlockPos.ZERO, nodeWorld);
        } else if (node instanceof StructureNode structure && structure.platform() == MinecraftScene.PLATFORM) {
            structure.worldMatrix(nodeWorld);
            StructureNode.DistantMesh distant = structure.distantMesh(activeCamera.orthographic() ? null : eye, nodeWorld);
            if (distant != null) {
                collectMesh(distant.mesh(), distant.material(), nodeWorld);
                return;
            }
            int topY = structure.topVisibleY();
            for (StructureNode.Section section : structure.sections()) {
                int bottom = section.minBlockY();
                if (bottom > topY) continue;
                collectSurfaces(blockMeshes.section(structure, section), nodeWorld);
                if (section.blockEntityCount() == 0) continue;
                for (int index = 0; index < StructureNode.Section.VOLUME; index++) {
                    if (section.blockEntity(index) instanceof BlockEntity blockEntity
                            && bottom + StructureNode.Section.localY(index) <= topY) {
                        vanilla().submit(blockEntity, blockEntity.getBlockPos(), nodeWorld);
                    }
                }
            }
        } else if (node instanceof EntityNode entity) {
            vanilla().submit(entity, entity.worldMatrix(nodeWorld));
        } else if (node instanceof ItemNode item) {
            vanilla().submit(item, item.worldMatrix(nodeWorld));
        }
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
     */
    private void collectSurfaces(List<SceneBlockMeshes.Surface> surfaces, MutableMat4 world) {
        for (int i = 0; i < surfaces.size(); i++) {
            SceneBlockMeshes.Surface surface = surfaces.get(i);
            if (frustum.intersects(surface.mesh().bounds(), world))
                collectMesh(surface.mesh(), surface.material(), world);
        }
    }

    /**
     * Записывает параметры видимого меша для отрисовки.
     *
     * @param mesh     локальная геометрия.
     * @param material поверхность меша.
     * @param world    мировое преобразование меша.
     */
    private void collectMesh(Mesh3D mesh, Material material, MutableMat4 world) {
        if (drawCount == draws.length) draws = Arrays.copyOf(draws, draws.length * 2);
        Draw draw = draws[drawCount];
        if (draw == null) draws[drawCount] = draw = new Draw();
        draw.order = drawCount++;
        draw.mesh = mesh;
        draw.material = material;
        draw.world.set(world);
        draw.translucent = material.alphaMode() == Material.AlphaMode.TRANSLUCENT;
        draw.depth = draw.translucent ? viewDepth(mesh.bounds(), draw.world) : 0;
        needsSort |= material.alphaMode() != Material.AlphaMode.OPAQUE;
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
            draw.texture = textures.resolve(draw.material.texture());
            if (draw.texture != null && draw.texture.view().texture() == output.color().texture()) draw.texture = null;
            boolean instanced = draw.instances > 1;
            int variant = (draw.texture == null ? 0 : 1) + (instanced ? 4 : draw.translucent ? 2 : 0);
            if (pipelines[variant] == null) {
                pipelines[variant] = UiMesh3DPipeline.get(draw.texture != null, draw.translucent, instanced);
            }
            draw.pipeline = pipelines[variant];
            draw.gpu = mesh(draw.mesh);

            clip.setProduct(viewProjection, draw.world);
            uniformBytes.position(draw.uniformOffset);
            SceneMeshData.writeUniform(uniformBytes, clip, draw.world, draw.material, scene.ambientLight(),
                    scene.directionalLight(), viewportX, viewportY, zeroToOne,
                    draw.texture != null && draw.texture.flipY(), draw.texture != null && draw.texture.premultipliedAlpha());
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
            int instances = 1;
            if (!draw.translucent && draw.material.shading() == Material.Shading.UNLIT) {
                int limit = Math.min(drawCount - i, UiMesh3DShaders.MAX_INSTANCES);
                while (instances < limit) {
                    Draw next = draws[i + instances];
                    if (next.mesh != draw.mesh || next.material != draw.material) break;
                    instances++;
                }
            }
            draw.instances = instances;
            draw.uniformOffset = align(end, alignment);
            end = Math.addExact(draw.uniformOffset, SceneMeshData.UNIFORM_BYTES);
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
        for (int i = start; i < end; i += draws[i].instances) {
            Draw draw = draws[i];
            if (pipeline != draw.pipeline) {
                pipeline = draw.pipeline;
                MinecraftGpuCompat.setPipeline(pass, pipeline);
                texture = null;
            }
            if (texture != draw.texture) {
                texture = draw.texture;
                if (texture != null)
                    MinecraftGpuCompat.bindTexture(pass, "Sampler0", texture.view(), texture.sampler());
            }
            pass.setUniform("SceneMesh", uniforms.buffer().slice(uniforms.offset() + draw.uniformOffset,
                    SceneMeshData.UNIFORM_BYTES));
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
         * Привязанные статические буферы.
         */
        private GpuMesh gpu;

        /**
         * Освобождает ссылки на текущую сцену перед следующим проходом.
         */
        private void releaseReferences() {
            mesh = null;
            material = null;
            pipeline = null;
            texture = null;
            gpu = null;
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
