package dev.sixik.unigui.demo.basic.main_demo;

import dev.sixik.unigui.widgets.containers.VBox;
import dev.sixik.unigui.widgets.containers.Box;
import dev.sixik.unigui.widgets.containers.ScrollView;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.widgets.navigation.ExpandablePanel;
import dev.sixik.unigui.widgets.navigation.NavigationCategory;
import dev.sixik.unigui.widgets.navigation.NavigationView;
import dev.sixik.unigui.api.style.StyleKeys;

import java.util.List;

final class NavigationWidgetsCategory extends DemoCategory {
    private static final List<Control> CONTROLS = List.of(
            new Control("expandable-panel", "ExpandablePanel", "Reveal secondary content without leaving the current view."),
            new Control("navigation-view", "NavigationView", "Group destinations and react to the selected item."));

    @Override
    String title() {
        return "Navigation";
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
            case "expandable-panel" -> addExpandablePanel(root);
            case "navigation-view" -> addNavigationView(root);
            default -> root.addChild(samplePanel("Preview", "Interactive sample", new Label(controlId), ""));
        }
    }

    private void addExpandablePanel(VBox root) {
        Label status = label("Panel is open", UniGuiDemo.GalleryTemplate.color(65, 86, 103));
        VBox settings = new VBox();
        settings.spacing(8.0f);
        settings.layout(style -> style.widthPercent(100.0f).padding(8.0f, 6.0f));
        settings.addChild(label("Advanced options", UniGuiDemo.GalleryTemplate.TEXT_DARK));
        settings.addChild(label("Keep this section for settings used less often.", UniGuiDemo.GalleryTemplate.color(86, 105, 121)));

        ExpandablePanel panel = new ExpandablePanel("Advanced settings")
                .expanded(true)
                .addContent(settings);
        panel.onExpandedChanged(event -> status.text(event.newValue() ? "Panel is open" : "Panel is closed"));
        panel.layout(style -> style.widthPercent(100.0f));
        VBox sample = new VBox();
        sample.spacing(12.0f);
        sample.layout(style -> style.widthPercent(100.0f));
        sample.addChild(panel);
        sample.addChild(status);
        root.addChild(samplePanel("Expandable settings", "Toggle the heading to show or hide its content.", sample,
                "ExpandablePanel panel = new ExpandablePanel(\"Advanced settings\")\n        .expanded(true)\n        .addContent(settings);\npanel.onExpandedChanged(event -> updateStatus(event.newValue()));"));
    }

    private void addNavigationView(VBox root) {
        Label status = label("Selected: Overview", UniGuiDemo.GalleryTemplate.color(65, 86, 103));
        NavigationView navigation = new NavigationView();
        navigation.categoryColor(UniGuiDemo.GalleryTemplate.NAV_GROUP);
        navigation.categoryTextColor(UniGuiDemo.GalleryTemplate.color(232, 239, 245));
        navigation.style(style -> style
                .put(StyleKeys.TREE_TEXT_COLOR, UniGuiDemo.GalleryTemplate.color(205, 217, 227))
                .put(StyleKeys.TREE_SELECTED_TEXT_COLOR, UniGuiDemo.GalleryTemplate.color(255, 255, 255))
                .put(StyleKeys.TREE_DISABLED_TEXT_COLOR, UniGuiDemo.GalleryTemplate.color(119, 137, 152))
                .put(StyleKeys.TREE_HOVER_COLOR, UniGuiDemo.GalleryTemplate.color(52, 76, 98))
                .put(StyleKeys.TREE_SELECTED_COLOR, UniGuiDemo.GalleryTemplate.NAV_SELECTED)
                .put(StyleKeys.TREE_DISCLOSURE_COLOR, UniGuiDemo.GalleryTemplate.color(205, 217, 227))
                .put(StyleKeys.TREE_ICON_COLOR, UniGuiDemo.GalleryTemplate.color(205, 217, 227)));
        NavigationCategory workspace = navigation.addCategory("Workspace", true);
        workspace.addItem("nav-overview", "Overview");
        workspace.addItem("nav-activity", "Activity");
        NavigationCategory account = navigation.addCategory("Account", true);
        account.addItem("nav-profile", "Profile");
        account.addItem("nav-preferences", "Preferences");
        navigation.selectItem("nav-overview");
        navigation.onItemSelected(item -> status.text("Selected: " + item.title()));
        navigation.layout(style -> style.widthPercent(100.0f));

        ScrollView scroll = new ScrollView(navigation);
        scroll.overlayScrollBars(false);
        scroll.layout(style -> style.widthPercent(100.0f).height(164.0f));
        scroll.scrollbarTrackColor().set(UniGuiDemo.GalleryTemplate.color(25, 39, 53));
        scroll.scrollbarThumbColor().set(UniGuiDemo.GalleryTemplate.color(78, 101, 120));
        scroll.scrollbarThumbHoverColor().set(UniGuiDemo.GalleryTemplate.color(112, 139, 160));

        Box navigationPanel = new Box();
        navigationPanel.themeEnabled(false);
        navigationPanel.backgroundVisible(true);
        navigationPanel.background().set(UniGuiDemo.GalleryTemplate.SIDEBAR);
        navigationPanel.borderVisible(false);
        navigationPanel.layout(style -> style.widthPercent(100.0f).height(180.0f).padding(8.0f, 8.0f));
        navigationPanel.addChild(scroll);

        VBox sample = new VBox();
        sample.spacing(10.0f);
        sample.layout(style -> style.widthPercent(100.0f));
        sample.addChild(navigationPanel);
        sample.addChild(status);
        root.addChild(samplePanel("Grouped destinations", "Categories organize destinations; selecting an item reports its identity.", sample,
                "NavigationView nav = new NavigationView();\nnav.addCategory(\"Workspace\").addItem(\"overview\", \"Overview\");\nnav.onItemSelected(item -> openPage(item.id()));\nnav.selectItem(\"overview\");"));
    }
}
