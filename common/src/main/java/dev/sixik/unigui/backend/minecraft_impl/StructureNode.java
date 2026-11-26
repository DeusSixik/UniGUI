package dev.sixik.unigui.backend.minecraft_impl;

import dev.sixik.unigui.api.animation.AnimationEasing;
import dev.sixik.unigui.api.animation.TransitionSpec;
import dev.sixik.unigui.api.math.Aabb;
import dev.sixik.unigui.api.math.MutableMat4;
import dev.sixik.unigui.api.math.MutableVec3;
import dev.sixik.unigui.api.math.Ray;
import dev.sixik.unigui.api.math.Vec3View;
import dev.sixik.unigui.api.scene.Material;
import dev.sixik.unigui.api.scene.Mesh3D;
import dev.sixik.unigui.api.scene.SceneNode;
import dev.sixik.unigui.widgets.rendering.Scene3D;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Разреженная структура с координатами относительно начала узла, одна единица сцены на блок.
 */
public final class StructureNode extends SceneNode {
    /**
     * Лимит распакованного NBT в байтах.
     */
    private static final long NBT_LIMIT = 64L * 1024 * 1024;
    /**
     * Непустые секции 16×16×16, ключом служит упакованная позиция секции Minecraft.
     */
    private final Long2ObjectLinkedOpenHashMap<Section> sections = new Long2ObjectLinkedOpenHashMap<>();
    /**
     * Внешние границы занятых блоков в локальных координатах.
     */
    private Aabb blockBounds;
    /**
     * Границы дополнительного меша, для которых рассчитан общий AABB.
     */
    private Aabb previousMeshBounds;
    /**
     * Объединённые границы блоков и дополнительного меша.
     */
    private Aabb combinedBounds;
    /**
     * Помечает кэш границ при изменении набора блоков.
     */
    private boolean boundsDirty;
    /**
     * Последняя видимая координата блока по Y включительно.
     */
    private int topVisibleY = Integer.MAX_VALUE;
    /**
     * Необязательная упрощённая модель для полностью видимой структуры.
     */
    private DistantMesh distantMesh;

    /**
     * Создаёт пустую структуру.
     */
    public StructureNode() {
    }

    /**
     * Читает блок в локальных координатах.
     *
     * @param pos локальная позиция блока.
     * @return состояние блока или воздух вне структуры.
     */
    public BlockState block(BlockPos pos) {
        Section section = sections.get(SectionPos.asLong(pos));
        return section == null ? Blocks.AIR.defaultBlockState()
                : section.blocks.getOrDefault(pos, Blocks.AIR.defaultBlockState());
    }

    /**
     * Читает объект блока для настройки данных. После изменения данных вызовите {@link #invalidateContent()}.
     *
     * @param pos локальная позиция блока.
     * @return block entity или null.
     */
    public BlockEntity blockEntity(BlockPos pos) {
        Section section = sections.get(SectionPos.asLong(pos));
        return section == null ? null : section.blockEntities.get(pos);
    }

    /**
     * Читает последний видимый слой структуры.
     *
     * @return максимальная видимая координата блока по Y включительно.
     */
    public int topVisibleY() {
        return topVisibleY;
    }

    /**
     * Показывает блоки до указанного слоя включительно. Увеличение границы по одному блоку
     * позволяет проиграть сборку структуры. {@link Integer#MAX_VALUE} показывает все слои.
     *
     * @param topVisibleY максимальная видимая локальная координата блока по Y.
     * @return этот узел.
     */
    public StructureNode setTopVisibleY(int topVisibleY) {
        if (this.topVisibleY == topVisibleY) return this;
        // Учитываем пробы AO и жидкости на расстоянии до двух блоков.
        long firstAffectedY = (long) Math.min(this.topVisibleY, topVisibleY) - 1L;
        int lastAffectedY = Math.max(this.topVisibleY, topVisibleY);
        for (Section section : sections.values()) {
            int bottom = section.pos.minBlockY();
            if (bottom <= lastAffectedY && bottom + 15 >= firstAffectedY) section.revision++;
        }
        this.topVisibleY = topVisibleY;
        boundsDirty = true;
        invalidateContent();
        return this;
    }

