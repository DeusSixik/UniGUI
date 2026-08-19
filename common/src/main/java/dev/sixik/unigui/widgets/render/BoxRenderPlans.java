package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.render.TexturePlacement;
import dev.sixik.unigui.api.render.plan.RenderPlan;
import dev.sixik.unigui.api.render.plan.RenderPrimitive;
import dev.sixik.unigui.api.render.plan.StyledRenderPlans;
import dev.sixik.unigui.api.style.Style;
import dev.sixik.unigui.api.style.StyleKeys;
import dev.sixik.unigui.api.style.WidgetState;
import dev.sixik.unigui.api.render.ImageFit;
import dev.sixik.unigui.api.render.TextureHandle;
import dev.sixik.unigui.api.math.ColorView;

import java.util.ArrayList;
import java.util.List;

/** Declarative render-plan builder for the default Box visuals. */
public final class BoxRenderPlans {
    private BoxRenderPlans() {
    }

    public static RenderPlan defaultPlan(BoxState state) {
        if (state == null) return RenderPlan.EMPTY;
        List<RenderPrimitive> primitives = new ArrayList<>(3);
        if (state.backgroundVisible()) {
            primitives.add(new RenderPrimitive.RoundedRect(
                    state.x(), state.y(), state.width(), state.height(), state.radius(),
                    Paint.fill(state.background())));
        }
        if (state.backgroundTexture() != null && state.backgroundTexturePlacement() != null) {
            primitives.add(new RenderPrimitive.Texture(
                    state.backgroundTexture(), state.backgroundTexturePlacement(), state.radius(),
                    Paint.fill(state.backgroundTextureTint())));
        }
        if (state.borderVisible()) {
            primitives.add(new RenderPrimitive.RoundedRect(
                    state.x(), state.y(), state.width(), state.height(), state.radius(),
                    Paint.stroke(state.borderColor(), state.borderWidth())));
        }
        return RenderPlan.of(primitives);
    }

    public static RenderPlan styledPlan(BoxState state, Style style, WidgetState widgetState) {
        return defaultPlan(styledState(state, style, widgetState));
    }

    private static BoxState styledState(BoxState state, Style style, WidgetState widgetState) {
        if (state == null) return null;
        ColorView background = StyledRenderPlans.value(style, StyleKeys.BACKGROUND_COLOR, widgetState, state.background());
        TextureHandle texture = StyledRenderPlans.value(style, StyleKeys.BACKGROUND_TEXTURE, widgetState, state.backgroundTexture());
        ColorView textureTint = StyledRenderPlans.value(style, StyleKeys.BACKGROUND_TEXTURE_TINT, widgetState, state.backgroundTextureTint());
        ImageFit textureFit = StyledRenderPlans.value(style, StyleKeys.BACKGROUND_TEXTURE_FIT, widgetState, state.backgroundTextureFit());
        ColorView borderColor = StyledRenderPlans.value(style, StyleKeys.BORDER_COLOR, widgetState, state.borderColor());
        Float borderWidth = StyledRenderPlans.value(style, StyleKeys.BORDER_WIDTH, widgetState, state.borderWidth());
        Float radius = StyledRenderPlans.value(style, StyleKeys.RADIUS, widgetState, state.radius());
        TexturePlacement placement = state.backgroundTexturePlacement();
        if (texture != state.backgroundTexture() || textureFit != state.backgroundTextureFit()) {
            placement = texture == null ? null : TexturePlacement.fit(texture,
                    new MutableRect(0.0f, 0.0f, 1.0f, 1.0f),
                    new MutableRect(state.x(), state.y(), state.width(), state.height()),
                    textureFit);
        }
        return new BoxState(
                state.x(),
                state.y(),
                state.width(),
                state.height(),
                state.backgroundVisible(),
                background,
                texture,
                textureTint,
                placement,
                textureFit,
                radius == null ? state.radius() : radius,
                state.borderVisible(),
                borderColor,
                borderWidth == null ? state.borderWidth() : borderWidth);
    }
}