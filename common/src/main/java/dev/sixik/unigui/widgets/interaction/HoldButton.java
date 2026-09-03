package dev.sixik.unigui.widgets.interaction;

import dev.sixik.unigui.api.core.FrameContext;
import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.core.UIContext;
import dev.sixik.unigui.api.event.Event;
import dev.sixik.unigui.api.event.EventListener;
import dev.sixik.unigui.api.event.EventPhase;
import dev.sixik.unigui.api.event.EventSubscription;
import dev.sixik.unigui.api.event.HoldCompletedEvent;
import dev.sixik.unigui.api.event.PointerEnteredEvent;
import dev.sixik.unigui.api.event.PointerExitedEvent;
import dev.sixik.unigui.api.event.PointerEvent;
import dev.sixik.unigui.api.event.PointerPressedEvent;
import dev.sixik.unigui.api.event.PointerReleasedEvent;
import dev.sixik.unigui.api.input.PointerButton;
import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.style.WidgetState;
import dev.sixik.unigui.api.text.RichText;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.xml.XmlAttribute;
import dev.sixik.unigui.api.xml.XmlWidgetName;
import dev.sixik.unigui.impl.text.TextEngine;
import dev.sixik.unigui.widgets.render.ButtonRenderer;
import dev.sixik.unigui.widgets.render.HoldButtonRenderer;
import dev.sixik.unigui.widgets.render.HoldButtonRenderers;
import dev.sixik.unigui.widgets.render.HoldButtonState;
import dev.sixik.unigui.api.widget.render.WidgetRole;

/**
 * Button that requires the primary pointer to be held for a configurable duration
 * before firing its action.
 *
 * <p>{@code HoldButton} keeps normal button layout, text, styling and click
 * semantics, but replaces release-to-click with hold-to-click. The default
 * renderer draws a progress fill over the button surface and then renders the
 * usual centered button label.</p>
 */
@XmlWidgetName("HoldButton")
public class HoldButton extends Button {
    public static final String STYLE_TYPE = dev.sixik.unigui.api.style.StyleIds.Widget.HOLD_BUTTON;

    private static final float DEFAULT_HOLD_DURATION_SECONDS = 0.65f;

    private final MutableColor holdColor = new MutableColor(0.25f, 0.78f, 1.0f, 0.35f);
    private HoldButtonRenderer holdRenderer;
    private float holdDurationSeconds = DEFAULT_HOLD_DURATION_SECONDS;
    private float holdElapsedSeconds;
    private boolean holding;
    private boolean completed;
    private boolean cancelOnPointerExit = true;
    private int activePointerId = -1;

    public HoldButton() {
        this("");
    }

    public HoldButton(String text) {
        super(text);
        holdColor.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
    }

    public HoldButton(RichText text) {
        super(text);
        holdColor.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
    }

    public float holdDurationSeconds() {
        return holdDurationSeconds;
    }

