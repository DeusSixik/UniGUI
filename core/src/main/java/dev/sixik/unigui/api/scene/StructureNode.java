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
import dev.sixik.unigui.api.animation.AnimationEasing;
import dev.sixik.unigui.api.animation.TransitionSpec;
import dev.sixik.unigui.api.math.Aabb;
import dev.sixik.unigui.api.math.MutableMat4;
import dev.sixik.unigui.api.math.MutableVec3;
import dev.sixik.unigui.api.math.Ray;
import dev.sixik.unigui.api.math.Vec3View;
import dev.sixik.unigui.widgets.rendering.Scene3D;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Objects;

/**
 * A sparse Minecraft block structure with one scene unit per block and integer positions relative to the node origin.
 *
 * <p>The structure resolves input to native states as blocks are written. The platform is chosen at the first
 * write unless passed to the constructor. The platform loads structure resources, and its renderer bakes sections.</p>
 * <p>Blocks are grouped into 16×16×16 sections. Each section stores a dense array of {@value Section#VOLUME}
 * cells and a revision for the renderer's geometry cache.</p>
 * <pre>{@code
 * StructureNode tower = new StructureNode();
 * for (int y = 0; y < 4; y++) tower.setBlock(0, y, 0, "minecraft:stone_bricks");
 * tower.setBlock(0, 4, 0, "minecraft:lantern[hanging=false]");
 * }</pre>
 */
@WikiPage(
        title = "StructureNode", slug = "structure-node",
        category = "API/3D Scene", order = 160,
        summary = "Sparse block structure with layer slicing, assembly and optional distant geometry.")
@WikiSection(title = "Blocks and resources", order = 10, markdown = """
        Populate cells with `setBlock` and remove cells with `removeBlock`. A platform that supports structure
        templates can load them by ID using `ScenePlatform.loadStructure`. 1.7.10 supports building the structure in
        code.
        """)
@WikiSection(title = "Layer views and distant meshes", order = 20, markdown = """
        Layer slicing affects rendering and picking. `block()` still reads hidden cells and `visibleBlock()` applies
        the cutoff. A distant mesh can replace block rendering outside a configured distance from the world-space
        block AABB. Keep it within the structure's local bounds and update it after edits.
        """)
@WikiExample(title = "Assemble a three-layer column", code = """
        StructureNode column = new StructureNode();
        for (int y = 0; y < 3; y++) {
            column.setBlock(0, y, 0, "minecraft:stone");
        }
        viewport.scene().addRoot(column);
        column.playAssembly(viewport, 2, Integer.MAX_VALUE);
        """)
@WikiLink(label = "BlockNode", target = "page:block-node")
@WikiLink(label = "BlockSpec", target = "page:block-spec")
@WikiLink(label = "SceneNode", target = "page:scene-node")
@WikiLink(label = "ScenePlatform", target = "page:scene-platform")
@WikiLink(label = "SceneTimeline", target = "page:scene-timeline")
@WikiLink(label = "Scene3D", target = "page:scene-3d")
@AiDoc(
        summary = "Sparse block structure with layer slicing, assembly and optional distant geometry.",
        purpose = "Build and animate groups of Minecraft blocks as a single transformable node.",
        criticalRules = {
                "Use integer coordinates local to the structure. Writes resolve native block states immediately and require an installed or explicit platform.",
                "Set `topVisibleY` to the highest visible local layer, inclusive. `Integer.MAX_VALUE` reveals all layers.",
                "Attach the node to the viewport's scene before calling `playAssembly`. For seeking use `SceneTimeline.assemble` and `sample`.",
                "Distant meshes render in a perspective view with the full structure visible. Picking still uses the source blocks."
        },
        related = {"BlockNode", "BlockSpec", "SceneNode", "ScenePlatform", "SceneTimeline", "Scene3D"},
        examples = @AiDoc.Example(title = "Assemble a three-layer column", code = """
                StructureNode column = new StructureNode();
                for (int y = 0; y < 3; y++) {
                    column.setBlock(0, y, 0, "minecraft:stone");
                }
                viewport.scene().addRoot(column);
                column.playAssembly(viewport, 2, Integer.MAX_VALUE);
                """))
public final class StructureNode extends SceneNode {
    /**
     * Minimum block X or Z supported by Minecraft's packed positions.
     */
    public static final int MIN_HORIZONTAL = -(1 << 25);
    /**
     * Maximum block X or Z.
     */
    public static final int MAX_HORIZONTAL = (1 << 25) - 1;
    /**
     * Minimum block Y.
     */
    public static final int MIN_Y = -2048;
    /**
     * Maximum block Y.
     */
    public static final int MAX_Y = 2047;

