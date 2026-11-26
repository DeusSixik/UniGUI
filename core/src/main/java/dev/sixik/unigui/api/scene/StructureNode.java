package dev.sixik.unigui.api.scene;

import dev.sixik.unigui.api.animation.AnimationEasing;
import dev.sixik.unigui.api.animation.TransitionSpec;
import dev.sixik.unigui.api.math.Aabb;
import dev.sixik.unigui.api.math.MutableMat4;
import dev.sixik.unigui.api.math.MutableVec3;
import dev.sixik.unigui.api.math.Ray;
import dev.sixik.unigui.api.math.Vec3View;
import dev.sixik.unigui.widgets.rendering.Scene3D;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Objects;

/**
 * Разреженная структура блоков, общая для всех версий Minecraft. Координаты блоков целые и задаются относительно
 * начала узла, одна единица сцены на блок.
 *
 * <p>Структура хранит нативные состояния платформы ({@link ScenePlatform}): строки и объекты пользователя
 * приводятся к ним сразу при записи. Платформа фиксируется при первой записи: это переданная в конструктор
 * платформа или установленная через {@link ScenePlatform#install}. Чтение файлов структур и запекание секций
 * выполняют платформа и её рендерер.</p>
 *
 * <p>Блоки сгруппированы в секции 16×16×16. Секция хранит плотный массив из {@value Section#VOLUME} ячеек и
 * версию, которую рендерер сравнивает со своим кэшем геометрии.</p>
 *
 * <pre>{@code
 * StructureNode tower = new StructureNode();
 * for (int y = 0; y < 4; y++) tower.setBlock(0, y, 0, "minecraft:stone_bricks");
 * tower.setBlock(0, 4, 0, "minecraft:lantern[hanging=false]");
 * }</pre>
 */
public final class StructureNode extends SceneNode {
    /**
     * Наименьшая координата блока по X и Z, совпадает с пределом упаковки позиции Minecraft.
     */
    public static final int MIN_HORIZONTAL = -(1 << 25);
    /**
     * Наибольшая координата блока по X и Z.
     */
    public static final int MAX_HORIZONTAL = (1 << 25) - 1;
    /**
     * Наименьшая координата блока по Y.
     */
    public static final int MIN_Y = -2048;
    /**
     * Наибольшая координата блока по Y.
     */
    public static final int MAX_Y = 2047;

    /**
     * Непустые секции в порядке создания по упакованным координатам секции.
     */
    private final LinkedHashMap<Long, Section> sections = new LinkedHashMap<>();
    /**
     * Только для чтения представление секций для рендереров.
     */
    private final Collection<Section> sectionView = Collections.unmodifiableCollection(sections.values());
    /**
     * Платформа нативных состояний или null до первой записи.
     */
    private ScenePlatform platform;
    /**
     * Внешние границы видимых блоков в локальных координатах.
     */
    private Aabb blockBounds;
    /**
     * Границы меша узла, для которых рассчитан общий AABB.
     */
    private Aabb previousMeshBounds;
    /**
     * Объединённые границы блоков и меша узла.
     */
    private Aabb combinedBounds;
    /**
     * Помечает кэш границ при изменении набора видимых блоков.
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
     * Нижний и верхний непустые слои для сборки или null, если их нужно пересчитать.
     */
    private int[] layers;
    /**
     * Число непустых блоков.
     */
    private int blockCount;

    /**
     * Создаёт пустую структуру. Платформа берётся из {@link ScenePlatform#current()} при первой записи.
     */
    public StructureNode() {
    }

    /**
     * Создаёт пустую структуру с явной платформой, например при чтении файла самой платформой.
     *
     * @param platform платформа нативных состояний.
     */
    public StructureNode(ScenePlatform platform) {
        this.platform = Objects.requireNonNull(platform, "platform");
    }

    /**
     * Читает платформу, которой принадлежат нативные состояния структуры.
     *
     * @return платформа или null, если в структуру ещё ничего не записано.
     */
    public ScenePlatform platform() {
        return platform;
    }

    /**
     * Считает непустые блоки.
     *
     * @return число блоков, включая скрытые срезом {@link #topVisibleY()}.
     */
    public int blockCount() {
        return blockCount;
    }

    /**
     * Читает нативное состояние блока.
     *
     * @param x локальная координата X.
     * @param y локальная координата Y.
     * @param z локальная координата Z.
     * @return состояние или null для пустой ячейки и позиции вне структуры.
     */
    public Object block(int x, int y, int z) {
        Section section = section(x, y, z);
        return section == null ? null : section.states[Section.index(x, y, z)];
    }

