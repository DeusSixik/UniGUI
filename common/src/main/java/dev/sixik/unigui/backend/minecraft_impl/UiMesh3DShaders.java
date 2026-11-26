package dev.sixik.unigui.backend.minecraft_impl;

import dev.sixik.unigui.impl.render.SceneMeshData;

/**
 * GLSL-проходы для индексированных мешей. Блок должен совпадать с {@link SceneMeshData}.
 */
final class UiMesh3DShaders {
    /**
     * Максимальное число матриц в одном инстансированном вызове.
     */
    static final int MAX_INSTANCES = 32;
    /**
     * Размер блока std140 для матриц экземпляров в байтах.
     */
    static final int INSTANCE_BYTES = MAX_INSTANCES * SceneMeshData.CLIP_BYTES;
    /**
     * Общий std140-блок вершинной и фрагментной стадий.
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
     * Преобразует локальные координаты в clip space и нормали в мировые координаты.
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
            
            void main() {
                #if INSTANCED
                vec4 p = Instances[INSTANCE_INDEX] * vec4(Position, 1.0);
                worldNormal = vec3(0.0);
                #else
                vec4 p = Clip * vec4(Position, 1.0);
                worldNormal = NormalTransform * Normal.xyz;
                #endif
                // Вершины проецируются на левый верхний прямоугольник общей цели.
                p.x = p.x * Viewport.x + p.w * (Viewport.x - 1.0);
                p.y = p.y * Viewport.y + p.w * (1.0 - Viewport.y);
                #if REVERSE_DEPTH
                p.z = -p.z;
                #endif
                if (Viewport.z > 0.5) p.z = (p.z + p.w) * 0.5;
                gl_Position = p;
            
                vertexUv = vec2(UV0.x, MaterialFlags.y > 0.5 ? 1.0 - UV0.y : UV0.y);
                vertexColor = Color * Tint;
            }
            """;

    /**
     * Запись альфы и освещения после получения исходного цвета.
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
     * Имена интерфейса фрагментной стадии.
     */
    private static final String FRAGMENT_INPUTS = """
            in vec2 vertexUv;
            in vec4 vertexColor;
            in vec3 worldNormal;
            
            out vec4 fragColor;
            
            """;

    private UiMesh3DShaders() {
    }

    /**
     * Собирает вершинный шейдер одиночного или инстансированного меша.
     *
     * @param version       версия GLSL.
     * @param reversedDepth требуется ли обратная глубина.
     * @param instanced     читать ли матрицы из массива экземпляров.
     * @return полный GLSL исходник вершинной стадии.
     */
    static String vertex(int version, boolean reversedDepth, boolean instanced) {
        return "#version " + version + "\n#define REVERSE_DEPTH "
                + (reversedDepth ? "1\n" : "0\n") + "#define INSTANCED " + (instanced ? "1\n" : "0\n")
                + "#define MAX_INSTANCES " + MAX_INSTANCES + "\n" + UNIFORMS + VERTEX_BODY;
    }

    /**
     * Собирает фрагментный шейдер с Ламбертом и premultiplied alpha для композиции с 2D.
     *
     * @param version  версия GLSL.
     * @param textured требуется ли сэмплер Sampler0.
     * @return полный GLSL исходник нужной стадии.
     */
    static String fragment(int version, boolean textured) {
        return "#version " + version + "\n" + UNIFORMS + FRAGMENT_INPUTS
                + (textured ? """
                uniform sampler2D Sampler0;
                
                void main() {
                    vec4 sampleColor = texture(Sampler0, vertexUv);
                    if (MaterialFlags.z > 0.5) {
                        sampleColor.rgb = sampleColor.a > 0.0 ? sampleColor.rgb / sampleColor.a : vec3(0.0);
                    }
                    vec4 color = vertexColor * sampleColor;
                """ : """
                void main() {
                    vec4 color = vertexColor;
                """) + SHADE;
    }
}
