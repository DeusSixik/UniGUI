package dev.sixik.unigui.cleanroom.backend;

import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.render.DrawCommand;
import dev.sixik.unigui.api.render.DrawCommandType;
import dev.sixik.unigui.api.render.DrawMesh;
import dev.sixik.unigui.api.render.DrawVertex;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.render.VectorPath;
import dev.sixik.unigui.impl.render.PathAntiAliasing;

import java.util.Arrays;

/**
 * CPU tessellation of UI shapes into a triangle list: the fallback behind the SDF shader and the only path for
 * {@code PATH} and {@code MESH}. Port of the 1.20 {@code MinecraftShapeBatchRenderer} geometry.
 *
 * <ul>
 *   <li>{@code RECT}: filled quad, or four edge quads for a stroke.</li>
 *   <li>{@code ROUNDED_RECT} / {@code CIRCLE}: triangle fan or ring around an arc-sampled outline.</li>
 *   <li>{@code LINE}: a quad of the stroke thickness; thin axis-aligned lines are centered on a pixel.</li>
 *   <li>{@code PATH}: curves flattened to a pixel tolerance, then {@link PathAntiAliasing} adds a one pixel
 *       fringe whose alpha fades with coverage (convex fill and mitered stroke).</li>
 *   <li>{@code MESH}: the vertices as they are, with per-vertex color.</li>
 * </ul>
 *
 * <p>Geometry is generated in command-local coordinates and transformed on the CPU by the collapsed command
 * transform (see {@link CleanroomRenderMath#affine}), so a batch of commands with different transforms still goes
 * out as one buffer. No OpenGL and no Minecraft access, so it is unit tested. Not thread safe.</p>
 */
final class CleanroomShapeTessellator implements PathAntiAliasing.VertexSink {
    private static final float TAU = (float) (Math.PI * 2.0);
    private static final float MIN_STROKE_WIDTH = 1.0f;
    /**
     * Tolerance of curve flattening in physical pixels.
     */
    private static final float CURVE_TOLERANCE_PIXELS = 0.05f;
    private static final int MIN_CURVE_SEGMENTS = 2;
    private static final int MAX_CURVE_SEGMENTS = 96;

    private final PathAntiAliasing pathAntiAliasing = new PathAntiAliasing();
    private final Points points = new Points();
    private final Points innerPoints = new Points();
    private final float[] affine = new float[6];
    private final float[] scratch = new float[2];
    private CleanroomVertexList out;
    private float pixelsPerUnit = 1.0f;
    // Color state of the PATH sink.
    private int sinkRgb;
    private float sinkAlpha;

    /**
     * Selects where vertices go.
     *
     * @param target        destination list
     * @param pixelsPerUnit physical pixels per UI unit; sizes the anti-aliasing fringe and curve tolerance
     */
    void begin(CleanroomVertexList target, float pixelsPerUnit) {
        this.out = target;
        this.pixelsPerUnit = Float.isFinite(pixelsPerUnit) && pixelsPerUnit > 0.0f ? pixelsPerUnit : 1.0f;
    }

    /**
     * Tessellates one command.
     *
     * @param command {@code RECT}, {@code ROUNDED_RECT}, {@code LINE}, {@code CIRCLE}, {@code PATH} or {@code MESH}
     * @param opacity extra alpha multiplier of the surface
     * @return number of vertices appended
     */
    int append(DrawCommand command, float opacity) {
        int before = out.size();
        CleanroomRenderMath.affine(command.bounds(), command.transform(), command.transformStackElements(),
                command.transformStackSize(), scratch, affine);
        switch (command.type()) {
            case RECT -> appendRect(command.bounds(), command.paint(), opacity);
            case ROUNDED_RECT -> appendRoundedRect(command, opacity);
            case LINE -> appendLine(command, opacity);
            case CIRCLE -> appendCircle(command, opacity);
            case PATH -> appendPath(command, opacity);
            case MESH -> appendMesh(command, opacity);
            case TEXTURE -> appendTexture(command, opacity);
            case TEXTURED_QUAD -> appendTexturedQuad(command, opacity);
            default -> {
            }
        }
        return out.size() - before;
    }

    /**
     * @return {@code true} for the command types {@link #append} understands
     */
    static boolean supports(DrawCommandType type) {
        return type == DrawCommandType.RECT || type == DrawCommandType.ROUNDED_RECT
                || type == DrawCommandType.LINE || type == DrawCommandType.CIRCLE
                || type == DrawCommandType.PATH || type == DrawCommandType.MESH;
    }

