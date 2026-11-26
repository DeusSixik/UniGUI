package dev.sixik.unigui.impl.render.shaders;

import dev.sixik.unigui.api.render.shaders.ShaderUniform;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Конвертация обычных uniform-переменных пользовательских шейдеров в std140-блок (нужна для Vulkan).
 */
class GlslUniformBlocksTest {
    private static final String VERTEX = """
            #version 150
            in vec3 Position;
            out vec2 uv;
            uniform vec2 ScreenSize;
            uniform vec4 SquareVertex;
            void main() {
                uv = Position.xy * ScreenSize;
                gl_Position = vec4(Position, 1.0) + SquareVertex;
            }
            """;

    private static final String FRAGMENT = """
            #version 150
            uniform sampler2D SourceTexture;
            uniform float Time; // comment with uniform float Fake;
            /* uniform vec3 AlsoFake; */
            uniform vec2 ScreenSize;
            in vec2 uv;
            out vec4 FragColor;
            void main() {
                FragColor = texture(SourceTexture, uv) * Time;
            }
            """;

    @Test
    void movesPlainUniformsIntoOneSharedBlock() {
        GlslUniformBlocks.Result result = GlslUniformBlocks.convert(VERTEX, FRAGMENT);

        assertEquals(List.of("ScreenSize", "SquareVertex", "Time"), names(result));
        assertEquals(List.of("SourceTexture"), result.samplers());
        String block = "layout(std140) uniform UniGuiUniforms { vec2 ScreenSize; vec4 SquareVertex; float Time; };";
        assertTrue(result.vertex().contains(block), result.vertex());
        assertTrue(result.fragment().contains(block), result.fragment());
        assertFalse(result.fragment().contains("uniform float Time;\n"));
        assertTrue(result.fragment().contains("uniform sampler2D SourceTexture;"));
        assertTrue(result.fragment().contains("// comment with uniform float Fake;"), "comments stay untouched");
    }

    @Test
    void upgradesVersionAndKeepsLineCount() {
        GlslUniformBlocks.Result result = GlslUniformBlocks.convert(VERTEX, FRAGMENT);

        assertTrue(result.vertex().startsWith("#version 330\n"));
        assertTrue(result.fragment().startsWith("#version 330\n"));
        assertEquals(lines(VERTEX), lines(result.vertex()));
        assertEquals(lines(FRAGMENT), lines(result.fragment()));
    }

    @Test
    void addsVersionWhenMissingAndStripsBom() {
        GlslUniformBlocks.Result result = GlslUniformBlocks.convert(null,
                "﻿uniform float A;\nout vec4 c;\nvoid main() { c = vec4(A); }\n");

        assertNull(result.vertex());
        assertTrue(result.fragment().startsWith("#version 330\n"));
        assertFalse(result.fragment().contains("﻿"));
    }

    @Test
    void keepsNewerVersionAndProfile() {
        GlslUniformBlocks.Result result = GlslUniformBlocks.convert(null,
                "#version 150 core\nuniform float A;\nvoid main() {}\n");
        assertTrue(result.fragment().startsWith("#version 330 core\n"));

        GlslUniformBlocks.Result newer = GlslUniformBlocks.convert(null, "#version 410\nuniform float A;\nvoid main() {}\n");
        assertTrue(newer.fragment().startsWith("#version 410\n"));
    }

    @Test
    void computesStd140Offsets() {
        GlslUniformBlocks.Result result = GlslUniformBlocks.convert(null, """
                #version 330
                uniform float a;
                uniform vec3 b;
                uniform float c;
                uniform vec2 d;
                uniform mat4 e;
                uniform float f[3];
                uniform mat3 g;
                uniform int h, i;
                void main() {}
                """);

        assertEquals(List.of(0, 16, 28, 32, 48, 112, 160, 208, 212), offsets(result));
        assertEquals(224, result.blockSize());
    }

