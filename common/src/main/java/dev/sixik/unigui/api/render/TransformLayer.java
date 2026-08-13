package dev.sixik.unigui.api.render;

import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.math.Transform;

/**
 * A parent widget transform captured for draw commands emitted by descendants.
 */
public final class TransformLayer {
    private final MutableRect bounds = new MutableRect();
    private final Transform transform = new Transform();

    public TransformLayer(RectView bounds, Transform transform) {
        if (bounds != null) {
            this.bounds.set(bounds);
        }
        if (transform != null) {
            this.transform.copyFrom(transform);
        }
    }

    public RectView bounds() {
        return bounds;
    }

    public Transform transform() {
        return transform;
    }

    public TransformLayer copy() {
        return new TransformLayer(bounds, transform);
    }
}
