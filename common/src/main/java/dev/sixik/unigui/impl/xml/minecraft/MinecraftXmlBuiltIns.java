package dev.sixik.unigui.impl.xml.minecraft;

import dev.sixik.unigui.xaml.schema.SchemaContributions;

/** Installs Minecraft-specific XAML widgets (namespace {@code mc}) into the shared schema. */
public final class MinecraftXmlBuiltIns {
    private static AutoCloseable contribution;

    private MinecraftXmlBuiltIns() {
    }

    public static synchronized void install() {
        if (contribution != null) return;
        contribution = SchemaContributions.register(BuiltInMinecraftXml::register);
    }
}