    /**
     * Nonempty sections in insertion order, keyed by packed section coordinates.
     */
    private final LinkedHashMap<Long, Section> sections = new LinkedHashMap<>();
    /**
     * Read-only section view for renderers.
     */
    private final Collection<Section> sectionView = Collections.unmodifiableCollection(sections.values());
    /**
     * Adapter for native states, or null before the first write.
     */
    private ScenePlatform platform;
    /**
     * Outer bounds of visible blocks in local coordinates.
     */
    private Aabb blockBounds;
    /**
     * Bounds of the node mesh used for the cached union.
     */
    private Aabb previousMeshBounds;
    /**
     * Combined block and mesh bounds.
     */
    private Aabb combinedBounds;
    /**
     * Whether the visible block bounds need recomputing.
     */
    private boolean boundsDirty;
    /**
     * Highest visible block Y, inclusive.
     */
    private int topVisibleY = Integer.MAX_VALUE;
    /**
     * Optional simplified mesh for the entire structure.
     */
    private DistantMesh distantMesh;
    /**
     * Lowest and highest occupied layers for assembly, or null when stale.
     */
    private int[] layers;
    /**
     * Number of nonempty blocks.
     */
    private int blockCount;

    /**
     * Creates an empty structure. The first write uses {@link ScenePlatform#current()}.
     */
    @WikiMethod(section = "Block data", sectionOrder = 10, order = 10,
            summary = "Creates an empty structure. The first write uses `ScenePlatform#current()`.")
    @AiDoc(section = "Block data", sectionOrder = 10, order = 10,
            summary = "Creates an empty structure. The first write uses `ScenePlatform#current()`.")
    public StructureNode() {
    }

    /**
     * Creates an empty structure for a specific platform.
     *
     * @param platform adapter for native block states.
     */
    @WikiMethod(section = "Block data", sectionOrder = 10, order = 10,
            summary = "Creates an empty structure for a specific platform.")
    @AiDoc(section = "Block data", sectionOrder = 10, order = 10,
            summary = "Creates an empty structure for a specific platform.")
    public StructureNode(
            @WikiParameter(description = "adapter for native block states.") ScenePlatform platform) {
        this.platform = Objects.requireNonNull(platform, "platform");
    }

    /**
     * Reads the platform associated with the structure's native states.
     *
     * @return the adapter, or null if it has not been selected.
     */
    @WikiMethod(section = "Block data", sectionOrder = 10, order = 20,
            summary = "Reads the platform associated with the structure's native states.")
    @WikiReturn("the adapter, or null if it has not been selected")
    @AiDoc(section = "Block data", sectionOrder = 10, order = 20,
            summary = "Reads the platform associated with the structure's native states.")
    public ScenePlatform platform() {
        return platform;
    }

    /**
     * Counts nonempty blocks.
     *
     * @return count including layers hidden by {@link #topVisibleY()}.
     */
    @WikiMethod(section = "Block data", sectionOrder = 10, order = 30,
            summary = "Counts nonempty blocks.")
    @WikiReturn("count including layers hidden by `topVisibleY()`")
    @AiDoc(section = "Block data", sectionOrder = 10, order = 30,
            summary = "Counts nonempty blocks.")
    public int blockCount() {
        return blockCount;
    }

    /**
     * Reads a native block state.
     *
     * @param x local block X.
     * @param y local block Y.
     * @param z local block Z.
     * @return the state, or null for an empty or out-of-range cell.
     */
    @WikiMethod(section = "Block data", sectionOrder = 10, order = 40,
            summary = "Reads a native block state.")
    @WikiReturn("the state, or null for an empty or out-of-range cell")
    @AiDoc(section = "Block data", sectionOrder = 10, order = 40,
            summary = "Reads a native block state.")
    public Object block(
            @WikiParameter(description = "local block X.") int x,
            @WikiParameter(description = "local block Y.") int y,
            @WikiParameter(description = "local block Z.") int z) {
        Section section = section(x, y, z);
        return section == null ? null : section.states[Section.index(x, y, z)];
    }

    /**
     * Reads a block subject to the layer cutoff. Renderers use this lookup for face culling and ambient occlusion.
     *
     * @param x local block X.
     * @param y local block Y.
     * @param z local block Z.
     * @return the state, or null for an empty or hidden cell.
     */
    @WikiMethod(section = "Block data", sectionOrder = 10, order = 50,
            summary = "Reads a block subject to the layer cutoff. Renderers use this lookup for face culling and ambient occlusion.")
    @WikiReturn("the state, or null for an empty or hidden cell")
    @AiDoc(section = "Block data", sectionOrder = 10, order = 50,
            summary = "Reads a block subject to the layer cutoff. Renderers use this lookup for face culling and ambient occlusion.")
    public Object visibleBlock(
            @WikiParameter(description = "local block X.") int x,
            @WikiParameter(description = "local block Y.") int y,
            @WikiParameter(description = "local block Z.") int z) {
        return y <= topVisibleY ? block(x, y, z) : null;
    }