    /** @return {@code true} for the textured command types {@link #append} understands */
    static boolean supportsTexture(DrawCommandType type) {
        return type == DrawCommandType.TEXTURE || type == DrawCommandType.TEXTURED_QUAD;
    }

    // ---- shapes -------------------------------------------------------------------------------------------

    private void appendRect(RectView bounds, Paint paint, float opacity) {
        float x1 = Math.min(bounds.x(), bounds.x() + bounds.width());
        float y1 = Math.min(bounds.y(), bounds.y() + bounds.height());
        float x2 = Math.max(bounds.x(), bounds.x() + bounds.width());
        float y2 = Math.max(bounds.y(), bounds.y() + bounds.height());
        if (x2 <= x1 || y2 <= y1) return;
        int color = argb(paint.color(), opacity);
        if ((color >>> 24) == 0) return;
        if (!paint.isStroke()) {
            quad(x1, y1, x2, y2, color);
            return;
        }
        float thickness = Math.min(positiveThickness(paint.strokeWidth()), Math.min(x2 - x1, y2 - y1) * 0.5f);
        if (thickness <= 0.0f) return;
        quad(x1, y1, x2, y1 + thickness, color);
        quad(x1, y2 - thickness, x2, y2, color);
        if (y2 - y1 > thickness * 2.0f) {
            quad(x1, y1 + thickness, x1 + thickness, y2 - thickness, color);
            quad(x2 - thickness, y1 + thickness, x2, y2 - thickness, color);
        }
    }

    private void appendRoundedRect(DrawCommand command, float opacity) {
        RectView bounds = command.bounds();
        float x1 = Math.min(bounds.x(), bounds.x() + bounds.width());
        float y1 = Math.min(bounds.y(), bounds.y() + bounds.height());
        float x2 = Math.max(bounds.x(), bounds.x() + bounds.width());
        float y2 = Math.max(bounds.y(), bounds.y() + bounds.height());
        float width = x2 - x1;
        float height = y2 - y1;
        if (width <= 0.0f || height <= 0.0f) return;
        Paint paint = command.paint();
        float radius = clamp(Math.abs(command.radius()), 0.0f, Math.min(width, height) * 0.5f);
        if (radius <= 0.5f) {
            appendRect(bounds, paint, opacity);
            return;
        }
        int color = argb(paint.color(), opacity);
        if ((color >>> 24) == 0) return;

        int cornerSegments = cornerSegments(radius);
        points.clear();
        roundedOutline(points, x1, y1, x2, y2, radius, cornerSegments);
        if (!paint.isStroke()) {
            fan((x1 + x2) * 0.5f, (y1 + y2) * 0.5f, points, color);
            return;
        }
        float thickness = Math.min(positiveThickness(paint.strokeWidth()), Math.min(width, height) * 0.5f);
        float innerX1 = x1 + thickness;
        float innerY1 = y1 + thickness;
        float innerX2 = x2 - thickness;
        float innerY2 = y2 - thickness;
        if (innerX2 <= innerX1 || innerY2 <= innerY1) {
            fan((x1 + x2) * 0.5f, (y1 + y2) * 0.5f, points, color);
            return;
        }
        innerPoints.clear();
        roundedOutline(innerPoints, innerX1, innerY1, innerX2, innerY2, Math.max(0.0f, radius - thickness), cornerSegments);
        ring(points, innerPoints, color);
    }

    private void appendLine(DrawCommand command, float opacity) {
        RectView bounds = command.bounds();
        float x1 = bounds.x();
        float y1 = bounds.y();
        float x2 = bounds.x() + bounds.width();
        float y2 = bounds.y() + bounds.height();
        float thickness = positiveThickness(command.paint().strokeWidth());
        if (thickness <= 1.0f) {
            // A one pixel line straddling two pixel rows would be drawn as two half-bright rows.
            if (Math.abs(y2 - y1) < 0.0001f) {
                y1 = y2 = pixelCenter(y1);
            } else if (Math.abs(x2 - x1) < 0.0001f) {
                x1 = x2 = pixelCenter(x1);
            }
        }
        int color = argb(command.paint().color(), opacity);
        if ((color >>> 24) == 0) return;
        lineQuad(x1, y1, x2, y2, thickness, color);
    }

