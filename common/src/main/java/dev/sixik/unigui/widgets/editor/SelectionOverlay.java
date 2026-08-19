package dev.sixik.unigui.widgets.editor;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.core.UIContext;
import dev.sixik.unigui.api.event.Event;
import dev.sixik.unigui.api.event.EventPhase;
import dev.sixik.unigui.api.event.EventSubscription;
import dev.sixik.unigui.api.event.PointerEvent;
import dev.sixik.unigui.api.event.PointerMovedEvent;
import dev.sixik.unigui.api.event.PointerPressedEvent;
import dev.sixik.unigui.api.event.PointerReleasedEvent;
import dev.sixik.unigui.api.input.MouseCursor;
import dev.sixik.unigui.api.input.PointerButton;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.xml.XmlAttribute;
import dev.sixik.unigui.api.xml.XmlWidgetDocument;
import dev.sixik.unigui.api.xml.XmlWidgetDocumentResult;
import dev.sixik.unigui.api.xml.XmlWidgetElement;
import dev.sixik.unigui.api.xml.XmlWidgetLayoutFrame;
import dev.sixik.unigui.api.xml.XmlWidgetLayoutHandle;
import dev.sixik.unigui.api.xml.XmlWidgetLayoutHandles;
import dev.sixik.unigui.api.xml.XmlWidgetName;
import dev.sixik.unigui.api.xml.XmlWidgetNode;
import dev.sixik.unigui.api.xml.XmlWidgetNodePath;
import dev.sixik.unigui.api.xml.XmlWidgetSelectionModel;
import dev.sixik.unigui.impl.widget.WidgetBase;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

@XmlWidgetName("SelectionOverlay")
public class SelectionOverlay extends WidgetBase {
    public static final String STYLE_TYPE = dev.sixik.unigui.api.style.StyleIds.Widget.SELECTION_OVERLAY;

    private static final float DEFAULT_HANDLE_SIZE = 6.0f;
    private static final float DEFAULT_OUTLINE_THICKNESS = 1.0f;

    private final MutableColor selectionColor = new MutableColor(0.25f, 0.78f, 1.0f, 1.0f);
    private final MutableColor hoverColor = new MutableColor(1.0f, 0.82f, 0.25f, 0.82f);
    private final MutableColor handleFillColor = new MutableColor(0.07f, 0.11f, 0.16f, 0.95f);
    private final MutableColor handleBorderColor = new MutableColor(0.25f, 0.78f, 1.0f, 1.0f);
    private final XmlWidgetSelectionModel selection = new XmlWidgetSelectionModel();
    private XmlWidgetDocument document;
    private XmlWidgetNodePath hoverPath;
    private XmlWidgetLayoutHandle hotHandle;
    private XmlWidgetLayoutHandle activeHandle;
    private XmlWidgetNodePath activePath;
    private int activePointerId = -1;
    private float dragStartX;
    private float dragStartY;
    private float handleSize = DEFAULT_HANDLE_SIZE;
    private float outlineThickness = DEFAULT_OUTLINE_THICKNESS;
    private float minResizeWidth = 1.0f;
    private float minResizeHeight = 1.0f;
    private boolean editMode = true;
    private boolean resizeHandlesVisible = true;
    private boolean moveHandleVisible = true;
    private XmlWidgetDocumentResult lastResult;
    private XmlWidgetDocumentResult pendingDragResult;
    private XmlWidgetNodePath pendingDragPath;
    private XmlWidgetLayoutHandle pendingDragHandle;
    private float pendingDragDeltaX;
    private float pendingDragDeltaY;
    private Function<XmlWidgetNodePath, Optional<XmlWidgetLayoutFrame>> frameResolver;
    private Consumer<DocumentChange> documentChanged = change -> {
    };
    private Consumer<SelectionChange> selectionChanged = change -> {
    };

    public SelectionOverlay() {
        focusable(false);
        mouseCursor(MouseCursor.DEFAULT);
        selectionColor.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
        hoverColor.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
        handleFillColor.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
        handleBorderColor.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
    }

