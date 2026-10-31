package dev.sixik.unigui.widgets.interaction;

import dev.sixik.unigui.api.animation.AnimationEasing;
import dev.sixik.unigui.api.animation.TransitionSpec;
import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.core.UIContext;
import dev.sixik.unigui.api.event.Event;
import dev.sixik.unigui.api.event.EventListener;
import dev.sixik.unigui.api.event.EventPhase;
import dev.sixik.unigui.api.event.EventSubscription;
import dev.sixik.unigui.api.event.KeyPressedEvent;
import dev.sixik.unigui.api.event.PointerEvent;
import dev.sixik.unigui.api.event.PointerMovedEvent;
import dev.sixik.unigui.api.event.PointerPressedEvent;
import dev.sixik.unigui.api.event.PointerReleasedEvent;
import dev.sixik.unigui.api.event.ScrollBarValueChangedEvent;
import dev.sixik.unigui.api.input.KeyCodes;
import dev.sixik.unigui.api.input.PointerButton;
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
import dev.sixik.unigui.widgets.core.Orientation;
import dev.sixik.unigui.widgets.WidgetStyles;
import dev.sixik.unigui.api.style.StyleAnimationIds;
import dev.sixik.unigui.api.style.StyleIds;
import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;

@AiDoc(
        summary = "Draggable scrollbar control for scrolling content along horizontal or vertical axes.",
        purpose = "Provides an interactive scrollbar track and thumb for navigating larger scrollable viewports.",
        useWhen = {
                "Scroll large documents, lists, or custom canvas areas.",
                "Custom scrolling implementation alongside ScrollView."
        },
        criticalRules = {
                "Orientation defines the axis of movement (VERTICAL or HORIZONTAL).",
                "Thumb size is proportional to pageSize divided by range."
        },
        related = {"ScrollView", "VirtualListView", "Slider"})
@WikiPage(
        title = "<key:scroll.bar.title>",
        slug = "scroll-bar",
        category = "<key:core.widgets.interaction.category>",
        summary = "<key:scroll.bar.summary>",
        order = 190)
@WikiSection(
        title = "<key:scroll.bar.section.overview.title>",
        markdown = "<key:scroll.bar.section.overview.body>",
        order = 10)
@WikiExample(
        title = "<key:scroll.bar.example.basic.title>",
        code = "<key:scroll.bar.example.basic.code>",
        order = 10)
@XmlWidgetName("ScrollBar")
public class ScrollBar extends Box {
    public static final String STYLE_TYPE = StyleIds.Widget.SCROLL_BAR;

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

    public static final float DEFAULT_SIZE = 6.0f;
    public static final float DEFAULT_GAP = 8.0f;
    public static final float DEFAULT_PREFERRED_LENGTH = 120.0f;
    public static final float DEFAULT_PREFERRED_THICKNESS = DEFAULT_SIZE;
    /** Толщина тамба в покое: тонкая полоска, не отвлекает от контента. */
    public static final float DEFAULT_IDLE_THICKNESS = 3.0f;
    /** Толщина тамба под курсором/в драге: удобная зона хвата. */
    public static final float DEFAULT_HOVER_THICKNESS = 6.0f;
    /** Длительность expand/collapse анимации, секунды. */
    public static final float DEFAULT_HOVER_ANIMATION_SECONDS = 0.15f;

    private static final Object THICKNESS_ANIMATION_KEY = new Object();

    private final MutableColor trackColor = new MutableColor(0.0f, 0.0f, 0.0f, 0.28f);
    private final MutableColor thumbColor = new MutableColor(0.55f, 0.58f, 0.62f, 0.60f);
    private final MutableColor thumbHoverColor = new MutableColor(0.80f, 0.82f, 0.86f, 0.90f);
    private final MutableColor renderThumbColor = new MutableColor(0.55f, 0.58f, 0.62f, 0.60f);
    private float idleThickness = DEFAULT_IDLE_THICKNESS;
    private float hoverThickness = DEFAULT_HOVER_THICKNESS;
    private float visualThickness = DEFAULT_IDLE_THICKNESS;
    private TransitionSpec hoverAnimation = TransitionSpec.of(DEFAULT_HOVER_ANIMATION_SECONDS, AnimationEasing.CUBIC_OUT);
    private Orientation orientation = Orientation.VERTICAL;
    private float preferredLength = DEFAULT_PREFERRED_LENGTH;
    private float preferredThickness = DEFAULT_PREFERRED_THICKNESS;
    private float min;
    private float max;
    private float value;
    private float pageSize = 1.0f;
    private float step = 16.0f;
    private boolean dragging;

