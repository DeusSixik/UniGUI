package dev.sixik.unigui.backend.minecraft_impl;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.DestFactor;
import com.mojang.blaze3d.platform.SourceFactor;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuQuery;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Места GPU-API, которые отличаются между 26.1 и 26.2. Это реализация для Minecraft 26.1.
 *
 * <p>Остальной рендер линейки 26.x (src/main/java-26) общий и обращается к этим API только через этот класс.</p>
 */
final class MinecraftGpuCompat {
    /** Аддитивное смешивание: {@code rgb += src.rgb*a}, alpha насыщается. */
    static final BlendFunction ADDITIVE = new BlendFunction(SourceFactor.SRC_ALPHA, DestFactor.ONE, SourceFactor.ONE, DestFactor.ONE);

    private MinecraftGpuCompat() {
    }

    // --- Сборка вершин ---------------------------------------------------------------------------------------

    /**
     * Начинает сборку вершин в общем буфере ({@code Tesselator}).
     */
    static BufferBuilder begin(UiPrimitive primitive, VertexFormat format) {
        return Tesselator.getInstance().begin(mode(primitive), format);
    }

    /**
     * Начинает сборку вершин в собственном буфере.
     */
    static BufferBuilder builder(ByteBufferBuilder bytes, UiPrimitive primitive, VertexFormat format) {
        return new BufferBuilder(bytes, mode(primitive), format);
    }

    /**
     * @return вид примитивов меша
     */
    static UiPrimitive primitive(MeshData mesh) {
        return mesh.drawState().mode() == VertexFormat.Mode.QUADS ? UiPrimitive.QUADS : UiPrimitive.TRIANGLES;
    }

    private static VertexFormat.Mode mode(UiPrimitive primitive) {
        return primitive == UiPrimitive.QUADS ? VertexFormat.Mode.QUADS : VertexFormat.Mode.TRIANGLES;
    }

    // --- Пайплайны и проходы -----------------------------------------------------------------------------------

    /**
     * Пайплайн UniGUI на ванильном шейдере: униформы {@code DynamicTransforms} и {@code Projection}, без глубины и
     * отсечения граней.
     *
     * @param function функция смешивания или {@code null} (без смешивания)
     */
    static RenderPipeline pipeline(Identifier location, String shader, boolean textured, VertexFormat format,
                                   UiPrimitive primitive, @Nullable BlendFunction function) {
        RenderPipeline.Builder builder = RenderPipeline.builder()
                .withLocation(location)
                .withVertexShader(shader)
                .withFragmentShader(shader)
                .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
                .withUniform("Projection", UniformType.UNIFORM_BUFFER)
                .withColorTargetState(function == null ? ColorTargetState.DEFAULT : new ColorTargetState(function))
                .withCull(false)
                .withVertexFormat(format, mode(primitive));
        if (textured) builder.withSampler("Sampler0");
        return builder.build();
    }

    /**
     * @return {@code true}, если глубина клипа {@code [0, 1]} (иначе {@code [-1, 1]})
     */
    static boolean zZeroToOne() {
        return RenderSystem.getDevice().isZZeroToOne();
    }

    /**
     * @return выравнивание смещения uniform-блока в буфере
     */
    static int uniformAlignment() {
        return RenderSystem.getDevice().getUniformOffsetAlignment();
    }

    /**
     * Очищает цвет прозрачным, глубину (если есть) дальним значением.
     */
    static void clear(GpuTexture color, @Nullable GpuTexture depth) {
        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
        if (depth != null) {
            encoder.clearColorAndDepthTextures(color, 0, depth, 1.0);
        } else {
            encoder.clearColorTexture(color, 0);
        }
    }

    /**
     * Открывает проход без очистки.
     */
    static RenderPass openPass(Supplier<String> label, GpuTextureView color, @Nullable GpuTextureView depth) {
        return RenderSystem.getDevice().createCommandEncoder()
                .createRenderPass(label, color, OptionalInt.empty(), depth, OptionalDouble.empty());
    }

