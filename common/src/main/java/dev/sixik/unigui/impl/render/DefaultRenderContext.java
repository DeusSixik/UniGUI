package dev.sixik.unigui.impl.render;

import dev.sixik.unigui.api.render.DrawList;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.math.Transform;
import dev.sixik.unigui.api.render.RenderBackend;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.render.TransformLayer;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Collections;
import java.util.List;

public final class DefaultRenderContext implements RenderContext {
    private static final int INITIAL_STACK_CAPACITY = 8;
    private final DrawList drawList;
    private float[] opacityStack = new float[INITIAL_STACK_CAPACITY];
    private int opacityStackSize;
    private boolean[] textPixelSnapStack = new boolean[INITIAL_STACK_CAPACITY];
    private int textPixelSnapStackSize;
    private final List<TransformLayer> transformStack = new ObjectArrayList<>();
    private final ObjectArrayList<TransformLayer> recycledTransformLayers = new ObjectArrayList<>();
    private final List<TransformLayer> transformStackView = Collections.unmodifiableList(transformStack);
    private RenderBackend backend;
    private float opacityMultiplier = 1.0f;
    private boolean textPixelSnapEnabled = true;

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
        ensureOpacityStackCapacity();
        opacityStack[opacityStackSize++] = opacityMultiplier;
        opacityMultiplier *= clamp01(opacity);
    }

    @Override
    public void popOpacity() {
        opacityMultiplier = opacityStackSize == 0 ? 1.0f : opacityStack[--opacityStackSize];
    }

    @Override
    public float opacityMultiplier() {
        return opacityMultiplier;
    }

    @Override
    public void pushTextPixelSnap(boolean enabled) {
        ensureTextPixelSnapStackCapacity();
        textPixelSnapStack[textPixelSnapStackSize++] = textPixelSnapEnabled;
        textPixelSnapEnabled = textPixelSnapEnabled && enabled;
    }

    @Override
    public void popTextPixelSnap() {
        textPixelSnapEnabled = textPixelSnapStackSize == 0
                ? true
                : textPixelSnapStack[--textPixelSnapStackSize];
    }

    @Override
    public boolean textPixelSnapEnabled() {
        return textPixelSnapEnabled;
    }

    @Override
    public void pushTransform(RectView bounds, Transform transform) {
        int recycledSize = recycledTransformLayers.size();
        TransformLayer layer = recycledSize == 0
                ? new TransformLayer(null, null)
                : recycledTransformLayers.remove(recycledSize - 1);
        layer.set(bounds, transform);
        transformStack.add(layer);
    }

    @Override
    public void popTransform() {
        if (!transformStack.isEmpty()) {
            recycledTransformLayers.add(transformStack.remove(transformStack.size() - 1));
        }
    }

    @Override
    public List<TransformLayer> transformStack() {
        return transformStackView;
    }

    private void ensureOpacityStackCapacity() {
        if (opacityStackSize == opacityStack.length) {
            float[] expanded = new float[opacityStack.length << 1];
            System.arraycopy(opacityStack, 0, expanded, 0, opacityStack.length);
            opacityStack = expanded;
        }
    }

    private void ensureTextPixelSnapStackCapacity() {
        if (textPixelSnapStackSize == textPixelSnapStack.length) {
            boolean[] expanded = new boolean[textPixelSnapStack.length << 1];
            System.arraycopy(textPixelSnapStack, 0, expanded, 0, textPixelSnapStack.length);
            textPixelSnapStack = expanded;
        }
    }

    private static float clamp01(float value) {
        if (!Float.isFinite(value)) return 1.0f;
        return Math.max(0.0f, Math.min(1.0f, value));
    }
}
