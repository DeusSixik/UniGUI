package dev.sixik.unigui.widgets;

import dev.sixik.unigui.api.render.TextureHandle;
import dev.sixik.unigui.api.widget.skin.WidgetsRender;
import dev.sixik.unigui.widgets.render.TextureWidgetRenderer;

public final class ImageView extends TextureWidget {
    public ImageView() {
    }

    public ImageView(TextureHandle texture) {
        super(texture);
    }

    @Override
    protected TextureWidgetRenderer effectiveRenderer() {
        return renderer() == null ? styleRenderer(TextureWidgetRenderer.class, WidgetsRender.imageView()) : renderer();
    }
}
