package dev.sixik.unigui.tests;

import dev.sixik.unigui.api.animation.AnimationEasing;
import dev.sixik.unigui.api.animation.Easing;
import dev.sixik.unigui.api.animation.FloatInterpolator;
import dev.sixik.unigui.api.math.MutableQuat;
import dev.sixik.unigui.api.math.MutableVec3;
import dev.sixik.unigui.api.scene.Material;
import dev.sixik.unigui.api.scene.Mesh3D;
import dev.sixik.unigui.api.scene.SceneNode;
import dev.sixik.unigui.backend.minecraft_impl.BlockNode;
import dev.sixik.unigui.tests.SceneTutorialDemo.Checkpoint;
import dev.sixik.unigui.tests.SceneTutorialDemo.Sequence;
import dev.sixik.unigui.widgets.rendering.Scene3D;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FurnaceBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/** Собирает пол, ставит печку с отскоком и зажигает её. Все позы вычисляются по времени сценария. */
final class SceneFurnaceTutorial {
    /** Сторона пола в блоках. */
    private static final int FLOOR_SIDE = 3;
    /** Интервал между началом подъёма соседних плиток в секундах. */
    private static final float TILE_STAGGER = 0.15f;
    /** Время подъёма одной плитки в секундах. */
    private static final float TILE_RISE = 0.58f;
    /** Начало падения печки в секундах. */
    private static final float DROP = 2.05f;
    /** Первое касание пола в секундах. */
    private static final float IMPACT = 2.85f;
    /** Окончание отскока в секундах. */
    private static final float SETTLED = 3.35f;
    /** Включение ванильной горящей модели в секундах. */
    private static final float IGNITION = 4;
    /** Высота пола над началом координат в блоках. */
    private static final float FLOOR_TOP = 1;
    /** Высота отскока над полом в блоках. */
    private static final float BOUNCE_HEIGHT = 0.28f;
    /** Полное время показа вместе с финальным кадром в секундах. */
    private static final int DURATION = 6;
    /** Меш искры с вершинами на расстоянии одной локальной единицы от центра. */
    private static final Mesh3D SPARK_MESH = new Mesh3D(new float[]{
            0, 1, 0, 0, -1, 0, 1, 0, 0, 0, 0, 1, -1, 0, 0, 0, 0, -1
    }, null, null, null, new int[]{0, 3, 2, 0, 4, 3, 0, 5, 4, 0, 2, 5, 1, 2, 3, 1, 3, 4, 1, 4, 5, 1, 5, 2});
    /** Материал искр с тёплым цветом, независимый от освещения сцены. */
    private static final Material SPARK_MATERIAL = new Material(0xFFFFAB55, null, Material.Shading.UNLIT,
            Material.AlphaMode.OPAQUE, 0, Material.Culling.BACK);

    /** Узлы плиток в порядке появления. */
    private final BlockNode[] tiles = new BlockNode[FLOOR_SIDE * FLOOR_SIDE];
    /** Поворот печки происходит вокруг центра её основания. */
    private final SceneNode pivot = new SceneNode();
    /** Состояние обычной печки с фасадом к начальной позиции камеры. */
    private final BlockState cold = Blocks.FURNACE.defaultBlockState().setValue(FurnaceBlock.FACING, Direction.SOUTH);
    /** Состояние горящей печки с той же ориентацией. */
    private final BlockState lit = cold.setValue(FurnaceBlock.LIT, true);
    /** Блок печки со смещением относительно центра основания. */
    private final BlockNode furnace = new BlockNode(cold);
    /** Рабочий кватернион поворота печки. */
    private final MutableQuat rotation = new MutableQuat();
    /** Искры зажигания. Каждая получает позу по времени. */
    private final SceneNode[] sparks = new SceneNode[6];

    /**
     * Расставляет узлы пола и печки.
     * @param viewport вьюпорт, граф которого принадлежит этому сценарию.
     */
    private SceneFurnaceTutorial(Scene3D viewport) {
        var scene = viewport.scene();
        for (int i = 0; i < tiles.length; i++) {
            int x = i % FLOOR_SIDE - 1;
            int z = i / FLOOR_SIDE - 1;
            var block = x == 0 && z == 0 ? Blocks.POLISHED_DEEPSLATE : Blocks.DEEPSLATE_TILES;
            var tile = new BlockNode(block.defaultBlockState());
            tile.setPosition(x, 0, z);
            tiles[i] = tile;
            scene.addRoot(tile);
        }
        furnace.setPosition(-0.5f, 0, -0.5f);
        pivot.addChild(furnace);
        scene.addRoot(pivot);
        for (int i = 0; i < sparks.length; i++) {
            sparks[i] = new SceneNode().setMesh(SPARK_MESH).setMaterial(SPARK_MATERIAL);
            scene.addRoot(sparks[i]);
        }
    }

