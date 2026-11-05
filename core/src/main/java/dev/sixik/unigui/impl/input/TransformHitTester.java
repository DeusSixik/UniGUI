package dev.sixik.unigui.impl.input;

import dev.sixik.unigui.api.input.HitTestChildrenOnly;
import dev.sixik.unigui.api.input.HitTestCoordinateMapper;
import dev.sixik.unigui.api.input.HitTestResult;
import dev.sixik.unigui.api.input.HitTester;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.math.Transform;
import dev.sixik.unigui.api.math.TransformGeometry;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.impl.layout.ZOrder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.List;
import java.util.Optional;

public final class TransformHitTester implements HitTester {
    @Override
    public Optional<HitTestResult> hitTest(Widget root, float rootX, float rootY) {
        if (root == null || root.visibility() != Visibility.VISIBLE || !root.enabled()) return Optional.empty();
        return hitTestRecursive(root, rootX, rootY, rootX, rootY);
    }

    @Override
    public Optional<HitTestResult> localPoint(Widget root, Widget target, float rootX, float rootY) {
        if (root == null || target == null) return Optional.empty();
        List<Widget> route = route(root, target);
        if (route.isEmpty()) return Optional.empty();

        float x = rootX;
        float y = rootY;
        for (int index = 0; index < route.size(); index++) {
            Widget widget = route.get(index);
            RectView bounds = widget.layoutBounds();
            float[] untransformed = untransformedInto(widget, x, y);
            float ux = untransformed == null ? x : untransformed[0];
            float uy = untransformed == null ? y : untransformed[1];
            if (widget == target) {
                return Optional.of(new HitTestResult(widget, rootX, rootY,
                        ux - bounds.x(), uy - bounds.y()));
            }
            Widget child = route.get(index + 1);
            float[] mapped = mapPointForChildInto(widget, child, ux, uy);
            x = mapped == null ? ux : mapped[0];
            y = mapped == null ? uy : mapped[1];
        }
        return Optional.empty();
    }

    private Optional<HitTestResult> hitTestRecursive(Widget widget, float rootX, float rootY, float x, float y) {
        if (widget.visibility() != Visibility.VISIBLE || !widget.enabled()) {
            return Optional.empty();
        }

        RectView bounds = widget.layoutBounds();
        float[] untransformed = untransformedInto(widget, x, y);
        float ux = untransformed == null ? x : untransformed[0];
        float uy = untransformed == null ? y : untransformed[1];
        if (!contains(bounds, ux, uy)) {
            return Optional.empty();
        }

        List<Widget> children = ZOrder.paintOrder(widget.children());
        for (int index = children.size() - 1; index >= 0; index--) {
            Widget child = children.get(index);
            float[] mapped = mapPointForChildInto(widget, child, ux, uy);
            float childX = mapped == null ? ux : mapped[0];
            float childY = mapped == null ? uy : mapped[1];
            Optional<HitTestResult> childHit = hitTestRecursive(child, rootX, rootY, childX, childY);
            if (childHit.isPresent()) {
                return childHit;
            }
        }

        // Полноэкранные overlay-host (например MinecraftZLayer) служат только
        // системой координат для своих детей и не блокируют нижние виджеты.
        if (widget instanceof HitTestChildrenOnly) {
            return Optional.empty();
        }

        return Optional.of(new HitTestResult(widget, rootX, rootY, ux - bounds.x(), uy - bounds.y()));
    }

    /**
     * Инвертирует transform виджета без аллокаций в общем случае.
     *
     * <p>У большинства виджетов transform единичный (нет сдвига/масштаба/поворота):
     * инверсия тогда тождественна и метод возвращает {@code null} — вызывающий
     * использует исходные координаты. Объект-пара выделяется только для реально
     * трансформированных виджетов.</p>
     *
     * @param widget виджет с transform и bounds
     * @param x X-координата в пространстве родителя
     * @param y Y-координата в пространстве родителя
     * @return пара {@code [x, y]} в локальных координатах или {@code null}, если равны входным
     */
    private static float[] untransformedInto(Widget widget, float x, float y) {
        Transform transform = widget.transform();
        if (transform == null || transform.isIdentity()) return null;
        TransformGeometry.Point untransformed =
                TransformGeometry.inverseTransformPoint(x, y, widget.layoutBounds(), transform);
        return new float[]{untransformed.x(), untransformed.y()};
    }

    /**
     * Маппит точку в пространство ребёнка без аллокаций в общем случае.
     *
     * @param widget родитель
     * @param child ребёнок, для которого маппится точка
     * @param x X-координата в пространстве родителя
     * @param y Y-координата в пространстве родителя
     * @return пара {@code [x, y]} в пространстве ребёнка или {@code null}, если точка не изменилась
     */
    private static float[] mapPointForChildInto(Widget widget, Widget child, float x, float y) {
        if (widget instanceof HitTestCoordinateMapper mapper) {
            HitTestCoordinateMapper.HitTestPoint mapped = mapper.mapHitTestPointForChild(child, x, y);
            if (mapped != null && Float.isFinite(mapped.x()) && Float.isFinite(mapped.y())) {
                return new float[]{mapped.x(), mapped.y()};
            }
        }
        return null;
    }

    /**
     * Строит маршрут от root к target одним проходом вверх по родителям.
     *
     * <p>Старая версия шла рекурсивно вниз через {@code children()} и собирала
     * новый список на каждом уровне (O(depth) списков на вызов). Проход вверх
     * по {@code parent()} даёт тот же маршрут одним списком: parent/children
     * ссылки поддерживаются консистентно при attach/detach.</p>
     *
     * @param root корень поиска
     * @param target целевой виджет
     * @return маршрут от корня к цели или пустой список, если цель не под корнем
     */
    private static List<Widget> route(Widget root, Widget target) {
        if (root == target) return List.of(root);
        ObjectArrayList<Widget> reversed = new ObjectArrayList<>();
        Widget current = target;
        while (current != null && current != root) {
            reversed.add(current);
            current = current.parent();
        }
        if (current != root) return List.of();
        ObjectArrayList<Widget> fullRoute = new ObjectArrayList<>(reversed.size() + 1);
        fullRoute.add(root);
        for (int i = reversed.size() - 1; i >= 0; i--) {
            fullRoute.add(reversed.get(i));
        }
        return fullRoute;
    }

    private static boolean contains(RectView bounds, float x, float y) {
        float minX = Math.min(bounds.x(), bounds.x() + bounds.width());
        float maxX = Math.max(bounds.x(), bounds.x() + bounds.width());
        float minY = Math.min(bounds.y(), bounds.y() + bounds.height());
        float maxY = Math.max(bounds.y(), bounds.y() + bounds.height());
        return x >= minX && x <= maxX && y >= minY && y <= maxY;
    }
}
