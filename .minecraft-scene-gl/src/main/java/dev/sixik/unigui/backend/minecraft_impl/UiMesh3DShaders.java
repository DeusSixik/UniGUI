package dev.sixik.unigui.backend.minecraft_impl;

import dev.sixik.unigui.impl.render.SceneMeshData;

/**
 * GLSL passes for indexed meshes. The uniform block must match {@link SceneMeshData}.
 */
final class UiMesh3DShaders {
    /**
     * Maximum number of matrices in a single instanced draw call.
     */
    static final int MAX_INSTANCES = 32;
    /**
     * Maximum number of point and spot lights in the shared scene block.
     */
    static final int MAX_LOCAL_LIGHTS = 8;
    /**
     * Fixed GLSL sampler names, indexed by the mask slot.
     */
    static final String[] LOCAL_MASK_NAMES = {"SceneLightMask0", "SceneLightMask1", "SceneLightMask2", "SceneLightMask3",
            "SceneLightMask4", "SceneLightMask5", "SceneLightMask6", "SceneLightMask7"};
    /**
     * Fixed GLSL sampler names, indexed by the perspective depth slot.
     */
    static final String[] LOCAL_SHADOW_NAMES = {"SceneSpotShadow0", "SceneSpotShadow1"};
    /**
     * Size of the std140 block for instance matrices in bytes.
     */
    static final int INSTANCE_BYTES = MAX_INSTANCES * SceneMeshData.CLIP_BYTES;
    /**
     * Shared std140 uniform block for vertex and fragment stages.
     */
    private static final String UNIFORMS = """
            layout(std140) uniform SceneMesh {
                mat4 Clip;
                mat3 NormalTransform;
                vec4 Tint;
                vec4 Ambient;
                vec4 Light;
                vec4 LightDirection;
                vec4 Viewport;
                vec4 MaterialFlags;
            };
            """;
    /**
     * Per-draw surface data. Two mat4s and seven vec4s occupy 240 bytes in std140.
     */
    private static final String SURFACE_UNIFORMS = """
            layout(std140) uniform SceneSurface {
                mat4 World;
                mat4 ShadowClip;
                vec4 SurfaceSettings;
                vec4 SurfaceParameters;
                vec4 GlintU;
                vec4 GlintV;
                vec4 SurfaceBoundsMin;
                vec4 SurfaceBoundsMax;
                vec4 SurfaceClock;
            };
            """;
    /**
     * Size of the surface block in bytes.
     */
    static final int SURFACE_BYTES = 192 + SceneMeshData.EFFECT_BYTES;

    /**
     * Transforms local positions to clip space and normals to world coordinates.
     */
    private static final String VERTEX_BODY = """
            #if INSTANCED
            layout(std140) uniform SceneInstances {
                mat4 Instances[MAX_INSTANCES];
            };
            #if defined(VULKAN)
            #define INSTANCE_INDEX gl_InstanceIndex
            #else
            #define INSTANCE_INDEX gl_InstanceID
            #endif
            #endif
            
            in vec3 Position;
            in vec2 UV0;
            in vec4 Color;
            in vec4 Normal;
            
            out vec2 vertexUv;
            out vec4 vertexColor;
            out vec3 worldNormal;
            #if SCENE_SURFACE
            out vec3 localPosition;
            out vec3 worldPosition;
            out vec3 shadowPosition;
            #endif
            
            void main() {
                #if INSTANCED
                vec4 p = Instances[INSTANCE_INDEX] * vec4(Position, 1.0);
                worldNormal = vec3(0.0);
                #else
                vec4 p = Clip * vec4(Position, 1.0);
                worldNormal = NormalTransform * Normal.xyz;
                #endif
                // Vertices are projected onto the top-left rectangle of the shared target.
                p.x = p.x * Viewport.x + p.w * (Viewport.x - 1.0);
                p.y = p.y * Viewport.y + p.w * (1.0 - Viewport.y);
                #if REVERSE_DEPTH
                p.z = -p.z;
                #endif
                if (Viewport.z > 0.5) p.z = (p.z + p.w) * 0.5;
                gl_Position = p;
                #if SCENE_SURFACE
                localPosition = Position;
                worldPosition = (World * vec4(Position, 1.0)).xyz;
                shadowPosition = (ShadowClip * vec4(Position, 1.0)).xyz;
                #endif
            
                vertexUv = vec2(UV0.x, MaterialFlags.y > 0.5 ? 1.0 - UV0.y : UV0.y);
                vertexColor = Color * Tint;
            }
            """;

