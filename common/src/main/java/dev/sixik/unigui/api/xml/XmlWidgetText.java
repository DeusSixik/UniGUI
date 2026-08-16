package dev.sixik.unigui.api.xml;

/**
 * Текстовый узел исходного XML-документа виджетов.
 *
 * <p>Текст сохраняется как часть source tree, чтобы serializer мог восстановить простое
 * содержимое элементов. Whitespace-only text nodes могут быть пропущены при сериализации,
 * если {@link XmlWidgetSerializationOptions#preserveWhitespaceText()} выключен.</p>
 *
 * @param text текст узла; {@code null} нормализуется в пустую строку
 */
public record XmlWidgetText(String text) implements XmlWidgetNode {
    public XmlWidgetText {
        text = text == null ? "" : text;
    }

    /**
     * Возвращает тип узла.
     *
     * @return {@link Kind#TEXT}
     */
    @Override
    public Kind kind() {
        return Kind.TEXT;
    }

    /**
     * Создаёт независимую копию текстового узла.
     *
     * @return копия узла
     */
    @Override
    public XmlWidgetText copy() {
        return new XmlWidgetText(text);
    }
}
