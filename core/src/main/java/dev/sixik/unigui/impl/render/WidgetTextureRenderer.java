package dev.sixik.unigui.impl.render;

import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.render.DrawCommand;
import dev.sixik.unigui.api.render.DrawList;
import dev.sixik.unigui.api.render.RenderBackend;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.render.RenderTarget;
import dev.sixik.unigui.api.render.RenderTargetOptions;
import dev.sixik.unigui.api.render.TextureHandle;
import dev.sixik.unigui.api.widget.Widget;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Рендерит виджеты и произвольные draw-команды в offscreen target, который держит у себя (один на renderer).
 */
public final class WidgetTextureRenderer implements AutoCloseable {
    private final RenderBackend backend;
    private final RenderTargetCache targetCache;
    private final DrawList drawList = new DrawList();
    private final DrawList offsetDrawList = new DrawList();
    private final DefaultRenderContext renderContext = new DefaultRenderContext(drawList);
    private final MutableRect targetBounds = new MutableRect();

    public WidgetTextureRenderer(RenderBackend backend) {
        this.backend = Objects.requireNonNull(backend, "backend");
        this.targetCache = new RenderTargetCache(backend);
        this.renderContext.backend(backend);
    }

    public TextureHandle renderWidgetToTexture(Widget widget, int width, int height) {
        return renderWidgetToTexture(widget, width, height, RenderTargetOptions.COLOR);
    }

    public TextureHandle renderWidgetToTexture(Widget widget, int width, int height, RenderTargetOptions options) {
        return renderWidgetToTarget(widget, width, height, options).colorTexture();
    }

    public TextureHandle renderWidgetToTexture(Widget widget, int width, int height, float originX, float originY) {
        return renderWidgetToTexture(widget, width, height, originX, originY, RenderTargetOptions.COLOR);
    }

    public TextureHandle renderWidgetToTexture(Widget widget, int width, int height, float originX, float originY, RenderTargetOptions options) {
        return renderWidgetToTarget(widget, width, height, originX, originY, options).colorTexture();
    }

    public TextureHandle renderDrawListToTexture(DrawList drawList, int width, int height, RenderTargetOptions options) {
        return renderDrawListToTarget(drawList, width, height, options).colorTexture();
    }

    public RenderTarget renderWidgetToTarget(Widget widget, int width, int height) {
        return renderWidgetToTarget(widget, width, height, RenderTargetOptions.COLOR);
    }

    public RenderTarget renderWidgetToTarget(Widget widget, int width, int height, RenderTargetOptions options) {
        RenderTarget target = targetCache.acquire(width, height, options);
        renderWidget(widget, target);
        return target;
    }

    public RenderTarget renderWidgetToTarget(Widget widget, int width, int height, float originX, float originY) {
        return renderWidgetToTarget(widget, width, height, originX, originY, RenderTargetOptions.COLOR);
    }

    public RenderTarget renderWidgetToTarget(Widget widget, int width, int height, float originX, float originY, RenderTargetOptions options) {
        RenderTarget target = targetCache.acquire(width, height, options);
        renderWidgetSnapshot(widget, target, originX, originY);
        return target;
    }

    public RenderTarget renderDrawListToTarget(DrawList drawList, int width, int height, RenderTargetOptions options) {
        Objects.requireNonNull(drawList, "drawList");

        RenderTarget target = targetCache.acquire(width, height, options);
        backend.render(drawList, target);
        return target;
    }

    /**
     * Рендерит в target то, что нарисует {@code painter}.
     *
     * <p>{@code painter} пишет команды в логических координатах: точка {@code (0, 0)} попадает в левый верхний
     * угол текстуры, а одна логическая единица занимает {@code coordinateScale} пикселей. Внутри painter'а
     * {@link RenderContext#pixelScale()} равен {@code coordinateScale}, так что вложенные offscreen-виджеты тоже
     * выбирают чёткое разрешение.</p>
     *
     * @param width           ширина target'а в пикселях
     * @param height          высота target'а в пикселях
     * @param options         параметры target'а
     * @param coordinateScale пикселей target'а на логическую единицу
     * @param painter         пишет команды в переданный context
     * @return target с результатом (принадлежит renderer'у, живёт до следующего вызова с другим размером)
     */
    public RenderTarget renderToTarget(int width, int height, RenderTargetOptions options, float coordinateScale,
                                       Consumer<RenderContext> painter) {
        return renderToTarget(width, height, options, coordinateScale, painter, null);
    }

    /**
     * Исполняет сырой проход, затем записывает и рисует 2D-команды в ту же цель.
     *
     * @param width           физическая ширина цели.
     * @param height          физическая высота цели.
     * @param options         параметры цели. Сырой проход требует выключенной автоочистки перед 2D.
     * @param coordinateScale число физических пикселей на единицу UI.
     * @param painter         записывает 2D-команды в контекст.
     * @param rawPass         рисует напрямую в цель перед исполнением 2D либо равен null.
     * @return цель, которой владеет этот рендерер.
     */
    public RenderTarget renderToTarget(int width, int height, RenderTargetOptions options, float coordinateScale,
                                       Consumer<RenderContext> painter, Consumer<RenderTarget> rawPass) {
        Objects.requireNonNull(painter, "painter");
        RenderTarget target = targetCache.acquire(width, height, options);
        if (rawPass != null) rawPass.accept(target);
        drawList.clear();
        renderContext.pixelScale(coordinateScale);
        try {
            painter.accept(renderContext);
        } finally {
            renderContext.pixelScale(1.0f);
        }
        backend.render(drawList, target, coordinateScale);
        return target;
    }

    /**
     * @return текущий target или {@code null}, если ещё ничего не рендерилось
     */
    public RenderTarget currentTarget() {
        return targetCache.current();
    }

    public void renderWidget(Widget widget, RenderTarget target) {
        Objects.requireNonNull(widget, "widget");
        Objects.requireNonNull(target, "target");

        targetBounds.set(0.0f, 0.0f, target.width(), target.height());
        widget.arrange(targetBounds);

        drawList.clear();
        widget.renderCached(renderContext);
        backend.render(drawList, target);
    }

    public void renderWidgetSnapshot(Widget widget, RenderTarget target, float originX, float originY) {
        Objects.requireNonNull(widget, "widget");
        Objects.requireNonNull(target, "target");

        drawList.clear();
        widget.renderCached(renderContext);

        DrawList targetDrawList = drawList;
        if (originX != 0.0f || originY != 0.0f) {
            targetDrawList = offset(drawList, originX, originY);
        }

        backend.render(targetDrawList, target);
    }

    private DrawList offset(DrawList source, float originX, float originY) {
        offsetDrawList.clear();
        Object[] rawCommands = source.commandElements();
        for (int i = 0, size = source.size(); i < size; i++) {
            DrawCommand command = (DrawCommand) rawCommands[i];
            DrawCommand copy = command.copy();
            RectView bounds = copy.bounds();
            copy.bounds().set(bounds.x() - originX, bounds.y() - originY, bounds.width(), bounds.height());
            if (copy.hasBrushBounds()) {
                RectView brush = copy.brushBounds();
                copy.brushBounds(brush.x() - originX, brush.y() - originY, brush.width(), brush.height());
            }
            offsetDrawList.add(copy);
        }
        return offsetDrawList;
    }

    @Override
    public void close() {
        targetCache.close();
    }
}
