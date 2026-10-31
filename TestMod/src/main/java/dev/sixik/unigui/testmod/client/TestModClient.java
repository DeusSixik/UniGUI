package dev.sixik.unigui.testmod.client;

import dev.sixik.unigui.demo.ui.DominionScreen;
import dev.sixik.unigui.demo.ui.BindingDemoScreen;
import dev.sixik.unigui.demo.ui.DockingWindowsDemoScreen;
import dev.sixik.unigui.demo.ui.MarkerCullingDemoScreen;
import dev.sixik.unigui.demo.ui.MarkerScalingDemoScreen;
import dev.sixik.unigui.demo.ui.ForkInSocketMinigameScreen;
import dev.sixik.unigui.testmod.client.ui.LevelMapScreen;
import dev.sixik.unigui.testmod.client.ui.LoadingScreenDemo;
import dev.sixik.unigui.demo.ui.OverlayRenderDemo;
import dev.sixik.unigui.demo.ui.PlugSocketMinigameScreen;
import dev.sixik.unigui.demo.ui.PostProcessingLayerTestScreen;
import dev.sixik.unigui.demo.ui.RetroTerminalScreen;
import dev.sixik.unigui.demo.ui.RibbonRoadsDemoScreen;
import dev.sixik.unigui.testmod.client.ui.SolarNavigationScreen;
import dev.sixik.unigui.demo.ui.SpannerRhythmMinigameScreen;
import dev.sixik.unigui.demo.ui.SyncBatteryMinigameScreen;
import dev.sixik.unigui.demo.ui.TutorialScreen;
import dev.sixik.unigui.demo.ui.unity.UnityInspectorScreen;
import dev.sixik.unigui.demo.ui.WrenchNutMinigameScreen;
import dev.sixik.unigui.demo.ui.WireConnectionMinigameScreen;
import dev.sixik.unigui.demo.ui_tests.TestWidgetsScreen;
import dev.sixik.unigui.demo.ui.DemoAssets;
import net.fabricmc.api.ClientModInitializer;

/**
 * Client entrypoint тестового Fabric-мода.
 */
public final class TestModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        DemoAssets.provider(MinecraftDemoAssets::load);
//        LoadingScreenDemo.register();
//        OverlayRenderDemo.register();
    }

    /**
     * Открывает основное UniGUI demo.
     */
    public static void openDemo() {
        TestWidgetsScreen.openGui();
    }

    public static void openBindingDemo() {
        BindingDemoScreen.open();
    }

    public static void openRetroTerminal() {
        RetroTerminalScreen.open();
    }

    public static void openLevelMap() {
        LevelMapScreen.open();
    }

    public static void openSolarNavigation() {
        SolarNavigationScreen.open();
    }

    public static void openPlugSocketMinigame() {
        PlugSocketMinigameScreen.open();
    }

    public static void openWrenchNutMinigame() {
        WrenchNutMinigameScreen.open();
    }

    public static void openWireConnectionMinigame() {
        WireConnectionMinigameScreen.open();
    }

    public static void openSyncBatteryMinigame() {
        SyncBatteryMinigameScreen.open();
    }

    public static void openSpannerRhythmMinigame() {
        SpannerRhythmMinigameScreen.open();
    }

    public static void openForkInSocketMinigame() {
        ForkInSocketMinigameScreen.open();
    }

    public static void openPostProcessingLayerTest() {
        PostProcessingLayerTestScreen.open();
    }

    public static void openDockingWindowsDemo() {
        DockingWindowsDemoScreen.open();
    }

    public static void openMarkerCullingDemo() {
        MarkerCullingDemoScreen.open();
    }

    public static void openMarkerScalingDemo() {
        MarkerScalingDemoScreen.open();
    }

    public static void openRibbonRoadsDemo() {
        RibbonRoadsDemoScreen.open();
    }

    public static void openTutorial() {
        TutorialScreen.createScreen(LevelMapScreen::open).openGui();
    }

    public static void openUnityInspector() {
        UnityInspectorScreen.open();
    }
}
