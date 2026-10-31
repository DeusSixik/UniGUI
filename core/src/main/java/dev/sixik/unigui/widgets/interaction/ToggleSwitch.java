package dev.sixik.unigui.widgets.interaction;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiLink;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;
import dev.sixik.unigui.api.animation.TransitionSpec;
import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.style.StyleIds;
import dev.sixik.unigui.api.style.StyleKeys;
import dev.sixik.unigui.api.style.Style;
import dev.sixik.unigui.api.style.WidgetState;
import dev.sixik.unigui.api.text.RichText;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.xml.XmlAttribute;
import dev.sixik.unigui.api.xml.XmlWidgetName;
import dev.sixik.unigui.impl.text.TextEngine;
import dev.sixik.unigui.api.widget.render.WidgetRole;
import dev.sixik.unigui.api.style.StyleAnimationIds;
import dev.sixik.unigui.widgets.WidgetStyles;

@AiDoc(
        summary = "Pill-style sliding switch with animated thumb for on/off configuration.",
        purpose = "Modern mobile and settings switch replacing checkboxes for instant-toggle preferences.",
        useWhen = {"Settings screens, configuration dialogs, enabling features."},
        related = {"Checkbox", "ToggleButton"}
)
@WikiPage(
        title = "<key:toggle.switch.title>",
        slug = "toggle-switch",
        category = "<key:core.widgets.interaction.category>",
        summary = "<key:toggle.switch.summary>",
        order = 70
)
@WikiSection(
        title = "<key:toggle.switch.section.overview.title>",
        markdown = "<key:toggle.switch.section.overview.body>",
        order = 10
)
@WikiExample(
        title = "<key:toggle.switch.example.basic.title>",
        code = "<key:toggle.switch.example.basic.code>",
        order = 10
)
@WikiLink(label = "Checkbox", target = "page:checkbox")
@WikiLink(label = "ToggleButton", target = "page:toggle-button")
@XmlWidgetName("ToggleSwitch")
public class ToggleSwitch extends ToggleButton {
    /** Style type id для StylePack selector/binding. */
    public static final String STYLE_TYPE = StyleIds.Widget.TOGGLE_SWITCH;

    /** Event id, которые имеет смысл показывать для checked-контролов. */
    public static final class AnimationEvents {
        public static final String ON_CLICK = StyleAnimationIds.Event.ON_CLICK;
        public static final String ON_CHECKED_CHANGED = StyleAnimationIds.Event.ON_CHECKED_CHANGED;
        public static final String ON_STATE_CHANGED = StyleAnimationIds.Event.ON_STATE_CHANGED;
        public static final String ON_FOCUS = StyleAnimationIds.Event.ON_FOCUS;
        public static final String ON_BLUR = StyleAnimationIds.Event.ON_BLUR;
        public static final String ON_HOVER = StyleAnimationIds.Event.ON_HOVER;
        public static final String ON_PRESS = StyleAnimationIds.Event.ON_PRESS;
        public static final String ON_RELEASE = StyleAnimationIds.Event.ON_RELEASE;
        public static final java.util.List<String> ALL = StyleAnimationIds.Event.CHECKED_CONTROL;

        private AnimationEvents() {
        }
    }

    public static final float DEFAULT_TRACK_WIDTH = 34.0f;
    public static final float DEFAULT_TRACK_HEIGHT = 18.0f;
    public static final float DEFAULT_THUMB_SIZE = 14.0f;
    public static final float DEFAULT_LABEL_GAP = 6.0f;
    public static final float DEFAULT_SWITCH_ANIMATION_SECONDS = 0.16f;

    private static final Object SWITCH_PROGRESS_ANIMATION_KEY = new Object();

    private final MutableColor thumbColor = new MutableColor(0.95f, 0.95f, 0.95f, 1.0f);
    private float trackWidth = DEFAULT_TRACK_WIDTH;
    private float trackHeight = DEFAULT_TRACK_HEIGHT;
    private float thumbSize = DEFAULT_THUMB_SIZE;
    private float labelGap = DEFAULT_LABEL_GAP;
    private boolean labelLeft;
    private float switchProgress;
    private final MutableColor trackBlend = new MutableColor();
    private final MutableColor thumbBlend = new MutableColor();
    private final MutableColor borderBlend = new MutableColor();
    private TransitionSpec switchAnimation = TransitionSpec.of(DEFAULT_SWITCH_ANIMATION_SECONDS);

