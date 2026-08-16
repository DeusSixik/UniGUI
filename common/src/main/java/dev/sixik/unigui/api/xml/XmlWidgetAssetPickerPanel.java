package dev.sixik.unigui.api.xml;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.layout.LayoutConstraints;
import dev.sixik.unigui.api.text.TextOverflowMode;
import dev.sixik.unigui.widgets.containers.Box;
import dev.sixik.unigui.widgets.containers.HBox;
import dev.sixik.unigui.widgets.containers.VBox;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.widgets.display.TextBlock;
import dev.sixik.unigui.widgets.interaction.Button;

import java.util.List;
import java.util.Locale;

/** Компактный редакторский виджет без привязки к backend-у для выбора XML-ассетов: текстур, шрифтов и shader-ов. */
public final class XmlWidgetAssetPickerPanel extends Box {
    private final VBox content = new VBox();
    private final Label title = new Label("XML Assets");
    private final Label summary = new Label();
    private final HBox kindBar = new HBox();
    private final VBox entries = new VBox();
    private XmlWidgetAssetPickerModel model;
    private int entryLimit = 8;

    public XmlWidgetAssetPickerPanel() {
        this(new XmlWidgetAssetPickerModel(XmlWidgetAssetCatalog.empty(), XmlWidgetAssetKind.TEXTURE));
    }

    public XmlWidgetAssetPickerPanel(XmlWidgetAssetPickerModel model) {
        this.model = model == null
                ? new XmlWidgetAssetPickerModel(XmlWidgetAssetCatalog.empty(), XmlWidgetAssetKind.TEXTURE)
                : model;

        themeEnabled(false);
        backgroundVisible(true);
        borderVisible(true);
        background().set(0.030f, 0.036f, 0.048f, 0.94f);
        borderColor().set(0.30f, 0.36f, 0.46f, 0.82f);
        borderWidth(1.0f);
        radius(4.0f);

        content.spacing(5.0f);
        content.layout(style -> style.margin(8.0f).flexGrow(1.0f).flexShrink(1.0f));
        title.color().set(0.88f, 0.93f, 1.0f, 1.0f);
        title.layout(style -> style.size(LayoutConstraints.AUTO, 18.0f).flexGrow(0).flexShrink(0.0f));
        summary.layout(style -> style.size(LayoutConstraints.AUTO, 18.0f).flexGrow(0).flexShrink(0.0f));
        kindBar.spacing(4.0f);
        kindBar.layout(style -> style.size(LayoutConstraints.AUTO, 20.0f).flexGrow(0).flexShrink(0.0f));
        entries.spacing(3.0f);
        entries.layout(style -> style.flexGrow(1.0f).flexShrink(1.0f));

        content.addChild(title);
        content.addChild(summary);
        content.addChild(kindBar);
        content.addChild(entries);
        addChild(content);
        applyQueuedMutations();
        content.applyQueuedMutations();
        rebuild();
    }

    public XmlWidgetAssetPickerModel model() {
        return model;
    }

    public XmlWidgetAssetPickerPanel model(XmlWidgetAssetPickerModel model) {
        this.model = model == null
                ? new XmlWidgetAssetPickerModel(XmlWidgetAssetCatalog.empty(), XmlWidgetAssetKind.TEXTURE)
                : model;
        rebuild();
        return this;
    }

    public XmlWidgetAssetPickerPanel catalog(XmlWidgetAssetCatalog catalog) {
        model.catalog(catalog);
        rebuild();
        return this;
    }

    public XmlWidgetAssetPickerPanel kind(XmlWidgetAssetKind kind) {
        model.kind(kind);
        rebuild();
        return this;
    }

    public XmlWidgetAssetPickerPanel query(String query) {
        model.query(query);
        rebuildEntries();
        return this;
    }

    public XmlWidgetAssetPickerPanel select(String id) {
        model.select(id);
        rebuildEntries();
        return this;
    }

    public int entryLimit() {
        return entryLimit;
    }