    /**
     * Reads a block entity for configuration. Call {@link #invalidateContent()} after editing its data.
     *
     * @param x local block X.
     * @param y local block Y.
     * @param z local block Z.
     * @return the native block entity, or null.
     */
    @WikiMethod(section = "Block data", sectionOrder = 10, order = 80,
            summary = "Reads a block entity for configuration. Call `invalidateContent()` after editing its data.")
    @WikiReturn("the native block entity, or null")
    @AiDoc(section = "Block data", sectionOrder = 10, order = 80,
            summary = "Reads a block entity for configuration. Call `invalidateContent()` after editing its data.")
    public Object blockEntity(
            @WikiParameter(description = "local block X.") int x,
            @WikiParameter(description = "local block Y.") int y,
            @WikiParameter(description = "local block Z.") int z) {
        Section section = section(x, y, z);
        return section == null ? null : section.blockEntity(Section.index(x, y, z));
    }

    /**
     * Sets a block using a {@link BlockSpec}.
     *
     * @param x local block X.
     * @param y local block Y.
     * @param z local block Z.
     * @param block block specification. Air removes the cell.
     * @return this node.
     * @throws IllegalArgumentException if the specification, block or position is invalid.
     * @throws IllegalStateException if the platform or block data is unavailable.
     */
    @WikiMethod(section = "Block data", sectionOrder = 10, order = 60,
            summary = "Sets a block using a `BlockSpec`.")
    @WikiReturn("this node")
    @WikiThrows(type = IllegalArgumentException.class, description = "if the specification, block or position is invalid.")
    @WikiThrows(type = IllegalStateException.class, description = "if the platform or block data is unavailable.")
    @AiDoc(section = "Block data", sectionOrder = 10, order = 60,
            summary = "Sets a block using a `BlockSpec`.")
    public StructureNode setBlock(
            @WikiParameter(description = "local block X.") int x,
            @WikiParameter(description = "local block Y.") int y,
            @WikiParameter(description = "local block Z.") int z,
            @WikiParameter(description = "block specification. Air removes the cell.") String block) {
        BlockSpec spec = BlockSpec.parse(block);
        checkPosition(x, y, z);
        ScenePlatform target = writer();
        return put(x, y, z, available(target.resolveBlock(spec), block));
    }

    /**
     * Sets a native block state or a native block with its default state.
     *
     * @param x local block X.
     * @param y local block Y.
     * @param z local block Z.
     * @param block native state or block for the current platform.
     * @return this node.
     * @throws IllegalArgumentException if the object type or position is invalid.
     * @throws IllegalStateException if the platform is unavailable.
     */
    @WikiMethod(section = "Block data", sectionOrder = 10, order = 60,
            summary = "Sets a native block state or a native block with its default state.")
    @WikiReturn("this node")
    @WikiThrows(type = IllegalArgumentException.class, description = "if the object type or position is invalid.")
    @WikiThrows(type = IllegalStateException.class, description = "if the platform is unavailable.")
    @AiDoc(section = "Block data", sectionOrder = 10, order = 60,
            summary = "Sets a native block state or a native block with its default state.")
    public StructureNode setBlock(
            @WikiParameter(description = "local block X.") int x,
            @WikiParameter(description = "local block Y.") int y,
            @WikiParameter(description = "local block Z.") int z,
            @WikiParameter(description = "native state or block for the current platform.") Object block) {
        return setBlock(x, y, z, block, -1);
    }

    /**
     * Sets a block with metadata on 1.7.10 and 1.12.2.
     *
     * @param x local block X.
     * @param y local block Y.
     * @param z local block Z.
     * @param block native block.
     * @param meta metadata from 0 to 15, or -1 for the default.
     * @return this node.
     * @throws IllegalArgumentException if the object, metadata or position is invalid.
     * @throws IllegalStateException if the platform is unavailable.
     */
    @WikiMethod(section = "Block data", sectionOrder = 10, order = 60,
            summary = "Sets a block with metadata on 1.7.10 and 1.12.2.")
    @WikiReturn("this node")
    @WikiThrows(type = IllegalArgumentException.class, description = "if the object, metadata or position is invalid.")
    @WikiThrows(type = IllegalStateException.class, description = "if the platform is unavailable.")
    @AiDoc(section = "Block data", sectionOrder = 10, order = 60,
            summary = "Sets a block with metadata on 1.7.10 and 1.12.2.")
    public StructureNode setBlock(
            @WikiParameter(description = "local block X.") int x,
            @WikiParameter(description = "local block Y.") int y,
            @WikiParameter(description = "local block Z.") int z,
            @WikiParameter(description = "native block.") Object block,
            @WikiParameter(description = "metadata from 0 to 15, or -1 for the default.") int meta) {
        Objects.requireNonNull(block, "block");
        checkPosition(x, y, z);
        ScenePlatform target = writer();
        return put(x, y, z, available(target.nativeBlock(block, meta), block));
    }

    /**
     * Removes a block and its block entity.
     *
     * @param x local block X.
     * @param y local block Y.
     * @param z local block Z.
     * @return this node.
     */
    @WikiMethod(section = "Block data", sectionOrder = 10, order = 70,
            summary = "Removes a block and its block entity.")
    @WikiReturn("this node")
    @AiDoc(section = "Block data", sectionOrder = 10, order = 70,
            summary = "Removes a block and its block entity.")
    public StructureNode removeBlock(
            @WikiParameter(description = "local block X.") int x,
            @WikiParameter(description = "local block Y.") int y,
            @WikiParameter(description = "local block Z.") int z) {
        if (!inBounds(x, y, z) || block(x, y, z) == null) return this;
        return put(x, y, z, null);
    }

