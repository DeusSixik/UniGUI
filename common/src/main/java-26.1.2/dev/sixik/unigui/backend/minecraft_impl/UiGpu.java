package dev.sixik.unigui.backend.minecraft_impl;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.sixik.unigui.api.backend.UniGuiLogger;
import dev.sixik.unigui.api.backend.UniGuiRuntime;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;
import java.util.OptionalInt;

/**
 * Немедленный рендер UniGUI поверх GPU-абстракции Minecraft 26.1 ({@code GpuDevice}/{@code RenderPass}).
 *
 * <p>В 1.20/1.21 бэкенд рисовал сразу в текущий framebuffer, переключая глобальное GL-состояние через
 * {@code RenderSystem}. В 26.1 глобального состояния нет: blend, шейдер и формат вершин зашиты в
 * {@link RenderPipeline}, а каждая отрисовка идёт через {@link RenderPass} в явную цель. Этот класс сохраняет
 * старую модель «нарисовать сейчас»: он держит стек целей отрисовки с их ортопроекцией и scissor, а каждая
 * отрисовка загружает вершины в кольцевой буфер и тут же выполняется отдельным проходом.</p>
 *
 * <p>Кольцевые буферы не сбрасываются по кадрам: при переполнении запись начинается с начала. В OpenGL запись
 * через {@code glBufferSubData} упорядочена с уже отправленными отрисовками, так что перезапись безопасна.</p>
 *
 * <p>Все вершины приходят уже в координатах цели (позы применяются на CPU, как и раньше), поэтому ModelView
 * всегда единичная, а {@code ColorModulator} передаёт общий множитель цвета (замена {@code setShaderColor}).</p>
 *
 * <p>Цель может отслеживать «грязную» область: прямоугольник пикселей, в который что-то рисовали. По нему
 * {@link UiLayerCompositor} переносит на экран только реально изменённую часть слоя.</p>
 */
final class UiGpu {
    private static final UniGuiLogger LOGGER = UniGuiRuntime.logger(UiGpu.class);

    /** Минимальная полуглубина ортопроекции: вершины UI лежат около {@code z = 0}. */
    private static final float MIN_PROJECTION_DEPTH = 1000.0f;
    /** Начальный размер кольцевого буфера вершин. */
    private static final int INITIAL_VERTEX_STREAM_BYTES = 4 * 1024 * 1024;
    /** Начальный размер кольцевого буфера униформ. */
    private static final int INITIAL_UNIFORM_STREAM_BYTES = 1024 * 1024;
    private static final Matrix4f IDENTITY = new Matrix4f();
    private static final Vector3f ZERO_OFFSET = new Vector3f();
    private static final List<TextureBind> NO_TEXTURES = List.of();
    private static final List<UniformBind> NO_UNIFORMS = List.of();

    private static final List<State> STATES = new ArrayList<>();
    private static final List<ProjectionMatrixBuffer> PROJECTION_BUFFERS = new ArrayList<>();
    private static final Vector4f COLOR_MODULATOR = new Vector4f(1.0f, 1.0f, 1.0f, 1.0f);
    private static final Stream VERTEX_STREAM = new Stream("UniGUI vertex stream",
            GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_COPY_DST, INITIAL_VERTEX_STREAM_BYTES);
    private static final Stream UNIFORM_STREAM = new Stream("UniGUI uniform stream",
            GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST, INITIAL_UNIFORM_STREAM_BYTES);
    private static int depth;

    private UiGpu() {
    }

    /**
     * Цель отрисовки: цветовая текстура, необязательная глубина и размер в пикселях.
     *
     * <p>Если цель создана с {@code trackDirty = true}, она накапливает ограничивающий прямоугольник всех
     * отрисовок (в пикселях цели, начало в левом верхнем углу).</p>
     */
    static final class Target {
        private final GpuTextureView color;
        private final GpuTextureView depth;
        private final int width;
        private final int height;
        private final boolean trackDirty;
        private int dirtyMinX;
        private int dirtyMinY;
        private int dirtyMaxX;
        private int dirtyMaxY;

        /**
         * @param color      цветовой attachment
         * @param depth      attachment глубины или {@code null}
         * @param width      ширина в пикселях
         * @param height     высота в пикселях
         * @param trackDirty накапливать ли изменённую область
         */
        Target(GpuTextureView color, GpuTextureView depth, int width, int height, boolean trackDirty) {
            this.color = color;
            this.depth = depth;
            this.width = width;
            this.height = height;
            this.trackDirty = trackDirty;
            resetDirty();
        }

