package dev.sixik.unigui.widgets.map;


import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiLink;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;

import dev.sixik.unigui.api.viewport.ViewportPoint;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.widgets.world.AnchorWidget;
import dev.sixik.unigui.widgets.interaction.Button;

/**
 * Ручка управления маркером, добавленным в {@link MapCanvas}.
 *
 * <p>{@code MapCanvas} не навязывает внешний вид маркеров: маркером может быть
 * любой {@link Widget}. Этот handle хранит связь виджета с {@link AnchorWidget}
 * и дает fluent API для позиции, размера, pivot, видимости и будущей сортировки.</p>
 *
 * <pre>{@code
 * MapMarkerHandle<Button> handle = map.addProjectedMarkerWidget("shop", worldX, worldZ, button)
 *         .screenSize(96.0f, 24.0f)
 *         .pivot(0.5f, 0.5f)
 *         .visibleZoom(0.35f, Float.POSITIVE_INFINITY);
 * }</pre>
 *
 * @param <T> тип UI-виджета, который используется как маркер
 */
@AiDoc(summary = "Handle for fluent configuration of marker positioning, screen sizing, zoom scaling, pivot, and visibility on MapCanvas.")
@WikiPage(
        title = "MapMarkerHandle",
        slug = "map-marker-handle",
        category = "<key:map.category>",
        summary = "<key:map.marker.handle.summary>"
)
@WikiSection(
        title = "<key:wiki.section.overview.title>",
        markdown = "<key:map.marker.handle.section.overview.body>",
        order = 10
)
@WikiExample(
        title = "<key:map.marker.handle.example.basic.title>",
        code = "MapMarkerHandle<MapMarker> handle = map.addMarker(\"home\", 128.0f, 256.0f, \"Base\")\n    .screenSize(32.0f, 32.0f)\n    .pivot(0.5f, 1.0f);"
)
@WikiLink(
        label = "MapCanvas",
        target = "page:map-canvas",
        description = "<key:map.canvas.summary>"
)
@WikiLink(
        label = "MapMarker",
        target = "page:map-marker",
        description = "<key:map.marker.summary>"
)
@WikiLink(
        label = "AnchorWidget",
        target = "page:anchor-widget",
        description = "<key:anchor.widget.summary>"
)
public final class MapMarkerHandle<T extends Widget> {
    private final MapCanvas owner;
    private final String id;
    private final T widget;
    private final AnchorWidget anchor;
    private int priority;

    MapMarkerHandle(MapCanvas owner, AnchorWidget anchor, T widget) {
        this.owner = owner;
        this.anchor = anchor;
        this.widget = widget;
        this.id = anchor == null ? "" : anchor.id();
    }

    /**
     * Id маркера внутри карты.
     */
        @AiDoc(summary = "Returns the unique string identifier of this marker within the map.")
    @WikiMethod(summary = "<key:map.marker.handle.method.id>")
    @WikiReturn("<key:map.marker.handle.return.id>")
    public String id() {
        return id;
    }

    /**
     * X маркера в координатах карты.
     */
        @AiDoc(summary = "Returns the X coordinate of the marker in map coordinate space.")
    @WikiMethod(summary = "<key:map.marker.handle.method.mapX>")
    @WikiReturn("<key:map.marker.handle.return.coordinate>")
    public float mapX() {
        return anchor == null ? 0.0f : anchor.worldX();
    }

    /**
     * Y маркера в координатах карты.
     */
        @AiDoc(summary = "Returns the Y coordinate of the marker in map coordinate space.")
    @WikiMethod(summary = "<key:map.marker.handle.method.mapY>")
    @WikiReturn("<key:map.marker.handle.return.coordinate>")
    public float mapY() {
        return anchor == null ? 0.0f : anchor.worldY();
    }

    /**
     * Виджет, который рендерится как маркер.
     */
        @AiDoc(summary = "Returns the UI widget rendered as this marker.")
    @WikiMethod(summary = "<key:map.marker.handle.method.widget>")
    @WikiReturn("<key:map.marker.handle.return.widget>")
    public T widget() {
        return widget;
    }