    public XmlWidgetAssetPickerPanel entryLimit(int entryLimit) {
        this.entryLimit = Math.max(1, entryLimit);
        rebuildEntries();
        return this;
    }

    public Label titleLabel() {
        return title;
    }

    public Label summaryLabel() {
        return summary;
    }

    public HBox kindBar() {
        return kindBar;
    }

    public VBox entriesHost() {
        return entries;
    }

    private void rebuild() {
        rebuildKindBar();
        rebuildEntries();
    }

    private void rebuildKindBar() {
        kindBar.clearChildren();
        for (XmlWidgetAssetKind kind : XmlWidgetAssetKind.values()) {
            kindBar.addChild(kindButton(kind));
        }
        kindBar.applyQueuedMutations();
    }

    private void rebuildEntries() {
        List<XmlWidgetAsset> assets = model.visibleAssets();
        summary.text(summaryText(assets.size()));
        summary.color().set(assets.isEmpty() ? 1.0f : 0.72f, assets.isEmpty() ? 0.64f : 0.86f, assets.isEmpty() ? 0.42f : 1.0f, 1.0f);

        entries.clearChildren();
        if (assets.isEmpty()) {
            entries.addChild(emptyRow());
        } else {
            int visible = Math.min(entryLimit, assets.size());
            for (int i = 0; i < visible; i++) {
                entries.addChild(assetRow(assets.get(i)));
            }
            int hidden = assets.size() - visible;
            if (hidden > 0) entries.addChild(messageRow("+ " + hidden + " more assets"));
        }
        entries.applyQueuedMutations();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
    }

    private Button kindButton(XmlWidgetAssetKind kind) {
        Button button = new Button((model.kind() == kind ? "* " : "") + displayKind(kind));
        button.layout(style -> style.size(72.0f, 20.0f).flexGrow(0).flexShrink(0.0f));
        button.onClick(event -> kind(kind));
        return button;
    }

    private Button assetRow(XmlWidgetAsset asset) {
        boolean selected = asset.id().equals(model.selectedId());
        Button row = new Button((selected ? "* " : "") + assetLabel(asset));
        row.layout(style -> style.size(LayoutConstraints.AUTO, 22.0f).flexGrow(0).flexShrink(0.0f));
        row.onClick(event -> select(asset.id()));
        return row;
    }

    private static TextBlock emptyRow() {
        TextBlock row = new TextBlock("No matching assets.");
        row.wrap(false);
        row.overflowMode(TextOverflowMode.CLIP);
        row.color().set(0.90f, 0.72f, 0.48f, 1.0f);
        row.layout(style -> style.size(LayoutConstraints.AUTO, 18.0f).flexGrow(0).flexShrink(0.0f));
        return row;
    }

    private static TextBlock messageRow(String text) {
        TextBlock row = new TextBlock(text);
        row.wrap(false);
        row.overflowMode(TextOverflowMode.CLIP);
        row.color().set(0.72f, 0.78f, 0.86f, 1.0f);
        row.layout(style -> style.size(LayoutConstraints.AUTO, 18.0f).flexGrow(0).flexShrink(0.0f));
        return row;
    }

    private String summaryText(int visibleCount) {
        String kind = displayKind(model.kind()).toLowerCase(Locale.ROOT);
        String query = model.query().isEmpty() ? "" : " matching '" + model.query() + "'";
        String selected = model.selectedAsset().map(asset -> ", selected " + asset.id()).orElse("");
        return visibleCount == 1
                ? "1 " + kind + " asset" + query + selected
                : visibleCount + " " + kind + " assets" + query + selected;
    }

    private static String assetLabel(XmlWidgetAsset asset) {
        String dimensions = asset.hasDimensions() ? " (" + asset.width() + "x" + asset.height() + ")" : "";
        String name = asset.displayName().equals(asset.id()) ? asset.id() : asset.displayName() + " - " + asset.id();
        return name + dimensions;
    }

    private static String displayKind(XmlWidgetAssetKind kind) {
        if (kind == null) return "Texture";
        String lower = kind.name().toLowerCase(Locale.ROOT);
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
}
