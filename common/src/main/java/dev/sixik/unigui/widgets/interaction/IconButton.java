package dev.sixik.unigui.widgets.interaction;

import dev.sixik.unigui.api.xml.XmlWidgetName;

/** Icon-only toolbar button convenience wrapper. */
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
