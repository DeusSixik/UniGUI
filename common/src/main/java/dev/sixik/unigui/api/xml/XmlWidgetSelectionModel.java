package dev.sixik.unigui.api.xml;

import java.util.Optional;

/**
 * Минимальная модель выделения в редакторе для узлов исходного XML-документа.
 *
 * <p>Selection хранит только {@link XmlWidgetNodePath}, а не ссылку на node. Это делает модель
 * устойчивой к пересозданию hierarchy snapshot-а и позволяет проверять валидность выбора
 * относительно актуального документа.</p>
 */
public final class XmlWidgetSelectionModel {
    private XmlWidgetNodePath selectedPath;

    /**
     * Проверяет наличие выделения.
     *
     * @return {@code true}, если selected path задан
     */
    public boolean hasSelection() {
        return selectedPath != null;
    }

    /**
     * Возвращает текущий selected path.
     *
     * @return selected path или empty
     */
    public Optional<XmlWidgetNodePath> selectedPath() {
        return Optional.ofNullable(selectedPath);
    }

    /**
     * Сбрасывает выделение.
     *
     * @return эта модель для fluent-настройки
     */
    public XmlWidgetSelectionModel clear() {
        selectedPath = null;
        return this;
    }

    /**
     * Выделяет root element документа.
     *
     * @return эта модель для fluent-настройки
     */
    public XmlWidgetSelectionModel selectRoot() {
        return select(XmlWidgetNodePath.root());
    }

    /**
     * Выделяет узел по path без проверки документа.
     *
     * @param path path узла; не может быть {@code null}
     * @return эта модель для fluent-настройки
     */
    public XmlWidgetSelectionModel select(XmlWidgetNodePath path) {
        if (path == null) throw new IllegalArgumentException("XML widget selection path must not be null");
        selectedPath = path;
        return this;
    }

    /**
     * Выделяет узел только если path существует в документе.
     *
     * @param document актуальный XML document
     * @param path candidate path
     * @return эта модель; выделение очищается, если path не найден
     */
    public XmlWidgetSelectionModel selectIfPresent(XmlWidgetDocument document, XmlWidgetNodePath path) {
        if (path == null || path.resolve(document).isEmpty()) return clear();
        return select(path);
    }

    /**
     * Возвращает выбранный node из документа.
     *
     * @param document актуальный XML document
     * @return selected node или empty, если выделения нет либо path устарел
     */
    public Optional<XmlWidgetNode> selectedNode(XmlWidgetDocument document) {
        return selectedPath == null ? Optional.empty() : selectedPath.resolve(document);
    }

    /**
     * Возвращает выбранный node только если это element.
     *
     * @param document актуальный XML document
     * @return selected element или empty
     */
    public Optional<XmlWidgetElement> selectedElement(XmlWidgetDocument document) {
        return selectedPath == null ? Optional.empty() : selectedPath.resolveElement(document);
    }

    /**
     * Проверяет, что текущее выделение всё ещё указывает на существующий node.
     *
     * @param document актуальный XML document
     * @return {@code true}, если selected path разрешается в node
     */
    public boolean validFor(XmlWidgetDocument document) {
        return selectedNode(document).isPresent();
    }
}
