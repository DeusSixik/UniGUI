package dev.sixik.unigui.widgets.minecraft;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.core.UIContext;
import dev.sixik.unigui.api.event.EventListener;
import dev.sixik.unigui.api.event.EventSubscription;
import dev.sixik.unigui.api.event.PointerEnteredEvent;
import dev.sixik.unigui.api.event.SelectionChangedEvent;
import dev.sixik.unigui.api.layout.Alignment;
import dev.sixik.unigui.api.layout.EdgeInsets;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.text.RichText;
import dev.sixik.unigui.api.text.TextRun;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.backend.minecraft.MinecraftFonts;
import dev.sixik.unigui.widgets.Button;
import dev.sixik.unigui.widgets.HBox;
import dev.sixik.unigui.widgets.Label;
import dev.sixik.unigui.widgets.LinearBox;
import dev.sixik.unigui.widgets.Orientation;
import dev.sixik.unigui.widgets.OverlayLayer;
import dev.sixik.unigui.widgets.SearchField;
import dev.sixik.unigui.widgets.Tooltip;
import dev.sixik.unigui.widgets.VBox;
import dev.sixik.unigui.widgets.VirtualListView;
import dev.sixik.unigui.widgets.WindowWidget;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * Compact Minecraft item selector that opens a modal searchable icon grid.
 */
public class MinecraftItemPickerWidget extends LinearBox {
    private static final float SUMMARY_HEIGHT = 38.0f;
    private static final float TILE_SIZE = 34.0f;
    private static final float ICON_SIZE = 28.0f;
    private static final float TILE_GAP = 4.0f;
    private static final float ROW_HEIGHT = TILE_SIZE + 4.0f;
    private static final int DEFAULT_GRID_COLUMNS = 10;
    private static final float DIALOG_WIDTH = 430.0f;
    private static final float DIALOG_HEIGHT = 420.0f;
    private static final float TOOLTIP_Z = 260.0f;

    private final MinecraftItemPreviewWidget selectedPreview =
            new MinecraftItemPreviewWidget("", ItemStack.EMPTY);
    private final Button openButton = new Button("Select item...");
    private final SearchField searchField = new SearchField();
    private final VirtualListView resultGrid = new VirtualListView() {
        @Override
        public void arrange(RectView bounds) {
            syncGridColumnsForWidth(bounds.width());
            super.arrange(bounds);
        }
    };
    private final Label emptyLabel = new Label("No items");
    private final Tooltip idTooltip = new Tooltip();
    private final OverlayLayer dialogOverlay = new OverlayLayer();
    private final VBox dialogPanel = new VBox();
    private final WindowWidget dialog = new WindowWidget("Select Minecraft Item", dialogOverlay);
    private final List<ItemEntry> items = new ArrayList<>();
    private final List<ItemEntry> filteredItems = new ArrayList<>();
    private OverlayLayer explicitOverlayLayer;
    private OverlayLayer attachedOverlayLayer;
    private ItemEntry selectedEntry;
    private int gridColumns = DEFAULT_GRID_COLUMNS;

    public MinecraftItemPickerWidget() {
        this(true);
    }

    public MinecraftItemPickerWidget(boolean reloadFromRegistry) {
        super(Orientation.HORIZONTAL);
        spacing(6.0f);
        layout(style -> style.align(Alignment.START, Alignment.CENTER));

        selectedPreview.labelVisible(false);
        selectedPreview.previewSize(24.0f);
        selectedPreview.layout(style -> style.size(SUMMARY_HEIGHT, SUMMARY_HEIGHT).flexGrow(0).flexShrink(0.0f));

        openButton.layout(style -> style.size(220.0f, SUMMARY_HEIGHT).flexGrow(0).flexShrink(0.0f));
        openButton.onClick(event -> open());

        addChild(selectedPreview);
        addChild(openButton);
        buildDialog();

        if (reloadFromRegistry) {
            reloadFromRegistry();
        } else {
            applyFilter();
        }
    }

    public Button openButton() {
        return openButton;
    }

    public SearchField searchField() {
        return searchField;
    }

    public MinecraftItemPreviewWidget selectedPreview() {
        return selectedPreview;
    }

    public VirtualListView resultGrid() {
        return resultGrid;
    }

    public VirtualListView resultRows() {
        return resultGrid;
    }

