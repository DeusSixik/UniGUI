package dev.sixik.unigui.widgets.navigation;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiLink;
import dev.sixik.unigui.api.docs.WikiPage;
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
    public static final String STYLE_TYPE = dev.sixik.unigui.api.style.StyleIds.Widget.NAVIGATION_VIEW;

    private final List<NavigationCategory> categories = new ArrayList<>();
    private final Map<String, NavigationItem> itemsById = new LinkedHashMap<>();
    private final MutableColor categoryColor = MutableColor.rgba(0.176f, 0.212f, 0.251f, 1.0f);
    private final MutableColor categoryTextColor = MutableColor.rgba(0.953f, 0.965f, 0.976f, 1.0f);

    /**
     * Создаёт пустую навигацию.
     */
    public NavigationView() {
        iconMode(TreeIconMode.NONE);
    }

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
    public NavigationCategory addCategory(String title) {
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
    public NavigationCategory addCategory(String title, boolean expanded) {
        return addCategory(title).expanded(expanded);
    }

    /**
     * @return группы в порядке добавления (только чтение)
     */
    public List<NavigationCategory> categories() {
        return Collections.unmodifiableList(categories);
    }

    /**
     * @return все пункты всех групп в порядке добавления (только чтение)
     */
    public List<NavigationItem> items() {
        return List.copyOf(itemsById.values());
    }

    /**
     * Ищет пункт по идентификатору.
     *
     * @param id идентификатор
     * @return пункт или {@code null}
     */
    public NavigationItem item(String id) {
        return id == null ? null : itemsById.get(id);
    }

    /**
     * @return выбранный пункт или {@code null}, если ничего не выбрано
     */
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
    public NavigationView selectItem(String id) {
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
    public EventSubscription onItemSelected(Consumer<? super NavigationItem> listener) {
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
    public NavigationView clear() {
        clearRoots();
        categories.clear();
        itemsById.clear();
        return this;
    }

    /**
     * @return цвет плашки заголовка группы (изменяемый)
     */
    public MutableColor categoryColor() {
        return categoryColor;
    }

    /**
     * Задаёт цвет плашки заголовка группы.
     *
     * @param color цвет
     * @return эта навигация
     */
    public NavigationView categoryColor(ColorView color) {
        if (color != null) {
            categoryColor.set(color);
            invalidate(InvalidationFlags.VISUAL);
        }
        return this;
    }

    /**
     * @return цвет текста заголовка группы (изменяемый)
     */
    public MutableColor categoryTextColor() {
        return categoryTextColor;
    }

    /**
     * Задаёт цвет текста заголовка группы.
     *
     * @param color цвет
     * @return эта навигация
     */
    public NavigationView categoryTextColor(ColorView color) {
        if (color != null) {
            categoryTextColor.set(color);
            invalidate(InvalidationFlags.VISUAL);
            for (NavigationCategory category : categories) {
                onNodeVisualChanged(category.node());
            }
        }
        return this;
    }

    @Override
    protected boolean keepSelectionWhenCollapsed() {
        return true;
    }

    @Override
    protected ColorView styledTextColor(TreeViewNode node) {
        return node.userData() instanceof NavigationCategory ? categoryTextColor : super.styledTextColor(node);
    }

    /**
     * Рисует плашку заголовка группы под строкой; наведённый заголовок красит сам {@code TreeView} цветом hover.
     */
    @Override
    protected void renderRow(DrawScope draw, TreeRowButton row) {
        if (row.node().userData() instanceof NavigationCategory && !row.hovered()) {
            RectView bounds = row.layoutBounds();
            draw.rectFilled(bounds.x(), bounds.y(), bounds.width(), bounds.height(), categoryColor);
        }
        super.renderRow(draw, row);
    }

    void register(NavigationItem item) {
        itemsById.put(item.id(), item);
    }
}
