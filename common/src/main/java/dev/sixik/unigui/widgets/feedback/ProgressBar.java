package dev.sixik.unigui.widgets.feedback;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.core.FrameContext;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.style.StyleKeys;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.widget.skin.WidgetsRender;
import dev.sixik.unigui.api.xml.XmlAttribute;
import dev.sixik.unigui.api.xml.XmlWidgetName;
import dev.sixik.unigui.widgets.render.ProgressBarRenderer;
import dev.sixik.unigui.widgets.render.ProgressBarState;
import dev.sixik.unigui.widgets.containers.Box;
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
    private ProgressBarRenderer renderer;
    private float preferredWidth = DEFAULT_PREFERRED_WIDTH;
    private float preferredHeight = DEFAULT_PREFERRED_HEIGHT;
    private float min;
    private float max = 1.0f;
    private float value;
    private boolean indeterminate;
    private float indeterminateOffset;
    private float indeterminateSpeed = 0.85f;

    public ProgressBar() {
        backgroundVisible(false);
        borderVisible(false);
        trackColor.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
        fillColor.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
    }

    public float min() {
        return min;
    }

    @XmlAttribute(value = "min", category = "Behavior", defaultValue = "0", description = "Minimum progress value.")
    public ProgressBar min(float min) {
        return range(min, max);
    }

    public float max() {
        return max;
    }

    @XmlAttribute(value = "max", category = "Behavior", defaultValue = "1", description = "Maximum progress value.")
    public ProgressBar max(float max) {
        return range(min, max);
    }

    public ProgressBar range(float min, float max) {
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

    public float value() {
        return value;
    }

    @XmlAttribute(value = "value", category = "Behavior", defaultValue = "0", description = "Current progress value clamped to min/max.")
    public ProgressBar value(float value) {
        float normalized = clamp(value, min, max);
        if (this.value == normalized) return this;
        this.value = normalized;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public float progress() {
        float range = max - min;
        return range == 0.0f ? 0.0f : clamp((value - min) / range, 0.0f, 1.0f);
    }

    public boolean indeterminate() {
        return indeterminate;
    }

    @XmlAttribute(value = "indeterminate", category = "Behavior", defaultValue = "false", description = "Whether the progress bar shows an indeterminate animation.")
    public ProgressBar indeterminate(boolean indeterminate) {
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

    public ProgressBarRenderer renderer() {
        return renderer;
    }

    public ProgressBar renderer(ProgressBarRenderer renderer) {
        if (this.renderer == renderer) return this;
        this.renderer = renderer;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public ProgressBar useDefaultRenderer() {
        return renderer(null);
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
        ProgressBarState state = snapshot();
        DrawScope draw = new DrawScope(context, transform(), layoutBounds());
        if (renderer != null) {
            renderer.render(draw, state);
        } else {
            ProgressBarRenderer styled = styleRendererOverride(ProgressBarRenderer.class);
            if (styled != null) {
                styled.render(draw, state);
            } else if (!renderStylePlan(context, ProgressBarState.class, state)) {
                WidgetsRender.progressBar().render(draw, state);
            }
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

    private ProgressBarRenderer effectiveRenderer() {
        return renderer == null ? styleRenderer(ProgressBarRenderer.class, WidgetsRender.progressBar()) : renderer;
    }

    private ProgressBarState snapshot() {
        return new ProgressBarState(
                layoutBounds().x(),
                layoutBounds().y(),
                layoutBounds().width(),
                layoutBounds().height(),
                min,
                max,
                value,
                progress(),
                indeterminate,
                indeterminateOffset,
                trackColor.copy(),
                fillColor.copy());
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
