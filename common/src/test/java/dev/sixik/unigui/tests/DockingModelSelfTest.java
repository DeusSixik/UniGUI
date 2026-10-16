package dev.sixik.unigui.tests;

import dev.sixik.unigui.widgets.docking.DockArea;
import dev.sixik.unigui.widgets.docking.DockLayoutSnapshot;
import dev.sixik.unigui.widgets.docking.DockLayoutSnapshotCodec;
import dev.sixik.unigui.widgets.docking.DockNode;
import dev.sixik.unigui.widgets.docking.DockPane;
import dev.sixik.unigui.widgets.docking.DockSplitOrientation;
import dev.sixik.unigui.widgets.docking.DockingRoot;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.widgets.feedback.WindowWidget;

import java.util.List;
import java.util.Map;

public final class DockingModelSelfTest {
    public static void main(String[] args) {
        new DockingModelSelfTest().run();
    }

    private void run() {
        testSplitTabFloatCloseAndRestoreEdgeCases();
        testSnapshotCodecRoundTripsSpecialIdsAndToleratesCorruptInput();
        testDockEventsCopyCancellationAcrossRoutedSnapshots();
        testReorderPaneMovesWithinLeaf();
        System.out.println("DockingModelSelfTest passed");
    }

    private void testSplitTabFloatCloseAndRestoreEdgeCases() {
        DockingRoot root = new DockingRoot();
        DockPane scene = pane("scene", "Scene");
        DockPane duplicateScene = pane("scene", "Duplicate Scene");
        DockPane inspector = pane("inspector", "Inspector");
        DockPane console = pane("console", "Console");
        DockPane locked = pane("locked", "Locked").closable(false);

        root.addPane(scene);
        root.addPane(duplicateScene);
        expect(root.manager().paneCount() == 1 && root.manager().selectedPane() == scene,
                "Adding a pane with an existing id should select the original pane without duplicating it");

        root.splitPane(scene.id(), DockArea.RIGHT, inspector);
        root.tabPane(scene.id(), console);
        root.tabPane(scene.id(), locked);
        expect(root.manager().paneCount() == 4
                        && root.rootNode().isSplit()
                        && root.rootNode().orientation() == DockSplitOrientation.HORIZONTAL,
                "Split and tab model operations should retain all panes in a stable split tree");

        root.selectPane(locked.id());
        expect(!root.closeActivePane() && root.manager().containsPane(locked.id()),
                "closeActivePane should ignore non-closable panes");

        boolean activeSelfTabbed = root.manager().dockPane(locked.id(), locked.id(), DockArea.TAB);
        expect(!activeSelfTabbed && root.manager().containsPane(locked.id()) && root.manager().paneCount() == 4,
                "Tabbing a pane onto itself should be a no-op that preserves the model");

        WindowWidget floated = root.manager().floatPane(console.id());
        expect(floated != null
                        && floated.content() == console.content()
                        && floated.title().equals("Console")
                        && !root.manager().containsPane(console.id())
                        && root.manager().paneCount() == 3,
                "floatPane should detach the pane and bridge its content into a WindowWidget");

        root.manager().closePane(inspector.id());
        expect(root.manager().paneCount() == 2
                        && root.rootNode().isLeaf()
                        && root.manager().containsPane(scene.id())
                        && root.manager().containsPane(locked.id()),
                "Closing the final pane in a split branch should compact the tree back to a leaf");

        boolean splitActiveFromSameGroup = root.manager().dockPane(locked.id(), locked.id(), DockArea.RIGHT);
        expect(splitActiveFromSameGroup
                        && root.rootNode().isSplit()
                        && root.manager().containsPane(scene.id())
                        && root.manager().containsPane(locked.id())
                        && root.manager().paneCount() == 2,
                "Splitting the active pane from a single tab group should create a real side pane");
    }

    private void testSnapshotCodecRoundTripsSpecialIdsAndToleratesCorruptInput() {
        DockingRoot root = new DockingRoot();
        DockPane special = pane("pane|,]\u00FE", "Special");
        DockPane side = pane("side", "Side");
        root.addPane(special);
        root.splitPane(special.id(), DockArea.BOTTOM, side);
        root.selectPane(special.id());

        DockLayoutSnapshot snapshot = root.manager().snapshot();
        DockLayoutSnapshot decoded = DockLayoutSnapshotCodec.decode(DockLayoutSnapshotCodec.encode(snapshot));
        expect(decoded.equals(snapshot),
                "DockLayoutSnapshotCodec should round-trip delimiter and unicode pane ids");

        DockLayoutSnapshot corrupt = DockLayoutSnapshotCodec.decode("DLS1|%%%|S[H|not-a-number|L[%%%|%%%]");
        expect(corrupt.activePaneId().isEmpty()
                        && corrupt.root().kind() == DockNode.Kind.LEAF
                        && corrupt.root().paneIds().isEmpty(),
                "Corrupt persisted dock snapshots should decode to an empty fallback layout");

        DockLayoutSnapshot.Node splitWithBadRatio = new DockLayoutSnapshot.Node(
                DockNode.Kind.SPLIT,
                null,
                Float.NaN,
                List.of(),
                "",
                DockLayoutSnapshot.Node.emptyLeaf(),
                DockLayoutSnapshot.Node.emptyLeaf());
        expect(splitWithBadRatio.orientation() == DockSplitOrientation.HORIZONTAL
                        && near(splitWithBadRatio.splitRatio(), 0.5f),
                "Snapshot split nodes should normalize missing orientation and non-finite ratios");
    }

