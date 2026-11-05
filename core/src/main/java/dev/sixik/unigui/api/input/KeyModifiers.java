package dev.sixik.unigui.api.input;

public final class KeyModifiers {
    public static final int SHIFT = 1;
    public static final int CONTROL = 1 << 1;
    public static final int ALT = 1 << 2;
    public static final int SUPER = 1 << 3;

    private KeyModifiers() {
    }

    public static boolean has(int modifiers, int mask) {
        return (modifiers & mask) == mask;
    }
}
