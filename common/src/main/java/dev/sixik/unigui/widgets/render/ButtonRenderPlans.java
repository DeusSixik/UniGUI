package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.render.plan.RenderPlan;
import dev.sixik.unigui.api.render.plan.RenderPrimitive;
import dev.sixik.unigui.api.render.plan.StyledRenderPlans;
import dev.sixik.unigui.api.style.Style;
import dev.sixik.unigui.api.style.StyleKeys;
import dev.sixik.unigui.api.style.WidgetState;

import java.util.ArrayList;
import java.util.List;

/** Declarative render-plan builder for Button visuals. */
public final class ButtonRenderPlans {
    private static final float LEADING_LABEL_VISUAL_CENTER_OFFSET = 1.0f;

    private ButtonRenderPlans() {
    }

    public static RenderPlan defaultPlan(ButtonState state) {
        if (state == null) return RenderPlan.EMPTY;
        List<RenderPrimitive> primitives = new ArrayList<>(3);
        addChrome(primitives, state);
        addDefaultText(primitives, state);
        return RenderPlan.of(primitives);
    }

    public static RenderPlan chromePlan(ButtonState state) {
        if (state == null) return RenderPlan.EMPTY;
        List<RenderPrimitive> primitives = new ArrayList<>(2);
        addChrome(primitives, state);
        return RenderPlan.of(primitives);
    }

    public static RenderPlan textPlan(ButtonState state) {
        if (state == null || !state.hasText()) return RenderPlan.EMPTY;
        List<RenderPrimitive> primitives = new ArrayList<>(1);
        addDefaultText(primitives, state);
        return RenderPlan.of(primitives);
    }

    public static RenderPlan checkboxPlan(ButtonState state) {
        if (state == null) return RenderPlan.EMPTY;
        List<RenderPrimitive> primitives = new ArrayList<>(3);
        float labelGap = state.hasText() ? Math.max(0.0f, state.indicatorGap()) : 0.0f;
        float labelWidth = state.hasText()
                ? Math.min(Math.max(0.0f, state.textWidth()), Math.max(0.0f, state.width() - state.indicatorSize() - labelGap))
                : 0.0f;
        float indicatorX = state.labelLeft() ? state.x() + labelWidth + labelGap : state.x();
        float indicatorY = state.y() + Math.max(0.0f, state.height() - state.indicatorSize()) * 0.5f;
        primitives.add(new RenderPrimitive.RoundedRect(indicatorX, indicatorY,
                state.indicatorSize(), state.indicatorSize(), 2.0f,
                Paint.stroke(state.indicatorBorderColor(), 1.0f)));

        if (state.indeterminate()) {
            float dashWidth = Math.max(1.0f, state.indicatorInnerSize());
            float dashHeight = Math.max(1.0f, state.indicatorInnerSize() * 0.28f);
            float offsetX = Math.max(0.0f, (state.indicatorSize() - dashWidth) * 0.5f);
            float offsetY = Math.max(0.0f, (state.indicatorSize() - dashHeight) * 0.5f);
            primitives.add(new RenderPrimitive.Rect(indicatorX + offsetX, indicatorY + offsetY,
                    dashWidth, dashHeight,
                    Paint.fill(state.indicatorColor())));
        } else if (state.checked()) {
            float offset = Math.max(0.0f, (state.indicatorSize() - state.indicatorInnerSize()) * 0.5f);
            primitives.add(new RenderPrimitive.Rect(indicatorX + offset, indicatorY + offset,
                    state.indicatorInnerSize(), state.indicatorInnerSize(),
                    Paint.fill(state.indicatorColor())));
        }

        if (state.labelLeft()) {
            addLabel(primitives, state, state.x(), labelWidth);
        } else {
            addLeadingLabel(primitives, state);
        }
        return RenderPlan.of(primitives);
    }

