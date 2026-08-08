package dev.sixik.unigui.widgets;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.event.CheckedChangedEvent;
import dev.sixik.unigui.api.event.Event;
import dev.sixik.unigui.api.event.EventListener;
import dev.sixik.unigui.api.event.EventPhase;
import dev.sixik.unigui.api.event.EventSubscription;
import dev.sixik.unigui.api.event.KeyPressedEvent;
import dev.sixik.unigui.api.input.KeyCodes;
import dev.sixik.unigui.api.layout.Alignment;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.style.StyleKeys;
import dev.sixik.unigui.api.style.WidgetState;
import dev.sixik.unigui.api.text.RichText;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.impl.text.TextEngine;

import java.util.Objects;

public class RadioButton extends Button {
    private static final float OUTER_SIZE = 12.0f;
    private static final float INNER_SIZE = 6.0f;

    private final MutableColor checkedColor = new MutableColor(0.30f, 0.62f, 0.95f, 1.0f);
    private String value;
    private RadioGroup group;
    private boolean checked;

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
        float textWidth = text().isEmpty() ? 0.0f : 4.0f + TextEngine.measureLineWidth(richText());
        setDesiredSize(resolveDesiredSize(context, OUTER_SIZE + textWidth, DEFAULT_HEIGHT));
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

        float x = layoutBounds().x();
        float y = layoutBounds().y() + Math.max(0.0f, layoutBounds().height() - OUTER_SIZE) * 0.5f;
        context.circle(x, y, OUTER_SIZE, OUTER_SIZE, Paint.stroke(checked ? checkedColor : borderColor(), 1.0f), transform());
        if (checked) {
            float innerOffset = (OUTER_SIZE - INNER_SIZE) * 0.5f;
            context.circle(x + innerOffset, y + innerOffset, INNER_SIZE, INNER_SIZE, Paint.fill(checkedColor), transform());
        }

        if (!text().isEmpty()) {
            TextEngine.draw(context,
                    richText(),
                    x + OUTER_SIZE + 4.0f,
                    layoutBounds().y(),
                    Math.max(0.0f, layoutBounds().width() - OUTER_SIZE - 4.0f),
                    layoutBounds().height(),
                    Paint.fill(textColor()),
                    transform(),
                    Alignment.START,
                    Alignment.CENTER);
        }

        renderChildren(context);
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
        invalidate(InvalidationFlags.VISUAL);
        if (emitChange) {
            emit(new CheckedChangedEvent(this, oldValue, checked));
        }
    }

    private boolean isFocused() {
        return uiContext() != null && uiContext().focusManager().isFocused(this);
    }

    private static String normalize(String value) {
        return value == null ? "" : value;
    }
}
