package dev.sixik.unigui.impl.text;

import dev.sixik.unigui.api.text.FontFace;

/** Internal capability for faces that can provide CPU SDF glyphs. */
public interface SdfGlyphProvider extends FontFace {
    SdfGlyph sdfGlyph(int codePoint, int pixelSize, int spread);
}
