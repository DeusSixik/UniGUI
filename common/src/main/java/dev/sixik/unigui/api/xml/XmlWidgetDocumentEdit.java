package dev.sixik.unigui.api.xml;

/** Отменяемая мутация над {@link XmlWidgetDocument}. */
public interface XmlWidgetDocumentEdit {
    String description();

    void apply(XmlWidgetDocument document);

    void undo(XmlWidgetDocument document);
}
