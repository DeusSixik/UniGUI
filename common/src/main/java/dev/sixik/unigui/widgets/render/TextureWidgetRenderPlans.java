package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.render.ImageFit;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.render.TextureHandle;
import dev.sixik.unigui.api.render.TexturePlacement;
import dev.sixik.unigui.api.render.plan.RenderPlan;
import dev.sixik.unigui.api.render.plan.RenderPrimitive;
import dev.sixik.unigui.api.render.plan.StyledRenderPlans;
import dev.sixik.unigui.api.style.Style;
import dev.sixik.unigui.api.style.StyleKeys;
import dev.sixik.unigui.api.style.WidgetState;

import java.util.List;

/** Declarative render-plan builder for TextureWidget visuals. */
public final class TextureWidgetRenderPlans {
    private TextureWidgetRenderPlans() {
    }

    public static RenderPlan defaultPlan(TextureWidgetState state) {
        if (state == null || state.texture() == null || state.placement() == null) return RenderPlan.EMPTY;
        return RenderPlan.of(List.of(new RenderPrimitive.Texture(
                state.texture(), state.placement(), state.radius(), Paint.fill(state.tint()))));
    }

    public static RenderPlan styledPlan(TextureWidgetState state, Style style, WidgetState widgetState) {
        if (state == null) return RenderPlan.EMPTY;
        TextureHandle texture = StyledRenderPlans.value(style, StyleKeys.BACKGROUND_TEXTURE, widgetState, state.texture());
        ColorView tint = StyledRenderPlans.value(style, StyleKeys.BACKGROUND_TEXTURE_TINT, widgetState, state.tint());
        ImageFit fit = StyledRenderPlans.value(style, StyleKeys.BACKGROUND_TEXTURE_FIT, widgetState, state.fit());
        Float radius = StyledRenderPlans.value(style, StyleKeys.RADIUS, widgetState, state.radius());
        TexturePlacement placement = state.placement();
        if (texture != state.texture() || fit != state.fit()) {
            placement = texture == null ? null : TexturePlacement.fit(texture,
                    state.source() == null ? new MutableRect(0.0f, 0.0f, 1.0f, 1.0f) : state.source(),
                    new MutableRect(state.x(), state.y(), state.width(), state.height()),
                    fit);
        }
        return defaultPlan(new TextureWidgetState(
                state.x(), state.y(), state.width(), state.height(), texture, state.source(), fit,
                radius == null ? state.radius() : radius,
                tint,
                placement));
    }
}