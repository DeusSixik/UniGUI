package dev.sixik.unigui.impl.input;

import dev.sixik.unigui.api.input.HitTestCoordinateMapper;
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

    @Override
    public Optional<HitTestResult> localPoint(Widget root, Widget target, float rootX, float rootY) {
        if (root == null || target == null) return Optional.empty();
        List<Widget> route = route(root, target);
        if (route.isEmpty()) return Optional.empty();

        float x = rootX;
        float y = rootY;
        for (int index = 0; index < route.size(); index++) {
            Widget widget = route.get(index);
            Point untransformed = inverseTransform(widget, x, y);
            RectView bounds = widget.layoutBounds();
            if (widget == target) {
                return Optional.of(new HitTestResult(widget, rootX, rootY,
                        untransformed.x - bounds.x(), untransformed.y - bounds.y()));
            }
            Widget child = route.get(index + 1);
            Point childPoint = mapPointForChild(widget, child, untransformed.x, untransformed.y);
            x = childPoint.x;
            y = childPoint.y;
        }
        return Optional.empty();
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

        List<Widget> children = widget.children();
        for (int index = children.size() - 1; index >= 0; index--) {
            Widget child = children.get(index);
            Point childPoint = mapPointForChild(widget, child, untransformed.x, untransformed.y);
            Optional<HitTestResult> childHit = hitTestRecursive(child, rootX, rootY, childPoint.x, childPoint.y);
            if (childHit.isPresent()) {
                return childHit;
            }
        }

        return Optional.of(new HitTestResult(widget, rootX, rootY, untransformed.x - bounds.x(), untransformed.y - bounds.y()));
    }

    private static Point mapPointForChild(Widget widget, Widget child, float x, float y) {
        if (widget instanceof HitTestCoordinateMapper mapper) {
            HitTestCoordinateMapper.HitTestPoint mapped = mapper.mapHitTestPointForChild(child, x, y);
            if (mapped != null && Float.isFinite(mapped.x()) && Float.isFinite(mapped.y())) {
                return new Point(mapped.x(), mapped.y());
            }
        }
        return new Point(x, y);
    }

    private static List<Widget> route(Widget root, Widget target) {
        if (root == target) return List.of(root);
        for (Widget child : root.children()) {
            List<Widget> childRoute = route(child, target);
            if (!childRoute.isEmpty()) {
                List<Widget> fullRoute = new it.unimi.dsi.fastutil.objects.ObjectArrayList<>(childRoute.size() + 1);
                fullRoute.add(root);
                fullRoute.addAll(childRoute);
                return fullRoute;
            }
        }
        return List.of();
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