    public XmlWidgetDocument document() {
        return document;
    }

    public SelectionOverlay document(XmlWidgetDocument document) {
        if (this.document == document) return this;
        this.document = document;
        hoverPath = null;
        if (document == null) {
            selection.clear();
        } else if (!selection.validFor(document)) {
            selection.clear();
        }
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public XmlWidgetSelectionModel selectionModel() {
        return selection;
    }

    public Optional<XmlWidgetNodePath> selectedPath() {
        return selection.selectedPath();
    }

    public SelectionOverlay selectedPath(XmlWidgetNodePath path) {
        Optional<XmlWidgetNodePath> previous = selectedPath();
        if (path == null) {
            selection.clear();
        } else if (document == null || path.resolve(document).isPresent()) {
            selection.select(path);
        } else {
            selection.clear();
        }
        Optional<XmlWidgetNodePath> next = selectedPath();
        if (!Objects.equals(previous, next)) {
            selectionChanged.accept(new SelectionChange(
                    this,
                    previous.orElse(null),
                    next.orElse(null)));
        }
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public Optional<XmlWidgetNodePath> hoverPath() {
        return Optional.ofNullable(hoverPath);
    }

    public Optional<XmlWidgetLayoutHandle> hotHandle() {
        return Optional.ofNullable(hotHandle);
    }

    public Optional<XmlWidgetLayoutHandle> activeHandle() {
        return Optional.ofNullable(activeHandle);
    }

    public Optional<XmlWidgetDocumentResult> lastResult() {
        return Optional.ofNullable(lastResult);
    }

    public boolean editMode() {
        return editMode;
    }

    @XmlAttribute(value = "editMode", category = "Behavior", defaultValue = "true", description = "Whether the overlay consumes pointer events for editor selection and handles.")
    public SelectionOverlay editMode(boolean editMode) {
        if (this.editMode == editMode) return this;
        this.editMode = editMode;
        if (!editMode) cancelDrag();
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public boolean resizeHandlesVisible() {
        return resizeHandlesVisible;
    }

    @XmlAttribute(value = "resizeHandlesVisible", category = "Behavior", defaultValue = "true", description = "Whether selected widgets show resize handles.")
    public SelectionOverlay resizeHandlesVisible(boolean resizeHandlesVisible) {
        if (this.resizeHandlesVisible == resizeHandlesVisible) return this;
        this.resizeHandlesVisible = resizeHandlesVisible;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public boolean moveHandleVisible() {
        return moveHandleVisible;
    }

    @XmlAttribute(value = "moveHandleVisible", category = "Behavior", defaultValue = "true", description = "Whether selected widget bodies can be dragged as move handles.")
    public SelectionOverlay moveHandleVisible(boolean moveHandleVisible) {
        if (this.moveHandleVisible == moveHandleVisible) return this;
        this.moveHandleVisible = moveHandleVisible;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public float handleSize() {
        return handleSize;
    }

    @XmlAttribute(value = "handleSize", category = "Layout", defaultValue = "6", description = "Resize handle size in UI pixels.")
    public SelectionOverlay handleSize(float handleSize) {
        float normalized = Float.isFinite(handleSize) && handleSize > 0.0f ? handleSize : DEFAULT_HANDLE_SIZE;
        if (this.handleSize == normalized) return this;
        this.handleSize = normalized;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public float outlineThickness() {
        return outlineThickness;
    }

    @XmlAttribute(value = "outlineThickness", category = "Appearance", defaultValue = "1", description = "Outline stroke thickness in UI pixels.")
    public SelectionOverlay outlineThickness(float outlineThickness) {
        float normalized = Float.isFinite(outlineThickness) && outlineThickness > 0.0f ? outlineThickness : DEFAULT_OUTLINE_THICKNESS;
        if (this.outlineThickness == normalized) return this;
        this.outlineThickness = normalized;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public float minResizeWidth() {
        return minResizeWidth;
    }

    @XmlAttribute(value = "minResizeWidth", category = "Layout", defaultValue = "1", description = "Minimum width applied by resize handles.")
    public SelectionOverlay minResizeWidth(float minResizeWidth) {
        this.minResizeWidth = Float.isFinite(minResizeWidth) ? Math.max(0.0f, minResizeWidth) : 1.0f;
        return this;
    }

    public float minResizeHeight() {
        return minResizeHeight;
    }

    @XmlAttribute(value = "minResizeHeight", category = "Layout", defaultValue = "1", description = "Minimum height applied by resize handles.")
    public SelectionOverlay minResizeHeight(float minResizeHeight) {
        this.minResizeHeight = Float.isFinite(minResizeHeight) ? Math.max(0.0f, minResizeHeight) : 1.0f;
        return this;
    }

    public MutableColor selectionColor() {
        return selectionColor;
    }

    @XmlAttribute(value = "selectionColor", category = "Appearance", defaultValue = "#40C7FFFF", description = "Selected widget outline color.")
    public SelectionOverlay selectionColor(ColorView color) {
        if (color != null) selectionColor.set(color);
        return this;
    }

    public MutableColor hoverColor() {
        return hoverColor;
    }

    @XmlAttribute(value = "hoverColor", category = "Appearance", defaultValue = "#FFD13FD1", description = "Hovered widget outline color.")
    public SelectionOverlay hoverColor(ColorView color) {
        if (color != null) hoverColor.set(color);
        return this;
    }

    public MutableColor handleFillColor() {
        return handleFillColor;
    }

    public MutableColor handleBorderColor() {
        return handleBorderColor;
    }

    public EventSubscription onDocumentChanged(Consumer<DocumentChange> listener) {
        documentChanged = listener == null ? change -> {
        } : listener;
        return () -> documentChanged = change -> {
        };
    }

    public EventSubscription onSelectionChanged(Consumer<SelectionChange> listener) {
        selectionChanged = listener == null ? change -> {
        } : listener;
        return () -> selectionChanged = change -> {
        };
    }

    public SelectionOverlay frameResolver(Function<XmlWidgetNodePath, Optional<XmlWidgetLayoutFrame>> frameResolver) {
        if (this.frameResolver == frameResolver) return this;
        this.frameResolver = frameResolver;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public Optional<XmlWidgetLayoutFrame> selectedFrame() {
        return selectedPath().flatMap(this::frameFor);
    }

    public Optional<XmlWidgetLayoutFrame> hoverFrame() {
        return hoverPath == null ? Optional.empty() : frameFor(hoverPath);
    }

    public Optional<XmlWidgetNodePath> pathAt(float localX, float localY) {
        if (document == null) return Optional.empty();
        List<PathFrame> frames = new ArrayList<>();
        collectFrames(document.root(), XmlWidgetNodePath.root(), frames);
        PathFrame best = null;
        for (PathFrame frame : frames) {
            if (!contains(frame.frame(), localX, localY)) continue;
            if (best == null || frame.path().depth() >= best.path().depth()) {
                best = frame;
            }
        }
        return best == null ? Optional.empty() : Optional.of(best.path());
    }

    public Optional<XmlWidgetLayoutHandle> handleAt(float localX, float localY) {
        Optional<XmlWidgetLayoutFrame> frame = selectedFrame();
        if (frame.isEmpty()) return Optional.empty();
        XmlWidgetLayoutFrame selectedFrame = frame.get();
        if (resizeHandlesVisible) {
            for (XmlWidgetLayoutHandle handle : resizeHandles()) {
                if (handleContains(selectedFrame, handle, localX, localY)) return Optional.of(handle);
            }
        }
        if (moveHandleVisible && contains(selectedFrame, localX, localY)) return Optional.of(XmlWidgetLayoutHandle.MOVE);
        return Optional.empty();
    }

    @Override
    public void measure(LayoutContext context) {
        if (visibility() == Visibility.COLLAPSED) {
            setDesiredSize(0.0f, 0.0f);
            return;
        }
        float width = context == null || !Float.isFinite(context.availableWidth()) ? 0.0f : context.availableWidth();
        float height = context == null || !Float.isFinite(context.availableHeight()) ? 0.0f : context.availableHeight();
        setDesiredSize(resolveDesiredSize(context, width, height));
    }

    @Override
    public void render(RenderContext context) {
        if (visibility() != Visibility.VISIBLE || context == null) return;
        pushOpacity(context);
        try {
            renderHover(context);
            renderSelection(context);
        } finally {
            popOpacity(context);
        }
    }

    @Override
    public void handle(Event event) {
        if (visibility() != Visibility.VISIBLE || !enabled()) return;
        super.handle(event);
        if (!editMode || event.isCancelled()) return;
        if (event instanceof PointerPressedEvent pointer && pointer.phase() == EventPhase.TARGET) {
            handlePointerPressed(pointer);
        } else if (event instanceof PointerMovedEvent pointer && pointer.phase() == EventPhase.TARGET) {
            handlePointerMoved(pointer);
        } else if (event instanceof PointerReleasedEvent pointer && pointer.phase() == EventPhase.TARGET) {
            handlePointerReleased(pointer);
        }
    }

    private void handlePointerPressed(PointerPressedEvent pointer) {
        if (pointer.button() != PointerButton.PRIMARY) return;
        Optional<XmlWidgetLayoutHandle> handle = handleAt(pointer.localX(), pointer.localY());
        if (handle.isPresent() && selectedPath().isPresent()) {
            startDrag(selectedPath().orElseThrow(), handle.get(), pointer);
            pointer.cancel();
            return;
        }
        Optional<XmlWidgetNodePath> hit = pathAt(pointer.localX(), pointer.localY());
        if (hit.isPresent()) {
            selectedPath(hit.get());
            startDrag(hit.get(), XmlWidgetLayoutHandle.MOVE, pointer);
        } else {
            selectedPath(null);
            hoverPath = null;
        }
        pointer.cancel();
        invalidate(InvalidationFlags.VISUAL);
    }

    private void handlePointerMoved(PointerMovedEvent pointer) {
        if (activeHandle != null && pointer.pointerId() != activePointerId) return;
        if (activeHandle != null && activePath != null && document != null) {
            float deltaX = pointer.localX() - dragStartX;
            float deltaY = pointer.localY() - dragStartY;
            XmlWidgetDocumentResult result = activeHandle.move()
                    ? XmlWidgetLayoutHandles.move(document, activePath, deltaX, deltaY)
                    : XmlWidgetLayoutHandles.resize(document, activePath, activeHandle, deltaX, deltaY, minResizeWidth, minResizeHeight);
            applyResult(result, activePath, activeHandle, deltaX, deltaY);
            dragStartX = pointer.localX();
            dragStartY = pointer.localY();
            pointer.cancel();
            return;
        }
        Optional<XmlWidgetLayoutHandle> handle = handleAt(pointer.localX(), pointer.localY());
        XmlWidgetLayoutHandle nextHotHandle = handle.orElse(null);
        XmlWidgetNodePath nextHoverPath = handle.isPresent()
                ? selectedPath().orElse(null)
                : pathAt(pointer.localX(), pointer.localY()).orElse(null);
        if (!Objects.equals(hoverPath, nextHoverPath) || hotHandle != nextHotHandle) {
            hoverPath = nextHoverPath;
            hotHandle = nextHotHandle;
            mouseCursor(cursorFor(hotHandle));
            invalidate(InvalidationFlags.VISUAL);
        }
        if (nextHoverPath != null || nextHotHandle != null) pointer.cancel();
    }

    private void handlePointerReleased(PointerReleasedEvent pointer) {
        if (activeHandle == null) return;
        if (pointer.pointerId() != activePointerId) return;
        XmlWidgetDocumentResult commitResult = pendingDragResult;
        XmlWidgetNodePath commitPath = pendingDragPath == null ? activePath : pendingDragPath;
        XmlWidgetLayoutHandle commitHandle = pendingDragHandle == null ? activeHandle : pendingDragHandle;
        float commitDeltaX = pendingDragDeltaX;
        float commitDeltaY = pendingDragDeltaY;
        cancelDrag();
        if (commitResult != null) {
            documentChanged.accept(new DocumentChange(this, commitResult, commitPath, commitHandle, commitDeltaX, commitDeltaY, true));
        }
        clearPendingDragResult();
        pointer.cancel();
    }

    private void startDrag(XmlWidgetNodePath path, XmlWidgetLayoutHandle handle, PointerEvent pointer) {
        clearPendingDragResult();
        activePath = path;
        activeHandle = handle == null ? XmlWidgetLayoutHandle.MOVE : handle;
        hotHandle = activeHandle;
        activePointerId = pointer.pointerId();
        dragStartX = pointer.localX();
        dragStartY = pointer.localY();
        mouseCursor(cursorFor(activeHandle));
        UIContext context = uiContext();
        if (context != null) context.capturePointer(pointer.pointerId(), this);
        invalidate(InvalidationFlags.VISUAL);
    }

    private void cancelDrag() {
        int pointerId = activePointerId;
        activeHandle = null;
        activePath = null;
        activePointerId = -1;
        mouseCursor(cursorFor(hotHandle));
        UIContext context = uiContext();
        if (context != null && pointerId >= 0) context.releasePointer(pointerId, this);
        invalidate(InvalidationFlags.VISUAL);
    }

    private void applyResult(XmlWidgetDocumentResult result,
                             XmlWidgetNodePath path,
                             XmlWidgetLayoutHandle handle,
                             float deltaX,
                             float deltaY) {
        lastResult = result;
        pendingDragResult = result;
        pendingDragPath = path;
        pendingDragHandle = handle;
        pendingDragDeltaX = deltaX;
        pendingDragDeltaY = deltaY;
        if (result.valid()) {
            document = result.document();
            selection.selectIfPresent(document, path);
        }
        documentChanged.accept(new DocumentChange(this, result, path, handle, deltaX, deltaY, false));
        invalidate(InvalidationFlags.VISUAL);
    }

    private void clearPendingDragResult() {
        pendingDragResult = null;
        pendingDragPath = null;
        pendingDragHandle = null;
        pendingDragDeltaX = 0.0f;
        pendingDragDeltaY = 0.0f;
    }

    private void renderHover(RenderContext context) {
        if (hoverPath == null || selectedPath().filter(hoverPath::equals).isPresent()) return;
        frameFor(hoverPath).ifPresent(frame -> drawOutline(context, frame, hoverColor, outlineThickness, true));
    }

    private void renderSelection(RenderContext context) {
        Optional<XmlWidgetLayoutFrame> frame = selectedFrame();
        if (frame.isEmpty()) return;
        XmlWidgetLayoutFrame selected = frame.get();
        drawOutline(context, selected, selectionColor, outlineThickness, false);
        if (resizeHandlesVisible) {
            for (XmlWidgetLayoutHandle handle : resizeHandles()) {
                drawHandle(context, selected, handle);
            }
        }
    }

    private void drawOutline(RenderContext context, XmlWidgetLayoutFrame frame, ColorView color, float thickness, boolean dashed) {
        float x = layoutBounds().x() + frame.x();
        float y = layoutBounds().y() + frame.y();
        Paint paint = Paint.stroke(color, thickness);
        if (dashed) paint.dash(4.0f, 3.0f);
        context.rect(x, y, frame.width(), frame.height(), paint);
    }

    private void drawHandle(RenderContext context, XmlWidgetLayoutFrame frame, XmlWidgetLayoutHandle handle) {
        float[] center = handleCenter(frame, handle);
        float x = layoutBounds().x() + center[0] - handleSize * 0.5f;
        float y = layoutBounds().y() + center[1] - handleSize * 0.5f;
        context.addRectFilled(x, y, handleSize, handleSize, 1.0f, handleFillColor);
        context.addRect(x, y, handleSize, handleSize, 1.0f, handleBorderColor, 1.0f);
    }

    private Optional<XmlWidgetLayoutFrame> frameFor(XmlWidgetNodePath path) {
        if (document == null || path == null) return Optional.empty();
        if (frameResolver != null) {
            Optional<XmlWidgetLayoutFrame> resolved = frameResolver.apply(path);
            if (resolved != null && resolved.isPresent()) {
                return resolved;
            }
        }
        return path.resolveElement(document).flatMap(XmlWidgetLayoutHandles::frame);
    }

    private void collectFrames(XmlWidgetElement element, XmlWidgetNodePath path, List<PathFrame> frames) {
        frameFor(path).ifPresent(frame -> frames.add(new PathFrame(path, frame)));
        List<XmlWidgetNode> children = element.children();
        for (int i = 0; i < children.size(); i++) {
            if (children.get(i) instanceof XmlWidgetElement child) {
                collectFrames(child, path.child(i), frames);
            }
        }
    }

    private boolean handleContains(XmlWidgetLayoutFrame frame, XmlWidgetLayoutHandle handle, float localX, float localY) {
        float[] center = handleCenter(frame, handle);
        float half = handleSize * 0.5f;
        return localX >= center[0] - half && localX <= center[0] + half
                && localY >= center[1] - half && localY <= center[1] + half;
    }

    private static boolean contains(XmlWidgetLayoutFrame frame, float localX, float localY) {
        return localX >= frame.x() && localY >= frame.y()
                && localX <= frame.x() + frame.width()
                && localY <= frame.y() + frame.height();
    }

    private static float[] handleCenter(XmlWidgetLayoutFrame frame, XmlWidgetLayoutHandle handle) {
        float left = frame.x();
        float centerX = frame.x() + frame.width() * 0.5f;
        float right = frame.x() + frame.width();
        float top = frame.y();
        float centerY = frame.y() + frame.height() * 0.5f;
        float bottom = frame.y() + frame.height();
        return switch (handle) {
            case NORTH -> new float[]{centerX, top};
            case SOUTH -> new float[]{centerX, bottom};
            case EAST -> new float[]{right, centerY};
            case WEST -> new float[]{left, centerY};
            case NORTH_EAST -> new float[]{right, top};
            case NORTH_WEST -> new float[]{left, top};
            case SOUTH_EAST -> new float[]{right, bottom};
            case SOUTH_WEST -> new float[]{left, bottom};
            case MOVE -> new float[]{centerX, centerY};
        };
    }

    private static XmlWidgetLayoutHandle[] resizeHandles() {
        return new XmlWidgetLayoutHandle[]{
                XmlWidgetLayoutHandle.NORTH_WEST,
                XmlWidgetLayoutHandle.NORTH,
                XmlWidgetLayoutHandle.NORTH_EAST,
                XmlWidgetLayoutHandle.WEST,
                XmlWidgetLayoutHandle.EAST,
                XmlWidgetLayoutHandle.SOUTH_WEST,
                XmlWidgetLayoutHandle.SOUTH,
                XmlWidgetLayoutHandle.SOUTH_EAST
        };
    }

    private static MouseCursor cursorFor(XmlWidgetLayoutHandle handle) {
        if (handle == null) return MouseCursor.DEFAULT;
        if (handle.move()) return MouseCursor.CROSSHAIR;
        if (handle.east() || handle.west()) return MouseCursor.RESIZE_HORIZONTAL;
        if (handle.north() || handle.south()) return MouseCursor.RESIZE_VERTICAL;
        return MouseCursor.DEFAULT;
    }

    private record PathFrame(XmlWidgetNodePath path, XmlWidgetLayoutFrame frame) {
    }

    public record DocumentChange(SelectionOverlay overlay,
                                 XmlWidgetDocumentResult result,
                                 XmlWidgetNodePath path,
                                 XmlWidgetLayoutHandle handle,
                                 float deltaX,
                                 float deltaY,
                                 boolean finalChange) {
    }

    public record SelectionChange(SelectionOverlay overlay,
                                  XmlWidgetNodePath previousPath,
                                  XmlWidgetNodePath path) {
    }
}
