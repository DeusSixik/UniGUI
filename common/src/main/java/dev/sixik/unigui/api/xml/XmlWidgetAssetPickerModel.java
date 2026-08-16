package dev.sixik.unigui.api.xml;

import java.util.List;
import java.util.Optional;

/** Небольшая модель состояния для редакторских выборщиков ассетов поверх каталогов текстур, шрифтов и shader-ов. */
public final class XmlWidgetAssetPickerModel {
    private XmlWidgetAssetCatalog catalog;
    private XmlWidgetAssetKind kind;
    private String query = "";
    private String selectedId = "";

    public XmlWidgetAssetPickerModel(XmlWidgetAssetCatalog catalog, XmlWidgetAssetKind kind) {
        this.catalog = catalog == null ? XmlWidgetAssetCatalog.empty() : catalog;
        this.kind = kind == null ? XmlWidgetAssetKind.TEXTURE : kind;
    }

    public XmlWidgetAssetCatalog catalog() {
        return catalog;
    }

    public XmlWidgetAssetPickerModel catalog(XmlWidgetAssetCatalog catalog) {
        this.catalog = catalog == null ? XmlWidgetAssetCatalog.empty() : catalog;
        if (selectedAsset().isEmpty()) selectedId = "";
        return this;
    }

    public XmlWidgetAssetKind kind() {
        return kind;
    }

    public XmlWidgetAssetPickerModel kind(XmlWidgetAssetKind kind) {
        this.kind = kind == null ? XmlWidgetAssetKind.TEXTURE : kind;
        if (selectedAsset().isEmpty()) selectedId = "";
        return this;
    }

    public String query() {
        return query;
    }

    public XmlWidgetAssetPickerModel query(String query) {
        this.query = query == null ? "" : query.trim();
        return this;
    }

    public String selectedId() {
        return selectedId;
    }

    public Optional<XmlWidgetAsset> selectedAsset() {
        return catalog.find(kind, selectedId);
    }

    public XmlWidgetAssetPickerModel select(String id) {
        String normalized = id == null ? "" : id.trim();
        selectedId = catalog.find(kind, normalized).map(XmlWidgetAsset::id).orElse("");
        return this;
    }

    public XmlWidgetAssetPickerModel clearSelection() {
        selectedId = "";
        return this;
    }

    public List<XmlWidgetAsset> visibleAssets() {
        return catalog.search(kind, query);
    }
}
