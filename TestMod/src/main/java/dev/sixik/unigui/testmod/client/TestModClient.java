package dev.sixik.unigui.testmod.client;

import dev.sixik.unigui.api.render.TextureOptions;
import dev.sixik.unigui.api.text.*;
import dev.sixik.unigui.demo.ui.*;
import dev.sixik.unigui.demo.ui.interactive.space_ship_minigame.SolarNavigationScreen;
import dev.sixik.unigui.demo.ui.unity.UnityInspectorScreen;
import dev.sixik.unigui.widgets.minecraft.MinecraftDemoHost;
import dev.sixik.unigui.demo.ui_tests.TestWidgetsScreen;
import dev.sixik.unigui.testmod.client.ui.InventoryDemoScreen;
import dev.sixik.unigui.testmod.client.ui.LevelMapScreen;
import net.fabricmc.api.ClientModInitializer;

/**
 * Client entrypoint тестового Fabric-мода.
 */
public final class TestModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        DemoAssets.provider(MinecraftDemoAssets::load);

/*        TextureHandle icon = new MinecraftTextureHandle(
                ResourceLocation.tryBuild("minecraft", "textures/gui_background.png"), 64, 64);
        InlineContentRenderer iconRenderer = InlineContentRenderer.texture(icon);
        InlineMarkers.register("key", (argument, size) ->
                InlineContentSpan.sized("key:" + argument, size, iconRenderer)
                        .withAlignment(InlineContentAlignment.BASELINE)
                        .withMargins(1, 1));*/
//        LoadingScreenDemo.register();
//        OverlayRenderDemo.register();

        InlineMarkers.register("key", new InlineMarkerHandler() {
            private InlineContentRenderer renderer;

            @Override
            public InlineContentSpan create(String argument, InlineSize size) {
                if (renderer == null) {
                    renderer = InlineContentRenderer.texture(DemoAssets.texture(
                            "unigui_testmod:keys/gui_background",
                            "assets/testmod/textures/gui_background.png",
                            TextureOptions.defaults()));
                }
                return InlineContentSpan.sized("key:" + argument, size, renderer)
                        .withAlignment(InlineContentAlignment.BASELINE)
                        .withMargins(1, 1);
            }
        });
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
        MinecraftDemoHost.install();
        UnityInspectorScreen.open();
    }

    public static void openInventoryDemo() {
        InventoryDemoScreen.open();
    }
}
