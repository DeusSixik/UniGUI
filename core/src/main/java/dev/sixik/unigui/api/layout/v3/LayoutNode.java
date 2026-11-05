package dev.sixik.unigui.api.layout.v3;

import dev.sixik.unigui.api.layout.LayoutConstraints;
import dev.sixik.unigui.api.layout.LayoutSize;
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
     * Идентификатор узла в дереве компоновки.
     */
    private final LayoutNodeId id;
    /**
     * Отладочное имя узла для логов и дампов дерева.
     */
    private final String debugName;
    /**
     * Снимок стиля узла, неизменяемый на время прохода компоновки.
     */
    private final LayoutStyleSnapshot style;
    /**
     * Функция измерения содержимого листа; NONE, если измерение не нужно.
     */
    private final LayoutMeasureFunc measureFunc;
    /**
     * Baseline контента от cross-start края border box или {@code NaN}.
     */
    private final float baseline;
    /**
     * Минимальный контентный размер для intrinsic-раскладки или {@code null}.
     */
    private final LayoutSize minContentSize;
    /**
     * Максимальный контентный размер для intrinsic-раскладки или {@code null}.
     */
    private final LayoutSize maxContentSize;
    /**
     * Дочерние узлы дерева компоновки.
     */
    private final List<LayoutNode> children;

    /**
     * Создаёт узел из накопленного строителем состояния.
     */
    private LayoutNode(Builder builder) {
        this.id = Objects.requireNonNull(builder.id, "id");
        this.debugName = builder.debugName == null || builder.debugName.isBlank()
                ? id.value()
                : builder.debugName;
        this.style = builder.style == null ? LayoutStyleSnapshot.defaults() : builder.style;
        this.measureFunc = builder.measureFunc == null ? LayoutMeasureFunc.NONE : builder.measureFunc;
        this.baseline = builder.baseline;
        this.minContentSize = builder.minContentSize;
        this.maxContentSize = builder.maxContentSize;
        this.children = List.copyOf(builder.children);
    }

    /**
     * Создаёт строитель узла с заданным строковым идентификатором.
     */
    public static Builder builder(String id) {
        return builder(LayoutNodeId.of(id));
    }

    /**
     * Создаёт строитель узла с заданным идентификатором.
     */
    public static Builder builder(LayoutNodeId id) {
        return new Builder(id);
    }

    /**
     * Возвращает идентификатор узла.
     */
    public LayoutNodeId id() {
        return id;
    }

    /**
     * Возвращает отладочное имя узла.
     */
    public String debugName() {
        return debugName;
    }

    /**
     * Возвращает снимок стиля узла.
     */
    public LayoutStyleSnapshot style() {
        return style;
    }

    /**
     * Возвращает функцию измерения содержимого листа.
     */
    public LayoutMeasureFunc measureFunc() {
        return measureFunc;
    }

    /**
     * Возвращает baseline контента от cross-start края border box или {@code NaN}.
     */
    public float baseline() {
        return baseline;
    }

    /**
     * Возвращает минимальный контентный размер или {@code null}.
     */
    public LayoutSize minContentSize() {
        return minContentSize;
    }

    /**
     * Возвращает максимальный контентный размер или {@code null}.
     */
    public LayoutSize maxContentSize() {
        return maxContentSize;
    }

    /**
     * Возвращает дочерние элементы, связанные с этим объектом.
     */
    public List<LayoutNode> children() {
        return children;
    }

    /**
     * Проверяет, является ли узел листом (без детей).
     */
    public boolean leaf() {
        return children.isEmpty();
    }

    public static final class Builder {
        /**
         * Идентификатор строящегося узла.
         */
        private final LayoutNodeId id;
        /**
         * Отладочное имя; по умолчанию совпадает с идентификатором.
         */
        private String debugName;
        /**
         * Снимок стиля; по умолчанию стиль по умолчанию.
         */
        private LayoutStyleSnapshot style;
        /**
         * Функция измерения листа; по умолчанию {@code NONE}.
         */
        private LayoutMeasureFunc measureFunc;
        /**
         * Baseline контента от cross-start края border box или {@code NaN}.
         */
        private float baseline = Float.NaN;
        /**
         * Минимальный контентный размер для intrinsic-раскладки или {@code null}.
         */
        private LayoutSize minContentSize;
        /**
         * Максимальный контентный размер для intrinsic-раскладки или {@code null}.
         */
        private LayoutSize maxContentSize;
        /**
         * Накапливаемые дочерние узлы.
         */
        private final List<LayoutNode> children = new ObjectArrayList<>();

        /**
         * Создаёт строитель для узла с заданным идентификатором.
         */
        private Builder(LayoutNodeId id) {
            this.id = Objects.requireNonNull(id, "id");
        }

        /**
         * Задаёт отладочное имя узла.
         */
        public Builder debugName(String debugName) {
            this.debugName = debugName;
            return this;
        }

        /**
         * Задаёт снимок стиля узла.
         */
        public Builder style(LayoutStyleSnapshot style) {
            this.style = style == null ? LayoutStyleSnapshot.defaults() : style;
            return this;
        }

        /**
         * Задаёт стиль узла из изменяемого {@code LayoutStyle}.
         */
        public Builder style(LayoutStyle style) {
            return style(LayoutStyleMapper.from(style));
        }

        /**
         * Задаёт стиль узла из ограничений старого формата.
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
         * Задаёт baseline контента от cross-start края border box.
         */
        public Builder baseline(float baseline) {
            this.baseline = baseline;
            return this;
        }

        /**
         * Задаёт минимальный контентный размер для intrinsic-раскладки.
         */
        public Builder minContentSize(LayoutSize minContentSize) {
            this.minContentSize = minContentSize;
            return this;
        }

        /**
         * Задаёт максимальный контентный размер для intrinsic-раскладки.
         */
        public Builder maxContentSize(LayoutSize maxContentSize) {
            this.maxContentSize = maxContentSize;
            return this;
        }

        /**
         * Добавляет дочерний узел; {@code null} игнорируется.
         */
        public Builder child(LayoutNode child) {
            if (child != null) {
                children.add(child);
            }
            return this;
        }

        /**
         * Добавляет несколько дочерних узлов; {@code null}-коллекция игнорируется.
         */
        public Builder children(Collection<LayoutNode> children) {
            if (children != null) {
                for (LayoutNode child : children) {
                    /** Добавляет очередной дочерний узел. */
                    child(child);
                }
            }
            return this;
        }

        /**
         * Строит неизменяемый узел из накопленного состояния.
         */
        public LayoutNode build() {
            return new LayoutNode(this);
        }
    }
}
