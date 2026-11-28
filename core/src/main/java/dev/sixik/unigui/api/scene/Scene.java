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
@WikiSection(title = "<key:scene.section.shadows.title>", order = 30, markdown = "<key:scene.section.shadows.body>")
@WikiExample(title = "<key:scene.example.directional.shadows.title>", code = """
        scene.setAmbientLight(new AmbientLight(0xD7E7FF, 0.6f))
             .setDirectionalLight(new DirectionalLight(-1, -2, 1, 0xFFF1DC, 0.8f))
             .setDirectionalShadows(new DirectionalShadows(0, 2, 0, 12))
             .setMinecraftBlockLighting(true);
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
@WikiLink(label = "LightNode", target = "page:scene-light-node")
@WikiLink(label = "FogVolume", target = "page:scene-fog-volume")
@AiDoc(
        summary = "Scene graph that owns root nodes and shared lighting.",
        purpose = "Manage scene content independently of the widget that displays it.",
        criticalRules = {
                "Read and mutate the scene on the UI thread.",
                "Reparenting retains the local transform. A root node reports null from `parent()` and the owning graph from `scene()`.",
                "Do not mutate the scene inside `visitVisible` callbacks.",
                "Scene lights apply to custom LIT meshes and optionally to baked Minecraft blocks on Cleanroom 1.12.2 and Minecraft 1.20.1, 1.21.1 and 26.3.",
                "On these versions set directional shadows to let baked blocks and custom meshes cast shadows on each other."
        },
        related = {"Scene3D", "SceneNode", "AmbientLight", "DirectionalLight", "LightNode", "FogVolume"},
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
     * Optional light-space shadow volume for the directional light.
     */
    private DirectionalShadows directionalShadows;
    /**
     * Whether baked Minecraft geometry uses the scene lights on supported renderers.
     */
    private boolean minecraftBlockLighting;
    /**
     * Timeline time supplied to scene surface functions, in seconds.
     */
    private float materialTime;
    /**
     * Shared ambient light for materials using LIT shading.
     */
    private AmbientLight ambientLight = AmbientLight.WHITE;
    /**
     * Shared directional light, or null when disabled.
     */
    private DirectionalLight directionalLight;
    /**
     * Number of nested {@link #renderUnlit(Runnable)} passes in progress. While positive, the lighting getters
     * report a flat setup and {@link #visitVisible} skips light nodes. It is not part of the revision.
     */
    private int unlitPasses;
    /**
     * Wireframe settings of the {@link #renderWireframe} pass in progress, or null. It is not part of the revision.
     */
    private SceneWireframe wireframe;

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
    @WikiMethod(section = "<key:scene.api.section.lighting>", sectionOrder = 20, order = 10,
            summary = "Reads the ambient light.")
    @WikiReturn("the ambient light settings")
    @AiDoc(section = "Lighting", sectionOrder = 20, order = 10,
            summary = "Reads the ambient light.")
    public AmbientLight ambientLight() {
        return unlitPasses > 0 ? AmbientLight.WHITE : ambientLight;
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
    @WikiMethod(section = "<key:scene.api.section.lighting>", sectionOrder = 20, order = 20,
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
    @WikiMethod(section = "<key:scene.api.section.lighting>", sectionOrder = 20, order = 30,
            summary = "Reads the directional light.")
    @WikiReturn("the directional light, or null")
    @AiDoc(section = "Lighting", sectionOrder = 20, order = 30,
            summary = "Reads the directional light.")
    public DirectionalLight directionalLight() {
        return unlitPasses > 0 ? null : directionalLight;
    }

    /**
     * Updates directional light for custom meshes using {@link Material.Shading#LIT}.
     *
     * @param light directional light settings, or null to disable it.
     * @return this scene.
     */
    @WikiMethod(section = "<key:scene.api.section.lighting>", sectionOrder = 20, order = 40,
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
     * @return settings for the directional light's shadow map, or null if disabled
     */
    public DirectionalShadows directionalShadows() {
        return unlitPasses > 0 ? null : directionalShadows;
    }

    /**
     * Configures directional shadows for indexed meshes and baked blocks on Cleanroom 1.12.2 and Minecraft 1.20.1, 1.21.1 and 26.3.
     *
     * @param shadows shadow volume and quality settings, or null to disable the shadow pass
     * @return this scene
     */
    @WikiMethod(section = "<key:scene.api.section.lighting>", sectionOrder = 20, summary = "<key:scene.api.set.directional.shadows.summary>")
    @WikiReturn("<key:scene.api.return.scene>")
    @AiDoc(section = "Lighting", summary = "Configures a directional shadow volume for custom meshes and baked blocks on Cleanroom 1.12.2 and Minecraft 1.20.1, 1.21.1 and 26.3.")
    public Scene setDirectionalShadows(
            @WikiParameter(description = "<key:scene.api.set.directional.shadows.param.shadows>") DirectionalShadows shadows) {
        if (!Objects.equals(directionalShadows, shadows)) {
            directionalShadows = shadows;
            changed();
        }
        return this;
    }

    /**
     * @return whether baked blocks opt into scene lighting
     */
    public boolean minecraftBlockLighting() {
        return unlitPasses <= 0 && minecraftBlockLighting;
    }

    /**
     * Uses the scene ambient and directional lights for baked Minecraft blocks on Cleanroom 1.12.2 and Minecraft 1.20.1, 1.21.1 and 26.3.
     *
     * @param enabled true to use the scene lights with Minecraft's texture tint and ambient occlusion
     * @return this scene
     */
    @WikiMethod(section = "<key:scene.api.section.lighting>", sectionOrder = 20, summary = "<key:scene.api.set.minecraft.block.lighting.summary>")
    @WikiReturn("<key:scene.api.return.scene>")
    @AiDoc(section = "Lighting", summary = "Uses scene lights for baked Minecraft blocks while preserving biome tint and ambient occlusion.")
    public Scene setMinecraftBlockLighting(
            @WikiParameter(description = "<key:scene.api.set.minecraft.block.lighting.param.enabled>") boolean enabled) {
        if (minecraftBlockLighting != enabled) {
            minecraftBlockLighting = enabled;
            changed();
        }
        return this;
    }

    /**
     * @return the time in seconds passed to material effects
     */
    public float materialTime() {
        return materialTime;
    }

    /**
     * Supplies material effect time from a timeline or another application clock.
     * Changing the time invalidates the scene, including on-change viewports.
     *
     * @param seconds finite time in seconds
     * @return this scene
     * @throws IllegalArgumentException if seconds is nonfinite
     */
    @WikiMethod(section = "<key:scene.api.section.lighting>", sectionOrder = 20, summary = "<key:scene.api.set.material.time.summary>")
    @WikiReturn("<key:scene.api.return.scene>")
    @WikiThrows(type = IllegalArgumentException.class, description = "<key:scene.api.throws.nonfinite.time>")
    @AiDoc(section = "Lighting", summary = "Sets the GLSL material time in seconds and invalidates cached rendering on change.")
    public Scene setMaterialTime(
            @WikiParameter(description = "<key:scene.api.set.material.time.param.seconds>") float seconds) {
        if (!Float.isFinite(seconds)) throw new IllegalArgumentException("Material time must be finite");
        if (materialTime != seconds) {
            materialTime = seconds;
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
     * @throws NullPointerException            if visitor is null.
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
            if (visible && !(unlitPasses > 0 && node instanceof LightNode)
                    && (frustum == null || node.intersects(frustum))) {
                visitor.accept(node);
                if (revision != expectedRevision) {
                    throw new ConcurrentModificationException("Scene modified during traversal");
                }
            }
            node = node.nextInSubtree(root, visible);
        }
    }

    /**
     * Runs a render pass that sees the scene without lighting, for example an editor viewport with lights turned off.
     *
     * <p>During the pass the ambient light reads as {@link AmbientLight#WHITE}, the directional light and its
     * shadows read as null, baked blocks use their own lighting and {@link #visitVisible} skips every
     * {@link LightNode}. The stored settings and the revision do not change, so other viewports keep the lit
     * result. The pass must run synchronously on the render thread.</p>
     *
     * @param pass renderer call that reads the scene
     * @throws NullPointerException if pass is null
     */
    public void renderUnlit(Runnable pass) {
        Objects.requireNonNull(pass, "pass");
        unlitPasses++;
        try {
            pass.run();
        } finally {
            unlitPasses--;
        }
    }

    /**
     * Runs a render pass that draws the scene as a wireframe, for example an editor viewport in Wireframe mode.
     *
     * <p>During the pass {@link #wireframe()} returns the settings; renderers that support it draw the edges of
     * the geometry they render. The stored scene and its revision do not change, so other viewports keep the
     * filled result. Passes nest: the innermost settings apply. The pass must run synchronously on the render
     * thread.</p>
     *
     * @param settings edge colors and whether surfaces stay visible
     * @param pass     renderer call that reads the scene
     * @throws NullPointerException if an argument is null
     */
    public void renderWireframe(SceneWireframe settings, Runnable pass) {
        Objects.requireNonNull(settings, "settings");
        Objects.requireNonNull(pass, "pass");
        SceneWireframe previous = wireframe;
        wireframe = settings;
        try {
            pass.run();
        } finally {
            wireframe = previous;
        }
    }

    /**
     * @return wireframe settings of the render pass in progress, or null to draw filled surfaces only
     */
    public SceneWireframe wireframe() {
        return wireframe;
    }

    /**
     * Increments the scene revision.
     */
    void changed() {
        revision++;
    }
}
