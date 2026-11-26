package dev.sixik.unigui.demo.ui.scene;

import dev.sixik.unigui.api.animation.NamedWidgetRegistry;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.render.DrawCommandType;
import dev.sixik.unigui.api.render.DrawList;
import dev.sixik.unigui.api.scene.SceneNode;
import dev.sixik.unigui.impl.core.DefaultUIContext;
import dev.sixik.unigui.impl.render.DefaultRenderContext;
import dev.sixik.unigui.widgets.containers.Box;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.widgets.interaction.Slider;
import dev.sixik.unigui.widgets.rendering.SceneAnchor3D;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Проверяет замену иконок, которые поставляют адаптеры Minecraft.
 */
class SceneTutorialPlayerTest {
    /**
     * Проверяет видимость плашки на границе отскока и её привязку при перемотке.
     */
    @Test
    void furnaceNote_followsFallAndDisappearsAfterBounce() {
        var sequence = SceneFurnaceTutorial.create(center -> new SceneNode(), new SceneNode(), lit -> {
                },
                Box::new, Box::new, Box::new);
        var player = new SceneTutorialPlayer(sequence);
        player.setUiContextInternal(new DefaultUIContext());
        var slider = (Slider) layout(player).require("tutorial-time");
        var viewport = sequence.viewport();
        var note = viewport.children().stream().filter(SceneAnchor3D.class::isInstance)
                .map(SceneAnchor3D.class::cast).findFirst().orElseThrow();
        assertEquals("Falling!", ((Label) note.widget()).text());
        assertFalse(note.visible());

        slider.value(sequence.checkpoints().get(1).seconds());
        viewport.tick(null);
        assertTrue(note.visible());
        float startY = note.layoutBounds().y();

        slider.value(3.1f);
        viewport.tick(null);
        assertTrue(note.visible());
        assertTrue(note.layoutBounds().y() > startY);

        slider.value(3.35f);
        viewport.tick(null);
        assertFalse(note.visible());

        slider.value(2.5f);
        viewport.tick(null);
        assertTrue(note.visible());

        slider.value(0);
        viewport.tick(null);
        assertFalse(note.visible());
    }

    /**
     * Проверяет размеры звёздного фона и обновление параллакса после движения камеры.
     */
    @Test
    void furnaceSky_drawsBehindViewportAndTracksOrbit() {
        var sequence = SceneFurnaceTutorial.create(center -> new SceneNode(), new SceneNode(), lit -> {
                },
                Box::new, Box::new, Box::new);
        var player = new SceneTutorialPlayer(sequence);
        player.setUiContextInternal(new DefaultUIContext());
        layout(player);

        var backdrop = sequence.backdrop();
        var viewport = sequence.viewport();
        assertSame(viewport.parent(), backdrop.parent());
        assertSame(backdrop, viewport.parent().children().get(0));

        viewport.orbit().setAngles(Math.PI * 0.5, 0.25);
        viewport.orbit().zoom(2);
        var list = new DrawList();
        backdrop.render(new DefaultRenderContext(list));
        var draw = list.commands().stream().filter(command -> command.type() == DrawCommandType.SHADER)
                .findFirst().orElseThrow();
        assertEquals("unigui:furnace_sky", draw.shader().id());
        assertEquals(viewport.layoutBounds().width(), draw.bounds().width(), 0);
        assertEquals(viewport.layoutBounds().height(), draw.bounds().height(), 0);
        // Uniforms use single-precision values even when the orbit is computed in double precision.
        assertArrayEquals(new float[]{1, 0, 0.25f, 2}, draw.shaderUniforms().values().get("Orbit").floats(), 1e-6f);
    }

    /**
     * Проверяет карточку при переходах вперёд и назад, включая повторное создание её иконки.
     */
    @Test
    void checkpoint_seekBothWays_showsOnlyCurrentIcon() {
        var sequence = SceneFurnaceTutorial.create(center -> new SceneNode(), new SceneNode(), lit -> {
                },
                () -> icon("floor-icon"), () -> icon("furnace-icon"), () -> icon("fire-icon"));
        var player = new SceneTutorialPlayer(sequence);
        player.setUiContextInternal(new DefaultUIContext());
        var slider = (Slider) NamedWidgetRegistry.from(player).require("tutorial-time");

        var initial = layout(player);
        assertNotNull(initial.find("floor-icon"));
        assertNull(initial.find("fire-icon"));

        slider.value(4);
        var ignition = layout(player);
        assertNotNull(ignition.find("fire-icon"));
        assertNull(ignition.find("floor-icon"));

        slider.value(0);
        var rewound = layout(player);
        assertNotNull(rewound.find("floor-icon"));
        assertNull(rewound.find("fire-icon"));
    }

    /**
     * Создаёт замену ванильной иконки со стабильным ID для проверки дерева.
     *
     * @param id идентификатор в реестре виджетов.
     * @return новый экземпляр иконки.
     */
    private static Box icon(String id) {
        Box icon = new Box();
        icon.id(id);
        return icon;
    }

    /**
     * Применяет изменения дерева и пересобирает реестр после смены этапа.
     *
     * @param player корневой виджет проигрывателя.
     * @return реестр текущего дерева UI.
     */
    private static NamedWidgetRegistry layout(SceneTutorialPlayer player) {
        player.measure(new LayoutContext(427, 240));
        player.arrange(new MutableRect(0, 0, 427, 240));
        return NamedWidgetRegistry.from(player);
    }
}
