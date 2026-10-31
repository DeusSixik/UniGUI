package dev.sixik.unigui.gtnh.backend;

import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.math.Transform;
import dev.sixik.unigui.api.render.DrawCommand;
import dev.sixik.unigui.api.render.DrawCommandType;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.render.TransformLayer;
import dev.sixik.unigui.impl.render.shaders.SdfShapeShaderSources;
import org.junit.jupiter.api.Test;

import java.nio.FloatBuffer;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GtnhSdfShapeInstancesTest {
    private static final float EPS = 1.0e-4f;
    private static final int FLOATS = SdfShapeShaderSources.INSTANCE_FLOATS;
    // Offsets inside one packed instance.
    private static final int LOCAL = 12;
    private static final int COLOR = 16;
    private static final int SIZE = 20;
    private static final int LINE_START = 22;
    private static final int LINE_END = 24;
    private static final int SHAPE = 26;
    private static final int FLAGS = 28;

    private final float[] scratch = new float[2];

    @Test
    void supports_onlyDistanceFieldShapes() {
        assertTrue(GtnhSdfShapeInstances.supports(DrawCommandType.RECT));
        assertTrue(GtnhSdfShapeInstances.supports(DrawCommandType.ROUNDED_RECT));
        assertTrue(GtnhSdfShapeInstances.supports(DrawCommandType.CIRCLE));
        assertTrue(GtnhSdfShapeInstances.supports(DrawCommandType.LINE));
        assertFalse(GtnhSdfShapeInstances.supports(DrawCommandType.PATH));
        assertFalse(GtnhSdfShapeInstances.supports(DrawCommandType.TEXT));
        assertFalse(GtnhSdfShapeInstances.supports(DrawCommandType.PUSH_CLIP));
    }

    @Test
    void needsSdf_integerFilledRect_isDrawnByTessellation() {
        assertFalse(GtnhSdfShapeInstances.needsSdf(rect(1, 2, 30, 40, false)));
    }

    @Test
    void needsSdf_fractionalOrStrokedRect_usesShader() {
        assertTrue(GtnhSdfShapeInstances.needsSdf(rect(1.5f, 2, 30, 40, false)));
        assertTrue(GtnhSdfShapeInstances.needsSdf(rect(1, 2, 30, 40.25f, false)));
        assertTrue(GtnhSdfShapeInstances.needsSdf(rect(1, 2, 30, 40, true)));
    }

    @Test
    void needsSdf_roundedRectNeedsRadiusOverHalfPixel() {
        assertFalse(GtnhSdfShapeInstances.needsSdf(rounded(0, 0, 10, 10, 0.5f)));
        assertTrue(GtnhSdfShapeInstances.needsSdf(rounded(0, 0, 10, 10, 2.0f)));
    }

    @Test
    void needsSdf_circleAndLineAlways() {
        assertTrue(GtnhSdfShapeInstances.needsSdf(command(DrawCommandType.CIRCLE, 0, 0, 10, 10, false)));
        assertTrue(GtnhSdfShapeInstances.needsSdf(command(DrawCommandType.LINE, 0, 0, 10, 0, false)));
    }

    @Test
    void pack_roundedRect_writesAllThirtyFloatsInLayoutOrder() {
        FloatBuffer buffer = FloatBuffer.allocate(FLOATS);
        DrawCommand command = rounded(10, 20, 40, 30, 6);

        assertTrue(GtnhSdfShapeInstances.pack(buffer, command, 1.0f, scratch));
        assertEquals(FLOATS, buffer.position());
        float[] data = buffer.array();

        // Corners are padded by one pixel and ordered (0,0), (0,1), (1,1), (1,0).
        assertArrayEquals(new float[]{9, 19, 0, 9, 51, 0, 51, 51, 0, 51, 19, 0}, slice(data, 0, 12), EPS);
        assertArrayEquals(new float[]{-1, -1, 41, 31}, slice(data, LOCAL, 4), EPS);
        assertArrayEquals(new float[]{0.25f, 0.5f, 0.75f, 1.0f}, slice(data, COLOR, 4), EPS);
        assertArrayEquals(new float[]{40, 30}, slice(data, SIZE, 2), EPS);
        // A fill paint has no stroke width: the shader gets the minimum instead of zero to avoid dividing by it.
        assertArrayEquals(new float[]{6, SdfShapeShaderSources.MIN_STROKE_WIDTH}, slice(data, SHAPE, 2), EPS);
        assertArrayEquals(new float[]{SdfShapeShaderSources.SHAPE_ROUNDED_RECT, 0}, slice(data, FLAGS, 2), EPS);
    }

    @Test
    void pack_radiusIsClampedToHalfOfShorterSide() {
        FloatBuffer buffer = FloatBuffer.allocate(FLOATS);
        GtnhSdfShapeInstances.pack(buffer, rounded(0, 0, 40, 10, 100), 1.0f, scratch);
        assertEquals(5.0f, buffer.array()[SHAPE], EPS);
    }

    @Test
    void pack_stroke_setsFlagAndWidth() {
        FloatBuffer buffer = FloatBuffer.allocate(FLOATS);
        DrawCommand command = new DrawCommand(DrawCommandType.RECT).bounds(new MutableRect(0, 0, 10, 10))
                .paint(Paint.stroke(new MutableColor(1, 1, 1, 1), 3.0f));
        GtnhSdfShapeInstances.pack(buffer, command, 1.0f, scratch);
        assertEquals(3.0f, buffer.array()[SHAPE + 1], EPS);
        assertEquals(1.0f, buffer.array()[FLAGS + 1], EPS);
    }

    @Test
    void pack_circle_usesEllipseShapeType() {
        FloatBuffer buffer = FloatBuffer.allocate(FLOATS);
        GtnhSdfShapeInstances.pack(buffer, command(DrawCommandType.CIRCLE, 0, 0, 10, 10, false), 1.0f, scratch);
        assertEquals(SdfShapeShaderSources.SHAPE_ELLIPSE, buffer.array()[FLAGS], EPS);
    }

    @Test
    void pack_horizontalLine_isNotRejectedAndCarriesEndPoint() {
        FloatBuffer buffer = FloatBuffer.allocate(FLOATS);
        DrawCommand line = new DrawCommand(DrawCommandType.LINE).bounds(new MutableRect(5, 7, 20, 0))
                .paint(Paint.stroke(new MutableColor(1, 1, 1, 1), 2.0f));

        assertTrue(GtnhSdfShapeInstances.pack(buffer, line, 1.0f, scratch));
        float[] data = buffer.array();
        assertArrayEquals(new float[]{20, 0}, slice(data, LINE_END, 2), EPS);
        assertArrayEquals(new float[]{0, 0}, slice(data, LINE_START, 2), EPS);
        assertEquals(SdfShapeShaderSources.SHAPE_LINE, data[FLAGS], EPS);
        // Lines are always filled quads regardless of the stroke flag of the paint.
        assertEquals(0.0f, data[FLAGS + 1], EPS);
        // Padding is half the thickness plus the anti-aliasing margin: 2 * 0.5 + 1 = 2.
        assertArrayEquals(new float[]{-2, -2, 22, 2}, slice(data, LOCAL, 4), EPS);
    }

    @Test
    void pack_degenerateRect_writesNothing() {
        FloatBuffer buffer = FloatBuffer.allocate(FLOATS);
        assertFalse(GtnhSdfShapeInstances.pack(buffer, rect(0, 0, 0, 10, false), 1.0f, scratch));
        assertEquals(0, buffer.position());
    }

    @Test
    void pack_opacityMultipliesAlphaOnly() {
        FloatBuffer buffer = FloatBuffer.allocate(FLOATS);
        GtnhSdfShapeInstances.pack(buffer, rounded(0, 0, 10, 10, 2), 0.5f, scratch);
        assertArrayEquals(new float[]{0.25f, 0.5f, 0.75f, 0.5f}, slice(buffer.array(), COLOR, 4), EPS);
    }

    @Test
    void pack_appliesCommandTransformToCorners() {
        Transform transform = new Transform();
        transform.position().set(100.0f, 0.0f);
        DrawCommand command = rounded(0, 0, 10, 10, 2).transform(transform);
        FloatBuffer buffer = FloatBuffer.allocate(FLOATS);

        GtnhSdfShapeInstances.pack(buffer, command, 1.0f, scratch);

        // First corner (-1, -1) shifted by +100 in x; local coordinates stay untransformed.
        assertEquals(99.0f, buffer.array()[0], EPS);
        assertEquals(-1.0f, buffer.array()[1], EPS);
        assertEquals(-1.0f, buffer.array()[LOCAL], EPS);
    }

    @Test
    void transformPoint_appliesCommandThenStackFromLastToFirst() {
        Transform outer = new Transform();
        outer.scale().set(2.0f, 2.0f);
        Transform inner = new Transform();
        inner.position().set(10.0f, 0.0f);
        MutableRect layerBounds = new MutableRect(0, 0, 100, 100);
        Object[] layers = {new TransformLayer(layerBounds, outer), new TransformLayer(layerBounds, inner)};

        float[] xy = {1.0f, 1.0f};
        GtnhRenderMath.transformPoint(xy, new MutableRect(0, 0, 10, 10), null, layers, 2);

        // Inner shift first (11, 1), then outer scale around the origin (22, 2).
        assertArrayEquals(new float[]{22.0f, 2.0f}, xy, EPS);
    }

    @Test
    void transformPoint_noTransforms_isIdentity() {
        float[] xy = {3.0f, 4.0f};
        GtnhRenderMath.transformPoint(xy, new MutableRect(0, 0, 1, 1), null, new Object[0], 0);
        assertArrayEquals(new float[]{3.0f, 4.0f}, xy, EPS);
    }

    @Test
    void layoutConstants_matchSharedShaderContract() {
        assertEquals(30, FLOATS);
        assertEquals(List.of(3, 3, 3, 3, 4, 4, 2, 2, 2, 2, 2).size(),
                SdfShapeShaderSources.INSTANCED_ATTRIBUTE_COMPONENTS.length);
    }

    private static DrawCommand rect(float x, float y, float w, float h, boolean stroke) {
        return command(DrawCommandType.RECT, x, y, w, h, stroke);
    }

    private static DrawCommand rounded(float x, float y, float w, float h, float radius) {
        return command(DrawCommandType.ROUNDED_RECT, x, y, w, h, false).radius(radius);
    }

    private static DrawCommand command(DrawCommandType type, float x, float y, float w, float h, boolean stroke) {
        MutableColor color = new MutableColor(0.25f, 0.5f, 0.75f, 1.0f);
        return new DrawCommand(type).bounds(new MutableRect(x, y, w, h))
                .paint(stroke ? Paint.stroke(color, 1.0f) : Paint.fill(color));
    }

    private static float[] slice(float[] data, int from, int count) {
        float[] result = new float[count];
        System.arraycopy(data, from, result, 0, count);
        return result;
    }
}
