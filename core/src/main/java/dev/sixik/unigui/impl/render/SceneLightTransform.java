package dev.sixik.unigui.impl.render;

import dev.sixik.unigui.api.math.MutableMat4;
import dev.sixik.unigui.api.math.MutableQuat;
import dev.sixik.unigui.api.math.MutableVec3;
import dev.sixik.unigui.api.math.Vec3View;
import dev.sixik.unigui.api.scene.LightNode;
import dev.sixik.unigui.api.scene.SceneNode;
import dev.sixik.unigui.api.scene.SpotLight;

/**
 * Reuses scratch storage when resolving a light's world pose and perspective projection.
 */
public final class SceneLightTransform {
    /**
     * The light's world origin, in scene units.
     */
    private final MutableVec3 position = new MutableVec3();
    /**
     * Unit world ray direction for a spotlight.
     */
    private final MutableVec3 direction = new MutableVec3();
    /**
     * Composed orientation from the light and its ancestors.
     */
    private final MutableQuat rotation = new MutableQuat();
    /**
     * Scratch quaternion for each ancestor in the composition.
     */
    private final MutableQuat parentRotation = new MutableQuat();
    /**
     * Scratch node world matrix or rigid world-to-light view matrix.
     */
    private final MutableMat4 view = new MutableMat4();
    /**
     * World-to-light clip transform, with normalized depth in [-1, 1].
     */
    private final MutableMat4 clip = new MutableMat4();

    /**
     * Updates the world origin, and for a spotlight computes a perspective transform.
     * The origin is the translation of the node's full world matrix. The direction is
     * {@code q * (0, 0, -1) * q^-1}, with q the ordered product of all ancestor rotations and the node rotation.
     * The clip transform is {@code P * V}, where V uses that rigid pose and P has full FOV
     * {@code 2 * outerAngle}, square aspect, near plane {@code 0.01 * range} and far plane {@code range}.
     *
     * @param node light whose configuration and parent chain to resolve
     * @return false if the world origin falls outside the finite GPU coordinate range
     */
    public boolean update(LightNode node) {
        node.worldMatrix(view);
        float x = (float) view.get(0, 3);
        float y = (float) view.get(1, 3);
        float z = (float) view.get(2, 3);
        if (!Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(z)) return false;
        position.set(x, y, z);
        if (node.light() instanceof SpotLight spot) {
            node.rotation(rotation);
            for (SceneNode parent = node.parent(); parent != null; parent = parent.parent()) {
                rotation.setProduct(parent.rotation(parentRotation), rotation);
            }
            rotation.rotate(0, 0, -1, direction);
            view.setView(position, rotation);
            clip.setPerspective(2.0 * spot.outerAngle(), 1, 0.01 * spot.range(), spot.range()).setProduct(clip, view);
        }
        return true;
    }

    /**
     * @return borrowed world origin in scene units, valid until the next update
     */
    public Vec3View position() {
        return position;
    }

    /**
     * @return borrowed unit ray direction, valid after a spotlight update
     */
    public Vec3View direction() {
        return direction;
    }

    /**
     * @return borrowed world-to-light clip transform, valid after a spotlight update
     */
    public MutableMat4 clip() {
        return clip;
    }
}
