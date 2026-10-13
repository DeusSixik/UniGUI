package dev.sixik.unigui.impl.layout;

import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.impl.widget.WidgetBase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Порядок отрисовки соседей по {@code z-index}.
 *
 * <p>Дети рисуются в порядке возрастания z-index со стабильным разрешением равных по порядку дерева;
 * хит-тестирование обходит тот же порядок с конца. Если у всех соседей индекс по умолчанию нулевой,
 * входная коллекция возвращается как есть без аллокации.</p>
 */
public final class ZOrder {
    private ZOrder() {
    }

    /**
     * Возвращает z-index виджета.
     *
     * @param child виджет или {@code null}
     * @return z-index, ноль для сторонних реализаций без доступа к стилю
     */
    public static int of(Widget child) {
        if (child instanceof WidgetBase base) {
            return base.layoutStyle().zIndex();
        }
        return 0;
    }

    /**
     * Упорядочивает детей для отрисовки.
     *
     * @param children соседи в порядке дерева
     * @return порядок отрисовки, возможно сам вход, если все индексы нулевые
     */
    public static List<Widget> paintOrder(List<Widget> children) {
        if (children == null || children.isEmpty() || allZero(children)) {
            return children;
        }
        List<Widget> output = new ArrayList<>(children);
        output.sort((first, second) -> Integer.compare(of(first), of(second)));
        return output;
    }

    /**
     * Упорядочивает массив снапшотов детей для отрисовки.
     *
     * @param children соседи в порядке дерева
     * @return порядок отрисовки, возможно представление входа, если все индексы нулевые
     */
    public static List<Widget> paintOrder(Widget[] children) {
        if (children == null || children.length == 0) {
            return List.of();
        }
        return paintOrder(Arrays.asList(children));
    }

    private static boolean allZero(List<Widget> children) {
        for (Widget child : children) {
            if (of(child) != 0) {
                return false;
            }
        }
        return true;
    }
}
