package dev.sixik.unigui.widgets.editor;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.event.EventSubscription;
import dev.sixik.unigui.api.layout.LayoutConstraints;
import dev.sixik.unigui.api.xml.XmlAttribute;
import dev.sixik.unigui.api.xml.XmlWidgetDescriptor;
import dev.sixik.unigui.api.xml.XmlWidgetDocumentEdit;
import dev.sixik.unigui.api.xml.XmlWidgetDocumentEdits;
import dev.sixik.unigui.api.xml.XmlWidgetElement;
import dev.sixik.unigui.api.xml.XmlWidgetName;
import dev.sixik.unigui.api.xml.XmlWidgetNodePath;
import dev.sixik.unigui.api.xml.XmlWidgetRegistry;
import dev.sixik.unigui.widgets.containers.HBox;
import dev.sixik.unigui.widgets.containers.LinearBox;
import dev.sixik.unigui.widgets.containers.VBox;
import dev.sixik.unigui.widgets.core.Orientation;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.widgets.interaction.Button;
import dev.sixik.unigui.widgets.interaction.SearchField;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

/** Descriptor-backed palette for inserting XML-visible widgets into an editor document. */
@XmlWidgetName("WidgetPalette")
public class WidgetPalette extends LinearBox {
    private static final String ALL_CATEGORIES = "All";

    private final Label titleLabel = new Label("Widget Palette");
    private final SearchField searchField = new SearchField();
    private final HBox actionRow = new HBox();
    private final Button insertButton = new Button("Insert");
    private final Label categoriesHeader = new Label("Categories");
    private final VBox categoryList = new VBox();
    private final Label itemsHeader = new Label("Widgets");
    private final VBox itemList = new VBox();
    private final List<PaletteItem> visibleItems = new ArrayList<>();
    private final List<String> categories = new ArrayList<>();
    private final List<Consumer<PaletteInsertRequest>> insertListeners = new ArrayList<>();
    private final List<Consumer<PaletteSelectionChange>> selectionListeners = new ArrayList<>();

    private XmlWidgetRegistry registry = XmlWidgetRegistry.builtIns();
    private String searchText = "";
    private String selectedCategory = ALL_CATEGORIES;
    private String selectedXmlName = "";
    private boolean includeInternalWidgets;
    private boolean syncingSearchField;

    public WidgetPalette() {
        super(Orientation.VERTICAL);
        spacing(5.0f);
        layout(style -> style.size(LayoutConstraints.AUTO, LayoutConstraints.AUTO).flexGrow(1.0f).flexShrink(1.0f));

        titleLabel.layout(style -> style.height(20.0f).flexGrow(0.0f).flexShrink(0.0f));
        searchField.searchChangeDebounceSeconds(0.0f);
        searchField.layout(style -> style.height(20.0f).flexGrow(0.0f).flexShrink(0.0f));
        searchField.onSearchChanged(event -> {
            if (!syncingSearchField) search(event.newQuery());
        });
        searchField.onSearchSubmitted(event -> requestInsertSelected());

        actionRow.spacing(4.0f);
        actionRow.layout(style -> style.height(24.0f).flexGrow(0.0f).flexShrink(0.0f));
        insertButton.enabled(false);
        insertButton.onClick(event -> requestInsertSelected());
        actionRow.addChild(insertButton);
        actionRow.applyQueuedMutations();

        categoriesHeader.layout(style -> style.height(18.0f).flexGrow(0.0f).flexShrink(0.0f));
        categoryList.spacing(2.0f);
        categoryList.layout(style -> style.size(LayoutConstraints.AUTO, LayoutConstraints.AUTO).flexGrow(0.0f).flexShrink(0.0f));
        itemsHeader.layout(style -> style.height(18.0f).flexGrow(0.0f).flexShrink(0.0f));
        itemList.spacing(2.0f);
        itemList.layout(style -> style.size(LayoutConstraints.AUTO, LayoutConstraints.AUTO).flexGrow(1.0f).flexShrink(1.0f));

        addChild(titleLabel);
        addChild(searchField);
        addChild(actionRow);
        addChild(categoriesHeader);
        addChild(categoryList);
        addChild(itemsHeader);
        addChild(itemList);
        applyQueuedMutations();
        rebuildPalette();
    }

