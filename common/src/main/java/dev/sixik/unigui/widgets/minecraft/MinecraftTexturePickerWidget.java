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
import dev.sixik.unigui.api.render.ImageFit;
import dev.sixik.unigui.api.text.RichText;
import dev.sixik.unigui.api.text.TextRun;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.backend.minecraft.MinecraftFonts;
import dev.sixik.unigui.backend.minecraft.MinecraftTextureHandle;
import dev.sixik.unigui.widgets.Button;
import dev.sixik.unigui.widgets.HBox;
import dev.sixik.unigui.widgets.Label;
import dev.sixik.unigui.widgets.LinearBox;
import dev.sixik.unigui.widgets.Orientation;
import dev.sixik.unigui.widgets.OverlayLayer;
import dev.sixik.unigui.widgets.SearchField;
import dev.sixik.unigui.widgets.TextureWidget;
import dev.sixik.unigui.widgets.Tooltip;
import dev.sixik.unigui.widgets.VBox;
import dev.sixik.unigui.widgets.VirtualListView;
import dev.sixik.unigui.widgets.WindowWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Compact Minecraft texture selector that opens a modal searchable icon grid.
 */
public class MinecraftTexturePickerWidget extends LinearBox {
    private static final float SUMMARY_HEIGHT = 38.0f;
    private static final float TILE_SIZE = 34.0f;
    private static final float ICON_SIZE = 26.0f;
    private static final float TILE_GAP = 4.0f;
    private static final float ROW_HEIGHT = TILE_SIZE + 4.0f;
    private static final int DEFAULT_GRID_COLUMNS = 10;
    private static final float DIALOG_WIDTH = 430.0f;
    private static final float DIALOG_HEIGHT = 420.0f;
    private static final float TOOLTIP_Z = 260.0f;

    private final TextureWidget selectedPreview = new TextureWidget();
    private final Button openButton = new Button("Select texture...");
    private final SearchField searchField = new SearchField();
    private final VirtualListView resultGrid = new VirtualListView() {
        @Override
        public void arrange(RectView bounds) {
            syncGridColumnsForWidth(bounds.width());
            super.arrange(bounds);
        }
    };
    private final Label emptyLabel = new Label("No textures");
    private final Tooltip idTooltip = new Tooltip();
    private final OverlayLayer dialogOverlay = new OverlayLayer();
    private final VBox dialogPanel = new VBox();
    private final WindowWidget dialog = new WindowWidget("Select Minecraft Texture", dialogOverlay);
    private final List<TextureEntry> textures = new ArrayList<>();
    private final List<TextureEntry> filteredTextures = new ArrayList<>();
    private OverlayLayer explicitOverlayLayer;
    private OverlayLayer attachedOverlayLayer;
    private TextureEntry selectedEntry;
    private int gridColumns = DEFAULT_GRID_COLUMNS;

    public MinecraftTexturePickerWidget() {
        this(true);
    }

