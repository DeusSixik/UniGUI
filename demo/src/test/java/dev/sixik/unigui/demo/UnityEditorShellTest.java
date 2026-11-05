package dev.sixik.unigui.demo;

import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.demo.ui.unity.UnityInspectorScreen;
import dev.sixik.unigui.widgets.containers.PanelWidget;
import dev.sixik.unigui.widgets.docking.DockLayoutSnapshot;
import dev.sixik.unigui.widgets.docking.DockingRoot;
import dev.sixik.unigui.widgets.feedback.ContextMenu;
import dev.sixik.unigui.widgets.interaction.Button;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Демо Unity собрано каркасом редакторов; тест фиксирует то, что пользователь видит: раскладку и меню.
 */
class UnityEditorShellTest {
    private static Widget root() {
        Widget root = UnityInspectorScreen.createScreen().root();
        applyAll(root);
        return root;
    }

    private static void applyAll(Widget widget) {
        if (widget instanceof PanelWidget panel) panel.applyQueuedMutations();
        if (widget instanceof DockingRoot) return;
        for (Widget child : widget.children()) applyAll(child);
    }

    private static <T extends Widget> List<T> find(Widget root, Class<T> type) {
        List<T> found = new ArrayList<>();
        walk(root, type, found);
        return found;
    }

    private static <T extends Widget> void walk(Widget widget, Class<T> type, List<T> into) {
        if (type.isInstance(widget)) into.add(type.cast(widget));
        if (widget instanceof PanelWidget panel) panel.applyQueuedMutations();
        for (Widget child : widget.children()) walk(child, type, into);
    }

    @Test
    void defaultLayoutPlacesAllSixPanelsLikeTheOriginalEditor() {
        DockingRoot dock = find(root(), DockingRoot.class).get(0);
        DockLayoutSnapshot snapshot = dock.manager().snapshot();

        assertEquals("scene", snapshot.activePaneId());
        assertEquals(0.18f, snapshot.root().splitRatio(), 0.0001f);
        List<String> ids = new ArrayList<>();
        collect(snapshot.root(), ids);
        assertEquals(List.of("hierarchy", "scene", "game", "project", "console", "inspector"), ids);
    }

    private static void collect(DockLayoutSnapshot.Node node, List<String> into) {
        if (node == null) return;
        into.addAll(node.paneIds());
        collect(node.first(), into);
        collect(node.second(), into);
    }

    @Test
    void menuBarKeepsTheUnityMenuOrder() {
        List<String> texts = find(root(), Button.class).stream().map(Button::text).toList();
        assertEquals(List.of("File", "Edit", "Assets", "GameObject", "Component", "Window", "Help"),
                texts.subList(0, 7));
    }

    @Test
    void windowMenuListsThePanelsInTheOriginalOrder() {
        Widget root = root();
        Button window = find(root, Button.class).stream()
                .filter(button -> "Window".equals(button.text())).findFirst().orElseThrow();
        window.click();
        applyAll(root);

        List<ContextMenu> menus = find(root, ContextMenu.class);
        assertTrue(menus.stream().anyMatch(menu -> titles(menu)
                .equals(List.of("Scene", "Game", "Hierarchy", "Inspector", "Project", "Console"))),
                "menus found: " + menus.stream().map(UnityEditorShellTest::titles).toList());
    }

    private static List<String> titles(ContextMenu menu) {
        return menu.itemWidgets().stream().filter(Button.class::isInstance)
                .map(widget -> ((Button) widget).text()).toList();
    }
}
