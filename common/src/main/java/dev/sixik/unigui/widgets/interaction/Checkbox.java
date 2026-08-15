package dev.sixik.unigui.widgets.interaction;

import dev.sixik.unigui.api.animation.TransitionSpec;
import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.event.ButtonClickEvent;
import dev.sixik.unigui.api.event.CheckboxStateChangedEvent;
import dev.sixik.unigui.api.event.CheckedChangedEvent;
import dev.sixik.unigui.api.event.Event;
import dev.sixik.unigui.api.event.EventListener;
import dev.sixik.unigui.api.event.EventPhase;
import dev.sixik.unigui.api.event.EventSubscription;
import dev.sixik.unigui.api.event.KeyPressedEvent;
import dev.sixik.unigui.api.input.KeyCodes;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.text.RichText;
import dev.sixik.unigui.api.widget.CheckboxState;
import dev.sixik.unigui.api.widget.skin.WidgetsRender;
import dev.sixik.unigui.impl.text.TextEngine;
import dev.sixik.unigui.widgets.render.ButtonRenderType;
import dev.sixik.unigui.widgets.render.ButtonRenderer;
import dev.sixik.unigui.widgets.render.ButtonState;

public class Checkbox extends ToggleButton {
    private static final float BOX_SIZE = 12.0f;
    private static final float CHECK_SIZE = 6.0f;
    private static final float TEXT_GAP = 4.0f;
    public static final float DEFAULT_CHECK_ANIMATION_SECONDS = 0.12f;

    private static final Object CHECK_PROGRESS_ANIMATION_KEY = new Object();

    private CheckboxState state = CheckboxState.UNCHECKED;
    private boolean triState;
    private boolean cyclingFromClick;
    private float boxSize = BOX_SIZE;
    private float checkSize = CHECK_SIZE;
    private float textGap = TEXT_GAP;
    private boolean labelLeft;
    private float checkProgress;
    private TransitionSpec checkAnimation = TransitionSpec.of(DEFAULT_CHECK_ANIMATION_SECONDS);

    public Checkbox() {
        this("");
    }

    public Checkbox(String text) {
        super(text);
        backgroundVisible(false);
        borderVisible(false);
    }

    public Checkbox(RichText text) {
        super(text);
        backgroundVisible(false);
        borderVisible(false);
    }

    public CheckboxState state() {
        return state;
    }

    public Checkbox state(CheckboxState state) {
        setState(state, true);
        return this;
    }

    public Checkbox silentState(CheckboxState state) {
        setState(state, false);
        return this;
    }

    public boolean indeterminate() {
        return state == CheckboxState.INDETERMINATE;
    }

    public Checkbox indeterminate(boolean indeterminate) {
        return state(indeterminate ? CheckboxState.INDETERMINATE : CheckboxState.UNCHECKED);
    }

    public boolean triState() {
        return triState;
    }

    public Checkbox triState(boolean triState) {
        this.triState = triState;
        return this;
    }

    public float boxSize() {
        return boxSize;
    }

