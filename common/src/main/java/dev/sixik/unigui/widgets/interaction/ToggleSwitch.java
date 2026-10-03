package dev.sixik.unigui.widgets.interaction;

import dev.sixik.unigui.api.animation.TransitionSpec;
import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.style.StyleIds;
import dev.sixik.unigui.api.style.StyleKeys;
import dev.sixik.unigui.api.text.RichText;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.xml.XmlAttribute;
import dev.sixik.unigui.api.xml.XmlWidgetName;
import dev.sixik.unigui.impl.text.TextEngine;
import dev.sixik.unigui.api.widget.render.WidgetRole;
import dev.sixik.unigui.api.style.StyleAnimationIds;

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
    private TransitionSpec switchAnimation = TransitionSpec.of(DEFAULT_SWITCH_ANIMATION_SECONDS);

    public ToggleSwitch() {
        this("");
    }

    public ToggleSwitch(String text) {
        super(text);
        backgroundVisible(false);
        borderVisible(false);
        checkedBackground().set(0.25f, 0.78f, 1.0f, 1.0f);
        uncheckedBackground().set(0.22f, 0.22f, 0.22f, 1.0f);
        thumbColor.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
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
        if (changed) animateSwitchProgress(checked);
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
        float textWidth = Math.max(TextEngine.measureLineWidth(richText()), TextEngine.measureLineWidth(text()));
        float labelWidth = hasLabel() ? labelGap + textWidth : 0.0f;
        float textHeight = TextEngine.measureTextHeight(richText());
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
        boolean hasText = hasLabel();
        if (trackWidth > 0.0f && trackHeight > 0.0f && thumbSize > 0.0f) {
            float gap = hasText ? Math.max(0.0f, labelGap) : 0.0f;
            float textWidth = hasText ? TextEngine.measureLineWidth(draw.context(), richText()) : 0.0f;
            float textHeight = hasText ? TextEngine.measureTextHeight(draw.context(), richText()) : 0.0f;
            float labelWidth = hasText
                    ? Math.min(Math.max(0.0f, textWidth), Math.max(0.0f, width - trackWidth - gap))
                    : 0.0f;
            float trackX = labelLeft ? x + labelWidth + gap : x;
            float trackY = y + Math.max(0.0f, height - trackHeight) * 0.5f;
            draw.roundedRect(trackX, trackY, trackWidth, trackHeight, trackHeight * 0.5f,
                    Paint.fill(switchTrackColor()));

            float thumbPadding = Math.max(1.0f, (trackHeight - thumbSize) * 0.5f);
            float thumbTravel = Math.max(0.0f, trackWidth - thumbSize - thumbPadding * 2.0f);
            float thumbX = trackX + thumbPadding + (checked() ? thumbTravel : 0.0f);
            float thumbY = trackY + Math.max(0.0f, trackHeight - thumbSize) * 0.5f;
            draw.circle(thumbX, thumbY, thumbSize, thumbSize, Paint.fill(thumbColor));

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
                    renderLabel(draw, richText(), contentX, y, contentWidth, height,
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
    protected void applyTheme() {
        super.applyTheme();
        thumbColor.set(styleValue(StyleKeys.THUMB_COLOR, thumbColor));
    }

    @Override
    protected String styleType() {
        return StyleIds.Widget.TOGGLE_SWITCH;
    }

    private ColorView switchTrackColor() {
        ColorView fallback = checked() ? checkedBackground().copy() : uncheckedBackground().copy();
        ColorView themed = styleValue(StyleKeys.BACKGROUND_COLOR, styleState(), fallback);
        return themed == null ? fallback : themed;
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
