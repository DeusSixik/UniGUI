package dev.sixik.unigui.api.xml;

/** Узел XML-комментария, сохраняемый моделью исходного документа. */
public record XmlWidgetComment(String text) implements XmlWidgetNode {
    public XmlWidgetComment {
        text = text == null ? "" : text;
        if (text.contains("--")) {
            throw new IllegalArgumentException("XML comments must not contain '--'");
        }
    }

    @Override
    public Kind kind() {
        return Kind.COMMENT;
    }

    @Override
    public XmlWidgetComment copy() {
        return new XmlWidgetComment(text);
    }
}
