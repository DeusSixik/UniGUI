package dev.sixik.unigui.impl.render.shaders;

import org.intellij.lang.annotations.Language;

/**
 * Version-neutral GLSL sources and glyph-instance layout of the SDF text renderer.
 *
 * <p>Glyphs are rasterized once into a single-channel signed distance field atlas (see
 * {@code SdfGlyphProvider}) and drawn as instanced quads; the fragment shader reconstructs a crisp edge at any scale.
 * Each instance carries four transformed corner positions, an atlas rectangle and one color per corner (so
 * gradient brushes cost nothing extra). Contains only strings and numbers, no OpenGL.</p>
 */
public final class SdfTextShaderSources {
    /** Pixel size the glyphs are rasterized at; runs are scaled from this size. */
    public static final int BASE_PIXEL_SIZE = 48;
    /** Distance field spread in atlas pixels, passed to the shader as {@code SdfSpread}. */
    public static final int SPREAD = 8;
    /** Empty pixels kept between glyphs in the atlas. */
    public static final int ATLAS_PADDING = 2;
    /** Side of one square atlas page in pixels. */
    public static final int ATLAS_SIZE = 1024;
    /** Floats per glyph instance, see {@link #INSTANCE_ATTRIBUTE_COMPONENTS}. */
    public static final int FLOATS_PER_INSTANCE = 32;

    /** Index of the unit-quad {@code Position} attribute. */
    public static final int POSITION_LOCATION = 0;
    /** Index of the first per-instance attribute; the unit quad uses {@link #POSITION_LOCATION}. */
    public static final int FIRST_INSTANCE_LOCATION = 3;

    /**
     * Names of the per-instance attributes in layout order, bound to consecutive locations starting at
     * {@link #FIRST_INSTANCE_LOCATION}.
     */
    public static final String[] INSTANCE_ATTRIBUTE_NAMES = {
            "InstancePosition0", "InstancePosition1", "InstancePosition2", "InstancePosition3",
            "InstanceUv",
            "InstanceColor0", "InstanceColor1", "InstanceColor2", "InstanceColor3"
    };

    /** Component count of each entry of {@link #INSTANCE_ATTRIBUTE_NAMES}; offsets are the running sum. */
    public static final int[] INSTANCE_ATTRIBUTE_COMPONENTS = {3, 3, 3, 3, 4, 4, 4, 4, 4};

    @Language("GLSL")
    public static final String VERTEX = """
            #version 150
            in vec2 Position;
            in vec3 InstancePosition0;
            in vec3 InstancePosition1;
            in vec3 InstancePosition2;
            in vec3 InstancePosition3;
            in vec4 InstanceUv;
            in vec4 InstanceColor0;
            in vec4 InstanceColor1;
            in vec4 InstanceColor2;
            in vec4 InstanceColor3;
            uniform mat4 ModelViewMat;
            uniform mat4 ProjMat;
            out vec2 vertexUV;
            out vec4 vertexColor;
            void main() {
                vec3 left = mix(InstancePosition0, InstancePosition1, Position.y);
                vec3 right = mix(InstancePosition3, InstancePosition2, Position.y);
                vec3 position = mix(left, right, Position.x);
                vertexUV = vec2(
                        mix(InstanceUv.x, InstanceUv.z, Position.x),
                        mix(InstanceUv.y, InstanceUv.w, Position.y));
                vec4 topColor = mix(InstanceColor0, InstanceColor3, Position.x);
                vec4 bottomColor = mix(InstanceColor1, InstanceColor2, Position.x);
                vertexColor = mix(topColor, bottomColor, Position.y);
                gl_Position = ProjMat * ModelViewMat * vec4(position, 1.0);
            }
            """;

    @Language("GLSL")
    public static final String FRAGMENT = """
            #version 150
            uniform sampler2D GlyphAtlas;
            uniform float SdfSpread;
            in vec2 vertexUV;
            in vec4 vertexColor;
            out vec4 fragColor;
            void main() {
                float distance = texture(GlyphAtlas, vertexUV).r;
                vec2 unitRange = vec2(SdfSpread) / vec2(textureSize(GlyphAtlas, 0));
                vec2 screenTexSize = vec2(1.0) / fwidth(vertexUV);
                float screenPxRange = max(0.5 * dot(unitRange, screenTexSize), 1.0);
                float alpha = clamp(screenPxRange * (distance - 0.5) + 0.5, 0.0, 1.0);
                fragColor = vec4(vertexColor.rgb, vertexColor.a * alpha);
            }
            """;

    private SdfTextShaderSources() {
    }
}
