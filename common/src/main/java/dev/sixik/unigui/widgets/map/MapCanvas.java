package dev.sixik.unigui.widgets.map;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.render.TextureHandle;
import dev.sixik.unigui.api.render.TexturePlacement;
import dev.sixik.unigui.api.viewport.Viewport2D;
import dev.sixik.unigui.api.viewport.ViewportPoint;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.api.xml.XmlAttribute;
import dev.sixik.unigui.api.xml.XmlWidgetName;
import dev.sixik.unigui.widgets.world.AnchorWidget;
import dev.sixik.unigui.widgets.world.WorldCanvas;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import dev.sixik.unigui.widgets.interaction.Button;

/**
 * Карточный/картографический виджет поверх {@link WorldCanvas}.
 *
 * <p>{@code MapCanvas} добавляет поверх generic pan/zoom viewport несколько
 * готовых вещей, которые чаще всего нужны именно карте:</p>
 *
 * <ul>
 *     <li>размер карты в map-координатах через {@link #mapSize(float, float)};</li>
 *     <li>отрисовку texture или fallback-прямоугольника карты;</li>
 *     <li>grid/border для debug или editor-like режимов;</li>
 *     <li>проекцию внешних world координат в координаты карты через {@link MapProjection};</li>
 *     <li>удобный API маркеров поверх {@link AnchorWidget}.</li>
 * </ul>
 *
 * <p>Системы координат:</p>
 *
 * <ul>
 *     <li><b>map coordinates</b> — координаты внутри самой карты: обычно {@code 0,0}
 *     в левом верхнем углу и {@code mapWidth,mapHeight} в правом нижнем;</li>
 *     <li><b>external/world coordinates</b> — координаты игры/данных, например Minecraft X/Z;</li>
 *     <li><b>root coordinates</b> — экранные координаты UI после pan/zoom.</li>
 * </ul>
 *
 * <pre>{@code
 * MapCanvas map = new MapCanvas()
 *         .mapSize(4096.0f, 2048.0f)
 *         .projection(MapProjection.affine()
 *                 .worldPoint(-2048.0f, -1024.0f).mapPoint(64.0f, 64.0f)
 *                 .worldPoint( 2048.0f,  1024.0f).mapPoint(4032.0f, 1984.0f)
 *                 .build())
 *         .viewport(120.0f, 80.0f, 0.5f)
 *         .zoomRange(0.2f, 2.0f);
 *
 * map.addProjectedMarkerWidget("shop", worldX, worldZ, new Button("Shop"))
 *         .screenSize(88.0f, 24.0f)
 *         .pivot(0.5f, 0.5f);
 * }</pre>
 */
@XmlWidgetName("MapCanvas")
public class MapCanvas extends WorldCanvas {
    public static final String STYLE_TYPE = dev.sixik.unigui.api.style.StyleIds.Widget.MAP_CANVAS;

    private static final float DEFAULT_MAP_WIDTH = 1024.0f;
    private static final float DEFAULT_MAP_HEIGHT = 512.0f;

    private TextureHandle texture;
    private final MutableRect source = new MutableRect(0.0f, 0.0f, 1.0f, 1.0f);
    private final MutableColor tint = new MutableColor(1.0f, 1.0f, 1.0f, 1.0f);
    private final MutableColor backgroundColor = new MutableColor(0.030f, 0.038f, 0.058f, 0.96f);
    private final MutableColor mapColor = new MutableColor(0.055f, 0.072f, 0.108f, 0.92f);
    private final MutableColor borderColor = new MutableColor(0.30f, 0.42f, 0.68f, 0.82f);
    private final MutableColor gridColor = new MutableColor(0.22f, 0.32f, 0.48f, 0.28f);
    private final ObjectArrayList<MapMarkerHandle<?>> markers = new ObjectArrayList<>();
    private MapProjection projection = MapProjection.identity();
    private float mapWidth = DEFAULT_MAP_WIDTH;
    private float mapHeight = DEFAULT_MAP_HEIGHT;
    private float gridSize = 128.0f;
    private boolean backgroundVisible = true;
    private boolean mapBorderVisible = true;
    private boolean gridVisible = true;

