package dev.sixik.unigui.widgets.interaction;

import dev.sixik.unigui.api.animation.TransitionSpec;
import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.event.CheckedChangedEvent;
import dev.sixik.unigui.api.event.Event;
import dev.sixik.unigui.api.event.EventListener;
import dev.sixik.unigui.api.event.EventPhase;
import dev.sixik.unigui.api.event.EventSubscription;
import dev.sixik.unigui.api.event.KeyPressedEvent;
import dev.sixik.unigui.api.input.KeyCodes;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.style.StyleKeys;
import dev.sixik.unigui.api.style.WidgetState;
import dev.sixik.unigui.api.text.RichText;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.widget.skin.WidgetsRender;
import dev.sixik.unigui.impl.text.TextEngine;
import dev.sixik.unigui.widgets.render.ButtonRenderType;
import dev.sixik.unigui.widgets.render.ButtonRenderer;
import dev.sixik.unigui.widgets.render.ButtonState;

import java.util.Objects;

public class RadioButton extends Button {
    private static final float OUTER_SIZE = 12.0f;
    private static final float INNER_SIZE = 6.0f;
    private static final float TEXT_GAP = 4.0f;
    public static final float DEFAULT_SELECTION_ANIMATION_SECONDS = 0.12f;

    private static final Object SELECTION_PROGRESS_ANIMATION_KEY = new Object();

    private final MutableColor checkedColor = new MutableColor(0.30f, 0.62f, 0.95f, 1.0f);
    private String value;
    private RadioGroup group;
    private boolean checked;
    private float outerSize = OUTER_SIZE;
    private float innerSize = INNER_SIZE;
    private float textGap = TEXT_GAP;
    private boolean labelLeft;
    private float selectionProgress;
    private TransitionSpec selectionAnimation = TransitionSpec.of(DEFAULT_SELECTION_ANIMATION_SECONDS);

    public RadioButton() {
        this("", "");
    }

    public RadioButton(String text) {
        this(text, text);
    }

    public RadioButton(String text, String value) {
        super(text);
        this.value = normalize(value);
        backgroundVisible(false);
        borderVisible(false);
        checkedColor.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
        onClick(event -> checked(true));
    }

    public RadioButton(RichText text) {
        this(text, text == null ? "" : text.plainText());
    }

    public RadioButton(RichText text, String value) {
        super(text);
        this.value = normalize(value);
        backgroundVisible(false);
        borderVisible(false);
        checkedColor.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
        onClick(event -> checked(true));
    }

    public String value() {
        return value;
    }

    public RadioButton value(String value) {
        String normalized = normalize(value);
        if (Objects.equals(this.value, normalized)) return this;
        this.value = normalized;
        return this;
    }

    public boolean checked() {
        return checked;
    }

    public RadioButton checked(boolean checked) {
        setChecked(checked, true);
        return this;
    }

    public RadioButton silentChecked(boolean checked) {
        setChecked(checked, false);
        return this;
    }

    public float outerSize() {
        return outerSize;
    }

