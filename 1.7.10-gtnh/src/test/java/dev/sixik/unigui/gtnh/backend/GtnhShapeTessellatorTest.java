package dev.sixik.unigui.gtnh.backend;

import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.math.Transform;
import dev.sixik.unigui.api.render.DrawCommand;
import dev.sixik.unigui.api.render.DrawCommandType;
import dev.sixik.unigui.api.render.DrawMesh;
import dev.sixik.unigui.api.render.DrawVertex;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.render.VectorPath;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GtnhShapeTessellatorTest {
    private static final float EPS = 1.0e-4f;
    private static final MutableColor WHITE = new MutableColor(1, 1, 1, 1);

    private final GtnhShapeTessellator tessellator = new GtnhShapeTessellator();
    private final GtnhVertexList out = new GtnhVertexList();

    private int append(DrawCommand command, float opacity, float pixelsPerUnit) {
        out.clear();
        tessellator.begin(out, pixelsPerUnit);
        return tessellator.append(command, opacity);
    }

    private int append(DrawCommand command) {
        return append(command, 1.0f, 1.0f);
    }

    @Test
    void supports_shapeAndMeshTypesOnly() {
        assertTrue(GtnhShapeTessellator.supports(DrawCommandType.PATH));
        assertTrue(GtnhShapeTessellator.supports(DrawCommandType.MESH));
        assertFalse(GtnhShapeTessellator.supports(DrawCommandType.TEXT));
        assertFalse(GtnhShapeTessellator.supports(DrawCommandType.TEXTURE));
        assertFalse(GtnhShapeTessellator.supports(DrawCommandType.PUSH_CLIP));
    }

    @Test
    void rectFill_isTwoTrianglesCoveringBounds() {
        assertEquals(6, append(DrawCommand.rect(new MutableRect(2, 3, 10, 20), Paint.fill(WHITE))));
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        for (int i = 0; i < 6; i++) {
            minX = Math.min(minX, out.x(i));
            maxX = Math.max(maxX, out.x(i));
            minY = Math.min(minY, out.y(i));
            maxY = Math.max(maxY, out.y(i));
        }
        assertEquals(2, minX, EPS);
        assertEquals(12, maxX, EPS);
        assertEquals(3, minY, EPS);
        assertEquals(23, maxY, EPS);
    }

    @Test
    void rectStroke_isFourEdgeQuads() {
        assertEquals(24, append(DrawCommand.rect(new MutableRect(0, 0, 20, 20), Paint.stroke(WHITE, 2.0f))));
    }

    @Test
    void rectStroke_thickerThanHalfTheRect_isClamped() {
        // 4 x 4 rect with a 10 px stroke: thickness clamps to 2, the two side quads are skipped.
        assertEquals(12, append(DrawCommand.rect(new MutableRect(0, 0, 4, 4), Paint.stroke(WHITE, 10.0f))));
    }

    @Test
    void degenerateOrInvisibleRect_emitsNothing() {
        assertEquals(0, append(DrawCommand.rect(new MutableRect(0, 0, 0, 10), Paint.fill(WHITE))));
        assertEquals(0, append(DrawCommand.rect(new MutableRect(0, 0, 10, 10),
                Paint.fill(new MutableColor(1, 1, 1, 0)))));
    }

    @Test
    void opacity_scalesVertexAlpha() {
        append(DrawCommand.rect(new MutableRect(0, 0, 4, 4), Paint.fill(WHITE)), 0.5f, 1.0f);
        assertEquals(128, out.argb(0) >>> 24);
    }

    @Test
    void commandTransform_isAppliedOnTheCpu() {
        Transform transform = new Transform();
        transform.position().set(100.0f, 50.0f);
        DrawCommand command = DrawCommand.rect(new MutableRect(0, 0, 10, 10), Paint.fill(WHITE)).transform(transform);
        append(command);
        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        for (int i = 0; i < out.size(); i++) {
            minX = Math.min(minX, out.x(i));
            minY = Math.min(minY, out.y(i));
        }
        assertEquals(100.0f, minX, EPS);
        assertEquals(50.0f, minY, EPS);
    }

    @Test
    void thinHorizontalLine_isCenteredOnAPixelRow() {
        DrawCommand line = DrawCommand.line(new MutableRect(0, 3.2f, 10, 0), Paint.stroke(WHITE, 1.0f));
        assertEquals(6, append(line));
        for (int i = 0; i < out.size(); i++) {
            assertTrue(out.y(i) == 3.0f || out.y(i) == 4.0f, "y=" + out.y(i));
        }
    }

    @Test
    void thickLine_expandsPerpendicularlyByHalfTheThickness() {
        DrawCommand line = DrawCommand.line(new MutableRect(0, 10, 20, 0), Paint.stroke(WHITE, 4.0f));
        append(line);
        for (int i = 0; i < out.size(); i++) {
            assertTrue(Math.abs(out.y(i) - 10.0f) - 2.0f < EPS, "y=" + out.y(i));
        }
        float spread = 0.0f;
        for (int i = 0; i < out.size(); i++) spread = Math.max(spread, Math.abs(out.y(i) - 10.0f));
        assertEquals(2.0f, spread, EPS);
    }

    @Test
    void zeroLengthLine_isASquareDot() {
        DrawCommand dot = DrawCommand.line(new MutableRect(5, 5, 0, 0), Paint.stroke(WHITE, 3.0f));
        assertEquals(6, append(dot));
    }

    @Test
    void roundedRect_smallRadiusFallsBackToPlainRect() {
        DrawCommand command = new DrawCommand(DrawCommandType.ROUNDED_RECT)
                .bounds(new MutableRect(0, 0, 10, 10)).paint(Paint.fill(WHITE)).radius(0.25f);
        assertEquals(6, append(command));
    }

    @Test
    void roundedRect_fillIsAFanOverTheSampledOutline() {
        float radius = 5.0f;
        DrawCommand command = new DrawCommand(DrawCommandType.ROUNDED_RECT)
                .bounds(new MutableRect(0, 0, 40, 30)).paint(Paint.fill(WHITE)).radius(radius);
        int segments = GtnhShapeTessellator.cornerSegments(radius);
        // 4 corners * (segments + 1) outline points, one triangle per point.
        assertEquals(3 * 4 * (segments + 1), append(command));
    }

    @Test
    void roundedRect_strokeIsARingBetweenOuterAndInnerOutline() {
        DrawCommand command = new DrawCommand(DrawCommandType.ROUNDED_RECT)
                .bounds(new MutableRect(0, 0, 40, 30)).paint(Paint.stroke(WHITE, 2.0f)).radius(6.0f);
        int segments = GtnhShapeTessellator.cornerSegments(6.0f);
        assertEquals(6 * 4 * (segments + 1), append(command));
    }

    @Test
    void roundedRect_vertexNeverLeavesBounds() {
        DrawCommand command = new DrawCommand(DrawCommandType.ROUNDED_RECT)
                .bounds(new MutableRect(10, 10, 40, 30)).paint(Paint.fill(WHITE)).radius(8.0f);
        append(command);
        for (int i = 0; i < out.size(); i++) {
            assertTrue(out.x(i) >= 10 - EPS && out.x(i) <= 50 + EPS, "x=" + out.x(i));
            assertTrue(out.y(i) >= 10 - EPS && out.y(i) <= 40 + EPS, "y=" + out.y(i));
        }
    }

    @Test
    void circle_fillAndStrokeUseTheRequestedSegmentCount() {
        DrawCommand fill = DrawCommand.circle(new MutableRect(0, 0, 20, 20), Paint.fill(WHITE)).segments(16);
        assertEquals(3 * 16, append(fill));
        DrawCommand stroke = DrawCommand.circle(new MutableRect(0, 0, 20, 20), Paint.stroke(WHITE, 2.0f)).segments(16);
        assertEquals(6 * 16, append(stroke));
    }

    @Test
    void circle_withoutSegments_derivesThemFromRadius() {
        assertEquals(12, GtnhShapeTessellator.curveSegments(1.0f));
        assertEquals(64, GtnhShapeTessellator.curveSegments(1000.0f));
        assertEquals(3, GtnhShapeTessellator.cornerSegments(1.0f));
        assertEquals(12, GtnhShapeTessellator.cornerSegments(1000.0f));
    }

    @Test
    void convexPathFill_hasAFadingFringeOneUnitWide() {
        VectorPath path = new VectorPath().moveTo(0, 0).lineTo(10, 0).lineTo(10, 10).lineTo(0, 10).close();
        DrawCommand command = DrawCommand.path(path, new MutableRect(0, 0, 10, 10), Paint.fill(WHITE));
        int vertices = append(command, 1.0f, 1.0f);

        assertTrue(vertices > 6);
        boolean opaque = false;
        boolean transparent = false;
        float minX = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        for (int i = 0; i < out.size(); i++) {
            int alpha = out.argb(i) >>> 24;
            opaque |= alpha == 255;
            transparent |= alpha == 0;
            minX = Math.min(minX, out.x(i));
            maxX = Math.max(maxX, out.x(i));
        }
        assertTrue(opaque, "interior must be fully covered");
        assertTrue(transparent, "outer fringe edge must have zero coverage");
        // Fringe is one physical pixel; at 1 pixel per unit it extends at most ~1 unit past the outline.
        assertTrue(minX >= -1.0f - EPS && maxX <= 11.0f + EPS, "x range " + minX + ".." + maxX);
    }

    @Test
    void fringeShrinksWithMorePixelsPerUnit() {
        VectorPath path = new VectorPath().moveTo(0, 0).lineTo(10, 0).lineTo(10, 10).lineTo(0, 10).close();
        DrawCommand command = DrawCommand.path(path, new MutableRect(0, 0, 10, 10), Paint.fill(WHITE));
        append(command, 1.0f, 4.0f);
        float minX = Float.MAX_VALUE;
        for (int i = 0; i < out.size(); i++) minX = Math.min(minX, out.x(i));
        assertTrue(minX >= -0.25f - EPS, "4 px per unit gives a quarter-unit fringe, got " + minX);
    }

    @Test
    void pathStroke_emitsGeometryAlongTheOpenPolyline() {
        VectorPath path = new VectorPath().moveTo(0, 0).lineTo(20, 0).lineTo(20, 20);
        DrawCommand command = DrawCommand.path(path, new MutableRect(0, 0, 20, 20), Paint.stroke(WHITE, 3.0f));
        assertTrue(append(command) > 0);
    }

    @Test
    void pathWithFewerThanThreePoints_isNotFilled() {
        VectorPath path = new VectorPath().moveTo(0, 0).lineTo(5, 5);
        DrawCommand command = DrawCommand.path(path, new MutableRect(0, 0, 5, 5), Paint.fill(WHITE));
        assertEquals(0, append(command));
    }

    @Test
    void pathBoundsOffsetsEveryPoint() {
        VectorPath path = new VectorPath().moveTo(0, 0).lineTo(10, 0).lineTo(10, 10).lineTo(0, 10).close();
        DrawCommand command = DrawCommand.path(path, new MutableRect(100, 200, 10, 10), Paint.fill(WHITE));
        append(command);
        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        for (int i = 0; i < out.size(); i++) {
            minX = Math.min(minX, out.x(i));
            minY = Math.min(minY, out.y(i));
        }
        assertTrue(minX >= 99.0f && minX < 101.0f);
        assertTrue(minY >= 199.0f && minY < 201.0f);
    }

    @Test
    void curveSegments_growWithPixelDensityAndStayClamped() {
        int coarse = GtnhShapeTessellator.quadraticSegments(0, 0, 50, 100, 100, 0, 1.0f);
        int fine = GtnhShapeTessellator.quadraticSegments(0, 0, 50, 100, 100, 0, 8.0f);
        assertTrue(fine > coarse);
        assertEquals(2, GtnhShapeTessellator.quadraticSegments(0, 0, 5, 0, 10, 0, 1.0f));
        assertEquals(96, GtnhShapeTessellator.cubicSegments(0, 0, 0, 1000, 1000, -1000, 1000, 0, 1000.0f));
    }

    @Test
    void mesh_passesVerticesThroughWithTextureCoordinatesAndOpacity() {
        DrawMesh mesh = DrawMesh.triangles(List.of(
                new DrawVertex(0, 0, 0.0f, 0.0f, new MutableColor(1, 0, 0, 1)),
                new DrawVertex(10, 0, 1.0f, 0.0f, new MutableColor(0, 1, 0, 1)),
                new DrawVertex(0, 10, 0.0f, 1.0f, new MutableColor(0, 0, 1, 1))));
        DrawCommand command = new DrawCommand(DrawCommandType.MESH).mesh(mesh).paint(Paint.fill(WHITE));

        assertEquals(3, append(command, 0.5f, 1.0f));
        assertEquals(10.0f, out.x(1), EPS);
        assertEquals(1.0f, out.u(1), EPS);
        assertEquals(1.0f, out.v(2), EPS);
        assertEquals(0x80FF0000, out.argb(0));
    }

    @Test
    void emptyMesh_emitsNothing() {
        DrawCommand command = new DrawCommand(DrawCommandType.MESH).paint(Paint.fill(WHITE));
        assertEquals(0, append(command));
    }
}
