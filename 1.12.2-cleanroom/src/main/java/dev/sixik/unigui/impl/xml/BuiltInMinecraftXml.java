package dev.sixik.unigui.impl.xml;

import dev.sixik.unigui.api.xml.XmlAttributeDescriptor;
import dev.sixik.unigui.api.xml.XmlAttributeValueType;
import dev.sixik.unigui.widgets.minecraft.MinecraftBlockPreviewWidget;
import dev.sixik.unigui.widgets.minecraft.MinecraftEntityPreviewWidget;
import dev.sixik.unigui.widgets.minecraft.MinecraftItemPickerWidget;
import dev.sixik.unigui.widgets.minecraft.MinecraftItemPreviewWidget;
import dev.sixik.unigui.widgets.minecraft.MinecraftTexturePickerWidget;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

final class BuiltInMinecraftXml {
    private BuiltInMinecraftXml() {
    }

    static void register(WidgetXmlRegistry registry) {
        itemPreview(registry.register("MinecraftItemPreviewWidget",
                () -> new MinecraftItemPreviewWidget("", ItemStack.EMPTY)));
        blockPreview(registry.register("MinecraftBlockPreviewWidget",
                () -> new MinecraftBlockPreviewWidget("", (Block) null)));
        entityPreview(registry.register("MinecraftEntityPreviewWidget",
                () -> new MinecraftEntityPreviewWidget("", (EntityEntry) null)));
        itemPicker(registry.register("MinecraftItemPickerWidget",
                () -> new MinecraftItemPickerWidget(false)));
        texturePicker(registry.register("MinecraftTexturePickerWidget",
                () -> new MinecraftTexturePickerWidget(false)));
    }

    private static WidgetXmlType<MinecraftItemPreviewWidget> itemPreview(WidgetXmlType<MinecraftItemPreviewWidget> type) {
        return BuiltInWidgetXmlSupport.commonWidget(type, MinecraftItemPreviewWidget.class)
                .describe("Minecraft Item Preview", "Minecraft", "Minecraft item stack preview with optional count/decorations.")
                .attribute("item", XmlValueParsers.STRING, BuiltInMinecraftXml::item,
                        XmlAttributeDescriptor.of("item")
                                .category("Minecraft")
                                .defaultValue("")
                                .valueType(XmlAttributeValueType.RESOURCE_ID)
                                .description("Minecraft item id, for example minecraft:diamond or minecraft:wool@5 (metadata)."))
                .attribute("decorations", XmlValueParsers.BOOLEAN, MinecraftItemPreviewWidget::decorations,
                        XmlAttributeDescriptor.of("decorations")
                                .category("Appearance")
                                .defaultValue("true")
                                .description("Whether vanilla item decorations are rendered."));
    }

    private static WidgetXmlType<MinecraftBlockPreviewWidget> blockPreview(WidgetXmlType<MinecraftBlockPreviewWidget> type) {
        return BuiltInWidgetXmlSupport.commonWidget(type, MinecraftBlockPreviewWidget.class)
                .describe("Minecraft Block Preview", "Minecraft", "Minecraft block preview rendered through the item/block pipeline.")
                .attribute("block", XmlValueParsers.STRING, BuiltInMinecraftXml::block,
                        XmlAttributeDescriptor.of("block")
                                .category("Minecraft")
                                .defaultValue("")
                                .valueType(XmlAttributeValueType.RESOURCE_ID)
                                .description("Minecraft block id, for example minecraft:stone or minecraft:wool@5 (metadata)."));
    }

    private static WidgetXmlType<MinecraftEntityPreviewWidget> entityPreview(WidgetXmlType<MinecraftEntityPreviewWidget> type) {
        return BuiltInWidgetXmlSupport.commonWidget(type, MinecraftEntityPreviewWidget.class)
                .describe("Minecraft Entity Preview", "Minecraft", "Minecraft living entity preview with player-head fallback rendering.")
                .attribute("entityType", XmlValueParsers.STRING, BuiltInMinecraftXml::entityType,
                        XmlAttributeDescriptor.of("entityType")
                                .category("Minecraft")
                                .defaultValue("")
                                .valueType(XmlAttributeValueType.RESOURCE_ID)
                                .description("Minecraft living entity id, for example minecraft:zombie."));
    }

    private static WidgetXmlType<MinecraftItemPickerWidget> itemPicker(WidgetXmlType<MinecraftItemPickerWidget> type) {
        return BuiltInWidgetXmlSupport.commonWidget(type)
                .describe("Minecraft Item Picker", "Minecraft", "Searchable Minecraft item picker. Use items for deterministic XML lists.")
                .attribute("query", XmlValueParsers.STRING, MinecraftItemPickerWidget::query,
                        XmlAttributeDescriptor.of("query")
                                .category("Behavior")
                                .defaultValue("")
                                .description("Initial search query used to filter items."))
                .attribute("items", XmlValueParsers.STRING, BuiltInMinecraftXml::itemEntries,
                        XmlAttributeDescriptor.of("items")
                                .category("Minecraft")
                                .defaultValue("")
                                .valueType(XmlAttributeValueType.RESOURCE_ID)
                                .description("Minecraft item ids separated by '|', ';' or line breaks."))
                .attribute("selectedIndex", XmlValueParsers.INT, MinecraftItemPickerWidget::selectIndex,
                        XmlAttributeDescriptor.of("selectedIndex")
                                .category("Behavior")
                                .defaultValue("-1")
                                .description("Initial selected item index after XML items are loaded."))
                .attribute("selectedFilteredIndex", XmlValueParsers.INT, MinecraftItemPickerWidget::selectFilteredIndex,
                        XmlAttributeDescriptor.of("selectedFilteredIndex")
                                .category("Behavior")
                                .defaultValue("-1")
                                .description("Initial selected index in the filtered item list after XML items are loaded."));
    }

