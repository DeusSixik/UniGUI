package dev.sixik.unigui.backend.minecraft_impl;

/**
 * GLSL sources for depth-clipped volumetric integration and depth-aware upsampling.
 */
final class SceneFogShaders {
    /**
     * Maximum number of overlapping visible volumes.
     */
    static final int MAX_VOLUMES = 4;
    /**
     * One camera matrix, two vec4s, then one mat4 and three vec4s per volume, in bytes.
     */
    static final int UNIFORM_BYTES = 96 + MAX_VOLUMES * 112;
    /**
     * Only these flags can appear in a fog shader variant.
     */
    static final int SPOT = 1, MASK = 2, SHADOW = 4;
    /**
     * Camera and volume data shared with the GPU staging code.
     */
    private static final String UNIFORMS = """
            struct FogZone {
                mat4 worldToLocal;
                vec4 colorDensity;
                vec4 shapeEdgeNoise;
                vec4 velocityScatter;
            };
            layout(std140) uniform SceneFog {
                mat4 ClipToWorld;
                vec4 DepthRegion;
                vec4 FogFrame;
                FogZone Zones[%d];
            };
            """.formatted(MAX_VOLUMES);
    /**
     * The source occupies the upper-left fraction of its backing texture.
     * Its reverse depth maps canonical clip z to depth {@code (1-z)/2}.
     */
    private static final String DEPTH = """
            uniform sampler2D FogSceneDepth;
            in vec2 fogUv;
            out vec4 fragColor;
            
            float sceneDepth(vec2 uv) {
                vec2 texUv = vec2(uv.x, uv.y - 1.0) * DepthRegion.xy + vec2(0.0, 1.0);
                float depth = textureLod(FogSceneDepth, texUv, 0.0).r;
                #if FORWARD_DEPTH
                return 1.0 - depth;
                #else
                return depth;
                #endif
            }
            """;
    /**
     * Maps device depth to camera distance and interpolates fog over samples of similar depth.
     */
    private static final String COMPOSITE_BODY = UNIFORMS + DEPTH + SceneDepthShaders.VIEW_DISTANCE + """
            uniform sampler2D FogIntegrated;
            
            float fogDistance(vec2 uv) {
                return viewDistance(sceneDepth(uv), DepthRegion.zw, FogFrame.y > 0.5);
            }
            
            void main() {
                float center = fogDistance(fogUv);
                ivec2 size = textureSize(FogIntegrated, 0);
                vec2 p = fogUv * vec2(size) - 0.5;
                vec2 fraction = fract(p);
                ivec2 first = ivec2(floor(p));
                vec4 result = vec4(0.0);
                float weights = 0.0;
                for (int y = 0; y < 2; y++) {
                    for (int x = 0; x < 2; x++) {
                        ivec2 offset = ivec2(x, y);
                        ivec2 pixel = clamp(first + offset, ivec2(0), size - ivec2(1));
                        vec2 sampleUv = (vec2(pixel) + 0.5) / vec2(size);
                        float distance = fogDistance(sampleUv);
                        float depthTolerance = max(0.035, 0.01 * min(center, distance));
                        float similarity = 1.0 - smoothstep(0.0, 3.0, abs(distance - center) / depthTolerance);
                        vec2 blend = mix(1.0 - fraction, fraction, vec2(offset));
                        float weight = blend.x * blend.y * similarity;
                        result += texelFetch(FogIntegrated, pixel, 0) * weight;
                        weights += weight;
                    }
                }
                fragColor = weights > 0.0001 ? result / weights : vec4(0.0);
            }
            """;

    /**
     * Complete reverse-depth composite source for the GPU backend.
     */
    static final String COMPOSITE = composite(330, false);

    /**
     * @param version      GLSL language version
     * @param forwardDepth whether the sampled depth increases from near to far
     * @return complete depth-aware composite shader
     */
    static String composite(int version, boolean forwardDepth) {
        return header(version, forwardDepth) + COMPOSITE_BODY;
    }

