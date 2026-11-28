package dev.sixik.unigui.impl.render;

import dev.sixik.unigui.api.math.MutableQuat;
import dev.sixik.unigui.api.scene.LightNode;
import dev.sixik.unigui.api.scene.SceneNode;
import dev.sixik.unigui.api.scene.SpotLight;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Checks world placement and the projection shared by the light mask and depth pass. */
class SceneLightTransformTest {
    /** Float quaternion rounding is below this tolerance for the poses and coordinates tested here. */
    private static final double EPSILON = 1e-6;

    /** A parent scale moves the child origin while the cone keeps the parent's rigid rotation. */
    @Test
    void update_parentRotationAndScale_aimsRigidConeFromWorldOrigin() {
        var spot = new LightNode(new SpotLight(0xFFFFFF, 1, 10, 0.4f, 0.6f));
        spot.setPosition(0, 0, -1);
        var parent = new SceneNode().setPosition(1, 2, 3).setScale(2, 3, 4)
                .setRotation(new MutableQuat().setYawPitch(Math.PI / 2, 0));
        parent.addChild(spot);
        var transform = new SceneLightTransform();
        assertTrue(transform.update(spot));
        var position = transform.position();
        assertEquals(-3, position.x(), EPSILON);
        assertEquals(2, position.y(), EPSILON);
        assertEquals(3, position.z(), EPSILON);
        assertEquals(-1, transform.direction().x(), EPSILON);
        assertEquals(0, transform.direction().y(), EPSILON);
        assertEquals(0, transform.direction().z(), EPSILON);
        var clip = transform.clip();
        assertEquals(0, clip.transformComponent(0, -5, 2, 3, 1), EPSILON);
        assertEquals(0, clip.transformComponent(1, -5, 2, 3, 1), EPSILON);
        assertEquals(2, clip.transformComponent(3, -5, 2, 3, 1), EPSILON);
    }

    /** The cone edge fills the projected square and the near/far planes map to canonical depth. */
    @Test
    void update_perspectiveCone_matchesClipEdges() {
        var light = new SpotLight(0xFFFFFF, 1, 10, 0.5f, 0.6f);
        var transform = new SceneLightTransform();
        assertTrue(transform.update(new LightNode(light)));
        var clip = transform.clip();
        double edge = 2 * Math.tan(light.outerAngle());
        assertEquals(2, clip.transformComponent(0, edge, 0, -2, 1), EPSILON);
        assertEquals(2, clip.transformComponent(3, edge, 0, -2, 1), EPSILON);
        assertEquals(-0.1, clip.transformComponent(2, 0, 0, -0.1, 1), EPSILON);
        assertEquals(10, clip.transformComponent(2, 0, 0, -10, 1), EPSILON);
    }
}
