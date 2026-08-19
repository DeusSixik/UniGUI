package dev.sixik.unigui.widgets.display;

import dev.sixik.unigui.api.animation.TransitionSpec;
import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.render.ImageFit;
import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.render.TextureFilter;
import dev.sixik.unigui.api.render.TextureHandle;
import dev.sixik.unigui.api.render.TexturePlacement;
import dev.sixik.unigui.api.render.TextureWrap;
import dev.sixik.unigui.api.widget.skin.WidgetsRender;
import dev.sixik.unigui.api.xml.XmlAttribute;
import dev.sixik.unigui.api.xml.XmlTextureAttributes;
import dev.sixik.unigui.api.xml.XmlWidgetName;
import dev.sixik.unigui.impl.widget.WidgetBase;
import dev.sixik.unigui.widgets.render.TextureWidgetRenderer;
import dev.sixik.unigui.widgets.render.TextureWidgetState;
import dev.sixik.unigui.api.style.StyleAnimationIds;
import dev.sixik.unigui.api.style.StyleIds;

@XmlWidgetName("TextureWidget")
public class TextureWidget extends WidgetBase {
    public static final String STYLE_TYPE = StyleIds.Widget.TEXTURE_WIDGET;

    public static final class StyleProperties {
        public static final String BACKGROUND_TEXTURE = StyleIds.Key.BACKGROUND_TEXTURE;
        public static final String BACKGROUND_TEXTURE_TINT = StyleIds.Key.BACKGROUND_TEXTURE_TINT;
        public static final String BACKGROUND_TEXTURE_FIT = StyleIds.Key.BACKGROUND_TEXTURE_FIT;
        public static final String RADIUS = StyleIds.Key.RADIUS;

        private StyleProperties() {
        }
    }

    public static final class AnimationProperties {
        public static final String TEXTURE = StyleAnimationIds.Property.TEXTURE;
        public static final String TEXTURE_TINT = StyleAnimationIds.Property.TEXTURE_TINT;
        public static final String RADIUS = StyleAnimationIds.Property.RADIUS;
        public static final String OPACITY = StyleAnimationIds.Property.OPACITY;
        public static final String SCALE = StyleAnimationIds.Property.SCALE;
        public static final String ROTATION_DEGREES = StyleAnimationIds.Property.ROTATION_DEGREES;
        public static final java.util.List<String> ALL = StyleAnimationIds.Property.TEXTURE_WIDGET;

        private AnimationProperties() {
        }
    }

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

    @XmlAttribute(value = "texture", displayName = "Texture", category = "Assets", defaultValue = "", description = "Texture resource id resolved through XmlWidgetOptions.textureResolver.")
    public TextureWidget texture(TextureHandle texture) {
        if (this.texture == texture) return this;
        this.texture = texture;
        this.previousTexture = null;
        this.textureCrossfadeProgress = 1.0f;
        stopParameterAnimation(TEXTURE_CROSSFADE_KEY);
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    @XmlAttribute(value = "textureWidth", displayName = "Texture Width", category = "Assets", defaultValue = "16", description = "Source texture width used for contain and cover placement.")
    public TextureWidget textureWidth(int width) {
        return texture(XmlTextureAttributes.resize(texture, width, null));
    }

    @XmlAttribute(value = "textureHeight", displayName = "Texture Height", category = "Assets", defaultValue = "16", description = "Source texture height used for contain and cover placement.")
    public TextureWidget textureHeight(int height) {
        return texture(XmlTextureAttributes.resize(texture, null, height));
    }

    @XmlAttribute(value = "textureSampling", displayName = "Texture Sampling", category = "Assets", defaultValue = "nearest", description = "Texture filtering mode used by the renderer backend.")
    public TextureWidget textureSampling(TextureFilter filter) {
        return texture(XmlTextureAttributes.options(texture, options -> options.sampling(filter)));
    }

    @XmlAttribute(value = "textureWrap", displayName = "Texture Wrap", category = "Assets", defaultValue = "clamp-to-edge", description = "Texture coordinate wrap mode used by the renderer backend.")
    public TextureWidget textureWrap(TextureWrap wrap) {
        return texture(XmlTextureAttributes.options(texture, options -> options.wrap(wrap)));
    }

    @XmlAttribute(value = "textureMipmaps", displayName = "Texture Mipmaps", category = "Assets", defaultValue = "false", description = "Whether the texture should use mipmapped sampling.")
    public TextureWidget textureMipmaps(boolean mipmaps) {
        return texture(XmlTextureAttributes.options(texture, options -> options.mipmaps(mipmaps)));
    }

    @XmlAttribute(value = "texturePremultipliedAlpha", displayName = "Texture Premultiplied Alpha", category = "Assets", defaultValue = "false", description = "Whether the texture color data already uses premultiplied alpha.")
    public TextureWidget texturePremultipliedAlpha(boolean premultipliedAlpha) {
        return texture(XmlTextureAttributes.options(texture, options -> options.premultipliedAlpha(premultipliedAlpha)));
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

    @XmlAttribute(value = "tint", displayName = "Tint", category = "Assets", defaultValue = "#FFFFFFFF", description = "Tint color applied while drawing the texture.")
    public TextureWidget tint(ColorView color) {
        if (color != null) tint.set(color);
        return this;
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

    @XmlAttribute(value = "source", displayName = "Source", category = "Assets", defaultValue = "0 0 1 1", description = "Normalized UV source rectangle: u v width height.")
    public TextureWidget source(RectView source) {
        this.source.set(source == null ? new MutableRect(0.0f, 0.0f, 1.0f, 1.0f) : source);
        return this;
    }

    public ImageFit fit() {
        return fit;
    }

    @XmlAttribute(value = "fit", displayName = "Fit", category = "Assets", defaultValue = "stretch", description = "Placement mode for the texture.")
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

    @XmlAttribute(value = "radius", category = "Appearance", defaultValue = "0", description = "Corner radius used while rendering the texture.")
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
            DrawScope draw = new DrawScope(context, transform(), layoutBounds());
            if (previousTexture != null && textureCrossfadeProgress < 1.0f) {
                renderTextureState(context, draw, snapshot(
                        previousTexture,
                        previousSource,
                        previousFit,
                        multipliedAlpha(previousTint, 1.0f - textureCrossfadeProgress)));
            }
            if (texture != null) {
                ColorView effectiveTint = previousTexture == null
                        ? tint.copy()
                        : multipliedAlpha(tint, textureCrossfadeProgress);
                renderTextureState(context, draw, snapshot(texture, source, fit, effectiveTint));
            }
        } finally {
            popOpacity(context);
        }
    }

    protected void renderTextureState(RenderContext context, DrawScope draw, TextureWidgetState state) {
        if (renderer != null) {
            renderer.render(draw, state);
            return;
        }
        TextureWidgetRenderer styled = styleRendererOverride(TextureWidgetRenderer.class);
        if (styled != null) {
            styled.render(draw, state);
            return;
        }
        if (renderStylePlan(context, TextureWidgetState.class, state)) return;
        defaultRenderer().render(draw, state);
    }

    protected TextureWidgetRenderer defaultRenderer() {
        return WidgetsRender.textureWidget();
    }

    protected TextureWidgetRenderer effectiveRenderer() {
        return renderer == null ? styleRenderer(TextureWidgetRenderer.class, defaultRenderer()) : renderer;
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
