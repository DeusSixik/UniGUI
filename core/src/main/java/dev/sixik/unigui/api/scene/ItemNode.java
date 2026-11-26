package dev.sixik.unigui.api.scene;

import dev.sixik.unigui.api.math.Aabb;

import java.util.Objects;

/**
 * 3D-модель предмета Minecraft с центром в начале координат, единая для всех версий игры.
 *
 * <pre>{@code
 * new ItemNode("minecraft:diamond_sword");
 * ItemNode.of(new ItemStack(Items.DIAMOND_SWORD));   // нативный стек версии
 * }</pre>
 *
 * <p>Нативный стек копируется платформой при разрешении, поэтому его дальнейшие изменения на узел не влияют.
 * Типизированный доступ к стеку даёт помощник платформы, например {@code MinecraftScene.stack(node)}.
 * Версии без поддержки предметов в сцене ничего не рисуют и один раз пишут предупреждение в лог.</p>
 */
public final class ItemNode extends SceneNode {
    /**
     * Границы, пока модель предмета неизвестна.
     */
    private static final Aabb FALLBACK_BOUNDS = new Aabb(-0.5f, -0.5f, -0.5f, 0.5f, 0.5f, 0.5f);

    /**
     * Идентификатор предмета или null для нативного источника.
     */
    private String id;
    /**
     * Нативный объект пользователя или null для идентификатора.
     */
    private Object nativeSource;
    /**
     * Кэш разрешённого предмета.
     */
    private final ResolvedContent item = new ResolvedContent();
    /**
     * Как модель предмета расположена вокруг начала координат.
     */
    private Display display = Display.FIXED;

    /**
     * Ванильное расположение модели предмета.
     */
    public enum Display {
        /**
         * Как в рамке: плоские предметы стоят вертикально, блоки уменьшены вдвое.
         */
        FIXED,
        /**
         * Как выброшенный на землю предмет: в четверть-полблока.
         */
        GROUND,
        /**
         * Как иконка в инвентаре: блоки повёрнуты в изометрию.
         */
        GUI
    }

    /**
     * Создаёт узел по идентификатору предмета.
     *
     * @param id идентификатор, например {@code minecraft:diamond_sword}.
     * @throws IllegalArgumentException если идентификатор пуст.
     */
    public ItemNode(String id) {
        this.id = requireId(id);
    }

    /**
     * Создаёт узел из нативного стека или предмета версии.
     *
     * @param item стек, предмет или строка-идентификатор.
     * @return новый узел.
     * @throws NullPointerException если item равен null.
     */
    public static ItemNode of(Object item) {
        if (item instanceof String text) return new ItemNode(text);
        ItemNode node = new ItemNode();
        node.nativeSource = Objects.requireNonNull(item, "item");
        return node;
    }

    /**
     * Внутренний конструктор для нативного источника.
     */
    private ItemNode() {
    }

    /**
     * @return расположение модели, по умолчанию {@link Display#FIXED}.
     */
    public Display display() {
        return display;
    }

    /**
     * Меняет расположение модели. На 1.7.10 различается только размер {@link Display#GROUND}.
     *
     * @param display расположение.
     * @return этот узел.
     */
    public ItemNode setDisplay(Display display) {
        Objects.requireNonNull(display, "display");
        if (this.display != display) {
            this.display = display;
            invalidateContent();
        }
        return this;
    }

    /**
     * @return идентификатор предмета или null для нативного источника.
     */
    public String id() {
        return id;
    }

    /**
     * Читает нативный объект, которым задан узел.
     *
     * @return объект пользователя или null, если узел задан идентификатором.
     */
    public Object nativeSource() {
        return nativeSource;
    }

    /**
     * Меняет предмет по идентификатору. Тот же идентификатор ничего не меняет.
     *
     * @param id идентификатор предмета.
     * @return этот узел.
     */
    public ItemNode setItem(String id) {
        String next = requireId(id);
        if (next.equals(this.id)) return this;
        this.id = next;
        nativeSource = null;
        sourceChanged();
        return this;
    }

