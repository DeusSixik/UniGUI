package dev.sixik.unigui.cleanroom.backend;

import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.math.Transform;
import dev.sixik.unigui.api.render.BlendMode;
import dev.sixik.unigui.api.render.TransformLayer;
import dev.sixik.unigui.impl.render.ScissorStack;

/**
 * Backend-independent math shared by the legacy 1.12.2 renderer.
 *
 * <p>Everything here is a pure function with no OpenGL or Minecraft access, so it can be unit tested
 * without a game instance. Methods that need to return several values write into caller-supplied
 * scratch objects to keep the render hot path allocation-free.</p>
 */
final class CleanroomRenderMath {
    /**
     * OpenGL {@code GL_ONE}; the constants are duplicated to keep this class free of LWJGL class loading.
     */
    static final int GL_ONE = 1;
    /**
     * OpenGL {@code GL_SRC_ALPHA}.
     */
    static final int GL_SRC_ALPHA = 0x0302;
    /**
     * OpenGL {@code GL_ONE_MINUS_SRC_ALPHA}.
     */
    static final int GL_ONE_MINUS_SRC_ALPHA = 0x0303;

    private CleanroomRenderMath() {
    }

    /**
     * Packs a color into {@code 0xAARRGGBB}.
     *
     * @param color   source color, {@code null} is treated as fully transparent
     * @param opacity extra alpha multiplier, clamped to {@code [0, 1]}
     * @return packed ARGB color
     */
    static int argb(ColorView color, float opacity) {
        if (color == null) return 0;
        int alpha = channel(color.a() * clamp01(opacity));
        return alpha << 24 | channel(color.r()) << 16 | channel(color.g()) << 8 | channel(color.b());
    }

    /**
     * Replaces a non-finite or non-positive UI scale with {@code 1}.
     *
     * @param scale requested scale
     * @return usable scale
     */
    static float sanitizeScale(float scale) {
        return Float.isFinite(scale) && scale > 0.0f ? scale : 1.0f;
    }

    /**
     * Number of framebuffer pixels per Minecraft GUI pixel along one axis.
     *
     * <p>{@code displayExtent / guiExtent}; with GUI Scale 2 on a 1920 px window the GUI width is 960,
     * so the result is {@code 2}.</p>
     *
     * @param displayExtent framebuffer extent in physical pixels
     * @param guiExtent     {@code GuiScreen.width} or {@code height} in scaled GUI pixels
     * @return physical pixels per GUI pixel
     */
    static float framebufferPerGuiPixel(int displayExtent, int guiExtent) {
        return displayExtent / (float) Math.max(1, guiExtent);
    }

    /**
     * Resolves the multiplier applied to the whole UniGUI draw list.
     *
     * <p>When {@code scaleWithGameGui} is set the configured scale is used as is on top of the
     * Minecraft GUI scale. Otherwise the Minecraft scale is divided out so the UI is sized in physical
     * pixels, but never below the configured scale ({@code max(1, mcScale)} guard).</p>
     *
     * @param configured       scale requested by the screen or theme
     * @param scaleWithGameGui whether Minecraft's GUI Scale is part of the result
     * @param displayWidth     framebuffer width in physical pixels
     * @param guiWidth         scaled GUI width
     * @return scale applied through {@code glScalef} before drawing
     */
    static float effectiveUiScale(float configured, boolean scaleWithGameGui, int displayWidth, int guiWidth) {
        float scale = sanitizeScale(configured);
        if (scaleWithGameGui) return scale;
        return scale / Math.max(1.0f, framebufferPerGuiPixel(displayWidth, guiWidth));
    }

    /**
     * Converts a UI-space extent into physical window pixels along one axis.
     *
     * @param uiScale       value from {@link #effectiveUiScale}
     * @param displayExtent framebuffer extent in physical pixels
     * @param guiExtent     scaled GUI extent
     * @return physical pixels per UI unit
     */
    static float windowScale(float uiScale, int displayExtent, int guiExtent) {
        return sanitizeScale(uiScale) * framebufferPerGuiPixel(displayExtent, guiExtent);
    }

