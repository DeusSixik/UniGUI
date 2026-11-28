package dev.sixik.unigui.api.scene;

import java.util.Objects;
import java.util.function.Predicate;

/**
 * Wireframe drawing of a render pass, for example the Wireframe and Shaded Wireframe modes of an editor viewport.
 *
 * <p>The renderer draws the triangle edges of every surface it renders: custom meshes and Minecraft blocks, items
 * and entities alike, because it uses the geometry it would otherwise fill. Edges are drawn in
 * {@link #colorArgb()}, and in {@link #highlightArgb()} for nodes accepted by {@link #highlighted()}. Content a
 * renderer cannot capture as geometry has no edges.</p>
 *
 * @param colorArgb     packed ARGB color of ordinary edges
 * @param highlightArgb packed ARGB color of highlighted edges
 * @param shaded        true to draw the surfaces and the visible edges over them; false to draw every edge, hidden
 *                      ones included, without surfaces
 * @param highlighted   nodes whose edges use the highlight color; called on the render thread for every drawn node
 */
public record SceneWireframe(int colorArgb, int highlightArgb, boolean shaded, Predicate<SceneNode> highlighted) {
    /**
     * @throws NullPointerException if highlighted is null
     */
    public SceneWireframe {
        Objects.requireNonNull(highlighted, "highlighted");
    }

    /**
     * @param node drawn node, or null for geometry without a node
     * @return color of the node's edges
     */
    public int colorFor(SceneNode node) {
        return node != null && highlighted.test(node) ? highlightArgb : colorArgb;
    }
}
