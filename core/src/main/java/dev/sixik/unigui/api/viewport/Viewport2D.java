package dev.sixik.unigui.api.viewport;

/**
 * Backend-neutral 2D viewport model.
 *
 * <p>The current coordinate convention matches the existing NodeGraph behavior:
 * {@code x/y} are screen-space offsets applied before world coordinates are
 * multiplied by {@code zoom}.</p>
 *
 * <pre>
 * screenX = offsetX + worldX * zoom
 * worldX  = (screenX - offsetX) / zoom
 * </pre>
 */
public final class Viewport2D {
    private static final float DEFAULT_MIN_ZOOM = 0.25f;
    private static final float DEFAULT_MAX_ZOOM = 4.0f;

    private float x;
    private float y;
    private float zoom = 1.0f;
    private float minZoom = DEFAULT_MIN_ZOOM;
    private float maxZoom = DEFAULT_MAX_ZOOM;
    private ViewportBounds worldBounds;
    private boolean clampToWorldBounds;

    public float x() {
        return x;
    }

    public float y() {
        return y;
    }

    public float zoom() {
        return zoom;
    }

    public float minZoom() {
        return minZoom;
    }

    public float maxZoom() {
        return maxZoom;
    }

    public ViewportBounds worldBounds() {
        return worldBounds;
    }

    public boolean clampToWorldBounds() {
        return clampToWorldBounds;
    }

    public Viewport2D position(float x, float y) {
        setPosition(x, y);
        return this;
    }

    public boolean setPosition(float x, float y) {
        return set(sanitize(x), sanitize(y), zoom);
    }

    public Viewport2D zoom(float zoom) {
        setZoom(zoom);
        return this;
    }

    public boolean setZoom(float zoom) {
        return set(x, y, zoom);
    }

    public Viewport2D zoomRange(float minZoom, float maxZoom) {
        float nextMin = Float.isFinite(minZoom) && minZoom > 0.0f ? minZoom : DEFAULT_MIN_ZOOM;
        float nextMax = Float.isFinite(maxZoom) && maxZoom >= nextMin ? maxZoom : Math.max(nextMin, DEFAULT_MAX_ZOOM);
        this.minZoom = nextMin;
        this.maxZoom = nextMax;
        set(x, y, zoom);
        return this;
    }

    public Viewport2D worldBounds(float x, float y, float width, float height) {
        this.worldBounds = new ViewportBounds(x, y, width, height);
        return this;
    }

    public Viewport2D clearWorldBounds() {
        worldBounds = null;
        return this;
    }

    public Viewport2D clampToWorldBounds(boolean clampToWorldBounds) {
        this.clampToWorldBounds = clampToWorldBounds;
        return this;
    }

    public boolean set(float x, float y, float zoom) {
        float nextX = sanitize(x);
        float nextY = sanitize(y);
        float nextZoom = sanitizeZoom(zoom);
        if (this.x == nextX && this.y == nextY && this.zoom == nextZoom) return false;
        this.x = nextX;
        this.y = nextY;
        this.zoom = nextZoom;
        return true;
    }

    public ViewportChange setWithChange(float x, float y, float zoom) {
        float oldX = this.x;
        float oldY = this.y;
        float oldZoom = this.zoom;
        set(x, y, zoom);
        return new ViewportChange(oldX, oldY, oldZoom, this.x, this.y, this.zoom);
    }

    public boolean panBy(float deltaScreenX, float deltaScreenY) {
        return set(x + sanitize(deltaScreenX), y + sanitize(deltaScreenY), zoom);
    }

    public boolean panBy(float deltaScreenX, float deltaScreenY, float viewportWidth, float viewportHeight) {
        boolean moved = set(x + sanitize(deltaScreenX), y + sanitize(deltaScreenY), zoom);
        boolean clamped = clamp(viewportWidth, viewportHeight);
        return moved || clamped;
    }

    public boolean zoomAt(float screenX, float screenY, float factor) {
        if (!Float.isFinite(factor) || factor <= 0.0f) return false;
        return zoomTo(screenX, screenY, zoom * factor);
    }

    public boolean zoomTo(float screenX, float screenY, float nextZoom) {
        float normalizedZoom = sanitizeZoom(nextZoom);
        if (normalizedZoom == zoom) return false;

        float pivotX = sanitize(screenX);
        float pivotY = sanitize(screenY);
        float worldX = screenToWorldX(pivotX);
        float worldY = screenToWorldY(pivotY);

        zoom = normalizedZoom;
        x = sanitize(pivotX - worldX * zoom);
        y = sanitize(pivotY - worldY * zoom);
        return true;
    }

    public boolean zoomTo(float screenX, float screenY, float nextZoom, float viewportWidth, float viewportHeight) {
        if (!zoomTo(screenX, screenY, nextZoom)) return false;
        clamp(viewportWidth, viewportHeight);
        return true;
    }

    public boolean clamp(float viewportWidth, float viewportHeight) {
        if (!clampToWorldBounds || worldBounds == null || worldBounds.empty()) return false;

        float oldX = x;
        float oldY = y;
        x = clampOffset(x, viewportWidth, worldBounds.x(), worldBounds.right(), zoom);
        y = clampOffset(y, viewportHeight, worldBounds.y(), worldBounds.bottom(), zoom);
        return oldX != x || oldY != y;
    }

    public float worldToScreenX(float worldX) {
        return x + sanitize(worldX) * zoom;
    }

    public float worldToScreenY(float worldY) {
        return y + sanitize(worldY) * zoom;
    }

    public ViewportPoint worldToScreen(float worldX, float worldY) {
        return new ViewportPoint(worldToScreenX(worldX), worldToScreenY(worldY));
    }

    public float screenToWorldX(float screenX) {
        return (sanitize(screenX) - x) / zoom;
    }

    public float screenToWorldY(float screenY) {
        return (sanitize(screenY) - y) / zoom;
    }

    public ViewportPoint screenToWorld(float screenX, float screenY) {
        return new ViewportPoint(screenToWorldX(screenX), screenToWorldY(screenY));
    }

    private float sanitizeZoom(float value) {
        float safe = Float.isFinite(value) && value > 0.0f ? value : 1.0f;
        return clamp(safe, minZoom, maxZoom);
    }

    private static float clampOffset(float offset, float viewportSize, float worldMin, float worldMax, float zoom) {
        float size = Float.isFinite(viewportSize) ? Math.max(0.0f, viewportSize) : 0.0f;
        float contentMin = worldMin * zoom;
        float contentMax = worldMax * zoom;
        float contentSize = contentMax - contentMin;
        if (contentSize <= size) {
            return sanitize((size - contentSize) * 0.5f - contentMin);
        }

        float minOffset = size - contentMax;
        float maxOffset = -contentMin;
        return clamp(offset, minOffset, maxOffset);
    }

    private static float sanitize(float value) {
        return Float.isFinite(value) ? value : 0.0f;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}