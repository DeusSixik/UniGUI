package dev.sixik.unigui.tests;

import dev.sixik.unigui.api.animation.NamedWidgetRegistry;
import dev.sixik.unigui.api.core.FrameContext;
import dev.sixik.unigui.api.core.FramePhase;
import dev.sixik.unigui.api.input.InputHandle;
import dev.sixik.unigui.api.input.PointerButton;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.math.MutableQuat;
import dev.sixik.unigui.api.math.MutableVec3;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.api.scene.BlockNode;
import dev.sixik.unigui.api.scene.EntityNode;
import dev.sixik.unigui.api.scene.ItemNode;
import dev.sixik.unigui.api.scene.SceneNode;
import dev.sixik.unigui.backend.minecraft_impl.MinecraftClientCompat;
import dev.sixik.unigui.backend.minecraft_impl.MinecraftParticleEmitter;
import dev.sixik.unigui.backend.minecraft_impl.MinecraftScene;
import dev.sixik.unigui.demo.ui.scene.SceneTutorialPlayer;
import dev.sixik.unigui.impl.core.DefaultUIContext;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.widgets.interaction.Button;
import dev.sixik.unigui.widgets.interaction.Slider;
import dev.sixik.unigui.widgets.rendering.RenderToTexture.UpdateMode;
import dev.sixik.unigui.widgets.rendering.Scene3D;
import dev.sixik.unigui.widgets.rendering.SceneAnchor3D;
import net.minecraft.SharedConstants;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FurnaceBlock;
import dev.sixik.unigui.api.scene.ScenePlatform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Проверяет ввод и восстановление состояния Java-туториала. */
final class SceneTutorialDemoTest {
    /** Допуск учитывает округление float при интерполяции и переводе позиции курсора во время. */
    private static final float EPS = 1e-5f;

