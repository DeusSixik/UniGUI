package dev.sixik.unigui.backend.minecraft_impl;

/**
 * Shader sources for drawing native sprites with a view-distance intersection fade.
 */
final class SceneSoftParticleShaders {
    /**
     * Number of sprites per draw, sized to fit in the minimum supported uniform block.
     */
    static final int BATCH_SIZE = 64;
    /**
     * Size in bytes of a projection matrix and two vec4s.
     */
    static final int SCENE_BYTES = 96;
    /**
     * Each sprite occupies five aligned vec4s.
     */
    static final int BATCH_BYTES = BATCH_SIZE * 80;
    /**
     * Canonical camera projection, active viewport fraction, clip planes and backend flags.
     */
    private static final String CAMERA = """
            layout(std140) uniform SoftCamera {
                mat4 CameraProjection;
                vec4 ViewportPlanes;
                vec4 CameraFlags;
            };
            """;
    /**
     * The cloud's alpha is {@code 1 - smoothstep(r - 0.24, r, length(p))}, where {@code p} is
     * centered billboard UV in [-1, 1] and {@code r} is the seeded radius perturbed by two scales
     * of value noise. A short fade at the billboard boundary keeps even the largest clouds seamless.
     */
    private static final String CLOUD_MASK = """
            #if PROCEDURAL_CLOUDS
            flat in float cloudSeed;
            
            float cloudHash(vec2 p) {
                vec3 q = fract(vec3(p.xyx) * vec3(0.1031, 0.11369, 0.13787));
                q += dot(q, q.yzx + 19.19);
                return fract((q.x + q.y) * q.z);
            }
            
            float cloudNoise(vec2 p) {
                vec2 cell = floor(p);
                vec2 f = fract(p);
                f = f * f * (3.0 - 2.0 * f);
                return mix(mix(cloudHash(cell), cloudHash(cell + vec2(1, 0)), f.x),
                        mix(cloudHash(cell + vec2(0, 1)), cloudHash(cell + vec2(1, 1)), f.x), f.y);
            }
            
            vec4 cloudColor(vec2 uv, float seed) {
                vec2 p = uv * 2.0 - 1.0;
                vec2 shift = seed * vec2(13.17, 7.51);
                float coarse = cloudNoise(p * 2.7 + shift);
                float detail = cloudNoise(p * 6.8 - shift);
                float radius = mix(0.48, 0.79, cloudHash(shift)) + (coarse - 0.5) * 0.34
                        + (detail - 0.5) * 0.15;
                float mask = 1.0 - smoothstep(radius - 0.24, radius, length(p));
                float border = min(min(uv.x, 1.0 - uv.x), min(uv.y, 1.0 - uv.y));
                return vec4(vec3(mix(0.88, 1.06, coarse)), mask * smoothstep(0.0, 0.045, border));
            }
            #endif
            """;

    /**
     * Positions the card in camera space and supplies texture coordinates for the scene target's depth.
     */
    static final String VERTEX = vertex(330, false);

    /**
     * @param version      GLSL language version
     * @param forwardDepth whether near maps to zero on this backend
     * @return full instanced billboard vertex source
     */
    static String vertex(int version, boolean forwardDepth) {
        return vertex(version, forwardDepth, false);
    }

    /**
     * @param version          GLSL language version
     * @param forwardDepth     whether near maps to zero
     * @param proceduralClouds whether to carry an optional cloud seed to the fragment shader
     * @return full instanced billboard vertex source
     */
    static String vertex(int version, boolean forwardDepth, boolean proceduralClouds) {
        return header(version, forwardDepth, proceduralClouds) + CAMERA + """
                #if defined(VULKAN)
                #define INSTANCE_INDEX gl_InstanceIndex
                #else
                #define INSTANCE_INDEX gl_InstanceID
                #endif
                struct Sprite {
                    vec4 centerFade;
                    vec4 halfSize;
                    vec4 rotation;
                    vec4 uvRect;
                    vec4 color;
                };
                layout(std140) uniform SoftSprites {
                    Sprite Sprites[%d];
                };
                in vec3 Position;
                out vec2 spriteUv;
                out vec4 tint;
                out vec3 depthUvClip;
                out vec2 distanceFade;
                #if PROCEDURAL_CLOUDS
                flat out float cloudSeed;
                #endif
                
                void main() {
                    Sprite sprite = Sprites[INSTANCE_INDEX];
                    vec3 corner = vec3(Position.xy * sprite.halfSize.xy, 0.0);
                    vec4 q = sprite.rotation;
                    vec3 offset = corner + 2.0 * cross(q.xyz, cross(q.xyz, corner) + q.w * corner);
                    vec3 view = sprite.centerFade.xyz + offset;
                    vec4 clip = CameraProjection * vec4(view, 1.0);
                    vec2 region = ViewportPlanes.xy;
                    clip.xy = clip.xy * region + clip.w * vec2(region.x - 1.0, 1.0 - region.y);
                    #if !FORWARD_DEPTH
                    clip.z = CameraFlags.y > 0.5 ? (clip.w - clip.z) * 0.5 : -clip.z;
                    #endif
                    gl_Position = clip;
                    depthUvClip = vec3(clip.xy, clip.w);
                    spriteUv = mix(sprite.uvRect.xy, sprite.uvRect.zw, vec2(0.5) + Position.xy * vec2(0.5, -0.5));
                    tint = sprite.color;
                    distanceFade = vec2(-view.z, sprite.centerFade.w);
                    #if PROCEDURAL_CLOUDS
                    cloudSeed = sprite.halfSize.z;
                    #endif
                }
                """.formatted(BATCH_SIZE);
    }

