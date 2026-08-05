package dev.sixik.unigui.widgets;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.core.UIContext;
import dev.sixik.unigui.api.event.ButtonClickEvent;
import dev.sixik.unigui.api.event.EventPhase;
import dev.sixik.unigui.api.event.Event;
import dev.sixik.unigui.api.event.EventListener;
import dev.sixik.unigui.api.event.EventSubscription;
import dev.sixik.unigui.api.event.PointerEvent;
import dev.sixik.unigui.api.event.PointerPressedEvent;
import dev.sixik.unigui.api.event.PointerReleasedEvent;
import dev.sixik.unigui.api.input.PointerButton;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.render.RenderContext;

import java.util.Objects;

public class Button extends Box {
    private String text = "";
    private final MutableColor textColor = new MutableColor(1.0f, 1.0f, 1.0f, 1.0f);
    private boolean pressed;

    public Button() {
        backgroundVisible(true);
        borderVisible(true);
        textColor.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
    }

    public Button(String text) {
        this();
        this.text = normalize(text);
    }

    public String text() {
        return text;
    }

    public Button text(String text) {
        String normalized = normalize(text);
        if (Objects.equals(this.text, normalized)) return this;
        this.text = normalized;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public MutableColor textColor() {
        return textColor;
    }

    public boolean pressed() {
        return pressed;
    }

    public EventSubscription onClick(EventListener<? super ButtonClickEvent> listener) {
        return on(ButtonClickEvent.TYPE, listener);
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
        super.handle(event);
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
        if (!text.isEmpty()) {
            context.text(text,
                    layoutBounds().x(),
                    layoutBounds().y(),
                    layoutBounds().width(),
                    layoutBounds().height(),
                    Paint.fill(textColor),
                    transform());
        }
        super.renderContent(context);
    }

    private static String normalize(String text) {
        return text == null ? "" : text;
    }

    private void setPressed(boolean pressed) {
        if (this.pressed == pressed) return;
        this.pressed = pressed;
        invalidate(InvalidationFlags.VISUAL);
    }
}