    public WindowWidget dialog() {
        return dialog;
    }

    public OverlayLayer dialogOverlay() {
        return dialogOverlay;
    }

    public Tooltip idTooltip() {
        return idTooltip;
    }

    public boolean opened() {
        return dialog.opened();
    }

    public MinecraftItemPickerWidget open() {
        syncDialogAttachment();
        if (attachedOverlayLayer != null) {
            dialog.openModal();
        }
        return this;
    }

    public MinecraftItemPickerWidget close() {
        dialog.close();
        return this;
    }

    public OverlayLayer overlayLayer() {
        return explicitOverlayLayer != null ? explicitOverlayLayer : findTopmostOverlayLayer();
    }

    public MinecraftItemPickerWidget overlayLayer(OverlayLayer overlayLayer) {
        if (explicitOverlayLayer == overlayLayer) return this;
        detachDialog();
        explicitOverlayLayer = overlayLayer;
        syncDialogAttachment();
        return this;
    }

    public OverlayLayer attachedOverlayLayer() {
        return attachedOverlayLayer;
    }

    public List<ItemEntry> items() {
        return List.copyOf(items);
    }

    public int itemCount() {
        return items.size();
    }

    public List<ItemEntry> filteredItems() {
        return List.copyOf(filteredItems);
    }

    public int filteredItemCount() {
        return filteredItems.size();
    }

    public String query() {
        return searchField.text();
    }

    public MinecraftItemPickerWidget query(String query) {
        searchField.text(query);
        return this;
    }

    public MinecraftItemPickerWidget clearQuery() {
        return query("");
    }

    public Optional<ItemEntry> selectedEntry() {
        return Optional.ofNullable(selectedEntry);
    }

    public ResourceLocation selectedId() {
        return selectedEntry == null ? null : selectedEntry.id();
    }

    public Item selectedItem() {
        return selectedEntry == null ? null : selectedEntry.item();
    }

    public ItemStack selectedStack() {
        return selectedEntry == null ? ItemStack.EMPTY : selectedEntry.stack();
    }

    public int selectedIndex() {
        return indexOf(items, selectedEntry);
    }

    public int selectedFilteredIndex() {
        return indexOf(filteredItems, selectedEntry);
    }

    public int gridColumns() {
        return gridColumns;
    }

    public MinecraftItemPickerWidget reloadFromRegistry() {
        List<ItemEntry> registryItems = new ArrayList<>();
        try {
            for (Item item : BuiltInRegistries.ITEM) {
                if (item == null || item == Items.AIR) continue;
                ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
                if (id != null) {
                    registryItems.add(new ItemEntry(id, item));
                }
            }
        } catch (RuntimeException | LinkageError ignored) {
            registryItems.clear();
        }
        return items(registryItems);
    }

    public MinecraftItemPickerWidget items(Collection<ItemEntry> entries) {
        ItemEntry previous = selectedEntry;
        items.clear();
        if (entries != null) {
            for (ItemEntry entry : entries) {
                if (entry != null) {
                    items.add(entry);
                }
            }
        }
        selectedEntry = containsEntry(items, previous) ? previous : null;
        updateSelectedPreview();
        applyFilter();
        return this;
    }

    public MinecraftItemPickerWidget selectIndex(int index) {
        ItemEntry entry = index >= 0 && index < items.size() ? items.get(index) : null;
        selectEntry(entry, true, false);
        return this;
    }

    public MinecraftItemPickerWidget selectFilteredIndex(int filteredIndex) {
        ItemEntry entry = filteredIndex >= 0 && filteredIndex < filteredItems.size()
                ? filteredItems.get(filteredIndex)
                : null;
        selectEntry(entry, true, false);
        return this;
    }

    public MinecraftItemPickerWidget selectId(ResourceLocation id) {
        selectEntry(findById(id), true, false);
        return this;
    }

    public MinecraftItemPickerWidget selectItem(Item item) {
        selectEntry(findByItem(item), true, false);
        return this;
    }

    public MinecraftItemPickerWidget clearSelection() {
        selectEntry(null, true, false);
        return this;
    }

    public EventSubscription onSelectionChanged(EventListener<? super SelectionChangedEvent> listener) {
        return on(SelectionChangedEvent.TYPE, listener);
    }

    @Override
    public void setParentInternal(Widget parent) {
        super.setParentInternal(parent);
        syncDialogAttachment();
    }

