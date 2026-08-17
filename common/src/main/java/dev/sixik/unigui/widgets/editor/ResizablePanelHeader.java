package dev.sixik.unigui.widgets.editor;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.event.EventSubscription;
import dev.sixik.unigui.api.xml.XmlAttribute;
import dev.sixik.unigui.api.xml.XmlWidgetName;
import dev.sixik.unigui.widgets.docking.DockPane;
import dev.sixik.unigui.widgets.interaction.ToolButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/** Pane header variant with a resize affordance for custom splitter-backed editor panels. */
@XmlWidgetName("ResizablePanelHeader")
public class ResizablePanelHeader extends PaneHeader {
    private final ToolButton resizeButton = new ToolButton();
    private final List<Consumer<PanelResizeRequest>> resizeListeners = new ArrayList<>();
    private ResizeEdge resizeEdge = ResizeEdge.RIGHT;
    private boolean resizeHandleVisible = true;
    private boolean resizingEnabled = true;
    private float panelSize = 240.0f;
    private float minPanelSize = 96.0f;
    private float maxPanelSize = 10000.0f;

    public ResizablePanelHeader() {
        super();
        configureActionButton(resizeButton, "||");
        resizeButton.tooltip("Resize");
        resizeButton.onClick(event -> requestResize(panelSize));
        addChild(resizeButton);
        applyQueuedMutations();
        refreshResize();
    }

    public ToolButton resizeButton() {
        return resizeButton;
    }

    public ResizeEdge resizeEdge() {
        return resizeEdge;
    }

    @XmlAttribute(value = "resizeEdge", category = "Behavior", defaultValue = "right", description = "Panel edge controlled by the resize affordance.")
    public ResizablePanelHeader resizeEdge(ResizeEdge resizeEdge) {
        this.resizeEdge = resizeEdge == null ? ResizeEdge.RIGHT : resizeEdge;
        refreshResize();
        return this;
    }

    public boolean resizeHandleVisible() {
        return resizeHandleVisible;
    }

    @XmlAttribute(value = "resizeHandleVisible", category = "Behavior", defaultValue = "true", description = "Whether the resize affordance is shown.")
    public ResizablePanelHeader resizeHandleVisible(boolean resizeHandleVisible) {
        if (this.resizeHandleVisible == resizeHandleVisible) return this;
        this.resizeHandleVisible = resizeHandleVisible;
        refreshResize();
        return this;
    }

    public boolean resizingEnabled() {
        return resizingEnabled;
    }

    @XmlAttribute(value = "resizingEnabled", category = "Behavior", defaultValue = "true", description = "Whether resize requests can be emitted.")
    public ResizablePanelHeader resizingEnabled(boolean resizingEnabled) {
        if (this.resizingEnabled == resizingEnabled) return this;
        this.resizingEnabled = resizingEnabled;
        refreshResize();
        return this;
    }

    public float panelSize() {
        return panelSize;
    }

