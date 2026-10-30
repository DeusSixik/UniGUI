package dev.sixik.unigui.api.xml;

import java.util.List;
import java.util.Optional;

/**
 * Небольшая модель состояния для редакторских выборщиков ассетов поверх каталогов текстур, шрифтов и shader-ов.
 *
 * <p>Модель хранит активную категорию, поисковую строку и выбранный id. Она не зависит от UI-widget-а,
 * поэтому один и тот же state можно использовать в custom panel, popup или property inspector.</p>
 */
public final class XmlWidgetAssetPickerModel {
    private XmlWidgetAssetCatalog catalog;
    private XmlWidgetAssetKind kind;
    private String query = "";
    private String selectedId = "";

    /**
     * Создаёт модель picker-а.
     *
     * @param catalog каталог assets; {@code null} заменяется empty catalog
     * @param kind активная категория; {@code null} заменяется {@link XmlWidgetAssetKind#TEXTURE}
     */
    public XmlWidgetAssetPickerModel(XmlWidgetAssetCatalog catalog, XmlWidgetAssetKind kind) {
        this.catalog = catalog == null ? XmlWidgetAssetCatalog.empty() : catalog;
        this.kind = kind == null ? XmlWidgetAssetKind.TEXTURE : kind;
    }

    /**
     * Возвращает текущий catalog.
     *
     * @return asset catalog
     */
    public XmlWidgetAssetCatalog catalog() {
        return catalog;
    }

    /**
     * Заменяет catalog и сбрасывает selection, если выбранный asset пропал.
     *
     * @param catalog новый catalog; {@code null} заменяется empty catalog
     * @return эта модель для fluent-настройки
     */
    public XmlWidgetAssetPickerModel catalog(XmlWidgetAssetCatalog catalog) {
        this.catalog = catalog == null ? XmlWidgetAssetCatalog.empty() : catalog;
        if (selectedAsset().isEmpty()) selectedId = "";
        return this;
    }

    /**
     * Возвращает активную категорию assets.
     *
     * @return asset kind
     */
    public XmlWidgetAssetKind kind() {
        return kind;
    }

    /**
     * Задаёт активную категорию и сбрасывает selection, если выбранный asset не подходит.
     *
     * @param kind новая категория; {@code null} заменяется texture
     * @return эта модель для fluent-настройки
     */
    public XmlWidgetAssetPickerModel kind(XmlWidgetAssetKind kind) {
        this.kind = kind == null ? XmlWidgetAssetKind.TEXTURE : kind;
        if (selectedAsset().isEmpty()) selectedId = "";
        return this;
    }

    /**
     * Возвращает текущий query.
     *
     * @return строка поиска
     */
    public String query() {
        return query;
    }

    /**
     * Задаёт строку поиска.
     *
     * @param query query для поиска по каталогу
     * @return эта модель для fluent-настройки
     */
    public XmlWidgetAssetPickerModel query(String query) {
        this.query = query == null ? "" : query.trim();
        return this;
    }

    /**
     * Возвращает id выбранного asset-а.
     *
     * @return selected id или пустая строка
     */
    public String selectedId() {
        return selectedId;
    }

    /**
     * Возвращает выбранный asset.
     *
     * @return asset или empty, если selection отсутствует
     */
    public Optional<XmlWidgetAsset> selectedAsset() {
        return catalog.find(kind, selectedId);
    }

    /**
     * Выбирает asset по id, если он существует в текущей категории.
     *
     * @param id candidate id
     * @return эта модель для fluent-настройки
     */
    public XmlWidgetAssetPickerModel select(String id) {
        String normalized = id == null ? "" : id.trim();
        selectedId = catalog.find(kind, normalized).map(XmlWidgetAsset::id).orElse("");
        return this;
    }

    /**
     * Сбрасывает выбор asset-а.
     *
     * @return эта модель для fluent-настройки
     */
    public XmlWidgetAssetPickerModel clearSelection() {
        selectedId = "";
        return this;
    }

    /**
     * Возвращает assets, видимые с текущими kind/query фильтрами.
     *
     * @return список найденных assets
     */
    public List<XmlWidgetAsset> visibleAssets() {
        return catalog.search(kind, query);
    }
}
