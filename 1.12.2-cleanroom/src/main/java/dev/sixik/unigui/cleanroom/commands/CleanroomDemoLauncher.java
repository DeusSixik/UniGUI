package dev.sixik.unigui.cleanroom.commands;

import dev.sixik.unigui.demo.ui.DockingWindowsDemoScreen;
import dev.sixik.unigui.demo.ui.DominionScreen;
import dev.sixik.unigui.demo.ui.ForkInSocketMinigameScreen;
import dev.sixik.unigui.demo.ui.MarkerCullingDemoScreen;
import dev.sixik.unigui.demo.ui.MarkerScalingDemoScreen;
import dev.sixik.unigui.demo.ui.OverlayRenderDemo;
import dev.sixik.unigui.demo.ui.PlugSocketMinigameScreen;
import dev.sixik.unigui.demo.ui.PostProcessingLayerTestScreen;
import dev.sixik.unigui.demo.ui.RetroTerminalScreen;
import dev.sixik.unigui.demo.ui.RibbonRoadsDemoScreen;
import dev.sixik.unigui.demo.ui.SpannerRhythmMinigameScreen;
import dev.sixik.unigui.demo.ui.SyncBatteryMinigameScreen;
import dev.sixik.unigui.demo.ui.TutorialScreen;
import dev.sixik.unigui.demo.ui.WireConnectionMinigameScreen;
import dev.sixik.unigui.demo.ui.WrenchNutMinigameScreen;
import dev.sixik.unigui.demo.ui.unity.UnityInspectorScreen;
import dev.sixik.unigui.demo.ui_tests.TestWidgetsScreen;
import net.minecraft.client.Minecraft;

import java.util.List;
import java.util.Locale;

/**
 * Opens the version-neutral demo screens on the Cleanroom client thread.
 */
public final class CleanroomDemoLauncher {
    private static final List<String> NAMES = List.of(
            "widgets", "docking", "dominion", "retro", "ribbon", "tutorial", "unity", "overlay",
            "plug", "wrench", "wire", "sync", "spanner", "fork", "postprocess", "culling", "scaling");

    private CleanroomDemoLauncher() {
    }

    public static List<String> names() {
        return NAMES;
    }

    public static boolean supported(String name) {
        return name != null && NAMES.contains(name.toLowerCase(Locale.ROOT));
    }

    public static boolean open(String name) {
        if (!supported(name)) return false;
        String normalized = name.toLowerCase(Locale.ROOT);
        Minecraft.getMinecraft().addScheduledTask(() -> openClient(normalized));
        return true;
    }

    private static void openClient(String name) {
        switch (name) {
            case "widgets" -> TestWidgetsScreen.openGui();
            case "docking" -> DockingWindowsDemoScreen.open();
            case "dominion" -> DominionScreen.openGui();
            case "retro" -> RetroTerminalScreen.open();
            case "ribbon" -> RibbonRoadsDemoScreen.open();
            case "tutorial" -> TutorialScreen.open();
            case "unity" -> UnityInspectorScreen.open();
            case "overlay" -> OverlayRenderDemo.register();
            case "plug" -> PlugSocketMinigameScreen.open();
            case "wrench" -> WrenchNutMinigameScreen.open();
            case "wire" -> WireConnectionMinigameScreen.open();
            case "sync" -> SyncBatteryMinigameScreen.open();
            case "spanner" -> SpannerRhythmMinigameScreen.open();
            case "fork" -> ForkInSocketMinigameScreen.open();
            case "postprocess" -> PostProcessingLayerTestScreen.open();
            case "culling" -> MarkerCullingDemoScreen.open();
            case "scaling" -> MarkerScalingDemoScreen.open();
            default -> throw new IllegalArgumentException("Unknown UniGUI demo: " + name);
        }
    }
}
