package dev.sixik.unigui.backend.minecraft_impl;

import dev.sixik.unigui.api.math.Aabb;

import com.mojang.blaze3d.vertex.QuadInstance;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.sixik.unigui.api.render.SimpleTextureHandle;
import dev.sixik.unigui.api.scene.Material;
import dev.sixik.unigui.api.scene.Mesh3D;
import dev.sixik.unigui.api.scene.StructureNode;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.BlockStateModelSet;
import net.minecraft.client.renderer.block.FluidRenderer;
import net.minecraft.client.renderer.block.FluidStateModelSet;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.CardinalLighting;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import org.joml.Vector3fc;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Запекает геометрию ванильных блоков и жидкостей в общий формат сцены Minecraft 26.x.
 */
final class SceneBlockMeshes {
    /**
     * Neutral cardinal shade retains vertex tint and AO for scene-lit surfaces.
     */
    private static final CardinalLighting NEUTRAL_CARDINAL = new CardinalLighting(1, 1, 1, 1, 1, 1);
    /**
     * Surface mode of this bake cache.
     */
    private final Material.Shading shading;
    /**
     * Предел числа переиспользуемых состояний одиночных блоков.
     */
    private static final int SINGLE_LIMIT = 256;
    /**
     * Равнинный цвет травы, листвы и воды для сцены вне игрового мира.
     */
    private static final Biome PREVIEW_BIOME = new Biome.BiomeBuilder().hasPrecipitation(true)
            .temperature(0.8f).downfall(0.4f).generationSettings(BiomeGenerationSettings.EMPTY)
            .mobSpawnSettings(MobSpawnSettings.EMPTY)
            .specialEffects(new BiomeSpecialEffects.Builder().waterColor(0x3F76E4).build()).build();

    /**
     * Кэш одиночных блоков по состоянию и давности использования.
     */
    private final LinkedHashMap<BlockState, List<Surface>> singles = new LinkedHashMap<>(16, 0.75f, true);
    /**
     * Кэш секций с версией. Удалённые секции не удерживаются.
     */
    private final Map<StructureNode.Section, BakedSection> sections = new WeakHashMap<>();
    /**
     * Материалы атласов по слою, общие в пределах загрузки ресурсов.
     */
    private final Map<SurfaceKey, Material> materials = new LinkedHashMap<>();
    /**
     * Рабочий источник блоков для отсечения, AO, tint и жидкостей.
     */
    private final BlockView view;
    /**
     * Текущие модели блоков Minecraft. Идентичность меняется при перезагрузке ресурсов.
     */
    private BlockStateModelSet models;
    /**
     * Текущие модели жидкостей Minecraft.
     */
    private FluidStateModelSet fluidModels;
    /**
     * Ванильный тесселятор блоков с включённым AO и отсечением граней.
     */
    private ModelBlockRenderer blocks;
    /**
     * Ванильный тесселятор жидкостей.
     */
    private FluidRenderer fluids;

    /**
     * Создаёт пустые кэши для одного рендер-бэкенда.
     */
    SceneBlockMeshes() {
        this(false);
    }

    /**
     * Creates a separate bake cache for vanilla lighting or real-time scene lighting.
     *
     * @param relight whether to bake tint and AO with neutral directional shading
     */
    SceneBlockMeshes(boolean relight) {
        shading = relight ? Material.Shading.LIT : Material.Shading.UNLIT;
        view = new BlockView(relight ? NEUTRAL_CARDINAL : CardinalLighting.DEFAULT);
    }

    /**
     * Получает запечённую модель одиночного блока в начале координат.
     *
     * @param state состояние блока.
     * @return слои видимой геометрии.
     */
    List<Surface> single(BlockState state) {
        refreshModels();
        List<Surface> cached = singles.get(state);
        if (cached != null) return cached;
        Bake bake = new Bake(0, 0, 0);
        view.single = state;
        try {
            bake.addBlock(BlockPos.ZERO, state);
        } finally {
            view.single = null;
        }
        cached = bake.finish();
        singles.put(state, cached);
        if (singles.size() > SINGLE_LIMIT) singles.pollFirstEntry();
        return cached;
    }

    /**
     * Получает слои секции, перестраивая их после локальных изменений или перезагрузки ресурсов.
     *
     * @param structure источник соседей секции.
     * @param section   запекаемая секция.
     * @return слои в координатах структуры.
     */
    List<Surface> section(StructureNode structure, StructureNode.Section section) {
        refreshModels();
        BakedSection cached = sections.get(section);
        if (cached != null && cached.revision == section.revision()) return cached.surfaces;
        int west = section.minBlockX(), bottom = section.minBlockY(), north = section.minBlockZ();
        Bake bake = new Bake(west, bottom, north);
        view.structure = structure;
        try {
            int topY = structure.topVisibleY();
            for (int index = 0; index < StructureNode.Section.VOLUME; index++) {
                Object state = section.state(index);
                if (state == null) continue;
                int y = bottom + StructureNode.Section.localY(index);
                if (y > topY) continue;
                bake.addBlock(new BlockPos(west + StructureNode.Section.localX(index), y,
                        north + StructureNode.Section.localZ(index)), (BlockState) state);
            }
        } finally {
            view.structure = null;
        }
        List<Surface> surfaces = bake.finish();
        sections.put(section, new BakedSection(section.revision(), surfaces));
        return surfaces;
    }