    /**
     * Меняет предмет на нативный стек или предмет. Тот же объект ничего не меняет.
     *
     * @param item стек, предмет или строка-идентификатор.
     * @return этот узел.
     */
    public ItemNode setItem(Object item) {
        if (item instanceof String text) return setItem(text);
        Objects.requireNonNull(item, "item");
        if (nativeSource == item) return this;
        id = null;
        nativeSource = item;
        sourceChanged();
        return this;
    }

    /**
     * Разрешает предмет для бэкенда или помощника платформы.
     *
     * @param platform платформа версии или null.
     * @return нативный предмет или null, если он недоступен или неверен.
     */
    public Object nativeItem(ScenePlatform platform) {
        return item.get(platform, this::resolve, this, false);
    }

    /**
     * Разрешает предмет и бросает ошибку неверного источника.
     *
     * @param platform платформа версии.
     * @return нативный предмет или null, если предметы ещё недоступны.
     * @throws IllegalArgumentException      если предмет неизвестен.
     * @throws UnsupportedOperationException если версия не рисует предметы в сцене.
     */
    public Object requireNativeItem(ScenePlatform platform) {
        return item.get(Objects.requireNonNull(platform, "platform"), this::resolve, this, true);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Объединяет границы модели предмета и собственного меша узла. Пока модель неизвестна, используется куб
     * со стороной 1 вокруг начала координат. Пустой стек даёт только границы меша.</p>
     */
    @Override
    protected Aabb contentBounds() {
        Aabb mesh = super.contentBounds();
        ScenePlatform platform = ScenePlatform.current();
        Object resolved = nativeItem(platform);
        if (resolved == null) return union(FALLBACK_BOUNDS, mesh);
        Aabb model = platform.itemBounds(resolved, display);
        return model == null ? mesh : union(model, mesh);
    }

    /**
     * Описание для логов и отладки.
     *
     * @return идентификатор или нативный объект.
     */
    @Override
    public String toString() {
        return "ItemNode[" + (id != null ? id : nativeSource) + "]";
    }

    /**
     * Объединяет границы модели и меша.
     *
     * @param model границы модели.
     * @param mesh  границы меша или null.
     * @return общие границы.
     */
    private static Aabb union(Aabb model, Aabb mesh) {
        if (mesh == null) return model;
        return new Aabb(Math.min(model.minX(), mesh.minX()), Math.min(model.minY(), mesh.minY()),
                Math.min(model.minZ(), mesh.minZ()), Math.max(model.maxX(), mesh.maxX()),
                Math.max(model.maxY(), mesh.maxY()), Math.max(model.maxZ(), mesh.maxZ()));
    }

    /**
     * Вызывает платформу для текущего источника.
     *
     * @param platform платформа версии.
     * @return нативный предмет или null.
     */
    private Object resolve(ScenePlatform platform) {
        return id != null ? platform.resolveItem(id) : platform.nativeItem(nativeSource);
    }

    /**
     * Сбрасывает кэш после смены источника.
     */
    private void sourceChanged() {
        item.reset();
        invalidateContent();
    }

    /**
     * Проверяет идентификатор.
     *
     * @param id идентификатор.
     * @return идентификатор без пробелов по краям.
     */
    private static String requireId(String id) {
        String trimmed = Objects.requireNonNull(id, "id").trim();
        if (trimmed.isEmpty()) throw new IllegalArgumentException("Item id is empty");
        return trimmed;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ItemNode setPosition(float x, float y, float z) {
        super.setPosition(x, y, z);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ItemNode setRotation(dev.sixik.unigui.api.math.QuatView value) {
        super.setRotation(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ItemNode setScale(float x, float y, float z) {
        super.setScale(x, y, z);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ItemNode setVisible(boolean visible) {
        super.setVisible(visible);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ItemNode setOrigin(float x, float y, float z) {
        super.setOrigin(x, y, z);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ItemNode centerOrigin() {
        super.centerOrigin();
        return this;
    }
}
