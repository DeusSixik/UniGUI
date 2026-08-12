package dev.sixik.unigui.widgets;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.core.UIContext;
import dev.sixik.unigui.api.event.DateChangedEvent;
import dev.sixik.unigui.api.event.Event;
import dev.sixik.unigui.api.event.EventListener;
import dev.sixik.unigui.api.event.EventSubscription;
import dev.sixik.unigui.api.event.FocusLostEvent;
import dev.sixik.unigui.api.event.TextInputEvent;
import dev.sixik.unigui.api.layout.Alignment;
import dev.sixik.unigui.api.layout.EdgeInsets;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.layout.LayoutConstraints;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.text.RichText;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.api.widget.skin.WidgetsRender;
import dev.sixik.unigui.impl.text.TextEngine;
import dev.sixik.unigui.impl.widget.WidgetBase;
import dev.sixik.unigui.widgets.render.ButtonRenderer;
import dev.sixik.unigui.widgets.render.DatePickerRenderer;
import dev.sixik.unigui.widgets.render.DatePickerState;
import dev.sixik.unigui.widgets.render.TextInputRenderer;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;

public final class DatePicker extends LinearBox {
    private static final float CALENDAR_WIDTH = 184.0f;
    private static final float DAY_CELL_WIDTH = 24.0f;
    private static final float DAY_CELL_HEIGHT = 20.0f;
    private static final float DAY_GAP = 2.0f;
    private static final int WEEK_ROWS = 6;
    private static final float DAYS_PANEL_WIDTH = 7.0f * DAY_CELL_WIDTH + 6.0f * DAY_GAP;
    private static final float DAYS_PANEL_HEIGHT = WEEK_ROWS * DAY_CELL_HEIGHT + (WEEK_ROWS - 1.0f) * DAY_GAP;
    private static final float CALENDAR_PANEL_HEIGHT = 20.0f + 4.0f + 14.0f + 4.0f + DAYS_PANEL_HEIGHT;
    private static final TextInputRenderer CENTERED_TEXT_INPUT_RENDERER = (draw, state) -> {
        float textOffset = Math.max(0.0f, state.viewportWidth() - state.measuredTextWidth()) * 0.5f;
        draw.pushClip(state.viewportX(), state.viewportY(), state.viewportWidth(), state.viewportHeight());
        try {
            if (state.focused() && state.hasSelection() && !state.showingPlaceholder()) {
                float selectionX = state.viewportX() + textOffset + state.prefixWidth(state.selectionStart()) - state.horizontalScrollPixels();
                float selectionWidth = Math.max(1.0f,
                        state.prefixWidth(state.selectionEnd()) - state.prefixWidth(state.selectionStart()));
                draw.rect(selectionX,
                        state.viewportY(),
                        selectionWidth,
                        state.viewportHeight(),
                        Paint.fill(state.caretColor()));
            }

            if (state.hasVisibleText()) {
                draw.text(state.richText(),
                        state.viewportX() + textOffset - state.horizontalScrollPixels(),
                        state.textY(),
                        Math.max(state.viewportWidth(), state.measuredTextWidth()),
                        state.textHeight(),
                        Paint.fill(state.showingPlaceholder() ? state.placeholderColor() : state.textColor()));
            }

            if (state.focused()) {
                float caretX = state.viewportX() + textOffset + state.prefixWidth(state.cursorIndex()) - state.horizontalScrollPixels();
                draw.rect(caretX,
                        state.viewportY(),
                        1.0f,
                        state.viewportHeight(),
                        Paint.fill(state.caretColor()));
            }
        } finally {
            draw.popClip();
        }
    };
    private static final ButtonRenderer COMPACT_CENTER_BUTTON_RENDERER = (draw, state) -> {
        if (!state.hasText()) return;
        TextEngine.draw(draw.context(), state.richText(),
                state.x(), state.y(), state.width(), state.height(),
                Paint.fill(state.textColor()), draw.transform(),
                Alignment.CENTER, Alignment.CENTER);
    };

