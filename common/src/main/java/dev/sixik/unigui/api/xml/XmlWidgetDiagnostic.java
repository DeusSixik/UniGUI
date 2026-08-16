package dev.sixik.unigui.api.xml;

/**
 * Диагностика, полученная при загрузке или валидации XML widget tree.
 *
 * <p>Line/column могут отсутствовать для diagnostics, которые создаются не SAX-парсером,
 * а validator-ом descriptor-ов. В таком случае используются значения {@code -1}.</p>
 *
 * @param message человекочитаемое сообщение диагностики
 * @param line номер строки или {@code -1}, если позиция неизвестна
 * @param column номер колонки или {@code -1}, если позиция неизвестна
 */
public record XmlWidgetDiagnostic(String message, int line, int column) {
    /**
     * Создаёт диагностику без source location.
     *
     * @param message текст диагностики
     */
    public XmlWidgetDiagnostic(String message) {
        this(message, -1, -1);
    }

    /**
     * Проверяет, есть ли у диагностики координаты в исходном XML.
     *
     * @return {@code true}, если line и column заданы
     */
    public boolean hasLocation() {
        return line >= 0 && column >= 0;
    }

    /**
     * Возвращает текст диагностики с location suffix, если координаты известны.
     *
     * @return строка для логов и exception message
     */
    @Override
    public String toString() {
        if (!hasLocation()) return message;
        return message + " at line " + line + ", column " + column;
    }
}
