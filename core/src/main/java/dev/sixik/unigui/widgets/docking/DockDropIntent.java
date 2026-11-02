package dev.sixik.unigui.widgets.docking;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiLink;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.math.RectView;

@AiDoc(summary = "Resolved docking drop intention capturing the target area, preview bounds, and source/target pane identifiers."
)
@WikiPage(
        title = "DockDropIntent",
        slug = "dock-drop-intent",
        category = "<key:docking.category>",
        summary = "<key:dock.drop.intent.summary>"
)
@WikiSection(
        title = "<key:wiki.section.overview.title>",
        markdown = "<key:dock.drop.intent.section.overview.body>",
        order = 10
)
@WikiLink(
        label = "DockArea",
        target = "page:dock-area",
        description = "<key:dock.area.summary>"
)
@WikiLink(
        label = "DockingRoot",
        target = "page:docking-root",
        description = "<key:docking.root.summary>"
)
public record DockDropIntent(
        @WikiParameter(description = "<key:dock.drop.intent.param.valid>")
        boolean valid,
        @WikiParameter(description = "<key:dock.drop.intent.param.sourcePaneId>")
        String sourcePaneId,
        @WikiParameter(description = "<key:dock.drop.intent.param.targetPaneId>")
        String targetPaneId,
        @WikiParameter(description = "<key:dock.drop.intent.param.area>")
        DockArea area,
        @WikiParameter(description = "<key:dock.drop.intent.param.x>")
        float x,
        @WikiParameter(description = "<key:dock.drop.intent.param.y>")
        float y,
        @WikiParameter(description = "<key:dock.drop.intent.param.width>")
        float width,
        @WikiParameter(description = "<key:dock.drop.intent.param.height>")
        float height,
        @WikiParameter(description = "<key:dock.drop.intent.param.pointerX>")
        float pointerX,
        @WikiParameter(description = "<key:dock.drop.intent.param.pointerY>")
        float pointerY
) {
    private static final DockDropIntent NONE = new DockDropIntent(
            false, "", "", DockArea.CENTER,
            0.0f, 0.0f, 0.0f, 0.0f,
            0.0f, 0.0f);

    public DockDropIntent {
        sourcePaneId = sourcePaneId == null ? "" : sourcePaneId;
        targetPaneId = targetPaneId == null ? "" : targetPaneId;
        area = area == null ? DockArea.CENTER : area;
        if (!valid) {
            x = 0.0f;
            y = 0.0f;
            width = 0.0f;
            height = 0.0f;
        }
    }

    @AiDoc(summary = "Returns an invalid empty drop intent representing no drop target.")
    @WikiMethod(summary = "<key:dock.drop.intent.method.none>")
    @WikiReturn("<key:dock.drop.intent.return.intent>")
    public static DockDropIntent none() {
        return NONE;
    }

    @AiDoc(summary = "Creates a valid dock drop intent for the specified area and bounds.")
    @WikiMethod(summary = "<key:dock.drop.intent.method.of>")
    @WikiReturn("<key:dock.drop.intent.return.intent>")
    public static DockDropIntent of(
            @WikiParameter(description = "<key:dock.drop.intent.param.sourcePaneId>")
            String sourcePaneId,
            @WikiParameter(description = "<key:dock.drop.intent.param.targetPaneId>")
            String targetPaneId,
            @WikiParameter(description = "<key:dock.drop.intent.param.area>")
            DockArea area,
            @WikiParameter(description = "<key:dock.drop.intent.param.previewBounds>")
            RectView previewBounds,
            @WikiParameter(description = "<key:dock.drop.intent.param.pointerX>")
            float pointerX,
            @WikiParameter(description = "<key:dock.drop.intent.param.pointerY>")
            float pointerY
    ) {
        RectView bounds = previewBounds == null ? new MutableRect() : previewBounds;
        return new DockDropIntent(true, sourcePaneId, targetPaneId, area,
                bounds.x(), bounds.y(), bounds.width(), bounds.height(), pointerX, pointerY);
    }

    @AiDoc(summary = "Creates a floating tear-off drop intent around the pointer.")
    @WikiMethod(summary = "<key:dock.drop.intent.method.floating>")
    @WikiReturn("<key:dock.drop.intent.return.intent>")
    public static DockDropIntent floating(
            @WikiParameter(description = "<key:dock.drop.intent.param.sourcePaneId>")
            String sourcePaneId,
            @WikiParameter(description = "<key:dock.drop.intent.param.pointerX>")
            float pointerX,
            @WikiParameter(description = "<key:dock.drop.intent.param.pointerY>")
            float pointerY
    ) {
        return new DockDropIntent(true, sourcePaneId, "", DockArea.FLOAT,
                pointerX - 80.0f, pointerY - 11.0f, 160.0f, 96.0f, pointerX, pointerY);
    }

    @AiDoc(summary = "Returns true if this intent detaches the pane into a floating window.")
    @WikiMethod(summary = "<key:dock.drop.intent.method.isFloating>")
    @WikiReturn("<key:dock.drop.intent.return.isFloating>")
    public boolean floating() {
        return valid && area == DockArea.FLOAT;
    }

    @AiDoc(summary = "Returns true if this intent adds the pane as a tab.")
    @WikiMethod(summary = "<key:dock.drop.intent.method.isTabbed>")
    @WikiReturn("<key:dock.drop.intent.return.isTabbed>")
    public boolean tabbed() {
        return valid && (area == DockArea.CENTER || area == DockArea.TAB);
    }

    @AiDoc(summary = "Returns true if this intent splits an existing node.")
    @WikiMethod(summary = "<key:dock.drop.intent.method.isSplit>")
    @WikiReturn("<key:dock.drop.intent.return.isSplit>")
    public boolean split() {
        return valid && area != DockArea.CENTER && area != DockArea.TAB && area != DockArea.FLOAT;
    }
}