    private final Button previous = new Button("<");
    private final DateField field = new DateField();
    private final Button next = new Button(">");
    private final Button calendarButton = new Button("v");
    private final Popup popup = new Popup();
    private final VBox calendarPanel = new VBox();
    private final HBox calendarHeader = new HBox();
    private final Button previousMonth = new Button("<");
    private final CenteredText monthLabel = new CenteredText("", Part.MONTH_LABEL);
    private final Button nextMonth = new Button(">");
    private final WrapPanel daysPanel = new WrapPanel();
    private OverlayLayer explicitOverlayLayer;
    private OverlayLayer attachedOverlayLayer;
    private DatePickerRenderer renderer;
    private LocalDate value = LocalDate.now();
    private YearMonth displayedMonth = YearMonth.from(value);
    private boolean syncing;

    public DatePicker() {
        super(Orientation.HORIZONTAL);
        spacing(3.0f);

        previous.layout(style -> style.size(22.0f, 22.0f).flexGrow(0).flexShrink(0.0f));
        field.layout(style -> style.size(92.0f, 22.0f).flexGrow(0).flexShrink(0.0f));
        next.layout(style -> style.size(22.0f, 22.0f).flexGrow(0).flexShrink(0.0f));
        calendarButton.layout(style -> style.size(22.0f, 22.0f).flexGrow(0).flexShrink(0.0f));

        addChild(previous);
        addChild(field);
        addChild(next);
        addChild(calendarButton);

        previous.onClick(event -> setValue(value.minusDays(1L), true, true));
        next.onClick(event -> setValue(value.plusDays(1L), true, true));
        calendarButton.onClick(event -> togglePopup());
        field.onTextChanged(event -> {
            if (!syncing) syncFromText(false);
        });

        buildCalendarPanel();
        popup.anchor(calendarButton);
        popup.content(calendarPanel);
        popup.padding(EdgeInsets.all(5.0f));
        popup.closeOnOutsideClick(true);

        syncField();
        rebuildCalendar();
    }

    public LocalDate value() {
        return value;
    }

    public DatePicker value(LocalDate value) {
        setValue(value, false, true);
        return this;
    }

    public TextField field() {
        return field;
    }

    public Popup popup() {
        return popup;
    }

    public DatePickerRenderer renderer() {
        return renderer;
    }

