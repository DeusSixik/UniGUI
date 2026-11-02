package dev.sixik.unigui.demo.ui.minecraft;

/**
 * Installs and looks up the {@link DemoMinecraftHost} used by the Minecraft category of the demo gallery.
 *
 * <p>Same idea as {@code DemoAssets}: the platform module registers its host once during client start-up, and the
 * portable demo asks for it when a screen is built. Without a host the category shows a notice instead of widgets,
 * so the demo stays constructible in headless tests.</p>
 */
public final class DemoMinecraft {
    private static volatile DemoMinecraftHost host;

    private DemoMinecraft() {
    }

    /**
     * Installs the host of the running Minecraft version.
     *
     * @param next the host, or {@code null} to remove it
     */
    public static void host(DemoMinecraftHost next) {
        host = next;
    }

    /**
     * @return the installed host, or {@code null} when the demo runs without Minecraft
     */
    public static DemoMinecraftHost host() {
        return host;
    }
}