    public static RenderPlan radioButtonPlan(ButtonState state) {
        if (state == null) return RenderPlan.EMPTY;
        List<RenderPrimitive> primitives = new ArrayList<>(3);
        float labelGap = state.hasText() ? Math.max(0.0f, state.indicatorGap()) : 0.0f;
        float labelWidth = state.hasText()
                ? Math.min(Math.max(0.0f, state.textWidth()), Math.max(0.0f, state.width() - state.indicatorSize() - labelGap))
                : 0.0f;
        float indicatorX = state.labelLeft() ? state.x() + labelWidth + labelGap : state.x();
        float indicatorY = state.y() + Math.max(0.0f, state.height() - state.indicatorSize()) * 0.5f;
        primitives.add(new RenderPrimitive.Circle(indicatorX, indicatorY,
                state.indicatorSize(), state.indicatorSize(),
                Paint.stroke(state.indicatorBorderColor(), 1.0f)));

        float progress = state.indicatorProgress();
        if (progress > 0.0f) {
            float innerSize = state.indicatorInnerSize() * progress;
            float offset = Math.max(0.0f, (state.indicatorSize() - innerSize) * 0.5f);
            primitives.add(new RenderPrimitive.Circle(indicatorX + offset, indicatorY + offset,
                    innerSize, innerSize,
                    Paint.fill(state.indicatorColor())));
        }

        if (state.labelLeft()) {
            addLabel(primitives, state, state.x(), labelWidth);
        } else {
            addLeadingLabel(primitives, state);
        }
        return RenderPlan.of(primitives);
    }

    public static RenderPlan toggleSwitchPlan(ButtonState state) {
        if (state == null) return RenderPlan.EMPTY;
        float trackWidth = Math.max(0.0f, state.indicatorSize());
        float trackHeight = Math.max(0.0f, state.textPaddingX());
        float thumbSize = Math.max(0.0f, state.indicatorInnerSize());
        if (trackWidth <= 0.0f || trackHeight <= 0.0f || thumbSize <= 0.0f) return RenderPlan.EMPTY;

        List<RenderPrimitive> primitives = new ArrayList<>(3);
        float labelGap = state.hasText() ? Math.max(0.0f, state.indicatorGap()) : 0.0f;
        float labelWidth = state.hasText()
                ? Math.min(Math.max(0.0f, state.textWidth()), Math.max(0.0f, state.width() - trackWidth - labelGap))
                : 0.0f;
        float trackX = state.labelLeft() ? state.x() + labelWidth + labelGap : state.x();
        float trackY = state.y() + Math.max(0.0f, state.height() - trackHeight) * 0.5f;
        float radius = trackHeight * 0.5f;
        primitives.add(new RenderPrimitive.RoundedRect(trackX, trackY,
                trackWidth, trackHeight, radius,
                Paint.fill(state.indicatorColor())));

        float thumbPadding = Math.max(1.0f, (trackHeight - thumbSize) * 0.5f);
        float thumbTravel = Math.max(0.0f, trackWidth - thumbSize - thumbPadding * 2.0f);
        float thumbX = trackX + thumbPadding + (state.checked() ? thumbTravel : 0.0f);
        float thumbY = trackY + Math.max(0.0f, trackHeight - thumbSize) * 0.5f;
        primitives.add(new RenderPrimitive.Circle(thumbX, thumbY, thumbSize, thumbSize,
                Paint.fill(state.indicatorBorderColor())));

        if (state.labelLeft()) {
            addLabel(primitives, state, state.x(), labelWidth);
        } else {
            addTrailingLabel(primitives, state, trackX + trackWidth + labelGap);
        }
        return RenderPlan.of(primitives);
    }

    public static RenderPlan styledPlan(ButtonState state, Style style, WidgetState widgetState) {
        ButtonState styled = styledState(state, style, widgetState);
        if (styled == null) return RenderPlan.EMPTY;
        return switch (styled.type() == null ? ButtonRenderType.BUTTON : styled.type()) {
            case CHECKBOX -> checkboxPlan(styled);
            case RADIO_BUTTON -> radioButtonPlan(styled);
            case TOGGLE_SWITCH -> toggleSwitchPlan(styled);
            case BUTTON, TOGGLE_BUTTON -> defaultPlan(styled);
        };
    }

    private static ButtonState styledState(ButtonState state, Style style, WidgetState widgetState) {
        if (state == null) return null;
        ColorView textColor = StyledRenderPlans.value(style, StyleKeys.TEXT_COLOR, widgetState, state.textColor());
        ColorView indicatorColor = StyledRenderPlans.value(style,
                state.type() == ButtonRenderType.TOGGLE_SWITCH ? StyleKeys.BACKGROUND_COLOR : StyleKeys.ACCENT_COLOR,
                widgetState,
                state.indicatorColor());
        ColorView indicatorBorderColor = StyledRenderPlans.value(style,
                state.type() == ButtonRenderType.TOGGLE_SWITCH ? StyleKeys.THUMB_COLOR : StyleKeys.BORDER_COLOR,
                widgetState,
                state.indicatorBorderColor());
        return new ButtonState(
                state.type(),
                state.x(),
                state.y(),
                state.width(),
                state.height(),
                state.text(),
                state.richText(),
                state.textPaddingX(),
                state.textWidth(),
                state.textHeight(),
                textColor,
                state.pressed(),
                state.hovered(),
                state.enabled(),
                state.checked(),
                state.indeterminate(),
                state.indicatorSize(),
                state.indicatorInnerSize(),
                state.indicatorGap(),
                indicatorColor,
                indicatorBorderColor,
                state.indicatorProgress(),
                state.labelLeft(),
                state.backgroundVisible(),
                StyledRenderPlans.value(style, StyleKeys.BACKGROUND_COLOR, widgetState, state.backgroundColor()),
                StyledRenderPlans.value(style, StyleKeys.RADIUS, widgetState, state.radius()),
                state.borderVisible(),
                StyledRenderPlans.value(style, StyleKeys.BORDER_COLOR, widgetState, state.borderColor()),
                StyledRenderPlans.value(style, StyleKeys.BORDER_WIDTH, widgetState, state.borderWidth()));
    }

