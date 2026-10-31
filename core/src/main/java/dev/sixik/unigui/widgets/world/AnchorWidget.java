package dev.sixik.unigui.widgets.world;


import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiLink;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.widget.Widget;

/**
 * Настройка одного виджета, закрепленного за world-точкой {@link WorldCanvas}.
 *
 * <p>{@code AnchorWidget} не является самим UI-виджетом. Это "ручка" над
 * виджетом: где он находится в world/map координатах, какого он экранного
 * размера, где его pivot и когда он видим.</p>
 *
 * <p>Anchor полезен для маркеров карты: позиция живет в координатах карты, а
 * размер и интерактивность остаются screen-space. Поэтому иконка не обязана
 * масштабироваться вместе с картой, если ты задал {@link #screenSize(float, float)}.</p>
 */
@AiDoc(summary = "Spatial anchor pinning a screen-space UI widget to 3D/2D world coordinates with zoom scaling, pivot, and viewport culling.")
@WikiPage(
        title = "AnchorWidget",
        slug = "anchor-widget",
        category = "<key:world.category>",
        summary = "<key:anchor.widget.summary>"
)
@WikiSection(
        title = "<key:wiki.section.overview.title>",
        markdown = "<key:anchor.widget.section.overview.body>",
        order = 10
)
@WikiExample(
        title = "<key:anchor.widget.example.basic.title>",
        code = "AnchorWidget anchor = canvas.anchorLayer().add(\"pin\", 100, 200, new Button(\"Marker\"))\n    .screenSize(64, 24)\n    .pivot(0.5f, 1.0f);"
)
@WikiLink(
        label = "AnchorLayer",
        target = "page:anchor-layer",
        description = "<key:anchor.layer.summary>"
)
@WikiLink(
        label = "WorldCanvas",
        target = "page:world-canvas",
        description = "<key:world.canvas.summary>"
)
public final class AnchorWidget {
    private final String id;
    private final Widget widget;
    private AnchorLayer owner;
    private float worldX;
    private float worldY;
    private float screenWidth = -1.0f;
    private float screenHeight = -1.0f;
    private float minScreenWidth = -1.0f;
    private float minScreenHeight = -1.0f;
    private float maxScreenWidth = -1.0f;
    private float maxScreenHeight = -1.0f;
    private boolean zoomScaling;
    private float referenceZoom = 1.0f;
    private float pivotX = 0.5f;
    private float pivotY = 0.5f;
    private float minVisibleZoom = 0.0f;
    private float maxVisibleZoom = Float.POSITIVE_INFINITY;
    private boolean visible = true;
    private boolean cullOutsideViewport = true;
    private boolean arrangedVisible = true;
    private float projectedRootX;
    private float projectedRootY;

    AnchorWidget(String id, float worldX, float worldY, Widget widget) {
        this.id = id == null ? "" : id;
        this.worldX = sanitize(worldX);
        this.worldY = sanitize(worldY);
        this.widget = widget;
    }

    /**
     * Уникальный id anchor внутри {@link AnchorLayer}; может быть пустым.
     */
        @AiDoc(summary = "Returns the unique identifier of this anchor within its layer.")
    @WikiMethod(summary = "<key:anchor.widget.method.id>")
    @WikiReturn("<key:anchor.widget.return.id>")
    public String id() {
        return id;
    }

    /**
     * Виджет, который реально рендерится и получает события.
     */
        @AiDoc(summary = "Returns the underlying UI widget hosted and positioned by this anchor.")
    @WikiMethod(summary = "<key:anchor.widget.method.widget>")
    @WikiReturn("<key:anchor.widget.return.widget>")
    public Widget widget() {
        return widget;
    }

    /**
     * X в world/map координатах.
     */
        @AiDoc(summary = "Returns the anchor world X coordinate.")
    @WikiMethod(summary = "<key:anchor.widget.method.worldX>")
    @WikiReturn("<key:anchor.widget.return.coordinate>")
    public float worldX() {
        return worldX;
    }

    /**
     * Y в world/map координатах.
     */
        @AiDoc(summary = "Returns the anchor world Y coordinate.")
    @WikiMethod(summary = "<key:anchor.widget.method.worldY>")
    @WikiReturn("<key:anchor.widget.return.coordinate>")
    public float worldY() {
        return worldY;
    }

