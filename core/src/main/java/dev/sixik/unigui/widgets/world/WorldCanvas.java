package dev.sixik.unigui.widgets.world;


import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiLink;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;

import dev.sixik.unigui.api.core.FrameContext;
import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.core.UIContext;
import dev.sixik.unigui.api.event.Event;
import dev.sixik.unigui.api.event.EventListener;
import dev.sixik.unigui.api.event.EventPhase;
import dev.sixik.unigui.api.event.EventSubscription;
import dev.sixik.unigui.api.event.PointerEvent;
import dev.sixik.unigui.api.event.PointerMovedEvent;
import dev.sixik.unigui.api.event.PointerPressedEvent;
import dev.sixik.unigui.api.event.PointerReleasedEvent;
import dev.sixik.unigui.api.event.RoutableWidgetEvent;
import dev.sixik.unigui.api.event.ScrollEvent;
import dev.sixik.unigui.api.event.ViewportChangedEvent;
import dev.sixik.unigui.api.input.KeyModifiers;
import dev.sixik.unigui.api.input.PointerButton;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.layout.LayoutSize;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.viewport.Viewport2D;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.api.xml.XmlAttribute;
import dev.sixik.unigui.api.xml.XmlWidgetName;
import dev.sixik.unigui.impl.widget.WidgetBase;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Базовый 2D-холст с панорамированием, зумом и привязкой обычных виджетов к
 * координатам виртуального мира.
 *
 * <p>{@code WorldCanvas} сам ничего не знает про карты, ноды или игровые
 * координаты. Он хранит только {@link Viewport2D}: смещение экрана
 * {@code x/y}, текущий {@code zoom}, диапазон зума и опциональные границы мира.
 * Все координаты {@code worldX/worldY} задаются в произвольных единицах
 * вызывающего кода.</p>
 *
 * <p>Есть два способа рисовать содержимое:</p>
 *
 * <ul>
 *     <li>{@link WorldLayer} — лёгкий render-only слой для сеток, линий,
 *     фоновых шейпов и прочей геометрии, которой не нужны layout/events.</li>
 *     <li>{@link AnchorLayer} / {@link AnchorWidget} — слой обычных виджетов,
 *     которые проецируются из world-space в screen-space. Размер таких
 *     виджетов задаётся в пикселях экрана и не масштабируется зумом, если
 *     пользователь явно не сделает это сам.</li>
 * </ul>
 *
 * <p>Минимальный пример:</p>
 *
 * <pre>{@code
 * WorldCanvas canvas = new WorldCanvas()
 *         .viewport(140.0f, 92.0f, 1.0f)
 *         .zoomRange(0.45f, 3.5f)
 *         .worldBounds(-2048.0f, -1024.0f, 4096.0f, 2048.0f)
 *         .clampToWorldBounds(true);
 *
 * canvas.addWorldLayer((world, draw) -> {
 *     float x = world.worldToRootX(nodeWorldX);
 *     float y = world.worldToRootY(nodeWorldY);
 *     draw.addCircleFilled(x, y, 4.0f, color);
 * });
 *
 * canvas.anchorLayer().add("shop", shopWorldX, shopWorldY, shopButton)
 *         .screenSize(88.0f, 24.0f)
 *         .pivot(0.5f, 0.5f);
 * }</pre>
 *
 * @see Viewport2D
 * @see WorldLayer
 * @see AnchorLayer
 * @see dev.sixik.unigui.widgets.map.MapCanvas
 */
@AiDoc(summary = "Infinite 2D/3D world canvas supporting camera pan and zoom, coordinate transformations, background world layers, and pinned screen-space anchor widgets.")
@WikiPage(
        title = "WorldCanvas",
        slug = "world-canvas",
        category = "<key:world.category>",
        summary = "<key:world.canvas.summary>"
)
@WikiSection(
        title = "<key:wiki.section.overview.title>",
        markdown = "<key:world.canvas.section.overview.body>",
        order = 10
)
@WikiExample(
        title = "<key:world.canvas.example.basic.title>",
        code = "WorldCanvas canvas = new WorldCanvas()\n    .viewport(0, 0, 1.0f)\n    .zoomRange(0.1f, 5.0f);\ncanvas.anchorLayer().add(\"marker\", 100, 200, new Button(\"POI\"));"
)
@WikiLink(
        label = "MapCanvas",
        target = "page:map-canvas",
        description = "<key:map.canvas.summary>"
)
@WikiLink(
        label = "AnchorLayer",
        target = "page:anchor-layer",
        description = "<key:anchor.layer.summary>"
)
@WikiLink(
        label = "RibbonLayer",
        target = "page:ribbon-layer",
        description = "<key:ribbon.layer.summary>"
)
@WikiLink(
        label = "WorldLayer",
        target = "page:world-layer",
        description = "<key:world.layer.summary>"
)
@XmlWidgetName("WorldCanvas")
public class WorldCanvas extends WidgetBase {
    public static final String STYLE_TYPE = dev.sixik.unigui.api.style.StyleIds.Widget.WORLD_CANVAS;

    public static final float DEFAULT_PREFERRED_WIDTH = 320.0f;
    public static final float DEFAULT_PREFERRED_HEIGHT = 240.0f;

    private Viewport2D viewport = new Viewport2D();
    /**
     * Screen-space виджеты, привязанные к world-space точкам.
     */
    private final AnchorLayer anchorLayer = new AnchorLayer(this);
    /**
     * Render-only слои. Они рисуются до {@link #anchorLayer}, то есть под
     * интерактивными якорями.
     */
    private final ObjectArrayList<WorldLayer> worldLayers = new ObjectArrayList<>();
    private final List<WorldLayer> worldLayersView = Collections.unmodifiableList(worldLayers);
    private final ObjectArrayList<Widget> children = new ObjectArrayList<>();
    private final List<Widget> childrenView = Collections.unmodifiableList(children);

    /*
     * Снапшот слоёв нужен для горячего цикла render: структура меняется редко,
     * а обходить массив дешевле, чем каждый кадр создавать iterator.
     */
    private WorldLayer[] worldLayerSnapshot = new WorldLayer[0];
    private boolean worldLayerSnapshotDirty = true;

    private boolean clippingEnabled = true;
    private boolean panningEnabled = true;
    private boolean zoomEnabled = true;
    private boolean wheelPanningEnabled = true;
    private boolean consumeWheelWhileHovered = true;
    private float wheelPanStep = 32.0f;
    private float zoomStep = 1.1f;
    private PointerButton panButton = PointerButton.PRIMARY;
    private float preferredWidth = DEFAULT_PREFERRED_WIDTH;
    private float preferredHeight = DEFAULT_PREFERRED_HEIGHT;
    private PanDragState panDrag;

