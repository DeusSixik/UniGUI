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
import dev.sixik.unigui.api.math.Frustum3D;

import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * A hierarchy of 3D nodes and shared lighting settings. Read and modify it on the UI thread.
 */
@WikiPage(
        title = "Scene", slug = "scene",
        category = "API/3D Scene", order = 20,
        summary = "Scene graph that owns root nodes and shared lighting.")
@WikiSection(title = "Ownership", order = 10, markdown = """
        A Scene can be shared by multiple viewports. `roots()` is a live read-only list. `addRoot` moves an existing
        node out of its previous hierarchy. Changes to nodes, hierarchy or light advance `revision()`.
        """)
@WikiSection(title = "Traversal and lighting", order = 20, markdown = """
        Traversal is depth-first in insertion order. A hidden parent hides descendants. A node whose own bounds are
        culled can still have visible children. The default lighting is white ambient light with unit intensity.
        """)
@WikiExample(title = "Add a group with a light source", code = """
        Scene scene = new Scene();
        SceneNode group = new SceneNode().setPosition(2, 0, 0);
        group.addChild(new BlockNode("minecraft:stone"));
        scene.addRoot(group);
        scene.setDirectionalLight(new DirectionalLight(1, -2, 1, 0xFFF1DB, 0.7f));
        """)
@WikiLink(label = "Scene3D", target = "page:scene-3d")
@WikiLink(label = "SceneNode", target = "page:scene-node")
@WikiLink(label = "AmbientLight", target = "page:scene-ambient-light")
@WikiLink(label = "DirectionalLight", target = "page:scene-directional-light")
@AiDoc(
        summary = "Scene graph that owns root nodes and shared lighting.",
        purpose = "Manage scene content independently of the widget that displays it.",
        criticalRules = {
                "Read and mutate the scene on the UI thread.",
                "Reparenting retains the local transform. A root node reports null from `parent()` and the owning graph from `scene()`.",
                "Do not mutate the scene inside `visitVisible` callbacks.",
                "Scene lights apply to custom LIT meshes. Minecraft content uses vanilla or baked lighting."
        },
        related = {"Scene3D", "SceneNode", "AmbientLight", "DirectionalLight"},
        examples = @AiDoc.Example(title = "Add a group with a light source", code = """
                Scene scene = new Scene();
                SceneNode group = new SceneNode().setPosition(2, 0, 0);
                group.addChild(new BlockNode("minecraft:stone"));
                scene.addRoot(group);
                scene.setDirectionalLight(new DirectionalLight(1, -2, 1, 0xFFF1DB, 0.7f));
                """))
public final class Scene {
    /**
     * Internal parent of the scene's root nodes.
     */
    private final SceneNode root = new SceneNode(this);
    /**
     * Revision of the visible state, starting at zero.
     */
    private long revision;
    /**
     * Shared ambient light for materials using LIT shading.
     */
    private AmbientLight ambientLight = AmbientLight.WHITE;
    /**
     * Shared directional light, or null when disabled.
     */
    private DirectionalLight directionalLight;

    /**
     * Creates an empty scene with white ambient light.
     */
    @WikiMethod(section = "Hierarchy", sectionOrder = 10, order = 10,
            summary = "Creates an empty scene with white ambient light.")
    @AiDoc(section = "Hierarchy", sectionOrder = 10, order = 10,
            summary = "Creates an empty scene with white ambient light.")
    public Scene() {
    }

    /**
     * Reads the scene revision for redraw decisions.
     *
     * @return revision counter for hierarchy, node property and lighting changes.
     */
    @WikiMethod(section = "Hierarchy", sectionOrder = 10, order = 50,
            summary = "Reads the scene revision for redraw decisions.")
    @WikiReturn("revision counter for hierarchy, node property and lighting changes")
    @AiDoc(section = "Hierarchy", sectionOrder = 10, order = 50,
            summary = "Reads the scene revision for redraw decisions.")
    public long revision() {
        return revision;
    }

    /**
     * Reads the roots in insertion order.
     *
     * @return a live, read-only list.
     */
    @WikiMethod(section = "Hierarchy", sectionOrder = 10, order = 40,
            summary = "Reads the roots in insertion order.")
    @WikiReturn("a live, read-only list")
    @AiDoc(section = "Hierarchy", sectionOrder = 10, order = 40,
            summary = "Reads the roots in insertion order.")
    public List<SceneNode> roots() {
        return root.children();
    }

    /**
     * Adds a root node, detaching it from its previous parent if needed.
     *
     * @param node node to move.
     * @return this scene.
     * @throws NullPointerException if node is null.
     */
    @WikiMethod(section = "Hierarchy", sectionOrder = 10, order = 20,
            summary = "Adds a root node, detaching it from its previous parent if needed.")
    @WikiReturn("this scene")
    @WikiThrows(type = NullPointerException.class, description = "if node is null.")
    @AiDoc(section = "Hierarchy", sectionOrder = 10, order = 20,
            summary = "Adds a root node, detaching it from its previous parent if needed.")
    public Scene addRoot(
            @WikiParameter(description = "node to move.") SceneNode node) {
        root.addChild(node);
        return this;
    }

