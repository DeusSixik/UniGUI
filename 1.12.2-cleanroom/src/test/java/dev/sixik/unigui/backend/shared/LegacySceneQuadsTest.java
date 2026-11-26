package dev.sixik.unigui.backend.shared;

import dev.sixik.unigui.api.math.Aabb;
import dev.sixik.unigui.api.math.MutableVec3;
import dev.sixik.unigui.api.scene.BlockNode;
import dev.sixik.unigui.api.scene.Camera3D;
import dev.sixik.unigui.api.scene.Material;
import dev.sixik.unigui.api.scene.Mesh3D;
import dev.sixik.unigui.api.scene.Scene;
import dev.sixik.unigui.api.scene.ScenePicker;
import dev.sixik.unigui.api.scene.BlockSpec;
import dev.sixik.unigui.api.scene.ScenePlatform;
import dev.sixik.unigui.api.scene.StructureNode;
import org.junit.jupiter.api.Test;

import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class LegacySceneQuadsTest {
    @Test
    void build_twoQuads_preservesWindingAtlasCoordinatesAndBounds() {
        LegacySceneQuads quads = quad(1);
        appendQuad(quads, 2);
        Mesh3D mesh = quads.build();
        assertArrayEquals(new int[]{0, 1, 2, 0, 2, 3, 4, 5, 6, 4, 6, 7},
                IntStream.range(0, mesh.indexCount()).map(mesh::index).toArray());
        assertEquals(new Aabb(0, 0, 1, 1, 1, 2), mesh.bounds());
        assertEquals(0.5f, mesh.uv(2, 0), Math.ulp(0.5f));
        assertEquals(0.25f, mesh.uv(2, 1), Math.ulp(0.25f));
        assertEquals(0xFFAABBCC, mesh.colorArgb(2));
    }

    @Test
    void build_emptyOrPartialQuad_doesNotProduceCorruptTriangles() {
        LegacySceneQuads quads = new LegacySceneQuads();
        assertNull(quads.build());
        quads.vertex(0, 0, 0, 0, 0, 0, 0, 1, -1);
        assertThrows(IllegalStateException.class, quads::build);
    }

    @Test
    void models_sameStateAndResources_bakeOnceAndRebuildAfterReload() {
        TestBaker baker = new TestBaker();
        LegacyBlockModels models = new LegacyBlockModels(baker);
        var first = models.surface("stone");
        assertSame(first, models.surface("stone"));
        assertEquals(1, baker.bakes);
        assertNull(models.surface(null));

        baker.empty = true;
        assertNull(models.surface("air"));
        assertNull(models.surface("air"));
        assertEquals(2, baker.bakes);

        baker.empty = false;
        baker.generation++;
        assertNotSame(first, models.surface("stone"));
        assertEquals(3, baker.bakes);
    }

    @Test
    void pick_blockNode_reportsItsLocalCellOrigin() {
        BlockNode block = new BlockNode("minecraft:stone");
        block.setPosition(-0.5f, -0.5f, -3);
        Scene scene = new Scene().addRoot(block);
        Camera3D camera = new Camera3D().setViewport(100, 100).setOrthographic(4, 1, 10);
        MutableVec3 cell = new MutableVec3();
        assertSame(block, new ScenePicker().pick(scene, camera, 50, 50, cell));
        assertEquals(0, cell.x(), 0);
        assertEquals(0, cell.y(), 0);
        assertEquals(0, cell.z(), 0);
    }

    @Test
    void section_mergesBlocksPerMaterialAndCullsFacesBetweenOpaqueCubes() {
        ScenePlatform strings = new ScenePlatform() {
            @Override
            public Object resolveBlock(BlockSpec spec) {
                return spec.id();
            }

            @Override
            public Object nativeBlock(Object block, int meta) {
                return block;
            }
        };
        TestBaker baker = new TestBaker();
        baker.sides = true;
        LegacyBlockModels models = new LegacyBlockModels(baker);
        StructureNode structure = new StructureNode(strings)
                .setBlock(0, 0, 0, "minecraft:stone").setBlock(1, 0, 0, "minecraft:stone");
        StructureNode.Section section = structure.sections().iterator().next();

        var surfaces = models.section(structure, section);
        assertEquals(1, surfaces.size());
        Mesh3D merged = surfaces.get(0).mesh();
        // Two blocks with two side faces each; the faces touching at x = 1 are hidden.
        assertEquals(8, merged.vertexCount());
        assertEquals(new Aabb(0, 0, 0, 2, 1, 1), merged.bounds());
        assertSame(surfaces, models.section(structure, section));

        structure.setBlock(1, 0, 0, "minecraft:glass");
        baker.opaque = false;
        Mesh3D open = models.section(structure, section).get(0).mesh();
        assertEquals(16, open.vertexCount());
        structure.setTopVisibleY(-1);
        assertTrue(models.section(structure, section).isEmpty());
    }

    /** @param z plane depth in block units.
     * @return accumulator containing one quad.
     */
    private static LegacySceneQuads quad(float z) {
        LegacySceneQuads quads = new LegacySceneQuads();
        appendQuad(quads, z);
        return quads;
    }

    /** @param quads destination for the four vertices.
     * @param z plane depth in block units.
     */
    private static void appendQuad(LegacySceneQuads quads, float z) {
        quads.vertex(0, 0, z, 0.25f, 0.5f, 0, 0, 1, 0xFFAABBCC);
        quads.vertex(1, 0, z, 0.5f, 0.5f, 0, 0, 1, 0xFFAABBCC);
        quads.vertex(1, 1, z, 0.5f, 0.25f, 0, 0, 1, 0xFFAABBCC);
        quads.vertex(0, 1, z, 0.25f, 0.25f, 0, 0, 1, 0xFFAABBCC);
    }

    /** Model source independent of Minecraft's resource manager. */
    private static final class TestBaker implements LegacyBlockModels.Baker {
        /** Reload counter driven by the test. */
        private int generation;
        /** Number of bake calls. */
        private int bakes;
        /** Whether the next bake reports a block without geometry. */
        private boolean empty;
        /** Whether bakes produce the -X and +X faces of a unit cube instead of one quad. */
        private boolean sides;
        /** Whether every block is an opaque cube. */
        private boolean opaque = true;

        /** {@inheritDoc} */
        @Override
        public boolean isOpaqueCube(Object state) {
            return opaque;
        }

        /** {@inheritDoc} */
        @Override
        public ScenePlatform platform() {
            return null;
        }

        /** {@inheritDoc} */
        @Override
        public int resourceGeneration() {
            return generation;
        }

        /** {@inheritDoc} */
        @Override
        public LegacyBlockModels.Surface bake(Object state) {
            bakes++;
            if (empty) return null;
            if (!sides) return new LegacyBlockModels.Surface(quad(0).build(), Material.DEFAULT);
            LegacySceneQuads cube = new LegacySceneQuads();
            for (int x = 0; x <= 1; x++) {
                float normal = x == 0 ? -1 : 1;
                cube.vertex(x, 0, 0, 0, 0, normal, 0, 0, -1);
                cube.vertex(x, 1, 0, 0, 1, normal, 0, 0, -1);
                cube.vertex(x, 1, 1, 1, 1, normal, 0, 0, -1);
                cube.vertex(x, 0, 1, 1, 0, normal, 0, 0, -1);
            }
            return new LegacyBlockModels.Surface(cube.build(), Material.DEFAULT);
        }
    }
}
