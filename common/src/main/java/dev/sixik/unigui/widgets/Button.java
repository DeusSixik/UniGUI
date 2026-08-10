package dev.sixik.unigui.widgets;

import dev.sixik.unigui.api.animation.AnimationEasing;
import dev.sixik.unigui.api.animation.TransitionSpec;
import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.core.UIContext;
import dev.sixik.unigui.api.event.ButtonClickEvent;
import dev.sixik.unigui.api.event.EventPhase;
import dev.sixik.unigui.api.event.Event;
import dev.sixik.unigui.api.event.EventListener;
import dev.sixik.unigui.api.event.EventSubscription;
import dev.sixik.unigui.api.event.PointerEvent;
import dev.sixik.unigui.api.event.PointerEnteredEvent;
import dev.sixik.unigui.api.event.PointerExitedEvent;
import dev.sixik.unigui.api.event.PointerPressedEvent;
import dev.sixik.unigui.api.event.PointerReleasedEvent;
import dev.sixik.unigui.api.input.PointerButton;
import dev.sixik.unigui.api.input.MouseCursor;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.text.RichText;
import dev.sixik.unigui.api.style.StyleKeys;
import dev.sixik.unigui.api.style.WidgetState;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.widget.skin.WidgetsRender;
import dev.sixik.unigui.impl.text.TextEngine;
import dev.sixik.unigui.widgets.render.ButtonRenderer;
import dev.sixik.unigui.widgets.render.ButtonRenderType;
import dev.sixik.unigui.widgets.render.ButtonState;

import java.util.Objects;

public class Button extends Box {
    protected static final float TEXT_PADDING_X = 8.0f;
    protected static final float DEFAULT_HEIGHT = 18.0f;
    protected static final float APPROX_CHAR_WIDTH = TextEngine.APPROX_CHAR_WIDTH;

    private String text = "";
    private RichText richText = RichText.plain("");
    private final MutableColor textColor = new MutableColor(1.0f, 1.0f, 1.0f, 1.0f);
    private ButtonRenderer renderer;
    private boolean pressed;
    private boolean interactionTransitions;
    private TransitionSpec interactionTransition = TransitionSpec.of(0.10f, AnimationEasing.EASE_OUT);
    private float normalScale = 1.0f;
    private float hoveredScale = 1.025f;
    private float pressedScale = 0.970f;
    private float normalOpacity = 1.0f;
    private float pressedOpacity = 0.88f;
    private float disabledOpacity = 0.55f;

    public Button() {
        mouseCursor(MouseCursor.POINTER);
        backgroundVisible(true);
        borderVisible(true);
        focusable(true);
        textColor.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
    }

    public Button(String text) {
        this();
        text(text);
    }

    public Button(RichText text) {
        this();
        richText(text);
    }

    public String text() {
        return text;
    }