    /**
     * Applies alpha and lighting after resolving the base color.
     */
    private static final String SHADE = """
                if (MaterialFlags.w == 1.0 && !gl_FrontFacing) discard;
                if (MaterialFlags.w == 2.0 && gl_FrontFacing) discard;
                if (Viewport.w == 1.0 && color.a < MaterialFlags.x) discard;
            
                vec3 rgb = color.rgb;
                if (LightDirection.w > 0.5) {
                    float nLength = length(worldNormal);
                    float diffuse = nLength > 0.0 ? max(dot(worldNormal / nLength, LightDirection.xyz), 0.0) : 0.0;
                    rgb *= Ambient.rgb + Light.rgb * diffuse;
                }
                float alpha = Viewport.w == 2.0 ? color.a : 1.0;
                fragColor = vec4(clamp(rgb, 0.0, 1.0) * alpha, alpha);
            }
            """;

    /**
     * Fragment stage interface declarations.
     */
    private static final String FRAGMENT_INPUTS = """
            in vec2 vertexUv;
            in vec4 vertexColor;
            in vec3 worldNormal;
            
            out vec4 fragColor;
            
            """;

    /**
     * Inputs for surface functions and sampling the orthographic shadow map.
     */
    private static final String SURFACE_INPUTS = """
            in vec3 localPosition;
            in vec3 worldPosition;
            in vec3 shadowPosition;
            """;

    /**
     * Shader functions available to custom surface code. For a local axis coordinate x and bounds a and b,
     * the normalized coordinate is {@code clamp((x - a) / (b - a), 0, 1)}. Flat axes map to the midpoint.
     * If the span overflows a GLSL float, halving the numerator and denominator preserves the ratio.
     */
    private static final String EFFECT_HELPERS = """
            vec3 sceneLocalBoundsMin() { return SurfaceBoundsMin.xyz; }
            vec3 sceneLocalBoundsMax() { return SurfaceBoundsMax.xyz; }
            float sceneEffectElapsed() { return SurfaceClock.x; }
            float sceneEffectProgress() { return SurfaceClock.y; }
            
            float sceneNormalizeAxis(float value, float low, float high) {
                float span = high - low;
                if (span <= 0.0) return 0.5;
                float sampleValue = clamp(value, low, high);
                if (isinf(span)) {
                    return (sampleValue * 0.5 - low * 0.5) / (high * 0.5 - low * 0.5);
                }
                return (sampleValue - low) / span;
            }
            
            float sceneNormalizedHeight(vec3 position) {
                return sceneNormalizeAxis(position.y, SurfaceBoundsMin.y, SurfaceBoundsMax.y);
            }
            
            vec3 sceneNormalizedPosition(vec3 position) {
                return vec3(sceneNormalizeAxis(position.x, SurfaceBoundsMin.x, SurfaceBoundsMax.x),
                            sceneNormalizedHeight(position),
                            sceneNormalizeAxis(position.z, SurfaceBoundsMin.z, SurfaceBoundsMax.z));
            }
            
            float sceneDissolve(float threshold, float edgeWidth) {
                float progress = sceneEffectProgress();
                if (progress <= 0.0) return 0.0;
                float remaining = clamp(threshold, 0.0, 1.0) - progress;
                if (remaining <= 0.0) discard;
                return edgeWidth > 0.0 ? 1.0 - smoothstep(0.0, edgeWidth, remaining) : 0.0;
            }
            """;