    /**
     * Обновляет зависимости от моделей и атласов при изменении загруженных ресурсов.
     */
    private void refreshModels() {
        var minecraft = Minecraft.getInstance();
        var manager = minecraft.getModelManager();
        BlockStateModelSet next = manager.getBlockStateModelSet();
        FluidStateModelSet nextFluid = manager.getFluidStateModelSet();
        if (next == models && nextFluid == fluidModels && blocks != null) return;
        models = next;
        fluidModels = nextFluid;
        singles.clear();
        sections.clear();
        materials.clear();
        blocks = new ModelBlockRenderer(true, true, minecraft.getBlockColors());
        fluids = new FluidRenderer(fluidModels);
    }

    /**
     * Создаёт материал, использующий ванильное освещение из цвета вершины и UV атласа.
     *
     * @param key атлас и слой геометрии.
     * @return материал для загрузки в рендерер сцены.
     */
    private Material material(SurfaceKey key) {
        var atlas = Minecraft.getInstance().getTextureManager().getTexture(key.atlas);
        var texture = atlas.getTexture();
        var handle = new SimpleTextureHandle(key.atlas.toString(), texture.getWidth(0), texture.getHeight(0), atlas);
        Material.AlphaMode alpha = switch (key.layer) {
            case SOLID -> Material.AlphaMode.OPAQUE;
            case CUTOUT -> Material.AlphaMode.CUTOUT;
            case TRANSLUCENT -> Material.AlphaMode.TRANSLUCENT;
        };
        return new Material(0xFFFFFFFF, handle, shading, alpha, 0.5f, Material.Culling.BACK);
    }

    /**
     * Подготовленный меш.
     *
     * @param mesh        геометрия в координатах узла.
     * @param material    атлас и режим прозрачности.
     * @param nativeLayer texture bindings and render state for native geometry, or null for blocks
     */
    record Surface(Mesh3D mesh, Material material, SceneNativeLayer nativeLayer) {
        /**
         * Wraps baked block geometry using a texture handle provided by the material.
         *
         * @param mesh     geometry in node-local units
         * @param material block layer material
         */
        Surface(Mesh3D mesh, Material material) {
            this(mesh, material, null);
        }
    }

    /**
     * Computes a common local AABB for an effect across all of a node's meshes and layers.
     * The component-wise union extends each minimum downward and each maximum upward.
     *
     * @param surfaces   baked model layers regardless of their visibility in a particular pass
     * @param meshBounds optional bounds of an additional custom mesh on the same node
     * @return shared bounds in node-local coordinates, or null if there is no geometry
     */
    static Aabb surfaceBounds(List<Surface> surfaces, Aabb meshBounds) {
        if (surfaces.isEmpty()) return meshBounds;
        Aabb first = meshBounds == null ? surfaces.getFirst().mesh().bounds() : meshBounds;
        float minX = first.minX(), minY = first.minY(), minZ = first.minZ();
        float maxX = first.maxX(), maxY = first.maxY(), maxZ = first.maxZ();
        for (int i = meshBounds == null ? 1 : 0; i < surfaces.size(); i++) {
            Aabb next = surfaces.get(i).mesh().bounds();
            minX = Math.min(minX, next.minX());
            minY = Math.min(minY, next.minY());
            minZ = Math.min(minZ, next.minZ());
            maxX = Math.max(maxX, next.maxX());
            maxY = Math.max(maxY, next.maxY());
            maxZ = Math.max(maxZ, next.maxZ());
        }
        if (minX == first.minX() && minY == first.minY() && minZ == first.minZ()
                && maxX == first.maxX() && maxY == first.maxY() && maxZ == first.maxZ()) return first;
        return new Aabb(minX, minY, minZ, maxX, maxY, maxZ);
    }

    /**
     * Результат запекания секции.
     *
     * @param revision версия секции.
     * @param surfaces слои готовой геометрии.
     */
    private record BakedSection(long revision, List<Surface> surfaces) {
    }

    /**
     * Ключ материала.
     *
     * @param atlas идентификатор атласа текстур Minecraft.
     * @param layer слой чанка с заданным режимом прозрачности.
     */
    private record SurfaceKey(Identifier atlas, ChunkSectionLayer layer) {
    }

