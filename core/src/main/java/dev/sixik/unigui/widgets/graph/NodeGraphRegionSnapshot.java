package dev.sixik.unigui.widgets.graph;

/**
 * Сериализуемое состояние региона-комментария графа.
 *
 * @param id id региона
 * @param title заголовок
 * @param comment текст комментария
 * @param argb цвет {@code 0xAARRGGBB}
 * @param x X в world-координатах
 * @param y Y в world-координатах
 * @param width ширина
 * @param height высота
 * @param movable можно ли двигать
 * @param resizable можно ли растягивать
 */
public record NodeGraphRegionSnapshot(
        String id,
        String title,
        String comment,
        int argb,
        float x,
        float y,
        float width,
        float height,
        boolean movable,
        boolean resizable
) {
    public NodeGraphRegionSnapshot {
        id = id == null ? "" : id;
        title = title == null ? "" : title;
        comment = comment == null ? "" : comment;
    }
}
