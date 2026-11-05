package dev.sixik.unigui.widgets.graph.runtime;

import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiSection;

/**
 * Ошибка исполнения графа: нет поведения, цикл данных, превышен лимит шагов или сбой в поведении ноды.
 */
@WikiPage(title = "GraphExecutionException", slug = "graph-execution-exception", category = "<key:graph.runtime.category>", summary = "<key:graph.execution.exception.summary>")
@WikiSection(title = "<key:wiki.section.overview.title>", markdown = "<key:graph.execution.exception.overview>", order = 10)
public class GraphExecutionException extends RuntimeException {
    public GraphExecutionException(String message) {
        super(message);
    }

    public GraphExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
