package dev.sixik.unigui.api.xml;

/** Текстовый узел исходного XML-документа виджетов. */
public record XmlWidgetText(String text) implements XmlWidgetNode {
    public XmlWidgetText {
        text = text == null ? "" : text;
    }

    @Override
    public Kind kind() {
        return Kind.TEXT;
    }

    @Override
    public XmlWidgetText copy() {
        return new XmlWidgetText(text);
    }
}
