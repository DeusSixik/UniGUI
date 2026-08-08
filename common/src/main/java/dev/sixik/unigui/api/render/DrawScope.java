package dev.sixik.unigui.api.render;

import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.math.Transform;
import dev.sixik.unigui.api.text.RichText;

import java.util.List;
import java.util.Objects;

/**
 * Safe draw-list-style facade for widget renderers.
 *
 * <p>DrawScope intentionally exposes backend-neutral primitives instead of the
 * active RenderBackend. Custom widget renderers can use it like a small
 * ImGui-style draw list while UniGUI still records normal DrawCommands.</p>
 */
public final class DrawScope {
    private final RenderContext context;
    private final Transform transform;

    public DrawScope(RenderContext context, Transform transform) {
        this.context = Objects.requireNonNull(context, "context");
        this.transform = transform;
    }

    public RenderContext context() {
        return context;
    }

    public Transform transform() {
        return transform;
    }

    public DrawScope withTransform(Transform transform) {
        return new DrawScope(context, transform);
    }

    public void rect(float x, float y, float width, float height, Paint paint) {
        if (transform == null) {
            context.rect(x, y, width, height, paint);
        } else {
            context.rect(x, y, width, height, paint, transform);
        }
    }

    public void roundedRect(float x, float y, float width, float height, float radius, Paint paint) {
        if (transform == null) {
            context.roundedRect(x, y, width, height, radius, paint);
        } else {
            context.roundedRect(x, y, width, height, radius, paint, transform);
        }
    }

    public void circle(float x, float y, float width, float height, Paint paint) {
        if (transform == null) {
            context.circle(x, y, width, height, paint);
        } else {
            context.circle(x, y, width, height, paint, transform);
        }
    }

    public void line(float x1, float y1, float x2, float y2, Paint paint) {
        if (transform == null) {
            context.line(x1, y1, x2, y2, paint);
        } else {
            context.line(x1, y1, x2, y2, paint, transform);
        }
    }

    public void path(VectorPath path, float x, float y, float width, float height, Paint paint) {
        if (transform == null) {
            context.path(path, x, y, width, height, paint);
        } else {
            context.path(path, x, y, width, height, paint, transform);
        }
    }

    public void texture(TextureHandle texture, float x, float y, float width, float height, Paint paint) {
        if (transform == null) {
            context.texture(texture, x, y, width, height, paint);
        } else {
            context.texture(texture, x, y, width, height, paint, transform);
        }
    }

    public void texture(TextureHandle texture, TexturePlacement placement, float radius, Paint paint) {
        if (transform == null) {
            context.texture(texture, placement, radius, paint);
        } else {
            context.texture(texture, placement, radius, paint, transform);
        }
    }

    public void text(String text, float x, float y, float width, float height, Paint paint) {
        if (transform == null) {
            context.text(text, x, y, width, height, paint);
        } else {
            context.text(text, x, y, width, height, paint, transform);
        }
    }

    public void text(RichText text, float x, float y, float width, float height, Paint paint) {
        if (transform == null) {
            context.text(text, x, y, width, height, paint);
        } else {
            context.text(text, x, y, width, height, paint, transform);
        }
    }

    public void addDrawCmd(DrawCommand command) {
        context.addDrawCmd(command, transform);
    }

    public void addCallback(CustomDraw callback) {
        context.addCallback(callback);
    }

    public void addLine(float x1, float y1, float x2, float y2, ColorView color, float thickness) {
        context.addLine(x1, y1, x2, y2, color, thickness, transform);
    }

    public void addRect(float x, float y, float width, float height, ColorView color, float thickness) {
        context.addRect(x, y, width, height, color, thickness, transform);
    }

    public void addRect(float x, float y, float width, float height, float radius,
                        ColorView color, float thickness) {
        context.addRect(x, y, width, height, radius, color, thickness, transform);
    }

    public void addRectFilled(float x, float y, float width, float height, ColorView color) {
        context.addRectFilled(x, y, width, height, color, transform);
    }

    public void addRectFilled(float x, float y, float width, float height, float radius, ColorView color) {
        context.addRectFilled(x, y, width, height, radius, color, transform);
    }

    public void addRectFilledMultiColor(float x, float y, float width, float height,
                                        ColorView topLeft, ColorView topRight,
                                        ColorView bottomRight, ColorView bottomLeft) {
        context.addRectFilledMultiColor(x, y, width, height, topLeft, topRight, bottomRight, bottomLeft, transform);
    }

    public void addQuad(DrawPoint p1, DrawPoint p2, DrawPoint p3, DrawPoint p4,
                        ColorView color, float thickness) {
        context.addQuad(p1, p2, p3, p4, color, thickness, transform);
    }

    public void addQuadFilled(DrawPoint p1, DrawPoint p2, DrawPoint p3, DrawPoint p4, ColorView color) {
        context.addQuadFilled(p1, p2, p3, p4, color, transform);
    }

    public void addTriangle(DrawPoint p1, DrawPoint p2, DrawPoint p3, ColorView color, float thickness) {
        context.addTriangle(p1, p2, p3, color, thickness, transform);
    }

    public void addTriangleFilled(DrawPoint p1, DrawPoint p2, DrawPoint p3, ColorView color) {
        context.addTriangleFilled(p1, p2, p3, color, transform);
    }

    public void addCircle(float centerX, float centerY, float radius,
                          ColorView color, int segments, float thickness) {
        context.addCircle(centerX, centerY, radius, color, segments, thickness, transform);
    }

