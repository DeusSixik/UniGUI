package dev.sixik.unigui.impl.input;

import dev.sixik.unigui.api.input.HitTestResult;
import dev.sixik.unigui.api.input.HitTester;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.math.Transform;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.widget.Widget;

import java.util.List;
import java.util.Optional;

public final class TransformHitTester implements HitTester {
    @Override
    public Optional<HitTestResult> hitTest(Widget root, float rootX, float rootY) {
        if (root == null || root.visibility() != Visibility.VISIBLE || !root.enabled()) return Optional.empty();
        return hitTestRecursive(root, rootX, rootY, rootX, rootY);
    }

    private Optional<HitTestResult> hitTestRecursive(Widget widget, float rootX, float rootY, float x, float y) {
        if (widget.visibility() != Visibility.VISIBLE || !widget.enabled()) {
            return Optional.empty();
        }

        Point untransformed = inverseTransform(widget, x, y);
        RectView bounds = widget.layoutBounds();
        if (!contains(bounds, untransformed.x, untransformed.y)) {
            return Optional.empty();
        }

        List<Widget> children = List.copyOf(widget.children());
        for (int index = children.size() - 1; index >= 0; index--) {
            Optional<HitTestResult> childHit = hitTestRecursive(children.get(index), rootX, rootY, untransformed.x, untransformed.y);
            if (childHit.isPresent()) {
                return childHit;
            }
        }

        return Optional.of(new HitTestResult(widget, rootX, rootY, untransformed.x - bounds.x(), untransformed.y - bounds.y()));
    }

    private static Point inverseTransform(Widget widget, float x, float y) {
        RectView bounds = widget.layoutBounds();
        Transform transform = widget.transform();

        x -= transform.position().x();
        y -= transform.position().y();

        float pivotX = bounds.x() + transform.pivot().x();
        float pivotY = bounds.y() + transform.pivot().y();
        x -= pivotX;
        y -= pivotY;

        float rotation = (float) Math.toRadians(-transform.rotationDegrees());
        float cos = (float) Math.cos(rotation);
        float sin = (float) Math.sin(rotation);
        float rotatedX = x * cos - y * sin;
        float rotatedY = x * sin + y * cos;

        float scaleX = transform.scale().x();
        float scaleY = transform.scale().y();
        if (scaleX != 0.0f) rotatedX /= scaleX;
        if (scaleY != 0.0f) rotatedY /= scaleY;

        return new Point(rotatedX + pivotX, rotatedY + pivotY);
    }

    private static boolean contains(RectView bounds, float x, float y) {
        float minX = Math.min(bounds.x(), bounds.x() + bounds.width());
        float maxX = Math.max(bounds.x(), bounds.x() + bounds.width());
        float minY = Math.min(bounds.y(), bounds.y() + bounds.height());
        float maxY = Math.max(bounds.y(), bounds.y() + bounds.height());
        return x >= minX && x <= maxX && y >= minY && y <= maxY;
    }

    private record Point(float x, float y) {
    }
}
