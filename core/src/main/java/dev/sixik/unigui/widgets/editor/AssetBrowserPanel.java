package dev.sixik.unigui.widgets.editor;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.event.EventSubscription;
import dev.sixik.unigui.api.layout.LayoutConstraints;
import dev.sixik.unigui.api.xml.XmlAttribute;
import dev.sixik.unigui.api.xml.XmlWidgetAsset;
import dev.sixik.unigui.api.xml.XmlWidgetAssetCatalog;
import dev.sixik.unigui.api.xml.XmlWidgetAssetKind;
import dev.sixik.unigui.api.xml.XmlWidgetName;
import dev.sixik.unigui.widgets.containers.LinearBox;
import dev.sixik.unigui.widgets.containers.VBox;
import dev.sixik.unigui.widgets.core.Orientation;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.widgets.interaction.Button;
import dev.sixik.unigui.widgets.interaction.SearchField;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

/** Editor asset browser for catalog-backed texture/font/shader resource selection. */
@XmlWidgetName("AssetBrowserPanel")
public class AssetBrowserPanel extends LinearBox {
    public static final String STYLE_TYPE = dev.sixik.unigui.api.style.StyleIds.Widget.ASSET_BROWSER_PANEL;

    private final Label titleLabel = new Label("Asset Browser");
    private final SearchField searchField = new SearchField();
    private final Label categoriesHeader = new Label("Categories");
    private final VBox categoryList = new VBox();
    private final Label assetsHeader = new Label("Assets");
    private final VBox assetList = new VBox();
    private final Label previewLabel = new Label("No asset selected");
    private final Button applyButton = new Button("Use Asset");
    private final List<XmlWidgetAsset> visibleAssets = new ArrayList<>();
    private final List<Consumer<AssetSelection>> selectionListeners = new ArrayList<>();
    private final List<Consumer<AssetSelection>> applyListeners = new ArrayList<>();

    private XmlWidgetAssetCatalog catalog = XmlWidgetAssetCatalog.empty();
    private XmlWidgetAssetKind kind = XmlWidgetAssetKind.TEXTURE;
    private String searchText = "";
    private String selectedAssetId = "";
    private String targetAttribute = "";
    private int maxVisibleAssets = 12;
    private boolean syncingSearchField;

    public AssetBrowserPanel() {
        super(Orientation.VERTICAL);
        spacing(5.0f);
        layout(style -> style.size(LayoutConstraints.AUTO, LayoutConstraints.AUTO).flexGrow(1.0f).flexShrink(1.0f));

        titleLabel.layout(style -> style.height(20.0f).flexGrow(0.0f).flexShrink(0.0f));
        searchField.searchChangeDebounceSeconds(0.0f);
        searchField.layout(style -> style.height(20.0f).flexGrow(0.0f).flexShrink(0.0f));
        searchField.onSearchChanged(event -> {
            if (!syncingSearchField) search(event.newQuery());
        });
        categoriesHeader.layout(style -> style.height(18.0f).flexGrow(0.0f).flexShrink(0.0f));
        categoryList.spacing(2.0f);
        categoryList.layout(style -> style.size(LayoutConstraints.AUTO, LayoutConstraints.AUTO).flexGrow(0.0f).flexShrink(0.0f));
        assetsHeader.layout(style -> style.height(18.0f).flexGrow(0.0f).flexShrink(0.0f));
        assetList.spacing(2.0f);
        assetList.layout(style -> style.size(LayoutConstraints.AUTO, LayoutConstraints.AUTO).flexGrow(1.0f).flexShrink(1.0f));
        previewLabel.layout(style -> style.height(36.0f).flexGrow(0.0f).flexShrink(0.0f));
        applyButton.enabled(false);
        applyButton.layout(style -> style.height(22.0f).flexGrow(0.0f).flexShrink(0.0f));
        applyButton.onClick(event -> requestApplySelected());

        addChild(titleLabel);
        addChild(searchField);
        addChild(categoriesHeader);
        addChild(categoryList);
        addChild(assetsHeader);
        addChild(assetList);
        addChild(previewLabel);
        addChild(applyButton);
        applyQueuedMutations();
        rebuild();
    }

    public String title() {
        return titleLabel.text();
    }

    @XmlAttribute(value = "title", category = "Content", defaultValue = "Asset Browser", description = "Panel title shown above asset search.")
    public AssetBrowserPanel title(String title) {
        titleLabel.text(normalize(title, "Asset Browser"));
        return this;
    }

    public XmlWidgetAssetCatalog catalog() {
        return catalog;
    }

