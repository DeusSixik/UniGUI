package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.widget.render.WidgetRenderer;
import dev.sixik.unigui.api.widget.render.WidgetRole;

/** Типизированный renderer semantic role toggle button. */
@FunctionalInterface
public interface ToggleButtonRenderer extends WidgetRenderer<ToggleButtonRenderState> {
    @Override
    default WidgetRole role() {
        return WidgetRole.TOGGLE_BUTTON;
    }
}
