package dev.sixik.unigui.widgets.graph.runtime;

import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiSection;

/**
 * Проблема, найденная при проверке графа.
 *
 * @param severity серьёзность
 * @param itemId   id ноды, к которой относится проблема
 * @param portId   id порта или пустая строка, если проблема относится ко всей ноде
 * @param message  описание для пользователя
 */
@WikiPage(title = "GraphIssue", slug = "graph-issue", category = "<key:graph.runtime.category>", summary = "<key:graph.issue.summary>")
@WikiSection(title = "<key:wiki.section.overview.title>", markdown = "<key:graph.issue.overview>", order = 10)
public record GraphIssue(Severity severity, String itemId, String portId, String message) {
    /**
     * Серьёзность проблемы.
     */
    public enum Severity {
        /**
         * Граф можно исполнить, но результат, скорее всего, не тот.
         */
        WARNING,
        /**
         * Граф исполнить нельзя или он завершится ошибкой.
         */
        ERROR
    }

    public GraphIssue {
        itemId = itemId == null ? "" : itemId;
        portId = portId == null ? "" : portId;
        message = message == null ? "" : message;
    }
}