    /**
     * Проигрывает сборку снизу вверх. За первый интервал видимых блоков ещё нет.
     * Для прогресса {@code p} от 0 до 1 отображаются слои до
     * {@code floor(lowestY - 1 + p * (highestY - lowestY + 1))}, где границы берутся
     * по блокам не выше {@code targetTopY}. В конце устанавливает {@code targetTopY}.
     *
     * @param viewport   виджет, которому принадлежит узел и который обновляет анимацию.
     * @param seconds    длительность сборки в секундах. Ноль сразу показывает результат.
     * @param targetTopY последний видимый локальный слой после сборки, включительно.
     * @return этот узел.
     * @throws IllegalArgumentException если узел находится в другой сцене, ещё не добавлен или длительность не конечна либо отрицательна.
     */
    public StructureNode playAssembly(Scene3D viewport, float seconds, int targetTopY) {
        if (scene() != viewport.scene() || !Float.isFinite(seconds) || seconds < 0) {
            throw new IllegalArgumentException("Assembly requires an attached structure and a finite nonnegative duration");
        }
        viewport.stopParameterAnimation(this);
        if (seconds == 0) return setTopVisibleY(targetTopY);

        int lowestY = Integer.MAX_VALUE;
        int highestY = Integer.MIN_VALUE;
        for (Section section : sections.values()) {
            if (section.pos.minBlockY() > targetTopY) continue;
            for (BlockPos position : section.blocks.keySet()) {
                int y = position.getY();
                if (y <= targetTopY) {
                    lowestY = Math.min(lowestY, y);
                    highestY = Math.max(highestY, y);
                }
            }
        }
        if (lowestY == Integer.MAX_VALUE) return setTopVisibleY(targetTopY);

        int first = lowestY - 1;
        int count = highestY - first;
        viewport.animateParameterFrom(this, 0, progress -> {
            if (scene() == viewport.scene()) {
                setTopVisibleY(progress >= 1 ? targetTopY : first + (int) (progress * count));
            }
        }, 1, TransitionSpec.of(seconds, AnimationEasing.LINEAR));
        return this;
    }

    /**
     * Задаёт меш, заменяющий ванильную геометрию и модели block entity вдали от камеры.
     * Меш должен лежать в локальных границах блоков. При активном срезе и в ортографическом
     * режиме используются ванильные модели. Выбор блока курсором использует исходные блоки.
     * После редактирования блоков обновите упрощённую модель при необходимости.
     *
     * @param mesh        упрощённая геометрия в координатах структуры или null для отключения.
     * @param material    материал меша или null для {@link Material#DEFAULT}.
     * @param minDistance минимальное расстояние от камеры до мирового AABB структуры в единицах сцены.
     *                    Игнорируется при mesh равном null.
     * @return этот узел.
     * @throws IllegalArgumentException если задан меш с неположительным или неконечным расстоянием.
     */
    public StructureNode setDistantMesh(Mesh3D mesh, Material material, float minDistance) {
        if (mesh != null && (!(minDistance > 0) || !Float.isFinite(minDistance))) {
            throw new IllegalArgumentException("Distant mesh requires a positive finite distance");
        }
        DistantMesh next = mesh == null ? null : new DistantMesh(mesh,
                material == null ? Material.DEFAULT : material, (double) minDistance * minDistance);
        if (!Objects.equals(next, distantMesh)) {
            distantMesh = next;
            invalidateContent();
        }
        return this;
    }

    /**
     * Выбирает дальний меш для камеры вне AABB структуры на заданном расстоянии.
     *
     * @param eye   мировые координаты перспективной камеры или null для ортографической.
     * @param world матрица из координат структуры в мировые.
     * @return подходящий дальний меш или null для ванильной геометрии.
     */
    DistantMesh distantMesh(Vec3View eye, MutableMat4 world) {
        DistantMesh candidate = distantMesh;
        if (candidate == null || eye == null || topVisibleY != Integer.MAX_VALUE) return null;
        contentBounds();
        Aabb bounds = blockBounds;
        if (bounds == null) return null;
        Aabb proxy = candidate.mesh.bounds();
        if (proxy.minX() < bounds.minX() || proxy.minY() < bounds.minY() || proxy.minZ() < bounds.minZ()
                || proxy.maxX() > bounds.maxX() || proxy.maxY() > bounds.maxY() || proxy.maxZ() > bounds.maxZ()) {
            return null;
        }
        double dx = distanceToWorldBounds(bounds, world, 0, eye.x());
        double dy = distanceToWorldBounds(bounds, world, 1, eye.y());
        double dz = distanceToWorldBounds(bounds, world, 2, eye.z());
        return dx * dx + dy * dy + dz * dz >= candidate.distanceSquared ? candidate : null;
    }

