package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.widget.render.WidgetRender;
import dev.sixik.unigui.impl.text.TextEngine;
import dev.sixik.unigui.widgets.display.TextWidget;

public final class TextWidgetRenderers {
    public static final WidgetRender DEFAULT = WidgetRender.of(TextWidget.class, (draw, w) -> {
        TextWidgetState state = w.snapshot(draw.context());
        if (state.clipped()) {
            draw.pushTextClip(state.clipX(), state.clipY(), state.clipWidth(), state.clipHeight());
        }
        try {
            for (TextWidgetSegment segment : state.segments()) {
                if (segment.text() == null || segment.text().isEmpty()) continue;
                DrawScope segmentDraw = segment.transform() == null ? draw : draw.withTransform(segment.transform());
                TextEngine.drawInline(segmentDraw, segment.text(), segment.x(), segment.y(), segment.width(), segment.height(),
                        Paint.fill(state.color()));
            }
        } finally {
            if (state.clipped()) {
                draw.popClip();
            }
        }
    });

    private TextWidgetRenderers() {
    }
}
