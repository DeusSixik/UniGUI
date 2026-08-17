package dev.sixik.unigui.widgets.editor;

import dev.sixik.unigui.api.xml.XmlWidgetName;

/** XML-visible alias for the editor status strip when used as a diagnostics footer. */
@XmlWidgetName("DiagnosticsStrip")
public class DiagnosticsStrip extends StatusBar {
    public DiagnosticsStrip() {
        mode("Diagnostics");
    }
}