    private void appendCircle(DrawCommand command, float opacity) {
        RectView bounds = command.bounds();
        Paint paint = command.paint();
        float cx = bounds.x() + bounds.width() * 0.5f;
        float cy = bounds.y() + bounds.height() * 0.5f;
        float rx = Math.abs(bounds.width()) * 0.5f;
        float ry = Math.abs(bounds.height()) * 0.5f;
        if (rx <= 0.0f || ry <= 0.0f) return;
        int color = argb(paint.color(), opacity);
        if ((color >>> 24) == 0) return;
        int segments = command.segments() > 0 ? command.segments() : curveSegments(Math.max(rx, ry));
        points.clear();
        arc(points, cx, cy, rx, ry, 0.0f, TAU, segments);
        if (paint.isStroke()) {
            polylineStroke(points, positiveThickness(paint.strokeWidth()), color, true);
        } else {
            fan(cx, cy, points, color);
        }
    }

    private void appendPath(DrawCommand command, float opacity) {
        VectorPath path = command.path();
        if (path == null || path.isEmpty()) return;
        Paint paint = command.paint();
        ColorView color = paint.color();
        sinkRgb = (channel(color.r()) << 16) | (channel(color.g()) << 8) | channel(color.b());
        sinkAlpha = clamp(color.a(), 0.0f, 1.0f) * clamp(opacity, 0.0f, 1.0f);
        if (channel(sinkAlpha) == 0) return;

        RectView bounds = command.bounds();
        float fringe = fringeWidth();
        float pixelsPerLocalUnit = fringe > 0.0f ? 1.0f / fringe : pixelsPerUnit;
        points.clear();
        float currentX = bounds.x();
        float currentY = bounds.y();
        float startX = currentX;
        float startY = currentY;

        Object[] elements = path.elementElements();
        for (int i = 0, size = path.size(); i < size; i++) {
            VectorPath.Element element = (VectorPath.Element) elements[i];
            switch (element.verb()) {
                case MOVE_TO -> {
                    flushPath(paint, false, fringe);
                    currentX = bounds.x() + element.x1();
                    currentY = bounds.y() + element.y1();
                    startX = currentX;
                    startY = currentY;
                    points.add(currentX, currentY);
                }
                case LINE_TO -> {
                    currentX = bounds.x() + element.x1();
                    currentY = bounds.y() + element.y1();
                    points.add(currentX, currentY);
                }
                case QUADRATIC_TO -> {
                    float controlX = bounds.x() + element.x1();
                    float controlY = bounds.y() + element.y1();
                    float nextX = bounds.x() + element.x2();
                    float nextY = bounds.y() + element.y2();
                    int segments = quadraticSegments(currentX, currentY, controlX, controlY, nextX, nextY,
                            pixelsPerLocalUnit);
                    for (int step = 1; step <= segments; step++) {
                        float t = step / (float) segments;
                        points.add(quadratic(currentX, controlX, nextX, t), quadratic(currentY, controlY, nextY, t));
                    }
                    currentX = nextX;
                    currentY = nextY;
                }
                case CUBIC_TO -> {
                    float controlX1 = bounds.x() + element.x1();
                    float controlY1 = bounds.y() + element.y1();
                    float controlX2 = bounds.x() + element.x2();
                    float controlY2 = bounds.y() + element.y2();
                    float nextX = bounds.x() + element.x3();
                    float nextY = bounds.y() + element.y3();
                    int segments = cubicSegments(currentX, currentY, controlX1, controlY1, controlX2, controlY2,
                            nextX, nextY, pixelsPerLocalUnit);
                    for (int step = 1; step <= segments; step++) {
                        float t = step / (float) segments;
                        points.add(cubic(currentX, controlX1, controlX2, nextX, t),
                                cubic(currentY, controlY1, controlY2, nextY, t));
                    }
                    currentX = nextX;
                    currentY = nextY;
                }
                case CLOSE -> {
                    if (points.size() > 0 && (currentX != startX || currentY != startY)) points.add(startX, startY);
                    flushPath(paint, true, fringe);
                    currentX = startX;
                    currentY = startY;
                }
            }
        }
        flushPath(paint, false, fringe);
    }