    public Checkbox boxSize(float boxSize) {
        float normalized = positiveOr(boxSize, BOX_SIZE);
        if (this.boxSize == normalized) return this;
        this.boxSize = normalized;
        if (checkSize > normalized) checkSize = normalized;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public float checkSize() {
        return checkSize;
    }

    public Checkbox checkSize(float checkSize) {
        float normalized = Math.min(positiveOr(checkSize, CHECK_SIZE), boxSize);
        if (this.checkSize == normalized) return this;
        this.checkSize = normalized;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public float textGap() {
        return textGap;
    }

    public Checkbox textGap(float textGap) {
        float normalized = Float.isFinite(textGap) ? Math.max(0.0f, textGap) : TEXT_GAP;
        if (this.textGap == normalized) return this;
        this.textGap = normalized;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public boolean labelLeft() {
        return labelLeft;
    }

    public Checkbox labelLeft(boolean labelLeft) {
        if (this.labelLeft == labelLeft) return this;
        this.labelLeft = labelLeft;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public float checkProgress() {
        return checkProgress;
    }

    public TransitionSpec checkAnimation() {
        return checkAnimation;
    }

    public Checkbox checkAnimation(float durationSeconds) {
        return checkAnimation(TransitionSpec.of(durationSeconds));
    }

    public Checkbox checkAnimation(TransitionSpec checkAnimation) {
        this.checkAnimation = checkAnimation == null ? TransitionSpec.DEFAULT : checkAnimation;
        return this;
    }

    @Override
    public boolean checked() {
        return state == CheckboxState.CHECKED;
    }

    @Override
    public Checkbox checked(boolean checked) {
        setState(checked ? CheckboxState.CHECKED : CheckboxState.UNCHECKED, true);
        return this;
    }

    @Override
    public Checkbox silentChecked(boolean checked) {
        setState(checked ? CheckboxState.CHECKED : CheckboxState.UNCHECKED, false);
        return this;
    }

    public EventSubscription onStateChanged(EventListener<? super CheckboxStateChangedEvent> listener) {
        return on(CheckboxStateChangedEvent.TYPE, listener);
    }

    @Override
    public ButtonClickEvent click() {
        if (!triState) {
            return super.click();
        }
        cyclingFromClick = true;
        try {
            return super.click();
        } finally {
            cyclingFromClick = false;
        }
    }

    @Override
    public void measure(LayoutContext context) {
        if (visibility() == dev.sixik.unigui.api.widget.Visibility.COLLAPSED) {
            setDesiredSize(0.0f, 0.0f);
            return;
        }
        boolean hasText = !text().isEmpty();
        float textWidth = hasText ? textGap + TextEngine.measureLineWidth(richText()) : 0.0f;
        float textHeight = hasText ? TextEngine.measureTextHeight(richText()) : 0.0f;
        setDesiredSize(resolveDesiredSize(context, boxSize + textWidth, Math.max(boxSize, textHeight)));
    }

    @Override
    public void handle(Event event) {
        if (triState
                && event instanceof KeyPressedEvent key
                && key.phase() == EventPhase.TARGET
                && uiContext() != null
                && uiContext().focusManager().isFocused(this)
                && (key.keyCode() == KeyCodes.SPACE || key.keyCode() == KeyCodes.ENTER || key.keyCode() == KeyCodes.KEYPAD_ENTER)) {
            cycleState(true);
            event.cancel();
            return;
        }
        super.handle(event);
    }

    @Override
    protected void renderContent(RenderContext context) {
        effectiveRenderer().render(new DrawScope(context, transform(), layoutBounds()), snapshot(context));
        renderChildren(context);
    }

    @Override
    protected ButtonRenderer effectiveRenderer() {
        return renderer() == null ? styleRenderer(ButtonRenderer.class, WidgetsRender.checkbox()) : renderer();
    }

    @Override
    protected ButtonState snapshot(RenderContext context) {
        return new ButtonState(
                ButtonRenderType.CHECKBOX,
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
                state == CheckboxState.CHECKED,
                state == CheckboxState.INDETERMINATE,
                boxSize,
                checkSize,
                textGap,
                checkedBackground().copy(),
                borderColor().copy(),
                checkProgress,
                labelLeft);
    }

    private void setState(CheckboxState state, boolean emitChange) {
        CheckboxState next = normalize(state);
        if (cyclingFromClick && triState) {
            next = nextState(this.state);
        }
        if (this.state == next) return;
        CheckboxState oldState = this.state;
        boolean oldChecked = checked();
        this.state = next;
        super.silentChecked(next == CheckboxState.CHECKED);
        animateCheckProgress(next != CheckboxState.UNCHECKED);
        invalidate(InvalidationFlags.VISUAL);
        if (emitChange) {
            emit(new CheckboxStateChangedEvent(this, oldState, next));
            if (oldChecked != checked()) {
                emit(new CheckedChangedEvent(this, oldChecked, checked()));
            }
        }
    }

    private void cycleState(boolean emitChange) {
        setState(nextState(state), emitChange);
    }

    private void animateCheckProgress(boolean visible) {
        animateParameter(
                CHECK_PROGRESS_ANIMATION_KEY,
                this::checkProgress,
                this::setCheckProgress,
                visible ? 1.0f : 0.0f,
                checkAnimation);
    }

    private void setCheckProgress(float progress) {
        float normalized = clamp01(progress);
        if (this.checkProgress == normalized) return;
        this.checkProgress = normalized;
        invalidate(InvalidationFlags.VISUAL);
    }

    private static CheckboxState nextState(CheckboxState state) {
        return switch (normalize(state)) {
            case UNCHECKED -> CheckboxState.CHECKED;
            case CHECKED -> CheckboxState.INDETERMINATE;
            case INDETERMINATE -> CheckboxState.UNCHECKED;
        };
    }

    private static CheckboxState normalize(CheckboxState state) {
        return state == null ? CheckboxState.UNCHECKED : state;
    }

    private static float positiveOr(float value, float fallback) {
        return Float.isFinite(value) && value > 0.0f ? value : fallback;
    }

    private static float clamp01(float value) {
        if (!Float.isFinite(value)) return 0.0f;
        return Math.max(0.0f, Math.min(1.0f, value));
    }
}
