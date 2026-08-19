package dev.sixik.unigui.widgets.editor;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.event.EventSubscription;
import dev.sixik.unigui.api.layout.LayoutConstraints;
import dev.sixik.unigui.api.xml.XmlAttribute;
import dev.sixik.unigui.api.xml.XmlWidgetName;
import dev.sixik.unigui.widgets.containers.HBox;
import dev.sixik.unigui.widgets.containers.LinearBox;
import dev.sixik.unigui.widgets.core.Orientation;
import dev.sixik.unigui.widgets.interaction.Button;
import dev.sixik.unigui.widgets.interaction.SearchField;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

/** Search box paired with reusable toggle chips for editor filtering surfaces. */
@XmlWidgetName("SearchBoxWithFilterChips")
public class SearchBoxWithFilterChips extends LinearBox {
    public static final String STYLE_TYPE = dev.sixik.unigui.api.style.StyleIds.Widget.SEARCH_BOX_WITH_FILTER_CHIPS;

    private final SearchField searchField = new SearchField();
    private final HBox chipRow = new HBox();
    private final List<FilterChip> filters = new ArrayList<>();
    private final Set<String> activeFilterIds = new LinkedHashSet<>();
    private final List<Consumer<FilterChange>> listeners = new ArrayList<>();
    private String searchText = "";
    private boolean syncingSearchField;

    public SearchBoxWithFilterChips() {
        super(Orientation.VERTICAL);
        spacing(4.0f);
        layout(style -> style.size(LayoutConstraints.AUTO, LayoutConstraints.AUTO).flexGrow(0.0f).flexShrink(0.0f));

        searchField.searchChangeDebounceSeconds(0.0f);
        searchField.layout(style -> style.height(20.0f).flexGrow(0.0f).flexShrink(0.0f));
        searchField.onSearchChanged(event -> {
            if (!syncingSearchField) search(event.newQuery());
        });
        chipRow.spacing(4.0f);
        chipRow.layout(style -> style.height(22.0f).flexGrow(0.0f).flexShrink(0.0f));

        addChild(searchField);
        addChild(chipRow);
        applyQueuedMutations();
        rebuildChips();
    }

    public String search() {
        return searchText;
    }

    @XmlAttribute(value = "search", category = "Behavior", defaultValue = "", description = "Search text shared by the surrounding editor surface.")
    public SearchBoxWithFilterChips search(String search) {
        String normalized = search == null ? "" : search.trim();
        if (searchText.equals(normalized)) return this;
        searchText = normalized;
        if (!searchField.text().equals(normalized)) {
            syncingSearchField = true;
            searchField.text(normalized);
            searchField.flushSearchChanged();
            syncingSearchField = false;
        }
        emit("", false);
        return this;
    }

    public List<FilterChip> filters() {
        return List.copyOf(filters);
    }

    @XmlAttribute(value = "filters", category = "Content", defaultValue = "", description = "Filter chips as id:Label pairs separated by pipes.")
    public SearchBoxWithFilterChips filters(String filters) {
        this.filters.clear();
        if (filters != null && !filters.isBlank()) {
            for (String token : filters.split("\\|")) {
                FilterChip chip = parseChip(token);
                if (chip != null && this.filters.stream().noneMatch(existing -> existing.id().equals(chip.id()))) {
                    this.filters.add(chip);
                }
            }
        }
        activeFilterIds.removeIf(id -> this.filters.stream().noneMatch(chip -> chip.id().equals(id)));
        rebuildChips();
        emit("", false);
        return this;
    }

    public SearchBoxWithFilterChips filters(List<FilterChip> filters) {
        this.filters.clear();
        if (filters != null) {
            for (FilterChip filter : filters) {
                if (filter != null && this.filters.stream().noneMatch(existing -> existing.id().equals(filter.id()))) {
                    this.filters.add(filter);
                }
            }
        }
        activeFilterIds.removeIf(id -> this.filters.stream().noneMatch(chip -> chip.id().equals(id)));
        rebuildChips();
        emit("", false);
        return this;
    }