        GpuTextureView color() {
            return color;
        }

        GpuTextureView depth() {
            return depth;
        }

        int width() {
            return width;
        }

        int height() {
            return height;
        }

        /**
         * @return {@code true}, если с последнего сброса в цель что-то рисовали
         */
        boolean dirty() {
            return dirtyMaxX > dirtyMinX && dirtyMaxY > dirtyMinY;
        }

        int dirtyMinX() {
            return dirtyMinX;
        }

        int dirtyMinY() {
            return dirtyMinY;
        }

        int dirtyMaxX() {
            return dirtyMaxX;
        }

        int dirtyMaxY() {
            return dirtyMaxY;
        }

        /**
         * Сбрасывает изменённую область.
         */
        void resetDirty() {
            dirtyMinX = Integer.MAX_VALUE;
            dirtyMinY = Integer.MAX_VALUE;
            dirtyMaxX = Integer.MIN_VALUE;
            dirtyMaxY = Integer.MIN_VALUE;
        }

        private void include(int minX, int minY, int maxX, int maxY) {
            if (!trackDirty) return;
            minX = Math.max(0, minX);
            minY = Math.max(0, minY);
            maxX = Math.min(width, maxX);
            maxY = Math.min(height, maxY);
            if (maxX <= minX || maxY <= minY) return;
            dirtyMinX = Math.min(dirtyMinX, minX);
            dirtyMinY = Math.min(dirtyMinY, minY);
            dirtyMaxX = Math.max(dirtyMaxX, maxX);
            dirtyMaxY = Math.max(dirtyMaxY, maxY);
        }
    }

    /**
     * Привязка текстуры к сэмплеру шейдера.
     *
     * @param name    имя сэмплера в шейдере ({@code Sampler0} и т.п.)
     * @param view    вид текстуры
     * @param sampler сэмплер
     */
    record TextureBind(String name, GpuTextureView view, GpuSampler sampler) {
    }

    /**
     * Дополнительный uniform-блок отрисовки.
     *
     * @param name  имя блока в шейдере
     * @param slice данные блока
     */
    record UniformBind(String name, GpuBufferSlice slice) {
    }

    private static final class State {
        private Target target;
        private float coordinateWidth;
        private float coordinateHeight;
        private final Matrix4f projectionMatrix = new Matrix4f();
        private GpuBufferSlice projection;
        private boolean scissor;
        private int scissorX;
        private int scissorY;
        private int scissorWidth;
        private int scissorHeight;
    }

    /**
     * Делает цель текущей. Координаты вершин {@code [0..coordinateWidth] x [0..coordinateHeight]} растягиваются
     * на всю цель, ось Y направлена вниз, как в GUI.
     *
     * @param target           цель отрисовки
     * @param coordinateWidth  ширина координатного пространства
     * @param coordinateHeight высота координатного пространства
     */
    static void pushTarget(Target target, float coordinateWidth, float coordinateHeight) {
        if (target == null) throw new IllegalArgumentException("target");
        while (STATES.size() <= depth) STATES.add(new State());
        while (PROJECTION_BUFFERS.size() <= depth) {
            PROJECTION_BUFFERS.add(new ProjectionMatrixBuffer("UniGUI target " + PROJECTION_BUFFERS.size()));
        }

        State state = STATES.get(depth);
        state.target = target;
        state.coordinateWidth = Math.max(1.0e-3f, coordinateWidth);
        state.coordinateHeight = Math.max(1.0e-3f, coordinateHeight);
        float projectionDepth = Math.max(MIN_PROJECTION_DEPTH, Math.max(target.width(), target.height()) * 4.0f);
        state.projectionMatrix.setOrtho(0.0f, state.coordinateWidth, state.coordinateHeight, 0.0f,
                -projectionDepth, projectionDepth, RenderSystem.getDevice().isZZeroToOne());
        state.projection = PROJECTION_BUFFERS.get(depth).getBuffer(state.projectionMatrix);
        state.scissor = false;
        depth++;
    }

    /**
     * Возвращает предыдущую цель отрисовки.
     */
    static void popTarget() {
        if (depth <= 0) throw new IllegalStateException("UniGUI target stack underflow");
        State state = STATES.get(--depth);
        state.target = null;
        state.projection = null;
        state.scissor = false;
    }

    /**
     * @return {@code true}, если есть текущая цель отрисовки
     */
    static boolean hasTarget() {
        return depth > 0;
    }

    /**
     * @return текущая цель отрисовки
     */
    static Target target() {
        return current().target;
    }

