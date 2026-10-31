package dev.sixik.unigui.widgets.map;


import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiLink;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;

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
@AiDoc(summary = "Interactive 2D map widget extending WorldCanvas with map sizing, texture and grid layers, coordinate projection, and marker handles.")
@WikiPage(
        title = "MapCanvas",
        slug = "map-canvas",
        category = "<key:map.category>",
        summary = "<key:map.canvas.summary>"
)
@WikiSection(
        title = "<key:wiki.section.overview.title>",
        markdown = "<key:map.canvas.section.overview.body>",
        order = 10
)
@WikiExample(
        title = "<key:map.canvas.example.basic.title>",
        code = "MapCanvas map = new MapCanvas()\n    .mapSize(2048, 1024)\n    .viewport(0, 0, 1.0f);\nmap.addMarker(\"spawn\", 512, 256, \"Spawn\");"
)
@WikiLink(
        label = "WorldCanvas",
        target = "page:world-canvas",
        description = "<key:world.canvas.summary>"
)
@WikiLink(
        label = "MapProjection",
        target = "page:map-projection",
        description = "<key:map.projection.summary>"
)
@WikiLink(
        label = "MapMarker",
        target = "page:map-marker",
        description = "<key:map.marker.summary>"
)
@WikiLink(
        label = "MapMarkerHandle",
        target = "page:map-marker-handle",
        description = "<key:map.marker.handle.summary>"
)
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
        @AiDoc(summary = "Constructs an empty MapCanvas with default 1024x512 map dimensions.")
    @WikiMethod(summary = "<key:map.canvas.method.ctorDefault>")
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
        @AiDoc(summary = "Constructs a MapCanvas displaying the given texture with dimensions derived from the texture.")
    @WikiMethod(summary = "<key:map.canvas.method.ctorTexture>")
    public MapCanvas(
            @WikiParameter(description = "<key:map.canvas.param.texture>")
            TextureHandle texture
    ) {
        this();
        texture(texture);
        if (texture != null) {
            mapSize(texture.width(), texture.height());
        }
    }

    /**
     * Создает карту с texture и явным размером в map-координатах.
     */
        @AiDoc(summary = "Constructs a MapCanvas displaying a texture with explicit map width and height dimensions.")
    @WikiMethod(summary = "<key:map.canvas.method.ctorTextureSize>")
    public MapCanvas(
            @WikiParameter(description = "<key:map.canvas.param.texture>")
            TextureHandle texture,
            @WikiParameter(description = "<key:map.canvas.param.mapWidth>")
            float mapWidth,
            @WikiParameter(description = "<key:map.canvas.param.mapHeight>")
            float mapHeight
    ) {
        this();
        texture(texture);
        mapSize(mapWidth, mapHeight);
    }

    /**
     * Fluent override для установки готового viewport.
     */
    @Override
        @AiDoc(summary = "Sets the 2D pan and zoom viewport model.")
    @WikiMethod(summary = "<key:map.canvas.method.viewportModel>")
    @WikiReturn("<key:map.canvas.return.this>")
    public MapCanvas viewport(
            @WikiParameter(description = "<key:map.canvas.param.viewport>")
            Viewport2D viewport
    ) {
        super.viewport(viewport);
        return this;
    }

    /**
     * Fluent override для изменения pan offset без изменения zoom.
     */
    @Override
        @AiDoc(summary = "Sets viewport center coordinates while maintaining current zoom.")
    @WikiMethod(summary = "<key:map.canvas.method.viewportCoords>")
    @WikiReturn("<key:map.canvas.return.this>")
    public MapCanvas viewport(
            @WikiParameter(description = "<key:map.canvas.param.x>")
            float x,
            @WikiParameter(description = "<key:map.canvas.param.y>")
            float y
    ) {
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
        @AiDoc(summary = "Sets viewport center coordinates and zoom level simultaneously.")
    @WikiMethod(summary = "<key:map.canvas.method.viewportCoordsZoom>")
    @WikiReturn("<key:map.canvas.return.this>")
    public MapCanvas viewport(
            @WikiParameter(description = "<key:map.canvas.param.x>")
            float x,
            @WikiParameter(description = "<key:map.canvas.param.y>")
            float y,
            @WikiParameter(description = "<key:map.canvas.param.zoom>")
            float zoom
    ) {
        super.viewport(x, y, zoom);
        return this;
    }

    /**
     * Fluent override для ограничения zoom.
     */
    @Override
        @AiDoc(summary = "Sets the allowed minimum and maximum zoom scale bounds.")
    @WikiMethod(summary = "<key:map.canvas.method.zoomRange>")
    @WikiReturn("<key:map.canvas.return.this>")
    public MapCanvas zoomRange(
            @WikiParameter(description = "<key:map.canvas.param.minZoom>")
            float minZoom,
            @WikiParameter(description = "<key:map.canvas.param.maxZoom>")
            float maxZoom
    ) {
        super.zoomRange(minZoom, maxZoom);
        return this;
    }

    /**
     * Fluent override для скорости wheel-pan.
    */
    @Override
    @XmlAttribute(value = "wheelPanStep", category = "Behavior", defaultValue = "32", description = "Viewport pan distance applied per wheel delta unit.")
        @AiDoc(summary = "Sets the distance panned per mouse wheel click.")
    @WikiMethod(summary = "<key:map.canvas.method.wheelPanStep>")
    @WikiReturn("<key:map.canvas.return.this>")
    public MapCanvas wheelPanStep(
            @WikiParameter(description = "<key:map.canvas.param.wheelPanStep>")
            float wheelPanStep
    ) {
        super.wheelPanStep(wheelPanStep);
        return this;
    }

    /**
     * Fluent override для шага Ctrl+wheel zoom.
    */
    @Override
    @XmlAttribute(value = "zoomStep", category = "Behavior", defaultValue = "1.1", description = "Multiplicative zoom factor applied per wheel delta unit.")
        @AiDoc(summary = "Sets the zoom factor multiplier applied per mouse wheel step.")
    @WikiMethod(summary = "<key:map.canvas.method.zoomStep>")
    @WikiReturn("<key:map.canvas.return.this>")
    public MapCanvas zoomStep(
            @WikiParameter(description = "<key:map.canvas.param.zoomStep>")
            float zoomStep
    ) {
        super.zoomStep(zoomStep);
        return this;
    }

    /**
     * Texture, которая рисуется как поверхность карты; может быть {@code null}.
     */
        @AiDoc(summary = "Returns the texture handle of the rendered map background.")
    @WikiMethod(summary = "<key:map.canvas.method.textureGet>")
    @WikiReturn("<key:map.canvas.return.texture>")
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
        @AiDoc(summary = "Sets the map background texture handle.")
    @WikiMethod(summary = "<key:map.canvas.method.textureSet>")
    @WikiReturn("<key:map.canvas.return.this>")
    public MapCanvas texture(
            @WikiParameter(description = "<key:map.canvas.param.texture>")
            TextureHandle texture
    ) {
        if (this.texture == texture) return this;
        this.texture = texture;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    /**
     * UV/source rectangle texture. По умолчанию используется вся texture: {@code 0,0,1,1}.
     */
        @AiDoc(summary = "Returns the normalized UV source rectangle of the map texture.")
    @WikiMethod(summary = "<key:map.canvas.method.sourceGet>")
    @WikiReturn("<key:map.canvas.return.source>")
    public MutableRect source() {
        return source;
    }

    /**
     * Задает UV/source rectangle texture.
     */
        @AiDoc(summary = "Sets the normalized UV sub-region of the texture to display.")
    @WikiMethod(summary = "<key:map.canvas.method.sourceSet>")
    @WikiReturn("<key:map.canvas.return.this>")
    public MapCanvas source(
            @WikiParameter(description = "<key:map.canvas.param.u>")
            float u,
            @WikiParameter(description = "<key:map.canvas.param.v>")
            float v,
            @WikiParameter(description = "<key:map.canvas.param.width>")
            float width,
            @WikiParameter(description = "<key:map.canvas.param.height>")
            float height
    ) {
        source.set(u, v, width, height);
        return this;
    }

    /**
     * Tint для texture карты.
     */
        @AiDoc(summary = "Returns the mutable tint color applied to the map texture.")
    @WikiMethod(summary = "<key:map.canvas.method.tint>")
    @WikiReturn("<key:map.canvas.return.mutableColor>")
    public MutableColor tint() {
        return tint;
    }

    /**
     * Цвет фона всего виджета карты за пределами поверхности карты.
     */
        @AiDoc(summary = "Returns the mutable background fill color rendered outside map bounds.")
    @WikiMethod(summary = "<key:map.canvas.method.backgroundColor>")
    @WikiReturn("<key:map.canvas.return.mutableColor>")
    public MutableColor backgroundColor() {
        return backgroundColor;
    }

    /**
     * Цвет поверхности карты, если texture не задана.
     */
        @AiDoc(summary = "Returns the mutable fallback fill color used when no texture is loaded.")
    @WikiMethod(summary = "<key:map.canvas.method.mapColor>")
    @WikiReturn("<key:map.canvas.return.mutableColor>")
    public MutableColor mapColor() {
        return mapColor;
    }

    /**
     * Цвет рамки поверхности карты.
     */
        @AiDoc(summary = "Returns the mutable border line color surrounding the map boundaries.")
    @WikiMethod(summary = "<key:map.canvas.method.borderColor>")
    @WikiReturn("<key:map.canvas.return.mutableColor>")
    public MutableColor borderColor() {
        return borderColor;
    }

    /**
     * Цвет debug/grid линий.
     */
        @AiDoc(summary = "Returns the mutable line color of the coordinate grid overlay.")
    @WikiMethod(summary = "<key:map.canvas.method.gridColor>")
    @WikiReturn("<key:map.canvas.return.mutableColor>")
    public MutableColor gridColor() {
        return gridColor;
    }

    /**
     * Ширина карты в map-координатах.
     */
        @AiDoc(summary = "Returns the width dimension of the map in map coordinate units.")
    @WikiMethod(summary = "<key:map.canvas.method.mapWidthGet>")
    @WikiReturn("<key:map.canvas.return.dimension>")
    public float mapWidth() {
        return mapWidth;
    }

    /**
     * Высота карты в map-координатах.
     */
        @AiDoc(summary = "Returns the height dimension of the map in map coordinate units.")
    @WikiMethod(summary = "<key:map.canvas.method.mapHeightGet>")
    @WikiReturn("<key:map.canvas.return.dimension>")
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
        @AiDoc(summary = "Sets the dimensions of the map in map coordinate units.")
    @WikiMethod(summary = "<key:map.canvas.method.mapSize>")
    @WikiReturn("<key:map.canvas.return.this>")
    public MapCanvas mapSize(
            @WikiParameter(description = "<key:map.canvas.param.width>")
            float width,
            @WikiParameter(description = "<key:map.canvas.param.height>")
            float height
    ) {
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
        @AiDoc(summary = "Returns the active MapProjection used for external coordinate conversion.")
    @WikiMethod(summary = "<key:map.canvas.method.projectionGet>")
    @WikiReturn("<key:map.canvas.return.projection>")
    public MapProjection projection() {
        return projection;
    }

    /**
     * Устанавливает проекцию для {@link #addProjectedMarkerWidget(String, float, float, Widget)}
     * и {@link #externalToMapX(float)} / {@link #externalToMapY(float)}.
     */
        @AiDoc(summary = "Sets the coordinate projection converting world points to map space.")
    @WikiMethod(summary = "<key:map.canvas.method.projectionSet>")
    @WikiReturn("<key:map.canvas.return.this>")
    public MapCanvas projection(
            @WikiParameter(description = "<key:map.canvas.param.projection>")
            MapProjection projection
    ) {
        this.projection = projection == null ? MapProjection.identity() : projection;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    /**
     * Видимость фона всего canvas за пределами поверхности карты.
     */
        @AiDoc(summary = "Returns true if the outer background canvas fill is rendered.")
    @WikiMethod(summary = "<key:map.canvas.method.backgroundVisibleGet>")
    @WikiReturn("<key:map.canvas.return.visible>")
    public boolean backgroundVisible() {
        return backgroundVisible;
    }

    /**
     * Включает/выключает фон всего canvas за пределами поверхности карты.
     */
    @XmlAttribute(value = "backgroundVisible", category = "Appearance", defaultValue = "true", description = "Whether the map canvas background is rendered behind the map surface.")
        @AiDoc(summary = "Enables or disables outer canvas background rendering.")
    @WikiMethod(summary = "<key:map.canvas.method.backgroundVisibleSet>")
    @WikiReturn("<key:map.canvas.return.this>")
    public MapCanvas backgroundVisible(
            @WikiParameter(description = "<key:map.canvas.param.visible>")
            boolean backgroundVisible
    ) {
        if (this.backgroundVisible == backgroundVisible) return this;
        this.backgroundVisible = backgroundVisible;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    /**
     * Видимость рамки поверхности карты.
     */
        @AiDoc(summary = "Returns true if the border rectangle around map boundaries is rendered.")
    @WikiMethod(summary = "<key:map.canvas.method.mapBorderVisibleGet>")
    @WikiReturn("<key:map.canvas.return.visible>")
    public boolean mapBorderVisible() {
        return mapBorderVisible;
    }

    /**
     * Включает/выключает рамку поверхности карты.
     */
    @XmlAttribute(value = "mapBorderVisible", category = "Appearance", defaultValue = "true", description = "Whether the map surface border is rendered.")
        @AiDoc(summary = "Enables or disables the border outline around the map perimeter.")
    @WikiMethod(summary = "<key:map.canvas.method.mapBorderVisibleSet>")
    @WikiReturn("<key:map.canvas.return.this>")
    public MapCanvas mapBorderVisible(
            @WikiParameter(description = "<key:map.canvas.param.visible>")
            boolean mapBorderVisible
    ) {
        if (this.mapBorderVisible == mapBorderVisible) return this;
        this.mapBorderVisible = mapBorderVisible;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    /**
     * Видимость встроенной grid-сетки карты.
     */
        @AiDoc(summary = "Returns true if the overlay grid lines are rendered.")
    @WikiMethod(summary = "<key:map.canvas.method.gridVisibleGet>")
    @WikiReturn("<key:map.canvas.return.visible>")
    public boolean gridVisible() {
        return gridVisible;
    }

    /**
     * Включает/выключает встроенную grid-сетку.
     */
    @XmlAttribute(value = "gridVisible", category = "Appearance", defaultValue = "true", description = "Whether the built-in map grid is rendered.")
        @AiDoc(summary = "Enables or disables coordinate grid lines across the map.")
    @WikiMethod(summary = "<key:map.canvas.method.gridVisibleSet>")
    @WikiReturn("<key:map.canvas.return.this>")
    public MapCanvas gridVisible(
            @WikiParameter(description = "<key:map.canvas.param.visible>")
            boolean gridVisible
    ) {
        if (this.gridVisible == gridVisible) return this;
        this.gridVisible = gridVisible;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    /**
     * Размер клетки grid в map-координатах.
     */
        @AiDoc(summary = "Returns the distance between grid lines in map units.")
    @WikiMethod(summary = "<key:map.canvas.method.gridSizeGet>")
    @WikiReturn("<key:map.canvas.return.dimension>")
    public float gridSize() {
        return gridSize;
    }

    /**
     * Задает размер клетки grid в map-координатах.
     */
    @XmlAttribute(value = "gridSize", category = "Appearance", defaultValue = "128", description = "Map-space size of one rendered grid cell.")
        @AiDoc(summary = "Sets the cell spacing distance for the coordinate grid overlay.")
    @WikiMethod(summary = "<key:map.canvas.method.gridSizeSet>")
    @WikiReturn("<key:map.canvas.return.this>")
    public MapCanvas gridSize(
            @WikiParameter(description = "<key:map.canvas.param.gridSize>")
            float gridSize
    ) {
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
        @AiDoc(summary = "Clamps pan navigation within the map area bounds.")
    @WikiMethod(summary = "<key:map.canvas.method.clampToMapBounds>")
    @WikiReturn("<key:map.canvas.return.this>")
    public MapCanvas clampToMapBounds(
            @WikiParameter(description = "<key:map.canvas.param.clamp>")
            boolean clamp
    ) {
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
        @AiDoc(summary = "Adds an arbitrary UI widget as a map marker at map coordinates.")
    @WikiMethod(summary = "<key:map.canvas.method.addMarkerWidget>")
    @WikiReturn("<key:map.canvas.return.markerHandle>")
    public <T extends Widget> MapMarkerHandle<T> addMarkerWidget(
            @WikiParameter(description = "<key:map.canvas.param.id>")
            String id,
            @WikiParameter(description = "<key:map.canvas.param.mapX>")
            float mapX,
            @WikiParameter(description = "<key:map.canvas.param.mapY>")
            float mapY,
            @WikiParameter(description = "<key:map.canvas.param.widget>")
            T widget
    ) {
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
        @AiDoc(summary = "Adds an arbitrary UI widget as a marker using external world coordinates projected to map space.")
    @WikiMethod(summary = "<key:map.canvas.method.addProjectedMarkerWidget>")
    @WikiReturn("<key:map.canvas.return.markerHandle>")
    public <T extends Widget> MapMarkerHandle<T> addProjectedMarkerWidget(
            @WikiParameter(description = "<key:map.canvas.param.id>")
            String id,
            @WikiParameter(description = "<key:map.canvas.param.worldX>")
            float worldX,
            @WikiParameter(description = "<key:map.canvas.param.worldY>")
            float worldY,
            @WikiParameter(description = "<key:map.canvas.param.widget>")
            T widget
    ) {
        ViewportPoint point = projection.project(worldX, worldY);
        return addMarkerWidget(id, point.x(), point.y(), widget);
    }

    /**
     * Добавляет {@link MapMarker} в координатах карты.
     */
        @AiDoc(summary = "Adds a MapMarker shell at map coordinates.")
    @WikiMethod(summary = "<key:map.canvas.method.addMarkerInstance>")
    @WikiReturn("<key:map.canvas.return.markerHandle>")
    public MapMarkerHandle<MapMarker> addMarker(
            @WikiParameter(description = "<key:map.canvas.param.id>")
            String id,
            @WikiParameter(description = "<key:map.canvas.param.mapX>")
            float mapX,
            @WikiParameter(description = "<key:map.canvas.param.mapY>")
            float mapY,
            @WikiParameter(description = "<key:map.canvas.param.marker>")
            MapMarker marker
    ) {
        return addMarkerWidget(id, mapX, mapY, marker == null ? new MapMarker() : marker);
    }

    /**
     * Добавляет {@link MapMarker} во внешних world координатах через {@link #projection()}.
     */
        @AiDoc(summary = "Adds a MapMarker shell using external world coordinates projected to map space.")
    @WikiMethod(summary = "<key:map.canvas.method.addProjectedMarkerInstance>")
    @WikiReturn("<key:map.canvas.return.markerHandle>")
    public MapMarkerHandle<MapMarker> addProjectedMarker(
            @WikiParameter(description = "<key:map.canvas.param.id>")
            String id,
            @WikiParameter(description = "<key:map.canvas.param.worldX>")
            float worldX,
            @WikiParameter(description = "<key:map.canvas.param.worldY>")
            float worldY,
            @WikiParameter(description = "<key:map.canvas.param.marker>")
            MapMarker marker
    ) {
        return addProjectedMarkerWidget(id, worldX, worldY, marker == null ? new MapMarker() : marker);
    }

    /**
     * Ищет маркер по id.
     *
     * <p>Если anchor был удален напрямую через {@link #anchorLayer()}, устаревший
     * handle будет автоматически вычищен из внутреннего списка маркеров.</p>
     */
        @AiDoc(summary = "Finds an existing marker handle by its string identifier, or returns null if not found.")
    @WikiMethod(summary = "<key:map.canvas.method.marker>")
    @WikiReturn("<key:map.canvas.return.markerHandle>")
    public MapMarkerHandle<?> marker(
            @WikiParameter(description = "<key:map.canvas.param.id>")
            String id
    ) {
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
        @AiDoc(summary = "Moves an existing marker to new map coordinates.")
    @WikiMethod(summary = "<key:map.canvas.method.moveMarker>")
    @WikiReturn("<key:map.canvas.return.success>")
    public boolean moveMarker(
            @WikiParameter(description = "<key:map.canvas.param.id>")
            String id,
            @WikiParameter(description = "<key:map.canvas.param.mapX>")
            float mapX,
            @WikiParameter(description = "<key:map.canvas.param.mapY>")
            float mapY
    ) {
        MapMarkerHandle<?> handle = marker(id);
        if (handle == null) return false;
        handle.position(mapX, mapY);
        return true;
    }

    /**
     * Перемещает маркер во внешних world координатах через {@link #projection()}.
     */
        @AiDoc(summary = "Moves an existing marker to new external world coordinates projected to map space.")
    @WikiMethod(summary = "<key:map.canvas.method.moveProjectedMarker>")
    @WikiReturn("<key:map.canvas.return.success>")
    public boolean moveProjectedMarker(
            @WikiParameter(description = "<key:map.canvas.param.id>")
            String id,
            @WikiParameter(description = "<key:map.canvas.param.worldX>")
            float worldX,
            @WikiParameter(description = "<key:map.canvas.param.worldY>")
            float worldY
    ) {
        MapMarkerHandle<?> handle = marker(id);
        if (handle == null) return false;
        handle.projectedPosition(worldX, worldY);
        return true;
    }

    /**
     * Удаляет маркер по id.
     */
        @AiDoc(summary = "Removes a marker by its string identifier.")
    @WikiMethod(summary = "<key:map.canvas.method.removeMarker>")
    @WikiReturn("<key:map.canvas.return.success>")
    public boolean removeMarker(
            @WikiParameter(description = "<key:map.canvas.param.id>")
            String id
    ) {
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
        @AiDoc(summary = "Converts an external world X coordinate to map X space using the current projection.")
    @WikiMethod(summary = "<key:map.canvas.method.externalToMapX>")
    @WikiReturn("<key:map.canvas.return.coordinate>")
    public float externalToMapX(
            @WikiParameter(description = "<key:map.canvas.param.worldX>")
            float worldX
    ) {
        return projection.mapX(worldX);
    }

    /**
     * Переводит внешний world Y в координату карты.
     */
        @AiDoc(summary = "Converts an external world Y coordinate to map Y space using the current projection.")
    @WikiMethod(summary = "<key:map.canvas.method.externalToMapY>")
    @WikiReturn("<key:map.canvas.return.coordinate>")
    public float externalToMapY(
            @WikiParameter(description = "<key:map.canvas.param.worldY>")
            float worldY
    ) {
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
        // Рисуем только линии, пересекающие видимый rect canvas'а: при большой
        // карте и мелкой сетке невидимые линии составляли бы тысячи draw-команд.
        float visibleMinX = Math.max(0.0f, rootToWorldX(layoutBounds().x()));
        float visibleMaxX = Math.min(mapWidth, rootToWorldX(layoutBounds().x() + layoutBounds().width()));
        float visibleMinY = Math.max(0.0f, rootToWorldY(layoutBounds().y()));
        float visibleMaxY = Math.min(mapHeight, rootToWorldY(layoutBounds().y() + layoutBounds().height()));
        if (visibleMaxX < 0.0f || visibleMinX > mapWidth || visibleMaxY < 0.0f || visibleMinY > mapHeight) return;
        float firstX = (float) Math.floor(visibleMinX / safeGrid) * safeGrid;
        float firstY = (float) Math.floor(visibleMinY / safeGrid) * safeGrid;
        for (float x = firstX; x <= visibleMaxX + 0.0001f; x += safeGrid) {
            float gx = worldToRootX(x);
            draw.addLine(gx, mapY, gx, mapY + mapH, gridColor, 1.0f);
        }
        for (float y = firstY; y <= visibleMaxY + 0.0001f; y += safeGrid) {
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
