package dev.sixik.unigui.cleanroom.backend;

/**
 * Minecraft's legacy {@code §} formatting codes (pure logic, no Minecraft access).
 *
 * <p>Semantics follow {@code FontRenderer.renderStringAtPos} of 1.12.2: {@code §} followed by one character is a code
 * and takes no space; {@code 0-9a-f} (case-insensitive) pick a colour and clear all styles, {@code k l m n o} switch
 * obfuscated / bold / strikethrough / underline / italic on, {@code r} resets everything, and any other character
 * after {@code §} behaves like {@code §f}. A {@code §} that ends the string is an ordinary character.</p>
 *
 * <p>The vanilla font draws these itself; the layout only has to keep them out of the advance and carry the state
 * across glyphs it draws one by one. SDF faces apply the colour and ignore the styles.</p>
 */
final class CleanroomLegacyFormatting {
    /** The section sign that introduces a code. */
    static final char SIGN = '\u00a7';

    private static final int[] PALETTE = {
            0x000000, 0x0000AA, 0x00AA00, 0x00AAAA, 0xAA0000, 0xAA00AA, 0xFFAA00, 0xAAAAAA,
            0x555555, 0x5555FF, 0x55FF55, 0x55FFFF, 0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF};
    private static final String CODES = "0123456789abcdefklmnor";
    private static final int OBFUSCATED = 1;
    private static final int BOLD = 1 << 1;
    private static final int STRIKETHROUGH = 1 << 2;
    private static final int UNDERLINE = 1 << 3;
    private static final int ITALIC = 1 << 4;

    private CleanroomLegacyFormatting() {
    }

    /**
     * @param text  string
     * @param index position of a {@link #SIGN}
     * @return {@code true} if the sign at {@code index} has a code character after it
     */
    static boolean isCode(String text, int index) {
        return text.charAt(index) == SIGN && index + 1 < text.length();
    }

    /** @return {@code text} without codes (the sign and the character after it) */
    static String strip(String text) {
        if (text == null || text.indexOf(SIGN) < 0) return text;
        StringBuilder out = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            if (isCode(text, i)) {
                i++;
            } else {
                out.append(text.charAt(i));
            }
        }
        return out.toString();
    }

    /**
     * @param colorIndex {@code 0..15}
     * @return palette colour as {@code 0xRRGGBB}
     */
    static int rgb(int colorIndex) {
        return PALETTE[colorIndex & 15];
    }

    /**
     * Formatting state while walking a string.
     *
     * <p>Not thread safe; one instance is reused for every run of a command.</p>
     */
    static final class State {
        private int color = -1;
        private int styles;

        /** Clears colour and styles. */
        void reset() {
            color = -1;
            styles = 0;
        }

        /**
         * Applies the code character that follows a {@link #SIGN}.
         *
         * @param code character after the sign
         */
        void apply(char code) {
            int index = CODES.indexOf(Character.toLowerCase(code));
            if (index == 21) {
                reset();
            } else if (index >= 16) {
                styles |= 1 << (index - 16);
            } else {
                // Unknown characters count as white, like in FontRenderer.
                styles = 0;
                color = index < 0 ? 15 : index;
            }
        }

        /** @return colour index {@code 0..15}, or {@code -1} if the base colour applies */
        int color() {
            return color;
        }

        /** @return {@code true} if nothing is active */
        boolean isDefault() {
            return color < 0 && styles == 0;
        }

        /**
         * Appends the codes that recreate this state at the start of a fresh string.
         *
         * @param out destination
         */
        void appendPrefix(StringBuilder out) {
            if (color >= 0) out.append(SIGN).append(CODES.charAt(color));
            if ((styles & OBFUSCATED) != 0) out.append(SIGN).append('k');
            if ((styles & BOLD) != 0) out.append(SIGN).append('l');
            if ((styles & STRIKETHROUGH) != 0) out.append(SIGN).append('m');
            if ((styles & UNDERLINE) != 0) out.append(SIGN).append('n');
            if ((styles & ITALIC) != 0) out.append(SIGN).append('o');
        }
    }
}