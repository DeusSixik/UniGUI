package dev.sixik.unigui.api.layout.v3;

import dev.sixik.unigui.api.layout.LayoutConstraints;
import dev.sixik.unigui.api.layout.LayoutStyle;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/** Immutable Layout V3 tree node. */
public final class LayoutNode {
    private final LayoutNodeId id;
    private final String debugName;
    private final LayoutStyleSnapshot style;
    private final LayoutMeasureFunc measureFunc;
    private final List<LayoutNode> children;

    private LayoutNode(Builder builder) {
        this.id = Objects.requireNonNull(builder.id, "id");
        this.debugName = builder.debugName == null || builder.debugName.isBlank()
                ? id.value()
                : builder.debugName;
        this.style = builder.style == null ? LayoutStyleSnapshot.defaults() : builder.style;
        this.measureFunc = builder.measureFunc == null ? LayoutMeasureFunc.NONE : builder.measureFunc;
        this.children = List.copyOf(builder.children);
    }

    public static Builder builder(String id) {
        return builder(LayoutNodeId.of(id));
    }

    public static Builder builder(LayoutNodeId id) {
        return new Builder(id);
    }

    public LayoutNodeId id() {
        return id;
    }

    public String debugName() {
        return debugName;
    }

    public LayoutStyleSnapshot style() {
        return style;
    }

    public LayoutMeasureFunc measureFunc() {
        return measureFunc;
    }

    public List<LayoutNode> children() {
        return children;
    }

    public boolean leaf() {
        return children.isEmpty();
    }

    public static final class Builder {
        private final LayoutNodeId id;
        private String debugName;
        private LayoutStyleSnapshot style;
        private LayoutMeasureFunc measureFunc;
        private final List<LayoutNode> children = new ArrayList<>();

        private Builder(LayoutNodeId id) {
            this.id = Objects.requireNonNull(id, "id");
        }

        public Builder debugName(String debugName) {
            this.debugName = debugName;
            return this;
        }

        public Builder style(LayoutStyleSnapshot style) {
            this.style = style == null ? LayoutStyleSnapshot.defaults() : style;
            return this;
        }

        public Builder style(LayoutStyle style) {
            return style(LayoutStyleMapper.from(style));
        }

        public Builder legacyConstraints(LayoutConstraints constraints) {
            return style(LayoutStyleMapper.from(constraints));
        }

        public Builder measure(LayoutMeasureFunc measureFunc) {
            this.measureFunc = measureFunc == null ? LayoutMeasureFunc.NONE : measureFunc;
            return this;
        }

        public Builder child(LayoutNode child) {
            if (child != null) {
                children.add(child);
            }
            return this;
        }

        public Builder children(Collection<LayoutNode> children) {
            if (children != null) {
                for (LayoutNode child : children) {
                    child(child);
                }
            }
            return this;
        }

        public LayoutNode build() {
            return new LayoutNode(this);
        }
    }
}