    /**
     * Группа квадов для запекания.
     *
     * @param material       ключ материала группы.
     * @param translucentPos позиция блока для отдельной сортировки прозрачности или null для общей пачки.
     */
    private record MeshKey(SurfaceKey material, BlockPos translucentPos) {
    }

    /**
     * Собирает меши одного блока или одной секции.
     */
    private final class Bake {
        /**
         * Начало секции по X в координатах узла.
         */
        private final int x;
        /**
         * Начало секции по Y в координатах узла.
         */
        private final int y;
        /**
         * Начало секции по Z в координатах узла.
         */
        private final int z;
        /**
         * Выходы по атласам и слоям.
         */
        private final Map<MeshKey, MeshBuilder> builders = new LinkedHashMap<>();
        /**
         * Позиция блока для группировки прозрачных граней.
         */
        private BlockPos pos;
        /**
         * Атлас текущей жидкости.
         */
        private Identifier fluidAtlas;

        /**
         * Задаёт начало секции для перевода координат жидкости.
         *
         * @param x координата X в блоках.
         * @param y координата Y в блоках.
         * @param z координата Z в блоках.
         */
        private Bake(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        /**
         * Добавляет ванильную модель и жидкость в позиции.
         *
         * @param pos   позиция в координатах узла.
         * @param state состояние блока.
         */
        private void addBlock(BlockPos pos, BlockState state) {
            this.pos = pos;
            if (state.getRenderShape() == RenderShape.MODEL) {
                blocks.tesselateBlock(this::putQuad, pos.getX(), pos.getY(), pos.getZ(), view, pos, state,
                        models.get(state), state.getSeed(pos));
            }
            FluidState fluid = state.getFluidState();
            if (!fluid.isEmpty()) {
                fluidAtlas = fluidModels.get(fluid).stillMaterial().sprite().atlasLocation();
                fluids.tesselate(view, pos, layer -> builder(fluidAtlas, layer), state, fluid);
            }
        }

        /**
         * Сохраняет вершины квада в порядке модели с tint и AO из ванильного тесселятора.
         *
         * @param x        смещение модели по X в единицах сцены.
         * @param y        смещение модели по Y в единицах сцены.
         * @param z        смещение модели по Z в единицах сцены.
         * @param quad     исходная геометрия с UV атласа.
         * @param instance цвета и свет вершин конкретного блока.
         */
        private void putQuad(float x, float y, float z, BakedQuad quad, QuadInstance instance) {
            var info = quad.materialInfo();
            MeshBuilder out = builder(info.sprite().atlasLocation(), info.layer());
            Direction normal = quad.direction();
            for (int i = 0; i < BakedQuad.VERTEX_COUNT; i++) {
                Vector3fc vertex = quad.position(i);
                long uv = quad.packedUV(i);
                out.vertex(vertex.x() + x, vertex.y() + y, vertex.z() + z, instance.getColor(i),
                        UVPair.unpackU(uv), UVPair.unpackV(uv), normal.getStepX(), normal.getStepY(), normal.getStepZ());
            }
        }

        /**
         * Возвращает буфер слоя. Прозрачные поверхности группируются по одному блоку для сортировки.
         *
         * @param atlas атлас UV.
         * @param layer слой геометрии Minecraft.
         * @return накопитель вершин.
         */
        private MeshBuilder builder(Identifier atlas, ChunkSectionLayer layer) {
            MeshKey key = new MeshKey(new SurfaceKey(atlas, layer), layer.translucent() ? pos : null);
            return builders.computeIfAbsent(key, ignored -> new MeshBuilder(x, y, z));
        }

        /**
         * Собирает неизменяемые меши и их материалы.
         *
         * @return готовые непустые поверхности.
         */
        private List<Surface> finish() {
            List<Surface> result = new ArrayList<>(builders.size());
            builders.forEach((key, builder) -> {
                if (!builder.positions.isEmpty()) {
                    result.add(new Surface(builder.build(), materials.computeIfAbsent(key.material, SceneBlockMeshes.this::material)));
                }
            });
            return result;
        }
    }

    /**
     * Локальный мир с постоянным светом и биомом, доступный тесселяторам Minecraft.
     */
    private static final class BlockView implements BlockAndTintGetter {
        /**
         * Per-face brightness supplied to the block and fluid bakers.
         */
        private final CardinalLighting cardinal;
        /**
         * Текущая структура или null для одиночного блока.
         */
        private StructureNode structure;
        /**
         * Состояние одиночного блока в начале координат.
         */
        private BlockState single;

