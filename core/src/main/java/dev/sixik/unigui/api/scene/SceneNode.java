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
import dev.sixik.unigui.api.math.Aabb;
import dev.sixik.unigui.api.math.Frustum3D;
import dev.sixik.unigui.api.math.MutableMat4;
import dev.sixik.unigui.api.math.MutableQuat;
import dev.sixik.unigui.api.math.MutableVec3;
import dev.sixik.unigui.api.math.Ray;
import dev.sixik.unigui.api.math.QuatView;
import dev.sixik.unigui.api.math.Vec3View;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A node with a local transform and its own content. Use it on the UI thread.
 */
@WikiPage(
        title = "SceneNode", slug = "scene-node",
        category = "API/3D Scene", order = 30,
        summary = "Transformable scene graph node with optional mesh, material and hover geometry.")
@WikiSection(title = "Transforms", order = 10, markdown = """
        The node maps local coordinates into parent space with `T(position) * R * S * T(-origin)`. The parent world
        matrix then maps the result into scene space. `centerOrigin()` uses the center of a unit block's base at
        `(0.5, 0, 0.5)`.
        """)
@WikiSection(title = "Custom picking and highlights", order = 20, markdown = """
        `bounds()` covers the node's own content. Subclasses can supply local geometry bounds through
        `contentBounds()` and implement cell hits through `intersectPickBounds`. A returned `HoverHighlight(mesh,
        material)` is depth-tested. Place its geometry relative to the node origin for object hits, or to the
        selected cell's lower corner for cell hits.
        """)
@WikiExample(title = "Supply custom object hover geometry", code = """
        SceneNode node = new SceneNode() {
            private final HoverHighlight highlight = new HoverHighlight(outlineMesh, outlineMaterial);
        
            @Override
            public HoverHighlight hoverHighlight(Vec3View cellOrigin) {
                return highlight;
            }
        };
        node.setMesh(objectMesh).setMaterial(objectMaterial);
        viewport.highlightObjects(true).scene().addRoot(node);
        """)
@WikiLink(label = "Scene", target = "page:scene")
@WikiLink(label = "Mesh3D", target = "page:mesh-3d")
@WikiLink(label = "Material", target = "page:scene-material")
@WikiLink(label = "ScenePicker", target = "page:scene-picker")
@WikiLink(label = "BlockNode", target = "page:block-node")
@WikiLink(label = "ItemNode", target = "page:item-node")
@WikiLink(label = "EntityNode", target = "page:entity-node")
@WikiLink(label = "StructureNode", target = "page:structure-node")
@AiDoc(
        summary = "Transformable scene graph node with optional mesh, material and hover geometry.",
        purpose = "Group content, attach custom geometry or extend a node with specialized bounds and hover highlights.",
        criticalRules = {
                "Position is relative to the parent and locates the pivot. `setOrigin` takes node-local coordinates.",
                "A mesh is immutable and shareable. The AABB covers this node's own content.",
                "Picking tests indexed triangles when a mesh is present. `setBounds` overrides local bounds for broad-phase picking and culling.",
                "Override `hoverHighlight(Vec3View)` for custom geometry. The argument is a borrowed cell origin or null for an object hit. Return null for the viewport default.",
                "Call `invalidateContent()` after external changes such as an updated dynamic texture or custom highlight."
        },
        related = {"Scene", "Mesh3D", "Material", "ScenePicker", "BlockNode", "ItemNode", "EntityNode", "StructureNode"},
        examples = @AiDoc.Example(title = "Supply custom object hover geometry", code = """
                SceneNode node = new SceneNode() {
                    private final HoverHighlight highlight = new HoverHighlight(outlineMesh, outlineMaterial);
                
                    @Override
                    public HoverHighlight hoverHighlight(Vec3View cellOrigin) {
                        return highlight;
                    }
                };
                node.setMesh(objectMesh).setMaterial(objectMaterial);
                viewport.highlightObjects(true).scene().addRoot(node);
                """))
public class SceneNode {
    /**
     * Scene owner of the internal root, or null for ordinary nodes.
     */
    private final Scene rootScene;
    /**
     * Local position in scene units.
     */
    private final MutableVec3 position = new MutableVec3();
    /**
     * Unit local rotation quaternion in XYZW order.
     */
    private final MutableQuat rotation = new MutableQuat();
    /**
     * Dimensionless scale factors along the local axes.
     */
    private final MutableVec3 scale = new MutableVec3(1, 1, 1);
    /**
     * Local pivot around which the node rotates and scales. {@link #position} places this point.
     */
    private final MutableVec3 origin = new MutableVec3();
    /**
     * Cached local matrix.
     */
    private final MutableMat4 local = new MutableMat4();
    /**
     * Cached transform from local space to world space.
     */
    private final MutableMat4 world = new MutableMat4();

