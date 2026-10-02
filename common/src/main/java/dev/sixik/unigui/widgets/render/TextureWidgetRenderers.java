package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.widget.render.WidgetRender;
import dev.sixik.unigui.widgets.display.TextureWidget;

public final class TextureWidgetRenderers {
    public static final WidgetRender DEFAULT = WidgetRender.of(TextureWidget.class, (draw, w) -> w.renderDefaultVisual(draw));

    private TextureWidgetRenderers() {
    }
}
