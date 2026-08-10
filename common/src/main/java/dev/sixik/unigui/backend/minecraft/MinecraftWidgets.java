package dev.sixik.unigui.backend.minecraft;

import dev.sixik.unigui.widgets.TextureWidget;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

/** Concise factory entry points for Minecraft-backed UniGUI widgets. */
public final class MinecraftWidgets {
    private MinecraftWidgets() {
    }

    public static MinecraftItemPreviewWidget itemPreview(String label, ItemLike item) {
        return new MinecraftItemPreviewWidget(label, item);
    }

    public static MinecraftItemPreviewWidget itemPreview(String label, ItemStack stack) {
        return new MinecraftItemPreviewWidget(label, stack);
    }

    public static TextureWidget texture(ResourceLocation location, int width, int height) {
        return new TextureWidget(new MinecraftTextureHandle(location, width, height));
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