    /**
     * Children in insertion order, allocated on first insertion.
     */
    private ArrayList<SceneNode> children;
    /**
     * Live read-only view of the children, allocated on first access.
     */
    private List<SceneNode> childrenView;
    /**
     * Parent, internal scene root, or null for a detached node.
     */
    private SceneNode parent;
    /**
     * Index in the parent's child list when attached.
     */
    private int siblingIndex;
    /**
     * Whether the local transform needs recomputing after a pose change.
     */
    private boolean localDirty;
    /**
     * Whether the world transform needs recomputing after a pose or parent change.
     */
    private boolean worldDirty;
    /**
     * Visibility flag for this node and its subtree, subject to ancestor visibility.
     */
    private boolean visible = true;
    /**
     * Mesh in node-local space, or null.
     */
    private Mesh3D mesh;
    /**
     * Surface settings for the mesh.
     */
    private Material material = Material.DEFAULT;
    /**
     * Base GLSL function for this node's indexed surfaces on Cleanroom 1.12.2 and Minecraft 1.20.1, 1.21.1 and 26.3, or null.
     */
    private SceneMaterialEffect materialEffect;
    /**
     * Scheduled override for the base effect, or null.
     */
    private SceneMaterialEffect timelineMaterialEffect;
    /**
     * Shader time in local timeline seconds while {@link #timelineMaterialEffect} is set.
     */
    private float timelineMaterialTime;
    /**
     * Whether a completed material clip hides the node and its subtree.
     */
    private boolean hiddenByMaterialEffect;
    /**
     * Whether opaque and cutout indexed surfaces of this node enter the shadow map.
     */
    private boolean castsShadow = true;
    /**
     * Whether indexed surfaces of this node sample the scene's shadow map.
     */
    private boolean receivesShadow = true;
    /**
     * Explicit content bounds, or null to derive bounds from the content.
     */
    private Aabb boundsOverride;

    /**
     * Creates a detached, visible node with an identity transform.
     */
    @WikiMethod(section = "Hierarchy", sectionOrder = 10, order = 10,
            summary = "Creates a detached, visible node with an identity transform.")
    @AiDoc(section = "Hierarchy", sectionOrder = 10, order = 10,
            summary = "Creates a detached, visible node with an identity transform.")
    public SceneNode() {
        this(null);
    }

    /**
     * Creates a node with optional scene ownership.
     *
     * @param rootScene owning scene for the internal root, or null for an ordinary node.
     */
    SceneNode(Scene rootScene) {
        this.rootScene = rootScene;
    }

    /**
     * Reads the parent.
     *
     * @return the parent node, or null for scene roots and detached nodes.
     */
    @WikiMethod(section = "Hierarchy", sectionOrder = 10, order = 20,
            summary = "Reads the parent.")
    @WikiReturn("the parent node, or null for scene roots and detached nodes")
    @AiDoc(section = "Hierarchy", sectionOrder = 10, order = 20,
            summary = "Reads the parent.")
    public final SceneNode parent() {
        return parent == null || parent.rootScene != null ? null : parent;
    }

    /**
     * Finds the scene through the hierarchy root.
     *
     * @return scene, or null for a detached hierarchy.
     */
    @WikiMethod(section = "Hierarchy", sectionOrder = 10, order = 30,
            summary = "Finds the scene through the hierarchy root.")
    @WikiReturn("scene, or null for a detached hierarchy")
    @AiDoc(section = "Hierarchy", sectionOrder = 10, order = 30,
            summary = "Finds the scene through the hierarchy root.")
    public final Scene scene() {
        SceneNode root = this;
        while (root.parent != null) root = root.parent;
        return root.rootScene;
    }

    /**
     * Reads the children in insertion order.
     *
     * @return a live, read-only view.
     */
    @WikiMethod(section = "Hierarchy", sectionOrder = 10, order = 40,
            summary = "Reads the children in insertion order.")
    @WikiReturn("a live, read-only view")
    @AiDoc(section = "Hierarchy", sectionOrder = 10, order = 40,
            summary = "Reads the children in insertion order.")
    public final List<SceneNode> children() {
        if (childrenView == null) {
            if (children == null) children = new ArrayList<>();
            childrenView = Collections.unmodifiableList(children);
        }
        return childrenView;
    }

