package dev.sixik.unigui.api.layout.v3;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Упорядоченный набор результатов одного прохода вычисления компоновки V3.
 */
public final class LayoutOutput {
    /**
     * Хранит текстовое или идентификационное значение {@code rootId}.
     */
    private final LayoutNodeId rootId;
    /**
     * Хранит коллекцию {@code Map<LayoutNodeId}, с которой работает этот объект.
     */
    private final Map<LayoutNodeId, LayoutResult> results;

    /**
     * Создаёт экземпляр {@code LayoutOutput} и подготавливает его начальное состояние.
     */
    public LayoutOutput(LayoutNodeId rootId, Map<LayoutNodeId, LayoutResult> results) {
        this.rootId = Objects.requireNonNull(rootId, "rootId");
        this.results = Collections.unmodifiableMap(new LinkedHashMap<>(
                results == null ? Map.of() : results));
    }

    /**
     * Выполняет операцию {@code builder} с переданными параметрами.
     */
    public static Builder builder(LayoutNodeId rootId) {
        return new Builder(rootId);
    }

    /**
     * Возвращает текущее значение или выполняет операцию {@code rootResult}.
     */
    public LayoutResult rootResult() {
        return result(rootId);
    }

    /**
     * Выполняет операцию {@code result} с переданными параметрами.
     */
    public LayoutResult result(LayoutNodeId id) {
        return results.get(id);
    }

    /**
     * Возвращает текущее значение или выполняет операцию {@code results}.
     */
    public Map<LayoutNodeId, LayoutResult> results() {
        return results;
    }

    /**
     * Возвращает текущее значение или выполняет операцию {@code orderedResults}.
     */
    public Collection<LayoutResult> orderedResults() {
        return results.values();
    }

    public static final class Builder {
        /**
         * Хранит текстовое или идентификационное значение {@code rootId}.
         */
        private final LayoutNodeId rootId;
        /**
         * Хранит текстовое или идентификационное значение {@code LinkedHashMap<LayoutNodeId}.
         */
        private final LinkedHashMap<LayoutNodeId, LayoutResult> results = new LinkedHashMap<>();

        /**
         * Выполняет операцию {@code Builder} с переданными параметрами.
         */
        private Builder(LayoutNodeId rootId) {
            this.rootId = Objects.requireNonNull(rootId, "rootId");
        }

        /**
         * Добавляет данные или команду через операцию {@code add}.
         */
        public Builder add(LayoutResult result) {
            if (result != null) {
                results.put(result.id(), result);
            }
            return this;
        }

        /**
         * Выполняет операцию {@code peek} с переданными параметрами.
         */
        public LayoutResult peek(LayoutNodeId id) {
            return results.get(id);
        }

        /**
         * Возвращает текущее значение или выполняет операцию {@code build}.
         */
        public LayoutOutput build() {
            return new LayoutOutput(rootId, results);
        }
    }
}
