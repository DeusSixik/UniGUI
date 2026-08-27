package dev.sixik.unigui.backend.minecraft_impl;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Quaternionf;
import org.joml.Vector3f;

final class MinecraftEntityPreviewCompat {
    private MinecraftEntityPreviewCompat() {
    }

    static void render(GuiGraphics graphics, int centerX, int bottomY, int entityScale, float size,
                       float mouseX, float mouseY, LivingEntity entity) {
        float angleX = (float) Math.atan(mouseX / 40.0f);
        float angleY = (float) Math.atan(mouseY / 40.0f);
        Quaternionf rotation = new Quaternionf().rotateZ((float) Math.PI);
        Quaternionf cameraRotation = new Quaternionf().rotateX(angleY * 20.0f * ((float) Math.PI / 180.0f));
        rotation.mul(cameraRotation);

        float bodyYaw = entity.yBodyRot;
        float yaw = entity.getYRot();
        float pitch = entity.getXRot();
        float previousHeadYaw = entity.yHeadRotO;
        float headYaw = entity.yHeadRot;

        entity.yBodyRot = 180.0f + angleX * 20.0f;
        entity.setYRot(180.0f + angleX * 40.0f);
        entity.setXRot(-angleY * 20.0f);
        entity.yHeadRot = entity.getYRot();
        entity.yHeadRotO = entity.getYRot();

        try {
            InventoryScreen.renderEntityInInventory(
                    graphics,
                    centerX,
                    bottomY,
                    entityScale,
                    new Vector3f(0.0f, 0.0f, 0.0f),
                    rotation,
                    cameraRotation,
                    entity);
        } finally {
            entity.yBodyRot = bodyYaw;
            entity.setYRot(yaw);
            entity.setXRot(pitch);
            entity.yHeadRotO = previousHeadYaw;
            entity.yHeadRot = headYaw;
        }
    }
}