    /**
     * Возвращает живую модель viewport'а.
     *
     * <p>Через неё можно читать текущие {@code x/y/zoom}. Если нужно менять
     * viewport и автоматически получить invalidate + {@link ViewportChangedEvent},
     * предпочитай fluent-методы самого {@code WorldCanvas}: {@link #viewport(float, float, float)},
     * {@link #zoomRange(float, float)}, {@link #worldBounds(float, float, float, float)}.</p>
     *
     * @return текущий {@link Viewport2D}
     */
        @AiDoc(summary = "Returns the active 2D viewport model.")
    @WikiMethod(summary = "<key:world.canvas.method.viewportGet>")
    @WikiReturn("<key:world.canvas.return.viewport>")
    public Viewport2D viewport() {
        return viewport;
    }

    /**
     * Подменяет модель viewport'а.
     *
     * <p>{@code null} безопасно заменяется на новый пустой {@link Viewport2D}.
     * Метод полезен, если один и тот же viewport нужно шарить между несколькими
     * компонентами или заранее настроить снаружи.</p>
     *
     * @param viewport новая модель viewport'а
     * @return этот canvas для fluent-настройки
     */
        @AiDoc(summary = "Sets the 2D pan and zoom viewport model.")
    @WikiMethod(summary = "<key:world.canvas.method.viewportModel>")
    @WikiReturn("<key:world.canvas.return.this>")
    public WorldCanvas viewport(
            @WikiParameter(description = "<key:world.canvas.param.viewport>")
            Viewport2D viewport
    ) {
        Viewport2D next = viewport == null ? new Viewport2D() : viewport;
        if (this.viewport == next) return this;
        float oldX = this.viewport.x();
        float oldY = this.viewport.y();
        float oldZoom = this.viewport.zoom();
        this.viewport = next;
        afterViewportChanged(oldX, oldY, oldZoom);
        return this;
    }

    /**
     * Меняет только screen-space смещение viewport'а, сохраняя текущий zoom.
     *
     * <p>Смещение задаётся в пикселях локального пространства canvas'а:
     * {@code screenX = viewportX + worldX * zoom}.</p>
     *
     * @param x смещение по X в screen-space
     * @param y смещение по Y в screen-space
     * @return этот canvas для fluent-настройки
     */
        @AiDoc(summary = "Sets viewport center coordinates while maintaining current zoom.")
    @WikiMethod(summary = "<key:world.canvas.method.viewportCoords>")
    @WikiReturn("<key:world.canvas.return.this>")
    public WorldCanvas viewport(
            @WikiParameter(description = "<key:world.canvas.param.x>")
            float x,
            @WikiParameter(description = "<key:world.canvas.param.y>")
            float y
    ) {
        return viewport(x, y, viewport.zoom());
    }

    /**
     * Меняет screen-space смещение и масштаб viewport'а.
     *
     * <p>{@code zoom} автоматически приводится к текущему диапазону
     * {@link #zoomRange(float, float)}.</p>
     *
     * @param x screen-space смещение по X
     * @param y screen-space смещение по Y
     * @param zoom масштаб мира относительно экрана
     * @return этот canvas для fluent-настройки
     */
        @AiDoc(summary = "Sets viewport center coordinates and zoom level simultaneously.")
    @WikiMethod(summary = "<key:world.canvas.method.viewportCoordsZoom>")
    @WikiReturn("<key:world.canvas.return.this>")
    public WorldCanvas viewport(
            @WikiParameter(description = "<key:world.canvas.param.x>")
            float x,
            @WikiParameter(description = "<key:world.canvas.param.y>")
            float y,
            @WikiParameter(description = "<key:world.canvas.param.zoom>")
            float zoom
    ) {
        float oldX = viewport.x();
        float oldY = viewport.y();
        float oldZoom = viewport.zoom();
        if (!viewport.set(x, y, zoom)) return this;
        afterViewportChanged(oldX, oldY, oldZoom);
        return this;
    }

    /**
     * Задаёт допустимый диапазон зума.
     *
     * <p>Если текущий zoom выходит за новый диапазон, он будет сразу зажат, а
     * anchors будут переложены под новое состояние viewport'а.</p>
     *
     * @param minZoom минимальный положительный zoom
     * @param maxZoom максимальный zoom, не меньше {@code minZoom}
     * @return этот canvas для fluent-настройки
     */
        @AiDoc(summary = "Sets the allowed minimum and maximum zoom scale bounds.")
    @WikiMethod(summary = "<key:world.canvas.method.zoomRange>")
    @WikiReturn("<key:world.canvas.return.this>")
    public WorldCanvas zoomRange(
            @WikiParameter(description = "<key:world.canvas.param.minZoom>")
            float minZoom,
            @WikiParameter(description = "<key:world.canvas.param.maxZoom>")
            float maxZoom
    ) {
        float oldX = viewport.x();
        float oldY = viewport.y();
        float oldZoom = viewport.zoom();
        viewport.zoomRange(minZoom, maxZoom);
        if (oldX != viewport.x() || oldY != viewport.y() || oldZoom != viewport.zoom()) {
            afterViewportChanged(oldX, oldY, oldZoom);
        }
        return this;
    }

