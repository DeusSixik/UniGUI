package dev.sixik.unigui.widgets.navigation;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.render.TextureHandle;

/**
 * Пункт боковой навигации: то, что пользователь выбирает кликом.
 *
 * <p>Пункт создаётся только через {@link NavigationCategory#addItem(String, String)}; это тонкая обёртка над
 * узлом дерева, поэтому изменения названия, иконки и доступности сразу видны в {@link NavigationView}.</p>
 */
@AiDoc(summary = "A selectable entry of a NavigationView: stable id, title, optional icon and enabled flag.")
public class NavigationItem {
    private final NavigationCategory category;
    private final TreeViewNode node;
    private final String id;
    private Object data;

    NavigationItem(NavigationCategory category, TreeViewNode node, String id) {
        this.category = category;
        this.node = node;
        this.id = id;
        node.value(id);
        node.userData(this);
    }

    /**
     * @return стабильный идентификатор, по которому пункт ищут и выбирают
     */
    public String id() {
        return id;
    }

    /**
     * @return группа, в которой лежит пункт
     */
    public NavigationCategory category() {
        return category;
    }

    /**
     * @return отображаемое название
     */
    public String title() {
        return node.text();
    }

    /**
     * Меняет отображаемое название; идентификатор остаётся прежним.
     *
     * @param title новое название
     * @return этот пункт
     */
    public NavigationItem title(String title) {
        node.text(title);
        return this;
    }

    /**
     * Задаёт векторную иконку пункта в слоте 16×16 (рисуется каждый кадр, не должна аллоцировать).
     *
     * @param icon отрисовка или {@code null}, чтобы убрать иконку
     * @return этот пункт
     */
    public NavigationItem icon(TreeViewNode.IconDraw icon) {
        node.iconDraw(icon);
        return this;
    }

    /**
     * Задаёт текстурную иконку пункта.
     *
     * @param icon текстура или {@code null}, чтобы убрать иконку
     * @return этот пункт
     */
    public NavigationItem icon(TextureHandle icon) {
        node.icon(icon);
        return this;
    }

    /**
     * @return {@code true}, если пункт можно выбрать
     */
    public boolean enabled() {
        return node.selectable();
    }

    /**
     * Включает или выключает пункт. Выключенный пункт рисуется приглушённо и не выбирается кликом;
     * если он был выбран, выбор сбрасывается.
     *
     * @param enabled {@code false}, чтобы запретить выбор
     * @return этот пункт
     */
    public NavigationItem enabled(boolean enabled) {
        node.selectable(enabled);
        return this;
    }

    /**
     * @return пользовательские данные пункта или {@code null}
     */
    public Object data() {
        return data;
    }

    /**
     * Привязывает к пункту произвольный объект, например экран, который он открывает.
     *
     * @param data данные или {@code null}
     * @return этот пункт
     */
    public NavigationItem data(Object data) {
        this.data = data;
        return this;
    }

    /**
     * @return {@code true}, если пункт сейчас выбран
     */
    public boolean selected() {
        return node.owner() != null && node.owner().selectedNode() == node;
    }

    TreeViewNode node() {
        return node;
    }
}