    /**
     * @return ширина координатного пространства текущей цели
     */
    static float coordinateWidth() {
        return current().coordinateWidth;
    }

    /**
     * @return высота координатного пространства текущей цели
     */
    static float coordinateHeight() {
        return current().coordinateHeight;
    }

    /**
     * @return ортопроекция текущей цели (только для чтения)
     */
    static Matrix4fc projectionMatrix() {
        return current().projectionMatrix;
    }

    /**
     * Включает scissor текущей цели. Координаты в пикселях цели, начало в левом нижнем углу (как в OpenGL).
     */
    static void setScissor(int x, int y, int width, int height) {
        State state = current();
        state.scissor = true;
        state.scissorX = x;
        state.scissorY = y;
        state.scissorWidth = Math.max(0, width);
        state.scissorHeight = Math.max(0, height);
    }

    /**
     * Выключает scissor текущей цели.
     */
    static void clearScissor() {
        if (depth > 0) current().scissor = false;
    }

    /**
     * @return {@code true}, если у текущей цели включён scissor
     */
    static boolean scissorEnabled() {
        return depth > 0 && current().scissor;
    }

    static int scissorX() {
        return current().scissorX;
    }

    static int scissorY() {
        return current().scissorY;
    }

    static int scissorWidth() {
        return current().scissorWidth;
    }

    static int scissorHeight() {
        return current().scissorHeight;
    }

    /**
     * Задаёт общий множитель цвета последующих отрисовок (аналог {@code RenderSystem.setShaderColor}).
     */
    static void setColorModulator(float red, float green, float blue, float alpha) {
        COLOR_MODULATOR.set(red, green, blue, alpha);
    }

    /**
     * Сбрасывает множитель цвета в белый.
     */
    static void resetColorModulator() {
        COLOR_MODULATOR.set(1.0f, 1.0f, 1.0f, 1.0f);
    }

    /**
     * Очищает цветовой attachment текущей цели прозрачным цветом (и глубину, если она есть).
     */
    static void clearTarget() {
        clear(target());
    }

    /**
     * Очищает цель прозрачным цветом (и глубину, если она есть).
     */
    static void clear(Target target) {
        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
        if (target.depth() != null) {
            encoder.clearColorAndDepthTextures(target.color().texture(), 0, target.depth().texture(), 1.0);
        } else {
            encoder.clearColorTexture(target.color().texture(), 0);
        }
        target.resetDirty();
    }

    /**
     * Отмечает изменённую область текущей цели в координатах цели (с учётом scissor).
     */
    static void markDirty(float minX, float minY, float maxX, float maxY) {
        if (depth <= 0) return;
        State state = current();
        Target target = state.target;
        if (!target.trackDirty) return;
        if (!(minX <= maxX) || !(minY <= maxY) || !Float.isFinite(minX + minY + maxX + maxY)) {
            markDirtyAll();
            return;
        }
        float scaleX = target.width() / state.coordinateWidth;
        float scaleY = target.height() / state.coordinateHeight;
        int x0 = (int) Math.floor(minX * scaleX) - 1;
        int y0 = (int) Math.floor(minY * scaleY) - 1;
        int x1 = (int) Math.ceil(maxX * scaleX) + 1;
        int y1 = (int) Math.ceil(maxY * scaleY) + 1;
        includeClipped(state, x0, y0, x1, y1);
    }

    /**
     * Отмечает изменённой область прямоугольника {@code (x1, y1)-(x2, y2)} после преобразования матрицей.
     */
    static void markDirtyQuad(Matrix4fc matrix, float x1, float y1, float x2, float y2) {
        if (depth <= 0 || !current().target.trackDirty) return;
        float ax = matrix.m00() * x1 + matrix.m10() * y1 + matrix.m30();
        float ay = matrix.m01() * x1 + matrix.m11() * y1 + matrix.m31();
        float bx = matrix.m00() * x2 + matrix.m10() * y1 + matrix.m30();
        float by = matrix.m01() * x2 + matrix.m11() * y1 + matrix.m31();
        float cx = matrix.m00() * x2 + matrix.m10() * y2 + matrix.m30();
        float cy = matrix.m01() * x2 + matrix.m11() * y2 + matrix.m31();
        float dx = matrix.m00() * x1 + matrix.m10() * y2 + matrix.m30();
        float dy = matrix.m01() * x1 + matrix.m11() * y2 + matrix.m31();
        markDirty(Math.min(Math.min(ax, bx), Math.min(cx, dx)), Math.min(Math.min(ay, by), Math.min(cy, dy)),
                Math.max(Math.max(ax, bx), Math.max(cx, dx)), Math.max(Math.max(ay, by), Math.max(cy, dy)));
    }

