package dev.sixik.unigui.api.text;

import dev.sixik.unigui.impl.text.DefaultFontRegistry;

/** Entry point for the process-wide default font registry. */
public final class Fonts {
    private Fonts() {
    }

    public static FontRegistry global() {
        return DefaultFontRegistry.global();
    }

    public static FontFace defaultFace() {
        return global().defaultFace();
    }
}