    public void addCircleFilled(float centerX, float centerY, float radius, ColorView color, int segments) {
        context.addCircleFilled(centerX, centerY, radius, color, segments, transform);
    }

    public void addNgon(float centerX, float centerY, float radius,
                        ColorView color, int segments, float thickness) {
        context.addNgon(centerX, centerY, radius, color, segments, thickness, transform);
    }

    public void addNgonFilled(float centerX, float centerY, float radius, ColorView color, int segments) {
        context.addNgonFilled(centerX, centerY, radius, color, segments, transform);
    }

    public void addEllipse(float centerX, float centerY, float radiusX, float radiusY,
                           ColorView color, int segments, float thickness) {
        context.addEllipse(centerX, centerY, radiusX, radiusY, color, segments, thickness, transform);
    }

    public void addEllipseFilled(float centerX, float centerY, float radiusX, float radiusY,
                                 ColorView color, int segments) {
        context.addEllipseFilled(centerX, centerY, radiusX, radiusY, color, segments, transform);
    }

    public void addText(String text, float x, float y, float width, float height, ColorView color) {
        context.addText(text, x, y, width, height, color, transform);
    }

    public void addText(RichText text, float x, float y, float width, float height, ColorView color) {
        context.addText(text, x, y, width, height, color, transform);
    }

    public void addPolyline(List<DrawPoint> points, ColorView color, boolean closed, float thickness) {
        context.addPolyline(points, color, closed, thickness, transform);
    }

    public void addConvexPolyFilled(List<DrawPoint> points, ColorView color) {
        context.addConvexPolyFilled(points, color, transform);
    }

    public void addConcavePolyFilled(List<DrawPoint> points, ColorView color) {
        context.addConcavePolyFilled(points, color, transform);
    }

    public void addImage(TextureHandle texture, float x, float y, float width, float height, ColorView tint) {
        context.addImage(texture, x, y, width, height, tint, transform);
    }

    public void addImage(TextureHandle texture, float x, float y, float width, float height,
                         DrawPoint uvMin, DrawPoint uvMax, ColorView tint) {
        context.addImage(texture, x, y, width, height, uvMin, uvMax, tint, transform);
    }

    public void addImageQuad(TextureHandle texture,
                             DrawPoint p1, DrawPoint p2, DrawPoint p3, DrawPoint p4,
                             DrawPoint uv1, DrawPoint uv2, DrawPoint uv3, DrawPoint uv4,
                             ColorView tint) {
        context.addImageQuad(texture, p1, p2, p3, p4, uv1, uv2, uv3, uv4, tint, transform);
    }

    public void addImageRounded(TextureHandle texture, float x, float y, float width, float height,
                                DrawPoint uvMin, DrawPoint uvMax, ColorView tint, float radius) {
        context.addImageRounded(texture, x, y, width, height, uvMin, uvMax, tint, radius, transform);
    }

    public void addMesh(DrawMesh mesh, TextureHandle texture) {
        context.addMesh(mesh, texture, transform);
    }

    public void channelsSplit(int count) {
        context.channelsSplit(count);
    }

    public void channelsMerge() {
        context.channelsMerge();
    }

    public void channelsSetCurrent(int channel) {
        context.channelsSetCurrent(channel);
    }

    public VectorPath path() {
        return context.path();
    }

    public void pathClear() {
        context.pathClear();
    }

    public void pathLineTo(DrawPoint point) {
        context.pathLineTo(point);
    }

    public void pathLineTo(float x, float y) {
        context.pathLineTo(x, y);
    }

    public void pathLineToMergeDuplicate(DrawPoint point) {
        context.pathLineToMergeDuplicate(point);
    }

    public void pathLineToMergeDuplicate(float x, float y) {
        context.pathLineToMergeDuplicate(x, y);
    }

    public void pathFillConvex(ColorView color) {
        context.pathFillConvex(color, transform);
    }

    public void pathStroke(ColorView color, boolean closed, float thickness) {
        context.pathStroke(color, closed, thickness, transform);
    }

    public void pathArcTo(float centerX, float centerY, float radius,
                          float minAngle, float maxAngle, int segments) {
        context.pathArcTo(centerX, centerY, radius, minAngle, maxAngle, segments);
    }

    public void pathArcToFast(float centerX, float centerY, float radius,
                              int minSampleOf12, int maxSampleOf12) {
        context.pathArcToFast(centerX, centerY, radius, minSampleOf12, maxSampleOf12);
    }

    public void pathBezierCubicCurveTo(float controlX1, float controlY1,
                                       float controlX2, float controlY2,
                                       float x, float y, int segments) {
        context.pathBezierCubicCurveTo(controlX1, controlY1, controlX2, controlY2, x, y, segments);
    }

    public void pathBezierQuadraticCurveTo(float controlX, float controlY,
                                           float x, float y, int segments) {
        context.pathBezierQuadraticCurveTo(controlX, controlY, x, y, segments);
    }

    public void pathRect(float x, float y, float width, float height) {
        context.pathRect(x, y, width, height);
    }

    public void pathRect(float x, float y, float width, float height, float radius) {
        context.pathRect(x, y, width, height, radius);
    }

    public void pushClip(float x, float y, float width, float height) {
        context.pushClip(x, y, width, height);
    }

    public void popClip() {
        context.popClip();
    }
}
