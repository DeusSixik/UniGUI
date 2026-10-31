package dev.sixik.unigui.widgets.minecraft;

import dev.sixik.unigui.gtnh.backend.GtnhTextureHandle;
import dev.sixik.unigui.widgets.display.TextureWidget;
import net.minecraft.block.Block;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

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

    /**
     * @param entityType name the entity is registered under in {@code EntityList} (1.7.10 has no entity registry
     *                   entries), for example {@code "Zombie"}
     */
    public static MinecraftEntityPreviewWidget entityPreview(String label, String entityType) {
        return new MinecraftEntityPreviewWidget(label, entityType);
    }

    public static MinecraftEntityPreviewWidget entityPreview(String label, EntityLivingBase entity) {
        return new MinecraftEntityPreviewWidget(label, null).entity(entity);
    }

    public static TextureWidget texture(ResourceLocation location, int width, int height) {
        return new TextureWidget(new GtnhTextureHandle(location, width, height));
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