    @Override
    public void setUiContextInternal(UIContext uiContext) {
        super.setUiContextInternal(uiContext);
        syncDialogAttachment();
    }

    private void buildDialog() {
        dialogOverlay.content(dialogPanel);
        dialogOverlay.addOverlay(new MinecraftZLayer(idTooltip, TOOLTIP_Z));
        dialogOverlay.layout(style -> style.widthPercent(100.0f).heightPercent(100.0f).flexGrow(1.0f).flexShrink(1.0f));

        dialog.padding(EdgeInsets.all(6.0f));
        dialog.position(24.0f, 24.0f);
        dialog.modal(true);
        dialog.closeOnOutsideClick(false);
        dialog.layout(style -> style.size(DIALOG_WIDTH, DIALOG_HEIGHT).flexGrow(0).flexShrink(0.0f));

        dialogPanel.spacing(6.0f);
        dialogPanel.layout(style -> style.widthPercent(100.0f).heightPercent(100.0f).flexGrow(1.0f).flexShrink(1.0f));

        searchField.placeholder("Search item...");
        searchField.layout(style -> style.widthPercent(100.0f).height(22.0f).flexShrink(0.0f));
        searchField.onTextChanged(event -> applyFilter());

        resultGrid.itemHeight(ROW_HEIGHT)
                .overscan(3)
                .consumeWheelAtScrollBounds(false)
                .itemFactory(this::createRow);
        resultGrid.layout(style -> style.widthPercent(100.0f).height(326.0f).flexGrow(1.0f).flexShrink(1.0f));

        emptyLabel.layout(style -> style.widthPercent(100.0f).height(20.0f).flexGrow(0).flexShrink(0.0f));
        emptyLabel.visible(false);

        dialogPanel.addChild(searchField);
        dialogPanel.addChild(resultGrid);
        dialogPanel.addChild(emptyLabel);
        dialogPanel.applyQueuedMutations();
        dialogOverlay.applyQueuedMutations();
        dialog.applyQueuedMutations();
    }

    private void applyFilter() {
        String query = normalizeQuery(searchField.text());
        filteredItems.clear();
        for (ItemEntry entry : items) {
            if (matches(entry, query)) {
                filteredItems.add(entry);
            }
        }
        syncGridRows();
        resultGrid.visible(!filteredItems.isEmpty());
        emptyLabel.visible(filteredItems.isEmpty());
        resultGrid.scrollTo(0.0f);
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
    }

    private void syncGridRows() {
        resultGrid.itemFactory(this::createRow);
        resultGrid.itemCount(rowCount(filteredItems.size()));
    }

    private void syncGridColumnsForWidth(float width) {
        int nextColumns = columnsForWidth(width);
        if (nextColumns == gridColumns) return;
        gridColumns = nextColumns;
        syncGridRows();
    }

    private HBox createRow(int rowIndex) {
        HBox row = new HBox();
        row.spacing(TILE_GAP);
        row.layout(style -> style.widthPercent(100.0f).height(ROW_HEIGHT).flexGrow(0).flexShrink(0.0f));
        int first = rowIndex * gridColumns;
        int last = Math.min(filteredItems.size(), first + gridColumns);
        for (int index = first; index < last; index++) {
            row.addChild(createTile(filteredItems.get(index)));
        }
        return row;
    }

    private Button createTile(ItemEntry entry) {
        Button tile = new Button("");
        tile.textPaddingX(0.0f);
        tile.layout(style -> style.size(TILE_SIZE, TILE_SIZE).flexGrow(0).flexShrink(0.0f));
        tile.on(PointerEnteredEvent.TYPE, event -> idTooltip.anchor(tile).richText(minecraftTooltipText(entry.id().toString())));
        tile.onClick(event -> selectEntry(entry, true, true));

        MinecraftItemPreviewWidget icon = new MinecraftItemPreviewWidget("", entry.stack());
        icon.labelVisible(false);
        icon.decorations(false);
        icon.enabled(false);
        icon.backgroundVisible(false);
        icon.borderVisible(false);
        icon.previewSize(20.0f);
        icon.layout(style -> style.size(ICON_SIZE, ICON_SIZE)
                .align(Alignment.CENTER, Alignment.CENTER)
                .flexGrow(0)
                .flexShrink(0.0f));
        tile.addChild(icon);
        return tile;
    }