    @XmlAttribute(value = "panelSize", category = "State", defaultValue = "240", description = "Current panel size controlled by the header.")
    public ResizablePanelHeader panelSize(float panelSize) {
        float normalized = clamp(panelSize);
        if (this.panelSize == normalized) return this;
        this.panelSize = normalized;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public float minPanelSize() {
        return minPanelSize;
    }

    @XmlAttribute(value = "minPanelSize", category = "Layout", defaultValue = "96", description = "Minimum size allowed for resize requests.")
    public ResizablePanelHeader minPanelSize(float minPanelSize) {
        this.minPanelSize = sanitizeSize(minPanelSize, 96.0f);
        if (maxPanelSize < this.minPanelSize) {
            maxPanelSize = this.minPanelSize;
        }
        panelSize(panelSize);
        return this;
    }

    public float maxPanelSize() {
        return maxPanelSize;
    }

    @XmlAttribute(value = "maxPanelSize", category = "Layout", defaultValue = "10000", description = "Maximum size allowed for resize requests.")
    public ResizablePanelHeader maxPanelSize(float maxPanelSize) {
        this.maxPanelSize = Math.max(minPanelSize, sanitizeSize(maxPanelSize, 10000.0f));
        panelSize(panelSize);
        return this;
    }

    public ResizablePanelHeader sizeRange(float minPanelSize, float maxPanelSize) {
        this.minPanelSize = sanitizeSize(minPanelSize, 96.0f);
        this.maxPanelSize = Math.max(this.minPanelSize, sanitizeSize(maxPanelSize, 10000.0f));
        panelSize(panelSize);
        return this;
    }

    public boolean requestResize(float requestedSize) {
        if (!resizingEnabled) return false;
        float oldSize = panelSize;
        float newSize = clamp(requestedSize);
        panelSize = newSize;
        PanelResizeRequest request = new PanelResizeRequest(this, paneId(), resizeEdge, oldSize, newSize);
        List<Consumer<PanelResizeRequest>> snapshot = List.copyOf(resizeListeners);
        for (Consumer<PanelResizeRequest> listener : snapshot) {
            listener.accept(request);
        }
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return true;
    }

    public EventSubscription onResizeRequested(Consumer<PanelResizeRequest> listener) {
        Objects.requireNonNull(listener, "listener");
        resizeListeners.add(listener);
        return () -> resizeListeners.remove(listener);
    }

    @Override
    public ResizablePanelHeader pane(DockPane pane) {
        super.pane(pane);
        return this;
    }

    @Override
    @XmlAttribute(value = "paneId", category = "State", defaultValue = "", description = "Editor pane id represented by this header.")
    public ResizablePanelHeader paneId(String paneId) {
        super.paneId(paneId);
        return this;
    }

    @Override
    @XmlAttribute(value = "title", category = "Content", defaultValue = "Pane", description = "Pane title shown in the header.")
    public ResizablePanelHeader title(String title) {
        super.title(title);
        return this;
    }

    @Override
    @XmlAttribute(value = "dirty", category = "State", defaultValue = "false", description = "Whether the pane has unsaved changes.")
    public ResizablePanelHeader dirty(boolean dirty) {
        super.dirty(dirty);
        return this;
    }

    @Override
    @XmlAttribute(value = "pinned", category = "State", defaultValue = "true", description = "Whether the pane is pinned instead of auto-hidden.")
    public ResizablePanelHeader pinned(boolean pinned) {
        super.pinned(pinned);
        return this;
    }

    @Override
    @XmlAttribute(value = "closable", category = "Behavior", defaultValue = "true", description = "Whether the close action is enabled.")
    public ResizablePanelHeader closable(boolean closable) {
        super.closable(closable);
        return this;
    }

    @Override
    @XmlAttribute(value = "pinVisible", category = "Behavior", defaultValue = "true", description = "Whether the pin toggle is shown.")
    public ResizablePanelHeader pinVisible(boolean pinVisible) {
        super.pinVisible(pinVisible);
        return this;
    }

    @Override
    @XmlAttribute(value = "menuVisible", category = "Behavior", defaultValue = "true", description = "Whether the pane menu button is shown.")
    public ResizablePanelHeader menuVisible(boolean menuVisible) {
        super.menuVisible(menuVisible);
        return this;
    }

    @Override
    @XmlAttribute(value = "closeVisible", category = "Behavior", defaultValue = "true", description = "Whether the close button is shown when closable.")
    public ResizablePanelHeader closeVisible(boolean closeVisible) {
        super.closeVisible(closeVisible);
        return this;
    }

    private void refreshResize() {
        resizeButton.visible(resizeHandleVisible);
        resizeButton.enabled(resizingEnabled);
        resizeButton.icon(iconFor(resizeEdge));
        resizeButton.tooltip("Resize " + resizeEdge.name().toLowerCase(java.util.Locale.ROOT));
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
    }

    private float clamp(float value) {
        float sanitized = sanitizeSize(value, panelSize);
        return Math.max(minPanelSize, Math.min(maxPanelSize, sanitized));
    }

    private static float sanitizeSize(float value, float fallback) {
        return Float.isFinite(value) ? Math.max(0.0f, value) : fallback;
    }

    private static String iconFor(ResizeEdge edge) {
        return switch (edge == null ? ResizeEdge.RIGHT : edge) {
            case LEFT, RIGHT -> "||";
            case TOP, BOTTOM -> "=";
        };
    }

    public enum ResizeEdge {
        LEFT,
        RIGHT,
        TOP,
        BOTTOM
    }

    public record PanelResizeRequest(ResizablePanelHeader source,
                                     String paneId,
                                     ResizeEdge edge,
                                     float oldSize,
                                     float newSize) {
        public PanelResizeRequest {
            Objects.requireNonNull(source, "source");
            paneId = paneId == null ? "" : paneId.trim();
            edge = edge == null ? ResizeEdge.RIGHT : edge;
        }
    }
}
