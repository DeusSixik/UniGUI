package dev.sixik.unigui.backend.minecraft_impl;

import com.mojang.blaze3d.vertex.VertexConsumer;

/**
 * Преобразует вызовы VertexConsumer в данные запекаемого меша.
 */
final class SceneBlockVertexConsumer extends LegacyBlockVertices implements VertexConsumer {
    /**
     * Цвет, подставляемый при начале следующей вершины.
     */
    private int red = 255, green = 255, blue = 255, alpha = 255;

    /**
     * {@inheritDoc}
     */
    @Override
    public VertexConsumer vertex(double x, double y, double z) {
        position((float) x, (float) y, (float) z);
        rgba(red, green, blue, alpha);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public VertexConsumer color(int r, int g, int b, int a) {
        rgba(r, g, b, a);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public VertexConsumer uv(float u, float v) {
        tex(u, v);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public VertexConsumer overlayCoords(int u, int v) {
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public VertexConsumer uv2(int u, int v) {
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public VertexConsumer normal(float x, float y, float z) {
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void endVertex() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void defaultColor(int r, int g, int b, int a) {
        red = r;
        green = g;
        blue = b;
        alpha = a;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unsetDefaultColor() {
        defaultColor(255, 255, 255, 255);
    }
}