    /**
     * Низкоуровневый anchor маркера.
     */
        @AiDoc(summary = "Returns the underlying low-level AnchorWidget managing this marker.")
    @WikiMethod(summary = "<key:map.marker.handle.method.anchor>")
    @WikiReturn("<key:map.marker.handle.return.anchor>")
    public AnchorWidget anchor() {
        return anchor;
    }

    /**
     * Пользовательский приоритет для будущей сортировки/кластеризации.
     */
        @AiDoc(summary = "Returns the user-defined priority value of this marker.")
    @WikiMethod(summary = "<key:map.marker.handle.method.priorityGet>")
    @WikiReturn("<key:map.marker.handle.return.priority>")
    public int priority() {
        return priority;
    }

    /**
     * Перемещает маркер в координатах карты.
     */
        @AiDoc(summary = "Positions the marker in map coordinate space.")
    @WikiMethod(summary = "<key:map.marker.handle.method.position>")
    @WikiReturn("<key:map.marker.handle.return.this>")
    public MapMarkerHandle<T> position(
            @WikiParameter(description = "<key:map.marker.handle.param.mapX>")
            float mapX,
            @WikiParameter(description = "<key:map.marker.handle.param.mapY>")
            float mapY
    ) {
        if (anchor != null) {
            anchor.worldPosition(mapX, mapY);
        }
        return this;
    }

    /**
     * Перемещает маркер во внешних world координатах через {@link MapCanvas#projection()}.
     */
        @AiDoc(summary = "Positions the marker in external world coordinates projected via MapCanvas projection.")
    @WikiMethod(summary = "<key:map.marker.handle.method.projectedPosition>")
    @WikiReturn("<key:map.marker.handle.return.this>")
    public MapMarkerHandle<T> projectedPosition(
            @WikiParameter(description = "<key:map.marker.handle.param.worldX>")
            float worldX,
            @WikiParameter(description = "<key:map.marker.handle.param.worldY>")
            float worldY
    ) {
        if (owner == null) return this;
        ViewportPoint point = owner.projection().project(worldX, worldY);
        return position(point.x(), point.y());
    }

    /**
     * Задает постоянный экранный размер маркера.
     */
        @AiDoc(summary = "Sets the base fixed screen dimensions of this marker in UI pixels.")
    @WikiMethod(summary = "<key:map.marker.handle.method.screenSize>")
    @WikiReturn("<key:map.marker.handle.return.this>")
    public MapMarkerHandle<T> screenSize(
            @WikiParameter(description = "<key:map.marker.handle.param.width>")
            float width,
            @WikiParameter(description = "<key:map.marker.handle.param.height>")
            float height
    ) {
        if (anchor != null) {
            anchor.screenSize(width, height);
        }
        return this;
    }

    /**
     * Задает минимальный экранный размер маркера после zoom-scaling.
     *
     * <p>При сильном отдалении маркер не сожмётся меньше заданного размера.
     * Отрицательное/NaN значение убирает пол.</p>
     */
        @AiDoc(summary = "Sets minimum screen dimensions to prevent marker over-shrinking at low zoom.")
    @WikiMethod(summary = "<key:map.marker.handle.method.minScreenSize>")
    @WikiReturn("<key:map.marker.handle.return.this>")
    public MapMarkerHandle<T> minScreenSize(
            @WikiParameter(description = "<key:map.marker.handle.param.width>")
            float width,
            @WikiParameter(description = "<key:map.marker.handle.param.height>")
            float height
    ) {
        if (anchor != null) {
            anchor.minScreenSize(width, height);
        }
        return this;
    }

    /**
     * Задает максимальный экранный размер маркера после zoom-scaling.
     *
     * <p>При сильном приближении маркер не раздуется больше заданного размера.
     * Отрицательное/NaN значение убирает потолок.</p>
     */
        @AiDoc(summary = "Sets maximum screen dimensions to prevent marker over-expanding at high zoom.")
    @WikiMethod(summary = "<key:map.marker.handle.method.maxScreenSize>")
    @WikiReturn("<key:map.marker.handle.return.this>")
    public MapMarkerHandle<T> maxScreenSize(
            @WikiParameter(description = "<key:map.marker.handle.param.width>")
            float width,
            @WikiParameter(description = "<key:map.marker.handle.param.height>")
            float height
    ) {
        if (anchor != null) {
            anchor.maxScreenSize(width, height);
        }
        return this;
    }

