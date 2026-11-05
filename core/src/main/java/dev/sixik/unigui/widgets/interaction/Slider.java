package dev.sixik.unigui.widgets.interaction;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.core.UIContext;
import dev.sixik.unigui.api.docs.*;
import dev.sixik.unigui.api.event.*;
import dev.sixik.unigui.api.input.KeyCodes;
import dev.sixik.unigui.api.input.PointerButton;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.style.Style;
import dev.sixik.unigui.api.style.StyleAnimationIds;
import dev.sixik.unigui.api.style.StyleIds;
import dev.sixik.unigui.api.style.StyleKeys;
import dev.sixik.unigui.api.utils.reactive.*;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.widget.render.WidgetRole;
import dev.sixik.unigui.xaml.XamlProperty;
import dev.sixik.unigui.xaml.XamlWidget;
import dev.sixik.unigui.widgets.WidgetStyles;
import dev.sixik.unigui.widgets.containers.Box;
import dev.sixik.unigui.widgets.core.Orientation;

@AiDoc(
        summary = "Range slider control allowing continuous or stepped value adjustment with draggable thumb.",
        purpose = "Provides an interactive slider input for numerical values within min and max bounds.",
        useWhen = {
                "Adjust continuous float or stepped numerical parameters.",
                "Settings screens for audio volume, field of view, or brightness."
        },
        criticalRules = {
                "Value is automatically clamped between min and max.",
                "When step > 0, values are quantized to multiples of step."
        },
        related = {"ScrollBar", "NumberField", "SliderValueChangedEvent"})
@WikiPage(
        title = "<key:slider.title>",
        slug = "slider",
        category = "<key:core.widgets.interaction.category>",
        summary = "<key:slider.summary>",
        order = 180)
@WikiSection(
        title = "<key:slider.section.overview.title>",
        markdown = "<key:slider.section.overview.body>",
        order = 10)
@WikiExample(
        title = "<key:slider.example.basic.title>",
        code = "<key:slider.example.basic.code>",
        order = 10)
@XamlWidget("Slider")
public class Slider extends Box {
    public static final String STYLE_TYPE = StyleIds.Widget.SLIDER;

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
    public static final float DEFAULT_PREFERRED_HEIGHT = 18.0f;

    private static final float DEFAULT_TRACK_THICKNESS = 0.0f;
    private static final float DEFAULT_KNOB_WIDTH = 8.0f;

    private final MutableColor trackColor = new MutableColor(0.25f, 0.25f, 0.25f, 1.0f);
    private final MutableColor fillColor = new MutableColor(0.25f, 0.78f, 1.0f, 1.0f);
    private final MutableColor knobColor = new MutableColor(0.95f, 0.95f, 0.95f, 1.0f);
    private float preferredWidth = DEFAULT_PREFERRED_WIDTH;
    private float preferredHeight = DEFAULT_PREFERRED_HEIGHT;
    private float min;
    private float max = 1.0f;
    private float value;
    private float step;
    private boolean dragging;
    private Orientation orientation = Orientation.HORIZONTAL;

    public Slider() {
        boxVisualEnabled(false);
        applyComponentStyleDefaults(WidgetStyles.RangeControls.SLIDER);
        focusable(true);
        trackColor.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
        fillColor.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
        knobColor.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
    }

    /**
     * Возвращает стандартный стиль ползунка, самостоятельно рисующего дорожку и бегунок.
     *
     * @return стандартные значения без фоновой поверхности и рамки
     */
    @Override
    protected Style defaultStyle() {
        return WidgetStyles.RangeControls.SLIDER;
    }

    @WikiMethod(
            title = "<key:slider.method.min.title>",
            summary = "<key:slider.method.min.summary>")
    public float min() {
        return min;
    }

    @XamlProperty(value = "min", category = "Behavior", defaultValue = "0", description = "Minimum slider value.")
    public Slider min(float min) {
        return range(min, max);
    }

    @WikiMethod(
            title = "<key:slider.method.max.title>",
            summary = "<key:slider.method.max.summary>")
    public float max() {
        return max;
    }

    @XamlProperty(value = "max", category = "Behavior", defaultValue = "1", description = "Maximum slider value.")
    public Slider max(float max) {
        return range(min, max);
    }

