package dev.sixik.unigui.tests;

import dev.sixik.unigui.impl.core.DefaultUIContext;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.widgets.containers.VBox;
import dev.sixik.unigui.widgets.editor.ElementsInspector;
import dev.sixik.unigui.widgets.feedback.OverlayLayer;

/** Самопроверка runtime-инспектора и его undo-команд над живым деревом. */
public final class ElementsInspectorSelfTest {
    public static void main(String[] args) {
        VBox root = new VBox();
        OverlayLayer overlays = new OverlayLayer(root);
        overlays.setUiContextInternal(new DefaultUIContext());

        ElementsInspector inspector = new ElementsInspector().attach(overlays, root);
        inspector.open();
        expect(inspector.collapsed(), "inspector should open in collapsed mode");
        inspector.measure(new LayoutContext(800.0f, 600.0f));
        expect(inspector.desiredSize().height() <= 80.0f,
                "collapsed inspector should measure close to its title bar");
        inspector.collapsed(false);
        inspector.measure(new LayoutContext(800.0f, 600.0f));
        expect(inspector.desiredSize().height() > 80.0f,
                "expanded inspector should measure its content");
        expect(inspector.selectedWidget().orElse(null) == root,
                "inspector should select the inspected root after refresh");

        inspector.addDefaultChild();
        expect(root.children().size() == 1,
                "inspector Add should mutate the live panel tree");
        inspector.undo();
        expect(root.children().isEmpty(),
                "inspector undo should remove the added live child");
        inspector.redo();
        expect(root.children().size() == 1,
                "inspector redo should restore the added live child");
        inspector.select(root.children().get(0));
        inspector.deleteSelected();
        expect(root.children().isEmpty(),
                "inspector Delete should mutate the live panel tree");

        System.out.println("ElementsInspectorSelfTest passed");
    }

    private static void expect(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
