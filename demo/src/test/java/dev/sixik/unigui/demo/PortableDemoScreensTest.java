package dev.sixik.unigui.demo;

import dev.sixik.unigui.api.screen.UniScreen;
import dev.sixik.unigui.demo.ui.DemoScaffold;
import dev.sixik.unigui.demo.ui.DockingWindowsDemoScreen;
import dev.sixik.unigui.demo.ui.DominionScreen;
import dev.sixik.unigui.demo.ui.PlugSocketMinigameScreen;
import dev.sixik.unigui.demo.ui.PostProcessingLayerTestScreen;
import dev.sixik.unigui.demo.ui.RetroTerminalScreen;
import dev.sixik.unigui.demo.ui.RibbonRoadsDemoScreen;
import dev.sixik.unigui.demo.ui.TutorialScreen;
import dev.sixik.unigui.demo.ui.WrenchNutMinigameScreen;
import dev.sixik.unigui.demo.ui.ForkInSocketMinigameScreen;
import dev.sixik.unigui.demo.ui.SpannerRhythmMinigameScreen;
import dev.sixik.unigui.demo.ui.SyncBatteryMinigameScreen;
import dev.sixik.unigui.demo.ui.WireConnectionMinigameScreen;
import dev.sixik.unigui.demo.ui.unity.UnityInspectorScreen;
import dev.sixik.unigui.demo.ui_tests.TestWidgetsScreen;
import dev.sixik.unigui.widgets.containers.Box;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PortableDemoScreensTest {

    @Test
    void demoRuntimeClasspath_withoutMinecraft_keepsMinecraftClassesAbsent() {
        assertThrows(ClassNotFoundException.class,
                () -> Class.forName("net.minecraft.client.Minecraft"));
    }

    @Test
    void demoResources_textureFontAndShaders_areAvailableOnClasspath() {
        ClassLoader classLoader = PortableDemoScreensTest.class.getClassLoader();

        assertNotNull(classLoader.getResource("assets/unigui_testmod/textures/gui/cables/cable_blue.png"));
        assertNotNull(classLoader.getResource("assets/fonts/Neue Haas Grotesk Display Pro 55 Roman.otf"));
        assertNotNull(classLoader.getResource("assets/unigui_testmod/shaders/retro_terminal.fsh"));
    }

    @Test
    void screenFactories_withoutInstalledBackend_createCompleteDescriptions() {
        List<Supplier<UniScreen>> factories = List.of(
                TestWidgetsScreen::createScreen,
                DockingWindowsDemoScreen::createScreen,
                DominionScreen::createScreen,
                PlugSocketMinigameScreen::createScreen,
                PostProcessingLayerTestScreen::createScreen,
                RetroTerminalScreen::createScreen,
                RibbonRoadsDemoScreen::createScreen,
                TutorialScreen::createScreen,
                WrenchNutMinigameScreen::createScreen,
                ForkInSocketMinigameScreen::createScreen,
                SpannerRhythmMinigameScreen::createScreen,
                SyncBatteryMinigameScreen::createScreen,
                WireConnectionMinigameScreen::createScreen,
                UnityInspectorScreen::createScreen);

        for (Supplier<UniScreen> factory : factories) {
            UniScreen screen = factory.get();
            assertNotNull(screen);
            assertNotNull(screen.root());
            assertNotNull(screen.context());
        }
    }

    @Test
    void demoScaffold_builder_createsScreenWithoutOpeningPlatformWindow() {
        UniScreen screen = DemoScaffold.create(
                "Portable Demo",
                "PANEL",
                "Hint",
                shell -> shell.content(new Box()));

        assertNotNull(screen.root());
        assertNotNull(screen.context());
    }
}