    /**
     * Rows of the native texture matrix transform homogeneous UV into the scrollable glint coordinates.
     */
    private static final String GLINT_UV = """
            vec2 sceneGlintUv(vec2 uv) {
                vec3 p = vec3(uv, 1.0);
                return vec2(dot(GlintU.xyz, p), dot(GlintV.xyz, p));
            }
            """;

    /**
     * Tests receiver depth against nine neighboring texels. Reverse depth maps light clip Z to
     * {@code 0.5 - 0.5*z}. A caster is closer to the light if its stored depth is greater than the receiver's.
     * The bias grows on grazing surfaces to reduce self-shadowing.
     */
    private static final String SHADOW_LOOKUP = """
            uniform sampler2D SceneShadowMap;
            float sceneVisibility(float diffuse) {
                vec2 uv = shadowPosition.xy * 0.5 + 0.5;
                float depth = 0.5 - shadowPosition.z * 0.5;
                if (depth <= 0.0 || depth >= 1.0 || any(lessThanEqual(uv, vec2(0.0)))
                        || any(greaterThanEqual(uv, vec2(1.0)))) return 1.0;
            
                vec2 texel = 1.0 / vec2(textureSize(SceneShadowMap, 0));
                float bias = SurfaceSettings.x * (1.0 + 2.0 * (1.0 - diffuse));
                float occluded = 0.0;
                for (int y = -1; y <= 1; y++) {
                    for (int x = -1; x <= 1; x++) {
                        vec2 sampleUv = uv + vec2(x, y) * texel;
                        float casterDepth = texture(SceneShadowMap, sampleUv).r;
                        occluded += casterDepth > depth + bias ? 1.0 : 0.0;
                    }
                }
                return 1.0 - SurfaceSettings.y * occluded / 9.0;
            }
            """;

    /**
     * Shared std140 local light records, including spotlight projection and shadow data.
     */
    static final String LOCAL_LIGHT_UNIFORMS = """
            struct SceneLocalLight {
                vec4 positionRange;
                vec4 radiance;
                vec4 directionOuter;
                vec4 coneSlots;
                vec4 maskUv;
                vec4 shadowSettings;
                mat4 projection;
            };
            layout(std140) uniform SceneLocalLights {
                ivec4 LocalLightCount;
                SceneLocalLight LocalLights[%d];
            };
            """.formatted(MAX_LOCAL_LIGHTS);

    /**
     * Accumulates direct diffuse light. With distance d, range r and normalized direction l from the
     * fragment to the light, the weight is max(0, 1 - d/r)^2 times max(0, dot(normal, l)).
     * Spotlights multiply that weight by the cone, projected color and their own visibility term.
     */
    private static final String LOCAL_LIGHT_LOOKUP = """
            float sceneSpotVisibility(sampler2D map, vec3 projected, vec2 uv, float bias, float strength) {
                float depth = 0.5 - projected.z * 0.5;
                if (depth <= 0.0 || depth >= 1.0) return 1.0;
                vec2 texel = 1.0 / vec2(textureSize(map, 0));
                float occluded = 0.0;
                for (int y = -1; y <= 1; ++y) {
                    for (int x = -1; x <= 1; ++x) {
                        float caster = textureLod(map, uv + vec2(x, y) * texel, 0.0).r;
                        occluded += caster > depth + bias ? 1.0 : 0.0;
                    }
                }
                return 1.0 - strength * occluded / 9.0;
            }
            
            vec3 sceneLocalIllumination(vec3 normal) {
                vec3 total = vec3(0.0);
                for (int i = 0; i < LocalLightCount.x; ++i) {
                    SceneLocalLight light = LocalLights[i];
                    vec3 delta = (light.positionRange.xyz - worldPosition) / light.positionRange.w;
                    float distanceRatio = length(delta);
                    if (distanceRatio >= 1.0 || distanceRatio <= 0.0) continue;
                    vec3 toLight = delta / distanceRatio;
                    float diffuse = dot(normal, toLight);
                    if (diffuse <= 0.0) continue;
                    float falloff = 1.0 - distanceRatio;
                    vec3 direct = light.radiance.rgb * (diffuse * falloff * falloff);
                    if (light.directionOuter.w >= 0.0) {
                        float angle = dot(light.directionOuter.xyz, -toLight);
                        float outer = light.directionOuter.w;
                        if (angle < outer) continue;
                        float inner = light.coneSlots.x;
                        if (inner > outer) direct *= smoothstep(outer, inner, angle);
                        int maskSlot = int(light.coneSlots.y);
                        int shadowSlot = SurfaceClock.w > 0.5 ? int(light.coneSlots.z) : 0;
                        if (maskSlot != 0 || shadowSlot != 0) {
                            vec4 clip = light.projection * vec4(worldPosition, 1.0);
                            if (clip.w <= 0.0) continue;
                            vec3 projected = clip.xyz / clip.w;
                            vec2 uv = projected.xy * 0.5 + 0.5;
                            if (any(lessThan(uv, vec2(0.0))) || any(greaterThan(uv, vec2(1.0)))) continue;
                            vec2 maskUv = light.maskUv.xy + vec2(uv.x, 1.0 - uv.y) * light.maskUv.zw;
                            vec4 maskColor = vec4(1.0);
                            %s
                            direct *= maskColor.rgb * mix(maskColor.a, 1.0, light.coneSlots.w);
                            float bias = light.shadowSettings.x * (1.0 + 2.0 * (1.0 - diffuse));
                            %s
                        }
                    }
                    total += direct;
                }
                return total;
            }
            """;

