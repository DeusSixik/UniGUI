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
import dev.sixik.unigui.backend.minecraft_impl.BlockNode;
import dev.sixik.unigui.backend.minecraft_impl.ItemNode;
import dev.sixik.unigui.impl.core.DefaultUIContext;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.widgets.interaction.Button;
import dev.sixik.unigui.widgets.interaction.Slider;
import dev.sixik.unigui.widgets.rendering.Scene3D;
import dev.sixik.unigui.widgets.rendering.SceneAnchor3D;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FurnaceBlock;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
    }

    /** Проверяет одинаковый промежуточный кадр при прямой и обратной перемотке. */
    @Test
    void scrub_backwards_restoresVisibilityPoseAndCaption() {
        Harness h = new Harness();
        assertFalse(h.hopper.visible());
        assertFalse(h.item.visible());
        assertEquals("Поставьте сундук", h.caption.text());

        h.slider.value(2.125f);
        assertTrue(h.hopper.visible());
        assertEquals(2.13125f, h.hopper.position(new MutableVec3()).y(), EPS);
        assertFalse(h.item.visible());
        assertEquals("Направьте воронку", h.caption.text());

        h.slider.value(5.375f);
        assertTrue(h.item.visible());
        assertEquals(2, h.hopper.position(new MutableVec3()).y(), EPS);
        assertEquals(3.5125f, h.item.position(new MutableVec3()).y(), EPS);
        assertEquals("Загрузите воронку", h.caption.text());

        h.slider.value(6.25f);
        assertTrue(h.hopper.visible());
        assertFalse(h.item.visible());
        assertEquals("Слиток в сундуке", h.caption.text());

        h.slider.value(5.375f);
        assertTrue(h.item.visible());
        assertEquals(3.5125f, h.item.position(new MutableVec3()).y(), EPS);
        assertEquals("Загрузите воронку", h.caption.text());

        h.slider.value(0);
        assertFalse(h.hopper.visible());
        assertFalse(h.item.visible());
        assertEquals("Поставьте сундук", h.caption.text());
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
        assertEquals("Играть", h.play.text());
        h.input.pointerMoved(bounds.x() + bounds.width() * (5f / 9), y, 0);
        h.input.pointerReleased(bounds.x() + bounds.width() * (5f / 9), y, 0, PointerButton.PRIMARY);
        h.tick();
        assertEquals(5, h.slider.value(), EPS);
        assertTrue(h.item.visible());

        h.click(h.replay);
        assertEquals(0, h.slider.value(), EPS);
        assertFalse(h.hopper.visible());
        assertEquals("Пауза", h.play.text());
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
        assertEquals("Слиток в сундуке", h.caption.text());
        assertEquals("Играть", h.play.text());

        h.click(h.play);
        assertEquals(0, h.slider.value(), EPS);
        assertEquals("Пауза", h.play.text());
        assertFalse(h.hopper.visible());
    }

    /** Проверяет сборку, отскок, зажигание и сброс всех узлов при повторе. */
    @Test
    void furnace_seekAndReplay_restorePositionsRotationAndFire() {
        var sequence = SceneFurnaceTutorial.create();
        var demo = new SceneTutorialDemo(sequence);
        DefaultUIContext ui = new DefaultUIContext();
        demo.setUiContextInternal(ui);
        demo.measure(new LayoutContext(480, 270));
        demo.arrange(new MutableRect(0, 0, 480, 270));
        assertTrue(sequence.viewport().children().stream().noneMatch(SceneAnchor3D.class::isInstance));
        var widgets = NamedWidgetRegistry.from(demo);
        var slider = (Slider) widgets.require("tutorial-time");
        var nodes = sequence.viewport().scene().roots();
        var firstTile = nodes.get(0);
        var lastTile = nodes.get(8);
        var pivot = nodes.stream().filter(node -> !node.children().isEmpty()).findFirst().orElseThrow();
        var furnace = (BlockNode) pivot.children().get(0);
        var sparks = nodes.stream().filter(node -> node.mesh() != null).toList();
        var position = new MutableVec3();
        var rotation = new MutableQuat();

        assertFalse(pivot.visible());
        assertFalse(firstTile.visible());
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
        assertFalse(furnace.state().getValue(FurnaceBlock.LIT));

        slider.value(impact.seconds() + 0.25f);
        assertEquals(1.28f, pivot.position(position).y(), EPS);
        assertTrue(pivot.rotation(rotation).y() > 0.08f);
        slider.value(3.5f);
        assertEquals(1, pivot.position(position).y(), EPS);
        assertEquals(0, pivot.rotation(rotation).y(), EPS);
        assertFalse(furnace.state().getValue(FurnaceBlock.LIT));

        slider.value(ignition.seconds() + 0.26f);
        assertTrue(furnace.state().getValue(FurnaceBlock.LIT));
        assertEquals(4, sparks.stream().filter(node -> node.visible()).count());
        slider.value(5.25f);
        assertTrue(furnace.state().getValue(FurnaceBlock.LIT));
        assertTrue(sparks.stream().noneMatch(node -> node.visible()));
        slider.value(impact.seconds() + 0.25f);
        assertFalse(furnace.state().getValue(FurnaceBlock.LIT));
        assertEquals(1.28f, pivot.position(position).y(), EPS);
        assertTrue(sparks.stream().noneMatch(node -> node.visible()));

        var replay = widgets.require("tutorial-replay").layoutBounds();
        InputHandle input = new InputHandle(demo, ui);
        float x = replay.x() + replay.width() / 2;
        float y = replay.y() + replay.height() / 2;
        input.pointerPressed(x, y, 0, PointerButton.PRIMARY);
        input.pointerReleased(x, y, 0, PointerButton.PRIMARY);
        assertEquals(0, slider.value(), EPS);
        assertFalse(firstTile.visible());
        assertFalse(lastTile.visible());
        assertFalse(pivot.visible());
        demo.tick(new FrameContext(1, 0.25f, 0, FramePhase.ANIMATION));
        assertTrue(firstTile.visible());
    }

    /** Проверяет переход назад по отметкам, обновление карточки и паузу на выбранном кадре. */
    @Test
    void checkpoint_click_seeksAndKeepsItsExplanationVisible() {
        Harness h = new Harness();
        h.click(h.checkpoints[2]);
        assertEquals(3.75f, h.slider.value(), EPS);
        assertEquals("Играть", h.play.text());
        assertEquals("ЭТАП 3 / 4", h.step.text());
        assertTrue(h.item.visible());
        assertTrue(h.details.text().contains("Воронка подберёт его"));
        h.tick();
        assertEquals(3.75f, h.slider.value(), EPS);

        h.click(h.checkpoints[1]);
        assertEquals(1.5f, h.slider.value(), EPS);
        assertEquals("ЭТАП 2 / 4", h.step.text());
        assertTrue(h.details.text().contains("носик должен смотреть вниз"));
        assertTrue(h.hopper.visible());
        assertFalse(h.item.visible());

        h.click(h.checkpoints[3]);
        assertEquals(6.25f, h.slider.value(), EPS);
        assertEquals("ЭТАП 4 / 4", h.step.text());
        assertTrue(h.details.text().contains("редстоуна"));
        assertFalse(h.item.visible());

        h.click(h.checkpoints[0]);
        assertEquals(0, h.slider.value(), EPS);
        assertEquals("ЭТАП 1 / 4", h.step.text());
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
        assertEquals(width == 320 ? "2" : "Воронка", h.checkpoints[1].text());
    }

    /** Создаёт виджет с настоящей раскладкой и маршрутизацией событий. */
    private static final class Harness {
        /** Корневой виджет проверяемого туториала. */
        private final SceneTutorialDemo demo = new SceneTutorialDemo();
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
                    .filter(node -> node.state().is(Blocks.HOPPER)).findFirst().orElseThrow();
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
