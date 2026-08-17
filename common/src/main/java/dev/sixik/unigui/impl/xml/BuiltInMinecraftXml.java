package dev.sixik.unigui.impl.xml;

import dev.sixik.unigui.api.xml.XmlAttributeDescriptor;
import dev.sixik.unigui.api.xml.XmlAttributeValueType;
import dev.sixik.unigui.widgets.minecraft.MinecraftBlockPreviewWidget;
import dev.sixik.unigui.widgets.minecraft.MinecraftEntityPreviewWidget;
import dev.sixik.unigui.widgets.minecraft.MinecraftItemPickerWidget;
import dev.sixik.unigui.widgets.minecraft.MinecraftItemPreviewWidget;
import dev.sixik.unigui.widgets.minecraft.MinecraftTexturePickerWidget;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;

final class BuiltInMinecraftXml {
    private BuiltInMinecraftXml() {
    }

    static void register(WidgetXmlRegistry registry) {
        itemPreview(registry.register("MinecraftItemPreviewWidget",
                () -> new MinecraftItemPreviewWidget("", ItemStack.EMPTY)));
        blockPreview(registry.register("MinecraftBlockPreviewWidget",
                () -> new MinecraftBlockPreviewWidget("", null)));
        entityPreview(registry.register("MinecraftEntityPreviewWidget",
                () -> new MinecraftEntityPreviewWidget("", null)));
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
                                .description("Minecraft item id, for example minecraft:diamond."))
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
                                .description("Minecraft block id, for example minecraft:stone."));
    }

    private static WidgetXmlType<MinecraftEntityPreviewWidget> entityPreview(WidgetXmlType<MinecraftEntityPreviewWidget> type) {
        return BuiltInWidgetXmlSupport.commonWidget(type, MinecraftEntityPreviewWidget.class)
                .describe("Minecraft Entity Preview", "Minecraft", "Minecraft living entity preview with player-head fallback rendering.")
                .attribute("entityType", XmlValueParsers.STRING, BuiltInMinecraftXml::entityType,
                        XmlAttributeDescriptor.of("entityType")
                                .category("Minecraft")
                                .defaultValue("")
                                .valueType(XmlAttributeValueType.RESOURCE_ID)
                                .description("Minecraft living entity type id, for example minecraft:zombie."));
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
        Item item = itemById(value);
        preview.stack(item == null || item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item));
    }

    private static void block(MinecraftBlockPreviewWidget preview, String value) {
        ResourceLocation id = id(value);
        Block block = id == null ? null : BuiltInRegistries.BLOCK.getOptional(id).orElse(null);
        preview.block(block);
    }

    @SuppressWarnings("unchecked")
    private static void entityType(MinecraftEntityPreviewWidget preview, String value) {
        ResourceLocation id = id(value);
        EntityType<?> type = id == null ? null : BuiltInRegistries.ENTITY_TYPE.getOptional(id).orElse(null);
        preview.entityType((EntityType<? extends LivingEntity>) type);
    }

    private static void itemEntries(MinecraftItemPickerWidget picker, String value) {
        List<MinecraftItemPickerWidget.ItemEntry> entries = new ArrayList<>();
        for (String token : tokens(value)) {
            ResourceLocation id = id(token);
            Item item = id == null ? null : BuiltInRegistries.ITEM.getOptional(id).orElse(null);
            if (item != null && item != Items.AIR) {
                entries.add(new MinecraftItemPickerWidget.ItemEntry(id, item));
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

    private static Item itemById(String value) {
        ResourceLocation id = id(value);
        return id == null ? null : BuiltInRegistries.ITEM.getOptional(id).orElse(null);
    }

    private static ResourceLocation id(String value) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isEmpty()) return null;
        ResourceLocation id = ResourceLocation.tryParse(normalized);
        if (id == null) {
            throw new IllegalArgumentException("Invalid Minecraft resource id: " + value);
        }
        return id;
    }

    private static List<String> tokens(String value) {
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
