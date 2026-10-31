package dev.sixik.unigui.widgets.feedback;


import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiLink;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;
import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.core.FrameContext;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.style.StyleKeys;
import dev.sixik.unigui.api.style.Style;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.xml.XmlAttribute;
import dev.sixik.unigui.api.xml.XmlWidgetName;
import dev.sixik.unigui.widgets.containers.Box;
import dev.sixik.unigui.widgets.WidgetStyles;
import dev.sixik.unigui.api.style.StyleAnimationIds;
import dev.sixik.unigui.api.style.StyleIds;

/**
 * Progress widget for operations with a measurable range.
 *
 * <p>Use {@link #indeterminate(boolean)} when the operation is still progress-like,
 * but the current value is temporarily unknown. For a generic "busy" activity
 * indicator that is not tied to a range/value contract, use {@link LoadingIndicator}
 * with {@link LoadingIndicator.Mode#BAR} instead.</p>
 */
@AiDoc(summary = "Progress bar widget visualizing task progress across a numeric range with support for indeterminate mode and animations.")
@WikiPage(
        title = "ProgressBar",
        slug = "progress-bar",
        category = "<key:feedback.category>",
        summary = "<key:progress.bar.summary>"
)
@WikiSection(
        title = "<key:wiki.section.overview.title>",
        markdown = "<key:progress.bar.section.overview.body>",
        order = 10
)
@WikiExample(
        title = "<key:progress.bar.example.basic.title>",
        code = "ProgressBar bar = new ProgressBar();\nbar.range(0.0f, 100.0f).value(45.0f);"
)
@WikiLink(
        label = "LoadingIndicator",
        target = "page:loading-indicator",
        description = "<key:loading.indicator.summary>"
)
@XmlWidgetName("ProgressBar")
public class ProgressBar extends Box {
    public static final String STYLE_TYPE = StyleIds.Widget.PROGRESS_BAR;

    public static final class StyleProperties {
        public static final String TRACK_COLOR = StyleIds.Key.TRACK_COLOR;
        public static final String ACCENT_COLOR = StyleIds.Key.ACCENT_COLOR;
        public static final String THUMB_COLOR = StyleIds.Key.THUMB_COLOR;

        private StyleProperties() {
        }
    }

    public static final class AnimationProperties {
        public static final String VALUE = StyleAnimationIds.Property.VALUE;
        public static final String PROGRESS = StyleAnimationIds.Property.PROGRESS;
        public static final String TRACK_COLOR = StyleAnimationIds.Property.TRACK_COLOR;
        public static final String ACCENT_COLOR = StyleAnimationIds.Property.ACCENT_COLOR;
        public static final String THUMB_COLOR = StyleAnimationIds.Property.THUMB_COLOR;
        public static final String OPACITY = StyleAnimationIds.Property.OPACITY;
        public static final String SCALE = StyleAnimationIds.Property.SCALE;
        public static final java.util.List<String> ALL = StyleAnimationIds.Property.VALUE_CONTROL;

        private AnimationProperties() {
        }
    }

    public static final class AnimationEvents {
        public static final String ON_VALUE_CHANGED = StyleAnimationIds.Event.ON_VALUE_CHANGED;
        public static final String ON_FOCUS = StyleAnimationIds.Event.ON_FOCUS;
        public static final String ON_BLUR = StyleAnimationIds.Event.ON_BLUR;
        public static final String ON_HOVER = StyleAnimationIds.Event.ON_HOVER;
        public static final String ON_PRESS = StyleAnimationIds.Event.ON_PRESS;
        public static final String ON_RELEASE = StyleAnimationIds.Event.ON_RELEASE;
        public static final java.util.List<String> ALL = StyleAnimationIds.Event.VALUE_CONTROL;

        private AnimationEvents() {
        }
    }

    public static final float DEFAULT_PREFERRED_WIDTH = 120.0f;
    public static final float DEFAULT_PREFERRED_HEIGHT = 12.0f;

    private final MutableColor trackColor = new MutableColor(0.16f, 0.16f, 0.16f, 1.0f);
    private final MutableColor fillColor = new MutableColor(0.25f, 0.78f, 1.0f, 1.0f);
    private float preferredWidth = DEFAULT_PREFERRED_WIDTH;
    private float preferredHeight = DEFAULT_PREFERRED_HEIGHT;
    private float min;
    private float max = 1.0f;
    private float value;
    private boolean indeterminate;
    private float indeterminateOffset;
    private float indeterminateSpeed = 0.85f;

