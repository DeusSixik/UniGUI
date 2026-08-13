package dev.sixik.unigui.impl.render;

import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.render.DrawCommand;
import dev.sixik.unigui.api.render.DrawList;
import dev.sixik.unigui.api.render.RenderBackend;
import dev.sixik.unigui.api.render.RenderTarget;
import dev.sixik.unigui.api.render.RenderTargetOptions;
import dev.sixik.unigui.api.render.TextureHandle;
import dev.sixik.unigui.api.widget.Widget;

import java.util.Objects;

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

    public void renderWidget(Widget widget, RenderTarget target) {
        Objects.requireNonNull(widget, "widget");
        Objects.requireNonNull(target, "target");

        targetBounds.set(0.0f, 0.0f, target.width(), target.height());
        widget.arrange(targetBounds);

        drawList.clear();
        widget.render(renderContext);
        backend.render(drawList, target);
    }

    public void renderWidgetSnapshot(Widget widget, RenderTarget target, float originX, float originY) {
        Objects.requireNonNull(widget, "widget");
        Objects.requireNonNull(target, "target");

        drawList.clear();
        widget.render(renderContext);

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
            offsetDrawList.add(copy);
        }
        return offsetDrawList;
    }

    @Override
    public void close() {
        targetCache.close();
    }
}
