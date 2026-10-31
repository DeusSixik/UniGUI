package dev.sixik.unigui.impl.xml;

import dev.sixik.unigui.api.xml.XmlWidgetRegistry;
import dev.sixik.unigui.api.xml.XmlWidgetRegistryContributions;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuiltInMinecraftXmlTest {
    @Test
    void itemSpecReadsIdAndOptionalMetadata() {
        var plain = BuiltInMinecraftXml.itemSpec(" minecraft:diamond ");
        assertEquals("minecraft:diamond", plain.id().toString());
        assertEquals(0, plain.metadata());
        var wool = BuiltInMinecraftXml.itemSpec("minecraft:wool@5");
        assertEquals("minecraft:wool", wool.id().toString());
        assertEquals(5, wool.metadata());
        assertNull(BuiltInMinecraftXml.itemSpec("  "));
        assertNull(BuiltInMinecraftXml.itemSpec(null));
    }

    @Test
    void badMetadataIsRejectedWithAClearMessage() {
        assertThrows(IllegalArgumentException.class, () -> BuiltInMinecraftXml.itemSpec("minecraft:wool@red"));
        assertThrows(IllegalArgumentException.class, () -> BuiltInMinecraftXml.itemSpec("minecraft:wool@-1"));
        assertThrows(IllegalArgumentException.class, () -> BuiltInMinecraftXml.itemSpec("@3"));
    }

    @Test
    void tokensSplitOnPipeSemicolonAndNewline() {
        assertEquals(List.of("a:b", "c:d", "e:f"), BuiltInMinecraftXml.tokens("a:b | c:d;\ne:f;;"));
        assertTrue(BuiltInMinecraftXml.tokens(" ").isEmpty());
    }

    /**
     * The widgets reach the XML loader only through the ServiceLoader file of this module; a typo in it would make
     * every {@code <MinecraftItemPreviewWidget>} unknown without any error at build time.
     */
    @Test
    void contributorIsDiscoveredAndRegistersTheMinecraftWidgets() {
        assertTrue(XmlWidgetRegistryContributions.contributors().stream()
                .anyMatch(contributor -> contributor instanceof MinecraftXmlRegistryContributor));
        XmlWidgetRegistry registry = XmlWidgetRegistry.builtIns();
        for (String name : List.of("MinecraftItemPreviewWidget", "MinecraftBlockPreviewWidget",
                "MinecraftEntityPreviewWidget", "MinecraftItemPickerWidget", "MinecraftTexturePickerWidget")) {
            assertTrue(registry.contains(name), name + " is not registered");
        }
    }
}