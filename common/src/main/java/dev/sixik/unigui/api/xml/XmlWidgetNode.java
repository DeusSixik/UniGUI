package dev.sixik.unigui.api.xml;

/** Базовый тип узла исходного XML-документа виджетов. */
public sealed interface XmlWidgetNode permits XmlWidgetElement, XmlWidgetText, XmlWidgetComment {
    Kind kind();

    XmlWidgetNode copy();

    enum Kind {
        ELEMENT,
        TEXT,
        COMMENT
    }
}
