package dev.sixik.isf.client.widgets;

import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.render.DrawPoint;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.widgets.interaction.Button;

public class IconButton extends Button {
    @Override
    protected void renderContent(RenderContext context) {
        float x = layoutBounds().x();
        float y = layoutBounds().y();
        float width = layoutBounds().width();
        float height = layoutBounds().height();
        renderChildren(context);
    }
}