    /**
     * Adds a child, detaching it from the previous hierarchy as needed. The child keeps its local transform.
     *
     * @param child node to move.
     * @return this node.
     * @throws NullPointerException     if child is null.
     * @throws IllegalArgumentException if adding the child would create a cycle.
     */
    @WikiMethod(section = "Hierarchy", sectionOrder = 10, order = 50,
            summary = "Adds a child, detaching it from the previous hierarchy as needed. The child keeps its local transform.")
    @WikiReturn("this node")
    @WikiThrows(type = NullPointerException.class, description = "if child is null.")
    @WikiThrows(type = IllegalArgumentException.class, description = "if adding the child would create a cycle.")
    @AiDoc(section = "Hierarchy", sectionOrder = 10, order = 50,
            summary = "Adds a child, detaching it from the previous hierarchy as needed. The child keeps its local transform.")
    public final SceneNode addChild(
            @WikiParameter(description = "node to move.") SceneNode child) {
        child.attachTo(this);
        return this;
    }

    /**
     * Detaches a direct child with its subtree.
     *
     * @param child node to remove, or null.
     * @return true if this node was the parent of child.
     */
    @WikiMethod(section = "Hierarchy", sectionOrder = 10, order = 60,
            summary = "Detaches a direct child with its subtree.")
    @WikiReturn("true if this node was the parent of child")
    @AiDoc(section = "Hierarchy", sectionOrder = 10, order = 60,
            summary = "Detaches a direct child with its subtree.")
    public final boolean removeChild(
            @WikiParameter(description = "node to remove, or null.") SceneNode child) {
        if (child == null || child.parent != this) return false;
        child.detach();
        return true;
    }

    /**
     * Detaches this node and its subtree from its parent or scene.
     *
     * @return this node.
     */
    @WikiMethod(section = "Hierarchy", sectionOrder = 10, order = 70,
            summary = "Detaches this node and its subtree from its parent or scene.")
    @WikiReturn("this node")
    @AiDoc(section = "Hierarchy", sectionOrder = 10, order = 70,
            summary = "Detaches this node and its subtree from its parent or scene.")
    public final SceneNode detach() {
        attachTo(null);
        return this;
    }

    /**
     * Copies the local position.
     *
     * @param out reusable vector in scene units.
     * @return out.
     */
    @WikiMethod(section = "Transforms and visibility", sectionOrder = 20, order = 10,
            summary = "Copies the local position.")
    @WikiReturn("out")
    @AiDoc(section = "Transforms and visibility", sectionOrder = 20, order = 10,
            summary = "Copies the local position.")
    public final MutableVec3 position(
            @WikiParameter(description = "reusable vector in scene units.") MutableVec3 out) {
        return out.set(position);
    }

    /**
     * Sets the position relative to the parent.
     *
     * @param x finite X coordinate in scene units.
     * @param y finite Y coordinate in scene units.
     * @param z finite Z coordinate in scene units.
     * @return this node.
     * @throws IllegalArgumentException if a coordinate is nonfinite.
     */
    @WikiMethod(section = "Transforms and visibility", sectionOrder = 20, order = 20,
            summary = "Sets the position relative to the parent.")
    @WikiReturn("this node")
    @WikiThrows(type = IllegalArgumentException.class, description = "if a coordinate is nonfinite.")
    @AiDoc(section = "Transforms and visibility", sectionOrder = 20, order = 20,
            summary = "Sets the position relative to the parent.")
    public SceneNode setPosition(
            @WikiParameter(description = "finite X coordinate in scene units.") float x,
            @WikiParameter(description = "finite Y coordinate in scene units.") float y,
            @WikiParameter(description = "finite Z coordinate in scene units.") float z) {
        requireFinite(x, y, z);
        if (position.x() != x || position.y() != y || position.z() != z) {
            position.set(x, y, z);
            transformChanged();
        }
        return this;
    }

    /**
     * Copies the local rotation.
     *
     * @param out reusable quaternion.
     * @return out.
     */
    @WikiMethod(section = "Transforms and visibility", sectionOrder = 20, order = 30,
            summary = "Copies the local rotation.")
    @WikiReturn("out")
    @AiDoc(section = "Transforms and visibility", sectionOrder = 20, order = 30,
            summary = "Copies the local rotation.")
    public final MutableQuat rotation(
            @WikiParameter(description = "reusable quaternion.") MutableQuat out) {
        return out.set(rotation);
    }

    /**
     * Sets the local rotation.
     *
     * @param value unit quaternion representing an active rotation.
     * @return this node.
     */
    @WikiMethod(section = "Transforms and visibility", sectionOrder = 20, order = 40,
            summary = "Sets the local rotation.")
    @WikiReturn("this node")
    @AiDoc(section = "Transforms and visibility", sectionOrder = 20, order = 40,
            summary = "Sets the local rotation.")
    public SceneNode setRotation(
            @WikiParameter(description = "unit quaternion representing an active rotation.") QuatView value) {
        if (rotation.x() != value.x() || rotation.y() != value.y()
                || rotation.z() != value.z() || rotation.w() != value.w()) {
            rotation.set(value);
            transformChanged();
        }
        return this;
    }