    public String title() {
        return titleLabel.text();
    }

    @XmlAttribute(value = "title", category = "Content", defaultValue = "Widget Palette", description = "Panel title shown above palette search.")
    public WidgetPalette title(String title) {
        titleLabel.text(normalize(title, "Widget Palette"));
        return this;
    }

    public XmlWidgetRegistry registry() {
        return registry;
    }

    public WidgetPalette registry(XmlWidgetRegistry registry) {
        this.registry = registry == null ? XmlWidgetRegistry.builtIns() : registry;
        if (!selectedXmlName.isEmpty()
                && this.registry.descriptor(selectedXmlName).filter(this::allowedForPalette).isEmpty()) {
            selectedXmlName = "";
        }
        rebuildPalette();
        return this;
    }

    public String search() {
        return searchText;
    }

    @XmlAttribute(value = "search", category = "Behavior", defaultValue = "", description = "Search text used to filter palette items by tag, display name or description.")
    public WidgetPalette search(String search) {
        String normalized = search == null ? "" : search.trim();
        if (searchText.equals(normalized)) return this;
        searchText = normalized;
        if (!searchField.text().equals(normalized)) {
            syncingSearchField = true;
            searchField.text(normalized);
            searchField.flushSearchChanged();
            syncingSearchField = false;
        }
        rebuildPalette();
        return this;
    }

    public String selectedCategory() {
        return selectedCategory;
    }

    @XmlAttribute(value = "category", category = "Behavior", defaultValue = "All", description = "Palette category filter, or All to show every category.")
    public WidgetPalette selectedCategory(String category) {
        String normalized = normalize(category, ALL_CATEGORIES);
        if (selectedCategory.equals(normalized)) return this;
        selectedCategory = normalized;
        rebuildPalette();
        return this;
    }

    public String selectedXmlName() {
        return selectedXmlName;
    }

    @XmlAttribute(value = "selectedWidget", category = "State", defaultValue = "", description = "Currently selected XML widget tag from the palette.")
    public WidgetPalette selectedWidget(String xmlName) {
        selectWidget(xmlName);
        return this;
    }

    public boolean includeInternalWidgets() {
        return includeInternalWidgets;
    }

    @XmlAttribute(value = "includeInternalWidgets", category = "Behavior", defaultValue = "false", description = "Whether editor-only/internal widgets are shown in this palette.")
    public WidgetPalette includeInternalWidgets(boolean includeInternalWidgets) {
        if (this.includeInternalWidgets == includeInternalWidgets) return this;
        this.includeInternalWidgets = includeInternalWidgets;
        if (!this.includeInternalWidgets
                && !selectedXmlName.isEmpty()
                && registry.descriptor(selectedXmlName).filter(this::allowedForPalette).isEmpty()) {
            selectedXmlName = "";
        }
        rebuildPalette();
        return this;
    }

    public Optional<XmlWidgetDescriptor> selectedDescriptor() {
        return selectedXmlName.isEmpty() ? Optional.empty() : registry.descriptor(selectedXmlName);
    }

    public List<PaletteItem> visibleItems() {
        return List.copyOf(visibleItems);
    }

    public List<String> categories() {
        return List.copyOf(categories);
    }

    public boolean selectWidget(String xmlName) {
        String normalized = xmlName == null ? "" : xmlName.trim();
        Optional<XmlWidgetDescriptor> descriptor = registry.descriptor(normalized).filter(this::allowedForPalette);
        if (normalized.isEmpty() || descriptor.isEmpty()) return false;
        if (selectedXmlName.equals(normalized)) return true;
        String previous = selectedXmlName;
        selectedXmlName = normalized;
        insertButton.enabled(true);
        rebuildItems();
        emitSelection(previous, selectedXmlName);
        return true;
    }

