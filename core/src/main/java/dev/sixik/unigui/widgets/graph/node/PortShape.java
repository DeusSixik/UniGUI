package dev.sixik.unigui.widgets.graph.node;

/**
 * Форма маркера порта. Вместе с цветом {@link PortType} отличает потоки выполнения (exec) от данных.
 */
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
