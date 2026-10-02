package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.render.ImageFit;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.render.TextureHandle;
import dev.sixik.unigui.api.render.plan.RenderPlan;
import dev.sixik.unigui.api.render.plan.RenderPrimitive;
import dev.sixik.unigui.api.render.plan.StyledRenderPlans;
import dev.sixik.unigui.api.render.shaders.ShaderDrawOptions;
import dev.sixik.unigui.api.render.shaders.ShaderHandle;
import dev.sixik.unigui.api.render.shaders.ShaderUniforms;
import dev.sixik.unigui.api.style.Style;
import dev.sixik.unigui.api.style.StyleKeys;
import dev.sixik.unigui.api.style.WidgetState;
import dev.sixik.unigui.api.widget.visual.BackgroundKind;
import dev.sixik.unigui.api.widget.visual.SurfaceSnapshot;
import dev.sixik.unigui.widgets.containers.SurfaceWidget;

import java.util.ArrayList;
import java.util.List;

/**
 * Декларативный план отрисовки единой поверхности виджета.
 *
 * <p>Один рендер для всех kind фона: цвет, текстура или шейдер выбираются данными,
 * а не сменой renderer-класса. Стиль переключает kind через {@code background.kind}
 * без Java-кода.</p>
 */
public final class SurfacePlans {
    private SurfacePlans() {
    }

    public static RenderPlan defaultPlan(SurfaceSnapshot state) {
        if (state == null) return RenderPlan.EMPTY;
        List<RenderPrimitive> primitives = new ArrayList<>(3);
        if (state.backgroundVisible()) {
            switch (state.kind()) {
                case COLOR -> primitives.add(new RenderPrimitive.RoundedRect(
                        state.x(), state.y(), state.width(), state.height(), state.radius(),
                        Paint.fill(state.background())));
                case TEXTURE -> {
                    if (state.background() != null && state.background().a() > 0.0f) {
                        primitives.add(new RenderPrimitive.RoundedRect(
                                state.x(), state.y(), state.width(), state.height(), state.radius(),
                                Paint.fill(state.background())));
                    }
                    if (state.backgroundTexture() != null && state.backgroundTexturePlacement() != null) {
                        primitives.add(new RenderPrimitive.Texture(
                                state.backgroundTexture(), state.backgroundTexturePlacement(), state.radius(),
                                Paint.fill(state.backgroundTextureTint())));
                    }
                }
                case SHADER -> {
                    if (state.backgroundShader() != null) {
                        primitives.add(new RenderPrimitive.Shader(
                                state.backgroundShader(),
                                state.x(), state.y(), state.width(), state.height(),
                                state.shaderUniforms(), state.shaderOptions()));
                    } else if (state.background() != null) {
                        primitives.add(new RenderPrimitive.RoundedRect(
                                state.x(), state.y(), state.width(), state.height(), state.radius(),
                                Paint.fill(state.background())));
                    }
                }
                case NONE -> {
                }
            }
        }
        if (state.borderVisible()) {
            primitives.add(new RenderPrimitive.RoundedRect(
                    state.x(), state.y(), state.width(), state.height(), state.radius(),
                    Paint.stroke(state.borderColor(), state.borderWidth())));
        }
        return RenderPlan.of(primitives);
    }

    public static RenderPlan styledPlan(SurfaceSnapshot state, Style style, WidgetState widgetState) {
        return defaultPlan(styledState(state, style, widgetState));
    }

    /**
     * Рисует поверхность виджета, если она включена.
     *
     * <p>Используется skin-default renderer'ами кнопочного семейства: хром всегда
     * идёт из поверхности (цвет, текстура или шейдер по active kind), а контентный
     * renderer добавляет только foreground. Поэтому фон никогда не дублируется.</p>
     *
     * @param draw draw scope виджета
     * @param widget виджет с поверхностью
     */
    public static void renderWidgetSurface(dev.sixik.unigui.api.render.DrawScope draw, SurfaceWidget<?> widget) {
        if (draw == null || widget == null || !widget.boxVisualEnabled()) return;
        defaultPlan(widget.surface().snapshot(widget.layoutBounds())).render(draw);
    }

    private static SurfaceSnapshot styledState(SurfaceSnapshot state, Style style, WidgetState widgetState) {
        if (state == null) return null;
        BackgroundKind kind = StyledRenderPlans.value(style, StyleKeys.BACKGROUND_KIND, widgetState, state.kind());
        ColorView background = StyledRenderPlans.value(style, StyleKeys.BACKGROUND_COLOR, widgetState, state.background());
        TextureHandle texture = StyledRenderPlans.value(style, StyleKeys.BACKGROUND_TEXTURE, widgetState, state.backgroundTexture());
        ColorView tint = StyledRenderPlans.value(style, StyleKeys.BACKGROUND_TEXTURE_TINT, widgetState, state.backgroundTextureTint());
        ImageFit fit = StyledRenderPlans.value(style, StyleKeys.BACKGROUND_TEXTURE_FIT, widgetState, state.backgroundTextureFit());
        ShaderHandle shader = StyledRenderPlans.value(style, StyleKeys.BACKGROUND_SHADER, widgetState, state.backgroundShader());
        ColorView borderColor = StyledRenderPlans.value(style, StyleKeys.BORDER_COLOR, widgetState, state.borderColor());
        Float borderWidth = StyledRenderPlans.value(style, StyleKeys.BORDER_WIDTH, widgetState, state.borderWidth());
        Float radius = StyledRenderPlans.value(style, StyleKeys.RADIUS, widgetState, state.radius());
        dev.sixik.unigui.api.render.TexturePlacement placement = state.backgroundTexturePlacement();
        if (texture != state.backgroundTexture() || fit != state.backgroundTextureFit()) {
            placement = texture == null ? null : dev.sixik.unigui.api.render.TexturePlacement.fit(texture,
                    new dev.sixik.unigui.api.math.MutableRect(0.0f, 0.0f, 1.0f, 1.0f),
                    new dev.sixik.unigui.api.math.MutableRect(state.x(), state.y(), state.width(), state.height()),
                    fit);
        }
        BackgroundKind effectiveKind = kind == null ? state.kind() : kind;
        if (effectiveKind == null) effectiveKind = BackgroundKind.NONE;
        return new SurfaceSnapshot(
                state.x(), state.y(), state.width(), state.height(),
                effectiveKind, state.backgroundVisible(),
                background, texture, tint, placement, fit,
                shader, state.shaderUniforms(), state.shaderOptions(),
                radius == null ? state.radius() : radius,
                state.borderVisible(), borderColor,
                borderWidth == null ? state.borderWidth() : borderWidth);
    }
}
