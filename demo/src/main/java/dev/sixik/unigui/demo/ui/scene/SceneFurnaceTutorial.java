package dev.sixik.unigui.demo.ui.scene;

import dev.sixik.unigui.api.animation.AnimationEasing;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.math.MutableVec3;
import dev.sixik.unigui.api.scene.Material;
import dev.sixik.unigui.api.scene.Mesh3D;
import dev.sixik.unigui.api.scene.SceneNode;
import dev.sixik.unigui.api.scene.SceneTimeline;
import dev.sixik.unigui.api.text.RichText;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.demo.ui.scene.SceneTutorialPlayer.Checkpoint;
import dev.sixik.unigui.demo.ui.scene.SceneTutorialPlayer.Sequence;
import dev.sixik.unigui.widgets.containers.Box;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.widgets.rendering.Scene3D;
import dev.sixik.unigui.widgets.rendering.SceneAnchor3D;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Собирает пол, ставит печку с отскоком и зажигает её. Вся анимация описана ключевыми кадрами
 * {@link SceneTimeline}, поэтому её можно перематывать в обе стороны.
 */
public final class SceneFurnaceTutorial {
    /**
     * Сторона пола в блоках.
     */
    private static final int FLOOR_SIDE = 3;
    /**
     * Интервал между началом подъёма соседних плиток в секундах.
     */
    private static final float TILE_STAGGER = 0.15f;
    /**
     * Время подъёма одной плитки в секундах.
     */
    private static final float TILE_RISE = 0.58f;
    /**
     * Начало падения печки в секундах.
     */
    private static final float DROP = 2.05f;
    /**
     * Первое касание пола в секундах.
     */
    private static final float IMPACT = 2.85f;
    /**
     * Окончание отскока в секундах.
     */
    private static final float SETTLED = 3.35f;
    /**
     * Включение ванильной горящей модели в секундах.
     */
    private static final float IGNITION = 4;
    /**
     * Высота пола над началом координат в блоках.
     */
    private static final float FLOOR_TOP = 1;
    /**
     * Высота отскока над полом в блоках.
     */
    private static final float BOUNCE_HEIGHT = 0.28f;
    /**
     * Полное время показа вместе с финальным кадром в секундах.
     */
    private static final int DURATION = 6;
    /**
     * Время жизни одной искры в секундах.
     */
    private static final float SPARK_LIFE = 0.66f;
    /**
     * Меш искры с вершинами на расстоянии одной локальной единицы от центра.
     */
    private static final Mesh3D SPARK_MESH = new Mesh3D(new float[]{
            0, 1, 0, 0, -1, 0, 1, 0, 0, 0, 0, 1, -1, 0, 0, 0, 0, -1
    }, null, null, null, new int[]{0, 3, 2, 0, 4, 3, 0, 5, 4, 0, 2, 5, 1, 2, 3, 1, 3, 4, 1, 4, 5, 1, 5, 2});
    /**
     * Материал искр с тёплым цветом, независимый от освещения сцены.
     */
    private static final Material SPARK_MATERIAL = new Material(0xFFFFAB55, null, Material.Shading.UNLIT,
            Material.AlphaMode.OPAQUE, 0, Material.Culling.BACK);

    private SceneFurnaceTutorial() {
    }

    /**
     * Создаёт вьюпорт, стадии и покадровое обновление для проигрывателя.
     *
     * @param floor       создаёт отдельный узел плитки для каждого места.
     * @param furnace     узел печки с началом координат в углу блока.
     * @param setLit      меняет состояние этого узла и допускает повторные вызовы.
     * @param floorIcon   фабрика иконки этапа сборки пола.
     * @param furnaceIcon фабрика иконки этапов установки печки.
     * @param fireIcon    фабрика иконки этапа зажигания.
     * @return самостоятельный сценарий с новой сценой и собственными узлами.
     */
    public static Sequence create(Function<Boolean, ? extends SceneNode> floor, SceneNode furnace,
                                  Consumer<Boolean> setLit, Supplier<? extends Box> floorIcon,
                                  Supplier<? extends Box> furnaceIcon, Supplier<? extends Box> fireIcon) {
        Scene3D viewport = new Scene3D();
        viewport.camera().setOrthographic(6.05f, 0.1f, 32);
        viewport.orbit().setOrbit(new MutableVec3(0.5f, 2.11f, 0.5f), 8, 0.60, 0.43);
        SceneTimeline timeline = new SceneTimeline().duration(DURATION);
        addFloor(viewport, timeline, floor);
        addFurnace(viewport, timeline, furnace);
        addFallNote(viewport, timeline, furnace);
        addSparks(viewport, timeline);
        timeline.onSample(seconds -> setLit.accept(seconds >= IGNITION));
        return new Sequence("Placing a furnace", viewport, DURATION, List.of(
                new Checkpoint(0, "Floor", "Build the floor",
                        "The tiles rise one after another and form a platform.", floorIcon),
                new Checkpoint(DROP, "Drop", "The furnace drops",
                        "The furnace speeds up on its way to the platform and turns slightly.", furnaceIcon),
                new Checkpoint(IMPACT, "Bounce", "Soft landing",
                        "The furnace bounces after touching the floor, then settles straight.", furnaceIcon),
                new Checkpoint(IGNITION, "Fire", "The furnace is lit",
                        "After the landing the fire starts and sparks fly out.", fireIcon)
        ), timeline::sample, new FurnaceSky(viewport));
    }

