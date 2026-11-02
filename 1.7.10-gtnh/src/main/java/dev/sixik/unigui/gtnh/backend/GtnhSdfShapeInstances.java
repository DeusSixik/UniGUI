package dev.sixik.unigui.gtnh.backend;

import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.render.DrawCommand;
import dev.sixik.unigui.api.render.DrawCommandType;
import dev.sixik.unigui.impl.render.DrawBatch;
import dev.sixik.unigui.impl.render.shaders.SdfShapeShaderSources;

import java.nio.FloatBuffer;

/**
 * Decides which shape commands go to the SDF shader and packs them into the per-instance layout described by
 * {@link SdfShapeShaderSources}. Pure Java (no OpenGL): the renderer only uploads the resulting buffer.
 *
 * <p>Per instance, in order: four transformed corner positions (3 floats each), local bounds (4), color (4),
 * size (2), line start (2), line end (2), {@code radius, strokeWidth} (2), {@code shapeType, isStroke} (2) = 30.</p>
 */
final class GtnhSdfShapeInstances {
    private static final float FRACTION_EPSILON = 0.001f;

    private GtnhSdfShapeInstances() {
    }

    /**
     * @param type command type
     * @return {@code true} if the SDF shader can draw this command type
     */
    static boolean supports(DrawCommandType type) {
        return type == DrawCommandType.RECT
            || type == DrawCommandType.ROUNDED_RECT
            || type == DrawCommandType.LINE
            || type == DrawCommandType.CIRCLE;
    }

    /**
     * Whether SDF rendering produces a visibly different result from plain tessellation. Axis-aligned integer
     * rectangles are drawn as is by the cheaper path.
     *
     * @param command shape command
     * @return {@code true} if the command needs anti-aliased distance-field edges
     */
    static boolean needsSdf(DrawCommand command) {
        return switch (command.type()) {
            case ROUNDED_RECT -> Math.abs(command.radius()) > 0.5f || command.paint().isStroke();
            case CIRCLE, LINE -> true;
            case RECT -> command.paint().isStroke()
                || isFractional(command.bounds().x())
                || isFractional(command.bounds().y())
                || isFractional(command.bounds().width())
                || isFractional(command.bounds().height());
            default -> false;
        };
    }

    /**
     * A batch is rendered by the SDF shader only when every command is supported (so draw order is preserved
     * inside a single pass) and at least one of them needs it.
     *
     * @param batch candidate batch
     * @return {@code true} if the whole batch should use the SDF path
     */
    static boolean shouldRender(DrawBatch batch) {
        if (batch == null || batch.size() == 0) return false;
        boolean needsSdf = false;
        Object[] commands = batch.commandElements();
        for (int i = 0, size = batch.size(); i < size; i++) {
            DrawCommand command = (DrawCommand) commands[i];
            if (command == null || !supports(command.type())) return false;
            needsSdf |= needsSdf(command);
        }
        return needsSdf;
    }

