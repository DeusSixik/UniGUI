package dev.sixik.unigui.widgets.display;

import dev.sixik.unigui.api.render.TextureHandle;
import dev.sixik.unigui.api.xml.XmlWidgetName;
import dev.sixik.unigui.api.style.StyleIds;

/** Совместимое имя {@link TextureWidget}; для нового кода предпочтителен {@link TextureWidget}. */
@XmlWidgetName("ImageView")
@Deprecated(forRemoval = false)
public final class ImageView extends TextureWidget {
    public static final String STYLE_TYPE = StyleIds.Widget.IMAGE_VIEW;

    public ImageView() {
    }

    public ImageView(TextureHandle texture) {
        super(texture);
    }
}
