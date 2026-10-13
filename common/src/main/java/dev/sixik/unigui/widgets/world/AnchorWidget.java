package dev.sixik.unigui.widgets.world;

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
    public String id() {
        return id;
    }

    /**
     * Виджет, который реально рендерится и получает события.
     */
    public Widget widget() {
        return widget;
    }

    /**
     * X в world/map координатах.
     */
    public float worldX() {
        return worldX;
    }

    /**
     * Y в world/map координатах.
     */
    public float worldY() {
        return worldY;
    }

    /**
     * Перемещает anchor в world/map координатах.
     */
    public AnchorWidget worldPosition(float x, float y) {
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
    public float screenWidth() {
        return screenWidth;
    }

    /**
     * Фиксированная экранная высота виджета или {@code -1}, если используется desired size.
     */
    public float screenHeight() {
        return screenHeight;
    }

    /**
     * Задает постоянный экранный размер виджета.
     *
     * <p>Это главный способ сделать маркер карты читаемым при любом zoom.
     * Передай отрицательное/NaN значение, чтобы вернуться к desired size.</p>
     */
    public AnchorWidget screenSize(float width, float height) {
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
    public float minScreenWidth() {
        return minScreenWidth;
    }

    /**
     * Минимальная экранная высота после zoom-scaling или {@code -1}, если пол не задан.
     */
    public float minScreenHeight() {
        return minScreenHeight;
    }

    /**
     * Максимальная экранная ширина после zoom-scaling или {@code -1}, если потолок не задан.
     */
    public float maxScreenWidth() {
        return maxScreenWidth;
    }

    /**
     * Максимальная экранная высота после zoom-scaling или {@code -1}, если потолок не задан.
     */
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
    public AnchorWidget minScreenSize(float width, float height) {
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
    public AnchorWidget maxScreenSize(float width, float height) {
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
    public boolean zoomScaling() {
        return zoomScaling;
    }

    /**
     * Zoom, при котором маркер имеет свой базовый размер.
     *
     * <p>Базовый размер — это {@link #screenSize(float, float)} или desired size
     * виджета, если screenSize не задан.</p>
     */
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
    public AnchorWidget zoomScaling(boolean zoomScaling, float referenceZoom) {
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
    public float pivotX() {
        return pivotX;
    }

    /**
     * Вертикальный pivot: {@code 0} — верх, {@code 0.5} — центр, {@code 1} — низ.
     */
    public float pivotY() {
        return pivotY;
    }

    /**
     * Настраивает точку виджета, которая должна попасть в world/map позицию anchor.
     */
    public AnchorWidget pivot(float x, float y) {
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
    public float minVisibleZoom() {
        return minVisibleZoom;
    }

    /**
     * Максимальный zoom, при котором anchor видим.
     */
    public float maxVisibleZoom() {
        return maxVisibleZoom;
    }

    /**
     * Делает anchor видимым только в заданном диапазоне zoom.
     *
     * <p>Удобно для LOD: например, на дальнем zoom показывать только крупные
     * области, а при приближении включать магазины, NPC, мелкие POI.</p>
     */
    public AnchorWidget visibleZoomRange(float minZoom, float maxZoom) {
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
    public boolean visible() {
        return visible;
    }

    /**
     * Включает или выключает anchor вручную.
     */
    public AnchorWidget visible(boolean visible) {
        if (this.visible == visible) return this;
        this.visible = visible;
        invalidateLayout();
        return this;
    }

    /**
     * Нужно ли автоматически скрывать anchor, если он вне видимой области canvas.
     */
    public boolean cullOutsideViewport() {
        return cullOutsideViewport;
    }

    /**
     * Включает culling вне viewport.
     *
     * <p>Для обычных маркеров лучше оставить {@code true}. Для больших popup-like
     * элементов, которые могут частично выходить за карту, можно поставить {@code false}.</p>
     */
    public AnchorWidget cullOutsideViewport(boolean cullOutsideViewport) {
        if (this.cullOutsideViewport == cullOutsideViewport) return this;
        this.cullOutsideViewport = cullOutsideViewport;
        invalidateLayout();
        return this;
    }

    /**
     * Итоговая видимость после zoom/culling/layout проверки.
     */
    public boolean arrangedVisible() {
        return arrangedVisible;
    }

    /**
     * Последняя рассчитанная root X позиция world-точки anchor.
     */
    public float projectedRootX() {
        return projectedRootX;
    }

    /**
     * Последняя рассчитанная root Y позиция world-точки anchor.
     */
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
