package dev.sixik.unigui.backend.minecraft_impl;

import dev.sixik.unigui.api.math.Aabb;
import dev.sixik.unigui.api.math.MutableVec3;
import dev.sixik.unigui.api.math.Ray;
import dev.sixik.unigui.api.scene.SceneNode;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Objects;

/**
 * Ванильная модель с началом координат в нижнем углу блока.
 */
public final class BlockNode extends SceneNode {
    /**
     * Границы блока в единицах сцены для выбора мышью.
     */
    private static final Aabb BLOCK_BOUNDS = new Aabb(0, 0, 0, 1, 1, 1);
    /**
     * Состояние блока со всеми свойствами модели.
     */
    private BlockState state;
    /**
     * Block entity из состояния блока, если модель рисует отдельный рендерер.
     */
    private BlockEntity blockEntity;

    /**
     * Создаёт узел для одного блока.
     *
     * @param state состояние блока.
     */
    public BlockNode(BlockState state) {
        this.state = Objects.requireNonNull(state, "state");
        blockEntity = createBlockEntity(BlockPos.ZERO, state);
    }

    /**
     * Читает состояние блока.
     *
     * @return текущее состояние блока.
     */
    public BlockState state() {
        return state;
    }

    /**
     * Обновляет модель блока при изменении его состояния.
     *
     * @param state состояние блока.
     * @return этот узел.
     */
    public BlockNode setState(BlockState state) {
        Objects.requireNonNull(state, "state");
        if (this.state != state) {
            blockEntity = updateBlockEntity(BlockPos.ZERO, state, blockEntity);
            this.state = state;
            invalidateContent();
        }
        return this;
    }

    /**
     * Читает block entity для настройки данных и анимации. После правки данных
     * вызовите {@link #invalidateContent()}, если виджет обновляется по изменению.
     *
     * @return объект ванильного рендерера или null для обычного блока.
     */
    public BlockEntity blockEntity() {
        return blockEntity;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Aabb contentBounds() {
        return state.isAir() ? super.contentBounds() : BLOCK_BOUNDS;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected double intersectPickBounds(Ray ray, double entry, double limit, MutableVec3 cellOrigin) {
        if (state.isAir()) return super.intersectPickBounds(ray, entry, limit, cellOrigin);
        double distance = ray.intersectAabb(0, 0, 0, 1, 1, 1);
        if (distance <= limit) cellOrigin.set(0, 0, 0);
        return distance;
    }

    /**
     * Создаёт block entity вне игрового уровня.
     *
     * @param pos   локальная позиция блока.
     * @param state состояние блока.
     * @return block entity либо null для блока без неё.
     */
    static BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return state.getBlock() instanceof EntityBlock factory ? factory.newBlockEntity(pos, state) : null;
    }

    /**
     * Обновляет состояние совместимого block entity с сохранением его данных и анимации.
     *
     * @param pos      локальная позиция блока.
     * @param state    новое состояние блока.
     * @param previous текущий block entity или null.
     * @return совместимый block entity или null для блока без неё.
     */
    static BlockEntity updateBlockEntity(BlockPos pos, BlockState state, BlockEntity previous) {
        if (previous != null && previous.getType().isValid(state)) {
            previous.setBlockState(state);
            return previous;
        }
        return createBlockEntity(pos, state);
    }
}