    /**
     * Replaces an existing block's block entity, for example with data from a structure template.
     *
     * @param x local block X.
     * @param y local block Y.
     * @param z local block Z.
     * @param blockEntity native block entity, or null to remove it.
     * @return this node.
     * @throws IllegalArgumentException if the position has no block.
     */
    @WikiMethod(section = "Block data", sectionOrder = 10, order = 90,
            summary = "Replaces an existing block's block entity, for example with data from a structure template.")
    @WikiReturn("this node")
    @WikiThrows(type = IllegalArgumentException.class, description = "if the position has no block.")
    @AiDoc(section = "Block data", sectionOrder = 10, order = 90,
            summary = "Replaces an existing block's block entity, for example with data from a structure template.")
    public StructureNode setBlockEntity(
            @WikiParameter(description = "local block X.") int x,
            @WikiParameter(description = "local block Y.") int y,
            @WikiParameter(description = "local block Z.") int z,
            @WikiParameter(description = "native block entity, or null to remove it.") Object blockEntity) {
        Section section = section(x, y, z);
        int index = Section.index(x, y, z);
        if (section == null || section.states[index] == null) {
            throw new IllegalArgumentException("No block at " + x + ", " + y + ", " + z);
        }
        if (section.blockEntity(index) != blockEntity) {
            section.setBlockEntity(index, blockEntity);
            invalidateContent();
        }
        return this;
    }

    /**
     * Reads the layer cutoff.
     *
     * @return highest visible local block Y, inclusive.
     */
    @WikiMethod(section = "Layer slicing and assembly", sectionOrder = 20, order = 10,
            summary = "Reads the layer cutoff.")
    @WikiReturn("highest visible local block Y, inclusive")
    @AiDoc(section = "Layer slicing and assembly", sectionOrder = 20, order = 10,
            summary = "Reads the layer cutoff.")
    public int topVisibleY() {
        return topVisibleY;
    }

    /**
     * Shows blocks through the specified layer, inclusive. Use {@link Integer#MAX_VALUE} to show all layers.
     *
     * @param topVisibleY highest visible local block Y.
     * @return this node.
     */
    @WikiMethod(section = "Layer slicing and assembly", sectionOrder = 20, order = 20,
            summary = "Shows blocks through the specified layer, inclusive. Use `Integer#MAX_VALUE` to show all layers.")
    @WikiReturn("this node")
    @AiDoc(section = "Layer slicing and assembly", sectionOrder = 20, order = 20,
            summary = "Shows blocks through the specified layer, inclusive. Use `Integer#MAX_VALUE` to show all layers.")
    public StructureNode setTopVisibleY(
            @WikiParameter(description = "highest visible local block Y.") int topVisibleY) {
        if (this.topVisibleY == topVisibleY) return this;
        // Include ambient occlusion and fluid probes up to two blocks away.
        long firstAffectedY = (long) Math.min(this.topVisibleY, topVisibleY) - 1L;
        int lastAffectedY = Math.max(this.topVisibleY, topVisibleY);
        for (Section section : sections.values()) {
            int bottom = section.minBlockY();
            if (bottom <= lastAffectedY && bottom + 15 >= firstAffectedY) section.revision++;
        }
        this.topVisibleY = topVisibleY;
        boundsDirty = true;
        invalidateContent();
        return this;
    }

    /**
     * Finds the lowest and highest occupied layers through an inclusive limit. An unrestricted result is cached
     * until the block set changes.
     *
     * @param targetTopY highest layer to search, inclusive.
     * @return {@code {lowestY, highestY}}, or null if no blocks were found.
     */
    int[] layerRange(int targetTopY) {
        boolean cacheable = targetTopY == Integer.MAX_VALUE;
        if (cacheable && layers != null) return layers.length == 0 ? null : layers;
        int lowestY = Integer.MAX_VALUE;
        int highestY = Integer.MIN_VALUE;
        for (Section section : sections.values()) {
            int bottom = section.minBlockY();
            if (bottom > targetTopY) continue;
            for (int index = 0; index < Section.VOLUME; index++) {
                if (section.states[index] == null) continue;
                int y = bottom + Section.localY(index);
                if (y <= targetTopY) {
                    lowestY = Math.min(lowestY, y);
                    highestY = Math.max(highestY, y);
                }
            }
        }
        int[] range = lowestY == Integer.MAX_VALUE ? new int[0] : new int[]{lowestY, highestY};
        if (cacheable) layers = range;
        return range.length == 0 ? null : range;
    }

