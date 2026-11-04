package dev.sixik.unigui.demo;

import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.demo.ui.editor.UiEditorScreen;
import dev.sixik.unigui.widgets.containers.PanelWidget;
import dev.sixik.unigui.widgets.docking.DockingRoot;
import dev.sixik.unigui.widgets.interaction.Slider;
import dev.sixik.unigui.xaml.Xaml;
import dev.sixik.unigui.xaml.load.DocumentValidator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UiEditorScreenTest {
    @Test
    void theSampleDocumentIsValidAndLoadsStrictly() {
        DocumentValidator.Result result = new DocumentValidator(Xaml.defaultSchema()).validate(UiEditorScreen.SAMPLE);
        assertTrue(result.valid(), result.diagnostics().toString());
        assertNotNull(Xaml.load(UiEditorScreen.SAMPLE));
    }

    @Test
    void theScreenShowsThePreviewOfTheSampleInsideTheDock() {
        Widget root = UiEditorScreen.createScreen().root();
        DockingRoot dock = find(root, DockingRoot.class);
        assertNotNull(dock, "the editor window has a dock area");
        assertNotNull(dock.findPane("viewport"));
        Widget preview = dock.findPane("viewport").content();
        applyAll(preview);
        assertTrue(contains(preview, Slider.class), "the live preview contains the sample slider");
    }

    private static void applyAll(Widget widget) {
        if (widget instanceof PanelWidget panel) panel.applyQueuedMutations();
        for (Widget child : widget.children()) applyAll(child);
    }

    private static boolean contains(Widget widget, Class<?> type) {
        if (type.isInstance(widget)) return true;
        for (Widget child : widget.children()) if (contains(child, type)) return true;
        return false;
    }

    private static <T extends Widget> T find(Widget widget, Class<T> type) {
        if (widget instanceof PanelWidget panel) panel.applyQueuedMutations();
        if (type.isInstance(widget)) return type.cast(widget);
        for (Widget child : widget.children()) {
            T found = find(child, type);
            if (found != null) return found;
        }
        return null;
    }
}
