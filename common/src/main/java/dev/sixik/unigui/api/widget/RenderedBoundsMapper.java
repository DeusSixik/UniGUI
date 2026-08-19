package dev.sixik.unigui.api.widget;

import dev.sixik.unigui.api.math.RectView;

/**
 * Hook для контейнеров, у которых фактические экранные bounds потомка отличаются от layout-bounds.
 *
 * <p>Layout engine размещает виджет в одном прямоугольнике, но render pipeline может дополнительно
 * применять scroll offset, zoom, canvas transform или clipping. Для editor overlay, selection box,
 * hierarchy preview и hit diagnostics иногда нужны именно визуальные bounds, а не исходные bounds
 * после arrange. Контейнер может реализовать этот интерфейс и вернуть скорректированный прямоугольник
 * для своего потомка.</p>
 *
 * <p>Если метод возвращает {@code null}, вызывающий код должен использовать переданные bounds без
 * изменений. Это позволяет подключать mapper точечно и не ломать обычные контейнеры.</p>
 */
public interface RenderedBoundsMapper {
    /**
     * Преобразует arranged bounds потомка в bounds, которые реально видны на экране.
     *
     * @param child прямой или вложенный потомок, для которого запрошены визуальные bounds
     * @param bounds arranged bounds потомка в координатах root/screen
     * @return визуальные bounds или {@code null}, если преобразование не требуется
     */
    RectView renderedBoundsForChild(Widget child, RectView bounds);
}