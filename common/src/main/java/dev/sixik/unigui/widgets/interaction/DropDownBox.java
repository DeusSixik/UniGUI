package dev.sixik.unigui.widgets.interaction;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.core.UIContext;
import dev.sixik.unigui.api.event.Event;
import dev.sixik.unigui.api.event.EventPhase;
import dev.sixik.unigui.api.event.KeyPressedEvent;
import dev.sixik.unigui.api.input.KeyCodes;
import dev.sixik.unigui.api.layout.Alignment;
import dev.sixik.unigui.api.layout.EdgeInsets;
import dev.sixik.unigui.api.layout.LayoutConstraints;
import dev.sixik.unigui.api.layout.Overflow;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.text.RichText;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.api.xml.XmlAttribute;
import dev.sixik.unigui.api.xml.XmlWidgetName;

import java.util.Objects;
import dev.sixik.unigui.widgets.containers.Box;
import dev.sixik.unigui.widgets.containers.LinearBox;
import dev.sixik.unigui.widgets.containers.ScrollView;
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
@XmlWidgetName("DropDownBox")
public class DropDownBox extends LinearBox {
    private static final float HEADER_HEIGHT = 22.0f;
    private static final float CONTENT_INSET = 2.0f;
    private static final float DEFAULT_DROP_DOWN_ROW_HEIGHT = HEADER_HEIGHT;
    private static final int DEFAULT_MAX_VISIBLE_ROWS = 6;
    private static final float DEFAULT_MAX_CONTENT_HEIGHT = DEFAULT_DROP_DOWN_ROW_HEIGHT * DEFAULT_MAX_VISIBLE_ROWS;

    private final Button headerButton = new Button();
    private final Box contentHost = new Box();
    private final ScrollView contentScroll = new ScrollView();
    private final Popup dropDownPopup = new Popup();
    private Widget content;
    private String headerText = "Open...";
    private RichText richHeaderText = RichText.plain(headerText);
    private ComboBox.DropDownMode dropDownMode = ComboBox.DropDownMode.OVERLAY;
    private OverlayLayer explicitOverlayLayer;
    private OverlayLayer attachedOverlayLayer;
    private float dropDownWidth;
    private boolean dropDownMatchesWidgetWidth;
    private float maxContentHeight = DEFAULT_MAX_CONTENT_HEIGHT;
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
        contentHost.layout(style -> style
                .size(LayoutConstraints.AUTO, LayoutConstraints.AUTO)
                .overflow(Overflow.HIDDEN)
                .flexGrow(0)
                .flexShrink(0.0f));

        contentScroll.scrollStep(DEFAULT_DROP_DOWN_ROW_HEIGHT);
        contentScroll.scrollbarGap(1.0f);
        contentScroll.scrollbarTrackColor().set(0.0f, 0.0f, 0.0f, 0.36f);
        contentScroll.scrollbarThumbColor().set(0.82f, 0.84f, 0.88f, 0.82f);
        contentScroll.layout(style -> style
                .size(LayoutConstraints.AUTO, LayoutConstraints.AUTO)
                .maxHeight(DEFAULT_MAX_CONTENT_HEIGHT)
                .margin(CONTENT_INSET)
                .align(Alignment.STRETCH, Alignment.START)
                .overflowX(Overflow.HIDDEN)
                .overflowY(Overflow.HIDDEN)
                .flexGrow(0)
                .flexShrink(0.0f));
        contentHost.addChild(contentScroll);

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
        this.content = content;
        contentScroll.content(content);
        contentScroll.scrollTo(0.0f, 0.0f);
        syncDropDownAttachment();
        syncDropDownVisibility();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public String headerText() {
        return headerText;
    }

    @XmlAttribute(value = "headerText", category = "Content", defaultValue = "Open...", description = "Header button text when the dropdown is closed.")
    public DropDownBox headerText(String headerText) {
        String normalized = normalize(headerText);
        if (Objects.equals(this.headerText, normalized)) return this;
        this.headerText = normalized;
        this.richHeaderText = RichText.resolve(normalized);
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

    @XmlAttribute(value = "opened", category = "Behavior", defaultValue = "false", description = "Whether the dropdown content starts open.")
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

    @XmlAttribute(value = "dropDownMode", category = "Behavior", defaultValue = "overlay", description = "Whether dropdown content is shown inline or in an overlay popup.")
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

    public ScrollView contentScroll() {
        return contentScroll;
    }

    public float dropDownWidth() {
        return dropDownWidth;
    }

    @XmlAttribute(value = "dropDownWidth", category = "Layout", defaultValue = "0", description = "Explicit dropdown width; 0 uses automatic sizing.")
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

    @XmlAttribute(value = "dropDownMatchesWidgetWidth", category = "Layout", defaultValue = "false", description = "Whether dropdown width follows the host widget width.")
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

    public float maxContentHeight() {
        return maxContentHeight;
    }

    @XmlAttribute(value = "maxContentHeight", category = "Layout", defaultValue = "132", description = "Maximum dropdown content height before scrolling is used; 0 disables the cap.")
    public DropDownBox maxContentHeight(float height) {
        float normalized = Float.isFinite(height) && height > 0.0f ? height : 0.0f;
        if (maxContentHeight == normalized) return this;
        maxContentHeight = normalized;
        syncDropDownSize();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public DropDownBox unrestrictedContentHeight() {
        return maxContentHeight(0.0f);
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
        float width = resolvedWidth > 0.0f ? resolvedWidth : LayoutConstraints.AUTO;
        float scrollWidth = resolvedWidth > CONTENT_INSET * 2.0f ? resolvedWidth - CONTENT_INSET * 2.0f : LayoutConstraints.AUTO;
        float hostMaxHeight = maxContentHeight > 0.0f
                ? maxContentHeight + CONTENT_INSET * 2.0f
                : Float.POSITIVE_INFINITY;
        float scrollMaxHeight = maxContentHeight > 0.0f ? maxContentHeight : Float.POSITIVE_INFINITY;
        Overflow verticalOverflow = shouldScrollContent() ? Overflow.SCROLL : Overflow.HIDDEN;

        contentHost.layout(style -> style
                .size(width, LayoutConstraints.AUTO)
                .maxHeight(hostMaxHeight)
                .overflow(Overflow.HIDDEN)
                .flexGrow(0)
                .flexShrink(0.0f));
        contentScroll.layout(style -> style
                .size(scrollWidth, LayoutConstraints.AUTO)
                .maxHeight(scrollMaxHeight)
                .margin(CONTENT_INSET)
                .align(Alignment.STRETCH, Alignment.START)
                .overflowX(Overflow.HIDDEN)
                .overflowY(verticalOverflow)
                .flexGrow(0)
                .flexShrink(0.0f));
    }

    private boolean shouldScrollContent() {
        if (maxContentHeight <= 0.0f || content == null) {
            return false;
        }
        float contentHeight = content.desiredSize().height() + content.layoutConstraints().margin().vertical();
        return contentHeight > maxContentHeight + 0.5f;
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
