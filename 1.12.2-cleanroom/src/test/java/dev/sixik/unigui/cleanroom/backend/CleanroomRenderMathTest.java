package dev.sixik.unigui.cleanroom.backend;

import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.math.Transform;
import dev.sixik.unigui.api.render.BlendMode;
import dev.sixik.unigui.api.render.TransformLayer;
import dev.sixik.unigui.impl.render.ScissorStack;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CleanroomRenderMathTest {
    private static final float EPS = 1.0e-4f;

    @Test
    void argb_fullOpacity_encodesChannels() {
        assertEquals(0x80402010, CleanroomRenderMath.argb(new MutableColor(0.25f, 0.125f, 0.0625f, 0.5f), 1.0f));
    }

    @Test
    void argb_multipliesOpacityAndClampsChannels() {
        assertEquals(0x6600FF00, CleanroomRenderMath.argb(new MutableColor(-1.0f, 2.0f, 0.0f, 0.8f), 0.5f));
    }

    @Test
    void argb_nullColor_isTransparent() {
        assertEquals(0, CleanroomRenderMath.argb(null, 1.0f));
    }

    @Test
    void sanitizeScale_replacesInvalidValues() {
        assertEquals(1.0f, CleanroomRenderMath.sanitizeScale(Float.NaN));
        assertEquals(1.0f, CleanroomRenderMath.sanitizeScale(0.0f));
        assertEquals(1.0f, CleanroomRenderMath.sanitizeScale(-2.0f));
        assertEquals(1.5f, CleanroomRenderMath.sanitizeScale(1.5f));
    }

    @Test
    void effectiveUiScale_withGameGui_keepsConfiguredScale() {
        assertEquals(1.5f, CleanroomRenderMath.effectiveUiScale(1.5f, true, 1920, 480), EPS);
    }

    @Test
    void effectiveUiScale_withoutGameGui_dividesOutMinecraftScale() {
        // GUI Scale 2 on a 1920 px window: GuiScreen.width is 960.
        assertEquals(1.0f, CleanroomRenderMath.effectiveUiScale(2.0f, false, 1920, 960), EPS);
        assertEquals(0.5f, CleanroomRenderMath.effectiveUiScale(1.0f, false, 1920, 960), EPS);
    }

    @Test
    void effectiveUiScale_withoutGameGui_neverDividesBelowOne() {
        // A GUI wider than the framebuffer must not enlarge the UI.
        assertEquals(2.0f, CleanroomRenderMath.effectiveUiScale(2.0f, false, 640, 960), EPS);
    }

    @Test
    void windowScale_combinesUiScaleAndFramebufferRatio() {
        assertEquals(1.0f, CleanroomRenderMath.windowScale(0.5f, 1920, 960), EPS);
        assertEquals(4.0f, CleanroomRenderMath.windowScale(2.0f, 1920, 960), EPS);
        assertEquals(1.0f, CleanroomRenderMath.windowScale(1.0f, 100, 0) / 100.0f, EPS);
    }

    @Test
    void applyTransform_nullAndIdentity_doNotMovePoint() {
        float[] xy = {3.0f, 4.0f};
        CleanroomRenderMath.applyTransform(xy, new MutableRect(0, 0, 10, 10), null);
        CleanroomRenderMath.applyTransform(xy, new MutableRect(0, 0, 10, 10), new Transform());
        assertArrayEquals(new float[]{3.0f, 4.0f}, xy, EPS);
    }

    @Test
    void applyTransform_scaleAroundPivot() {
        Transform transform = new Transform();
        transform.scale().set(2.0f, 3.0f);
        transform.pivot().set(5.0f, 5.0f);
        float[] xy = {10.0f, 10.0f};
        CleanroomRenderMath.applyTransform(xy, new MutableRect(0, 0, 10, 10), transform);
        assertArrayEquals(new float[]{15.0f, 20.0f}, xy, EPS);
    }

    @Test
    void applyTransform_pivotIsRelativeToBounds() {
        Transform transform = new Transform();
        transform.scale().set(2.0f, 2.0f);
        float[] xy = {110.0f, 100.0f};
        // bounds start at (100, 100) and pivot is (0, 0), so the point scales around (100, 100).
        CleanroomRenderMath.applyTransform(xy, new MutableRect(100, 100, 10, 10), transform);
        assertArrayEquals(new float[]{120.0f, 100.0f}, xy, EPS);
    }

    @Test
    void applyTransform_rotationIsCounterClockwiseInGlSpaceAndTranslates() {
        Transform transform = new Transform();
        transform.setRotationDegrees(90.0f);
        transform.position().set(1.0f, 2.0f);
        float[] xy = {10.0f, 0.0f};
        CleanroomRenderMath.applyTransform(xy, new MutableRect(0, 0, 10, 10), transform);
        assertArrayEquals(new float[]{1.0f, 12.0f}, xy, EPS);
    }

    @Test
    void transformedBounds_withoutTransforms_returnsBounds() {
        MutableRect out = new MutableRect();
        CleanroomRenderMath.transformedBounds(new MutableRect(5, 6, 10, 20), null, new Object[0], 0,
                new float[2], out);
        assertRect(5, 6, 10, 20, out);
    }

    @Test
    void transformedBounds_rotation90_swapsExtentAroundPivot() {
        Transform transform = new Transform();
        transform.pivot().set(5.0f, 10.0f);
        transform.setRotationDegrees(90.0f);
        MutableRect out = new MutableRect();
        CleanroomRenderMath.transformedBounds(new MutableRect(0, 0, 10, 20), transform, new Object[0], 0,
                new float[2], out);
        assertRect(-5, 5, 20, 10, out);
    }

    @Test
    void transformedBounds_commandTransformIsInnermostAndStackIsAppliedLastToFirst() {
        MutableRect parentBounds = new MutableRect(0, 0, 100, 100);
        Transform parentShift = new Transform();
        parentShift.position().set(5.0f, 0.0f);
        Transform commandScale = new Transform();
        commandScale.scale().set(2.0f, 2.0f);
        Object[] layers = {new TransformLayer(parentBounds, parentShift)};

        MutableRect out = new MutableRect();
        CleanroomRenderMath.transformedBounds(new MutableRect(0, 0, 10, 10), commandScale, layers, 1,
                new float[2], out);

        // Scale 2x first (0..20), then shift by 5.
        assertRect(5, 0, 20, 20, out);
    }

    @Test
    void transformedBounds_stackedLayers_composeInOrder() {
        // Outer layer scales by 2 around the origin, inner layer shifts by 10: shift first, then scale.
        Transform outer = new Transform();
        outer.scale().set(2.0f, 2.0f);
        Transform inner = new Transform();
        inner.position().set(10.0f, 0.0f);
        MutableRect layerBounds = new MutableRect(0, 0, 100, 100);
        Object[] layers = {new TransformLayer(layerBounds, outer), new TransformLayer(layerBounds, inner)};

        MutableRect out = new MutableRect();
        CleanroomRenderMath.transformedBounds(new MutableRect(0, 0, 10, 10), null, layers, 2,
                new float[2], out);

        assertRect(20, 0, 20, 20, out);
    }

    @Test
    void scaleRect_scalesEveryComponent() {
        MutableRect out = new MutableRect();
        CleanroomRenderMath.scaleRect(new MutableRect(1, 2, 3, 4), 2.0f, 3.0f, out);
        assertRect(2, 6, 6, 12, out);
    }

    @Test
    void glScissor_flipsYAxis() {
        int[] out = new int[4];
        CleanroomRenderMath.glScissor(new ScissorStack.Rect(10, 20, 110, 70), 800, 600, out);
        assertArrayEquals(new int[]{10, 530, 100, 50}, out);
    }

    @Test
    void glScissor_clampsToFramebuffer() {
        int[] out = new int[4];
        CleanroomRenderMath.glScissor(new ScissorStack.Rect(-10, -10, 900, 700), 800, 600, out);
        assertArrayEquals(new int[]{0, 0, 800, 600}, out);
    }

    @Test
    void glScissor_rectOutsideFramebuffer_isEmpty() {
        int[] out = new int[4];
        CleanroomRenderMath.glScissor(new ScissorStack.Rect(900, 700, 950, 750), 800, 600, out);
        assertEquals(0, out[2]);
        assertEquals(0, out[3]);
    }

    @Test
    void scissorStack_popRestoresParentClip() {
        ScissorStack stack = new ScissorStack();
        stack.push(new MutableRect(0, 0, 100, 100));
        ScissorStack.Rect child = stack.push(new MutableRect(50, 50, 100, 100));
        assertEquals(new ScissorStack.Rect(50, 50, 100, 100), child);
        assertEquals(new ScissorStack.Rect(0, 0, 100, 100), stack.pop());
        assertNull(stack.pop());
    }

    @Test
    void blend_normalIsStraightAlpha() {
        assertEquals(CleanroomRenderMath.GL_SRC_ALPHA, CleanroomRenderMath.blendSrcRgb(BlendMode.NORMAL));
        assertEquals(CleanroomRenderMath.GL_ONE_MINUS_SRC_ALPHA, CleanroomRenderMath.blendDstRgb(BlendMode.NORMAL));
        assertEquals(CleanroomRenderMath.GL_ONE, CleanroomRenderMath.blendSrcAlpha(BlendMode.NORMAL));
        assertEquals(CleanroomRenderMath.GL_ONE_MINUS_SRC_ALPHA, CleanroomRenderMath.blendDstAlpha(BlendMode.NORMAL));
    }

    @Test
    void blend_additiveAddsSourceToDestination() {
        assertEquals(CleanroomRenderMath.GL_SRC_ALPHA, CleanroomRenderMath.blendSrcRgb(BlendMode.ADDITIVE));
        assertEquals(CleanroomRenderMath.GL_ONE, CleanroomRenderMath.blendDstRgb(BlendMode.ADDITIVE));
        assertEquals(CleanroomRenderMath.GL_ONE, CleanroomRenderMath.blendSrcAlpha(BlendMode.ADDITIVE));
        assertEquals(CleanroomRenderMath.GL_ONE, CleanroomRenderMath.blendDstAlpha(BlendMode.ADDITIVE));
    }

    @Test
    void blend_nullFallsBackToNormal() {
        assertEquals(CleanroomRenderMath.blendDstRgb(BlendMode.NORMAL), CleanroomRenderMath.blendDstRgb(null));
    }

    private static void assertRect(float x, float y, float width, float height, MutableRect actual) {
        assertEquals(x, actual.x(), EPS, "x");
        assertEquals(y, actual.y(), EPS, "y");
        assertEquals(width, actual.width(), EPS, "width");
        assertEquals(height, actual.height(), EPS, "height");
    }
}
