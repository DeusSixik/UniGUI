package dev.sixik.unigui.api.scene;

import dev.sixik.unigui.api.math.Aabb;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SceneContentTest {
    private static final float[] TRIANGLE = {-2, 1, 2, 0, 3, -1, 4, 0, 0};
    private static final float EPS = 1e-6f;

    @Test
    void mesh_copiesVertexAndIndexAttributes_andComputesBounds() {
        float[] positions = TRIANGLE.clone();
        float[] normals = {0, 1, 0, 0, 1, 0, 0, 1, 0};
        float[] uv = {0, 0, 0, 1, 1, 0};
        int[] colors = {0xFFAABBCC, 0xFFFFFFFF, 0xFFFFFFFF};
        int[] indices = {0, 1, 2};
        Mesh3D mesh = new Mesh3D(positions, normals, uv, colors, indices);
        positions[0] = 99;
        normals[1] = 99;
        uv[4] = 99;
        colors[0] = 0;
        indices[0] = 99;

        assertEquals(3, mesh.vertexCount());
        assertEquals(3, mesh.indexCount());
        assertEquals(0, mesh.index(0));
        assertEquals(-2.0f, mesh.position(0, 0), EPS);
        assertEquals(1.0f, mesh.normal(0, 1), EPS);
        assertEquals(1.0f, mesh.uv(2, 0), EPS);
        assertEquals(0xFFAABBCC, mesh.colorArgb(0));
        assertEquals(new Aabb(-2, 0, -1, 4, 3, 2), mesh.bounds());
    }

    @Test
    void mesh_missingOptionalAttributes_providesRenderDefaults() {
        Mesh3D mesh = triangle();
        assertEquals(0.0f, mesh.normal(0, 0), EPS);
        assertEquals(1.0f, mesh.normal(0, 2), EPS);
        assertEquals(0.0f, mesh.uv(0, 1), EPS);
        assertEquals(0xFFFFFFFF, mesh.colorArgb(0));
        assertThrows(IndexOutOfBoundsException.class, () -> mesh.uv(0, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> mesh.colorArgb(3));
    }

    @Test
    void mesh_invalidData_rejectsMismatchedOrNonFiniteGeometry() {
        int[] indices = {0, 1, 2};
        float[] invalidPositions = TRIANGLE.clone();
        invalidPositions[1] = Float.NaN;
        assertThrows(IllegalArgumentException.class, () -> new Mesh3D(invalidPositions, null, null, null, indices));
        assertThrows(IllegalArgumentException.class, () -> new Mesh3D(TRIANGLE, null, null, null, new int[]{0, 1, 3}));
        assertThrows(IllegalArgumentException.class, () -> new Mesh3D(TRIANGLE, null, null, null, new int[]{0, 1}));
        assertThrows(IllegalArgumentException.class, () -> new Mesh3D(TRIANGLE, new float[8], null, null, indices));
        assertThrows(IllegalArgumentException.class,
                () -> new Mesh3D(TRIANGLE, null, new float[]{Float.POSITIVE_INFINITY, 0, 0, 0, 0, 0}, null, indices));
        assertThrows(IllegalArgumentException.class, () -> new Aabb(2, 0, 0, 0, 0, 0));
    }

    @Test
    void nodeContent_changesAndBoundsOverride_updateSceneRevision() {
        SceneNode node = new SceneNode();
        Scene scene = new Scene().addRoot(node);
        long start = scene.revision();
        Mesh3D mesh = triangle();
        Material material = new Material(0x80FF0000, null, Material.Shading.LIT,
                Material.AlphaMode.TRANSLUCENT, 0.5f, Material.Culling.NONE);
        assertNull(node.bounds());
        assertSame(Material.DEFAULT, node.material());

        node.setMesh(mesh).setMaterial(material);
        assertSame(mesh.bounds(), node.bounds());
        assertSame(material, node.material());
        Aabb override = new Aabb(0, 0, 0, 1, 1, 1);
        node.setBounds(override).setMesh(null).setVisible(false);
        assertSame(override, node.bounds());
        node.setBounds(null);
        assertNull(node.bounds());
        assertEquals(start + 6, scene.revision());

        node.setVisible(false).setMesh(null).setMaterial(material).setBounds(null).setScale(1, 1, 1);
        assertEquals(start + 6, scene.revision());
        node.invalidateContent();
        assertEquals(start + 7, scene.revision());
    }

    @Test
    void sceneLighting_normalizesDirectionAndMarksActualChanges() {
        Scene scene = new Scene();
        DirectionalLight directional = new DirectionalLight(0, -3, 4, 0xFF00FFFF, 2);
        AmbientLight ambient = new AmbientLight(0xFFFFFF, 0.4f);

        scene.setAmbientLight(ambient).setDirectionalLight(directional);
        assertEquals(2, scene.revision());
        assertEquals(0.4f, scene.ambientLight().intensity(), EPS);
        assertEquals(-0.6f, scene.directionalLight().y(), EPS);
        assertEquals(0.8f, scene.directionalLight().z(), EPS);
        assertEquals(0x00FFFF, scene.directionalLight().colorRgb());

        scene.setAmbientLight(new AmbientLight(0xFFFFFF, 0.4f)).setDirectionalLight(directional);
        assertEquals(2, scene.revision());
        scene.setDirectionalLight(null);
        assertEquals(3, scene.revision());
        assertNull(scene.directionalLight());
    }

    @Test
    void materialsAndLights_invalidValues_areRejected() {
        assertThrows(IllegalArgumentException.class, () -> new AmbientLight(0, -1));
        assertThrows(IllegalArgumentException.class, () -> new DirectionalLight(0, 0, 0, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> new DirectionalLight(1, 0, 0, 0, Float.NaN));
        assertThrows(IllegalArgumentException.class, () -> new Material(0, null, Material.Shading.UNLIT,
                Material.AlphaMode.CUTOUT, Float.NaN, Material.Culling.NONE));
    }

    private static Mesh3D triangle() {
        return new Mesh3D(TRIANGLE, null, null, null, new int[]{0, 1, 2});
    }
}
