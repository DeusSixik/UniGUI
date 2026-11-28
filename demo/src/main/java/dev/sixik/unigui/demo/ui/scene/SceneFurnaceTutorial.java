package dev.sixik.unigui.demo.ui.scene;

import dev.sixik.unigui.api.animation.AnimationEasing;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.math.MutableVec3;
import dev.sixik.unigui.api.render.TextureOptions;
import dev.sixik.unigui.api.render.shaders.ShaderHandle;
import dev.sixik.unigui.api.scene.AmbientLight;
import dev.sixik.unigui.api.scene.DirectionalLight;
import dev.sixik.unigui.api.scene.DirectionalShadows;
import dev.sixik.unigui.api.scene.FogVolume;
import dev.sixik.unigui.api.scene.LightFlicker;
import dev.sixik.unigui.api.scene.LightNode;
import dev.sixik.unigui.api.scene.Material;
import dev.sixik.unigui.api.scene.Mesh3D;
import dev.sixik.unigui.api.scene.PointLight;
import dev.sixik.unigui.api.scene.SceneBuilder;
import dev.sixik.unigui.api.scene.SceneMaterialEffect;
import dev.sixik.unigui.api.scene.SceneMaterialEffects;
import dev.sixik.unigui.api.scene.SceneNode;
import dev.sixik.unigui.api.scene.SceneTimeline;
import dev.sixik.unigui.api.scene.SceneTimeline.EffectEnd;
import dev.sixik.unigui.api.scene.SceneTimeline.MaterialEffectClip;
import dev.sixik.unigui.api.scene.SpotLight;
import dev.sixik.unigui.api.scene.SpotShadows;
import dev.sixik.unigui.api.text.RichText;
import dev.sixik.unigui.demo.ui.DemoAssets;
import dev.sixik.unigui.demo.ui.scene.SceneTutorialPlayer.Checkpoint;
import dev.sixik.unigui.demo.ui.scene.SceneTutorialPlayer.EffectOption;
import dev.sixik.unigui.demo.ui.scene.SceneTutorialPlayer.EffectPicker;
import dev.sixik.unigui.demo.ui.scene.SceneTutorialPlayer.Sequence;
import dev.sixik.unigui.widgets.containers.Box;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.widgets.rendering.Scene3D;
import dev.sixik.unigui.widgets.rendering.RenderToTexture.UpdateMode;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Builds the floor, drops and lights the furnace, then previews a material effect on the models.
 * The {@link SceneTimeline} can sample every stage in either direction.
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
     * Start of the material effect, in timeline seconds.
     */
    private static final float EFFECT_START = 6;
    /**
     * Time in seconds for a paused preview when choosing an effect outside the final stage.
     */
    private static final float EFFECT_PREVIEW = 6.85f;
    /**
     * Description shared by the default checkpoint and the dissolve option.
     */
    private static final String DISSOLVE_DETAILS = "The surface breaks apart from the top down, with blue light along the dissolving edges.";
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
    private static final int DURATION = 8;
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
    /**
     * Adds a warm flicker to the lit furnace front outside the selected material effect.
     * The two sine waves use the scene clock in seconds and the model position in block units as phase offsets.
     */
    private static final SceneMaterialEffect FURNACE_HEAT = new SceneMaterialEffect(
            ShaderHandle.fragmentSource("unigui:furnace_heat", """
                    vec4 sceneSurface(vec4 color, vec2 uv, vec3 localPosition, vec3 worldPosition,
                                      vec3 normal, vec4 parameters, float timeSeconds, out vec3 emission) {
                        float face = smoothstep(0.99, 1.0, localPosition.z);
                        float flame = smoothstep(0.10, 0.42, color.r - color.b);
                        float flicker = 0.74 + 0.14 * sin(timeSeconds * 17.0 + localPosition.x * 8.0)
                                + 0.12 * sin(timeSeconds * 29.0 - localPosition.y * 10.0);
                        emission = vec3(1.0, 0.37, 0.08) * (face * flame * flicker * parameters.x);
                        return color;
                    }
                    """), 0.42f, 0, 0, 0);

    /**
     * Prevents instantiation of the tutorial factory.
     */
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
        return create(floor, furnace, setLit, floorIcon, furnaceIcon, fireIcon, List.of());
    }

    /**
     * Creates the furnace sequence with optional native models and the material effect picker.
     *
     * @param floor       creates one tile per platform position, receiving true for the center
     * @param furnace     furnace node with local coordinates spanning one block
     * @param setLit      switches the furnace model between its cold and lit states
     * @param floorIcon   checkpoint icon supplier for the floor
     * @param furnaceIcon checkpoint icon supplier for the furnace
     * @param fireIcon    checkpoint icon supplier for ignition
     * @param models      positioned item and mob nodes to show from ignition
     * @return a sequence whose timeline can be sampled in either direction
     */
    public static Sequence create(Function<Boolean, ? extends SceneNode> floor, SceneNode furnace,
                                  Consumer<Boolean> setLit, Supplier<? extends Box> floorIcon,
                                  Supplier<? extends Box> furnaceIcon, Supplier<? extends Box> fireIcon,
                                  List<? extends SceneNode> models) {
        return create(floor, furnace, setLit, floorIcon, furnaceIcon, fireIcon, models, true);
    }

    /**
     * Creates the furnace sequence with a picker when the renderer supports material effects.
     *
     * @param floor           creates one tile per platform position, receiving true for the center
     * @param furnace         furnace node with local coordinates spanning one block
     * @param setLit          switches the furnace model between its cold and lit states
     * @param floorIcon       checkpoint icon supplier for the floor
     * @param furnaceIcon     checkpoint icon supplier for the furnace
     * @param fireIcon        checkpoint icon supplier for ignition
     * @param models          positioned item and mob nodes to show from ignition
     * @param supportsEffects whether the renderer supports the presets
     * @return a sequence whose timeline can be sampled in either direction
     */
    public static Sequence create(Function<Boolean, ? extends SceneNode> floor, SceneNode furnace,
                                  Consumer<Boolean> setLit, Supplier<? extends Box> floorIcon,
                                  Supplier<? extends Box> furnaceIcon, Supplier<? extends Box> fireIcon,
                                  List<? extends SceneNode> models, boolean supportsEffects) {
        return create(floor, furnace, setLit, floorIcon, furnaceIcon, fireIcon, models, supportsEffects, null);
    }

    /**
     * Creates the furnace sequence with a backend-specific animation at the furnace opening.
     *
     * @param floor           creates one floor tile per position
     * @param furnace         furnace node spanning one local block
     * @param setLit          switches between the cold and lit models
     * @param floorIcon       floor checkpoint icon supplier
     * @param furnaceIcon     furnace checkpoint icon supplier
     * @param fireIcon        ignition checkpoint icon supplier
     * @param models          positioned item and mob nodes
     * @param supportsEffects whether to include the surface effect picker
     * @param sampleFire      samples backend animations in seconds relative to ignition, or null for mesh sparks and haze
     * @return a new seekable sequence
     */
    public static Sequence create(Function<Boolean, ? extends SceneNode> floor, SceneNode furnace,
                                  Consumer<Boolean> setLit, Supplier<? extends Box> floorIcon,
                                  Supplier<? extends Box> furnaceIcon, Supplier<? extends Box> fireIcon,
                                  List<? extends SceneNode> models, boolean supportsEffects,
                                  SceneTimeline.Sampler sampleFire) {
        return createWithIgnition(floor, furnace, setLit, floorIcon, furnaceIcon, fireIcon, ignition -> {
            if (sampleFire != null) ignition.onSample(sampleFire);
            return models;
        }, supportsEffects, sampleFire != null);
    }

    /**
     * Creates the furnace sequence and lets the backend add models and tracks to its ignition stage.
     *
     * @param floor             creates one floor tile per position, receiving true for the center tile
     * @param furnace           furnace node spanning one local block
     * @param setLit            switches between the cold and lit models
     * @param floorIcon         floor checkpoint icon supplier
     * @param furnaceIcon       furnace checkpoint icon supplier
     * @param fireIcon          ignition checkpoint icon supplier
     * @param configureIgnition adds tracks in seconds relative to ignition and returns positioned model nodes;
     *                          called after the floor is built
     * @param supportsEffects   whether to include the surface effect picker
     * @param nativeParticles   whether the backend supplies particles in place of the demo sparks and haze
     * @return a new seekable sequence
     */
    public static Sequence createWithIgnition(Function<Boolean, ? extends SceneNode> floor, SceneNode furnace,
                                              Consumer<Boolean> setLit, Supplier<? extends Box> floorIcon,
                                              Supplier<? extends Box> furnaceIcon, Supplier<? extends Box> fireIcon,
                                              Function<SceneTimeline, List<? extends SceneNode>> configureIgnition,
                                              boolean supportsEffects, boolean nativeParticles) {
        var builder = SceneBuilder.create().duration(DURATION);
        addFloor(builder, floor);
        addFurnace(builder, furnace);
        var ignition = builder.stage(IGNITION);
        if (!nativeParticles) addSparks(builder, ignition);
        var models = configureIgnition.apply(ignition);
        var modelNodes = models.toArray(SceneNode[]::new);
        builder.addElements(modelNodes)
                .when(0, step -> step.visible(false, modelNodes))
                .when(IGNITION, step -> step.visible(true, modelNodes));
        if (supportsEffects) {
            addLights(builder, ignition, furnace);
            if (!nativeParticles) addHaze(builder, ignition);
        }

        var sequence = builder.build();
        var timeline = sequence.timeline();
        Scene3D viewport = new Scene3D(sequence.scene());
        viewport.camera().setOrthographic(6.05f, 0.1f, 32);
        viewport.orbit().setOrbit(new MutableVec3(0.5f, 2.11f, 0.5f), 8, 0.60, 0.43);
        viewport.scene().setAmbientLight(new AmbientLight(0xD7E7FF, supportsEffects ? 0.52f : 0.67f))
                .setDirectionalLight(new DirectionalLight(-0.55f, -1, 0.45f, 0xFFF1DC, supportsEffects ? 0.48f : 0.64f))
                .setDirectionalShadows(new DirectionalShadows(0.5f, 2, 0.5f, 8))
                .setMinecraftBlockLighting(true);
        addFallNote(viewport, timeline, furnace);
        timeline.track(furnace).materialEffect(0, null).materialEffect(IGNITION, FURNACE_HEAT);
        SceneNode[] effectTargets = new SceneNode[modelNodes.length + 1];
        effectTargets[0] = furnace;
        System.arraycopy(modelNodes, 0, effectTargets, 1, modelNodes.length);
        var effect = timeline.materialEffect(EFFECT_START, DURATION, SceneMaterialEffects.dissolve(),
                EffectEnd.HIDE, effectTargets);
        ignition.onSample(seconds -> setLit.accept(seconds >= 0));
        timeline.onSample(seconds -> {
            viewport.scene().setMaterialTime(seconds >= IGNITION ? seconds : 0);
            // Minecraft's glint keeps moving while playback is paused on a visible item.
            if (!models.isEmpty()) {
                viewport.updateMode(models.get(0).visible() ? UpdateMode.EVERY_FRAME : UpdateMode.ON_CHANGE);
            }
        });
        String dissolveTitle = models.isEmpty() ? "The furnace dissolves" : "The models dissolve";
        String ignitionDetails = nativeParticles ? "Flames flicker and smoke drifts across the floor."
                : supportsEffects ? "The fire flickers and the spotlight cuts through drifting haze."
                : "After the landing the fire starts and sparks fly out.";
        return new Sequence("Placing a furnace", viewport, DURATION, List.of(
                new Checkpoint(0, "Floor", "Build the floor",
                        "The tiles rise one after another and form a platform.", floorIcon),
                new Checkpoint(DROP, "Drop", "The furnace drops",
                        "The furnace speeds up on its way to the platform and turns slightly.", furnaceIcon),
                new Checkpoint(IMPACT, "Bounce", "Soft landing",
                        "The furnace bounces after touching the floor, then settles straight.", furnaceIcon),
                new Checkpoint(IGNITION, "Fire", "The furnace is lit",
                        ignitionDetails, fireIcon),
                new Checkpoint(EFFECT_START, supportsEffects ? "Effect" : "Dissolve", dissolveTitle,
                        DISSOLVE_DETAILS, furnaceIcon)
        ), timeline::sample, new FurnaceSky(viewport), supportsEffects ? effectPicker(effect, dissolveTitle) : null);
    }

    /**
     * Brings a shallow fog layer to full density over 0.9 seconds after ignition.
     *
     * @param builder  builder receiving the fog node
     * @param ignition stage whose time zero is ignition
     */
    private static void addHaze(SceneBuilder builder, SceneTimeline ignition) {
        var haze = new FogVolume(FogVolume.Shape.BOX, 0x435470, 0)
                .setEdgeSoftness(0.72f)
                .setNoise(new FogVolume.Noise(0.74f, 2.35f, 0.19f, 0.02f, 0.08f))
                .setLightScattering(1.6f);
        haze.setPosition(0.5f, 1.60f, 0.5f).setScale(2.10f, 1.18f, 2.10f);
        builder.addElements(haze);
        ignition.during(0, 0.9f, AnimationEasing.CUBIC_OUT, progress -> haze.setDensity(0.35f * progress));
    }

    /**
     * Flickers a warm point source at the furnace opening and sweeps a cool window projection overhead.
     *
     * @param builder  builder containing the receiver geometry
     * @param ignition stage whose time zero is ignition
     * @param furnace  parent of the point light at the furnace opening
     */
    private static void addLights(SceneBuilder builder, SceneTimeline ignition, SceneNode furnace) {
        var baseFire = new PointLight(0xFF9E55, 1.65f, 3.0f);
        var fire = new LightNode(baseFire);
        var flicker = LightFlicker.fire(42);
        fire.setPosition(0.5f, 0.55f, 1.18f);
        furnace.addChild(fire);
        var mask = DemoAssets.texture("unigui:demo/furnace_window", "assets/unigui/textures/scene/furnace_window.png",
                TextureOptions.linear());
        var window = new LightNode(new SpotLight(0xA8D8FF, 3.1f, 9f, 0.36f, 0.48f)
                .withMask(mask).withShadows(new SpotShadows(1024, 0.0012f, 0.95f)));
        window.setPosition(1.65f, 4.65f, 0.95f);
        builder.addElements(window);
        ignition.track(fire).visible(0, false).visible(0, true);
        ignition.onSample(seconds -> fire.setLight(seconds >= 0 ? flicker.sample(baseFire, seconds) : baseFire));
        ignition.track(window).visible(0, false).visible(0, true)
                .rotation(0, 0.82f, -1.23f, AnimationEasing.LINEAR)
                .rotation(4, 1.28f, -1.25f, AnimationEasing.LINEAR);
    }

    /**
     * Builds the dropdown that changes this scene's final material clip.
     *
     * @param clip          shared effect interval for the furnace and its surrounding models
     * @param dissolveTitle name of the default stage, reflecting the models in this scene
     * @return the final-stage preview and its available effects
     */
    private static EffectPicker effectPicker(MaterialEffectClip clip, String dissolveTitle) {
        return new EffectPicker(EFFECT_PREVIEW, List.of(
                new EffectOption("Dissolve", dissolveTitle, DISSOLVE_DETAILS,
                        () -> clip.setEffect(SceneMaterialEffects.dissolve(), EffectEnd.HIDE)),
                new EffectOption("Scanline", "A scanline sweeps upward", "A green band travels from the base of each model to the top.",
                        () -> clip.setEffect(SceneMaterialEffects.scanline(), EffectEnd.RESTORE)),
                new EffectOption("Hologram", "The surface becomes a hologram", "Violet stripes and gaps travel across the models and their shadows.",
                        () -> clip.setEffect(SceneMaterialEffects.hologram(), EffectEnd.RESTORE)),
                new EffectOption("Pulse", "The surface pulses", "An amber glow builds and fades across each model.",
                        () -> clip.setEffect(SceneMaterialEffects.pulse(), EffectEnd.RESTORE)),
                new EffectOption("None", "The models hold their pose", "The scene uses its normal materials for the final stage.",
                        () -> clip.setEffect(null, EffectEnd.RESTORE))));
    }

    /**
     * Плитки поднимаются из-под пола с задержкой {@link #TILE_STAGGER} друг за другом: от {@code y = -1} до
     * {@code y = 0} за {@link #TILE_RISE} с плавностью cubic-out. До начала подъёма плитка скрыта.
     *
     * @param builder builder receiving the tiles and their animation keys
     * @param floor   creates a tile, receiving true for the center tile under the furnace
     */
    private static void addFloor(SceneBuilder builder, Function<Boolean, ? extends SceneNode> floor) {
        for (int i = 0; i < FLOOR_SIDE * FLOOR_SIDE; i++) {
            int x = i % FLOOR_SIDE - 1;
            int z = i / FLOOR_SIDE - 1;
            SceneNode tile = floor.apply(x == 0 && z == 0).setPosition(x, -1, z);
            float start = i * TILE_STAGGER;
            builder.addElements(tile)
                    .when(0, step -> step.visible(false, tile))
                    .when(start, step -> step.moveTo(tile, x, 0, z, TILE_RISE, AnimationEasing.CUBIC_OUT))
                    .when(start + 0.01f, step -> step.visible(true, tile));
        }
    }

    /**
     * Печка падает с высоты 3,92 с ускорением (quad-in), подпрыгивает на {@link #BOUNCE_HEIGHT} и встаёт.
     * Точка опоры в центре основания, поэтому печка поворачивается вокруг своей оси.
     *
     * @param builder builder receiving the furnace and its animation keys
     * @param furnace block node with local coordinates spanning one block
     */
    private static void addFurnace(SceneBuilder builder, SceneNode furnace) {
        furnace.centerOrigin();
        float apex = (IMPACT + SETTLED) / 2;
        builder.addElements(furnace)
                .when(0, step -> step.visible(false, furnace))
                .when(DROP, step -> step.visible(true, furnace)
                        .position(furnace, 0.5f, 3.92f, 0.5f, null)
                        .rotation(furnace, -0.20f, 0, null))
                .when(IMPACT, step -> step.position(furnace, 0.5f, FLOOR_TOP, 0.5f, AnimationEasing.QUAD_IN)
                        .rotation(furnace, 0.06f, 0, AnimationEasing.QUAD_IN))
                .when(apex, step -> step.position(furnace, 0.5f, FLOOR_TOP + BOUNCE_HEIGHT, 0.5f, AnimationEasing.QUAD_OUT)
                        .rotation(furnace, 0.20f, 0, AnimationEasing.QUAD_OUT))
                .when(SETTLED, step -> step.position(furnace, 0.5f, FLOOR_TOP, 0.5f, AnimationEasing.QUAD_IN)
                        .rotation(furnace, 0, 0, AnimationEasing.QUAD_IN));
    }

    /**
     * Подпись «Falling!» следует за печкой от начала падения до конца отскока.
     *
     * @param viewport widget hosting the note
     * @param timeline timeline driving the note's visibility
     * @param furnace  node the note follows
     */
    private static void addFallNote(Scene3D viewport, SceneTimeline timeline, SceneNode furnace) {
        Label note = new Label(RichText.of("Falling!", SceneTutorialPlayer.UI_FONT, 11,
                MutableColor.fromArgb(0xFFF1F5FB)));
        note.enabled(false);
        note.background(MutableColor.fromArgb(0xF2202F3E)).border(MutableColor.fromArgb(0xFFFFC879)).radius(5);
        note.layout(style -> style.padding(7, 4));
        var anchor = viewport.addAnchor(note, furnace, 1.65f, 0.70f, 0.5f).pivot(0, 0.5f);
        anchor.visible(false);
        timeline.trackAnchor(anchor).visibleBetween(DROP, SETTLED);
    }

    /**
     * Искры вылетают от фасада с шагом 0,075 с, летят вверх с замедлением, расходятся веером и гаснут за
     * {@link #SPARK_LIFE}.
     *
     * @param builder  builder receiving the spark nodes
     * @param ignition stage whose time zero is ignition
     */
    private static void addSparks(SceneBuilder builder, SceneTimeline ignition) {
        int count = 6;
        for (int i = 0; i < count; i++) {
            SceneNode spark = new SceneNode().setMesh(SPARK_MESH).setMaterial(SPARK_MATERIAL)
                    .setCastShadow(false).setReceiveShadow(false);
            builder.addElements(spark);
            float start = i * 0.075f;
            float end = start + SPARK_LIFE;
            float side = i - (count - 1) * 0.5f;
            ignition.track(spark)
                    .visibleBetween(start, end)
                    .position(start, 0.5f + side * 0.043f, 1.4f, 1.04f)
                    .position(end, 0.5f + side * (0.043f + 0.15f * SPARK_LIFE), 1.4f + SPARK_LIFE * (1.42f - SPARK_LIFE),
                            1.04f + SPARK_LIFE * (0.30f + 0.055f * i), AnimationEasing.QUAD_OUT)
                    .scale(start, 0.046f)
                    .scale(end, 0);
        }
    }
}