    /**
     * Считает расстояние вдоль одной оси до мирового AABB. Для строки R матрицы world и точки камеры e
     * это {@code max(0, |e - (R · center + translation)| - |R| · halfSize)}.
     * Здесь center и halfSize берутся из локальных границ блоков, а |R| берётся поэлементно.
     *
     * @param bounds локальный AABB структуры в блоках.
     * @param world  аффинная матрица из локальной системы в мировую.
     * @param axis   номер мировой оси от 0 до 2.
     * @param eye    мировая координата камеры на этой оси.
     * @return расстояние до AABB вдоль этой оси в мировых единицах.
     */
    private static double distanceToWorldBounds(Aabb bounds, MutableMat4 world, int axis, double eye) {
        double center = world.transformComponent(axis, (bounds.minX() + (double) bounds.maxX()) * 0.5,
                (bounds.minY() + (double) bounds.maxY()) * 0.5, (bounds.minZ() + (double) bounds.maxZ()) * 0.5, 1);
        double radius = (Math.abs(world.get(axis, 0)) * (bounds.maxX() - (double) bounds.minX())
                + Math.abs(world.get(axis, 1)) * (bounds.maxY() - (double) bounds.minY())
                + Math.abs(world.get(axis, 2)) * (bounds.maxZ() - (double) bounds.minZ())) * 0.5;
        return Math.max(0, Math.abs(eye - center) - radius);
    }

    /**
     * Подготовленная упрощённая модель структуры.
     *
     * @param mesh            геометрия в локальных координатах структуры.
     * @param material        материал меша.
     * @param distanceSquared квадрат порога расстояния в мировых единицах.
     */
    record DistantMesh(Mesh3D mesh, Material material, double distanceSquared) {
    }

    /**
     * Заменяет блок. Воздух удаляет запись. При изменении помечаются секции, где геометрия,
     * жидкость или ambient occlusion могут зависеть от изменённой позиции.
     *
     * @param pos   локальная позиция блока в пределах упаковываемых координат Minecraft.
     * @param state состояние для записи.
     * @return этот узел.
     */
    public StructureNode setBlock(BlockPos pos, BlockState state) {
        Objects.requireNonNull(state, "state");
        if (!BlockPos.of(pos.asLong()).equals(pos)) {
            throw new IllegalArgumentException("Structure position exceeds Minecraft block coordinates");
        }
        long key = SectionPos.asLong(pos);
        Section section = sections.get(key);
        BlockState previous = section == null ? null : section.blocks.get(pos);
        if (previous == state) return this;
        if (state.isAir()) {
            if (previous == null) return this;
            section.blocks.remove(pos);
            section.blockEntities.remove(pos);
            if (section.blocks.isEmpty()) sections.remove(key);
        } else {
            BlockPos position = pos.immutable();
            BlockEntity current = section == null ? null : section.blockEntities.get(pos);
            BlockEntity blockEntity = BlockNode.updateBlockEntity(position, state, current);
            if (section == null) {
                section = new Section(SectionPos.of(pos));
                sections.put(key, section);
            }
            section.blocks.put(position, state);
            if (blockEntity == null) section.blockEntities.remove(position);
            else section.blockEntities.put(position, blockEntity);
        }
        // Запросы к соседним блокам включают угловые пробы AO и высоту соседнего столбца жидкости.
        for (int y = (pos.getY() - 2) >> 4; y <= (pos.getY() + 2) >> 4; y++) {
            for (int z = (pos.getZ() - 2) >> 4; z <= (pos.getZ() + 2) >> 4; z++) {
                for (int x = (pos.getX() - 2) >> 4; x <= (pos.getX() + 2) >> 4; x++) {
                    Section affected = sections.get(SectionPos.asLong(x, y, z));
                    if (affected != null) affected.revision++;
                }
            }
        }
        boundsDirty |= pos.getY() <= topVisibleY && (state.isAir() || previous == null);
        invalidateContent();
        return this;
    }

    /**
     * Пересчитывает границы после изменения блоков, включая верхние и боковые грани крайних блоков.
     *
     * @return локальные границы геометрии или null для пустого узла.
     */
    @Override
    protected Aabb contentBounds() {
        Aabb meshBounds = super.contentBounds();
        if (!boundsDirty && previousMeshBounds == meshBounds) return combinedBounds;
        if (boundsDirty) {
            blockBounds = null;
            if (!sections.isEmpty()) {
                int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
                int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
                for (Section section : sections.values()) {
                    if (section.pos.minBlockY() > topVisibleY) continue;
                    for (BlockPos pos : section.blocks.keySet()) {
                        if (pos.getY() > topVisibleY) continue;
                        minX = Math.min(minX, pos.getX());
                        minY = Math.min(minY, pos.getY());
                        minZ = Math.min(minZ, pos.getZ());
                        maxX = Math.max(maxX, pos.getX());
                        maxY = Math.max(maxY, pos.getY());
                        maxZ = Math.max(maxZ, pos.getZ());
                    }
                }
                if (minY != Integer.MAX_VALUE) {
                    blockBounds = new Aabb(minX, minY, minZ, maxX + 1, maxY + 1, maxZ + 1);
                }
            }
            boundsDirty = false;
        }
        previousMeshBounds = meshBounds;
        if (blockBounds == null) combinedBounds = meshBounds;
        else if (meshBounds == null) combinedBounds = blockBounds;
        else combinedBounds = new Aabb(Math.min(blockBounds.minX(), meshBounds.minX()),
                    Math.min(blockBounds.minY(), meshBounds.minY()), Math.min(blockBounds.minZ(), meshBounds.minZ()),
                    Math.max(blockBounds.maxX(), meshBounds.maxX()), Math.max(blockBounds.maxY(), meshBounds.maxY()),
                    Math.max(blockBounds.maxZ(), meshBounds.maxZ()));
        return combinedBounds;
    }

