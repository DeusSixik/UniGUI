package dev.sixik.unigui.widgets.graph;

import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiSection;

/**
 * Способ выравнивания выделенных нод по общей рамке.
 */
@WikiPage(title = "NodeGraphAlign", slug = "node-graph-align", category = "<key:graph.category>", summary = "<key:node.graph.align.summary>")
@WikiSection(title = "<key:wiki.section.overview.title>", markdown = "<key:node.graph.align.overview>", order = 10)
public enum NodeGraphAlign {
    /**
     * Левые края по самому левому.
     */
    LEFT,
    /**
     * Правые края по самому правому.
     */
    RIGHT,
    /**
     * Верхние края по самому верхнему.
     */
    TOP,
    /**
     * Нижние края по самому нижнему.
     */
    BOTTOM,
    /**
     * Центры по вертикальной оси общей рамки.
     */
    CENTER_X,
    /**
     * Центры по горизонтальной оси общей рамки.
     */
    CENTER_Y
}
