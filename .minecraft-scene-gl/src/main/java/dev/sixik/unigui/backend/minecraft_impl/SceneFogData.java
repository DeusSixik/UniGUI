package dev.sixik.unigui.backend.minecraft_impl;

import dev.sixik.unigui.api.math.Aabb;
import dev.sixik.unigui.api.math.Frustum3D;
import dev.sixik.unigui.api.math.Mat4View;
import dev.sixik.unigui.api.math.MutableMat4;
import dev.sixik.unigui.api.scene.Camera3D;
import dev.sixik.unigui.api.scene.FogVolume;
import dev.sixik.unigui.impl.render.SceneMeshData;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/**
 * Selects visible fog volumes and packs the input block shared by both graphics backends.
 */
final class SceneFogData {
    /**
     * Bounds shared by the unit box and unit sphere.
     */
    private static final Aabb UNIT_BOUNDS = new Aabb(-1, -1, -1, 1, 1, 1);
    /**
     * Fog descriptors in scene traversal order.
     */
    private final FogVolume[] volumes = new FogVolume[SceneFogShaders.MAX_VOLUMES];
    /**
     * Inverse world transform for each accepted descriptor.
     */
    private final MutableMat4[] worldToLocal = new MutableMat4[SceneFogShaders.MAX_VOLUMES];
    /**
     * Reusable current node's world transform.
     */
    private final MutableMat4 world = new MutableMat4();
    /**
     * Inverse canonical view-projection of the current camera.
     */
    private final MutableMat4 clipToWorld = new MutableMat4();
    /**
     * Camera and active volume data for both passes in native byte order.
     */
    private final ByteBuffer bytes = ByteBuffer.allocateDirect(SceneFogShaders.UNIFORM_BYTES).order(ByteOrder.nativeOrder());
    /**
     * Number of volume descriptors in the current frame.
     */
    private int count;
    /**
     * Whether any collected descriptor needs the spotlight bindings.
     */
    private boolean scattering;

    /**
     * Allocates the reusable inverse matrices for the visible volumes.
     */
    SceneFogData() {
        Arrays.setAll(worldToLocal, index -> new MutableMat4());
    }

    /**
     * @param volume  fog node whose local shape may intersect the active view
     * @param frustum active camera's visibility volume
     */
    void collect(FogVolume volume, Frustum3D frustum) {
        if (count == volumes.length || volume.density() == 0) return;
        volume.worldMatrix(world);
        if (!frustum.intersects(UNIT_BOUNDS, world) || !worldToLocal[count].setInverse(world)) return;
        volumes[count++] = volume;
        scattering |= volume.lightScattering() > 0;
    }

    /**
     * @param viewProjection canonical world-to-clip camera matrix
     * @return whether visible fog can be integrated with this camera
     */
    boolean prepare(Mat4View viewProjection) {
        return count > 0 && clipToWorld.setInverse(viewProjection);
    }

    /**
     * @return whether a visible volume requests light scattering
     */
    boolean scattering() {
        return scattering;
    }

    /**
     * Packs the active view and fog descriptors in std140 order. RGB channels are normalized by 255.
     *
     * @param viewportX fraction of the source texture used horizontally
     * @param viewportY fraction of the source texture used vertically
     * @param camera    clips in world units and projection mode
     * @param seconds   scene material time in seconds
     * @param spot      selected light block index, or -1
     * @return native-order buffer of active uniform data
     */
    ByteBuffer write(float viewportX, float viewportY, Camera3D camera, float seconds, int spot) {
        bytes.clear();
        SceneMeshData.writeClipMatrix(bytes, clipToWorld);
        bytes.putFloat(viewportX).putFloat(viewportY).putFloat(camera.near()).putFloat(camera.far());
        bytes.putFloat(seconds).putFloat(camera.orthographic() ? 1 : 0).putFloat(count).putFloat(spot);
        for (int i = 0; i < count; i++) {
            FogVolume volume = volumes[i];
            SceneMeshData.writeClipMatrix(bytes, worldToLocal[i]);
            int rgb = volume.colorRgb();
            bytes.putFloat((rgb >> 16 & 255) / 255f).putFloat((rgb >> 8 & 255) / 255f)
                    .putFloat((rgb & 255) / 255f).putFloat(volume.density());
            var noise = volume.noise();
            bytes.putFloat(volume.shape() == FogVolume.Shape.SPHERE ? 1 : 0).putFloat(volume.edgeSoftness())
                    .putFloat(noise.strength()).putFloat(noise.scale());
            bytes.putFloat(noise.velocityX()).putFloat(noise.velocityY()).putFloat(noise.velocityZ())
                    .putFloat(volume.lightScattering());
        }
        return bytes.position(0);
    }

    /**
     * Drops scene references before reusing the staging block.
     */
    void reset() {
        Arrays.fill(volumes, 0, count, null);
        count = 0;
        scattering = false;
    }
}