    public AssetBrowserPanel catalog(XmlWidgetAssetCatalog catalog) {
        this.catalog = catalog == null ? XmlWidgetAssetCatalog.empty() : catalog;
        if (selectedAsset().isEmpty() && !selectedAssetId.isEmpty()) selectedAssetId = "";
        rebuild();
        return this;
    }

    public XmlWidgetAssetKind kind() {
        return kind;
    }

    @XmlAttribute(value = "kind", category = "Behavior", defaultValue = "TEXTURE", description = "Asset category shown in the browser.")
    public AssetBrowserPanel kind(XmlWidgetAssetKind kind) {
        XmlWidgetAssetKind normalized = kind == null ? XmlWidgetAssetKind.TEXTURE : kind;
        if (this.kind == normalized) return this;
        this.kind = normalized;
        if (selectedAsset().isEmpty()) selectedAssetId = "";
        rebuild();
        return this;
    }

    public String search() {
        return searchText;
    }

    @XmlAttribute(value = "search", category = "Behavior", defaultValue = "", description = "Search text used to filter assets by id, display name or description.")
    public AssetBrowserPanel search(String search) {
        String normalized = search == null ? "" : search.trim();
        if (searchText.equals(normalized)) return this;
        searchText = normalized;
        if (!searchField.text().equals(normalized)) {
            syncingSearchField = true;
            searchField.text(normalized);
            searchField.flushSearchChanged();
            syncingSearchField = false;
        }
        rebuildAssets();
        return this;
    }

    public String selectedAssetId() {
        return selectedAssetId;
    }

    @XmlAttribute(value = "selectedAsset", category = "State", defaultValue = "", description = "Currently selected asset id.")
    public AssetBrowserPanel selectedAsset(String assetId) {
        selectedAssetId = assetId == null ? "" : assetId.trim();
        refreshPreview();
        rebuildAssets();
        return this;
    }

    public Optional<XmlWidgetAsset> selectedAsset() {
        return catalog.find(kind, selectedAssetId);
    }

    public String targetAttribute() {
        return targetAttribute;
    }

    @XmlAttribute(value = "targetAttribute", category = "Behavior", defaultValue = "", description = "Inspector attribute that should receive the selected asset id.")
    public AssetBrowserPanel targetAttribute(String targetAttribute) {
        this.targetAttribute = targetAttribute == null ? "" : targetAttribute.trim();
        return this;
    }

    public int maxVisibleAssets() {
        return maxVisibleAssets;
    }

    @XmlAttribute(value = "maxVisibleAssets", category = "Behavior", defaultValue = "12", description = "Maximum number of asset rows rendered before showing an overflow row.")
    public AssetBrowserPanel maxVisibleAssets(int maxVisibleAssets) {
        int normalized = Math.max(1, maxVisibleAssets);
        if (this.maxVisibleAssets == normalized) return this;
        this.maxVisibleAssets = normalized;
        rebuildAssets();
        return this;
    }

    public List<XmlWidgetAssetKind> categories() {
        return List.of(XmlWidgetAssetKind.values());
    }

    public List<XmlWidgetAsset> visibleAssets() {
        return List.copyOf(visibleAssets);
    }

    public boolean selectAsset(String assetId) {
        String normalized = assetId == null ? "" : assetId.trim();
        Optional<XmlWidgetAsset> asset = catalog.find(kind, normalized);
        if (asset.isEmpty()) return false;
        if (selectedAssetId.equals(asset.get().id())) return true;
        selectedAssetId = asset.get().id();
        rebuildAssets();
        emit(selectionListeners, asset.get(), false);
        return true;
    }

    public boolean requestApplySelected() {
        Optional<XmlWidgetAsset> asset = selectedAsset();
        if (asset.isEmpty()) return false;
        emit(applyListeners, asset.get(), true);
        return true;
    }

    public EventSubscription onAssetSelected(Consumer<AssetSelection> listener) {
        Objects.requireNonNull(listener, "listener");
        selectionListeners.add(listener);
        return () -> selectionListeners.remove(listener);
    }

    public EventSubscription onAssetApplied(Consumer<AssetSelection> listener) {
        Objects.requireNonNull(listener, "listener");
        applyListeners.add(listener);
        return () -> applyListeners.remove(listener);
    }

    public Label titleLabel() {
        return titleLabel;
    }

    public SearchField searchField() {
        return searchField;
    }

    public VBox categoryList() {
        return categoryList;
    }

    public VBox assetList() {
        return assetList;
    }