    /**
     * Reveals layers from bottom to top. For progress p in [0, 1], visible layers end at
     * {@code floor(lowestY - 1 + p * (highestY - lowestY + 1))}. The occupied range is computed through
     * targetTopY. The first interval is empty, and completion restores the exact target cutoff.
     *
     * @param viewport viewport whose scene owns this node and whose clock drives the animation.
     * @param seconds duration in seconds. Zero applies the final cutoff immediately.
     * @param targetTopY final inclusive local block Y cutoff.
     * @return this node.
     * @throws IllegalArgumentException if the node belongs to a different scene or the duration is negative or nonfinite.
     */
    @WikiMethod(section = "Layer slicing and assembly", sectionOrder = 20, order = 30,
            summary = "Reveals layers from bottom to top. For progress p in [0, 1], visible layers end at `floor(lowestY - 1 + p * (highestY - lowestY + 1))`. The occupied range is computed through targetTopY. The first interval is empty, and completion restores the exact target cutoff.")
    @WikiReturn("this node")
    @WikiThrows(type = IllegalArgumentException.class, description = "if the node belongs to a different scene or the duration is negative or nonfinite.")
    @AiDoc(section = "Layer slicing and assembly", sectionOrder = 20, order = 30,
            summary = "Reveals layers from bottom to top. For progress p in [0, 1], visible layers end at `floor(lowestY - 1 + p * (highestY - lowestY + 1))`. The occupied range is computed through targetTopY. The first interval is empty, and completion restores the exact target cutoff.")
    public StructureNode playAssembly(
            @WikiParameter(description =
                    "viewport whose scene owns this node and whose clock drives the animation.")
            Scene3D viewport,
            @WikiParameter(description = "duration in seconds. Zero applies the final cutoff immediately.") float seconds,
            @WikiParameter(description = "final inclusive local block Y cutoff.") int targetTopY) {
        if (scene() != viewport.scene() || !Float.isFinite(seconds) || seconds < 0) {
            throw new IllegalArgumentException("Assembly requires an attached structure and a finite nonnegative duration");
        }
        viewport.stopParameterAnimation(this);
        if (seconds == 0) return setTopVisibleY(targetTopY);

        int[] range = layerRange(targetTopY);
        if (range == null) return setTopVisibleY(targetTopY);

        int first = range[0] - 1;
        int count = range[1] - first;
        viewport.animateParameterFrom(this, 0, progress -> {
            if (scene() == viewport.scene()) {
                setTopVisibleY(progress >= 1 ? targetTopY : first + (int) (progress * count));
            }
        }, 1, TransitionSpec.of(seconds, AnimationEasing.LINEAR));
        return this;
    }

    /**
     * Sets a distant replacement mesh for the blocks and block entities. Keep the mesh inside the local block bounds.
     * Layer slicing and orthographic views use block models. Picking always uses the original blocks.
     * Update the mesh when the structure changes as needed.
     *
     * @param mesh simplified geometry in structure-local space, or null to disable it.
     * @param material mesh material, or null for {@link Material#DEFAULT}.
     * @param minDistance minimum distance from the camera to the world-space block AABB, in scene units.
     * @return this node.
     * @throws IllegalArgumentException if a supplied mesh has a nonpositive or nonfinite distance.
     */
    @WikiMethod(section = "Renderer integration", sectionOrder = 30, order = 10,
            summary = "Sets a distant replacement mesh for the blocks and block entities. Keep the mesh inside the local block bounds. Layer slicing and orthographic views use block models. Picking always uses the original blocks. Update the mesh when the structure changes as needed.")
    @WikiReturn("this node")
    @WikiThrows(type = IllegalArgumentException.class, description = "if a supplied mesh has a nonpositive or nonfinite distance.")
    @AiDoc(section = "Renderer integration", sectionOrder = 30, order = 10,
            summary = "Sets a distant replacement mesh for the blocks and block entities. Keep the mesh inside the local block bounds. Layer slicing and orthographic views use block models. Picking always uses the original blocks. Update the mesh when the structure changes as needed.")
    public StructureNode setDistantMesh(
            @WikiParameter(description = "simplified geometry in structure-local space, or null to disable it.") Mesh3D mesh,
            @WikiParameter(description = "mesh material, or null for `Material#DEFAULT`.") Material material,
            @WikiParameter(description =
                    "minimum distance from the camera to the world-space block AABB, in scene units.")
            float minDistance) {
        if (mesh != null && (!(minDistance > 0) || !Float.isFinite(minDistance))) {
            throw new IllegalArgumentException("Distant mesh requires a positive finite distance");
        }
        DistantMesh next = mesh == null ? null : new DistantMesh(mesh,
                material == null ? Material.DEFAULT : material, (double) minDistance * minDistance);
        if (!Objects.equals(next, distantMesh)) {
            distantMesh = next;
            invalidateContent();
        }
        return this;
    }

