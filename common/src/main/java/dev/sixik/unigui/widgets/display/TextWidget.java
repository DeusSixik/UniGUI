package dev.sixik.unigui.widgets.display;

import dev.sixik.unigui.api.animation.TransitionSpec;
import dev.sixik.unigui.api.core.FrameContext;
import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.layout.Alignment;
import dev.sixik.unigui.api.layout.EdgeInsets;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.layout.LayoutSize;
import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.math.Transform;
import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.render.ImageFit;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.render.TextureFilter;
import dev.sixik.unigui.api.render.TextureHandle;
import dev.sixik.unigui.api.render.TexturePlacement;
import dev.sixik.unigui.api.render.TextureWrap;
import dev.sixik.unigui.api.text.FontFace;
import dev.sixik.unigui.api.text.RichText;
import dev.sixik.unigui.api.text.RichTextSpan;
import dev.sixik.unigui.api.text.RichTextSpan;
import dev.sixik.unigui.api.text.TextBrush;
import dev.sixik.unigui.api.text.TextRun;
import dev.sixik.unigui.api.text.TextOverflowMode;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.xml.XmlAttribute;
import dev.sixik.unigui.api.xml.XmlTextureAttributes;
import dev.sixik.unigui.api.xml.XmlWidgetName;
import dev.sixik.unigui.impl.text.TextEngine;
import dev.sixik.unigui.impl.widget.WidgetBase;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;
import java.util.Objects;

@XmlWidgetName("TextWidget")
public class TextWidget extends WidgetBase {
    public static final String STYLE_TYPE = dev.sixik.unigui.api.style.StyleIds.Widget.TEXT_WIDGET;

    protected static final float APPROX_CHAR_WIDTH = TextEngine.APPROX_CHAR_WIDTH;
    protected static final float LINE_HEIGHT = TextEngine.LINE_HEIGHT;

    private String text = "";
    private RichText richText;
    private final MutableColor color = new MutableColor(1.0f, 1.0f, 1.0f, 1.0f);
    private boolean wrap = true;
    private TextOverflowMode overflowMode = TextOverflowMode.VISIBLE;
    private float marqueeSpeed = 24.0f;
    private float marqueeGap = 24.0f;
    private float marqueeOffset;
    private boolean marqueeActive;
    private RichText wrappedCacheText;
    private Object wrappedCacheBackend;
    private float wrappedCacheWidth = Float.NaN;
    private List<RichText> wrappedCacheLines = List.of();

    private final MutableColor background = new MutableColor(0.0f, 0.0f, 0.0f, 0.0f);
    private boolean backgroundVisible;
    private boolean boxVisualEnabled = true;
    private TextureHandle backgroundTexture;
    private final MutableColor backgroundTextureTint = new MutableColor(1.0f, 1.0f, 1.0f, 1.0f);
    private final MutableRect backgroundTextureSource = new MutableRect(0.0f, 0.0f, 1.0f, 1.0f);
    private ImageFit backgroundTextureFit = ImageFit.STRETCH;

    private final MutableColor borderColor = new MutableColor(1.0f, 1.0f, 1.0f, 1.0f);
    private boolean borderVisible;
    private float borderWidth = 1.0f;
    private float radius;

    public TextWidget() {
        color.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
        background.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
        backgroundTextureTint.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
        backgroundTextureSource.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
        borderColor.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
    }

    public TextWidget(String text) {
        this();
        text(text);
    }

    public String text() {
        return text;
    }

