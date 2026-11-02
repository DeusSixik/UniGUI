package dev.sixik.unigui.widgets.navigation;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiLink;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;
import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.event.EventSubscription;
import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.style.Style;
import dev.sixik.unigui.widgets.WidgetStyles;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Боковая навигация: сворачиваемые группы с пунктами, один выбранный пункт на всё дерево.
 *
 * <p>Это {@link TreeView} с готовой семантикой «группа / пункт»: заголовок группы лежит на своей плашке и только
 * сворачивает группу, пункт выбирается кликом или клавишами {@code Up/Down/Home/End}, выбранный пункт заливается
 * акцентным цветом на всю ширину. Цвета строк берутся из стиля ({@link WidgetStyles.Navigation#NAVIGATION_VIEW}),
 * цвет плашки группы задаётся {@link #categoryColor(ColorView)}.</p>
 *
 * <pre>{@code
 * NavigationView nav = new NavigationView();
 * NavigationCategory input = nav.addCategory("Basic Input");
 * input.addItem("button", "Button");
 * input.addItem("slider", "Slider");
 * nav.onItemSelected(item -> openPage(item.id()));
 * nav.selectItem("slider");
 * }</pre>
 */
@AiDoc(
        summary = "Sidebar navigation: collapsible categories of selectable items with full-width accent selection, built on TreeView.",
        purpose = "Gives gallery-style left menus a real widget instead of hand-built button stacks.",
        useWhen = {"A sidebar or settings menu groups pages into collapsible categories and exactly one page is shown at a time."},
        criticalRules = {
                "Category headers are never selected; clicking one only expands or collapses it.",
                "Item ids must be unique across the whole view.",
                "selectItem ignores unknown and disabled ids; selecting a valid item expands its category first.",
                "onItemSelected fires for item selection, not when selection is cleared.",
                "Put the view into a ScrollView when the list can be taller than its container."
        },
        related = {"TreeView", "ExpandablePanel", "Accordion"})
@WikiPage(
        title = "NavigationView",
        slug = "navigation-view",
        category = "<key:navigation.category>",
        summary = "<key:navigation.view.summary>"
)
@WikiSection(
        title = "<key:wiki.section.overview.title>",
        markdown = "<key:navigation.view.overview>",
        order = 10
)
@WikiExample(
        title = "<key:navigation.view.example.basic.title>",
        code = "NavigationView nav = new NavigationView();\nnav.addCategory(\"Basic Input\").addItem(\"slider\", \"Slider\");\nnav.onItemSelected(item -> open(item.id()));"
)
@WikiLink(
        label = "TreeView",
        target = "page:tree-view",
        description = "<key:tree.view.summary>"
)
public class NavigationView extends TreeView {
    /**
     * Style registry id used to resolve this widget's default navigation style.
     */
    public static final String STYLE_TYPE = dev.sixik.unigui.api.style.StyleIds.Widget.NAVIGATION_VIEW;

    /**
     * Categories in insertion order.
     */
    private final List<NavigationCategory> categories = new ArrayList<>();
    /**
     * Item lookup indexed by ids that are unique within this view.
     */
    private final Map<String, NavigationItem> itemsById = new LinkedHashMap<>();
    /**
     * Mutable RGBA color for category header backgrounds; each component is in [0, 1].
     */
    private final MutableColor categoryColor = MutableColor.rgba(0.176f, 0.212f, 0.251f, 1.0f);
    /**
     * Mutable RGBA color for category header text; each component is in [0, 1].
     */
    private final MutableColor categoryTextColor = MutableColor.rgba(0.953f, 0.965f, 0.976f, 1.0f);

    /**
     * Создаёт пустую навигацию.
     */
    @AiDoc(summary = "Creates an empty navigation menu with category headers and selectable item rows.")
    @WikiMethod(title = "Create a navigation view", summary = "Creates an empty sidebar navigation view.", order = 10)
    public NavigationView() {
        iconMode(TreeIconMode.NONE);
    }

    /**
     * Возвращает стиль навигации по умолчанию.
     *
     * @return стиль {@link WidgetStyles.Navigation#NAVIGATION_VIEW}
     */
    @Override
    protected Style defaultStyle() {
        return WidgetStyles.Navigation.NAVIGATION_VIEW;
    }

    /**
     * Добавляет группу в конец списка. Группа создаётся развёрнутой.
     *
     * @param title заголовок группы
     * @return новая группа; пункты добавляются через {@link NavigationCategory#addItem(String, String)}
     */
    @AiDoc(summary = "Appends an expanded category to this navigation view.", section = "Categories", sectionOrder = 10, order = 10)
    @WikiMethod(title = "Add a category", summary = "Appends a category in the expanded state.", section = "Categories", sectionOrder = 10, order = 10)
    @WikiReturn("The newly created category, used to add navigation items.")
    public NavigationCategory addCategory(@WikiParameter(name = "title", description = "Visible category header text.") String title) {
        NavigationCategory category = new NavigationCategory(this, addRoot(title));
        categories.add(category);
        return category;
    }

    /**
     * Добавляет группу и сразу задаёт, развёрнута ли она.
     *
     * @param title    заголовок группы
     * @param expanded {@code false}, чтобы группа была свёрнута
     * @return новая группа
     */
    @AiDoc(summary = "Appends a category and sets its initial expanded state.", section = "Categories", sectionOrder = 10, order = 20)
    @WikiMethod(title = "Add a category with initial state", summary = "Appends a category and controls whether its items are initially visible.", section = "Categories", sectionOrder = 10, order = 20)
    @WikiReturn("The newly created category.")
    public NavigationCategory addCategory(
            @WikiParameter(name = "title", description = "Visible category header text.") String title,
            @WikiParameter(name = "expanded", description = "True to show the category items initially; false to collapse the category.") boolean expanded) {
        return addCategory(title).expanded(expanded);
    }

    /**
     * @return группы в порядке добавления (только чтение)
     */
    @WikiMethod(title = "Get categories", summary = "Returns categories in insertion order.", section = "Categories", sectionOrder = 10, order = 30)
    @WikiReturn("An unmodifiable list of categories in insertion order.")
    public List<NavigationCategory> categories() {
        return Collections.unmodifiableList(categories);
    }

    /**
     * @return все пункты всех групп в порядке добавления (только чтение)
     */
    @WikiMethod(title = "Get items", summary = "Returns all items in category and insertion order.", section = "Items and selection", sectionOrder = 20, order = 10)
    @WikiReturn("An immutable snapshot of all items in insertion order.")
    public List<NavigationItem> items() {
        return List.copyOf(itemsById.values());
    }

    /**
     * Ищет пункт по идентификатору.
     *
     * @param id идентификатор
     * @return пункт или {@code null}
     */
    @WikiMethod(title = "Find an item", summary = "Looks up an item by its id.", section = "Items and selection", sectionOrder = 20, order = 20)
    @WikiReturn("The matching item, or null when the id is null or unknown.")
    public NavigationItem item(@WikiParameter(name = "id", description = "Item id; null returns null.") String id) {
        return id == null ? null : itemsById.get(id);
    }

    /**
     * @return выбранный пункт или {@code null}, если ничего не выбрано
     */
    @WikiMethod(title = "Get selected item", summary = "Returns the currently selected item, if any.", section = "Items and selection", sectionOrder = 20, order = 30)
    @WikiReturn("The selected item, or null when there is no selection.")
    public NavigationItem selectedItem() {
        TreeViewNode node = selectedNode();
        return node != null && node.userData() instanceof NavigationItem item ? item : null;
    }

    /**
     * Выбирает пункт и, если он в свёрнутой группе, разворачивает её. Слушатели получают событие.
     *
     * @param id идентификатор пункта; неизвестный идентификатор игнорируется
     * @return эта навигация
     */
    @AiDoc(summary = "Selects an enabled item by id, expanding its category first.", section = "Items and selection", sectionOrder = 20, order = 40)
    @WikiMethod(title = "Select an item", summary = "Selects an enabled item by id and expands its category if needed.", section = "Items and selection", sectionOrder = 20, order = 40)
    @WikiReturn("This view, for fluent configuration.")
    public NavigationView selectItem(@WikiParameter(name = "id", description = "Id of an item in this view; unknown or null ids are ignored.") String id) {
        NavigationItem item = item(id);
        if (item == null || !item.enabled()) return this;
        if (!item.category().expanded()) item.category().expanded(true);
        select(item.node());
        return this;
    }

    /**
     * Подписывается на выбор пункта. Вызывается только когда пункт выбран, а не когда выбор снят.
     *
     * @param listener получает выбранный пункт
     * @return подписка; {@code unsubscribe()} отключает слушателя
     */
    @AiDoc(summary = "Subscribes to item selection changes; no callback is made when selection is cleared.", section = "Items and selection", sectionOrder = 20, order = 50)
    @WikiMethod(title = "Listen for item selection", summary = "Registers a callback invoked when an item is selected.", section = "Items and selection", sectionOrder = 20, order = 50)
    @WikiReturn("A subscription that can be cancelled with unsubscribe().")
    public EventSubscription onItemSelected(
            @WikiParameter(name = "listener", description = "Callback receiving the selected item; null is accepted and ignored.") Consumer<? super NavigationItem> listener) {
        return onSelectionChanged(event -> {
            NavigationItem item = selectedItem();
            if (item != null && listener != null) listener.accept(item);
        });
    }

    /**
     * Удаляет все группы и пункты.
     *
     * @return эта навигация
     */
    @AiDoc(summary = "Removes all categories and items and clears the item lookup.", section = "Categories", sectionOrder = 10, order = 40)
    @WikiMethod(title = "Clear navigation", summary = "Removes every category and item.", section = "Categories", sectionOrder = 10, order = 40)
    @WikiReturn("This view, for fluent configuration.")
    public NavigationView clear() {
        clearRoots();
        categories.clear();
        itemsById.clear();
        return this;
    }

    /**
     * @return цвет плашки заголовка группы (изменяемый)
     */
    @WikiMethod(title = "Get category background color", summary = "Returns the mutable background color used for category headers.", section = "Appearance", sectionOrder = 30, order = 10)
    @WikiReturn("The mutable RGBA category header background color; components are in [0, 1].")
    public MutableColor categoryColor() {
        return categoryColor;
    }

    /**
     * Задаёт цвет плашки заголовка группы.
     *
     * @param color цвет
     * @return эта навигация
     */
    @AiDoc(summary = "Sets the background color used to paint category header rows; null leaves the current color unchanged.", section = "Appearance", sectionOrder = 30, order = 10)
    @WikiMethod(title = "Set category background color", summary = "Sets the category header background color and invalidates the view visually.", section = "Appearance", sectionOrder = 30, order = 20)
    @WikiReturn("This view, for fluent configuration.")
    public NavigationView categoryColor(@WikiParameter(name = "color", description = "Source RGBA color; null is ignored.") ColorView color) {
        if (color != null) {
            categoryColor.set(color);
            invalidate(InvalidationFlags.VISUAL);
        }
        return this;
    }

    /**
     * @return цвет текста заголовка группы (изменяемый)
     */
    @WikiMethod(title = "Get category text color", summary = "Returns the mutable text color used for category headers.", section = "Appearance", sectionOrder = 30, order = 30)
    @WikiReturn("The mutable RGBA category header text color; components are in [0, 1].")
    public MutableColor categoryTextColor() {
        return categoryTextColor;
    }

    /**
     * Задаёт цвет текста заголовка группы.
     *
     * @param color цвет
     * @return эта навигация
     */
    @AiDoc(summary = "Sets category header text color, refreshes category rows, and invalidates the view; null leaves the current color unchanged.", section = "Appearance", sectionOrder = 30, order = 20)
    @WikiMethod(title = "Set category text color", summary = "Sets the category header text color and refreshes category rows.", section = "Appearance", sectionOrder = 30, order = 40)
    @WikiReturn("This view, for fluent configuration.")
    public NavigationView categoryTextColor(@WikiParameter(name = "color", description = "Source RGBA color; null is ignored.") ColorView color) {
        if (color != null) {
            categoryTextColor.set(color);
            invalidate(InvalidationFlags.VISUAL);
            for (NavigationCategory category : categories) {
                onNodeVisualChanged(category.node());
            }
        }
        return this;
    }

    /**
     * Сохраняет выбранный пункт при сворачивании его группы.
     *
     * @return {@code true}, чтобы сворачивание группы не снимало выбор
     */
    @Override
    protected boolean keepSelectionWhenCollapsed() {
        return true;
    }

    /**
     * Возвращает цвет текста узла, используя специальный цвет для заголовков групп.
     *
     * @param node узел дерева, для которого определяется цвет
     * @return цвет текста заголовка группы или цвет, выбранный базовым {@link TreeView}
     */
    @Override
    protected ColorView styledTextColor(TreeViewNode node) {
        return node.userData() instanceof NavigationCategory ? categoryTextColor : super.styledTextColor(node);
    }

    /**
     * Рисует плашку заголовка группы под строкой; наведённый заголовок красит сам {@code TreeView} цветом hover.
     *
     * @param draw активная область отрисовки
     * @param row  строка дерева, включая узел, границы раскладки и состояние hover
     */
    @Override
    protected void renderRow(DrawScope draw, TreeRowButton row) {
        if (row.node().userData() instanceof NavigationCategory && !row.hovered()) {
            RectView bounds = row.layoutBounds();
            draw.rectFilled(bounds.x(), bounds.y(), bounds.width(), bounds.height(), categoryColor);
        }
        super.renderRow(draw, row);
    }

    /**
     * Регистрирует новый пункт в индексе идентификаторов этой навигации.
     *
     * @param item пункт с уже проверенным уникальным идентификатором
     */
    void register(NavigationItem item) {
        itemsById.put(item.id(), item);
    }
}
