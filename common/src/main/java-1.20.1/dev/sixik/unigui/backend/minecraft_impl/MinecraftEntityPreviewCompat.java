package dev.sixik.unigui.backend.minecraft_impl;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Quaternionf;

/**
 * Детерминированный фронтальный рендер живой сущности для запечки иконок.
 *
 * <p>В отличие от {@code InventoryScreen.renderEntityInInventoryFollowsMouse}, поворот
 * не зависит от положения курсора (в offscreen-пекарне «мыши» нет), а второй кватернион
 * камеры передаётся как {@code null}: ванильный метод тогда пропускает
 * {@code EntityRenderDispatcher.overrideCameraOrientation} и состояние диспетчера
 * не протекает в последующий рендер мира. Углы сущности сохраняются/восстанавливаются,
 * как в 1.21-варианте.</p>
 */
final class MinecraftEntityPreviewCompat {
    private MinecraftEntityPreviewCompat() {
    }

    static void render(GuiGraphics graphics, int centerX, int bottomY, int entityScale, float size,
                       float mouseX, float mouseY, LivingEntity entity) {
        float angleX = (float) Math.atan(mouseX / 40.0f);
        float angleY = (float) Math.atan(mouseY / 40.0f);
        // Обязательный флип модели (иначе рисуется «вверх ногами») + лёгкий наклон камеры.
        Quaternionf rotation = new Quaternionf().rotateZ((float) Math.PI);
        rotation.mul(new Quaternionf().rotateX(angleY * 20.0f * ((float) Math.PI / 180.0f)));

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
                    graphics, centerX, bottomY, entityScale, rotation, null, entity);
        } finally {
            entity.yBodyRot = bodyYaw;
            entity.setYRot(yaw);
            entity.setXRot(pitch);
            entity.yHeadRotO = previousHeadYaw;
            entity.yHeadRot = headYaw;
        }
    }
}
