package dev.sixik.unigui.xaml;

import java.util.ArrayList;
import java.util.List;

/**
 * Документ разметки: корневой узел и комментарии перед ним.
 *
 * <p>Документ не зависит от формата, из которого был прочитан: XAML, Java DSL и скриптовые языки дают одно и то
 * же дерево, а {@link dev.sixik.unigui.xaml.format.UiFormat} превращает его обратно в текст.</p>
 */
public final class UiDocument {
    private final UiNode root;
    private final List<String> prologComments;

    /**
     * Создаёт документ.
     *
     * @param root           корневой узел
     * @param prologComments комментарии до корня (могут быть {@code null})
     */
    public UiDocument(UiNode root, List<String> prologComments) {
        if (root == null) throw new IllegalArgumentException("document root must not be null");
        this.root = root;
        this.prologComments = prologComments == null ? new ArrayList<>() : new ArrayList<>(prologComments);
    }

    /**
     * Создаёт документ без комментариев.
     *
     * @param root корневой узел
     * @return документ
     */
    public static UiDocument of(UiNode root) {
        return new UiDocument(root, null);
    }

    /**
     * Возвращает корневой узел.
     *
     * @return корень
     */
    public UiNode root() {
        return root;
    }

    /**
     * Возвращает комментарии, стоявшие перед корнем.
     *
     * @return копия списка
     */
    public List<String> prologComments() {
        return List.copyOf(prologComments);
    }

    /**
     * Создаёт глубокую копию документа.
     *
     * @return независимая копия
     */
    public UiDocument copy() {
        return new UiDocument(root.copy(), prologComments);
    }

    /**
     * Сравнивает документы по смыслу.
     *
     * @param other другой документ
     * @return {@code true}, если корни эквивалентны по {@link UiNode#sameStructure(UiNode)}
     */
    public boolean sameStructure(UiDocument other) {
        return other != null && root.sameStructure(other.root);
    }
}
