package dev.sixik.unigui.widgets;

import dev.sixik.unigui.api.animation.TransitionSpec;
import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.render.ImageFit;
import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.render.TextureHandle;
import dev.sixik.unigui.api.render.TexturePlacement;
import dev.sixik.unigui.api.widget.skin.WidgetsRender;
import dev.sixik.unigui.impl.widget.WidgetBase;
import dev.sixik.unigui.widgets.render.TextureWidgetRenderer;
import dev.sixik.unigui.widgets.render.TextureWidgetState;

public class TextureWidget extends WidgetBase {
    private static final String TEXTURE_CROSSFADE_KEY = "TextureWidget.textureCrossfade";

    private TextureHandle texture;
    private TextureHandle previousTexture;
    private final MutableColor tint = new MutableColor(1.0f, 1.0f, 1.0f, 1.0f);
    private final MutableRect source = new MutableRect(0.0f, 0.0f, 1.0f, 1.0f);
    private final MutableRect previousSource = new MutableRect(0.0f, 0.0f, 1.0f, 1.0f);
    private TextureWidgetRenderer renderer;
    private ImageFit fit = ImageFit.STRETCH;
    private ImageFit previousFit = ImageFit.STRETCH;
    private final MutableColor previousTint = new MutableColor(1.0f, 1.0f, 1.0f, 1.0f);
    private float radius;
    private float textureCrossfadeProgress = 1.0f;

    public TextureWidget() {
        tint.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
        source.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
    }

    public TextureWidget(TextureHandle texture) {
        this();
        this.texture = texture;
    }

    public TextureHandle texture() {
        return texture;
    }

    public TextureWidget texture(TextureHandle texture) {
        if (this.texture == texture) return this;
        this.texture = texture;
        this.previousTexture = null;
        this.textureCrossfadeProgress = 1.0f;
        stopParameterAnimation(TEXTURE_CROSSFADE_KEY);
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public TextureWidget animateTexture(TextureHandle texture, float durationSeconds) {
        return animateTexture(texture, TransitionSpec.of(durationSeconds));
    }

    public TextureWidget animateTexture(TextureHandle texture, TransitionSpec spec) {
        if (this.texture == texture) return this;
        TransitionSpec normalized = spec == null ? TransitionSpec.DEFAULT : spec;
        if (this.texture == null || normalized.durationSeconds() <= 0.0f) {
            return texture(texture);
        }

        previousTexture = this.texture;
        previousSource.set(source);
        previousFit = fit;
        previousTint.set(tint);
        this.texture = texture;
        textureCrossfadeProgress = 0.0f;
        animateParameterFrom(TEXTURE_CROSSFADE_KEY, 0.0f, this::setTextureCrossfadeProgress, 1.0f, normalized);
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public MutableColor tint() {
        return tint;
    }

    public TextureWidget animateTint(ColorView color, float durationSeconds) {
        animateColor(tint, color, durationSeconds);
        return this;
    }

    public TextureWidget animateTint(ColorView color, TransitionSpec spec) {
        animateColor(tint, color, spec);
        return this;
    }

    public MutableRect source() {
        return source;
    }

    public TextureWidgetRenderer renderer() {
        return renderer;
    }

    public TextureWidget renderer(TextureWidgetRenderer renderer) {
        if (this.renderer == renderer) return this;
        this.renderer = renderer;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public TextureWidget useDefaultRenderer() {
        return renderer(null);
    }

    public TextureWidget source(float u, float v, float width, float height) {
        source.set(u, v, width, height);
        return this;
    }

    public ImageFit fit() {
        return fit;
    }

    public TextureWidget fit(ImageFit fit) {
        ImageFit effectiveFit = fit == null ? ImageFit.STRETCH : fit;
        if (this.fit == effectiveFit) return this;
        this.fit = effectiveFit;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public float radius() {
        return radius;
    }

    public TextureWidget radius(float radius) {
        float effectiveRadius = Float.isFinite(radius) ? Math.max(0.0f, radius) : 0.0f;
        if (this.radius == effectiveRadius) return this;
        this.radius = effectiveRadius;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public TextureWidget animateRadius(float radius, float durationSeconds) {
        return animateRadius(radius, TransitionSpec.of(durationSeconds));
    }

    public TextureWidget animateRadius(float radius, TransitionSpec spec) {
        animateParameter("TextureWidget.radius", this::radius, this::radius, radius, spec);
        return this;
    }

    @Override
    public void render(RenderContext context) {
        if (texture == null && previousTexture == null) return;
        pushOpacity(context);
        try {
            TextureWidgetRenderer effective = effectiveRenderer();
            DrawScope draw = new DrawScope(context, transform(), layoutBounds());
            if (previousTexture != null && textureCrossfadeProgress < 1.0f) {
                effective.render(draw, snapshot(
                        previousTexture,
                        previousSource,
                        previousFit,
                        multipliedAlpha(previousTint, 1.0f - textureCrossfadeProgress)));
            }
            if (texture != null) {
                ColorView effectiveTint = previousTexture == null
                        ? tint.copy()
                        : multipliedAlpha(tint, textureCrossfadeProgress);
                effective.render(draw, snapshot(texture, source, fit, effectiveTint));
            }
        } finally {
            popOpacity(context);
        }
    }

    protected TextureWidgetRenderer effectiveRenderer() {
        return renderer == null ? styleRenderer(TextureWidgetRenderer.class, WidgetsRender.textureWidget()) : renderer;
    }

    protected TextureWidgetState snapshot() {
        return snapshot(texture, source, fit, tint.copy());
    }

    protected TextureWidgetState snapshot(TextureHandle texture, RectView source, ImageFit fit, ColorView tint) {
        TexturePlacement placement = texture == null ? null : TexturePlacement.fit(texture, source, layoutBounds(), fit);
        return new TextureWidgetState(
                layoutBounds().x(),
                layoutBounds().y(),
                layoutBounds().width(),
                layoutBounds().height(),
                texture,
                source == null ? new MutableRect(0.0f, 0.0f, 1.0f, 1.0f) : new MutableRect(source.x(), source.y(), source.width(), source.height()),
                fit,
                radius,
                tint == null ? new MutableColor() : new MutableColor(tint.r(), tint.g(), tint.b(), tint.a()),
                placement);
    }

    private void setTextureCrossfadeProgress(float progress) {
        textureCrossfadeProgress = Math.max(0.0f, Math.min(1.0f, Float.isFinite(progress) ? progress : 1.0f));
        if (textureCrossfadeProgress >= 1.0f) {
            previousTexture = null;
        }
        invalidate(InvalidationFlags.VISUAL);
    }

    private static MutableColor multipliedAlpha(ColorView color, float alphaMultiplier) {
        ColorView safe = color == null ? new MutableColor() : color;
        float multiplier = Float.isFinite(alphaMultiplier) ? Math.max(0.0f, Math.min(1.0f, alphaMultiplier)) : 1.0f;
        return new MutableColor(safe.r(), safe.g(), safe.b(), safe.a() * multiplier);
    }
}
