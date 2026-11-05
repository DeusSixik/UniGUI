package dev.sixik.unigui.cleanroom.demo;

import dev.sixik.unigui.api.core.FrameContext;
import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.posteffect.UiPostEffects;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.screen.UniRenderLayer;
import dev.sixik.unigui.api.screen.UniRenderLayerRegistration;
import dev.sixik.unigui.api.screen.UniScreenOverlayRender;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.impl.widget.WidgetBase;
import net.minecraft.client.Minecraft;

/**
 * Manual check of post effects ({@code /unigui posteffect}): every call switches an overlay layer to the next
 * effect (vignette, scanlines, barrel distortion, chromatic aberration) and finally off. The layer is a full-window
 * panel with a gradient, a frame and text, so what the effect does to colors, edges and fine lines is visible.
 */
public final class PostEffectLab {
    private static final String[] EFFECTS = {
            UiPostEffects.VIGNETTE, UiPostEffects.SCANLINE, UiPostEffects.BARREL_DISTORTION,
            UiPostEffects.CHROMATIC_ABERRATION};

    private static UniRenderLayerRegistration registration;
    private static int step = -1;

    private PostEffectLab() {
    }

    /** Advances to the next effect on the client thread; returns a short description for chat. */
    public static String next() {
        Minecraft.getMinecraft().addScheduledTask(PostEffectLab::advance);
        int upcoming = step + 1;
        return upcoming >= EFFECTS.length ? "post effect: off" : "post effect: " + EFFECTS[upcoming];
    }

    private static void advance() {
        step++;
        if (registration != null) {
            registration.close();
            registration = null;
        }
        if (step >= EFFECTS.length) {
            step = -1;
            return;
        }
        UniRenderLayer layer = new UniRenderLayer(new Panel()).postEffect(UiPostEffects.chain(EFFECTS[step]));
        registration = UniScreenOverlayRender.register(layer);
    }

    /** A window-sized panel: vertical gradient, thin frame, a diagonal and a label. */
    private static final class Panel extends WidgetBase {
        private float phase;

        @Override
        public void measure(LayoutContext context) {
            setDesiredSize(resolveDesiredSize(context, context.availableWidth(), context.availableHeight()));
        }

        @Override
        public void tick(FrameContext frame) {
            super.tick(frame);
            phase += frame == null ? 1.0f / 60.0f : Math.max(0.0f, frame.deltaSeconds());
            invalidate(InvalidationFlags.VISUAL);
        }

        @Override
        public void render(RenderContext context) {
            if (visibility() != Visibility.VISIBLE || context == null) return;
            float x = layoutBounds().x();
            float y = layoutBounds().y();
            float w = layoutBounds().width();
            float h = layoutBounds().height();
            if (w <= 0.0f || h <= 0.0f) return;

            int bands = 24;
            for (int i = 0; i < bands; i++) {
                float t = i / (float) (bands - 1);
                context.rect(x, y + h * i / bands, w, h / bands + 1.0f,
                        Paint.fill(MutableColor.rgba(0.1f + 0.5f * t, 0.25f, 0.8f - 0.5f * t, 0.55f)));
            }
            context.rect(x + 8.0f, y + 8.0f, w - 16.0f, h - 16.0f,
                    Paint.stroke(MutableColor.rgba(1.0f, 1.0f, 1.0f, 1.0f), 2.0f));
            context.line(x, y, x + w, y + h, Paint.stroke(MutableColor.rgba(1.0f, 0.9f, 0.2f, 1.0f), 1.0f));
            float cx = x + w * 0.5f;
            float cy = y + h * 0.5f;
            context.line(cx, cy, cx + (float) Math.cos(phase) * 80.0f, cy + (float) Math.sin(phase) * 80.0f,
                    Paint.stroke(MutableColor.rgba(0.3f, 1.0f, 0.5f, 1.0f), 3.0f));
            context.text("POST EFFECT LAB", cx - 80.0f, cy + 90.0f, 160.0f, 14.0f,
                    Paint.fill(MutableColor.rgba(1.0f, 1.0f, 1.0f, 1.0f)));
        }
    }
}
