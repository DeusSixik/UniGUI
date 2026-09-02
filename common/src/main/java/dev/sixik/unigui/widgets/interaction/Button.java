package dev.sixik.unigui.widgets.interaction;

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
import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.text.RichText;
import dev.sixik.unigui.api.style.StyleKeys;
import dev.sixik.unigui.api.style.WidgetState;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.widget.skin.WidgetsRender;
import dev.sixik.unigui.api.xml.XmlAttribute;
import dev.sixik.unigui.api.xml.XmlWidgetName;
import dev.sixik.unigui.impl.text.TextEngine;
import dev.sixik.unigui.api.style.StyleAnimationIds;
import dev.sixik.unigui.api.style.StyleIds;
import dev.sixik.unigui.widgets.render.ButtonRenderer;
import dev.sixik.unigui.widgets.render.ButtonRenderType;
import dev.sixik.unigui.widgets.render.ButtonState;

import java.util.Objects;
import dev.sixik.unigui.widgets.containers.Box;

@XmlWidgetName("Button")
public class Button extends Box {
    public static final String STYLE_TYPE = StyleIds.Widget.BUTTON;

    public static final class StyleProperties {
        public static final String BACKGROUND_COLOR = StyleIds.Key.BACKGROUND_COLOR;
        public static final String BORDER_COLOR = StyleIds.Key.BORDER_COLOR;
        public static final String BORDER_WIDTH = StyleIds.Key.BORDER_WIDTH;
        public static final String RADIUS = StyleIds.Key.RADIUS;
        public static final String TEXT_COLOR = StyleIds.Key.TEXT_COLOR;
        public static final String TRANSITION_DURATION = StyleIds.Key.TRANSITION_DURATION;
        public static final String TRANSITION_EASING = StyleIds.Key.TRANSITION_EASING;

        private StyleProperties() {
        }
    }

    public static final class AnimationProperties {
        public static final String TEXT_COLOR = StyleAnimationIds.Property.TEXT_COLOR;
        public static final String BACKGROUND_COLOR = StyleAnimationIds.Property.BACKGROUND_COLOR;
        public static final String BORDER_COLOR = StyleAnimationIds.Property.BORDER_COLOR;
        public static final String BORDER_WIDTH = StyleAnimationIds.Property.BORDER_WIDTH;
        public static final String RADIUS = StyleAnimationIds.Property.RADIUS;
        public static final String OPACITY = StyleAnimationIds.Property.OPACITY;
        public static final String SCALE = StyleAnimationIds.Property.SCALE;
        public static final String ROTATION_DEGREES = StyleAnimationIds.Property.ROTATION_DEGREES;
        public static final java.util.List<String> ALL = StyleAnimationIds.Property.BUTTON;

        private AnimationProperties() {
        }
    }

    public static final class AnimationEvents {
        public static final String ON_CLICK = StyleAnimationIds.Event.ON_CLICK;
        public static final String ON_FOCUS = StyleAnimationIds.Event.ON_FOCUS;
        public static final String ON_BLUR = StyleAnimationIds.Event.ON_BLUR;
        public static final String ON_HOVER = StyleAnimationIds.Event.ON_HOVER;
        public static final String ON_HOVER_ENTER = StyleAnimationIds.Event.ON_HOVER_ENTER;
        public static final String ON_HOVER_EXIT = StyleAnimationIds.Event.ON_HOVER_EXIT;
        public static final String ON_PRESS = StyleAnimationIds.Event.ON_PRESS;
        public static final String ON_RELEASE = StyleAnimationIds.Event.ON_RELEASE;
        public static final java.util.List<String> ALL = StyleAnimationIds.Event.BUTTON;

        private AnimationEvents() {
        }
    }

    protected static final float DEFAULT_TEXT_PADDING_X = 8.0f;
    protected static final float TEXT_PADDING_X = DEFAULT_TEXT_PADDING_X;
    protected static final float DEFAULT_HEIGHT = 18.0f;
    protected static final float DEFAULT_TEXT_PADDING_Y = (DEFAULT_HEIGHT - TextEngine.LINE_HEIGHT) * 0.5f;
    protected static final float APPROX_CHAR_WIDTH = TextEngine.APPROX_CHAR_WIDTH;

    private String text = "";
    private RichText richText = RichText.plain("");
    private final MutableColor textColor = new MutableColor(1.0f, 1.0f, 1.0f, 1.0f);
    private ButtonRenderer renderer;
    private int pressedPointerId = -1;
    private float textPaddingX = DEFAULT_TEXT_PADDING_X;
    private float textPaddingY = DEFAULT_TEXT_PADDING_Y;
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
        boxVisualEnabled(false);
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

