package dev.sixik.unigui.widgets.display;

import dev.sixik.unigui.api.core.FrameContext;
import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.layout.Alignment;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.math.Transform;
import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.text.FontFace;
import dev.sixik.unigui.api.text.RichText;
import dev.sixik.unigui.api.text.TextBrush;
import dev.sixik.unigui.api.text.TextOverflowMode;
import dev.sixik.unigui.api.widget.skin.WidgetsRender;
import dev.sixik.unigui.api.xml.XmlAttribute;
import dev.sixik.unigui.api.xml.XmlWidgetName;
import dev.sixik.unigui.impl.text.TextEngine;
import dev.sixik.unigui.impl.widget.WidgetBase;
import dev.sixik.unigui.widgets.render.TextWidgetRenderer;
import dev.sixik.unigui.widgets.render.TextWidgetSegment;
import dev.sixik.unigui.widgets.render.TextWidgetState;

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
    private TextWidgetRenderer renderer;
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

    public TextWidget() {
        color.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
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

    public MutableColor color() {
        return color;
    }

    @XmlAttribute(value = "color", category = "Appearance", defaultValue = "#FFFFFFFF", description = "Text color parsed from XML color syntax.")
    public TextWidget color(ColorView color) {
        if (color != null) this.color.set(color);
        return this;
    }

    public TextWidgetRenderer renderer() {
        return renderer;
    }

    public TextWidget renderer(TextWidgetRenderer renderer) {
        if (this.renderer == renderer) return this;
        this.renderer = renderer;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public TextWidget useDefaultRenderer() {
        return renderer(null);
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

    @Override
    public void measure(LayoutContext context) {
        if (visibility() == dev.sixik.unigui.api.widget.Visibility.COLLAPSED) {
            setDesiredSize(0.0f, 0.0f);
            return;
        }
        setDesiredSize(resolveDesiredSize(context, measuredTextWidth(context), measuredTextHeight(context)));
    }

    @Override
    public void render(RenderContext context) {
        if (text.isEmpty()) return;
        pushOpacity(context);
        try {
            effectiveRenderer().render(new DrawScope(context, transform(), layoutBounds()), snapshot(context));
        } finally {
            popOpacity(context);
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

    protected TextWidgetRenderer effectiveRenderer() {
        return renderer == null ? styleRenderer(TextWidgetRenderer.class, WidgetsRender.textWidget()) : renderer;
    }

    protected TextWidgetState snapshot(RenderContext context) {
        return switch (overflowMode) {
            case CLIP -> clippedState(context);
            case SHRINK_TO_FIT -> shrinkToFitState(context);
            case MARQUEE_ON_HOVER -> marqueeState(context);
            case VISIBLE -> visibleState(context);
        };
    }

    private TextWidgetState visibleState(RenderContext context) {
        return textState(visibleSegments(context), false,
                layoutBounds().x(), layoutBounds().y(), layoutBounds().width(), layoutBounds().height());
    }

    private TextWidgetState clippedState(RenderContext context) {
        return textState(visibleSegments(context), true,
                layoutBounds().x(), layoutBounds().y(), layoutBounds().width(), layoutBounds().height());
    }

    private List<TextWidgetSegment> visibleSegments(RenderContext context) {
        if (wrap) {
            return wrappedSegments(context);
        }
        TextWidgetSegment segment = alignedSegment(context, effectiveRichText(),
                layoutBounds().x(), layoutBounds().y(), layoutBounds().width(), layoutBounds().height(),
                textHorizontalAlignment(), textVerticalAlignment(), null);
        return segment == null ? List.of() : List.of(segment);
    }

    private List<TextWidgetSegment> wrappedSegments(RenderContext context) {
        float availableWidth = scaledAvailableWidth();
        float availableHeight = scaledAvailableHeight();
        if (availableWidth <= 0.0f || availableHeight <= 0.0f) return List.of();

        List<RichText> lines = cachedWrappedLines(context, availableWidth);
        if (lines.isEmpty()) return List.of();

        float totalHeight = TextEngine.linesHeight(context, lines);
        float drawY = TextEngine.alignedStart(layoutBounds().y(), availableHeight, totalHeight, textVerticalAlignment());
        List<TextWidgetSegment> segments = new ObjectArrayList<>(lines.size());
        for (RichText line : lines) {
            float lineHeight = TextEngine.lineHeight(context, line);
            if (drawY >= layoutBounds().y() + availableHeight) break;
            TextWidgetSegment segment = alignedSegment(context, line,
                    layoutBounds().x(), drawY, availableWidth, lineHeight,
                    textHorizontalAlignment(), Alignment.CENTER, null);
            if (segment != null) segments.add(segment);
            drawY += lineHeight;
        }
        return segments;
    }

    private TextWidgetState shrinkToFitState(RenderContext context) {
        float availableWidth = Math.max(0.0f, layoutBounds().width());
        float availableHeight = Math.max(0.0f, layoutBounds().height());
        RichText drawText = effectiveRichText();
        float textWidth = TextEngine.measureLineWidth(context, drawText);
        float scale = textWidth <= 0.0f || availableWidth <= 0.0f ? 1.0f : Math.min(1.0f, availableWidth / textWidth);
        float sourceHeight = TextEngine.measureTextHeight(context, drawText);
        float textHeight = Math.min(availableHeight, sourceHeight * scale);
        float scaledTextWidth = textWidth * scale;
        float drawX = TextEngine.alignedStart(layoutBounds().x(), availableWidth, scaledTextWidth, textHorizontalAlignment());
        float drawY = TextEngine.alignedStart(layoutBounds().y(), availableHeight, textHeight, textVerticalAlignment());
        Transform scaled = scaledTransform(scale);
        TextWidgetSegment segment = new TextWidgetSegment(drawText, drawX, drawY,
                textWidth, sourceHeight, scaled);
        return textState(List.of(segment), true,
                layoutBounds().x(), layoutBounds().y(), availableWidth, availableHeight);
    }

    private TextWidgetState marqueeState(RenderContext context) {
        float availableWidth = Math.max(0.0f, layoutBounds().width());
        float availableHeight = Math.max(0.0f, layoutBounds().height());
        RichText drawText = effectiveRichText();
        float textWidth = TextEngine.measureLineWidth(context, drawText);
        if (textWidth <= availableWidth) {
            return visibleState(context);
        }

        float textHeight = Math.min(availableHeight, TextEngine.measureTextHeight(context, drawText));
        float drawY = TextEngine.alignedStart(layoutBounds().y(), availableHeight, textHeight, textVerticalAlignment());
        float period = Math.max(1.0f, textWidth + marqueeGap);
        boolean activeMarquee = hovered() || marqueeActive;
        float offset = activeMarquee ? marqueeOffset % period : 0.0f;
        float firstX = layoutBounds().x() - offset;

        List<TextWidgetSegment> segments;
        if (activeMarquee) {
            segments = List.of(
                    new TextWidgetSegment(drawText, firstX, drawY, textWidth, textHeight, null),
                    new TextWidgetSegment(drawText, firstX + textWidth + marqueeGap, drawY, textWidth, textHeight, null));
        } else {
            segments = List.of(new TextWidgetSegment(drawText, firstX, drawY, textWidth, textHeight, null));
        }
        return textState(segments, true,
                layoutBounds().x(), layoutBounds().y(), availableWidth, availableHeight);
    }

    private TextWidgetSegment alignedSegment(RenderContext context, RichText drawText,
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
        return new TextWidgetSegment(drawText, drawX, drawY, textWidth, textHeight, transform);
    }

    private TextWidgetState textState(List<TextWidgetSegment> segments, boolean clipped,
                                      float clipX, float clipY, float clipWidth, float clipHeight) {
        return new TextWidgetState(
                layoutBounds().x(),
                layoutBounds().y(),
                layoutBounds().width(),
                layoutBounds().height(),
                effectiveRichText(),
                color.copy(),
                wrap,
                overflowMode,
                hovered(),
                textVerticalAlignment(),
                clipped,
                clipX,
                clipY,
                clipWidth,
                clipHeight,
                segments);
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
}