    /**
     * Appends one instance to {@code target}.
     *
     * @param target  destination buffer, must have {@link SdfShapeShaderSources#INSTANCE_FLOATS} floats remaining
     * @param command shape command
     * @param opacity extra alpha multiplier of the surface
     * @param scratch two-element scratch array
     * @return {@code false} (and nothing written) for degenerate shapes
     */
    static boolean pack(FloatBuffer target, DrawCommand command, float opacity, float[] scratch) {
        RectView bounds = command.bounds();
        float x1 = Math.min(bounds.x(), bounds.x() + bounds.width());
        float y1 = Math.min(bounds.y(), bounds.y() + bounds.height());
        float x2 = Math.max(bounds.x(), bounds.x() + bounds.width());
        float y2 = Math.max(bounds.y(), bounds.y() + bounds.height());
        float width = x2 - x1;
        float height = y2 - y1;
        boolean line = command.type() == DrawCommandType.LINE;
        // A horizontal line has zero height and a vertical one zero width: their quad is expanded by the
        // thickness below, so the degenerate-rectangle check only applies to the other shapes.
        if (!line && (width <= 0.0f || height <= 0.0f)) return false;

        int shapeType;
        float radius = 0.0f;
        float lineEndX = 0.0f;
        float lineEndY = 0.0f;
        float quadX1;
        float quadY1;
        float quadX2;
        float quadY2;
        float localX1;
        float localY1;
        float localX2;
        float localY2;
        float strokeWidth = positiveThickness(command.paint().strokeWidth());
        boolean stroke = command.paint().isStroke();

        if (line) {
            shapeType = SdfShapeShaderSources.SHAPE_LINE;
            lineEndX = bounds.width();
            lineEndY = bounds.height();
            float minX = Math.min(0.0f, lineEndX);
            float minY = Math.min(0.0f, lineEndY);
            float maxX = Math.max(0.0f, lineEndX);
            float maxY = Math.max(0.0f, lineEndY);
            float pad = strokeWidth * 0.5f + SdfShapeShaderSources.AA_PAD;
            quadX1 = bounds.x() + minX - pad;
            quadY1 = bounds.y() + minY - pad;
            quadX2 = bounds.x() + maxX + pad;
            quadY2 = bounds.y() + maxY + pad;
            localX1 = minX - pad;
            localY1 = minY - pad;
            localX2 = maxX + pad;
            localY2 = maxY + pad;
            width = Math.max(1.0f, maxX - minX);
            height = Math.max(1.0f, maxY - minY);
            stroke = false;
        } else {
            shapeType = command.type() == DrawCommandType.CIRCLE
                ? SdfShapeShaderSources.SHAPE_ELLIPSE
                : SdfShapeShaderSources.SHAPE_ROUNDED_RECT;
            if (command.type() == DrawCommandType.ROUNDED_RECT) {
                radius = clamp(Math.abs(command.radius()), 0.0f, Math.min(width, height) * 0.5f);
            }
            float pad = SdfShapeShaderSources.AA_PAD;
            quadX1 = x1 - pad;
            quadY1 = y1 - pad;
            quadX2 = x2 + pad;
            quadY2 = y2 + pad;
            localX1 = -pad;
            localY1 = -pad;
            localX2 = width + pad;
            localY2 = height + pad;
        }

        // Corner order matches the instanced vertex shader: (0,0), (0,1), (1,1), (1,0) of the unit quad.
        writePosition(target, command, quadX1, quadY1, scratch);
        writePosition(target, command, quadX1, quadY2, scratch);
        writePosition(target, command, quadX2, quadY2, scratch);
        writePosition(target, command, quadX2, quadY1, scratch);
        target.put(localX1).put(localY1).put(localX2).put(localY2);

        ColorView color = command.paint().color();
        target.put(color.r()).put(color.g()).put(color.b()).put(color.a() * clamp(opacity, 0.0f, 1.0f));
        target.put(width).put(height);
        target.put(0.0f).put(0.0f);
        target.put(lineEndX).put(lineEndY);
        target.put(radius).put(strokeWidth);
        target.put(shapeType).put(stroke ? 1.0f : 0.0f);
        return true;
    }

    private static void writePosition(FloatBuffer target, DrawCommand command, float x, float y, float[] scratch) {
        scratch[0] = x;
        scratch[1] = y;
        GtnhRenderMath.transformPoint(scratch, command.bounds(), command.transform(),
            command.transformStackElements(), command.transformStackSize());
        target.put(scratch[0]).put(scratch[1]).put(0.0f);
    }

    private static boolean isFractional(float value) {
        return Math.abs(value - Math.round(value)) > FRACTION_EPSILON;
    }

    private static float positiveThickness(float value) {
        return Float.isFinite(value) ? Math.max(SdfShapeShaderSources.MIN_STROKE_WIDTH, value) : 1.0f;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
