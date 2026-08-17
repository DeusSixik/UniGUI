package dev.sixik.unigui.widgets.editor;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.event.EventSubscription;
import dev.sixik.unigui.api.layout.Alignment;
import dev.sixik.unigui.api.layout.LayoutConstraints;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.math.TransformGeometry;
import dev.sixik.unigui.api.widget.RenderedBoundsMapper;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.api.xml.XMLWidget;
import dev.sixik.unigui.api.xml.XmlAttribute;
import dev.sixik.unigui.api.xml.XmlWidgetDocument;
import dev.sixik.unigui.api.xml.XmlWidgetDocumentResult;
import dev.sixik.unigui.api.xml.XmlWidgetElement;
import dev.sixik.unigui.api.xml.XmlWidgetLoadException;
import dev.sixik.unigui.api.xml.XmlWidgetLayoutFrame;
import dev.sixik.unigui.api.xml.XmlWidgetName;
import dev.sixik.unigui.api.xml.XmlWidgetNode;
import dev.sixik.unigui.api.xml.XmlWidgetNodePath;
import dev.sixik.unigui.api.xml.XmlWidgetOptions;
import dev.sixik.unigui.api.xml.XmlWidgetRegistry;
import dev.sixik.unigui.api.xml.XmlWidgetSerializationOptions;
import dev.sixik.unigui.api.xml.editor.XmlEditorDiagnosticChannel;
import dev.sixik.unigui.api.xml.editor.XmlEditorMode;
import dev.sixik.unigui.api.xml.editor.XmlEditorSession;
import dev.sixik.unigui.widgets.containers.Box;
import dev.sixik.unigui.widgets.containers.PanelWidget;
import dev.sixik.unigui.widgets.containers.ScrollView;
import dev.sixik.unigui.widgets.containers.StackPanel;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Design-surface host that layers XML preview, grid and editor selection overlay. */
@XmlWidgetName("XmlDesignCanvas")
public class XmlDesignCanvas extends PanelWidget {
    private static final XmlWidgetSerializationOptions PREVIEW_XML =
            XmlWidgetSerializationOptions.COMPACT.xmlDeclaration(false);
    private static final XmlWidgetSerializationOptions EDITOR_XML =
            XmlWidgetSerializationOptions.PRETTY.xmlDeclaration(false);

    private final Box background = new Box();
    private final GridOverlay gridOverlay = new GridOverlay();
    private final StackPanel previewHost = new StackPanel();
    private final DesignCanvasOverlay overlay = new DesignCanvasOverlay();
    private final EventSubscription overlayDocumentSubscription;
    private final EventSubscription overlaySelectionSubscription;

    private XmlEditorSession session;
    private EventSubscription sessionSubscription;
    private XmlWidgetDocument document;
    private Widget previewRoot;
    private final Map<XmlWidgetNodePath, Widget> previewWidgets = new LinkedHashMap<>();
    private String previewXml = "";
    private String previewError = "";
    private XmlWidgetDocumentResult lastDesignResult;
    private boolean syncingOverlaySelection;

    public XmlDesignCanvas() {
        background.themeEnabled(false)
                .backgroundVisible(true)
                .borderVisible(true)
                .radius(4.0f);
        background.background().set(0.020f, 0.024f, 0.032f, 0.98f);
        background.borderColor().set(0.22f, 0.30f, 0.40f, 0.82f);

        stretch(background);
        stretch(gridOverlay);
        stretch(previewHost);
        stretch(overlay);

        gridOverlay.spacing(16.0f).majorEvery(4).snapSize(8.0f).snapEnabled(true);
        overlay.editMode(true).resizeHandlesVisible(true).moveHandleVisible(true).outlineThickness(1.0f);
        overlay.frameResolver(this::previewFrameFor);

        addChild(background);
        addChild(gridOverlay);
        addChild(previewHost);
        addChild(overlay);
        applyQueuedMutations();

        overlayDocumentSubscription = overlay.onDocumentChanged(this::handleOverlayDocumentChange);
        overlaySelectionSubscription = overlay.onSelectionChanged(this::handleOverlaySelectionChange);
    }

    public Optional<XmlEditorSession> session() {
        return Optional.ofNullable(session);
    }

    public XmlDesignCanvas session(XmlEditorSession session) {
        if (this.session == session) return this;
        if (sessionSubscription != null) {
            sessionSubscription.close();
            sessionSubscription = null;
        }
        this.session = session;
        if (session != null) {
            sessionSubscription = session.onChanged(change -> refreshFromSession());
        }
        refreshFromSession();
        return this;
    }

    public Optional<XmlWidgetDocument> document() {
        return Optional.ofNullable(document);
    }