    /**
     * Привязывает униформы, которые нужны ванильным шейдерам, кроме {@code Projection} и
     * {@code DynamicTransforms}. В 26.1 таких нет.
     */
    static void bindDefaultUniforms(RenderPass pass) {
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
        VertexFormat.IndexType indexType;
        ByteBuffer ownIndices = mesh.indexBuffer();
        if (ownIndices != null) {
            indexBuffer = drawState.format().uploadImmediateIndexBuffer(ownIndices);
            indexType = drawState.indexType();
        } else {
            RenderSystem.AutoStorageIndexBuffer sequential = RenderSystem.getSequentialBuffer(drawState.mode());
            indexBuffer = sequential.getBuffer(drawState.indexCount());
            indexType = sequential.type();
        }
        pass.setVertexBuffer(0, vertices.buffer());
        pass.setIndexBuffer(indexBuffer, indexType);
        pass.drawIndexed((int) (vertices.offset() / vertexSize), 0, drawState.indexCount(), 1);
    }

    // --- Текстуры и цели -----------------------------------------------------------------------------------------

    /**
     * Создаёт RGBA8-текстуру.
     */
    static GpuTexture createRgbaTexture(Supplier<String> label, int usage, int width, int height) {
        return RenderSystem.getDevice().createTexture(label, usage, TextureFormat.RGBA8, width, height, 1, 1);
    }

    /**
     * Создаёт одноканальную 8-битную текстуру (атлас SDF-глифов), в которую можно писать и из которой можно
     * читать в шейдере.
     */
    static GpuTexture createR8Texture(Supplier<String> label, int width, int height) {
        return RenderSystem.getDevice().createTexture(label,
                GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_COPY_DST, TextureFormat.RED8, width, height, 1, 1);
    }

    /**
     * Записывает прямоугольник одноканальных данных (плотные строки по {@code width} байт) в текстуру.
     */
    static void writeR8(GpuTexture texture, ByteBuffer data, int x, int y, int width, int height) {
        RenderSystem.getDevice().createCommandEncoder().writeToTexture(texture, data,
                com.mojang.blaze3d.platform.NativeImage.Format.LUMINANCE, 0, 0, x, y, width, height);
    }

    /**
     * Создаёт offscreen-цель с RGBA8-цветом.
     */
    static TextureTarget textureTarget(String label, int width, int height, boolean useDepth) {
        return new TextureTarget(label, width, height, useDepth);
    }

    /**
     * @return основная цель отрисовки окна
     */
    static RenderTarget mainRenderTarget() {
        return Minecraft.getInstance().getMainRenderTarget();
    }

    /**
     * Читает буфер после завершения копирования в него (вызывается из колбэка {@code copyTextureToBuffer}).
     */
    static void readBuffer(CommandEncoder encoder, GpuBuffer buffer, Consumer<ByteBuffer> reader) {
        try (GpuBuffer.MappedView view = encoder.mapBuffer(buffer, true, false)) {
            reader.accept(view.data());
        }
    }

    // --- GPU-таймер --------------------------------------------------------------------------------------------

    /**
     * Начинает замер времени GPU.
     *
     * @return замер или {@code null}, если замер недоступен
     */
    static @Nullable Object beginTimer() {
        return RenderSystem.getDevice().createCommandEncoder().timerQueryBegin();
    }

    /**
     * Заканчивает замер.
     */
    static void endTimer(Object timer) {
        RenderSystem.getDevice().createCommandEncoder().timerQueryEnd((GpuQuery) timer);
    }

    /**
     * @return время замера в наносекундах или пусто, если результат ещё не готов
     */
    static OptionalLong timerNanos(Object timer) {
        return ((GpuQuery) timer).getValue();
    }

    /**
     * Освобождает замер.
     */
    static void closeTimer(Object timer) {
        ((GpuQuery) timer).close();
    }

    // --- Прямой OpenGL (UiGl) ------------------------------------------------------------------------------------

    /**
     * @return всегда {@code true}: в 26.1 есть только OpenGL-бэкенд
     */
    static boolean openGl() {
        return true;
    }

    /**
     * Копирует прямоугольник цветовой текстуры (координаты GL: строки снизу вверх) через {@link UiGl#copyColor}.
     */
    static void copyColor(GpuTexture source, GpuTexture destination, int destX, int destY,
                          int sourceX, int sourceY, int width, int height) {
        UiGl.copyColor(source, destination, destX, destY, sourceX, sourceY, width, height);
    }

