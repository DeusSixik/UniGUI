package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.widget.render.WidgetRenderer;
import dev.sixik.unigui.api.widget.render.WidgetRole;

/** Typed renderer общего визуального контракта toolbar-кнопок. */
@FunctionalInterface
public interface ToolButtonRenderer extends WidgetRenderer<ToolButtonRenderState> {
    @Override
    default WidgetRole role() {
        return WidgetRole.TOOL_BUTTON;
    }
}
