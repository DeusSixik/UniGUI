package dev.sixik.unigui.api.math;

/** Geometry helpers for applying UniGUI render transforms outside the backend. */
public final class TransformGeometry {
    private TransformGeometry() {
    }

    public static Point transformPoint(float x, float y, RectView bounds, Transform transform) {
        if (bounds == null || transform == null) {
            return new Point(x, y);
        }

        float pivotX = bounds.x() + transform.pivot().x();
        float pivotY = bounds.y() + transform.pivot().y();
        float dx = (x - pivotX) * transform.scale().x();
        float dy = (y - pivotY) * transform.scale().y();
        float rotation = (float) Math.toRadians(transform.rotationDegrees());
        float cos = (float) Math.cos(rotation);
        float sin = (float) Math.sin(rotation);
        return new Point(
                pivotX + dx * cos - dy * sin + transform.position().x(),
                pivotY + dx * sin + dy * cos + transform.position().y());
    }

    public static Point inverseTransformPoint(float x, float y, RectView bounds, Transform transform) {
        if (bounds == null || transform == null) {
            return new Point(x, y);
        }

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

    public static MutableRect transformedBounds(RectView bounds, RectView pivotBounds, Transform transform) {
        return transformBoundsInto(new MutableRect(), bounds, pivotBounds, transform);
    }

    public static MutableRect transformBoundsInto(MutableRect target,
                                                  RectView bounds,
                                                  RectView pivotBounds,
                                                  Transform transform) {
        MutableRect result = target == null ? new MutableRect() : target;
        if (bounds == null) {
            return result.set(0.0f, 0.0f, 0.0f, 0.0f);
        }
        if (pivotBounds == null || transform == null) {
            return result.set(bounds);
        }

        float x1 = bounds.x();
        float y1 = bounds.y();
        float x2 = bounds.x() + bounds.width();
        float y2 = bounds.y() + bounds.height();

        Point topLeft = transformPoint(x1, y1, pivotBounds, transform);
        Point topRight = transformPoint(x2, y1, pivotBounds, transform);
        Point bottomRight = transformPoint(x2, y2, pivotBounds, transform);
        Point bottomLeft = transformPoint(x1, y2, pivotBounds, transform);

        float left = Math.min(Math.min(topLeft.x(), topRight.x()), Math.min(bottomRight.x(), bottomLeft.x()));
        float top = Math.min(Math.min(topLeft.y(), topRight.y()), Math.min(bottomRight.y(), bottomLeft.y()));
        float right = Math.max(Math.max(topLeft.x(), topRight.x()), Math.max(bottomRight.x(), bottomLeft.x()));
        float bottom = Math.max(Math.max(topLeft.y(), topRight.y()), Math.max(bottomRight.y(), bottomLeft.y()));
        return result.set(left, top, Math.max(0.0f, right - left), Math.max(0.0f, bottom - top));
    }

    public record Point(float x, float y) {
    }
}
