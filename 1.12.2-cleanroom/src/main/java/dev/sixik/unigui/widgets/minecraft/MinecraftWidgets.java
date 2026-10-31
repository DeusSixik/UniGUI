package dev.sixik.unigui.widgets.minecraft;

import dev.sixik.unigui.cleanroom.backend.CleanroomTextureHandle;
import dev.sixik.unigui.widgets.display.TextureWidget;
import net.minecraft.block.Block;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.EntityEntry;

/**
 * Concise factory entry points for Minecraft-backed UniGUI widgets.
 */
public final class MinecraftWidgets {
    private MinecraftWidgets() {
    }

    public static MinecraftItemPreviewWidget itemPreview(String label, Item item) {
        return new MinecraftItemPreviewWidget(label, item);
    }

    public static MinecraftItemPreviewWidget itemPreview(String label, Block block) {
        return new MinecraftItemPreviewWidget(label, block);
    }

    public static MinecraftItemPreviewWidget itemPreview(String label, ItemStack stack) {
        return new MinecraftItemPreviewWidget(label, stack);
    }

    public static MinecraftBlockPreviewWidget blockPreview(String label, Block block) {
        return new MinecraftBlockPreviewWidget(label, block);
    }

    public static MinecraftEntityPreviewWidget entityPreview(String label, EntityEntry entityType) {
        return new MinecraftEntityPreviewWidget(label, entityType);
    }

    public static MinecraftEntityPreviewWidget entityPreview(String label, EntityLivingBase entity) {
        return new MinecraftEntityPreviewWidget(label, null).entity(entity);
    }

    public static TextureWidget texture(ResourceLocation location, int width, int height) {
        return new TextureWidget(new CleanroomTextureHandle(location, width, height));
    }

    public static MinecraftItemPickerWidget itemPicker() {
        return new MinecraftItemPickerWidget();
    }

    public static MinecraftItemPickerWidget itemSelector() {
        return itemPicker();
    }

    public static MinecraftTexturePickerWidget texturePicker() {
        return new MinecraftTexturePickerWidget();
    }

    public static MinecraftTexturePickerWidget textureSelector() {
        return texturePicker();
    }
}