    /**
     * Applies one {@link Transform} to a point, mirroring the GL call order the backend uses:
     * {@code translate(position) translate(pivot) rotate scale translate(-pivot)}.
     *
     * @param xy        in/out point: {@code xy[0]} is x, {@code xy[1]} is y
     * @param bounds    bounds the pivot is relative to
     * @param transform transform to apply; {@code null} and identity are no-ops
     */
    static void applyTransform(float[] xy, RectView bounds, Transform transform) {
        if (transform == null || transform.isIdentity()) return;
        float pivotX = bounds.x() + transform.pivot().x();
        float pivotY = bounds.y() + transform.pivot().y();
        float x = (xy[0] - pivotX) * transform.scale().x();
        float y = (xy[1] - pivotY) * transform.scale().y();
        double radians = Math.toRadians(transform.rotationDegrees());
        float cos = (float) Math.cos(radians);
        float sin = (float) Math.sin(radians);
        xy[0] = x * cos - y * sin + pivotX + transform.position().x();
        xy[1] = x * sin + y * cos + pivotY + transform.position().y();
    }

    /**
     * Applies the full transform chain of a command to a point: the command transform first (innermost), then every
     * stack layer from the last to the first. This is the CPU equivalent of the nested {@code glPushMatrix} state
     * that the immediate-mode path builds, used where vertices are uploaded already transformed.
     *
     * @param xy               in/out point: {@code xy[0]} is x, {@code xy[1]} is y
     * @param bounds           command bounds the command transform pivots around
     * @param commandTransform transform stored on the command, may be {@code null}
     * @param layers           backing array of {@code TransformLayer}s
     * @param layerCount       number of valid entries in {@code layers}
     */
    static void transformPoint(float[] xy, RectView bounds, Transform commandTransform, Object[] layers,
                               int layerCount) {
        applyTransform(xy, bounds, commandTransform);
        for (int i = layerCount - 1; i >= 0; i--) {
            TransformLayer layer = (TransformLayer) layers[i];
            applyTransform(xy, layer.bounds(), layer.transform());
        }
    }

    /**
     * Collapses the whole transform chain of a command into one 2D affine matrix, so a renderer that emits many
     * vertices per command (text glyphs) transforms each of them with six multiplications instead of walking the
     * layer stack. The chain is affine, so mapping the origin and the two unit vectors determines it exactly.
     *
     * <p>Result layout: {@code x' = a*x + c*y + tx}, {@code y' = b*x + d*y + ty}.</p>
     *
     * @param bounds           command bounds the command transform pivots around
     * @param commandTransform transform stored on the command, may be {@code null}
     * @param layers           backing array of {@code TransformLayer}s
     * @param layerCount       number of valid entries in {@code layers}
     * @param scratch          two-element scratch array
     * @param out              six-element array receiving {@code a, b, c, d, tx, ty}
     */
    static void affine(RectView bounds, Transform commandTransform, Object[] layers, int layerCount,
                       float[] scratch, float[] out) {
        scratch[0] = 0.0f;
        scratch[1] = 0.0f;
        transformPoint(scratch, bounds, commandTransform, layers, layerCount);
        float tx = scratch[0];
        float ty = scratch[1];
        scratch[0] = 1.0f;
        scratch[1] = 0.0f;
        transformPoint(scratch, bounds, commandTransform, layers, layerCount);
        out[0] = scratch[0] - tx;
        out[1] = scratch[1] - ty;
        scratch[0] = 0.0f;
        scratch[1] = 1.0f;
        transformPoint(scratch, bounds, commandTransform, layers, layerCount);
        out[2] = scratch[0] - tx;
        out[3] = scratch[1] - ty;
        out[4] = tx;
        out[5] = ty;
    }

    /**
     * Computes the axis-aligned bounding box of {@code bounds} after the command transform and every
     * transform layer of its stack. The command transform is innermost; stack layers are applied from
     * the last to the first, which matches the order of nested {@code glPushMatrix} state.
     *
     * @param bounds           untransformed clip bounds
     * @param commandTransform transform stored on the command, may be {@code null}
     * @param layers           backing array of {@code TransformLayer}s (as returned by {@code transformStackElements()})
     * @param layerCount       number of valid entries in {@code layers}
     * @param scratch          two-element scratch array
     * @param out              receives the resulting rectangle
     */
    static void transformedBounds(RectView bounds, Transform commandTransform, Object[] layers, int layerCount,
                                  float[] scratch, MutableRect out) {
        float left = Float.POSITIVE_INFINITY;
        float top = Float.POSITIVE_INFINITY;
        float right = Float.NEGATIVE_INFINITY;
        float bottom = Float.NEGATIVE_INFINITY;
        for (int corner = 0; corner < 4; corner++) {
            scratch[0] = bounds.x() + ((corner & 1) == 0 ? 0.0f : bounds.width());
            scratch[1] = bounds.y() + ((corner & 2) == 0 ? 0.0f : bounds.height());
            transformPoint(scratch, bounds, commandTransform, layers, layerCount);
            left = Math.min(left, scratch[0]);
            right = Math.max(right, scratch[0]);
            top = Math.min(top, scratch[1]);
            bottom = Math.max(bottom, scratch[1]);
        }
        out.set(left, top, Math.max(0.0f, right - left), Math.max(0.0f, bottom - top));
    }