    /**
     * Alpha and facing rules shared by the scene-color and shadow passes of an advanced material.
     */
    private static final String SURFACE_MAIN = """
                if (MaterialFlags.w == 1.0 && !gl_FrontFacing) discard;
                if (MaterialFlags.w == 2.0 && gl_FrontFacing) discard;
                float normalLength = length(worldNormal);
                vec3 normal = normalLength > 0.0 ? worldNormal / normalLength : vec3(0.0);
                vec3 emission = vec3(0.0);
                #if CUSTOM_SURFACE
                color = sceneSurface(color, vertexUv, localPosition, worldPosition, normal, SurfaceParameters,
                        SurfaceSettings.z, emission);
                if (SurfaceClock.z > 0.0 && color.a < SurfaceClock.z) discard;
                #endif
                if ((NATIVE_CUTOFF == 1 || Viewport.w == 1.0) && color.a < MaterialFlags.x) discard;
                #if SHADOW_PASS
                fragColor = vec4(0.0);
                #elif GLINT_OVERLAY
                // The blend state squares the source RGB and leaves target alpha intact.
                fragColor = vec4(color.rgb * (SurfaceSettings.w * clamp(color.a, 0.0, 1.0)), 0.0);
                #else
                float diffuse = max(dot(normal, LightDirection.xyz), 0.0);
                float visibility = 1.0;
                #if RECEIVE_SHADOW
                visibility = sceneVisibility(diffuse);
                #endif
                vec3 rgb = color.rgb;
                if (LightDirection.w > 0.5) {
                    vec3 illumination = Ambient.rgb + Light.rgb * (diffuse * visibility);
                    #if LOCAL_LIGHTS
                    illumination += sceneLocalIllumination(normal);
                    #endif
                    rgb *= illumination;
                } else {
                    rgb *= visibility;
                }
                #if INTEGRATED_GLINT
                vec3 foil = texture(GlintSampler, sceneGlintUv(vertexUv)).rgb * SurfaceSettings.w;
                emission += foil * foil;
                #endif
                float alpha = Viewport.w == 2.0 ? clamp(color.a, 0.0, 1.0) : 1.0;
                fragColor = vec4(clamp(rgb + emission, 0.0, 1.0) * alpha, alpha);
                #endif
            }
            """;

    private UiMesh3DShaders() {
    }

