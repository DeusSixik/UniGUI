package dev.sixik.unigui.widgets.interaction;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.docs.*;
import dev.sixik.unigui.api.event.*;
import dev.sixik.unigui.api.input.KeyCodes;
import dev.sixik.unigui.api.style.Style;
import dev.sixik.unigui.api.style.StyleAnimationIds;
import dev.sixik.unigui.api.style.StyleIds;
import dev.sixik.unigui.api.utils.reactive.*;
import dev.sixik.unigui.api.xml.XmlAttribute;
import dev.sixik.unigui.api.xml.XmlWidgetName;
import dev.sixik.unigui.widgets.WidgetStyles;

import java.util.Locale;

@AiDoc(
        summary = "Numeric input field with validation, step adjustment, and min/max limits.",
        purpose = "Accepts integer and decimal numbers with drag and arrow key adjustment.",
        useWhen = {"Inputting coordinates, quantities, dimensions, volume levels."},
        related = {"TextInput", "Slider"}
)
@WikiPage(
        title = "<key:number.field.title>",
        slug = "number-field",
        category = "<key:core.widgets.interaction.category>",
        summary = "<key:number.field.summary>",
        order = 130
)
@WikiSection(
        title = "<key:number.field.section.overview.title>",
        markdown = "<key:number.field.section.overview.body>",
        order = 10
)
@WikiExample(
        title = "<key:number.field.example.basic.title>",
        code = "<key:number.field.example.basic.code>",
        order = 10
)
@WikiLink(label = "TextInput", target = "page:text-input")
@WikiLink(label = "Slider", target = "page:slider")
@XmlWidgetName("NumberField")
public class NumberField extends TextInput {
    public static final String STYLE_TYPE = StyleIds.Widget.NUMBER_FIELD;

    public static final class AnimationEvents {
        public static final String ON_VALUE_CHANGED = StyleAnimationIds.Event.ON_VALUE_CHANGED;
        public static final String ON_TEXT_CHANGED = StyleAnimationIds.Event.ON_TEXT_CHANGED;
        public static final java.util.List<String> ALL = java.util.List.of(ON_VALUE_CHANGED, ON_TEXT_CHANGED);

        private AnimationEvents() {
        }
    }

    private double min = -Double.MAX_VALUE;
    private double max = Double.MAX_VALUE;
    private double value;
    private double step = 1.0d;

    public NumberField() {
        applyComponentStyleDefaults(WidgetStyles.TextInput.DEFAULT_CHROME);
        text("0");
    }

    /**
     * Возвращает defaults chrome для семейства стандартных текстовых полей.
     *
     * @return стандартный style preset оформления поля
     */
    @Override
    protected Style defaultStyle() {
        return WidgetStyles.TextInput.DEFAULT_CHROME;
    }

    public double value() {
        return value;
    }

    @XmlAttribute(value = "value", category = "Behavior", defaultValue = "0", description = "Numeric value clamped to min/max.")
    public NumberField value(double value) {
        setNumberValue(value, true);
        return this;
    }

    @XmlAttribute(value = "min", category = "Behavior", defaultValue = "-Double.MAX_VALUE", description = "Minimum allowed numeric value.")
    public NumberField min(double min) {
        return range(min, max);
    }

    @XmlAttribute(value = "max", category = "Behavior", defaultValue = "Double.MAX_VALUE", description = "Maximum allowed numeric value.")
    public NumberField max(double max) {
        return range(min, max);
    }

    public NumberField silentValue(double value) {
        setNumberValue(value, false);
        return this;
    }

    @WikiMethod(order = 20, title = "<key:number.field.method.range.title>", summary = "<key:number.field.method.range.summary>")
    @WikiReturn("<key:widget.base.method.fluent.return>")
    public NumberField range(@WikiParameter(description = "<key:number.field.param.min>") double min,
                             @WikiParameter(description = "<key:number.field.param.max>") double max) {
        if (max < min) {
            double swap = min;
            min = max;
            max = swap;
        }
        this.min = min;
        this.max = max;
        setNumberValue(value, false);
        return this;
    }

