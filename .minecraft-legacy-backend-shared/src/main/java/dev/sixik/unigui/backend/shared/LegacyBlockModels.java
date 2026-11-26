package dev.sixik.unigui.backend.shared;

import dev.sixik.unigui.api.scene.Material;
import dev.sixik.unigui.api.scene.Mesh3D;
import dev.sixik.unigui.api.scene.ScenePlatform;
import dev.sixik.unigui.api.scene.StructureNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;

/**
 * Baked block geometry for the 1.7.10 and 1.12.2 scene renderer, shared by every
 * {@link dev.sixik.unigui.api.scene.BlockNode} with the same native state, and merged section geometry of
 * {@link StructureNode}s.
 *
 * <p>Atlas coordinates change on resource reload, so the whole cache is dropped when the platform reports a new
 * resource generation. Native states must have stable {@code equals}/{@code hashCode}: canonical
 * {@code IBlockState} instances on 1.12.2 and value records on 1.7.10.</p>
 */
public final class LegacyBlockModels {
    /**
     * Marker stored for states without supported geometry, so they are not baked every frame.
     */
    private static final Object NO_GEOMETRY = new Object();

    /**
     * Version-specific source of block geometry.
     */
    public interface Baker {
        /**
         * @return the platform that resolves {@link dev.sixik.unigui.api.scene.BlockNode} sources.
         */
        ScenePlatform platform();

        /**
         * @return a counter that changes whenever the block atlas is rebuilt.
         */
        int resourceGeneration();

        /**
         * Builds the geometry of one block state.
         *
         * @param state a native state returned by {@link #platform()}.
         * @return block-local geometry, or null for a block without supported geometry.
         */
        Surface bake(Object state);

        /**
         * Tells whether a block fills its cell and hides the faces of its neighbours.
         *
         * @param state a native state returned by {@link #platform()}.
         * @return true for an opaque full cube.
         */
        default boolean isOpaqueCube(Object state) {
            return false;
        }
    }

    /**
     * A block's atlas geometry and material.
     *
     * @param mesh     block-local triangles.
     * @param material atlas texture, shading and blend mode.
     */
    public record Surface(Mesh3D mesh, Material material) {
        /**
         * @throws NullPointerException if the mesh or material is null.
         */
        public Surface {
            Objects.requireNonNull(mesh, "mesh");
            Objects.requireNonNull(material, "material");
        }
    }

    /**
     * Version-specific geometry source.
     */
    private final Baker baker;
    /**
     * Baked surface or {@link #NO_GEOMETRY} per native state.
     */
    private final HashMap<Object, Object> surfaces = new HashMap<>();
    /**
     * Merged geometry per structure section. Removed sections are not retained.
     */
    private final Map<StructureNode.Section, BakedSection> sections = new WeakHashMap<>();
    /**
     * Resource generation of the cached surfaces.
     */
    private int generation;
    /**
     * Whether {@link #generation} has been read.
     */
    private boolean initialized;

    /**
     * @param baker version-specific geometry source.
     */
    public LegacyBlockModels(Baker baker) {
        this.baker = Objects.requireNonNull(baker, "baker");
    }

    /**
     * @return the platform used to resolve block nodes.
     */
    public ScenePlatform platform() {
        return baker.platform();
    }

    /**
     * Returns the geometry of a state, baking it on first use after a resource reload.
     *
     * @param state native block state, or null.
     * @return baked geometry, or null for a null state or a block without supported geometry.
     */
    public Surface surface(Object state) {
        if (state == null) return null;
        int current = baker.resourceGeneration();
        if (!initialized || generation != current) {
            surfaces.clear();
            sections.clear();
            generation = current;
            initialized = true;
        }
        Object cached = surfaces.get(state);
        if (cached == null) {
            Surface baked = baker.bake(state);
            cached = baked == null ? NO_GEOMETRY : baked;
            surfaces.put(state, cached);
        }
        return cached == NO_GEOMETRY ? null : (Surface) cached;
    }

