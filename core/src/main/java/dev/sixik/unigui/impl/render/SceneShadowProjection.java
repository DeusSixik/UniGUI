package dev.sixik.unigui.impl.render;

import dev.sixik.unigui.api.math.MutableMat4;
import dev.sixik.unigui.api.math.MutableQuat;
import dev.sixik.unigui.api.math.MutableVec3;
import dev.sixik.unigui.api.scene.DirectionalLight;
import dev.sixik.unigui.api.scene.DirectionalShadows;

/**
 * Reusable orthographic projection for a directional scene shadow.
 */
public final class SceneShadowProjection {
    /**
     * Volume center in world units.
     */
    private final MutableVec3 center = new MutableVec3();
    /**
     * Light camera orientation, with local negative Z along the rays.
     */
    private final MutableQuat orientation = new MutableQuat();
    /**
     * World-to-light view transform.
     */
    private final MutableMat4 view = new MutableMat4();
    /**
     * Light-to-clip projection.
     */
    private final MutableMat4 projection = new MutableMat4();

    /**
     * Computes {@code out = P * V}, with a cube of side {@code s = settings.size()} centered at the configured
     * world position. For normalized ray direction {@code d}, yaw is {@code atan2(-d.x,-d.z)} and pitch is
     * {@code atan2(d.y,hypot(d.x,d.z))}. The light camera looks along {@code d}. Translating light-space Z by
     * {@code -s} puts the cube center halfway between the near plane {@code s/2} and far plane {@code 3*s/2}.
     *
     * @param light    source of the world-space ray direction
     * @param settings center and side length of the shadow volume
     * @param out      destination for the world-to-light clip matrix, with clip coordinates in [-1, 1]
     * @return the destination matrix
     */
    public MutableMat4 update(DirectionalLight light, DirectionalShadows settings, MutableMat4 out) {
        double yaw = Math.atan2(-light.x(), -light.z());
        double pitch = Math.atan2(light.y(), Math.hypot(light.x(), light.z()));
        double side = settings.size();
        view.setView(center.set(settings.centerX(), settings.centerY(), settings.centerZ()),
                orientation.setYawPitch(yaw, pitch));
        view.setTranslationComponent(2, view.get(2, 3) - side);
        projection.setOrthographic(side, 1, side * 0.5, side * 1.5);
        return out.setProduct(projection, view);
    }
}