    /**
     * Плитки поднимаются из-под пола с задержкой {@link #TILE_STAGGER} друг за другом: от {@code y = -1} до
     * {@code y = 0} за {@link #TILE_RISE} с плавностью cubic-out. До начала подъёма плитка скрыта.
     */
    private static void addFloor(Scene3D viewport, SceneTimeline timeline,
                                 Function<Boolean, ? extends SceneNode> floor) {
        for (int i = 0; i < FLOOR_SIDE * FLOOR_SIDE; i++) {
            int x = i % FLOOR_SIDE - 1;
            int z = i / FLOOR_SIDE - 1;
            SceneNode tile = floor.apply(x == 0 && z == 0);
            viewport.scene().addRoot(tile);
            float start = i * TILE_STAGGER;
            timeline.track(tile)
                    .visible(0, false).visible(start + 0.01f, true)
                    .position(start, x, -1, z)
                    .position(start + TILE_RISE, x, 0, z, AnimationEasing.CUBIC_OUT);
        }
    }

    /**
     * Печка падает с высоты 3,92 с ускорением (quad-in), подпрыгивает на {@link #BOUNCE_HEIGHT} и встаёт.
     * Точка опоры в центре основания, поэтому печка поворачивается вокруг своей оси.
     */
    private static void addFurnace(Scene3D viewport, SceneTimeline timeline, SceneNode furnace) {
        furnace.centerOrigin();
        viewport.scene().addRoot(furnace);
        float apex = (IMPACT + SETTLED) / 2;
        timeline.track(furnace)
                .visible(0, false).visible(DROP, true)
                .position(DROP, 0.5f, 3.92f, 0.5f)
                .position(IMPACT, 0.5f, FLOOR_TOP, 0.5f, AnimationEasing.QUAD_IN)
                .position(apex, 0.5f, FLOOR_TOP + BOUNCE_HEIGHT, 0.5f, AnimationEasing.QUAD_OUT)
                .position(SETTLED, 0.5f, FLOOR_TOP, 0.5f, AnimationEasing.QUAD_IN)
                .yaw(DROP, -0.20f)
                .rotation(IMPACT, 0.06f, 0, AnimationEasing.QUAD_IN)
                .rotation(apex, 0.20f, 0, AnimationEasing.QUAD_OUT)
                .rotation(SETTLED, 0, 0, AnimationEasing.QUAD_IN);
    }

    /**
     * Подпись «Falling!» следует за печкой от начала падения до конца отскока.
     */
    private static void addFallNote(Scene3D viewport, SceneTimeline timeline, SceneNode furnace) {
        Label note = new Label(RichText.of("Falling!", SceneTutorialPlayer.UI_FONT, 11,
                MutableColor.fromArgb(0xFFF1F5FB)));
        note.enabled(false);
        note.background(MutableColor.fromArgb(0xF2202F3E)).border(MutableColor.fromArgb(0xFFFFC879)).radius(5);
        note.layout(style -> style.padding(7, 4));
        SceneAnchor3D anchor = viewport.addAnchor(note, furnace, new MutableVec3(1.65f, 0.70f, 0.5f)).pivot(0, 0.5f);
        anchor.visibility(Visibility.HIDDEN);
        timeline.onSample(seconds -> anchor.visibility(seconds >= DROP && seconds < SETTLED
                ? Visibility.VISIBLE : Visibility.HIDDEN));
    }

    /**
     * Искры вылетают от фасада с шагом 0,075 с, летят вверх с замедлением, расходятся веером и гаснут за
     * {@link #SPARK_LIFE}.
     */
    private static void addSparks(Scene3D viewport, SceneTimeline timeline) {
        int count = 6;
        for (int i = 0; i < count; i++) {
            SceneNode spark = new SceneNode().setMesh(SPARK_MESH).setMaterial(SPARK_MATERIAL);
            viewport.scene().addRoot(spark);
            float start = IGNITION + i * 0.075f;
            float end = start + SPARK_LIFE;
            float side = i - (count - 1) * 0.5f;
            timeline.track(spark)
                    .visibleBetween(start, end)
                    .position(start, 0.5f + side * 0.043f, 1.4f, 1.04f)
                    .position(end, 0.5f + side * (0.043f + 0.15f * SPARK_LIFE), 1.4f + SPARK_LIFE * (1.42f - SPARK_LIFE),
                            1.04f + SPARK_LIFE * (0.30f + 0.055f * i), AnimationEasing.QUAD_OUT)
                    .scale(start, 0.046f)
                    .scale(end, 0);
        }
    }
}
