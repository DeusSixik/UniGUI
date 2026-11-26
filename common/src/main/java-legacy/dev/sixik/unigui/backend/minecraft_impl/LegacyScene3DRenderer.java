package dev.sixik.unigui.backend.minecraft_impl;

import dev.sixik.unigui.api.scene.BlockNode;
import dev.sixik.unigui.api.scene.EntityNode;
import dev.sixik.unigui.api.scene.ItemNode;
import dev.sixik.unigui.api.scene.StructureNode;
import net.minecraft.world.level.block.state.BlockState;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import dev.sixik.unigui.api.math.Aabb;
import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.math.Frustum3D;
import dev.sixik.unigui.api.math.MutableMat4;
import dev.sixik.unigui.api.math.MutableVec3;
import dev.sixik.unigui.api.render.RenderTarget;
import dev.sixik.unigui.api.render.Scene3DRenderer;
import dev.sixik.unigui.api.render.TextureHandle;
import dev.sixik.unigui.api.scene.Camera3D;
import dev.sixik.unigui.api.scene.Material;
import dev.sixik.unigui.api.scene.Mesh3D;
import dev.sixik.unigui.api.scene.Scene;
import dev.sixik.unigui.api.scene.SceneNode;
import dev.sixik.unigui.impl.render.SceneMeshData;
import it.unimi.dsi.fastutil.objects.ObjectArrays;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;
import org.lwjgl.opengl.GL32;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Рисует индексированные меши и ванильные модели в GL-цель версий 1.20.1 и 1.21.1.
 */
final class LegacyScene3DRenderer implements Scene3DRenderer, AutoCloseable {
    /**
     * Точка привязки std140-блока SceneMesh.
     */
    private static final int UNIFORM_BINDING = 0;
    /**
     * Точка привязки std140-массива экземпляров.
     */
    private static final int INSTANCE_BINDING = 1;
    /**
     * Максимум буферов геометрии, сохраняемых между проходами.
     */
    private static final int CACHE_SIZE = 512;
    /**
     * Порядок слоёв и порядок прозрачных мешей от дальнего к ближнему.
     */
    private static final Comparator<Draw> ORDER = Comparator
            .comparingInt((Draw draw) -> draw.material.alphaMode().ordinal())
            .thenComparingDouble(draw -> draw.depth)
            .thenComparingInt(draw -> draw.order);

    /**
     * Резолвер текстур, общий с 2D UI.
     */
    private final Function<TextureHandle, MinecraftGuiRenderBackend.TextureBinding> textures;
    /**
     * Высылает накопленные GUI-команды перед сменой framebuffer.
     */
    private final Runnable flushGui;
    /**
     * Буферы недавно использованной геометрии, по идентичности Mesh3D.
     */
    private final LinkedHashMap<Mesh3D, GpuMesh> meshes = new LinkedHashMap<>(32, 0.75f, true);
    /**
     * Рабочий обходчик графа.
     */
    private final Consumer<SceneNode> collect = this::collect;
    /**
     * Кэш геометрии блоков и секций структуры.
     */
    private final LegacySceneBlockMeshes blocks = new LegacySceneBlockMeshes();
    /**
     * Ванильный проход, создаваемый при первой модели.
     */
    private LegacySceneFeatures features;
    /**
     * Видимые ванильные узлы текущего кадра.
     */
    private SceneNode[] models = new SceneNode[16];
    /**
     * Количество видимых узлов для ванильного прохода.
     */
    private int modelCount;
    /**
     * Пирамида видимости текущей камеры.
     */
    private final Frustum3D frustum = new Frustum3D();
    /**
     * Матрица вида для сортировки прозрачности.
     */
    private final MutableMat4 view = new MutableMat4();
    /**
     * Проекция, умноженная на матрицу вида.
     */
    private final MutableMat4 viewProjection = new MutableMat4();
    /**
     * Мировая матрица текущего узла.
     */
    private final MutableMat4 world = new MutableMat4();
    /**
     * Матрица из локальных координат текущего узла в clip space.
     */
    private final MutableMat4 clip = new MutableMat4();
    /**
     * Положение камеры в мировых координатах.
     */
    private final MutableVec3 eye = new MutableVec3();
    /**
     * Отключает выбор дальних моделей для ортографического прохода.
     */
    private boolean orthographic;
    /**
     * Идентификаторы программ по текстуре и инстансированию.
     */
    private final int[] programs = new int[4];
    /**
     * Переиспользуемые команды отрисовки.
     */
    private Draw[] draws = new Draw[16];
    /**
     * Число команд в текущем проходе.
     */
    private int drawCount;
    /**
     * Нужна ли сортировка по слоям.
     */
    private boolean sort;
    /**
     * GL-буфер материала и матриц экземпляров. Ноль до выделения.
     */
    private int uniforms;
    /**
     * Выровненное смещение массива экземпляров внутри буфера, в байтах.
     */
    private int instanceOffset;