    /**
     * Builds the vertex shader for a single or instanced mesh.
     *
     * @param version       GLSL version
     * @param reversedDepth whether reversed depth is required
     * @param instanced     whether to read matrices from the instance array
     * @return complete GLSL vertex shader source
     */
    static String vertex(int version, boolean reversedDepth, boolean instanced) {
        return "#version " + version + "\n#define REVERSE_DEPTH "
                + (reversedDepth ? "1\n" : "0\n") + "#define INSTANCED " + (instanced ? "1\n" : "0\n")
                + "#define MAX_INSTANCES " + MAX_INSTANCES + "\n" + UNIFORMS + VERTEX_BODY;
    }

    /**
     * Builds the fragment shader with Lambertian lighting and premultiplied alpha for 2D composition.
     *
     * @param version  GLSL version
     * @param textured whether the Sampler0 sampler is required
     * @return complete GLSL fragment shader source
     */
    static String fragment(int version, boolean textured) {
        return "#version " + version + "\n" + UNIFORMS + FRAGMENT_INPUTS + baseColor(textured, false) + SHADE;
    }

    /**
     * Builds the fragment shader of wireframe edges: the material tint alone, premultiplied, without vertex color,
     * texture or lighting, so edges keep one color over any surface.
     *
     * @param version GLSL version
     * @return complete fragment source for use with {@link #vertex}
     */
    static String wireFragment(int version) {
        return "#version " + version + "\n" + UNIFORMS + FRAGMENT_INPUTS + """
                void main() {
                    fragColor = vec4(Tint.rgb * Tint.a, Tint.a);
                }
                """;
    }

    /**
     * @return vertex source for one advanced 26.3 mesh draw with world and light-space coordinates
     */
    static String surfaceVertex() {
        return surfaceVertex(330, true);
    }

    /**
     * Builds a surface vertex shader for the selected depth convention.
     *
     * @param version       GLSL language version
     * @param reversedDepth whether the device compares greater depths first
     * @return complete vertex source
     */
    static String surfaceVertex(int version, boolean reversedDepth) {
        return "#version " + version + "\n#define REVERSE_DEPTH " + (reversedDepth ? 1 : 0)
                + "\n#define INSTANCED 0\n#define SCENE_SURFACE 1\n"
                + UNIFORMS + SURFACE_UNIFORMS + VERTEX_BODY;
    }

    /**
     * Builds the color or shadow variant of a surface shader.
     *
     * @param textured        whether the source material uses a texture
     * @param shadowPass      whether to write shadow depth
     * @param receiveShadow   whether to sample a shadow map during the color pass
     * @param nativeCutoff    whether to honor native alpha cutoffs on translucent layers
     * @param integratedGlint whether to add the second glint sampler in the color pass
     * @param glintOverlay    whether to transform the base sampler for a depth-equal foil layer
     * @param localLights     whether the color variant consumes the local lights block
     * @param masks           number of projected mask textures, packed in scene order
     * @param spotShadows     number of perspective shadow maps, packed in scene order
     * @param source          GLSL definition of sceneSurface, or null to use the base color
     * @return complete GLSL fragment source
     */
    static String surfaceFragment(boolean textured, boolean shadowPass, boolean receiveShadow, boolean nativeCutoff,
                                  boolean integratedGlint, boolean glintOverlay, boolean localLights,
                                  int masks, int spotShadows, String source) {
        return surfaceFragment(330, textured, shadowPass, receiveShadow, nativeCutoff,
                integratedGlint, glintOverlay, localLights, masks, spotShadows, source);
    }

