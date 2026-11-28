package dev.sixik.unigui.api.scene;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiLink;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;
import dev.sixik.unigui.api.docs.WikiThrows;

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
@WikiPage(title = "SceneWireframe", slug = "scene-wireframe", category = "<key:scene.category>", order = 85,
        summary = "<key:scene.wireframe.summary>")
@WikiSection(title = "<key:scene.wireframe.modes.title>", order = 10,
        markdown = "<key:scene.wireframe.modes.body>")
@WikiExample(title = "<key:scene.wireframe.example>", code = """
        var settings = new SceneWireframe(0xFF9AA8BC, 0xFFFFB052, true, node -> node == selected);
        viewport.wireframe(settings);
        """)
@WikiLink(label = "Scene3D", target = "page:scene-3d")
@WikiLink(label = "Scene", target = "page:scene")
@WikiLink(label = "Scene3DRenderer", target = "page:scene-3d-renderer")
@AiDoc(summary = "Selects triangle edge drawing and highlighting for one scene render pass.",
        criticalRules = {
                "Set a viewport's mode with Scene3D.wireframe(settings). Pass null to restore the filled view.",
                "With shaded enabled, draw visible edges over filled surfaces. Edges-only mode draws hidden edges too.",
                "The render-thread predicate receives a geometry owner and chooses its edge color.",
                "Native content rendered outside the captured geometry has no wireframe edges."
        }, related = {"Scene3D", "Scene", "Scene3DRenderer"})
public record SceneWireframe(
        @WikiParameter(description = "<key:scene.wireframe.color>") int colorArgb,
        @WikiParameter(description = "<key:scene.wireframe.highlight.color>") int highlightArgb,
        @WikiParameter(description = "<key:scene.wireframe.shaded>") boolean shaded,
        @WikiParameter(description = "<key:scene.wireframe.highlighted>") Predicate<SceneNode> highlighted) {
    /**
     * @throws NullPointerException if highlighted is null
     */
    @WikiMethod(order = 10, summary = "<key:scene.wireframe.create>")
    @WikiThrows(type = NullPointerException.class, description = "<key:scene.wireframe.null>")
    @AiDoc(order = 10, summary = "Configures edge colors, a depth-tested overlay or edges-only mode, and a selection predicate.")
    public SceneWireframe {
        Objects.requireNonNull(highlighted, "highlighted");
    }

    /**
     * @param node drawn node, or null for geometry without a node
     * @return color of the node's edges
     */
    @WikiMethod(order = 20, summary = "<key:scene.wireframe.color.for>")
    @WikiReturn("<key:scene.wireframe.color.return>")
    @AiDoc(order = 20, summary = "Returns the selected edge color when the node matches the predicate, or the ordinary color otherwise.")
    public int colorFor(@WikiParameter(description = "<key:scene.wireframe.node>") SceneNode node) {
        return node != null && highlighted.test(node) ? highlightArgb : colorArgb;
    }
}