    /**
     * Copies the local scale factors.
     *
     * @param out reusable vector of dimensionless factors.
     * @return out.
     */
    @WikiMethod(section = "Transforms and visibility", sectionOrder = 20, order = 50,
            summary = "Copies the local scale factors.")
    @WikiReturn("out")
    @AiDoc(section = "Transforms and visibility", sectionOrder = 20, order = 50,
            summary = "Copies the local scale factors.")
    public final MutableVec3 scale(
            @WikiParameter(description = "reusable vector of dimensionless factors.") MutableVec3 out) {
        return out.set(scale);
    }

    /**
     * Sets the scale along the local axes. Zero and negative factors are allowed.
     *
     * @param x finite dimensionless factor along X.
     * @param y finite dimensionless factor along Y.
     * @param z finite dimensionless factor along Z.
     * @return this node.
     * @throws IllegalArgumentException if a factor is nonfinite.
     */
    @WikiMethod(section = "Transforms and visibility", sectionOrder = 20, order = 60,
            summary = "Sets the scale along the local axes. Zero and negative factors are allowed.")
    @WikiReturn("this node")
    @WikiThrows(type = IllegalArgumentException.class, description = "if a factor is nonfinite.")
    @AiDoc(section = "Transforms and visibility", sectionOrder = 20, order = 60,
            summary = "Sets the scale along the local axes. Zero and negative factors are allowed.")
    public SceneNode setScale(
            @WikiParameter(description = "finite dimensionless factor along X.") float x,
            @WikiParameter(description = "finite dimensionless factor along Y.") float y,
            @WikiParameter(description = "finite dimensionless factor along Z.") float z) {
        requireFinite(x, y, z);
        if (scale.x() != x || scale.y() != y || scale.z() != z) {
            scale.set(x, y, z);
            transformChanged();
        }
        return this;
    }

    /**
     * Copies the pivot.
     *
     * @param out reusable vector in node-local units.
     * @return out.
     */
    @WikiMethod(section = "Transforms and visibility", sectionOrder = 20, order = 70,
            summary = "Copies the pivot.")
    @WikiReturn("out")
    @AiDoc(section = "Transforms and visibility", sectionOrder = 20, order = 70,
            summary = "Copies the pivot.")
    public final MutableVec3 origin(
            @WikiParameter(description = "reusable vector in node-local units.") MutableVec3 out) {
        return out.set(origin);
    }

    /**
     * Sets the local pivot. The node rotates and scales around it, and {@link #setPosition} places it in
     * parent space. For example, {@code setOrigin(0.5f, 0, 0.5f)} pivots a unit block around its base center.
     *
     * @param x local X coordinate.
     * @param y local Y coordinate.
     * @param z local Z coordinate.
     * @return this node.
     * @throws IllegalArgumentException if a coordinate is nonfinite.
     */
    @WikiMethod(section = "Transforms and visibility", sectionOrder = 20, order = 80,
            summary = "Sets the local pivot. The node rotates and scales around it, and `setPosition` places it in parent space. For example, `setOrigin(0.5f, 0, 0.5f)` pivots a unit block around its base center.")
    @WikiReturn("this node")
    @WikiThrows(type = IllegalArgumentException.class, description = "if a coordinate is nonfinite.")
    @AiDoc(section = "Transforms and visibility", sectionOrder = 20, order = 80,
            summary = "Sets the local pivot. The node rotates and scales around it, and `setPosition` places it in parent space. For example, `setOrigin(0.5f, 0, 0.5f)` pivots a unit block around its base center.")
    public SceneNode setOrigin(
            @WikiParameter(description = "local X coordinate.") float x,
            @WikiParameter(description = "local Y coordinate.") float y,
            @WikiParameter(description = "local Z coordinate.") float z) {
        requireFinite(x, y, z);
        if (origin.x() != x || origin.y() != y || origin.z() != z) {
            origin.set(x, y, z);
            transformChanged();
        }
        return this;
    }

    /**
     * Sets the pivot to the center of a unit block's base at {@code (0.5, 0, 0.5)}.
     *
     * @return this node.
     */
    @WikiMethod(section = "Transforms and visibility", sectionOrder = 20, order = 90,
            summary = "Sets the pivot to the center of a unit block's base at `(0.5, 0, 0.5)`.")
    @WikiReturn("this node")
    @AiDoc(section = "Transforms and visibility", sectionOrder = 20, order = 90,
            summary = "Sets the pivot to the center of a unit block's base at `(0.5, 0, 0.5)`.")
    public SceneNode centerOrigin() {
        return setOrigin(0.5f, 0, 0.5f);
    }

