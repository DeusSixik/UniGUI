package dev.sixik.unigui.gtnh.commands;

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
import dev.sixik.unigui.gtnh.backend.GtnhClientTasks;
import dev.sixik.unigui.gtnh.demo.MinecraftLab;
import dev.sixik.unigui.gtnh.demo.TextLab;
import dev.sixik.unigui.gtnh.demo.TextureLab;
import dev.sixik.unigui.demo.ui_tests.TestWidgetsScreen;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Locale;

/**
 * Opens the version-neutral demo screens on the client thread (client commands already run there).
 */
public final class GtnhDemoLauncher {
    private static final Logger LOGGER = LogManager.getLogger("unigui");
    private static final List<String> NAMES = List.of(
            "widgets", "textures", "text", "minecraft", "docking", "dominion", "retro", "ribbon", "tutorial", "unity",
            "plug", "wrench", "wire", "sync", "spanner", "fork", "postprocess", "overlay", "culling", "scaling");

    private GtnhDemoLauncher() {
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
        // A chat command runs inside GuiChat.keyTyped, which closes the chat screen right after sending: a screen
        // opened here would be dismissed at once, so it opens at the end of the tick.
        GtnhClientTasks.post(() -> {
            LOGGER.info("UniGUI opening demo '{}'", normalized);
            openClient(normalized);
        });
        return true;
    }

    private static void openClient(String name) {
        switch (name) {
            case "widgets" -> TestWidgetsScreen.openGui();
            case "textures" -> TextureLab.open();
            case "text" -> TextLab.open();
            case "minecraft" -> MinecraftLab.open();
            case "docking" -> DockingWindowsDemoScreen.open();
            case "dominion" -> DominionScreen.openGui();
            case "retro" -> RetroTerminalScreen.open();
            case "ribbon" -> RibbonRoadsDemoScreen.open();
            case "tutorial" -> TutorialScreen.open();
            case "unity" -> UnityInspectorScreen.open();
            case "plug" -> PlugSocketMinigameScreen.open();
            case "wrench" -> WrenchNutMinigameScreen.open();
            case "wire" -> WireConnectionMinigameScreen.open();
            case "sync" -> SyncBatteryMinigameScreen.open();
            case "spanner" -> SpannerRhythmMinigameScreen.open();
            case "fork" -> ForkInSocketMinigameScreen.open();
            case "overlay" -> OverlayRenderDemo.register();
            case "postprocess" -> PostProcessingLayerTestScreen.open();
            case "culling" -> MarkerCullingDemoScreen.open();
            case "scaling" -> MarkerScalingDemoScreen.open();
            default -> throw new IllegalArgumentException("Unknown UniGUI demo: " + name);
        }
    }
}
