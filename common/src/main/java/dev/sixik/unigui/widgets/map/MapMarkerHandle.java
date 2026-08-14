package dev.sixik.unigui.widgets.map;

import dev.sixik.unigui.api.viewport.ViewportPoint;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.widgets.AnchorWidget;

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
    public String id() {
        return id;
    }

    /**
     * X маркера в координатах карты.
     */
    public float mapX() {
        return anchor == null ? 0.0f : anchor.worldX();
    }

    /**
     * Y маркера в координатах карты.
     */
    public float mapY() {
        return anchor == null ? 0.0f : anchor.worldY();
    }

    /**
     * Виджет, который рендерится как маркер.
     */
    public T widget() {
        return widget;
    }

    /**
     * Низкоуровневый anchor маркера.
     */
    public AnchorWidget anchor() {
        return anchor;
    }

    /**
     * Пользовательский приоритет для будущей сортировки/кластеризации.
     */
    public int priority() {
        return priority;
    }

    /**
     * Перемещает маркер в координатах карты.
     */
    public MapMarkerHandle<T> position(float mapX, float mapY) {
        if (anchor != null) {
            anchor.worldPosition(mapX, mapY);
        }
        return this;
    }

    /**
     * Перемещает маркер во внешних world координатах через {@link MapCanvas#projection()}.
     */
    public MapMarkerHandle<T> projectedPosition(float worldX, float worldY) {
        if (owner == null) return this;
        ViewportPoint point = owner.projection().project(worldX, worldY);
        return position(point.x(), point.y());
    }

    /**
     * Задает постоянный экранный размер маркера.
     */
    public MapMarkerHandle<T> screenSize(float width, float height) {
        if (anchor != null) {
            anchor.screenSize(width, height);
        }
        return this;
    }

    /**
     * Задает pivot маркера относительно его прямоугольника.
     */
    public MapMarkerHandle<T> pivot(float x, float y) {
        if (anchor != null) {
            anchor.pivot(x, y);
        }
        return this;
    }

    /**
     * Показывает маркер только в заданном диапазоне zoom.
     */
    public MapMarkerHandle<T> visibleZoom(float minZoom, float maxZoom) {
        if (anchor != null) {
            anchor.visibleZoomRange(minZoom, maxZoom);
        }
        return this;
    }

    /**
     * Включает или выключает маркер вручную.
     */
    public MapMarkerHandle<T> visible(boolean visible) {
        if (anchor != null) {
            anchor.visible(visible);
        }
        return this;
    }

    /**
     * Включает/выключает автоматическое скрытие маркера вне видимого viewport.
     */
    public MapMarkerHandle<T> cullOutsideViewport(boolean cullOutsideViewport) {
        if (anchor != null) {
            anchor.cullOutsideViewport(cullOutsideViewport);
        }
        return this;
    }

    /**
     * Задает пользовательский приоритет маркера.
     */
    public MapMarkerHandle<T> priority(int priority) {
        this.priority = priority;
        return this;
    }
}