    /**
     * Selects a distant mesh for a camera far enough outside the structure's AABB.
     *
     * @param eye world-space perspective camera position, or null for an orthographic camera.
     * @param world transform from structure space to world space.
     * @return the applicable distant mesh, or null to draw blocks.
     */
    @WikiMethod(section = "Renderer integration", sectionOrder = 30, order = 20,
            summary = "Selects a distant mesh for a camera far enough outside the structure's AABB.")
    @WikiReturn("the applicable distant mesh, or null to draw blocks")
    @AiDoc(section = "Renderer integration", sectionOrder = 30, order = 20,
            summary = "Selects a distant mesh for a camera far enough outside the structure's AABB.")
    public DistantMesh distantMesh(
            @WikiParameter(description =
                    "world-space perspective camera position, or null for an orthographic camera.")
            Vec3View eye,
            @WikiParameter(description = "transform from structure space to world space.") MutableMat4 world) {
        DistantMesh candidate = distantMesh;
        if (candidate == null || eye == null || topVisibleY != Integer.MAX_VALUE) return null;
        contentBounds();
        Aabb bounds = blockBounds;
        if (bounds == null) return null;
        Aabb proxy = candidate.mesh.bounds();
        if (proxy.minX() < bounds.minX() || proxy.minY() < bounds.minY() || proxy.minZ() < bounds.minZ()
                || proxy.maxX() > bounds.maxX() || proxy.maxY() > bounds.maxY() || proxy.maxZ() > bounds.maxZ()) {
            return null;
        }
        double dx = distanceToWorldBounds(bounds, world, 0, eye.x());
        double dy = distanceToWorldBounds(bounds, world, 1, eye.y());
        double dz = distanceToWorldBounds(bounds, world, 2, eye.z());
        return dx * dx + dy * dy + dz * dz >= candidate.distanceSquared ? candidate : null;
    }

    /**
     * Measures separation from the world AABB on one axis. For world matrix row R and eye coordinate e,
     * the distance is {@code max(0, |e - (R · center + translation)| - |R| · halfSize)}.
     * The center and half-size come from the local block AABB, and |R| is componentwise absolute value.
     *
     * @param bounds local structure AABB in blocks.
     * @param world local-to-world affine transform.
     * @param axis world axis index from 0 to 2.
     * @param eye world camera coordinate on that axis.
     * @return the distance to the AABB on that axis in world units.
     */
    private static double distanceToWorldBounds(Aabb bounds, MutableMat4 world, int axis, double eye) {
        double center = world.transformComponent(axis, (bounds.minX() + (double) bounds.maxX()) * 0.5,
                (bounds.minY() + (double) bounds.maxY()) * 0.5, (bounds.minZ() + (double) bounds.maxZ()) * 0.5, 1);
        double radius = (Math.abs(world.get(axis, 0)) * (bounds.maxX() - (double) bounds.minX())
                + Math.abs(world.get(axis, 1)) * (bounds.maxY() - (double) bounds.minY())
                + Math.abs(world.get(axis, 2)) * (bounds.maxZ() - (double) bounds.minZ())) * 0.5;
        return Math.max(0, Math.abs(eye - center) - radius);
    }

    /**
     * Prepared simplified mesh for a structure.
     *
     * @param mesh geometry in structure-local space.
     * @param material surface settings.
     * @param distanceSquared squared threshold in scene units squared.
     */
    public record DistantMesh(Mesh3D mesh, Material material, double distanceSquared) {
    }

    /**
     * Reads sections for geometry baking.
     *
     * @return a live, read-only collection.
     */
    @WikiMethod(section = "Renderer integration", sectionOrder = 30, order = 30,
            summary = "Reads sections for geometry baking.")
    @WikiReturn("a live, read-only collection")
    @AiDoc(section = "Renderer integration", sectionOrder = 30, order = 30,
            summary = "Reads sections for geometry baking.")
    public Collection<Section> sections() {
        return sectionView;
    }

