package dev.sixik.unigui.backend.minecraft;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.math.Transform;
import dev.sixik.unigui.api.render.DrawCommand;
import dev.sixik.unigui.api.render.TransformLayer;
import org.joml.Matrix4f;

/** Applies inherited widget transforms in root-to-leaf order. */
final class MinecraftTransform {
    private MinecraftTransform() {
    }

    static void apply(DrawCommand command, PoseStack pose) {
        if (command == null || pose == null) return;
        Object[] rawLayers = command.transformStackElements();
        for (int i = 0, size = command.transformStackSize(); i < size; i++) {
            TransformLayer layer = (TransformLayer) rawLayers[i];
            if (layer != null) {
                apply(layer.bounds(), layer.transform(), pose);
            }
        }        apply(command.bounds(), command.transform(), pose);
    }

    static void apply(RectView bounds, Transform transform, PoseStack pose) {
        if (bounds == null || transform == null || pose == null) return;

        float pivotX = bounds.x() + transform.pivot().x();
        float pivotY = bounds.y() + transform.pivot().y();

        pose.translate(transform.position().x(), transform.position().y(), 0.0f);
        pose.translate(pivotX, pivotY, 0.0f);
        if (transform.rotationDegrees() != 0.0f) {
            pose.mulPose(Axis.ZP.rotationDegrees(transform.rotationDegrees()));
        }
        pose.scale(transform.scale().x(), transform.scale().y(), 1.0f);
        pose.translate(-pivotX, -pivotY, 0.0f);
    }

    static Matrix4f commandMatrix(Matrix4f basePose, DrawCommand command) {
        Matrix4f matrix = new Matrix4f(basePose);
        if (command == null) return matrix;
        Object[] rawLayers = command.transformStackElements();
        for (int i = 0, size = command.transformStackSize(); i < size; i++) {
            TransformLayer layer = (TransformLayer) rawLayers[i];
            if (layer != null) {
                apply(matrix, layer.bounds(), layer.transform());
            }
        }        apply(matrix, command.bounds(), command.transform());
        return matrix;
    }

    static void apply(Matrix4f matrix, RectView bounds, Transform transform) {
        if (matrix == null || bounds == null || transform == null) return;

        float pivotX = bounds.x() + transform.pivot().x();
        float pivotY = bounds.y() + transform.pivot().y();

        matrix.translate(transform.position().x(), transform.position().y(), 0.0f);
        matrix.translate(pivotX, pivotY, 0.0f);
        if (transform.rotationDegrees() != 0.0f) {
            matrix.rotateZ((float) Math.toRadians(transform.rotationDegrees()));
        }
        matrix.scale(transform.scale().x(), transform.scale().y(), 1.0f);
        matrix.translate(-pivotX, -pivotY, 0.0f);
    }
}
