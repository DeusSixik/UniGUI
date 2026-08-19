package dev.sixik.unigui.api.render.plan;

import dev.sixik.unigui.api.render.DrawScope;

import java.util.ArrayList;
import java.util.List;

/** Immutable list of declarative render primitives that can be inspected before drawing. */
public final class RenderPlan {
    public static final RenderPlan EMPTY = new RenderPlan(List.of());

    private final List<RenderPrimitive> primitives;

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

    public static RenderPlan of(List<RenderPrimitive> primitives) {
        return new RenderPlan(primitives);
    }

    public List<RenderPrimitive> primitives() {
        return primitives;
    }

    public boolean empty() {
        return primitives.isEmpty();
    }

    public void render(DrawScope draw) {
        for (RenderPrimitive primitive : primitives) {
            primitive.render(draw);
        }
    }
}