        @AiDoc(summary = "Constructs a ProgressBar with default [0, 100] range and styling.")
    @WikiMethod(summary = "<key:progress.bar.method.ctorDefault>")
    public ProgressBar() {
        boxVisualEnabled(false);
        applyComponentStyleDefaults(WidgetStyles.Surface.UNSURFACED);
        trackColor.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
        fillColor.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
    }

    /**
     * Возвращает стандартный стиль для полосы прогресса, самостоятельно рисующей дорожку и заполнение.
     *
     * @return стандартные значения без фоновой поверхности и рамки
     */
    @Override
    protected Style defaultStyle() {
        return WidgetStyles.Surface.UNSURFACED;
    }

        @AiDoc(summary = "Returns the minimum progress value.")
    @WikiMethod(summary = "<key:progress.bar.method.minGet>")
    @WikiReturn("<key:progress.bar.return.min>")
    public float min() {
        return min;
    }

    @XmlAttribute(value = "min", category = "Behavior", defaultValue = "0", description = "Minimum progress value.")
        @AiDoc(summary = "Sets the minimum progress value.")
    @WikiMethod(summary = "<key:progress.bar.method.minSet>")
    @WikiReturn("<key:progress.bar.return.this>")
    public ProgressBar min(
            @WikiParameter(description = "<key:progress.bar.param.min>")
            float min
    ) {
        return range(min, max);
    }

        @AiDoc(summary = "Returns the maximum progress value.")
    @WikiMethod(summary = "<key:progress.bar.method.maxGet>")
    @WikiReturn("<key:progress.bar.return.max>")
    public float max() {
        return max;
    }

    @XmlAttribute(value = "max", category = "Behavior", defaultValue = "1", description = "Maximum progress value.")
        @AiDoc(summary = "Sets the maximum progress value.")
    @WikiMethod(summary = "<key:progress.bar.method.maxSet>")
    @WikiReturn("<key:progress.bar.return.this>")
    public ProgressBar max(
            @WikiParameter(description = "<key:progress.bar.param.max>")
            float max
    ) {
        return range(min, max);
    }