    /**
     * Перемещает anchor в world/map координатах.
     */
        @AiDoc(summary = "Updates the anchor position in world coordinates.")
    @WikiMethod(summary = "<key:anchor.widget.method.worldPosition>")
    @WikiReturn("<key:anchor.widget.return.this>")
    public AnchorWidget worldPosition(
            @WikiParameter(description = "<key:anchor.widget.param.x>")
            float x,
            @WikiParameter(description = "<key:anchor.widget.param.y>")
            float y
    ) {
        float nextX = sanitize(x);
        float nextY = sanitize(y);
        if (worldX == nextX && worldY == nextY) return this;
        worldX = nextX;
        worldY = nextY;
        invalidateLayout();
        return this;
    }

    /**
     * Фиксированная экранная ширина виджета или {@code -1}, если используется desired size.
     */
        @AiDoc(summary = "Returns the fixed screen width in pixels, or -1 if using desired size.")
    @WikiMethod(summary = "<key:anchor.widget.method.screenWidth>")
    @WikiReturn("<key:anchor.widget.return.size>")
    public float screenWidth() {
        return screenWidth;
    }

    /**
     * Фиксированная экранная высота виджета или {@code -1}, если используется desired size.
     */
        @AiDoc(summary = "Returns the fixed screen height in pixels, or -1 if using desired size.")
    @WikiMethod(summary = "<key:anchor.widget.method.screenHeight>")
    @WikiReturn("<key:anchor.widget.return.size>")
    public float screenHeight() {
        return screenHeight;
    }

    /**
     * Задает постоянный экранный размер виджета.
     *
     * <p>Это главный способ сделать маркер карты читаемым при любом zoom.
     * Передай отрицательное/NaN значение, чтобы вернуться к desired size.</p>
     */
        @AiDoc(summary = "Sets fixed screen dimensions in UI pixels for this anchor.")
    @WikiMethod(summary = "<key:anchor.widget.method.screenSize>")
    @WikiReturn("<key:anchor.widget.return.this>")
    public AnchorWidget screenSize(
            @WikiParameter(description = "<key:anchor.widget.param.width>")
            float width,
            @WikiParameter(description = "<key:anchor.widget.param.height>")
            float height
    ) {
        float nextWidth = sanitizeSize(width, -1.0f);
        float nextHeight = sanitizeSize(height, -1.0f);
        if (screenWidth == nextWidth && screenHeight == nextHeight) return this;
        screenWidth = nextWidth;
        screenHeight = nextHeight;
        invalidateLayout();
        return this;
    }

    /**
     * Минимальная экранная ширина после zoom-scaling или {@code -1}, если пол не задан.
     */
        @AiDoc(summary = "Returns the minimum screen width constraint after zoom scaling.")
    @WikiMethod(summary = "<key:anchor.widget.method.minScreenWidth>")
    @WikiReturn("<key:anchor.widget.return.size>")
    public float minScreenWidth() {
        return minScreenWidth;
    }

    /**
     * Минимальная экранная высота после zoom-scaling или {@code -1}, если пол не задан.
     */
        @AiDoc(summary = "Returns the minimum screen height constraint after zoom scaling.")
    @WikiMethod(summary = "<key:anchor.widget.method.minScreenHeight>")
    @WikiReturn("<key:anchor.widget.return.size>")
    public float minScreenHeight() {
        return minScreenHeight;
    }

    /**
     * Максимальная экранная ширина после zoom-scaling или {@code -1}, если потолок не задан.
     */
        @AiDoc(summary = "Returns the maximum screen width constraint after zoom scaling.")
    @WikiMethod(summary = "<key:anchor.widget.method.maxScreenWidth>")
    @WikiReturn("<key:anchor.widget.return.size>")
    public float maxScreenWidth() {
        return maxScreenWidth;
    }

    /**
     * Максимальная экранная высота после zoom-scaling или {@code -1}, если потолок не задан.
     */
        @AiDoc(summary = "Returns the maximum screen height constraint after zoom scaling.")
    @WikiMethod(summary = "<key:anchor.widget.method.maxScreenHeight>")
    @WikiReturn("<key:anchor.widget.return.size>")
    public float maxScreenHeight() {
        return maxScreenHeight;
    }