    /**
     * Copies the matrix {@code T(position) * R * S * T(-origin)}, where R rotates, S scales and T translates.
     *
     * @param out reusable matrix.
     * @return out with the transform into parent space.
     */
    @WikiMethod(section = "Transforms and visibility", sectionOrder = 20, order = 100,
            summary = "Copies the matrix `T(position) * R * S * T(-origin)`, where R rotates, S scales and T translates.")
    @WikiReturn("out with the transform into parent space")
    @AiDoc(section = "Transforms and visibility", sectionOrder = 20, order = 100,
            summary = "Copies the matrix `T(position) * R * S * T(-origin)`, where R rotates, S scales and T translates.")
    public final MutableMat4 localMatrix(
            @WikiParameter(description = "reusable matrix.") MutableMat4 out) {
        return out.set(resolvedLocal());
    }

    /**
     * Copies the matrix {@code parentWorld * local}. For a node without a parent the world matrix is its local matrix.
     *
     * @param out reusable matrix.
     * @return out with the transform into scene space.
     */
    @WikiMethod(section = "Transforms and visibility", sectionOrder = 20, order = 110,
            summary = "Copies the matrix `parentWorld * local`. For a node without a parent the world matrix is its local matrix.")
    @WikiReturn("out with the transform into scene space")
    @AiDoc(section = "Transforms and visibility", sectionOrder = 20, order = 110,
            summary = "Copies the matrix `parentWorld * local`. For a node without a parent the world matrix is its local matrix.")
    public final MutableMat4 worldMatrix(
            @WikiParameter(description = "reusable matrix.") MutableMat4 out) {
        return out.set(resolvedWorld());
    }

    /**
     * Reads the node's visibility after any scheduled material effect has been applied.
     *
     * @return true if this node allows its subtree to be visited.
     */
    @WikiMethod(section = "Transforms and visibility", sectionOrder = 20, order = 120,
            summary = "Reads this node's own visibility flag.")
    @WikiReturn("true if this node allows its subtree to be visited")
    @AiDoc(section = "Transforms and visibility", sectionOrder = 20, order = 120,
            summary = "Reads this node's own visibility flag.")
    public final boolean visible() {
        return visible && !hiddenByMaterialEffect;
    }

    /**
     * Sets visibility for the entire subtree during scene traversal.
     *
     * @param visible true to show the subtree, false to hide it.
     * @return this node.
     */
    @WikiMethod(section = "Transforms and visibility", sectionOrder = 20, order = 130,
            summary = "Sets visibility for the entire subtree during scene traversal.")
    @WikiReturn("this node")
    @AiDoc(section = "Transforms and visibility", sectionOrder = 20, order = 130,
            summary = "Sets visibility for the entire subtree during scene traversal.")
    public SceneNode setVisible(
            @WikiParameter(description = "true to show the subtree, false to hide it.") boolean visible) {
        if (this.visible != visible) {
            this.visible = visible;
            invalidateContent();
        }
        return this;
    }

    /**
     * Reads the mesh.
     *
     * @return immutable geometry, or null.
     */
    @WikiMethod(section = "<key:scene.api.section.geometry>", sectionOrder = 30, order = 10,
            summary = "Reads the mesh.")
    @WikiReturn("immutable geometry, or null")
    @AiDoc(section = "Geometry and picking", sectionOrder = 30, order = 10,
            summary = "Reads the mesh.")
    public final Mesh3D mesh() {
        return mesh;
    }

    /**
     * Replaces this node's geometry.
     *
     * @param mesh geometry in node coordinates, or null to remove it.
     * @return this node.
     */
    @WikiMethod(section = "<key:scene.api.section.geometry>", sectionOrder = 30, order = 20,
            summary = "Replaces this node's geometry.")
    @WikiReturn("this node")
    @AiDoc(section = "Geometry and picking", sectionOrder = 30, order = 20,
            summary = "Replaces this node's geometry.")
    public final SceneNode setMesh(
            @WikiParameter(description = "geometry in node coordinates, or null to remove it.") Mesh3D mesh) {
        if (this.mesh != mesh) {
            this.mesh = mesh;
            invalidateContent();
        }
        return this;
    }

    /**
     * Reads the surface settings.
     *
     * @return the mesh material.
     */
    @WikiMethod(section = "<key:scene.api.section.geometry>", sectionOrder = 30, order = 30,
            summary = "Reads the surface settings.")
    @WikiReturn("the mesh material")
    @AiDoc(section = "Geometry and picking", sectionOrder = 30, order = 30,
            summary = "Reads the surface settings.")
    public final Material material() {
        return material;
    }

    /**
     * @return the current surface effect, including any scheduled override, or null for the default shader
     */
    public final SceneMaterialEffect materialEffect() {
        return timelineMaterialEffect != null ? timelineMaterialEffect : materialEffect;
    }

