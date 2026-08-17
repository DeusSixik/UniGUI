package dev.sixik.unigui.impl.input;

import dev.sixik.unigui.api.input.HitTestCoordinateMapper;
import dev.sixik.unigui.api.input.HitTestResult;
import dev.sixik.unigui.api.input.HitTester;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.math.TransformGeometry;
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
            TransformGeometry.Point untransformed = inverseTransform(widget, x, y);
            RectView bounds = widget.layoutBounds();
            if (widget == target) {
                return Optional.of(new HitTestResult(widget, rootX, rootY,
                        untransformed.x() - bounds.x(), untransformed.y() - bounds.y()));
            }
            Widget child = route.get(index + 1);
            TransformGeometry.Point childPoint = mapPointForChild(widget, child, untransformed.x(), untransformed.y());
            x = childPoint.x();
            y = childPoint.y();
        }
        return Optional.empty();
    }

    private Optional<HitTestResult> hitTestRecursive(Widget widget, float rootX, float rootY, float x, float y) {
        if (widget.visibility() != Visibility.VISIBLE || !widget.enabled()) {
            return Optional.empty();
        }

        TransformGeometry.Point untransformed = inverseTransform(widget, x, y);
        RectView bounds = widget.layoutBounds();
        if (!contains(bounds, untransformed.x(), untransformed.y())) {
            return Optional.empty();
        }

        List<Widget> children = widget.children();
        for (int index = children.size() - 1; index >= 0; index--) {
            Widget child = children.get(index);
            TransformGeometry.Point childPoint = mapPointForChild(widget, child, untransformed.x(), untransformed.y());
            Optional<HitTestResult> childHit = hitTestRecursive(child, rootX, rootY, childPoint.x(), childPoint.y());
            if (childHit.isPresent()) {
                return childHit;
            }
        }

        return Optional.of(new HitTestResult(widget, rootX, rootY, untransformed.x() - bounds.x(), untransformed.y() - bounds.y()));
    }

    private static TransformGeometry.Point mapPointForChild(Widget widget, Widget child, float x, float y) {
        if (widget instanceof HitTestCoordinateMapper mapper) {
            HitTestCoordinateMapper.HitTestPoint mapped = mapper.mapHitTestPointForChild(child, x, y);
            if (mapped != null && Float.isFinite(mapped.x()) && Float.isFinite(mapped.y())) {
                return new TransformGeometry.Point(mapped.x(), mapped.y());
            }
        }
        return new TransformGeometry.Point(x, y);
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

    private static TransformGeometry.Point inverseTransform(Widget widget, float x, float y) {
        return TransformGeometry.inverseTransformPoint(x, y, widget.layoutBounds(), widget.transform());
    }

    private static boolean contains(RectView bounds, float x, float y) {
        float minX = Math.min(bounds.x(), bounds.x() + bounds.width());
        float maxX = Math.max(bounds.x(), bounds.x() + bounds.width());
        float minY = Math.min(bounds.y(), bounds.y() + bounds.height());
        float maxY = Math.max(bounds.y(), bounds.y() + bounds.height());
        return x >= minX && x <= maxX && y >= minY && y <= maxY;
    }
}
