package dev.sixik.unigui.widgets.caching;

import dev.sixik.unigui.api.core.FrameContext;
import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.core.UIContext;
import dev.sixik.unigui.api.debug.DebugFlags;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.layout.LayoutSize;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.render.RenderBackend;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.render.RenderTargetOptions;
import dev.sixik.unigui.api.render.TextureHandle;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.api.widget.skin.WidgetsRender;
import dev.sixik.unigui.api.xml.XmlWidgetName;
import dev.sixik.unigui.impl.render.WidgetTextureRenderer;
import dev.sixik.unigui.impl.widget.WidgetBase;
import dev.sixik.unigui.widgets.render.CachedSubtreeRenderer;
import dev.sixik.unigui.widgets.render.CachedSubtreeState;

import java.util.Collections;
import java.util.List;
import dev.sixik.unigui.widgets.containers.StackPanel;

@XmlWidgetName("CachedSubtreeWidget")
public final class CachedSubtreeWidget extends WidgetBase {
    public static final String STYLE_TYPE = dev.sixik.unigui.api.style.StyleIds.Widget.CACHED_SUBTREE_WIDGET;

    private static final MutableColor DEBUG_HIT_COLOR = new MutableColor(0.15f, 0.85f, 0.25f, 0.9f);
    private static final MutableColor DEBUG_MISS_COLOR = new MutableColor(1.0f, 0.55f, 0.05f, 0.9f);
    private static final MutableColor DEBUG_TEXT_COLOR = new MutableColor(1.0f, 1.0f, 1.0f, 1.0f);
    private static final MutableColor DEBUG_BACKGROUND_COLOR = new MutableColor(0.0f, 0.0f, 0.0f, 0.55f);

    private final MutableColor tint = new MutableColor(1.0f, 1.0f, 1.0f, 1.0f);
    private CachedSubtreeRenderer renderer;
    private Widget content;
    private List<Widget> childrenView = Collections.emptyList();
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
        childrenView = content == null ? Collections.emptyList() : Collections.singletonList(content);
        markTextureDirty();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.TEXTURE);
        return this;
    }

    public MutableColor tint() {
        return tint;
    }

    public CachedSubtreeRenderer renderer() {
        return renderer;
    }

    public CachedSubtreeWidget renderer(CachedSubtreeRenderer renderer) {
        if (this.renderer == renderer) return this;
        this.renderer = renderer;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public CachedSubtreeWidget useDefaultRenderer() {
        return renderer(null);
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
        return childrenView;
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
            setDesiredSize(resolveDesiredSize(context,
                    StackPanel.preferredWidth(content, 0.0f),
                    StackPanel.preferredHeight(content, 0.0f)));
        }
    }

    @Override
    public void arrange(RectView bounds) {
        super.arrange(bounds);
        if (content != null && content.visibility() != Visibility.COLLAPSED) {
            StackPanel.arrangeChild(content,
                    bounds.x(), bounds.y(),
                    bounds.width(), bounds.height());
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
            renderChildWithInheritedTransform(context, content);
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

        effectiveRenderer().render(new DrawScope(context, transform(), layoutBounds()), cachedSubtreeState(missReason));
    }

    @Override
    public void dispose() {
        resetRenderer();
        if (content != null) {
            content.dispose();
        }
        detachContent();
        content = null;
        childrenView = Collections.emptyList();
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
        childrenView = Collections.emptyList();
    }

    private static void clearSubtreeInvalidation(Widget widget) {
        if (widget == null) return;
        for (Widget child : widget.children()) {
            clearSubtreeInvalidation(child);
        }
        widget.clearInvalidation(InvalidationFlags.ALL);
    }

    private CachedSubtreeRenderer effectiveRenderer() {
        return renderer == null ? styleRenderer(CachedSubtreeRenderer.class, WidgetsRender.cachedSubtree()) : renderer;
    }

    private CachedSubtreeState cachedSubtreeState(CachedSubtreeMissReason missReason) {
        UIContext uiContext = uiContext();
        boolean debugVisible = uiContext != null && DebugFlags.has(uiContext.debugFlags(), DebugFlags.CACHED_SUBTREE);
        boolean hit = missReason == CachedSubtreeMissReason.NONE && cachedTexture != null;
        float x = layoutBounds().x();
        float y = layoutBounds().y();
        float width = layoutBounds().width();
        float overlayWidth = Math.min(Math.max(96.0f, Math.abs(width)), 220.0f);
        String state = hit ? "HIT" : "MISS " + lastMissReason;
        String stats = "rt=" + textureRenders + " hit=" + cacheHits + " miss=" + cacheMisses + " " + cachedWidth + "x" + cachedHeight;
        return new CachedSubtreeState(
                x,
                y,
                width,
                layoutBounds().height(),
                cachedTexture,
                tint.copy(),
                debugVisible,
                hit,
                missReason,
                state,
                stats,
                overlayWidth,
                DEBUG_HIT_COLOR.copy(),
                DEBUG_MISS_COLOR.copy(),
                DEBUG_TEXT_COLOR.copy(),
                DEBUG_BACKGROUND_COLOR.copy());
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