    public RadioButton outerSize(float outerSize) {
        float normalized = positiveOr(outerSize, OUTER_SIZE);
        if (this.outerSize == normalized) return this;
        this.outerSize = normalized;
        if (innerSize > normalized) innerSize = normalized;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public float innerSize() {
        return innerSize;
    }

    public RadioButton innerSize(float innerSize) {
        float normalized = Math.min(positiveOr(innerSize, INNER_SIZE), outerSize);
        if (this.innerSize == normalized) return this;
        this.innerSize = normalized;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public float textGap() {
        return textGap;
    }

    public RadioButton textGap(float textGap) {
        float normalized = Float.isFinite(textGap) ? Math.max(0.0f, textGap) : TEXT_GAP;
        if (this.textGap == normalized) return this;
        this.textGap = normalized;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public boolean labelLeft() {
        return labelLeft;
    }

    public RadioButton labelLeft(boolean labelLeft) {
        if (this.labelLeft == labelLeft) return this;
        this.labelLeft = labelLeft;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public float selectionProgress() {
        return selectionProgress;
    }

    public TransitionSpec selectionAnimation() {
        return selectionAnimation;
    }

    public RadioButton selectionAnimation(float durationSeconds) {
        return selectionAnimation(TransitionSpec.of(durationSeconds));
    }

    public RadioButton selectionAnimation(TransitionSpec selectionAnimation) {
        this.selectionAnimation = selectionAnimation == null ? TransitionSpec.DEFAULT : selectionAnimation;
        return this;
    }

    public RadioGroup group() {
        return group;
    }

    public RadioButton group(RadioGroup group) {
        if (this.group == group) return this;
        if (this.group != null) {
            this.group.remove(this);
        }
        if (group != null) {
            group.add(this);
        }
        return this;
    }

    public MutableColor checkedColor() {
        return checkedColor;
    }

    public EventSubscription onCheckedChanged(EventListener<? super CheckedChangedEvent> listener) {
        return on(CheckedChangedEvent.TYPE, listener);
    }

    @Override
    public void measure(LayoutContext context) {
        if (visibility() == Visibility.COLLAPSED) {
            setDesiredSize(0.0f, 0.0f);
            return;
        }
        boolean hasText = !text().isEmpty();
        float textWidth = hasText ? textGap + TextEngine.measureLineWidth(richText()) : 0.0f;
        float textHeight = hasText ? TextEngine.measureTextHeight(richText()) : 0.0f;
        setDesiredSize(resolveDesiredSize(context, outerSize + textWidth, Math.max(outerSize, textHeight)));
    }

    @Override
    public void handle(Event event) {
        super.handle(event);
        if (event.isCancelled()) return;
        if (event instanceof KeyPressedEvent key
                && key.phase() == EventPhase.TARGET
                && isFocused()
                && (key.keyCode() == KeyCodes.SPACE
                || key.keyCode() == KeyCodes.ENTER
                || key.keyCode() == KeyCodes.KEYPAD_ENTER)) {
            checked(true);
            event.cancel();
        }
    }

    @Override
    protected void applyTheme() {
        super.applyTheme();
        checkedColor.set(styleValue(StyleKeys.ACCENT_COLOR, WidgetState.CHECKED, checkedColor));
    }

    @Override
    protected WidgetState styleState() {
        if (!enabled()) return super.styleState();
        if (pressed()) return WidgetState.PRESSED;
        return checked ? WidgetState.CHECKED : super.styleState();
    }

    @Override
    protected void renderContent(RenderContext context) {
        applyTheme();
        effectiveRenderer().render(new DrawScope(context, transform(), layoutBounds()), snapshot(context));
        renderChildren(context);
    }

    @Override
    protected ButtonRenderer effectiveRenderer() {
        return renderer() == null ? styleRenderer(ButtonRenderer.class, WidgetsRender.radioButton()) : renderer();
    }

    @Override
    protected ButtonState snapshot(RenderContext context) {
        return new ButtonState(
                ButtonRenderType.RADIO_BUTTON,
                layoutBounds().x(),
                layoutBounds().y(),
                layoutBounds().width(),
                layoutBounds().height(),
                text(),
                richText(),
                TEXT_PADDING_X,
                TextEngine.measureLineWidth(context, richText()),
                TextEngine.measureTextHeight(richText()),
                textColor().copy(),
                pressed(),
                hovered(),
                enabled(),
                checked,
                false,
                outerSize,
                innerSize,
                textGap,
                checkedColor.copy(),
                (checked ? checkedColor : borderColor()).copy(),
                selectionProgress,
                labelLeft);
    }

    void setGroupInternal(RadioGroup group) {
        this.group = group;
    }

    void setCheckedFromGroup(boolean checked, boolean emitChange) {
        updateChecked(checked, emitChange);
    }

    private void setChecked(boolean checked, boolean emitChange) {
        if (group != null) {
            if (checked) {
                group.select(this, emitChange);
            } else if (group.selectedButton() == this) {
                group.clearSelection(emitChange);
            } else {
                updateChecked(false, emitChange);
            }
            return;
        }
        updateChecked(checked, emitChange);
    }

    private void updateChecked(boolean checked, boolean emitChange) {
        if (this.checked == checked) return;
        boolean oldValue = this.checked;
        this.checked = checked;
        animateSelectionProgress(checked);
        invalidate(InvalidationFlags.VISUAL);
        if (emitChange) {
            emit(new CheckedChangedEvent(this, oldValue, checked));
        }
    }

    private void animateSelectionProgress(boolean checked) {
        animateParameter(
                SELECTION_PROGRESS_ANIMATION_KEY,
                this::selectionProgress,
                this::setSelectionProgress,
                checked ? 1.0f : 0.0f,
                selectionAnimation);
    }

    private void setSelectionProgress(float progress) {
        float normalized = clamp01(progress);
        if (this.selectionProgress == normalized) return;
        this.selectionProgress = normalized;
        invalidate(InvalidationFlags.VISUAL);
    }

    private boolean isFocused() {
        return uiContext() != null && uiContext().focusManager().isFocused(this);
    }

    private static String normalize(String value) {
        return value == null ? "" : value;
    }

    private static float positiveOr(float value, float fallback) {
        return Float.isFinite(value) && value > 0.0f ? value : fallback;
    }

    private static float clamp01(float value) {
        if (!Float.isFinite(value)) return 0.0f;
        return Math.max(0.0f, Math.min(1.0f, value));
    }
}