    public DatePicker renderer(DatePickerRenderer renderer) {
        if (this.renderer == renderer) return this;
        this.renderer = renderer;
        calendarPanel.invalidate(InvalidationFlags.VISUAL);
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public DatePicker useDefaultRenderer() {
        return renderer(null);
    }

    public EventSubscription onDateChanged(EventListener<? super DateChangedEvent> listener) {
        return on(DateChangedEvent.TYPE, listener);
    }

    public OverlayLayer overlayLayer() {
        return explicitOverlayLayer != null ? explicitOverlayLayer : findTopmostOverlayLayer();
    }

    public DatePicker overlayLayer(OverlayLayer overlayLayer) {
        if (explicitOverlayLayer == overlayLayer) return this;
        detachPopup();
        explicitOverlayLayer = overlayLayer;
        syncPopupAttachment();
        return this;
    }

    @Override
    public void setParentInternal(Widget parent) {
        super.setParentInternal(parent);
        syncPopupAttachment();
    }

    @Override
    public void setUiContextInternal(UIContext uiContext) {
        super.setUiContextInternal(uiContext);
        syncPopupAttachment();
    }

    private void togglePopup() {
        syncPopupAttachment();
        if (attachedOverlayLayer == null) return;
        popup.toggle();
    }

    private void syncPopupAttachment() {
        OverlayLayer target = overlayLayer();
        if (target == null) {
            detachPopup();
            return;
        }
        if (attachedOverlayLayer == target) return;
        detachPopup();
        attachedOverlayLayer = target;
        attachedOverlayLayer.addOverlay(popup);
    }

    private void detachPopup() {
        if (attachedOverlayLayer != null) {
            attachedOverlayLayer.removeOverlay(popup);
            attachedOverlayLayer = null;
        }
    }

    private OverlayLayer findTopmostOverlayLayer() {
        OverlayLayer result = null;
        Widget current = this;
        while (current != null) {
            if (current instanceof OverlayLayer layer) {
                result = layer;
            }
            current = current.parent();
        }
        return result;
    }

    private void syncField() {
        syncing = true;
        try {
            field.text(value.toString());
        } finally {
            syncing = false;
        }
    }

    private void syncFromText(boolean normalize) {
        String text = field.text();
        if (text.length() < 10) return;
        try {
            LocalDate parsed = LocalDate.parse(text);
            setValue(parsed, true, normalize);
        } catch (DateTimeParseException ignored) {
            if (normalize) syncField();
        }
    }

    private void setValue(LocalDate value, boolean emitChange, boolean syncField) {
        LocalDate normalized = value == null ? LocalDate.now() : value;
        if (this.value.equals(normalized)) {
            YearMonth normalizedMonth = YearMonth.from(normalized);
            if (!displayedMonth.equals(normalizedMonth)) {
                displayedMonth = normalizedMonth;
                rebuildCalendar();
                invalidate(InvalidationFlags.VISUAL);
            }
            if (syncField) {
                syncField();
            }
            return;
        }

        LocalDate oldValue = this.value;
        this.value = normalized;
        this.displayedMonth = YearMonth.from(this.value);
        if (syncField) {
            syncField();
        }
        rebuildCalendar();
        invalidate(InvalidationFlags.VISUAL);
        if (emitChange) {
            dispatchDateChanged(oldValue, this.value);
        }
    }

    private DateChangedEvent dispatchDateChanged(LocalDate oldValue, LocalDate newValue) {
        DateChangedEvent event = new DateChangedEvent(this, oldValue, newValue);
        UIContext context = uiContext();
        if (context == null) {
            emit(event);
        } else {
            context.routedEvents().dispatch(event);
        }
        return event;
    }

    private DatePickerRenderer effectiveRenderer() {
        return renderer == null ? styleRenderer(DatePickerRenderer.class, WidgetsRender.datePicker()) : renderer;
    }

    private void buildCalendarPanel() {
        calendarPanel.spacing(4.0f);
        calendarPanel.layout(style -> style.size(CALENDAR_WIDTH, CALENDAR_PANEL_HEIGHT).flexGrow(0).flexShrink(0.0f));

        previousMonth.layout(style -> style.size(24.0f, 20.0f).flexGrow(0).flexShrink(0.0f));
        nextMonth.layout(style -> style.size(24.0f, 20.0f).flexGrow(0).flexShrink(0.0f));
        monthLabel.layout(style -> style.size(LayoutConstraints.AUTO, 20.0f).flexGrow(1).flexShrink(1.0f));
        previousMonth.onClick(event -> {
            displayedMonth = displayedMonth.minusMonths(1L);
            rebuildCalendar();
        });
        nextMonth.onClick(event -> {
            displayedMonth = displayedMonth.plusMonths(1L);
            rebuildCalendar();
        });

        calendarHeader.spacing(4.0f);
        calendarHeader.layout(style -> style.flexGrow(0).flexShrink(0.0f));
        calendarHeader.addChild(previousMonth);
        calendarHeader.addChild(monthLabel);
        calendarHeader.addChild(nextMonth);

        HBox week = new HBox();
        week.spacing(2.0f);
        week.layout(style -> style.flexGrow(0).flexShrink(0.0f));
        for (String day : List.of("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su")) {
            CenteredText label = new CenteredText(day, Part.WEEKDAY_LABEL);
            label.layout(style -> style.size(DAY_CELL_WIDTH, 14.0f).flexGrow(0).flexShrink(0.0f));
            week.addChild(label);
        }

        daysPanel.spacing(2.0f);
        daysPanel.lineSpacing(2.0f);
        daysPanel.layout(style -> style.size(DAYS_PANEL_WIDTH, DAYS_PANEL_HEIGHT).flexGrow(0).flexShrink(0.0f));

        calendarPanel.addChild(calendarHeader);
        calendarPanel.addChild(week);
        calendarPanel.addChild(daysPanel);
    }

    private void rebuildCalendar() {
        monthLabel.text(displayedMonth.toString());
        daysPanel.clearChildren();

        LocalDate first = displayedMonth.atDay(1);
        int leading = first.getDayOfWeek().getValue() - DayOfWeek.MONDAY.getValue();
        for (int i = 0; i < leading; i++) {
            Label empty = new Label("");
            empty.layout(style -> style.size(DAY_CELL_WIDTH, DAY_CELL_HEIGHT).flexGrow(0).flexShrink(0.0f));
            daysPanel.addChild(empty);
        }

        for (int day = 1; day <= displayedMonth.lengthOfMonth(); day++) {
            LocalDate date = displayedMonth.atDay(day);
            ToggleButton button = new ToggleButton(Integer.toString(day));
            button.renderer(COMPACT_CENTER_BUTTON_RENDERER);
            button.layout(style -> style.size(DAY_CELL_WIDTH, DAY_CELL_HEIGHT).flexGrow(0).flexShrink(0.0f));
            button.silentChecked(date.equals(value));
            button.onClick(event -> {
                setValue(date, true, true);
                popup.close();
            });
            daysPanel.addChild(button);
        }
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
    }

    private final class DateField extends TextField {
        private DateField() {
            placeholder("YYYY-MM-DD");
            maxLength(10);
            renderer(CENTERED_TEXT_INPUT_RENDERER);
        }

        @Override
        public void handle(Event event) {
            if (event instanceof TextInputEvent input && !isAllowed(input.codePoint())) {
                event.cancel();
                return;
            }
            super.handle(event);
            if (event instanceof FocusLostEvent) {
                syncFromText(true);
            }
        }

        @Override
        protected String sanitizeTextInput(String text) {
            StringBuilder builder = new StringBuilder(text.length());
            text.codePoints()
                    .filter(DateField::isAllowed)
                    .limit(10)
                    .forEach(builder::appendCodePoint);
            return builder.toString();
        }

        private static boolean isAllowed(int codePoint) {
            return (codePoint >= '0' && codePoint <= '9') || codePoint == '-';
        }
    }

    public enum Part {
        LABEL,
        MONTH_LABEL,
        WEEKDAY_LABEL
    }

    private final class CenteredText extends WidgetBase {
        private final MutableColor color = new MutableColor(1.0f, 1.0f, 1.0f, 1.0f);
        private final Part part;
        private String text;

        private CenteredText(String text, Part part) {
            this.text = text == null ? "" : text;
            this.part = part == null ? Part.LABEL : part;
            color.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
        }

        private void text(String text) {
            String normalized = text == null ? "" : text;
            if (this.text.equals(normalized)) return;
            this.text = normalized;
            invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        }

        @Override
        public void measure(LayoutContext context) {
            if (visibility() == dev.sixik.unigui.api.widget.Visibility.COLLAPSED) {
                setDesiredSize(0.0f, 0.0f);
                return;
            }
            setDesiredSize(resolveDesiredSize(context,
                    TextEngine.measureLineWidth(text),
                    TextEngine.LINE_HEIGHT));
        }

        @Override
        public void render(RenderContext context) {
            if (text.isEmpty()) return;
            effectiveRenderer().render(new DrawScope(context, transform()), snapshot());
        }

        private DatePickerState snapshot() {
            return new DatePickerState(
                    layoutBounds().x(),
                    layoutBounds().y(),
                    layoutBounds().width(),
                    layoutBounds().height(),
                    part,
                    text,
                    value,
                    displayedMonth,
                    hovered(),
                    enabled(),
                    color.copy());
        }
    }
}