    /**
     * Задает минимальный экранный размер маркера.
     *
     * <p>Пол применяется после {@link #zoomScaling(boolean) zoom-scaling}: при
     * сильном отдалении маркер не сожмётся меньше заданного размера и останется
     * читаемым/кликабельным. Отрицательное/NaN значение убирает пол.</p>
     *
     * <p>Если задан и пол, и {@link #maxScreenSize(float, float) потолок}, а пол
     * больше потолка — побеждает потолок.</p>
     */
        @AiDoc(summary = "Sets minimum screen dimensions to prevent over-shrinking at low zoom.")
    @WikiMethod(summary = "<key:anchor.widget.method.minScreenSize>")
    @WikiReturn("<key:anchor.widget.return.this>")
    public AnchorWidget minScreenSize(
            @WikiParameter(description = "<key:anchor.widget.param.width>")
            float width,
            @WikiParameter(description = "<key:anchor.widget.param.height>")
            float height
    ) {
        float nextWidth = sanitizeSize(width, -1.0f);
        float nextHeight = sanitizeSize(height, -1.0f);
        if (minScreenWidth == nextWidth && minScreenHeight == nextHeight) return this;
        minScreenWidth = nextWidth;
        minScreenHeight = nextHeight;
        invalidateLayout();
        return this;
    }

    /**
     * Задает максимальный экранный размер маркера.
     *
     * <p>Потолок применяется после {@link #zoomScaling(boolean) zoom-scaling}: при
     * сильном приближении маркер не раздуется больше заданного размера.
     * Отрицательное/NaN значение убирает потолок.</p>
     */
        @AiDoc(summary = "Sets maximum screen dimensions to prevent over-expanding at high zoom.")
    @WikiMethod(summary = "<key:anchor.widget.method.maxScreenSize>")
    @WikiReturn("<key:anchor.widget.return.this>")
    public AnchorWidget maxScreenSize(
            @WikiParameter(description = "<key:anchor.widget.param.width>")
            float width,
            @WikiParameter(description = "<key:anchor.widget.param.height>")
            float height
    ) {
        float nextWidth = sanitizeSize(width, -1.0f);
        float nextHeight = sanitizeSize(height, -1.0f);
        if (maxScreenWidth == nextWidth && maxScreenHeight == nextHeight) return this;
        maxScreenWidth = nextWidth;
        maxScreenHeight = nextHeight;
        invalidateLayout();
        return this;
    }

    /**
     * Включено ли масштабирование размера от zoom viewport'а.
     */
        @AiDoc(summary = "Returns true if widget dimensions scale proportionally with viewport zoom.")
    @WikiMethod(summary = "<key:anchor.widget.method.zoomScalingGet>")
    @WikiReturn("<key:anchor.widget.return.zoomScaling>")
    public boolean zoomScaling() {
        return zoomScaling;
    }

    /**
     * Zoom, при котором маркер имеет свой базовый размер.
     *
     * <p>Базовый размер — это {@link #screenSize(float, float)} или desired size
     * виджета, если screenSize не задан.</p>
     */
        @AiDoc(summary = "Returns the reference zoom factor where widget renders at base screen size.")
    @WikiMethod(summary = "<key:anchor.widget.method.referenceZoom>")
    @WikiReturn("<key:anchor.widget.return.zoom>")
    public float referenceZoom() {
        return referenceZoom;
    }

    /**
     * Включает масштабирование размера маркера от zoom viewport'а.
     *
     * <p>Эффективный размер считается как
     * {@code baseSize * (zoom / referenceZoom)}, затем зажимается в
     * [{@link #minScreenSize(float, float)}, {@link #maxScreenSize(float, float)}].
     * При отдалении маркеры уменьшаются, при приближении растут — но не выходят
     * за заданные пол/потолок.</p>
     *
     * <p>По умолчанию выключено: размер фиксирован, как раньше.</p>
     *
     * @param zoomScaling {@code true}, чтобы размер следовал за zoom
     * @param referenceZoom zoom, при котором размер равен базовому; неположительные/NaN нормализуются к {@code 1}
     */
        @AiDoc(summary = "Configures dynamic scaling of widget dimensions based on viewport zoom.")
    @WikiMethod(summary = "<key:anchor.widget.method.zoomScalingSet>")
    @WikiReturn("<key:anchor.widget.return.this>")
    public AnchorWidget zoomScaling(
            @WikiParameter(description = "<key:anchor.widget.param.zoomScaling>")
            boolean zoomScaling,
            @WikiParameter(description = "<key:anchor.widget.param.referenceZoom>")
            float referenceZoom
    ) {
        float nextReference = Float.isFinite(referenceZoom) && referenceZoom > 0.0f ? referenceZoom : 1.0f;
        if (this.zoomScaling == zoomScaling && this.referenceZoom == nextReference) return this;
        this.zoomScaling = zoomScaling;
        this.referenceZoom = nextReference;
        invalidateLayout();
        return this;
    }