    /**
     * Recomputes content bounds after block changes, including the outer faces of edge blocks.
     *
     * @return local geometry bounds, or null for an empty node.
     */
    @Override
    protected Aabb contentBounds() {
        Aabb meshBounds = super.contentBounds();
        if (!boundsDirty && previousMeshBounds == meshBounds) return combinedBounds;
        if (boundsDirty) {
            blockBounds = null;
            int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
            for (Section section : sections.values()) {
                int bottom = section.minBlockY();
                if (bottom > topVisibleY) continue;
                int west = section.minBlockX(), north = section.minBlockZ();
                for (int index = 0; index < Section.VOLUME; index++) {
                    if (section.states[index] == null) continue;
                    int y = bottom + Section.localY(index);
                    if (y > topVisibleY) continue;
                    int x = west + Section.localX(index), z = north + Section.localZ(index);
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    minZ = Math.min(minZ, z);
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, y);
                    maxZ = Math.max(maxZ, z);
                }
            }
            if (minY != Integer.MAX_VALUE) blockBounds = new Aabb(minX, minY, minZ, maxX + 1, maxY + 1, maxZ + 1);
            boundsDirty = false;
        }
        previousMeshBounds = meshBounds;
        if (blockBounds == null) combinedBounds = meshBounds;
        else if (meshBounds == null) combinedBounds = blockBounds;
        else combinedBounds = new Aabb(Math.min(blockBounds.minX(), meshBounds.minX()),
                    Math.min(blockBounds.minY(), meshBounds.minY()), Math.min(blockBounds.minZ(), meshBounds.minZ()),
                    Math.max(blockBounds.maxX(), meshBounds.maxX()), Math.max(blockBounds.maxY(), meshBounds.maxY()),
                    Math.max(blockBounds.maxZ(), meshBounds.maxZ()));
        return combinedBounds;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected double intersectPickBounds(Ray ray, double entry, double limit, MutableVec3 cellOrigin) {
        double nearest = Double.POSITIVE_INFINITY;
        for (Section section : sections.values()) {
            int y = section.minBlockY();
            if (y > topVisibleY) continue;
            int x = section.minBlockX(), z = section.minBlockZ();
            double sectionDistance = ray.intersectAabb(x, y, z, x + 16, Math.min((long) topVisibleY + 1, y + 16), z + 16);
            if (sectionDistance > limit || sectionDistance >= nearest) continue;
            for (int index = 0; index < Section.VOLUME; index++) {
                if (section.states[index] == null) continue;
                int blockY = y + Section.localY(index);
                if (blockY > topVisibleY) continue;
                int blockX = x + Section.localX(index), blockZ = z + Section.localZ(index);
                double distance = ray.intersectAabb(blockX, blockY, blockZ, blockX + 1, blockY + 1, blockZ + 1);
                if (distance <= limit && distance < nearest) {
                    nearest = distance;
                    cellOrigin.set(blockX, blockY, blockZ);
                }
            }
        }
        return nearest;
    }

    /**
     * Writes a native state. Null and air remove the cell. Marks sections whose geometry, fluid surfaces
     * or ambient occlusion can be affected by the update.
     *
     * @param x valid local block X.
     * @param y valid local block Y.
     * @param z valid local block Z.
     * @param state native state, or null.
     * @return this node.
     */
    private StructureNode put(int x, int y, int z, Object state) {
        if (state != null && platform.isAirBlock(state)) state = null;
        long key = sectionKey(x >> 4, y >> 4, z >> 4);
        Section section = sections.get(key);
        int index = Section.index(x, y, z);
        Object previous = section == null ? null : section.states[index];
        if (Objects.equals(previous, state)) return this;
        if (state == null) {
            section.states[index] = null;
            section.setBlockEntity(index, null);
            if (--section.blockCount == 0) sections.remove(key);
            blockCount--;
        } else {
            if (section == null) {
                section = new Section(x >> 4, y >> 4, z >> 4);
                sections.put(key, section);
            }
            if (previous == null) {
                section.blockCount++;
                blockCount++;
            }
            section.states[index] = state;
            section.setBlockEntity(index, platform.blockEntity(state, section.blockEntity(index), x, y, z));
        }
        // Neighbor queries include corner AO samples and the height of nearby fluid columns.
        for (int sy = (y - 2) >> 4; sy <= (y + 2) >> 4; sy++) {
            for (int sz = (z - 2) >> 4; sz <= (z + 2) >> 4; sz++) {
                for (int sx = (x - 2) >> 4; sx <= (x + 2) >> 4; sx++) {
                    Section affected = sections.get(sectionKey(sx, sy, sz));
                    if (affected != null) affected.revision++;
                }
            }
        }
        boundsDirty |= y <= topVisibleY && (state == null || previous == null);
        if (state == null || previous == null) layers = null;
        invalidateContent();
        return this;
    }

    /**
     * Selects and retains the adapter used for writes.
     *
     * @return the structure platform.
     * @throws IllegalStateException if no platform was supplied or installed.
     */
    private ScenePlatform writer() {
        if (platform == null) {
            platform = ScenePlatform.current();
            if (platform == null) {
                throw new IllegalStateException("No scene platform is installed; create the structure on the client "
                        + "or pass a platform to the constructor");
            }
        }
        return platform;
    }

    /**
     * Checks that a platform produced a block state.
     *
     * @param state platform result.
     * @param source original block input used in the error message.
     * @return non-null native state.
     * @throws IllegalStateException if the state is unavailable.
     */
    private static Object available(Object state, Object source) {
        if (state == null) throw new IllegalStateException("Blocks are not available yet: " + source);
        return state;
    }

    /**
     * Finds the section that contains the block.
     *
     * @param x local block X.
     * @param y local block Y.
     * @param z local block Z.
     * @return the section, or null.
     */
    private Section section(int x, int y, int z) {
        return inBounds(x, y, z) ? sections.get(sectionKey(x >> 4, y >> 4, z >> 4)) : null;
    }

    /**
     * Checks coordinates against the packed block position limits.
     *
     * @param x local block X.
     * @param y local block Y.
     * @param z local block Z.
     * @return true if the position is representable.
     */
    private static boolean inBounds(int x, int y, int z) {
        return x >= MIN_HORIZONTAL && x <= MAX_HORIZONTAL && z >= MIN_HORIZONTAL && z <= MAX_HORIZONTAL
                && y >= MIN_Y && y <= MAX_Y;
    }

