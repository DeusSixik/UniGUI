package dev.sixik.unigui.api.scene;

import dev.sixik.unigui.api.math.Frustum3D;

import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Иерархия 3D-узлов и общие параметры освещения. Изменяется и читается в UI-потоке.
 */
public final class Scene {
    /**
     * Служебный родитель корневых узлов.
     */
    private final SceneNode root = new SceneNode(this);
    /**
     * Номер изменения видимого состояния, начиная с нуля.
     */
    private long revision;
    /**
     * Общий окружающий свет для материалов в режиме LIT.
     */
    private AmbientLight ambientLight = AmbientLight.WHITE;
    /**
     * Общий направленный свет или null при его отсутствии.
     */
    private DirectionalLight directionalLight;

    /**
     * Создаёт пустую сцену с белым окружающим светом.
     */
    public Scene() {
    }

    /**
     * Читает версию сцены для решения о перерисовке.
     *
     * @return счётчик изменений иерархии, свойств узлов и освещения.
     */
    public long revision() {
        return revision;
    }

    /**
     * Читает корневые узлы в порядке добавления.
     *
     * @return живой список для чтения.
     */
    public List<SceneNode> roots() {
        return root.children();
    }

    /**
     * Добавляет корневой узел, при необходимости отсоединяя его от предыдущего родителя.
     *
     * @param node перемещаемый узел.
     * @return эта сцена.
     * @throws NullPointerException если node равен null.
     */
    public Scene addRoot(SceneNode node) {
        root.addChild(node);
        return this;
    }

    /**
     * Отсоединяет корневой узел вместе с потомками.
     *
     * @param node удаляемый узел. Допускается null.
     * @return true, если узел был корнем этой сцены.
     */
    public boolean removeRoot(SceneNode node) {
        return root.removeChild(node);
    }

    /**
     * Читает окружающий свет.
     *
     * @return параметры окружающего света.
     */
    public AmbientLight ambientLight() {
        return ambientLight;
    }

    /**
     * Обновляет окружающий свет для материалов в режиме LIT.
     *
     * <p>Свет сцены освещает только собственные меши с {@link Material.Shading#LIT}. Блоки, предметы и сущности
     * Minecraft на всех версиях выглядят как в игре: у блоков запечено затенение граней, предметы и сущности
     * освещены двумя ванильными источниками, закреплёнными в мире сцены.</p>
     *
     * @param light параметры окружающего света.
     * @return эта сцена.
     * @throws NullPointerException если light равен null.
     */
    public Scene setAmbientLight(AmbientLight light) {
        Objects.requireNonNull(light, "light");
        if (!ambientLight.equals(light)) {
            ambientLight = light;
            changed();
        }
        return this;
    }

    /**
     * Читает направленный свет.
     *
     * @return параметры света или null.
     */
    public DirectionalLight directionalLight() {
        return directionalLight;
    }

    /**
     * Обновляет направленный свет для материалов в режиме LIT. Контент Minecraft он не меняет, см.
     * {@link #setAmbientLight}.
     *
     * @param light параметры направленного света или null, чтобы его убрать.
     * @return эта сцена.
     */
    public Scene setDirectionalLight(DirectionalLight light) {
        if (!Objects.equals(directionalLight, light)) {
            directionalLight = light;
            changed();
        }
        return this;
    }

    /**
     * Обходит видимые узлы в глубину в порядке добавления. Скрытый узел скрывает потомков.
     * Отсечение по AABB применяется к содержимому каждого узла. Дети отсечённого узла
     * проверяются самостоятельно. Узлы без границ, в том числе группы, передаются посетителю.
     *
     * @param frustum объём видимости или null для обхода без отсечения.
     * @param visitor обработчик видимого узла, вызывается синхронно.
     * @throws NullPointerException            если visitor равен null.
     * @throws ConcurrentModificationException если посетитель изменил сцену во время обхода.
     */
    public void visitVisible(Frustum3D frustum, Consumer<? super SceneNode> visitor) {
        Objects.requireNonNull(visitor, "visitor");
        long expectedRevision = revision;
        SceneNode node = root.nextInSubtree(root, true);
        while (node != null) {
            boolean visible = node.visible();
            if (visible && (frustum == null || node.intersects(frustum))) {
                visitor.accept(node);
                if (revision != expectedRevision) {
                    throw new ConcurrentModificationException("Scene modified during traversal");
                }
            }
            node = node.nextInSubtree(root, visible);
        }
    }

    /**
     * Обновляет версию сцены.
     */
    void changed() {
        revision++;
    }
}
