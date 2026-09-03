package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.widget.render.WidgetRenderer;
import dev.sixik.unigui.api.widget.render.WidgetRole;

/** Типизированный renderer semantic role radio button. */
@FunctionalInterface
public interface RadioButtonRenderer extends WidgetRenderer<RadioButtonRenderState> {
    @Override
    default WidgetRole role() {
        return WidgetRole.RADIO_BUTTON;
    }
}