    /**
     * Читает блок с учётом среза слоёв. Рендереры используют его для отсечения граней и AO у верхнего видимого слоя.
     *
     * @param x локальная координата X.
     * @param y локальная координата Y.
     * @param z локальная координата Z.
     * @return состояние или null для пустой либо скрытой ячейки.
     */
    public Object visibleBlock(int x, int y, int z) {
        return y <= topVisibleY ? block(x, y, z) : null;
    }

    /**
     * Читает block entity для настройки данных. После изменения данных вызовите {@link #invalidateContent()}.
     *
     * @param x локальная координата X.
     * @param y локальная координата Y.
     * @param z локальная координата Z.
     * @return нативный block entity или null.
     */
    public Object blockEntity(int x, int y, int z) {
        Section section = section(x, y, z);
        return section == null ? null : section.blockEntity(Section.index(x, y, z));
    }

    /**
     * Ставит блок по строке вида {@code minecraft:furnace[facing=south]} или {@code minecraft:wool@14}.
     *
     * @param x     локальная координата X.
     * @param y     локальная координата Y.
     * @param z     локальная координата Z.
     * @param block строка блока. Воздух удаляет блок.
     * @return этот узел.
     * @throws IllegalArgumentException если строка некорректна, блок неизвестен или позиция вне допустимых границ.
     * @throws IllegalStateException    если платформа не установлена или блоки ещё не загружены.
     */
    public StructureNode setBlock(int x, int y, int z, String block) {
        BlockSpec spec = BlockSpec.parse(block);
        checkPosition(x, y, z);
        ScenePlatform target = writer();
        return put(x, y, z, available(target.resolveBlock(spec), block));
    }

    /**
     * Ставит нативное состояние или блок с состоянием по умолчанию.
     *
     * @param x     локальная координата X.
     * @param y     локальная координата Y.
     * @param z     локальная координата Z.
     * @param block {@code BlockState}, {@code IBlockState}, {@code Block} или нативное состояние платформы.
     * @return этот узел.
     * @throws IllegalArgumentException если тип объекта не подходит версии или позиция вне границ.
     * @throws IllegalStateException    если платформа не установлена.
     */
    public StructureNode setBlock(int x, int y, int z, Object block) {
        return setBlock(x, y, z, block, -1);
    }

    /**
     * Ставит блок с метаданными на 1.7.10 и 1.12.2.
     *
     * @param x     локальная координата X.
     * @param y     локальная координата Y.
     * @param z     локальная координата Z.
     * @param block блок версии.
     * @param meta  метаданные от 0 до 15 или {@code -1}.
     * @return этот узел.
     * @throws IllegalArgumentException если тип объекта или метаданные не подходят версии.
     * @throws IllegalStateException    если платформа не установлена.
     */
    public StructureNode setBlock(int x, int y, int z, Object block, int meta) {
        Objects.requireNonNull(block, "block");
        checkPosition(x, y, z);
        ScenePlatform target = writer();
        return put(x, y, z, available(target.nativeBlock(block, meta), block));
    }

    /**
     * Удаляет блок и его block entity.
     *
     * @param x локальная координата X.
     * @param y локальная координата Y.
     * @param z локальная координата Z.
     * @return этот узел.
     */
    public StructureNode removeBlock(int x, int y, int z) {
        if (!inBounds(x, y, z) || block(x, y, z) == null) return this;
        return put(x, y, z, null);
    }