    /**
     * Горизонтальный pivot: {@code 0} — левый край, {@code 0.5} — центр, {@code 1} — правый край.
     */
        @AiDoc(summary = "Returns the normalized horizontal pivot coordinate (0.0=left, 0.5=center, 1.0=right).")
    @WikiMethod(summary = "<key:anchor.widget.method.pivotX>")
    @WikiReturn("<key:anchor.widget.return.pivot>")
    public float pivotX() {
        return pivotX;
    }

    /**
     * Вертикальный pivot: {@code 0} — верх, {@code 0.5} — центр, {@code 1} — низ.
     */
        @AiDoc(summary = "Returns the normalized vertical pivot coordinate (0.0=top, 0.5=center, 1.0=bottom).")
    @WikiMethod(summary = "<key:anchor.widget.method.pivotY>")
    @WikiReturn("<key:anchor.widget.return.pivot>")
    public float pivotY() {
        return pivotY;
    }

    /**
     * Настраивает точку виджета, которая должна попасть в world/map позицию anchor.
     */
        @AiDoc(summary = "Configures the normalized pivot point aligned to the anchor world position.")
    @WikiMethod(summary = "<key:anchor.widget.method.pivot>")
    @WikiReturn("<key:anchor.widget.return.this>")
    public AnchorWidget pivot(
            @WikiParameter(description = "<key:anchor.widget.param.x>")
            float x,
            @WikiParameter(description = "<key:anchor.widget.param.y>")
            float y
    ) {
        float nextX = clamp01(x);
        float nextY = clamp01(y);
        if (pivotX == nextX && pivotY == nextY) return this;
        pivotX = nextX;
        pivotY = nextY;
        invalidateLayout();
        return this;
    }

    /**
     * Минимальный zoom, при котором anchor видим.
     */
        @AiDoc(summary = "Returns the lower zoom threshold for anchor visibility.")
    @WikiMethod(summary = "<key:anchor.widget.method.minVisibleZoom>")
    @WikiReturn("<key:anchor.widget.return.zoom>")
    public float minVisibleZoom() {
        return minVisibleZoom;
    }

    /**
     * Максимальный zoom, при котором anchor видим.
     */
        @AiDoc(summary = "Returns the upper zoom threshold for anchor visibility.")
    @WikiMethod(summary = "<key:anchor.widget.method.maxVisibleZoom>")
    @WikiReturn("<key:anchor.widget.return.zoom>")
    public float maxVisibleZoom() {
        return maxVisibleZoom;
    }

    /**
     * Делает anchor видимым только в заданном диапазоне zoom.
     *
     * <p>Удобно для LOD: например, на дальнем zoom показывать только крупные
     * области, а при приближении включать магазины, NPC, мелкие POI.</p>
     */
        @AiDoc(summary = "Restricts anchor visibility to a specified viewport zoom range.")
    @WikiMethod(summary = "<key:anchor.widget.method.visibleZoomRange>")
    @WikiReturn("<key:anchor.widget.return.this>")
    public AnchorWidget visibleZoomRange(
            @WikiParameter(description = "<key:anchor.widget.param.minZoom>")
            float minZoom,
            @WikiParameter(description = "<key:anchor.widget.param.maxZoom>")
            float maxZoom
    ) {
        float nextMin = Float.isFinite(minZoom) && minZoom >= 0.0f ? minZoom : 0.0f;
        float nextMax = Float.isFinite(maxZoom) && maxZoom >= nextMin ? maxZoom : Float.POSITIVE_INFINITY;
        if (minVisibleZoom == nextMin && maxVisibleZoom == nextMax) return this;
        minVisibleZoom = nextMin;
        maxVisibleZoom = nextMax;
        invalidateLayout();
        return this;
    }

    /**
     * Явная видимость anchor без учета zoom/culling.
     */
        @AiDoc(summary = "Returns the manual visibility state of this anchor.")
    @WikiMethod(summary = "<key:anchor.widget.method.visibleGet>")
    @WikiReturn("<key:anchor.widget.return.visible>")
    public boolean visible() {
        return visible;
    }

    /**
     * Включает или выключает anchor вручную.
     */
        @AiDoc(summary = "Manually sets the visibility state of this anchor.")
    @WikiMethod(summary = "<key:anchor.widget.method.visibleSet>")
    @WikiReturn("<key:anchor.widget.return.this>")
    public AnchorWidget visible(
            @WikiParameter(description = "<key:anchor.widget.param.visible>")
            boolean visible
    ) {
        if (this.visible == visible) return this;
        this.visible = visible;
        invalidateLayout();
        return this;
    }

