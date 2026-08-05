package dev.sixik.unigui.api.math;

public interface RectView {
    float x();

    float y();

    float width();

    float height();

    default float right() {
        return x() + width();
    }

    default float bottom() {
        return y() + height();
    }

    default boolean contains(float px, float py) {
        return px >= x() && py >= y() && px <= right() && py <= bottom();
    }
}
