package dev.sixik.unigui.widgets;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.core.UIContext;
import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.style.Style;
import dev.sixik.unigui.api.style.StyleKey;
import dev.sixik.unigui.api.style.StyleKeys;
import dev.sixik.unigui.api.style.Theme;
import dev.sixik.unigui.api.style.WidgetState;

public class Box extends PanelWidget {
    private final MutableColor background = new MutableColor(0.0f, 0.0f, 0.0f, 0.0f);
    private final MutableColor borderColor = new MutableColor(1.0f, 1.0f, 1.0f, 1.0f);
    private boolean backgroundVisible;
    private boolean borderVisible;
    private float borderWidth = 1.0f;
    private float radius;
    private boolean themeEnabled = true;

    public Box() {
        background.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
        borderColor.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
    }

    public MutableColor background() {
        return background;
    }

    public Box backgroundVisible(boolean backgroundVisible) {
        if (this.backgroundVisible == backgroundVisible) return this;
        this.backgroundVisible = backgroundVisible;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public boolean backgroundVisible() {
        return backgroundVisible;
    }

    public MutableColor borderColor() {
        return borderColor;
    }

    public Box borderVisible(boolean borderVisible) {
        if (this.borderVisible == borderVisible) return this;
        this.borderVisible = borderVisible;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public boolean borderVisible() {
        return borderVisible;
    }

    public float borderWidth() {
        return borderWidth;
    }

    public Box borderWidth(float borderWidth) {
        if (this.borderWidth == borderWidth) return this;
        this.borderWidth = borderWidth;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public float radius() {
        return radius;
    }

    public Box radius(float radius) {
        if (this.radius == radius) return this;
        this.radius = radius;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public boolean themeEnabled() {
        return themeEnabled;
    }

    public Box themeEnabled(boolean themeEnabled) {
        if (this.themeEnabled == themeEnabled) return this;
        this.themeEnabled = themeEnabled;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    @Override
    public void render(RenderContext context) {
        renderBox(context);
        renderContent(context);
    }

    protected void renderBox(RenderContext context) {
        applyTheme();

        float x = layoutBounds().x();
        float y = layoutBounds().y();
        float width = layoutBounds().width();
        float height = layoutBounds().height();

        if (backgroundVisible) {
            context.roundedRect(x, y, width, height, radius, Paint.fill(background), transform());
        }

        if (borderVisible) {
            context.roundedRect(x, y, width, height, radius, Paint.stroke(borderColor, borderWidth), transform());
        }
    }

    protected void renderContent(RenderContext context) {
        super.render(context);
    }

    protected void applyTheme() {
        if (!themeEnabled) return;

        ColorView themedBackground = styleValue(StyleKeys.BACKGROUND_COLOR, background);
        ColorView themedBorder = styleValue(StyleKeys.BORDER_COLOR, borderColor);
        Float themedBorderWidth = styleValue(StyleKeys.BORDER_WIDTH, borderWidth);
        Float themedRadius = styleValue(StyleKeys.RADIUS, radius);

        if (themedBackground != null) {
            background.set(themedBackground);
        }
        if (themedBorder != null) {
            borderColor.set(themedBorder);
        }
        if (themedBorderWidth != null && borderWidth != themedBorderWidth) {
            borderWidth = themedBorderWidth;
            invalidate(InvalidationFlags.VISUAL);
        }
        if (themedRadius != null && radius != themedRadius) {
            radius = themedRadius;
            invalidate(InvalidationFlags.VISUAL);
        }
    }

    protected WidgetState styleState() {
        return WidgetState.NORMAL;
    }

    protected String styleType() {
        return getClass().getSimpleName();
    }

    protected <T> T styleValue(StyleKey<T> key, T fallback) {
        return styleValue(key, styleState(), fallback);
    }

    protected <T> T styleValue(StyleKey<T> key, WidgetState state, T fallback) {
        if (!themeEnabled) return fallback;
        UIContext context = uiContext();
        Theme theme = context == null ? Theme.EMPTY : context.theme();
        Style style = theme.styleFor(styleType());
        return style.get(key, state, fallback);
    }
}