    public Button text(String text) {
        String normalized = normalize(text);
        RichText normalizedRichText = RichText.plain(normalized);
        if (Objects.equals(this.richText, normalizedRichText)) return this;
        this.text = normalized;
        this.richText = normalizedRichText;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public RichText richText() {
        return richText;
    }

    public Button richText(RichText richText) {
        RichText normalized = richText == null ? RichText.plain("") : richText;
        if (Objects.equals(this.richText, normalized)) return this;
        this.richText = normalized;
        this.text = normalized.plainText();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public MutableColor textColor() {
        return textColor;
    }

    public ButtonRenderer renderer() {
        return renderer;
    }

    public Button renderer(ButtonRenderer renderer) {
        if (this.renderer == renderer) return this;
        this.renderer = renderer;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public Button useDefaultRenderer() {
        return renderer(null);
    }

    public boolean pressed() {
        return pressed;
    }

    public boolean interactionTransitions() {
        return interactionTransitions;
    }

    public Button interactionTransitions(boolean interactionTransitions) {
        if (this.interactionTransitions == interactionTransitions) return this;
        this.interactionTransitions = interactionTransitions;
        applyInteractionTransition();
        return this;
    }

    public Button interactionTransition(TransitionSpec interactionTransition) {
        this.interactionTransition = interactionTransition == null ? TransitionSpec.DEFAULT : interactionTransition;
        return this;
    }

    public Button interactionScales(float normal, float hovered, float pressed) {
        normalScale = sanitizeScale(normal);
        hoveredScale = sanitizeScale(hovered);
        pressedScale = sanitizeScale(pressed);
        applyInteractionTransition();
        return this;
    }

    public Button interactionOpacities(float normal, float pressed, float disabled) {
        normalOpacity = clamp01(normal);
        pressedOpacity = clamp01(pressed);
        disabledOpacity = clamp01(disabled);
        applyInteractionTransition();
        return this;
    }

    public EventSubscription onClick(EventListener<? super ButtonClickEvent> listener) {
        return on(ButtonClickEvent.TYPE, listener);
    }

    @Override
    public Button enabled(boolean enabled) {
        boolean changed = enabled() != enabled;
        super.enabled(enabled);
        if (changed) {
            applyInteractionTransition();
        }
        return this;
    }

    @Override
    public void measure(LayoutContext context) {
        if (visibility() == Visibility.COLLAPSED) {
            setDesiredSize(0.0f, 0.0f);
            return;
        }
        float textWidth = Math.max(
                TextEngine.measureLineWidth(richText),
                TextEngine.measureLineWidth(text));
        setDesiredSize(resolveDesiredSize(context, textWidth + TEXT_PADDING_X * 2.0f, DEFAULT_HEIGHT));
    }

    public ButtonClickEvent click() {
        ButtonClickEvent event = new ButtonClickEvent(this);
        UIContext context = uiContext();
        if (context == null) {
            emit(event);
        } else {
            context.routedEvents().dispatch(event);
        }
        return event;
    }

    @Override
    public void handle(Event event) {
        if (visibility() != Visibility.VISIBLE || !enabled()) return;
        boolean hoverChanged = event instanceof PointerEnteredEvent entered && entered.phase() == EventPhase.TARGET
                || event instanceof PointerExitedEvent exited && exited.phase() == EventPhase.TARGET;
        super.handle(event);
        if (hoverChanged) {
            applyInteractionTransition();
        }
        if (event.isCancelled()) return;
        if (event instanceof PointerEvent pointerEvent && pointerEvent.phase() == EventPhase.CAPTURE) return;

        if (event instanceof PointerPressedEvent pointer && pointer.button() == PointerButton.PRIMARY) {
            setPressed(true);
            event.cancel();
        } else if (event instanceof PointerReleasedEvent pointer && pointer.button() == PointerButton.PRIMARY) {
            boolean wasPressed = pressed;
            setPressed(false);
            if (wasPressed) {
                click();
                event.cancel();
            }
        }
    }

    @Override
    protected void renderContent(RenderContext context) {
        applyTheme();
        effectiveRenderer().render(new DrawScope(context, transform()), snapshot(context));
        super.renderContent(context);
    }

    protected ButtonRenderer effectiveRenderer() {
        return renderer == null ? WidgetsRender.button() : renderer;
    }

    protected ButtonState snapshot(RenderContext context) {
        return new ButtonState(
                ButtonRenderType.BUTTON,
                layoutBounds().x(),
                layoutBounds().y(),
                layoutBounds().width(),
                layoutBounds().height(),
                text,
                richText,
                TEXT_PADDING_X,
                TextEngine.measureLineWidth(context, richText),
                TextEngine.measureTextHeight(richText),
                textColor.copy(),
                pressed,
                hovered(),
                enabled(),
                false,
                0.0f,
                0.0f,
                0.0f,
                background().copy(),
                borderColor().copy());
    }

    private static String normalize(String text) {
        return text == null ? "" : text;
    }

    @Override
    protected void applyTheme() {
        super.applyTheme();
        textColor.set(styleValue(StyleKeys.TEXT_COLOR, textColor));
    }

    @Override
    protected WidgetState styleState() {
        if (!enabled()) return super.styleState();
        return pressed ? WidgetState.PRESSED : super.styleState();
    }

    private void setPressed(boolean pressed) {
        if (this.pressed == pressed) return;
        this.pressed = pressed;
        invalidate(InvalidationFlags.VISUAL);
        applyInteractionTransition();
    }

    private void applyInteractionTransition() {
        if (!interactionTransitions) return;
        float targetScale = pressed ? pressedScale : hovered() ? hoveredScale : normalScale;
        float targetOpacity = !enabled() ? disabledOpacity : pressed ? pressedOpacity : normalOpacity;
        animateScale(targetScale, targetScale, interactionTransition);
        animateOpacity(targetOpacity, interactionTransition);
    }

    private static float sanitizeScale(float value) {
        return Float.isFinite(value) ? Math.max(0.01f, value) : 1.0f;
    }

    private static float clamp01(float value) {
        if (!Float.isFinite(value)) return 1.0f;
        return Math.max(0.0f, Math.min(1.0f, value));
    }
}