    /**
     * Multiplies sprite alpha by {@code smoothstep(0, r, s-p)} for surface distance s, fragment
     * distance p and configured fade r. Clear depth indicates no opaque surface. Color is premultiplied.
     * A hard sprite uses the nearest base-level texel at {@code floor(uv * size)}, where {@code uv}
     * is the interpolated atlas coordinate and {@code size} is the atlas extent in texels.
     */
    static final String FRAGMENT = fragment(330, false);

    /**
     * @param version      GLSL language version
     * @param forwardDepth whether to convert sampled scene depth into reverse depth
     * @return full depth-fading fragment source
     */
    static String fragment(int version, boolean forwardDepth) {
        return fragment(version, forwardDepth, false);
    }

    /**
     * @param version          GLSL language version
     * @param forwardDepth     whether to convert sampled scene depth into reverse depth
     * @param proceduralClouds whether to support feathered procedural cloud masks
     * @return full depth-fading fragment source
     */
    static String fragment(int version, boolean forwardDepth, boolean proceduralClouds) {
        return header(version, forwardDepth, proceduralClouds) + CAMERA + SceneDepthShaders.VIEW_DISTANCE + CLOUD_MASK + """
                uniform sampler2D SoftAtlas;
                uniform sampler2D SoftSceneDepth;
                in vec2 spriteUv;
                in vec4 tint;
                in vec3 depthUvClip;
                in vec2 distanceFade;
                out vec4 fragColor;
                
                void main() {
                    vec2 depthUv = depthUvClip.xy / depthUvClip.z * 0.5 + 0.5;
                    float depth = textureLod(SoftSceneDepth, depthUv, 0.0).r;
                    #if FORWARD_DEPTH
                    depth = 1.0 - depth;
                    #endif
                    float fade = 1.0;
                    if (depth > 0.0) {
                        float surface = viewDistance(depth, ViewportPlanes.zw, CameraFlags.x > 0.5);
                        fade = distanceFade.y > 0.0 ? smoothstep(0.0, distanceFade.y, surface - distanceFade.x)
                                : step(distanceFade.x, surface);
                    }
                    vec4 atlasColor;
                    #if PROCEDURAL_CLOUDS
                    if (cloudSeed > 0.0) {
                        atlasColor = cloudColor(spriteUv, cloudSeed);
                    } else
                    #endif
                    if (distanceFade.y > 0.0) {
                        atlasColor = textureLod(SoftAtlas, spriteUv, 0.0);
                    } else {
                        ivec2 size = textureSize(SoftAtlas, 0);
                        ivec2 pixel = clamp(ivec2(floor(spriteUv * vec2(size))), ivec2(0), size - 1);
                        atlasColor = texelFetch(SoftAtlas, pixel, 0);
                    }
                    vec4 sprite = atlasColor * tint;
                    float alpha = sprite.a * fade;
                    fragColor = vec4(sprite.rgb * alpha, alpha);
                }
                """;
    }

    /**
     * @param version          GLSL language version
     * @param forwardDepth     whether the target uses forward depth
     * @param proceduralClouds whether cloud shading is compiled in
     * @return shader header shared by the two billboard stages
     */
    private static String header(int version, boolean forwardDepth, boolean proceduralClouds) {
        return "#version " + version + "\n#define FORWARD_DEPTH " + (forwardDepth ? "1\n" : "0\n")
                + "#define PROCEDURAL_CLOUDS " + (proceduralClouds ? "1\n" : "0\n");
    }

    /**
     * This class stores shader source only.
     */
    private SceneSoftParticleShaders() {
    }
}
