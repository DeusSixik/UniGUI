package dev.sixik.unigui.widgets.world;

import dev.sixik.unigui.api.render.DrawScope;

/**
 * Рисуемый слой внутри {@link WorldCanvas}.
 *
 * <p>Слой нужен для фоновых/декоративных вещей, которые не являются отдельными
 * интерактивными виджетами: сетка, карта, линии маршрутов, подсветки, шейдерная
 * подложка, debug-геометрия. Если нужен кликабельный объект с hover/click/layout,
 * добавляй его через {@link WorldCanvas#anchorLayer()} как {@link AnchorWidget}.</p>
 *
 * <p>Важно: {@link DrawScope} принимает root/screen координаты, а не world
 * координаты. Поэтому внутри слоя обычно делают так:</p>
 *
 * <pre>{@code
 * canvas.addWorldLayer((world, draw) -> {
 *     float x = world.worldToRootX(worldX);
 *     float y = world.worldToRootY(worldY);
 *     draw.addCircleFilled(x, y, 4.0f, color);
 * });
 * }</pre>
 */
@FunctionalInterface
public interface WorldLayer {
    /**
     * Рендерит слой.
     *
     * @param canvas канвас, который вызывает слой; через него конвертируют world/local/root координаты
     * @param draw   объект для записи draw-команд в текущий frame
     */
    void render(WorldCanvas canvas, DrawScope draw);
}
