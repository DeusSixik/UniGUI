package dev.sixik.unigui.api.core;

public final class InvalidationFlags {
    public static final int NONE = 0;
    public static final int LAYOUT = 1;
    public static final int VISUAL = 1 << 1;
    public static final int TEXTURE = 1 << 2;
    public static final int ALL = LAYOUT | VISUAL | TEXTURE;

    private InvalidationFlags() {
    }

    public static boolean has(int flags, int flag) {
        return (flags & flag) == flag;
    }
}
