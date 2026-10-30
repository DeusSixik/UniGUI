package dev.sixik.unigui.cleanroom.backend;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.render.DrawList;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.render.RenderBackend;
import dev.sixik.unigui.api.render.RenderTargetOptions;
import dev.sixik.unigui.api.render.TextureHandle;
import dev.sixik.unigui.api.render.UiRenderPolicy;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.impl.render.DefaultRenderContext;
import dev.sixik.unigui.impl.render.WidgetTextureRenderer;
import dev.sixik.unigui.impl.widget.WidgetBase;
import net.minecraft.client.Minecraft;

/**
 * Keeps the last frame of a retained UI in a window-sized texture, so a screen with a non-continuous
 * {@link UiRenderPolicy} redraws its widgets only when something changed and otherwise draws one textured quad.
 *
 * <p>One cache per screen or layer. Client thread only.</p>
 */
final class CleanroomRenderCache implements AutoCloseable {
    private static final MutableColor TINT = new MutableColor(1.0f, 1.0f, 1.0f, 1.0f);

    private final RenderBackend backend;
    private final DrawList drawList = new DrawList();
    private final DefaultRenderContext context = new DefaultRenderContext(drawList);
    private WidgetTextureRenderer renderer;
    private TextureHandle texture;
    private int width;
    private int height;
    private long lastRenderNanos;
    private long lastFrameNanos;
    private long frameNanos;

    CleanroomRenderCache(RenderBackend backend) {
        this.backend = backend;
        this.context.backend(backend);
    }

    /**
     * Adds this frame's picture of {@code root} to {@code frame}: a cache texture, redrawn first if the policy says so.
     *
     * @param policy        render policy, not {@code CONTINUOUS}
     * @param root          widget tree
     * @param frame         draw list of the frame; receives one texture command
     * @param frameContext  render context writing into {@code frame}
     * @param layoutChanged {@code true} if layout ran this frame
     * @param logicalWidth  width of the UI in layout units
     * @param logicalHeight height of the UI in layout units
     * @param nowNanos      current time
     */
    void render(UiRenderPolicy policy, Widget root, DefaultRenderContext frameContext, boolean layoutChanged,
                float logicalWidth, float logicalHeight, long nowNanos) {
        Minecraft minecraft = Minecraft.getMinecraft();
        int targetWidth = Math.max(1, minecraft.displayWidth);
        int targetHeight = Math.max(1, minecraft.displayHeight);
        trackFrameTime(nowNanos);
        CleanroomRenderPolicies.Decision decision = CleanroomRenderPolicies.decide(
                policy.mode(), CleanroomRenderPolicies.intervalNanos(policy, CleanroomDisplay.refreshRate()),
                texture != null,
                width != targetWidth || height != targetHeight, layoutChanged,
                root.subtreeInvalidationFlags() != InvalidationFlags.NONE, () -> animationsRunning(root),
                nowNanos, lastRenderNanos, frameNanos);
        if (decision.action() == CleanroomRenderPolicies.Action.REFRESH) refresh(root, targetWidth, targetHeight);
        if (texture != null) {
            frameContext.texture(texture, 0.0f, 0.0f, logicalWidth, logicalHeight, Paint.fill(TINT));
        }
    }

    /**
     * Smoothed time between frames, so one slow frame does not change the cadence.
     */
    private void trackFrameTime(long nowNanos) {
        if (lastFrameNanos != 0L) {
            long delta = Math.max(0L, nowNanos - lastFrameNanos);
            frameNanos = frameNanos == 0L ? delta : frameNanos + (delta - frameNanos) / 8L;
        }
        lastFrameNanos = nowNanos;
    }

    /**
     * Throws the cached picture away; the next frame redraws it.
     */
    void invalidate() {
        texture = null;
        lastRenderNanos = 0L;
    }

    private void refresh(Widget root, int targetWidth, int targetHeight) {
        if (renderer == null || width != targetWidth || height != targetHeight) {
            if (renderer != null) renderer.close();
            renderer = new WidgetTextureRenderer(backend);
            width = targetWidth;
            height = targetHeight;
            texture = null;
        }
        context.backend(backend);
        drawList.clear();
        root.renderCached(context);
        // A window-sized target keeps the screen's pixels-per-unit, so the list is drawn in layout units.
        texture = renderer.renderDrawListToTexture(drawList, width, height, RenderTargetOptions.COLOR_DEPTH);
        lastRenderNanos = System.nanoTime();
        clearInvalidation(root);
    }

    private static boolean animationsRunning(Widget widget) {
        if (widget instanceof WidgetBase base && base.animationsRunning()) return true;
        for (Widget child : widget.children()) {
            if (animationsRunning(child)) return true;
        }
        return false;
    }

    private static void clearInvalidation(Widget widget) {
        if (widget instanceof WidgetBase base) {
            // The whole subtree is current after a full redraw; skips the bubbling recompute (O(N) instead of O(N^2)).
            base.clearInvalidationDirect(InvalidationFlags.ALL);
        } else {
            widget.clearInvalidation(InvalidationFlags.ALL);
        }
        for (Widget child : widget.children()) clearInvalidation(child);
    }

    @Override
    public void close() {
        if (renderer != null) {
            renderer.close();
            renderer = null;
        }
        texture = null;
        width = 0;
        height = 0;
        lastRenderNanos = 0L;
    }
}