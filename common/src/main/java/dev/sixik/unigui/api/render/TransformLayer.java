package dev.sixik.unigui.api.render;

import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.math.Transform;

/**
 * Снимок transform'а родительского виджета для команд, созданных потомками.
 *
 * <p>RenderContext хранит стек таких слоёв, чтобы draw-команды потомков могли быть записаны в
 * root-space, но backend всё ещё знал, какие parent transforms нужно применить к визуалу.</p>
 */
public final class TransformLayer {
    private final MutableRect bounds = new MutableRect();
    private final Transform transform = new Transform();

    /**
     * Создаёт слой transform'а.
     *
     * @param bounds bounds виджета, к которому привязан transform
     * @param transform transform слоя
     */
    public TransformLayer(RectView bounds, Transform transform) {
        if (bounds != null) {
            this.bounds.set(bounds);
        }
        if (transform != null) {
            this.transform.copyFrom(transform);
        }
    }

    /** @return bounds слоя */
    public RectView bounds() {
        return bounds;
    }

    /** @return transform слоя */
    public Transform transform() {
        return transform;
    }

    /** @return независимая копия слоя */
    public TransformLayer copy() {
        return new TransformLayer(bounds, transform);
    }
}