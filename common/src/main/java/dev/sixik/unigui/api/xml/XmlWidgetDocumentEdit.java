package dev.sixik.unigui.api.xml;

/**
 * Отменяемая мутация над {@link XmlWidgetDocument}.
 *
 * <p>Editor code может хранить такие объекты в undo/redo stack. Реализация должна быть
 * достаточно маленькой и детерминированной: {@link #apply(XmlWidgetDocument)} выполняет изменение,
 * а {@link #undo(XmlWidgetDocument)} возвращает документ к состоянию до apply.</p>
 */
public interface XmlWidgetDocumentEdit {
    /**
     * Возвращает короткое описание команды для меню undo/redo.
     *
     * @return человекочитаемое описание edit-а
     */
    String description();

    /**
     * Применяет изменение к документу.
     *
     * @param document документ, который нужно изменить
     */
    void apply(XmlWidgetDocument document);

    /**
     * Откатывает ранее применённое изменение.
     *
     * @param document документ, к которому был применён этот edit
     */
    void undo(XmlWidgetDocument document);
}
