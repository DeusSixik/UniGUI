package dev.sixik.unigui.impl.render.shaders;

import org.intellij.lang.annotations.Language;

/**
 * Version-neutral GLSL sources and instance layout of the SDF shape renderer.
 *
 * <p>One quad is drawn per primitive (rounded rectangle, ellipse or line) and the fragment shader evaluates a signed
 * distance field, so edges are anti-aliased with {@code fwidth} instead of being approximated with geometry. The
 * same sources are used by every version backend (Minecraft 1.20 / 1.21 and Cleanroom 1.12.2); the backends only
 * own the OpenGL calls. This class has no OpenGL dependency on purpose: it contains strings and numbers.</p>
 *
 * <p>Two program variants exist. The basic program takes the shape parameters as uniforms and is drawn once per
 * command. The instanced program reads the same parameters from per-instance vertex attributes (packed as
 * {@link #INSTANCE_FLOATS} floats) so a whole batch is one draw call.</p>
 */
public final class SdfShapeShaderSources {
    /** Shape type: rounded rectangle (radius {@code 0} gives a plain rectangle). */
    public static final int SHAPE_ROUNDED_RECT = 0;
    /** Shape type: circle or ellipse inscribed in the quad. */
    public static final int SHAPE_ELLIPSE = 1;
    /** Shape type: line segment with round caps. */
    public static final int SHAPE_LINE = 2;

    /** Extra pixels around a shape so its anti-aliased edge is not clipped by the quad. */
    public static final float AA_PAD = 1.0f;
    /** Smallest stroke width passed to the shader, avoids division by zero in the coverage functions. */
    public static final float MIN_STROKE_WIDTH = 0.001f;

    /** Floats per instance in the instanced program, see {@link #INSTANCED_ATTRIBUTE_COMPONENTS}. */
    public static final int INSTANCE_FLOATS = 30;

    /** Attribute names of the basic program; the array index is the attribute location. */
    public static final String[] BASIC_ATTRIBUTE_NAMES = {"Position", "UV0"};

    /** Attribute names of the instanced program; the array index is the attribute location. */
    public static final String[] INSTANCED_ATTRIBUTE_NAMES = {
            "Position",
            "InstancePosition0", "InstancePosition1", "InstancePosition2", "InstancePosition3",
            "InstanceLocalBounds", "InstanceColor", "InstanceSize",
            "InstanceLineStart", "InstanceLineEnd", "InstanceShape", "InstanceFlags"
    };

    /**
     * Component count of each per-instance attribute, starting at location {@code 1} (location {@code 0} is the
     * unit quad). Offsets inside an instance are the running sum of the previous entries.
     */
    public static final int[] INSTANCED_ATTRIBUTE_COMPONENTS = {3, 3, 3, 3, 4, 4, 2, 2, 2, 2, 2};

    @Language("GLSL")
    public static final String VERTEX = """
            #version 150

            in vec3 Position;
            in vec2 UV0;

            uniform mat4 ModelViewMat;
            uniform mat4 ProjMat;

            out vec2 localPos;

            void main() {
                gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
                localPos = UV0;
            }
            """;

