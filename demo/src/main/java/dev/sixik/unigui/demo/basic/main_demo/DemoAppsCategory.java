package dev.sixik.unigui.demo.basic.main_demo;

import dev.sixik.unigui.demo.ui.RetroTerminalScreen;
import dev.sixik.unigui.demo.ui.interactive.space_ship_minigame.SolarNavigationScreen;
import dev.sixik.unigui.demo.ui.unity.UnityInspectorScreen;
import dev.sixik.unigui.widgets.containers.VBox;
import dev.sixik.unigui.widgets.interaction.Button;

import java.util.List;

final class DemoAppsCategory extends DemoCategory {
    private static final List<Control> CONTROLS = List.of(
            new Control("solar-navigation", "Solar Navigation", "Fly through a seeded star system, locate stations, and dock at your targets."),
            new Control("retro-terminal", "Retro Terminal", "Explore a CRT-style command terminal with interactive output and post-processing."),
            new Control("unity", "Unity", "Open a Unity-inspired editor interface with hierarchy, scene, project, and inspector panels."));

    @Override
    String title() {
        return "Demo";
    }

    @Override
    boolean expandedByDefault() {
        return false;
    }

    @Override
    List<Control> controls() {
        return CONTROLS;
    }

    @Override
    void addDemo(String controlId, VBox root) {
        switch (controlId) {
            case "solar-navigation" -> addLauncher(root, "Solar Navigation", CONTROLS.get(0).description(), SolarNavigationScreen::open);
            case "retro-terminal" -> addLauncher(root, "Retro Terminal", CONTROLS.get(1).description(), RetroTerminalScreen::open);
            case "unity" -> addLauncher(root, "Unity", CONTROLS.get(2).description(), UnityInspectorScreen::open);
            default -> root.addChild(samplePanel("Preview", "Interactive demo", label(controlId, UniGuiDemo.GalleryTemplate.TEXT_DARK), ""));
        }
    }

    private void addLauncher(VBox root, String title, String description, Runnable action) {
        Button launch = new Button("Open " + title);
        UniGuiDemo.GalleryTemplate.styleButton(launch,
                UniGuiDemo.GalleryTemplate.color(30, 166, 222),
                UniGuiDemo.GalleryTemplate.color(255, 255, 255));
        launch.layout(style -> style.width(190.0f).height(36.0f).flexGrow(0.0f).flexShrink(0.0f));
        launch.onClick(event -> action.run());
        root.addChild(samplePanel(title, description, launch, null));
    }
}