    @Test
    void ignoresDeclarationsInsideFunctionsAndExistingBlocks() {
        GlslUniformBlocks.Result result = GlslUniformBlocks.convert(null, """
                #version 330
                layout(std140) uniform Existing {
                    float inside;
                };
                uniform float outside;
                void main() {}
                """);

        assertEquals(List.of("outside"), names(result));
        assertTrue(result.fragment().contains("uniform Existing {"));
    }

    @Test
    void noBlockWithoutPlainUniforms() {
        GlslUniformBlocks.Result result = GlslUniformBlocks.convert(null,
                "#version 150\nuniform sampler2D Tex;\nvoid main() {}\n");

        assertFalse(result.hasBlock());
        assertFalse(result.fragment().contains("UniGuiUniforms"));
        assertEquals(List.of("Tex"), result.samplers());
    }

    @Test
    void rejectsUnsupportedDeclarations() {
        assertThrows(IllegalArgumentException.class,
                () -> GlslUniformBlocks.convert(null, "uniform float a = 1.0;\nvoid main() {}"));
        assertThrows(IllegalArgumentException.class,
                () -> GlslUniformBlocks.convert(null, "uniform MyStruct s;\nvoid main() {}"));
        assertThrows(IllegalArgumentException.class,
                () -> GlslUniformBlocks.convert("uniform float a;\nvoid main() {}", "uniform vec2 a;\nvoid main() {}"));
        assertThrows(IllegalArgumentException.class,
                () -> GlslUniformBlocks.convert(null, "#version 300 es\nvoid main() {}"));
    }

    @Test
    void writesValuesByStd140Layout() {
        GlslUniformBlocks.Result result = GlslUniformBlocks.convert(null, """
                #version 330
                uniform float time;
                uniform vec3 color;
                uniform int mode;
                uniform bool flag;
                uniform mat4 matrix;
                uniform float weights[2];
                uniform vec2 missing;
                void main() {}
                """);
        float[] matrix = new float[16];
        for (int i = 0; i < 16; i++) matrix[i] = i;
        Map<String, ShaderUniform> values = Map.of(
                "time", ShaderUniform.float1(2.5f),
                "color", ShaderUniform.vec3(1, 2, 3),
                "mode", ShaderUniform.int1(7),
                "flag", ShaderUniform.float1(5.0f),
                "matrix", ShaderUniform.mat4(matrix),
                "weights", ShaderUniform.vec2(0.25f, 0.75f));

        ByteBuffer buffer = ByteBuffer.allocateDirect(result.blockSize() + 8).order(ByteOrder.nativeOrder());
        buffer.putFloat(-1.0f).putFloat(-1.0f);
        result.write(values, buffer);

        assertEquals(8 + result.blockSize(), buffer.position());
        int base = 8;
        assertEquals(2.5f, buffer.getFloat(base + offset(result, "time")));
        assertEquals(3.0f, buffer.getFloat(base + offset(result, "color") + 8));
        assertEquals(7, buffer.getInt(base + offset(result, "mode")));
        assertEquals(1, buffer.getInt(base + offset(result, "flag")));
        assertEquals(5.0f, buffer.getFloat(base + offset(result, "matrix") + 16 + 4), "column 1, row 1");
        assertEquals(0.75f, buffer.getFloat(base + offset(result, "weights") + 16), "array stride is 16");
        assertEquals(0.0f, buffer.getFloat(base + offset(result, "missing")));
        assertEquals(-1.0f, buffer.getFloat(0), "bytes before the block are untouched");
    }

    private static List<String> names(GlslUniformBlocks.Result result) {
        return result.members().stream().map(GlslUniformBlocks.Member::name).toList();
    }

    private static List<Integer> offsets(GlslUniformBlocks.Result result) {
        return result.members().stream().map(GlslUniformBlocks.Member::offset).toList();
    }

    private static int offset(GlslUniformBlocks.Result result, String name) {
        return result.members().stream().filter(m -> m.name().equals(name)).findFirst().orElseThrow().offset();
    }

    private static long lines(String source) {
        return source.chars().filter(c -> c == '\n').count();
    }
}
