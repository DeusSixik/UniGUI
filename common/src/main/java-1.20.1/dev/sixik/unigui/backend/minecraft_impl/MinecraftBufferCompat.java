package dev.sixik.unigui.backend.minecraft_impl;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.MultiBufferSource;
import org.joml.Matrix4f;

final class MinecraftBufferCompat {
    private MinecraftBufferCompat() {
    }

    static Object begin(VertexFormat.Mode mode, VertexFormat format) {
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(mode, format);
        return buffer;
    }

    static MultiBufferSource.BufferSource immediate(int initialCapacity) {
        return MultiBufferSource.immediate(new BufferBuilder(initialCapacity));
    }

    static void draw(Object buffer) {
        BufferUploader.draw(((BufferBuilder) buffer).end());
    }

    static void drawWithShader(Object buffer) {
        BufferUploader.drawWithShader(((BufferBuilder) buffer).end());
    }

    static void vertex(Object buffer, Matrix4f matrix, float x, float y, float z) {
        VertexConsumer consumer = ((BufferBuilder) buffer).vertex(matrix, x, y, z);
        consumer.endVertex();
    }

    static void colorVertex(Object buffer, Matrix4f matrix, float x, float y, int argb) {
        VertexConsumer consumer = ((BufferBuilder) buffer).vertex(matrix, x, y, 0.0f);
        consumer.color(red(argb), green(argb), blue(argb), alpha(argb));
        consumer.endVertex();
    }

    static void textureVertex(Object buffer, Matrix4f matrix, float x, float y, float u, float v) {
        VertexConsumer consumer = ((BufferBuilder) buffer).vertex(matrix, x, y, 0.0f);
        consumer.uv(u, v);
        consumer.endVertex();
    }

    static void textureColorVertex(Object buffer, Matrix4f matrix, float x, float y, float u, float v, int argb) {
        VertexConsumer consumer = ((BufferBuilder) buffer).vertex(matrix, x, y, 0.0f);
        consumer.uv(u, v);
        consumer.color(red(argb), green(argb), blue(argb), alpha(argb));
        consumer.endVertex();
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