    /**
     * Заменяет block entity существующего блока, например данными из файла структуры.
     *
     * @param x            локальная координата X.
     * @param y            локальная координата Y.
     * @param z            локальная координата Z.
     * @param blockEntity  нативный block entity или null для удаления.
     * @return этот узел.
     * @throws IllegalArgumentException если в позиции нет блока.
     */
    public StructureNode setBlockEntity(int x, int y, int z, Object blockEntity) {
        Section section = section(x, y, z);
        int index = Section.index(x, y, z);
        if (section == null || section.states[index] == null) {
            throw new IllegalArgumentException("No block at " + x + ", " + y + ", " + z);
        }
        if (section.blockEntity(index) != blockEntity) {
            section.setBlockEntity(index, blockEntity);
            invalidateContent();
        }
        return this;
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
            int bottom = section.minBlockY();
            if (bottom <= lastAffectedY && bottom + 15 >= firstAffectedY) section.revision++;
        }
        this.topVisibleY = topVisibleY;
        boundsDirty = true;
        invalidateContent();
        return this;
    }

    /**
     * Находит нижний и верхний непустые слои не выше границы. Результат без границы кэшируется до изменения
     * набора блоков.
     *
     * @param targetTopY верхняя граница поиска, включительно.
     * @return массив {@code {lowestY, highestY}} или null для пустой структуры.
     */
    int[] layerRange(int targetTopY) {
        boolean cacheable = targetTopY == Integer.MAX_VALUE;
        if (cacheable && layers != null) return layers.length == 0 ? null : layers;
        int lowestY = Integer.MAX_VALUE;
        int highestY = Integer.MIN_VALUE;
        for (Section section : sections.values()) {
            int bottom = section.minBlockY();
            if (bottom > targetTopY) continue;
            for (int index = 0; index < Section.VOLUME; index++) {
                if (section.states[index] == null) continue;
                int y = bottom + Section.localY(index);
                if (y <= targetTopY) {
                    lowestY = Math.min(lowestY, y);
                    highestY = Math.max(highestY, y);
                }
            }
        }
        int[] range = lowestY == Integer.MAX_VALUE ? new int[0] : new int[]{lowestY, highestY};
        if (cacheable) layers = range;
        return range.length == 0 ? null : range;
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
     * @throws IllegalArgumentException если узел находится в другой сцене, ещё не добавлен или длительность не конечна
     *                                  либо отрицательна.
     */
    public StructureNode playAssembly(Scene3D viewport, float seconds, int targetTopY) {
        if (scene() != viewport.scene() || !Float.isFinite(seconds) || seconds < 0) {
            throw new IllegalArgumentException("Assembly requires an attached structure and a finite nonnegative duration");
        }
        viewport.stopParameterAnimation(this);
        if (seconds == 0) return setTopVisibleY(targetTopY);

        int[] range = layerRange(targetTopY);
        if (range == null) return setTopVisibleY(targetTopY);

        int first = range[0] - 1;
        int count = range[1] - first;
        viewport.animateParameterFrom(this, 0, progress -> {
            if (scene() == viewport.scene()) {
                setTopVisibleY(progress >= 1 ? targetTopY : first + (int) (progress * count));
            }
        }, 1, TransitionSpec.of(seconds, AnimationEasing.LINEAR));
        return this;
    }

    /**
     * Задаёт меш, заменяющий геометрию блоков и модели block entity вдали от камеры.
     * Меш должен лежать в локальных границах блоков. При активном срезе и в ортографическом
     * режиме используются модели блоков. Выбор блока курсором использует исходные блоки.
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
     * Выбирает дальний меш для камеры вне AABB структуры на заданном расстоянии. Вызывается рендерером.
     *
     * @param eye   мировые координаты перспективной камеры или null для ортографической.
     * @param world матрица из координат структуры в мировые.
     * @return подходящий дальний меш или null для геометрии блоков.
     */
    public DistantMesh distantMesh(Vec3View eye, MutableMat4 world) {
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
    public record DistantMesh(Mesh3D mesh, Material material, double distanceSquared) {
    }

    /**
     * Читает секции структуры для запекания геометрии.
     *
     * @return неизменяемое представление, отражающее последующие изменения структуры.
     */
    public Collection<Section> sections() {
        return sectionView;
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
            int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
            for (Section section : sections.values()) {
                int bottom = section.minBlockY();
                if (bottom > topVisibleY) continue;
                int west = section.minBlockX(), north = section.minBlockZ();
                for (int index = 0; index < Section.VOLUME; index++) {
                    if (section.states[index] == null) continue;
                    int y = bottom + Section.localY(index);
                    if (y > topVisibleY) continue;
                    int x = west + Section.localX(index), z = north + Section.localZ(index);
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    minZ = Math.min(minZ, z);
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, y);
                    maxZ = Math.max(maxZ, z);
                }
            }
            if (minY != Integer.MAX_VALUE) blockBounds = new Aabb(minX, minY, minZ, maxX + 1, maxY + 1, maxZ + 1);
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
     * {@inheritDoc}
     */
    @Override
    protected double intersectPickBounds(Ray ray, double entry, double limit, MutableVec3 cellOrigin) {
        double nearest = Double.POSITIVE_INFINITY;
        for (Section section : sections.values()) {
            int y = section.minBlockY();
            if (y > topVisibleY) continue;
            int x = section.minBlockX(), z = section.minBlockZ();
            double sectionDistance = ray.intersectAabb(x, y, z, x + 16, Math.min((long) topVisibleY + 1, y + 16), z + 16);
            if (sectionDistance > limit || sectionDistance >= nearest) continue;
            for (int index = 0; index < Section.VOLUME; index++) {
                if (section.states[index] == null) continue;
                int blockY = y + Section.localY(index);
                if (blockY > topVisibleY) continue;
                int blockX = x + Section.localX(index), blockZ = z + Section.localZ(index);
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
     * Записывает приведённое состояние. Воздух и null удаляют запись. При изменении помечаются секции, где
     * геометрия, жидкость или ambient occlusion могут зависеть от изменённой позиции.
     *
     * @param x     локальная координата X в допустимых границах.
     * @param y     локальная координата Y в допустимых границах.
     * @param z     локальная координата Z в допустимых границах.
     * @param state нативное состояние или null.
     * @return этот узел.
     */
    private StructureNode put(int x, int y, int z, Object state) {
        if (state != null && platform.isAirBlock(state)) state = null;
        long key = sectionKey(x >> 4, y >> 4, z >> 4);
        Section section = sections.get(key);
        int index = Section.index(x, y, z);
        Object previous = section == null ? null : section.states[index];
        if (Objects.equals(previous, state)) return this;
        if (state == null) {
            section.states[index] = null;
            section.setBlockEntity(index, null);
            if (--section.blockCount == 0) sections.remove(key);
            blockCount--;
        } else {
            if (section == null) {
                section = new Section(x >> 4, y >> 4, z >> 4);
                sections.put(key, section);
            }
            if (previous == null) {
                section.blockCount++;
                blockCount++;
            }
            section.states[index] = state;
            section.setBlockEntity(index, platform.blockEntity(state, section.blockEntity(index), x, y, z));
        }
        // Запросы к соседним блокам включают угловые пробы AO и высоту соседнего столбца жидкости.
        for (int sy = (y - 2) >> 4; sy <= (y + 2) >> 4; sy++) {
            for (int sz = (z - 2) >> 4; sz <= (z + 2) >> 4; sz++) {
                for (int sx = (x - 2) >> 4; sx <= (x + 2) >> 4; sx++) {
                    Section affected = sections.get(sectionKey(sx, sy, sz));
                    if (affected != null) affected.revision++;
                }
            }
        }
        boundsDirty |= y <= topVisibleY && (state == null || previous == null);
        if (state == null || previous == null) layers = null;
        invalidateContent();
        return this;
    }

    /**
     * Фиксирует платформу записи.
     *
     * @return платформа структуры.
     * @throws IllegalStateException если платформа не задана и не установлена.
     */
    private ScenePlatform writer() {
        if (platform == null) {
            platform = ScenePlatform.current();
            if (platform == null) {
                throw new IllegalStateException("No scene platform is installed; create the structure on the client "
                        + "or pass a platform to the constructor");
            }
        }
        return platform;
    }

    /**
     * Проверяет, что платформа уже может создать состояние.
     *
     * @param state  результат платформы.
     * @param source исходный блок для сообщения.
     * @return непустое состояние.
     * @throws IllegalStateException если блоки ещё не загружены.
     */
    private static Object available(Object state, Object source) {
        if (state == null) throw new IllegalStateException("Blocks are not available yet: " + source);
        return state;
    }

    /**
     * Находит секцию блока.
     *
     * @param x координата блока X.
     * @param y координата блока Y.
     * @param z координата блока Z.
     * @return секция или null.
     */
    private Section section(int x, int y, int z) {
        return inBounds(x, y, z) ? sections.get(sectionKey(x >> 4, y >> 4, z >> 4)) : null;
    }

    /**
     * Проверяет координаты по пределам позиции блока Minecraft.
     *
     * @param x координата X.
     * @param y координата Y.
     * @param z координата Z.
     * @return true, если позиция представима.
     */
    private static boolean inBounds(int x, int y, int z) {
        return x >= MIN_HORIZONTAL && x <= MAX_HORIZONTAL && z >= MIN_HORIZONTAL && z <= MAX_HORIZONTAL
                && y >= MIN_Y && y <= MAX_Y;
    }

    /**
     * Отклоняет позицию вне пределов.
     *
     * @param x координата X.
     * @param y координата Y.
     * @param z координата Z.
     * @throws IllegalArgumentException если позиция не представима в Minecraft.
     */
    private static void checkPosition(int x, int y, int z) {
        if (!inBounds(x, y, z)) {
            throw new IllegalArgumentException("Structure position exceeds Minecraft block coordinates: "
                    + x + ", " + y + ", " + z);
        }
    }

    /**
     * Упаковывает координаты секции: 22 бита X, 22 бита Z и 8 бит Y.
     *
     * @param x координата секции X.
     * @param y координата секции Y.
     * @param z координата секции Z.
     * @return ключ секции.
     */
    private static long sectionKey(int x, int y, int z) {
        return ((long) (x & 0x3FFFFF) << 30) | ((long) (z & 0x3FFFFF) << 8) | (y & 0xFF);
    }

    /**
     * Одна секция 16×16×16. Ячейка с индексом {@code (y << 8) | (z << 4) | x} хранит состояние блока
     * с локальными координатами секции x, y, z.
     */
    public static final class Section {
        /**
         * Число ячеек секции.
         */
        public static final int VOLUME = 16 * 16 * 16;
        /**
         * Координаты секции в сетке 16×16×16.
         */
        private final int x, y, z;
        /**
         * Нативные состояния по индексу ячейки или null.
         */
        private final Object[] states = new Object[VOLUME];
        /**
         * Block entity по индексу ячейки. Создаётся с первым таким блоком.
         */
        private Object[] blockEntities;
        /**
         * Число непустых ячеек.
         */
        private int blockCount;
        /**
         * Число block entity.
         */
        private int blockEntityCount;
        /**
         * Версия блоков секции и ближайших соседей для кэша геометрии.
         */
        private long revision;

        /**
         * Создаёт пустую секцию.
         *
         * @param x координата секции X.
         * @param y координата секции Y.
         * @param z координата секции Z.
         */
        private Section(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        /**
         * Вычисляет индекс ячейки по координатам блока.
         *
         * @param x координата блока X в структуре.
         * @param y координата блока Y в структуре.
         * @param z координата блока Z в структуре.
         * @return индекс от 0 до {@link #VOLUME} - 1.
         */
        public static int index(int x, int y, int z) {
            return ((y & 15) << 8) | ((z & 15) << 4) | (x & 15);
        }

        /**
         * @param index индекс ячейки.
         * @return локальная координата X внутри секции.
         */
        public static int localX(int index) {
            return index & 15;
        }

        /**
         * @param index индекс ячейки.
         * @return локальная координата Y внутри секции.
         */
        public static int localY(int index) {
            return index >>> 8;
        }

        /**
         * @param index индекс ячейки.
         * @return локальная координата Z внутри секции.
         */
        public static int localZ(int index) {
            return (index >>> 4) & 15;
        }

        /**
         * @return координата X первого блока секции.
         */
        public int minBlockX() {
            return x << 4;
        }

        /**
         * @return координата Y первого блока секции.
         */
        public int minBlockY() {
            return y << 4;
        }

        /**
         * @return координата Z первого блока секции.
         */
        public int minBlockZ() {
            return z << 4;
        }

        /**
         * Читает версию для сравнения с кэшем геометрии.
         *
         * @return число изменений блоков секции, соседей и среза слоёв.
         */
        public long revision() {
            return revision;
        }

        /**
         * @return число непустых ячеек.
         */
        public int blockCount() {
            return blockCount;
        }

        /**
         * @return число block entity в секции.
         */
        public int blockEntityCount() {
            return blockEntityCount;
        }

        /**
         * Читает состояние ячейки.
         *
         * @param index индекс ячейки.
         * @return нативное состояние или null.
         */
        public Object state(int index) {
            return states[index];
        }

        /**
         * Читает block entity ячейки.
         *
         * @param index индекс ячейки.
         * @return нативный block entity или null.
         */
        public Object blockEntity(int index) {
            return blockEntities == null ? null : blockEntities[index];
        }

        /**
         * Заменяет block entity ячейки и обновляет счётчик.
         *
         * @param index индекс ячейки.
         * @param value новый объект или null.
         */
        private void setBlockEntity(int index, Object value) {
            if (blockEntities == null) {
                if (value == null) return;
                blockEntities = new Object[VOLUME];
            }
            Object previous = blockEntities[index];
            if (previous == null && value != null) blockEntityCount++;
            else if (previous != null && value == null) blockEntityCount--;
            blockEntities[index] = value;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StructureNode setPosition(float x, float y, float z) {
        super.setPosition(x, y, z);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StructureNode setRotation(dev.sixik.unigui.api.math.QuatView value) {
        super.setRotation(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StructureNode setScale(float x, float y, float z) {
        super.setScale(x, y, z);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StructureNode setVisible(boolean visible) {
        super.setVisible(visible);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StructureNode setOrigin(float x, float y, float z) {
        super.setOrigin(x, y, z);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StructureNode centerOrigin() {
        super.centerOrigin();
        return this;
    }
}
