package dev.sixik.unigui.backend.minecraft;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.LivingEntity;

final class MinecraftEntityPreviewCompat {
    private MinecraftEntityPreviewCompat() {
    }

    static void render(GuiGraphics graphics, int centerX, int bottomY, int entityScale, float size,
                       float mouseX, float mouseY, LivingEntity entity) {
        InventoryScreen.renderEntityInInventoryFollowsMouse(
                graphics, centerX, bottomY, entityScale, mouseX, mouseY, entity);
    }
}
