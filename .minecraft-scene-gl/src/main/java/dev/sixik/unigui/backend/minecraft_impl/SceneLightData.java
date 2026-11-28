package dev.sixik.unigui.backend.minecraft_impl;

import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.scene.LocalLight;
import dev.sixik.unigui.api.scene.SpotLight;
import dev.sixik.unigui.impl.render.SceneLightTransform;
import dev.sixik.unigui.impl.render.SceneMeshData;

import java.nio.ByteBuffer;

/**
 * Staging entry shared by the GPU and OpenGL scene light blocks.
 */
final class     SceneLightData {
    /**
     * std140 size in bytes of one light entry.
     */
    static final int BYTES = 160;
    /**
     * Pose shared by the color and depth passes.
     */
    final SceneLightTransform transform = new SceneLightTransform();
    /**
     * Mask's UV origin and extent in the physical texture.
     */
    final MutableRect uv = new MutableRect();
    /**
     * Source configuration for this frame, null after reset.
     */
    LocalLight light;
    /**
     * One-based sampler indices, zero for an absent texture.
     */
    int maskIndex, shadowIndex;
    /**
     * Whether the source RGB already includes its alpha.
     */
    boolean premultiplied;

    /**
     * Writes world pose, RGB radiance, cone data and projection in std140 order. Each packed RGB channel
     * uses {@code channel / 255 * intensity}. Cone cutoff values are the cosines of the half-angles.
     *
     * @param out native-order buffer with 160 writable bytes
     */
    void write(ByteBuffer out) {
        var position = transform.position();
        out.putFloat(position.x()).putFloat(position.y()).putFloat(position.z()).putFloat(light.range());
        int rgb = light.colorRgb();
        out.putFloat((rgb >> 16 & 255) / 255f * light.intensity())
                .putFloat((rgb >> 8 & 255) / 255f * light.intensity())
                .putFloat((rgb & 255) / 255f * light.intensity()).putFloat(0);
        if (light instanceof SpotLight spot) {
            var direction = transform.direction();
            out.putFloat(direction.x()).putFloat(direction.y()).putFloat(direction.z())
                    .putFloat((float) Math.cos(spot.outerAngle()));
            out.putFloat((float) Math.cos(spot.innerAngle())).putFloat(maskIndex)
                    .putFloat(shadowIndex).putFloat(premultiplied ? 1 : 0);
            out.putFloat(uv.x()).putFloat(uv.y()).putFloat(uv.width()).putFloat(uv.height());
            var settings = spot.shadows();
            out.putFloat(settings == null ? 0 : settings.bias()).putFloat(settings == null ? 0 : settings.strength()).putLong(0);
            SceneMeshData.writeClipMatrix(out, transform.clip());
        } else {
            out.putLong(0).putFloat(0).putFloat(-1);
            for (int i = 0; i < 14; i++) out.putLong(0);
        }
    }

    /**
     * Releases the source and sampler state between scenes.
     */
    void reset() {
        light = null;
        maskIndex = shadowIndex = 0;
        premultiplied = false;
    }
}
