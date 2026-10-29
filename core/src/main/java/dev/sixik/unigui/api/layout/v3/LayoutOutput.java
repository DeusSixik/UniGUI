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
     * Идентификатор корневого узла прохода компоновки.
     */
    private final LayoutNodeId rootId;
    /**
     * Результаты компоновки по идентификаторам узлов.
     */
    private final Map<LayoutNodeId, LayoutResult> results;

    /**
     * Создаёт вывод прохода с корневым идентификатором и картой результатов.
     */
    public LayoutOutput(LayoutNodeId rootId, Map<LayoutNodeId, LayoutResult> results) {
        this.rootId = Objects.requireNonNull(rootId, "rootId");
        this.results = Collections.unmodifiableMap(new LinkedHashMap<>(
                results == null ? Map.of() : results));
    }

    /**
     * Создаёт строитель вывода для заданного корневого узла.
     */
    public static Builder builder(LayoutNodeId rootId) {
        return new Builder(rootId);
    }

    /**
     * Возвращает результат корневого узла.
     */
    public LayoutResult rootResult() {
        return result(rootId);
    }

    /**
     * Возвращает результат узла по его идентификатору или {@code null}.
     */
    public LayoutResult result(LayoutNodeId id) {
        return results.get(id);
    }

    /**
     * Возвращает все результаты по идентификаторам узлов.
     */
    public Map<LayoutNodeId, LayoutResult> results() {
        return results;
    }

    /**
     * Возвращает результаты в порядке обхода дерева.
     */
    public Collection<LayoutResult> orderedResults() {
        return results.values();
    }

    public static final class Builder {
        /**
         * Идентификатор корневого узла собираемого вывода.
         */
        private final LayoutNodeId rootId;
        /**
         * Накапливаемые результаты по идентификаторам узлов.
         */
        private final LinkedHashMap<LayoutNodeId, LayoutResult> results = new LinkedHashMap<>();

        /**
         * Создаёт строитель вывода для заданного корневого узла.
         */
        private Builder(LayoutNodeId rootId) {
            this.rootId = Objects.requireNonNull(rootId, "rootId");
        }

        /**
         * Добавляет результат узла; {@code null} игнорируется.
         */
        public Builder add(LayoutResult result) {
            if (result != null) {
                results.put(result.id(), result);
            }
            return this;
        }

        /**
         * Возвращает уже добавленный результат по идентификатору или {@code null}.
         */
        public LayoutResult peek(LayoutNodeId id) {
            return results.get(id);
        }

        /**
         * Строит неизменяемый вывод из накопленных результатов.
         */
        public LayoutOutput build() {
            return new LayoutOutput(rootId, results);
        }
    }
}
