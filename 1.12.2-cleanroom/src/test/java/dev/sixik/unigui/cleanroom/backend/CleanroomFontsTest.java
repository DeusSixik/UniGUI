package dev.sixik.unigui.cleanroom.backend;

import dev.sixik.unigui.api.text.FontFace;
import dev.sixik.unigui.impl.text.DefaultFontRegistry;
import dev.sixik.unigui.impl.text.SdfGlyph;
import dev.sixik.unigui.impl.text.SdfGlyphProvider;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The fonts that ship in the jar must be readable from the classpath, parse as fonts and rasterize in the
 * environment's AWT (headless in CI), because the SDF atlas depends on exactly that.
 */
class CleanroomFontsTest {
    @Test
    void everyBuiltinFontIsPackagedAndRasterizes() throws IOException {
        System.setProperty("java.awt.headless", "true");
        for (CleanroomFonts.Builtin font : CleanroomFonts.Builtin.values()) {
            String path = "/assets/" + font.location().getNamespace() + "/" + font.location().getPath();
            byte[] data;
            try (InputStream input = CleanroomFontsTest.class.getResourceAsStream(path)) {
                assertNotNull(input, path + " is missing from the module resources");
                data = input.readAllBytes();
            }
            FontFace face = DefaultFontRegistry.global().register("test:" + font.id(), data);
            assertTrue(face instanceof SdfGlyphProvider, font.id() + " must be an SDF face");
            SdfGlyph glyph = ((SdfGlyphProvider) face).sdfGlyph('H', 48, 8);
            assertNotNull(glyph, font.id() + " has no glyph for H");
            assertTrue(glyph.advance() > 0.0f, font.id() + " has a zero advance for H");
            assertTrue(face.metrics(14.0f).lineHeight() > 0.0f);
        }
    }
}