    public List<String> activeFilters() {
        return List.copyOf(activeFilterIds);
    }

    @XmlAttribute(value = "activeFilters", category = "State", defaultValue = "", description = "Active filter ids separated by commas, semicolons or pipes.")
    public SearchBoxWithFilterChips activeFilters(String activeFilters) {
        activeFilterIds.clear();
        if (activeFilters != null && !activeFilters.isBlank()) {
            for (String token : activeFilters.split("[|,;]")) {
                String id = normalizeId(token);
                if (!id.isEmpty()) activeFilterIds.add(id);
            }
        }
        rebuildChips();
        emit("", false);
        return this;
    }

    public boolean filterActive(String filterId) {
        return activeFilterIds.contains(normalizeId(filterId));
    }

    public boolean setFilterActive(String filterId, boolean active) {
        String id = normalizeId(filterId);
        if (id.isEmpty() || !hasFilter(id)) return false;
        boolean changed = active ? activeFilterIds.add(id) : activeFilterIds.remove(id);
        if (!changed) return false;
        rebuildChips();
        emit(id, active);
        return true;
    }

    public boolean toggleFilter(String filterId) {
        String id = normalizeId(filterId);
        if (id.isEmpty() || !hasFilter(id)) return false;
        return setFilterActive(id, !activeFilterIds.contains(id));
    }

    public SearchBoxWithFilterChips clearFilters() {
        if (activeFilterIds.isEmpty()) return this;
        activeFilterIds.clear();
        rebuildChips();
        emit("", false);
        return this;
    }

    public EventSubscription onFilterChanged(Consumer<FilterChange> listener) {
        Objects.requireNonNull(listener, "listener");
        listeners.add(listener);
        return () -> listeners.remove(listener);
    }

    public SearchField searchField() {
        return searchField;
    }

    public HBox chipRow() {
        return chipRow;
    }

    private void rebuildChips() {
        chipRow.clearChildren();
        chipRow.applyQueuedMutations();
        for (FilterChip filter : filters) {
            Button chip = new Button((activeFilterIds.contains(filter.id()) ? "* " : "") + filter.label());
            chip.layout(style -> style.height(20.0f).flexGrow(0.0f).flexShrink(0.0f));
            chip.onClick(event -> toggleFilter(filter.id()));
            chipRow.addChild(chip);
        }
        chipRow.applyQueuedMutations();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
    }

    private boolean hasFilter(String filterId) {
        String id = normalizeId(filterId);
        return filters.stream().anyMatch(filter -> filter.id().equals(id));
    }

    private void emit(String changedFilterId, boolean active) {
        FilterChange change = new FilterChange(this, searchText, activeFilters(), changedFilterId, active);
        List<Consumer<FilterChange>> snapshot = List.copyOf(listeners);
        for (Consumer<FilterChange> listener : snapshot) {
            listener.accept(change);
        }
    }

    private static FilterChip parseChip(String token) {
        String normalized = token == null ? "" : token.trim();
        if (normalized.isEmpty()) return null;
        int colon = normalized.indexOf(':');
        if (colon < 0) return new FilterChip(normalized, normalized);
        return new FilterChip(normalized.substring(0, colon), normalized.substring(colon + 1));
    }

    private static String normalizeId(String value) {
        return value == null ? "" : value.trim();
    }

    public record FilterChip(String id, String label) {
        public FilterChip {
            id = normalizeId(id);
            if (id.isEmpty()) throw new IllegalArgumentException("Filter chip id must not be blank");
            label = label == null || label.isBlank() ? id : label.trim();
        }
    }

    public record FilterChange(SearchBoxWithFilterChips source,
                               String search,
                               List<String> activeFilters,
                               String changedFilterId,
                               boolean active) {
        public FilterChange {
            Objects.requireNonNull(source, "source");
            search = search == null ? "" : search;
            activeFilters = List.copyOf(activeFilters == null ? List.of() : activeFilters);
            changedFilterId = changedFilterId == null ? "" : changedFilterId;
        }
    }
}