        /**
         * @param cardinal directional shade to use in the vertex colors
         */
        private BlockView(CardinalLighting cardinal) {
            this.cardinal = cardinal;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public BlockState getBlockState(BlockPos pos) {
            if (structure != null) return MinecraftScene.visibleBlock(structure, pos);
            return single != null && pos.equals(BlockPos.ZERO) ? single : Blocks.AIR.defaultBlockState();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public FluidState getFluidState(BlockPos pos) {
            return getBlockState(pos).getFluidState();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public BlockEntity getBlockEntity(BlockPos pos) {
            return structure != null && pos.getY() <= structure.topVisibleY()
                    ? MinecraftScene.blockEntity(structure, pos) : null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public CardinalLighting cardinalLighting() {
            return cardinal;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public LevelLightEngine getLightEngine() {
            return LevelLightEngine.EMPTY;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getBrightness(LightLayer layer, BlockPos pos) {
            return 15;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getRawBrightness(BlockPos pos, int darken) {
            return Math.max(0, 15 - darken);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getMinY() {
            return -2048;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getHeight() {
            return 4096;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getBlockTint(BlockPos pos, ColorResolver resolver) {
            return ARGB.opaque(resolver.getColor(PREVIEW_BIOME, pos.getX(), pos.getZ()));
        }
    }

    /**
     * Накопитель квадов. Жидкости Minecraft передают вершины в координатах секции.
     */
    static final class MeshBuilder implements VertexConsumer {
        /**
         * XYZ вершин в единицах узла.
         */
        private final FloatArrayList positions = new FloatArrayList();
        /**
         * XYZ нормалей по вершинам.
         */
        private final FloatArrayList normals = new FloatArrayList();
        /**
         * Нормализованные UV атласа по вершинам.
         */
        private final FloatArrayList uvs = new FloatArrayList();
        /**
         * Цвета ARGB по вершинам.
         */
        private final IntArrayList colors = new IntArrayList();
        /**
         * Начало секции по X.
         */
        private final int x;
        /**
         * Начало секции по Y.
         */
        private final int y;
        /**
         * Начало секции по Z.
         */
        private final int z;

        /**
         * Создаёт накопитель слоя.
         *
         * @param x координата начала секции по X.
         * @param y координата начала секции по Y.
         * @param z координата начала секции по Z.
         */
        MeshBuilder(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        /**
         * Добавляет одну вершину квада блока в координатах узла.
         *
         * @param x     координата X.
         * @param y     координата Y.
         * @param z     координата Z.
         * @param color цвет ARGB.
         * @param u     горизонтальная координата атласа.
         * @param v     вертикальная координата атласа.
         * @param nx    компонента нормали по X.
         * @param ny    компонента нормали по Y.
         * @param nz    компонента нормали по Z.
         */
        private void vertex(float x, float y, float z, int color, float u, float v, float nx, float ny, float nz) {
            positions.add(x);
            positions.add(y);
            positions.add(z);
            colors.add(color);
            uvs.add(u);
            uvs.add(v);
            normals.add(nx);
            normals.add(ny);
            normals.add(nz);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            vertex(x + this.x, y + this.y, z + this.z, 0xFFFFFFFF, 0, 0, 0, 1, 0);
            return this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public VertexConsumer setColor(int r, int g, int b, int a) {
            return setColor(ARGB.color(a, r, g, b));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public VertexConsumer setColor(int color) {
            colors.set(colors.size() - 1, color);
            return this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public VertexConsumer setUv(float u, float v) {
            uvs.set(uvs.size() - 2, u);
            uvs.set(uvs.size() - 1, v);
            return this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            int end = normals.size();
            normals.set(end - 3, x);
            normals.set(end - 2, y);
            normals.set(end - 1, z);
            return this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public VertexConsumer setUv1(int u, int v) {
            return this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public VertexConsumer setUv2(int u, int v) {
            return this;
        }

        /**
         * Принимает дополнительный UV-канал Minecraft 26.3.
         *
         * @param u горизонтальная координата.
         * @param v вертикальная координата.
         * @return этот накопитель.
         */
        public VertexConsumer setUv3(float u, float v) {
            return this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public VertexConsumer setLineWidth(float width) {
            return this;
        }

        /**
         * @return whether the layer emitted any vertices
         */
        boolean isEmpty() {
            return colors.isEmpty();
        }

        /**
         * Разбивает квады на два треугольника по диагонали 0–2.
         *
         * @return индексированная геометрия.
         */
        Mesh3D build() {
            int count = colors.size();
            if (count % 4 != 0) throw new IllegalStateException("Minecraft emitted an incomplete block quad");
            int[] indices = new int[count / 4 * 6];
            for (int i = 0, j = 0; i < count; i += 4) {
                indices[j++] = i;
                indices[j++] = i + 1;
                indices[j++] = i + 2;
                indices[j++] = i;
                indices[j++] = i + 2;
                indices[j++] = i + 3;
            }
            return new Mesh3D(positions.toFloatArray(), normals.toFloatArray(), uvs.toFloatArray(),
                    colors.toIntArray(), indices);
        }
    }
}
