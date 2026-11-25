package dev.sixik.unigui.backend.minecraft_impl;

import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.pipeline.IndexType;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.backend.opengl.GlConst;
import com.mojang.renderpearl.backend.opengl.GlStateManager;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.CompiledRenderPipeline;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.renderpearl.api.pipeline.BlendFactor;
import com.mojang.renderpearl.api.commands.CommandEncoder;
import com.mojang.renderpearl.api.commands.GpuQueryPool;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.textures.GpuTexture;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.renderpearl.api.vertex.VertexFormat;
import com.mojang.renderpearl.api.vertex.VertexFormatElement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector4f;

import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalLong;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Места GPU-API, которые отличаются между версиями 26.x. Это реализация для Minecraft 26.3.
 *
 * <p>В 26.3 GPU-API переехал из {@code com.mojang.blaze3d} в {@code com.mojang.renderpearl} (общий код линейки
 * компилируется с переименованными пакетами, см. {@code versions/26.3/package-remap.properties}), проход принимает
 * скомпилированный пайплайн, текстуры привязываются через {@code setUniform}, группа проекции называется
 * {@code PROJECTION}, а формат глубины цели задаётся явно.</p>
 *
 * <p>Что поменялось в 26.2: {@code VertexFormat.Mode} стал {@link PrimitiveTopology}, {@code TextureFormat} —
 * {@link GpuFormat}, {@code SourceFactor}/{@code DestFactor} — {@link BlendFactor}; униформы пайплайна задаются
 * группами ({@link BindGroupLayouts}), {@code Tesselator} убран, очистка принимает цвет вектором, а замер времени
 * GPU идёт через пул timestamp-запросов.</p>
 *
 * <p>Минимальная поддержка Vulkan-бэкенда 26.2 ограничена проходами {@link UiGpu}; прямой GL-путь ({@link UiGl})
 * работает только на OpenGL-бэкенде, который выбран по умолчанию.</p>
 */
final class MinecraftGpuCompat {
    /** Аддитивное смешивание: {@code rgb += src.rgb*a}, alpha насыщается. */
    static final BlendFunction ADDITIVE = new BlendFunction(BlendFactor.SRC_ALPHA, BlendFactor.ONE, BlendFactor.ONE, BlendFactor.ONE);

    /** Прозрачный цвет очистки. */
    private static final Vector4f TRANSPARENT = new Vector4f(0.0f, 0.0f, 0.0f, 0.0f);
    /** Общий буфер сборки вершин (замена {@code Tesselator}, которого в 26.2 нет). */
    private static final ByteBufferBuilder SHARED_BYTES = new ByteBufferBuilder(786432);
    /** Пулы timestamp-запросов на два значения (начало и конец замера), готовые к повторному использованию. */
    private static final ArrayDeque<GpuQueryPool> FREE_TIMER_POOLS = new ArrayDeque<>();

    private MinecraftGpuCompat() {
    }

    // --- Сборка вершин ---------------------------------------------------------------------------------------

    /**
     * Начинает сборку вершин в общем буфере. Как и {@code Tesselator} в 26.1, предыдущий меш должен быть
     * закрыт до начала следующей сборки.
     */
    static BufferBuilder begin(UiPrimitive primitive, VertexFormat format) {
        return new BufferBuilder(SHARED_BYTES, topology(primitive), format);
    }

    /**
     * Начинает сборку вершин в собственном буфере.
     */
    static BufferBuilder builder(ByteBufferBuilder bytes, UiPrimitive primitive, VertexFormat format) {
        return new BufferBuilder(bytes, topology(primitive), format);
    }

    /**
     * @return вид примитивов меша
     */
    static UiPrimitive primitive(MeshData mesh) {
        return mesh.drawState().primitiveTopology() == PrimitiveTopology.QUADS ? UiPrimitive.QUADS : UiPrimitive.TRIANGLES;
    }

    private static PrimitiveTopology topology(UiPrimitive primitive) {
        return primitive == UiPrimitive.QUADS ? PrimitiveTopology.QUADS : PrimitiveTopology.TRIANGLES;
    }