    @XmlAttribute(value = "text", category = "Content", defaultValue = "", description = "Plain text content displayed by the widget.")
    public TextWidget text(String text) {
        String normalized = normalize(text);
        RichText normalizedRichText = RichText.resolve(normalized);
        if (Objects.equals(this.richText, normalizedRichText)) return this;
        this.text = normalized;
        this.richText = normalizedRichText;
        clearWrapCache();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public RichText richText() {
        return richText;
    }

    public TextWidget richText(RichText richText) {
        RichText normalized = richText == null ? RichText.plain("") : richText;
        if (Objects.equals(this.richText, normalized)) return this;
        this.richText = normalized;
        this.text = normalized.plainText();
        clearWrapCache();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    /** Selects a face for the current plain text while preserving the normal TextWidget API. */
    public TextWidget font(FontFace font, float pixelSize) {
        return richText(RichText.of(text, font, pixelSize));
    }

    /**
     * Применяет brush-заливку ко всем текстовым run'ам текущего rich text.
     *
     * @param brush brush или {@code null}, чтобы вернуть обычную solid-заливку
     * @return этот widget для fluent-настройки
     */
    public TextWidget textBrush(TextBrush brush) {
        return richText(effectiveRichText().withBrush(brush));
    }

    /**
     * Применяет brush-заливку из XML/XAML-строки.
     *
     * <p>Примеры: {@code solid(#FFFFFF)}, {@code #FFFFFF},
     * {@code linear-gradient(#60D8FF, #F7C45A, 35)} или {@code none}.</p>
     *
     * @param expression строковое описание brush'а
     * @return этот widget для fluent-настройки
     */
    @XmlAttribute(value = "textBrush", category = "Appearance", defaultValue = "none",
            description = "Text brush expression: solid(#RRGGBB) or linear-gradient(#RRGGBB, #RRGGBB, angle).")
    public TextWidget textBrushExpression(String expression) {
        return textBrush(TextBrush.parse(expression));
    }

    /**
     * Применяет линейный градиент ко всем текстовым run'ам текущего rich text.
     *
     * @param startColor цвет начала градиента
     * @param endColor цвет конца градиента
     * @param angleDegrees угол направления в градусах
     * @return этот widget для fluent-настройки
     */
    public TextWidget textGradient(ColorView startColor, ColorView endColor, float angleDegrees) {
        return textBrush(TextBrush.linearGradient(startColor, endColor, angleDegrees));
    }

    /**
     * Сбрасывает brush-заливку текста.
     *
     * @return этот widget для fluent-настройки
     */
    public TextWidget clearTextBrush() {
        return textBrush(null);
    }

    public Alignment textAlignment() {
        return textHorizontalAlignment();
    }

    public TextWidget textAlignment(Alignment alignment) {
        layoutStyle().horizontalAlignment(alignment);
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public MutableColor color() {
        return color;
    }

    @XmlAttribute(value = "color", category = "Appearance", defaultValue = "#FFFFFFFF", description = "Text color parsed from XML color syntax.")
    public TextWidget color(ColorView color) {
        if (color != null) this.color.set(color);
        return this;
    }

    public boolean wrap() {
        return wrap;
    }

    @XmlAttribute(value = "wrap", category = "Content", defaultValue = "true", description = "Whether text wraps within available width.")
    public TextWidget wrap(boolean wrap) {
        if (this.wrap == wrap) return this;
        this.wrap = wrap;
        clearWrapCache();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public TextWidget wrapText() {
        return wrap(true);
    }

    public TextWidget noWrap() {
        return wrap(false);
    }

    public TextOverflowMode overflowMode() {
        return overflowMode;
    }

    @XmlAttribute(value = "overflowMode", category = "Content", defaultValue = "visible", description = "How text behaves when it exceeds its layout bounds.")
    public TextWidget overflowMode(TextOverflowMode overflowMode) {
        TextOverflowMode normalized = overflowMode == null ? TextOverflowMode.VISIBLE : overflowMode;
        if (this.overflowMode == normalized) return this;
        this.overflowMode = normalized;
        marqueeOffset = 0.0f;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public TextWidget clipOverflow() {
        return overflowMode(TextOverflowMode.CLIP);
    }

    public TextWidget shrinkToFit() {
        return overflowMode(TextOverflowMode.SHRINK_TO_FIT);
    }

    public TextWidget marqueeOnHover() {
        return overflowMode(TextOverflowMode.MARQUEE_ON_HOVER);
    }

    public float marqueeSpeed() {
        return marqueeSpeed;
    }

    @XmlAttribute(value = "marqueeSpeed", category = "Content", defaultValue = "24", description = "Marquee scroll speed in pixels per second.")
    public TextWidget marqueeSpeed(float marqueeSpeed) {
        float normalized = Float.isFinite(marqueeSpeed) ? Math.max(0.0f, marqueeSpeed) : 24.0f;
        if (this.marqueeSpeed == normalized) return this;
        this.marqueeSpeed = normalized;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public float hoverScrollSpeed() {
        return marqueeSpeed();
    }

    public TextWidget hoverScrollSpeed(float pixelsPerSecond) {
        return marqueeSpeed(pixelsPerSecond);
    }

    public boolean marqueeActive() {
        return marqueeActive;
    }

    public TextWidget marqueeActive(boolean marqueeActive) {
        if (this.marqueeActive == marqueeActive) return this;
        this.marqueeActive = marqueeActive;
        if (!marqueeActive) {
            marqueeOffset = 0.0f;
        }
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public float marqueeGap() {
        return marqueeGap;
    }

    @XmlAttribute(value = "marqueeGap", category = "Content", defaultValue = "24", description = "Gap between repeated marquee text runs.")
    public TextWidget marqueeGap(float marqueeGap) {
        float normalized = Math.max(0.0f, marqueeGap);
        if (this.marqueeGap == normalized) return this;
        this.marqueeGap = normalized;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public MutableColor background() {
        return background;
    }

    @XmlAttribute(value = "background", category = "Appearance", defaultValue = "#00000000", description = "Background color; setting it also enables background rendering.")
    public TextWidget background(ColorView color) {
        background.set(color == null ? new MutableColor(0.0f, 0.0f, 0.0f, 0.0f) : color);
        backgroundVisible(true);
        return this;
    }

    public TextWidget background(float r, float g, float b, float a) {
        background.set(r, g, b, a);
        backgroundVisible(true);
        return this;
    }

    public TextWidget animateBackgroundColor(ColorView color, float durationSeconds) {
        animateColor(background, color, durationSeconds);
        return this;
    }

    public TextWidget animateBackgroundColor(ColorView color, TransitionSpec spec) {
        animateColor(background, color, spec);
        return this;
    }

    @XmlAttribute(value = "backgroundVisible", category = "Appearance", defaultValue = "false", description = "Whether background is rendered.")
    public TextWidget backgroundVisible(boolean backgroundVisible) {
        if (this.backgroundVisible == backgroundVisible) return this;
        this.backgroundVisible = backgroundVisible;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public boolean backgroundVisible() {
        return backgroundVisible;
    }

    public boolean boxVisualEnabled() {
        return boxVisualEnabled;
    }

    @XmlAttribute(value = "boxVisualEnabled", category = "Appearance", defaultValue = "true", description = "Whether background/border visual rendering is enabled.")
    public TextWidget boxVisualEnabled(boolean boxVisualEnabled) {
        if (this.boxVisualEnabled == boxVisualEnabled) return this;
        this.boxVisualEnabled = boxVisualEnabled;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public TextureHandle backgroundTexture() {
        return backgroundTexture;
    }

    @XmlAttribute(value = "backgroundTexture", displayName = "Background Texture", category = "Assets", defaultValue = "", description = "Background texture.")
    public TextWidget backgroundTexture(TextureHandle backgroundTexture) {
        if (this.backgroundTexture == backgroundTexture) return this;
        this.backgroundTexture = backgroundTexture;
        if (backgroundTexture != null) {
            backgroundVisible(true);
        }
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    @XmlAttribute(value = "backgroundTextureWidth", displayName = "Background Texture Width", category = "Assets", defaultValue = "16", description = "Source texture width.")
    public TextWidget backgroundTextureWidth(int width) {
        return backgroundTexture(XmlTextureAttributes.resize(backgroundTexture, width, null));
    }

    @XmlAttribute(value = "backgroundTextureHeight", displayName = "Background Texture Height", category = "Assets", defaultValue = "16", description = "Source texture height.")
    public TextWidget backgroundTextureHeight(int height) {
        return backgroundTexture(XmlTextureAttributes.resize(backgroundTexture, null, height));
    }

    @XmlAttribute(value = "backgroundTextureSampling", displayName = "Background Texture Sampling", category = "Assets", defaultValue = "nearest", description = "Texture filtering mode.")
    public TextWidget backgroundTextureSampling(TextureFilter filter) {
        return backgroundTexture(XmlTextureAttributes.options(backgroundTexture, options -> options.sampling(filter)));
    }

    @XmlAttribute(value = "backgroundTextureWrap", displayName = "Background Texture Wrap", category = "Assets", defaultValue = "clamp-to-edge", description = "Texture wrap mode.")
    public TextWidget backgroundTextureWrap(TextureWrap wrap) {
        return backgroundTexture(XmlTextureAttributes.options(backgroundTexture, options -> options.wrap(wrap)));
    }

    @XmlAttribute(value = "backgroundTextureMipmaps", displayName = "Background Texture Mipmaps", category = "Assets", defaultValue = "false", description = "Whether texture uses mipmaps.")
    public TextWidget backgroundTextureMipmaps(boolean mipmaps) {
        return backgroundTexture(XmlTextureAttributes.options(backgroundTexture, options -> options.mipmaps(mipmaps)));
    }

    @XmlAttribute(value = "backgroundTexturePremultipliedAlpha", displayName = "Background Texture Premultiplied Alpha", category = "Assets", defaultValue = "false", description = "Premultiplied alpha flag.")
    public TextWidget backgroundTexturePremultipliedAlpha(boolean premultipliedAlpha) {
        return backgroundTexture(XmlTextureAttributes.options(backgroundTexture, options -> options.premultipliedAlpha(premultipliedAlpha)));
    }

    public MutableColor backgroundTextureTint() {
        return backgroundTextureTint;
    }

    @XmlAttribute(value = "backgroundTextureTint", displayName = "Background Texture Tint", category = "Assets", defaultValue = "#FFFFFFFF", description = "Background texture tint color.")
    public TextWidget backgroundTextureTint(ColorView color) {
        if (color != null) backgroundTextureTint.set(color);
        return this;
    }

    public TextWidget animateBackgroundTextureTint(ColorView color, float durationSeconds) {
        animateColor(backgroundTextureTint, color, durationSeconds);
        return this;
    }

    public TextWidget animateBackgroundTextureTint(ColorView color, TransitionSpec spec) {
        animateColor(backgroundTextureTint, color, spec);
        return this;
    }

    public MutableRect backgroundTextureSource() {
        return backgroundTextureSource;
    }

    @XmlAttribute(value = "backgroundTextureSource", displayName = "Background Texture Source", category = "Assets", defaultValue = "0 0 1 1", description = "UV source rectangle.")
    public TextWidget backgroundTextureSource(MutableRect source) {
        backgroundTextureSource.set(source == null ? new MutableRect(0.0f, 0.0f, 1.0f, 1.0f) : source);
        return this;
    }

    public TextWidget backgroundTextureSource(float u, float v, float width, float height) {
        backgroundTextureSource.set(u, v, width, height);
        return this;
    }

    public ImageFit backgroundTextureFit() {
        return backgroundTextureFit;
    }

    @XmlAttribute(value = "backgroundTextureFit", displayName = "Background Texture Fit", category = "Assets", defaultValue = "stretch", description = "Placement mode for the background texture.")
    public TextWidget backgroundTextureFit(ImageFit fit) {
        ImageFit effectiveFit = fit == null ? ImageFit.STRETCH : fit;
        if (backgroundTextureFit == effectiveFit) return this;
        backgroundTextureFit = effectiveFit;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public MutableColor borderColor() {
        return borderColor;
    }

    @XmlAttribute(value = "border", category = "Appearance", defaultValue = "#FFFFFFFF", description = "Border color; setting it also enables border rendering.")
    public TextWidget border(ColorView color) {
        borderColor(color);
        borderVisible(true);
        return this;
    }

    public TextWidget border(ColorView color, float width) {
        borderColor(color);
        borderWidth(width);
        borderVisible(true);
        return this;
    }

    public TextWidget border(float r, float g, float b, float a) {
        borderColor.set(r, g, b, a);
        borderVisible(true);
        return this;
    }

    public TextWidget border(float r, float g, float b, float a, float width) {
        borderColor.set(r, g, b, a);
        borderWidth(width);
        borderVisible(true);
        return this;
    }

    @XmlAttribute(value = "borderColor", category = "Appearance", defaultValue = "#FFFFFFFF", description = "Border color used when border rendering is enabled.")
    public TextWidget borderColor(ColorView color) {
        if (color != null) borderColor.set(color);
        return this;
    }

    public TextWidget animateBorderColor(ColorView color, float durationSeconds) {
        animateColor(borderColor, color, durationSeconds);
        return this;
    }

    public TextWidget animateBorderColor(ColorView color, TransitionSpec spec) {
        animateColor(borderColor, color, spec);
        return this;
    }

    @XmlAttribute(value = "borderVisible", category = "Appearance", defaultValue = "false", description = "Whether the border is rendered.")
    public TextWidget borderVisible(boolean borderVisible) {
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

    @XmlAttribute(value = "borderWidth", category = "Appearance", defaultValue = "1", description = "Border thickness in UI pixels.")
    public TextWidget borderWidth(float borderWidth) {
        if (this.borderWidth == borderWidth) return this;
        this.borderWidth = borderWidth;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public TextWidget animateBorderWidth(float borderWidth, float durationSeconds) {
        return animateBorderWidth(borderWidth, TransitionSpec.of(durationSeconds));
    }

    public TextWidget animateBorderWidth(float borderWidth, TransitionSpec spec) {
        animateParameter("TextWidget.borderWidth", this::borderWidth, this::borderWidth, borderWidth, spec);
        return this;
    }

    public float radius() {
        return radius;
    }

    @XmlAttribute(value = "radius", category = "Appearance", defaultValue = "0", description = "Corner radius in UI pixels.")
    public TextWidget radius(float radius) {
        if (this.radius == radius) return this;
        this.radius = radius;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public TextWidget animateRadius(float radius, float durationSeconds) {
        return animateRadius(radius, TransitionSpec.of(durationSeconds));
    }

    public TextWidget animateRadius(float radius, TransitionSpec spec) {
        animateParameter("TextWidget.radius", this::radius, this::radius, radius, spec);
        return this;
    }

    protected void renderBox(RenderContext context) {
        if (!boxVisualEnabled || (!backgroundVisible && !borderVisible)) return;
        DrawScope draw = new DrawScope(context, transform(), layoutBounds());
        float x = layoutBounds().x();
        float y = layoutBounds().y();
        float width = layoutBounds().width();
        float height = layoutBounds().height();
        if (backgroundVisible) {
            if (backgroundTexture != null) {
                if (background.a() > 0.0f) {
                    draw.roundedRect(x, y, width, height, radius, Paint.fill(background));
                }
                TexturePlacement placement = TexturePlacement.fit(backgroundTexture,
                        backgroundTextureSource, layoutBounds(), backgroundTextureFit);
                if (placement != null) {
                    draw.texture(backgroundTexture, placement, radius, Paint.fill(backgroundTextureTint));
                }
            } else {
                draw.roundedRect(x, y, width, height, radius, Paint.fill(background));
            }
        }
        if (borderVisible) {
            draw.roundedRect(x, y, width, height, radius, Paint.stroke(borderColor, borderWidth));
        }
    }

    @Override
    public void measure(LayoutContext context) {
        if (visibility() == dev.sixik.unigui.api.widget.Visibility.COLLAPSED) {
            setDesiredSize(0.0f, 0.0f);
            return;
        }
        EdgeInsets padding = layoutStyle().padding();
        float contentW = measuredTextWidth(context) + padding.horizontal();
        float contentH = measuredTextHeight(context) + padding.vertical();
        setDesiredSize(resolveDesiredSize(context, contentW, contentH));
    }

    @Override
    public LayoutSize minContentSize() {
        if (visibility() == Visibility.COLLAPSED || text.isEmpty()) {
            return LayoutSize.ZERO;
        }
        EdgeInsets padding = layoutStyle().padding();
        float minWidth = TextEngine.minContentWidth(effectiveRichText()) + padding.horizontal();
        float height = measuredTextHeight(new LayoutContext(minWidth, Float.POSITIVE_INFINITY))
                + padding.vertical();
        return LayoutSize.of(minWidth, height);
    }

    @Override
    public float contentBaseline() {
        RichText text = effectiveRichText();
        if (text == null || text.isEmpty()) {
            return Float.NaN;
        }
        for (RichTextSpan span : text.spans()) {
            if (span instanceof TextRun run && run.font() != null && run.pixelSize() > 0.0f) {
                return layoutStyle().padding().top() + run.font().metrics(run.pixelSize()).ascent();
            }
        }
        return Float.NaN;
    }

    @Override
    public void render(RenderContext context) {
        if (visibility() == Visibility.COLLAPSED || visibility() == Visibility.HIDDEN) return;
        boolean hasText = !text.isEmpty();
        boolean hasBox = boxVisualEnabled && (backgroundVisible || borderVisible);
        if (!hasText && !hasBox) return;

        pushOpacity(context);
        try {
            DrawScope draw = new DrawScope(context, transform(), layoutBounds());
            if (renderCustomVisual(draw)) {
                return;
            }
            if (hasBox) {
                renderBox(context);
            }
            if (hasText) {
                renderText(draw);
            }
        } finally {
            popOpacity(context);
        }
    }

    private void renderText(DrawScope draw) {
        RenderContext context = draw.context();
        List<Segment> segments;
        boolean clipped;
        float clipX;
        float clipY;
        float clipWidth;
        float clipHeight;
        switch (overflowMode) {
            case CLIP -> {
                segments = visibleSegments(context);
                clipped = true;
                clipX = layoutBounds().x();
                clipY = layoutBounds().y();
                clipWidth = layoutBounds().width();
                clipHeight = layoutBounds().height();
            }
            case SHRINK_TO_FIT -> {
                EdgeInsets padding = layoutStyle().padding();
                float contentX = layoutBounds().x() + padding.left();
                float contentY = layoutBounds().y() + padding.top();
                float availableWidth = Math.max(0.0f, layoutBounds().width() - padding.horizontal());
                float availableHeight = Math.max(0.0f, layoutBounds().height() - padding.vertical());
                RichText drawText = effectiveRichText();
                float textWidth = TextEngine.measureLineWidth(context, drawText);
                float scale = textWidth <= 0.0f || availableWidth <= 0.0f ? 1.0f : Math.min(1.0f, availableWidth / textWidth);
                float sourceHeight = TextEngine.measureTextHeight(context, drawText);
                float textHeight = Math.min(availableHeight, sourceHeight * scale);
                float scaledTextWidth = textWidth * scale;
                float drawX = TextEngine.alignedStart(contentX, availableWidth, scaledTextWidth, textHorizontalAlignment());
                float drawY = TextEngine.alignedStart(contentY, availableHeight, textHeight, textVerticalAlignment());
                Transform scaled = scaledTransform(scale);
                segments = List.of(new Segment(drawText, drawX, drawY, textWidth, sourceHeight, scaled));
                clipped = true;
                clipX = contentX;
                clipY = contentY;
                clipWidth = availableWidth;
                clipHeight = availableHeight;
            }
            case MARQUEE_ON_HOVER -> {
                EdgeInsets padding = layoutStyle().padding();
                float contentX = layoutBounds().x() + padding.left();
                float contentY = layoutBounds().y() + padding.top();
                float availableWidth = Math.max(0.0f, layoutBounds().width() - padding.horizontal());
                float availableHeight = Math.max(0.0f, layoutBounds().height() - padding.vertical());
                RichText drawText = effectiveRichText();
                float textWidth = TextEngine.measureLineWidth(context, drawText);
                if (textWidth <= availableWidth) {
                    segments = visibleSegments(context);
                    clipped = false;
                    clipX = layoutBounds().x();
                    clipY = layoutBounds().y();
                    clipWidth = layoutBounds().width();
                    clipHeight = layoutBounds().height();
                } else {
                    float textHeight = Math.min(availableHeight, TextEngine.measureTextHeight(context, drawText));
                    float drawY = TextEngine.alignedStart(contentY, availableHeight, textHeight, textVerticalAlignment());
                    float period = Math.max(1.0f, textWidth + marqueeGap);
                    boolean activeMarquee = hovered() || marqueeActive;
                    float offset = activeMarquee ? marqueeOffset % period : 0.0f;
                    float firstX = contentX - offset;
                    if (activeMarquee) {
                        segments = List.of(
                                new Segment(drawText, firstX, drawY, textWidth, textHeight, null),
                                new Segment(drawText, firstX + textWidth + marqueeGap, drawY, textWidth, textHeight, null));
                    } else {
                        segments = List.of(new Segment(drawText, firstX, drawY, textWidth, textHeight, null));
                    }
                    clipped = true;
                    clipX = contentX;
                    clipY = contentY;
                    clipWidth = availableWidth;
                    clipHeight = availableHeight;
                }
            }
            default -> {
                segments = visibleSegments(context);
                clipped = false;
                clipX = layoutBounds().x();
                clipY = layoutBounds().y();
                clipWidth = layoutBounds().width();
                clipHeight = layoutBounds().height();
            }
        }
        if (clipped) {
            draw.pushTextClip(clipX, clipY, clipWidth, clipHeight);
        }
        try {
            for (Segment segment : segments) {
                if (segment.text() == null || segment.text().isEmpty()) continue;
                DrawScope segmentDraw = segment.transform() == null ? draw : draw.withTransform(segment.transform());
                TextEngine.drawInline(segmentDraw, segment.text(), segment.x(), segment.y(), segment.width(), segment.height(),
                        Paint.fill(color));
            }
        } finally {
            if (clipped) {
                draw.popClip();
            }
        }
    }

    @Override
    public void tick(FrameContext frame) {
        super.tick(frame);
        boolean activeMarquee = hovered() || marqueeActive;
        if (overflowMode != TextOverflowMode.MARQUEE_ON_HOVER || !activeMarquee || text.isEmpty()) {
            if (marqueeOffset != 0.0f) {
                marqueeOffset = 0.0f;
                invalidate(InvalidationFlags.VISUAL);
            }
            return;
        }

        float textWidth = intrinsicTextWidth();
        if (textWidth <= Math.max(0.0f, layoutBounds().width())) {
            if (marqueeOffset != 0.0f) {
                marqueeOffset = 0.0f;
                invalidate(InvalidationFlags.VISUAL);
            }
            return;
        }

        float deltaSeconds = frame == null || frame.deltaSeconds() <= 0.0f ? 1.0f / 60.0f : frame.deltaSeconds();
        marqueeOffset += marqueeSpeed * deltaSeconds;
        float period = Math.max(1.0f, textWidth + marqueeGap);
        if (marqueeOffset >= period) {
            marqueeOffset %= period;
        }
        invalidate(InvalidationFlags.VISUAL);
    }

    protected Alignment textVerticalAlignment() {
        return Alignment.CENTER;
    }

    protected Alignment textHorizontalAlignment() {
        Alignment alignment = layoutStyle().horizontalAlignment();
        return alignment == Alignment.STRETCH ? Alignment.START : alignment;
    }

    private List<Segment> visibleSegments(RenderContext context) {
        EdgeInsets padding = layoutStyle().padding();
        float x = layoutBounds().x() + padding.left();
        float y = layoutBounds().y() + padding.top();
        float w = Math.max(0.0f, layoutBounds().width() - padding.horizontal());
        float h = Math.max(0.0f, layoutBounds().height() - padding.vertical());
        if (wrap) {
            return wrappedSegments(context, x, y, w, h);
        }
        Segment segment = alignedSegment(context, effectiveRichText(),
                x, y, w, h,
                textHorizontalAlignment(), textVerticalAlignment(), null);
        return segment == null ? List.of() : List.of(segment);
    }

    private List<Segment> wrappedSegments(RenderContext context, float x, float y, float availableWidth, float availableHeight) {
        float effectiveW = availableWidth / effectiveScale(transform().scale().x());
        float effectiveH = availableHeight / effectiveScale(transform().scale().y());
        if (effectiveW <= 0.0f || effectiveH <= 0.0f) return List.of();

        List<RichText> lines = cachedWrappedLines(context, effectiveW);
        if (lines.isEmpty()) return List.of();

        float totalHeight = TextEngine.linesHeight(context, lines);
        float drawY = TextEngine.alignedStart(y, effectiveH, totalHeight, textVerticalAlignment());
        List<Segment> segments = new ObjectArrayList<>(lines.size());
        for (RichText line : lines) {
            float lineHeight = TextEngine.lineHeight(context, line);
            if (drawY >= y + effectiveH) break;
            Segment segment = alignedSegment(context, line,
                    x, drawY, effectiveW, lineHeight,
                    textHorizontalAlignment(), Alignment.CENTER, null);
            if (segment != null) segments.add(segment);
            drawY += lineHeight;
        }
        return segments;
    }

    private Segment alignedSegment(RenderContext context, RichText drawText,
                                             float x, float y, float width, float height,
                                             Alignment horizontal, Alignment vertical,
                                             Transform transform) {
        if (drawText == null || drawText.isEmpty()) return null;
        float availableWidth = Math.max(0.0f, width);
        float availableHeight = Math.max(0.0f, height);
        float textWidth = Math.min(availableWidth, TextEngine.measureLineWidth(context, drawText));
        float textHeight = Math.min(availableHeight, TextEngine.measureTextHeight(context, drawText));
        float drawX = TextEngine.alignedStart(x, availableWidth, textWidth, horizontal);
        float drawY = TextEngine.alignedStart(y, availableHeight, textHeight, vertical);
        return new Segment(drawText, drawX, drawY, textWidth, textHeight, transform);
    }

    private Transform scaledTransform(float scale) {
        Transform scaled = transform().copy();
        scaled.scale().set(transform().scale().x() * scale, transform().scale().y() * scale);
        return scaled;
    }

    private static String normalize(String text) {
        return text == null ? "" : text;
    }

    private float measuredTextWidth(LayoutContext context) {
        if (text.isEmpty()) return 0.0f;
        float intrinsicWidth = intrinsicTextWidth();
        if (!wrap) return intrinsicWidth;
        float availableWidth = context == null ? Float.POSITIVE_INFINITY : context.availableWidth();
        return Float.isFinite(availableWidth) && availableWidth > 0.0f
                ? Math.min(intrinsicWidth, availableWidth)
                : intrinsicWidth;
    }

    private float measuredTextHeight(LayoutContext context) {
        if (text.isEmpty()) return 0.0f;
        if (!wrap) return TextEngine.measureTextHeight(effectiveRichText());

        float availableWidth = context == null ? Float.POSITIVE_INFINITY : context.availableWidth();
        if (!Float.isFinite(availableWidth) || availableWidth <= 0.0f) {
            return TextEngine.measureTextHeight(effectiveRichText());
        }

        return TextEngine.linesHeight(TextEngine.wrapLines(effectiveRichText(), availableWidth));
    }

    private float intrinsicTextWidth() {
        return TextEngine.measureLineWidth(effectiveRichText());
    }

    private RichText effectiveRichText() {
        return richText == null ? RichText.plain(text) : richText;
    }

    private List<RichText> cachedWrappedLines(RenderContext context, float availableWidth) {
        RichText currentText = effectiveRichText();
        Object backend = context == null ? null : context.backend();
        if (Objects.equals(wrappedCacheText, currentText)
                && wrappedCacheBackend == backend
                && Float.compare(wrappedCacheWidth, availableWidth) == 0) {
            return wrappedCacheLines;
        }

        wrappedCacheText = currentText;
        wrappedCacheBackend = backend;
        wrappedCacheWidth = availableWidth;
        wrappedCacheLines = TextEngine.wrapLines(context, currentText, availableWidth);
        return wrappedCacheLines;
    }

    private void clearWrapCache() {
        wrappedCacheText = null;
        wrappedCacheBackend = null;
        wrappedCacheWidth = Float.NaN;
        wrappedCacheLines = List.of();
    }

    private float scaledAvailableWidth() {
        return Math.max(0.0f, layoutBounds().width()) / effectiveScale(transform().scale().x());
    }

    private float scaledAvailableHeight() {
        return Math.max(0.0f, layoutBounds().height()) / effectiveScale(transform().scale().y());
    }

    private static float effectiveScale(float scale) {
        return Float.isFinite(scale) && Math.abs(scale) > 0.0001f ? Math.abs(scale) : 1.0f;
    }

    record Segment(
            RichText text,
            float x,
            float y,
            float width,
            float height,
            Transform transform
    ) {
    }
}
