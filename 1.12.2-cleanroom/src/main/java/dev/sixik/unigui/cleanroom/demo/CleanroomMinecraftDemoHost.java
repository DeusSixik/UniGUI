package dev.sixik.unigui.cleanroom.demo;

import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.demo.ui.minecraft.DemoMinecraftHost;
import dev.sixik.unigui.widgets.feedback.OverlayLayer;
import dev.sixik.unigui.widgets.minecraft.MinecraftBlockPreviewWidget;
import dev.sixik.unigui.widgets.minecraft.MinecraftEntityPreviewWidget;
import dev.sixik.unigui.widgets.minecraft.MinecraftItemPreviewWidget;
import dev.sixik.unigui.widgets.minecraft.MinecraftItemTooltip;
import dev.sixik.unigui.widgets.minecraft.MinecraftWidgets;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.init.Enchantments;
import net.minecraft.init.Items;
import net.minecraft.init.PotionTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.function.Consumer;

/**
 * Maps the version-neutral samples of the demo gallery to Minecraft 1.12.2 items, blocks and entities.
 */
public final class CleanroomMinecraftDemoHost implements DemoMinecraftHost {
    @Override
    public String platform() {
        return "Minecraft 1.12.2";
    }

    @Override
    public Widget item(String label, SampleItem item, int count, boolean decorations, float opacity,
                       float size, Consumer<Widget> overlays) {
        ItemStack stack = stack(item);
        stack.setCount(Math.max(1, count));
        MinecraftItemPreviewWidget preview = MinecraftWidgets.itemPreview(label, stack);
        preview.previewSize(size);
        preview.decorations(decorations);
        if (overlays != null) {
            MinecraftItemTooltip tooltip = preview.vanillaTooltip();
            // Like the vanilla inventory: the box follows the mouse (12 right, 12 up) instead of sitting under the icon.
            tooltip.followPointer(true);
            tooltip.offset(12.0f, -12.0f);
            overlays.accept(tooltip);
        }
        if (opacity < 1.0f) preview.opacity(opacity);
        return preview;
    }

    @Override
    public Widget block(String label, SampleBlock block, float size) {
        MinecraftBlockPreviewWidget preview = switch (block) {
            case GRASS -> new MinecraftBlockPreviewWidget(label, Blocks.GRASS);
            case RED_WOOL -> new MinecraftBlockPreviewWidget(label, Blocks.WOOL).block(Blocks.WOOL, 14);
            case GLASS -> new MinecraftBlockPreviewWidget(label, Blocks.GLASS);
            case LOG -> new MinecraftBlockPreviewWidget(label, Blocks.LOG);
        };
        preview.previewSize(size);
        return preview;
    }

    @Override
    public Widget entity(String label, SampleEntity entity, float size) {
        MinecraftEntityPreviewWidget preview = switch (entity) {
            case ZOMBIE -> entityPreview(label, "minecraft:zombie");
            case PIG -> entityPreview(label, "minecraft:pig");
            case CREEPER -> entityPreview(label, "minecraft:creeper");
            case CHICKEN -> entityPreview(label, "minecraft:chicken");
            case PLAYER -> {
                if (Minecraft.getMinecraft().player == null) yield null;
                yield MinecraftWidgets.entityPreview(label, Minecraft.getMinecraft().player);
            }
        };
        if (preview == null) return null;
        preview.previewSize(size);
        return preview;
    }

    @Override
    public Widget itemPicker(OverlayLayer overlay) {
        return MinecraftWidgets.itemPicker().overlayLayer(overlay);
    }

    @Override
    public Widget texturePicker(OverlayLayer overlay) {
        return MinecraftWidgets.texturePicker().overlayLayer(overlay);
    }

    private static MinecraftEntityPreviewWidget entityPreview(String label, String id) {
        EntityEntry entry = ForgeRegistries.ENTITIES.getValue(new ResourceLocation(id));
        return entry == null ? null : MinecraftWidgets.entityPreview(label, entry);
    }

    private static ItemStack stack(SampleItem item) {
        return switch (item) {
            case DIAMOND -> new ItemStack(Items.DIAMOND);
            case IRON_INGOT -> new ItemStack(Items.IRON_INGOT);
            case WORN_PICKAXE -> {
                ItemStack worn = new ItemStack(Items.DIAMOND_PICKAXE);
                worn.setItemDamage(900);
                yield worn;
            }
            case HEALING_POTION -> PotionUtils.addPotionToItemStack(new ItemStack(Items.POTIONITEM), PotionTypes.HEALING);
            case BOW -> new ItemStack(Items.BOW);
            case EMERALD -> new ItemStack(Items.EMERALD);
            case ENCHANTED_SWORD -> {
                ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
                sword.addEnchantment(Enchantments.SHARPNESS, 3);
                yield sword;
            }
            case CLOCK -> new ItemStack(Items.CLOCK);
            case COMPASS -> new ItemStack(Items.COMPASS);
            case CHEST -> new ItemStack(Blocks.CHEST);
        };
    }
}