    public ToggleSwitch() {
        this("");
    }

    public ToggleSwitch(String text) {
        super(text);
        applyComponentStyleDefaults(WidgetStyles.Surface.UNSURFACED);
        checkedBackground().set(0.25f, 0.78f, 1.0f, 1.0f);
        uncheckedBackground().set(0.22f, 0.22f, 0.22f, 1.0f);
        thumbColor.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
    }

    /**
     * Возвращает стандартный стиль переключателя, дорожка которого рисуется самим элементом.
     *
     * @return стандартные значения без фоновой поверхности и рамки
     */
    @Override
    protected Style defaultStyle() {
        return WidgetStyles.Surface.UNSURFACED;
    }

    public ToggleSwitch(RichText text) {
        this("");
        richText(text);
    }

    @Override
    public ToggleSwitch checked(boolean checked) {
        boolean changed = checked() != checked;
        super.checked(checked);
        if (changed) animateSwitchProgress(checked);
        return this;
    }

    @Override
    @XmlAttribute(value = "checked", category = "Behavior", defaultValue = "false", description = "Initial checked state without emitting change events during XML load.")
    public ToggleSwitch silentChecked(boolean checked) {
        boolean changed = checked() != checked;
        super.silentChecked(checked);
        // Начальное состояние (XML, конструктор) не проигрывает анимацию:
        // иначе уже включённый свитч при открытии UI ехал бы из выключенного.
        if (changed) setSwitchProgress(checked ? 1.0f : 0.0f);
        return this;
    }

    public float trackWidth() {
        return trackWidth;
    }

