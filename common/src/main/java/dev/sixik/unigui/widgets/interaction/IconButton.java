package dev.sixik.unigui.widgets.interaction;

import dev.sixik.unigui.api.xml.XmlWidgetName;

/** Icon-only toolbar button convenience wrapper. */
@XmlWidgetName("IconButton")
public final class IconButton extends ToolButton {
    public IconButton() {
        displayMode(DisplayMode.ICON_ONLY);
        layout(style -> style.size(22.0f, 22.0f).flexGrow(0.0f).flexShrink(0.0f));
    }

    public IconButton(String icon) {
        this();
        icon(icon);
    }
}
