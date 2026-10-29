package dev.sixik.unigui.widgets.editor;

import dev.sixik.unigui.api.xml.XmlWidgetName;

/** XML-visible alias for the descriptor-backed widget palette panel. */
@XmlWidgetName("PalettePanel")
public class PalettePanel extends WidgetPalette {
    public static final String STYLE_TYPE = dev.sixik.unigui.api.style.StyleIds.Widget.PALETTE_PANEL;

    public PalettePanel() {
        title("Palette");
    }
}
