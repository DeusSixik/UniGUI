package dev.sixik.unigui.demo.ui.scene;

import dev.sixik.unigui.api.animation.NamedWidgetRegistry;
import dev.sixik.unigui.api.input.InputHandle;
import dev.sixik.unigui.api.input.PointerButton;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.render.DrawCommandType;
import dev.sixik.unigui.api.render.DrawList;
import dev.sixik.unigui.api.scene.FogVolume;
import dev.sixik.unigui.api.scene.LightNode;
import dev.sixik.unigui.api.scene.PointLight;
import dev.sixik.unigui.api.scene.SceneMaterialEffects;
import dev.sixik.unigui.api.scene.SceneNode;
import dev.sixik.unigui.api.scene.SpotLight;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.impl.core.DefaultUIContext;
import dev.sixik.unigui.impl.render.DefaultRenderContext;
import dev.sixik.unigui.widgets.containers.Box;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.widgets.feedback.OverlayLayer;
import dev.sixik.unigui.widgets.interaction.ComboBox;
import dev.sixik.unigui.widgets.interaction.Slider;
import dev.sixik.unigui.widgets.rendering.SceneAnchor3D;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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

    /** Exercises the overlay dropdown and verifies that changing presets updates the same scene on seek and replay. */
    @Test
    void furnaceEffects_chooseInDropdown_previewAndPersistAcrossReplay() {
        var furnace = new SceneNode();
        var base = SceneMaterialEffects.pulse();
        var model = new SceneNode().setMaterialEffect(base);
        var sequence = SceneFurnaceTutorial.create(center -> new SceneNode(), furnace, lit -> {
        }, Box::new, Box::new, Box::new, List.of(model));
        var player = new SceneTutorialPlayer(sequence);
        var root = new OverlayLayer(player);
        var ui = new DefaultUIContext();
        root.setUiContextInternal(ui);
        arrange(root);
        var widgets = NamedWidgetRegistry.from(player);
        var selector = (ComboBox) widgets.require("tutorial-effect");
        var slider = (Slider) widgets.require("tutorial-time");
        var caption = (Label) widgets.require("tutorial-caption");
        assertTrue(widgets.require("tutorial-title").layoutBounds().right() <= selector.layoutBounds().x());
        assertEquals(List.of("Dissolve", "Scanline", "Hologram", "Pulse", "None"), selector.items());
        assertEquals("Dissolve", selector.selectedItem());

        var input = new InputHandle(root, ui);
        click(input, selector.headerButton());
        arrange(root);
        assertTrue(selector.dropDownPopup().opened());
        click(input, selector.optionButton(2));
        assertFalse(selector.dropDownPopup().opened());
        assertEquals("Hologram", selector.selectedItem());
        assertEquals(sequence.effects().previewSeconds(), slider.value(), 0f);
        assertEquals("The surface becomes a hologram", caption.text());
        assertSame(SceneMaterialEffects.hologram().shader(), furnace.materialEffect().shader());
        assertSame(furnace.materialEffect(), model.materialEffect());

        click(input, widgets.require("tutorial-replay"));
        assertEquals(0f, slider.value(), 0f);
        assertFalse(furnace.visible());
        slider.value(7);
        assertSame(SceneMaterialEffects.hologram().shader(), model.materialEffect().shader());

        selector.selectedIndex(1);
        assertEquals(7f, slider.value(), 0f);
        assertSame(SceneMaterialEffects.scanline().shader(), model.materialEffect().shader());
        selector.selectedIndex(3);
        assertSame(SceneMaterialEffects.pulse().shader(), model.materialEffect().shader());
        slider.value(8);
        assertSame(base, model.materialEffect());
        assertTrue(model.visible());
        selector.selectedIndex(4);
        assertSame(base, model.materialEffect());
        slider.value(8);
        assertTrue(furnace.visible());
        assertTrue(model.visible());

        selector.selectedIndex(0);
        assertEquals(sequence.effects().previewSeconds(), slider.value(), 0f);
        assertSame(SceneMaterialEffects.dissolve().shader(), model.materialEffect().shader());
        slider.value(8);
        assertFalse(furnace.visible());
        assertFalse(model.visible());
    }

    /** Verifies the renderer fallback omits an effect selector it cannot use. */
    @Test
    void furnaceEffects_unsupportedRenderer_omitsDropdown() {
        var sequence = SceneFurnaceTutorial.create(center -> new SceneNode(), new SceneNode(), lit -> {
        }, Box::new, Box::new, Box::new, List.of(), false);
        assertNull(layout(new SceneTutorialPlayer(sequence)).find("tutorial-effect"));
        assertTrue(sequence.viewport().scene().roots().stream().noneMatch(LightNode.class::isInstance));
    }

    /** Verifies light visibility and restoration of the same flicker sample when seeking backward. */
    @Test
    void furnaceLights_seekAcrossIgnition_restoresVisibility() {
        var furnace = new SceneNode();
        var sequence = SceneFurnaceTutorial.create(center -> new SceneNode(), furnace, lit -> {
        }, Box::new, Box::new, Box::new);
        var scene = sequence.viewport().scene();
        var lights = new ArrayList<LightNode>();
        float ignition = sequence.checkpoints().get(3).seconds();
        var sparks = scene.roots().stream().filter(node -> node.mesh() != null).toList();
        sequence.frame().sample(ignition - 0.01f);
        assertTrue(sparks.stream().noneMatch(SceneNode::visible));
        sequence.frame().sample(ignition);
        assertEquals(1, sparks.stream().filter(SceneNode::visible).count());
        scene.visitVisible(null, node -> {
            if (node instanceof LightNode light) lights.add(light);
        });
        assertEquals(2, lights.size());
        var fire = lights.get(0);
        assertTrue(fire.light() instanceof PointLight);
        assertSame(furnace, fire.parent());
        var spot = (SpotLight) lights.get(1).light();
        assertNotNull(spot.mask());
        assertNotNull(spot.shadows());
        var atIgnition = fire.light();
        sequence.frame().sample(ignition + 1.89f);
        assertNotEquals(atIgnition, fire.light());
        sequence.frame().sample(ignition);
        assertEquals(atIgnition, fire.light());
        sequence.frame().sample(sequence.duration());
        assertFalse(furnace.visible());
        assertTrue(lights.get(1).visible());
        sequence.frame().sample(0);
        assertTrue(lights.stream().noneMatch(SceneNode::visible));
        assertTrue(sparks.stream().noneMatch(SceneNode::visible));
    }

    /** Samples the fog ramp on the same clock used for the furnace and its shaders. */
    @Test
    void furnaceFog_seekAcrossIgnition_restoresDensityAndClock() {
        var sequence = SceneFurnaceTutorial.create(center -> new SceneNode(), new SceneNode(), lit -> {
        }, Box::new, Box::new, Box::new);
        var scene = sequence.viewport().scene();
        var fog = scene.roots().stream().filter(FogVolume.class::isInstance).map(FogVolume.class::cast)
                .findFirst().orElseThrow();
        assertTrue(fog.noise().strength() > 0);
        assertTrue(fog.lightScattering() > 0);
        sequence.frame().sample(4.4f);
        float density = fog.density();
        assertTrue(density > 0 && density < 0.35f);
        assertEquals(4.4f, scene.materialTime(), 0f);
        sequence.frame().sample(2);
        assertEquals(0, fog.density(), 0f);
        assertEquals(0, scene.materialTime(), 0f);
        sequence.frame().sample(4.4f);
        assertEquals(density, fog.density(), 0f);
        assertEquals(4.4f, scene.materialTime(), 0f);
    }

    /**
     * Lays out the root and its popup in a compact game window.
     *
     * @param root overlay hosting the tutorial
     */
    private static void arrange(OverlayLayer root) {
        root.measure(new LayoutContext(320, 240));
        root.arrange(new MutableRect(0, 0, 320, 240));
    }

    /**
     * Sends a primary mouse click through the regular input pipeline.
     *
     * @param input scene input router
     * @param target widget hit at its layout center
     */
    private static void click(InputHandle input, Widget target) {
        var bounds = target.layoutBounds();
        float x = bounds.x() + bounds.width() / 2;
        float y = bounds.y() + bounds.height() / 2;
        input.pointerPressed(x, y, 0, PointerButton.PRIMARY);
        input.pointerReleased(x, y, 0, PointerButton.PRIMARY);
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