    @XmlAttribute(value = "holdDurationSeconds", category = "Behavior", defaultValue = "0.65", description = "Required hold duration before firing the button action.")
    public HoldButton holdDurationSeconds(float holdDurationSeconds) {
        float normalized = Float.isFinite(holdDurationSeconds)
                ? Math.max(0.01f, holdDurationSeconds)
                : DEFAULT_HOLD_DURATION_SECONDS;
        if (this.holdDurationSeconds == normalized) return this;
        this.holdDurationSeconds = normalized;
        if (holding) {
            holdElapsedSeconds = Math.min(holdElapsedSeconds, normalized);
        }
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public float holdElapsedSeconds() {
        return holdElapsedSeconds;
    }

    public float holdProgress() {
        return holdDurationSeconds <= 0.0f
                ? 1.0f
                : Math.max(0.0f, Math.min(1.0f, holdElapsedSeconds / holdDurationSeconds));
    }

    public boolean holding() {
        return holding;
    }

    public boolean completed() {
        return completed;
    }

    public boolean cancelOnPointerExit() {
        return cancelOnPointerExit;
    }

    @XmlAttribute(value = "cancelOnPointerExit", category = "Behavior", defaultValue = "true", description = "Whether leaving the button cancels an active hold gesture.")
    public HoldButton cancelOnPointerExit(boolean cancelOnPointerExit) {
        this.cancelOnPointerExit = cancelOnPointerExit;
        return this;
    }

    public MutableColor holdColor() {
        return holdColor;
    }

    public HoldButton animateHoldColor(ColorView color, float durationSeconds) {
        animateColor(holdColor, color, durationSeconds);
        return this;
    }

    public HoldButton holdRenderer(HoldButtonRenderer holdRenderer) {
        if (this.holdRenderer == holdRenderer) return this;
        this.holdRenderer = holdRenderer;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public HoldButtonRenderer holdRenderer() {
        return holdRenderer;
    }

    public HoldButton useDefaultHoldRenderer() {
        return holdRenderer(null);
    }

    public EventSubscription onHoldCompleted(EventListener<? super HoldCompletedEvent> listener) {
        return on(HoldCompletedEvent.TYPE, listener);
    }

    public HoldButton resetHold() {
        stopHolding(true);
        completed = false;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    @Override
    public boolean pressed() {
        return holding || super.pressed();
    }

    @Override
    public void tick(FrameContext frame) {
        super.tick(frame);
        if (!holding || completed || visibility() != Visibility.VISIBLE || !enabled()) return;

        float delta = frame == null || !Float.isFinite(frame.deltaSeconds()) || frame.deltaSeconds() <= 0.0f
                ? 1.0f / 60.0f
                : frame.deltaSeconds();
        holdElapsedSeconds = Math.min(holdDurationSeconds, holdElapsedSeconds + delta);
        invalidate(InvalidationFlags.VISUAL);

        if (holdElapsedSeconds >= holdDurationSeconds) {
            completeHold();
        }
    }

    @Override
    public void handle(Event event) {
        if (visibility() != Visibility.VISIBLE || !enabled()) {
            stopHolding(true);
            return;
        }

        if (event instanceof PointerPressedEvent pointer
                && pointer.phase() == EventPhase.TARGET
                && pointer.button() == PointerButton.PRIMARY) {
            emit(event);
            if (!event.isCancelled()) {
                startHolding(pointer.pointerId());
                event.cancel();
            }
            return;
        }

        if (event instanceof PointerReleasedEvent pointer
                && pointer.phase() == EventPhase.TARGET
                && pointer.button() == PointerButton.PRIMARY
                && holding
                && isActivePointer(pointer)) {
            emit(event);
            stopHolding(true);
            event.cancel();
            return;
        }

        if (event instanceof PointerExitedEvent exited && exited.phase() == EventPhase.TARGET && cancelOnPointerExit) {
            stopHolding(true);
        }

        super.handle(event);
    }

    @Override
    protected void renderContent(RenderContext context) {
        applyTheme();
        effectiveHoldRenderer().render(new DrawScope(context, transform(), layoutBounds()), holdSnapshot(context));
        renderChildren(context);
    }

    @Override
    protected WidgetState styleState() {
        if (!enabled()) return super.styleState();
        return holding ? WidgetState.PRESSED : super.styleState();
    }

    protected HoldButtonRenderer effectiveHoldRenderer() {
        if (holdRenderer != null) return holdRenderer;

        ButtonRenderer buttonRenderer = renderer();
        if (buttonRenderer != null) {
            return withHoldProgress(buttonRenderer);
        }

        HoldButtonRenderer styled = styleRenderer(WidgetRole.HOLD_BUTTON, HoldButtonRenderer.class, null);
        if (styled != null) return styled;

        ButtonRenderer styledLegacy = styleRenderer(WidgetRole.HOLD_BUTTON, ButtonRenderer.class, null);
        return styledLegacy == null ? HoldButtonRenderers.DEFAULT : withHoldProgress(styledLegacy);
    }

    private HoldButtonRenderer withHoldProgress(ButtonRenderer buttonRenderer) {
        return (draw, state) -> {
            buttonRenderer.render(draw, state.button());
            float progress = Math.max(0.0f, Math.min(1.0f, state.holdProgress()));
            if (progress > 0.0f) {
                draw.rect(state.x(), state.y(), state.width() * progress, state.height(),
                        Paint.fill(state.holdColor()));
            }
        };
    }

    protected HoldButtonState holdSnapshot(RenderContext context) {
        return new HoldButtonState(
                layoutBounds().x(),
                layoutBounds().y(),
                layoutBounds().width(),
                layoutBounds().height(),
                text(),
                richText(),
                textPaddingX(),
                TextEngine.measureLineWidth(context, richText()),
                TextEngine.measureTextHeight(context, richText()),
                textColor().copy(),
                pressed(),
                hovered(),
                enabled(),
                backgroundVisible(),
                background().copy(),
                radius(),
                borderVisible(),
                borderColor().copy(),
                borderWidth(),
                holdProgress(),
                holdElapsedSeconds,
                holdDurationSeconds,
                holding,
                completed,
                holdColor.copy());
    }

    private void startHolding(int pointerId) {
        activePointerId = pointerId;
        UIContext context = uiContext();
        if (context != null) {
            context.capturePointer(pointerId, this);
        }
        holding = true;
        completed = false;
        holdElapsedSeconds = 0.0f;
        invalidate(InvalidationFlags.VISUAL);
    }

    private void stopHolding(boolean resetProgress) {
        if (!holding && (!resetProgress || holdElapsedSeconds == 0.0f)) return;
        holding = false;
        releaseActivePointer();
        if (resetProgress) {
            holdElapsedSeconds = 0.0f;
        }
        invalidate(InvalidationFlags.VISUAL);
    }

    private boolean isActivePointer(PointerEvent pointer) {
        return activePointerId < 0 || pointer.pointerId() == activePointerId;
    }

    private void completeHold() {
        if (completed) return;
        completed = true;
        holding = false;
        releaseActivePointer();
        holdElapsedSeconds = holdDurationSeconds;
        invalidate(InvalidationFlags.VISUAL);

        HoldCompletedEvent event = dispatchHoldCompleted();
        if (!event.isCancelled()) {
            click();
        }

        holdElapsedSeconds = 0.0f;
        invalidate(InvalidationFlags.VISUAL);
    }

    private void releaseActivePointer() {
        if (activePointerId < 0) return;
        UIContext context = uiContext();
        if (context != null) {
            context.releasePointer(activePointerId, this);
        }
        activePointerId = -1;
    }

    private HoldCompletedEvent dispatchHoldCompleted() {
        HoldCompletedEvent event = new HoldCompletedEvent(this, holdDurationSeconds);
        UIContext context = uiContext();
        if (context == null) {
            emit(event);
        } else {
            context.routedEvents().dispatch(event);
        }
        return event;
    }
}