    /**
     * An image: bounds filled with the command's UV rectangle, tinted by the paint color. A radius rounds the corners
     * by turning the quad into a fan over the rounded outline, with UVs interpolated from the position.
     */
    private void appendTexture(DrawCommand command, float opacity) {
        RectView bounds = command.bounds();
        float width = bounds.width();
        float height = bounds.height();
        if (Math.abs(width) <= 0.0001f || Math.abs(height) <= 0.0001f) return;
        int color = argb(command.paint().color(), opacity);
        if ((color >>> 24) == 0) return;
        RectView uv = command.uv();

        float x1 = Math.min(bounds.x(), bounds.x() + width);
        float y1 = Math.min(bounds.y(), bounds.y() + height);
        float x2 = Math.max(bounds.x(), bounds.x() + width);
        float y2 = Math.max(bounds.y(), bounds.y() + height);
        float radius = clamp(Math.abs(command.radius()), 0.0f, Math.min(x2 - x1, y2 - y1) * 0.5f);
        if (radius <= 0.5f) {
            texturedVertex(bounds, uv, x1, y1, color);
            texturedVertex(bounds, uv, x1, y2, color);
            texturedVertex(bounds, uv, x2, y2, color);
            texturedVertex(bounds, uv, x1, y1, color);
            texturedVertex(bounds, uv, x2, y2, color);
            texturedVertex(bounds, uv, x2, y1, color);
            return;
        }
        points.clear();
        roundedOutline(points, x1, y1, x2, y2, radius, cornerSegments(radius));
        float centerX = (x1 + x2) * 0.5f;
        float centerY = (y1 + y2) * 0.5f;
        int count = points.size();
        for (int i = 0; i < count; i++) {
            int next = (i + 1) % count;
            texturedVertex(bounds, uv, centerX, centerY, color);
            texturedVertex(bounds, uv, points.x(i), points.y(i), color);
            texturedVertex(bounds, uv, points.x(next), points.y(next), color);
        }
    }

    /** A free quad with its own UV at each corner (rotated or skewed images), drawn as two triangles. */
    private void appendTexturedQuad(DrawCommand command, float opacity) {
        int color = argb(command.paint().color(), opacity);
        if ((color >>> 24) == 0) return;
        vertex(command.quadX1(), command.quadY1(), command.quadU1(), command.quadV1(), color);
        vertex(command.quadX2(), command.quadY2(), command.quadU2(), command.quadV2(), color);
        vertex(command.quadX3(), command.quadY3(), command.quadU3(), command.quadV3(), color);
        vertex(command.quadX1(), command.quadY1(), command.quadU1(), command.quadV1(), color);
        vertex(command.quadX3(), command.quadY3(), command.quadU3(), command.quadV3(), color);
        vertex(command.quadX4(), command.quadY4(), command.quadU4(), command.quadV4(), color);
    }

    /** Emits a vertex whose UV is the position inside {@code bounds} mapped onto the {@code uv} rectangle. */
    private void texturedVertex(RectView bounds, RectView uv, float x, float y, int color) {
        float horizontal = (x - bounds.x()) / bounds.width();
        float vertical = (y - bounds.y()) / bounds.height();
        vertex(x, y, uv.x() + uv.width() * horizontal, uv.y() + uv.height() * vertical, color);
    }

    private void appendMesh(DrawCommand command, float opacity) {
        DrawMesh mesh = command.mesh();
        if (mesh == null || mesh.isEmpty()) return;
        Object[] vertices = mesh.vertexElements();
        for (int i = 0, count = mesh.vertexCount(); i < count; i++) {
            DrawVertex vertex = (DrawVertex) vertices[i];
            vertex(vertex.x(), vertex.y(), vertex.u(), vertex.v(), argb(vertex.color(), opacity));
        }
    }

    /**
     * Sends the accumulated sub-path through the anti-aliasing tessellator and clears it.
     */
    private void flushPath(Paint paint, boolean closed, float fringe) {
        int count = points.size();
        if (count >= 2) {
            if (paint.isStroke()) {
                pathAntiAliasing.stroke(points.values, count, closed, positiveThickness(paint.strokeWidth()),
                        fringe, this);
            } else if (count >= 3) {
                pathAntiAliasing.fillConvex(points.values, count, fringe, this);
            }
        }
        points.clear();
    }

