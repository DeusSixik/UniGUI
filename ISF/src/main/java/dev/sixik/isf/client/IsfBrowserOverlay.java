package dev.sixik.isf.client;

import dev.sixik.isf.network.IsfNetwork;
import dev.sixik.isf.definition.IsfRecipeDefinition;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import dev.sixik.unigui.api.core.FrameContext;
import dev.sixik.unigui.api.event.PointerEnteredEvent;
import dev.sixik.unigui.api.event.PointerExitedEvent;
import dev.sixik.unigui.api.layout.Alignment;
import dev.sixik.unigui.api.layout.PositionType;
import dev.sixik.unigui.backend.minecraft_impl.MinecraftRenderLayerRegistration;
import dev.sixik.unigui.backend.minecraft_impl.MinecraftWidgetRenderLayer;
import dev.sixik.unigui.backend.minecraft_impl.MinecraftWidgetScreen;
import dev.sixik.unigui.backend.minecraft_impl.ScreenOverlayRender;
import dev.sixik.unigui.widgets.containers.Box;
import dev.sixik.unigui.widgets.containers.GridBox;
import dev.sixik.unigui.widgets.containers.HBox;
import dev.sixik.unigui.widgets.containers.ScrollView;
import dev.sixik.unigui.widgets.containers.VBox;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.widgets.feedback.OverlayLayer;
import dev.sixik.unigui.widgets.interaction.Button;
import dev.sixik.unigui.widgets.minecraft.MinecraftItemTooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.lang.reflect.Field;

/** UniGUI screen-overlay с вертикально перелистываемой сеткой доступных предметов. */
final class IsfBrowserOverlay {
    private static final int COLUMNS = 6;
    private static final float CELL = 18.0f;

    private final BrowserRoot contentRoot = new BrowserRoot();
    private final OverlayLayer overlayRoot = new OverlayLayer(contentRoot);
    private final Box bookmarkPanel = panelShell();
    private final Box browserPanel = panelShell();
    private final GridBox grid = new GridBox().columns(COLUMNS);
    private final ScrollView itemScroll = new ScrollView(grid);
    private final GridBox bookmarkGrid = new GridBox().columns(4);
    private final ScrollView bookmarkScroll = new ScrollView(bookmarkGrid);
    private final Box detailPanel = new Box();
    private final Label detailTitle = new Label();
    private final Label detailSource = new Label();
    private final IsfItemIconWidget detailIcon = new IsfItemIconWidget(ItemStack.EMPTY);
    private final GridBox detailIngredients = new GridBox().columns(3);
    private final Label detailStats = new Label();
    private final List<MinecraftItemTooltip> tooltips = new ArrayList<>();
    private final Map<ResourceLocation, Button> bookmarkCells = new LinkedHashMap<>();
    private final Map<ResourceLocation, MinecraftItemTooltip> bookmarkTooltips = new LinkedHashMap<>();

    private MinecraftRenderLayerRegistration<Screen> registration;
    private List<ItemEntry> catalogEntries = List.of();
    private Map<ResourceLocation, ResourceLocation> observedRecipeResults = Map.of();
    private ItemEntry hoveredEntry;
    private ItemEntry selectedEntry;
    private long observedStateVersion = Long.MIN_VALUE;
    private float savedScrollY;
    private boolean restoreScroll;
    private boolean itemScrollBarDragging;
    private boolean bookmarkScrollBarDragging;

    IsfBrowserOverlay() {
        configureTree();
    }

    void register() {
        if (registration != null && !registration.closed()) return;
        MinecraftWidgetRenderLayer layer = new MinecraftWidgetRenderLayer(overlayRoot);
        registration = ScreenOverlayRender.register(
                layer,
                screen -> screen instanceof AbstractContainerScreen<?>
                        && !(screen instanceof MinecraftWidgetScreen),
                100);
    }

    void resetPage() {
        hoveredEntry = null;
        selectedEntry = null;
        itemScrollBarDragging = false;
        bookmarkScrollBarDragging = false;
    }

