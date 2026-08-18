package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.layout.Alignment;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.text.RichText;
import dev.sixik.unigui.impl.text.TextEngine;

public final class DatePickerRenderers {
    public static final DatePickerRenderer DEFAULT = (draw, state) -> {
        if (state.text().isEmpty()) return;
        TextEngine.draw(draw.context(), RichText.resolve(state.text()),
                state.x(), state.y(), state.width(), state.height(),
                Paint.fill(state.textColor()), draw.transform(),
                Alignment.CENTER, Alignment.CENTER);
    };

    private DatePickerRenderers() {
    }
}