        @AiDoc(summary = "Sets both minimum and maximum bounds simultaneously.")
    @WikiMethod(summary = "<key:progress.bar.method.range>")
    @WikiReturn("<key:progress.bar.return.this>")
    public ProgressBar range(
            @WikiParameter(description = "<key:progress.bar.param.min>")
            float min,
            @WikiParameter(description = "<key:progress.bar.param.max>")
            float max
    ) {
        if (max < min) {
            float swap = min;
            min = max;
            max = swap;
        }
        this.min = min;
        this.max = max;
        value(value);
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

        @AiDoc(summary = "Returns the current numeric progress value.")
    @WikiMethod(summary = "<key:progress.bar.method.valueGet>")
    @WikiReturn("<key:progress.bar.return.value>")
    public float value() {
        return value;
    }

    @XmlAttribute(value = "value", category = "Behavior", defaultValue = "0", description = "Current progress value clamped to min/max.")
        @AiDoc(summary = "Sets the current numeric progress value, clamped to [min, max].")
    @WikiMethod(summary = "<key:progress.bar.method.valueSet>")
    @WikiReturn("<key:progress.bar.return.this>")
    public ProgressBar value(
            @WikiParameter(description = "<key:progress.bar.param.value>")
            float value
    ) {
        float normalized = clamp(value, min, max);
        if (this.value == normalized) return this;
        this.value = normalized;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

        @AiDoc(summary = "Returns the normalized progress ratio between 0.0 and 1.0.")
    @WikiMethod(summary = "<key:progress.bar.method.progressGet>")
    @WikiReturn("<key:progress.bar.return.progress>")
    public float progress() {
        float range = max - min;
        return range == 0.0f ? 0.0f : clamp((value - min) / range, 0.0f, 1.0f);
    }

        @AiDoc(summary = "Returns true if indeterminate animation mode is enabled.")
    @WikiMethod(summary = "<key:progress.bar.method.indeterminateGet>")
    @WikiReturn("<key:progress.bar.return.indeterminate>")
    public boolean indeterminate() {
        return indeterminate;
    }

    @XmlAttribute(value = "indeterminate", category = "Behavior", defaultValue = "false", description = "Whether the progress bar shows an indeterminate animation.")
        @AiDoc(summary = "Enables or disables indeterminate continuous activity animation.")
    @WikiMethod(summary = "<key:progress.bar.method.indeterminateSet>")
    @WikiReturn("<key:progress.bar.return.this>")
    public ProgressBar indeterminate(
            @WikiParameter(description = "<key:progress.bar.param.indeterminate>")
            boolean indeterminate
    ) {
        if (this.indeterminate == indeterminate) return this;
        this.indeterminate = indeterminate;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public float indeterminateOffset() {
        return indeterminateOffset;
    }

    public float indeterminateSpeed() {
        return indeterminateSpeed;
    }

    @XmlAttribute(value = "indeterminateSpeed", category = "Behavior", defaultValue = "0.85", description = "Indeterminate animation speed multiplier.")
    public ProgressBar indeterminateSpeed(float indeterminateSpeed) {
        float normalized = Float.isFinite(indeterminateSpeed) ? Math.max(0.0f, indeterminateSpeed) : 0.85f;
        if (this.indeterminateSpeed == normalized) return this;
        this.indeterminateSpeed = normalized;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public MutableColor trackColor() {
        return trackColor;
    }

    public MutableColor fillColor() {
        return fillColor;
    }

    public float preferredWidth() {
        return preferredWidth;
    }

    @XmlAttribute(value = "preferredWidth", category = "Layout", defaultValue = "120", description = "Intrinsic progress bar width before layout constraints are applied.")
    public ProgressBar preferredWidth(float preferredWidth) {
        float normalized = positiveOr(preferredWidth, DEFAULT_PREFERRED_WIDTH);
        if (this.preferredWidth == normalized) return this;
        this.preferredWidth = normalized;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public float preferredHeight() {
        return preferredHeight;
    }

    @XmlAttribute(value = "preferredHeight", category = "Layout", defaultValue = "12", description = "Intrinsic progress bar height before layout constraints are applied.")
    public ProgressBar preferredHeight(float preferredHeight) {
        float normalized = positiveOr(preferredHeight, DEFAULT_PREFERRED_HEIGHT);
        if (this.preferredHeight == normalized) return this;
        this.preferredHeight = normalized;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public ProgressBar preferredSize(float width, float height) {
        return preferredWidth(width).preferredHeight(height);
    }

    @Override
    public void measure(LayoutContext context) {
        if (visibility() == Visibility.COLLAPSED) {
            setDesiredSize(0.0f, 0.0f);
            return;
        }
        setDesiredSize(resolveDesiredSize(context, preferredWidth, preferredHeight));
    }

    @Override
    protected void applyTheme() {
        super.applyTheme();
        trackColor.set(styleValue(StyleKeys.TRACK_COLOR, trackColor));
        fillColor.set(styleValue(StyleKeys.ACCENT_COLOR, fillColor));
    }

    @Override
    protected void renderContent(RenderContext context) {
        applyTheme();
        DrawScope draw = new DrawScope(context, transform(), layoutBounds());
        if (renderCustomVisual(draw)) {
            super.renderContent(context);
            return;
        }
        float x = layoutBounds().x();
        float y = layoutBounds().y();
        float width = Math.max(0.0f, layoutBounds().width());
        float height = layoutBounds().height();
        float progress = progress();
        float fillWidth = Math.max(0.0f, Math.min(width, width * progress));
        draw.rect(x, y, width, height, Paint.fill(trackColor));
        if (indeterminate) {
            float segmentWidth = Math.max(8.0f, width * 0.32f);
            float travel = width + segmentWidth;
            float offset = indeterminateOffset - (float) Math.floor(indeterminateOffset);
            float segmentX = x + offset * travel - segmentWidth;
            draw.pushClip(x, y, width, height);
            try {
                draw.rect(segmentX, y, segmentWidth, height, Paint.fill(fillColor));
            } finally {
                draw.popClip();
            }
        } else if (fillWidth > 0.0f) {
            draw.rect(x, y, fillWidth, height, Paint.fill(fillColor));
        }
        super.renderContent(context);
    }

    @Override
    public void tick(FrameContext frame) {
        super.tick(frame);
        if (!indeterminate || frame == null) return;
        float delta = Float.isFinite(frame.deltaSeconds()) ? Math.max(0.0f, frame.deltaSeconds()) : 0.0f;
        indeterminateOffset = wrap01(indeterminateOffset + delta * indeterminateSpeed);
        invalidate(InvalidationFlags.VISUAL);
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static float positiveOr(float value, float fallback) {
        return Float.isFinite(value) && value > 0.0f ? value : fallback;
    }

    private static float wrap01(float value) {
        if (!Float.isFinite(value)) return 0.0f;
        return value - (float) Math.floor(value);
    }
}
