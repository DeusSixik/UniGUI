package dev.sixik.unigui.impl.xml.minecraft;

import dev.sixik.unigui.xaml.UiName;
import dev.sixik.unigui.xaml.XamlNamespaces;
import dev.sixik.unigui.xaml.schema.Converters;
import dev.sixik.unigui.xaml.schema.Meta;
import dev.sixik.unigui.xaml.schema.Schema;
import dev.sixik.unigui.xaml.schema.TypeInfo;
import dev.sixik.unigui.xaml.schema.ValueKind;
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

    static void register(Schema schema) {
        schema.define(XamlNamespaces.MINECRAFT, "ItemPreview", () -> itemPreview());
        schema.define(XamlNamespaces.MINECRAFT, "BlockPreview", () -> blockPreview());
        schema.define(XamlNamespaces.MINECRAFT, "EntityPreview", () -> entityPreview());
        schema.define(XamlNamespaces.MINECRAFT, "ItemPicker", () -> itemPicker());
        schema.define(XamlNamespaces.MINECRAFT, "TexturePicker", () -> texturePicker());
    }

    private static UiName name(String local) {
        return new UiName(XamlNamespaces.MINECRAFT, local, XamlNamespaces.MINECRAFT_PREFIX);
    }

    private static Meta resourceId(String description) {
        return Meta.category("Minecraft").defaultValue("").kind(ValueKind.RESOURCE_ID).description(description);
    }

    private static TypeInfo<?> itemPreview() {
        return TypeInfo.of(name("ItemPreview"), MinecraftItemPreviewWidget.class,
                        () -> new MinecraftItemPreviewWidget("", ItemStack.EMPTY))
                .describe("Minecraft Item Preview", "Minecraft", "Minecraft item stack preview with optional count/decorations.")
                .property("item", Converters.STRING, BuiltInMinecraftXml::item,
                        resourceId("Minecraft item id, for example minecraft:diamond."))
                .property("decorations", Converters.BOOLEAN, MinecraftItemPreviewWidget::decorations,
                        Meta.category("Appearance").defaultValue("true")
                                .description("Whether vanilla item decorations are rendered."))
                .build();
    }

    private static TypeInfo<?> blockPreview() {
        return TypeInfo.of(name("BlockPreview"), MinecraftBlockPreviewWidget.class,
                        () -> new MinecraftBlockPreviewWidget("", null))
                .describe("Minecraft Block Preview", "Minecraft", "Minecraft block preview rendered through the item/block pipeline.")
                .property("block", Converters.STRING, BuiltInMinecraftXml::block,
                        resourceId("Minecraft block id, for example minecraft:stone."))
                .build();
    }

    private static TypeInfo<?> entityPreview() {
        return TypeInfo.of(name("EntityPreview"), MinecraftEntityPreviewWidget.class,
                        () -> new MinecraftEntityPreviewWidget("", null))
                .describe("Minecraft Entity Preview", "Minecraft", "Minecraft living entity preview with player-head fallback rendering.")
                .property("entityType", Converters.STRING, BuiltInMinecraftXml::entityType,
                        resourceId("Minecraft living entity type id, for example minecraft:zombie."))
                .build();
    }

    private static TypeInfo<?> itemPicker() {
        return TypeInfo.of(name("ItemPicker"), MinecraftItemPickerWidget.class,
                        () -> new MinecraftItemPickerWidget(false))
                .withoutAnnotations()
                .describe("Minecraft Item Picker", "Minecraft", "Searchable Minecraft item picker. Use items for deterministic XAML lists.")
                .property("query", Converters.STRING, MinecraftItemPickerWidget::query,
                        Meta.category("Behavior").defaultValue("").description("Initial search query used to filter items."))
                .property("items", Converters.STRING, BuiltInMinecraftXml::itemEntries,
                        resourceId("Minecraft item ids separated by '|', ';' or line breaks."))
                .property("selectedIndex", Converters.INT, MinecraftItemPickerWidget::selectIndex,
                        Meta.category("Behavior").defaultValue("-1")
                                .description("Initial selected item index after XAML items are loaded."))
                .property("selectedFilteredIndex", Converters.INT, MinecraftItemPickerWidget::selectFilteredIndex,
                        Meta.category("Behavior").defaultValue("-1")
                                .description("Initial selected index in the filtered item list after XAML items are loaded."))
                .build();
    }

    private static TypeInfo<?> texturePicker() {
        return TypeInfo.of(name("TexturePicker"), MinecraftTexturePickerWidget.class,
                        () -> new MinecraftTexturePickerWidget(false))
                .withoutAnnotations()
                .describe("Minecraft Texture Picker", "Minecraft", "Searchable Minecraft texture picker. Use textures for deterministic XAML lists.")
                .property("query", Converters.STRING, MinecraftTexturePickerWidget::query,
                        Meta.category("Behavior").defaultValue("").description("Initial search query used to filter textures."))
                .property("textures", Converters.STRING, BuiltInMinecraftXml::textureEntries,
                        resourceId("Minecraft texture ids separated by '|', ';' or line breaks."))
                .property("selectedIndex", Converters.INT, MinecraftTexturePickerWidget::selectIndex,
                        Meta.category("Behavior").defaultValue("-1")
                                .description("Initial selected texture index after XAML textures are loaded."))
                .property("selectedFilteredIndex", Converters.INT, MinecraftTexturePickerWidget::selectFilteredIndex,
                        Meta.category("Behavior").defaultValue("-1")
                                .description("Initial selected index in the filtered texture list after XAML textures are loaded."))
                .build();
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