    private int rowCount(int itemCount) {
        return rowCount(itemCount, gridColumns);
    }

    private static int rowCount(int itemCount, int columns) {
        int normalizedColumns = Math.max(1, columns);
        return Math.max(0, (itemCount + normalizedColumns - 1) / normalizedColumns);
    }

    private static int columnsForWidth(float width) {
        if (!Float.isFinite(width) || width <= 0.0f) return DEFAULT_GRID_COLUMNS;
        return Math.max(1, (int) Math.floor((width + TILE_GAP) / (TILE_SIZE + TILE_GAP)));
    }

    private void selectEntry(ItemEntry entry, boolean emitChange, boolean closeDialog) {
        if (Objects.equals(selectedEntry, entry)) {
            if (closeDialog) close();
            return;
        }
        int oldIndex = selectedIndex();
        selectedEntry = entry;
        updateSelectedPreview();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        if (emitChange) {
            emit(new SelectionChangedEvent(this, indexList(oldIndex), indexList(selectedIndex())));
        }
        if (closeDialog) {
            close();
        }
    }

    private void updateSelectedPreview() {
        if (selectedEntry == null) {
            selectedPreview.stack(ItemStack.EMPTY);
            openButton.text("Select item...");
        } else {
            selectedPreview.stack(selectedEntry.stack());
            openButton.text(selectedEntry.id().toString());
        }
    }

    private void syncDialogAttachment() {
        OverlayLayer target = overlayLayer();
        if (target == null) {
            detachDialog();
            return;
        }
        if (attachedOverlayLayer == target) return;
        detachDialog();
        attachedOverlayLayer = target;
        attachedOverlayLayer.addOverlay(dialog);
    }

    private void detachDialog() {
        if (attachedOverlayLayer != null) {
            dialog.close();
            attachedOverlayLayer.removeOverlay(dialog);
            attachedOverlayLayer = null;
        }
    }

    private OverlayLayer findTopmostOverlayLayer() {
        OverlayLayer result = null;
        Widget current = this;
        while (current != null) {
            if (current instanceof OverlayLayer layer) {
                result = layer;
            }
            current = current.parent();
        }
        return result;
    }

    private ItemEntry findById(ResourceLocation id) {
        if (id == null) return null;
        for (ItemEntry entry : items) {
            if (entry.id().equals(id)) {
                return entry;
            }
        }
        return null;
    }

    private ItemEntry findByItem(Item item) {
        if (item == null) return null;
        for (ItemEntry entry : items) {
            if (entry.item() == item) {
                return entry;
            }
        }
        return null;
    }

    private static boolean matches(ItemEntry entry, String query) {
        if (query.isEmpty()) return true;
        String id = entry.id().toString().toLowerCase(Locale.ROOT);
        String path = entry.id().getPath().toLowerCase(Locale.ROOT);
        String descriptionId = safeDescriptionId(entry.item()).toLowerCase(Locale.ROOT);
        return id.contains(query) || path.contains(query) || descriptionId.contains(query);
    }

    private static String safeDescriptionId(Item item) {
        try {
            return item.getDescriptionId();
        } catch (RuntimeException | LinkageError ignored) {
            return "";
        }
    }

    private static String normalizeQuery(String query) {
        return query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
    }

    private static RichText minecraftTooltipText(String text) {
        return RichText.of(text, MinecraftFonts.defaultFace(), TextRun.DEFAULT_PIXEL_SIZE);
    }

    private static boolean containsEntry(List<ItemEntry> entries, ItemEntry candidate) {
        return indexOf(entries, candidate) >= 0;
    }

    private static int indexOf(List<ItemEntry> entries, ItemEntry candidate) {
        if (candidate == null) return -1;
        for (int i = 0; i < entries.size(); i++) {
            if (Objects.equals(entries.get(i), candidate)) {
                return i;
            }
        }
        return -1;
    }

    private static List<Integer> indexList(int index) {
        return index < 0 ? List.of() : List.of(index);
    }

    public record ItemEntry(ResourceLocation id, Item item) {
        public ItemEntry {
            id = Objects.requireNonNull(id, "id");
            item = Objects.requireNonNull(item, "item");
        }

        public ItemStack stack() {
            return new ItemStack(item);
        }
    }
}