    // --- Пайплайны и проходы -----------------------------------------------------------------------------------

    /**
     * Пайплайн UniGUI на ванильном шейдере: группы униформ {@code Globals}, {@code DynamicTransforms} и
     * {@code Projection} (как у ванильных GUI-пайплайнов), без глубины и отсечения граней.
     *
     * @param function функция смешивания или {@code null} (без смешивания)
     */
    static RenderPipeline pipeline(Identifier location, String shader, boolean textured, VertexFormat format,
                                   UiPrimitive primitive, @Nullable BlendFunction function) {
        RenderPipeline.Builder builder = RenderPipeline.builder()
                .withLocation(location)
                .withBindGroupLayout(BindGroupLayouts.GLOBALS)
                // В 26.3 DynamicTransforms и Projection — отдельные группы (в 26.2 — общая MATRICES_PROJECTION).
                .withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
                .withBindGroupLayout(BindGroupLayouts.PROJECTION)
                .withVertexShader(shader)
                .withFragmentShader(shader)
                .withColorTargetState(function == null ? ColorTargetState.DEFAULT : new ColorTargetState(function))
                .withCull(false)
                .withVertexBinding(0, format)
                .withPrimitiveTopology(topology(primitive));
        if (textured) builder.withBindGroupLayout(BindGroupLayouts.SAMPLER0);
        return builder.build();
    }

    /**
     * @return {@code true}, если глубина клипа {@code [0, 1]} (иначе {@code [-1, 1]})
     */
    static boolean zZeroToOne() {
        return RenderSystem.getDevice().getDeviceInfo().isZZeroToOne();
    }

    /**
     * @return выравнивание смещения uniform-блока в буфере
     */
    static int uniformAlignment() {
        return RenderSystem.getDevice().getDeviceInfo().limits().minUniformOffsetAlignment();
    }

    /**
     * Очищает цвет прозрачным, глубину (если есть) дальним значением.
     */
    static void clear(GpuTexture color, @Nullable GpuTexture depth) {
        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
        if (depth != null) {
            encoder.clearColorAndDepthTextures(color, TRANSPARENT, depth, RenderSystem.DEFAULT_DEPTH_CLEAR_VALUE);
        } else {
            encoder.clearColorTexture(color, TRANSPARENT);
        }
    }

    /**
     * Открывает проход без очистки.
     */
    static RenderPass openPass(Supplier<String> label, GpuTextureView color, @Nullable GpuTextureView depth) {
        return RenderSystem.getDevice().createCommandEncoder()
                .createRenderPass(label, color, Optional.empty(), depth, OptionalDouble.empty());
    }

    /**
     * Ставит пайплайн прохода. В 26.3 проход принимает только скомпилированный пайплайн: свои GLSL-пайплайны
     * UniGUI компилирует {@link UiCustomPipeline}, остальные (на ванильных шейдерах) берутся из кэша Minecraft.
     */
    static void setPipeline(RenderPass pass, RenderPipeline pipeline) {
        CompiledRenderPipeline compiled = UiCustomPipeline.compiled(pipeline);
        pass.setPipeline(compiled != null ? compiled : RenderSystem.getCompiledPipeline(pipeline));
    }

    /**
     * Привязывает текстуру с сэмплером к сэмплеру шейдера {@code name}.
     */
    static void bindTexture(RenderPass pass, String name, com.mojang.renderpearl.api.textures.GpuTextureView view, com.mojang.renderpearl.api.textures.GpuSampler sampler) {
        pass.setUniform(name, view, sampler);
    }

    /**
     * Привязывает униформы, которые нужны ванильным шейдерам, кроме {@code Projection} и
     * {@code DynamicTransforms}: в 26.2 это блок {@code Globals}.
     */
    static void bindDefaultUniforms(RenderPass pass) {
        GpuBuffer globals = RenderSystem.getGlobalSettingsUniform();
        if (globals != null) pass.setUniform("Globals", globals);
    }