    private void testDockEventsCopyCancellationAcrossRoutedSnapshots() {
        DockingRoot root = new DockingRoot();
        DockLayoutSnapshot snapshot = root.manager().snapshot();

        var changed = new dev.sixik.unigui.api.event.DockLayoutChangedEvent(root, "test", "pane", "target");
        changed.cancel();
        expect(changed.routeTo(root, dev.sixik.unigui.api.event.EventPhase.CAPTURE).isCancelled(),
                "DockLayoutChangedEvent.routeTo should copy cancellation state");

        var restored = new dev.sixik.unigui.api.event.DockLayoutRestoredEvent(root, snapshot, 1, 0);
        restored.cancel();
        expect(restored.routeTo(root, dev.sixik.unigui.api.event.EventPhase.BUBBLE).isCancelled(),
                "DockLayoutRestoredEvent.routeTo should copy cancellation state");

        DockingRoot target = new DockingRoot();
        DockPane known = pane("known", "Known");
        DockPane missing = pane("missing", "Missing");
        root.addPane(known);
        root.splitPane(known.id(), DockArea.RIGHT, missing);
        DockLayoutSnapshot restoreSnapshot = root.manager().snapshot();
        Counter restoreCounter = new Counter();
        target.onLayoutRestored(event -> {
            restoreCounter.count++;
            restoreCounter.restored = event.restoredPaneCount();
            restoreCounter.missing = event.missingPaneCount();
        });

        target.restoreLayout(restoreSnapshot, Map.of(known.id(), pane("known", "Known Restored")));
        expect(target.manager().paneCount() == 1
                        && target.manager().containsPane(known.id())
                        && restoreCounter.count == 1
                        && restoreCounter.restored == 1
                        && restoreCounter.missing == 1,
                "Restoring with missing panes should prune unknown ids and report missing counts");
    }

    private void testReorderPaneMovesWithinLeaf() {
        DockingRoot root = new DockingRoot();
        DockPane scene = pane("scene", "Scene");
        DockPane inspector = pane("inspector", "Inspector");
        DockPane log = pane("log", "Log");
        root.addPane(scene);
        root.tabPane(scene.id(), inspector);
        root.tabPane(scene.id(), log);
        root.selectPane("scene");
        expect(order(root).equals(java.util.List.of("scene", "inspector", "log")),
                "Fixture should tab three panes into one leaf in insertion order");

        expect(root.manager().reorderPane("scene", "log"),
                "Reorder should move the dragged pane onto the hovered tab position");
        expect(order(root).equals(java.util.List.of("inspector", "log", "scene"))
                        && root.manager().selectedPane().id().equals("scene"),
                "reorderPane should shift the rest and keep the dragged pane selected");

        expect(root.manager().reorderPane("scene", "inspector"),
                "Reorder back to the front should work the same way");
        expect(order(root).equals(java.util.List.of("scene", "inspector", "log")),
                "Moving the last tab before the first should restore the original order");

        expect(!root.manager().reorderPane("scene", "scene")
                        && !root.manager().reorderPane("scene", "ghost")
                        && !root.manager().reorderPane("ghost", "log")
                        && order(root).equals(java.util.List.of("scene", "inspector", "log")),
                "Self, unknown and foreign ids should be a no-op that preserves the order");

        root.splitPane("scene", DockArea.RIGHT, pane("side", "Side"));
        expect(!root.manager().reorderPane("scene", "side")
                        && root.manager().containsPane("scene")
                        && root.manager().containsPane("side"),
                "Reorder across leaves should be a no-op: cross-leaf moves go through drop");
    }

    private static java.util.List<String> order(DockingRoot root) {
        java.util.List<String> ids = new java.util.ArrayList<>();
        for (DockPane pane : root.manager().panes()) {
            ids.add(pane.id());
        }
        return ids;
    }

    private static DockPane pane(String id, String title) {
        Label label = new Label(title + " body");
        label.layout(style -> style.size(90.0f, 18.0f).flexGrow(1).flexShrink(1.0f));
        return new DockPane(id, title, label);
    }

    private static boolean near(float left, float right) {
        return Math.abs(left - right) < 0.01f;
    }

    private static void expect(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static final class Counter {
        private int count;
        private int restored;
        private int missing;
    }
}