    /**
     * Rejects an out-of-range position.
     *
     * @param x local block X.
     * @param y local block Y.
     * @param z local block Z.
     * @throws IllegalArgumentException if the position is outside the supported range.
     */
    private static void checkPosition(int x, int y, int z) {
        if (!inBounds(x, y, z)) {
            throw new IllegalArgumentException("Structure position exceeds Minecraft block coordinates: "
                    + x + ", " + y + ", " + z);
        }
    }

    /**
     * Packs section coordinates using 22 bits for X, 22 for Z and 8 for Y.
     *
     * @param x section X.
     * @param y section Y.
     * @param z section Z.
     * @return packed section key.
     */
    private static long sectionKey(int x, int y, int z) {
        return ((long) (x & 0x3FFFFF) << 30) | ((long) (z & 0x3FFFFF) << 8) | (y & 0xFF);
    }

    /**
     * A 16×16×16 section. Index {@code (y << 8) | (z << 4) | x} uses coordinates relative to the section.
     */
    public static final class Section {
        /**
         * Number of cells in a section.
         */
        public static final int VOLUME = 16 * 16 * 16;
        /**
         * Section coordinates in the grid of 16-block sections.
         */
        private final int x, y, z;
        /**
         * Native block states by cell index, or null for air.
         */
        private final Object[] states = new Object[VOLUME];
        /**
         * Block entities by cell index. The array is allocated when needed.
         */
        private Object[] blockEntities;
        /**
         * Number of occupied cells.
         */
        private int blockCount;
        /**
         * Number of block entities.
         */
        private int blockEntityCount;
        /**
         * Geometry cache revision for this section and affected neighbor changes.
         */
        private long revision;

        /**
         * Creates an empty section.
         *
         * @param x section X.
         * @param y section Y.
         * @param z section Z.
         */
        private Section(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        /**
         * Computes the cell index from block coordinates.
         *
         * @param x block X in structure coordinates.
         * @param y block Y in structure coordinates.
         * @param z block Z in structure coordinates.
         * @return index from 0 through {@link #VOLUME} - 1.
         */
        public static int index(int x, int y, int z) {
            return ((y & 15) << 8) | ((z & 15) << 4) | (x & 15);
        }

        /**
         * Decodes the local X coordinate.
         *
         * @param index cell index.
         * @return X within the section.
         */
        public static int localX(int index) {
            return index & 15;
        }

        /**
         * Decodes the local Y coordinate.
         *
         * @param index cell index.
         * @return Y within the section.
         */
        public static int localY(int index) {
            return index >>> 8;
        }

        /**
         * Decodes the local Z coordinate.
         *
         * @param index cell index.
         * @return Z within the section.
         */
        public static int localZ(int index) {
            return (index >>> 4) & 15;
        }

        /**
         * Reads the section origin along X.
         *
         * @return X of the first block in the section.
         */
        public int minBlockX() {
            return x << 4;
        }

        /**
         * Reads the section origin along Y.
         *
         * @return Y of the first block in the section.
         */
        public int minBlockY() {
            return y << 4;
        }

        /**
         * Reads the section origin along Z.
         *
         * @return Z of the first block in the section.
         */
        public int minBlockZ() {
            return z << 4;
        }

        /**
         * Reads the geometry revision.
         *
         * @return change counter affected by blocks, neighbors and the layer cutoff.
         */
        public long revision() {
            return revision;
        }

        /**
         * Counts occupied cells.
         *
         * @return number of occupied cells.
         */
        public int blockCount() {
            return blockCount;
        }

        /**
         * Counts block entities.
         *
         * @return number of block entities.
         */
        public int blockEntityCount() {
            return blockEntityCount;
        }

        /**
         * Reads a cell's state.
         *
         * @param index cell index.
         * @return native state, or null.
         */
        public Object state(int index) {
            return states[index];
        }

        /**
         * Reads a cell's block entity.
         *
         * @param index cell index.
         * @return native block entity, or null.
         */
        public Object blockEntity(int index) {
            return blockEntities == null ? null : blockEntities[index];
        }

        /**
         * Replaces a cell's block entity and updates the count.
         *
         * @param index cell index.
         * @param value new block entity, or null.
         */
        private void setBlockEntity(int index, Object value) {
            if (blockEntities == null) {
                if (value == null) return;
                blockEntities = new Object[VOLUME];
            }
            Object previous = blockEntities[index];
            if (previous == null && value != null) blockEntityCount++;
            else if (previous != null && value == null) blockEntityCount--;
            blockEntities[index] = value;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StructureNode setPosition(float x, float y, float z) {
        super.setPosition(x, y, z);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StructureNode setRotation(dev.sixik.unigui.api.math.QuatView value) {
        super.setRotation(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StructureNode setScale(float x, float y, float z) {
        super.setScale(x, y, z);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StructureNode setVisible(boolean visible) {
        super.setVisible(visible);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StructureNode setOrigin(float x, float y, float z) {
        super.setOrigin(x, y, z);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StructureNode centerOrigin() {
        super.centerOrigin();
        return this;
    }
}