    private static void addDefaultText(List<RenderPrimitive> primitives, ButtonState state) {
        if (!state.hasText()) return;
        float contentX = state.textContentX();
        float contentWidth = state.textContentWidth();
        if (contentWidth <= 0.0f) return;
        float drawWidth = Math.min(Math.max(0.0f, contentWidth), Math.max(0.0f, state.textWidth()));
        float drawHeight = Math.min(Math.max(0.0f, state.height()), Math.max(0.0f, state.textHeight()));
        float drawX = contentX + Math.max(0.0f, contentWidth - drawWidth) * 0.5f;
        float drawY = state.y() + Math.max(0.0f, state.height() - drawHeight) * 0.5f;
        primitives.add(richText(state, drawX, drawY, drawWidth, drawHeight,
                contentX, state.y(), contentWidth, state.height()));
    }

    private static void addChrome(List<RenderPrimitive> primitives, ButtonState state) {
        if (state.backgroundVisible()) {
            primitives.add(new RenderPrimitive.RoundedRect(
                    state.x(), state.y(), state.width(), state.height(), state.radius(),
                    Paint.fill(state.backgroundColor())));
        }
        if (state.borderVisible() && state.borderWidth() > 0.0f) {
            primitives.add(new RenderPrimitive.RoundedRect(
                    state.x(), state.y(), state.width(), state.height(), state.radius(),
                    Paint.stroke(state.borderColor(), state.borderWidth())));
        }
    }

    private static void addLeadingLabel(List<RenderPrimitive> primitives, ButtonState state) {
        if (!state.hasText()) return;
        float contentX = state.x() + state.indicatorSize() + state.indicatorGap();
        float contentWidth = Math.max(0.0f, state.width() - state.indicatorSize() - state.indicatorGap());
        float drawHeight = Math.min(Math.max(0.0f, state.height()), Math.max(0.0f, state.textHeight()));
        float indicatorY = state.y() + Math.max(0.0f, state.height() - state.indicatorSize()) * 0.5f;
        float indicatorCenterY = indicatorY + state.indicatorSize() * 0.5f;
        float drawY = indicatorCenterY - drawHeight * 0.5f + LEADING_LABEL_VISUAL_CENTER_OFFSET;
        primitives.add(richText(state, contentX, drawY, contentWidth, drawHeight,
                contentX, state.y(), contentWidth, state.height()));
    }

    private static void addTrailingLabel(List<RenderPrimitive> primitives, ButtonState state, float contentX) {
        if (!state.hasText()) return;
        float contentWidth = Math.max(0.0f, state.width() - (contentX - state.x()));
        addLabel(primitives, state, contentX, contentWidth);
    }

    private static void addLabel(List<RenderPrimitive> primitives, ButtonState state, float contentX, float contentWidth) {
        if (!state.hasText() || contentWidth <= 0.0f) return;
        float drawHeight = Math.min(Math.max(0.0f, state.height()), Math.max(0.0f, state.textHeight()));
        float drawY = state.y() + Math.max(0.0f, state.height() - drawHeight) * 0.5f;
        primitives.add(richText(state, contentX, drawY, contentWidth, drawHeight,
                contentX, state.y(), contentWidth, state.height()));
    }

    private static RenderPrimitive.RichTextBlock richText(ButtonState state,
                                                          float x,
                                                          float y,
                                                          float width,
                                                          float height,
                                                          float clipX,
                                                          float clipY,
                                                          float clipWidth,
                                                          float clipHeight) {
        return new RenderPrimitive.RichTextBlock(
                state.richText(),
                x,
                y,
                width,
                height,
                Paint.fill(state.textColor()),
                clipX,
                clipY,
                clipWidth,
                clipHeight,
                true);
    }
}