    /**
     * Включает смешивание по функции ({@code null} выключает).
     */
    static void glBlend(@Nullable BlendFunction function) {
        if (function == null) {
            GlStateManager._disableBlend();
            return;
        }
        GlStateManager._enableBlend();
        GlStateManager._blendFuncSeparate(
                source(function.sourceColor()), destination(function.destColor()),
                source(function.sourceAlpha()), destination(function.destAlpha()));
    }

    /**
     * Настраивает атрибуты вершин текущего VAO: элемент {@code i} формата идёт в location {@code i}.
     */
    static void glVertexAttributes(VertexFormat format) {
        List<VertexFormatElement> elements = format.getElements();
        int stride = format.getVertexSize();
        for (int i = 0; i < elements.size(); i++) {
            VertexFormatElement element = elements.get(i);
            GlStateManager._enableVertexAttribArray(i);
            int type = glType(element.type());
            if (!element.normalized() && element.type() != VertexFormatElement.Type.FLOAT) {
                GlStateManager._vertexAttribIPointer(i, element.count(), type, stride, format.getOffset(element));
            } else {
                GlStateManager._vertexAttribPointer(i, element.count(), type, element.normalized(), stride,
                        format.getOffset(element));
            }
        }
    }

    private static int glType(VertexFormatElement.Type type) {
        return switch (type) {
            case FLOAT -> GL11.GL_FLOAT;
            case UBYTE -> GL11.GL_UNSIGNED_BYTE;
            case BYTE -> GL11.GL_BYTE;
            case USHORT -> GL11.GL_UNSIGNED_SHORT;
            case SHORT -> GL11.GL_SHORT;
            case UINT -> GL11.GL_UNSIGNED_INT;
            case INT -> GL11.GL_INT;
        };
    }

    private static int source(SourceFactor factor) {
        return switch (factor) {
            case CONSTANT_ALPHA -> 32771;
            case CONSTANT_COLOR -> 32769;
            case DST_ALPHA -> GL11.GL_DST_ALPHA;
            case DST_COLOR -> GL11.GL_DST_COLOR;
            case ONE -> GL11.GL_ONE;
            case ONE_MINUS_CONSTANT_ALPHA -> 32772;
            case ONE_MINUS_CONSTANT_COLOR -> 32770;
            case ONE_MINUS_DST_ALPHA -> GL11.GL_ONE_MINUS_DST_ALPHA;
            case ONE_MINUS_DST_COLOR -> GL11.GL_ONE_MINUS_DST_COLOR;
            case ONE_MINUS_SRC_ALPHA -> GL11.GL_ONE_MINUS_SRC_ALPHA;
            case ONE_MINUS_SRC_COLOR -> GL11.GL_ONE_MINUS_SRC_COLOR;
            case SRC_ALPHA -> GL11.GL_SRC_ALPHA;
            case SRC_ALPHA_SATURATE -> GL11.GL_SRC_ALPHA_SATURATE;
            case SRC_COLOR -> GL11.GL_SRC_COLOR;
            case ZERO -> GL11.GL_ZERO;
        };
    }

    private static int destination(DestFactor factor) {
        return switch (factor) {
            case CONSTANT_ALPHA -> 32771;
            case CONSTANT_COLOR -> 32769;
            case DST_ALPHA -> GL11.GL_DST_ALPHA;
            case DST_COLOR -> GL11.GL_DST_COLOR;
            case ONE -> GL11.GL_ONE;
            case ONE_MINUS_CONSTANT_ALPHA -> 32772;
            case ONE_MINUS_CONSTANT_COLOR -> 32770;
            case ONE_MINUS_DST_ALPHA -> GL11.GL_ONE_MINUS_DST_ALPHA;
            case ONE_MINUS_DST_COLOR -> GL11.GL_ONE_MINUS_DST_COLOR;
            case ONE_MINUS_SRC_ALPHA -> GL11.GL_ONE_MINUS_SRC_ALPHA;
            case ONE_MINUS_SRC_COLOR -> GL11.GL_ONE_MINUS_SRC_COLOR;
            case SRC_ALPHA -> GL11.GL_SRC_ALPHA;
            case SRC_COLOR -> GL11.GL_SRC_COLOR;
            case ZERO -> GL11.GL_ZERO;
        };
    }
}
