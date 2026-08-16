package dev.sixik.unigui.api.xml;

/** Настройки форматирования при сериализации исходных XML-документов виджетов. */
public record XmlWidgetSerializationOptions(boolean xmlDeclaration, String indent, boolean preserveWhitespaceText) {
    public static final XmlWidgetSerializationOptions PRETTY = new XmlWidgetSerializationOptions(true, "    ", false);
    public static final XmlWidgetSerializationOptions COMPACT = new XmlWidgetSerializationOptions(false, "", false);

    public XmlWidgetSerializationOptions {
        indent = indent == null ? "" : indent;
    }

    public XmlWidgetSerializationOptions xmlDeclaration(boolean xmlDeclaration) {
        return new XmlWidgetSerializationOptions(xmlDeclaration, indent, preserveWhitespaceText);
    }

    public XmlWidgetSerializationOptions indent(String indent) {
        return new XmlWidgetSerializationOptions(xmlDeclaration, indent, preserveWhitespaceText);
    }

    public XmlWidgetSerializationOptions preserveWhitespaceText(boolean preserveWhitespaceText) {
        return new XmlWidgetSerializationOptions(xmlDeclaration, indent, preserveWhitespaceText);
    }
}
