package dev.sixik.unigui.api.math;

public final class Transform {
    private final MutableVec2 position = new MutableVec2();
    private final MutableVec2 scale = new MutableVec2(1.0f, 1.0f);
    private final MutableVec2 pivot = new MutableVec2();
    private float rotationDegrees;
    private Runnable onChanged;

    public Transform() {
        Runnable changed = this::changed;
        position.onChanged(changed);
        scale.onChanged(changed);
        pivot.onChanged(changed);
    }

    public MutableVec2 position() {
        return position;
    }

    public MutableVec2 scale() {
        return scale;
    }

    public MutableVec2 pivot() {
        return pivot;
    }

    public float rotationDegrees() {
        return rotationDegrees;
    }

    public Transform setRotationDegrees(float rotationDegrees) {
        if (this.rotationDegrees == rotationDegrees) return this;
        this.rotationDegrees = rotationDegrees;
        changed();
        return this;
    }

    public Transform copyFrom(Transform other) {
        position.set(other.position);
        scale.set(other.scale);
        pivot.set(other.pivot);
        setRotationDegrees(other.rotationDegrees);
        return this;
    }

    public Transform onChanged(Runnable onChanged) {
        this.onChanged = onChanged;
        return this;
    }

    public Transform copy() {
        Transform transform = new Transform();
        transform.copyFrom(this);
        return transform;
    }

    /**
     * Проверяет, является ли transform единичным: нулевое смещение, единичный масштаб,
     * нулевой поворот.
     *
     * <p>Pivot при единичном transform'е визуально irrelevant: backend применяет
     * {@code translate(pivot) * scale/rotate * translate(-pivot)}, что при единичных
     * параметрах сворачивается в identity при любом pivot. Используется горячим
     * render-путём, чтобы пропускать pivot-коррекцию без аллокаций.</p>
     *
     * @return {@code true}, если transform не меняет геометрию
     */
    public boolean isIdentity() {
        return position.x() == 0.0f
                && position.y() == 0.0f
                && scale.x() == 1.0f
                && scale.y() == 1.0f
                && rotationDegrees == 0.0f;
    }

    private void changed() {
        if (onChanged != null) onChanged.run();
    }
}
