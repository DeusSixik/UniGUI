package dev.sixik.unigui.widgets.interaction;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.core.UIContext;
import dev.sixik.unigui.api.event.Event;
import dev.sixik.unigui.api.event.EventPhase;
import dev.sixik.unigui.api.event.KeyPressedEvent;
import dev.sixik.unigui.api.input.KeyCodes;
import dev.sixik.unigui.api.layout.EdgeInsets;
import dev.sixik.unigui.api.layout.LayoutConstraints;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.text.RichText;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.widget.Widget;

import java.util.Objects;
import dev.sixik.unigui.widgets.containers.Box;
import dev.sixik.unigui.widgets.containers.LinearBox;
import dev.sixik.unigui.widgets.feedback.OverlayLayer;
import dev.sixik.unigui.widgets.core.Orientation;
import dev.sixik.unigui.widgets.feedback.Popup;

/**
 * Generic drop-down host for arbitrary popup content.
 *
 * <p>Use {@link ComboBox} when the drop-down content is a selectable list.
 * DropDownBox intentionally owns only the header/open state and a single
 * content widget.</p>
 */
public class DropDownBox extends LinearBox {
    private static final float HEADER_HEIGHT = 22.0f;

    private final Button headerButton = new Button();
    private final Box contentHost = new Box();
    private final Popup dropDownPopup = new Popup();
    private Widget content;
    private String headerText = "Open...";
    private RichText richHeaderText = RichText.plain(headerText);
    private ComboBox.DropDownMode dropDownMode = ComboBox.DropDownMode.OVERLAY;
    private OverlayLayer explicitOverlayLayer;
    private OverlayLayer attachedOverlayLayer;
    private float dropDownWidth;
    private boolean dropDownMatchesWidgetWidth;
    private boolean syncingPopup;
    private boolean opened;

    public DropDownBox() {
        super(Orientation.VERTICAL);
        spacing(2.0f);
        focusable(true);

        headerButton.layout(style -> style.size(LayoutConstraints.AUTO, HEADER_HEIGHT).flexGrow(0).flexShrink(0.0f));
        headerButton.onClick(event -> toggle());

        contentHost.backgroundVisible(true);
        contentHost.borderVisible(true);
        contentHost.radius(3.0f);
        contentHost.background().set(0.025f, 0.030f, 0.040f, 0.97f);
        contentHost.borderColor().set(0.25f, 0.78f, 1.0f, 0.75f);
        contentHost.visibility(Visibility.COLLAPSED);
        contentHost.layout(style -> style.size(LayoutConstraints.AUTO, LayoutConstraints.AUTO).flexGrow(0).flexShrink(0.0f));

        dropDownPopup.anchor(headerButton);
        dropDownPopup.padding(EdgeInsets.all(0.0f));
        dropDownPopup.backgroundVisible(false);
        dropDownPopup.borderVisible(false);
        dropDownPopup.closeOnOutsideClick(true);
        dropDownPopup.onOpenChanged(() -> {
            if (!syncingPopup && dropDownMode == ComboBox.DropDownMode.OVERLAY && opened != dropDownPopup.opened()) {
                opened = dropDownPopup.opened();
                updateHeaderText();
                invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
            }
        });

        super.addChild(headerButton);
        updateHeaderText();
    }

    public Widget content() {
        return content;
    }

