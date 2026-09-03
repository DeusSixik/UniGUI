package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.widget.render.WidgetRenderer;
import dev.sixik.unigui.api.widget.render.WidgetRole;

/** Типизированный renderer semantic role toggle switch. */
@FunctionalInterface
public interface ToggleSwitchRenderer extends WidgetRenderer<ToggleSwitchRenderState> {
    @Override
    default WidgetRole role() {
        return WidgetRole.TOGGLE_SWITCH;
    }
}