    /**
     * Detaches a root node with its descendants.
     *
     * @param node node to remove, or null.
     * @return true if the node was a root of this scene.
     */
    @WikiMethod(section = "Hierarchy", sectionOrder = 10, order = 30,
            summary = "Detaches a root node with its descendants.")
    @WikiReturn("true if the node was a root of this scene")
    @AiDoc(section = "Hierarchy", sectionOrder = 10, order = 30,
            summary = "Detaches a root node with its descendants.")
    public boolean removeRoot(
            @WikiParameter(description = "node to remove, or null.") SceneNode node) {
        return root.removeChild(node);
    }

    /**
     * Reads the ambient light.
     *
     * @return the ambient light settings.
     */
    @WikiMethod(section = "Lighting", sectionOrder = 20, order = 10,
            summary = "Reads the ambient light.")
    @WikiReturn("the ambient light settings")
    @AiDoc(section = "Lighting", sectionOrder = 20, order = 10,
            summary = "Reads the ambient light.")
    public AmbientLight ambientLight() {
        return ambientLight;
    }

    /**
     * Updates ambient light for custom meshes using {@link Material.Shading#LIT}.
     *
     * <p>Minecraft content uses its own lighting. Block models have baked face shading, and items and
     * entities use vanilla lighting fixed in scene space.</p>
     *
     * @param light ambient light settings.
     * @return this scene.
     * @throws NullPointerException if light is null.
     */
    @WikiMethod(section = "Lighting", sectionOrder = 20, order = 20,
            summary = "Updates ambient light for custom meshes using `Material.Shading#LIT`.")
    @WikiReturn("this scene")
    @WikiThrows(type = NullPointerException.class, description = "if light is null.")
    @AiDoc(section = "Lighting", sectionOrder = 20, order = 20,
            summary = "Updates ambient light for custom meshes using `Material.Shading#LIT`.")
    public Scene setAmbientLight(
            @WikiParameter(description = "ambient light settings.") AmbientLight light) {
        Objects.requireNonNull(light, "light");
        if (!ambientLight.equals(light)) {
            ambientLight = light;
            changed();
        }
        return this;
    }

    /**
     * Reads the directional light.
     *
     * @return the directional light, or null.
     */
    @WikiMethod(section = "Lighting", sectionOrder = 20, order = 30,
            summary = "Reads the directional light.")
    @WikiReturn("the directional light, or null")
    @AiDoc(section = "Lighting", sectionOrder = 20, order = 30,
            summary = "Reads the directional light.")
    public DirectionalLight directionalLight() {
        return directionalLight;
    }

    /**
     * Updates directional light for custom meshes using {@link Material.Shading#LIT}.
     *
     * @param light directional light settings, or null to disable it.
     * @return this scene.
     */
    @WikiMethod(section = "Lighting", sectionOrder = 20, order = 40,
            summary = "Updates directional light for custom meshes using `Material.Shading#LIT`.")
    @WikiReturn("this scene")
    @AiDoc(section = "Lighting", sectionOrder = 20, order = 40,
            summary = "Updates directional light for custom meshes using `Material.Shading#LIT`.")
    public Scene setDirectionalLight(
            @WikiParameter(description = "directional light settings, or null to disable it.") DirectionalLight light) {
        if (!Objects.equals(directionalLight, light)) {
            directionalLight = light;
            changed();
        }
        return this;
    }

    /**
     * Visits visible nodes depth-first in insertion order. Hiding a node hides its subtree.
     * Frustum culling tests each node's own AABB and checks its children independently.
     * Unbounded nodes, including groups, are passed to the visitor.
     *
     * @param frustum view volume, or null to skip frustum culling.
     * @param visitor callback invoked synchronously for each visible node.
     * @throws NullPointerException if visitor is null.
     * @throws ConcurrentModificationException if the visitor modifies the scene.
     */
    @WikiMethod(section = "Hierarchy", sectionOrder = 10, order = 60,
            summary = "Visits visible nodes depth-first in insertion order. Hiding a node hides its subtree. Frustum culling tests each node's own AABB and checks its children independently. Unbounded nodes, including groups, are passed to the visitor.")
    @WikiThrows(type = NullPointerException.class, description = "if visitor is null.")
    @WikiThrows(type = ConcurrentModificationException.class, description = "if the visitor modifies the scene.")
    @AiDoc(section = "Hierarchy", sectionOrder = 10, order = 60,
            summary = "Visits visible nodes depth-first in insertion order. Hiding a node hides its subtree. Frustum culling tests each node's own AABB and checks its children independently. Unbounded nodes, including groups, are passed to the visitor.")
    public void visitVisible(
            @WikiParameter(description = "view volume, or null to skip frustum culling.") Frustum3D frustum,
            @WikiParameter(description =
                    "callback invoked synchronously for each visible node.")
            Consumer<? super SceneNode> visitor) {
        Objects.requireNonNull(visitor, "visitor");
        long expectedRevision = revision;
        SceneNode node = root.nextInSubtree(root, true);
        while (node != null) {
            boolean visible = node.visible();
            if (visible && (frustum == null || node.intersects(frustum))) {
                visitor.accept(node);
                if (revision != expectedRevision) {
                    throw new ConcurrentModificationException("Scene modified during traversal");
                }
            }
            node = node.nextInSubtree(root, visible);
        }
    }

    /**
     * Increments the scene revision.
     */
    void changed() {
        revision++;
    }
}
