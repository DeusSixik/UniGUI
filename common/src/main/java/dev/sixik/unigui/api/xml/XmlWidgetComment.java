package dev.sixik.unigui.api.xml;

/**
 * Узел XML-комментария, сохраняемый моделью исходного документа.
 *
 * <p>Комментарий хранится без delimiters {@code <!--} и {@code -->}. Значение не может содержать
 * {@code --}, потому что такая последовательность запрещена внутри XML comments.</p>
 *
 * @param text текст комментария без delimiters; {@code null} нормализуется в пустую строку
 */
public record XmlWidgetComment(String text) implements XmlWidgetNode {
    public XmlWidgetComment {
        text = text == null ? "" : text;
        if (text.contains("--")) {
            throw new IllegalArgumentException("XML comments must not contain '--'");
        }
    }

    /**
     * Возвращает тип узла.
     *
     * @return {@link Kind#COMMENT}
     */
    @Override
    public Kind kind() {
        return Kind.COMMENT;
    }

    /**
     * Создаёт независимую копию комментария.
     *
     * @return копия узла
     */
    @Override
    public XmlWidgetComment copy() {
        return new XmlWidgetComment(text);
    }
}
