package dev.sixik.isf.client;

import com.google.gson.JsonElement;
import dev.sixik.isf.IsfMod;
import dev.sixik.isf.client.widgets.IconButton;
import dev.sixik.isf.client.widgets.NineSliceHBox;
import dev.sixik.isf.client.widgets.NineSliceVBox;
import dev.sixik.isf.network.IsfNetwork;
import dev.sixik.isf.definition.IsfCatalystDefinition;
import dev.sixik.isf.definition.IsfRecipeDefinition;
import dev.sixik.isf.runtime.IsfRecipePaging;
import dev.sixik.isf.runtime.IsfRecipeQueryMatcher;
import dev.sixik.unigui.api.core.FrameContext;
import dev.sixik.unigui.api.event.PointerEnteredEvent;
import dev.sixik.unigui.api.event.PointerExitedEvent;
import dev.sixik.unigui.api.layout.*;
import dev.sixik.unigui.api.render.TextureOptions;
import dev.sixik.unigui.backend.minecraft_impl.*;
import dev.sixik.unigui.widgets.containers.Box;
import dev.sixik.unigui.widgets.containers.GridBox;
import dev.sixik.unigui.widgets.containers.HBox;
import dev.sixik.unigui.widgets.containers.ScrollView;
import dev.sixik.unigui.widgets.containers.VBox;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.widgets.feedback.OverlayLayer;
import dev.sixik.unigui.widgets.interaction.Button;
import dev.sixik.unigui.widgets.interaction.ScrollBar;
import dev.sixik.unigui.widgets.interaction.TextField;
import dev.sixik.unigui.widgets.interaction.ToggleButton;
import dev.sixik.unigui.widgets.minecraft.MinecraftItemTooltip;
import dev.sixik.unigui.widgets.minecraft.MinecraftZLayer;
import dev.sixik.unigui.api.widget.Widget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Locale;
import java.lang.reflect.Field;

/** UniGUI screen-overlay с вертикально перелистываемой сеткой доступных предметов. */
final class IsfBrowserOverlay {
    private static final float CELL = 18.0f;
    private static final float PANEL_PADDING = 4.0f;

    private final BrowserRoot contentRoot = new BrowserRoot();
    private final OverlayLayer overlayRoot = new OverlayLayer(contentRoot);
    private final Box bookmarkPanel = panelShell();
    private final Box browserPanel = panelShell();
    private final GridBox grid = new GridBox().columns(1);
    private final ScrollView itemScroll = new ScrollView(grid);
    private final GridBox bookmarkGrid = new GridBox().columns(4);
    private final ScrollView bookmarkScroll = new ScrollView(bookmarkGrid);
    private final Box detailPanel = new Box();
    private final MinecraftZLayer detailLayer = new MinecraftZLayer(detailPanel, 300.0f);
    private final Label detailTitle = new Label();
    private final ToggleButton detailPin = new ToggleButton();
    private final Button detailClose = new Button();
    private final NineSliceHBox tabRow = new NineSliceHBox();
    /** Горизонтальный скролл вкладок RecipeType: заменяет стрелку-кнопку. */
    private final ScrollView tabScroll = new ScrollView(tabRow);
    private final Label detailPage = new Label();
    private final Button detailPrevious = new Button();
    private final Button detailNext = new Button();
    private final TextField search = new TextField();
    private final HBox pagerRow = new HBox();
    private final NineSliceVBox catalystColumn = new NineSliceVBox();
    private final NineSliceVBox recipeArea = new NineSliceVBox();
    private final List<MinecraftItemTooltip> tooltips = new ArrayList<>();
    /** Ленивые тултипы клеток каталога: создаются при первом наведении, а не все сразу. */
    private final Map<Button, MinecraftItemTooltip> catalogTooltips = new LinkedHashMap<>();
    private final List<MinecraftItemTooltip> tabTooltips = new ArrayList<>();
    private final List<MinecraftItemTooltip> catalystTooltips = new ArrayList<>();
    private final List<MinecraftItemTooltip> recipeItemTooltips = new ArrayList<>();
    private final List<IsfItemButton> recipeItemButtons = new ArrayList<>();
    private final List<IsfTransferButton> transferButtons = new ArrayList<>();
    private final List<Widget> pageVisuals = new ArrayList<>();
    private boolean recipeItemButtonsPopulated;
    private final Map<ResourceLocation, Button> bookmarkCells = new LinkedHashMap<>();
    private final Map<ResourceLocation, MinecraftItemTooltip> bookmarkTooltips = new LinkedHashMap<>();
    private final Map<ResourceLocation, Button> itemCells = new LinkedHashMap<>();
    private String browserFilter = "";
    /** Предметы, у которых есть рецепты/применения/станция: остальные в каталоге скрыты. */
    private Set<ResourceLocation> itemsWithContent = Set.of();
    private long contentIndexVersion = Long.MIN_VALUE;
    /** Последняя применённая высота tabScroll; -1 — ещё не применялась. */
    private float appliedTabScrollHeight = -1.0f;

    private MinecraftRenderLayerRegistration<Screen> registration;
    private AutoCloseable pointerBlocker;
    private List<ItemEntry> catalogEntries = List.of();
    private Map<ResourceLocation, ResourceLocation> observedRecipeResults = Map.of();
    private ItemEntry hoveredEntry;
    private ItemEntry selectedEntry;
    private PendingRecipeQuery pendingRecipeQuery;
    private long observedStateVersion = Long.MIN_VALUE;
    private float savedScrollY;
    private boolean restoreScroll;
    private boolean itemScrollBarDragging;
    private boolean bookmarkScrollBarDragging;
    private boolean detailPinned;
    private boolean detailPositionSet;
    private boolean detailDragging;
    private float detailLeft;
    private float detailTop;
    private float detailDragOffsetX;
    private float detailDragOffsetY;
    private ResourceLocation selectedTypeId;
    private ResourceLocation selectedCatalyst;
    private int recipePage;
    private List<TypeTab> typeTabs = List.of();
    private List<CatalystCell> catalystCells = List.of();
    private List<RecipeView> builtRecipes = List.of();
    private List<List<Integer>> recipePages = List.of();
    private float pageAreaHeight = -1.0f;
    private double pointerX = -1.0;
    private double pointerY = -1.0;
    /** Запрос (R/U), которым открыто текущее окно; null — окно открыто кликом по каталогу. */
    private PendingRecipeQuery selectedQuery;