    /**
     * Returns the merged geometry of a structure section, rebuilding it after the section or its neighbours change.
     * Faces lying on a cell boundary are skipped when the visible neighbour is an opaque full cube.
     *
     * @param structure structure providing neighbours and the visible layer limit.
     * @param section   section to bake.
     * @return one surface per material in structure coordinates; empty when nothing is visible.
     */
    public List<Surface> section(StructureNode structure, StructureNode.Section section) {
        int current = baker.resourceGeneration();
        if (!initialized || generation != current) {
            surfaces.clear();
            sections.clear();
            generation = current;
            initialized = true;
        }
        BakedSection cached = sections.get(section);
        if (cached != null && cached.revision == section.revision()) return cached.surfaces;
        LinkedHashMap<Material, LegacySceneQuads> merged = new LinkedHashMap<>();
        int top = structure.topVisibleY();
        int west = section.minBlockX(), bottom = section.minBlockY(), north = section.minBlockZ();
        for (int index = 0; index < StructureNode.Section.VOLUME; index++) {
            Object state = section.state(index);
            if (state == null) continue;
            int y = bottom + StructureNode.Section.localY(index);
            if (y > top) continue;
            Surface surface = surface(state);
            if (surface == null) continue;
            int x = west + StructureNode.Section.localX(index), z = north + StructureNode.Section.localZ(index);
            append(merged.computeIfAbsent(surface.material(), key -> new LegacySceneQuads()), surface.mesh(),
                    structure, x, y, z);
        }
        List<Surface> result = new ArrayList<>(merged.size());
        merged.forEach((material, quads) -> {
            Mesh3D mesh = quads.build();
            if (mesh != null) result.add(new Surface(mesh, material));
        });
        List<Surface> baked = List.copyOf(result);
        sections.put(section, new BakedSection(section.revision(), baked));
        return baked;
    }

    /**
     * Copies the quads of one block, translated to its cell, without faces hidden by an opaque neighbour.
     * Block meshes come from {@link LegacySceneQuads}, so each run of four vertices is one quad.
     *
     * @param out       accumulator of one material.
     * @param mesh      block-local geometry.
     * @param structure neighbour source.
     * @param x         block X in the structure.
     * @param y         block Y in the structure.
     * @param z         block Z in the structure.
     */
    private void append(LegacySceneQuads out, Mesh3D mesh, StructureNode structure, int x, int y, int z) {
        for (int quad = 0; quad + 3 < mesh.vertexCount(); quad += 4) {
            if (hidden(mesh, quad, structure, x, y, z)) continue;
            for (int v = quad; v < quad + 4; v++) {
                out.vertex(mesh.position(v, 0) + x, mesh.position(v, 1) + y, mesh.position(v, 2) + z,
                        mesh.uv(v, 0), mesh.uv(v, 1), mesh.normal(v, 0), mesh.normal(v, 1), mesh.normal(v, 2),
                        mesh.colorArgb(v));
            }
        }
    }

    /**
     * Tests whether a quad lies on a cell face whose neighbour is a visible opaque cube. The face is identified by an
     * axis-aligned unit normal n and all four vertices having coordinate 1 (for n = +1) or 0 (for n = -1) on that axis.
     *
     * @param mesh      block-local geometry.
     * @param quad      first vertex of the quad.
     * @param structure neighbour source.
     * @param x         block X in the structure.
     * @param y         block Y in the structure.
     * @param z         block Z in the structure.
     * @return true when the quad cannot be seen.
     */
    private boolean hidden(Mesh3D mesh, int quad, StructureNode structure, int x, int y, int z) {
        for (int axis = 0; axis < 3; axis++) {
            float n = mesh.normal(quad, axis);
            if (n != 1 && n != -1) continue;
            float plane = n > 0 ? 1 : 0;
            for (int v = quad; v < quad + 4; v++) {
                if (mesh.position(v, axis) != plane) return false;
            }
            int step = (int) n;
            Object neighbour = structure.visibleBlock(axis == 0 ? x + step : x, axis == 1 ? y + step : y,
                    axis == 2 ? z + step : z);
            return neighbour != null && baker.isOpaqueCube(neighbour);
        }
        return false;
    }

    /**
     * Section geometry with the revision it was built from.
     *
     * @param revision section revision at bake time.
     * @param surfaces merged geometry per material.
     */
    private record BakedSection(long revision, List<Surface> surfaces) {
    }
}
