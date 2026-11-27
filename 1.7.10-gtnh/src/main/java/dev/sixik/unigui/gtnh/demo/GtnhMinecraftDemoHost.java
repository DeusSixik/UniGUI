package dev.sixik.unigui.gtnh.demo;

import dev.sixik.unigui.demo.ui.scene.SceneShowcase;
import dev.sixik.unigui.demo.ui.scene.SceneTutorialPlayer;
import dev.sixik.unigui.api.scene.BlockNode;
import dev.sixik.unigui.backend.shared.LegacyFurnaceDemo;
import net.minecraftforge.common.util.ForgeDirection;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.demo.ui.minecraft.DemoMinecraftHost;
import dev.sixik.unigui.widgets.feedback.OverlayLayer;
import dev.sixik.unigui.widgets.minecraft.MinecraftBlockPreviewWidget;
import dev.sixik.unigui.widgets.minecraft.MinecraftEntityPreviewWidget;
import dev.sixik.unigui.widgets.minecraft.MinecraftItemPreviewWidget;
import dev.sixik.unigui.widgets.minecraft.MinecraftItemTooltip;
import dev.sixik.unigui.widgets.minecraft.MinecraftWidgets;
import net.minecraft.client.Minecraft;
import net.minecraft.block.Block;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.EntityList;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

/**
 * Maps the version-neutral samples of the demo gallery to Minecraft 1.7.10 items, blocks and entities.
 */
public final class GtnhMinecraftDemoHost implements DemoMinecraftHost {
    @Override
    public String platform() {
        return "Minecraft 1.7.10";
    }

    @Override
    public SceneShowcase.Blocks sceneBlocks() {
        return SceneShowcase.Blocks.MODERN;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> sceneElementIds(SceneElementKind kind) {
        Collection<?> keys = switch (kind) {
            case BLOCK -> Block.blockRegistry.getKeys();
            case ITEM -> Item.itemRegistry.getKeys();
            case ENTITY -> EntityList.stringToClassMapping.keySet();
        };
        List<String> ids = new ArrayList<>(keys.size());
        for (Object key : keys) {
            if (key != null && !"minecraft:air".equals(key.toString())) ids.add(key.toString());
        }
        return ids;
    }

    @Override
    public SceneTutorialPlayer.Sequence furnaceTutorial() {
        int facing = ForgeDirection.SOUTH.ordinal();
        var furnace = BlockNode.of(Blocks.furnace, facing);
        return LegacyFurnaceDemo.create(center -> BlockNode.of(Blocks.stonebrick, center ? 3 : 0),
            furnace, lit -> furnace.setState(lit ? Blocks.lit_furnace : Blocks.furnace, facing),
            new ItemStack(Blocks.stonebrick), new ItemStack(Blocks.furnace), new ItemStack(Items.coal));
    }

    @Override
    public Widget item(String label, SampleItem item, int count, boolean decorations, float opacity,
                       float size, Consumer<Widget> overlays) {
        ItemStack stack = stack(item);
        stack.stackSize = Math.max(1, count);
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
            case GRASS -> new MinecraftBlockPreviewWidget(label, Blocks.grass);
            case RED_WOOL -> new MinecraftBlockPreviewWidget(label, Blocks.wool).block(Blocks.wool, 14);
            case GLASS -> new MinecraftBlockPreviewWidget(label, Blocks.glass);
            case LOG -> new MinecraftBlockPreviewWidget(label, Blocks.log);
        };
        preview.previewSize(size);
        return preview;
    }

    @Override
    public Widget entity(String label, SampleEntity entity, float size) {
        MinecraftEntityPreviewWidget preview = switch (entity) {
            // 1.7.10 has no entity registry entries: entities are created by their EntityList name.
            case ZOMBIE -> MinecraftWidgets.entityPreview(label, "Zombie");
            case PIG -> MinecraftWidgets.entityPreview(label, "Pig");
            case CREEPER -> MinecraftWidgets.entityPreview(label, "Creeper");
            case CHICKEN -> MinecraftWidgets.entityPreview(label, "Chicken");
            case PLAYER -> {
                if (Minecraft.getMinecraft().thePlayer == null) yield null;
                yield MinecraftWidgets.entityPreview(label, Minecraft.getMinecraft().thePlayer);
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

    private static ItemStack stack(SampleItem item) {
        return switch (item) {
            case DIAMOND -> new ItemStack(Items.diamond);
            case IRON_INGOT -> new ItemStack(Items.iron_ingot);
            case WORN_PICKAXE -> {
                ItemStack worn = new ItemStack(Items.diamond_pickaxe);
                worn.setItemDamage(900);
                yield worn;
            }
            // Potion metadata 8261 is "Potion of Healing".
            case HEALING_POTION -> new ItemStack(Items.potionitem, 1, 8261);
            case BOW -> new ItemStack(Items.bow);
            case EMERALD -> new ItemStack(Items.emerald);
            case ENCHANTED_SWORD -> {
                ItemStack sword = new ItemStack(Items.diamond_sword);
                sword.addEnchantment(Enchantment.sharpness, 3);
                yield sword;
            }
            case CLOCK -> new ItemStack(Items.clock);
            case COMPASS -> new ItemStack(Items.compass);
            case CHEST -> new ItemStack(Blocks.chest);
        };
    }
}
