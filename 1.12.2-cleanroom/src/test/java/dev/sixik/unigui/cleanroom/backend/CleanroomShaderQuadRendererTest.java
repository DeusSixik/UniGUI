package dev.sixik.unigui.cleanroom.backend;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CleanroomShaderQuadRendererTest {
    @Test
    void component_readsValuesAndPadsWithZero() {
        float[] values = {1.0f, 2.0f};
        assertEquals(1.0f, CleanroomShaderQuadRenderer.component(values, 0));
        assertEquals(2.0f, CleanroomShaderQuadRenderer.component(values, 1));
        assertEquals(0.0f, CleanroomShaderQuadRenderer.component(values, 2));
        assertEquals(0.0f, CleanroomShaderQuadRenderer.component(null, 0));
    }

    @Test
    void defaultVertexShader_matchesTheContractOfUserFragmentShaders() {
        String source = CleanroomShaderQuadRenderer.DEFAULT_VERTEX_SOURCE;
        assertTrue(source.startsWith("#version 150"));
        assertTrue(source.contains("in vec3 Position;"), "attribute 0 is bound to Position");
        assertTrue(source.contains("out vec2 uv;"), "fragment shaders read uv in -1..1");
        assertTrue(source.contains("uv = Position.xy;"));
        assertTrue(source.contains("uniform vec2 ScreenSize;"));
    }
}