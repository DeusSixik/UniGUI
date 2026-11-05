package dev.sixik.unigui.demo.basic.main_demo;

import dev.sixik.unigui.widgets.containers.Box;
import dev.sixik.unigui.widgets.containers.GridBox;
import dev.sixik.unigui.widgets.containers.SplitPanel;
import dev.sixik.unigui.widgets.containers.VBox;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.widgets.docking.DockArea;
import dev.sixik.unigui.widgets.docking.DockPane;
import dev.sixik.unigui.widgets.docking.DockingRoot;
import dev.sixik.unigui.widgets.navigation.TreeView;

import java.util.List;

final class LayoutCategory extends DemoCategory {
    private static final List<Control> CONTROLS = List.of(
            new Control("grid", "GridBox", "Arrange controls in consistent columns and rows."),
            new Control("split-panel", "SplitPanel", "Resize two adjacent content panes with a draggable divider."),
            new Control("docking", "Docking", "Rearrange tabbed panes by docking them into workspace zones."),
            new Control("tree", "TreeView", "Browse hierarchical data with expandable branches.")
    );

    @Override
    String title() {
        return "Layout";
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
            case "grid" -> addGrid(root);
            case "split-panel" -> addSplitPanel(root);
            case "docking" -> addDocking(root);
            case "tree" -> addTree(root);
            default -> root.addChild(samplePanel("Preview", "Interactive sample", new Label(controlId), ""));
        }
    }

    private void addGrid(VBox root) {
        GridBox grid = new GridBox().columns(3).horizontalSpacing(8.0f).verticalSpacing(8.0f);
        grid.layout(style -> style.widthPercent(100.0f));
        for (int i = 1; i <= 6; i++) {
            Box tile = new Box();
            tile.backgroundVisible(true);
            tile.background().set(i % 2 == 0
                    ? UniGuiDemo.GalleryTemplate.color(226, 237, 246)
                    : UniGuiDemo.GalleryTemplate.color(235, 242, 247));
            tile.layout(style -> style.height(48.0f).padding(10.0f, 8.0f));
            tile.addChild(label("Item 0" + i, UniGuiDemo.GalleryTemplate.TEXT_DARK));
            grid.addChild(tile);
        }
        root.addChild(samplePanel("Uniform grid", "A fixed column count keeps repeated controls aligned.", grid,
                "GridBox grid = new GridBox().columns(3).horizontalSpacing(8);"));
    }

    private void addTree(VBox root) {
        TreeView tree = UniGuiDemo.cardTree(new TreeView());
        tree.addRoot("Project").addChild("Scenes").addChild("Materials");
        tree.layout(style -> style.widthPercent(100.0f).height(170.0f));
        root.addChild(samplePanel("Hierarchy", "Tree views help navigate nested data without flattening its structure.", tree,
                "TreeView tree = new TreeView();\ntree.addRoot(\"Project\").addChild(\"Scenes\");"));

        TreeView treeNoIcon = UniGuiDemo.cardTree(new TreeView().iconMode(TreeView.TreeIconMode.NONE));
        treeNoIcon.addRoot("Workspace").addChild("Applications").addChild("Settings");
        treeNoIcon.addRoot("Documents").addChild("Reports").addChild("Notes");
        treeNoIcon.layout(style -> style.widthPercent(100.0f).height(180.0f));
        root.addChild(samplePanel("Text-only hierarchy", "Disable folder/file glyphs when labels and nesting are enough.", treeNoIcon,
                "TreeView tree = new TreeView()\n        .iconMode(TreeView.TreeIconMode.NONE);\ntree.addRoot(\"Workspace\").addChild(\"Applications\");"));

    }

    private void addSplitPanel(VBox root) {
        VBox files = new VBox();
        files.spacing(5.0f);
        files.layout(style -> style.widthPercent(100.0f).padding(10.0f, 8.0f));
        files.addChild(label("EXPLORER", UniGuiDemo.GalleryTemplate.TEXT_MUTED));
        files.addChild(label("src", UniGuiDemo.GalleryTemplate.TEXT_DARK));
        files.addChild(label("assets", UniGuiDemo.GalleryTemplate.TEXT_DARK));
        files.addChild(label("build.gradle", UniGuiDemo.GalleryTemplate.TEXT_DARK));
        files.backgroundVisible(true);
        files.background().set(UniGuiDemo.GalleryTemplate.color(235, 241, 246));

        VBox editor = new VBox();
        editor.spacing(8.0f);
        editor.layout(style -> style.widthPercent(100.0f).padding(14.0f, 12.0f));
        editor.addChild(label("Editor", UniGuiDemo.GalleryTemplate.TEXT_DARK));
        editor.addChild(label("Drag the divider to resize both panes.", UniGuiDemo.GalleryTemplate.color(86, 105, 121)));
        editor.backgroundVisible(true);
        editor.background().set(UniGuiDemo.GalleryTemplate.color(255, 255, 255));

        SplitPanel split = new SplitPanel(files, editor)
                .splitRatio(0.32f)
                .minFirstSize(90.0f)
                .minSecondSize(150.0f)
                .splitterThickness(5.0f);
        split.layout(style -> style.widthPercent(100.0f).height(180.0f));
        root.addChild(samplePanel("Resizable panes", "The first pane is a small explorer; the second is an editor workspace.", split,
                "SplitPanel split = new SplitPanel(explorer, editor)\n        .splitRatio(0.32f)\n        .minFirstSize(90.0f)\n        .minSecondSize(150.0f);"));
    }

    private void addDocking(VBox root) {
        DockingRoot docking = new DockingRoot();
        docking.addPane(DockPane.document("dock-scene", "Scene", dockingPane("Scene workspace")))
                .addToolPane("dock-inspector", "Inspector", dockingPane("Inspector tools"), DockArea.RIGHT)
                .addToolPane("dock-console", "Console", dockingPane("Build and runtime output"), DockArea.BOTTOM)
                .selectPane("dock-scene");
        docking.layout(style -> style.widthPercent(100.0f).height(250.0f));
        root.addChild(samplePanel("Dockable workspace", "Drag a tab into another zone to split the workspace, or onto a pane to create a tab group.", docking,
                "DockingRoot docking = new DockingRoot();\ndocking.addPane(DockPane.document(\"scene\", \"Scene\", sceneView))\n        .addToolPane(\"inspector\", \"Inspector\", inspector, DockArea.RIGHT)\n        .addToolPane(\"console\", \"Console\", console, DockArea.BOTTOM);"));
    }

    private VBox dockingPane(String title) {
        VBox content = new VBox();
        content.spacing(6.0f);
        content.layout(style -> style.widthPercent(100.0f).heightPercent(100.0f).padding(10.0f, 8.0f));
        content.addChild(label(title, UniGuiDemo.GalleryTemplate.color(220, 228, 235)));
        content.addChild(label("Dock this pane by dragging its tab.", UniGuiDemo.GalleryTemplate.color(158, 174, 188)));
        return content;
    }
}
