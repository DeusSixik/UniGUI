package dev.sixik.unigui.widgets;

import dev.sixik.unigui.api.core.FrameContext;
import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.core.UIContext;
import dev.sixik.unigui.api.debug.DebugFlags;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.layout.LayoutSize;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.render.RenderBackend;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.render.RenderTargetOptions;
import dev.sixik.unigui.api.render.TextureHandle;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.impl.render.WidgetTextureRenderer;
import dev.sixik.unigui.impl.widget.WidgetBase;

import java.util.Collections;
import java.util.List;

public final class CachedSubtreeWidget extends WidgetBase {
    private static final MutableColor DEBUG_HIT_COLOR = new MutableColor(0.15f, 0.85f, 0.25f, 0.9f);
    private static final MutableColor DEBUG_MISS_COLOR = new MutableColor(1.0f, 0.55f, 0.05f, 0.9f);
    private static final MutableColor DEBUG_TEXT_COLOR = new MutableColor(1.0f, 1.0f, 1.0f, 1.0f);
    private static final MutableColor DEBUG_BACKGROUND_COLOR = new MutableColor(0.0f, 0.0f, 0.0f, 0.55f);

    private final MutableColor tint = new MutableColor(1.0f, 1.0f, 1.0f, 1.0f);
    private Widget content;
    private WidgetTextureRenderer textureRenderer;
    private RenderBackend rendererBackend;
    private RenderTargetOptions targetOptions = RenderTargetOptions.COLOR;
    private TextureHandle cachedTexture;
    private int cachedWidth;
    private int cachedHeight;
    private boolean textureDirty = true;
    private boolean targetOptionsChanged;
    private boolean backendChanged;
    private long renderCalls;
    private long cacheHits;
    private long cacheMisses;
    private long textureRenders;
    private CachedSubtreeMissReason lastMissReason = CachedSubtreeMissReason.NONE;

    public CachedSubtreeWidget() {
        tint.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
    }

    public CachedSubtreeWidget(Widget content) {
        this();
        content(content);
    }

    public Widget content() {
        return content;
    }

