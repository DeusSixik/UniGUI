package dev.sixik.unigui.impl.xml.minecraft;

import dev.sixik.unigui.xaml.schema.Schema;
import dev.sixik.unigui.xaml.schema.SchemaContributor;

/** ServiceLoader contribution for Minecraft-only XAML widget types. */
public final class MinecraftXmlRegistryContributor implements SchemaContributor {
    @Override
    public void contribute(Schema schema) {
        BuiltInMinecraftXml.register(schema);
    }
}
