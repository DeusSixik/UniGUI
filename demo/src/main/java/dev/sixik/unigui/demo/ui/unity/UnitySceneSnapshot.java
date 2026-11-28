package dev.sixik.unigui.demo.ui.unity;

import dev.sixik.unigui.api.math.MutableQuat;
import dev.sixik.unigui.api.math.MutableVec3;
import dev.sixik.unigui.api.scene.Scene;
import dev.sixik.unigui.api.scene.SceneNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Captures the editable demo graph while keeping the node identities shared by its editor panels.
 */
final class UnitySceneSnapshot {
    /**
     * Graph rendered by both the Scene and Game viewports.
     */
    private final Scene scene;
    /**
     * Original nodes in parent-first order, including their original sibling order.
     */
    private final List<NodeState> nodes = new ArrayList<>();

    /**
     * Records the pre-Play hierarchy and local transforms.
     *
     * @param scene editable demo graph
     */
    UnitySceneSnapshot(Scene scene) {
        this.scene = scene;
        for (SceneNode root : scene.roots()) capture((UnitySceneObject) root);
    }

    /**
     * Records a node before recording any of its children.
     *
     * @param object demo node to capture
     */
    private void capture(UnitySceneObject object) {
        nodes.add(new NodeState(object, object.parent(), object.name(), object.visible(),
                object.position(new MutableVec3()), object.rotation(new MutableQuat()), object.angles(),
                object.scale(new MutableVec3()), object.light(), object.meshShape(), object.rotatorSpeed(),
                object.disabledComponents(), object.materialEffectSettings(), object.fog()));
        for (SceneNode child : object.children()) {
            if (child instanceof UnitySceneObject editable) capture(editable);
        }
    }

    /**
     * Removes Play-created objects and reinstates the original objects and their local state.
     */
    void restore() {
        for (SceneNode root : List.copyOf(scene.roots())) root.detach();
        for (NodeState state : nodes) {
            state.object.detach();
            for (SceneNode child : List.copyOf(state.object.children())) {
                if (child instanceof UnitySceneObject) child.detach();
            }
        }
        for (NodeState state : nodes) {
            state.restoreProperties();
            if (state.parent == null) scene.addRoot(state.object);
            else state.parent.addChild(state.object);
        }
    }

    /**
     * Editable properties and placement of one original node.
     *
     * @param object   identity retained by the demo
     * @param parent   original parent, or null for a root
     * @param name     original label
     * @param visible  original active flag
     * @param position original local position in scene units
     * @param rotation original local quaternion
     * @param angles   original Inspector angles in degrees
     * @param scale    original local scale factors
     * @param light        original Light component, or null
     * @param meshShape    original Mesh Renderer shape, or null
     * @param rotatorSpeed original Rotator speed, or NaN
     * @param disabled     original switched-off components
     * @param effect       original Material Effect, or null
     * @param fog          original Fog Volume, or null
     */
    private record NodeState(UnitySceneObject object, SceneNode parent, String name, boolean visible,
                             MutableVec3 position, MutableQuat rotation, MutableVec3 angles, MutableVec3 scale,
                             UnityLight light, UnityEditorHierarchyView.GameObjectType meshShape, float rotatorSpeed,
                             java.util.Set<UnitySceneObject.Component> disabled, UnityMaterialEffect effect,
                             UnityFog fog) {
        /**
         * Restores the captured scene properties on the original node.
         */
        void restoreProperties() {
            object.rename(name);
            object.setVisible(visible);
            object.setPosition(position.x(), position.y(), position.z());
            object.restoreRotation(rotation, angles);
            object.setScale(scale.x(), scale.y(), scale.z());
            object.setLight(light);
            object.setMeshShape(meshShape);
            object.setRotatorSpeed(rotatorSpeed);
            object.setMaterialEffectSettings(effect);
            object.setFog(fog);
            object.setDisabledComponents(disabled);
        }
    }
}
