package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.widget.render.WidgetRenderer;
import dev.sixik.unigui.api.widget.render.WidgetRole;

/** Типизированный renderer semantic role checkbox. */
@FunctionalInterface
public interface CheckboxRenderer extends WidgetRenderer<CheckboxRenderState> {
    @Override
    default WidgetRole role() {
        return WidgetRole.CHECKBOX;
    }
}
