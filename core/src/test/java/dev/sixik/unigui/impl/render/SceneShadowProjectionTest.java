package dev.sixik.unigui.impl.render;

import dev.sixik.unigui.api.math.MutableMat4;
import dev.sixik.unigui.api.math.MutableVec3;
import dev.sixik.unigui.api.scene.DirectionalLight;
import dev.sixik.unigui.api.scene.DirectionalShadows;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Checks the projection used for both drawing the shadow map and sampling it in a material. */
class SceneShadowProjectionTest {
    /** Covers float error from quaternion conversion and transforming world points. */
    private static final float EPSILON = 2e-6f;

    /**
     * Places three points on the light axis and checks their known positions across the shadow depth range.
     * @param x world X component of the light ray
     * @param y world Y component of the light ray
     * @param z world Z component of the light ray
     */
    @ParameterizedTest
    @CsvSource({"0,0,-1", "0,-1,0", "0,1,0", "1,-2,3"})
    void projection_lightAxisCoversDepthRange(float x, float y, float z) {
        DirectionalLight light = new DirectionalLight(x, y, z, 0xFFFFFF, 1);
        DirectionalShadows settings = new DirectionalShadows(3, 4, -5, 8);
        MutableMat4 clip = new SceneShadowProjection().update(light, settings, new MutableMat4());
        for (int offset = -1; offset <= 1; offset++) {
            float distance = offset * settings.size() * 0.5f;
            MutableVec3 point = new MutableVec3(settings.centerX() + light.x() * distance,
                    settings.centerY() + light.y() * distance, settings.centerZ() + light.z() * distance);
            assertTrue(clip.transformPoint(point, point));
            assertEquals(0, point.x(), EPSILON);
            assertEquals(0, point.y(), EPSILON);
            assertEquals(offset, point.z(), EPSILON);
        }
    }
}
