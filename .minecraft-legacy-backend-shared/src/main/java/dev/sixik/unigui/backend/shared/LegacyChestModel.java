package dev.sixik.unigui.backend.shared;

import dev.sixik.unigui.api.render.SimpleTextureHandle;
import dev.sixik.unigui.api.scene.Material;

/**
 * Bakes the closed single chest used by the vanilla 1.7.10 and 1.12.2 chest renderers.
 */
public final class LegacyChestModel {
    /**
     * Box corners encoded as upper-end flags for X, Y and Z, in vanilla ModelBox quad order.
     */
    private static final int[][] FACES = {
            {5, 1, 3, 7}, {0, 4, 6, 2}, {5, 4, 0, 1}, {3, 2, 6, 7}, {1, 0, 2, 3}, {4, 5, 7, 6}
    };
    /**
     * Unit normals in the block coordinate system of a south-facing chest.
     */
    private static final int[][] NORMALS = {
            {1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}
    };
    /**
     * Vanilla chest lid, latch and base in model pixels, including the lid and base overlap.
     */
    private static final Box LID = new Box(1, 2, 1, 14, 5, 14, 0, 0);
    private static final Box LATCH = new Box(7, 5, 0, 2, 4, 1, 0, 0);
    private static final Box BASE = new Box(1, 6, 1, 14, 10, 14, 0, 19);

    private LegacyChestModel() {
    }

    /**
     * @param texture vanilla chest texture name without directory or extension
     * @param facing  legacy horizontal direction index (north 2, south 3, west 4, east 5)
     * @param lit     whether the scene supplies face lighting
     * @return one surface with the closed chest in block coordinates and its original 64×64 texture
     */
    public static LegacyBlockModels.Surface bake(String texture, int facing, boolean lit) {
        int cos = switch (facing) {
            case 2 -> -1;
            case 4, 5 -> 0;
            default -> 1;
        };
        int sin = facing == 4 ? -1 : facing == 5 ? 1 : 0;
        var quads = new LegacySceneQuads();
        append(quads, LID, cos, sin, lit);
        append(quads, LATCH, cos, sin, lit);
        append(quads, BASE, cos, sin, lit);
        var image = new SimpleTextureHandle(textureId(texture), 64, 64);
        var shading = lit ? Material.Shading.LIT : Material.Shading.UNLIT;
        var material = new Material(0xFFFFFFFF, image, shading, Material.AlphaMode.CUTOUT, 0.1f, Material.Culling.BACK);
        return new LegacyBlockModels.Surface(quads.build(), material);
    }

    /**
     * @param texture vanilla chest texture name @return its namespaced texture resource
     */
    public static String textureId(String texture) {
        return "minecraft:textures/entity/chest/" + texture + ".png";
    }

    /**
     * Maps ModelChest pixels {@code (px,py,pz)} to {@code (px/16, 1-py/16, 1-pz/16)}, as in the
     * vanilla tile renderer. Rotates the centered horizontal vector {@code (x-0.5,z-0.5)} and
     * the normal's horizontal components with {@code (x',z') = (cos*x+sin*z, cos*z-sin*x)}.
     *
     * @param quads destination quad stream
     * @param box   model-space position, extent and UV origin in pixels
     * @param cos   cosine of the facing rotation about Y
     * @param sin   sine of the facing rotation about Y
     * @param lit   whether color is kept unshaded
     */
    private static void append(LegacySceneQuads quads, Box box, int cos, int sin, boolean lit) {
        int u = box.u, v = box.v, w = box.width, h = box.height, d = box.depth;
        // The two caps sit above the strip of four side faces in the vanilla chest texture.
        int[][] uv = {
                {u + d + w, v + d, u + d + w + d, v + d + h},
                {u, v + d, u + d, v + d + h},
                {u + d, v, u + d + w, v + d},
                {u + d + w, v + d, u + d + 2 * w, v},
                {u + d, v + d, u + d + w, v + d + h},
                {u + 2 * d + w, v + d, u + 2 * d + 2 * w, v + d + h}
        };
        for (int f = 0; f < FACES.length; f++) {
            int[] normal = NORMALS[f];
            int nx = cos * normal[0] + sin * normal[2];
            int ny = normal[1];
            int nz = cos * normal[2] - sin * normal[0];
            int color = lit ? 0xFFFFFFFF : LegacySceneQuads.shade(0xFFFFFFFF, nx, ny, nz);
            int[] rect = uv[f];
            for (int i = 0; i < 4; i++) {
                int corner = FACES[f][i];
                float x = (box.x + ((corner & 1) == 0 ? 0 : w)) / 16f - 0.5f;
                float y = 1 - (box.y + ((corner & 2) == 0 ? 0 : h)) / 16f;
                float z = 0.5f - (box.z + ((corner & 4) == 0 ? 0 : d)) / 16f;
                quads.vertex(0.5f + cos * x + sin * z, y, 0.5f + cos * z - sin * x,
                        rect[i == 0 || i == 3 ? 2 : 0] / 64f, rect[i < 2 ? 1 : 3] / 64f, nx, ny, nz, color);
            }
        }
    }

    /**
     * @param x      model X origin in pixels
     * @param y      model Y origin in pixels, increasing downward
     * @param z      model Z origin in pixels, increasing toward the back
     * @param width  model X extent in pixels
     * @param height model Y extent in pixels
     * @param depth  model Z extent in pixels
     * @param u      horizontal texture origin in pixels
     * @param v      vertical texture origin in pixels
     */
    private record Box(int x, int y, int z, int width, int height, int depth, int u, int v) {
    }
}