    /**
     * Нужно ли автоматически скрывать anchor, если он вне видимой области canvas.
     */
        @AiDoc(summary = "Returns true if this anchor is automatically culled when outside the canvas viewport.")
    @WikiMethod(summary = "<key:anchor.widget.method.cullOutsideViewportGet>")
    @WikiReturn("<key:anchor.widget.return.cull>")
    public boolean cullOutsideViewport() {
        return cullOutsideViewport;
    }

    /**
     * Включает culling вне viewport.
     *
     * <p>Для обычных маркеров лучше оставить {@code true}. Для больших popup-like
     * элементов, которые могут частично выходить за карту, можно поставить {@code false}.</p>
     */
        @AiDoc(summary = "Enables or disables automatic culling when outside the visible viewport.")
    @WikiMethod(summary = "<key:anchor.widget.method.cullOutsideViewportSet>")
    @WikiReturn("<key:anchor.widget.return.this>")
    public AnchorWidget cullOutsideViewport(
            @WikiParameter(description = "<key:anchor.widget.param.cullOutsideViewport>")
            boolean cullOutsideViewport
    ) {
        if (this.cullOutsideViewport == cullOutsideViewport) return this;
        this.cullOutsideViewport = cullOutsideViewport;
        invalidateLayout();
        return this;
    }

    /**
     * Итоговая видимость после zoom/culling/layout проверки.
     */
        @AiDoc(summary = "Returns true if the anchor is currently visible after zoom, culling, and layout passes.")
    @WikiMethod(summary = "<key:anchor.widget.method.arrangedVisible>")
    @WikiReturn("<key:anchor.widget.return.visible>")
    public boolean arrangedVisible() {
        return arrangedVisible;
    }

    /**
     * Последняя рассчитанная root X позиция world-точки anchor.
     */
        @AiDoc(summary = "Returns the latest projected screen root X coordinate of the anchor point.")
    @WikiMethod(summary = "<key:anchor.widget.method.projectedRootX>")
    @WikiReturn("<key:anchor.widget.return.coordinate>")
    public float projectedRootX() {
        return projectedRootX;
    }

    /**
     * Последняя рассчитанная root Y позиция world-точки anchor.
     */
        @AiDoc(summary = "Returns the latest projected screen root Y coordinate of the anchor point.")
    @WikiMethod(summary = "<key:anchor.widget.method.projectedRootY>")
    @WikiReturn("<key:anchor.widget.return.coordinate>")
    public float projectedRootY() {
        return projectedRootY;
    }

    void owner(AnchorLayer owner) {
        this.owner = owner;
    }

    void arrangedVisible(boolean arrangedVisible) {
        this.arrangedVisible = arrangedVisible;
    }

    void projectedRoot(float x, float y) {
        projectedRootX = x;
        projectedRootY = y;
    }

    boolean visibleAtZoom(float zoom) {
        return visible && zoom >= minVisibleZoom && zoom <= maxVisibleZoom;
    }

    /**
     * Вычисляет эффективную экранную ширину с учётом zoom-scaling и min/max.
     *
     * @param baseWidth базовый размер: screenSize или desired size виджета
     * @param zoom текущий zoom viewport'а
     */
    float effectiveWidth(float baseWidth, float zoom) {
        return clampSize(baseWidth * zoomFactor(zoom), minScreenWidth, maxScreenWidth);
    }

    /**
     * Вычисляет эффективную экранную высоту с учётом zoom-scaling и min/max.
     *
     * @param baseHeight базовый размер: screenSize или desired size виджета
     * @param zoom текущий zoom viewport'а
     */
    float effectiveHeight(float baseHeight, float zoom) {
        return clampSize(baseHeight * zoomFactor(zoom), minScreenHeight, maxScreenHeight);
    }

    private float zoomFactor(float zoom) {
        if (!zoomScaling) return 1.0f;
        float safeZoom = Float.isFinite(zoom) && zoom > 0.0f ? zoom : referenceZoom;
        return safeZoom / referenceZoom;
    }

    private static float clampSize(float value, float min, float max) {
        float clamped = value;
        if (min >= 0.0f) clamped = Math.max(clamped, min);
        if (max >= 0.0f) clamped = Math.min(clamped, max);
        return clamped;
    }

    private void invalidateLayout() {
        if (owner != null) {
            owner.invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        }
    }

    private static float sanitize(float value) {
        return Float.isFinite(value) ? value : 0.0f;
    }

    private static float sanitizeSize(float value, float fallback) {
        return Float.isFinite(value) && value >= 0.0f ? value : fallback;
    }

    private static float clamp01(float value) {
        if (!Float.isFinite(value)) return 0.5f;
        return Math.max(0.0f, Math.min(1.0f, value));
    }
}
