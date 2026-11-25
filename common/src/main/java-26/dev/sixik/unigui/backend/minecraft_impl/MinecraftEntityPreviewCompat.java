package dev.sixik.unigui.backend.minecraft_impl;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.LivingEntity;

/**
 * Превью сущности для Minecraft 26.1.
 *
 * <p>В 26.1 сущность в GUI рисуется через picture-in-picture: extractor ставит её в очередь, а
 * {@code GuiRenderer} рисует в отдельную текстуру и накладывает её на экран. Поэтому превью задаётся
 * прямоугольником, а поворот к курсору считает сам Minecraft относительно центра этого прямоугольника.</p>
 */
final class MinecraftEntityPreviewCompat {
    /** Вертикальный сдвиг модели, как у инвентаря игрока. */
    private static final float OFFSET_Y = 0.0625f;

    private MinecraftEntityPreviewCompat() {
    }

    /**
     * Ставит сущность в очередь GUI.
     *
     * @param graphics    extractor (поза должна быть уже выставлена)
     * @param x0          левый край в ванильных единицах
     * @param y0          верхний край
     * @param x1          правый край
     * @param y1          нижний край
     * @param entityScale масштаб модели
     * @param mouseX      X курсора в тех же единицах
     * @param mouseY      Y курсора в тех же единицах
     * @param entity      сущность
     */
    static void render(GuiGraphicsExtractor graphics, int x0, int y0, int x1, int y1, int entityScale,
                       float mouseX, float mouseY, LivingEntity entity) {
        InventoryScreen.extractEntityInInventoryFollowsMouse(graphics, x0, y0, x1, y1, entityScale, OFFSET_Y,
                mouseX, mouseY, entity);
    }
}
