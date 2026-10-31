package dev.sixik.unigui.gtnh.backend;

import dev.sixik.unigui.gtnh.backend.GtnhGlCapabilities.ShaderTier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class GtnhGlCapabilitiesTest {
    @Test
    void parseVersion_vendorStrings() {
        assertArrayEquals(new int[]{4, 6}, GtnhGlCapabilities.parseVersion("4.6.0 NVIDIA 551.23"));
        assertArrayEquals(new int[]{3, 2}, GtnhGlCapabilities.parseVersion("3.2 Mesa 23.1.4"));
        assertArrayEquals(new int[]{2, 1}, GtnhGlCapabilities.parseVersion("2.1.2 INTEL-Mac"));
        assertArrayEquals(new int[]{3, 0}, GtnhGlCapabilities.parseVersion("OpenGL ES 3.0 Mesa"));
    }

    @Test
    void parseVersion_garbageIsZero() {
        assertArrayEquals(new int[]{0, 0}, GtnhGlCapabilities.parseVersion(null));
        assertArrayEquals(new int[]{0, 0}, GtnhGlCapabilities.parseVersion(""));
        assertArrayEquals(new int[]{0, 0}, GtnhGlCapabilities.parseVersion("no digits here"));
        assertArrayEquals(new int[]{0, 0}, GtnhGlCapabilities.parseVersion("7"));
    }

    @Test
    void selectTier_modernContext_isInstanced() {
        assertEquals(ShaderTier.INSTANCED, GtnhGlCapabilities.selectTier(4, 6, true, true, true));
        assertEquals(ShaderTier.INSTANCED, GtnhGlCapabilities.selectTier(3, 2, true, true, true));
    }

    @Test
    void selectTier_withoutInstancingButGl32_isBasic() {
        assertEquals(ShaderTier.BASIC, GtnhGlCapabilities.selectTier(3, 2, false, true, true));
    }

    @Test
    void selectTier_below_glsl150_isLegacy() {
        // Mesa legacy compat contexts stop at 3.0 or 3.1: GLSL 1.50 is not available.
        assertEquals(ShaderTier.LEGACY, GtnhGlCapabilities.selectTier(3, 1, true, true, true));
        assertEquals(ShaderTier.LEGACY, GtnhGlCapabilities.selectTier(2, 1, false, false, true));
    }

    @Test
    void selectTier_withoutVaoCannotUseGlsl150Paths() {
        assertEquals(ShaderTier.LEGACY, GtnhGlCapabilities.selectTier(4, 5, true, false, true));
    }

    @Test
    void selectTier_ancientContext_hasNoShaders() {
        assertEquals(ShaderTier.NONE, GtnhGlCapabilities.selectTier(1, 4, false, false, false));
        assertEquals(ShaderTier.NONE, GtnhGlCapabilities.selectTier(0, 0, false, false, false));
    }
}