    /**
     * Накопитель ограничивающего прямоугольника для пакетных отрисовок: точки добавляются по одной, затем
     * область отмечается одним вызовом {@link #commit()}.
     */
    static final class Bounds {
        private float minX;
        private float minY;
        private float maxX;
        private float maxY;

        Bounds() {
            reset();
        }

        void reset() {
            minX = Float.POSITIVE_INFINITY;
            minY = Float.POSITIVE_INFINITY;
            maxX = Float.NEGATIVE_INFINITY;
            maxY = Float.NEGATIVE_INFINITY;
        }

        void add(float x, float y) {
            if (x < minX) minX = x;
            if (x > maxX) maxX = x;
            if (y < minY) minY = y;
            if (y > maxY) maxY = y;
        }

        void commit() {
            if (minX <= maxX && minY <= maxY) markDirty(minX, minY, maxX, maxY);
            reset();
        }
    }

    /**
     * Отмечает всю текущую цель (с учётом scissor) как изменённую.
     */
    static void markDirtyAll() {
        if (depth <= 0) return;
        State state = current();
        includeClipped(state, 0, 0, state.target.width(), state.target.height());
    }

    private static void includeClipped(State state, int x0, int y0, int x1, int y1) {
        Target target = state.target;
        if (state.scissor) {
            int scissorTop = target.height() - (state.scissorY + state.scissorHeight);
            x0 = Math.max(x0, state.scissorX);
            y0 = Math.max(y0, scissorTop);
            x1 = Math.min(x1, state.scissorX + state.scissorWidth);
            y1 = Math.min(y1, scissorTop + state.scissorHeight);
        }
        target.include(x0, y0, x1, y1);
    }

    /**
     * Загружает данные uniform-блока в кольцевой буфер.
     *
     * @param data данные в layout std140 (позиция..limit)
     * @return срез с данными
     */
    static GpuBufferSlice uploadUniform(ByteBuffer data) {
        int alignment = Math.max(16, RenderSystem.getDevice().getUniformOffsetAlignment());
        return UNIFORM_STREAM.write(data, alignment);
    }

    /**
     * Рисует меш в текущую цель без дополнительных привязок и закрывает его.
     */
    static void drawMesh(RenderPipeline pipeline, MeshData mesh) {
        drawMesh(pipeline, mesh, NO_TEXTURES, NO_UNIFORMS);
    }

    /**
     * Рисует меш с одной текстурой {@code Sampler0} и закрывает его.
     */
    static void drawMesh(RenderPipeline pipeline, MeshData mesh, GpuTextureView texture, GpuSampler sampler) {
        drawMesh(pipeline, mesh, List.of(new TextureBind("Sampler0", texture, sampler)), NO_UNIFORMS);
    }

    /**
     * Рисует меш в текущую цель и закрывает его.
     *
     * <p>Позиция должна быть первым элементом формата вершин (так у всех форматов UniGUI): по ней считается
     * изменённая область цели.</p>
     *
     * @param pipeline пайплайн (формат вершин меша должен совпадать с форматом пайплайна)
     * @param mesh     готовый меш или {@code null}
     * @param textures привязки текстур
     * @param uniforms дополнительные uniform-блоки
     */
    static void drawMesh(RenderPipeline pipeline, MeshData mesh, List<TextureBind> textures, List<UniformBind> uniforms) {
        if (mesh == null) return;
        try {
            if (!hasTarget()) return;
            MeshData.DrawState drawState = mesh.drawState();
            VertexFormat format = drawState.format();
            ByteBuffer vertices = mesh.vertexBuffer();
            int vertexSize = format.getVertexSize();
            markMeshDirty(vertices, vertexSize, drawState.vertexCount());
            GpuBufferSlice vertexSlice = VERTEX_STREAM.write(vertices, vertexSize);
            int baseVertex = (int) (vertexSlice.offset() / vertexSize);

            GpuBuffer indexBuffer;
            VertexFormat.IndexType indexType;
            ByteBuffer ownIndices = mesh.indexBuffer();
            if (ownIndices != null) {
                indexBuffer = format.uploadImmediateIndexBuffer(ownIndices);
                indexType = drawState.indexType();
            } else {
                RenderSystem.AutoStorageIndexBuffer sequential = RenderSystem.getSequentialBuffer(drawState.mode());
                indexBuffer = sequential.getBuffer(drawState.indexCount());
                indexType = sequential.type();
            }

            UiPipelines.prepare(pipeline);
            try (RenderPass pass = openPass(pipeline, textures, uniforms)) {
                pass.setVertexBuffer(0, vertexSlice.buffer());
                pass.setIndexBuffer(indexBuffer, indexType);
                pass.drawIndexed(baseVertex, 0, drawState.indexCount(), 1);
            }
        } catch (RuntimeException exception) {
            LOGGER.warn("UniGUI draw failed for pipeline {}: {}", pipeline.getLocation(), exception.toString());
        } finally {
            mesh.close();
        }
    }

