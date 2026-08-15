package dev.sixik.unigui.widgets.interaction;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.event.ButtonClickEvent;
import dev.sixik.unigui.api.event.CheckedChangedEvent;
import dev.sixik.unigui.api.event.EventListener;
import dev.sixik.unigui.api.event.EventSubscription;
import dev.sixik.unigui.api.event.KeyPressedEvent;
import dev.sixik.unigui.api.input.KeyCodes;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.style.StyleKeys;
import dev.sixik.unigui.api.style.WidgetState;
import dev.sixik.unigui.api.text.RichText;
import dev.sixik.unigui.api.widget.skin.WidgetsRender;
import dev.sixik.unigui.impl.text.TextEngine;
import dev.sixik.unigui.widgets.render.ButtonRenderType;
import dev.sixik.unigui.widgets.render.ButtonRenderer;
import dev.sixik.unigui.widgets.render.ButtonState;

public class ToggleButton extends Button {
    private final MutableColor checkedBackground = new MutableColor(0.18f, 0.45f, 0.75f, 1.0f);
    private final MutableColor uncheckedBackground = new MutableColor(0.12f, 0.12f, 0.12f, 1.0f);
    private boolean checked;

    public ToggleButton() {
        this("");
    }

    public ToggleButton(String text) {
        super(text);
        background().set(uncheckedBackground);
        checkedBackground.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
        uncheckedBackground.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
        onClick(this::toggleOnClick);
    }

    public ToggleButton(RichText text) {
        this("");
        richText(text);
    }

    public boolean checked() {
        return checked;
    }

    public ToggleButton checked(boolean checked) {
        setChecked(checked, true);
        return this;
    }

    public ToggleButton silentChecked(boolean checked) {
        setChecked(checked, false);
        return this;
    }

    public MutableColor checkedBackground() {
        return checkedBackground;
    }

    public MutableColor uncheckedBackground() {
        return uncheckedBackground;
    }

    public EventSubscription onCheckedChanged(EventListener<? super CheckedChangedEvent> listener) {
        return on(CheckedChangedEvent.TYPE, listener);
    }

    @Override
    protected void applyTheme() {
        checkedBackground.set(styleValue(StyleKeys.BACKGROUND_COLOR, WidgetState.CHECKED, checkedBackground));
        uncheckedBackground.set(styleValue(StyleKeys.BACKGROUND_COLOR, WidgetState.NORMAL, uncheckedBackground));
        super.applyTheme();
    }

    @Override
    protected WidgetState styleState() {
        if (!enabled()) return super.styleState();
        if (pressed()) return WidgetState.PRESSED;
        return checked ? WidgetState.CHECKED : super.styleState();
    }

    @Override
    protected ButtonRenderer effectiveRenderer() {
        return renderer() == null ? styleRenderer(ButtonRenderer.class, WidgetsRender.toggleButton()) : renderer();
    }

    @Override
    protected ButtonState snapshot(RenderContext context) {
        return new ButtonState(
                ButtonRenderType.TOGGLE_BUTTON,
                layoutBounds().x(),
                layoutBounds().y(),
                layoutBounds().width(),
                layoutBounds().height(),
                text(),
                richText(),
                textPaddingX(),
                TextEngine.measureLineWidth(context, richText()),
                TextEngine.measureTextHeight(richText()),
                textColor().copy(),
                pressed(),
                hovered(),
                enabled(),
                checked,
                false,
                0.0f,
                0.0f,
                0.0f,
                checkedBackground.copy(),
                uncheckedBackground.copy());
    }

    @Override
    public void handle(dev.sixik.unigui.api.event.Event event) {
        super.handle(event);
        if (event.isCancelled()) return;
        if (event instanceof KeyPressedEvent key && key.phase() == dev.sixik.unigui.api.event.EventPhase.TARGET
                && uiContext() != null && uiContext().focusManager().isFocused(this)) {
            if (key.keyCode() == KeyCodes.SPACE || key.keyCode() == KeyCodes.ENTER || key.keyCode() == KeyCodes.KEYPAD_ENTER) {
                checked(!checked);
                event.cancel();
            }
        }
    }

    private void toggleOnClick(ButtonClickEvent ignored) {
        checked(!checked);
    }

    private void setChecked(boolean checked, boolean emitChange) {
        if (this.checked == checked) return;
        boolean oldValue = this.checked;
        this.checked = checked;
        background().set(checked ? checkedBackground : uncheckedBackground);
        invalidate(InvalidationFlags.VISUAL);
        if (emitChange) {
            emit(new CheckedChangedEvent(this, oldValue, checked));
        }
    }
}
