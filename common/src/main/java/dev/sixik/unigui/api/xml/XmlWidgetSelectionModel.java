package dev.sixik.unigui.api.xml;

import java.util.Optional;

/** Минимальная модель выделения в редакторе для узлов исходного XML-документа. */
public final class XmlWidgetSelectionModel {
    private XmlWidgetNodePath selectedPath;

    public boolean hasSelection() {
        return selectedPath != null;
    }

    public Optional<XmlWidgetNodePath> selectedPath() {
        return Optional.ofNullable(selectedPath);
    }

    public XmlWidgetSelectionModel clear() {
        selectedPath = null;
        return this;
    }

    public XmlWidgetSelectionModel selectRoot() {
        return select(XmlWidgetNodePath.root());
    }

    public XmlWidgetSelectionModel select(XmlWidgetNodePath path) {
        if (path == null) throw new IllegalArgumentException("XML widget selection path must not be null");
        selectedPath = path;
        return this;
    }

    public XmlWidgetSelectionModel selectIfPresent(XmlWidgetDocument document, XmlWidgetNodePath path) {
        if (path == null || path.resolve(document).isEmpty()) return clear();
        return select(path);
    }

    public Optional<XmlWidgetNode> selectedNode(XmlWidgetDocument document) {
        return selectedPath == null ? Optional.empty() : selectedPath.resolve(document);
    }

    public Optional<XmlWidgetElement> selectedElement(XmlWidgetDocument document) {
        return selectedPath == null ? Optional.empty() : selectedPath.resolveElement(document);
    }

    public boolean validFor(XmlWidgetDocument document) {
        return selectedNode(document).isPresent();
    }
}
