package dev.sixik.unigui.widgets.map;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.render.TextureHandle;
import dev.sixik.unigui.api.render.TexturePlacement;
import dev.sixik.unigui.api.viewport.ViewportPoint;
import dev.sixik.unigui.api.viewport.Viewport2D;
import dev.sixik.unigui.widgets.AnchorWidget;
import dev.sixik.unigui.widgets.WorldCanvas;

/**
 * Thin map-oriented wrapper over {@link WorldCanvas}.
 */
public class MapCanvas extends WorldCanvas {
    private static final float DEFAULT_MAP_WIDTH = 1024.0f;
    private static final float DEFAULT_MAP_HEIGHT = 512.0f;

    private TextureHandle texture;
    private final MutableRect source = new MutableRect(0.0f, 0.0f, 1.0f, 1.0f);
    private final MutableColor tint = new MutableColor(1.0f, 1.0f, 1.0f, 1.0f);
    private final MutableColor backgroundColor = new MutableColor(0.030f, 0.038f, 0.058f, 0.96f);
    private final MutableColor mapColor = new MutableColor(0.055f, 0.072f, 0.108f, 0.92f);
    private final MutableColor borderColor = new MutableColor(0.30f, 0.42f, 0.68f, 0.82f);
    private final MutableColor gridColor = new MutableColor(0.22f, 0.32f, 0.48f, 0.28f);
    private MapProjection projection = MapProjection.identity();
    private float mapWidth = DEFAULT_MAP_WIDTH;
    private float mapHeight = DEFAULT_MAP_HEIGHT;
    private float gridSize = 128.0f;
    private boolean backgroundVisible = true;
    private boolean mapBorderVisible = true;
    private boolean gridVisible = true;

    public MapCanvas() {
        tint.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
        backgroundColor.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
        mapColor.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
        borderColor.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
        gridColor.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
        source.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
        addWorldLayer(this::renderMapLayer);
        worldBounds(0.0f, 0.0f, mapWidth, mapHeight);
    }

    public MapCanvas(TextureHandle texture) {
        this();
        texture(texture);
        if (texture != null) {
            mapSize(texture.width(), texture.height());
        }
    }

    public MapCanvas(TextureHandle texture, float mapWidth, float mapHeight) {
        this();
        texture(texture);
        mapSize(mapWidth, mapHeight);
    }

    @Override
    public MapCanvas viewport(Viewport2D viewport) {
        super.viewport(viewport);
        return this;
    }

    @Override
    public MapCanvas viewport(float x, float y) {
        super.viewport(x, y);
        return this;
    }

    @Override
    public MapCanvas viewport(float x, float y, float zoom) {
        super.viewport(x, y, zoom);
        return this;
    }

    @Override
    public MapCanvas zoomRange(float minZoom, float maxZoom) {
        super.zoomRange(minZoom, maxZoom);
        return this;
    }

    @Override
    public MapCanvas wheelPanStep(float wheelPanStep) {
        super.wheelPanStep(wheelPanStep);
        return this;
    }

    @Override
    public MapCanvas zoomStep(float zoomStep) {
        super.zoomStep(zoomStep);
        return this;
    }

    public TextureHandle texture() {
        return texture;
    }