    /** Загружает ванильные реестры блоков и предметов без запуска окна Minecraft. */
    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        ScenePlatform.install(MinecraftScene.PLATFORM);
    }

    /** Проверяет одинаковый промежуточный кадр при прямой и обратной перемотке. */
    @Test
    void scrub_backwards_restoresVisibilityPoseAndCaption() {
        Harness h = new Harness();
        assertFalse(h.hopper.visible());
        assertFalse(h.item.visible());
        assertEquals("Place a chest", h.caption.text());

        h.slider.value(2.125f);
        assertTrue(h.hopper.visible());
        assertEquals(2.13125f, h.hopper.position(new MutableVec3()).y(), EPS);
        assertFalse(h.item.visible());
        assertEquals("Aim the hopper", h.caption.text());

        h.slider.value(5.375f);
        assertTrue(h.item.visible());
        assertEquals(2, h.hopper.position(new MutableVec3()).y(), EPS);
        assertEquals(3.5125f, h.item.position(new MutableVec3()).y(), EPS);
        assertEquals("Load the hopper", h.caption.text());

        h.slider.value(6.25f);
        assertTrue(h.hopper.visible());
        assertFalse(h.item.visible());
        assertEquals("The ingot is in the chest", h.caption.text());

        h.slider.value(5.375f);
        assertTrue(h.item.visible());
        assertEquals(3.5125f, h.item.position(new MutableVec3()).y(), EPS);
        assertEquals("Load the hopper", h.caption.text());

        h.slider.value(0);
        assertFalse(h.hopper.visible());
        assertFalse(h.item.visible());
        assertEquals("Place a chest", h.caption.text());
    }

    /** Проверяет реальный ввод в ползунок и кнопки повтора и паузы. */
    @Test
    void controls_dragAndReplay_keepClockAndSceneInSync() {
        Harness h = new Harness();
        h.tick();
        assertEquals(0.25f, h.slider.value(), EPS);

        var bounds = h.slider.layoutBounds();
        assertTrue(bounds.width() > 0);
        float y = bounds.y() + bounds.height() / 2;
        h.input.pointerPressed(bounds.x() + bounds.width() * (0.25f / 9), y, 0, PointerButton.PRIMARY);
        assertEquals("Play", h.play.text());
        h.input.pointerMoved(bounds.x() + bounds.width() * (5f / 9), y, 0);
        h.input.pointerReleased(bounds.x() + bounds.width() * (5f / 9), y, 0, PointerButton.PRIMARY);
        h.tick();
        assertEquals(5, h.slider.value(), EPS);
        assertTrue(h.item.visible());

        h.click(h.replay);
        assertEquals(0, h.slider.value(), EPS);
        assertFalse(h.hopper.visible());
        assertEquals("Pause", h.play.text());
        h.tick();
        assertEquals(0.25f, h.slider.value(), EPS);

        h.click(h.play);
        h.tick();
        assertEquals(0.25f, h.slider.value(), EPS);
        h.click(h.play);
        h.tick();
        assertEquals(0.5f, h.slider.value(), EPS);
    }

    /** Проверяет остановку на конечном кадре и повтор с начала после окончания. */
    @Test
    void playback_atEnd_stopsAndCanStartAgain() {
        Harness h = new Harness();
        for (int i = 0; i < 37; i++) h.tick();
        assertEquals(9, h.slider.value(), EPS);
        assertEquals("The ingot is in the chest", h.caption.text());
        assertEquals("Play", h.play.text());

        h.click(h.play);
        assertEquals(0, h.slider.value(), EPS);
        assertEquals("Pause", h.play.text());
        assertFalse(h.hopper.visible());
    }

    /** Checks floor construction, bounce, ignition, dissolve and replay through the tutorial controls. */
    @Test
    void furnace_seekAndReplay_restorePositionsFireAndDissolve() {
        var sequence = SceneFurnaceTutorial.create();
        var demo = new SceneTutorialPlayer(sequence);
        DefaultUIContext ui = new DefaultUIContext();
        demo.setUiContextInternal(ui);
        demo.measure(new LayoutContext(480, 270));
        demo.arrange(new MutableRect(0, 0, 480, 270));
        assertEquals(1, sequence.viewport().children().stream().filter(SceneAnchor3D.class::isInstance).count());
        var widgets = NamedWidgetRegistry.from(demo);
        var slider = (Slider) widgets.require("tutorial-time");
        var nodes = sequence.viewport().scene().roots();
        var firstTile = nodes.get(0);
        var lastTile = nodes.get(8);
        var furnace = nodes.stream().filter(BlockNode.class::isInstance).map(BlockNode.class::cast)
                .filter(node -> MinecraftScene.state(node).getBlock() instanceof FurnaceBlock).findFirst().orElseThrow();
        // The furnace turns around its own bottom center, so its position is the pivot.
        var pivot = furnace;
        var sparks = nodes.stream().filter(node -> node.mesh() != null).toList();
        var emitters = furnace.children().stream().filter(MinecraftParticleEmitter.class::isInstance)
                .map(MinecraftParticleEmitter.class::cast).toList();
        var floorSmoke = nodes.subList(0, 9).stream().flatMap(tile -> tile.children().stream())
                .filter(MinecraftParticleEmitter.class::isInstance).map(MinecraftParticleEmitter.class::cast).toList();
        var models = nodes.stream().filter(node -> node instanceof ItemNode || node instanceof EntityNode).toList();
        assertEquals(MinecraftClientCompat.NATIVE_SCENE_MATERIALS ? 4 : 0, models.size());
        if (MinecraftClientCompat.NATIVE_SCENE_MATERIALS) {
            assertEquals(2, models.stream().filter(ItemNode.class::isInstance).count());
            assertEquals(2, models.stream().filter(EntityNode.class::isInstance).count());
        }
        var position = new MutableVec3();
        var rotation = new MutableQuat();
        assertNotNull(sequence.viewport().scene().directionalShadows());
        assertTrue(sequence.viewport().scene().minecraftBlockLighting());

        assertFalse(pivot.visible());
        assertFalse(firstTile.visible());
        assertTrue(models.stream().noneMatch(SceneNode::visible));
        slider.value(0.29f);
        assertTrue(firstTile.visible());
        assertEquals(-0.125f, firstTile.position(position).y(), EPS);
        assertFalse(lastTile.visible());
        assertFalse(pivot.visible());

        var fall = sequence.checkpoints().get(1);
        var impact = sequence.checkpoints().get(2);
        var ignition = sequence.checkpoints().get(3);
        slider.value(fall.seconds());
        assertTrue(lastTile.visible());
        assertEquals(0, lastTile.position(position).y(), EPS);
        assertTrue(pivot.visible());
        assertEquals(3.92f, pivot.position(position).y(), EPS);
        slider.value((fall.seconds() + impact.seconds()) / 2);
        assertEquals(3.19f, pivot.position(position).y(), EPS);
        slider.value(impact.seconds());
        assertEquals(1, pivot.position(position).y(), EPS);
        assertFalse(MinecraftScene.state(furnace).getValue(FurnaceBlock.LIT));
        assertNull(furnace.materialEffect());

        slider.value(impact.seconds() + 0.25f);
        assertEquals(1.28f, pivot.position(position).y(), EPS);
        assertTrue(pivot.rotation(rotation).y() > 0.08f);
        slider.value(3.5f);
        assertEquals(1, pivot.position(position).y(), EPS);
        assertEquals(0, pivot.rotation(rotation).y(), EPS);
        assertFalse(MinecraftScene.state(furnace).getValue(FurnaceBlock.LIT));

        slider.value(ignition.seconds() + 0.26f);
        assertTrue(MinecraftScene.state(furnace).getValue(FurnaceBlock.LIT));
        assertNotNull(furnace.materialEffect());
        assertEquals(ignition.seconds() + 0.26f, sequence.viewport().scene().materialTime(), EPS);
        if (MinecraftClientCompat.NATIVE_SCENE_PARTICLES) {
            assertTrue(sparks.isEmpty());
            assertEquals(1, emitters.size());
            assertSame(ParticleTypes.FLAME, emitters.get(0).particle());
            assertNull(emitters.get(0).softParticles());
            assertEquals(0.26, emitters.get(0).time(), EPS);
            assertEquals(8, floorSmoke.size());
            for (int i = 0; i < floorSmoke.size(); i++) {
                var smoke = floorSmoke.get(i);
                assertSame(nodes.get(i < 4 ? i : i + 1), smoke.parent());
                assertSame(ParticleTypes.CAMPFIRE_COSY_SMOKE, smoke.particle());
                assertNotNull(smoke.softParticles());
                assertTrue(smoke.softParticles().widthScale() > smoke.softParticles().heightScale());
                smoke.position(position);
                assertEquals(0.5f, position.x(), EPS);
                assertEquals(1.16f, position.y(), EPS);
                assertEquals(0.5f, position.z(), EPS);
                assertTrue(Math.abs(smoke.motion().y()) < Math.abs(smoke.motion().z()));
                assertEquals(0.26, smoke.time(), EPS);
            }
        } else {
            assertTrue(floorSmoke.isEmpty());
            assertEquals(4, sparks.stream().filter(node -> node.visible()).count());
        }
        slider.value(5.25f);
        assertTrue(MinecraftScene.state(furnace).getValue(FurnaceBlock.LIT));
        var zombie = models.stream().filter(EntityNode.class::isInstance).map(EntityNode.class::cast)
                .filter(mob -> "minecraft:zombie".equals(mob.typeId())).findFirst().orElse(null);
        float stride = 0;
        float forward = 0;
        if (zombie != null) {
            assertTrue(zombie.hasAnimation());
            assertEquals(1.25f, zombie.animationSeconds(), EPS);
            assertTrue(zombie.walkStrength() > 0);
            stride = zombie.walkPhase();
            forward = zombie.position(position).z();
            assertTrue(forward > 0.2f);
            assertEquals(15 * Math.hypot(position.x() + 0.55f, forward - 0.2f), stride, EPS);
            assertFalse(zombie.hasAction());
            slider.value(ignition.seconds() + 1.75f);
            assertTrue(zombie.hasAction());
            assertEquals(0, zombie.walkStrength(), EPS);
            slider.value(5.25f);
            assertFalse(zombie.hasAction());
        }
        assertTrue(sparks.stream().noneMatch(node -> node.visible()));
        var heat = furnace.materialEffect();
        assertTrue(models.stream().allMatch(node -> node.materialEffect() == null));

        assertEquals(8, sequence.duration());
        assertEquals(6, sequence.checkpoints().get(4).seconds(), EPS);
        var dissolve = widgets.require("tutorial-checkpoint-5").layoutBounds();
        InputHandle input = new InputHandle(demo, ui);
        float markerX = dissolve.x() + dissolve.width() / 2;
        float markerY = dissolve.y() + dissolve.height() / 2;
        input.pointerPressed(markerX, markerY, 0, PointerButton.PRIMARY);
        input.pointerReleased(markerX, markerY, 0, PointerButton.PRIMARY);
        assertEquals(6, slider.value(), EPS);
        assertEquals(6, sequence.viewport().scene().materialTime(), EPS);
        var surface = furnace.materialEffect();
        assertEquals(6, surface.timing().startSeconds(), EPS);
        assertEquals(2, surface.timing().durationSeconds(), EPS);
        assertEquals(0, surface.timing().progressAt(sequence.viewport().scene().materialTime()), EPS);
        assertTrue(furnace.visible());
        assertEquals(models.isEmpty() ? UpdateMode.ON_CHANGE : UpdateMode.EVERY_FRAME, sequence.viewport().updateMode());
        for (SceneNode model : models) {
            assertTrue(model.visible());
            assertSame(surface, model.materialEffect());
        }
        if (zombie != null) assertFalse(zombie.hasAction());

        slider.value(7);
        assertTrue(furnace.visible());
        assertTrue(models.stream().allMatch(SceneNode::visible));
        assertSame(surface, furnace.materialEffect());
        assertEquals(7, sequence.viewport().scene().materialTime(), EPS);
        assertEquals(0.5f, surface.timing().progressAt(sequence.viewport().scene().materialTime()), EPS);
        slider.value(8);
        assertEquals(1, surface.timing().progressAt(sequence.viewport().scene().materialTime()), EPS);
        assertFalse(furnace.visible());
        assertEquals(UpdateMode.ON_CHANGE, sequence.viewport().updateMode());
        assertTrue(models.stream().noneMatch(SceneNode::visible));
        assertTrue(firstTile.visible());
        assertTrue(lastTile.visible());
        slider.value(5.25f);
        if (zombie != null) {
            assertEquals(stride, zombie.walkPhase(), EPS);
            assertEquals(forward, zombie.position(position).z(), EPS);
        }
        assertEquals(0, surface.timing().progressAt(sequence.viewport().scene().materialTime()), EPS);
        assertTrue(furnace.visible());
        assertTrue(models.stream().allMatch(SceneNode::visible));
        assertSame(heat, furnace.materialEffect());
        assertEquals(5.25f, sequence.viewport().scene().materialTime(), EPS);

        slider.value(impact.seconds() + 0.25f);
        assertFalse(MinecraftScene.state(furnace).getValue(FurnaceBlock.LIT));
        assertNull(furnace.materialEffect());
        assertEquals(0, sequence.viewport().scene().materialTime(), EPS);
        assertEquals(1.28f, pivot.position(position).y(), EPS);
        assertTrue(sparks.stream().noneMatch(node -> node.visible()));
        for (var emitter : emitters) assertEquals(impact.seconds() + 0.25 - ignition.seconds(), emitter.time(), EPS);
        for (var smoke : floorSmoke) assertEquals(impact.seconds() + 0.25 - ignition.seconds(), smoke.time(), EPS);

        slider.value(8);
        var replay = widgets.require("tutorial-replay").layoutBounds();
        float x = replay.x() + replay.width() / 2;
        float y = replay.y() + replay.height() / 2;
        input.pointerPressed(x, y, 0, PointerButton.PRIMARY);
        input.pointerReleased(x, y, 0, PointerButton.PRIMARY);
        assertEquals(0, slider.value(), EPS);
        assertFalse(firstTile.visible());
        assertFalse(lastTile.visible());
        assertFalse(pivot.visible());
        assertTrue(models.stream().noneMatch(SceneNode::visible));
        assertNull(furnace.materialEffect());
        assertEquals(0, sequence.viewport().scene().materialTime(), EPS);
        for (var smoke : floorSmoke) assertEquals(-ignition.seconds(), smoke.time(), EPS);
        if (zombie != null) {
            assertEquals(0, zombie.walkStrength(), EPS);
            assertEquals(0.2f, zombie.position(position).z(), EPS);
        }
        demo.tick(new FrameContext(1, 0.25f, 0, FramePhase.ANIMATION));
        assertTrue(firstTile.visible());
    }

    /** Проверяет переход назад по отметкам, обновление карточки и паузу на выбранном кадре. */
    @Test
    void checkpoint_click_seeksAndKeepsItsExplanationVisible() {
        Harness h = new Harness();
        h.click(h.checkpoints[2]);
        assertEquals(3.75f, h.slider.value(), EPS);
        assertEquals("Play", h.play.text());
        assertEquals("STEP 3 / 4", h.step.text());
        assertTrue(h.item.visible());
        assertTrue(h.details.text().contains("The hopper picks it up"));
        h.tick();
        assertEquals(3.75f, h.slider.value(), EPS);

        h.click(h.checkpoints[1]);
        assertEquals(1.5f, h.slider.value(), EPS);
        assertEquals("STEP 2 / 4", h.step.text());
        assertTrue(h.details.text().contains("spout must point down"));
        assertTrue(h.hopper.visible());
        assertFalse(h.item.visible());

        h.click(h.checkpoints[3]);
        assertEquals(6.25f, h.slider.value(), EPS);
        assertEquals("STEP 4 / 4", h.step.text());
        assertTrue(h.details.text().contains("redstone signal"));
        assertFalse(h.item.visible());

        h.click(h.checkpoints[0]);
        assertEquals(0, h.slider.value(), EPS);
        assertEquals("STEP 1 / 4", h.step.text());
        assertFalse(h.hopper.visible());
    }

    /**
     * Проверяет совмещение центров кнопок с координатами их времени на ползунке.
     * @param width ширина окна в логических пикселях.
     */
    @ParameterizedTest
    @ValueSource(ints = {320, 480})
    void checkpoint_layout_alignsWithTimelineAndAvoidsOverlap(int width) {
        Harness h = new Harness(width);
        var slider = h.slider.layoutBounds();
        float[] times = {0, 1.5f, 3.75f, 6.25f};
        float previousRight = Float.NEGATIVE_INFINITY;
        for (int i = 0; i < times.length; i++) {
            var bounds = h.checkpoints[i].layoutBounds();
            assertEquals(slider.x() + slider.width() * times[i] / h.slider.max(),
                    bounds.x() + bounds.width() / 2, EPS);
            assertTrue(bounds.x() >= previousRight);
            assertTrue(bounds.bottom() <= slider.y());
            previousRight = bounds.right();
        }
        assertEquals(width == 320 ? "2" : "Hopper", h.checkpoints[1].text());
    }

    /** Создаёт виджет с настоящей раскладкой и маршрутизацией событий. */
    private static final class Harness {
        /** Корневой виджет проверяемого туториала. */
        private final SceneTutorialPlayer demo = new SceneTutorialPlayer(SceneTutorialDemo.hopperSequence());
        /** Обработчик ввода для корневого дерева. */
        private final InputHandle input;
        /** Ползунок, значение которого измеряется в секундах. */
        private final Slider slider;
        /** Поясняющая подпись текущего этапа. */
        private final Label caption;
        /** Дополнительный текст карточки этапа. */
        private final Label details;
        /** Счётчик активного этапа. */
        private final Label step;
        /** Кнопка паузы и продолжения. */
        private final Button play;
        /** Кнопка повтора. */
        private final Button replay;
        /** Кнопки переходов в порядке времени. */
        private final Button[] checkpoints;
        /** Узел воронки. */
        private final BlockNode hopper;
        /** Узел движущегося слитка. */
        private final ItemNode item;
        /** Номер последнего тестового кадра. */
        private int frame;

        /** Размещает туториал на холсте 480 на 270 логических пикселей. */
        private Harness() {
            this(480);
        }

        /**
         * Размещает туториал на холсте заданной ширины и высотой 270 пикселей.
         * @param width ширина холста в единицах UI.
         */
        private Harness(int width) {
            DefaultUIContext ui = new DefaultUIContext();
            demo.setUiContextInternal(ui);
            input = new InputHandle(demo, ui);
            demo.measure(new LayoutContext(width, 270));
            demo.arrange(new MutableRect(0, 0, width, 270));
            var widgets = NamedWidgetRegistry.from(demo);
            slider = (Slider) widgets.require("tutorial-time");
            caption = (Label) widgets.require("tutorial-caption");
            details = (Label) widgets.require("tutorial-details");
            step = (Label) widgets.require("tutorial-step");
            play = (Button) widgets.require("tutorial-play");
            replay = (Button) widgets.require("tutorial-replay");
            checkpoints = new Button[4];
            for (int i = 0; i < checkpoints.length; i++) {
                checkpoints[i] = (Button) widgets.require("tutorial-checkpoint-" + (i + 1));
            }
            var nodes = ((Scene3D) widgets.require("tutorial-scene")).scene().roots();
            hopper = nodes.stream().filter(BlockNode.class::isInstance).map(BlockNode.class::cast)
                    .filter(node -> MinecraftScene.state(node).is(Blocks.HOPPER)).findFirst().orElseThrow();
            item = nodes.stream().filter(ItemNode.class::isInstance).map(ItemNode.class::cast).findFirst().orElseThrow();
        }

        /** Продвигает реальный tick виджета на 0,25 секунды. */
        private void tick() {
            demo.tick(new FrameContext(++frame, 0.25f, 0, FramePhase.ANIMATION));
        }

        /**
         * Нажимает и отпускает основную кнопку мыши в центре виджета.
         * @param widget цель клика после layout.
         */
        private void click(Widget widget) {
            var bounds = widget.layoutBounds();
            float x = bounds.x() + bounds.width() / 2;
            float y = bounds.y() + bounds.height() / 2;
            input.pointerPressed(x, y, 0, PointerButton.PRIMARY);
            input.pointerReleased(x, y, 0, PointerButton.PRIMARY);
        }
    }
}
