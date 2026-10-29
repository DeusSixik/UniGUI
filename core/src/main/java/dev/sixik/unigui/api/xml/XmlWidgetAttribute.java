package dev.sixik.unigui.api.xml;

/**
 * XML-атрибут исходного документа, сохраняемый моделью документа.
 *
 * <p>Атрибут хранит нормализованное имя, строковое значение и необязательную source location.
 * Позиция нужна редактору для диагностик и подсветки, но при создании документа вручную её можно не задавать.</p>
 *
 * @param name имя XML-атрибута
 * @param value строковое значение; {@code null} нормализуется в пустую строку
 * @param line номер строки или {@code -1}, если позиция неизвестна
 * @param column номер колонки или {@code -1}, если позиция неизвестна
 */
public record XmlWidgetAttribute(String name, String value, int line, int column) {
    /**
     * Создаёт атрибут без source location.
     *
     * @param name имя XML-атрибута
     * @param value строковое значение
     */
    public XmlWidgetAttribute(String name, String value) {
        this(name, value, -1, -1);
    }

    public XmlWidgetAttribute {
        name = normalizeName(name);
        value = value == null ? "" : value;
    }

    /**
     * Проверяет наличие source location.
     *
     * @return {@code true}, если line и column заданы
     */
    public boolean hasLocation() {
        return line >= 0 && column >= 0;
    }

    private static String normalizeName(String name) {
        String normalized = name == null ? "" : name.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("XML attribute name must not be blank");
        }
        return normalized;
    }
}
