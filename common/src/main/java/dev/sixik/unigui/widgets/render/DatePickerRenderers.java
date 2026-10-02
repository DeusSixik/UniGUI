package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.layout.Alignment;
import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.text.RichText;
import dev.sixik.unigui.api.widget.render.WidgetRender;
import dev.sixik.unigui.impl.text.TextEngine;
import dev.sixik.unigui.widgets.interaction.DatePicker;

public final class DatePickerRenderers {
    /**
     * Совместимый skin-visual внешнего контейнера DatePicker.
     *
     * <p>Сам контейнер не имеет прямого визуала: подписи календаря рисуют себя сами
     * через {@link #renderLabel(DrawScope, DatePickerState)} в собственных scopes.</p>
     */
    public static final WidgetRender DEFAULT = WidgetRender.of(DatePicker.class, (draw, picker) -> {
    });

    public static void renderLabel(DrawScope draw, DatePickerState state) {
        if (state.text().isEmpty()) return;
        TextEngine.draw(draw.context(), RichText.resolve(state.text()),
                state.x(), state.y(), state.width(), state.height(),
                Paint.fill(state.textColor()), draw.transform(),
                Alignment.CENTER, Alignment.CENTER);
    }

    private DatePickerRenderers() {
    }
}