    public Optional<XmlWidgetElement> createSelectedElement() {
        return selectedDescriptor().map(this::createElement);
    }

    public Optional<XmlWidgetDocumentEdit> createInsertEdit(XmlWidgetNodePath parentPath, int index) {
        return createSelectedElement().map(element -> XmlWidgetDocumentEdits.addChild(
                parentPath == null ? XmlWidgetNodePath.root() : parentPath,
                index,
                element));
    }

    public boolean requestInsertSelected() {
        return requestInsertSelected(XmlWidgetNodePath.root(), Integer.MAX_VALUE);
    }

    public boolean requestInsertSelected(XmlWidgetNodePath parentPath, int index) {
        Optional<XmlWidgetDescriptor> descriptor = selectedDescriptor();
        if (descriptor.isEmpty()) return false;
        PaletteInsertRequest request = new PaletteInsertRequest(
                descriptor.get(),
                createElement(descriptor.get()),
                parentPath == null ? XmlWidgetNodePath.root() : parentPath,
                index);
        List<Consumer<PaletteInsertRequest>> snapshot = List.copyOf(insertListeners);
        for (Consumer<PaletteInsertRequest> listener : snapshot) {
            listener.accept(request);
        }
        return true;
    }

    public boolean requestInsert(String xmlName, XmlWidgetNodePath parentPath, int index) {
        return selectWidget(xmlName) && requestInsertSelected(parentPath, index);
    }

    public EventSubscription onInsertRequested(Consumer<PaletteInsertRequest> listener) {
        Objects.requireNonNull(listener, "listener");
        insertListeners.add(listener);
        return () -> insertListeners.remove(listener);
    }

    public EventSubscription onSelectionChanged(Consumer<PaletteSelectionChange> listener) {
        Objects.requireNonNull(listener, "listener");
        selectionListeners.add(listener);
        return () -> selectionListeners.remove(listener);
    }

    public Label titleLabel() {
        return titleLabel;
    }

    public SearchField searchField() {
        return searchField;
    }

    public Button insertButton() {
        return insertButton;
    }

    public VBox categoryList() {
        return categoryList;
    }

    public VBox itemList() {
        return itemList;
    }

    private XmlWidgetElement createElement(XmlWidgetDescriptor descriptor) {
        return new XmlWidgetElement(descriptor.xmlName());
    }

    private void rebuildPalette() {
        if (!selectedXmlName.isEmpty()
                && registry.descriptor(selectedXmlName).filter(this::allowedForPalette).isEmpty()) {
            selectedXmlName = "";
        }
        rebuildCategories();
        rebuildItems();
        insertButton.enabled(!selectedXmlName.isEmpty());
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
    }

    private void rebuildCategories() {
        Set<String> next = new LinkedHashSet<>();
        next.add(ALL_CATEGORIES);
        registry.descriptors().stream()
                .filter(this::allowedForPalette)
                .map(XmlWidgetDescriptor::category)
                .filter(category -> category != null && !category.isBlank())
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .forEach(next::add);
        categories.clear();
        categories.addAll(next);
        if (!selectedCategory.equals(ALL_CATEGORIES) && !categories.contains(selectedCategory)) {
            selectedCategory = ALL_CATEGORIES;
        }

        categoryList.clearChildren();
        categoryList.applyQueuedMutations();
        for (String category : categories) {
            Button row = new Button(category.equals(selectedCategory) ? "> " + category : category);
            row.layout(style -> style.height(20.0f).flexGrow(0.0f).flexShrink(0.0f));
            row.onClick(event -> selectedCategory(category));
            categoryList.addChild(row);
        }
        categoryList.applyQueuedMutations();
    }