    /**
     * {@link PathAntiAliasing.VertexSink}: path vertices carry their coverage in the alpha channel.
     */
    @Override
    public void vertex(float x, float y, float coverage) {
        int alpha = channel(sinkAlpha * coverage);
        vertex(x, y, 0.0f, 0.0f, (alpha << 24) | sinkRgb);
    }

    // ---- primitives ---------------------------------------------------------------------------------------

    private void quad(float x1, float y1, float x2, float y2, int color) {
        vertex(x1, y1, color);
        vertex(x1, y2, color);
        vertex(x2, y2, color);
        vertex(x1, y1, color);
        vertex(x2, y2, color);
        vertex(x2, y1, color);
    }

    private void lineQuad(float x1, float y1, float x2, float y2, float thickness, int color) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        float half = thickness * 0.5f;
        if (length <= 0.0001f) {
            quad(x1 - half, y1 - half, x1 + half, y1 + half, color);
            return;
        }
        float nx = -dy / length * half;
        float ny = dx / length * half;
        vertex(x1 + nx, y1 + ny, color);
        vertex(x1 - nx, y1 - ny, color);
        vertex(x2 - nx, y2 - ny, color);
        vertex(x1 + nx, y1 + ny, color);
        vertex(x2 - nx, y2 - ny, color);
        vertex(x2 + nx, y2 + ny, color);
    }

    private void fan(float centerX, float centerY, Points outline, int color) {
        int count = uniqueClosedCount(outline);
        if (count < 3) return;
        for (int i = 0; i < count; i++) {
            int next = (i + 1) % count;
            vertex(centerX, centerY, color);
            vertex(outline.x(i), outline.y(i), color);
            vertex(outline.x(next), outline.y(next), color);
        }
    }

    private void ring(Points outer, Points inner, int color) {
        int count = uniqueClosedCount(outer);
        if (count < 3 || count != uniqueClosedCount(inner)) return;
        for (int i = 0; i < count; i++) {
            int next = (i + 1) % count;
            vertex(outer.x(i), outer.y(i), color);
            vertex(inner.x(i), inner.y(i), color);
            vertex(inner.x(next), inner.y(next), color);
            vertex(outer.x(i), outer.y(i), color);
            vertex(inner.x(next), inner.y(next), color);
            vertex(outer.x(next), outer.y(next), color);
        }
    }

    private void polylineStroke(Points line, float thickness, int color, boolean closed) {
        int count = line.size();
        if (count < 2) return;
        int segmentCount = closed ? count : count - 1;
        if (closed && samePoint(line, 0, count - 1)) segmentCount--;
        for (int i = 0; i < segmentCount; i++) {
            int next = (i + 1) % count;
            lineQuad(line.x(i), line.y(i), line.x(next), line.y(next), thickness, color);
        }
    }

    private void vertex(float x, float y, int color) {
        vertex(x, y, 0.0f, 0.0f, color);
    }

    /**
     * Transforms a command-local vertex by the collapsed command transform and stores it.
     */
    private void vertex(float x, float y, float u, float v, int color) {
        out.add(affine[0] * x + affine[2] * y + affine[4], affine[1] * x + affine[3] * y + affine[5], u, v, color);
    }

    // ---- geometry helpers ---------------------------------------------------------------------------------

    /**
     * Width of one physical pixel in command-local units, or {@code 0} if the transform is degenerate.
     */
    private float fringeWidth() {
        float determinant = Math.abs(affine[0] * affine[3] - affine[1] * affine[2]);
        float scale = pixelsPerUnit * (float) Math.sqrt(determinant);
        return scale > 1.0e-6f && Float.isFinite(scale) ? 1.0f / scale : 0.0f;
    }

    private static void roundedOutline(Points target, float x1, float y1, float x2, float y2, float radius,
                                       int cornerSegments) {
        arc(target, x1 + radius, y1 + radius, radius, radius, (float) Math.PI, (float) (Math.PI * 1.5), cornerSegments);
        arc(target, x2 - radius, y1 + radius, radius, radius, (float) (Math.PI * 1.5), TAU, cornerSegments);
        arc(target, x2 - radius, y2 - radius, radius, radius, 0.0f, (float) (Math.PI * 0.5), cornerSegments);
        arc(target, x1 + radius, y2 - radius, radius, radius, (float) (Math.PI * 0.5), (float) Math.PI, cornerSegments);
    }

    private static void arc(Points target, float cx, float cy, float rx, float ry, float start, float end,
                            int segments) {
        for (int i = 0; i <= segments; i++) {
            float angle = start + (end - start) * i / segments;
            target.add(cx + (float) Math.cos(angle) * rx, cy + (float) Math.sin(angle) * ry);
        }
    }

    /**
     * Points of a closed outline without its repeated first point.
     */
    private static int uniqueClosedCount(Points outline) {
        int count = outline.size();
        return count > 1 && samePoint(outline, 0, count - 1) ? count - 1 : count;
    }

    private static boolean samePoint(Points p, int left, int right) {
        return Math.abs(p.x(left) - p.x(right)) < 0.0001f && Math.abs(p.y(left) - p.y(right)) < 0.0001f;
    }

    /**
     * Arc samples per rounded-rectangle corner.
     */
    static int cornerSegments(float radius) {
        return Math.max(3, Math.min(12, Math.round(radius * 0.4f)));
    }

    /**
     * Samples of a full circle / ellipse outline.
     */
    static int curveSegments(float radius) {
        return Math.max(12, Math.min(64, Math.round(radius * 0.75f)));
    }

    /**
     * Segments of a quadratic curve for a pixel tolerance: the error is at most {@code |P0 - 2 P1 + P2| / (4 n^2)}.
     */
    static int quadraticSegments(float x0, float y0, float x1, float y1, float x2, float y2, float pixelsPerLocalUnit) {
        float sx = x0 - 2.0f * x1 + x2;
        float sy = y0 - 2.0f * y1 + y2;
        float deviation = (float) Math.sqrt(sx * sx + sy * sy) * pixelsPerLocalUnit;
        return clampSegments(Math.sqrt(deviation / (4.0f * CURVE_TOLERANCE_PIXELS)));
    }

    /**
     * Segments of a cubic curve for a pixel tolerance: the error is at most {@code 0.75 D / n^2}, with {@code D} the
     * larger second difference of the control polygon.
     */
    static int cubicSegments(float x0, float y0, float x1, float y1, float x2, float y2, float x3, float y3,
                             float pixelsPerLocalUnit) {
        float ax = x0 - 2.0f * x1 + x2;
        float ay = y0 - 2.0f * y1 + y2;
        float bx = x1 - 2.0f * x2 + x3;
        float by = y1 - 2.0f * y2 + y3;
        float deviation = (float) Math.sqrt(Math.max(ax * ax + ay * ay, bx * bx + by * by)) * pixelsPerLocalUnit;
        return clampSegments(Math.sqrt(0.75f * deviation / CURVE_TOLERANCE_PIXELS));
    }

    private static int clampSegments(double estimate) {
        if (!Double.isFinite(estimate)) return MIN_CURVE_SEGMENTS;
        return Math.max(MIN_CURVE_SEGMENTS, Math.min(MAX_CURVE_SEGMENTS, (int) Math.ceil(estimate)));
    }

    private static float quadratic(float p0, float p1, float p2, float t) {
        float inverse = 1.0f - t;
        return inverse * inverse * p0 + 2.0f * inverse * t * p1 + t * t * p2;
    }

    private static float cubic(float p0, float p1, float p2, float p3, float t) {
        float inverse = 1.0f - t;
        return inverse * inverse * inverse * p0 + 3.0f * inverse * inverse * t * p1
                + 3.0f * inverse * t * t * p2 + t * t * t * p3;
    }

    private static float positiveThickness(float value) {
        return Float.isFinite(value) ? Math.max(MIN_STROKE_WIDTH, value) : MIN_STROKE_WIDTH;
    }

    private static float pixelCenter(float value) {
        return (float) Math.floor(value) + 0.5f;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static int argb(ColorView color, float opacity) {
        return CleanroomRenderMath.argb(color, opacity);
    }

    private static int channel(float value) {
        return Math.max(0, Math.min(255, Math.round(value * 255.0f)));
    }

    /**
     * Reusable polyline of {@code x, y} pairs.
     */
    private static final class Points {
        private float[] values = new float[64];
        private int size;

        private void add(float x, float y) {
            if (size + 2 > values.length) values = Arrays.copyOf(values, values.length * 2);
            values[size++] = x;
            values[size++] = y;
        }

        private int size() {
            return size / 2;
        }

        private float x(int index) {
            return values[index * 2];
        }

        private float y(int index) {
            return values[index * 2 + 1];
        }

        private void clear() {
            size = 0;
        }
    }
}
