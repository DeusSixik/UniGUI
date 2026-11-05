package dev.sixik.unigui.impl.style;

import dev.sixik.unigui.api.style.StylePack;

/**
 * Factory for editable packs based on the built-in widget styles.
 *
 * <p>The returned pack contains the standard fallback and per-widget rules, including
 * state-specific values such as hover, pressed, checked, and disabled styles.</p>
 */
public final class DefaultStylePack {
    private DefaultStylePack() {
    }

    /**
     * Creates a mutable copy of the built-in theme as a {@link StylePack}.
     *
     * @return an independent pack that can be customized and installed as a theme
     */
    public static StylePack copy() {
        return DefaultTheme.INSTANCE.copyStylePack();
    }
}