    private static final float RECIPE_GAP = 2.0f;
    private static final float CATALYST_CELL = 18.0f;
    private static final float TAB_CELL = 20.0f;
    /** Высота строки вкладок: padding 1 сверху и снизу + вкладка 20. */
    private static final float TAB_ROW_CONTENT = TAB_CELL + 2.0f;
    /** Толщина горизонтального scrollbar вкладок: ~1.5x уже дефолтной полосы. */
    private static final float TAB_SCROLLBAR_SIZE = 4.0f;
    /** Резерв под scrollbar вкладок: толщина полосы + gap. */
    private static final float TAB_ROW_RESERVED = TAB_SCROLLBAR_SIZE + 1.0f;
    private static final float FALLBACK_RECIPE_HEIGHT = 92.0f;
    /** padding(4)*2 + заголовок 16 + вкладки 20+2 + пагинация 14 + три отступа VBox (без scrollbar). */
    private static final float DETAIL_FIXED_HEIGHT =
            8.0f + 16.0f + TAB_ROW_CONTENT + 14.0f + 3 * RECIPE_GAP;

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
        if (pointerBlocker == null) {
            pointerBlocker = ScreenOverlayRender.addPointerBlocker(this::blocksVanillaPointer);
        }
    }

    /**
     * Пока окно рецептов открыто, ванильные слоты под ним не должны реагировать
     * на курсор: не подсвечиваться, не показывать tooltip и не принимать клики.
     */
    private boolean blocksVanillaPointer(Screen ignoredScreen, double mouseX, double mouseY) {
        return (detailPanel.visibility() == dev.sixik.unigui.api.widget.Visibility.VISIBLE
                && contains(detailPanel.layoutBounds(), (float) mouseX, (float) mouseY))
                || (browserPanel.visibility() == dev.sixik.unigui.api.widget.Visibility.VISIBLE
                && contains(browserPanel.layoutBounds(), (float) mouseX, (float) mouseY))
                || (bookmarkPanel.visibility() == dev.sixik.unigui.api.widget.Visibility.VISIBLE
                && contains(bookmarkPanel.layoutBounds(), (float) mouseX, (float) mouseY));
    }

    void resetPage() {
        hoveredEntry = null;
        pendingRecipeQuery = null;
        itemScrollBarDragging = false;
        bookmarkScrollBarDragging = false;
        detailDragging = false;
        if (!detailPinned) {
            selectedEntry = null;
            detailPositionSet = false;
            detailPanel.visibility(dev.sixik.unigui.api.widget.Visibility.COLLAPSED);
        } else if (selectedEntry != null) {
            detailPanel.visibility(dev.sixik.unigui.api.widget.Visibility.VISIBLE);
        }
    }

    boolean toggleHoveredBookmark() {
        ItemEntry entry = hoveredEntry;
        return entry != null && IsfItemTriggerUtils.toggleBookmark(entry.id());
    }

    /** Закладка по id: используется триггерами R/U/A (клавиша A). */
    boolean toggleBookmark(ResourceLocation itemId) {
        return IsfItemTriggerUtils.toggleBookmark(itemId);
    }

    /**
     * Предмет под курсором для R/U/A-триггеров.
     * Порядок: визуалы рецептов → катализаторы окна → hovered-клетка каталога →
     * клетки каталога/закладок (только внутри viewport'ов скролла).
     * Слоты инвентаря добавляет {@link IsfItemTriggerUtils#resolveItemAt}.
     */
    ResourceLocation triggerItemAt(double mouseX, double mouseY) {
        if (mouseX < 0.0 || mouseY < 0.0) return null;
        float x = (float) mouseX;
        float y = (float) mouseY;
        ResourceLocation itemId = recipeItemAt(mouseX, mouseY);
        if (itemId == null
                && detailPanel.visibility() == dev.sixik.unigui.api.widget.Visibility.VISIBLE) {
            for (CatalystCell cell : catalystCells) {
                if (contains(cell.button().layoutBounds(), x, y)) {
                    itemId = cell.itemId();
                    break;
                }
            }
        }
        if (itemId == null) itemId = hoveredItemId();
        if (itemId == null && contains(itemScroll.layoutBounds(), x, y)) {
            itemId = cellIdAt(itemCells, x, y);
        }
        if (itemId == null && contains(bookmarkScroll.layoutBounds(), x, y)) {
            itemId = cellIdAt(bookmarkCells, x, y);
        }
        return itemId;
    }

    ResourceLocation hoveredItemId() {
        return hoveredEntry == null ? null : hoveredEntry.id();
    }

    /**
     * Предмет из сетки крафта под курсором: позволяет смотреть рецепты R/U
     * прямо из визуала рецепта, не выходя из окна.
     */
    ResourceLocation recipeItemAt() {
        if (pointerX < 0.0 || pointerY < 0.0) return null;
        return recipeItemAt(pointerX, pointerY);
    }

    ResourceLocation recipeItemAt(double mouseX, double mouseY) {
        ensureRecipeItemButtons();
        float x = (float) mouseX;
        float y = (float) mouseY;
        for (IsfItemButton itemButton : recipeItemButtons) {
            if (contains(itemButton.layoutBounds(), x, y)) {
                return itemButton.itemId();
            }
        }
        return null;
    }

    /** Запоминает координаты курсора из Forge mouse-событий для R/U по сетке. */
    void updatePointerPosition(double mouseX, double mouseY) {
        pointerX = mouseX;
        pointerY = mouseY;
    }

    /** Трекаемая X-координата курсора (GUI-пространство); -1 — ещё не известна. */
    double pointerX() {
        return pointerX;
    }

    /** Трекаемая Y-координата курсора (GUI-пространство); -1 — ещё не известна. */
    double pointerY() {
        return pointerY;
    }

    void showRecipes(ResourceLocation itemId, boolean usages) {
        if (itemId == null) return;
        pendingRecipeQuery = new PendingRecipeQuery(itemId, usages, IsfClientState.version());
        showRecipeQuery(pendingRecipeQuery);
        IsfNetwork.requestRecipes(itemId, usages);
    }

    /**
     * Обрабатывает навигацию как совместимый резервный путь для Minecraft screen hooks.
     * Панель рецепта рисуется в отдельном Z-слое, а Forge одновременно передаёт координаты
     * мыши базовому экрану. Проверка на границе overlay не зависит от его маршрутизации ввода.
     */
    boolean clickRecipeNavigation(double mouseX, double mouseY, int button) {
        if (button != 0 || selectedEntry == null || recipePages.size() < 2
                || detailPanel.visibility() != dev.sixik.unigui.api.widget.Visibility.VISIBLE) {
            return false;
        }
        float x = (float) mouseX;
        float y = (float) mouseY;
        if (contains(detailPrevious.layoutBounds(), x, y)) {
            changeRecipePage(-1);
            return true;
        }
        if (contains(detailNext.layoutBounds(), x, y)) {
            changeRecipePage(1);
            return true;
        }
        return false;
    }

    /**
     * Резервная обработка кликов по элементам окна рецептов: закрепление, закрытие,
     * вкладки RecipeType, стрелка прокрутки вкладок, пагинация и катализаторы.
     * Кнопки живут в Z-слое, куда маршрут ввода Minecraft-хуков доходит не всегда,
     * поэтому состояние проверяем прямо по layout-границам кнопок.
     */
    boolean clickDetailControls(double mouseX, double mouseY, int button) {
        if ((button != 0 && button != 1) || selectedEntry == null
                || detailPanel.visibility() != dev.sixik.unigui.api.widget.Visibility.VISIBLE) {
            return false;
        }
        float x = (float) mouseX;
        float y = (float) mouseY;
        // ПКМ по предмету в крафте — применения (U). Катализаторы — тоже:
        // ЛКМ остаётся переключением фильтра станции, ПКМ показывает usages.
        if (button == 1) {
            ensureRecipeItemButtons();
            for (IsfItemButton itemButton : recipeItemButtons) {
                if (contains(itemButton.layoutBounds(), x, y)) {
                    showRecipes(itemButton.itemId(), true);
                    return true;
                }
            }
            for (CatalystCell cell : catalystCells) {
                if (contains(cell.button().layoutBounds(), x, y)) {
                    showRecipes(cell.itemId(), true);
                    return true;
                }
            }
            return false;
        }
        if (contains(detailClose.layoutBounds(), x, y)) {
            closeDetail();
            return true;
        }
        if (contains(detailPin.layoutBounds(), x, y)) {
            setDetailPinned(!detailPinned);
            return true;
        }
        for (TypeTab tab : typeTabs) {
            if (tab.button().visibility() == dev.sixik.unigui.api.widget.Visibility.VISIBLE
                    && contains(tab.button().layoutBounds(), x, y)) {
                selectType(tab.typeId());
                return true;
            }
        }
        if (recipePages.size() > 1 && contains(detailPrevious.layoutBounds(), x, y)) {
            changeRecipePage(-1);
            return true;
        }
        if (recipePages.size() > 1 && contains(detailNext.layoutBounds(), x, y)) {
            changeRecipePage(1);
            return true;
        }
        for (CatalystCell cell : catalystCells) {
            if (contains(cell.button().layoutBounds(), x, y)) {
                toggleCatalyst(cell.itemId());
                return true;
            }
        }
        // Предметы в крафтах: ЛКМ — крафты (R).
        ensureRecipeItemButtons();
        for (IsfItemButton itemButton : recipeItemButtons) {
            if (contains(itemButton.layoutBounds(), x, y)) {
                showRecipes(itemButton.itemId(), false);
                return true;
            }
        }
        return false;
    }

    private void setDetailPinned(boolean pinned) {
        detailPinned = pinned;
        detailPin.silentChecked(pinned);
    }

    boolean beginDetailDrag(double mouseX, double mouseY, int button) {
        if (button != 0 || selectedEntry == null
                || detailPanel.visibility() != dev.sixik.unigui.api.widget.Visibility.VISIBLE
                || !contains(detailTitle.parent() == null ? detailTitle.layoutBounds()
                : detailTitle.parent().layoutBounds(), (float) mouseX, (float) mouseY)
                || contains(detailPin.layoutBounds(), (float) mouseX, (float) mouseY)
                || contains(detailClose.layoutBounds(), (float) mouseX, (float) mouseY)) {
            return false;
        }
        detailDragging = true;
        detailDragOffsetX = (float) mouseX - detailPanel.layoutBounds().x();
        detailDragOffsetY = (float) mouseY - detailPanel.layoutBounds().y();
        return true;
    }

    boolean dragDetail(double mouseX, double mouseY, int button) {
        if (button != 0 || !detailDragging) return false;
        moveDetailPanel((float) mouseX - detailDragOffsetX,
                (float) mouseY - detailDragOffsetY);
        return true;
    }

    boolean endDetailDrag(int button) {
        if (button != 0 || !detailDragging) return false;
        detailDragging = false;
        return true;
    }

    private void closeDetail() {
        setDetailPinned(false);
        detailDragging = false;
        pendingRecipeQuery = null;
        selectedQuery = null;
        selectedEntry = null;
        detailPositionSet = false;
        selectedTypeId = null;
        selectedCatalyst = null;
        recipePage = 0;
        typeTabs = List.of();
        catalystCells = List.of();
        builtRecipes = List.of();
        recipePages = List.of();
        tabRow.clearChildren();
        tabScroll.scrollTo(0.0f, 0.0f);
        catalystColumn.clearChildren();
        recipeArea.clearChildren();
        clearDetailTooltips();
        clearRecipeItemTooltips();
        detailPanel.visibility(dev.sixik.unigui.api.widget.Visibility.COLLAPSED);
    }

    private void moveDetailPanel(float left, float top) {
        float panelWidth = detailPanel.layoutStyle().width().value();
        float panelHeight = detailPanel.layoutStyle().height().value();
        if (panelWidth <= 0.0f) panelWidth = detailPanel.layoutBounds().width();
        if (panelHeight <= 0.0f) panelHeight = detailPanel.layoutBounds().height();
        if (panelWidth <= 0.0f) panelWidth = 206.0f;
        if (panelHeight <= 0.0f) panelHeight = 140.0f;
        moveDetailPanel(left, top, panelWidth, panelHeight);
    }

    private void moveDetailPanel(float left, float top, float panelWidth, float panelHeight) {
        Screen screen = net.minecraft.client.Minecraft.getInstance().screen;
        float width = screen == null ? panelWidth : screen.width;
        float height = screen == null ? panelHeight : screen.height;
        float margin = 4.0f;
        float maxLeft = Math.max(margin, width - panelWidth - margin);
        float maxTop = Math.max(margin, height - panelHeight - margin);
        detailPositionSet = true;
        detailLeft = Math.max(margin, Math.min(maxLeft, left));
        detailTop = Math.max(margin, Math.min(maxTop, top));
        detailPanel.layout(style -> style
                .left(detailLeft)
                .top(detailTop));
    }

    private void showRecipeQuery(PendingRecipeQuery query) {
        List<ResourceLocation> recipeIds = queryRecipeIds(query.itemId(), query.usages());
        // Если у предмета нет доступных рецептов — окно вообще не открываем.
        if (recipeIds.isEmpty()) return;
        // Запоминаем запрос: refreshSelectedDetail обновляет окно строго им.
        selectedQuery = query;
        Item item = BuiltInRegistries.ITEM.get(query.itemId());
        updateDetail(new ItemEntry(query.itemId(), new ItemStack(item), recipeIds));
    }

    private List<ResourceLocation> queryRecipeIds(ResourceLocation itemId, boolean usages) {
        return IsfClientState.recipes().values().stream()
                .filter(recipe -> matchesQuery(recipe, itemId, usages))
                .map(IsfRecipeDefinition::id)
                .toList();
    }

    private static boolean matchesQuery(IsfRecipeDefinition recipe,
                                        ResourceLocation itemId,
                                        boolean usages) {
        return IsfRecipeQueryMatcher.matches(recipe.parameters(), recipe.triggers(), itemId, usages);
    }

    /**
     * Открывает окно с конкретными рецептами (пуш с сервера, вне R/U-запроса).
     * Неизвестные клиенту id молча пропускаются (библиотека едет тем же каналом).
     */
    void showExplicitRecipes(List<ResourceLocation> recipeIds) {
        if (recipeIds == null || recipeIds.isEmpty()) return;
        List<ResourceLocation> known = new ArrayList<>();
        for (ResourceLocation recipeId : recipeIds) {
            if (recipeId != null && IsfClientState.recipes().containsKey(recipeId)
                    && !known.contains(recipeId)) {
                known.add(recipeId);
            }
        }
        if (known.isEmpty()) return;
        // Явный показ не привязан к запросу предмета: обновление идёт только им.
        selectedQuery = null;
        updateDetail(new ItemEntry(known.get(0), titleStackFor(
                IsfClientState.recipes().get(known.get(0))), known));
    }

    /** Стак для заголовка окна: результат, иначе source_item, иначе пусто. */
    private static ItemStack titleStackFor(IsfRecipeDefinition recipe) {
        if (recipe != null) {
            ResourceLocation result = recipe.resultItemId();
            ItemStack stack = stackFor(result);
            if (!stack.isEmpty()) return stack;
            com.google.gson.JsonElement source = recipe.parameters().get("source_item");
            if (source != null && source.isJsonPrimitive()) {
                stack = stackFor(ResourceLocation.tryParse(source.getAsString()));
                if (!stack.isEmpty()) return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private static ItemStack stackFor(ResourceLocation itemId) {
        if (itemId == null) return ItemStack.EMPTY;
        Item item = BuiltInRegistries.ITEM.get(itemId);
        return item == null || item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }

    boolean scrollItemsAt(double mouseX, double mouseY, double delta) {
        if (delta == 0.0) {
            return false;
        }
        // Над вкладками RecipeType колесо листает сами вкладки по горизонтали,
        // если они не помещаются в ширину окна.
        if (contains(tabScroll.layoutBounds(), (float) mouseX, (float) mouseY)
                && tabScroll.maxScrollX() > 0.0f) {
            return scrollViewX(tabScroll, delta);
        }
        // Над окном рецептов колесо листает страницы: вверх — назад, вниз — вперёд.
        if (detailPanel.visibility() == dev.sixik.unigui.api.widget.Visibility.VISIBLE
                && contains(detailPanel.layoutBounds(), (float) mouseX, (float) mouseY)) {
            if (recipePages.size() > 1) {
                changeRecipePage(delta > 0.0 ? -1 : 1);
            }
            return true;
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

    /** Горизонтальный аналог {@link #scrollViewAt}: колесо вверх — влево, вниз — вправо. */
    private boolean scrollViewX(ScrollView scroll, double delta) {
        float before = scroll.scrollX();
        scroll.scrollBy((float) (-delta * scroll.scrollStep()), 0.0f);
        return before != scroll.scrollX();
    }

    /** {@code true}, если вкладки не помещаются по ширине и scrollbar показан. */
    private boolean tabScrollBarVisible() {
        return tabScroll.maxScrollX() > 0.0f;
    }

    /**
     * Фиксированная высота окна рецептов: базовая часть плюс резерв под
     * scrollbar вкладок, но только когда он реально показан.
     */
    private float detailFixedHeight() {
        return DETAIL_FIXED_HEIGHT + (tabScrollBarVisible() ? TAB_ROW_RESERVED : 0.0f);
    }

    /**
     * Держит высоту строки вкладок точной: без scrollbar — только вкладки,
     * с scrollbar — вкладки + резерв под полосу. Вызывается каждый тик,
     * применяется только при реальном изменении.
     */
    private void syncTabScrollHeight() {
        float desired = TAB_ROW_CONTENT + (tabScrollBarVisible() ? TAB_ROW_RESERVED : 0.0f);
        if (Math.abs(desired - appliedTabScrollHeight) < 0.01f) return;
        appliedTabScrollHeight = desired;
        tabScroll.layout(style -> style.height(desired));
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

    //Правая панель(список всех Items)
    private void configureTree() {
        overlayRoot.layout(style -> style.fill());
        contentRoot.themeEnabled(false);
        contentRoot.backgroundVisible(false);
        contentRoot.borderVisible(false);
        contentRoot.layout(style -> style.fill());

        Box panel = browserPanel;
        panel.layout(style -> style
                .position(PositionType.ABSOLUTE)
                .right(2.0f)
                .padding(PANEL_PADDING)
                .overflow(Overflow.HIDDEN)
        );
        VBox column = new VBox();
        column.spacing(2.0f);
        column.layout(style -> style.fill());
        grid.spacing(0.0f);
        grid.layout(style -> style.widthPercent(100.0f).flexNone());

        itemScroll.scrollStep(18.0f);
        itemScroll.scrollbarGap(1);
        itemScroll.layout(style -> style.widthPercent(100.0f).flexGrow(1.0f).flexShrink(1.0f));

        search.themeEnabled(false);
        search.boxVisualEnabled(true);
        search.backgroundVisible(false);
        search.borderVisible(false);
        search.renderer(new NineSliceBoxRenderer(
                new MinecraftTextureHandle(
                        ResourceLocation.tryBuild(IsfMod.MOD_ID, "textures/jei/atlas/gui/search_background_v2.png"),
                        20, 20, TextureOptions.nearest()),
                6.0f));
        search.layout(style -> style
                .widthPercent(100.0f)
                .maxWidthPercent(100.0f)
                .height(20.0f)
                .padding(6.0f, 0.0f)
                .flexShrink(0.0f)
        );
        search.onTextChanged(event -> {
            browserFilter = event.newText() == null ? "" : event.newText().trim();
            applyBrowserFilter();
        });

        column.addChild(itemScroll);
        column.addChild(search);
        panel.borderVisible(false);
        panel.backgroundVisible(false);
        panel.addChild(column);
        contentRoot.addChild(panel);

        configureBookmarksPanel();
        configureDetailPanel();
        rebuild(false, 0.0f);
    }

    //Левая панель(Избранные рецепты)
    private void configureBookmarksPanel() {
        Box panel = bookmarkPanel;
        panel.layout(style -> style.position(PositionType.ABSOLUTE)
                .left(8.0f).top(8.0f).size(96.0f, 200.0f).padding(4.0f));
        panel.backgroundVisible(false);
        panel.borderVisible(false);
        bookmarkGrid.spacing(0.0f);
        bookmarkGrid.layout(style -> style.widthPercent(100.0f).flexNone());
        bookmarkScroll.scrollStep(CELL);
        bookmarkScroll.layout(style -> style.widthPercent(100.0f).flexGrow(1.0f).flexShrink(1.0f));
        VBox column = new VBox();
        column.spacing(2.0f);
        column.layout(style -> style.fill());
        column.addChild(bookmarkScroll);
        panel.addChild(column);
        contentRoot.addChild(panel);
    }

    //Панель содержащая рецепты выбранного предмета
    private void configureDetailPanel() {
        detailPanel.themeEnabled(false);
        detailPanel.backgroundVisible(false);
        detailPanel.borderVisible(false);
        detailPanel.renderer(new NineSliceBoxRenderer(
                new MinecraftTextureHandle(
                        ResourceLocation.tryBuild(IsfMod.MOD_ID, "textures/jei/atlas/gui/recipe_preview_background_v2.png"),
                        64, 64, TextureOptions.nearest()),
                4.0f));
        detailPanel.layout(style -> style
                .position(PositionType.ABSOLUTE)
                .left(104.0f)
                .top(4.0f)
                .size(206.0f, 140.0f)
                .padding(4.0f));

        VBox content = new VBox();
        content.spacing(2.0f);
        content.layout(style -> style.fill());

        HBox detailHeader = new HBox();
        detailHeader.spacing(2.0f);
        detailHeader.layout(style -> style.widthPercent(100.0f).height(16.0f).flexNone());
        detailTitle.layout(style -> style.flexGrow(1.0f).flexShrink(1.0f));
        detailTitle.background(0.5f,0.5f,0.5f,0.5f);
        detailTitle.color().set(0,0,0,1);
        detailPin.renderer(detailButtonRenderer());
        detailPin.text("P").textPadding(0.0f, 0.0f);
        detailPin.layout(style -> style.size(16.0f, 16.0f).flexNone());
        detailPin.onCheckedChanged(event -> setDetailPinned(event.newValue()));
        detailClose.renderer(detailButtonRenderer());
        detailClose.themeEnabled(false);
        detailClose.text("X").textPadding(0.0f, 0.0f);
        detailClose.layout(style -> style.size(16.0f, 16.0f).flexNone());
        detailClose.onClick(event -> closeDetail());
        detailHeader.addChild(detailTitle);
        detailHeader.addChild(detailPin);
        detailHeader.addChild(detailClose);

        // Зелёная зона: вкладки доступных RecipeType. Все вкладки строятся целиком,
        // а переполнение по ширине обслуживает горизонтальный scrollbar (без стрелки).
        tabRow.backgroundRenderer(new NineSliceBoxRenderer(new MinecraftTextureHandle(ResourceLocation.tryBuild(IsfMod.MOD_ID, "textures/jei/atlas/gui/scrollbar_background_v2.png"),
                20, 20, TextureOptions.nearest()),
                4.0f));
        tabRow.layout(style -> style
                .width(SizeValue.auto())
                .flexNone()
                .padding(1)
                .alignSelf(Align.START)
        );
        tabScroll.scrollStep(TAB_CELL + 2.0f);
        tabScroll.scrollbarGap(1);
        tabScroll.scrollbarSize(TAB_SCROLLBAR_SIZE);
        tabScroll.layout(style -> style
                .widthPercent(100.0f)
                .height(TAB_ROW_CONTENT)
                .flexNone()
                .overflowX(Overflow.AUTO)
                .overflowY(Overflow.HIDDEN)
        );

        // Кнопки страниц находятся под вкладками RecipeType, как в оригинальном JEI.
        pagerRow.layout(style -> style.widthPercent(100.0f).height(14.0f).flexNone()
                .justifyContent(Justify.CENTER));
        detailPage.layout(style -> style.width(48.0f).height(14.0f).flexNone().horizontalAlignment(Alignment.CENTER));
        detailPage.background(0.5f,0.5f,0.5f,0.5f);
        detailPrevious.renderer(detailButtonRenderer());
        detailPrevious.themeEnabled(false);
        detailPrevious.textColor(1.0f, 1.0f, 1.0f, 1.0f);
        detailPrevious.text("<").textPadding(0.0f, 0.0f);
        detailPrevious.layout(style -> style.size(16.0f, 14.0f).flexNone());
        detailPrevious.onClick(event -> changeRecipePage(-1));
        detailNext.renderer(detailButtonRenderer());
        detailNext.themeEnabled(false);
        detailNext.textColor(1.0f, 1.0f, 1.0f, 1.0f);
        detailNext.text(">").textPadding(0.0f, 0.0f);
        detailNext.layout(style -> style.size(16.0f, 14.0f).flexNone());
        detailNext.onClick(event -> changeRecipePage(1));
        pagerRow.addChild(detailPrevious);
        pagerRow.addChild(detailPage);
        pagerRow.addChild(detailNext);

        // Тело: красная колонка катализаторов + синяя зона рецептов (страницами).
        HBox body = new HBox();
        body.spacing(2.0f);
        body.layout(style -> style.widthPercent(100.0f).flexGrow(1.0f).flexShrink(1.0f));
        catalystColumn.spacing(RECIPE_GAP);
        catalystColumn.layout(style -> style
                .width(SizeValue.auto())
                .flexNone()
                .padding(2)
                .alignSelf(Align.START)
        );
        catalystColumn.backgroundRenderer(new NineSliceBoxRenderer(
                new MinecraftTextureHandle(
                        ResourceLocation.tryBuild(IsfMod.MOD_ID, "textures/jei/atlas/gui/scrollbar_background_v2.png"),
                        14, 50, TextureOptions.nearest()),
                4.0f));
        recipeArea.spacing(RECIPE_GAP);
        recipeArea.layout(style -> style.widthPercent(100.0f).flexGrow(1.0f).flexShrink(1.0f).alignItems(Align.CENTER));

        body.addChild(catalystColumn);
        body.addChild(recipeArea);

        content.addChild(detailHeader);
        content.addChild(tabScroll);
        content.addChild(pagerRow);
        content.addChild(body);
        detailPanel.addChild(content);
        overlayRoot.addOverlay(detailLayer);
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

    /** Кнопка окна рецептов с текстурами под состояния (hover/press/disabled). */
    private static IsfStateButtonRenderer detailButtonRenderer() {
        return new IsfStateButtonRenderer(
                nineSlice("textures/jei/atlas/gui/button_enabled_v2.png"),
                nineSlice("textures/jei/atlas/gui/button_highlight_v2.png"),
                nineSlice("textures/jei/atlas/gui/button_pressed_v2.png"),
                nineSlice("textures/jei/atlas/gui/button_pressed_highlight_v2.png"),
                nineSlice("textures/jei/atlas/gui/button_disabled_v2.png"));
    }

    private static NineSliceButtonRenderer nineSlice(String path) {
        return new NineSliceButtonRenderer(new MinecraftTextureHandle(
                ResourceLocation.tryBuild(IsfMod.MOD_ID, path),
                20, 20, TextureOptions.nearest()), 4.0f);
    }

    private void rebuild(boolean ignored, float ignoredOffset) {
        List<ItemEntry> entries = entries();
        catalogEntries = entries;
        savedScrollY = itemScroll.scrollY();
        restoreScroll = true;
        hoveredEntry = null;

        for (MinecraftItemTooltip tooltip : tooltips) overlayRoot.removeOverlay(tooltip);
        tooltips.clear();
        catalogTooltips.clear();
        bookmarkCells.clear();
        bookmarkTooltips.clear();
        itemCells.clear();
        grid.clearChildren();
        bookmarkGrid.clearChildren();

        for (ItemEntry entry : entries) {
            Button cell = itemCell(entry);
            itemCells.put(entry.id(), cell);
            grid.addChild(cell);
        }

        refreshBrowserFilter();

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

    /**
     * Инкрементальное обновление каталога после ответа сервера.
     * Набор клеток не меняется (каталог содержит все предметы игры), поэтому
     * клетки и тултипы не пересоздаются — панели не мерцают, как с закладками.
     */
    private void syncCatalog() {
        catalogEntries = entries();
        refreshBrowserFilter();
        syncBookmarks();
        refreshSelectedDetail();
        observedRecipeResults = IsfClientState.recipeResults();
        observedStateVersion = IsfClientState.version();
    }

    /** Обновляет окно только по тому запросу, которым оно было открыто. */
    private void refreshSelectedDetail() {
        if (selectedEntry == null || selectedQuery == null) return;
        List<ResourceLocation> freshIds = queryRecipeIds(
                selectedQuery.itemId(), selectedQuery.usages());
        if (selectedEntry.recipeIds().equals(freshIds)) return;
        updateDetail(new ItemEntry(selectedQuery.itemId(), selectedEntry.stack(), freshIds));
    }

    /** Актуальный список result-рецептов предмета (для клеток каталога). */
    private List<ResourceLocation> currentRecipeIds(ResourceLocation itemId) {
        List<ResourceLocation> recipeIds = new ArrayList<>();
        IsfClientState.recipeResults().forEach((recipeId, resultId) -> {
            if (itemId.equals(resultId)) recipeIds.add(recipeId);
        });
        return List.copyOf(recipeIds);
    }

    /**
     * ПКМ по клетке в списке предметов или закладках — применения (U),
     * а если предмет является катализатором (печь, верстак...) — окно станции
     * со всеми категориями этого блока.
     *
     * <p>Hit-проверка ограничена viewport'ом соответствующего ScrollView: клетки
     * за пределами видимой области прокрутки остаются в layout-дереве со своими
     * layout-границами (выше/ниже viewport'а), и без этой проверки клик по
     * «пустой» зоне над/под списком попадал в обрезанную клетку.</p>
     */
    boolean clickItemList(double mouseX, double mouseY, int button) {
        if (button != 1) return false;
        float x = (float) mouseX;
        float y = (float) mouseY;
        ResourceLocation itemId = null;
        if (contains(itemScroll.layoutBounds(), x, y)) {
            itemId = cellIdAt(itemCells, x, y);
        }
        if (itemId == null && contains(bookmarkScroll.layoutBounds(), x, y)) {
            itemId = cellIdAt(bookmarkCells, x, y);
        }
        if (itemId == null) return false;
        if (isCatalystItem(itemId)) {
            showStationRecipes(itemId);
            return true;
        }
        showRecipes(itemId, true);
        return true;
    }

    /** @return {@code true}, если предмет выступает катализатором хотя бы одного типа. */
    private boolean isCatalystItem(ResourceLocation itemId) {
        for (List<IsfCatalystDefinition> catalysts : IsfClientState.typeCatalysts().values()) {
            for (IsfCatalystDefinition catalyst : catalysts) {
                if (catalyst.item().equals(itemId)) return true;
            }
        }
        return false;
    }

    private ResourceLocation cellIdAt(Map<ResourceLocation, Button> cells, float x, float y) {
        for (Map.Entry<ResourceLocation, Button> entry : cells.entrySet()) {
            if (entry.getValue().visibility() == dev.sixik.unigui.api.widget.Visibility.COLLAPSED) continue;
            if (contains(entry.getValue().layoutBounds(), x, y)) return entry.getKey();
        }
        return null;
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
        updateBookmarkScrollContentHeight();
    }

    private void updateBookmarkScrollContentHeight() {
        if (bookmarkCells.isEmpty()) {
            bookmarkScroll.contentHeight(0.0f);
            bookmarkScroll.disableScrolling();
            return;
        }
        int columns = Math.max(1, bookmarkGrid.columns());
        int rows = (bookmarkCells.size() + columns - 1) / columns;
        bookmarkScroll.enableScrolling();
        bookmarkScroll.contentHeight(rows * CELL);
    }

    private void updateItemScrollContentHeight() {
        int columns = Math.max(1, grid.columns());
        int visible = 0;
        for (ItemEntry entry : catalogEntries) {
            Button cell = itemCells.get(entry.id());
            if (cell != null
                    && cell.visibility() != dev.sixik.unigui.api.widget.Visibility.COLLAPSED) {
                visible++;
            }
        }
        if (visible == 0) {
            itemScroll.contentHeight(0.0f);
            itemScroll.scrollTo(0.0f, 0.0f);
            return;
        }
        int rows = (visible + columns - 1) / columns;
        itemScroll.contentHeight(rows * CELL);
        float maxScroll = itemScroll.maxScrollY();
        if (itemScroll.scrollY() > maxScroll) itemScroll.scrollTo(0.0f, maxScroll);
    }

    /**
     * Фильтрует правую панель (browserPanel) по текущему тексту из поля поиска.
     *
     * <ul>
     *   <li>Обычный текст — подстрока в DisplayName предмета, в его RegistryID
     *   (namespace или path, регистр не важен) или в полном {@code namespace:path}.</li>
     *   <li>{@code @Text} — то же сравнение, но только по Mod ID (namespace предмета).</li>
     * </ul>
     * Клетки не пересоздаются: фильтр только переключает видимость существующих
     * клеток и тултипы следуют за ними (тултип виден только при наведении на якорь).
     */
    private void applyBrowserFilter() {
        refreshBrowserFilter();
        itemScroll.scrollTo(0.0f, 0.0f);
        updateItemScrollContentHeight();
    }

    /**
     * Применяет текущий фильтр без сброса скролла: используется после rebuild/sync,
     * где позиция скролла восстанавливается отдельно.
     *
     * <p>Клетка видна, только если предмет подходит под поиск И у него есть контент
     * (рецепты, применения или станция): предметы-ни-о-чём в каталоге скрыты.</p>
     */
    private void refreshBrowserFilter() {
        if (contentIndexVersion != IsfClientState.version()) {
            contentIndexVersion = IsfClientState.version();
            refreshContentIndex();
        }
        String query = browserFilter == null ? "" : browserFilter.trim();
        boolean modOnly = query.startsWith("@");
        String needle = (modOnly ? query.substring(1) : query)
                .trim()
                .toLowerCase(Locale.ROOT);

        if (hoveredEntry != null) {
            // Поиск перестраивает сетку (скролл в 0, видимость клеток): прежний hover
            // геометрии больше не соответствует. Гасим флаг клетки синтетическим exit
            // (иначе тултип/подсветка зависают: exit по скрытию не прилетает)
            // и саму запись — мышь доедет заново при движении.
            Button cell = itemCells.get(hoveredEntry.id());
            if (cell != null) {
                cell.handle(new PointerExitedEvent(cell, cell,
                        dev.sixik.unigui.api.event.EventPhase.TARGET,
                        0.0f, 0.0f, 0.0f, 0.0f, 0));
            }
            hoveredEntry = null;
        }
        for (ItemEntry entry : catalogEntries) {
            Button cell = itemCells.get(entry.id());
            if (cell == null) continue;
            boolean visible = matchesBrowserQuery(entry, needle, modOnly)
                    && itemsWithContent.contains(entry.id());
            cell.visibility(visible
                    ? dev.sixik.unigui.api.widget.Visibility.VISIBLE
                    : dev.sixik.unigui.api.widget.Visibility.COLLAPSED);
        }

        itemScroll.scrollTo(0.0f, 0.0f);
        updateItemScrollContentHeight();
    }

    /**
     * Перестраивает индекс предметов с контентом: результаты разблокированных
     * рецептов, катализаторы станций и все id предметов, упомянутые в параметрах
     * рецептов (ингредиенты, дроп — поиск ведётся по строкам, годным в id
     * предметов из реестра).
     */
    private void refreshContentIndex() {
        Set<ResourceLocation> visible = new java.util.HashSet<>();
        visible.addAll(IsfClientState.recipeResults().values());
        for (List<IsfCatalystDefinition> catalysts : IsfClientState.typeCatalysts().values()) {
            for (IsfCatalystDefinition catalyst : catalysts) {
                if (catalyst.item() != null) visible.add(catalyst.item());
            }
        }
        Set<String> mentioned = new java.util.HashSet<>();
        for (IsfRecipeDefinition recipe : IsfClientState.recipes().values()) {
            if (recipe.parameters() != null) {
                for (JsonElement value : recipe.parameters().values()) {
                    collectMentionedIds(value, mentioned);
                }
            }
        }
        for (String raw : mentioned) {
            // Записи вида "minecraft:iron_ingot#9": суффикс количества отбрасываем.
            int hash = raw.lastIndexOf('#');
            ResourceLocation id = ResourceLocation.tryParse(hash < 0 ? raw : raw.substring(0, hash));
            if (id != null && BuiltInRegistries.ITEM.containsKey(id)) visible.add(id);
        }
        visible.removeIf(id -> !BuiltInRegistries.ITEM.containsKey(id));
        itemsWithContent = Set.copyOf(visible);
    }

    /**
     * Собирает все строковые id из JSON-значения (объекты/массивы рекурсивно).
     * Чистая функция без реестра — для тестов; годность строк проверяет вызыватель.
     */
    static void collectMentionedIds(JsonElement element, Set<String> out) {
        if (element == null || element.isJsonNull() || out == null) return;
        if (element.isJsonPrimitive()) {
            out.add(element.getAsString());
            return;
        }
        if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) collectMentionedIds(child, out);
            return;
        }
        if (element.isJsonObject()) {
            for (Map.Entry<String, JsonElement> member : element.getAsJsonObject().entrySet()) {
                collectMentionedIds(member.getValue(), out);
            }
        }
    }

    /**
     * Проверяет одну запись каталога против поискового запроса.
     *
     * @param entry запись каталога (id + ItemStack)
     * @param needle поисковый запрос без {@code @}, уже в нижнем регистре; пустой — совпадает всё
     * @param modOnly {@code true} — сверять только по Mod ID (namespace)
     * @return {@code true}, если запись подходит под фильтр
     */
    private static boolean matchesBrowserQuery(ItemEntry entry, String needle, boolean modOnly) {
        if (needle == null || needle.isEmpty()) return true;
        ResourceLocation id = entry.id();
        if (id == null) return false;
        String namespace = id.getNamespace().toLowerCase(Locale.ROOT);
        if (modOnly) return namespace.contains(needle);
        String displayName = entry.stack().getHoverName().getString().toLowerCase(Locale.ROOT);
        String path = id.getPath().toLowerCase(Locale.ROOT);
        String fullId = namespace + ":" + path;
        return displayName.contains(needle)
                || namespace.contains(needle)
                || path.contains(needle)
                || fullId.contains(needle);
    }

    private Button itemCell(ItemEntry entry) {
        Button cell = new IconButton();
        cell.textPadding(0.0f, 0.0f);
        cell.layout(style -> style.size(CELL, CELL).flexNone());
        cell.on(PointerEnteredEvent.TYPE, event -> {
            hoveredEntry = entry;
            ensureCatalogTooltip(cell, entry);
        });
        cell.on(PointerExitedEvent.TYPE, event -> {
            if (hoveredEntry == entry) hoveredEntry = null;
        });

        // ЛКМ по клетке = R: открываем из кэша и запрашиваем разблокировку на сервере,
        // иначе предметы, которые ещё ни разу не открывали, выглядели бы «без рецептов».
        cell.onClick(event -> showRecipes(entry.id(), false));

        IsfItemIconWidget icon = new IsfItemIconWidget(entry.stack());
        // Иконка только рисуется. Hit-box и все pointer-события принадлежат Button-клетке.
        icon.enabled(false);
        icon.layout(style -> style.size(16.0f, 16.0f).centerSelf().flexNone());
        cell.addChild(icon);
        return cell;
    }

    /**
     * Тултип клетки каталога по первому наведению: тысячи eager-тултипов —
     * это тысячи виджетов в дереве на каждый кадр (тик/measure/arrange/render)
     * и фриз первого открытия, а показывают из них единицы.
     */
    private void ensureCatalogTooltip(Button cell, ItemEntry entry) {
        if (cell == null || entry == null || catalogTooltips.containsKey(cell)) return;
        MinecraftItemTooltip tooltip = new MinecraftItemTooltip(cell, entry.stack());
        catalogTooltips.put(cell, tooltip);
        tooltips.add(tooltip);
        overlayRoot.addOverlay(tooltip);
    }

    private void updateDetail(ItemEntry entry) {
        boolean newItem = selectedEntry == null || entry == null
                || !selectedEntry.id().equals(entry.id());
        if (newItem) {
            recipePage = 0;
            selectedCatalyst = null;
            selectedTypeId = null;
        }
        selectedEntry = entry;
        if (entry == null) {
            detailPanel.visibility(dev.sixik.unigui.api.widget.Visibility.COLLAPSED);
            return;
        }
        detailPanel.visibility(dev.sixik.unigui.api.widget.Visibility.VISIBLE);
        detailPin.silentChecked(detailPinned);
        detailTitle.text(entry.stack().getHoverName().getString());
        List<IsfRecipeDefinition> recipes = new ArrayList<>();
        for (ResourceLocation recipeId : entry.recipeIds()) {
            IsfRecipeDefinition recipe = IsfClientState.recipes().get(recipeId);
            if (recipe != null) recipes.add(recipe);
        }
        rebuildTypeTabs(recipes);
        rebuildCatalysts();
        updateDetailTitle();
        rebuildRecipePages();
    }

    /** Заголовок окна — локализованное имя активного RecipeType, а не имя предмета. */
    private void updateDetailTitle() {
        detailTitle.text(selectedTypeId == null
                ? selectedEntry.stack().getHoverName().getString()
                : typeDisplayName(selectedTypeId).getString());
    }

    /** Ключ локализации {@code isf.recipe_type.<namespace>.<path>} с фолбэком по пути типа. */
    private static net.minecraft.network.chat.Component typeDisplayName(ResourceLocation typeId) {
        String key = "isf.recipe_type." + typeId.getNamespace() + "." + typeId.getPath();
        String fallback = prettifyPath(typeId.getPath());
        return net.minecraft.network.chat.Component.translatableWithFallback(key, fallback);
    }

    private static String prettifyPath(String path) {
        String[] words = path.replace('_', ' ').split(" ");
        StringBuilder text = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) continue;
            if (!text.isEmpty()) text.append(' ');
            text.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return text.toString();
    }

    /** Зелёная зона: вкладки всех RecipeType, у которых есть рецепты по запросу. */
    private void rebuildTypeTabs(List<IsfRecipeDefinition> recipes) {
        List<ResourceLocation> order = new ArrayList<>();
        for (IsfRecipeDefinition recipe : recipes) {
            if (!order.contains(recipe.recipeType())) order.add(recipe.recipeType());
        }
        // Порядок вкладок — из реестра сервера, а не из порядка сетевых чанков.
        order = IsfClientState.sortByTypeOrder(order);
        List<ResourceLocation> current = new ArrayList<>();
        for (TypeTab tab : typeTabs) current.add(tab.typeId());
        if (!current.equals(order)) {
            clearTabTooltips();
            selectedCatalyst = null;
            selectedTypeId = order.isEmpty() ? null : order.get(0);
            tabRow.clearChildren();
            List<TypeTab> tabs = new ArrayList<>();
            for (ResourceLocation typeId : order) {
                IconButton tab = new IconButton();
                tab.themeEnabled(false);
                tab.backgroundVisible(true);
                tab.borderVisible(true);
                tab.radius(2.0f);
                tab.layout(style -> style.size(TAB_CELL, TAB_CELL).flexNone());
                ItemStack icon = tabIcon(typeId);
                if (icon.isEmpty()) {
                    Label unknown = new Label("?");
                    unknown.layout(style -> style.size(16.0f, 16.0f).centerSelf().flexNone());
                    tab.addChild(unknown);
                } else {
                    IsfItemIconWidget iconWidget = new IsfItemIconWidget(icon);
                    iconWidget.enabled(false);
                    iconWidget.layout(style -> style.size(16.0f, 16.0f).centerSelf().flexNone());
                    tab.addChild(iconWidget);
                    MinecraftItemTooltip tooltip = new MinecraftItemTooltip(tab, icon);
                    tabTooltips.add(tooltip);
                    overlayRoot.addOverlay(tooltip);
                }
                // Кнопка вкладки обязана обрабатывать клик сама: Button поглощает
                // ЛКМ в widget-маршруте, и Forge-хук с bounds-проверкой уже не
                // вызывается (как у detailClose/detailPin/пагинации).
                tab.onClick(event -> selectType(typeId));
                tabRow.addChild(tab);
                tabs.add(new TypeTab(typeId, tab));
            }
            tabScroll.scrollTo(0.0f, 0.0f);
            typeTabs = List.copyOf(tabs);
        }
        if (selectedTypeId == null && !typeTabs.isEmpty()) {
            selectedTypeId = typeTabs.get(0).typeId();
        }
        applyTabHighlights();
    }

    /** Иконка вкладки: явная иконка типа, иначе первый катализатор. */
    private ItemStack tabIcon(ResourceLocation typeId) {
        ResourceLocation iconId = IsfClientState.typeIcons().get(typeId);
        if (iconId == null) {
            List<IsfCatalystDefinition> catalysts = IsfClientState.typeCatalysts().get(typeId);
            if (catalysts == null || catalysts.isEmpty()) return ItemStack.EMPTY;
            iconId = catalysts.get(0).item();
        }
        Item item = BuiltInRegistries.ITEM.get(iconId);
        return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }

    /**
     * ПКМ по блоку-катализатору: открывает окно со всеми категориями,
     * где этот блок является станцией крафта, и их разблокированными рецептами.
     */
    void showStationRecipes(ResourceLocation catalystItemId) {
        if (catalystItemId == null) return;
        List<ResourceLocation> typeIds = new ArrayList<>();
        IsfClientState.typeCatalysts().forEach((typeId, catalysts) -> {
            for (IsfCatalystDefinition catalyst : catalysts) {
                if (catalyst.item().equals(catalystItemId)) {
                    typeIds.add(typeId);
                    return;
                }
            }
        });
        if (typeIds.isEmpty()) return;
        List<ResourceLocation> recipeIds = new ArrayList<>();
        for (IsfRecipeDefinition recipe : IsfClientState.recipes().values()) {
            if (typeIds.contains(recipe.recipeType())) recipeIds.add(recipe.id());
        }
        if (recipeIds.isEmpty()) return;
        selectedQuery = null;
        Item item = BuiltInRegistries.ITEM.get(catalystItemId);
        updateDetail(new ItemEntry(catalystItemId, new ItemStack(item), List.copyOf(recipeIds)));
        // Гарантированно открываем вкладку станции, даже если порядок вкладок другой.
        if (!typeIds.contains(selectedTypeId)) {
            selectedTypeId = typeIds.get(0);
            applyTabHighlights();
            rebuildCatalysts();
            rebuildRecipePages();
        }
    }

    private void applyTabHighlights() {
        for (TypeTab tab : typeTabs) {
            boolean active = tab.typeId().equals(selectedTypeId);
            tab.button().background().set(active
                    ? new dev.sixik.unigui.api.math.MutableColor(0.259f, 0.463f, 0.608f, 0.95f)
                    : new dev.sixik.unigui.api.math.MutableColor(0.063f, 0.078f, 0.106f, 0.90f));
            tab.button().borderColor().set(active
                    ? new dev.sixik.unigui.api.math.MutableColor(0.85f, 0.92f, 1.0f, 1.0f)
                    : new dev.sixik.unigui.api.math.MutableColor(0.416f, 0.561f, 0.682f, 1.0f));
        }
    }

    private void selectType(ResourceLocation typeId) {
        if (typeId == null || typeId.equals(selectedTypeId)) return;
        selectedTypeId = typeId;
        selectedCatalyst = null;
        recipePage = 0;
        applyTabHighlights();
        updateDetailTitle();
        rebuildCatalysts();
        rebuildRecipePages();
    }

    /** Красная зона: катализаторы. Для LootTable-типа колонка скрыта целиком. */
    private void rebuildCatalysts() {
        catalystColumn.clearChildren();
        clearCatalystTooltips();
        // У таблиц добычи нет станции: красная колонка не нужна — схлопываем её,
        // чтобы рецепты занимали всю ширину окна.
        boolean hideCatalysts = dev.sixik.isf.importer.LootTableSupports.LOOT_TYPE_ID
                .equals(selectedTypeId);
        catalystColumn.visibility(hideCatalysts
                ? dev.sixik.unigui.api.widget.Visibility.COLLAPSED
                : dev.sixik.unigui.api.widget.Visibility.VISIBLE);
        if (hideCatalysts) {
            catalystCells = List.of();
            return;
        }
        List<IsfCatalystDefinition> catalysts = selectedTypeId == null
                ? List.of()
                : IsfClientState.typeCatalysts().getOrDefault(selectedTypeId, List.of());
        List<CatalystCell> cells = new ArrayList<>();
        for (IsfCatalystDefinition catalyst : catalysts) {
            Item item = BuiltInRegistries.ITEM.get(catalyst.item());
            if (item == Items.AIR) continue;
            ItemStack stack = new ItemStack(item, Math.max(1, catalyst.count()));
            IconButton cell = new IconButton();
            cell.themeEnabled(false);
            cell.backgroundVisible(true);
            cell.borderVisible(true);
            cell.radius(2.0f);
            cell.layout(style -> style.size(CATALYST_CELL, CATALYST_CELL).flexNone());
            IsfItemIconWidget icon = new IsfItemIconWidget(stack);
            icon.enabled(false);
            icon.layout(style -> style.size(16.0f, 16.0f).centerSelf().flexNone());
            cell.addChild(icon);
            MinecraftItemTooltip tooltip = new MinecraftItemTooltip(cell, stack);
            catalystTooltips.add(tooltip);
            overlayRoot.addOverlay(tooltip);
            catalystColumn.addChild(cell);
            cells.add(new CatalystCell(catalyst.item(), cell));
        }
        catalystCells = List.copyOf(cells);
        applyCatalystHighlights();
    }

    private void applyCatalystHighlights() {
        for (CatalystCell cell : catalystCells) {
            boolean active = cell.itemId().equals(selectedCatalyst);
            cell.button().background().set(active
                    ? new dev.sixik.unigui.api.math.MutableColor(0.259f, 0.463f, 0.608f, 0.95f)
                    : new dev.sixik.unigui.api.math.MutableColor(0.063f, 0.078f, 0.106f, 0.90f));
            cell.button().borderColor().set(active
                    ? new dev.sixik.unigui.api.math.MutableColor(0.85f, 0.92f, 1.0f, 1.0f)
                    : new dev.sixik.unigui.api.math.MutableColor(0.416f, 0.561f, 0.682f, 1.0f));
        }
    }

    private void toggleCatalyst(ResourceLocation itemId) {
        selectedCatalyst = itemId.equals(selectedCatalyst) ? null : itemId;
        recipePage = 0;
        applyCatalystHighlights();
        rebuildRecipePages();
    }

    /** Рецепты активного типа с учётом фильтра по катализатору. */
    private List<IsfRecipeDefinition> currentTypeRecipes() {
        if (selectedEntry == null || selectedTypeId == null) return List.of();
        List<IsfRecipeDefinition> result = new ArrayList<>();
        for (ResourceLocation recipeId : selectedEntry.recipeIds()) {
            IsfRecipeDefinition recipe = IsfClientState.recipes().get(recipeId);
            if (recipe == null || !selectedTypeId.equals(recipe.recipeType())) continue;
            if (selectedCatalyst != null
                    && !IsfRecipeQueryMatcher.usesStation(recipe.triggers(), selectedCatalyst)) {
                continue;
            }
            result.add(recipe);
        }
        return result;
    }

    /** Синяя зона: строит страницы рецептов по высоте области просмотра (до 3 элементов). */
    private void rebuildRecipePages() {
        rebuildRecipePages(maxRecipeAreaHeight());
    }

    private void rebuildRecipePages(float areaHeight) {
        builtRecipes = List.of();
        recipePages = List.of();
        recipeArea.clearChildren();
        List<RecipeView> views = new ArrayList<>();
        for (IsfRecipeDefinition recipe : currentTypeRecipes()) {
            views.add(new RecipeView(recipe, measureHeight(recipe)));
        }
        builtRecipes = List.copyOf(views);
        pageAreaHeight = areaHeight;
        recipePages = IsfRecipePaging.partitionByHeight(
                views.stream().map(RecipeView::height).toList(), pageAreaHeight, RECIPE_GAP, 3);
        recipePage = Math.max(0, Math.min(recipePage, Math.max(0, recipePages.size() - 1)));
        renderRecipePage();
        updateDetailPanelHeight();
    }

    private void renderRecipePage() {
        recipeArea.clearChildren();
        clearRecipeItemTooltips();
        List<Integer> page = recipePage < recipePages.size()
                ? recipePages.get(recipePage) : List.of();
        if (page.isEmpty()) {
            Label empty = new Label("No available recipes");
            empty.layout(style -> style.widthPercent(100.0f).height(14.0f).flexNone());
            recipeArea.addChild(empty);
        } else {
            // Визуалы создаются заново при каждом показе: clearChildren утилизирует
            // старые виджеты, и повторное использование их экземпляров недопустимо.
            // Кнопки предметов собираем лениво: дети добавляются отложенными
            // мутациями и появляются в дереве только после layout-прохода.
            for (int index : page) {
                IsfRecipeDefinition recipe = builtRecipes.get(index).recipe();
                Widget visual = IsfVisualWidgetFactory.create(recipe.visual(), recipe.parameters());
                if (visual == null) continue;
                recipeArea.addChild(visual);
                pageVisuals.add(visual);
            }
            // Дети добавлены отложенными мутациями: применяем их сразу, чтобы
            // тултипы предметов работали с первого кадра, а не после первого клика.
            for (Widget root : pageVisuals) flushWidgetTree(root);
            ensureRecipeItemButtons();
        }
        detailPage.text((recipePage + 1) + " / " + Math.max(1, recipePages.size()));
        boolean multiple = recipePages.size() > 1;
        detailPrevious.enabled(multiple);
        detailNext.enabled(multiple);
    }

    /** Заполняет recipeItemButtons один раз после того, как дерево применено. */
    private void ensureRecipeItemButtons() {
        if (recipeItemButtonsPopulated) return;
        recipeItemButtonsPopulated = true;
        List<IsfItemButton> collected = new ArrayList<>();
        for (Widget root : pageVisuals) collectItemButtons(root, collected);        for (IsfItemButton button : collected) {
            recipeItemButtons.add(button);
            MinecraftItemTooltip tooltip = new MinecraftItemTooltip(button, button.stack());
            if (button.acceptsGrid()) {
                // Тег-ингредиент: «Принимает:» + сетка моделей вариантов.
                tooltip.renderer(dev.sixik.unigui.widgets.minecraft.MinecraftTooltipRenderers
                        .acceptsGrid(button.acceptsStacks(),
                                () -> new IsfAcceptsTooltipData(button.acceptsStacks())));
            } else if (button.extraTooltipLines() != null && !button.extraTooltipLines().isEmpty()) {
                // Условие выпадения лута: vanilla-тултип предмета + строки условий.
                List<net.minecraft.network.chat.Component> lines = new ArrayList<>();
                lines.add(button.stack().getHoverName());
                lines.addAll(button.extraTooltipLines());
                tooltip.renderer(dev.sixik.unigui.widgets.minecraft.MinecraftTooltipRenderers
                        .vanilla(lines));
            } else if (button.tooltipLines() != null) {
                tooltip.renderer(dev.sixik.unigui.widgets.minecraft.MinecraftTooltipRenderers
                        .vanilla(button.tooltipLines()));
            }
            recipeItemTooltips.add(tooltip);
            overlayRoot.addOverlay(tooltip);
        }
        // Кнопки переноса закрывают окно рецептов после выкладки.
        for (Widget root : pageVisuals) collectTransferButtons(root, transferButtons);
        for (IsfTransferButton button : transferButtons) button.onTransferred(this::closeDetail);
    }

    private static void collectTransferButtons(Widget widget, List<IsfTransferButton> out) {
        if (widget instanceof IsfTransferButton button) out.add(button);
        for (Widget child : widget.children()) collectTransferButtons(child, out);
    }

    private static void collectItemButtons(Widget widget, List<IsfItemButton> out) {
        if (widget instanceof IsfItemButton button) out.add(button);
        for (Widget child : widget.children()) collectItemButtons(child, out);
    }

    /** Применяет отложенные addChild-мутации по всему поддереву. */
    private static void flushWidgetTree(Widget widget) {
        if (widget instanceof dev.sixik.unigui.widgets.containers.PanelWidget panel) {
            panel.applyQueuedMutations();
            for (Widget child : panel.children()) flushWidgetTree(child);
        }
    }

    private void clearRecipeItemTooltips() {
        for (MinecraftItemTooltip tooltip : recipeItemTooltips) overlayRoot.removeOverlay(tooltip);
        recipeItemTooltips.clear();
        recipeItemButtons.clear();
        transferButtons.clear();
        pageVisuals.clear();
        recipeItemButtonsPopulated = false;
    }

    private void changeRecipePage(int direction) {
        if (recipePages.size() < 2) return;
        recipePage = Math.floorMod(recipePage + direction, recipePages.size());
        renderRecipePage();
        updateDetailPanelHeight();
    }

    /** Суммарная высота рецептов на текущей странице (максимум до 3 рецептов). */
    private float currentRecipePageHeight() {
        if (recipePages.isEmpty() || recipePage >= recipePages.size()) return 20.0f;
        List<Integer> page = recipePages.get(recipePage);
        if (page.isEmpty()) return 20.0f;
        float h = 0.0f;
        for (int i = 0; i < page.size(); i++) {
            int index = page.get(i);
            if (index >= 0 && index < builtRecipes.size()) {
                if (i > 0) h += RECIPE_GAP;
                h += builtRecipes.get(index).height();
            }
        }
        return Math.max(20.0f, h);
    }

    /** Максимальная высота области рецептов, доступная на экране. */
    private float maxRecipeAreaHeight() {
        Screen screen = net.minecraft.client.Minecraft.getInstance().screen;
        float screenHeight = screen == null ? 240.0f : screen.height;
        float margin = 4.0f;
        float available = screenHeight - margin * 2.0f - detailFixedHeight();
        return Math.max(50.0f, available);
    }

    private void updateDetailPanelHeight() {
        Screen screen = net.minecraft.client.Minecraft.getInstance().screen;
        if (screen == null) return;
        float pageContentHeight = currentRecipePageHeight();
        int maxDetailHeight = Math.max(120, screen.height - 8);
        int detailHeight = Math.min(maxDetailHeight, Math.round(detailFixedHeight() + pageContentHeight));
        detailPanel.layout(style -> style.height(detailHeight));
        if (detailPositionSet) {
            float width = detailPanel.layoutStyle().width().value();
            if (width <= 0.0f) width = detailPanel.layoutBounds().width();
            if (width <= 0.0f) width = 206.0f;
            moveDetailPanel(detailLeft, detailTop, width, detailHeight);
        }
    }

    private static float measureHeight(IsfRecipeDefinition recipe) {
        Widget visual = IsfVisualWidgetFactory.create(recipe.visual(), recipe.parameters());
        if (visual == null) return 14.0f;
        try {
            float height = IsfVisualWidgetFactory.adaptHeightToChildren(visual);
            return height > 0.0f ? height : FALLBACK_RECIPE_HEIGHT;
        } catch (RuntimeException ignored) {
            return FALLBACK_RECIPE_HEIGHT;
        }
    }

    private void clearTabTooltips() {
        for (MinecraftItemTooltip tooltip : tabTooltips) overlayRoot.removeOverlay(tooltip);
        tabTooltips.clear();
    }

    private void clearCatalystTooltips() {
        for (MinecraftItemTooltip tooltip : catalystTooltips) overlayRoot.removeOverlay(tooltip);
        catalystTooltips.clear();
    }

    private void clearDetailTooltips() {
        clearTabTooltips();
        clearCatalystTooltips();
    }

    private List<ItemEntry> entries() {
        Set<ResourceLocation> bookmarks = IsfClientState.bookmarks();
        Map<ResourceLocation, ItemEntry> indexed = new LinkedHashMap<>();

        // Правая панель строится по всем предметам, но видимость клеток решает
        // refreshBrowserFilter: без рецептов/применений/станции предмет скрыт.
        for (Item item : BuiltInRegistries.ITEM) {
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);
            if (item == Items.AIR || itemId == null) continue;
            indexed.put(itemId, new ItemEntry(itemId, new ItemStack(item), List.of()));
        }

        IsfClientState.recipeResults().forEach((recipeId, itemId) -> {
            Item item = BuiltInRegistries.ITEM.get(itemId);
            if (item == Items.AIR) return;
            ItemEntry current = indexed.get(itemId);
            List<ResourceLocation> recipes = current == null
                    ? new ArrayList<>() : new ArrayList<>(current.recipeIds());
            if (!recipes.contains(recipeId)) recipes.add(recipeId);
            indexed.put(itemId, new ItemEntry(itemId, new ItemStack(item), List.copyOf(recipes)));
        });
        for (ResourceLocation bookmark : bookmarks) {
            Item item = BuiltInRegistries.ITEM.get(bookmark);
            if (item != Items.AIR) indexed.putIfAbsent(bookmark,
                    new ItemEntry(bookmark, new ItemStack(item), List.of()));
        }

        List<ItemEntry> entries = new ArrayList<>(indexed.values());
        entries.removeIf(entry -> entry.stack().isEmpty());
        entries.sort((left, right) -> Integer.compare(
                BuiltInRegistries.ITEM.getId(left.stack().getItem()),
                BuiltInRegistries.ITEM.getId(right.stack().getItem())));
        return entries;
    }

    private record ItemEntry(ResourceLocation id, ItemStack stack, List<ResourceLocation> recipeIds) {
    }

    /** Вкладка RecipeType в шапке окна рецептов. */
    private record TypeTab(ResourceLocation typeId, Button button) {
    }

    /** Катализатор в левой колонке окна рецептов. */
    private record CatalystCell(ResourceLocation itemId, Button button) {
    }

    /** Рецепт с измеренной высотой визуала для пагинации. */
    private record RecipeView(IsfRecipeDefinition recipe, float height) {
    }

    private final class BrowserRoot extends Box {
        @Override
        public void tick(FrameContext frame) {
            super.tick(frame);
            syncPanelBounds();
            syncTabScrollHeight();
            if (observedStateVersion != IsfClientState.version()) {
                if (!observedRecipeResults.equals(IsfClientState.recipeResults())) {
                    syncCatalog();
                } else {
                    syncBookmarks();
                    refreshSelectedDetail();
                    observedStateVersion = IsfClientState.version();
                }
                if (pendingRecipeQuery != null
                        && IsfClientState.version() > pendingRecipeQuery.stateVersion()) {
                    PendingRecipeQuery query = pendingRecipeQuery;
                    pendingRecipeQuery = null;
                    showRecipeQuery(query);
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
            int margin = 4;
            int guiLeft = container.getGuiLeft();
            int guiRight = Math.min(width - margin, guiLeft + imageWidth(container));
            int leftWidth = Math.max(0, guiLeft - margin * 2);
            int rightX = Math.min(width - margin, guiRight + margin);
            int rightWidth = Math.max(0, width - rightX - margin);
            int panelHeight = Math.max(0, height - margin * 2);
            bookmarkPanel.layout(style -> style
                    .position(PositionType.ABSOLUTE)
                    .left(margin)
                    .top(margin)
                    .size(leftWidth, panelHeight));
            browserPanel.layout(style -> style
                    .position(PositionType.ABSOLUTE)
                    .left(rightX)
                    .top(margin)
                    .right((SizeValue) null)
                    .bottom((SizeValue) null)
                    .size(rightWidth, panelHeight)
                    .padding(PANEL_PADDING)
                    .overflow(Overflow.HIDDEN));

            float itemContentWidth = Math.max(0.0f,
                    rightWidth - PANEL_PADDING * 2.0f
                            - ScrollBar.DEFAULT_SIZE
                            - itemScroll.scrollbarGap());
            int itemColumns = Math.max(1, (int) (itemContentWidth / CELL));
            if (grid.columns() != itemColumns) {
                grid.columns(itemColumns);
                refreshBrowserFilter();
            }

            int bookmarkContentWidth = Math.max(0, leftWidth - 8);
            int bookmarkColumns = Math.max(1, (int) (bookmarkContentWidth / CELL));
            if (bookmarkGrid.columns() != bookmarkColumns) {
                bookmarkGrid.columns(bookmarkColumns);
                updateBookmarkScrollContentHeight();
            }
            if (selectedEntry != null) {
                int detailWidth = Math.min(240, Math.max(206, width - margin * 2));
                int maxAvailableHeight = Math.max(120, height - margin * 2);
                float maxAreaHeight = Math.max(50.0f, maxAvailableHeight - detailFixedHeight());

                if (Math.abs(maxAreaHeight - pageAreaHeight) > 0.5f) {
                    rebuildRecipePages(maxAreaHeight);
                }

                float pageContentHeight = currentRecipePageHeight();
                int detailHeight = Math.min(maxAvailableHeight, Math.round(detailFixedHeight() + pageContentHeight));

                if (!detailPositionSet) {
                    detailLeft = Math.max(margin, Math.min(width - detailWidth - margin, (width - detailWidth) * 0.5f));
                    detailTop = Math.max(margin, Math.min(height - detailHeight - margin, (height - detailHeight) * 0.5f));
                    detailPositionSet = true;
                    detailPanel.layout(style -> style.left(detailLeft)
                            .top(detailTop)
                            .size(detailWidth, detailHeight));
                } else {
                    detailPanel.layout(style -> style.size(detailWidth, detailHeight));
                    moveDetailPanel(detailLeft, detailTop, detailWidth, detailHeight);
                }
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

    private record PendingRecipeQuery(ResourceLocation itemId, boolean usages, long stateVersion) {
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