    /**
     * Рисует меш, вершины которого уже загружены в буфер.
     *
     * @param vertices   срез с вершинами меша (смещение кратно размеру вершины)
     * @param vertexSize размер вершины в байтах
     */
    static void drawMesh(RenderPass pass, GpuBufferSlice vertices, int vertexSize, MeshData mesh) {
        MeshData.DrawState drawState = mesh.drawState();
        GpuBuffer indexBuffer;
        IndexType indexType;
        ByteBuffer ownIndices = mesh.indexBuffer();
        if (ownIndices != null) {
            // Свои индексы бывают только у отсортированных мешей; буфер освобождается, когда GPU его прочитает.
            GpuBuffer upload = RenderSystem.getDevice().createBuffer(() -> "UniGUI immediate indices",
                    GpuBuffer.USAGE_INDEX, ownIndices);
            RenderSystem.queueFencedTask(upload::close);
            indexBuffer = upload;
            indexType = drawState.indexType();
        } else {
            RenderSystem.AutoStorageIndexBuffer sequential = RenderSystem.getSequentialBuffer(drawState.primitiveTopology());
            indexBuffer = sequential.getBuffer(drawState.indexCount());
            indexType = sequential.type();
        }
        pass.setVertexBuffer(0, vertices.buffer().slice());
        pass.setIndexBuffer(indexBuffer, indexType);
        pass.drawIndexed(drawState.indexCount(), 1, 0, (int) (vertices.offset() / vertexSize), 0);
    }

    // --- Текстуры и цели -----------------------------------------------------------------------------------------

    /**
     * Создаёт RGBA8-текстуру.
     */
    static GpuTexture createRgbaTexture(Supplier<String> label, int usage, int width, int height) {
        return RenderSystem.getDevice().createTexture(label, usage, GpuFormat.RGBA8_UNORM, width, height, 1, 1);
    }

    /**
     * Создаёт одноканальную 8-битную текстуру (атлас SDF-глифов), в которую можно писать и из которой можно
     * читать в шейдере.
     */
    static GpuTexture createR8Texture(Supplier<String> label, int width, int height) {
        return RenderSystem.getDevice().createTexture(label,
                GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_COPY_DST, GpuFormat.R8_UNORM, width, height, 1, 1);
    }

    /**
     * Записывает прямоугольник одноканальных данных (плотные строки по {@code width} байт) в текстуру.
     */
    static void writeR8(GpuTexture texture, ByteBuffer data, int x, int y, int width, int height) {
        RenderSystem.getDevice().createCommandEncoder().writeToTexture(texture, data, 0, 0, x, y, width, height);
    }

    /**
     * Создаёт offscreen-цель с RGBA8-цветом.
     */
    static TextureTarget textureTarget(String label, int width, int height, boolean useDepth) {
        return new TextureTarget(label, width, height, GpuFormat.RGBA8_UNORM, useDepth ? GpuFormat.D32_FLOAT : null);
    }

    /**
     * @return основная цель отрисовки окна
     */
    static RenderTarget mainRenderTarget() {
        return Minecraft.getInstance().gameRenderer.mainRenderTarget();
    }

    /**
     * Читает буфер после завершения копирования в него (вызывается из колбэка {@code copyTextureToBuffer}).
     */
    static void readBuffer(CommandEncoder encoder, GpuBuffer buffer, Consumer<ByteBuffer> reader) {
        try (GpuBufferSlice.MappedView view = buffer.map(true, false)) {
            reader.accept(view.data());
        }
    }

    // --- GPU-таймер --------------------------------------------------------------------------------------------

    /**
     * Начинает замер времени GPU: пишет timestamp начала в пул из двух запросов.
     *
     * @return замер или {@code null}, если замер недоступен
     */
    static @Nullable Object beginTimer() {
        GpuQueryPool pool = FREE_TIMER_POOLS.poll();
        if (pool == null) pool = RenderSystem.getDevice().createTimestampQueryPool(2);
        RenderSystem.getDevice().createCommandEncoder().writeTimestamp(pool, 0);
        return pool;
    }

