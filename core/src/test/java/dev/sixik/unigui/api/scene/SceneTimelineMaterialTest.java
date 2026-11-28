package dev.sixik.unigui.api.scene;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Checks timed surface effects without requiring a graphics device. */
final class SceneTimelineMaterialTest {
    /** All sampled integer and half-second values in these tests are exact floats. */
    private static final float EXACT = 0f;

    /** Verifies that replacing or rewinding a group clip preserves independent base properties. */
    @Test
    void materialEffect_sharedClip_restoresSurfacesAndVisibilityOnSeek() {
        var furnace = new SceneNode();
        var model = new SceneNode();
        var child = new SceneNode();
        furnace.addChild(child);
        var scene = new Scene().addRoot(furnace).addRoot(model).setMaterialTime(25);
        var heat = SceneMaterialEffects.pulse();
        var base = SceneMaterialEffects.scanline();
        var dissolve = SceneMaterialEffects.dissolve();
        model.setMaterialEffect(base);
        var timeline = new SceneTimeline();
        timeline.track(furnace).visible(0, false).visible(2, true).materialEffect(0, null).materialEffect(4, heat);
        var clip = timeline.materialEffect(6, 8, dissolve, SceneTimeline.EffectEnd.HIDE, furnace, model);

        timeline.sample(6);
        assertSame(dissolve.shader(), furnace.materialEffect().shader());
        assertSame(furnace.materialEffect(), model.materialEffect());
        assertEquals(0, furnace.materialEffect().timing().progressAt(furnace.materialEffectTime(scene.materialTime())), EXACT);
        timeline.sample(8);
        assertFalse(furnace.visible());
        assertFalse(model.visible());
        var visible = new ArrayList<SceneNode>();
        scene.visitVisible(null, visible::add);
        assertTrue(visible.isEmpty());
        assertSame(heat, furnace.materialEffect());
        assertSame(base, model.materialEffect());

        timeline.sample(5);
        assertTrue(furnace.visible());
        assertTrue(model.visible());
        assertSame(heat, furnace.materialEffect());
        assertEquals(25, furnace.materialEffectTime(scene.materialTime()), EXACT);
        timeline.sample(0);
        assertFalse(furnace.visible());
        assertNull(furnace.materialEffect());

        model.setVisible(false);
        timeline.sample(8);
        clip.setEffect(null, SceneTimeline.EffectEnd.HIDE);
        timeline.sample(8);
        assertTrue(furnace.visible());
        assertFalse(model.visible());
        assertSame(base, model.materialEffect());
        scene.visitVisible(null, visible::add);
        assertEquals(List.of(furnace, child), visible);
    }

    /** Checks local shader time and a final frame that stays frozen during further samples. */
    @Test
    void materialEffect_stageHold_freezesLocalElapsedAndShaderTime() {
        var node = new SceneNode();
        var scene = new Scene().addRoot(node).setMaterialTime(200);
        var timeline = new SceneTimeline();
        var stage = timeline.stage(4);
        stage.materialEffect(1, 3, SceneMaterialEffects.hologram(), SceneTimeline.EffectEnd.HOLD, node);
        assertEquals(7, timeline.duration(), EXACT);

        timeline.sample(6);
        var timing = node.materialEffect().timing();
        assertEquals(2, node.materialEffectTime(scene.materialTime()), EXACT);
        assertEquals(0.5f, timing.progressAt(node.materialEffectTime(scene.materialTime())), EXACT);
        timeline.sample(7);
        long finalRevision = scene.revision();
        timeline.sample(30);
        assertEquals(finalRevision, scene.revision());
        assertEquals(3, node.materialEffectTime(scene.materialTime()), EXACT);
        assertEquals(2, timing.elapsedAt(node.materialEffectTime(scene.materialTime())), EXACT);
        assertEquals(1, timing.progressAt(node.materialEffectTime(scene.materialTime())), EXACT);
        timeline.sample(4);
        assertNull(node.materialEffect());
    }

    /** Verifies overlap precedence and the release of restoring clips at their exclusive end. */
    @Test
    void materialEffect_overlappingClips_usesLatestApplicableStartAndInsertion() {
        var node = new SceneNode();
        var timeline = new SceneTimeline();
        var earlier = SceneMaterialEffects.pulse();
        var chosen = SceneMaterialEffects.scanline();
        timeline.materialEffect(3, 4, SceneMaterialEffects.hologram(), SceneTimeline.EffectEnd.RESTORE, node);
        timeline.materialEffect(1, 6, earlier, SceneTimeline.EffectEnd.RESTORE, node);
        timeline.materialEffect(3, 4, chosen, SceneTimeline.EffectEnd.RESTORE, node);

        timeline.sample(3);
        assertSame(chosen.shader(), node.materialEffect().shader());
        timeline.sample(4);
        assertSame(earlier.shader(), node.materialEffect().shader());
        timeline.sample(6);
        assertNull(node.materialEffect());
    }

    /** Rejects invalid intervals or targets before registering any part of a clip. */
    @Test
    void materialEffect_invalidArguments_leaveTimelineUnchanged() {
        var timeline = new SceneTimeline();
        var node = new SceneNode();
        var effect = SceneMaterialEffects.dissolve();
        var end = SceneTimeline.EffectEnd.HIDE;
        assertThrows(IllegalArgumentException.class, () -> timeline.materialEffect(1, 1, effect, end, node));
        assertThrows(IllegalArgumentException.class, () -> timeline.materialEffect(0, Float.NaN, effect, end, node));
        assertThrows(IllegalArgumentException.class, () -> timeline.materialEffect(0, 1, effect, end));
        assertThrows(NullPointerException.class, () -> timeline.materialEffect(0, 1, effect, end, node, null));
        assertEquals(0, timeline.duration(), EXACT);
        timeline.sample(1);
        assertTrue(node.visible());
        assertNull(node.materialEffect());
    }
}
