package dev.sixik.unigui.api.scene;

import dev.sixik.unigui.api.math.Aabb;

import java.util.Objects;

/**
 * Модель сущности Minecraft, стоящая ногами в начале координат, единая для всех версий игры.
 *
 * <pre>{@code
 * new EntityNode("minecraft:zombie");      // превью по типу
 * EntityNode.of(entity);                   // готовая сущность версии
 * EntityNode.of(EntityType.ZOMBIE);        // тип версии
 * }</pre>
 *
 * <p>Превью по типу создаётся, когда у клиента есть мир, и пересоздаётся при смене мира. Без мира узел
 * ничего не рисует и повторяет попытку позже. Типизированный доступ к сущности даёт помощник платформы,
 * например {@code MinecraftScene.entity(node)}. Версии без поддержки сущностей в сцене ничего не рисуют и
 * один раз пишут предупреждение в лог.</p>
 */
public final class EntityNode extends SceneNode {
    /**
     * Идентификатор типа или null для нативного источника.
     */
    private final String typeId;
    /**
     * Нативный объект пользователя или null для идентификатора.
     */
    private final Object nativeSource;
    /**
     * Кэш разрешённой сущности.
     */
    private final ResolvedContent entity = new ResolvedContent();
    /**
     * Задан ли поворот головы. Без него голова смотрит так, как её оставила сущность.
     */
    private boolean head;
    /**
     * Поворот головы относительно тела в радианах: рыскание и тангаж.
     */
    private float headYaw, headPitch;

    /**
     * Создаёт превью сущности по идентификатору типа.
     *
     * @param typeId идентификатор, например {@code minecraft:zombie}.
     * @throws IllegalArgumentException если идентификатор пуст.
     */
    public EntityNode(String typeId) {
        String trimmed = Objects.requireNonNull(typeId, "typeId").trim();
        if (trimmed.isEmpty()) throw new IllegalArgumentException("Entity type id is empty");
        this.typeId = trimmed;
        this.nativeSource = null;
    }

    /**
     * Внутренний конструктор для нативного источника.
     *
     * @param source объект версии.
     */
    private EntityNode(Object source) {
        this.typeId = null;
        this.nativeSource = Objects.requireNonNull(source, "entity");
    }

    /**
     * Создаёт узел из нативной сущности, её типа или строки-идентификатора.
     *
     * @param entity объект версии.
     * @return новый узел.
     * @throws NullPointerException если entity равен null.
     */
    public static EntityNode of(Object entity) {
        if (entity instanceof String text) return new EntityNode(text);
        return new EntityNode(entity);
    }

    /**
     * Поворачивает голову относительно тела, как это делает Minecraft: рыскание 0 смотрит вперёд (+Z узла),
     * положительное поворачивает к правому плечу сущности, положительный тангаж наклоняет голову вниз.
     * Тело поворачивают через {@link #setRotation}.
     *
     * @param yaw   рыскание в радианах.
     * @param pitch тангаж в радианах.
     * @return этот узел.
     * @throws IllegalArgumentException если угол не конечен.
     */
    public EntityNode setHead(float yaw, float pitch) {
        if (!Float.isFinite(yaw) || !Float.isFinite(pitch)) throw new IllegalArgumentException("Head angles must be finite");
        if (!head || headYaw != yaw || headPitch != pitch) {
            head = true;
            headYaw = yaw;
            headPitch = pitch;
            invalidateContent();
        }
        return this;
    }

    /**
     * Возвращает голове позу самой сущности.
     *
     * @return этот узел.
     */
    public EntityNode clearHead() {
        if (head) {
            head = false;
            invalidateContent();
        }
        return this;
    }

    /**
     * @return задан ли поворот головы через {@link #setHead}.
     */
    public boolean hasHead() {
        return head;
    }

    /**
     * @return рыскание головы относительно тела в радианах.
     */
    public float headYaw() {
        return headYaw;
    }

    /**
     * @return тангаж головы в радианах, положительный — вниз.
     */
    public float headPitch() {
        return headPitch;
    }

    /**
     * @return идентификатор типа или null для нативного источника.
     */
    public String typeId() {
        return typeId;
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
     * Разрешает сущность для бэкенда или помощника платформы.
     *
     * @param platform платформа версии или null.
     * @return нативная сущность платформы или null, если она недоступна или неверна.
     */
    public Object nativeEntity(ScenePlatform platform) {
        return entity.get(platform, this::resolve, this, false);
    }

    /**
     * Разрешает сущность и бросает ошибку неверного источника.
     *
     * @param platform платформа версии.
     * @return нативная сущность или null, если её пока нельзя создать.
     * @throws IllegalArgumentException      если тип неизвестен.
     * @throws UnsupportedOperationException если версия не рисует сущности в сцене.
     */
    public Object requireNativeEntity(ScenePlatform platform) {
        return entity.get(Objects.requireNonNull(platform, "platform"), this::resolve, this, true);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Границы берутся из размеров сущности, а без них — из собственного меша узла.</p>
     */
    @Override
    protected Aabb contentBounds() {
        ScenePlatform platform = ScenePlatform.current();
        Object resolved = nativeEntity(platform);
        Aabb bounds = resolved == null ? null : platform.entityBounds(resolved);
        return bounds != null ? bounds : super.contentBounds();
    }

    /**
     * Описание для логов и отладки.
     *
     * @return идентификатор или нативный объект.
     */
    @Override
    public String toString() {
        return "EntityNode[" + (typeId != null ? typeId : nativeSource) + "]";
    }

    /**
     * Вызывает платформу для текущего источника.
     *
     * @param platform платформа версии.
     * @return нативная сущность или null.
     */
    private Object resolve(ScenePlatform platform) {
        return typeId != null ? platform.resolveEntity(typeId) : platform.nativeEntity(nativeSource);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EntityNode setPosition(float x, float y, float z) {
        super.setPosition(x, y, z);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EntityNode setRotation(dev.sixik.unigui.api.math.QuatView value) {
        super.setRotation(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EntityNode setScale(float x, float y, float z) {
        super.setScale(x, y, z);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EntityNode setVisible(boolean visible) {
        super.setVisible(visible);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EntityNode setOrigin(float x, float y, float z) {
        super.setOrigin(x, y, z);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EntityNode centerOrigin() {
        super.centerOrigin();
        return this;
    }
}
