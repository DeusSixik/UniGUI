package dev.sixik.unigui.api.widget;

import dev.sixik.unigui.api.math.RectView;

/**
 * Optional hook for containers that render children at visual bounds that differ
 * from the child's arranged layout bounds.
 */
public interface RenderedBoundsMapper {
    /**
     * Maps a descendant/direct child's bounds from arranged root-space into the
     * bounds that are actually visible on screen. Return {@code null} to use the
     * supplied bounds unchanged.
     */
    RectView renderedBoundsForChild(Widget child, RectView bounds);
}
