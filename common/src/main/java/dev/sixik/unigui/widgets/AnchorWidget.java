package dev.sixik.unigui.widgets;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.widget.Widget;

/**
 * Fixed-size widget projected from a world-space coordinate by {@link WorldCanvas}.
 */
public final class AnchorWidget {
    private final String id;
    private final Widget widget;
    private AnchorLayer owner;
    private float worldX;
    private float worldY;
    private float screenWidth = -1.0f;
    private float screenHeight = -1.0f;
    private float pivotX = 0.5f;
    private float pivotY = 0.5f;
    private float minVisibleZoom = 0.0f;
    private float maxVisibleZoom = Float.POSITIVE_INFINITY;
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

    public String id() {
        return id;
    }

    public Widget widget() {
        return widget;
    }

    public float worldX() {
        return worldX;
    }

    public float worldY() {
        return worldY;
    }

    public AnchorWidget worldPosition(float x, float y) {
        float nextX = sanitize(x);
        float nextY = sanitize(y);
        if (worldX == nextX && worldY == nextY) return this;
        worldX = nextX;
        worldY = nextY;
        invalidateLayout();
        return this;
    }

    public float screenWidth() {
        return screenWidth;
    }

    public float screenHeight() {
        return screenHeight;
    }

    public AnchorWidget screenSize(float width, float height) {
        float nextWidth = sanitizeSize(width, -1.0f);
        float nextHeight = sanitizeSize(height, -1.0f);
        if (screenWidth == nextWidth && screenHeight == nextHeight) return this;
        screenWidth = nextWidth;
        screenHeight = nextHeight;
        invalidateLayout();
        return this;
    }

    public float pivotX() {
        return pivotX;
    }

    public float pivotY() {
        return pivotY;
    }

    public AnchorWidget pivot(float x, float y) {
        float nextX = clamp01(x);
        float nextY = clamp01(y);
        if (pivotX == nextX && pivotY == nextY) return this;
        pivotX = nextX;
        pivotY = nextY;
        invalidateLayout();
        return this;
    }

    public float minVisibleZoom() {
        return minVisibleZoom;
    }

    public float maxVisibleZoom() {
        return maxVisibleZoom;
    }

    public AnchorWidget visibleZoomRange(float minZoom, float maxZoom) {
        float nextMin = Float.isFinite(minZoom) && minZoom >= 0.0f ? minZoom : 0.0f;
        float nextMax = Float.isFinite(maxZoom) && maxZoom >= nextMin ? maxZoom : Float.POSITIVE_INFINITY;
        if (minVisibleZoom == nextMin && maxVisibleZoom == nextMax) return this;
        minVisibleZoom = nextMin;
        maxVisibleZoom = nextMax;
        invalidateLayout();
        return this;
    }

    public boolean cullOutsideViewport() {
        return cullOutsideViewport;
    }

    public AnchorWidget cullOutsideViewport(boolean cullOutsideViewport) {
        if (this.cullOutsideViewport == cullOutsideViewport) return this;
        this.cullOutsideViewport = cullOutsideViewport;
        invalidateLayout();
        return this;
    }

    public boolean arrangedVisible() {
        return arrangedVisible;
    }

    public float projectedRootX() {
        return projectedRootX;
    }

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
        return zoom >= minVisibleZoom && zoom <= maxVisibleZoom;
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