    /**
     * Заканчивает замер: пишет timestamp конца.
     */
    static void endTimer(Object timer) {
        RenderSystem.getDevice().createCommandEncoder().writeTimestamp((GpuQueryPool) timer, 1);
    }

    /**
     * @return время замера в наносекундах или пусто, если результат ещё не готов
     */
    static OptionalLong timerNanos(Object timer) {
        OptionalLong[] values = ((GpuQueryPool) timer).getValues(0, 2);
        if (values[0].isEmpty() || values[1].isEmpty()) return OptionalLong.empty();
        long ticks = values[1].getAsLong() - values[0].getAsLong();
        return OptionalLong.of((long) (ticks * RenderSystem.getDevice().getDeviceInfo().timestampPeriod()));
    }

    /**
     * Возвращает пул замера для повторного использования.
     */
    static void closeTimer(Object timer) {
        GpuQueryPool pool = (GpuQueryPool) timer;
        if (FREE_TIMER_POOLS.size() < 8) {
            FREE_TIMER_POOLS.add(pool);
        } else {
            pool.close();
        }
    }

    // --- Прямой OpenGL (UiGl) ------------------------------------------------------------------------------------

    /** Кэш {@link #openGl()}: бэкенд выбирается при запуске и не меняется до перезапуска. */
    private static Boolean openGl;

    /**
     * @return {@code true}, если бэкенд — OpenGL (прямой GL-путь {@link UiGl} доступен). В 26.2 бэкенд может
     * быть Vulkan ({@code --graphicsBackend vulkan} или настройка графического API).
     */
    static boolean openGl() {
        Boolean value = openGl;
        if (value == null) {
            value = "OpenGL".equals(RenderSystem.getDevice().getDeviceInfo().backendName());
            openGl = value;
        }
        return value;
    }

    /**
     * Копирует прямоугольник цветовой текстуры (координаты GL: строки снизу вверх). На OpenGL — через
     * {@link UiGl#copyColor}, на Vulkan — через {@code CommandEncoder.copyTextureToTexture}.
     */
    static void copyColor(GpuTexture source, GpuTexture destination, int destX, int destY,
                          int sourceX, int sourceY, int width, int height) {
        if (openGl()) {
            UiGl.copyColor(source, destination, destX, destY, sourceX, sourceY, width, height);
        } else if (width > 0 && height > 0) {
            RenderSystem.getDevice().createCommandEncoder().copyTextureToTexture(source, destination, 0,
                    destX, destY, sourceX, sourceY, width, height);
        }
    }

    /**
     * Включает смешивание по функции ({@code null} выключает).
     */
    static void glBlend(@Nullable BlendFunction function) {
        if (function == null) {
            GlStateManager._disableBlend(0);
            return;
        }
        GlStateManager._enableBlend(0);
        GlStateManager._blendFuncSeparate(
                GlConst.toGl(function.color().sourceFactor()), GlConst.toGl(function.color().destFactor()),
                GlConst.toGl(function.alpha().sourceFactor()), GlConst.toGl(function.alpha().destFactor()));
    }

    /**
     * Настраивает атрибуты вершин текущего VAO: элемент {@code i} формата идёт в location {@code i}.
     */
    static void glVertexAttributes(VertexFormat format) {
        List<VertexFormatElement> elements = format.getElements();
        int stride = format.getVertexSize();
        for (int i = 0; i < elements.size(); i++) {
            VertexFormatElement element = elements.get(i);
            int externalId = GlConst.toGlExternalId(element.format());
            int type = GlConst.toGlType(element.format());
            int channels = GlConst.glFormatChannelCount(externalId);
            GlStateManager._enableVertexAttribArray(i);
            if (GlConst.isGlFormatInteger(externalId)) {
                GlStateManager._vertexAttribIPointer(i, channels, type, stride, element.offset());
            } else {
                GlStateManager._vertexAttribPointer(i, channels, type, GlConst.isFormatNormalized(element.format()),
                        stride, element.offset());
            }
        }
    }
}
