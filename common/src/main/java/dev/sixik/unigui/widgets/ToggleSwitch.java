package dev.sixik.unigui.widgets;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.style.StyleIds;
import dev.sixik.unigui.api.style.StyleKeys;
import dev.sixik.unigui.api.text.RichText;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.widget.skin.WidgetsRender;
import dev.sixik.unigui.impl.text.TextEngine;
import dev.sixik.unigui.widgets.render.ButtonRenderType;
import dev.sixik.unigui.widgets.render.ButtonRenderer;
import dev.sixik.unigui.widgets.render.ButtonState;

public class ToggleSwitch extends ToggleButton {
    public static final float DEFAULT_TRACK_WIDTH = 34.0f;
    public static final float DEFAULT_TRACK_HEIGHT = 18.0f;
    public static final float DEFAULT_THUMB_SIZE = 14.0f;
    public static final float DEFAULT_LABEL_GAP = 6.0f;

    private final MutableColor thumbColor = new MutableColor(0.95f, 0.95f, 0.95f, 1.0f);
    private float trackWidth = DEFAULT_TRACK_WIDTH;
    private float trackHeight = DEFAULT_TRACK_HEIGHT;
    private float thumbSize = DEFAULT_THUMB_SIZE;
    private float labelGap = DEFAULT_LABEL_GAP;

    public ToggleSwitch() {
        this("");
    }

    public ToggleSwitch(String text) {
        super(text);
        backgroundVisible(false);
        borderVisible(false);
        checkedBackground().set(0.25f, 0.78f, 1.0f, 1.0f);
        uncheckedBackground().set(0.22f, 0.22f, 0.22f, 1.0f);
        thumbColor.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
    }

    public ToggleSwitch(RichText text) {
        this("");
        richText(text);
    }

    @Override
    public ToggleSwitch checked(boolean checked) {
        super.checked(checked);
        return this;
    }

    @Override
    public ToggleSwitch silentChecked(boolean checked) {
        super.silentChecked(checked);
        return this;
    }

    public float trackWidth() {
        return trackWidth;
    }

    public ToggleSwitch trackWidth(float trackWidth) {
        float normalized = positiveOr(trackWidth, DEFAULT_TRACK_WIDTH);
        if (this.trackWidth == normalized) return this;
        this.trackWidth = normalized;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public float trackHeight() {
        return trackHeight;
    }

    public ToggleSwitch trackHeight(float trackHeight) {
        float normalized = positiveOr(trackHeight, DEFAULT_TRACK_HEIGHT);
        if (this.trackHeight == normalized) return this;
        this.trackHeight = normalized;
        if (thumbSize > normalized) thumbSize = Math.max(1.0f, normalized - 2.0f);
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public ToggleSwitch trackSize(float width, float height) {
        return trackWidth(width).trackHeight(height);
    }

    public float thumbSize() {
        return thumbSize;
    }

    public ToggleSwitch thumbSize(float thumbSize) {
        float normalized = Math.min(positiveOr(thumbSize, DEFAULT_THUMB_SIZE), trackHeight);
        if (this.thumbSize == normalized) return this;
        this.thumbSize = normalized;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public MutableColor thumbColor() {
        return thumbColor;
    }

    public ToggleSwitch thumbColor(ColorView color) {
        thumbColor.set(color);
        return this;
    }

    public float labelGap() {
        return labelGap;
    }

    public ToggleSwitch labelGap(float labelGap) {
        float normalized = Float.isFinite(labelGap) ? Math.max(0.0f, labelGap) : DEFAULT_LABEL_GAP;
        if (this.labelGap == normalized) return this;
        this.labelGap = normalized;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    @Override
    public void measure(LayoutContext context) {
        if (visibility() == Visibility.COLLAPSED) {
            setDesiredSize(0.0f, 0.0f);
            return;
        }
        float textWidth = Math.max(TextEngine.measureLineWidth(richText()), TextEngine.measureLineWidth(text()));
        float labelWidth = hasLabel() ? labelGap + textWidth : 0.0f;
        float textHeight = TextEngine.measureTextHeight(richText());
        setDesiredSize(resolveDesiredSize(context, trackWidth + labelWidth, Math.max(trackHeight, textHeight)));
    }

    @Override
    protected ButtonRenderer effectiveRenderer() {
        return renderer() == null ? styleRenderer(ButtonRenderer.class, WidgetsRender.toggleSwitch()) : renderer();
    }

    @Override
    protected ButtonState snapshot(RenderContext context) {
        return new ButtonState(
                ButtonRenderType.TOGGLE_SWITCH,
                layoutBounds().x(),
                layoutBounds().y(),
                layoutBounds().width(),
                layoutBounds().height(),
                text(),
                richText(),
                trackHeight,
                TextEngine.measureLineWidth(context, richText()),
                TextEngine.measureTextHeight(richText()),
                textColor().copy(),
                pressed(),
                hovered(),
                enabled(),
                checked(),
                false,
                trackWidth,
                thumbSize,
                hasLabel() ? labelGap : 0.0f,
                switchTrackColor(),
                thumbColor.copy());
    }

    @Override
    protected void applyTheme() {
        super.applyTheme();
        thumbColor.set(styleValue(StyleKeys.THUMB_COLOR, thumbColor));
    }

    @Override
    protected String styleType() {
        return StyleIds.Widget.TOGGLE_SWITCH;
    }

    private ColorView switchTrackColor() {
        ColorView fallback = checked() ? checkedBackground().copy() : uncheckedBackground().copy();
        ColorView themed = styleValue(StyleKeys.BACKGROUND_COLOR, styleState(), fallback);
        return themed == null ? fallback : themed;
    }

    private boolean hasLabel() {
        return richText() != null && !richText().isEmpty();
    }

    private static float positiveOr(float value, float fallback) {
        return Float.isFinite(value) && value > 0.0f ? value : fallback;
    }
}