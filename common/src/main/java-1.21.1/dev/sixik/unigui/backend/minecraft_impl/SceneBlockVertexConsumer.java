package dev.sixik.unigui.backend.minecraft_impl;

import com.mojang.blaze3d.vertex.VertexConsumer;

/**
 * Преобразует вызовы VertexConsumer в данные запекаемого меша.
 */
final class SceneBlockVertexConsumer extends LegacyBlockVertices implements VertexConsumer {
    /**
     * {@inheritDoc}
     */
    @Override
    public VertexConsumer addVertex(float x, float y, float z) {
        position(x, y, z);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public VertexConsumer setColor(int r, int g, int b, int a) {
        rgba(r, g, b, a);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public VertexConsumer setUv(float u, float v) {
        tex(u, v);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public VertexConsumer setUv1(int u, int v) {
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public VertexConsumer setUv2(int u, int v) {
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public VertexConsumer setNormal(float x, float y, float z) {
        return this;
    }
}
