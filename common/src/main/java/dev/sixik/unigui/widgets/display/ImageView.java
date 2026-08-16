package dev.sixik.unigui.widgets.display;

import dev.sixik.unigui.api.render.TextureHandle;
import dev.sixik.unigui.api.widget.skin.WidgetsRender;
import dev.sixik.unigui.api.xml.XmlWidgetName;
import dev.sixik.unigui.widgets.render.TextureWidgetRenderer;

@XmlWidgetName("ImageView")
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
