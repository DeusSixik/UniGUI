package dev.sixik.unigui.impl.render;

import dev.sixik.unigui.api.render.DrawList;
import dev.sixik.unigui.api.render.RenderBackend;
import dev.sixik.unigui.api.render.RenderContext;

import java.util.ArrayDeque;
import java.util.Deque;

public final class DefaultRenderContext implements RenderContext {
    private final DrawList drawList;
    private final Deque<Float> opacityStack = new ArrayDeque<>();
    private RenderBackend backend;
    private float opacityMultiplier = 1.0f;

    public DefaultRenderContext(DrawList drawList) {
        this.drawList = drawList;
    }

    public DefaultRenderContext backend(RenderBackend backend) {
        this.backend = backend;
        return this;
    }

    @Override
    public DrawList drawList() {
        return drawList;
    }

    @Override
    public RenderBackend backend() {
        return backend;
    }

    @Override
    public void pushOpacity(float opacity) {
        opacityStack.push(opacityMultiplier);
        opacityMultiplier *= clamp01(opacity);
    }

    @Override
    public void popOpacity() {
        opacityMultiplier = opacityStack.isEmpty() ? 1.0f : opacityStack.pop();
    }

    @Override
    public float opacityMultiplier() {
        return opacityMultiplier;
    }

    private static float clamp01(float value) {
        if (!Float.isFinite(value)) return 1.0f;
        return Math.max(0.0f, Math.min(1.0f, value));
    }
}
