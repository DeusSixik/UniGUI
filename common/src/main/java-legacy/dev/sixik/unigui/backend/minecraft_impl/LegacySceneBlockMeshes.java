package dev.sixik.unigui.backend.minecraft_impl;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.sixik.unigui.api.render.SimpleTextureHandle;
import dev.sixik.unigui.api.render.TextureOptions;
import dev.sixik.unigui.api.scene.Material;
import dev.sixik.unigui.api.scene.Mesh3D;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.BlockGetter;
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
import net.minecraft.world.level.chunk.LightChunk;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Запекает блоки и жидкости ванильным тесселятором с соседями из структуры.
 */
final class LegacySceneBlockMeshes {
    /**
     * Нижняя координата мира, представимая в BlockPos.
     */
    private static final int MIN_Y = -2048;
    /**
     * Высота диапазона BlockPos.
     */
    private static final int HEIGHT = 4096;
    /**
     * Источник среды для цветов травы, листвы и воды.
     */
    private static final Biome BIOME = new Biome.BiomeBuilder()
            .hasPrecipitation(true).temperature(0.8f).downfall(0.4f)
            .specialEffects(new BiomeSpecialEffects.Builder().waterColor(0x3F76E4).waterFogColor(0x050533)
                    .fogColor(0xC0D8FF).skyColor(0x78A7FF).build())
            .mobSpawnSettings(MobSpawnSettings.EMPTY).generationSettings(BiomeGenerationSettings.EMPTY).build();
    /**
     * Кэш одиночных моделей Minecraft.
     */
    private final LinkedHashMap<BlockState, List<Surface>> singles = new LinkedHashMap<>(32, 0.75f, true);
    /**
     * Кэш секций. После удаления секции её геометрия доступна сборщику мусора.
     */
    private final Map<StructureNode.Section, BakedSection> sections = new WeakHashMap<>();
    /**
     * Общий источник блоков для тесселятора.
     */
    private final BlockView view = new BlockView();
    /**
     * Временная поза блока при запекании.
     */
    private final PoseStack pose = new PoseStack();
    /**
     * Источник вариаций модели. Ваниль выставляет seed по позиции блока.
     */
    private final RandomSource random = RandomSource.create();
    /**
     * Маркер последней загрузки моделей.
     */
    private BakedModel missingModel;
    /**
     * Атласные материалы по режиму альфы.
     */
    private final Material[] materials = new Material[Material.AlphaMode.values().length];

    /**
     * Получает слои одной модели в начале координат.
     *
     * @param state ванильное состояние блока.
     * @return слои с готовыми мешами.
     */
    List<Surface> single(BlockState state) {
        var client = Minecraft.getInstance();
        refresh(client);
        List<Surface> cached = singles.get(state);
        if (cached != null) return cached;
        view.single = state;
        try {
            Bake bake = new Bake(client.getBlockRenderer());
            bake.add(BlockPos.ZERO, state);
            cached = bake.finish();
            singles.put(state, cached);
            if (singles.size() > 256) singles.remove(singles.keySet().iterator().next());
            return cached;
        } finally {
            view.single = null;
        }
    }

    /**
     * Получает слои секции, обновляя их при изменении блоков или ресурсов.
     *
     * @param structure локальный мир с соседними блоками.
     * @param section   запекаемая секция.
     * @return слои с готовыми мешами.
     */
    List<Surface> section(StructureNode structure, StructureNode.Section section) {
        var client = Minecraft.getInstance();
        refresh(client);
        BakedSection cached = sections.get(section);
        if (cached != null && cached.revision == section.revision) return cached.surfaces;
        view.structure = structure;
        try {
            Bake bake = new Bake(client.getBlockRenderer());
            int top = structure.topVisibleY();
            for (var entry : section.blocks.entrySet()) {
                if (entry.getKey().getY() <= top) bake.add(entry.getKey(), entry.getValue());
            }
            var surfaces = bake.finish();
            sections.put(section, new BakedSection(section.revision, surfaces));
            return surfaces;
        } finally {
            view.structure = null;
        }
    }

    /**
     * Обновляет атласные материалы после перезагрузки ресурсов.
     *
     * @param client клиент с активным набором моделей.
     */
    private void refresh(Minecraft client) {
        BakedModel marker = client.getModelManager().getMissingModel();
        if (marker == missingModel) return;
        missingModel = marker;
        singles.clear();
        sections.clear();
        var atlas = TextureAtlas.LOCATION_BLOCKS;
        var texture = new SimpleTextureHandle(atlas.toString(), 1, 1, atlas)
                .withOptions(TextureOptions.nearest());
        for (Material.AlphaMode alpha : Material.AlphaMode.values()) {
            materials[alpha.ordinal()] = new Material(0xFFFFFFFF, texture,
                    Material.Shading.UNLIT, alpha, 0.1f, Material.Culling.BACK);
        }
    }

    /**
     * Готовая геометрия слоя.
     *
     * @param mesh     индексированный меш.
     * @param material ванильная текстура и настройки прозрачности.
     */
    record Surface(Mesh3D mesh, Material material) {
    }

