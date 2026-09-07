package dev.sixik.isf.runtime;

import dev.sixik.isf.definition.IsfExpression;
import dev.sixik.isf.definition.IsfVisualNode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Сливает визуальные деревья по стабильным id узлов. */
public final class IsfVisualMerger {
    private IsfVisualMerger() {
    }

    public static IsfVisualNode merge(IsfVisualNode base, IsfVisualNode override) {
        if (base == null) return override;
        if (override == null) return base;
        if (!base.id().equals(override.id())) return override;

        Map<String, IsfExpression> properties = new LinkedHashMap<>(base.properties());
        properties.putAll(override.properties());

        Map<String, IsfVisualNode> overrides = new LinkedHashMap<>();
        for (IsfVisualNode child : override.children()) overrides.put(child.id(), child);

        List<IsfVisualNode> children = new ArrayList<>();
        for (IsfVisualNode child : base.children()) {
            IsfVisualNode replacement = overrides.remove(child.id());
            children.add(replacement == null ? child : merge(child, replacement));
        }
        children.addAll(overrides.values());

        return new IsfVisualNode(base.id(), override.widget(), properties, children);
    }
}
