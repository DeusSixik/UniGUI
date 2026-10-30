package dev.sixik.unigui.cleanroom.backend;

import dev.sixik.unigui.cleanroom.backend.CleanroomGlCapabilities.ShaderTier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CleanroomGlCapabilitiesTest {
    @Test
    void parseVersion_vendorStrings() {
        assertArrayEquals(new int[]{4, 6}, CleanroomGlCapabilities.parseVersion("4.6.0 NVIDIA 551.23"));
        assertArrayEquals(new int[]{3, 2}, CleanroomGlCapabilities.parseVersion("3.2 Mesa 23.1.4"));
        assertArrayEquals(new int[]{2, 1}, CleanroomGlCapabilities.parseVersion("2.1.2 INTEL-Mac"));
        assertArrayEquals(new int[]{3, 0}, CleanroomGlCapabilities.parseVersion("OpenGL ES 3.0 Mesa"));
    }

    @Test
    void parseVersion_garbageIsZero() {
        assertArrayEquals(new int[]{0, 0}, CleanroomGlCapabilities.parseVersion(null));
        assertArrayEquals(new int[]{0, 0}, CleanroomGlCapabilities.parseVersion(""));
        assertArrayEquals(new int[]{0, 0}, CleanroomGlCapabilities.parseVersion("no digits here"));
        assertArrayEquals(new int[]{0, 0}, CleanroomGlCapabilities.parseVersion("7"));
    }

    @Test
    void selectTier_modernContext_isInstanced() {
        assertEquals(ShaderTier.INSTANCED, CleanroomGlCapabilities.selectTier(4, 6, true, true, true));
        assertEquals(ShaderTier.INSTANCED, CleanroomGlCapabilities.selectTier(3, 2, true, true, true));
    }

    @Test
    void selectTier_withoutInstancingButGl32_isBasic() {
        assertEquals(ShaderTier.BASIC, CleanroomGlCapabilities.selectTier(3, 2, false, true, true));
    }

    @Test
    void selectTier_below_glsl150_isLegacy() {
        // Mesa legacy compat contexts stop at 3.0 or 3.1: GLSL 1.50 is not available.
        assertEquals(ShaderTier.LEGACY, CleanroomGlCapabilities.selectTier(3, 1, true, true, true));
        assertEquals(ShaderTier.LEGACY, CleanroomGlCapabilities.selectTier(2, 1, false, false, true));
    }

    @Test
    void selectTier_withoutVaoCannotUseGlsl150Paths() {
        assertEquals(ShaderTier.LEGACY, CleanroomGlCapabilities.selectTier(4, 5, true, false, true));
    }

    @Test
    void selectTier_ancientContext_hasNoShaders() {
        assertEquals(ShaderTier.NONE, CleanroomGlCapabilities.selectTier(1, 4, false, false, false));
        assertEquals(ShaderTier.NONE, CleanroomGlCapabilities.selectTier(0, 0, false, false, false));
    }
}
