package dev.sixik.unigui.api.layout.v3;

/**
 * Runtime opt-in switches for Layout V3 migration slices.
 *
 * <p>All switches default to off so existing UI behavior stays on Layout V2
 * until a migrated container is explicitly enabled for comparison or rollout.</p>
 */
public final class LayoutV3Settings {
    private static volatile boolean linearBoxEnabled = Boolean.getBoolean("unigui.layout.v3.linearbox");
    private static volatile boolean wrapPanelEnabled = Boolean.getBoolean("unigui.layout.v3.wrappanel");

    private LayoutV3Settings() {
    }

    public static boolean linearBoxEnabled() {
        return linearBoxEnabled;
    }

    public static void linearBoxEnabled(boolean enabled) {
        linearBoxEnabled = enabled;
    }

    public static boolean wrapPanelEnabled() {
        return wrapPanelEnabled;
    }

    public static void wrapPanelEnabled(boolean enabled) {
        wrapPanelEnabled = enabled;
    }
}
