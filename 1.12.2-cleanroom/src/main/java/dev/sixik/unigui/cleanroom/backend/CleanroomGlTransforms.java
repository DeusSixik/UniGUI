package dev.sixik.unigui.cleanroom.backend;

import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.math.Transform;
import dev.sixik.unigui.api.render.DrawCommand;
import dev.sixik.unigui.api.render.TransformLayer;
import net.minecraft.client.renderer.GlStateManager;

/**
 * Applies a command's transform chain to the GL modelview stack, for draws that keep their geometry untransformed.
 */
final class CleanroomGlTransforms {
    private CleanroomGlTransforms() {
    }

    /**
     * @param command draw command
     * @return {@code true} if the command has a non-identity transform or any stacked layer
     */
    static boolean has(DrawCommand command) {
        Transform transform = command.transform();
        return command.transformStackSize() > 0 || (transform != null && !transform.isIdentity());
    }

    /**
     * Multiplies the stack layers (outermost first) and then the command transform into the current matrix.
     * Caller is responsible for {@code pushMatrix} / {@code popMatrix}.
     *
     * @param command draw command
     */
    static void apply(DrawCommand command) {
        Object[] layers = command.transformStackElements();
        for (int i = 0; i < command.transformStackSize(); i++) {
            TransformLayer layer = (TransformLayer) layers[i];
            apply(layer.bounds(), layer.transform());
        }
        apply(command.bounds(), command.transform());
    }

    private static void apply(RectView bounds, Transform transform) {
        if (transform == null || transform.isIdentity()) return;
        float pivotX = bounds.x() + transform.pivot().x();
        float pivotY = bounds.y() + transform.pivot().y();
        GlStateManager.translate(transform.position().x(), transform.position().y(), 0.0f);
        GlStateManager.translate(pivotX, pivotY, 0.0f);
        GlStateManager.rotate(transform.rotationDegrees(), 0.0f, 0.0f, 1.0f);
        GlStateManager.scale(transform.scale().x(), transform.scale().y(), 1.0f);
        GlStateManager.translate(-pivotX, -pivotY, 0.0f);
    }
}
