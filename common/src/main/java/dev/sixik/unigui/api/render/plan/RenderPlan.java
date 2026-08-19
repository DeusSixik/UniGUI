package dev.sixik.unigui.api.render.plan;

import dev.sixik.unigui.api.render.DrawScope;

import java.util.ArrayList;
import java.util.List;

/**
 * Immutable список декларативных render primitives, который можно инспектировать до отрисовки.
 *
 * <p>RenderPlan является data-представлением внешнего вида виджета после применения StylePack.
 * В отличие от custom Java renderer'а, его можно показать в редакторе, сериализовать частично и
 * анализировать без запуска произвольного кода.</p>
 */
public final class RenderPlan {
    /** Пустой render plan без primitives. */
    public static final RenderPlan EMPTY = new RenderPlan(List.of());

    private final List<RenderPrimitive> primitives;

    /**
     * Создаёт plan из списка primitives.
     *
     * @param primitives primitives в порядке рендера; {@code null} элементы игнорируются
     */
    public RenderPlan(List<RenderPrimitive> primitives) {
        if (primitives == null || primitives.isEmpty()) {
            this.primitives = List.of();
            return;
        }
        List<RenderPrimitive> normalized = new ArrayList<>(primitives.size());
        for (RenderPrimitive primitive : primitives) {
            if (primitive != null) normalized.add(primitive);
        }
        this.primitives = List.copyOf(normalized);
    }

    /**
     * Создаёт render plan.
     *
     * @param primitives primitives в порядке рендера
     * @return новый plan
     */
    public static RenderPlan of(List<RenderPrimitive> primitives) {
        return new RenderPlan(primitives);
    }

    /** @return immutable список primitives */
    public List<RenderPrimitive> primitives() {
        return primitives;
    }

    /** @return {@code true}, если plan ничего не рисует */
    public boolean empty() {
        return primitives.isEmpty();
    }

    /**
     * Исполняет primitives через draw scope.
     *
     * @param draw draw scope текущего виджета
     */
    public void render(DrawScope draw) {
        for (RenderPrimitive primitive : primitives) {
            primitive.render(draw);
        }
    }
}