    /**
     * Resolves the clock used by the current surface effect in both color and shadow passes.
     *
     * @param sceneSeconds fallback time in seconds supplied by the scene
     * @return local shader time from a scheduled effect, or {@code sceneSeconds} for the base effect
     */
    public final float materialEffectTime(float sceneSeconds) {
        return timelineMaterialEffect != null ? timelineMaterialTime : sceneSeconds;
    }

    /**
     * Assigns a base surface shader to this node's mesh, baked blocks and supported native models on Cleanroom 1.12.2 and Minecraft 1.20.1, 1.21.1 and 26.3.
     *
     * @param effect effect and parameters, or null for the default shader
     * @return this node
     */
    @WikiMethod(section = "<key:scene.api.section.geometry>", sectionOrder = 30, summary = "<key:scene.node.api.set.material.effect.summary>")
    @WikiReturn("<key:scene.api.return.node>")
    @AiDoc(section = "Geometry and picking", summary = "Assigns a GLSL surface effect to meshes, baked blocks and supported item and entity models on Cleanroom 1.12.2 and Minecraft 1.20.1, 1.21.1 and 26.3.")
    public final SceneNode setMaterialEffect(
            @WikiParameter(description = "<key:scene.node.api.set.material.effect.param.effect>") SceneMaterialEffect effect) {
        if (!Objects.equals(materialEffect, effect)) {
            materialEffect = effect;
            invalidateContent();
        }
        return this;
    }

    /**
     * Applies a scheduled surface and visibility override while preserving the base properties.
     *
     * @param effect  timed surface effect, or null to release the surface override
     * @param seconds finite local timeline time used by the shader, or zero if effect is null
     * @param hide    true to suppress this node and its subtree until another sample releases it
     */
    final void setTimelineMaterialEffect(SceneMaterialEffect effect, float seconds, boolean hide) {
        if (!Objects.equals(timelineMaterialEffect, effect) || timelineMaterialTime != seconds
                || hiddenByMaterialEffect != hide) {
            timelineMaterialEffect = effect;
            timelineMaterialTime = seconds;
            hiddenByMaterialEffect = hide;
            invalidateContent();
        }
    }

    /**
     * @return whether this node's indexed opaque or cutout surfaces cast shadows
     */
    public final boolean castsShadow() {
        return castsShadow;
    }

    /**
     * Controls shadow casting for this node's indexed surfaces. Descendants own their own settings.
     *
     * @param enabled true to include opaque and cutout surfaces in the shadow pass
     * @return this node
     */
    @WikiMethod(section = "<key:scene.api.section.lighting>", sectionOrder = 40, summary = "<key:scene.node.api.set.cast.shadow.summary>")
    @WikiReturn("<key:scene.api.return.node>")
    @AiDoc(section = "Lighting", summary = "Controls shadow casting by the node's indexed opaque and cutout surfaces.")
    public final SceneNode setCastShadow(
            @WikiParameter(description = "<key:scene.node.api.set.cast.shadow.param.enabled>") boolean enabled) {
        if (castsShadow != enabled) {
            castsShadow = enabled;
            invalidateContent();
        }
        return this;
    }

    /**
     * @return whether this node's indexed surfaces receive shadows
     */
    public final boolean receivesShadow() {
        return receivesShadow;
    }

    /**
     * Controls shadow reception for this node's indexed surfaces. Descendants own their own settings.
     *
     * @param enabled true to apply directional shadows to this node
     * @return this node
     */
    @WikiMethod(section = "<key:scene.api.section.lighting>", sectionOrder = 40, summary = "<key:scene.node.api.set.receive.shadow.summary>")
    @WikiReturn("<key:scene.api.return.node>")
    @AiDoc(section = "Lighting", summary = "Controls directional shadow reception by the node's indexed surfaces.")
    public final SceneNode setReceiveShadow(
            @WikiParameter(description = "<key:scene.node.api.set.receive.shadow.param.enabled>") boolean enabled) {
        if (receivesShadow != enabled) {
            receivesShadow = enabled;
            invalidateContent();
        }
        return this;
    }

    /**
     * Replaces the surface settings.
     *
     * @param material new mesh material.
     * @return this node.
     * @throws NullPointerException if material is null.
     */
    @WikiMethod(section = "<key:scene.api.section.geometry>", sectionOrder = 30, order = 40,
            summary = "Replaces the surface settings.")
    @WikiReturn("this node")
    @WikiThrows(type = NullPointerException.class, description = "if material is null.")
    @AiDoc(section = "Geometry and picking", sectionOrder = 30, order = 40,
            summary = "Replaces the surface settings.")
    public final SceneNode setMaterial(
            @WikiParameter(description = "new mesh material.") Material material) {
        Objects.requireNonNull(material, "material");
        if (this.material != material) {
            this.material = material;
            invalidateContent();
        }
        return this;
    }