    public XmlDesignCanvas document(XmlWidgetDocument document) {
        if (this.document == document) {
            overlay.document(document);
            rebuildPreview();
            invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
            return this;
        }
        this.document = document;
        overlay.document(document);
        rebuildPreview();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public Optional<Widget> previewRoot() {
        return Optional.ofNullable(previewRoot);
    }

    public Optional<Widget> previewWidget(XmlWidgetNodePath path) {
        return Optional.ofNullable(previewWidgets.get(path == null ? XmlWidgetNodePath.root() : path));
    }

    public Map<XmlWidgetNodePath, Widget> previewWidgets() {
        return Map.copyOf(previewWidgets);
    }

    public String previewXml() {
        return previewXml;
    }

    public String previewError() {
        return previewError;
    }

    public Optional<XmlWidgetDocumentResult> lastDesignResult() {
        return Optional.ofNullable(lastDesignResult);
    }

    public Box backgroundLayer() {
        return background;
    }

    public GridOverlay gridOverlay() {
        return gridOverlay;
    }

    public StackPanel previewHost() {
        return previewHost;
    }

    public DesignCanvasOverlay overlay() {
        return overlay;
    }

    public boolean gridVisible() {
        return gridOverlay.gridVisible();
    }

    @XmlAttribute(value = "gridVisible", category = "Behavior", defaultValue = "true", description = "Whether the design canvas grid is rendered.")
    public XmlDesignCanvas gridVisible(boolean gridVisible) {
        gridOverlay.gridVisible(gridVisible);
        return this;
    }

    public boolean editMode() {
        return overlay.editMode();
    }

    @XmlAttribute(value = "editMode", category = "Behavior", defaultValue = "true", description = "Whether the design canvas overlay consumes selection and transform input.")
    public XmlDesignCanvas editMode(boolean editMode) {
        overlay.editMode(editMode);
        return this;
    }

    public Optional<XmlWidgetNodePath> selectedPath() {
        return overlay.selectedPath();
    }

    public XmlDesignCanvas selectedPath(XmlWidgetNodePath path) {
        syncOverlaySelection(path);
        if (session != null) {
            session.select(path);
        }
        return this;
    }

    public Optional<XmlWidgetNodePath> selectAt(float localX, float localY) {
        Optional<XmlWidgetNodePath> hit = overlay.pathAt(localX, localY);
        selectedPath(hit.orElse(null));
        return hit;
    }

    public boolean insertElement(XmlWidgetElement element) {
        return insertElement(element, selectedPath().orElse(XmlWidgetNodePath.root()), Integer.MAX_VALUE);
    }

    public boolean insertElement(XmlWidgetElement element, XmlWidgetNodePath parentPath, int index) {
        if (session == null || element == null || !editMode()) return false;
        XmlWidgetNodePath normalizedParent = parentPath == null ? XmlWidgetNodePath.root() : parentPath;
        return session.insertChild(normalizedParent, index, element);
    }

    public DropTarget.DropResult dropPaletteRequest(WidgetPalette.PaletteInsertRequest request, float localX, float localY) {
        if (request == null || !editMode()) return DropTarget.DropResult.IGNORED;
        XmlWidgetNodePath targetPath = overlay.pathAt(localX, localY).orElse(request.parentPath());
        return insertElement(request.element(), targetPath, request.index())
                ? DropTarget.DropResult.ACCEPTED
                : DropTarget.DropResult.REJECTED;
    }

    public boolean rebuildPreview() {
        if (document == null) {
            replacePreviewRoot(null, "");
            previewError = "";
            return true;
        }
        String xml = document.toXmlString(PREVIEW_XML);
        if (Objects.equals(previewXml, xml) && previewRoot != null) {
            previewError = "";
            return true;
        }
        try {
            Widget nextRoot = XMLWidget.createScreen(xml, registry(), previewOptions()).root();
            replacePreviewRoot(nextRoot, xml);
            previewError = "";
            return true;
        } catch (XmlWidgetLoadException failure) {
            previewError = failure.getMessage();
            return false;
        }
    }

    public void refreshFromSession() {
        if (session == null) return;
        document(session.document());
        syncOverlaySelection(session.selectedPath().orElse(null));
        overlay.editMode(session.mode() != XmlEditorMode.RUNTIME);
    }

    public boolean applyDesignResult(XmlWidgetDocumentResult result, XmlWidgetNodePath path) {
        if (result == null) return false;
        lastDesignResult = result;
        if (!result.valid()) {
            if (session != null) {
                session.setDiagnostics(XmlEditorDiagnosticChannel.EDIT, result.diagnostics());
            }
            return false;
        }

        XmlWidgetDocument nextDocument = result.document();
        document(nextDocument);
        syncOverlaySelection(path);
        if (session != null) {
            session.mode(XmlEditorMode.DESIGN);
            session.replaceText(nextDocument.toXmlString(EDITOR_XML));
            session.select(path);
        }
        return true;
    }

    @Override
    public void dispose() {
        if (sessionSubscription != null) {
            sessionSubscription.close();
            sessionSubscription = null;
        }
        overlayDocumentSubscription.close();
        overlaySelectionSubscription.close();
        super.dispose();
    }

    private void handleOverlayDocumentChange(SelectionOverlay.DocumentChange change) {
        applyDesignResult(change.result(), change.path());
    }

    private void handleOverlaySelectionChange(SelectionOverlay.SelectionChange change) {
        if (syncingOverlaySelection || session == null) return;
        session.select(change.path());
    }

    private void syncOverlaySelection(XmlWidgetNodePath path) {
        syncingOverlaySelection = true;
        try {
            overlay.selectedPath(path);
        } finally {
            syncingOverlaySelection = false;
        }
    }

    private XmlWidgetRegistry registry() {
        return session == null ? XMLWidget.registry() : session.registry();
    }

    private XmlWidgetOptions previewOptions() {
        XmlWidgetOptions options = XmlWidgetOptions.lenient();
        return session == null ? options : options.commands(session.commands());
    }

    private void replacePreviewRoot(Widget nextRoot, String xml) {
        previewHost.clearChildren();
        if (nextRoot != null) {
            previewHost.addChild(nextRoot);
        }
        previewHost.applyQueuedMutations();
        previewRoot = nextRoot;
        previewXml = xml == null ? "" : xml;
        rebuildPreviewWidgetMap();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
    }

    private void rebuildPreviewWidgetMap() {
        previewWidgets.clear();
        if (document == null || previewRoot == null) return;
        mapPreviewWidget(document.root(), XmlWidgetNodePath.root(), previewRoot);
    }

    private Optional<XmlWidgetLayoutFrame> previewFrameFor(XmlWidgetNodePath path) {
        Widget widget = previewWidgets.get(path == null ? XmlWidgetNodePath.root() : path);
        if (widget == null) return Optional.empty();
        RectView bounds = transformedPreviewBounds(widget);
        if (bounds == null || !isUsableBounds(bounds)) return Optional.empty();
        return Optional.of(new XmlWidgetLayoutFrame(
                bounds.x() - overlay.layoutBounds().x(),
                bounds.y() - overlay.layoutBounds().y(),
                bounds.width(),
                bounds.height()));
    }

    private RectView transformedPreviewBounds(Widget widget) {
        if (widget == null) return null;
        List<Widget> route = previewRoute(widget);
        if (route.isEmpty()) return null;

        MutableRect bounds = new MutableRect(
                widget.layoutBounds().x(),
                widget.layoutBounds().y(),
                widget.layoutBounds().width(),
                widget.layoutBounds().height());
        for (int index = route.size() - 1; index >= 0; index--) {
            Widget current = route.get(index);
            TransformGeometry.transformBoundsInto(bounds, bounds, current.layoutBounds(), current.transform());
            if (index > 0 && route.get(index - 1) instanceof RenderedBoundsMapper mapper) {
                RectView mapped = mapper.renderedBoundsForChild(current, bounds);
                if (mapped != null) {
                    bounds.set(mapped);
                }
            }
        }
        return bounds;
    }

    private List<Widget> previewRoute(Widget widget) {
        ArrayList<Widget> reversed = new ArrayList<>();
        Widget current = widget;
        while (current != null && current != this) {
            reversed.add(current);
            current = current.parent();
        }
        if (current != this) return List.of();

        ArrayList<Widget> route = new ArrayList<>(reversed.size());
        for (int i = reversed.size() - 1; i >= 0; i--) {
            route.add(reversed.get(i));
        }
        return route;
    }

    private static boolean isUsableBounds(RectView bounds) {
        return bounds != null
                && Float.isFinite(bounds.x())
                && Float.isFinite(bounds.y())
                && Float.isFinite(bounds.width())
                && Float.isFinite(bounds.height())
                && (bounds.width() > 0.0f || bounds.height() > 0.0f);
    }

    private void mapPreviewWidget(XmlWidgetElement element, XmlWidgetNodePath path, Widget widget) {
        if (element == null || path == null || widget == null) return;
        previewWidgets.put(path, widget);

        List<Widget> runtimeChildren = runtimeChildren(widget);
        int runtimeIndex = 0;
        List<XmlWidgetNode> sourceChildren = element.children();
        for (int sourceIndex = 0; sourceIndex < sourceChildren.size(); sourceIndex++) {
            XmlWidgetNode sourceChild = sourceChildren.get(sourceIndex);
            if (!(sourceChild instanceof XmlWidgetElement sourceElement)) continue;
            if (sourceElement.name().contains(".")) continue;
            if (runtimeIndex >= runtimeChildren.size()) break;
            mapPreviewWidget(sourceElement, path.child(sourceIndex), runtimeChildren.get(runtimeIndex));
            runtimeIndex++;
        }
    }

    private static List<Widget> runtimeChildren(Widget widget) {
        if (widget == null) return List.of();
        List<Widget> children = new ArrayList<>(widget.children());
        if (widget instanceof ScrollView scrollView
                && scrollView.content() != null
                && !children.contains(scrollView.content())) {
            children.add(scrollView.content());
        }
        return children;
    }

    private static void stretch(dev.sixik.unigui.impl.widget.WidgetBase widget) {
        widget.layout(style -> style.size(LayoutConstraints.AUTO, LayoutConstraints.AUTO)
                .align(Alignment.STRETCH, Alignment.STRETCH)
                .flexGrow(1.0f)
                .flexShrink(1.0f));
    }
}