    public MapCanvas texture(TextureHandle texture) {
        if (this.texture == texture) return this;
        this.texture = texture;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public MutableRect source() {
        return source;
    }

    public MapCanvas source(float u, float v, float width, float height) {
        source.set(u, v, width, height);
        return this;
    }

    public MutableColor tint() {
        return tint;
    }

    public MutableColor backgroundColor() {
        return backgroundColor;
    }

    public MutableColor mapColor() {
        return mapColor;
    }

    public MutableColor borderColor() {
        return borderColor;
    }

    public MutableColor gridColor() {
        return gridColor;
    }

    public float mapWidth() {
        return mapWidth;
    }

    public float mapHeight() {
        return mapHeight;
    }

    public MapCanvas mapSize(float width, float height) {
        float nextWidth = sanitizePositive(width, DEFAULT_MAP_WIDTH);
        float nextHeight = sanitizePositive(height, DEFAULT_MAP_HEIGHT);
        if (mapWidth == nextWidth && mapHeight == nextHeight) return this;
        mapWidth = nextWidth;
        mapHeight = nextHeight;
        worldBounds(0.0f, 0.0f, mapWidth, mapHeight);
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public MapProjection projection() {
        return projection;
    }

    public MapCanvas projection(MapProjection projection) {
        this.projection = projection == null ? MapProjection.identity() : projection;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public boolean backgroundVisible() {
        return backgroundVisible;
    }

    public MapCanvas backgroundVisible(boolean backgroundVisible) {
        if (this.backgroundVisible == backgroundVisible) return this;
        this.backgroundVisible = backgroundVisible;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public boolean mapBorderVisible() {
        return mapBorderVisible;
    }

    public MapCanvas mapBorderVisible(boolean mapBorderVisible) {
        if (this.mapBorderVisible == mapBorderVisible) return this;
        this.mapBorderVisible = mapBorderVisible;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public boolean gridVisible() {
        return gridVisible;
    }

    public MapCanvas gridVisible(boolean gridVisible) {
        if (this.gridVisible == gridVisible) return this;
        this.gridVisible = gridVisible;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public float gridSize() {
        return gridSize;
    }

    public MapCanvas gridSize(float gridSize) {
        float next = sanitizePositive(gridSize, 128.0f);
        if (this.gridSize == next) return this;
        this.gridSize = next;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public MapCanvas clampToMapBounds(boolean clamp) {
        worldBounds(0.0f, 0.0f, mapWidth, mapHeight);
        clampToWorldBounds(clamp);
        return this;
    }

    public MapMarker addMarker(String id, float mapX, float mapY, String label, MarkerStyle style) {
        return addMarker(id, mapX, mapY, new MapMarker(label).markerStyle(style));
    }

    public MapMarker addMarker(String id, float mapX, float mapY, MapMarker marker) {
        MapMarker effective = marker == null ? new MapMarker(id) : marker;
        AnchorWidget anchor = anchorLayer().add(id, mapX, mapY, effective)
                .screenSize(effective.markerStyle().width(), effective.markerStyle().height())
                .pivot(0.5f, 0.5f);
        effective.anchor(anchor);
        return effective;
    }

    public MapMarker addProjectedMarker(String id, float worldX, float worldY, String label, MarkerStyle style) {
        ViewportPoint point = projection.project(worldX, worldY);
        return addMarker(id, point.x(), point.y(), label, style);
    }

    public boolean removeMarker(String id) {
        return anchorLayer().remove(id);
    }

    public float externalToMapX(float worldX) {
        return projection.mapX(worldX);
    }

    public float externalToMapY(float worldY) {
        return projection.mapY(worldY);
    }

    private void renderMapLayer(WorldCanvas ignored, DrawScope draw) {
        float canvasX = layoutBounds().x();
        float canvasY = layoutBounds().y();
        float canvasWidth = layoutBounds().width();
        float canvasHeight = layoutBounds().height();
        if (backgroundVisible) {
            draw.addRectFilled(canvasX, canvasY, canvasWidth, canvasHeight, backgroundColor);
        }

        float mapX = worldToRootX(0.0f);
        float mapY = worldToRootY(0.0f);
        float mapW = viewport().zoom() * mapWidth;
        float mapH = viewport().zoom() * mapHeight;

        if (texture != null) {
            draw.texture(texture,
                    new TexturePlacement(mapX, mapY, mapW, mapH, source.x(), source.y(), source.width(), source.height()),
                    0.0f,
                    Paint.fill(tint));
        } else {
            draw.addRectFilled(mapX, mapY, mapW, mapH, mapColor);
        }

        if (gridVisible) {
            renderGrid(draw, mapX, mapY, mapW, mapH);
        }
        if (mapBorderVisible) {
            draw.addRect(mapX, mapY, mapW, mapH, borderColor, 1.0f);
        }
    }

    private void renderGrid(DrawScope draw, float mapX, float mapY, float mapW, float mapH) {
        float safeGrid = Math.max(1.0f, gridSize);
        for (float x = 0.0f; x <= mapWidth + 0.0001f; x += safeGrid) {
            float gx = worldToRootX(x);
            draw.addLine(gx, mapY, gx, mapY + mapH, gridColor, 1.0f);
        }
        for (float y = 0.0f; y <= mapHeight + 0.0001f; y += safeGrid) {
            float gy = worldToRootY(y);
            draw.addLine(mapX, gy, mapX + mapW, gy, gridColor, 1.0f);
        }
    }

    private static float sanitizePositive(float value, float fallback) {
        return Float.isFinite(value) && value > 0.0f ? value : fallback;
    }
}
