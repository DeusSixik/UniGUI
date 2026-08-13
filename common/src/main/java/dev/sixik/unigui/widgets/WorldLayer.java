package dev.sixik.unigui.widgets;

import dev.sixik.unigui.api.render.DrawScope;

/**
 * Draw-only layer rendered inside a {@link WorldCanvas}.
 *
 * <p>Coordinates passed to {@link DrawScope} are root/screen coordinates. Use
 * the projection helpers on {@link WorldCanvas} to convert world coordinates
 * before drawing.</p>
 */
@FunctionalInterface
public interface WorldLayer {
    void render(WorldCanvas canvas, DrawScope draw);
}