    /**
     * Создаёт рендерер без выделения GL-ресурсов.
     *
     * @param textures резолвер текстуры текущего бэкенда.
     * @param flushGui отправка накопленной ванильной геометрии UI.
     */
    LegacyScene3DRenderer(Function<TextureHandle, MinecraftGuiRenderBackend.TextureBinding> textures, Runnable flushGui) {
        this.textures = textures;
        this.flushGui = flushGui;
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
        if (!(target instanceof MinecraftRenderTarget output) || !output.options().useDepth()
                || output.options().clearBeforeRender()) {
            throw new IllegalArgumentException("UniGUI scene requires a Minecraft depth target with no 2D auto-clear");
        }
        flushGui.run();
        frustum.set(camera.viewProjection(viewProjection));
        camera.view(view);
        camera.position(eye);
        orthographic = camera.orthographic();
        try {
            scene.visitVisible(frustum, collect);
            if (highlight != null) addDraw(null, highlight.mesh(), highlight.material(), highlight);
            if (sort) ObjectArrays.quickSort(draws, 0, drawCount, ORDER);
            renderTarget(output, scene, camera, viewportWidth, viewportHeight, clearColor, partialTick);
            while (meshes.size() > Math.max(drawCount, CACHE_SIZE)) {
                var oldest = meshes.entrySet().iterator();
                oldest.next().getValue().close();
                oldest.remove();
            }
        } finally {
            for (int i = 0; i < drawCount; i++) draws[i].clear();
            Arrays.fill(models, 0, modelCount, null);
            modelCount = 0;
            drawCount = 0;
            sort = false;
        }
    }

