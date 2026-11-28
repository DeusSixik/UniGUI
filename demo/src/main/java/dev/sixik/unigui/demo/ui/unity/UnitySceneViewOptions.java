package dev.sixik.unigui.demo.ui.unity;

import dev.sixik.unigui.demo.ui.scene.SceneTransformGizmo.Pivot;
import dev.sixik.unigui.demo.ui.scene.SceneTransformGizmo.Space;

/**
 * Settings of the Scene view's top bar. They only change how the editor shows and edits the scene,
 * never the scene itself or the Game view.
 */
final class UnitySceneViewOptions {
    /**
     * How the Scene view draws geometry.
     */
    enum DrawMode {
        /**
         * Normal shaded surfaces.
         */
        SHADED("Shaded"),
        /**
         * Edges only, without surfaces.
         */
        WIREFRAME("Wireframe"),
        /**
         * Shaded surfaces with their front-facing edges drawn on top.
         */
        SHADED_WIREFRAME("Shaded Wireframe");

        /**
         * Label in the draw mode dropdown.
         */
        private final String label;

        DrawMode(String label) {
            this.label = label;
        }

        /**
         * @return label in the draw mode dropdown
         */
        String label() {
            return label;
        }

        /**
         * @param label dropdown text
         * @return matching mode, or {@link #SHADED} for unknown text
         */
        static DrawMode fromLabel(String label) {
            for (DrawMode mode : values()) {
                if (mode.label.equals(label)) return mode;
            }
            return SHADED;
        }
    }

    /**
     * Axes of the transform handles.
     */
    Space space = Space.GLOBAL;
    /**
     * Where the handles stand when several objects are selected.
     */
    Pivot pivot = Pivot.CENTER;
    /**
     * Whether the ground grid is drawn.
     */
    boolean grid = true;
    /**
     * Whether gizmo drags snap to increments. Holding Ctrl snaps temporarily.
     */
    boolean snap;
    /**
     * Translation snap step in blocks.
     */
    float moveStep = 1;
    /**
     * Rotation snap step in degrees.
     */
    float rotateStep = 15;
    /**
     * Scale snap step as a change of the scale factor.
     */
    float scaleStep = 0.1f;
    /**
     * Geometry draw mode.
     */
    DrawMode drawMode = DrawMode.SHADED;
    /**
     * Whether the Scene view applies the scene's lights.
     */
    boolean lighting = true;
}