    /**
     * Создает карту без texture с дефолтным размером {@code 1024 x 512}.
     */
    public MapCanvas() {
        tint.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
        backgroundColor.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
        mapColor.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
        borderColor.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
        gridColor.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
        source.onChanged(() -> invalidate(InvalidationFlags.VISUAL));

        // Базовый слой карты добавляется первым, чтобы пользовательские world layers
        // рисовались поверх поверхности карты.
        addWorldLayer(this::renderMapLayer);
        worldBounds(0.0f, 0.0f, mapWidth, mapHeight);
    }

    /**
     * Создает карту с texture. Размер карты берется из texture.
     */
    public MapCanvas(TextureHandle texture) {
        this();
        texture(texture);
        if (texture != null) {
            mapSize(texture.width(), texture.height());
        }
    }

    /**
     * Создает карту с texture и явным размером в map-координатах.
     */
    public MapCanvas(TextureHandle texture, float mapWidth, float mapHeight) {
        this();
        texture(texture);
        mapSize(mapWidth, mapHeight);
    }

    /**
     * Fluent override для установки готового viewport.
     */
    @Override
    public MapCanvas viewport(Viewport2D viewport) {
        super.viewport(viewport);
        return this;
    }

    /**
     * Fluent override для изменения pan offset без изменения zoom.
     */
    @Override
    public MapCanvas viewport(float x, float y) {
        super.viewport(x, y);
        return this;
    }

    /**
     * Fluent override для установки pan offset и zoom.
     *
     * <p>{@code x/y} — экранное смещение карты внутри виджета, {@code zoom} —
     * масштаб map-координат в экранные пиксели.</p>
     */
    @Override
    public MapCanvas viewport(float x, float y, float zoom) {
        super.viewport(x, y, zoom);
        return this;
    }

    /**
     * Fluent override для ограничения zoom.
     */
    @Override
    public MapCanvas zoomRange(float minZoom, float maxZoom) {
        super.zoomRange(minZoom, maxZoom);
        return this;
    }

    /**
     * Fluent override для скорости wheel-pan.
    */
    @Override
    @XmlAttribute(value = "wheelPanStep", category = "Behavior", defaultValue = "32", description = "Viewport pan distance applied per wheel delta unit.")
    public MapCanvas wheelPanStep(float wheelPanStep) {
        super.wheelPanStep(wheelPanStep);
        return this;
    }

    /**
     * Fluent override для шага Ctrl+wheel zoom.
    */
    @Override
    @XmlAttribute(value = "zoomStep", category = "Behavior", defaultValue = "1.1", description = "Multiplicative zoom factor applied per wheel delta unit.")
    public MapCanvas zoomStep(float zoomStep) {
        super.zoomStep(zoomStep);
        return this;
    }

    /**
     * Texture, которая рисуется как поверхность карты; может быть {@code null}.
     */
    public TextureHandle texture() {
        return texture;
    }

