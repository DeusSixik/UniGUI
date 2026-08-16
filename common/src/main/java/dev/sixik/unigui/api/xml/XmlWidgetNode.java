package dev.sixik.unigui.api.xml;

/**
 * Базовый тип узла исходного XML-документа виджетов.
 *
 * <p>Source document tree состоит из element, text и comment nodes. Этот sealed interface
 * позволяет editor-коду обрабатывать все допустимые варианты через switch по {@link #kind()}
 * или pattern matching без риска получить неизвестный тип узла.</p>
 */
public sealed interface XmlWidgetNode permits XmlWidgetElement, XmlWidgetText, XmlWidgetComment {
    /**
     * Возвращает тип узла для простых switch-ов без {@code instanceof}.
     *
     * @return kind текущего узла
     */
    Kind kind();

    /**
     * Создаёт независимую копию узла.
     *
     * @return копия узла и его subtree, если это элемент
     */
    XmlWidgetNode copy();

    /**
     * Типы узлов source XML tree.
     */
    enum Kind {
        /** XML element node. */
        ELEMENT,
        /** XML text node. */
        TEXT,
        /** XML comment node. */
        COMMENT
    }
}
