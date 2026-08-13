package dev.sixik.unigui.widgets.map;

import dev.sixik.unigui.api.viewport.ViewportPoint;

/**
 * Axis-aligned affine projection from an external world coordinate space into map coordinates.
 */
public final class MapProjection {
    private static final MapProjection IDENTITY = new MapProjection(1.0f, 0.0f, 1.0f, 0.0f);

    private final float scaleX;
    private final float offsetX;
    private final float scaleY;
    private final float offsetY;

    public MapProjection(float scaleX, float offsetX, float scaleY, float offsetY) {
        this.scaleX = sanitizeScale(scaleX);
        this.offsetX = sanitize(offsetX);
        this.scaleY = sanitizeScale(scaleY);
        this.offsetY = sanitize(offsetY);
    }

    public static MapProjection identity() {
        return IDENTITY;
    }

    public static MapProjection axisAligned(float worldX1, float worldY1,
                                            float mapX1, float mapY1,
                                            float worldX2, float worldY2,
                                            float mapX2, float mapY2) {
        float safeWorldX1 = sanitize(worldX1);
        float safeWorldY1 = sanitize(worldY1);
        float safeMapX1 = sanitize(mapX1);
        float safeMapY1 = sanitize(mapY1);
        float dx = sanitize(worldX2) - safeWorldX1;
        float dy = sanitize(worldY2) - safeWorldY1;
        float sx = Math.abs(dx) <= 0.000001f ? 1.0f : (sanitize(mapX2) - safeMapX1) / dx;
        float sy = Math.abs(dy) <= 0.000001f ? 1.0f : (sanitize(mapY2) - safeMapY1) / dy;
        return new MapProjection(sx, safeMapX1 - safeWorldX1 * sx, sy, safeMapY1 - safeWorldY1 * sy);
    }

    public static Builder affine() {
        return new Builder();
    }

    public float mapX(float worldX) {
        return sanitize(worldX) * scaleX + offsetX;
    }

    public float mapY(float worldY) {
        return sanitize(worldY) * scaleY + offsetY;
    }

    public ViewportPoint project(float worldX, float worldY) {
        return new ViewportPoint(mapX(worldX), mapY(worldY));
    }

    public float worldX(float mapX) {
        return (sanitize(mapX) - offsetX) / scaleX;
    }

    public float worldY(float mapY) {
        return (sanitize(mapY) - offsetY) / scaleY;
    }

    public ViewportPoint unproject(float mapX, float mapY) {
        return new ViewportPoint(worldX(mapX), worldY(mapY));
    }

    public float scaleX() {
        return scaleX;
    }

    public float scaleY() {
        return scaleY;
    }

    public float offsetX() {
        return offsetX;
    }

    public float offsetY() {
        return offsetY;
    }

    private static float sanitize(float value) {
        return Float.isFinite(value) ? value : 0.0f;
    }

    private static float sanitizeScale(float value) {
        return Float.isFinite(value) && Math.abs(value) > 0.000001f ? value : 1.0f;
    }

    public static final class Builder {
        private boolean pendingWorld;
        private float pendingWorldX;
        private float pendingWorldY;
        private int pairCount;
        private float worldX1;
        private float worldY1;
        private float mapX1;
        private float mapY1;
        private float worldX2;
        private float worldY2;
        private float mapX2;
        private float mapY2;

        public Builder worldPoint(float x, float y) {
            pendingWorldX = sanitize(x);
            pendingWorldY = sanitize(y);
            pendingWorld = true;
            return this;
        }

        public Builder mapPoint(float x, float y) {
            if (!pendingWorld) {
                throw new IllegalStateException("Call worldPoint(x, y) before mapPoint(x, y)");
            }
            if (pairCount == 0) {
                worldX1 = pendingWorldX;
                worldY1 = pendingWorldY;
                mapX1 = sanitize(x);
                mapY1 = sanitize(y);
            } else if (pairCount == 1) {
                worldX2 = pendingWorldX;
                worldY2 = pendingWorldY;
                mapX2 = sanitize(x);
                mapY2 = sanitize(y);
            } else {
                throw new IllegalStateException("Axis-aligned projection uses exactly two calibration pairs");
            }
            pairCount++;
            pendingWorld = false;
            return this;
        }

        public MapProjection build() {
            if (pairCount == 0) return identity();
            if (pairCount == 1) {
                return new MapProjection(1.0f, mapX1 - worldX1, 1.0f, mapY1 - worldY1);
            }
            return axisAligned(worldX1, worldY1, mapX1, mapY1, worldX2, worldY2, mapX2, mapY2);
        }
    }
}