    /**
     * Создаёт вьюпорт, стадии и покадровое обновление для проигрывателя.
     * @return самостоятельный сценарий с новой сценой и собственными узлами.
     */
    static Sequence create() {
        Scene3D viewport = new Scene3D();
        viewport.camera().setOrthographic(6.05f, 0.1f, 32);
        viewport.orbit().setOrbit(new MutableVec3(0.5f, 2.11f, 0.5f), 8, 0.60, 0.43);
        SceneFurnaceTutorial animation = new SceneFurnaceTutorial(viewport);
        return new Sequence("Установка печки", viewport, DURATION, List.of(
                new Checkpoint(0, "Пол", "Сборка пола",
                        "Плитки поднимаются одна за другой и образуют площадку.", Items.DEEPSLATE_TILES, "deepslate_tiles"),
                new Checkpoint(DROP, "Падение", "Печка падает",
                        "Печка ускоряется на пути к площадке и слегка разворачивается.", Items.FURNACE, "furnace"),
                new Checkpoint(IMPACT, "Отскок", "Мягкое приземление",
                        "Печка подпрыгивает после контакта с полом, затем выравнивается.", Items.FURNACE, "furnace"),
                new Checkpoint(IGNITION, "Огонь", "Печка зажглась",
                        "После приземления загорается топка и вылетают искры.", Items.COAL, "coal")
        ), animation::sample);
    }

    /**
     * Восстанавливает кадр, включая скрытые узлы для обратной перемотки и повтора.
     * @param seconds время в секундах от открытия сцены.
     */
    private void sample(float seconds) {
        sampleFloor(seconds);
        sampleFurnace(seconds);
        sampleSparks(seconds - IGNITION);
    }

    /**
     * Вычисляет подъём плитки как {@code y = -1 + p}, где {@code p = cubicOut((time - start) / TILE_RISE)}.
     * Начало подъёма start сдвигается на TILE_STAGGER секунд для каждого следующего блока.
     * При p равном нулю плитка скрыта, при единице её нижняя грань совпадает с полом сцены.
     * @param seconds абсолютное время в секундах.
     */
    private void sampleFloor(float seconds) {
        for (int i = 0; i < tiles.length; i++) {
            float progress = AnimationEasing.CUBIC_OUT.apply((seconds - i * TILE_STAGGER) / TILE_RISE);
            int x = i % FLOOR_SIDE - 1;
            int z = i / FLOOR_SIDE - 1;
            tiles[i].setVisible(progress > 0).setPosition(x, progress - 1, z);
        }
    }

    /**
     * Интерполирует высоту от 3,92 блока до пола с прогрессом {@code drop^2}. После касания
     * добавляет отскок {@code 4 * BOUNCE_HEIGHT * u * (1 - u)}, где u — доля времени от касания
     * до остановки. Угол в радианах возвращается к нулю и получает отклонение с тем же профилем.
     * @param seconds абсолютное время в секундах.
     */
    private void sampleFurnace(float seconds) {
        float drop = AnimationEasing.QUAD_IN.apply((seconds - DROP) / (IMPACT - DROP));
        float flight = Easing.clamp01((seconds - IMPACT) / (SETTLED - IMPACT));
        float bounce = 4 * flight * (1 - flight);
        float y = FloatInterpolator.LINEAR.interpolate(3.92f, FLOOR_TOP, drop) + BOUNCE_HEIGHT * bounce;
        float yaw = FloatInterpolator.LINEAR.interpolate(-0.20f, 0.06f, drop) * (1 - flight) + 0.17f * bounce;
        pivot.setVisible(seconds >= DROP).setPosition(0.5f, y, 0.5f).setRotation(rotation.setYawPitch(yaw, 0));
        furnace.setState(seconds >= IGNITION ? lit : cold);
    }

    /**
     * Разбрасывает искры от фасада. Для каждой искры s — время с её вылета, смещение по высоте
     * равно {@code s * (1.42 - s)} блока, размер линейно убывает до нуля за 0,66 секунды.
     * @param elapsed время в секундах от зажигания. До зажигания оно отрицательно.
     */
    private void sampleSparks(float elapsed) {
        for (int i = 0; i < sparks.length; i++) {
            float age = elapsed - i * 0.075f;
            boolean visible = age >= 0 && age < 0.66f;
            SceneNode spark = sparks[i];
            spark.setVisible(visible);
            if (visible) {
                float side = i - (sparks.length - 1) * 0.5f;
                float size = 0.046f * (1 - age / 0.66f);
                spark.setPosition(0.5f + side * (0.043f + 0.15f * age),
                        1.4f + age * (1.42f - age), 1.04f + age * (0.30f + 0.055f * i));
                spark.setScale(size, size, size);
            }
        }
    }
}