    /**
     * Слои, действительные для одной версии секции.
     *
     * @param revision версия структуры.
     * @param surfaces слои секции.
     */
    private record BakedSection(long revision, List<Surface> surfaces) {
    }

    /**
     * Накопитель одной порции геометрии.
     */
    private final class Bake {
        /**
         * Ванильный тесселятор блоков и жидкостей.
         */
        private final BlockRenderDispatcher dispatcher;
        /**
         * Накопители непрозрачных и вырезаемых квадов.
         */
        private final SceneBlockVertexConsumer solid = new SceneBlockVertexConsumer();
        /**
         * Накопитель квадов с alpha cutoff.
         */
        private final SceneBlockVertexConsumer cutout = new SceneBlockVertexConsumer();
        /**
         * Полупрозрачные меши сгруппированы по блоку для сортировки.
         */
        private final List<Surface> surfaces = new ArrayList<>();
        /**
         * Накопитель прозрачных квадов текущего блока.
         */
        private SceneBlockVertexConsumer translucent;

        /**
         * Создаёт сборщик слоёв.
         *
         * @param dispatcher клиентский тесселятор.
         */
        Bake(BlockRenderDispatcher dispatcher) {
            this.dispatcher = dispatcher;
        }

        /**
         * Передаёт ванильному тесселятору блок и его жидкость.
         *
         * @param pos   позиция блока в структуре.
         * @param state состояние блока.
         */
        void add(BlockPos pos, BlockState state) {
            translucent = null;
            if (state.getRenderShape() == RenderShape.MODEL) {
                var out = layer(ItemBlockRenderTypes.getChunkRenderType(state));
                out.offset(0, 0, 0);
                pose.setIdentity();
                pose.translate(pos.getX(), pos.getY(), pos.getZ());
                dispatcher.renderBatched(state, pos, view, pose, out, true, random);
            }
            var fluid = state.getFluidState();
            if (!fluid.isEmpty()) {
                var out = layer(ItemBlockRenderTypes.getRenderLayer(fluid));
                out.offset(pos.getX() & ~15, pos.getY() & ~15, pos.getZ() & ~15);
                dispatcher.renderLiquid(pos, view, out, state, fluid);
            }
            if (translucent != null) finishLayer(translucent, Material.AlphaMode.TRANSLUCENT);
        }

        /**
         * Выбирает накопитель по ванильному слою чанка.
         *
         * @param layer слой блочной геометрии.
         * @return накопитель квадов.
         */
        private SceneBlockVertexConsumer layer(RenderType layer) {
            if (layer == RenderType.solid()) return solid;
            if (layer != RenderType.translucent() && layer != RenderType.tripwire()) return cutout;
            if (translucent == null) translucent = new SceneBlockVertexConsumer();
            return translucent;
        }

        /**
         * Запекает непрозрачные слои и добавляет их к прозрачным.
         *
         * @return индексированные слои блока или секции.
         */
        List<Surface> finish() {
            finishLayer(solid, Material.AlphaMode.OPAQUE);
            finishLayer(cutout, Material.AlphaMode.CUTOUT);
            return surfaces;
        }

        /**
         * Упаковывает слой при наличии вершин.
         *
         * @param vertices вершины квадов.
         * @param alpha    способ обработки альфа-канала.
         */
        private void finishLayer(SceneBlockVertexConsumer vertices, Material.AlphaMode alpha) {
            Mesh3D mesh = vertices.build();
            if (mesh != null) surfaces.add(new Surface(mesh, materials[alpha.ordinal()]));
        }
    }

    /**
     * Ванильный интерфейс локального мира для моделей сцены.
     */
    private static final class BlockView implements BlockAndTintGetter, LightChunkGetter {
        /**
         * Движок без освещаемых чанков. Фиксированный свет задаёт этот BlockView.
         */
        private final LevelLightEngine lightEngine = new LevelLightEngine(this, false, false);
        /**
         * Текущая структура или null при запекании одиночного блока.
         */
        StructureNode structure;
        /**
         * Состояние блока в начале координат для одиночной модели.
         */
        BlockState single;

        /**
         * {@inheritDoc}
         */
        @Override
        public BlockState getBlockState(BlockPos pos) {
            if (structure != null) return structure.visibleBlock(pos);
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
            return structure != null && pos.getY() <= structure.topVisibleY() ? structure.blockEntity(pos) : null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public LevelLightEngine getLightEngine() {
            return lightEngine;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public LightChunk getChunkForLighting(int x, int z) {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public BlockGetter getLevel() {
            return this;
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
        public int getMinBuildHeight() {
            return MIN_Y;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getHeight() {
            return HEIGHT;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getBlockTint(BlockPos pos, ColorResolver resolver) {
            return resolver.getColor(BIOME, pos.getX(), pos.getZ());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public float getShade(Direction direction, boolean shade) {
            if (!shade) return 1.0f;
            return switch (direction) {
                case DOWN -> 0.5f;
                case UP -> 1.0f;
                case NORTH, SOUTH -> 0.8f;
                case EAST, WEST -> 0.6f;
            };
        }
    }
}
