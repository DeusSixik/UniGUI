package dev.sixik.unigui.impl.xml;

import dev.sixik.unigui.xaml.UiName;
import dev.sixik.unigui.xaml.Xaml;
import dev.sixik.unigui.xaml.XamlNamespaces;
import dev.sixik.unigui.xaml.schema.Schema;
import dev.sixik.unigui.xaml.schema.SchemaContributions;
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
     * The widgets reach the XAML loader only through the ServiceLoader file of this module; a typo in it would make
     * every {@code <mc:ItemPreview>} unknown without any error at build time.
     */
    @Test
    void contributorIsDiscoveredAndRegistersTheMinecraftWidgets() {
        assertTrue(SchemaContributions.contributors().stream()
                .anyMatch(contributor -> contributor instanceof MinecraftXmlRegistryContributor));
        assertTrue(SchemaContributions.loadFailures().isEmpty(), SchemaContributions.loadFailures().toString());
        Schema schema = Xaml.defaultSchema();
        for (String name : List.of("ItemPreview", "BlockPreview", "EntityPreview", "ItemPicker", "TexturePicker")) {
            assertTrue(schema.type(new UiName(XamlNamespaces.MINECRAFT, name, XamlNamespaces.MINECRAFT_PREFIX)).isPresent(), name + " is not registered");
        }
    }
}