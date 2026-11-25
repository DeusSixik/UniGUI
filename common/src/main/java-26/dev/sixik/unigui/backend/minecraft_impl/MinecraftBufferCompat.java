package dev.sixik.unigui.backend.minecraft_impl;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.joml.Matrix4f;

/**
 * Сборка и отрисовка вершин для Minecraft 26.1.
 *
 * <p>Сборка такая же, как в 1.21 ({@link BufferBuilder}; общий буфер даёт {@link MinecraftGpuCompat#begin}). Отрисовка другая: {@code BufferUploader}
 * в 26.1 нет, поэтому меш уходит либо в {@link UiGpu} с пайплайном на ванильном шейдере, либо в {@link UiGl} для
 * собственной GL-программы UniGUI.</p>
 */
final class MinecraftBufferCompat {
    private MinecraftBufferCompat() {
    }

    static Object begin(UiPrimitive primitive, VertexFormat format) {
        return MinecraftGpuCompat.begin(primitive, format);
    }

    /**
     * Рисует цветную геометрию ({@code POSITION_COLOR}).
     */
    static void drawColor(Object buffer, MinecraftUiBlend blend) {
        MeshData mesh = ((BufferBuilder) buffer).build();
        if (mesh != null) {
            UiGpu.drawMesh(UiPipelines.color(blend, MinecraftGpuCompat.primitive(mesh)), mesh);
        }
    }

    /**
     * Рисует текстурированную геометрию ({@code POSITION_TEX_COLOR}).
     */
    static void drawTexture(Object buffer, MinecraftUiBlend blend, UiTextureBinding binding) {
        MeshData mesh = ((BufferBuilder) buffer).build();
        if (mesh != null) {
            UiGpu.drawMesh(UiPipelines.texture(blend, MinecraftGpuCompat.primitive(mesh)), mesh, binding.view(), binding.sampler());
        }
    }

    /**
     * Завершает сборку и отдаёт меш (или {@code null}, если вершин нет); закрывает его получатель.
     */
    static MeshData build(Object buffer) {
        return ((BufferBuilder) buffer).build();
    }

    /**
     * Рисует геометрию текущей GL-программой (внутри {@link UiGl#begin()}).
     */
    static void draw(Object buffer) {
        MeshData mesh = ((BufferBuilder) buffer).build();
        if (mesh != null) {
            UiGl.drawImmediate(mesh);
        }
    }

    static void vertex(Object buffer, Matrix4f matrix, float x, float y, float z) {
        ((BufferBuilder) buffer).addVertex(matrix, x, y, z);
    }

    static void colorVertex(Object buffer, Matrix4f matrix, float x, float y, int argb) {
        VertexConsumer consumer = ((BufferBuilder) buffer).addVertex(matrix, x, y, 0.0f);
        consumer.setColor(red(argb), green(argb), blue(argb), alpha(argb));
    }

    static void textureVertex(Object buffer, Matrix4f matrix, float x, float y, float u, float v) {
        VertexConsumer consumer = ((BufferBuilder) buffer).addVertex(matrix, x, y, 0.0f);
        consumer.setUv(u, v);
    }

    static void textureColorVertex(Object buffer, Matrix4f matrix, float x, float y, float u, float v, int argb) {
        VertexConsumer consumer = ((BufferBuilder) buffer).addVertex(matrix, x, y, 0.0f);
        consumer.setUv(u, v);
        consumer.setColor(red(argb), green(argb), blue(argb), alpha(argb));
    }

    private static int alpha(int argb) {
        return (argb >>> 24) & 0xFF;
    }

    private static int red(int argb) {
        return (argb >>> 16) & 0xFF;
    }

    private static int green(int argb) {
        return (argb >>> 8) & 0xFF;
    }

    private static int blue(int argb) {
        return argb & 0xFF;
    }
}