    public CachedSubtreeWidget content(Widget content) {
        if (this.content == content) return this;
        if (content == this) throw new IllegalArgumentException("Cached subtree cannot contain itself");

        detachContent();
        this.content = content;
        attachContent(content);
        markTextureDirty();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.TEXTURE);
        return this;
    }

    public MutableColor tint() {
        return tint;
    }

    public RenderTargetOptions targetOptions() {
        return targetOptions;
    }

    public CachedSubtreeWidget targetOptions(RenderTargetOptions targetOptions) {
        RenderTargetOptions safeOptions = targetOptions == null ? RenderTargetOptions.COLOR : targetOptions;
        if (safeOptions.equals(this.targetOptions)) return this;
        this.targetOptions = safeOptions;
        resetRenderer();
        targetOptionsChanged = true;
        markTextureDirty();
        invalidate(InvalidationFlags.TEXTURE);
        return this;
    }

    public TextureHandle cachedTexture() {
        return cachedTexture;
    }

    public CachedSubtreeStats cacheStats() {
        return new CachedSubtreeStats(renderCalls, cacheHits, cacheMisses, textureRenders,
                cachedWidth, cachedHeight, lastMissReason);
    }

    public void resetCacheStats() {
        renderCalls = 0L;
        cacheHits = 0L;
        cacheMisses = 0L;
        textureRenders = 0L;
        lastMissReason = CachedSubtreeMissReason.NONE;
    }

    public void markTextureDirty() {
        textureDirty = true;
    }

    @Override
    public void setUiContextInternal(UIContext uiContext) {
        super.setUiContextInternal(uiContext);
        attachContent(content);
    }

    @Override
    public List<Widget> children() {
        return content == null ? Collections.emptyList() : List.of(content);
    }

    @Override
    public void measure(LayoutContext context) {
        if (content == null) {
            setDesiredSize(resolveDesiredSize(context, 0.0f, 0.0f));
            return;
        }
        if (visibility() == dev.sixik.unigui.api.widget.Visibility.COLLAPSED) {
            setDesiredSize(LayoutSize.ZERO);
            return;
        }
        if (content != null) {
            content.measure(context);
            setDesiredSize(resolveDesiredSize(context, content.desiredSize().width(), content.desiredSize().height()));
        }
    }

    @Override
    public void arrange(RectView bounds) {
        super.arrange(bounds);
        if (content != null) {
            content.arrange(bounds);
        }
    }

    @Override
    public void tick(FrameContext frame) {
        super.tick(frame);
        if (content != null) {
            content.tick(frame);
        }
    }

    @Override
    public void render(RenderContext context) {
        if (content == null) return;
        pushOpacity(context);
        try {
            renderCachedContent(context);
        } finally {
            popOpacity(context);
        }
    }

    private void renderCachedContent(RenderContext context) {
        renderCalls++;

        RenderBackend backend = context.backend();
        if (backend == null) {
            content.render(context);
            return;
        }

        WidgetTextureRenderer renderer = rendererFor(backend);
        int width = Math.max(1, Math.round(Math.abs(layoutBounds().width())));
        int height = Math.max(1, Math.round(Math.abs(layoutBounds().height())));
        CachedSubtreeMissReason missReason = missReason(width, height);

        if (missReason != CachedSubtreeMissReason.NONE) {
            cacheMisses++;
            textureRenders++;
            lastMissReason = missReason;
            recordCacheMiss(missReason);
            cachedTexture = renderer.renderWidgetToTexture(content, width, height, layoutBounds().x(), layoutBounds().y(), targetOptions);
            cachedWidth = width;
            cachedHeight = height;
            textureDirty = false;
            targetOptionsChanged = false;
            backendChanged = false;
            clearSubtreeInvalidation(content);
            clearInvalidation(InvalidationFlags.LAYOUT | InvalidationFlags.TEXTURE);
        } else {
            cacheHits++;
            recordCacheHit();
        }

        if (cachedTexture != null) {
            context.texture(cachedTexture,
                    layoutBounds().x(),
                    layoutBounds().y(),
                    layoutBounds().width(),
                    layoutBounds().height(),
                    Paint.fill(tint),
                    transform());
        }

        renderDebugOverlay(context, missReason);
    }

    @Override
    public void dispose() {
        resetRenderer();
        if (content != null) {
            content.dispose();
        }
        detachContent();
        content = null;
    }

    private CachedSubtreeMissReason missReason(int width, int height) {
        if (backendChanged) return CachedSubtreeMissReason.BACKEND_CHANGED;
        if (targetOptionsChanged) return CachedSubtreeMissReason.TARGET_OPTIONS_CHANGED;
        if (cachedTexture == null) return CachedSubtreeMissReason.NO_TEXTURE;
        if (textureDirty) return CachedSubtreeMissReason.MANUAL_DIRTY;
        if (cachedWidth != width || cachedHeight != height) return CachedSubtreeMissReason.RESIZED;
        if (InvalidationFlags.has(invalidationFlags(), InvalidationFlags.LAYOUT)) return CachedSubtreeMissReason.OWN_LAYOUT_DIRTY;
        if (InvalidationFlags.has(invalidationFlags(), InvalidationFlags.TEXTURE)) return CachedSubtreeMissReason.OWN_TEXTURE_DIRTY;
        if (content.subtreeInvalidationFlags() != InvalidationFlags.NONE) return CachedSubtreeMissReason.CHILD_SUBTREE_DIRTY;
        return CachedSubtreeMissReason.NONE;
    }

    private WidgetTextureRenderer rendererFor(RenderBackend backend) {
        if (textureRenderer == null || rendererBackend != backend) {
            resetRenderer();
            textureRenderer = new WidgetTextureRenderer(backend);
            rendererBackend = backend;
            cachedTexture = null;
            textureDirty = true;
            backendChanged = true;
        }
        return textureRenderer;
    }

    private void resetRenderer() {
        if (textureRenderer != null) {
            textureRenderer.close();
            textureRenderer = null;
        }
        rendererBackend = null;
        cachedTexture = null;
        cachedWidth = 0;
        cachedHeight = 0;
        backendChanged = false;
    }

    private void attachContent(Widget widget) {
        if (widget instanceof WidgetBase base) {
            base.setParentInternal(this);
            base.setUiContextInternal(uiContext());
        }
    }

    private void detachContent() {
        Widget previous = content;
        content = null;
        if (previous instanceof WidgetBase base) {
            base.setParentInternal(null);
            base.setUiContextInternal(null);
        }
    }

    private static void clearSubtreeInvalidation(Widget widget) {
        if (widget == null) return;
        for (Widget child : widget.children()) {
            clearSubtreeInvalidation(child);
        }
        widget.clearInvalidation(InvalidationFlags.ALL);
    }

    private void renderDebugOverlay(RenderContext context, CachedSubtreeMissReason missReason) {
        UIContext uiContext = uiContext();
        if (uiContext == null || !DebugFlags.has(uiContext.debugFlags(), DebugFlags.CACHED_SUBTREE)) return;

        boolean hit = missReason == CachedSubtreeMissReason.NONE && cachedTexture != null;
        float x = layoutBounds().x();
        float y = layoutBounds().y();
        float width = layoutBounds().width();
        float overlayWidth = Math.min(Math.max(96.0f, Math.abs(width)), 220.0f);
        String state = hit ? "HIT" : "MISS " + lastMissReason;
        String stats = "rt=" + textureRenders + " hit=" + cacheHits + " miss=" + cacheMisses + " " + cachedWidth + "x" + cachedHeight;

        context.rect(x, y, overlayWidth, 22.0f, Paint.fill(DEBUG_BACKGROUND_COLOR), transform());
        context.rect(x, y, width, layoutBounds().height(), Paint.stroke(hit ? DEBUG_HIT_COLOR : DEBUG_MISS_COLOR, 1.0f), transform());
        context.text("cache " + state, x + 3.0f, y + 3.0f, overlayWidth - 6.0f, 9.0f, Paint.fill(hit ? DEBUG_HIT_COLOR : DEBUG_MISS_COLOR), transform());
        context.text(stats, x + 3.0f, y + 13.0f, overlayWidth - 6.0f, 9.0f, Paint.fill(DEBUG_TEXT_COLOR), transform());
    }

    private void recordCacheHit() {
        UIContext uiContext = uiContext();
        if (uiContext != null) {
            uiContext.debugCounters().recordTextureCacheHit();
        }
    }

    private void recordCacheMiss(CachedSubtreeMissReason reason) {
        UIContext uiContext = uiContext();
        if (uiContext != null) {
            uiContext.debugCounters().recordTextureCacheMiss(reason.name());
            uiContext.debugCounters().recordTextureRender();
        }
    }
}