    public Label previewLabel() {
        return previewLabel;
    }

    public Button applyButton() {
        return applyButton;
    }

    private void rebuild() {
        rebuildCategories();
        rebuildAssets();
    }

    private void rebuildCategories() {
        categoryList.clearChildren();
        categoryList.applyQueuedMutations();
        for (XmlWidgetAssetKind candidate : XmlWidgetAssetKind.values()) {
            Button row = new Button((candidate == kind ? "> " : "") + displayKind(candidate));
            row.layout(style -> style.height(20.0f).flexGrow(0.0f).flexShrink(0.0f));
            row.onClick(event -> kind(candidate));
            categoryList.addChild(row);
        }
        categoryList.applyQueuedMutations();
    }

    private void rebuildAssets() {
        visibleAssets.clear();
        visibleAssets.addAll(catalog.search(kind, searchText));

        assetList.clearChildren();
        assetList.applyQueuedMutations();
        if (visibleAssets.isEmpty()) {
            Label empty = new Label("No matching assets");
            empty.layout(style -> style.height(20.0f).flexGrow(0.0f).flexShrink(0.0f));
            assetList.addChild(empty);
        } else {
            int rendered = Math.min(maxVisibleAssets, visibleAssets.size());
            for (int i = 0; i < rendered; i++) {
                XmlWidgetAsset asset = visibleAssets.get(i);
                Button row = new Button(assetRowLabel(asset));
                row.layout(style -> style.height(22.0f).flexGrow(0.0f).flexShrink(0.0f));
                row.onClick(event -> selectAsset(asset.id()));
                assetList.addChild(row);
            }
            int hidden = visibleAssets.size() - rendered;
            if (hidden > 0) {
                Label overflow = new Label("+ " + hidden + " more assets");
                overflow.layout(style -> style.height(20.0f).flexGrow(0.0f).flexShrink(0.0f));
                assetList.addChild(overflow);
            }
        }
        assetList.applyQueuedMutations();
        refreshPreview();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
    }

    private void refreshPreview() {
        Optional<XmlWidgetAsset> asset = selectedAsset();
        if (asset.isPresent()) {
            previewLabel.text(previewText(asset.get()));
            applyButton.enabled(true);
        } else if (!selectedAssetId.isEmpty()) {
            previewLabel.text("Missing asset: " + selectedAssetId);
            applyButton.enabled(false);
        } else {
            previewLabel.text("No asset selected");
            applyButton.enabled(false);
        }
    }

    private String assetRowLabel(XmlWidgetAsset asset) {
        return (asset.id().equals(selectedAssetId) ? "> " : "") + assetLabel(asset);
    }

    private String previewText(XmlWidgetAsset asset) {
        String target = targetAttribute.isEmpty() ? "" : " -> " + targetAttribute;
        String description = asset.description().isEmpty() ? "" : " - " + asset.description();
        return "Preview " + displayKind(asset.kind()) + target + ": " + assetLabel(asset) + description;
    }

    private static String assetLabel(XmlWidgetAsset asset) {
        String dimensions = asset.hasDimensions() ? " (" + asset.width() + "x" + asset.height() + ")" : "";
        String name = asset.displayName().equals(asset.id()) ? asset.id() : asset.displayName() + " - " + asset.id();
        return name + dimensions;
    }

    private static String displayKind(XmlWidgetAssetKind kind) {
        XmlWidgetAssetKind normalized = kind == null ? XmlWidgetAssetKind.TEXTURE : kind;
        String lower = normalized.name().toLowerCase(Locale.ROOT);
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    private void emit(List<Consumer<AssetSelection>> listeners, XmlWidgetAsset asset, boolean applyRequested) {
        AssetSelection selection = new AssetSelection(this, asset, targetAttribute, applyRequested);
        List<Consumer<AssetSelection>> snapshot = List.copyOf(listeners);
        for (Consumer<AssetSelection> listener : snapshot) {
            listener.accept(selection);
        }
    }

    private static String normalize(String value, String fallback) {
        String normalized = value == null ? "" : value.trim();
        return normalized.isEmpty() ? fallback : normalized;
    }

    public record AssetSelection(AssetBrowserPanel browser,
                                 XmlWidgetAsset asset,
                                 String targetAttribute,
                                 boolean applyRequested) {
        public AssetSelection {
            Objects.requireNonNull(browser, "browser");
            Objects.requireNonNull(asset, "asset");
            targetAttribute = targetAttribute == null ? "" : targetAttribute.trim();
        }
    }
}