    @Language("GLSL")
    public static final String FRAGMENT = """
            #version 150

            in vec2 localPos;
            out vec4 fragColor;

            uniform vec4 Color;
            uniform vec2 Size;
            uniform vec2 LineStart;
            uniform vec2 LineEnd;
            uniform float Radius;
            uniform float StrokeWidth;
            uniform int ShapeType;
            uniform int IsStroke;

            float sdRoundedBox(vec2 point, vec2 halfSize, float radius) {
                float safeRadius = clamp(radius, 0.0, min(halfSize.x, halfSize.y));
                vec2 q = abs(point) - halfSize + vec2(safeRadius);
                return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - safeRadius;
            }

            float sdCircle(vec2 point, float radius) {
                return length(point) - max(radius, 0.0001);
            }

            float sdEllipse(vec2 point, vec2 radius) {
                vec2 safeRadius = max(radius, vec2(0.0001));
                vec2 normalized = point / safeRadius;
                return (length(normalized) - 1.0) * min(safeRadius.x, safeRadius.y);
            }

            float sdSegment(vec2 point, vec2 a, vec2 b) {
                vec2 pa = point - a;
                vec2 ba = b - a;
                float denom = max(dot(ba, ba), 0.0001);
                float h = clamp(dot(pa, ba) / denom, 0.0, 1.0);
                return length(pa - ba * h);
            }

            float aaWidth(float distanceToEdge) {
                return max(fwidth(distanceToEdge), 0.0001);
            }

            float edgeCoverage(float distanceToEdge) {
                float aa = aaWidth(distanceToEdge);
                return clamp(0.5 - distanceToEdge / aa, 0.0, 1.0);
            }

            float fillCoverage(float distanceToEdge) {
                return edgeCoverage(distanceToEdge);
            }

            float innerStrokeCoverage(float distanceToEdge, float width) {
                float aa = aaWidth(distanceToEdge);
                float outer = clamp(0.5 - distanceToEdge / aa, 0.0, 1.0);
                float inner = clamp(0.5 - (-distanceToEdge - width) / aa, 0.0, 1.0);
                return outer * inner;
            }

            void main() {
                float distanceToEdge;
                if (ShapeType == 1) {
                    vec2 radius = Size * 0.5;
                    vec2 point = localPos - radius;
                    distanceToEdge = abs(Size.x - Size.y) <= 0.001
                            ? sdCircle(point, radius.x)
                            : sdEllipse(point, radius);
                } else if (ShapeType == 2) {
                    distanceToEdge = sdSegment(localPos, LineStart, LineEnd) - max(StrokeWidth, 0.0001) * 0.5;
                } else {
                    distanceToEdge = sdRoundedBox(localPos - Size * 0.5, Size * 0.5, Radius);
                }

                float coverage = (ShapeType == 2 || IsStroke == 0)
                        ? fillCoverage(distanceToEdge)
                        : innerStrokeCoverage(distanceToEdge, max(StrokeWidth, 0.0001));

                float alpha = Color.a * coverage;
                if (alpha <= 0.001) {
                    discard;
                }

                fragColor = vec4(Color.rgb, alpha);
            }
            """;

    /**
     * Fragment shader of the instanced program. All shape parameters arrive as flat varyings from the vertex
     * shader, so only the declarations differ from {@link #FRAGMENT}.
     */
    public static final String INSTANCED_FRAGMENT = FRAGMENT
            .replace("uniform vec4 Color;", "flat in vec4 Color;")
            .replace("uniform vec2 Size;", "flat in vec2 Size;")
            .replace("uniform vec2 LineStart;", "flat in vec2 LineStart;")
            .replace("uniform vec2 LineEnd;", "flat in vec2 LineEnd;")
            .replace("uniform float Radius;", "flat in float Radius;")
            .replace("uniform float StrokeWidth;", "flat in float StrokeWidth;")
            .replace("uniform int ShapeType;", "flat in int ShapeType;")
            .replace("uniform int IsStroke;", "flat in int IsStroke;");

    @Language("GLSL")
    public static final String INSTANCED_VERTEX = """
            #version 150

            in vec2 Position;
            in vec3 InstancePosition0;
            in vec3 InstancePosition1;
            in vec3 InstancePosition2;
            in vec3 InstancePosition3;
            in vec4 InstanceLocalBounds;
            in vec4 InstanceColor;
            in vec2 InstanceSize;
            in vec2 InstanceLineStart;
            in vec2 InstanceLineEnd;
            in vec2 InstanceShape;
            in vec2 InstanceFlags;

            uniform mat4 ModelViewMat;
            uniform mat4 ProjMat;

            out vec2 localPos;
            flat out vec4 Color;
            flat out vec2 Size;
            flat out vec2 LineStart;
            flat out vec2 LineEnd;
            flat out float Radius;
            flat out float StrokeWidth;
            flat out int ShapeType;
            flat out int IsStroke;

            void main() {
                vec3 left = mix(InstancePosition0, InstancePosition1, Position.y);
                vec3 right = mix(InstancePosition3, InstancePosition2, Position.y);
                vec3 position = mix(left, right, Position.x);
                gl_Position = ProjMat * ModelViewMat * vec4(position, 1.0);
                localPos = mix(InstanceLocalBounds.xy, InstanceLocalBounds.zw, Position);
                Color = InstanceColor;
                Size = InstanceSize;
                LineStart = InstanceLineStart;
                LineEnd = InstanceLineEnd;
                Radius = InstanceShape.x;
                StrokeWidth = InstanceShape.y;
                ShapeType = int(InstanceFlags.x + 0.5);
                IsStroke = int(InstanceFlags.y + 0.5);
            }
            """;

    private SdfShapeShaderSources() {
    }
}