    /**
     * Включает масштабирование размера маркера от zoom карты.
     *
     * <p>Эффективный размер: {@code baseSize * (zoom / referenceZoom)}, зажатый
     * в [{@link #minScreenSize(float, float)}, {@link #maxScreenSize(float, float)}].
     * По умолчанию выключено — размер фиксирован.</p>
     *
     * @param zoomScaling {@code true}, чтобы размер следовал за zoom
     * @param referenceZoom zoom, при котором размер равен базовому
     */
        @AiDoc(summary = "Configures dynamic scaling of the marker size relative to map zoom.")
    @WikiMethod(summary = "<key:map.marker.handle.method.zoomScaling>")
    @WikiReturn("<key:map.marker.handle.return.this>")
    public MapMarkerHandle<T> zoomScaling(
            @WikiParameter(description = "<key:map.marker.handle.param.zoomScaling>")
            boolean zoomScaling,
            @WikiParameter(description = "<key:map.marker.handle.param.referenceZoom>")
            float referenceZoom
    ) {
        if (anchor != null) {
            anchor.zoomScaling(zoomScaling, referenceZoom);
        }
        return this;
    }

    /**
     * Задает pivot маркера относительно его прямоугольника.
     */
        @AiDoc(summary = "Sets the normalized anchor pivot point (0.0 to 1.0) inside the marker bounds.")
    @WikiMethod(summary = "<key:map.marker.handle.method.pivot>")
    @WikiReturn("<key:map.marker.handle.return.this>")
    public MapMarkerHandle<T> pivot(
            @WikiParameter(description = "<key:map.marker.handle.param.x>")
            float x,
            @WikiParameter(description = "<key:map.marker.handle.param.y>")
            float y
    ) {
        if (anchor != null) {
            anchor.pivot(x, y);
        }
        return this;
    }

    /**
     * Показывает маркер только в заданном диапазоне zoom.
     */
        @AiDoc(summary = "Restricts marker visibility to the specified zoom range.")
    @WikiMethod(summary = "<key:map.marker.handle.method.visibleZoom>")
    @WikiReturn("<key:map.marker.handle.return.this>")
    public MapMarkerHandle<T> visibleZoom(
            @WikiParameter(description = "<key:map.marker.handle.param.minZoom>")
            float minZoom,
            @WikiParameter(description = "<key:map.marker.handle.param.maxZoom>")
            float maxZoom
    ) {
        if (anchor != null) {
            anchor.visibleZoomRange(minZoom, maxZoom);
        }
        return this;
    }

    /**
     * Включает или выключает маркер вручную.
     */
        @AiDoc(summary = "Sets the manual visibility flag of this marker.")
    @WikiMethod(summary = "<key:map.marker.handle.method.visible>")
    @WikiReturn("<key:map.marker.handle.return.this>")
    public MapMarkerHandle<T> visible(
            @WikiParameter(description = "<key:map.marker.handle.param.visible>")
            boolean visible
    ) {
        if (anchor != null) {
            anchor.visible(visible);
        }
        return this;
    }

    /**
     * Включает/выключает автоматическое скрытие маркера вне видимого viewport.
     */
        @AiDoc(summary = "Enables or disables automatic culling when the marker is outside the visible viewport.")
    @WikiMethod(summary = "<key:map.marker.handle.method.cullOutsideViewport>")
    @WikiReturn("<key:map.marker.handle.return.this>")
    public MapMarkerHandle<T> cullOutsideViewport(
            @WikiParameter(description = "<key:map.marker.handle.param.cullOutsideViewport>")
            boolean cullOutsideViewport
    ) {
        if (anchor != null) {
            anchor.cullOutsideViewport(cullOutsideViewport);
        }
        return this;
    }

    /**
     * Задает пользовательский приоритет маркера.
     */
        @AiDoc(summary = "Sets the marker sorting priority value.")
    @WikiMethod(summary = "<key:map.marker.handle.method.prioritySet>")
    @WikiReturn("<key:map.marker.handle.return.this>")
    public MapMarkerHandle<T> priority(
            @WikiParameter(description = "<key:map.marker.handle.param.priority>")
            int priority
    ) {
        this.priority = priority;
        return this;
    }
}