    @WikiMethod(
            title = "<key:slider.method.range.title>",
            summary = "<key:slider.method.range.summary>")
    public Slider range(@WikiParameter(description = "<key:slider.param.min>") float min, @WikiParameter(description = "<key:slider.param.max>") float max) {
        if (max < min) {
            float swap = min;
            min = max;
            max = swap;
        }
        if (this.min == min && this.max == max) return this;
        this.min = min;
        this.max = max;
        value(value);
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    @WikiMethod(
            title = "<key:slider.method.get_value.title>",
            summary = "<key:slider.method.get_value.summary>")
    public float value() {
        return value;
    }

    @XamlProperty(value = "value", category = "Behavior", defaultValue = "0", description = "Current slider value clamped to min/max.")
    public Slider value(float value) {
        setValue(value, true);
        return this;
    }

    @WikiMethod(
            title = "<key:slider.method.get_step.title>",
            summary = "<key:slider.method.get_step.summary>")
    public float step() {
        return step;
    }

    @XamlProperty(value = "step", category = "Behavior", defaultValue = "0", description = "Snap increment; 0 disables snapping.")
    public Slider step(float step) {
        this.step = Math.max(0.0f, step);
        return this;
    }

    @WikiMethod(
            title = "<key:slider.method.dragging.title>",
            summary = "<key:slider.method.dragging.summary>")
    public boolean dragging() {
        return dragging;
    }

    /**
     * @return ось слайдера: горизонтальная по умолчанию
     */
    public Orientation orientation() {
        return orientation;
    }

    /**
     * Задаёт ось слайдера. Вертикальный слайдер растёт снизу вверх: низ равен {@code min}, верх {@code max};
     * клавиши Up и Down меняют значение так же, как Right и Left. Предпочтительные размеры, которые не меняли
     * вручную, меняются местами, чтобы вертикальный слайдер был высоким, а не широким.
     *
     * @param orientation ось или {@code null} (трактуется как горизонтальная)
     * @return этот слайдер
     */
    @XamlProperty(value = "orientation", category = "Layout", defaultValue = "horizontal", description = "Slider axis; vertical sliders grow from bottom to top.")
    public Slider orientation(Orientation orientation) {
        Orientation normalized = orientation == null ? Orientation.HORIZONTAL : orientation;
        if (this.orientation == normalized) return this;
        boolean wasVertical = this.orientation == Orientation.VERTICAL;
        boolean untouched = preferredWidth == (wasVertical ? DEFAULT_PREFERRED_HEIGHT : DEFAULT_PREFERRED_WIDTH)
                && preferredHeight == (wasVertical ? DEFAULT_PREFERRED_WIDTH : DEFAULT_PREFERRED_HEIGHT);
        this.orientation = normalized;
        if (untouched) {
            boolean vertical = normalized == Orientation.VERTICAL;
            preferredWidth = vertical ? DEFAULT_PREFERRED_HEIGHT : DEFAULT_PREFERRED_WIDTH;
            preferredHeight = vertical ? DEFAULT_PREFERRED_WIDTH : DEFAULT_PREFERRED_HEIGHT;
        }
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    @WikiMethod(
            title = "<key:slider.method.track_color.title>",
            summary = "<key:slider.method.track_color.summary>")
    public MutableColor trackColor() {
        return trackColor;
    }

    @WikiMethod(
            title = "<key:slider.method.fill_color.title>",
            summary = "<key:slider.method.fill_color.summary>")
    public MutableColor fillColor() {
        return fillColor;
    }

    @WikiMethod(
            title = "<key:slider.method.knob_color.title>",
            summary = "<key:slider.method.knob_color.summary>")
    public MutableColor knobColor() {
        return knobColor;
    }

    @WikiMethod(
            title = "<key:slider.method.preferred_width.title>",
            summary = "<key:slider.method.preferred_width.summary>")
    public float preferredWidth() {
        return preferredWidth;
    }

    @XamlProperty(value = "preferredWidth", category = "Layout", defaultValue = "120", description = "Intrinsic slider width before layout constraints are applied.")
    @WikiMethod(
            title = "<key:slider.method.set_preferred_width.title>",
            summary = "<key:slider.method.set_preferred_width.summary>")
    public Slider preferredWidth(float preferredWidth) {
        float normalized = positiveOr(preferredWidth, DEFAULT_PREFERRED_WIDTH);
        if (this.preferredWidth == normalized) return this;
        this.preferredWidth = normalized;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    @WikiMethod(
            title = "<key:slider.method.preferred_height.title>",
            summary = "<key:slider.method.preferred_height.summary>")
    public float preferredHeight() {
        return preferredHeight;
    }

    @XamlProperty(value = "preferredHeight", category = "Layout", defaultValue = "18", description = "Intrinsic slider height before layout constraints are applied.")
    @WikiMethod(
            title = "<key:slider.method.set_preferred_height.title>",
            summary = "<key:slider.method.set_preferred_height.summary>")
    public Slider preferredHeight(float preferredHeight) {
        float normalized = positiveOr(preferredHeight, DEFAULT_PREFERRED_HEIGHT);
        if (this.preferredHeight == normalized) return this;
        this.preferredHeight = normalized;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public Slider preferredSize(float width, float height) {
        return preferredWidth(width).preferredHeight(height);
    }

    @WikiMethod(
            title = "<key:slider.method.on_value_changed.title>",
            summary = "<key:slider.method.on_value_changed.summary>")
    public EventSubscription onValueChanged(EventListener<? super SliderValueChangedEvent> listener) {
        return on(SliderValueChangedEvent.TYPE, listener);
    }

    @Override
    protected <T> SignalSubscription createBinding(BindingData<T> key, T signal) {
        if (key == BindingKeys.SLIDER_VALUE) {
            FloatSignal value = BindingKeys.SLIDER_VALUE.cast(signal);
            SignalSubscription signalSubscription = value.subscribe(this::value);
            EventSubscription widgetSubscription = onValueChanged(event -> value.set(event.newValue()));
            return () -> {
                widgetSubscription.close();
                signalSubscription.close();
            };
        }
        return super.createBinding(key, signal);
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
        knobColor.set(styleValue(StyleKeys.THUMB_COLOR, knobColor));
    }

    @Override
    public void handle(Event event) {
        if (visibility() != Visibility.VISIBLE || !enabled()) return;
        super.handle(event);
        if (event.isCancelled()) return;
        if (event instanceof PointerEvent pointerEvent && pointerEvent.phase() == EventPhase.CAPTURE) return;

        if (event instanceof PointerPressedEvent pointer && pointer.button() == PointerButton.PRIMARY) {
            UIContext context = uiContext();
            if (context != null) {
                context.focusManager().requestFocus(this);
                context.capturePointer(pointer.pointerId(), this);
            }
            dragging = true;
            updateFromPointer(pointer.rootX(), pointer.rootY());
            event.cancel();
        } else if (event instanceof PointerMovedEvent pointer && dragging) {
            updateFromPointer(pointer.rootX(), pointer.rootY());
            event.cancel();
        } else if (event instanceof PointerReleasedEvent pointer && pointer.button() == PointerButton.PRIMARY && dragging) {
            updateFromPointer(pointer.rootX(), pointer.rootY());
            dragging = false;
            UIContext context = uiContext();
            if (context != null) {
                context.releasePointer(pointer.pointerId(), this);
            }
            event.cancel();
        } else if (event instanceof KeyPressedEvent key && key.phase() == EventPhase.TARGET && isFocused()) {
            if (key.keyCode() == KeyCodes.LEFT) {
                nudge(-1.0f);
                event.cancel();
            } else if (key.keyCode() == KeyCodes.RIGHT) {
                nudge(1.0f);
                event.cancel();
            } else if (orientation == Orientation.VERTICAL && key.keyCode() == KeyCodes.UP) {
                nudge(1.0f);
                event.cancel();
            } else if (orientation == Orientation.VERTICAL && key.keyCode() == KeyCodes.DOWN) {
                nudge(-1.0f);
                event.cancel();
            }
        }
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
        float width = layoutBounds().width();
        float height = Math.max(1.0f, layoutBounds().height());
        if (orientation == Orientation.VERTICAL) {
            renderVertical(draw, x, y, Math.max(1.0f, width), height);
            super.renderContent(context);
            return;
        }
        float configuredTrackThickness = styleValue(StyleKeys.TRACK_THICKNESS, DEFAULT_TRACK_THICKNESS);
        float trackHeight = configuredTrackThickness > 0.0f
                ? Math.max(1.0f, Math.min(configuredTrackThickness, height))
                : Math.max(2.0f, Math.min(4.0f, height * 0.25f));
        float knobWidth = Math.max(1.0f, styleValue(StyleKeys.THUMB_THICKNESS, DEFAULT_KNOB_WIDTH));
        float knobRadius = Math.max(0.0f, styleValue(StyleKeys.THUMB_RADIUS, 2.0f));
        float trackY = y + (height - trackHeight) * 0.5f;
        float fillWidth = width * normalizedValue();
        float knobX = x + fillWidth - knobWidth * 0.5f;
        draw.roundedRectFilled(x, trackY, width, trackHeight, trackHeight * 0.5f,
                trackColor);
        draw.roundedRectFilled(x, trackY, fillWidth, trackHeight, trackHeight * 0.5f,
                fillColor);
        draw.roundedRectFilled(knobX, y + 2.0f, knobWidth, Math.max(1.0f, height - 4.0f), knobRadius,
                knobColor);
        super.renderContent(context);
    }

    /**
     * Рисует вертикальный слайдер: дорожка по центру, заливка растёт снизу, бегунок поперёк дорожки.
     *
     * <pre>
     *   fillHeight = height * normalizedValue
     *   knobY      = y + height - fillHeight - knobThickness / 2
     * </pre>
     */
    private void renderVertical(DrawScope draw, float x, float y, float width, float height) {
        float configured = styleValue(StyleKeys.TRACK_THICKNESS, DEFAULT_TRACK_THICKNESS);
        float trackWidth = configured > 0.0f
                ? Math.max(1.0f, Math.min(configured, width))
                : Math.max(2.0f, Math.min(4.0f, width * 0.25f));
        float knobThickness = Math.max(1.0f, styleValue(StyleKeys.THUMB_THICKNESS, DEFAULT_KNOB_WIDTH));
        float knobRadius = Math.max(0.0f, styleValue(StyleKeys.THUMB_RADIUS, 2.0f));
        float trackX = x + (width - trackWidth) * 0.5f;
        float fillHeight = height * normalizedValue();
        draw.roundedRectFilled(trackX, y, trackWidth, height, trackWidth * 0.5f, trackColor);
        draw.roundedRectFilled(trackX, y + height - fillHeight, trackWidth, fillHeight, trackWidth * 0.5f, fillColor);
        draw.roundedRectFilled(x + 2.0f, y + height - fillHeight - knobThickness * 0.5f,
                Math.max(1.0f, width - 4.0f), knobThickness, knobRadius, knobColor);
    }

    @Override
    protected WidgetRole renderRole() {
        return WidgetRole.SLIDER;
    }

    private boolean isFocused() {
        UIContext context = uiContext();
        return context != null && context.focusManager().isFocused(this);
    }

    private void updateFromLocalX(float localX) {
        float width = Math.max(1.0f, layoutBounds().width());
        float normalized = clamp(localX / width, 0.0f, 1.0f);
        setValue(min + normalized * (max - min), true);
    }

    private void updateFromRootX(float rootX) {
        updateFromLocalX(rootX - layoutBounds().x());
    }

    /**
     * Вертикальный слайдер: нижний край равен {@code min}, верхний {@code max}.
     *
     * <pre>
     *   normalized = 1 - localY / height
     * </pre>
     */
    private void updateFromRootY(float rootY) {
        float height = Math.max(1.0f, layoutBounds().height());
        float normalized = clamp(1.0f - (rootY - layoutBounds().y()) / height, 0.0f, 1.0f);
        setValue(min + normalized * (max - min), true);
    }

    private void updateFromPointer(float rootX, float rootY) {
        if (orientation == Orientation.VERTICAL) updateFromRootY(rootY);
        else updateFromRootX(rootX);
    }

    private void nudge(float direction) {
        float increment = step > 0.0f ? step : (max - min) / 20.0f;
        setValue(value + increment * direction, true);
    }

    private float normalizedValue() {
        float range = max - min;
        if (range == 0.0f) return 0.0f;
        return clamp((value - min) / range, 0.0f, 1.0f);
    }

    private void setValue(float value, boolean emitChange) {
        float normalized = normalizeValue(value);
        if (this.value == normalized) return;
        float oldValue = this.value;
        this.value = normalized;
        invalidate(InvalidationFlags.VISUAL);
        if (emitChange) {
            emit(new SliderValueChangedEvent(this, oldValue, this.value));
        }
    }

    private float normalizeValue(float value) {
        float clamped = clamp(value, min, max);
        if (step > 0.0f) {
            clamped = min + Math.round((clamped - min) / step) * step;
        }
        return clamp(clamped, min, max);
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static float positiveOr(float value, float fallback) {
        return Float.isFinite(value) && value > 0.0f ? value : fallback;
    }
}