    public MinecraftTexturePickerWidget(boolean reloadFromMinecraftResources) {
        super(Orientation.HORIZONTAL);
        spacing(6.0f);
        layout(style -> style.align(Alignment.START, Alignment.CENTER));

        selectedPreview.fit(ImageFit.CONTAIN);
        selectedPreview.layout(style -> style.size(SUMMARY_HEIGHT, SUMMARY_HEIGHT).flexGrow(0).flexShrink(0.0f));

        openButton.layout(style -> style.size(240.0f, SUMMARY_HEIGHT).flexGrow(0).flexShrink(0.0f));
        openButton.onClick(event -> open());

        addChild(selectedPreview);
        addChild(openButton);
        buildDialog();

        if (reloadFromMinecraftResources) {
            reloadFromMinecraftResources();
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

    public TextureWidget selectedPreview() {
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

    public MinecraftTexturePickerWidget open() {
        syncDialogAttachment();
        if (attachedOverlayLayer != null) {
            dialog.openModal();
        }
        return this;
    }

    public MinecraftTexturePickerWidget close() {
        dialog.close();
        return this;
    }

    public OverlayLayer overlayLayer() {
        return explicitOverlayLayer != null ? explicitOverlayLayer : findTopmostOverlayLayer();
    }

    public MinecraftTexturePickerWidget overlayLayer(OverlayLayer overlayLayer) {
        if (explicitOverlayLayer == overlayLayer) return this;
        detachDialog();
        explicitOverlayLayer = overlayLayer;
        syncDialogAttachment();
        return this;
    }

    public OverlayLayer attachedOverlayLayer() {
        return attachedOverlayLayer;
    }

    public List<TextureEntry> textures() {
        return List.copyOf(textures);
    }

    public int textureCount() {
        return textures.size();
    }

    public List<TextureEntry> filteredTextures() {
        return List.copyOf(filteredTextures);
    }

    public int filteredTextureCount() {
        return filteredTextures.size();
    }

    public String query() {
        return searchField.text();
    }

    public MinecraftTexturePickerWidget query(String query) {
        searchField.text(query);
        return this;
    }

    public MinecraftTexturePickerWidget clearQuery() {
        return query("");
    }

    public Optional<TextureEntry> selectedEntry() {
        return Optional.ofNullable(selectedEntry);
    }

    public ResourceLocation selectedId() {
        return selectedEntry == null ? null : selectedEntry.id();
    }

    public MinecraftTextureHandle selectedTexture() {
        return selectedEntry == null ? null : selectedEntry.texture();
    }

    public int selectedIndex() {
        return indexOf(textures, selectedEntry);
    }

    public int selectedFilteredIndex() {
        return indexOf(filteredTextures, selectedEntry);
    }

    public int gridColumns() {
        return gridColumns;
    }

    public MinecraftTexturePickerWidget reloadFromMinecraftResources() {
        List<TextureEntry> resourceTextures = new ArrayList<>();
        try {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft != null) {
                ResourceManager resourceManager = minecraft.getResourceManager();
                Map<ResourceLocation, Resource> resources = resourceManager.listResources(
                        "textures",
                        id -> id.getPath().endsWith(".png"));
                for (ResourceLocation id : resources.keySet()) {
                    resourceTextures.add(new TextureEntry(id));
                }
            }
        } catch (RuntimeException | LinkageError ignored) {
            resourceTextures.clear();
        }
        return textures(resourceTextures);
    }

    public MinecraftTexturePickerWidget textures(Collection<TextureEntry> entries) {
        TextureEntry previous = selectedEntry;
        textures.clear();
        if (entries != null) {
            for (TextureEntry entry : entries) {
                if (entry != null) {
                    textures.add(entry);
                }
            }
        }
        selectedEntry = containsEntry(textures, previous) ? previous : null;
        updateSelectedPreview();
        applyFilter();
        return this;
    }

    public MinecraftTexturePickerWidget textureIds(Collection<ResourceLocation> ids) {
        List<TextureEntry> entries = new ArrayList<>();
        if (ids != null) {
            for (ResourceLocation id : ids) {
                if (id != null) {
                    entries.add(new TextureEntry(id));
                }
            }
        }
        return textures(entries);
    }

    public MinecraftTexturePickerWidget selectIndex(int index) {
        TextureEntry entry = index >= 0 && index < textures.size() ? textures.get(index) : null;
        selectEntry(entry, true, false);
        return this;
    }

    public MinecraftTexturePickerWidget selectFilteredIndex(int filteredIndex) {
        TextureEntry entry = filteredIndex >= 0 && filteredIndex < filteredTextures.size()
                ? filteredTextures.get(filteredIndex)
                : null;
        selectEntry(entry, true, false);
        return this;
    }

    public MinecraftTexturePickerWidget selectId(ResourceLocation id) {
        selectEntry(findById(id), true, false);
        return this;
    }

    public MinecraftTexturePickerWidget clearSelection() {
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
        dialog.position(36.0f, 36.0f);
        dialog.modal(true);
        dialog.closeOnOutsideClick(false);
        dialog.layout(style -> style.size(DIALOG_WIDTH, DIALOG_HEIGHT).flexGrow(0).flexShrink(0.0f));

        dialogPanel.spacing(6.0f);
        dialogPanel.layout(style -> style.widthPercent(100.0f).heightPercent(100.0f).flexGrow(1.0f).flexShrink(1.0f));

        searchField.placeholder("Search texture...");
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
        filteredTextures.clear();
        for (TextureEntry entry : textures) {
            if (matches(entry, query)) {
                filteredTextures.add(entry);
            }
        }
        syncGridRows();
        resultGrid.visible(!filteredTextures.isEmpty());
        emptyLabel.visible(filteredTextures.isEmpty());
        resultGrid.scrollTo(0.0f);
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
    }

    private void syncGridRows() {
        resultGrid.itemFactory(this::createRow);
        resultGrid.itemCount(rowCount(filteredTextures.size()));
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
        int last = Math.min(filteredTextures.size(), first + gridColumns);
        for (int index = first; index < last; index++) {
            row.addChild(createTile(filteredTextures.get(index)));
        }
        return row;
    }

    private Button createTile(TextureEntry entry) {
        Button tile = new Button("");
        tile.textPaddingX(0.0f);
        tile.layout(style -> style.size(TILE_SIZE, TILE_SIZE).flexGrow(0).flexShrink(0.0f));
        tile.on(PointerEnteredEvent.TYPE, event -> idTooltip.anchor(tile).richText(minecraftTooltipText(entry.id().toString())));
        tile.onClick(event -> selectEntry(entry, true, true));

        TextureWidget icon = new TextureWidget(entry.texture());
        icon.enabled(false);
        icon.fit(ImageFit.CONTAIN);
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

    private void selectEntry(TextureEntry entry, boolean emitChange, boolean closeDialog) {
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
            selectedPreview.texture(null);
            openButton.text("Select texture...");
        } else {
            selectedPreview.texture(selectedEntry.texture());
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

    private TextureEntry findById(ResourceLocation id) {
        if (id == null) return null;
        for (TextureEntry entry : textures) {
            if (entry.id().equals(id)) {
                return entry;
            }
        }
        return null;
    }

    private static boolean matches(TextureEntry entry, String query) {
        if (query.isEmpty()) return true;
        String id = entry.id().toString().toLowerCase(Locale.ROOT);
        String path = entry.id().getPath().toLowerCase(Locale.ROOT);
        String shortPath = shortTexturePath(entry.id()).toLowerCase(Locale.ROOT);
        return id.contains(query) || path.contains(query) || shortPath.contains(query);
    }

    private static String shortTexturePath(ResourceLocation id) {
        String path = id.getPath();
        if (path.startsWith("textures/")) {
            path = path.substring("textures/".length());
        }
        if (path.endsWith(".png")) {
            path = path.substring(0, path.length() - ".png".length());
        }
        return id.getNamespace() + ":" + path;
    }

    private static String normalizeQuery(String query) {
        return query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
    }

    private static RichText minecraftTooltipText(String text) {
        return RichText.of(text, MinecraftFonts.defaultFace(), TextRun.DEFAULT_PIXEL_SIZE);
    }

    private static boolean containsEntry(List<TextureEntry> entries, TextureEntry candidate) {
        return indexOf(entries, candidate) >= 0;
    }

    private static int indexOf(List<TextureEntry> entries, TextureEntry candidate) {
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

    public record TextureEntry(ResourceLocation id, MinecraftTextureHandle texture) {
        public TextureEntry(ResourceLocation id) {
            this(id, new MinecraftTextureHandle(id, 16, 16));
        }

        public TextureEntry {
            id = Objects.requireNonNull(id, "id");
            texture = texture == null ? new MinecraftTextureHandle(id, 16, 16) : texture;
        }
    }
}