    private static WidgetXmlType<MinecraftTexturePickerWidget> texturePicker(WidgetXmlType<MinecraftTexturePickerWidget> type) {
        return BuiltInWidgetXmlSupport.commonWidget(type)
                .describe("Minecraft Texture Picker", "Minecraft", "Searchable Minecraft texture picker. Use textures for deterministic XML lists.")
                .attribute("query", XmlValueParsers.STRING, MinecraftTexturePickerWidget::query,
                        XmlAttributeDescriptor.of("query")
                                .category("Behavior")
                                .defaultValue("")
                                .description("Initial search query used to filter textures."))
                .attribute("textures", XmlValueParsers.STRING, BuiltInMinecraftXml::textureEntries,
                        XmlAttributeDescriptor.of("textures")
                                .category("Minecraft")
                                .defaultValue("")
                                .valueType(XmlAttributeValueType.RESOURCE_ID)
                                .description("Minecraft texture ids separated by '|', ';' or line breaks."))
                .attribute("selectedIndex", XmlValueParsers.INT, MinecraftTexturePickerWidget::selectIndex,
                        XmlAttributeDescriptor.of("selectedIndex")
                                .category("Behavior")
                                .defaultValue("-1")
                                .description("Initial selected texture index after XML textures are loaded."))
                .attribute("selectedFilteredIndex", XmlValueParsers.INT, MinecraftTexturePickerWidget::selectFilteredIndex,
                        XmlAttributeDescriptor.of("selectedFilteredIndex")
                                .category("Behavior")
                                .defaultValue("-1")
                                .description("Initial selected index in the filtered texture list after XML textures are loaded."));
    }

    private static void item(MinecraftItemPreviewWidget preview, String value) {
        ItemSpec spec = itemSpec(value);
        Item item = spec == null ? null : ForgeRegistries.ITEMS.getValue(spec.id());
        preview.stack(item == null || item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item, 1, spec.metadata()));
    }

    private static void block(MinecraftBlockPreviewWidget preview, String value) {
        ItemSpec spec = itemSpec(value);
        // The block registry answers air for an unknown id; an absent key means "no block".
        Block block = spec == null || !ForgeRegistries.BLOCKS.containsKey(spec.id())
                ? null : ForgeRegistries.BLOCKS.getValue(spec.id());
        preview.block(block == Blocks.AIR ? null : block, spec == null ? 0 : spec.metadata());
    }

    private static void entityType(MinecraftEntityPreviewWidget preview, String value) {
        ResourceLocation id = id(value);
        preview.entityType(id == null ? null : ForgeRegistries.ENTITIES.getValue(id));
    }

    private static void itemEntries(MinecraftItemPickerWidget picker, String value) {
        List<MinecraftItemPickerWidget.ItemEntry> entries = new ArrayList<>();
        for (String token : tokens(value)) {
            ItemSpec spec = itemSpec(token);
            Item item = spec == null ? null : ForgeRegistries.ITEMS.getValue(spec.id());
            if (item != null && item != Items.AIR) {
                entries.add(new MinecraftItemPickerWidget.ItemEntry(spec.id(), item, spec.metadata()));
            }
        }
        picker.items(entries);
    }

    private static void textureEntries(MinecraftTexturePickerWidget picker, String value) {
        List<MinecraftTexturePickerWidget.TextureEntry> entries = new ArrayList<>();
        for (String token : tokens(value)) {
            ResourceLocation id = id(token);
            if (id != null) {
                entries.add(new MinecraftTexturePickerWidget.TextureEntry(id));
            }
        }
        picker.textures(entries);
    }

    /** An item id with the optional {@code @metadata} suffix of 1.12.2. */
    record ItemSpec(ResourceLocation id, int metadata) {
    }

    /**
     * Parses {@code namespace:name} or {@code namespace:name@metadata}.
     *
     * @return the spec, or {@code null} for an empty value
     * @throws IllegalArgumentException if the metadata is not a non-negative number
     */
    static ItemSpec itemSpec(String value) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isEmpty()) return null;
        int at = normalized.lastIndexOf('@');
        if (at < 0) return new ItemSpec(new ResourceLocation(normalized), 0);
        String idPart = normalized.substring(0, at).trim();
        String metaPart = normalized.substring(at + 1).trim();
        int metadata;
        try {
            metadata = Integer.parseInt(metaPart);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid item metadata in: " + value, e);
        }
        if (metadata < 0 || idPart.isEmpty()) throw new IllegalArgumentException("Invalid item id: " + value);
        return new ItemSpec(new ResourceLocation(idPart), metadata);
    }

    private static ResourceLocation id(String value) {
        String normalized = value == null ? "" : value.trim();
        return normalized.isEmpty() ? null : new ResourceLocation(normalized);
    }

    static List<String> tokens(String value) {
        if (value == null || value.isBlank()) return List.of();
        List<String> tokens = new ArrayList<>();
        for (String token : value.split("[|;\\r\\n]+")) {
            String normalized = token.trim();
            if (!normalized.isEmpty()) {
                tokens.add(normalized);
            }
        }
        return tokens;
    }
}