    /**
     * Устанавливает texture поверхности карты.
     *
     * <p>Если texture не задана, карта рисует fallback-прямоугольник цветом
     * {@link #mapColor()}.</p>
     */
    @XmlAttribute(value = "texture", category = "Assets", defaultValue = "", description = "Texture resource id rendered as the map surface.")
    public MapCanvas texture(TextureHandle texture) {
        if (this.texture == texture) return this;
        this.texture = texture;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    /**
     * UV/source rectangle texture. По умолчанию используется вся texture: {@code 0,0,1,1}.
     */
    public MutableRect source() {
        return source;
    }

    /**
     * Задает UV/source rectangle texture.
     */
    public MapCanvas source(float u, float v, float width, float height) {
        source.set(u, v, width, height);
        return this;
    }

    /**
     * Tint для texture карты.
     */
    public MutableColor tint() {
        return tint;
    }

    /**
     * Цвет фона всего виджета карты за пределами поверхности карты.
     */
    public MutableColor backgroundColor() {
        return backgroundColor;
    }

    /**
     * Цвет поверхности карты, если texture не задана.
     */
    public MutableColor mapColor() {
        return mapColor;
    }

    /**
     * Цвет рамки поверхности карты.
     */
    public MutableColor borderColor() {
        return borderColor;
    }

    /**
     * Цвет debug/grid линий.
     */
    public MutableColor gridColor() {
        return gridColor;
    }

    /**
     * Ширина карты в map-координатах.
     */
    public float mapWidth() {
        return mapWidth;
    }

    /**
     * Высота карты в map-координатах.
     */
    public float mapHeight() {
        return mapHeight;
    }

    /**
     * Задает размер поверхности карты в map-координатах.
     *
     * <p>Это не размер UI-виджета. UI-размер задается layout-ом, а {@code mapSize}
     * задает внутреннюю координатную систему карты. Например, texture 4096x2048
     * обычно удобно использовать как {@code mapSize(4096, 2048)}.</p>
     */
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

    /**
     * Проекция внешних world координат в координаты карты.
     */
    public MapProjection projection() {
        return projection;
    }

    /**
     * Устанавливает проекцию для {@link #addProjectedMarkerWidget(String, float, float, Widget)}
     * и {@link #externalToMapX(float)} / {@link #externalToMapY(float)}.
     */
    public MapCanvas projection(MapProjection projection) {
        this.projection = projection == null ? MapProjection.identity() : projection;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    /**
     * Видимость фона всего canvas за пределами поверхности карты.
     */
    public boolean backgroundVisible() {
        return backgroundVisible;
    }

    /**
     * Включает/выключает фон всего canvas за пределами поверхности карты.
     */
    @XmlAttribute(value = "backgroundVisible", category = "Appearance", defaultValue = "true", description = "Whether the map canvas background is rendered behind the map surface.")
    public MapCanvas backgroundVisible(boolean backgroundVisible) {
        if (this.backgroundVisible == backgroundVisible) return this;
        this.backgroundVisible = backgroundVisible;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    /**
     * Видимость рамки поверхности карты.
     */
    public boolean mapBorderVisible() {
        return mapBorderVisible;
    }

    /**
     * Включает/выключает рамку поверхности карты.
     */
    @XmlAttribute(value = "mapBorderVisible", category = "Appearance", defaultValue = "true", description = "Whether the map surface border is rendered.")
    public MapCanvas mapBorderVisible(boolean mapBorderVisible) {
        if (this.mapBorderVisible == mapBorderVisible) return this;
        this.mapBorderVisible = mapBorderVisible;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    /**
     * Видимость встроенной grid-сетки карты.
     */
    public boolean gridVisible() {
        return gridVisible;
    }

    /**
     * Включает/выключает встроенную grid-сетку.
     */
    @XmlAttribute(value = "gridVisible", category = "Appearance", defaultValue = "true", description = "Whether the built-in map grid is rendered.")
    public MapCanvas gridVisible(boolean gridVisible) {
        if (this.gridVisible == gridVisible) return this;
        this.gridVisible = gridVisible;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    /**
     * Размер клетки grid в map-координатах.
     */
    public float gridSize() {
        return gridSize;
    }

    /**
     * Задает размер клетки grid в map-координатах.
     */
    @XmlAttribute(value = "gridSize", category = "Appearance", defaultValue = "128", description = "Map-space size of one rendered grid cell.")
    public MapCanvas gridSize(float gridSize) {
        float next = sanitizePositive(gridSize, 128.0f);
        if (this.gridSize == next) return this;
        this.gridSize = next;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    /**
     * Включает/выключает ограничение viewport границами карты.
     *
     * <p>Если включено, pan не даст полностью увести карту за пределы виджета.
     * Если выключено, карту можно свободно двигать как бесконечный canvas.</p>
     */
    @XmlAttribute(value = "clampToMapBounds", category = "Behavior", defaultValue = "false", description = "Whether pan/zoom is constrained to the map surface bounds.")
    public MapCanvas clampToMapBounds(boolean clamp) {
        worldBounds(0.0f, 0.0f, mapWidth, mapHeight);
        clampToWorldBounds(clamp);
        return this;
    }

    /**
     * Добавляет любой UI-виджет как маркер в координатах карты.
     *
     * <p>Это основной API для кастомных маркеров: кнопки, группы, карточки,
     * магазины, составные виджеты и т.п. Виджет останется screen-space элементом,
     * а позиция будет привязана к map-координате.</p>
     */
    public <T extends Widget> MapMarkerHandle<T> addMarkerWidget(String id, float mapX, float mapY, T widget) {
        AnchorWidget anchor = anchorLayer().add(id, mapX, mapY, widget)
                .pivot(0.5f, 0.5f);
        MapMarkerHandle<T> handle = new MapMarkerHandle<>(this, anchor, widget);
        markers.add(handle);
        attachMarkerAnchor(widget, anchor);
        return handle;
    }

    /**
     * Добавляет любой UI-виджет как маркер во внешних world координатах.
     *
     * <p>Координаты проходят через {@link #projection()}. Используй этот метод,
     * если твои данные живут в координатах игры, а не в пикселях карты.</p>
     */
    public <T extends Widget> MapMarkerHandle<T> addProjectedMarkerWidget(String id, float worldX, float worldY, T widget) {
        ViewportPoint point = projection.project(worldX, worldY);
        return addMarkerWidget(id, point.x(), point.y(), widget);
    }

    /**
     * Добавляет {@link MapMarker} в координатах карты.
     */
    public MapMarkerHandle<MapMarker> addMarker(String id, float mapX, float mapY, MapMarker marker) {
        return addMarkerWidget(id, mapX, mapY, marker == null ? new MapMarker() : marker);
    }

    /**
     * Добавляет {@link MapMarker} во внешних world координатах через {@link #projection()}.
     */
    public MapMarkerHandle<MapMarker> addProjectedMarker(String id, float worldX, float worldY, MapMarker marker) {
        return addProjectedMarkerWidget(id, worldX, worldY, marker == null ? new MapMarker() : marker);
    }

    /**
     * Ищет маркер по id.
     *
     * <p>Если anchor был удален напрямую через {@link #anchorLayer()}, устаревший
     * handle будет автоматически вычищен из внутреннего списка маркеров.</p>
     */
    public MapMarkerHandle<?> marker(String id) {
        String normalized = normalizeId(id);
        AnchorWidget anchor = anchorLayer().anchor(normalized);
        Object[] raw = markers.elements();
        for (int i = 0; i < markers.size(); i++) {
            MapMarkerHandle<?> marker = (MapMarkerHandle<?>) raw[i];
            if (!marker.id().equals(normalized)) continue;
            if (marker.anchor() == anchor) return marker;

            markers.remove(i);
            raw = markers.elements();
            i--;
        }
        return null;
    }

    /**
     * Перемещает маркер в координатах карты.
     */
    public boolean moveMarker(String id, float mapX, float mapY) {
        MapMarkerHandle<?> handle = marker(id);
        if (handle == null) return false;
        handle.position(mapX, mapY);
        return true;
    }

    /**
     * Перемещает маркер во внешних world координатах через {@link #projection()}.
     */
    public boolean moveProjectedMarker(String id, float worldX, float worldY) {
        MapMarkerHandle<?> handle = marker(id);
        if (handle == null) return false;
        handle.projectedPosition(worldX, worldY);
        return true;
    }

    /**
     * Удаляет маркер по id.
     */
    public boolean removeMarker(String id) {
        MapMarkerHandle<?> handle = marker(id);
        boolean removed = anchorLayer().remove(id);
        if (removed && handle != null) {
            markers.remove(handle);
            attachMarkerAnchor(handle.widget(), null);
        }
        return removed;
    }

    /**
     * Переводит внешний world X в координату карты.
     */
    public float externalToMapX(float worldX) {
        return projection.mapX(worldX);
    }

    /**
     * Переводит внешний world Y в координату карты.
     */
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

        // Поверхность карты живет в world/map координатах и поэтому двигается/зумится
        // вместе с viewport. Фон canvas выше не двигается.
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

    private static void attachMarkerAnchor(Widget widget, AnchorWidget anchor) {
        if (widget instanceof MapMarker marker) {
            marker.anchor(anchor);
        }
    }

    private static float sanitizePositive(float value, float fallback) {
        return Float.isFinite(value) && value > 0.0f ? value : fallback;
    }

    private static String normalizeId(String id) {
        return id == null ? "" : id.trim();
    }
}
