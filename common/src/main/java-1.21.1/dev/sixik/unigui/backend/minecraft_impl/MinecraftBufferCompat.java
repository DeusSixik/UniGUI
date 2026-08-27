package dev.sixik.unigui.backend.minecraft_impl;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.MultiBufferSource;
import org.joml.Matrix4f;

final class MinecraftBufferCompat {
    private MinecraftBufferCompat() {
    }

    static Object begin(VertexFormat.Mode mode, VertexFormat format) {
        return Tesselator.getInstance().begin(mode, format);
    }

    static MultiBufferSource.BufferSource immediate(int initialCapacity) {
        return MultiBufferSource.immediate(new ByteBufferBuilder(initialCapacity));
    }

    static void draw(Object buffer) {
        MeshData mesh = ((BufferBuilder) buffer).build();
        if (mesh != null) {
            BufferUploader.draw(mesh);
        }
    }

    static void drawWithShader(Object buffer) {
        MeshData mesh = ((BufferBuilder) buffer).build();
        if (mesh != null) {
            BufferUploader.drawWithShader(mesh);
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