    /**
     * Builds the surface fragment shader with the backend's GLSL version.
     *
     * @param version         GLSL language version
     * @param textured        whether the surface has a base texture
     * @param shadowPass      whether this pass writes shadow depth
     * @param receiveShadow   whether this pass samples directional shadow depth
     * @param nativeCutoff    whether to honor cutout on translucent native layers
     * @param integratedGlint whether to combine vanilla foil with the base color
     * @param glintOverlay    whether this pass writes a foil overlay
     * @param localLights     whether this pass uses local lights
     * @param masks           number of spotlight masks
     * @param spotShadows     number of spotlight maps
     * @param source          user-defined surface function or null
     * @return complete fragment source
     */
    static String surfaceFragment(int version, boolean textured, boolean shadowPass, boolean receiveShadow,
                                  boolean nativeCutoff, boolean integratedGlint, boolean glintOverlay,
                                  boolean localLights, int masks, int spotShadows, String source) {
        return "#version " + version + "\n#define CUSTOM_SURFACE " + (source == null ? "0\n" : "1\n")
                + "#define SHADOW_PASS " + (shadowPass ? "1\n" : "0\n")
                + "#define RECEIVE_SHADOW " + (receiveShadow ? "1\n" : "0\n")
                + "#define NATIVE_CUTOFF " + (nativeCutoff ? "1\n" : "0\n")
                + "#define INTEGRATED_GLINT " + (integratedGlint ? "1\n" : "0\n")
                + "#define GLINT_OVERLAY " + (glintOverlay ? "1\n" : "0\n")
                + "#define LOCAL_LIGHTS " + (localLights ? "1\n" : "0\n")
                + UNIFORMS + SURFACE_UNIFORMS + FRAGMENT_INPUTS + SURFACE_INPUTS
                + (integratedGlint || glintOverlay ? GLINT_UV : "")
                + (integratedGlint ? "uniform sampler2D GlintSampler;\n" : "")
                + (receiveShadow ? SHADOW_LOOKUP : "") + (source == null ? "" : EFFECT_HELPERS + source + "\n")
                + (localLights ? localLightsSource(masks, spotShadows) : "")
                + baseColor(textured, glintOverlay) + SURFACE_MAIN;
    }

    /**
     * Declares only the sampler slots the renderer can bind. GLSL 330 requires static sampler references.
     *
     * @param masks   number of projected textures bound in this variant
     * @param shadows number of spotlight shadows bound in this variant
     * @return local light block and the shading function for those bindings
     */
    private static String localLightsSource(int masks, int shadows) {
        StringBuilder declarations = new StringBuilder();
        StringBuilder maskReads = new StringBuilder();
        StringBuilder shadowReads = new StringBuilder();
        for (int i = 0; i < masks; i++) {
            String name = LOCAL_MASK_NAMES[i];
            declarations.append("uniform sampler2D ").append(name).append(";\n");
            maskReads.append("if (maskSlot == ").append(i + 1).append(") maskColor = textureLod(")
                    .append(name).append(", maskUv, 0.0);\n");
        }
        for (int i = 0; i < shadows; i++) {
            String name = LOCAL_SHADOW_NAMES[i];
            declarations.append("uniform sampler2D ").append(name).append(";\n");
            shadowReads.append("if (shadowSlot == ").append(i + 1).append(") direct *= sceneSpotVisibility(")
                    .append(name).append(", projected, uv, bias, light.shadowSettings.y);\n");
        }
        return declarations + LOCAL_LIGHT_UNIFORMS + LOCAL_LIGHT_LOOKUP.formatted(maskReads, shadowReads);
    }

    /**
     * Starts the fragment main function and reads the straight RGBA base color.
     *
     * @param textured     whether to sample the base texture
     * @param glintOverlay whether the base texture itself uses the animated UV matrix
     * @return opening of the fragment main function
     */
    private static String baseColor(boolean textured, boolean glintOverlay) {
        return textured ? """
                uniform sampler2D Sampler0;
                
                void main() {
                    vec4 sampleColor = texture(Sampler0, %s);
                    if (MaterialFlags.z > 0.5) {
                        sampleColor.rgb = sampleColor.a > 0.0 ? sampleColor.rgb / sampleColor.a : vec3(0.0);
                    }
                    vec4 color = vertexColor * sampleColor;
                """.formatted(glintOverlay ? "sceneGlintUv(vertexUv)" : "vertexUv") : """
                void main() {
                    vec4 color = vertexColor;
                """;
    }
}