    /**
     * Изолирует GL-цель и состояние, которыми пользуются остальные команды UI.
     *
     * @param target      целевой цвет и глубина.
     * @param scene       граф сцены с материалами и светом.
     * @param camera      камера сцены.
     * @param width       ширина области сцены в пикселях.
     * @param height      высота области сцены в пикселях.
     * @param color       фон без premultiplied alpha.
     * @param partialTick дробная часть тика для ванильных анимаций.
     */
    private void renderTarget(MinecraftRenderTarget target, Scene scene, Camera3D camera,
                              float width, float height, ColorView color, float partialTick) {
        var shaderState = MinecraftShaderQuadRenderer.RenderState.capture();
        int depthFunction = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
        int drawFramebuffer = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        int readFramebuffer = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        int vao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        int arrayBuffer = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
        int shaderTexture = RenderSystem.getShaderTexture(0);
        int uniformBuffer = GL11.glGetInteger(GL31.GL_UNIFORM_BUFFER_BINDING);
        int bindingBuffer = GL30.glGetIntegeri(GL31.GL_UNIFORM_BUFFER_BINDING, UNIFORM_BINDING);
        long bindingOffset = GL32.glGetInteger64i(GL31.GL_UNIFORM_BUFFER_START, UNIFORM_BINDING);
        long bindingSize = GL32.glGetInteger64i(GL31.GL_UNIFORM_BUFFER_SIZE, UNIFORM_BINDING);
        int instanceBuffer = GL30.glGetIntegeri(GL31.GL_UNIFORM_BUFFER_BINDING, INSTANCE_BINDING);
        long originalInstanceOffset = GL32.glGetInteger64i(GL31.GL_UNIFORM_BUFFER_START, INSTANCE_BINDING);
        long instanceSize = GL32.glGetInteger64i(GL31.GL_UNIFORM_BUFFER_SIZE, INSTANCE_BINDING);
        boolean scissorEnabled = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer viewport = stack.mallocInt(4);
            IntBuffer scissor = stack.mallocInt(4);
            ByteBuffer colorMask = stack.malloc(4);
            GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
            GL11.glGetIntegerv(GL11.GL_SCISSOR_BOX, scissor);
            GL11.glGetBooleanv(GL11.GL_COLOR_WRITEMASK, colorMask);
            try {
                target.bindWritePreserveContents();
                RenderSystem.disableScissor();
                RenderSystem.depthMask(true);
                RenderSystem.colorMask(true, true, true, true);
                float alpha = channel(color.a());
                GL30.glClearBufferfv(GL11.GL_COLOR, 0,
                        stack.floats(channel(color.r()) * alpha, channel(color.g()) * alpha, channel(color.b()) * alpha, alpha));
                GL30.glClearBufferfv(GL11.GL_DEPTH, 0, stack.floats(1, 0, 0, 0));
                if (drawCount == 0 && modelCount == 0) return;
                int pixelsWide = Math.max(1, Math.min((int) Math.ceil(width), target.width()));
                int pixelsHigh = Math.max(1, Math.min((int) Math.ceil(height), target.height()));
                RenderSystem.enableScissor(0, target.height() - pixelsHigh, pixelsWide, pixelsHigh);
                RenderSystem.enableDepthTest();
                RenderSystem.depthFunc(GL11.GL_LEQUAL);
                RenderSystem.disableCull();
                RenderSystem.activeTexture(GL13.GL_TEXTURE0);
                float scaleX = width / target.width(), scaleY = height / target.height();
                int opaqueEnd = drawCount;
                while (opaqueEnd > 0 && draws[opaqueEnd - 1].material.alphaMode() == Material.AlphaMode.TRANSLUCENT)
                    opaqueEnd--;
                ByteBuffer bytes = stack.malloc(Math.max(SceneMeshData.UNIFORM_BYTES, UiMesh3DShaders.INSTANCE_BYTES));
                draw(scene, target, scaleX, scaleY, bytes, 0, opaqueEnd);
                if (modelCount != 0) {
                    restoreUniformBinding(UNIFORM_BINDING, bindingBuffer, bindingOffset, bindingSize);
                    restoreUniformBinding(INSTANCE_BINDING, instanceBuffer, originalInstanceOffset, instanceSize);
                    GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, uniformBuffer);
                    if (features == null) features = new LegacySceneFeatures();
                    features.render(models, modelCount, target, camera, view, scaleX, scaleY, partialTick);
                }
                RenderSystem.enableDepthTest();
                RenderSystem.depthFunc(GL11.GL_LEQUAL);
                RenderSystem.disableCull();
                RenderSystem.activeTexture(GL13.GL_TEXTURE0);
                draw(scene, target, scaleX, scaleY, bytes, opaqueEnd, drawCount);
            } finally {
                BufferUploader.invalidate();
                GL30.glBindVertexArray(vao);
                GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, arrayBuffer);
                restoreUniformBinding(UNIFORM_BINDING, bindingBuffer, bindingOffset, bindingSize);
                restoreUniformBinding(INSTANCE_BINDING, instanceBuffer, originalInstanceOffset, instanceSize);
                GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, uniformBuffer);
                RenderSystem.depthFunc(depthFunction);
                RenderSystem.colorMask(colorMask.get(0) != 0, colorMask.get(1) != 0, colorMask.get(2) != 0, colorMask.get(3) != 0);
                RenderSystem.setShaderTexture(0, shaderTexture);
                shaderState.restore();
                GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, drawFramebuffer);
                GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, readFramebuffer);
                RenderSystem.viewport(viewport.get(0), viewport.get(1), viewport.get(2), viewport.get(3));
                if (scissorEnabled)
                    RenderSystem.enableScissor(scissor.get(0), scissor.get(1), scissor.get(2), scissor.get(3));
                else RenderSystem.disableScissor();
            }
        }
    }

    /**
     * Возвращает привязку, которую использовал предыдущий GL-проход.
     *
     * @param index  индекс точки привязки.
     * @param buffer имя буфера или ноль для пустой привязки.
     * @param offset начало диапазона в байтах.
     * @param size   длина диапазона в байтах или ноль при привязке целого буфера.
     */
    private static void restoreUniformBinding(int index, int buffer, long offset, long size) {
        if (size > 0) GL30.glBindBufferRange(GL31.GL_UNIFORM_BUFFER, index, buffer, offset, size);
        else GL30.glBindBufferBase(GL31.GL_UNIFORM_BUFFER, index, buffer);
    }

    /**
     * Добавляет меш видимого узла. Для полупрозрачного меша глубина равна Z координате центра AABB в матрице вида.
     *
     * @param node видимый узел сцены.
     */
    private void collect(SceneNode node) {
        if (node.mesh() != null) addDraw(node, node.mesh(), node.material());
        if (node instanceof BlockNode block) {
            BlockState state = MinecraftScene.state(block);
            if (state == null) return;
            addSurfaces(block, blocks.single(state));
            if (MinecraftScene.blockEntity(block) == null) return;
        } else if (node instanceof StructureNode structure && structure.platform() == MinecraftScene.PLATFORM) {
            StructureNode.DistantMesh distant = structure.distantMesh(orthographic ? null : eye, structure.worldMatrix(world));
            if (distant != null) {
                addDraw(node, distant.mesh(), distant.material());
                return;
            }
            int top = structure.topVisibleY();
            boolean hasBlockEntities = false;
            for (StructureNode.Section section : structure.sections()) {
                if (section.minBlockY() > top) continue;
                addSurfaces(node, blocks.section(structure, section));
                hasBlockEntities |= section.blockEntityCount() != 0;
            }
            if (!hasBlockEntities) return;
        } else if (!(node instanceof ItemNode) && !(node instanceof EntityNode)) {
            return;
        }
        if (modelCount == models.length) models = Arrays.copyOf(models, models.length * 2);
        models[modelCount++] = node;
    }

    /**
     * Отбирает поверхности блока или секции по собственным границам.
     *
     * @param node     владелец мирового преобразования.
     * @param surfaces меши, запечённые из ванильных моделей.
     */
    private void addSurfaces(SceneNode node, List<LegacySceneBlockMeshes.Surface> surfaces) {
        node.worldMatrix(world);
        for (int i = 0; i < surfaces.size(); i++) {
            var layer = surfaces.get(i);
            if (frustum.intersects(layer.mesh().bounds(), world)) addDraw(node, layer.mesh(), layer.material());
        }
    }

    /**
     * Записывает меш видимого узла.
     *
     * @param node     владелец мирового преобразования.
     * @param mesh     геометрия.
     * @param material настройки цвета, света и слоя.
     */
    private void addDraw(SceneNode node, Mesh3D mesh, Material material) {
        addDraw(node, mesh, material, null);
    }

    /**
     * Записывает меш сцены или независимый меш подсветки.
     *
     * @param node      владелец мирового преобразования для обычного меша.
     * @param mesh      индексированная геометрия.
     * @param material  настройки поверхности.
     * @param highlight необязательная подсветка с собственной матрицей.
     */
    private void addDraw(SceneNode node, Mesh3D mesh, Material material, Highlight highlight) {
        if (drawCount == draws.length) draws = Arrays.copyOf(draws, draws.length * 2);
        Draw draw = draws[drawCount];
        if (draw == null) draws[drawCount] = draw = new Draw();
        draw.node = node;
        draw.mesh = mesh;
        draw.material = material;
        draw.highlight = highlight;
        draw.order = drawCount++;
        var mode = material.alphaMode();
        sort |= mode != Material.AlphaMode.OPAQUE;
        draw.depth = 0;
        if (mode == Material.AlphaMode.TRANSLUCENT) {
            Aabb bounds = mesh.bounds();
            clip.setProduct(view, draw.worldMatrix(world));
            draw.depth = clip.transformComponent(2, ((double) bounds.minX() + bounds.maxX()) * 0.5,
                    ((double) bounds.minY() + bounds.maxY()) * 0.5, ((double) bounds.minZ() + bounds.maxZ()) * 0.5, 1);
        }
    }

    /**
     * Рисует слои с общей глубиной. Прозрачный слой читает глубину и смешивает premultiplied RGBA.
     *
     * @param scene  источник освещения.
     * @param target цель, которую нельзя одновременно читать как текстуру.
     * @param scaleX доля ширины, занимаемая сценой.
     * @param scaleY доля высоты, занимаемая сценой.
     * @param bytes  рабочий буфер, вмещающий любой из двух std140-блоков.
     * @param start  индекс первой команды.
     * @param end    индекс после последней команды.
     */
    private void draw(Scene scene, MinecraftRenderTarget target, float scaleX, float scaleY, ByteBuffer bytes, int start, int end) {
        if (start == end) return;
        if (uniforms == 0) {
            uniforms = GL15.glGenBuffers();
            GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, uniforms);
            int alignment = GL11.glGetInteger(GL31.GL_UNIFORM_BUFFER_OFFSET_ALIGNMENT);
            instanceOffset = (SceneMeshData.UNIFORM_BYTES + alignment - 1) / alignment * alignment;
            GL15.glBufferData(GL31.GL_UNIFORM_BUFFER, instanceOffset + UiMesh3DShaders.INSTANCE_BYTES, GL15.GL_STREAM_DRAW);
        }
        GL30.glBindBufferRange(GL31.GL_UNIFORM_BUFFER, UNIFORM_BINDING, uniforms, 0, SceneMeshData.UNIFORM_BYTES);
        int colorTexture = target.nativeTarget().getColorTextureId();
        for (int i = start; i < end; ) {
            Draw draw = draws[i];
            int instanceCount = countInstances(i, end);
            Material material = draw.material;
            var binding = textures.apply(material.texture());
            boolean blend = material.alphaMode() == Material.AlphaMode.TRANSLUCENT;
            RenderSystem.depthMask(!blend);
            if (blend) {
                RenderSystem.enableBlend();
                MinecraftUiBlend.applyTextureAlpha(true, true);
            } else RenderSystem.disableBlend();
            if (binding != null) {
                binding.bind();
                if (RenderSystem.getShaderTexture(0) == colorTexture) binding = null;
            }
            GL20.glUseProgram(program(binding != null, instanceCount > 1));
            int samplerDepth = MinecraftTextureSamplerState.depth();
            try {
                if (binding != null) {
                    RenderSystem.bindTexture(RenderSystem.getShaderTexture(0));
                    MinecraftTextureSamplerState.apply(binding.options());
                }
                clip.setProduct(viewProjection, draw.worldMatrix(world));
                bytes.clear();
                SceneMeshData.writeUniform(bytes, clip, world, material, scene.ambientLight(), scene.directionalLight(),
                        scaleX, scaleY, false, binding != null && binding.flipY(), binding != null && binding.premultipliedAlpha());
                GL15.glBufferSubData(GL31.GL_UNIFORM_BUFFER, 0, bytes.flip());
                GpuMesh mesh = meshes.computeIfAbsent(draw.mesh, GpuMesh::upload);
                GL30.glBindVertexArray(mesh.vao());
                if (instanceCount > 1) {
                    bytes.clear();
                    for (int index = i; index < i + instanceCount; index++) {
                        SceneMeshData.writeClipMatrix(bytes, clip.setProduct(viewProjection, draws[index].worldMatrix(world)));
                    }
                    GL30.glBindBufferRange(GL31.GL_UNIFORM_BUFFER, INSTANCE_BINDING, uniforms, instanceOffset,
                            UiMesh3DShaders.INSTANCE_BYTES);
                    GL15.glBufferSubData(GL31.GL_UNIFORM_BUFFER, instanceOffset, bytes.flip());
                    GL31.glDrawElementsInstanced(GL11.GL_TRIANGLES, mesh.indexCount(), GL11.GL_UNSIGNED_INT, 0L, instanceCount);
                } else {
                    GL11.glDrawElements(GL11.GL_TRIANGLES, mesh.indexCount(), GL11.GL_UNSIGNED_INT, 0);
                }
            } finally {
                MinecraftTextureSamplerState.restoreTo(samplerDepth);
            }
            i += instanceCount;
        }
    }

    /**
     * Считает соседние экземпляры одного меша с тем же материалом и записью глубины.
     *
     * @param start индекс первого экземпляра.
     * @param end   индекс после последней команды прохода.
     * @return число экземпляров для одного вызова GPU.
     */
    private int countInstances(int start, int end) {
        Draw first = draws[start];
        if (first.material.shading() != Material.Shading.UNLIT
                || first.material.alphaMode() == Material.AlphaMode.TRANSLUCENT) return 1;
        int limit = Math.min(end - start, UiMesh3DShaders.MAX_INSTANCES);
        int count = 1;
        while (count < limit) {
            Draw next = draws[start + count];
            if (next.mesh != first.mesh || next.material != first.material) break;
            count++;
        }
        return count;
    }

    /**
     * Находит или линкует нужную программу GL 3.2.
     *
     * @param textured  нужна ли текстурная выборка.
     * @param instanced читать ли матрицы экземпляров.
     * @return GL-идентификатор программы.
     */
    private int program(boolean textured, boolean instanced) {
        int slot = (textured ? 1 : 0) + (instanced ? 2 : 0);
        if (programs[slot] == 0) {
            int program = MinecraftShaderQuadRenderer.linkProgram("UniGUI Scene3D", UiMesh3DShaders.vertex(150, false, instanced),
                    UiMesh3DShaders.fragment(150, textured), "Position", "UV0", "Color", "Normal");
            if (program == 0) throw new IllegalStateException("Cannot link the UniGUI Scene3D OpenGL shader");
            GL31.glUniformBlockBinding(program, GL31.glGetUniformBlockIndex(program, "SceneMesh"), UNIFORM_BINDING);
            if (instanced) {
                GL31.glUniformBlockBinding(program, GL31.glGetUniformBlockIndex(program, "SceneInstances"), INSTANCE_BINDING);
            }
            programs[slot] = program;
        }
        return programs[slot];
    }

    /**
     * Ограничивает цвет фона диапазоном, который можно записать в текстуру.
     *
     * @param value компонента цвета.
     * @return значение от 0 до 1.
     */
    private static float channel(float value) {
        return Float.isFinite(value) ? Math.max(0, Math.min(1, value)) : 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        for (GpuMesh mesh : meshes.values()) mesh.close();
        meshes.clear();
        for (int program : programs) if (program != 0) GL20.glDeleteProgram(program);
        Arrays.fill(programs, 0);
        if (uniforms != 0) GL15.glDeleteBuffers(uniforms);
        uniforms = 0;
    }

    /**
     * Команда видимого узла, упорядоченная по материалу и глубине.
     */
    private static final class Draw {
        /**
         * Узел, задающий мировую матрицу.
         */
        SceneNode node;
        /**
         * Индексированная геометрия команды.
         */
        Mesh3D mesh;
        /**
         * Материал текущей поверхности.
         */
        Material material;
        /**
         * Подсветка, если команда не принадлежит узлу графа.
         */
        Highlight highlight;
        /**
         * Z центра меша в координатах вида.
         */
        double depth;
        /**
         * Исходный порядок обхода для совпадающей глубины.
         */
        int order;

        /**
         * Освобождает ссылки на объекты сцены после кадра.
         */
        void clear() {
            node = null;
            mesh = null;
            material = null;
            highlight = null;
        }

        /**
         * Копирует мировое преобразование в рабочую матрицу.
         *
         * @param out матрица для текущей отрисовки.
         * @return мировое преобразование меша.
         */
        MutableMat4 worldMatrix(MutableMat4 out) {
            return highlight == null ? node.worldMatrix(out) : out.set(highlight.world());
        }
    }

    /**
     * GL-ресурсы одного неизменяемого меша.
     *
     * @param vao        идентификатор массива атрибутов.
     * @param vertices   VBO с вершинами.
     * @param indices    IBO с 32-битными индексами.
     * @param indexCount число индексов треугольников.
     */
    private record GpuMesh(int vao, int vertices, int indices, int indexCount) implements AutoCloseable {
        /**
         * Загружает геометрию и настраивает атрибуты Position, UV0, Color, Normal.
         *
         * @param mesh неизменяемая геометрия.
         * @return выделенные ресурсы меша.
         */
        static GpuMesh upload(Mesh3D mesh) {
            int stride = SceneMeshData.VERTEX_BYTES;
            int vertexBytes = Math.multiplyExact(mesh.vertexCount(), stride);
            int indexBytes = Math.multiplyExact(mesh.indexCount(), Integer.BYTES);
            ByteBuffer bytes = MemoryUtil.memAlloc(Math.max(vertexBytes, indexBytes));
            GpuMesh gpu = new GpuMesh(GL30.glGenVertexArrays(), GL15.glGenBuffers(), GL15.glGenBuffers(), mesh.indexCount());
            try {
                GL30.glBindVertexArray(gpu.vao);
                GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, gpu.vertices);
                SceneMeshData.writeVertices(mesh, bytes);
                GL15.glBufferData(GL15.GL_ARRAY_BUFFER, bytes.flip(), GL15.GL_STATIC_DRAW);
                GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, stride, 0);
                GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, stride, 12);
                GL20.glVertexAttribPointer(2, 4, GL11.GL_UNSIGNED_BYTE, true, stride, 20);
                GL20.glVertexAttribPointer(3, 4, GL11.GL_BYTE, true, stride, 24);
                for (int i = 0; i < 4; i++) GL20.glEnableVertexAttribArray(i);
                GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, gpu.indices);
                SceneMeshData.writeIndices(mesh, bytes.clear());
                GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, bytes.flip(), GL15.GL_STATIC_DRAW);
                return gpu;
            } catch (RuntimeException | Error failure) {
                gpu.close();
                throw failure;
            } finally {
                MemoryUtil.memFree(bytes);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void close() {
            GL30.glDeleteVertexArrays(vao);
            GL15.glDeleteBuffers(vertices);
            GL15.glDeleteBuffers(indices);
        }
    }
}