    public ScrollBar() {
        boxVisualEnabled(false);
        applyComponentStyleDefaults(WidgetStyles.RangeControls.SCROLL_BAR);
        focusable(true);
        trackColor.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
        thumbColor.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
    }

    /**
     * Возвращает стандартный стиль полосы прокрутки, самостоятельно рисующей дорожку и бегунок.
     *
     * @return стандартные значения без фоновой поверхности и рамки
     */
    @Override
    protected Style defaultStyle() {
        return WidgetStyles.RangeControls.SCROLL_BAR;
    }

    @WikiMethod(
            title = "<key:scroll.bar.method.orientation.title>",
            summary = "<key:scroll.bar.method.orientation.summary>")
    public Orientation orientation() {
        return orientation;
    }

    @XmlAttribute(value = "orientation", category = "Layout", defaultValue = "vertical", description = "Scrollbar axis.")
    @WikiMethod(
            title = "<key:scroll.bar.method.set_orientation.title>",
            summary = "<key:scroll.bar.method.set_orientation.summary>")
    public ScrollBar orientation(Orientation orientation) {
        Orientation normalized = orientation == null ? Orientation.VERTICAL : orientation;
        if (this.orientation == normalized) return this;
        this.orientation = normalized;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    @WikiMethod(
            title = "<key:scroll.bar.method.min.title>",
            summary = "<key:scroll.bar.method.min.summary>")
    public float min() {
        return min;
    }

    @XmlAttribute(value = "min", category = "Behavior", defaultValue = "0", description = "Minimum scroll value.")
    @WikiMethod(
            title = "<key:scroll.bar.method.set_min.title>",
            summary = "<key:scroll.bar.method.set_min.summary>")
    public ScrollBar min(float min) {
        return range(min, max);
    }

    @WikiMethod(
            title = "<key:scroll.bar.method.max.title>",
            summary = "<key:scroll.bar.method.max.summary>")
    public float max() {
        return max;
    }

    @XmlAttribute(value = "max", category = "Behavior", defaultValue = "0", description = "Maximum scroll value.")
    @WikiMethod(
            title = "<key:scroll.bar.method.set_max.title>",
            summary = "<key:scroll.bar.method.set_max.summary>")
    public ScrollBar max(float max) {
        return range(min, max);
    }

    @WikiMethod(
            title = "<key:scroll.bar.method.range.title>",
            summary = "<key:scroll.bar.method.range.summary>")
    public ScrollBar range(float min, float max) {
        if (max < min) {
            float swap = min;
            min = max;
            max = swap;
        }
        if (this.min == min && this.max == max) return this;
        this.min = min;
        this.max = max;
        silentValue(value);
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    @WikiMethod(
            title = "<key:scroll.bar.method.value.title>",
            summary = "<key:scroll.bar.method.value.summary>")
    public float value() {
        return value;
    }

    @WikiMethod(
            title = "<key:scroll.bar.method.set_value.title>",
            summary = "<key:scroll.bar.method.set_value.summary>")
    public ScrollBar value(float value) {
        setValue(value, true);
        return this;
    }

    @XmlAttribute(value = "value", category = "Behavior", defaultValue = "0", description = "Initial scroll value without emitting change events during XML load.")
    @WikiMethod(
            title = "<key:scroll.bar.method.silent_value.title>",
            summary = "<key:scroll.bar.method.silent_value.summary>")
    public ScrollBar silentValue(float value) {
        setValue(value, false);
        return this;
    }

    @WikiMethod(
            title = "<key:scroll.bar.method.page_size.title>",
            summary = "<key:scroll.bar.method.page_size.summary>")
    public float pageSize() {
        return pageSize;
    }

    @XmlAttribute(value = "pageSize", category = "Behavior", defaultValue = "1", description = "Visible page size used to calculate thumb length.")
    @WikiMethod(
            title = "<key:scroll.bar.method.set_page_size.title>",
            summary = "<key:scroll.bar.method.set_page_size.summary>")
    public ScrollBar pageSize(float pageSize) {
        float normalized = Math.max(1.0f, pageSize);
        if (this.pageSize == normalized) return this;
        this.pageSize = normalized;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    @WikiMethod(
            title = "<key:scroll.bar.method.step.title>",
            summary = "<key:scroll.bar.method.step.summary>")
    public float step() {
        return step;
    }

    @XmlAttribute(value = "step", category = "Behavior", defaultValue = "16", description = "Keyboard nudge increment.")
    @WikiMethod(
            title = "<key:scroll.bar.method.set_step.title>",
            summary = "<key:scroll.bar.method.set_step.summary>")
    public ScrollBar step(float step) {
        this.step = Math.max(1.0f, step);
        return this;
    }

    @WikiMethod(
            title = "<key:scroll.bar.method.dragging.title>",
            summary = "<key:scroll.bar.method.dragging.summary>")
    public boolean dragging() {
        return dragging;
    }

    @WikiMethod(
            title = "<key:scroll.bar.method.track_color.title>",
            summary = "<key:scroll.bar.method.track_color.summary>")
    public MutableColor trackColor() {
        return trackColor;
    }

    @WikiMethod(
            title = "<key:scroll.bar.method.thumb_color.title>",
            summary = "<key:scroll.bar.method.thumb_color.summary>")
    public MutableColor thumbColor() {
        return thumbColor;
    }

    /**
     * Возвращает live-цвет тамба под курсором и в драге.
     *
     * @return изменяемый hover-цвет тамба
     */
    @WikiMethod(
            title = "<key:scroll.bar.method.thumb_hover_color.title>",
            summary = "<key:scroll.bar.method.thumb_hover_color.summary>")
    public MutableColor thumbHoverColor() {
        return thumbHoverColor;
    }

    /**
     * Возвращает текущую отрисовываемую толщину тамба.
     *
     * <p>Значение анимируется между {@link #idleThickness()} и
     * {@link #hoverThickness()} при наведении/драге. Layout-полоса при этом не
     * меняется: хит-зона стабильна, двигается только визуал.</p>
     *
     * @return визуальная толщина в пикселях UI-пространства
     */
    public float visualThickness() {
        return visualThickness;
    }

    /**
     * Возвращает толщину тамба в покое.
     *
     * @return толщина без ховера в пикселях UI-пространства
     */
    @WikiMethod(
            title = "<key:scroll.bar.method.idle_thickness.title>",
            summary = "<key:scroll.bar.method.idle_thickness.summary>")
    public float idleThickness() {
        return idleThickness;
    }

    /**
     * Задаёт толщину тамба в покое.
     *
     * @param idleThickness толщина в пикселях; минимум {@code 1}
     * @return этот scrollbar для fluent-настройки
     */
    @XmlAttribute(value = "idleThickness", category = "Appearance", defaultValue = "3", description = "Thumb thickness at rest in UI pixels.")
    @WikiMethod(
            title = "<key:scroll.bar.method.set_idle_thickness.title>",
            summary = "<key:scroll.bar.method.set_idle_thickness.summary>")
    public ScrollBar idleThickness(float idleThickness) {
        float normalized = Float.isFinite(idleThickness) ? Math.max(1.0f, idleThickness) : DEFAULT_IDLE_THICKNESS;
        if (this.idleThickness == normalized) return this;
        this.idleThickness = normalized;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    /**
     * Возвращает толщину тамба под курсором и в драге.
     *
     * @return расширенная толщина в пикселях UI-пространства
     */
    @WikiMethod(
            title = "<key:scroll.bar.method.hover_thickness.title>",
            summary = "<key:scroll.bar.method.hover_thickness.summary>")
    public float hoverThickness() {
        return hoverThickness;
    }

    /**
     * Задаёт толщину тамба под курсором и в драге.
     *
     * @param hoverThickness толщина в пикселях; минимум {@code 1}
     * @return этот scrollbar для fluent-настройки
     */
    @XmlAttribute(value = "hoverThickness", category = "Appearance", defaultValue = "6", description = "Thumb thickness on hover and drag in UI pixels.")
    @WikiMethod(
            title = "<key:scroll.bar.method.set_hover_thickness.title>",
            summary = "<key:scroll.bar.method.set_hover_thickness.summary>")
    public ScrollBar hoverThickness(float hoverThickness) {
        float normalized = Float.isFinite(hoverThickness) ? Math.max(1.0f, hoverThickness) : DEFAULT_HOVER_THICKNESS;
        if (this.hoverThickness == normalized) return this;
        this.hoverThickness = normalized;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    /**
     * Возвращает spec expand/collapse анимации.
     *
     * @return параметры перехода толщины и цвета
     */
    public TransitionSpec hoverAnimation() {
        return hoverAnimation;
    }

    /**
     * Задаёт spec expand/collapse анимации.
     *
     * @param hoverAnimation параметры перехода; {@code null} означает мгновенно
     * @return этот scrollbar для fluent-настройки
     */
    public ScrollBar hoverAnimation(TransitionSpec hoverAnimation) {
        this.hoverAnimation = hoverAnimation == null ? TransitionSpec.of(0.0f) : hoverAnimation;
        return this;
    }

    public float preferredLength() {
        return preferredLength;
    }

    @XmlAttribute(value = "preferredLength", category = "Layout", defaultValue = "120", description = "Intrinsic length along the scroll axis.")
    public ScrollBar preferredLength(float preferredLength) {
        float normalized = positiveOr(preferredLength, DEFAULT_PREFERRED_LENGTH);
        if (this.preferredLength == normalized) return this;
        this.preferredLength = normalized;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public float preferredThickness() {
        return preferredThickness;
    }

    @XmlAttribute(value = "preferredThickness", category = "Layout", defaultValue = "6", description = "Intrinsic thickness across the scroll axis.")
    public ScrollBar preferredThickness(float preferredThickness) {
        float normalized = positiveOr(preferredThickness, DEFAULT_PREFERRED_THICKNESS);
        if (this.preferredThickness == normalized) return this;
        this.preferredThickness = normalized;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public ScrollBar preferredSize(float length, float thickness) {
        return preferredLength(length).preferredThickness(thickness);
    }

    @WikiMethod(
            title = "<key:scroll.bar.method.on_value_changed.title>",
            summary = "<key:scroll.bar.method.on_value_changed.summary>")
    public EventSubscription onValueChanged(EventListener<? super ScrollBarValueChangedEvent> listener) {
        return on(ScrollBarValueChangedEvent.TYPE, listener);
    }

    @Override
    public void measure(LayoutContext context) {
        if (visibility() == Visibility.COLLAPSED) {
            setDesiredSize(0.0f, 0.0f);
            return;
        }
        if (orientation == Orientation.VERTICAL) {
            setDesiredSize(resolveDesiredSize(context, preferredThickness, preferredLength));
        } else {
            setDesiredSize(resolveDesiredSize(context, preferredLength, preferredThickness));
        }
    }

    @Override
    protected void applyTheme() {
        super.applyTheme();
        trackColor.set(styleValue(StyleKeys.TRACK_COLOR, trackColor));
        thumbColor.set(styleValue(StyleKeys.THUMB_COLOR, thumbColor));
        thumbHoverColor.set(styleValue(StyleKeys.THUMB_COLOR, dev.sixik.unigui.api.style.WidgetState.HOVERED, thumbHoverColor));
    }

    @Override
    public void tick(dev.sixik.unigui.api.core.FrameContext frame) {
        super.tick(frame);
        syncHoverVisuals();
    }

    /**
     * Перенацеливает анимации толщины и цвета под текущее состояние.
     *
     * <p>Активность = hover или drag. Анимации идут через существующие
     * {@code animateParameter}/{@code animateColor} с ретаргетом: повторный вызов
     * в том же состоянии — no-op, смена состояния — плавный переход от текущего
     * значения. Layout не трогаем: анимируется только отрисовка.</p>
     */
    private void syncHoverVisuals() {
        boolean active = dragging || hovered();
        float targetThickness = active
                ? styleValue(StyleKeys.THUMB_THICKNESS, dev.sixik.unigui.api.style.WidgetState.HOVERED, hoverThickness)
                : styleValue(StyleKeys.THUMB_THICKNESS, dev.sixik.unigui.api.style.WidgetState.NORMAL, idleThickness);
        targetThickness = Float.isFinite(targetThickness) ? Math.max(1.0f, targetThickness) : DEFAULT_IDLE_THICKNESS;
        animateParameter(THICKNESS_ANIMATION_KEY,
                () -> visualThickness,
                value -> visualThickness = value,
                targetThickness,
                hoverAnimation);
        animateColor(renderThumbColor, active ? thumbHoverColor : thumbColor, hoverAnimation);
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
            updateFromRoot(pointer.rootX(), pointer.rootY());
            event.cancel();
        } else if (event instanceof PointerMovedEvent pointer && dragging) {
            updateFromRoot(pointer.rootX(), pointer.rootY());
            event.cancel();
        } else if (event instanceof PointerReleasedEvent pointer && pointer.button() == PointerButton.PRIMARY && dragging) {
            updateFromRoot(pointer.rootX(), pointer.rootY());
            dragging = false;
            UIContext context = uiContext();
            if (context != null) {
                context.releasePointer(pointer.pointerId(), this);
            }
            event.cancel();
        } else if (event instanceof KeyPressedEvent key && key.phase() == EventPhase.TARGET && isFocused()) {
            if (key.keyCode() == KeyCodes.LEFT || key.keyCode() == KeyCodes.UP) {
                nudge(-1.0f);
                event.cancel();
            } else if (key.keyCode() == KeyCodes.RIGHT || key.keyCode() == KeyCodes.DOWN) {
                nudge(1.0f);
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
        float height = layoutBounds().height();
        Float styledRadius = styleValue(StyleKeys.THUMB_RADIUS, (Float) null);
        // Тамб якорится к внешнему краю полосы (справа/снизу): при expand
        // растёт внутрь вьюпорта, хит-зона layout-полосы при этом стабильна.
        float effectiveVisualThickness = dragging || hovered()
                ? visualThickness
                : styleValue(StyleKeys.THUMB_THICKNESS, dev.sixik.unigui.api.style.WidgetState.NORMAL, idleThickness);
        effectiveVisualThickness = Float.isFinite(effectiveVisualThickness)
                ? Math.max(1.0f, effectiveVisualThickness)
                : DEFAULT_IDLE_THICKNESS;
        float thickness = Math.min(effectiveVisualThickness,
                orientation == Orientation.VERTICAL ? width : height);
        draw.roundedRectFilled(x, y, width, height, styledRadius == null ? Math.min(width, height) * 0.5f : Math.max(0.0f, styledRadius),
                trackColor);
        if (orientation == Orientation.VERTICAL) {
            float thumbHeight = thumbLength(height);
            float thumbY = y + (height - thumbHeight) * normalizedValue();
            float thumbX = x + width - thickness;
            draw.roundedRectFilled(thumbX, thumbY, thickness, thumbHeight, styledRadius == null ? thickness * 0.5f : Math.max(0.0f, styledRadius),
                    renderThumbColor);
        } else {
            float thumbWidth = thumbLength(width);
            float thumbX = x + (width - thumbWidth) * normalizedValue();
            float thumbY = y + height - thickness;
            draw.roundedRectFilled(thumbX, thumbY, thumbWidth, thickness, styledRadius == null ? thickness * 0.5f : Math.max(0.0f, styledRadius),
                    renderThumbColor);
        }
        super.renderContent(context);
    }

    private boolean isFocused() {
        UIContext context = uiContext();
        return context != null && context.focusManager().isFocused(this);
    }

    private void updateFromLocal(float localX, float localY) {
        float length = Math.max(1.0f, orientation == Orientation.VERTICAL ? layoutBounds().height() : layoutBounds().width());
        float thumbLength = thumbLength(length);
        float position = orientation == Orientation.VERTICAL ? localY : localX;
        float trackTravel = Math.max(1.0f, length - thumbLength);
        float normalized = clamp((position - thumbLength * 0.5f) / trackTravel, 0.0f, 1.0f);
        setValue(min + normalized * (max - min), true);
    }

    private void updateFromRoot(float rootX, float rootY) {
        updateFromLocal(rootX - layoutBounds().x(), rootY - layoutBounds().y());
    }

    private void nudge(float direction) {
        setValue(value + step * direction, true);
    }

    private float normalizedValue() {
        float range = max - min;
        if (range <= 0.0f) return 0.0f;
        return clamp((value - min) / range, 0.0f, 1.0f);
    }

    private float thumbLength(float trackLength) {
        float contentExtent = Math.max(pageSize, pageSize + Math.max(0.0f, max - min));
        return Math.max(8.0f, trackLength * (pageSize / contentExtent));
    }

    private void setValue(float value, boolean emitChange) {
        float normalized = clamp(value, min, max);
        if (this.value == normalized) return;
        float oldValue = this.value;
        this.value = normalized;
        invalidate(InvalidationFlags.VISUAL);
        if (emitChange) {
            emit(new ScrollBarValueChangedEvent(this, oldValue, this.value));
        }
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static float positiveOr(float value, float fallback) {
        return Float.isFinite(value) && value > 0.0f ? value : fallback;
    }
}