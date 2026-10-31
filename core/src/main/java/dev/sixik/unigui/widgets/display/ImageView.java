package dev.sixik.unigui.widgets.display;

import dev.sixik.unigui.api.render.TextureHandle;
import dev.sixik.unigui.api.xml.XmlWidgetName;
import dev.sixik.unigui.api.style.StyleIds;
import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;

/** Совместимое имя {@link TextureWidget}; для нового кода предпочтителен {@link TextureWidget}. */
@AiDoc(
        summary = "Texture display widget presenting static images, icons, or animated sprites with scale modes.",
        purpose = "Provides declarative image display widget extending TextureWidget.",
        useWhen = {
                "Showing icons or image assets in UI layouts.",
                "XML layouts utilizing <ImageView> tags."
        },
        criticalRules = {
                "Direct extension of TextureWidget."
        },
        related = {"TextureWidget", "TextureHandle"})
@WikiPage(
        title = "<key:image.view.title>",
        slug = "image-view",
        category = "<key:core.widgets.display.category>",
        summary = "<key:image.view.summary>",
        order = 100)
@WikiSection(
        title = "<key:image.view.section.overview.title>",
        markdown = "<key:image.view.section.overview.body>",
        order = 10)
@WikiExample(
        title = "<key:image.view.example.basic.title>",
        code = "<key:image.view.example.basic.code>",
        order = 10)
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