    /**
     * @param version      GLSL language version
     * @param forwardDepth whether the sampled depth needs conversion to reverse depth
     * @return GLSL header shared by fog fragment stages
     */
    private static String header(int version, boolean forwardDepth) {
        return "#version " + version + "\n#define FORWARD_DEPTH " + (forwardDepth ? "1\n" : "0\n");
    }

    /**
     * A trilinear hash field with smooth interpolation for drifting fog density.
     */
    private static final String NOISE = """
            float fogHash(vec3 p) {
                p = fract(p * vec3(0.1031, 0.11369, 0.13787));
                p += dot(p, p.yzx + 19.19);
                return fract((p.x + p.y) * p.z);
            }
            
            float fogNoise(vec3 p) {
                vec3 cell = floor(p);
                vec3 t = smoothstep(vec3(0.0), vec3(1.0), fract(p));
                float lowerY0 = mix(fogHash(cell), fogHash(cell + vec3(1, 0, 0)), t.x);
                float lowerY1 = mix(fogHash(cell + vec3(0, 1, 0)), fogHash(cell + vec3(1, 1, 0)), t.x);
                float upperY0 = mix(fogHash(cell + vec3(0, 0, 1)), fogHash(cell + vec3(1, 0, 1)), t.x);
                float upperY1 = mix(fogHash(cell + vec3(0, 1, 1)), fogHash(cell + vec3(1, 1, 1)), t.x);
                return mix(mix(lowerY0, lowerY1, t.y), mix(upperY0, upperY1, t.y), t.z);
            }
            """;

    /**
     * Intersects the clipped view ray with each local volume. Transforming both ray origin and direction
     * without renormalizing direction preserves its world-distance parameter t. A sphere solves
     * {@code dot(o + t*d, o + t*d) = 1}. A box intersects the three slabs {@code -1 <= o_i + t*d_i <= 1}.
     */
    private static final String INTERSECT = """
            vec3 unproject(vec2 uv, float depth) {
                vec4 h = ClipToWorld * vec4(uv * 2.0 - 1.0, 1.0 - 2.0 * depth, 1.0);
                return h.xyz / h.w;
            }
            
            vec2 fogInterval(vec3 origin, vec3 ray, float limit, bool sphere) {
                vec2 interval = vec2(0.0, limit);
                if (sphere) {
                    float a = dot(ray, ray);
                    float b = dot(origin, ray);
                    float c = dot(origin, origin) - 1.0;
                    float discriminant = b * b - a * c;
                    if (discriminant <= 0.0 || a <= 0.0) return vec2(0.0);
                    float root = sqrt(discriminant);
                    interval.x = max(0.0, (-b - root) / a);
                    interval.y = min(limit, (-b + root) / a);
                } else {
                    for (int axis = 0; axis < 3; axis++) {
                        if (ray[axis] == 0.0) {
                            if (abs(origin[axis]) > 1.0) return vec2(0.0);
                        } else {
                            float t1 = (-1.0 - origin[axis]) / ray[axis];
                            float t2 = (1.0 - origin[axis]) / ray[axis];
                            interval.x = max(interval.x, min(t1, t2));
                            interval.y = min(interval.y, max(t1, t2));
                        }
                    }
                }
                return interval.y > interval.x ? interval : vec2(0.0);
            }
            """;

