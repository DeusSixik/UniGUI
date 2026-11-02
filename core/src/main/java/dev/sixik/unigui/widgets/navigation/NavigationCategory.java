package dev.sixik.unigui.widgets.navigation;

import dev.sixik.unigui.api.docs.AiDoc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Группа пунктов боковой навигации со сворачиваемым заголовком.
 *
 * <p>Заголовок выбрать нельзя: клик по нему только сворачивает или разворачивает группу и не сбрасывает
 * выбранный пункт. Группа создаётся через {@link NavigationView#addCategory(String)}.</p>
 */
@AiDoc(summary = "A collapsible group of NavigationItems inside a NavigationView; its header toggles but is never selected.")
public class NavigationCategory {
    private final NavigationView view;
    private final TreeViewNode node;
    private final List<NavigationItem> items = new ArrayList<>();

    NavigationCategory(NavigationView view, TreeViewNode node) {
        this.view = view;
        this.node = node;
        node.userData(this);
        node.selectable(false);
    }

    /**
     * @return текст заголовка
     */
    public String title() {
        return node.text();
    }

    /**
     * Меняет текст заголовка.
     *
     * @param title новый текст
     * @return эта группа
     */
    public NavigationCategory title(String title) {
        node.text(title);
        return this;
    }

    /**
     * @return {@code true}, если пункты группы показаны
     */
    public boolean expanded() {
        return node.expanded();
    }

    /**
     * Разворачивает или сворачивает группу.
     *
     * @param expanded {@code true}, чтобы показать пункты
     * @return эта группа
     */
    public NavigationCategory expanded(boolean expanded) {
        node.expanded(expanded);
        return this;
    }

    /**
     * @return пункты группы в порядке добавления (только чтение)
     */
    public List<NavigationItem> items() {
        return Collections.unmodifiableList(items);
    }

    /**
     * Добавляет пункт в конец группы.
     *
     * @param id    уникальный в пределах навигации идентификатор
     * @param title отображаемое название
     * @return новый пункт
     * @throws IllegalArgumentException если такой идентификатор уже есть
     */
    public NavigationItem addItem(String id, String title) {
        if (id == null || id.isEmpty()) throw new IllegalArgumentException("Navigation item id must not be empty");
        if (view.item(id) != null) throw new IllegalArgumentException("Duplicate navigation item id: " + id);
        NavigationItem item = new NavigationItem(this, node.addChild(title), id);
        items.add(item);
        view.register(item);
        return item;
    }

    /**
     * @return навигация, которой принадлежит группа
     */
    public NavigationView view() {
        return view;
    }

    TreeViewNode node() {
        return node;
    }
}