    /**
     * Reads the local bounds of this node's own content.
     *
     * @return the explicit AABB, derived content bounds, or null when neither is available.
     */
    @WikiMethod(section = "<key:scene.api.section.geometry>", sectionOrder = 30, order = 50,
            summary = "Reads the local bounds of this node's own content.")
    @WikiReturn("the explicit AABB, derived content bounds, or null when neither is available")
    @AiDoc(section = "Geometry and picking", sectionOrder = 30, order = 50,
            summary = "Reads the local bounds of this node's own content.")
    public final Aabb bounds() {
        if (boundsOverride != null) return boundsOverride;
        return contentBounds();
    }

    /**
     * Reads the local bounds of this node's own geometry when there is no explicit AABB.
     *
     * @return local content bounds, or null.
     */
    protected Aabb contentBounds() {
        return mesh == null ? null : mesh.bounds();
    }

    /**
     * Tests content represented by bounds or unit cells.
     *
     * @param ray        node-local ray.
     * @param entry      ray parameter at entry into the node bounds.
     * @param limit      maximum ray parameter for the search.
     * @param cellOrigin output for the lower cell corner. Leave it as NaN for an ordinary object hit.
     * @return nearest hit parameter, or positive infinity for a miss. Mesh triangles are tested separately.
     */
    protected double intersectPickBounds(Ray ray, double entry, double limit, MutableVec3 cellOrigin) {
        return mesh == null ? entry : Double.POSITIVE_INFINITY;
    }

    /**
     * Supplies a custom hover highlight. The viewport calls this on the UI thread when highlighting for
     * the hit type is enabled. Reuse the result if possible and call {@link #invalidateContent()} after
     * external highlight data changes to refresh it under a stationary pointer.
     *
     * @param cellOrigin lower corner of the hit cell in node coordinates, or null for an object hit.
     *                   The view is valid only during the call.
     * @return custom geometry and material, or null for the default AABB frame or cell highlight.
     */
    @WikiMethod(section = "<key:scene.api.section.geometry>", sectionOrder = 30, order = 70,
            summary = "Supplies a custom hover highlight. The viewport calls this on the UI thread when highlighting for the hit type is enabled. Reuse the result if possible and call `invalidateContent()` after external highlight data changes to refresh it under a stationary pointer.")
    @WikiReturn("custom geometry and material, or null for the default AABB frame or cell highlight")
    @AiDoc(section = "Geometry and picking", sectionOrder = 30, order = 70,
            summary = "Supplies a custom hover highlight. The viewport calls this on the UI thread when highlighting for the hit type is enabled. Reuse the result if possible and call `invalidateContent()` after external highlight data changes to refresh it under a stationary pointer.")
    public HoverHighlight hoverHighlight(
            @WikiParameter(description =
                    "lower corner of the hit cell in node coordinates, or null for an object hit. The view is valid only during the call.")
            Vec3View cellOrigin) {
        return null;
    }

    /**
     * Geometry and material rendered against the scene depth buffer.
     *
     * @param mesh     geometry in node-local axes, relative to the hit cell corner or the node origin.
     * @param material highlight material.
     */
    public record HoverHighlight(Mesh3D mesh, Material material) {
        /**
         * Validates the mesh and material.
         *
         * @param mesh     geometry in local scene units.
         * @param material highlight material.
         * @throws NullPointerException if mesh or material is null.
         */
        public HoverHighlight {
            Objects.requireNonNull(mesh, "mesh");
            Objects.requireNonNull(material, "material");
        }
    }

    /**
     * Overrides the bounds used for culling and picking this node's content.
     *
     * @param bounds local AABB, or null to use derived content bounds.
     * @return this node.
     */
    @WikiMethod(section = "<key:scene.api.section.geometry>", sectionOrder = 30, order = 60,
            summary = "Overrides the bounds used for culling and picking this node's content.")
    @WikiReturn("this node")
    @AiDoc(section = "Geometry and picking", sectionOrder = 30, order = 60,
            summary = "Overrides the bounds used for culling and picking this node's content.")
    public final SceneNode setBounds(
            @WikiParameter(description = "local AABB, or null to use derived content bounds.") Aabb bounds) {
        if (!Objects.equals(boundsOverride, bounds)) {
            boundsOverride = bounds;
            invalidateContent();
        }
        return this;
    }

