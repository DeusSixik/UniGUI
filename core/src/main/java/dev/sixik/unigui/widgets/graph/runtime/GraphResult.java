package dev.sixik.unigui.widgets.graph.runtime;

import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiSection;

import java.util.Map;

/**
 * Итог одного запуска графа.
 *
 * @param steps   сколько исполняемых нод отработало, включая повторы в циклах
 * @param outputs выходы исполняемых нод: id ноды -> id порта -> последнее опубликованное значение
 */
@WikiPage(title = "GraphResult", slug = "graph-result", category = "<key:graph.category>", summary = "<key:graph.result.summary>")
@WikiSection(title = "<key:wiki.section.overview.title>", markdown = "<key:graph.result.overview>", order = 10)
public record GraphResult(int steps, Map<String, Map<String, Object>> outputs) {
    /**
     * @param nodeId id исполняемой ноды
     * @param portId id её выходного порта
     * @return последнее опубликованное значение или {@code null}
     */
    public Object output(String nodeId, String portId) {
        Map<String, Object> node = outputs.get(nodeId);
        return node == null ? null : node.get(portId);
    }
}
