package dev.sixik.unigui.tests;

import dev.sixik.unigui.impl.core.DefaultUIContext;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.widgets.containers.PanelWidget;
import dev.sixik.unigui.widgets.editor.DragAndDropManager;
import dev.sixik.unigui.widgets.editor.DragSource;
import dev.sixik.unigui.widgets.editor.DropTarget;

import java.util.concurrent.atomic.AtomicInteger;

/** Самопроверка общего drag-and-drop цикла источника и target. */
public final class DragAndDropSelfTest {
    public static void main(String[] args) {
        PanelWidget root = new PanelWidget();
        root.setUiContextInternal(new DefaultUIContext());

        DragSource source = new DragSource()
                .payloadId("palette.label")
                .payloadType("widget")
                .dragPreview("Label");
        DropTarget target = new DropTarget()
                .acceptedPayloadTypes("widget");
        AtomicInteger drops = new AtomicInteger();
        target.onDrop(event -> drops.incrementAndGet());

        root.addChild(source);
        root.addChild(target);
        root.applyQueuedMutations();
        root.arrange(new MutableRect(0.0f, 0.0f, 200.0f, 40.0f));
        source.arrange(new MutableRect(0.0f, 0.0f, 20.0f, 20.0f));
        target.arrange(new MutableRect(40.0f, 0.0f, 80.0f, 20.0f));

        DragAndDropManager manager = new DragAndDropManager(root)
                .register(source)
                .register(target);

        expect(source.startDrag(5.0f, 5.0f), "source should start drag");
        source.moveDrag(60.0f, 5.0f);
        expect(manager.activeTarget() == target && target.dropPreviewActive(),
                "manager should preview the target under the pointer");
        expect(source.finishDrag(60.0f, 5.0f, DropTarget.DropResult.IGNORED),
                "source should finish drag through manager resolution");
        expect(drops.get() == 1 && source.lastDropResult() == DropTarget.DropResult.ACCEPTED,
                "manager should dispatch one accepted drop and report its result");
        expect(manager.activeSource() == null && manager.activeTarget() == null,
                "manager should clear the active session after drop");

        System.out.println("DragAndDropSelfTest passed");
    }

    private static void expect(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
