package dev.sixik.unigui.api.scene;

import dev.sixik.unigui.api.math.Aabb;
import dev.sixik.unigui.api.math.MutableVec3;
import dev.sixik.unigui.api.math.Ray;

import java.util.Objects;

/**
 * Блок Minecraft с началом координат в нижнем углу, единый для всех версий игры.
 *
 * <p>Блок задаётся строкой, которая работает везде, в том числе в XAML, или нативным объектом версии:</p>
 *
 * <pre>{@code
 * new BlockNode("minecraft:furnace[facing=north,lit=true]");
 * new BlockNode("minecraft:wool@14");                     // meta для 1.7.10 и 1.12.2
 * BlockNode.of(Blocks.FURNACE.defaultBlockState());       // 1.20+ и 26.x
 * BlockNode.of(Blocks.FURNACE.getDefaultState());         // 1.12.2
 * BlockNode.of(Blocks.furnace, 3);                        // 1.7.10
 * }</pre>
 *
 * <p>Строка разрешается лениво через {@link ScenePlatform}: синтаксис проверяется сразу, а существование
 * блока — при первой отрисовке или запросе состояния. Типизированный доступ к состоянию и block entity даёт
 * помощник платформы, например {@code MinecraftScene.state(node)}.</p>
 */
public final class BlockNode extends SceneNode {
    /**
     * Границы полного блока в единицах сцены.
     */
    private static final Aabb BLOCK_BOUNDS = new Aabb(0, 0, 0, 1, 1, 1);

    /**
     * Разобранная строка или null для нативного источника.
     */
    private BlockSpec spec;
    /**
     * Нативный объект пользователя или null для строки.
     */
    private Object nativeSource;
    /**
     * Метаданные нативного источника или {@code -1}.
     */
    private int nativeMeta = -1;
    /**
     * Кэш разрешённого состояния.
     */
    private final ResolvedContent state = new ResolvedContent();
    /**
     * Block entity последнего разрешённого состояния или null.
     */
    private Object blockEntity;
    /**
     * Состояние, для которого создан {@link #blockEntity}.
     */
    private Object blockEntityState;
    /**
     * Платформа, создавшая {@link #blockEntity}.
     */
    private ScenePlatform blockEntityPlatform;

    /**
     * Создаёт узел по строке блока.
     *
     * @param state строка вида {@code minecraft:furnace[facing=north]} или {@code minecraft:wool@14}.
     * @throws IllegalArgumentException если синтаксис строки неверен.
     */
    public BlockNode(String state) {
        this.spec = BlockSpec.parse(state);
    }

    /**
     * Создаёт узел из нативного состояния версии.
     *
     * @param state {@code BlockState}, {@code IBlockState}, блок или строка.
     * @return новый узел.
     * @throws NullPointerException если state равен null.
     */
    public static BlockNode of(Object state) {
        if (state instanceof String text) return new BlockNode(text);
        return of(state, -1);
    }

    /**
     * Создаёт узел из блока и метаданных (1.7.10, 1.12.2).
     *
     * @param block нативный блок.
     * @param meta  метаданные или {@code -1}.
     * @return новый узел.
     * @throws NullPointerException если block равен null.
     */
    public static BlockNode of(Object block, int meta) {
        BlockNode node = new BlockNode((BlockSpec) null);
        node.nativeSource = Objects.requireNonNull(block, "block");
        node.nativeMeta = Math.max(-1, meta);
        return node;
    }

    /**
     * Внутренний конструктор для готовой строки.
     *
     * @param spec разобранная строка или null для нативного источника.
     */
    private BlockNode(BlockSpec spec) {
        this.spec = spec;
    }

    /**
     * Читает строку блока.
     *
     * @return разобранная строка или null, если узел задан нативным объектом.
     */
    public BlockSpec spec() {
        return spec;
    }

    /**
     * Читает нативный объект, которым задан узел.
     *
     * @return объект пользователя или null, если узел задан строкой.
     */
    public Object nativeSource() {
        return nativeSource;
    }

    /**
     * Меняет блок по строке. Одинаковая строка ничего не меняет.
     *
     * @param state строка блока.
     * @return этот узел.
     * @throws IllegalArgumentException если синтаксис строки неверен.
     */
    public BlockNode setState(String state) {
        BlockSpec next = BlockSpec.parse(state);
        if (spec != null && spec.text().equals(next.text())) return this;
        spec = next;
        nativeSource = null;
        nativeMeta = -1;
        sourceChanged();
        return this;
    }

