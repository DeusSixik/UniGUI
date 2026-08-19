package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.render.plan.RenderPlan;
import dev.sixik.unigui.api.render.plan.RenderPrimitive;
import dev.sixik.unigui.api.render.plan.StyledRenderPlans;
import dev.sixik.unigui.api.style.Style;
import dev.sixik.unigui.api.style.StyleKeys;
import dev.sixik.unigui.api.style.WidgetState;
import dev.sixik.unigui.api.text.RichText;

import java.util.ArrayList;
import java.util.List;

/** Declarative render-plan builder for single-line text input visuals. */
public final class TextInputRenderPlans {
    private TextInputRenderPlans() {
    }

    public static RenderPlan defaultPlan(TextInputState state) {
        if (state == null) return RenderPlan.EMPTY;
        List<RenderPrimitive> viewportPrimitives = new ArrayList<>(3);
        if (state.focused() && state.hasSelection() && !state.showingPlaceholder()) {
            float selectionX = state.viewportX() + state.prefixWidth(state.selectionStart()) - state.horizontalScrollPixels();
            float selectionWidth = Math.max(1.0f,
                    state.prefixWidth(state.selectionEnd()) - state.prefixWidth(state.selectionStart()));
            viewportPrimitives.add(new RenderPrimitive.Rect(selectionX,
                    state.viewportY(),
                    selectionWidth,
                    state.viewportHeight(),
                    Paint.fill(state.caretColor())));
        }

        if (state.hasVisibleText()) {
            viewportPrimitives.add(new RenderPrimitive.RichTextBlock(
                    state.richText(),
                    state.viewportX() - state.horizontalScrollPixels(),
                    state.textY(),
                    Math.max(state.viewportWidth(), state.measuredTextWidth()),
                    state.textHeight(),
                    Paint.fill(state.showingPlaceholder() ? state.placeholderColor() : state.textColor()),
                    0.0f,
                    0.0f,
                    0.0f,
                    0.0f,
                    false));
        }

        if (state.focused()) {
            float caretX = state.viewportX() + state.prefixWidth(state.cursorIndex()) - state.horizontalScrollPixels();
            viewportPrimitives.add(new RenderPrimitive.Rect(caretX,
                    state.viewportY(),
                    1.0f,
                    state.viewportHeight(),
                    Paint.fill(state.caretColor())));
        }

        if (viewportPrimitives.isEmpty()) return RenderPlan.EMPTY;
        return RenderPlan.of(List.of(new RenderPrimitive.Clip(
                state.viewportX(),
                state.viewportY(),
                state.viewportWidth(),
                state.viewportHeight(),
                true,
                viewportPrimitives)));
    }

    public static RenderPlan searchFieldPlan(TextInputState state) {
        if (state == null) return RenderPlan.EMPTY;
        List<RenderPrimitive> primitives = new ArrayList<>(defaultPlan(state).primitives());
        if (state.clearButtonVisible()) {
            primitives.add(new RenderPrimitive.RichTextBlock(
                    RichText.plain("x"),
                    state.clearButtonX(),
                    state.clearButtonY(),
                    state.clearButtonWidth(),
                    state.clearButtonHeight(),
                    Paint.fill(state.placeholderColor()),
                    0.0f,
                    0.0f,
                    0.0f,
                    0.0f,
                    false));
        }
        return RenderPlan.of(primitives);
    }

    public static RenderPlan styledPlan(TextInputState state, Style style, WidgetState widgetState) {
        return defaultPlan(styledState(state, style, widgetState));
    }

    public static RenderPlan searchStyledPlan(TextInputState state, Style style, WidgetState widgetState) {
        return searchFieldPlan(styledState(state, style, widgetState));
    }

    private static TextInputState styledState(TextInputState state, Style style, WidgetState widgetState) {
        if (state == null) return null;
        ColorView textColor = StyledRenderPlans.value(style, StyleKeys.TEXT_COLOR, widgetState, state.textColor());
        ColorView placeholderColor = StyledRenderPlans.value(style, StyleKeys.PLACEHOLDER_COLOR, widgetState, state.placeholderColor());
        ColorView caretColor = StyledRenderPlans.value(style, StyleKeys.ACCENT_COLOR, widgetState, state.caretColor());
        return new TextInputState(
                state.type(),
                state.x(),
                state.y(),
                state.width(),
                state.height(),
                state.viewportX(),
                state.viewportY(),
                state.viewportWidth(),
                state.viewportHeight(),
                state.textY(),
                state.textHeight(),
                state.horizontalScrollPixels(),
                state.measuredTextWidth(),
                state.visibleText(),
                state.richText(),
                state.focused(),
                state.showingPlaceholder(),
                state.hasSelection(),
                state.selectionStart(),
                state.selectionEnd(),
                state.cursorIndex(),
                textColor,
                placeholderColor,
                caretColor,
                state.prefixWidths(),
                state.clearButtonVisible(),
                state.clearButtonX(),
                state.clearButtonY(),
                state.clearButtonWidth(),
                state.clearButtonHeight());
    }
}