    private void rebuildItems() {
        String query = searchText.toLowerCase(Locale.ROOT);
        visibleItems.clear();
        registry.descriptors().stream()
                .filter(this::allowedForPalette)
                .filter(this::matchesCategory)
                .filter(descriptor -> matchesSearch(descriptor, query))
                .sorted(Comparator
                        .comparing((XmlWidgetDescriptor descriptor) -> descriptor.category(), String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(descriptor -> descriptor.displayName(), String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(descriptor -> descriptor.xmlName(), String.CASE_INSENSITIVE_ORDER))
                .map(PaletteItem::from)
                .forEach(visibleItems::add);

        itemList.clearChildren();
        itemList.applyQueuedMutations();
        if (visibleItems.isEmpty()) {
            Label empty = new Label("No widgets found");
            empty.layout(style -> style.height(20.0f).flexGrow(0.0f).flexShrink(0.0f));
            itemList.addChild(empty);
        } else {
            for (PaletteItem item : visibleItems) {
                Button row = new Button(itemLabel(item));
                row.layout(style -> style.height(22.0f).flexGrow(0.0f).flexShrink(0.0f));
                row.onClick(event -> selectWidget(item.xmlName()));
                itemList.addChild(row);
            }
        }
        itemList.applyQueuedMutations();
    }

    private boolean matchesCategory(XmlWidgetDescriptor descriptor) {
        return selectedCategory.equals(ALL_CATEGORIES) || descriptor.category().equals(selectedCategory);
    }

    private boolean allowedForPalette(XmlWidgetDescriptor descriptor) {
        return includeInternalWidgets || !isInternalDescriptor(descriptor);
    }

    private static boolean isInternalDescriptor(XmlWidgetDescriptor descriptor) {
        return descriptor != null && "Editor".equalsIgnoreCase(descriptor.category());
    }

    private boolean matchesSearch(XmlWidgetDescriptor descriptor, String query) {
        if (query == null || query.isEmpty()) return true;
        return descriptor.xmlName().toLowerCase(Locale.ROOT).contains(query)
                || descriptor.displayName().toLowerCase(Locale.ROOT).contains(query)
                || descriptor.category().toLowerCase(Locale.ROOT).contains(query)
                || descriptor.description().toLowerCase(Locale.ROOT).contains(query);
    }

    private String itemLabel(PaletteItem item) {
        String marker = item.xmlName().equals(selectedXmlName) ? "> " : "";
        return marker + item.displayName() + " <" + item.xmlName() + ">";
    }

    private void emitSelection(String previous, String current) {
        PaletteSelectionChange change = new PaletteSelectionChange(
                this,
                previous == null ? "" : previous,
                current == null ? "" : current,
                selectedDescriptor().orElse(null));
        List<Consumer<PaletteSelectionChange>> snapshot = List.copyOf(selectionListeners);
        for (Consumer<PaletteSelectionChange> listener : snapshot) {
            listener.accept(change);
        }
    }

    private static String normalize(String value, String fallback) {
        String normalized = value == null ? "" : value.trim();
        return normalized.isEmpty() ? fallback : normalized;
    }

    public record PaletteItem(String xmlName,
                              String displayName,
                              String category,
                              String description,
                              boolean acceptsChildren) {
        private static PaletteItem from(XmlWidgetDescriptor descriptor) {
            return new PaletteItem(
                    descriptor.xmlName(),
                    descriptor.displayName(),
                    descriptor.category(),
                    descriptor.description(),
                    descriptor.acceptsChildren());
        }
    }

    public record PaletteSelectionChange(WidgetPalette palette,
                                         String previousXmlName,
                                         String selectedXmlName,
                                         XmlWidgetDescriptor descriptor) {
    }

    public record PaletteInsertRequest(XmlWidgetDescriptor descriptor,
                                       XmlWidgetElement element,
                                       XmlWidgetNodePath parentPath,
                                       int index) {
        public PaletteInsertRequest {
            Objects.requireNonNull(descriptor, "descriptor");
            if (element == null) element = new XmlWidgetElement(descriptor.xmlName());
            element = element.copy();
            parentPath = parentPath == null ? XmlWidgetNodePath.root() : parentPath;
        }

        public XmlWidgetDocumentEdit edit() {
            return XmlWidgetDocumentEdits.addChild(parentPath, index, element);
        }
    }
}