    @XmlAttribute(value = "trackWidth", category = "Layout", defaultValue = "34", description = "Switch track width in UI pixels.")
    public ToggleSwitch trackWidth(float trackWidth) {
        float normalized = positiveOr(trackWidth, DEFAULT_TRACK_WIDTH);
        if (this.trackWidth == normalized) return this;
        this.trackWidth = normalized;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public float trackHeight() {
        return trackHeight;
    }

    @XmlAttribute(value = "trackHeight", category = "Layout", defaultValue = "18", description = "Switch track height in UI pixels.")
    public ToggleSwitch trackHeight(float trackHeight) {
        float normalized = positiveOr(trackHeight, DEFAULT_TRACK_HEIGHT);
        if (this.trackHeight == normalized) return this;
        this.trackHeight = normalized;
        if (thumbSize > normalized) thumbSize = Math.max(1.0f, normalized - 2.0f);
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public ToggleSwitch trackSize(float width, float height) {
        return trackWidth(width).trackHeight(height);
    }

    public float thumbSize() {
        return thumbSize;
    }

    @XmlAttribute(value = "thumbSize", category = "Layout", defaultValue = "14", description = "Switch thumb size in UI pixels.")
    public ToggleSwitch thumbSize(float thumbSize) {
        float normalized = Math.min(positiveOr(thumbSize, DEFAULT_THUMB_SIZE), trackHeight);
        if (this.thumbSize == normalized) return this;
        this.thumbSize = normalized;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public MutableColor thumbColor() {
        return thumbColor;
    }

    @XmlAttribute(value = "thumbColor", category = "Appearance", defaultValue = "#F2F2F2FF", description = "Switch thumb color.")
    public ToggleSwitch thumbColor(ColorView color) {
        thumbColor.set(color);
        return this;
    }

    public float labelGap() {
        return labelGap;
    }

    @XmlAttribute(value = "labelGap", category = "Layout", defaultValue = "6", description = "Gap between switch track and label text.")
    public ToggleSwitch labelGap(float labelGap) {
        float normalized = Float.isFinite(labelGap) ? Math.max(0.0f, labelGap) : DEFAULT_LABEL_GAP;
        if (this.labelGap == normalized) return this;
        this.labelGap = normalized;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public boolean labelLeft() {
        return labelLeft;
    }

    @XmlAttribute(value = "labelLeft", category = "Layout", defaultValue = "false", description = "Whether label text is rendered before the switch track.")
    public ToggleSwitch labelLeft(boolean labelLeft) {
        if (this.labelLeft == labelLeft) return this;
        this.labelLeft = labelLeft;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public float switchProgress() {
        return switchProgress;
    }

    public TransitionSpec switchAnimation() {
        return switchAnimation;
    }

    public ToggleSwitch switchAnimation(float durationSeconds) {
        return switchAnimation(TransitionSpec.of(durationSeconds));
    }

    public ToggleSwitch switchAnimation(TransitionSpec switchAnimation) {
        this.switchAnimation = switchAnimation == null ? TransitionSpec.DEFAULT : switchAnimation;
        return this;
    }

    @Override
    public void measure(LayoutContext context) {
        if (visibility() == Visibility.COLLAPSED) {
            setDesiredSize(0.0f, 0.0f);
            return;
        }
        applyTheme();
        RichText label = renderedRichText();
        float textWidth = label == null ? 0.0f : TextEngine.measureLineWidth(label);
        float labelWidth = hasLabel() ? labelGap + textWidth : 0.0f;
        float textHeight = TextEngine.measureTextHeight(label);
        setDesiredSize(resolveDesiredSize(context, trackWidth + labelWidth, Math.max(trackHeight, textHeight)));
    }

    @Override
    protected void renderContent(RenderContext context) {
        applyTheme();
        DrawScope draw = new DrawScope(context, transform(), layoutBounds());
        float x = layoutBounds().x();
        float y = layoutBounds().y();
        float width = layoutBounds().width();
        float height = layoutBounds().height();
        RichText label = renderedRichText();
        boolean hasText = label != null && !label.isEmpty();
        if (trackWidth > 0.0f && trackHeight > 0.0f && thumbSize > 0.0f) {
            float gap = hasText ? Math.max(0.0f, labelGap) : 0.0f;
            float textWidth = hasText ? TextEngine.measureLineWidth(draw.context(), label) : 0.0f;
            float textHeight = hasText ? TextEngine.measureTextHeight(draw.context(), label) : 0.0f;
            float labelWidth = hasText
                    ? Math.min(Math.max(0.0f, textWidth), Math.max(0.0f, width - trackWidth - gap))
                    : 0.0f;
            float trackX = labelLeft ? x + labelWidth + gap : x;
            float trackY = y + Math.max(0.0f, height - trackHeight) * 0.5f;
            Float styledRadius = styleValue(StyleKeys.RADIUS, trackHeight * 0.5f);
            float radius = styledRadius == null ? trackHeight * 0.5f : Math.max(0.0f, styledRadius);
            draw.roundedRect(trackX, trackY, trackWidth, trackHeight, radius,
                    Paint.fill(switchTrackColor()));
            ColorView borderOff = styleValue(StyleKeys.BORDER_COLOR, WidgetState.NORMAL, null);
            ColorView borderOn = styleValue(StyleKeys.BORDER_COLOR, WidgetState.CHECKED, borderOff);
            ColorView borderHover = styleValue(StyleKeys.BORDER_COLOR, WidgetState.HOVERED, borderOff);
            Float borderWidthValue = styleValue(StyleKeys.BORDER_WIDTH, 0.0f);
            float borderWidth = borderWidthValue == null ? 0.0f : Math.max(0.0f, borderWidthValue);
            ColorView borderColor = hovered() && enabled() && borderHover != null
                    ? borderHover
                    : interpolate(borderBlend, borderOff, borderOn, switchProgress);
            if (borderWidth > 0.0f && borderColor != null) {
                draw.roundedRectStroke(trackX, trackY, trackWidth, trackHeight, radius,
                        borderColor, borderWidth);
            }

            float thumbPadding = Math.max(1.0f, (trackHeight - thumbSize) * 0.5f);
            float thumbTravel = Math.max(0.0f, trackWidth - thumbSize - thumbPadding * 2.0f);
            float thumbX = trackX + thumbPadding + switchProgress * thumbTravel;
            float thumbY = trackY + Math.max(0.0f, trackHeight - thumbSize) * 0.5f;
            Float thumbRadiusValue = styleValue(StyleKeys.THUMB_RADIUS, thumbSize * 0.5f);
            float thumbRadius = Math.max(0.0f, Math.min(thumbSize * 0.5f,
                    thumbRadiusValue == null ? thumbSize * 0.5f : thumbRadiusValue));
            if (thumbRadius >= thumbSize * 0.5f) {
                draw.circle(thumbX, thumbY, thumbSize, thumbSize, Paint.fill(switchThumbColor()));
            } else if (thumbRadius <= 0.0f) {
                draw.rect(thumbX, thumbY, thumbSize, thumbSize, Paint.fill(switchThumbColor()));
            } else {
                draw.roundedRect(thumbX, thumbY, thumbSize, thumbSize, thumbRadius,
                        Paint.fill(switchThumbColor()));
            }

            if (hasText) {
                float contentX;
                float contentWidth;
                if (labelLeft) {
                    contentX = x;
                    contentWidth = labelWidth;
                } else {
                    contentX = trackX + trackWidth + gap;
                    contentWidth = Math.max(0.0f, width - (contentX - x));
                }
                float drawHeight = Math.min(Math.max(0.0f, height), Math.max(0.0f, textHeight));
                if (contentWidth > 0.0f && drawHeight > 0.0f) {
                    float drawY = y + Math.max(0.0f, height - drawHeight) * 0.5f;
                    renderLabel(draw, label, contentX, y, contentWidth, height,
                            contentX, drawY, contentWidth, drawHeight, textColor());
                }
            }
        }
        renderChildren(context);
    }

    @Override
    protected WidgetRole renderRole() {
        return WidgetRole.TOGGLE_SWITCH;
    }

    @Override
    protected String styleType() {
        return StyleIds.Widget.TOGGLE_SWITCH;
    }

    /**
     * Возвращает цвет дорожки, смешанный по прогрессу переключения.
     *
     * <p>Формула по каждому каналу:
     * <pre>
     *   c = off + (on - off) * p
     * </pre>
     * {@code p} — {@link #switchProgress()} в диапазоне {@code [0, 1]}.
     * Крайние значения берут цвет состояния целиком, промежуточные дают
     * плавный переход вместе с бегунком.</p>
     *
     * @return цвет дорожки на текущем кадре
     */
    private ColorView switchTrackColor() {
        ColorView off = styleValue(StyleKeys.TRACK_COLOR, WidgetState.NORMAL,
                styleValue(StyleKeys.BACKGROUND_COLOR, WidgetState.NORMAL, uncheckedBackground()));
        ColorView on = styleValue(StyleKeys.TRACK_COLOR, WidgetState.CHECKED,
                styleValue(StyleKeys.BACKGROUND_COLOR, WidgetState.CHECKED, checkedBackground()));
        if (off == null) off = uncheckedBackground();
        if (on == null) on = checkedBackground();
        return interpolate(trackBlend, off, on, switchProgress);
    }

    private ColorView switchThumbColor() {
        ColorView off = styleValue(StyleKeys.THUMB_COLOR, WidgetState.NORMAL, thumbColor);
        ColorView on = styleValue(StyleKeys.THUMB_COLOR, WidgetState.CHECKED, off);
        return interpolate(thumbBlend, off == null ? thumbColor : off, on == null ? thumbColor : on,
                switchProgress);
    }

    private static ColorView interpolate(MutableColor target, ColorView from, ColorView to, float amount) {
        if (from == null) return to;
        if (to == null) return from;
        float progress = clamp01(amount);
        if (progress <= 0.0f) return from;
        if (progress >= 1.0f) return to;
        target.set(
                from.r() + (to.r() - from.r()) * progress,
                from.g() + (to.g() - from.g()) * progress,
                from.b() + (to.b() - from.b()) * progress,
                from.a() + (to.a() - from.a()) * progress);
        return target;
    }

    private void animateSwitchProgress(boolean checked) {
        animateParameter(
                SWITCH_PROGRESS_ANIMATION_KEY,
                this::switchProgress,
                this::setSwitchProgress,
                checked ? 1.0f : 0.0f,
                switchAnimation);
    }

    private void setSwitchProgress(float progress) {
        float normalized = clamp01(progress);
        if (this.switchProgress == normalized) return;
        this.switchProgress = normalized;
        invalidate(InvalidationFlags.VISUAL);
    }

    private boolean hasLabel() {
        return richText() != null && !richText().isEmpty();
    }

    private static float positiveOr(float value, float fallback) {
        return Float.isFinite(value) && value > 0.0f ? value : fallback;
    }

    private static float clamp01(float value) {
        if (!Float.isFinite(value)) return 0.0f;
        return Math.max(0.0f, Math.min(1.0f, value));
    }
}
