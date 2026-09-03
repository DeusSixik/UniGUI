package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.widget.render.WidgetRenderer;
import dev.sixik.unigui.api.widget.render.WidgetRole;

/** Основной typed renderer обычной кнопки. */
@FunctionalInterface
public interface ButtonVisualRenderer extends WidgetRenderer<ButtonRenderState> {
    @Override
    default WidgetRole role() {
        return WidgetRole.BUTTON;
    }
}
