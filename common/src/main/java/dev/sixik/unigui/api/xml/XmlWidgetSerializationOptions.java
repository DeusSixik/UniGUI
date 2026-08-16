package dev.sixik.unigui.api.xml;

/**
 * Настройки форматирования при сериализации исходных XML-документов виджетов.
 *
 * <p>Options immutable: fluent-методы возвращают новый экземпляр с изменённым одним полем.
 * {@link #PRETTY} подходит для файлов редактора, {@link #COMPACT} — для компактного runtime output.</p>
 *
 * @param xmlDeclaration добавлять ли {@code <?xml version="1.0" encoding="UTF-8"?>}
 * @param indent строка отступа; пустая строка отключает переносы строк и pretty-print
 * @param preserveWhitespaceText сохранять ли whitespace-only text nodes
 */
public record XmlWidgetSerializationOptions(boolean xmlDeclaration, String indent, boolean preserveWhitespaceText) {
    /** Pretty-print настройки по умолчанию для файлов редактора. */
    public static final XmlWidgetSerializationOptions PRETTY = new XmlWidgetSerializationOptions(true, "    ", false);
    /** Компактная сериализация без XML declaration и отступов. */
    public static final XmlWidgetSerializationOptions COMPACT = new XmlWidgetSerializationOptions(false, "", false);

    public XmlWidgetSerializationOptions {
        indent = indent == null ? "" : indent;
    }

    /**
     * Возвращает копию с новым флагом XML declaration.
     *
     * @param xmlDeclaration добавлять ли XML declaration
     * @return новый options instance
     */
    public XmlWidgetSerializationOptions xmlDeclaration(boolean xmlDeclaration) {
        return new XmlWidgetSerializationOptions(xmlDeclaration, indent, preserveWhitespaceText);
    }

    /**
     * Возвращает копию с новой строкой отступа.
     *
     * @param indent строка отступа; {@code null} нормализуется в пустую строку
     * @return новый options instance
     */
    public XmlWidgetSerializationOptions indent(String indent) {
        return new XmlWidgetSerializationOptions(xmlDeclaration, indent, preserveWhitespaceText);
    }

    /**
     * Возвращает копию с новым режимом сохранения whitespace text.
     *
     * @param preserveWhitespaceText сохранять ли whitespace-only text nodes
     * @return новый options instance
     */
    public XmlWidgetSerializationOptions preserveWhitespaceText(boolean preserveWhitespaceText) {
        return new XmlWidgetSerializationOptions(xmlDeclaration, indent, preserveWhitespaceText);
    }
}
