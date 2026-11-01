package dev.sixik.unigui.widgets.graph.node;

import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiSection;

/**
 * Форма маркера порта. Вместе с цветом {@link PortType} отличает потоки выполнения (exec) от данных.
 */
@WikiPage(title = "PortShape", slug = "port-shape", category = "<key:graph.category>", summary = "<key:port.shape.summary>")
@WikiSection(title = "<key:wiki.section.overview.title>", markdown = "<key:port.shape.overview>", order = 10)
public enum PortShape {
    /**
     * Круг: обычные data-порты.
     */
    CIRCLE,
    /**
     * Квадрат: порты потока выполнения.
     */
    SQUARE,
    /**
     * Ромб: массивы, коллекции и прочие составные значения.
     */
    DIAMOND,
    /**
     * Треугольник: делегаты, события.
     */
    TRIANGLE
}
