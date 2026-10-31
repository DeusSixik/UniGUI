package dev.sixik.unigui.impl.xml;

import dev.sixik.unigui.api.xml.XmlWidgetRegistry;
import dev.sixik.unigui.api.xml.XmlWidgetRegistryContributor;

/** ServiceLoader contribution for Minecraft-only XML widget descriptors. */
public final class MinecraftXmlRegistryContributor implements XmlWidgetRegistryContributor {
    @Override
    public void contribute(XmlWidgetRegistry registry) {
        registry.contribute(BuiltInMinecraftXml::register);
    }
}
