package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.widget.render.WidgetRender;
import dev.sixik.unigui.widgets.interaction.TextInput;

public final class TextInputRenderers {
    public static final WidgetRender DEFAULT = WidgetRender.of(TextInput.class, (draw, input) ->
            TextInputRenderPlans.defaultPlan(input.snapshot(draw.context())).render(draw));
    public static final WidgetRender SEARCH_FIELD = WidgetRender.of(TextInput.class, (draw, input) ->
            TextInputRenderPlans.searchFieldPlan(input.snapshot(draw.context())).render(draw));

    private TextInputRenderers() {
    }
}
