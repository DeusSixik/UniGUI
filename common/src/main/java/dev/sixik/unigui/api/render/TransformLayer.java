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
        set(bounds, transform);
    }

    /**
     * Записывает состояние слоя в уже существующий объект без создания временных значений.
     *
     * @param bounds bounds виджета; {@code null} означает пустые bounds
     * @param transform transform; {@code null} означает transform по умолчанию
     * @return этот слой
     */
    public TransformLayer set(RectView bounds, Transform transform) {
        if (bounds == null) {
            this.bounds.set(0.0f, 0.0f, 0.0f, 0.0f);
        } else {
            this.bounds.set(bounds);
        }
        if (transform == null) {
            resetTransform();
        } else {
            this.transform.copyFrom(transform);
        }
        return this;
    }

    /**
     * Копирует состояние в уже существующий слой без создания новых объектов.
     *
     * @param source источник состояния; {@code null} очищает слой
     * @return этот слой
     */
    public TransformLayer copyFrom(TransformLayer source) {
        if (source == null) {
            bounds.set(0.0f, 0.0f, 0.0f, 0.0f);
            resetTransform();
            return this;
        }
        bounds.set(source.bounds);
        transform.copyFrom(source.transform);
        return this;
    }

    private void resetTransform() {
        transform.position().set(0.0f, 0.0f);
        transform.scale().set(1.0f, 1.0f);
        transform.pivot().set(0.0f, 0.0f);
        transform.setRotationDegrees(0.0f);
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