    /**
     * Samples the same cone, projected texture and reverse depth map as surface lighting.
     * The volume uses isotropic scattering and a single depth tap per integration step.
     */
    private static final String SPOTLIGHT = """
            #if FOG_MASK
            uniform sampler2D FogSpotMask;
            #endif
            #if FOG_SHADOW
            uniform sampler2D FogSpotDepth;
            #endif
            
            vec3 fogSpotColor(vec3 point) {
                SceneLocalLight light = LocalLights[int(FogFrame.w)];
                vec3 delta = (point - light.positionRange.xyz) / light.positionRange.w;
                float distanceRatio = length(delta);
                if (distanceRatio <= 0.0 || distanceRatio >= 1.0) return vec3(0.0);
                float angle = dot(light.directionOuter.xyz, delta / distanceRatio);
                float outer = light.directionOuter.w;
                if (angle < outer) return vec3(0.0);
                float inner = light.coneSlots.x;
                float cone = inner > outer ? smoothstep(outer, inner, angle) : 1.0;
                float falloff = 1.0 - distanceRatio;
                vec3 color = light.radiance.rgb * (cone * falloff * falloff);
                #if FOG_MASK || FOG_SHADOW
                vec4 clip = light.projection * vec4(point, 1.0);
                if (clip.w <= 0.0) return vec3(0.0);
                vec3 projected = clip.xyz / clip.w;
                vec2 uv = projected.xy * 0.5 + 0.5;
                if (any(lessThan(uv, vec2(0.0))) || any(greaterThan(uv, vec2(1.0)))) return vec3(0.0);
                #endif
                #if FOG_MASK
                vec2 maskUv = light.maskUv.xy + vec2(uv.x, 1.0 - uv.y) * light.maskUv.zw;
                vec4 mask = textureLod(FogSpotMask, maskUv, 0.0);
                color *= mask.rgb * mix(mask.a, 1.0, light.coneSlots.w);
                #endif
                #if FOG_SHADOW
                float depth = 0.5 - projected.z * 0.5;
                if (depth > 0.0 && depth < 1.0
                        && textureLod(FogSpotDepth, uv, 0.0).r > depth + light.shadowSettings.x) {
                    color *= 1.0 - light.shadowSettings.y;
                }
                #endif
                return color;
            }
            """;

    /**
     * Integrates fog front to back. For constant extinction sigma over a segment of length ds,
     * alpha is {@code 1 - exp(-sigma * ds)} and outgoing radiance adds {@code T * alpha * L}, with
     * T the incoming transmittance and L the density-weighted haze and spotlight color.
     */
    private static final String MARCH = """
            void main() {
                vec3 origin = unproject(fogUv, 1.0);
                vec3 delta = unproject(fogUv, sceneDepth(fogUv)) - origin;
                float limit = length(delta);
                fragColor = vec4(0.0);
                if (limit <= 0.00001) return;
                vec3 ray = delta / limit;
                vec3 origins[%1$d];
                vec3 directions[%1$d];
                vec2 intervals[%1$d];
                float cursor = limit;
                float last = 0.0;
                float lengthSum = 0.0;
                int count = int(FogFrame.z);
                for (int i = 0; i < count; i++) {
                    mat4 worldToLocal = Zones[i].worldToLocal;
                    origins[i] = (worldToLocal * vec4(origin, 1.0)).xyz;
                    directions[i] = (worldToLocal * vec4(ray, 0.0)).xyz;
                    vec2 hit = fogInterval(origins[i], directions[i], limit, Zones[i].shapeEdgeNoise.x > 0.5);
                    intervals[i] = hit;
                    if (hit.y <= hit.x) continue;
                    cursor = min(cursor, hit.x);
                    last = max(last, hit.y);
                    lengthSum += hit.y - hit.x;
                }
                if (cursor >= last) return;
                float stepSize = max(0.00001, min(lengthSum, last - cursor) / 24.0);
                float transmittance = 1.0;
                vec3 color = vec3(0.0);
                // Extra iterations let disjoint volumes start and end on their exact interval bounds.
                for (int step = 0; step < 24 + 2 * %1$d; step++) {
                    if (cursor >= last || transmittance < 0.005) break;
                    float boundary = last;
                    bool inside = false;
                    for (int i = 0; i < count; i++) {
                        vec2 hit = intervals[i];
                        if (cursor < hit.x) {
                            boundary = min(boundary, hit.x);
                        } else if (cursor < hit.y) {
                            boundary = min(boundary, hit.y);
                            inside = true;
                        }
                    }
                    if (!inside) {
                        cursor = boundary;
                        continue;
                    }
                    float next = min(boundary, cursor + stepSize);
                    if (next <= cursor) break;
                    float t = (cursor + next) * 0.5;
                    float density = 0.0;
                    float scattering = 0.0;
                    vec3 tint = vec3(0.0);
                    for (int i = 0; i < count; i++) {
                        if (t < intervals[i].x || t >= intervals[i].y) continue;
                        FogZone zone = Zones[i];
                        vec3 local = origins[i] + t * directions[i];
                        vec3 extent = abs(local);
                        float r = zone.shapeEdgeNoise.x > 0.5 ? length(local) : max(extent.x, max(extent.y, extent.z));
                        float edge = zone.shapeEdgeNoise.y;
                        float weight = edge > 0.00001 ? 1.0 - smoothstep(1.0 - edge, 1.0, r) : 1.0;
                        if (zone.shapeEdgeNoise.z > 0.0) {
                            vec3 noisePos = (local - zone.velocityScatter.xyz * FogFrame.x) * zone.shapeEdgeNoise.w;
                            weight *= mix(1.0, fogNoise(noisePos), zone.shapeEdgeNoise.z);
                        }
                        float sigma = zone.colorDensity.a * weight;
                        density += sigma;
                        tint += zone.colorDensity.rgb * sigma;
                        scattering += zone.velocityScatter.w * sigma;
                    }
                    if (density > 0.0) {
                        #if FOG_SPOT
                        if (scattering > 0.0) tint += scattering * fogSpotColor(origin + ray * t);
                        #endif
                        float alpha = 1.0 - exp(-density * (next - cursor));
                        color += clamp(tint / density, 0.0, 1.0) * (transmittance * alpha);
                        transmittance *= 1.0 - alpha;
                    }
                    cursor = next;
                }
                fragColor = vec4(color, 1.0 - transmittance);
            }
            """.formatted(MAX_VOLUMES);