    @XmlAttribute(value = "text", category = "Content", defaultValue = "", description = "Button label text.")
    public Button text(String text) {
        String normalized = normalize(text);
        RichText normalizedRichText = RichText.resolve(normalized);
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

    public Button animateTextColor(ColorView color, float durationSeconds) {
        animateColor(textColor, color, durationSeconds);
        return this;
    }

    public Button animateTextColor(ColorView color, TransitionSpec spec) {
        animateColor(textColor, color, spec);
        return this;
    }

    public float textPaddingX() {
        return textPaddingX;
    }

    public Button textPadding(float horizontal, float vertical) {
        return textPaddingX(horizontal).textPaddingY(vertical);
    }

    @XmlAttribute(value = "textPaddingX", category = "Layout", defaultValue = "8", description = "Horizontal padding around button text.")
    public Button textPaddingX(float textPaddingX) {
        float normalized = Float.isFinite(textPaddingX) ? Math.max(0.0f, textPaddingX) : DEFAULT_TEXT_PADDING_X;
        if (this.textPaddingX == normalized) return this;
        this.textPaddingX = normalized;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public float textPaddingY() {
        return textPaddingY;
    }

    @XmlAttribute(value = "textPaddingY", category = "Layout", defaultValue = "4", description = "Vertical padding around button text.")
    public Button textPaddingY(float textPaddingY) {
        float normalized = Float.isFinite(textPaddingY) ? Math.max(0.0f, textPaddingY) : DEFAULT_TEXT_PADDING_Y;
        if (this.textPaddingY == normalized) return this;
        this.textPaddingY = normalized;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
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

    @XmlAttribute(value = "interactionTransitions", category = "Behavior", defaultValue = "false", description = "Enables built-in hover/press transition animation.")
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
    @XmlAttribute(value = "enabled", category = "Behavior", defaultValue = "true", description = "Whether the widget can be interacted with.")
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
        float textWidth = TextEngine.measureLineWidth(richText);
        float textHeight = intrinsicTextHeight();
        setDesiredSize(resolveDesiredSize(context,
                textWidth + textPaddingX * 2.0f,
                textHeight + textPaddingY * 2.0f));
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
        if (event instanceof PointerEvent pointerEvent && pointerEvent.phase() == EventPhase.CAPTURE) return;

        if (event instanceof PointerReleasedEvent pointer && pointer.button() == PointerButton.PRIMARY && ownsPressedPointer(pointer)) {
            boolean wasPressed = pressed;
            boolean releasedInside = pointerInside(pointer);
            releasePressedPointer(pointer.pointerId());
            if (wasPressed && releasedInside && !event.isCancelled()) {
                click();
            }
            if (wasPressed) {
                event.cancel();
            }
            return;
        }

        if (event instanceof PointerExitedEvent pointer && ownsPressedPointer(pointer)) {
            releasePressedPointer(pointer.pointerId());
        }

        if (event.isCancelled()) return;

        if (event instanceof PointerPressedEvent pointer && pointer.button() == PointerButton.PRIMARY) {
            pressPointer(pointer.pointerId());
            event.cancel();
        }
    }

    @Override
    protected void renderContent(RenderContext context) {
        applyTheme();
        renderButtonVisual(context, snapshot(context));
        super.renderContent(context);
    }

    protected void renderButtonVisual(RenderContext context, ButtonState state) {
        DrawScope draw = new DrawScope(context, transform(), layoutBounds());
        if (renderer != null) {
            renderer.render(draw, state);
            return;
        }
        ButtonRenderer styled = styleRendererOverride(ButtonRenderer.class);
        if (styled != null) {
            styled.render(draw, state);
            return;
        }
        if (renderStylePlan(context, ButtonState.class, state)) return;
        defaultRenderer().render(draw, state);
    }

    protected ButtonRenderer defaultRenderer() {
        return WidgetsRender.button();
    }

    protected ButtonRenderer effectiveRenderer() {
        return renderer == null ? styleRenderer(ButtonRenderer.class, defaultRenderer()) : renderer;
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
                textPaddingX,
                TextEngine.measureLineWidth(context, richText),
                TextEngine.measureTextHeight(context, richText),
                textColor.copy(),
                pressed,
                hovered(),
                enabled(),
                false,
                false,
                0.0f,
                0.0f,
                0.0f,
                background().copy(),
                borderColor().copy(),
                0.0f,
                false,
                backgroundVisible(),
                background().copy(),
                radius(),
                borderVisible(),
                borderColor().copy(),
                borderWidth());
    }

    private static String normalize(String text) {
        return text == null ? "" : text;
    }

    protected float intrinsicTextHeight() {
        float measured = TextEngine.measureTextHeight(richText);
        return measured > 0.0f ? measured : TextEngine.LINE_HEIGHT;
    }

    @Override
    protected void applyTheme() {
        super.applyTheme();
        ColorView themedText = styleValue(StyleKeys.TEXT_COLOR, styleState(), textColor);
        if (themedText != null) {
            animateColor(textColor, themedText, styleTransition());
        }
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

    private void pressPointer(int pointerId) {
        pressedPointerId = pointerId;
        UIContext context = uiContext();
        if (context != null) {
            context.capturePointer(pointerId, this);
        }
        setPressed(true);
    }

    private void releasePressedPointer(int pointerId) {
        UIContext context = uiContext();
        if (context != null) {
            context.releasePointer(pointerId, this);
        }
        pressedPointerId = -1;
        setPressed(false);
    }

    private boolean ownsPressedPointer(PointerEvent pointer) {
        return pressed && pointer != null && (pressedPointerId < 0 || pressedPointerId == pointer.pointerId());
    }

    private boolean pointerInside(PointerEvent pointer) {
        if (pointer == null) return false;
        return pointer.localX() >= 0.0f
                && pointer.localY() >= 0.0f
                && pointer.localX() <= layoutBounds().width()
                && pointer.localY() <= layoutBounds().height();
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
