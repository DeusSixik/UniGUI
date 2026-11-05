package dev.sixik.unigui.widgets.world;


import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiLink;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;

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
@AiDoc(summary = "Functional contract for background or decorative rendering layers drawn inside WorldCanvas.")
@WikiPage(
        title = "WorldLayer",
        slug = "world-layer",
        category = "<key:world.category>",
        summary = "<key:world.layer.summary>"
)
@WikiSection(
        title = "<key:wiki.section.overview.title>",
        markdown = "<key:world.layer.section.overview.body>",
        order = 10
)
@WikiExample(
        title = "<key:world.layer.example.basic.title>",
        code = "canvas.addWorldLayer((world, draw) -> {\n    float x = world.worldToRootX(100.0f);\n    float y = world.worldToRootY(200.0f);\n    draw.addCircleFilled(x, y, 6.0f, MutableColor.ofRgb(0xFF0000));\n});"
)
@WikiLink(
        label = "WorldCanvas",
        target = "page:world-canvas",
        description = "<key:world.canvas.summary>"
)
@WikiLink(
        label = "RibbonLayer",
        target = "page:ribbon-layer",
        description = "<key:ribbon.layer.summary>"
)
@FunctionalInterface
public interface WorldLayer {
    /**
     * Рендерит слой.
     *
     * @param canvas канвас, который вызывает слой; через него конвертируют world/local/root координаты
     * @param draw   объект для записи draw-команд в текущий frame
     */
    @AiDoc(summary = "Renders this world layer to the active frame draw scope.")
    @WikiMethod(summary = "<key:world.layer.method.render>")
    void render(
            @WikiParameter(description = "<key:world.layer.param.canvas>")
            WorldCanvas canvas,
            @WikiParameter(description = "<key:world.layer.param.draw>")
            DrawScope draw
    );
}
