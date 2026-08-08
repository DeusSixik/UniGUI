package dev.sixik.unigui.widgets;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.core.UIContext;
import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.render.ImageFit;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.render.TextureHandle;
import dev.sixik.unigui.api.render.TexturePlacement;
import dev.sixik.unigui.api.style.Style;
import dev.sixik.unigui.api.style.StyleKey;
import dev.sixik.unigui.api.style.StyleKeys;
import dev.sixik.unigui.api.style.Theme;
import dev.sixik.unigui.api.style.WidgetState;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.api.widget.skin.WidgetsRender;
import dev.sixik.unigui.widgets.render.BoxRenderer;
import dev.sixik.unigui.widgets.render.BoxState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Box extends PanelWidget {
    private final MutableColor background = new MutableColor(0.0f, 0.0f, 0.0f, 0.0f);
    private TextureHandle backgroundTexture;
    private final MutableColor backgroundTextureTint = new MutableColor(1.0f, 1.0f, 1.0f, 1.0f);
    private final MutableRect backgroundTextureSource = new MutableRect(0.0f, 0.0f, 1.0f, 1.0f);
    private ImageFit backgroundTextureFit = ImageFit.STRETCH;
    private final MutableColor borderColor = new MutableColor(1.0f, 1.0f, 1.0f, 1.0f);
    private BoxRenderer boxRenderer;
    private boolean backgroundVisible;
    private boolean borderVisible;
    private float borderWidth = 1.0f;
    private float radius;
    private boolean themeEnabled = true;
    private long lastAppliedStyleVersion = Long.MIN_VALUE;
    private long lastAppliedScopeStyleVersion = Long.MIN_VALUE;

    public Box() {
        background.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
        backgroundTextureTint.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
        backgroundTextureSource.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
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

    public BoxRenderer boxRenderer() {
        return boxRenderer;
    }

    public Box boxRenderer(BoxRenderer boxRenderer) {
        if (this.boxRenderer == boxRenderer) return this;
        this.boxRenderer = boxRenderer;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public Box useDefaultBoxRenderer() {
        return boxRenderer(null);
    }

    public TextureHandle backgroundTexture() {
        return backgroundTexture;
    }

    public Box backgroundTexture(TextureHandle backgroundTexture) {
        if (this.backgroundTexture == backgroundTexture) return this;
        this.backgroundTexture = backgroundTexture;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public MutableColor backgroundTextureTint() {
        return backgroundTextureTint;
    }

    public MutableRect backgroundTextureSource() {
        return backgroundTextureSource;
    }

    public Box backgroundTextureSource(float u, float v, float width, float height) {
        backgroundTextureSource.set(u, v, width, height);
        return this;
    }

    public ImageFit backgroundTextureFit() {
        return backgroundTextureFit;
    }

    public Box backgroundTextureFit(ImageFit fit) {
        ImageFit effectiveFit = fit == null ? ImageFit.STRETCH : fit;
        if (backgroundTextureFit == effectiveFit) return this;
        backgroundTextureFit = effectiveFit;
        invalidate(InvalidationFlags.VISUAL);
        return this;
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
        if (visibility() != Visibility.VISIBLE) return;
        pushOpacity(context);
        try {
            renderBox(context);
            renderContent(context);
        } finally {
            popOpacity(context);
        }
    }

    protected void renderBox(RenderContext context) {
        applyTheme();

        effectiveBoxRenderer().render(new DrawScope(context, transform()), boxState());
    }

    protected BoxRenderer effectiveBoxRenderer() {
        return boxRenderer == null ? WidgetsRender.box() : boxRenderer;
    }

    protected BoxState boxState() {
        TexturePlacement placement = backgroundTexture == null
                ? null
                : TexturePlacement.fit(backgroundTexture, backgroundTextureSource, layoutBounds(), backgroundTextureFit);
        return new BoxState(
                layoutBounds().x(),
                layoutBounds().y(),
                layoutBounds().width(),
                layoutBounds().height(),
                backgroundVisible,
                background.copy(),
                backgroundTexture,
                backgroundTextureTint.copy(),
                placement,
                backgroundTextureFit,
                radius,
                borderVisible,
                borderColor.copy(),
                borderWidth);
    }

    protected void renderContent(RenderContext context) {
        renderChildren(context);
    }

    protected void applyTheme() {
        if (!themeEnabled) return;
        UIContext context = uiContext();
        long styleVersion = context == null ? Theme.EMPTY.version() : context.styleVersion();
        long scopeStyleVersion = scopeStyleVersion();
        if ((lastAppliedStyleVersion != Long.MIN_VALUE && lastAppliedStyleVersion != styleVersion)
                || (lastAppliedScopeStyleVersion != Long.MIN_VALUE && lastAppliedScopeStyleVersion != scopeStyleVersion)) {
            invalidate(InvalidationFlags.VISUAL);
        }
        lastAppliedStyleVersion = styleVersion;
        lastAppliedScopeStyleVersion = scopeStyleVersion;

        ColorView themedBackground = styleValue(StyleKeys.BACKGROUND_COLOR, background);
        TextureHandle themedBackgroundTexture = styleValue(StyleKeys.BACKGROUND_TEXTURE, backgroundTexture);
        ColorView themedBackgroundTextureTint = styleValue(StyleKeys.BACKGROUND_TEXTURE_TINT, backgroundTextureTint);
        ImageFit themedBackgroundTextureFit = styleValue(StyleKeys.BACKGROUND_TEXTURE_FIT, backgroundTextureFit);
        ColorView themedBorder = styleValue(StyleKeys.BORDER_COLOR, borderColor);
        Float themedBorderWidth = styleValue(StyleKeys.BORDER_WIDTH, borderWidth);
        Float themedRadius = styleValue(StyleKeys.RADIUS, radius);

        if (themedBackground != null) {
            background.set(themedBackground);
        }
        if (backgroundTexture != themedBackgroundTexture) {
            backgroundTexture = themedBackgroundTexture;
            invalidate(InvalidationFlags.VISUAL);
        }
        if (themedBackgroundTextureTint != null) {
            backgroundTextureTint.set(themedBackgroundTextureTint);
        }
        if (themedBackgroundTextureFit != null && backgroundTextureFit != themedBackgroundTextureFit) {
            backgroundTextureFit = themedBackgroundTextureFit;
            invalidate(InvalidationFlags.VISUAL);
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
        if (!enabled()) return WidgetState.DISABLED;
        return hovered() ? WidgetState.HOVERED : WidgetState.NORMAL;
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
        String type = styleType();
        T value = theme.styleFor(type).get(key, state, fallback);
        for (Widget current : styleLookupChain()) {
            Style localStyle = current.localStyle(type);
            value = localStyle.get(key, state, value);
        }
        return value;
    }

    private long scopeStyleVersion() {
        long version = 0L;
        String type = styleType();
        for (Widget current : styleLookupChain()) {
            version += current.localStyle(type).version();
        }
        return version;
    }

    private List<Widget> styleLookupChain() {
        List<Widget> chain = new ArrayList<>();
        Widget current = this;
        while (current != null) {
            chain.add(current);
            if (current != this && current.styleScope()) {
                break;
            }
            current = current.parent();
        }
        Collections.reverse(chain);
        return chain;
    }
}
