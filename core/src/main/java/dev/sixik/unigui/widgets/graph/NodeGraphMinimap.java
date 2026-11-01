package dev.sixik.unigui.widgets.graph;

/**
 * Геометрия миникарты: перевод между world-координатами графа и пикселями миникарты.
 *
 * <p>Миникарта вписывает в свою рамку габариты всех нод и регионов вместе с текущим видимым окном, чтобы
 * рамка окна всегда оставалась внутри. Объект считается заново на кадр и ничего не хранит между кадрами.</p>
 */
final class NodeGraphMinimap {
    private static final float WORLD_PADDING = 40.0f;

    final float x;
    final float y;
    final float width;
    final float height;
    private final float worldX;
    private final float worldY;
    private final float scale;
    private final float offsetX;
    private final float offsetY;

    /**
     * @param x левая граница миникарты на экране
     * @param y верхняя граница миникарты на экране
     * @param width ширина миникарты
     * @param height высота миникарты
     * @param minX левая граница содержимого в world-координатах
     * @param minY верхняя граница содержимого
     * @param maxX правая граница содержимого
     * @param maxY нижняя граница содержимого
     */
    NodeGraphMinimap(float x, float y, float width, float height,
                     float minX, float minY, float maxX, float maxY) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        float spanX = Math.max(1.0f, maxX - minX + WORLD_PADDING * 2.0f);
        float spanY = Math.max(1.0f, maxY - minY + WORLD_PADDING * 2.0f);
        this.scale = Math.min(width / spanX, height / spanY);
        this.worldX = minX - WORLD_PADDING;
        this.worldY = minY - WORLD_PADDING;
        this.offsetX = (width - spanX * scale) * 0.5f;
        this.offsetY = (height - spanY * scale) * 0.5f;
    }

    float toScreenX(float worldXValue) {
        return x + offsetX + (worldXValue - worldX) * scale;
    }

    float toScreenY(float worldYValue) {
        return y + offsetY + (worldYValue - worldY) * scale;
    }

    float toWorldX(float screenX) {
        return worldX + (screenX - x - offsetX) / scale;
    }

    float toWorldY(float screenY) {
        return worldY + (screenY - y - offsetY) / scale;
    }

    float scale(float worldLength) {
        return worldLength * scale;
    }

    boolean contains(float screenX, float screenY) {
        return screenX >= x && screenX <= x + width && screenY >= y && screenY <= y + height;
    }
}