    /**
     * Prevents construction of a shader source library.
     */
    private SceneFogShaders() {
    }

    /**
     * Projects a triangle over the integration target or the active part of the scene target.
     *
     * @param composite whether to limit the shape to the scene's upper-left viewport
     * @return complete GLSL vertex source
     */
    static String vertex(boolean composite) {
        return vertex(330, composite);
    }

    /**
     * @param version   GLSL language version
     * @param composite whether to project into the scene viewport
     * @return complete vertex source
     */
    static String vertex(int version, boolean composite) {
        return "#version " + version + "\n" + (composite ? UNIFORMS : "") + """
                in vec3 Position;
                out vec2 fogUv;
                void main() {
                    fogUv = Position.xy * 0.5 + 0.5;
                    gl_Position = vec4(Position, 1.0);
                """ + (composite ? """
                    gl_Position.xy = (fogUv - vec2(0.0, 1.0)) * DepthRegion.xy * 2.0 + vec2(-1.0, 1.0);
                """ : "") + "}\n";
    }

    /**
     * Assembles the integration program with only the active spotlight bindings.
     *
     * @param flags spotlight, mask and depth availability bits
     * @return complete GLSL fragment source
     */
    static String integrate(int flags) {
        return integrate(330, false, flags);
    }

    /**
     * @param version      GLSL language version
     * @param forwardDepth whether sampled depth increases from near to far
     * @param flags        spotlight, mask and shadow bindings
     * @return complete integration source
     */
    static String integrate(int version, boolean forwardDepth, int flags) {
        return header(version, forwardDepth) + "#define FOG_SPOT " + ((flags & SPOT) != 0 ? "1\n" : "0\n")
                + "#define FOG_MASK " + ((flags & MASK) != 0 ? "1\n" : "0\n")
                + "#define FOG_SHADOW " + ((flags & SHADOW) != 0 ? "1\n" : "0\n")
                + UNIFORMS + DEPTH + NOISE + INTERSECT
                + ((flags & SPOT) != 0 ? UiMesh3DShaders.LOCAL_LIGHT_UNIFORMS + SPOTLIGHT : "") + MARCH;
    }
}
