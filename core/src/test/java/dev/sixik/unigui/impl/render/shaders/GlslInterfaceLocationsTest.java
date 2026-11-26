package dev.sixik.unigui.impl.render.shaders;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Расстановка явных location у входов и выходов шейдеров (нужна Minecraft 26.3: GLSL идёт через SPIR-V).
 */
class GlslInterfaceLocationsTest {
    private static final String VERTEX = """
            #version 330
            in vec3 Position;
            in mat4 InstanceMatrix;
            in vec4 Color;
            out vec2 uv;
            flat out int shape;
            out vec4 color;
            void main() {
                // out vec4 notADeclaration;
                uv = Position.xy;
                shape = 1;
                color = Color;
                gl_Position = InstanceMatrix * vec4(Position, 1.0);
            }
            """;
    private static final String FRAGMENT = """
            #version 330
            in vec4 color;
            flat in int shape;
            in vec2 uv;
            out vec4 FragColor;
            float helper(in float x) { return x; }
            void main() {
                FragColor = color * helper(uv.x) + float(shape);
            }
            """;

    @Test
    void vertexInputsAreSequentialAndMatricesTakeColumns() {
        String vertex = GlslInterfaceLocations.assign(VERTEX, FRAGMENT).vertex();
        assertTrue(vertex.contains("layout(location = 0) in vec3 Position;"), vertex);
        assertTrue(vertex.contains("layout(location = 1) in mat4 InstanceMatrix;"), vertex);
        assertTrue(vertex.contains("layout(location = 5) in vec4 Color;"), vertex);
    }

    @Test
    void varyingsMatchByNameAcrossStages() {
        GlslInterfaceLocations.Result result = GlslInterfaceLocations.assign(VERTEX, FRAGMENT);
        assertTrue(result.vertex().contains("layout(location = 0) out vec2 uv;"), result.vertex());
        assertTrue(result.vertex().contains("layout(location = 1) flat out int shape;"), result.vertex());
        assertTrue(result.vertex().contains("layout(location = 2) out vec4 color;"), result.vertex());
        assertTrue(result.fragment().contains("layout(location = 2) in vec4 color;"), result.fragment());
        assertTrue(result.fragment().contains("layout(location = 1) flat in int shape;"), result.fragment());
        assertTrue(result.fragment().contains("layout(location = 0) in vec2 uv;"), result.fragment());
        assertTrue(result.fragment().contains("layout(location = 0) out vec4 FragColor;"), result.fragment());
    }

    @Test
    void commentsFunctionParametersAndBodiesAreUntouched() {
        GlslInterfaceLocations.Result result = GlslInterfaceLocations.assign(VERTEX, FRAGMENT);
        assertTrue(result.vertex().contains("// out vec4 notADeclaration;"), result.vertex());
        assertTrue(result.fragment().contains("float helper(in float x)"), result.fragment());
    }

    @Test
    void extensionIsAddedOnceAfterVersion() {
        GlslInterfaceLocations.Result result = GlslInterfaceLocations.assign(VERTEX, FRAGMENT);
        assertTrue(result.vertex().startsWith("#version 330\n" + GlslInterfaceLocations.EXTENSION + "\n"), result.vertex());
        String again = GlslInterfaceLocations.assign(result.vertex(), result.fragment()).vertex();
        assertEquals(result.vertex(), again, "already processed sources stay unchanged");
    }

    @Test
    void explicitLocationsAreKeptAndNotReused() {
        String vertex = """
                #version 330
                layout(location = 0) in vec3 Position;
                in vec2 UV;
                layout(location = 1) out vec2 uv;
                out vec4 tint;
                void main() { uv = UV; tint = vec4(1.0); gl_Position = vec4(Position, 1.0); }
                """;
        String fragment = """
                #version 330
                in vec4 tint;
                in vec2 uv;
                layout(location = 0) out vec4 FragColor;
                void main() { FragColor = tint * uv.x; }
                """;
        GlslInterfaceLocations.Result result = GlslInterfaceLocations.assign(vertex, fragment);
        assertTrue(result.vertex().contains("layout(location = 1) in vec2 UV;"), result.vertex());
        assertTrue(result.vertex().contains("layout(location = 0) out vec4 tint;"), result.vertex());
        assertTrue(result.fragment().contains("layout(location = 0) in vec4 tint;"), result.fragment());
        assertTrue(result.fragment().contains("layout(location = 1) in vec2 uv;"), result.fragment());
        assertFalse(result.fragment().contains("location = 0, location"), result.fragment());
    }

    @Test
    void layoutWithoutLocationGetsLocationArgument() {
        String fragment = """
                #version 330
                layout(index = 0) out vec4 FragColor;
                void main() { FragColor = vec4(1.0); }
                """;
        String result = GlslInterfaceLocations.assign("#version 330\nvoid main() {}\n", fragment).fragment();
        assertTrue(result.contains("layout(location = 0, index = 0) out vec4 FragColor;"), result);
    }
}
