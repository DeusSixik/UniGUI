package dev.sixik.unigui.widgets.caching;

import dev.sixik.unigui.api.core.UIContext;
import dev.sixik.unigui.api.debug.DebugFlags;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.render.RenderTargetOptions;
import dev.sixik.unigui.api.render.TextureFilter;
import dev.sixik.unigui.api.render.TextureHandle;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.widgets.rendering.RenderToTexture;
import dev.sixik.unigui.xaml.XamlWidget;

/**
 * Кэширует дочернее поддерево в текстуре и перерисовывает её только при изменениях.
 *
 * <p>Частный случай {@link RenderToTexture}: содержимое — один дочерний виджет, режим
 * {@link RenderToTexture.UpdateMode#ON_CHANGE}, текстура показывается на месте виджета. Сверху добавлены
 * статистика попаданий в кэш ({@link #cacheStats()}) и отладочная плашка ({@link DebugFlags#CACHED_SUBTREE}).</p>
 */
@XamlWidget("CachedSubtreeWidget")
public final class CachedSubtreeWidget extends RenderToTexture {
    public static final String STYLE_TYPE = dev.sixik.unigui.api.style.StyleIds.Widget.CACHED_SUBTREE_WIDGET;

    private static final MutableColor DEBUG_HIT_COLOR = new MutableColor(0.15f, 0.85f, 0.25f, 0.9f);
    private static final MutableColor DEBUG_MISS_COLOR = new MutableColor(1.0f, 0.55f, 0.05f, 0.9f);
    private static final MutableColor DEBUG_TEXT_COLOR = new MutableColor(1.0f, 1.0f, 1.0f, 1.0f);
    private static final MutableColor DEBUG_BACKGROUND_COLOR = new MutableColor(0.0f, 0.0f, 0.0f, 0.55f);

    private long renderCalls;
    private long cacheHits;
    private long cacheMisses;
    private long textureRenders;
    private CachedSubtreeMissReason lastMissReason = CachedSubtreeMissReason.NONE;

    public CachedSubtreeWidget() {
        updateMode(UpdateMode.ON_CHANGE);
        // Текстура совпадает с экраном пиксель в пиксель, NEAREST копирует её без размытия.
        filter(TextureFilter.NEAREST);
    }

    public CachedSubtreeWidget(Widget content) {
        this();
        content(content);
    }

    @Override
    public CachedSubtreeWidget content(Widget content) {
        if (content == this) throw new IllegalArgumentException("Cached subtree cannot contain itself");
        super.content(content);
        return this;
    }

    @Override
    public CachedSubtreeWidget targetOptions(RenderTargetOptions targetOptions) {
        super.targetOptions(targetOptions);
        return this;
    }

    /**
     * @return текстура кэша или {@code null}, пока поддерево ни разу не отрисовано
     */
    public TextureHandle cachedTexture() {
        return texture();
    }

    public CachedSubtreeStats cacheStats() {
        return new CachedSubtreeStats(renderCalls, cacheHits, cacheMisses, textureRenders,
                textureWidth(), textureHeight(), lastMissReason);
    }

    public void resetCacheStats() {
        renderCalls = 0L;
        cacheHits = 0L;
        cacheMisses = 0L;
        textureRenders = 0L;
        lastMissReason = CachedSubtreeMissReason.NONE;
    }

    /**
     * Перерисует кэш при следующей отрисовке.
     */
    public void markTextureDirty() {
        requestRender();
    }

    @Override
    public void render(RenderContext context) {
        if (content() != null) renderCalls++;
        super.render(context);
    }

    @Override
    protected void afterTextureUpdate(RenderReason reason) {
        UIContext ui = uiContext();
        if (reason == RenderReason.NONE) {
            cacheHits++;
            if (ui != null) ui.debugCounters().recordTextureCacheHit();
            return;
        }
        CachedSubtreeMissReason missReason = missReason(reason);
        cacheMisses++;
        textureRenders++;
        lastMissReason = missReason;
        if (ui != null) {
            ui.debugCounters().recordTextureCacheMiss(missReason.name());
            ui.debugCounters().recordTextureRender();
        }
    }

    @Override
    protected void renderOverlay(DrawScope draw, RenderReason reason) {
        UIContext ui = uiContext();
        if (ui == null || !DebugFlags.has(ui.debugFlags(), DebugFlags.CACHED_SUBTREE)) return;

        RectView bounds = layoutBounds();
        float x = bounds.x();
        float y = bounds.y();
        float w = bounds.width();
        float h = bounds.height();
        boolean hit = reason == RenderReason.NONE && texture() != null;
        MutableColor debugColor = hit ? DEBUG_HIT_COLOR : DEBUG_MISS_COLOR;
        float overlayWidth = Math.min(Math.max(96.0f, Math.abs(w)), 220.0f);
        String stateText = hit ? "HIT" : "MISS " + lastMissReason;
        String statsText = "rt=" + textureRenders + " hit=" + cacheHits + " miss=" + cacheMisses + " "
                + textureWidth() + "x" + textureHeight();
        draw.rect(x, y, overlayWidth, 22.0f, Paint.fill(DEBUG_BACKGROUND_COLOR));
        draw.rect(x, y, w, h, Paint.stroke(debugColor, 1.0f));
        draw.text("cache " + stateText, x + 3.0f, y + 3.0f, overlayWidth - 6.0f, 9.0f, Paint.fill(debugColor));
        draw.text(statsText, x + 3.0f, y + 13.0f, overlayWidth - 6.0f, 9.0f, Paint.fill(DEBUG_TEXT_COLOR));
    }

    private static CachedSubtreeMissReason missReason(RenderReason reason) {
        return switch (reason) {
            case NONE -> CachedSubtreeMissReason.NONE;
            case NO_TEXTURE -> CachedSubtreeMissReason.NO_TEXTURE;
            case REQUESTED, EVERY_FRAME -> CachedSubtreeMissReason.MANUAL_DIRTY;
            case RESIZED -> CachedSubtreeMissReason.RESIZED;
            case TARGET_OPTIONS_CHANGED -> CachedSubtreeMissReason.TARGET_OPTIONS_CHANGED;
            case BACKEND_CHANGED -> CachedSubtreeMissReason.BACKEND_CHANGED;
            case OWN_LAYOUT_DIRTY -> CachedSubtreeMissReason.OWN_LAYOUT_DIRTY;
            case OWN_TEXTURE_DIRTY -> CachedSubtreeMissReason.OWN_TEXTURE_DIRTY;
            case CHILD_SUBTREE_DIRTY -> CachedSubtreeMissReason.CHILD_SUBTREE_DIRTY;
        };
    }
}