    /**
     * Marks external node content as changed, for example after editing a texture or subclass data.
     */
    @WikiMethod(section = "<key:scene.api.section.geometry>", sectionOrder = 30, order = 80,
            summary = "Marks external node content as changed, for example after editing a texture or subclass data.")
    @AiDoc(section = "Geometry and picking", sectionOrder = 30, order = 80,
            summary = "Marks external node content as changed, for example after editing a texture or subclass data.")
    public final void invalidateContent() {
        Scene scene = scene();
        if (scene != null) scene.changed();
    }

    /**
     * Reparents the subtree and keeps its local coordinates. Updates both scenes if they differ.
     *
     * @param nextParent new parent, internal scene root, or null to detach.
     * @throws IllegalArgumentException if nextParent belongs to this subtree.
     */
    final void attachTo(SceneNode nextParent) {
        if (parent == nextParent) return;
        for (SceneNode node = nextParent; node != null; node = node.parent) {
            if (node == this) throw new IllegalArgumentException("Scene hierarchy cannot contain a cycle");
        }
        Scene oldScene = scene();
        Scene newScene = nextParent == null ? null : nextParent.scene();
        int nextIndex = 0;
        if (nextParent != null) {
            if (nextParent.children == null) nextParent.children = new ArrayList<>();
            nextIndex = nextParent.children.size();
            nextParent.children.add(this);
        }
        if (parent != null) parent.unlinkChild(siblingIndex);
        parent = nextParent;
        siblingIndex = nextIndex;
        invalidateWorld();
        if (oldScene != null) oldScene.changed();
        if (newScene != null && newScene != oldScene) newScene.changed();
    }

    /**
     * Finds the next node in a nonrecursive preorder traversal.
     *
     * @param root    traversal boundary, this node or an ancestor.
     * @param descend whether to visit the current node's children first.
     * @return next node, or null at the end of the subtree.
     */
    final SceneNode nextInSubtree(SceneNode root, boolean descend) {
        if (descend && children != null && !children.isEmpty()) return children.get(0);
        for (SceneNode node = this; node != root; node = node.parent) {
            int next = node.siblingIndex + 1;
            List<SceneNode> siblings = node.parent.children;
            if (next < siblings.size()) return siblings.get(next);
        }
        return null;
    }

    /**
     * Tests whether this node's transformed content may be visible.
     *
     * @param frustum camera view volume.
     * @return true for unbounded or potentially visible content.
     */
    final boolean intersects(Frustum3D frustum) {
        MutableMat4 matrix = resolvedWorld();
        Aabb bounds = bounds();
        return bounds == null || frustum.intersects(bounds, matrix);
    }

    /**
     * Removes a child and updates the indices of later children.
     *
     * @param index position of an existing child.
     */
    private void unlinkChild(int index) {
        children.remove(index);
        for (int i = index; i < children.size(); i++) {
            children.get(i).siblingIndex = i;
        }
    }

    /**
     * Marks the local transform and dependent world transforms dirty.
     */
    private void transformChanged() {
        localDirty = true;
        invalidateWorld();
        invalidateContent();
    }

    /**
     * Marks world transforms in the subtree dirty without recursion.
     */
    private void invalidateWorld() {
        for (SceneNode node = this; node != null; node = node.nextInSubtree(this, true)) {
            node.worldDirty = true;
        }
    }

    /**
     * Recomputes the local matrix on demand after a pose change.
     *
     * @return the internal local matrix cache.
     */
    private MutableMat4 resolvedLocal() {
        if (localDirty) {
            local.setTransform(position, rotation, scale);
            if (origin.x() != 0 || origin.y() != 0 || origin.z() != 0) {
                // For T(p) * R * S * T(-o), the translation is p - (R * S) * o.
                for (int row = 0; row < 3; row++) {
                    double shifted = local.get(row, 3) - local.get(row, 0) * origin.x()
                            - local.get(row, 1) * origin.y() - local.get(row, 2) * origin.z();
                    local.setTranslationComponent(row, shifted);
                }
            }
            localDirty = false;
        }
        return local;
    }

    /**
     * Walks to the nearest ancestor with a current world matrix, multiplying local matrices on the left.
     *
     * @return the internal world matrix cache.
     */
    private MutableMat4 resolvedWorld() {
        if (!worldDirty) return world;
        world.set(resolvedLocal());
        for (SceneNode node = parent; node != null && node.rootScene == null; node = node.parent) {
            if (!node.worldDirty) {
                world.setProduct(node.world, world);
                break;
            }
            world.setProduct(node.resolvedLocal(), world);
        }
        worldDirty = false;
        return world;
    }

    /**
     * Validates translation or scale components.
     *
     * @param x X component.
     * @param y Y component.
     * @param z Z component.
     * @throws IllegalArgumentException if a component is nonfinite.
     */
    private static void requireFinite(float x, float y, float z) {
        if (!Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(z)) {
            throw new IllegalArgumentException("Transform components must be finite");
        }
    }
}
