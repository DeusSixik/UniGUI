package dev.sixik.unigui.xaml;

/**
 * Позиция в исходном тексте документа (нумерация с 1).
 *
 * <p>Для узлов, созданных программно, а не прочитанных из текста, используется {@link #UNKNOWN}.</p>
 *
 * @param line   номер строки или {@code -1}, если позиция неизвестна
 * @param column номер колонки или {@code -1}, если позиция неизвестна
 */
public record SourceLocation(int line, int column) {
    /** Позиция у программно созданных узлов. */
    public static final SourceLocation UNKNOWN = new SourceLocation(-1, -1);

    /**
     * Показывает, известна ли позиция.
     *
     * @return {@code true}, если строка и колонка заданы
     */
    public boolean known() {
        return line > 0;
    }

    @Override
    public String toString() {
        return known() ? line + ":" + column : "?";
    }
}