    @XmlAttribute(value = "step", category = "Behavior", defaultValue = "1", description = "Keyboard nudge increment.")
    public NumberField step(double step) {
        this.step = Math.max(0.0d, step);
        return this;
    }

    @Override
    @XmlAttribute(value = "textPaddingX", category = "Layout", defaultValue = "12", description = "Symmetric inset between both edges and editable text.")
    public NumberField textPaddingX(float textPaddingX) {
        super.textPaddingX(textPaddingX);
        return this;
    }

    @Override
    @XmlAttribute(value = "textPaddingLeft", category = "Layout", defaultValue = "12", description = "Inset between the left edge and editable text.")
    public NumberField textPaddingLeft(float textPaddingLeft) {
        super.textPaddingLeft(textPaddingLeft);
        return this;
    }

    @Override
    @XmlAttribute(value = "textPaddingRight", category = "Layout", defaultValue = "12", description = "Inset between editable text and the right edge.")
    public NumberField textPaddingRight(float textPaddingRight) {
        super.textPaddingRight(textPaddingRight);
        return this;
    }

    public EventSubscription onValueChanged(EventListener<? super NumberValueChangedEvent> listener) {
        return on(NumberValueChangedEvent.TYPE, listener);
    }

    @Override
    protected <T> SignalSubscription createBinding(BindingData<T> key, T source) {
        if (key == BindingKeys.NUMBER_FIELD_VALUE) {
            DoubleSignal value = BindingKeys.NUMBER_FIELD_VALUE.cast(source);
            SignalSubscription signalSubscription = value.subscribe(this::value);
            EventSubscription widgetSubscription = onValueChanged(event -> value.set(event.newValue()));
            return () -> {
                widgetSubscription.close();
                signalSubscription.close();
            };
        }
        return super.createBinding(key, source);
    }

    @Override
    protected String sanitizeTextInput(String text) {
        StringBuilder builder = new StringBuilder(text.length());
        text.codePoints()
                .filter(NumberField::isAllowed)
                .forEach(builder::appendCodePoint);
        return builder.toString();
    }

    @Override
    public void handle(Event event) {
        if (event instanceof TextInputEvent input && !isAllowed(input.codePoint())) {
            event.cancel();
            return;
        }

        super.handle(event);
        if (event.isCancelled()) {
            syncValueFromText();
            return;
        }

        if (event instanceof KeyPressedEvent key && focused()) {
            if (key.keyCode() == KeyCodes.UP) {
                setNumberValue(value + stepOrDefault(), true);
                event.cancel();
            } else if (key.keyCode() == KeyCodes.DOWN) {
                setNumberValue(value - stepOrDefault(), true);
                event.cancel();
            }
        }
    }

    private void syncValueFromText() {
        String text = text();
        if (text.isEmpty() || text.equals("-") || text.equals(".") || text.equals("-.")) return;
        try {
            setNumberValue(Double.parseDouble(text), true);
        } catch (NumberFormatException ignored) {
            // Keep editing text until it becomes a parseable number.
        }
    }

    private void setNumberValue(double value, boolean emitChange) {
        double normalized = clamp(value, min, max);
        if (Double.compare(this.value, normalized) == 0) {
            syncTextToValue(normalized);
            return;
        }
        double oldValue = this.value;
        this.value = normalized;
        syncTextToValue(normalized);
        invalidate(InvalidationFlags.VISUAL);
        if (emitChange) {
            emit(new NumberValueChangedEvent(this, oldValue, normalized));
        }
    }

    private void syncTextToValue(double value) {
        String rendered = renderNumber(value);
        if (!text().equals(rendered)) {
            super.text(rendered);
            cursorIndex(text().length());
        }
    }

    private double stepOrDefault() {
        return step > 0.0d ? step : 1.0d;
    }

    private static boolean isAllowed(int codePoint) {
        return (codePoint >= '0' && codePoint <= '9') || codePoint == '-' || codePoint == '.';
    }

    private static String renderNumber(double value) {
        if (value == Math.rint(value)) {
            return Long.toString((long) value);
        }
        return String.format(Locale.ROOT, "%s", value);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
