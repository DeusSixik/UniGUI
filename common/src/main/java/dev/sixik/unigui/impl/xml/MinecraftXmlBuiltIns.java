package dev.sixik.unigui.impl.xml;

import dev.sixik.unigui.api.xml.XmlWidgetRegistryContributions;
import dev.sixik.unigui.widgets.editor.XmlPropertiesPanel;

/** Installs Minecraft-specific XML widgets into Core's built-in registry. */
public final class MinecraftXmlBuiltIns {
    private static AutoCloseable contribution;

    private MinecraftXmlBuiltIns() {
    }

    public static synchronized void install() {
        if (contribution != null) return;
        contribution = XmlWidgetRegistryContributions.register(registry -> {
            registry.contribute(BuiltInMinecraftXml::register);
            registry.registerAnnotated(XmlPropertiesPanel.class)
                    .describe("XML Properties Panel", "Editor",
                            "Session-aware XML inspector pane that applies attribute edits through undoable document edits.");
        });
    }
}
