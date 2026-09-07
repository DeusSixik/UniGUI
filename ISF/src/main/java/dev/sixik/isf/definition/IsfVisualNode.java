package dev.sixik.isf.definition;

import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Один узел независимого визуального дерева рецепта. */
public record IsfVisualNode(
        String id,
        ResourceLocation widget,
        Map<String, IsfExpression> properties,
        List<IsfVisualNode> children
) {
    public IsfVisualNode {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("Visual node id cannot be empty");
        if (widget == null) throw new IllegalArgumentException("Visual widget id cannot be null");
        properties = properties == null ? Map.of() : Map.copyOf(new LinkedHashMap<>(properties));
        children = children == null ? List.of() : List.copyOf(children);
    }
}