    public DropDownBox content(Widget content) {
        if (this.content == content) return this;
        if (this.content != null) {
            contentHost.removeChild(this.content);
        }
        this.content = content;
        if (content != null) {
            contentHost.addChild(content);
        }
        syncDropDownAttachment();
        syncDropDownVisibility();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public String headerText() {
        return headerText;
    }

    public DropDownBox headerText(String headerText) {
        String normalized = normalize(headerText);
        if (Objects.equals(this.headerText, normalized)) return this;
        this.headerText = normalized;
        this.richHeaderText = RichText.plain(normalized);
        updateHeaderText();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public RichText richHeaderText() {
        return richHeaderText;
    }

    public DropDownBox richHeaderText(RichText headerText) {
        RichText normalized = headerText == null ? RichText.plain("") : headerText;
        if (Objects.equals(this.richHeaderText, normalized)) return this;
        this.richHeaderText = normalized;
        this.headerText = normalized.plainText();
        updateHeaderText();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public boolean opened() {
        return opened;
    }

    public DropDownBox open() {
        return opened(true);
    }

    public DropDownBox close() {
        return opened(false);
    }

    public DropDownBox toggle() {
        return opened(!opened);
    }

    public DropDownBox opened(boolean opened) {
        if (this.opened == opened) return this;
        this.opened = opened;
        syncDropDownAttachment();
        syncDropDownVisibility();
        updateHeaderText();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public ComboBox.DropDownMode dropDownMode() {
        return dropDownMode;
    }

    public DropDownBox dropDownMode(ComboBox.DropDownMode dropDownMode) {
        ComboBox.DropDownMode normalized = dropDownMode == null ? ComboBox.DropDownMode.INLINE : dropDownMode;
        if (this.dropDownMode == normalized) return this;
        this.dropDownMode = normalized;
        syncDropDownAttachment();
        syncDropDownVisibility();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public DropDownBox useInline() {
        return dropDownMode(ComboBox.DropDownMode.INLINE);
    }

    public DropDownBox useOverlay() {
        return dropDownMode(ComboBox.DropDownMode.OVERLAY);
    }

    public DropDownBox useOverlay(OverlayLayer overlayLayer) {
        overlayLayer(overlayLayer);
        return useOverlay();
    }

    public OverlayLayer overlayLayer() {
        return explicitOverlayLayer != null ? explicitOverlayLayer : findTopmostOverlayLayer();
    }

    public DropDownBox overlayLayer(OverlayLayer overlayLayer) {
        if (this.explicitOverlayLayer == overlayLayer) return this;
        detachFromOverlay();
        this.explicitOverlayLayer = overlayLayer;
        syncDropDownAttachment();
        syncDropDownVisibility();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public OverlayLayer attachedOverlayLayer() {
        return attachedOverlayLayer;
    }

    public Popup dropDownPopup() {
        return dropDownPopup;
    }

    public Button headerButton() {
        return headerButton;
    }

    public Box contentHost() {
        return contentHost;
    }

    public float dropDownWidth() {
        return dropDownWidth;
    }

    public DropDownBox dropDownWidth(float width) {
        float normalized = Float.isFinite(width) ? Math.max(0.0f, width) : 0.0f;
        if (dropDownWidth == normalized && !dropDownMatchesWidgetWidth) return this;
        dropDownWidth = normalized;
        dropDownMatchesWidgetWidth = false;
        syncDropDownSize();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public boolean dropDownMatchesWidgetWidth() {
        return dropDownMatchesWidgetWidth;
    }

    public DropDownBox dropDownMatchesWidgetWidth(boolean match) {
        if (dropDownMatchesWidgetWidth == match && (match || dropDownWidth <= 0.0f)) return this;
        dropDownMatchesWidgetWidth = match;
        if (match) {
            dropDownWidth = 0.0f;
        }
        syncDropDownSize();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public DropDownBox dropDownSameWidth() {
        return dropDownMatchesWidgetWidth(true);
    }

    public DropDownBox autoDropDownWidth() {
        boolean changed = dropDownWidth > 0.0f || dropDownMatchesWidgetWidth;
        dropDownWidth = 0.0f;
        dropDownMatchesWidgetWidth = false;
        if (changed) {
            syncDropDownSize();
            invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        }
        return this;
    }

    @Override
    public void addChild(Widget child) {
        content(child);
    }

    @Override
    public void clearChildren() {
        content(null);
    }

    @Override
    public void setParentInternal(Widget parent) {
        super.setParentInternal(parent);
        syncDropDownAttachment();
        syncDropDownVisibility();
    }

    @Override
    public void setUiContextInternal(UIContext uiContext) {
        super.setUiContextInternal(uiContext);
        syncDropDownAttachment();
        syncDropDownVisibility();
    }

    @Override
    public void handle(Event event) {
        super.handle(event);
        if (event.isCancelled()) return;
        if (event instanceof KeyPressedEvent key
                && key.phase() == EventPhase.TARGET
                && uiContext() != null
                && uiContext().focusManager().isFocused(this)) {
            if (key.keyCode() == KeyCodes.SPACE || key.keyCode() == KeyCodes.ENTER || key.keyCode() == KeyCodes.KEYPAD_ENTER) {
                toggle();
                event.cancel();
            } else if (key.keyCode() == KeyCodes.ESCAPE && opened) {
                close();
                event.cancel();
            }
        }
    }

    @Override
    public void arrange(RectView bounds) {
        super.arrange(bounds);
        syncDropDownSize();
    }

    private void updateHeaderText() {
        headerButton.richText(richHeaderText.append(RichText.plain(" ?")));
    }

    private void syncDropDownAttachment() {
        syncDropDownSize();
        if (dropDownMode == ComboBox.DropDownMode.INLINE) {
            detachFromOverlay();
            clearPopupContent();
            if (contentHost.parent() != this) {
                super.addChild(contentHost);
            }
            return;
        }

        detachContentFromInlineLayout();

        OverlayLayer targetOverlay = overlayLayer();
        if (targetOverlay != null) {
            if (dropDownPopup.content() != contentHost) {
                dropDownPopup.content(contentHost);
            }
            if (attachedOverlayLayer != targetOverlay) {
                detachFromOverlay();
                attachedOverlayLayer = targetOverlay;
                attachedOverlayLayer.addOverlay(dropDownPopup);
            }
        } else {
            detachFromOverlay();
            clearPopupContent();
        }
    }

    private void syncDropDownVisibility() {
        syncDropDownSize();
        if (dropDownMode == ComboBox.DropDownMode.OVERLAY) {
            contentHost.visibility(attachedOverlayLayer != null ? Visibility.VISIBLE : Visibility.COLLAPSED);
            syncingPopup = true;
            try {
                dropDownPopup.open(opened && attachedOverlayLayer != null);
            } finally {
                syncingPopup = false;
            }
        } else {
            syncingPopup = true;
            try {
                dropDownPopup.close();
            } finally {
                syncingPopup = false;
            }
            contentHost.visibility(opened ? Visibility.VISIBLE : Visibility.COLLAPSED);
        }
    }

    private void detachContentFromInlineLayout() {
        if (contentHost.parent() == this) {
            super.removeChild(contentHost);
            applyQueuedMutations();
        }
    }

    private void clearPopupContent() {
        syncingPopup = true;
        try {
            dropDownPopup.close();
            if (dropDownPopup.content() != null) {
                dropDownPopup.content(null);
                dropDownPopup.applyQueuedMutations();
            }
        } finally {
            syncingPopup = false;
        }
    }

    private void detachFromOverlay() {
        if (attachedOverlayLayer != null) {
            attachedOverlayLayer.removeOverlay(dropDownPopup);
            attachedOverlayLayer = null;
        }
    }

    private void syncDropDownSize() {
        float resolvedWidth = resolvedDropDownWidth();
        if (resolvedWidth > 0.0f) {
            contentHost.layout(style -> style.size(resolvedWidth, LayoutConstraints.AUTO).flexGrow(0).flexShrink(0.0f));
        } else {
            contentHost.layout(style -> style.size(LayoutConstraints.AUTO, LayoutConstraints.AUTO).flexGrow(0).flexShrink(0.0f));
        }
    }

    private float resolvedDropDownWidth() {
        if (dropDownWidth > 0.0f) {
            return dropDownWidth;
        }
        if (dropDownMatchesWidgetWidth) {
            float width = layoutBounds().width();
            if (width <= 0.0f && desiredSize().width() > 0.0f) {
                width = desiredSize().width();
            }
            if (width <= 0.0f && headerButton.layoutBounds().width() > 0.0f) {
                width = headerButton.layoutBounds().width();
            }
            if (width <= 0.0f && headerButton.desiredSize().width() > 0.0f) {
                width = headerButton.desiredSize().width();
            }
            return Math.max(0.0f, width);
        }
        return 0.0f;
    }

    private OverlayLayer findTopmostOverlayLayer() {
        OverlayLayer result = null;
        Widget current = this;
        while (current != null) {
            if (current instanceof OverlayLayer layer) {
                result = layer;
            }
            current = current.parent();
        }
        return result;
    }

    private static String normalize(String value) {
        return value == null ? "" : value;
    }
}