    /**
     * Меняет блок на нативное состояние версии. Тот же объект ничего не меняет.
     *
     * @param state нативное состояние, блок или строка.
     * @return этот узел.
     */
    public BlockNode setState(Object state) {
        if (state instanceof String text) return setState(text);
        return setState(state, -1);
    }

    /**
     * Меняет блок на нативный блок с метаданными.
     *
     * @param block нативный блок.
     * @param meta  метаданные или {@code -1}.
     * @return этот узел.
     */
    public BlockNode setState(Object block, int meta) {
        Objects.requireNonNull(block, "block");
        int normalized = Math.max(-1, meta);
        if (nativeSource == block && nativeMeta == normalized) return this;
        spec = null;
        nativeSource = block;
        nativeMeta = normalized;
        sourceChanged();
        return this;
    }

    /**
     * Разрешает состояние для бэкенда или помощника платформы.
     *
     * @param platform платформа версии или null.
     * @return нативное состояние или null, если оно недоступно или неверно.
     */
    public Object nativeState(ScenePlatform platform) {
        return state.get(platform, this::resolve, this, false);
    }

    /**
     * Разрешает состояние и бросает ошибку неверной строки, например для проверки XAML при загрузке.
     *
     * @param platform платформа версии.
     * @return нативное состояние или null, если блоки ещё недоступны.
     * @throws IllegalArgumentException      если блок неизвестен.
     * @throws UnsupportedOperationException если версия не поддерживает форму строки.
     */
    public Object requireNativeState(ScenePlatform platform) {
        return state.get(Objects.requireNonNull(platform, "platform"), this::resolve, this, true);
    }

    /**
     * Читает block entity текущего состояния. Объект переживает смену на совместимое состояние, поэтому
     * его данные и анимация сохраняются. После правки данных вызовите {@link #invalidateContent()}, если
     * виджет обновляется по изменению.
     *
     * @param platform платформа версии или null.
     * @return нативный block entity или null.
     */
    public Object blockEntity(ScenePlatform platform) {
        Object current = nativeState(platform);
        if (current == null) return null;
        if (current != blockEntityState || platform != blockEntityPlatform) {
            Object previous = platform == blockEntityPlatform ? blockEntity : null;
            blockEntity = platform.blockEntity(current, previous, 0, 0, 0);
            blockEntityState = current;
            blockEntityPlatform = platform;
        }
        return blockEntity;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Aabb contentBounds() {
        return isAir() ? super.contentBounds() : BLOCK_BOUNDS;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected double intersectPickBounds(Ray ray, double entry, double limit, MutableVec3 cellOrigin) {
        if (isAir()) return super.intersectPickBounds(ray, entry, limit, cellOrigin);
        double distance = ray.intersectAabb(0, 0, 0, 1, 1, 1);
        if (distance <= limit) cellOrigin.set(0, 0, 0);
        return distance;
    }

    /**
     * Описание для логов и отладки.
     *
     * @return строка блока или нативный объект.
     */
    @Override
    public String toString() {
        if (spec != null) return "BlockNode[" + spec + "]";
        return "BlockNode[" + nativeSource + (nativeMeta >= 0 ? "@" + nativeMeta : "") + "]";
    }

    /**
     * Проверяет, что установленная платформа считает блок воздухом. Без платформы блок считается полным.
     *
     * @return true для воздуха.
     */
    private boolean isAir() {
        ScenePlatform platform = ScenePlatform.current();
        Object current = nativeState(platform);
        return current != null && platform.isAirBlock(current);
    }

    /**
     * Вызывает платформу для текущего источника.
     *
     * @param platform платформа версии.
     * @return нативное состояние или null.
     */
    private Object resolve(ScenePlatform platform) {
        return spec != null ? platform.resolveBlock(spec) : platform.nativeBlock(nativeSource, nativeMeta);
    }

    /**
     * Сбрасывает кэш после смены источника. Block entity остаётся, чтобы переиспользовать совместимый.
     */
    private void sourceChanged() {
        state.reset();
        invalidateContent();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BlockNode setPosition(float x, float y, float z) {
        super.setPosition(x, y, z);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BlockNode setRotation(dev.sixik.unigui.api.math.QuatView value) {
        super.setRotation(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BlockNode setScale(float x, float y, float z) {
        super.setScale(x, y, z);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BlockNode setVisible(boolean visible) {
        super.setVisible(visible);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BlockNode setOrigin(float x, float y, float z) {
        super.setOrigin(x, y, z);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BlockNode centerOrigin() {
        super.centerOrigin();
        return this;
    }
}