    private static void markMeshDirty(ByteBuffer vertices, int vertexSize, int vertexCount) {
        State state = current();
        if (!state.target.trackDirty || vertexCount <= 0) return;
        int base = vertices.position();
        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        for (int i = 0; i < vertexCount; i++) {
            int offset = base + i * vertexSize;
            float x = vertices.getFloat(offset);
            float y = vertices.getFloat(offset + 4);
            if (x < minX) minX = x;
            if (x > maxX) maxX = x;
            if (y < minY) minY = y;
            if (y > maxY) maxY = y;
        }
        markDirty(minX, minY, maxX, maxY);
    }

    /**
     * Открывает проход отрисовки в текущую цель.
     *
     * <p>Всё, что пишет в GPU-буферы (здесь {@code DynamicTransforms}), делается до {@code createRenderPass}:
     * пока проход открыт, {@code CommandEncoder} запрещает любые другие команды. Если настройка прохода всё же
     * падает, проход закрывается здесь же, иначе encoder остаётся «внутри прохода» и все следующие команды
     * (очистки, копирования, отрисовка Minecraft) падают с «Close the existing render pass».</p>
     */
    private static RenderPass openPass(RenderPipeline pipeline, List<TextureBind> textures, List<UniformBind> uniforms) {
        State state = current();
        Target target = state.target;
        GpuBufferSlice transforms = RenderSystem.getDynamicUniforms()
                .writeTransform(IDENTITY, COLOR_MODULATOR, ZERO_OFFSET, IDENTITY);
        RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                () -> "UniGUI " + pipeline.getLocation(),
                target.color(),
                OptionalInt.empty(),
                pipeline.wantsDepthTexture() ? target.depth() : null,
                OptionalDouble.empty());
        try {
            pass.setPipeline(pipeline);
            pass.setUniform("Projection", state.projection);
            pass.setUniform("DynamicTransforms", transforms);
            for (int i = 0, size = uniforms.size(); i < size; i++) {
                UniformBind uniform = uniforms.get(i);
                pass.setUniform(uniform.name(), uniform.slice());
            }
            for (int i = 0, size = textures.size(); i < size; i++) {
                TextureBind texture = textures.get(i);
                pass.bindTexture(texture.name(), texture.view(), texture.sampler());
            }
            if (state.scissor) {
                pass.enableScissor(state.scissorX, state.scissorY, state.scissorWidth, state.scissorHeight);
            } else {
                pass.disableScissor();
            }
            return pass;
        } catch (RuntimeException exception) {
            pass.close();
            throw exception;
        }
    }

    private static State current() {
        if (depth <= 0) throw new IllegalStateException("No UniGUI render target is bound");
        return STATES.get(depth - 1);
    }

    /**
     * Кольцевой GPU-буфер с выравниванием записей. При переполнении запись идёт с начала, при слишком
     * большой записи буфер пересоздаётся.
     */
    private static final class Stream {
        private final String label;
        private final int usage;
        private GpuBuffer buffer;
        private long capacity;
        private long offset;

        private Stream(String label, int usage, long capacity) {
            this.label = label;
            this.usage = usage;
            this.capacity = capacity;
        }

        private GpuBufferSlice write(ByteBuffer data, int alignment) {
            int length = data.remaining();
            if (length <= 0) throw new IllegalArgumentException("Empty GPU upload");
            if (buffer == null || buffer.isClosed() || length > capacity) {
                if (buffer != null && !buffer.isClosed()) buffer.close();
                while (capacity < length) capacity *= 2;
                buffer = RenderSystem.getDevice().createBuffer(() -> label, usage, capacity);
                offset = 0;
            }
            long start = align(offset, alignment);
            if (start + length > capacity) start = 0;
            GpuBufferSlice slice = buffer.slice(start, length);
            RenderSystem.getDevice().createCommandEncoder().writeToBuffer(slice, data);
            offset = start + length;
            return slice;
        }

        private static long align(long value, int alignment) {
            long remainder = value % alignment;
            return remainder == 0 ? value : value + alignment - remainder;
        }
    }
}
