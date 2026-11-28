package dev.sixik.unigui.demo.ui.unity;

import dev.sixik.unigui.api.core.FrameContext;
import dev.sixik.unigui.api.scene.Scene;
import dev.sixik.unigui.api.scene.SceneNode;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Runs the scene's components: Rotators while Play is running and not paused, material effects always. Both the Scene and the Game view call
 * {@link #tick(Scene, FrameContext)}, since a hidden tab stops ticking; the frame index keeps each frame applied once.
 */
final class UnityPlayMode {
    /**
     * Whether Play is running for each scene.
     */
    private static final Map<Scene, Boolean> RUNNING = new WeakHashMap<>();
    /**
     * Last frame index applied to each scene.
     */
    private static final Map<Scene, Long> LAST_FRAME = new WeakHashMap<>();

    private UnityPlayMode() {
    }

    /**
     * @param scene   shared scene
     * @param running true while Play runs and is not paused
     */
    static void setRunning(Scene scene, boolean running) {
        RUNNING.put(scene, running);
    }

    /**
     * @param scene shared scene
     * @return whether components are running
     */
    static boolean running(Scene scene) {
        return RUNNING.getOrDefault(scene, false);
    }

    /**
     * Advances every component of the scene by one frame, at most once per frame index.
     *
     * @param scene shared scene
     * @param frame frame timing
     */
    static void tick(Scene scene, FrameContext frame) {
        Long last = LAST_FRAME.put(scene, frame.frameIndex());
        if (last != null && last == frame.frameIndex()) return;
        float seconds = Math.min(Math.max(frame.deltaSeconds(), 0), 0.1f);
        boolean running = running(scene);
        // Material effects animate in Edit mode too, as with Unity's Always Refresh; the clock only moves while
        // some object has an effect, so a scene without effects is not redrawn every frame.
        boolean effects = hasEffects(scene);
        float time = effects ? scene.materialTime() + seconds : scene.materialTime();
        if (effects) scene.setMaterialTime(time % 3600);
        for (SceneNode root : scene.roots()) tick(root, running ? seconds : 0, scene.materialTime());
    }

    /**
     * @param scene shared scene
     * @return whether an object has a Material Effect component or an enabled fog with drifting noise
     */
    private static boolean hasEffects(Scene scene) {
        boolean[] found = {false};
        scene.visitVisible(null, node -> {
            if (node instanceof UnitySceneObject object && (object.materialEffectSettings() != null
                    || object.fog() != null && object.fog().animated()
                    && object.componentEnabled(UnitySceneObject.Component.FOG_VOLUME))) found[0] = true;
        });
        return found[0];
    }

    /**
     * @param node      subtree
     * @param seconds   elapsed Play time, zero outside Play
     * @param sceneTime material clock
     */
    private static void tick(SceneNode node, float seconds, float sceneTime) {
        if (node instanceof UnitySceneObject object) {
            object.tickRotator(seconds);
            object.tickMaterialEffect(sceneTime);
        }
        for (SceneNode child : node.children()) tick(child, seconds, sceneTime);
    }
}
