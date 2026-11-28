package dev.sixik.unigui.backend.minecraft_impl;

/**
 * Reverse depth operations shared by offscreen scene shaders.
 */
final class SceneDepthShaders {
    /**
     * Reconstructs positive view-axis distance from device depth d, where d=0 is far and d=1 is near.
     * For orthographic projection distance is {@code far * (1-d) + near * d}.
     * For perspective projection reciprocal distance is {@code d / near + (1-d) / far}.
     */
    static final String VIEW_DISTANCE = """
            float viewDistance(float depth, vec2 planes, bool orthographic) {
                return orthographic ? mix(planes.y, planes.x, depth)
                        : planes.x / (depth + (1.0 - depth) * planes.x / planes.y);
            }
            """;

    /**
     * This class stores shader source only.
     */
    private SceneDepthShaders() {
    }
}
