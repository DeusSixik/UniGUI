package dev.sixik.unigui.api.layout.v3;

import dev.sixik.unigui.api.layout.LayoutConstraints;
import dev.sixik.unigui.api.layout.LayoutStyle;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Неизменяемый узел дерева компоновки V3.
 */
public final class LayoutNode {
    /**
     * Хранит текстовое или идентификационное значение {@code id}.
     */
    private final LayoutNodeId id;
    /**
     * Хранит текстовое или идентификационное значение {@code debugName}.
     */
    private final String debugName;
    /**
     * Хранит состояние или настройку {@code style}, используемую логикой объекта.
     */
    private final LayoutStyleSnapshot style;
    /**
     * Хранит обратный вызов {@code measureFunc}, который подключает внешнюю логику к этому публичному интерфейсу.
     */
    private final LayoutMeasureFunc measureFunc;
    /**
     * Хранит коллекцию {@code children}, с которой работает этот объект.
     */
    private final List<LayoutNode> children;

    /**
     * Создаёт экземпляр {@code LayoutNode} и подготавливает его начальное состояние.
     */
    private LayoutNode(Builder builder) {
        this.id = Objects.requireNonNull(builder.id, "id");
        this.debugName = builder.debugName == null || builder.debugName.isBlank()
                ? id.value()
                : builder.debugName;
        this.style = builder.style == null ? LayoutStyleSnapshot.defaults() : builder.style;
        this.measureFunc = builder.measureFunc == null ? LayoutMeasureFunc.NONE : builder.measureFunc;
        this.children = List.copyOf(builder.children);
    }

    /**
     * Выполняет операцию {@code builder} с переданными параметрами.
     */
    public static Builder builder(String id) {
        return builder(LayoutNodeId.of(id));
    }

    /**
     * Выполняет операцию {@code builder} с переданными параметрами.
     */
    public static Builder builder(LayoutNodeId id) {
        return new Builder(id);
    }

    /**
     * Возвращает текущее значение или выполняет операцию {@code id}.
     */
    public LayoutNodeId id() {
        return id;
    }

    /**
     * Возвращает текущее значение или выполняет операцию {@code debugName}.
     */
    public String debugName() {
        return debugName;
    }

    /**
     * Возвращает текущее значение или выполняет операцию {@code style}.
     */
    public LayoutStyleSnapshot style() {
        return style;
    }

    /**
     * Возвращает текущее значение или выполняет операцию {@code measureFunc}.
     */
    public LayoutMeasureFunc measureFunc() {
        return measureFunc;
    }

    /**
     * Возвращает дочерние элементы, связанные с этим объектом.
     */
    public List<LayoutNode> children() {
        return children;
    }

    /**
     * Возвращает текущее значение или выполняет операцию {@code leaf}.
     */
    public boolean leaf() {
        return children.isEmpty();
    }

    public static final class Builder {
        /**
         * Хранит текстовое или идентификационное значение {@code id}.
         */
        private final LayoutNodeId id;
        /**
         * Хранит текстовое или идентификационное значение {@code debugName}.
         */
        private String debugName;
        /**
         * Хранит состояние или настройку {@code style}, используемую логикой объекта.
         */
        private LayoutStyleSnapshot style;
        /**
         * Хранит обратный вызов {@code measureFunc}, который подключает внешнюю логику к этому публичному интерфейсу.
         */
        private LayoutMeasureFunc measureFunc;
        /**
         * Хранит коллекцию {@code children}, с которой работает этот объект.
         */
        private final List<LayoutNode> children = new ObjectArrayList<>();

        /**
         * Выполняет операцию {@code Builder} с переданными параметрами.
         */
        private Builder(LayoutNodeId id) {
            this.id = Objects.requireNonNull(id, "id");
        }

        /**
         * Выполняет операцию {@code debugName} с переданными параметрами.
         */
        public Builder debugName(String debugName) {
            this.debugName = debugName;
            return this;
        }

        /**
         * Выполняет операцию {@code style} с переданными параметрами.
         */
        public Builder style(LayoutStyleSnapshot style) {
            this.style = style == null ? LayoutStyleSnapshot.defaults() : style;
            return this;
        }

        /**
         * Выполняет операцию {@code style} с переданными параметрами.
         */
        public Builder style(LayoutStyle style) {
            return style(LayoutStyleMapper.from(style));
        }

        /**
         * Выполняет операцию {@code legacyConstraints} с переданными параметрами.
         */
        public Builder legacyConstraints(LayoutConstraints constraints) {
            return style(LayoutStyleMapper.from(constraints));
        }

        /**
         * Измеряет размер элемента или текста в заданном контексте.
         */
        public Builder measure(LayoutMeasureFunc measureFunc) {
            this.measureFunc = measureFunc == null ? LayoutMeasureFunc.NONE : measureFunc;
            return this;
        }

        /**
         * Выполняет операцию {@code child} с переданными параметрами.
         */
        public Builder child(LayoutNode child) {
            if (child != null) {
                children.add(child);
            }
            return this;
        }

        /**
         * Возвращает дочерние элементы, связанные с этим объектом.
         */
        public Builder children(Collection<LayoutNode> children) {
            if (children != null) {
                for (LayoutNode child : children) {
                    /** Выполняет операцию {@code child} с переданными параметрами. */
                    child(child);
                }
            }
            return this;
        }

        /**
         * Возвращает текущее значение или выполняет операцию {@code build}.
         */
        public LayoutNode build() {
            return new LayoutNode(this);
        }
    }
}