    /**
     * Задаёт прямоугольник доступной области мира.
     *
     * <p>Сами по себе границы только сохраняются в {@link Viewport2D}. Чтобы
     * реально зажимать pan/zoom внутри них, включи {@link #clampToWorldBounds(boolean)}.</p>
     *
     * @param x левая граница мира
     * @param y верхняя граница мира
     * @param width ширина мира
     * @param height высота мира
     * @return этот canvas для fluent-настройки
     */
        @AiDoc(summary = "Sets the rectangular world bounds constraining camera movement.")
    @WikiMethod(summary = "<key:world.canvas.method.worldBounds>")
    @WikiReturn("<key:world.canvas.return.this>")
    public WorldCanvas worldBounds(
            @WikiParameter(description = "<key:world.canvas.param.x>")
            float x,
            @WikiParameter(description = "<key:world.canvas.param.y>")
            float y,
            @WikiParameter(description = "<key:world.canvas.param.width>")
            float width,
            @WikiParameter(description = "<key:world.canvas.param.height>")
            float height
    ) {
        viewport.worldBounds(x, y, width, height);
        clampViewport();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    /**
     * Убирает сохранённые границы мира.
     *
     * @return этот canvas для fluent-настройки
     */
        @AiDoc(summary = "Clears world boundaries allowing infinite camera panning.")
    @WikiMethod(summary = "<key:world.canvas.method.clearWorldBounds>")
    @WikiReturn("<key:world.canvas.return.this>")
    public WorldCanvas clearWorldBounds() {
        viewport.clearWorldBounds();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    /**
     * Включает или выключает удержание viewport'а внутри {@link #worldBounds(float, float, float, float)}.
     *
     * <p>Если контент меньше самого canvas'а, viewport центрируется по этой оси.</p>
     *
     * @param clampToWorldBounds {@code true}, чтобы зажимать pan/zoom по границам мира
     * @return этот canvas для fluent-настройки
     */
    @XmlAttribute(value = "clampToWorldBounds", category = "Behavior", defaultValue = "false", description = "Whether pan/zoom is constrained to configured world bounds.")
        @AiDoc(summary = "Enables or disables clamping of camera pan within world boundaries.")
    @WikiMethod(summary = "<key:world.canvas.method.clampToWorldBounds>")
    @WikiReturn("<key:world.canvas.return.this>")
    public WorldCanvas clampToWorldBounds(
            @WikiParameter(description = "<key:world.canvas.param.clamp>")
            boolean clampToWorldBounds
    ) {
        viewport.clampToWorldBounds(clampToWorldBounds);
        clampViewport();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    /**
     * Возвращает слой якорей — обычных виджетов, привязанных к world-space координатам.
     *
     * <p>Через него добавляются маркеры, кнопки, тултипы и любые другие виджеты,
     * которые должны стоять в точке мира, но оставаться screen-space UI.</p>
     *
     * @return слой якорей этого canvas'а
     */
        @AiDoc(summary = "Returns the AnchorLayer managing world-pinned UI widgets.")
    @WikiMethod(summary = "<key:world.canvas.method.anchorLayer>")
    @WikiReturn("<key:world.canvas.return.anchorLayer>")
    public AnchorLayer anchorLayer() {
        return anchorLayer;
    }

    /**
     * Возвращает read-only список render-only слоёв в порядке отрисовки.
     *
     * @return неизменяемое представление слоёв мира
     */
        @AiDoc(summary = "Returns an unmodifiable list of all registered world rendering layers.")
    @WikiMethod(summary = "<key:world.canvas.method.worldLayers>")
    @WikiReturn("<key:world.canvas.return.worldLayers>")
    public List<WorldLayer> worldLayers() {
        return worldLayersView;
    }

    /**
     * Добавляет render-only слой мира.
     *
     * <p>Слой будет вызываться каждый render до отрисовки anchor-виджетов.
     * Это правильное место для сетки, линий связей, фона, glow-геометрии и
     * прочих элементов, которым не нужны layout и события.</p>
     *
     * @param layer слой для добавления
     * @return этот canvas для fluent-настройки
     */
        @AiDoc(summary = "Registers a new world rendering layer.")
    @WikiMethod(summary = "<key:world.canvas.method.addWorldLayer>")
    @WikiReturn("<key:world.canvas.return.this>")
    public WorldCanvas addWorldLayer(
            @WikiParameter(description = "<key:world.canvas.param.layer>")
            WorldLayer layer
    ) {
        if (layer == null) return this;
        worldLayers.add(layer);
        worldLayerSnapshotDirty = true;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    /**
     * Удаляет render-only слой мира.
     *
     * @param layer слой для удаления
     * @return этот canvas для fluent-настройки
     */
        @AiDoc(summary = "Removes a registered world rendering layer.")
    @WikiMethod(summary = "<key:world.canvas.method.removeWorldLayer>")
    @WikiReturn("<key:world.canvas.return.this>")
    public WorldCanvas removeWorldLayer(
            @WikiParameter(description = "<key:world.canvas.param.layer>")
            WorldLayer layer
    ) {
        if (layer == null || !worldLayers.remove(layer)) return this;
        worldLayerSnapshotDirty = true;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    /**
     * Удаляет все render-only слои мира.
     *
     * <p>Anchor-виджеты при этом не трогаются. Для них используй
     * {@link #anchorLayer()}.{@code clear()}.</p>
     *
     * @return этот canvas для fluent-настройки
     */
        @AiDoc(summary = "Clears all registered world rendering layers.")
    @WikiMethod(summary = "<key:world.canvas.method.clearWorldLayers>")
    @WikiReturn("<key:world.canvas.return.this>")
    public WorldCanvas clearWorldLayers() {
        if (worldLayers.isEmpty()) return this;
        worldLayers.clear();
        worldLayerSnapshotDirty = true;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    /**
     * Проверяет, включён ли clip по границам canvas'а.
     *
     * @return {@code true}, если world layers и anchors режутся по layout bounds
     */
        @AiDoc(summary = "Returns true if canvas rendering is clipped to widget layout bounds.")
    @WikiMethod(summary = "<key:world.canvas.method.clippingEnabledGet>")
    @WikiReturn("<key:world.canvas.return.flag>")
    public boolean clippingEnabled() {
        return clippingEnabled;
    }

    /**
     * Управляет clip'ом содержимого по bounds canvas'а.
     *
     * <p>Обычно стоит держать включённым. Выключать имеет смысл для debug-оверлеев
     * или эффектов, которые должны выходить за пределы canvas'а.</p>
     *
     * @param clippingEnabled {@code true}, чтобы резать содержимое по bounds
     * @return этот canvas для fluent-настройки
     */
    @XmlAttribute(value = "clippingEnabled", category = "Behavior", defaultValue = "true", description = "Whether world layers and anchors are clipped to canvas bounds.")
        @AiDoc(summary = "Enables or disables clipping of rendering to widget layout bounds.")
    @WikiMethod(summary = "<key:world.canvas.method.clippingEnabledSet>")
    @WikiReturn("<key:world.canvas.return.this>")
    public WorldCanvas clippingEnabled(
            @WikiParameter(description = "<key:world.canvas.param.clippingEnabled>")
            boolean clippingEnabled
    ) {
        if (this.clippingEnabled == clippingEnabled) return this;
        this.clippingEnabled = clippingEnabled;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    /**
     * Проверяет, включён ли drag-pan мышью.
     *
     * @return {@code true}, если canvas реагирует на {@link #panButton()}
     */
        @AiDoc(summary = "Returns true if mouse dragging can pan the camera viewport.")
    @WikiMethod(summary = "<key:world.canvas.method.panningEnabledGet>")
    @WikiReturn("<key:world.canvas.return.flag>")
    public boolean panningEnabled() {
        return panningEnabled;
    }

    /**
     * Включает или выключает drag-pan мышью.
     *
     * @param panningEnabled {@code true}, чтобы разрешить перетаскивание viewport'а
     * @return этот canvas для fluent-настройки
     */
    @XmlAttribute(value = "panningEnabled", category = "Behavior", defaultValue = "true", description = "Whether pointer drag can pan the viewport.")
        @AiDoc(summary = "Enables or disables pointer dragging to pan the viewport.")
    @WikiMethod(summary = "<key:world.canvas.method.panningEnabledSet>")
    @WikiReturn("<key:world.canvas.return.this>")
    public WorldCanvas panningEnabled(
            @WikiParameter(description = "<key:world.canvas.param.panningEnabled>")
            boolean panningEnabled
    ) {
        this.panningEnabled = panningEnabled;
        return this;
    }

    /**
     * Проверяет, включён ли zoom через {@code Ctrl + колесо}.
     *
     * @return {@code true}, если zoom жестом разрешён
     */
        @AiDoc(summary = "Returns true if mouse wheel zooming is enabled.")
    @WikiMethod(summary = "<key:world.canvas.method.zoomEnabledGet>")
    @WikiReturn("<key:world.canvas.return.flag>")
    public boolean zoomEnabled() {
        return zoomEnabled;
    }

    /**
     * Включает или выключает zoom через {@code Ctrl + колесо}.
     *
     * @param zoomEnabled {@code true}, чтобы разрешить интерактивный zoom
     * @return этот canvas для fluent-настройки
     */
    @XmlAttribute(value = "zoomEnabled", category = "Behavior", defaultValue = "true", description = "Whether Ctrl-wheel zoom gestures are enabled.")
        @AiDoc(summary = "Enables or disables mouse wheel zooming.")
    @WikiMethod(summary = "<key:world.canvas.method.zoomEnabledSet>")
    @WikiReturn("<key:world.canvas.return.this>")
    public WorldCanvas zoomEnabled(
            @WikiParameter(description = "<key:world.canvas.param.zoomEnabled>")
            boolean zoomEnabled
    ) {
        this.zoomEnabled = zoomEnabled;
        return this;
    }

    /**
     * Проверяет, включён ли pan колесом/тачпадом без {@code Ctrl}.
     *
     * @return {@code true}, если scroll двигает viewport
     */
        @AiDoc(summary = "Returns true if mouse wheel scrolling pans the camera.")
    @WikiMethod(summary = "<key:world.canvas.method.wheelPanningEnabledGet>")
    @WikiReturn("<key:world.canvas.return.flag>")
    public boolean wheelPanningEnabled() {
        return wheelPanningEnabled;
    }

    /**
     * Включает или выключает pan колесом/тачпадом без {@code Ctrl}.
     *
     * <p>{@code Shift + колесо} превращает вертикальный scroll в горизонтальный.</p>
     *
     * @param wheelPanningEnabled {@code true}, чтобы scroll двигал viewport
     * @return этот canvas для fluent-настройки
     */
    @XmlAttribute(value = "wheelPanningEnabled", category = "Behavior", defaultValue = "true", description = "Whether wheel gestures pan the viewport.")
        @AiDoc(summary = "Enables or disables mouse wheel scrolling to pan the camera.")
    @WikiMethod(summary = "<key:world.canvas.method.wheelPanningEnabledSet>")
    @WikiReturn("<key:world.canvas.return.this>")
    public WorldCanvas wheelPanningEnabled(
            @WikiParameter(description = "<key:world.canvas.param.wheelPanningEnabled>")
            boolean wheelPanningEnabled
    ) {
        this.wheelPanningEnabled = wheelPanningEnabled;
        return this;
    }

    /**
     * Проверяет, будет ли canvas поглощать scroll, даже если viewport не изменился.
     *
     * @return {@code true}, если scroll внутри hovered canvas отменяется
     */
        @AiDoc(summary = "Returns true if mouse wheel events are consumed when hovered over this canvas.")
    @WikiMethod(summary = "<key:world.canvas.method.consumeWheelWhileHoveredGet>")
    @WikiReturn("<key:world.canvas.return.flag>")
    public boolean consumeWheelWhileHovered() {
        return consumeWheelWhileHovered;
    }

    /**
     * Управляет поглощением scroll-событий, пока курсор находится над canvas'ом.
     *
     * <p>Если включено, родительские scroll-контейнеры не будут прокручиваться,
     * когда пользователь работает с картой/графом.</p>
     *
     * @param consumeWheelWhileHovered {@code true}, чтобы отменять scroll внутри canvas'а
     * @return этот canvas для fluent-настройки
     */
    @XmlAttribute(value = "consumeWheelWhileHovered", category = "Behavior", defaultValue = "true", description = "Whether wheel events are consumed while the canvas is hovered.")
        @AiDoc(summary = "Sets whether mouse wheel events are swallowed when hovered.")
    @WikiMethod(summary = "<key:world.canvas.method.consumeWheelWhileHoveredSet>")
    @WikiReturn("<key:world.canvas.return.this>")
    public WorldCanvas consumeWheelWhileHovered(
            @WikiParameter(description = "<key:world.canvas.param.consumeWheelWhileHovered>")
            boolean consumeWheelWhileHovered
    ) {
        this.consumeWheelWhileHovered = consumeWheelWhileHovered;
        return this;
    }

    /**
     * Возвращает множитель перемещения viewport'а на один шаг scroll.
     *
     * @return шаг pan в screen-space пикселях
     */
        @AiDoc(summary = "Returns the distance panned per wheel scroll click.")
    @WikiMethod(summary = "<key:world.canvas.method.wheelPanStepGet>")
    @WikiReturn("<key:world.canvas.return.step>")
    public float wheelPanStep() {
        return wheelPanStep;
    }

    /**
     * Задаёт множитель перемещения viewport'а на один шаг scroll.
     *
     * @param wheelPanStep шаг pan в screen-space пикселях, отрицательные/NaN значения нормализуются
     * @return этот canvas для fluent-настройки
     */
    @XmlAttribute(value = "wheelPanStep", category = "Behavior", defaultValue = "32", description = "Viewport pan distance applied per wheel delta unit.")
        @AiDoc(summary = "Sets the distance panned per wheel scroll click.")
    @WikiMethod(summary = "<key:world.canvas.method.wheelPanStepSet>")
    @WikiReturn("<key:world.canvas.return.this>")
    public WorldCanvas wheelPanStep(
            @WikiParameter(description = "<key:world.canvas.param.wheelPanStep>")
            float wheelPanStep
    ) {
        this.wheelPanStep = Float.isFinite(wheelPanStep) ? Math.max(0.0f, wheelPanStep) : 32.0f;
        return this;
    }

    /**
     * Возвращает multiplicative factor для zoom-шага.
     *
     * @return множитель зума на один шаг колеса
     */
        @AiDoc(summary = "Returns the zoom multiplier applied per wheel tick.")
    @WikiMethod(summary = "<key:world.canvas.method.zoomStepGet>")
    @WikiReturn("<key:world.canvas.return.step>")
    public float zoomStep() {
        return zoomStep;
    }

    /**
     * Задаёт multiplicative factor для {@code Ctrl + колесо}.
     *
     * <p>Например, {@code 1.1f} означает примерно +10%/-9% за один шаг.</p>
     *
     * @param zoomStep положительный множитель зума
     * @return этот canvas для fluent-настройки
     */
    @XmlAttribute(value = "zoomStep", category = "Behavior", defaultValue = "1.1", description = "Multiplicative zoom factor applied per wheel delta unit.")
        @AiDoc(summary = "Sets the zoom multiplier applied per wheel tick.")
    @WikiMethod(summary = "<key:world.canvas.method.zoomStepSet>")
    @WikiReturn("<key:world.canvas.return.this>")
    public WorldCanvas zoomStep(
            @WikiParameter(description = "<key:world.canvas.param.zoomStep>")
            float zoomStep
    ) {
        this.zoomStep = Float.isFinite(zoomStep) && zoomStep > 0.0f ? zoomStep : 1.1f;
        return this;
    }

    /**
     * Возвращает кнопку мыши, которая начинает drag-pan.
     *
     * @return текущая кнопка pan
     */
        @AiDoc(summary = "Returns the mouse pointer button used to drag and pan.")
    @WikiMethod(summary = "<key:world.canvas.method.panButtonGet>")
    @WikiReturn("<key:world.canvas.return.panButton>")
    public PointerButton panButton() {
        return panButton;
    }

    /**
     * Задаёт кнопку мыши для drag-pan.
     *
     * @param panButton кнопка pan; {@code null} возвращает {@link PointerButton#PRIMARY}
     * @return этот canvas для fluent-настройки
     */
    @XmlAttribute(value = "panButton", category = "Behavior", defaultValue = "primary", description = "Pointer button that starts drag panning.")
        @AiDoc(summary = "Sets the mouse pointer button used to drag and pan.")
    @WikiMethod(summary = "<key:world.canvas.method.panButtonSet>")
    @WikiReturn("<key:world.canvas.return.this>")
    public WorldCanvas panButton(
            @WikiParameter(description = "<key:world.canvas.param.panButton>")
            PointerButton panButton
    ) {
        this.panButton = panButton == null ? PointerButton.PRIMARY : panButton;
        return this;
    }

    /**
     * Возвращает intrinsic-ширину canvas'а, используемую при {@code AUTO} layout-size.
     *
     * @return preferred ширина в screen-space пикселях
     */
        @AiDoc(summary = "Returns the preferred layout width of the canvas.")
    @WikiMethod(summary = "<key:world.canvas.method.preferredWidthGet>")
    @WikiReturn("<key:world.canvas.return.dimension>")
    public float preferredWidth() {
        return preferredWidth;
    }

    /**
     * Задаёт intrinsic-ширину canvas'а без записи явного размера в layout-style.
     *
     * @param preferredWidth preferred ширина; неположительные/NaN значения сбрасываются к default
     * @return этот canvas для fluent-настройки
     */
    @XmlAttribute(value = "preferredWidth", category = "Layout", defaultValue = "320", description = "Intrinsic canvas width before layout constraints are applied.")
        @AiDoc(summary = "Sets the preferred layout width of the canvas.")
    @WikiMethod(summary = "<key:world.canvas.method.preferredWidthSet>")
    @WikiReturn("<key:world.canvas.return.this>")
    public WorldCanvas preferredWidth(
            @WikiParameter(description = "<key:world.canvas.param.preferredWidth>")
            float preferredWidth
    ) {
        float normalized = positiveOr(preferredWidth, DEFAULT_PREFERRED_WIDTH);
        if (this.preferredWidth == normalized) return this;
        this.preferredWidth = normalized;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    /**
     * Возвращает intrinsic-высоту canvas'а, используемую при {@code AUTO} layout-size.
     *
     * @return preferred высота в screen-space пикселях
     */
        @AiDoc(summary = "Returns the preferred layout height of the canvas.")
    @WikiMethod(summary = "<key:world.canvas.method.preferredHeightGet>")
    @WikiReturn("<key:world.canvas.return.dimension>")
    public float preferredHeight() {
        return preferredHeight;
    }

    /**
     * Задаёт intrinsic-высоту canvas'а без записи явного размера в layout-style.
     *
     * @param preferredHeight preferred высота; неположительные/NaN значения сбрасываются к default
     * @return этот canvas для fluent-настройки
     */
    @XmlAttribute(value = "preferredHeight", category = "Layout", defaultValue = "240", description = "Intrinsic canvas height before layout constraints are applied.")
        @AiDoc(summary = "Sets the preferred layout height of the canvas.")
    @WikiMethod(summary = "<key:world.canvas.method.preferredHeightSet>")
    @WikiReturn("<key:world.canvas.return.this>")
    public WorldCanvas preferredHeight(
            @WikiParameter(description = "<key:world.canvas.param.preferredHeight>")
            float preferredHeight
    ) {
        float normalized = positiveOr(preferredHeight, DEFAULT_PREFERRED_HEIGHT);
        if (this.preferredHeight == normalized) return this;
        this.preferredHeight = normalized;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    /**
     * Задаёт intrinsic-размер canvas'а для авто-компоновки.
     *
     * @param width preferred ширина
     * @param height preferred высота
     * @return этот canvas для fluent-настройки
     */
        @AiDoc(summary = "Sets the preferred width and height of the canvas simultaneously.")
    @WikiMethod(summary = "<key:world.canvas.method.preferredSize>")
    @WikiReturn("<key:world.canvas.return.this>")
    public WorldCanvas preferredSize(
            @WikiParameter(description = "<key:world.canvas.param.width>")
            float width,
            @WikiParameter(description = "<key:world.canvas.param.height>")
            float height
    ) {
        return preferredWidth(width).preferredHeight(height);
    }

    /**
     * Переводит X из world-space в root-space.
     *
     * <p>Root-space — координаты верхнего UI-дерева/экрана, то есть уже с
     * учётом позиции самого {@code WorldCanvas}.</p>
     *
     * @param worldX X в координатах мира
     * @return X в root-space
     */
        @AiDoc(summary = "Transforms a world X coordinate to screen root X coordinate space.")
    @WikiMethod(summary = "<key:world.canvas.method.worldToRootX>")
    @WikiReturn("<key:world.canvas.return.coordinate>")
    public float worldToRootX(
            @WikiParameter(description = "<key:world.canvas.param.worldX>")
            float worldX
    ) {
        return layoutBounds().x() + viewport.worldToScreenX(worldX);
    }

    /**
     * Переводит Y из world-space в root-space.
     *
     * @param worldY Y в координатах мира
     * @return Y в root-space
     */
        @AiDoc(summary = "Transforms a world Y coordinate to screen root Y coordinate space.")
    @WikiMethod(summary = "<key:world.canvas.method.worldToRootY>")
    @WikiReturn("<key:world.canvas.return.coordinate>")
    public float worldToRootY(
            @WikiParameter(description = "<key:world.canvas.param.worldY>")
            float worldY
    ) {
        return layoutBounds().y() + viewport.worldToScreenY(worldY);
    }

    /**
     * Переводит X из root-space обратно в world-space.
     *
     * @param rootX X в координатах root UI
     * @return X в координатах мира
     */
        @AiDoc(summary = "Transforms a screen root X coordinate back to world X coordinate space.")
    @WikiMethod(summary = "<key:world.canvas.method.rootToWorldX>")
    @WikiReturn("<key:world.canvas.return.coordinate>")
    public float rootToWorldX(
            @WikiParameter(description = "<key:world.canvas.param.rootX>")
            float rootX
    ) {
        return viewport.screenToWorldX(rootX - layoutBounds().x());
    }

    /**
     * Переводит Y из root-space обратно в world-space.
     *
     * @param rootY Y в координатах root UI
     * @return Y в координатах мира
     */
        @AiDoc(summary = "Transforms a screen root Y coordinate back to world Y coordinate space.")
    @WikiMethod(summary = "<key:world.canvas.method.rootToWorldY>")
    @WikiReturn("<key:world.canvas.return.coordinate>")
    public float rootToWorldY(
            @WikiParameter(description = "<key:world.canvas.param.rootY>")
            float rootY
    ) {
        return viewport.screenToWorldY(rootY - layoutBounds().y());
    }

    /**
     * Переводит X из world-space в local-space canvas'а.
     *
     * <p>Local-space начинается в левом верхнем углу самого canvas'а.</p>
     *
     * @param worldX X в координатах мира
     * @return X внутри canvas'а
     */
        @AiDoc(summary = "Transforms a world X coordinate to widget local X coordinate space.")
    @WikiMethod(summary = "<key:world.canvas.method.worldToLocalX>")
    @WikiReturn("<key:world.canvas.return.coordinate>")
    public float worldToLocalX(
            @WikiParameter(description = "<key:world.canvas.param.worldX>")
            float worldX
    ) {
        return viewport.worldToScreenX(worldX);
    }

    /**
     * Переводит Y из world-space в local-space canvas'а.
     *
     * @param worldY Y в координатах мира
     * @return Y внутри canvas'а
     */
        @AiDoc(summary = "Transforms a world Y coordinate to widget local Y coordinate space.")
    @WikiMethod(summary = "<key:world.canvas.method.worldToLocalY>")
    @WikiReturn("<key:world.canvas.return.coordinate>")
    public float worldToLocalY(
            @WikiParameter(description = "<key:world.canvas.param.worldY>")
            float worldY
    ) {
        return viewport.worldToScreenY(worldY);
    }

    /**
     * Переводит X из local-space canvas'а в world-space.
     *
     * @param localX X внутри canvas'а
     * @return X в координатах мира
     */
        @AiDoc(summary = "Transforms a widget local X coordinate back to world X coordinate space.")
    @WikiMethod(summary = "<key:world.canvas.method.localToWorldX>")
    @WikiReturn("<key:world.canvas.return.coordinate>")
    public float localToWorldX(
            @WikiParameter(description = "<key:world.canvas.param.localX>")
            float localX
    ) {
        return viewport.screenToWorldX(localX);
    }

    /**
     * Переводит Y из local-space canvas'а в world-space.
     *
     * @param localY Y внутри canvas'а
     * @return Y в координатах мира
     */
        @AiDoc(summary = "Transforms a widget local Y coordinate back to world Y coordinate space.")
    @WikiMethod(summary = "<key:world.canvas.method.localToWorldY>")
    @WikiReturn("<key:world.canvas.return.coordinate>")
    public float localToWorldY(
            @WikiParameter(description = "<key:world.canvas.param.localY>")
            float localY
    ) {
        return viewport.screenToWorldY(localY);
    }

    /**
     * Подписывает слушатель на изменение viewport'а.
     *
     * <p>Событие приходит после drag-pan, wheel-pan, zoom, программного
     * изменения viewport'а и clamp'а, если фактические {@code x/y/zoom}
     * изменились.</p>
     *
     * @param listener слушатель события
     * @return подписка, которую можно отменить
     */
    @AiDoc(summary = "Registers a listener invoked whenever the viewport position or zoom changes.")
    @WikiMethod(summary = "<key:world.canvas.method.onViewportChanged>")
    @WikiReturn("<key:world.canvas.return.subscription>")
    public EventSubscription onViewportChanged(
            @WikiParameter(description = "<key:world.canvas.param.listener>")
            EventListener<? super ViewportChangedEvent> listener
    ) {
        return on(ViewportChangedEvent.TYPE, listener);
    }

    @Override
    public List<Widget> children() {
        return childrenView;
    }

    @Override
    public void setUiContextInternal(UIContext uiContext) {
        super.setUiContextInternal(uiContext);
        Object[] raw = children.elements();
        for (int i = 0, size = children.size(); i < size; i++) {
            Widget child = (Widget) raw[i];
            if (child instanceof WidgetBase base) {
                base.setUiContextInternal(uiContext);
            }
        }
    }

    @Override
    public void measure(LayoutContext context) {
        if (visibility() == Visibility.COLLAPSED) {
            setDesiredSize(LayoutSize.ZERO);
            return;
        }
        LayoutContext childContext = new LayoutContext(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY);
        AnchorWidget[] anchors = anchorLayer.snapshot();
        for (AnchorWidget anchor : anchors) {
            Widget child = anchor.widget();
            if (child.visibility() == Visibility.COLLAPSED) continue;
            if (!hasPendingLayout(child) && hasKnownSize(anchor, child)) {
                // Размер известен и контент не просил пересчёт: пропускаем
                // измерение поддерева. Позиция якоря всё равно обновится в
                // arrangeAnchors(), а desired size не зависит от viewport.
                continue;
            }
            child.measure(childContext);
        }
        setDesiredSize(resolveDesiredSize(context, preferredWidth, preferredHeight));
    }

    @Override
    public void arrange(RectView bounds) {
        super.arrange(bounds);
        if (visibility() == Visibility.COLLAPSED) return;
        clampViewport();
        arrangeAnchors();
    }

    @Override
    public void tick(FrameContext frame) {
        if (visibility() != Visibility.VISIBLE) return;
        super.tick(frame);
        AnchorWidget[] anchors = anchorLayer.snapshot();
        for (AnchorWidget anchor : anchors) {
            if (!anchor.arrangedVisible()) continue;
            Widget child = anchor.widget();
            if (child.visibility() == Visibility.VISIBLE) {
                child.tick(frame);
            }
        }
    }

    @Override
    public void render(RenderContext context) {
        if (visibility() != Visibility.VISIBLE || context == null) return;
        if (layoutBounds().width() <= 0.0f || layoutBounds().height() <= 0.0f) return;

        pushOpacity(context);
        boolean pushedClip = false;
        try {
            DrawScope draw = new DrawScope(context, transform(), layoutBounds());
            if (clippingEnabled) {
                draw.pushClip(layoutBounds().x(), layoutBounds().y(), layoutBounds().width(), layoutBounds().height());
                pushedClip = true;
            }

            // Сначала рисуем дешёвые render-only слои: фон, сетку, связи, декоративную геометрию.
            WorldLayer[] layers = worldLayerSnapshot();
            for (WorldLayer layer : layers) {
                layer.render(this, draw);
            }

            // Потом рендерим обычные виджеты-якоря поверх world layers.
            AnchorWidget[] anchors = anchorLayer.snapshot();
            for (AnchorWidget anchor : anchors) {
                if (!anchor.arrangedVisible()) continue;
                renderChildWithInheritedTransform(context, anchor.widget());
            }
        } finally {
            if (pushedClip) {
                context.popClip();
            }
            popOpacity(context);
        }
    }

    @Override
    public void handle(Event event) {
        if (visibility() != Visibility.VISIBLE || !enabled()) return;
        super.handle(event);
        if (event.isCancelled()) return;
        if (event instanceof PointerEvent pointer && pointer.phase() == EventPhase.CAPTURE) return;

        if (event instanceof PointerPressedEvent pointer) {
            handlePointerPressed(pointer, event);
        } else if (event instanceof PointerMovedEvent pointer) {
            handlePointerMoved(pointer);
        } else if (event instanceof PointerReleasedEvent pointer) {
            handlePointerReleased(pointer, event);
        } else if (event instanceof ScrollEvent scroll) {
            handleScroll(scroll, event);
        }
    }

    @Override
    public void dispose() {
        anchorLayer.clear();
        clearWorldLayers();
        super.dispose();
    }

    /**
     * Подключает widget из {@link AnchorLayer} к обычному children-дереву.
     *
     * <p>Метод package-private: его должен вызывать только {@link AnchorLayer},
     * чтобы у якорей и списка детей не расходилось состояние.</p>
     */
    void attachAnchor(AnchorWidget anchor) {
        Widget child = anchor.widget();
        if (children.contains(child)) {
            throw new IllegalArgumentException("Anchor widget is already attached to this WorldCanvas");
        }
        children.add(child);
        if (child instanceof WidgetBase base) {
            base.setParentInternal(this);
            base.setUiContextInternal(uiContext());
        }
    }

    /**
     * Отключает widget из {@link AnchorLayer} от обычного children-дерева.
     */
    void detachAnchor(AnchorWidget anchor) {
        Widget child = anchor.widget();
        if (!children.remove(child)) return;
        if (child instanceof WidgetBase base) {
            base.setParentInternal(null);
            base.setUiContextInternal(null);
        }
    }

    private void handlePointerPressed(PointerPressedEvent pointer, Event sourceEvent) {
        if (!panningEnabled || pointer.button() != panButton) return;
        if (pointer.target() != this) return;

        panDrag = new PanDragState(pointer.pointerId(), pointer.rootX(), pointer.rootY(), viewport.x(), viewport.y());
        UIContext context = uiContext();
        if (context != null) {
            context.capturePointer(pointer.pointerId(), this);
        }
        sourceEvent.cancel();
    }

    private void handlePointerMoved(PointerMovedEvent pointer) {
        if (panDrag == null || panDrag.pointerId != pointer.pointerId()) return;

        float oldX = viewport.x();
        float oldY = viewport.y();
        float oldZoom = viewport.zoom();
        float nextX = panDrag.startViewportX + pointer.rootX() - panDrag.startRootX;
        float nextY = panDrag.startViewportY + pointer.rootY() - panDrag.startRootY;
        if (!viewport.setPosition(nextX, nextY)) return;
        afterViewportChanged(oldX, oldY, oldZoom);
    }

    private void handlePointerReleased(PointerReleasedEvent pointer, Event sourceEvent) {
        if (panDrag == null || panDrag.pointerId != pointer.pointerId()) return;
        UIContext context = uiContext();
        if (context != null) {
            context.releasePointer(pointer.pointerId(), this);
        }
        panDrag = null;
        sourceEvent.cancel();
    }

    private void handleScroll(ScrollEvent scroll, Event sourceEvent) {
        if (scroll.phase() == EventPhase.CAPTURE) return;

        boolean handled = false;
        boolean ctrlWheel = KeyModifiers.has(scroll.modifiers(), KeyModifiers.CONTROL);
        if (ctrlWheel) {
            // Ctrl + колесо — zoom-to-cursor, чтобы точка под курсором визуально оставалась на месте.
            if (zoomEnabled && scroll.deltaY() != 0.0f) {
                float factor = (float) Math.pow(zoomStep, scroll.deltaY());
                handled = zoomAt(scroll.rootX(), scroll.rootY(), factor);
            } else {
                handled = true;
            }
        } else if (wheelPanningEnabled && (scroll.deltaX() != 0.0f || scroll.deltaY() != 0.0f)) {
            float deltaX = scroll.deltaX();
            float deltaY = scroll.deltaY();
            if (KeyModifiers.has(scroll.modifiers(), KeyModifiers.SHIFT) && deltaY != 0.0f) {
                deltaX = deltaY;
                deltaY = 0.0f;
            }

            float oldX = viewport.x();
            float oldY = viewport.y();
            float oldZoom = viewport.zoom();
            if (viewport.panBy(deltaX * wheelPanStep, deltaY * wheelPanStep)) {
                afterViewportChanged(oldX, oldY, oldZoom);
                handled = true;
            }
        }

        if (handled || consumeWheelWhileHovered) {
            sourceEvent.cancel();
        }
    }

    private boolean zoomAt(float rootX, float rootY, float factor) {
        float oldX = viewport.x();
        float oldY = viewport.y();
        float oldZoom = viewport.zoom();

        float localX = rootX - layoutBounds().x();
        float localY = rootY - layoutBounds().y();
        if (!viewport.zoomAt(localX, localY, factor)) return false;
        afterViewportChanged(oldX, oldY, oldZoom);
        return true;
    }

    private void clampViewport() {
        if (viewport.clamp(layoutBounds().width(), layoutBounds().height())) {
            arrangeAnchors();
            invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        }
    }

    private void afterViewportChanged(float oldX, float oldY, float oldZoom) {
        arrangeAnchors();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        dispatch(new ViewportChangedEvent(this, oldX, oldY, oldZoom, viewport.x(), viewport.y(), viewport.zoom()));
    }

    /**
     * Перекладывает anchor-виджеты после layout/viewport changes.
     *
     * <p>Каждый anchor проецируется world→root, затем размещается как обычный
     * widget с учётом pivot, screenSize, zoom-scaling и zoom visibility range.</p>
     */
    private void arrangeAnchors() {
        AnchorWidget[] anchors = anchorLayer.snapshot();
        if (anchors.length == 0) return;

        LayoutContext childContext = new LayoutContext(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY);
        for (AnchorWidget anchor : anchors) {
            Widget child = anchor.widget();
            if (child.visibility() == Visibility.COLLAPSED) {
                anchor.arrangedVisible(false);
                continue;
            }

            if (hasPendingLayout(child) || !hasKnownSize(anchor, child)) {
                // Размер неизвестен или поддерево просило пересчёт: измеряем.
                // Чистые якоря с известным размером пропускаются: их desired
                // size не зависит от viewport, позиция обновится ниже. Двойной
                // measure грязных якорей в layout-кадры затрагивает только
                // грязное меньшинство и дешевле stale-позиций при pan.
                child.measure(childContext);
            }
            float baseWidth = anchor.screenWidth() >= 0.0f ? anchor.screenWidth() : child.desiredSize().width();
            float baseHeight = anchor.screenHeight() >= 0.0f ? anchor.screenHeight() : child.desiredSize().height();
            float width = sanitizeSize(anchor.effectiveWidth(baseWidth, viewport.zoom()));
            float height = sanitizeSize(anchor.effectiveHeight(baseHeight, viewport.zoom()));

            float rootX = worldToRootX(anchor.worldX());
            float rootY = worldToRootY(anchor.worldY());
            float x = rootX - width * anchor.pivotX();
            float y = rootY - height * anchor.pivotY();

            boolean visible = anchor.visibleAtZoom(viewport.zoom())
                    && (!anchor.cullOutsideViewport()
                    || rectsIntersect(x, y, width, height,
                    layoutBounds().x(), layoutBounds().y(), layoutBounds().width(), layoutBounds().height()));
            anchor.arrangedVisible(visible);
            anchor.projectedRoot(rootX, rootY);

            if (visible) {
                child.arrange(new MutableRect(x, y, width, height));
            } else {
                child.arrange(new MutableRect(rootX, rootY, 0.0f, 0.0f));
            }
        }
    }

    private WorldLayer[] worldLayerSnapshot() {
        if (worldLayerSnapshotDirty) {
            worldLayerSnapshot = worldLayers.toArray(new WorldLayer[worldLayers.size()]);
            worldLayerSnapshotDirty = false;
        }
        return worldLayerSnapshot;
    }

    private <T extends Event> T dispatch(T event) {
        UIContext context = uiContext();
        if (context != null && event instanceof RoutableWidgetEvent widgetEvent) {
            context.routedEvents().dispatch(widgetEvent);
        } else {
            emit(event);
        }
        return event;
    }

    private static float sanitizeSize(float value) {
        return Float.isFinite(value) ? Math.max(0.0f, value) : 0.0f;
    }

    /**
     * Проверяет, требует ли ребёнок повторного измерения.
     *
     * <p>Смотрится агрегированный subtree-флаг, а не только собственный флаг
     * виджета: изменение текста во вложенном лейбле маркера тоже должно
     * пересчитать размер. Якорь, ни разу не измеренный (desired size нулевой
     * без явного screenSize) или с висящим layout-флагом, нельзя каллить:
     * его размер ещё неизвестен, а пропуск measure закрепит неверную оценку.</p>
     */
    private static boolean hasPendingLayout(Widget child) {
        if (child == null) return true;
        return InvalidationFlags.has(child.subtreeInvalidationFlags(), InvalidationFlags.LAYOUT);
    }

    private static boolean hasKnownSize(AnchorWidget anchor, Widget child) {
        if (anchor == null) return false;
        if (anchor.screenWidth() >= 0.0f && anchor.screenHeight() >= 0.0f) return true;
        if (child == null) return false;
        float desiredWidth = anchor.screenWidth() >= 0.0f ? anchor.screenWidth() : child.desiredSize().width();
        float desiredHeight = anchor.screenHeight() >= 0.0f ? anchor.screenHeight() : child.desiredSize().height();
        return desiredWidth > 0.0f && desiredHeight > 0.0f;
    }

    private static float positiveOr(float value, float fallback) {
        return Float.isFinite(value) && value > 0.0f ? value : fallback;
    }

    private static boolean rectsIntersect(float ax, float ay, float aw, float ah,
                                          float bx, float by, float bw, float bh) {
        return ax + aw >= bx && bx + bw >= ax && ay + ah >= by && by + bh >= ay;
    }

    private record PanDragState(
            int pointerId,
            float startRootX,
            float startRootY,
            float startViewportX,
            float startViewportY
    ) {
    }
}
