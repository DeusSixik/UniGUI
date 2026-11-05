package dev.sixik.unigui.api.input;

import dev.sixik.unigui.api.widget.Widget;

/**
 * Optional hook for widgets that render child widgets in a coordinate space
 * that differs from the child's arranged layout bounds.
 */
public interface HitTestCoordinateMapper {
    /**
     * Maps a point from this widget's hit-test space into the supplied child's
     * hit-test space. Return {@code null} to use the point unchanged.
     */
    HitTestPoint mapHitTestPointForChild(Widget child, float x, float y);

    record HitTestPoint(float x, float y) {
    }
}