    boolean toggleHoveredBookmark() {
        ItemEntry entry = hoveredEntry;
        if (entry == null) return false;
        boolean bookmarked = !IsfClientState.bookmarks().contains(entry.id());
        IsfNetwork.toggleBookmark(entry.id(), bookmarked);
        return true;
    }

    boolean scrollItemsAt(double mouseX, double mouseY, double delta) {
        if (delta == 0.0) {
            return false;
        }
        if (contains(browserPanel.layoutBounds(), (float) mouseX, (float) mouseY)) {
            return scrollViewAt(itemScroll, delta);
        }
        if (contains(bookmarkPanel.layoutBounds(), (float) mouseX, (float) mouseY)) {
            return scrollViewAt(bookmarkScroll, delta);
        }
        return false;
    }

    boolean beginItemScrollBarDrag(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return false;
        }
        if (itemScroll.maxScrollY() > 0.0f
                && containsExpanded(itemScroll.verticalScrollBar().layoutBounds(),
                (float) mouseX, (float) mouseY, 3.0f)) {
            itemScrollBarDragging = true;
            updateScrollFromPointer(itemScroll, (float) mouseY);
            return true;
        }
        if (bookmarkScroll.maxScrollY() > 0.0f
                && containsExpanded(bookmarkScroll.verticalScrollBar().layoutBounds(),
                (float) mouseX, (float) mouseY, 3.0f)) {
            bookmarkScrollBarDragging = true;
            updateScrollFromPointer(bookmarkScroll, (float) mouseY);
            return true;
        }
        return false;
    }

    boolean dragItemScrollBar(double mouseY, int button) {
        if (button != 0) return false;
        if (itemScrollBarDragging) updateScrollFromPointer(itemScroll, (float) mouseY);
        if (bookmarkScrollBarDragging) updateScrollFromPointer(bookmarkScroll, (float) mouseY);
        if (!itemScrollBarDragging && !bookmarkScrollBarDragging) return false;
        return true;
    }

    boolean endItemScrollBarDrag(double mouseY, int button) {
        if (button != 0) return false;
        if (itemScrollBarDragging) updateScrollFromPointer(itemScroll, (float) mouseY);
        if (bookmarkScrollBarDragging) updateScrollFromPointer(bookmarkScroll, (float) mouseY);
        if (!itemScrollBarDragging && !bookmarkScrollBarDragging) return false;
        itemScrollBarDragging = false;
        bookmarkScrollBarDragging = false;
        return true;
    }

    private boolean scrollViewAt(ScrollView scroll, double delta) {
        float before = scroll.scrollY();
        scroll.scrollBy(0.0f, (float) (-delta * scroll.scrollStep()));
        return before != scroll.scrollY();
    }

    private void updateScrollFromPointer(ScrollView scroll, float mouseY) {
        dev.sixik.unigui.api.math.RectView track = scroll.verticalScrollBar().layoutBounds();
        float trackLength = Math.max(1.0f, track.height());
        float pageSize = Math.max(1.0f, scroll.verticalScrollBar().pageSize());
        float maxScroll = scroll.maxScrollY();
        float thumbLength = Math.max(8.0f, trackLength * (pageSize / (pageSize + maxScroll)));
        float travel = Math.max(1.0f, trackLength - thumbLength);
        float normalized = Math.max(0.0f, Math.min(1.0f,
                (mouseY - track.y() - thumbLength * 0.5f) / travel));
        scroll.scrollTo(0.0f, normalized * maxScroll);
    }

    private void configureTree() {
        overlayRoot.layout(style -> style.fill());
        contentRoot.themeEnabled(false);
        contentRoot.backgroundVisible(false);
        contentRoot.borderVisible(false);
        contentRoot.layout(style -> style.fill());

        Box panel = browserPanel;
        panel.layout(style -> style
                .position(PositionType.ABSOLUTE)
                .right(8.0f)
                .top(8.0f)
                .size(176.0f, 200.0f)
                .padding(4.0f));

        VBox column = new VBox();
        column.spacing(2.0f);
        column.layout(style -> style.fill());

        grid.spacing(0.0f);
        grid.layout(style -> style.widthPercent(100.0f).flexNone());
        itemScroll.scrollStep(18.0f);
        itemScroll.layout(style -> style.widthPercent(100.0f).flexGrow(1.0f).flexShrink(1.0f));

        column.addChild(itemScroll);
        panel.addChild(column);
        contentRoot.addChild(panel);

        configureBookmarksPanel();
        configureDetailPanel();
        rebuild(false, 0.0f);
    }

    private void configureBookmarksPanel() {
        Box panel = bookmarkPanel;
        panel.layout(style -> style.position(PositionType.ABSOLUTE)
                .left(8.0f).top(8.0f).size(96.0f, 200.0f).padding(4.0f));
        Label title = new Label("BOOKMARKS");
        title.layout(style -> style.size(88.0f, 14.0f).flexNone());
        bookmarkGrid.spacing(0.0f);
        bookmarkGrid.layout(style -> style.widthPercent(100.0f).flexNone());
        bookmarkScroll.scrollStep(CELL);
        bookmarkScroll.layout(style -> style.widthPercent(100.0f).flexGrow(1.0f).flexShrink(1.0f));
        VBox column = new VBox();
        column.spacing(2.0f);
        column.layout(style -> style.fill());
        column.addChild(title);
        column.addChild(bookmarkScroll);
        panel.addChild(column);
        contentRoot.addChild(panel);
    }

    private void configureDetailPanel() {
        detailPanel.themeEnabled(false);
        detailPanel.backgroundVisible(true);
        detailPanel.borderVisible(true);
        detailPanel.radius(3.0f);
        detailPanel.background().set(0.063f, 0.078f, 0.106f, 0.94f);
        detailPanel.borderColor().set(0.416f, 0.561f, 0.682f, 1.0f);
        detailPanel.layout(style -> style
                .position(PositionType.ABSOLUTE)
                .left(104.0f)
                .top(4.0f)
                .size(116.0f, 112.0f)
                .padding(4.0f));

        VBox content = new VBox();
        content.spacing(2.0f);
        content.layout(style -> style.fill());
        detailTitle.layout(style -> style.size(168.0f, 16.0f).flexNone());
        detailSource.layout(style -> style.size(168.0f, 14.0f).flexNone());
        detailIcon.layout(style -> style.size(32.0f, 32.0f).centerSelf().flexNone());
        detailIcon.enabled(false);
        detailIngredients.spacing(0.0f);
        detailIngredients.layout(style -> style.size(54.0f, 54.0f).centerSelf().flexNone());
        detailStats.layout(style -> style.size(168.0f, 14.0f).flexNone());
        HBox recipeBody = new HBox();
        recipeBody.spacing(18.0f);
        recipeBody.layout(style -> style.size(168.0f, 58.0f).center().flexNone());
        recipeBody.addChild(detailIngredients);
        recipeBody.addChild(detailIcon);
        content.addChild(detailTitle);
        content.addChild(detailSource);
        content.addChild(recipeBody);
        content.addChild(detailStats);
        detailPanel.addChild(content);
        contentRoot.addChild(detailPanel);
        updateDetail(null);
    }

    private Box panel(float left, float top, float width, float height) {
        Box panel = new Box();
        panel.themeEnabled(false);
        panel.backgroundVisible(true);
        panel.borderVisible(true);
        panel.radius(3.0f);
        panel.background().set(0.063f, 0.078f, 0.106f, 0.90f);
        panel.borderColor().set(0.416f, 0.561f, 0.682f, 1.0f);
        panel.layout(style -> style.position(PositionType.ABSOLUTE)
                .left(left).top(top).size(width, height).padding(4.0f));
        return panel;
    }

    private static Box panelShell() {
        Box panel = new Box();
        panel.themeEnabled(false);
        panel.backgroundVisible(true);
        panel.borderVisible(true);
        panel.radius(3.0f);
        panel.background().set(0.063f, 0.078f, 0.106f, 0.90f);
        panel.borderColor().set(0.416f, 0.561f, 0.682f, 1.0f);
        return panel;
    }

    private void rebuild(boolean ignored, float ignoredOffset) {
        List<ItemEntry> entries = entries();
        catalogEntries = entries;
        savedScrollY = itemScroll.scrollY();
        restoreScroll = true;
        hoveredEntry = null;

        for (MinecraftItemTooltip tooltip : tooltips) overlayRoot.removeOverlay(tooltip);
        tooltips.clear();
        bookmarkCells.clear();
        bookmarkTooltips.clear();
        grid.clearChildren();
        bookmarkGrid.clearChildren();

        for (ItemEntry entry : entries) {
            Button cell = itemCell(entry);
            grid.addChild(cell);
            addTooltip(cell, entry);
        }

        int rows = (entries.size() + COLUMNS - 1) / COLUMNS;
        itemScroll.contentHeight(rows * CELL);

        syncBookmarks();

        if (selectedEntry != null) {
            ItemEntry refreshedSelection = entries.stream()
                    .filter(entry -> entry.id().equals(selectedEntry.id()))
                    .findFirst().orElse(null);
            updateDetail(refreshedSelection);
        }
        observedRecipeResults = IsfClientState.recipeResults();
        observedStateVersion = IsfClientState.version();
    }

    private void syncBookmarks() {
        Set<ResourceLocation> desired = IsfClientState.bookmarks();

        for (ResourceLocation id : new ArrayList<>(bookmarkCells.keySet())) {
            if (desired.contains(id)) continue;
            Button cell = bookmarkCells.remove(id);
            if (cell != null) bookmarkGrid.removeChild(cell);
            MinecraftItemTooltip tooltip = bookmarkTooltips.remove(id);
            if (tooltip != null) {
                overlayRoot.removeOverlay(tooltip);
                tooltips.remove(tooltip);
            }
            if (hoveredEntry != null && hoveredEntry.id().equals(id)) hoveredEntry = null;
        }

        int bookmarkIndex = 0;
        for (ItemEntry entry : catalogEntries) {
            if (!desired.contains(entry.id())) continue;
            if (!bookmarkCells.containsKey(entry.id())) {
                Button cell = itemCell(entry);
                MinecraftItemTooltip tooltip = new MinecraftItemTooltip(cell, entry.stack());
                bookmarkGrid.insertChild(bookmarkIndex, cell);
                overlayRoot.addOverlay(tooltip);
                tooltips.add(tooltip);
                bookmarkCells.put(entry.id(), cell);
                bookmarkTooltips.put(entry.id(), tooltip);
            }
            bookmarkIndex++;
        }
        int columns = Math.max(1, bookmarkGrid.columns());
        int rows = (bookmarkCells.size() + columns - 1) / columns;
        bookmarkScroll.contentHeight(rows * CELL);
    }

    private Button itemCell(ItemEntry entry) {
        Button cell = new Button();
        cell.textPadding(0.0f, 0.0f);
        cell.layout(style -> style.size(CELL, CELL).flexNone());
        cell.on(PointerEnteredEvent.TYPE, event -> hoveredEntry = entry);
        cell.on(PointerExitedEvent.TYPE, event -> {
            if (hoveredEntry == entry) hoveredEntry = null;
        });
        cell.onClick(event -> updateDetail(entry));

        IsfItemIconWidget icon = new IsfItemIconWidget(entry.stack());
        // Иконка только рисуется. Hit-box и все pointer-события принадлежат Button-клетке.
        icon.enabled(false);
        icon.layout(style -> style.size(16.0f, 16.0f).centerSelf().flexNone());
        cell.addChild(icon);
        return cell;
    }

    private void addTooltip(Button cell, ItemEntry entry) {
        MinecraftItemTooltip tooltip = new MinecraftItemTooltip(cell, entry.stack());
        tooltips.add(tooltip);
        overlayRoot.addOverlay(tooltip);
    }

    private void updateDetail(ItemEntry entry) {
        selectedEntry = entry;
        if (entry == null) {
            detailPanel.visibility(dev.sixik.unigui.api.widget.Visibility.COLLAPSED);
            return;
        }
        detailPanel.visibility(dev.sixik.unigui.api.widget.Visibility.VISIBLE);
        detailTitle.text(entry.stack().getHoverName().getString());
        detailIcon.stack(entry.stack());
        IsfRecipeDefinition recipe = entry.recipeId() == null
                ? null : IsfClientState.recipes().get(entry.recipeId());
        detailSource.text(recipe == null ? "Bookmark" : recipe.recipeType().toString());
        populateIngredients(recipe);
        detailStats.text(recipeStats(recipe));
    }

    private void populateIngredients(IsfRecipeDefinition recipe) {
        detailIngredients.clearChildren();
        if (recipe == null) return;
        JsonElement value = recipe.parameters().get("ingredients");
        if (value == null || !value.isJsonArray()) return;
        for (JsonElement ingredient : value.getAsJsonArray()) {
            ItemStack stack = firstIngredient(ingredient);
            IsfItemIconWidget icon = new IsfItemIconWidget(stack);
            icon.layout(style -> style.size(16.0f, 16.0f).centerSelf().flexNone());
            detailIngredients.addChild(icon);
        }
    }

    private static ItemStack firstIngredient(JsonElement ingredient) {
        if (ingredient == null) return ItemStack.EMPTY;
        JsonElement idValue = ingredient;
        if (ingredient.isJsonArray()) {
            JsonArray alternatives = ingredient.getAsJsonArray();
            if (alternatives.isEmpty()) return ItemStack.EMPTY;
            idValue = alternatives.get(0);
        }
        if (!idValue.isJsonPrimitive()) return ItemStack.EMPTY;
        ResourceLocation id = ResourceLocation.tryParse(idValue.getAsString());
        if (id == null) return ItemStack.EMPTY;
        Item item = BuiltInRegistries.ITEM.get(id);
        return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }

    private static String recipeStats(IsfRecipeDefinition recipe) {
        if (recipe == null) return "";
        JsonElement time = recipe.parameters().get("cooking_time");
        JsonElement experience = recipe.parameters().get("experience");
        if (time == null && experience == null) return recipe.id().toString();
        StringBuilder text = new StringBuilder();
        if (time != null) text.append("Time: ").append(time.getAsInt()).append(" t");
        if (experience != null) {
            if (!text.isEmpty()) text.append("  ");
            text.append("XP: ").append(experience.getAsFloat());
        }
        return text.toString();
    }

    private List<ItemEntry> entries() {
        Set<ResourceLocation> bookmarks = IsfClientState.bookmarks();
        Map<ResourceLocation, ItemEntry> indexed = new LinkedHashMap<>();

        // Правая панель является полным каталогом предметов Minecraft и всех модов.
        // Поэтому её содержимое не зависит от того, открыты ли у игрока ISF-рецепты.
        for (Item item : BuiltInRegistries.ITEM) {
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);
            if (item == Items.AIR || itemId == null) continue;
            indexed.put(itemId, new ItemEntry(itemId, new ItemStack(item), null));
        }

        IsfClientState.recipeResults().forEach((recipeId, itemId) -> {
            Item item = BuiltInRegistries.ITEM.get(itemId);
            if (item == Items.AIR) return;
            ItemEntry current = indexed.get(itemId);
            if (current == null || current.recipeId() == null) {
                indexed.put(itemId, new ItemEntry(itemId, new ItemStack(item), recipeId));
            }
        });
        for (ResourceLocation bookmark : bookmarks) {
            Item item = BuiltInRegistries.ITEM.get(bookmark);
            if (item != Items.AIR) indexed.putIfAbsent(bookmark,
                    new ItemEntry(bookmark, new ItemStack(item), null));
        }

        List<ItemEntry> entries = new ArrayList<>(indexed.values());
        entries.removeIf(entry -> entry.stack().isEmpty());
        entries.sort(Comparator.comparing(entry -> entry.id().toString()));
        return entries;
    }

    private record ItemEntry(ResourceLocation id, ItemStack stack, ResourceLocation recipeId) {
    }

    private final class BrowserRoot extends Box {
        @Override
        public void tick(FrameContext frame) {
            super.tick(frame);
            syncPanelBounds();
            if (observedStateVersion != IsfClientState.version()) {
                if (!observedRecipeResults.equals(IsfClientState.recipeResults())) {
                    rebuild(false, 0.0f);
                } else {
                    syncBookmarks();
                    if (selectedEntry != null) updateDetail(selectedEntry);
                    observedStateVersion = IsfClientState.version();
                }
            }
            if (restoreScroll) {
                itemScroll.scrollTo(0.0f, savedScrollY);
                if (savedScrollY <= 0.0f || itemScroll.maxScrollY() > 0.0f) restoreScroll = false;
            }
        }

        private void syncPanelBounds() {
            Screen screen = net.minecraft.client.Minecraft.getInstance().screen;
            if (!(screen instanceof AbstractContainerScreen<?> container)) return;
            int width = screen.width;
            int height = screen.height;
            int margin = 8;
            int guiLeft = container.getGuiLeft();
            int guiRight = Math.min(width - margin, guiLeft + imageWidth(container));
            int leftWidth = Math.max(0, guiLeft - margin * 2);
            int rightX = Math.min(width - margin, guiRight + margin);
            int rightWidth = Math.max(0, width - rightX - margin);
            int panelHeight = Math.max(0, height - margin * 2);
            bookmarkPanel.layout(style -> style.left(margin).top(margin).size(leftWidth, panelHeight));
            browserPanel.layout(style -> style.left(rightX).top(margin).size(rightWidth, panelHeight));
            int bookmarkContentWidth = Math.max(0, leftWidth - 8);
            int bookmarkColumns = Math.max(1, (int) (bookmarkContentWidth / CELL));
            if (bookmarkGrid.columns() != bookmarkColumns) {
                bookmarkGrid.columns(bookmarkColumns);
                int rows = (bookmarkCells.size() + bookmarkColumns - 1) / bookmarkColumns;
                bookmarkScroll.contentHeight(rows * CELL);
            }
            if (selectedEntry != null) {
                int detailWidth = Math.min(220, Math.max(140, width - margin * 2));
                int detailHeight = 150;
                detailPanel.layout(style -> style.left((width - detailWidth) * 0.5f)
                        .top((height - detailHeight) * 0.5f)
                        .size(detailWidth, detailHeight));
            }
        }

        private int imageWidth(AbstractContainerScreen<?> screen) {
            try {
                Field field = AbstractContainerScreen.class.getDeclaredField("imageWidth");
                field.setAccessible(true);
                return Math.max(0, field.getInt(screen));
            } catch (ReflectiveOperationException ignored) {
                return 176;
            }
        }
    }

    private static boolean contains(dev.sixik.unigui.api.math.RectView bounds, float x, float y) {
        return x >= bounds.x() && x <= bounds.x() + bounds.width()
                && y >= bounds.y() && y <= bounds.y() + bounds.height();
    }

    private static boolean containsExpanded(dev.sixik.unigui.api.math.RectView bounds,
                                            float x, float y, float expansion) {
        return x >= bounds.x() - expansion && x <= bounds.x() + bounds.width() + expansion
                && y >= bounds.y() && y <= bounds.y() + bounds.height();
    }
}
