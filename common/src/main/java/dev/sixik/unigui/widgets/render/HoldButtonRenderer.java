package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.widget.render.WidgetRenderer;
import dev.sixik.unigui.api.widget.render.WidgetRole;

@FunctionalInterface
public interface HoldButtonRenderer extends WidgetRenderer<HoldButtonState> {
    @Override
    default WidgetRole role() {
        return WidgetRole.HOLD_BUTTON;
    }
}
