package dev.sixik.unigui.api.layout.v3;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Ordered result set for one Layout V3 compute pass. */
public final class LayoutOutput {
    private final LayoutNodeId rootId;
    private final Map<LayoutNodeId, LayoutResult> results;

    public LayoutOutput(LayoutNodeId rootId, Map<LayoutNodeId, LayoutResult> results) {
        this.rootId = Objects.requireNonNull(rootId, "rootId");
        this.results = Collections.unmodifiableMap(new LinkedHashMap<>(
                results == null ? Map.of() : results));
    }

    public static Builder builder(LayoutNodeId rootId) {
        return new Builder(rootId);
    }

    public LayoutResult rootResult() {
        return result(rootId);
    }

    public LayoutResult result(LayoutNodeId id) {
        return results.get(id);
    }

    public Map<LayoutNodeId, LayoutResult> results() {
        return results;
    }

    public Collection<LayoutResult> orderedResults() {
        return results.values();
    }

    public static final class Builder {
        private final LayoutNodeId rootId;
        private final LinkedHashMap<LayoutNodeId, LayoutResult> results = new LinkedHashMap<>();

        private Builder(LayoutNodeId rootId) {
            this.rootId = Objects.requireNonNull(rootId, "rootId");
        }

        public Builder add(LayoutResult result) {
            if (result != null) {
                results.put(result.id(), result);
            }
            return this;
        }

        public LayoutOutput build() {
            return new LayoutOutput(rootId, results);
        }
    }
}
