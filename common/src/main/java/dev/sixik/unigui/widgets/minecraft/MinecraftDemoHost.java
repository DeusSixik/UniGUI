package dev.sixik.unigui.widgets.minecraft;

import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.demo.ui.minecraft.DemoMinecraft;
import dev.sixik.unigui.demo.ui.minecraft.DemoMinecraftHost;
import dev.sixik.unigui.widgets.feedback.OverlayLayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.function.Consumer;

/**
 * Maps the version-neutral samples of the demo gallery to Minecraft 1.20+ items, blocks and entities.
 *
 * <p>The differences between 1.20.1 and 1.21.1 (potion and enchantment data) live in {@code MinecraftDemoCompat}.</p>
 */
public final class MinecraftDemoHost implements DemoMinecraftHost {
    private MinecraftDemoHost() {
    }

    /**
     * Makes the demo gallery show the Minecraft category. Safe to call more than once.
     */
    public static void install() {
        if (!(DemoMinecraft.host() instanceof MinecraftDemoHost)) DemoMinecraft.host(new MinecraftDemoHost());
    }

    @Override
    public String platform() {
        return "Minecraft 1.20+";
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
        Block mapped = switch (block) {
            case GRASS -> Blocks.GRASS_BLOCK;
            case RED_WOOL -> Blocks.RED_WOOL;
            case GLASS -> Blocks.GLASS;
            case LOG -> Blocks.OAK_LOG;
        };
        MinecraftBlockPreviewWidget preview = new MinecraftBlockPreviewWidget(label, mapped);
        preview.previewSize(size);
        return preview;
    }

    @Override
    public Widget entity(String label, SampleEntity entity, float size) {
        // The player cannot be created from its EntityType, so the preview would only show a head placeholder.
        EntityType<? extends net.minecraft.world.entity.LivingEntity> type = switch (entity) {
            case ZOMBIE -> EntityType.ZOMBIE;
            case PIG -> EntityType.PIG;
            case CREEPER -> EntityType.CREEPER;
            case CHICKEN -> EntityType.CHICKEN;
            case PLAYER -> null;
        };
        if (type == null) return null;
        MinecraftEntityPreviewWidget preview = new MinecraftEntityPreviewWidget(label, type);
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

    private static ItemStack stack(SampleItem item) {
        return switch (item) {
            case DIAMOND -> new ItemStack(Items.DIAMOND);
            case IRON_INGOT -> new ItemStack(Items.IRON_INGOT);
            case WORN_PICKAXE -> {
                ItemStack worn = new ItemStack(Items.DIAMOND_PICKAXE);
                worn.setDamageValue(900);
                yield worn;
            }
            case HEALING_POTION -> MinecraftDemoCompat.healingPotion();
            case BOW -> new ItemStack(Items.BOW);
            case EMERALD -> new ItemStack(Items.EMERALD);
            case ENCHANTED_SWORD -> MinecraftDemoCompat.enchantedSword();
            case CLOCK -> new ItemStack(Items.CLOCK);
            case COMPASS -> new ItemStack(Items.COMPASS);
            case CHEST -> new ItemStack(Blocks.CHEST);
        };
    }
}