    /**
     * Читает состояние для запекания граней и AO на границе видимых слоёв.
     *
     * @param pos локальная позиция блока.
     * @return состояние или воздух для скрытого блока.
     */
    BlockState visibleBlock(BlockPos pos) {
        return pos.getY() <= topVisibleY ? block(pos) : Blocks.AIR.defaultBlockState();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected double intersectPickBounds(Ray ray, double entry, double limit, MutableVec3 cellOrigin) {
        double nearest = Double.POSITIVE_INFINITY;
        for (Section section : sections.values()) {
            int y = section.pos.minBlockY();
            if (y > topVisibleY) continue;
            int x = section.pos.minBlockX(), z = section.pos.minBlockZ();
            double sectionDistance = ray.intersectAabb(x, y, z, x + 16, Math.min((long) topVisibleY + 1, y + 16), z + 16);
            if (sectionDistance > limit || sectionDistance >= nearest) continue;
            for (BlockPos block : section.blocks.keySet()) {
                int blockY = block.getY();
                if (blockY > topVisibleY) continue;
                int blockX = block.getX(), blockZ = block.getZ();
                double distance = ray.intersectAabb(blockX, blockY, blockZ, blockX + 1, blockY + 1, blockZ + 1);
                if (distance <= limit && distance < nearest) {
                    nearest = distance;
                    cellOrigin.set(blockX, blockY, blockZ);
                }
            }
        }
        return nearest;
    }

    /**
     * Загружает блоки и данные block entity из сжатого файла структуры Minecraft.
     * В многопалитровом шаблоне используется первая палитра.
     *
     * @param source поток с gzip-сжатым NBT. Чтение закрывает поток.
     * @return структура в координатах шаблона.
     * @throws IOException если поток не читается или поля блока не соответствуют формату.
     */
    public static StructureNode fromNbt(InputStream source) throws IOException {
        return fromNbt(SceneStructureNbt.readCompressed(source, NBT_LIMIT));
    }

    /**
     * Читает позиции и состояния блоков из уже загруженного шаблона.
     *
     * @param template NBT шаблона Minecraft с полями palette или palettes и blocks.
     * @return структура в координатах шаблона.
     * @throws IOException если запись блока или индекс палитры недействительны.
     */
    public static StructureNode fromNbt(CompoundTag template) throws IOException {
        return SceneStructureNbt.read(template);
    }

    /**
     * Устанавливает загруженные данные существующего блока при чтении шаблона.
     *
     * @param pos    локальная позиция блока.
     * @param loaded блок-сущность, созданная с состоянием и позицией блока.
     */
    void replaceBlockEntity(BlockPos pos, BlockEntity loaded) {
        sections.get(SectionPos.asLong(pos)).blockEntities.put(pos, loaded);
    }

    /**
     * Читает секции структуры для запекания геометрии.
     *
     * @return коллекция, действительная до изменения структуры.
     */
    Collection<Section> sections() {
        return sections.values();
    }

    /**
     * Изменяемые данные одной секции в координатах структуры.
     */
    static final class Section {
        /**
         * Позиция секции в сетке 16×16×16.
         */
        final SectionPos pos;
        /**
         * Непустые блоки по абсолютной позиции внутри структуры.
         */
        final Map<BlockPos, BlockState> blocks = new LinkedHashMap<>();
        /**
         * Данные и модели block entity по тем же локальным позициям.
         */
        final Map<BlockPos, BlockEntity> blockEntities = new LinkedHashMap<>();
        /**
         * Версия блока или ближайших соседей для кэша геометрии.
         */
        long revision;

        /**
         * Выделяет пустую секцию.
         *
         * @param pos позиция секции в координатах сетки.
         */
        Section(SectionPos pos) {
            this.pos = pos;
        }
    }
}
