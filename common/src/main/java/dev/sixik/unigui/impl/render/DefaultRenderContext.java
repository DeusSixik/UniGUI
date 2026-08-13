package dev.sixik.unigui.impl.render;

import dev.sixik.unigui.api.render.DrawList;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.math.Transform;
import dev.sixik.unigui.api.render.RenderBackend;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.render.TransformLayer;

import java.util.ArrayDeque;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

public final class DefaultRenderContext implements RenderContext {
    private final DrawList drawList;
    private final Deque<Float> opacityStack = new ArrayDeque<>();
    private final List<TransformLayer> transformStack = new ObjectArrayList<>();
    private final List<TransformLayer> transformStackView = Collections.unmodifiableList(transformStack);
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

    @Override
    public void pushTransform(RectView bounds, Transform transform) {
        transformStack.add(new TransformLayer(
                bounds == null ? new MutableRect() : bounds,
                transform == null ? new Transform() : transform));
    }

    @Override
    public void popTransform() {
        if (!transformStack.isEmpty()) {
            transformStack.remove(transformStack.size() - 1);
        }
    }

    @Override
    public List<TransformLayer> transformStack() {
        return transformStackView;
    }

    private static float clamp01(float value) {
        if (!Float.isFinite(value)) return 1.0f;
        return Math.max(0.0f, Math.min(1.0f, value));
    }
}
