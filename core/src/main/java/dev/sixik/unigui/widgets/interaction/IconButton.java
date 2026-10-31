package dev.sixik.unigui.widgets.interaction;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiLink;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;
import dev.sixik.unigui.api.xml.XmlWidgetName;

/** Icon-only toolbar button convenience wrapper. */
@AiDoc(
        summary = "Graphic icon button without text or borders, ideal for close buttons and media icons.",
        purpose = "Provides a minimalist clickable icon with hover/pressed tint feedback.",
        useWhen = {"Window close controls, audio toggles, pagination arrows."},
        related = {"ToolButton", "Button"}
)
@WikiPage(
        title = "<key:icon.button.title>",
        slug = "icon-button",
        category = "<key:core.widgets.interaction.category>",
        summary = "<key:icon.button.summary>",
        order = 40
)
@WikiSection(
        title = "<key:icon.button.section.overview.title>",
        markdown = "<key:icon.button.section.overview.body>",
        order = 10
)
@WikiExample(
        title = "<key:icon.button.example.basic.title>",
        code = "<key:icon.button.example.basic.code>",
        order = 10
)
@WikiLink(label = "ToolButton", target = "page:tool-button")
@XmlWidgetName("IconButton")
public final class IconButton extends ToolButton {
    public static final String STYLE_TYPE = dev.sixik.unigui.api.style.StyleIds.Widget.ICON_BUTTON;

    public IconButton() {
        displayMode(DisplayMode.ICON_ONLY);
        layout(style -> style.size(22.0f, 22.0f).flexGrow(0.0f).flexShrink(0.0f));
    }

    public IconButton(String icon) {
        this();
        icon(icon);
    }
}
