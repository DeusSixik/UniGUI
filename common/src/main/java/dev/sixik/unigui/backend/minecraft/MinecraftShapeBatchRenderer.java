package dev.sixik.unigui.backend.minecraft;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.math.Transform;
import dev.sixik.unigui.api.render.BlendMode;
import dev.sixik.unigui.api.render.DrawCommand;
import dev.sixik.unigui.api.render.DrawCommandType;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.render.VectorPath;
import dev.sixik.unigui.impl.render.DrawBatch;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/** Batches untextured UI primitives into one position-color draw call. */
final class MinecraftShapeBatchRenderer implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(MinecraftShapeBatchRenderer.class);
    private static final float TAU = (float) (Math.PI * 2.0);
    private static final float MIN_SCREEN_STROKE_WIDTH = 1.0f;
    private final MinecraftSdfShapeRenderer sdfRenderer = new MinecraftSdfShapeRenderer();

    boolean render(GuiGraphics graphics, DrawBatch batch, boolean renderingToPremultipliedTarget) {
        if (graphics == null || batch == null || batch.size() == 0) return false;
        Object[] rawCommands = batch.commandElements();
        int commandCount = batch.size();
        for (int i = 0; i < commandCount; i++) {
            DrawCommand command = (DrawCommand) rawCommands[i];
            if (command == null || !supports(command.type())) return false;
        }
        if (sdfRenderer.shouldRender(batch)
                && sdfRenderer.render(graphics, batch, renderingToPremultipliedTarget)) {
            return true;
        }

        graphics.flush();
        RenderState state = RenderState.capture();
        try {
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            RenderSystem.enableBlend();
            MinecraftUiBlend.applyStraightAlpha(renderingToPremultipliedTarget, batch.blendMode());
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.disableCull();

            Object buffer = MinecraftBufferCompat.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
            Matrix4f basePose = graphics.pose().last().pose();
            int vertices = 0;
            for (int i = 0; i < commandCount; i++) {
                DrawCommand command = (DrawCommand) rawCommands[i];
                Matrix4f matrix = MinecraftTransform.commandMatrix(basePose, command);
                vertices += append(buffer, matrix, command);
            }
            if (vertices == 0) {
                MinecraftBufferCompat.drawWithShader(buffer);
                return true;
            }
            MinecraftBufferCompat.drawWithShader(buffer);
            return true;
        } catch (Throwable failure) {
            LOGGER.error("UniGUI shape batch failed; falling back to legacy primitive rendering", failure);
            return false;
        } finally {
            state.restore();
        }
    }

    @Override
    public void close() {
        sdfRenderer.close();
    }

    static boolean supports(DrawCommandType type) {
        return type == DrawCommandType.RECT
                || type == DrawCommandType.ROUNDED_RECT
                || type == DrawCommandType.LINE
                || type == DrawCommandType.CIRCLE
                || type == DrawCommandType.PATH;
    }

    private static int append(Object buffer, Matrix4f matrix, DrawCommand command) {
        return switch (command.type()) {
            case RECT -> appendRect(buffer, matrix, command.bounds(), command.paint());
            case ROUNDED_RECT -> appendRoundedRect(buffer, matrix, command);
            case LINE -> appendLineCommand(buffer, matrix, command);
            case CIRCLE -> appendCircle(buffer, matrix, command.bounds(), command.paint());
            case PATH -> appendPath(buffer, matrix, command);
            default -> 0;
        };
    }

    private static int appendRect(Object buffer, Matrix4f matrix, RectView bounds, Paint paint) {
        float x1 = Math.min(bounds.x(), bounds.x() + bounds.width());
        float y1 = Math.min(bounds.y(), bounds.y() + bounds.height());
        float x2 = Math.max(bounds.x(), bounds.x() + bounds.width());
        float y2 = Math.max(bounds.y(), bounds.y() + bounds.height());
        if (x2 <= x1 || y2 <= y1) return 0;
        if (!paint.isStroke()) return quad(buffer, matrix, x1, y1, x2, y2, paint.color());

        float thickness = Math.min(positiveThickness(paint.strokeWidth()), Math.min(x2 - x1, y2 - y1) * 0.5f);
        if (thickness <= 0.0f) return 0;
        int vertices = 0;
        vertices += quad(buffer, matrix, x1, y1, x2, y1 + thickness, paint.color());
        vertices += quad(buffer, matrix, x1, y2 - thickness, x2, y2, paint.color());
        if (y2 - y1 > thickness * 2.0f) {
            vertices += quad(buffer, matrix, x1, y1 + thickness, x1 + thickness, y2 - thickness, paint.color());
            vertices += quad(buffer, matrix, x2 - thickness, y1 + thickness, x2, y2 - thickness, paint.color());
        }
        return vertices;
    }

    private static int appendRoundedRect(Object buffer, Matrix4f matrix, DrawCommand command) {
        RectView bounds = command.bounds();
        float x1 = Math.min(bounds.x(), bounds.x() + bounds.width());
        float y1 = Math.min(bounds.y(), bounds.y() + bounds.height());
        float x2 = Math.max(bounds.x(), bounds.x() + bounds.width());
        float y2 = Math.max(bounds.y(), bounds.y() + bounds.height());
        float width = x2 - x1;
        float height = y2 - y1;
        if (width <= 0.0f || height <= 0.0f) return 0;
        float radius = clamp(Math.abs(command.radius()), 0.0f, Math.min(width, height) * 0.5f);
        if (radius <= 0.5f) return appendRect(buffer, matrix, bounds, command.paint());

        int cornerSegments = cornerSegments(radius);
        FloatPoints outer = roundedOutline(x1, y1, x2, y2, radius, cornerSegments);
        if (!command.paint().isStroke()) {
            return fillFan(buffer, matrix, outer, (x1 + x2) * 0.5f, (y1 + y2) * 0.5f,
                    command.paint().color());
        }

        float thickness = Math.min(positiveThickness(command.paint().strokeWidth()),
                Math.min(width, height) * 0.5f);
        float innerX1 = x1 + thickness;
        float innerY1 = y1 + thickness;
        float innerX2 = x2 - thickness;
        float innerY2 = y2 - thickness;
        if (innerX2 <= innerX1 || innerY2 <= innerY1) {
            return fillFan(buffer, matrix, outer, (x1 + x2) * 0.5f, (y1 + y2) * 0.5f,
                    command.paint().color());
        }

        float innerRadius = Math.max(0.0f, radius - thickness);
        FloatPoints inner = roundedOutline(innerX1, innerY1, innerX2, innerY2,
                innerRadius, cornerSegments);
        return strokeRing(buffer, matrix, outer, inner, command.paint().color());
    }

    private static int appendLineCommand(Object buffer, Matrix4f matrix, DrawCommand command) {
        RectView bounds = command.bounds();
        float x1 = bounds.x();
        float y1 = bounds.y();
        float x2 = bounds.x() + bounds.width();
        float y2 = bounds.y() + bounds.height();
        float thickness = positiveThickness(command.paint().strokeWidth());
        if (thickness <= 1.0f) {
            if (Math.abs(y2 - y1) < 0.0001f) {
                y1 = y2 = pixelCenter(y1);
            } else if (Math.abs(x2 - x1) < 0.0001f) {
                x1 = x2 = pixelCenter(x1);
            }
        }
        return lineQuad(buffer, matrix, x1, y1, x2, y2, thickness, command.paint().color());
    }

    private static int appendCircle(Object buffer, Matrix4f matrix, RectView bounds, Paint paint) {
        float cx = bounds.x() + bounds.width() * 0.5f;
        float cy = bounds.y() + bounds.height() * 0.5f;
        float rx = Math.abs(bounds.width()) * 0.5f;
        float ry = Math.abs(bounds.height()) * 0.5f;
        if (rx <= 0.0f || ry <= 0.0f) return 0;
        int segments = curveSegments(Math.max(rx, ry));
        FloatPoints outline = new FloatPoints(segments);
        appendArc(outline, cx, cy, rx, ry, 0.0f, TAU, segments, true);
        return paint.isStroke()
                ? strokeClosed(buffer, matrix, outline, paint)
                : fillFan(buffer, matrix, outline, cx, cy, paint.color());
    }

    private static int appendPath(Object buffer, Matrix4f matrix, DrawCommand command) {
        VectorPath path = command.path();
        if (path == null || path.isEmpty()) return 0;
        RectView bounds = command.bounds();
        FloatPoints points = new FloatPoints(32);
        int vertices = 0;
        float currentX = bounds.x();
        float currentY = bounds.y();
        float startX = currentX;
        float startY = currentY;

        Object[] rawPathElements = path.elementElements();
        for (int i = 0, size = path.size(); i < size; i++) {
            VectorPath.Element element = (VectorPath.Element) rawPathElements[i];
            switch (element.verb()) {
                case MOVE_TO -> {
                    vertices += flushPath(buffer, matrix, points, command.paint(), false);
                    points.clear();
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
                    int segments = pathSegments(currentX, currentY, nextX, nextY);
                    for (int segmentIndex = 1; segmentIndex <= segments; segmentIndex++) {
                        float t = segmentIndex / (float) segments;
                        points.add(quadratic(currentX, controlX, nextX, t),
                                quadratic(currentY, controlY, nextY, t));
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
                    int segments = pathSegments(currentX, currentY, nextX, nextY);
                    for (int segmentIndex = 1; segmentIndex <= segments; segmentIndex++) {
                        float t = segmentIndex / (float) segments;
                        points.add(cubic(currentX, controlX1, controlX2, nextX, t),
                                cubic(currentY, controlY1, controlY2, nextY, t));
                    }
                    currentX = nextX;
                    currentY = nextY;
                }
                case CLOSE -> {
                    if (points.size() > 0 && (currentX != startX || currentY != startY)) {
                        points.add(startX, startY);
                    }
                    vertices += flushPath(buffer, matrix, points, command.paint(), true);
                    points.clear();
                    currentX = startX;
                    currentY = startY;
                }
            }
        }
        return vertices + flushPath(buffer, matrix, points, command.paint(), false);
    }

    private static int flushPath(Object buffer, Matrix4f matrix, FloatPoints points,
                                 Paint paint, boolean closed) {
        if (points.size() < 2) return 0;
        if (paint.isStroke()) return stroke(buffer, matrix, points, paint, closed);
        if (points.size() < 3) return 0;
        float centerX = 0.0f;
        float centerY = 0.0f;
        int count = closed && samePoint(points, 0, points.size() - 1) ? points.size() - 1 : points.size();
        for (int i = 0; i < count; i++) {
            centerX += points.x(i);
            centerY += points.y(i);
        }
        return fillFan(buffer, matrix, points, centerX / count, centerY / count, paint.color(), count);
    }

    private static int fillFan(Object buffer, Matrix4f matrix, FloatPoints points,
                               float centerX, float centerY, ColorView color) {
        return fillFan(buffer, matrix, points, centerX, centerY, color, points.size());
    }

    private static int fillFan(Object buffer, Matrix4f matrix, FloatPoints points,
                               float centerX, float centerY, ColorView color, int count) {
        if (count < 3) return 0;
        for (int i = 0; i < count; i++) {
            int next = (i + 1) % count;
            vertex(buffer, matrix, centerX, centerY, color);
            vertex(buffer, matrix, points.x(i), points.y(i), color);
            vertex(buffer, matrix, points.x(next), points.y(next), color);
        }
        return count * 3;
    }

    private static FloatPoints roundedOutline(float x1, float y1, float x2, float y2,
                                               float radius, int cornerSegments) {
        FloatPoints outline = new FloatPoints((cornerSegments + 1) * 4);
        appendArc(outline, x1 + radius, y1 + radius, radius, radius,
                (float) Math.PI, (float) (Math.PI * 1.5), cornerSegments, true);
        appendArc(outline, x2 - radius, y1 + radius, radius, radius,
                (float) (Math.PI * 1.5), TAU, cornerSegments, true);
        appendArc(outline, x2 - radius, y2 - radius, radius, radius,
                0.0f, (float) (Math.PI * 0.5), cornerSegments, true);
        appendArc(outline, x1 + radius, y2 - radius, radius, radius,
                (float) (Math.PI * 0.5), (float) Math.PI, cornerSegments, true);
        return outline;
    }

    private static int strokeRing(Object buffer, Matrix4f matrix,
                                  FloatPoints outer, FloatPoints inner, ColorView color) {
        int count = uniqueClosedCount(outer);
        if (count < 3 || count != uniqueClosedCount(inner)) return 0;
        for (int i = 0; i < count; i++) {
            int next = (i + 1) % count;
            vertex(buffer, matrix, outer.x(i), outer.y(i), color);
            vertex(buffer, matrix, inner.x(i), inner.y(i), color);
            vertex(buffer, matrix, inner.x(next), inner.y(next), color);
            vertex(buffer, matrix, outer.x(i), outer.y(i), color);
            vertex(buffer, matrix, inner.x(next), inner.y(next), color);
            vertex(buffer, matrix, outer.x(next), outer.y(next), color);
        }
        return count * 6;
    }

    private static int uniqueClosedCount(FloatPoints points) {
        int count = points.size();
        return count > 1 && samePoint(points, 0, count - 1) ? count - 1 : count;
    }

    private static int strokeClosed(Object buffer, Matrix4f matrix, FloatPoints points, Paint paint) {
        return stroke(buffer, matrix, points, paint, true);
    }

    private static int stroke(Object buffer, Matrix4f matrix, FloatPoints points,
                              Paint paint, boolean closed) {
        int count = points.size();
        if (count < 2) return 0;
        int segmentCount = closed ? count : count - 1;
        if (closed && samePoint(points, 0, count - 1)) segmentCount--;
        int vertices = 0;
        for (int i = 0; i < segmentCount; i++) {
            int next = (i + 1) % count;
            vertices += lineQuad(buffer, matrix, points.x(i), points.y(i),
                    points.x(next), points.y(next), positiveThickness(paint.strokeWidth()), paint.color());
        }
        return vertices;
    }

    private static int quad(Object buffer, Matrix4f matrix,
                            float x1, float y1, float x2, float y2, ColorView color) {
        vertex(buffer, matrix, x1, y1, color);
        vertex(buffer, matrix, x1, y2, color);
        vertex(buffer, matrix, x2, y2, color);
        vertex(buffer, matrix, x1, y1, color);
        vertex(buffer, matrix, x2, y2, color);
        vertex(buffer, matrix, x2, y1, color);
        return 6;
    }

    private static int lineQuad(Object buffer, Matrix4f matrix,
                                float x1, float y1, float x2, float y2,
                                float thickness, ColorView color) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length <= 0.0001f) {
            float half = thickness * 0.5f;
            return quad(buffer, matrix, x1 - half, y1 - half, x1 + half, y1 + half, color);
        }
        float half = thickness * 0.5f;
        float nx = -dy / length * half;
        float ny = dx / length * half;
        vertex(buffer, matrix, x1 + nx, y1 + ny, color);
        vertex(buffer, matrix, x1 - nx, y1 - ny, color);
        vertex(buffer, matrix, x2 - nx, y2 - ny, color);
        vertex(buffer, matrix, x1 + nx, y1 + ny, color);
        vertex(buffer, matrix, x2 - nx, y2 - ny, color);
        vertex(buffer, matrix, x2 + nx, y2 + ny, color);
        return 6;
    }

    private static void vertex(Object buffer, Matrix4f matrix, float x, float y, ColorView color) {
        MinecraftBufferCompat.colorVertex(buffer, matrix, x, y, argb(color));
    }

    private static int argb(ColorView color) {
        int a = channel(color.a());
        int r = channel(color.r());
        int g = channel(color.g());
        int b = channel(color.b());
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static Matrix4f commandMatrix(Matrix4f basePose, RectView bounds, Transform transform) {
        Matrix4f matrix = new Matrix4f(basePose);
        float pivotX = bounds.x() + transform.pivot().x();
        float pivotY = bounds.y() + transform.pivot().y();
        matrix.translate(transform.position().x(), transform.position().y(), 0.0f);
        matrix.translate(pivotX, pivotY, 0.0f);
        if (transform.rotationDegrees() != 0.0f) {
            matrix.rotateZ((float) Math.toRadians(transform.rotationDegrees()));
        }
        matrix.scale(transform.scale().x(), transform.scale().y(), 1.0f);
        matrix.translate(-pivotX, -pivotY, 0.0f);
        return matrix;
    }

    private static void appendArc(FloatPoints points, float cx, float cy, float rx, float ry,
                                  float start, float end, int segments, boolean includeStart) {
        int first = includeStart ? 0 : 1;
        for (int i = first; i <= segments; i++) {
            float angle = start + (end - start) * i / segments;
            points.add(cx + (float) Math.cos(angle) * rx, cy + (float) Math.sin(angle) * ry);
        }
    }

    private static boolean samePoint(FloatPoints points, int left, int right) {
        return Math.abs(points.x(left) - points.x(right)) < 0.0001f
                && Math.abs(points.y(left) - points.y(right)) < 0.0001f;
    }

    private static int cornerSegments(float radius) {
        return Math.max(3, Math.min(12, Math.round(radius * 0.4f)));
    }

    private static int curveSegments(float radius) {
        return Math.max(12, Math.min(64, Math.round(radius * 0.75f)));
    }

    private static int pathSegments(float x1, float y1, float x2, float y2) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        return Math.max(4, Math.min(32, Math.round((float) Math.sqrt(dx * dx + dy * dy) * 0.2f)));
    }

    private static float quadratic(float x0, float x1, float x2, float t) {
        float inverse = 1.0f - t;
        return inverse * inverse * x0 + 2.0f * inverse * t * x1 + t * t * x2;
    }

    private static float cubic(float x0, float x1, float x2, float x3, float t) {
        float inverse = 1.0f - t;
        return inverse * inverse * inverse * x0
                + 3.0f * inverse * inverse * t * x1
                + 3.0f * inverse * t * t * x2
                + t * t * t * x3;
    }

    private static float positiveThickness(float value) {
        return Float.isFinite(value) ? Math.max(MIN_SCREEN_STROKE_WIDTH, value) : MIN_SCREEN_STROKE_WIDTH;
    }

    private static float pixelCenter(float value) {
        return (float) Math.floor(value) + 0.5f;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static int channel(float value) {
        return Math.max(0, Math.min(255, Math.round(value * 255.0f)));
    }

    private static final class FloatPoints {
        private float[] values;
        private int size;

        private FloatPoints(int capacity) {
            values = new float[Math.max(4, capacity * 2)];
        }

        private void add(float x, float y) {
            ensure(size + 2);
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

        private void ensure(int required) {
            if (required <= values.length) return;
            values = java.util.Arrays.copyOf(values, Math.max(required, values.length * 2));
        }
    }

    private record RenderState(boolean blend, int blendSourceRgb, int blendDestinationRgb,
                               int blendSourceAlpha, int blendDestinationAlpha,
                               boolean depthTest, boolean depthMask, boolean cull) {
        private static RenderState capture() {
            return new RenderState(
                    GL11.glIsEnabled(GL11.GL_BLEND),
                    GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB),
                    GL11.glGetInteger(GL14.GL_BLEND_DST_RGB),
                    GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA),
                    GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA),
                    GL11.glIsEnabled(GL11.GL_DEPTH_TEST),
                    GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK),
                    GL11.glIsEnabled(GL11.GL_CULL_FACE));
        }

        private void restore() {
            RenderSystem.blendFuncSeparate(blendSourceRgb, blendDestinationRgb,
                    blendSourceAlpha, blendDestinationAlpha);
            if (blend) RenderSystem.enableBlend();
            else RenderSystem.disableBlend();
            RenderSystem.depthMask(depthMask);
            if (depthTest) RenderSystem.enableDepthTest();
            else RenderSystem.disableDepthTest();
            if (cull) RenderSystem.enableCull();
            else RenderSystem.disableCull();
        }
    }
}