    /**
     * Scales a UI-space rectangle into physical window pixels (top-left origin).
     *
     * @param bounds UI-space rectangle
     * @param scaleX physical pixels per UI unit along x
     * @param scaleY physical pixels per UI unit along y
     * @param out    receives the scaled rectangle
     */
    static void scaleRect(RectView bounds, float scaleX, float scaleY, MutableRect out) {
        out.set(bounds.x() * scaleX, bounds.y() * scaleY, bounds.width() * scaleX, bounds.height() * scaleY);
    }

    /**
     * Converts a top-left origin window rectangle into {@code glScissor} arguments (bottom-left origin),
     * clamped to the framebuffer.
     *
     * @param rect          rectangle in physical pixels, top-left origin
     * @param displayWidth  framebuffer width
     * @param displayHeight framebuffer height
     * @param out           four-element array receiving {@code x, y, width, height}
     */
    static void glScissor(ScissorStack.Rect rect, int displayWidth, int displayHeight, int[] out) {
        int x1 = clamp(rect.x1(), 0, displayWidth);
        int x2 = clamp(rect.x2(), x1, displayWidth);
        int y1 = clamp(rect.y1(), 0, displayHeight);
        int y2 = clamp(rect.y2(), y1, displayHeight);
        out[0] = x1;
        out[1] = displayHeight - y2;
        out[2] = x2 - x1;
        out[3] = y2 - y1;
    }

    /**
     * Source factor for the color channels of a blend mode.
     *
     * @param mode requested blend mode, {@code null} means {@link BlendMode#NORMAL}
     * @return GL blend factor
     */
    static int blendSrcRgb(BlendMode mode) {
        return GL_SRC_ALPHA;
    }

    /**
     * Source factor for the color channels when the source pixels may already be multiplied by their alpha.
     *
     * @param mode                requested blend mode, {@code null} means {@link BlendMode#NORMAL}
     * @param premultipliedSource {@code true} if the texture stores premultiplied alpha
     * @return {@code GL_ONE} for a premultiplied source in normal blending (multiplying again would darken the
     * edges), otherwise the same as {@link #blendSrcRgb(BlendMode)}
     */
    static int blendSrcRgb(BlendMode mode, boolean premultipliedSource) {
        return premultipliedSource && mode != BlendMode.ADDITIVE ? GL_ONE : GL_SRC_ALPHA;
    }

    /**
     * Destination factor for the color channels of a blend mode.
     *
     * @param mode requested blend mode, {@code null} means {@link BlendMode#NORMAL}
     * @return GL blend factor
     */
    static int blendDstRgb(BlendMode mode) {
        return mode == BlendMode.ADDITIVE ? GL_ONE : GL_ONE_MINUS_SRC_ALPHA;
    }

    /**
     * Source factor for the alpha channel of a blend mode.
     *
     * @param mode requested blend mode, {@code null} means {@link BlendMode#NORMAL}
     * @return GL blend factor
     */
    static int blendSrcAlpha(BlendMode mode) {
        return GL_ONE;
    }

    /**
     * Destination factor for the alpha channel of a blend mode.
     *
     * @param mode requested blend mode, {@code null} means {@link BlendMode#NORMAL}
     * @return GL blend factor
     */
    static int blendDstAlpha(BlendMode mode) {
        return mode == BlendMode.ADDITIVE ? GL_ONE : GL_ONE_MINUS_SRC_ALPHA;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static int channel(float value) {
        return Math.round(clamp01(value) * 255.0f);
    }

    private static float clamp01(float value) {
        if (!Float.isFinite(value)) return 0.0f;
        return Math.max(0.0f, Math.min(1.0f, value));
    }
}
