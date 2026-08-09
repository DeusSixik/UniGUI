package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.widgets.DatePicker;

import java.time.LocalDate;
import java.time.YearMonth;

public record DatePickerState(
        float x,
        float y,
        float width,
        float height,
        DatePicker.Part part,
        String text,
        LocalDate value,
        YearMonth displayedMonth,
        boolean hovered,
        boolean enabled,
        ColorView textColor
) {
    public DatePickerState {
        part = part == null ? DatePicker.Part.LABEL : part;
        text = text == null ? "" : text;
    }
}

