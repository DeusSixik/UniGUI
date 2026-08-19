package dev.sixik.unigui.widgets.display;

import dev.sixik.unigui.api.render.TextureHandle;
import dev.sixik.unigui.api.widget.skin.WidgetsRender;
import dev.sixik.unigui.api.xml.XmlWidgetName;
import dev.sixik.unigui.widgets.render.TextureWidgetRenderer;
import dev.sixik.unigui.api.style.StyleIds;

@XmlWidgetName("ImageView")
public final class ImageView extends TextureWidget {
    public static final String STYLE_TYPE = StyleIds.Widget.IMAGE_VIEW;

    public ImageView() {
    }

    public ImageView(TextureHandle texture) {
        super(texture);
    }

    @Override
    protected TextureWidgetRenderer defaultRenderer() {
        return WidgetsRender.imageView();
    }

    @Override
    protected TextureWidgetRenderer effectiveRenderer() {
        return renderer() == null ? styleRenderer(TextureWidgetRenderer.class, defaultRenderer()) : renderer();
    }
}
