package dev.sixik.unigui.widgets.interaction;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.event.Event;
import dev.sixik.unigui.api.event.EventListener;
import dev.sixik.unigui.api.event.EventPhase;
import dev.sixik.unigui.api.event.EventSubscription;
import dev.sixik.unigui.api.event.KeyPressedEvent;
import dev.sixik.unigui.api.event.SelectionChangedEvent;
import dev.sixik.unigui.api.core.UIContext;
import dev.sixik.unigui.api.input.KeyCodes;
import dev.sixik.unigui.api.layout.EdgeInsets;
import dev.sixik.unigui.api.layout.LayoutConstraints;
import dev.sixik.unigui.api.layout.Overflow;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.text.RichText;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.api.xml.XmlAttribute;
import dev.sixik.unigui.api.xml.XmlWidgetName;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import dev.sixik.unigui.widgets.containers.Box;
import dev.sixik.unigui.widgets.containers.LinearBox;
import dev.sixik.unigui.widgets.containers.ScrollView;
import dev.sixik.unigui.widgets.feedback.OverlayLayer;
import dev.sixik.unigui.widgets.containers.VBox;
import dev.sixik.unigui.widgets.core.Orientation;
import dev.sixik.unigui.widgets.feedback.Popup;

@XmlWidgetName("ComboBox")
public class ComboBox extends LinearBox {
    private static final float HEADER_HEIGHT = 22.0f;
    private static final float OPTION_HEIGHT = 20.0f;
    private static final float OPTIONS_INSET = 2.0f;
    private static final int DEFAULT_MAX_VISIBLE_OPTIONS = 6;

    private final Button headerButton = new Button();
    private final Box optionsHost = new Box();
    private final VBox optionsList = new VBox();
    private final ScrollView optionsScroll = new ScrollView(optionsList);
    private final Popup dropDownPopup = new Popup();
    private final List<String> items = new ObjectArrayList<>();
    private final List<RichText> richItems = new ObjectArrayList<>();
    private final List<ToggleButton> optionButtons = new ObjectArrayList<>();
    private int selectedIndex = -1;
    private boolean opened;
    private String placeholder = "Select...";
    private RichText richPlaceholder = RichText.plain(placeholder);
    private DropDownMode dropDownMode = DropDownMode.OVERLAY;
    private OverlayLayer explicitOverlayLayer;
    private OverlayLayer attachedOverlayLayer;
    private float dropDownWidth;
    private boolean dropDownMatchesWidgetWidth;
    private float optionRowHeight = OPTION_HEIGHT;
    private int maxVisibleOptions = DEFAULT_MAX_VISIBLE_OPTIONS;
    private boolean syncingPopup;

    public ComboBox() {
        super(Orientation.VERTICAL);
        spacing(2.0f);
        focusable(true);

        headerButton.layout(style -> style.size(LayoutConstraints.AUTO, HEADER_HEIGHT).flexGrow(0).flexShrink(0.0f));
        headerButton.onClick(event -> toggle());

        optionsHost.backgroundVisible(true);
        optionsHost.borderVisible(true);
        optionsHost.radius(3.0f);
        optionsHost.background().set(0.025f, 0.030f, 0.040f, 0.97f);
        optionsHost.borderColor().set(0.25f, 0.78f, 1.0f, 0.75f);
        optionsHost.visibility(Visibility.COLLAPSED);
        optionsHost.layout(style -> style.size(LayoutConstraints.AUTO, LayoutConstraints.AUTO).flexGrow(0).flexShrink(0.0f));

        optionsList.spacing(1.0f);
        optionsList.layout(style -> style.margin(0.0f));
        optionsList.layout(style -> style.flexGrow(0).flexShrink(0.0f));

        optionsScroll.scrollStep(optionRowHeight);
        optionsScroll.scrollbarGap(1.0f);
        optionsScroll.scrollbarTrackColor().set(0.0f, 0.0f, 0.0f, 0.36f);
        optionsScroll.scrollbarThumbColor().set(0.82f, 0.84f, 0.88f, 0.82f);
        optionsScroll.layout(style -> style
                .size(LayoutConstraints.AUTO, LayoutConstraints.AUTO)
                .margin(OPTIONS_INSET)
                .overflowX(Overflow.HIDDEN)
                .overflowY(Overflow.AUTO)
                .flexGrow(0)
                .flexShrink(0.0f));
        optionsHost.addChild(optionsScroll);

        dropDownPopup.anchor(headerButton);
        dropDownPopup.padding(EdgeInsets.all(0.0f));
        dropDownPopup.backgroundVisible(false);
        dropDownPopup.borderVisible(false);
        dropDownPopup.closeOnOutsideClick(true);
        dropDownPopup.onOpenChanged(() -> {
            if (!syncingPopup && dropDownMode == DropDownMode.OVERLAY && opened != dropDownPopup.opened()) {
                opened = dropDownPopup.opened();
                updateHeaderText();
                invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
            }
        });

        super.addChild(headerButton);
        updateHeaderText();
    }

    public List<String> items() {
        return Collections.unmodifiableList(items);
    }

    public ComboBox items(List<String> items) {
        this.items.clear();
        this.richItems.clear();
        if (items != null) {
            for (String item : items) {
                addItemInternal(RichText.plain(item));
            }
        }
        if (selectedIndex >= this.items.size()) {
            selectedIndex = this.items.isEmpty() ? -1 : this.items.size() - 1;
        }
        rebuildOptions();
        syncSelectionState();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public ComboBox richItems(List<RichText> items) {
        this.items.clear();
        this.richItems.clear();
        if (items != null) {
            for (RichText item : items) {
                addItemInternal(item);
            }
        }
        if (selectedIndex >= this.items.size()) {
            selectedIndex = this.items.isEmpty() ? -1 : this.items.size() - 1;
        }
        rebuildOptions();
        syncSelectionState();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public ComboBox addItem(String item) {
        addItemInternal(RichText.plain(item));
        rebuildOptions();
        if (selectedIndex < 0) {
            setSelectedIndex(0, false);
        } else {
            syncSelectionState();
        }
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public ComboBox addItem(RichText item) {
        addItemInternal(item);
        rebuildOptions();
        if (selectedIndex < 0) {
            setSelectedIndex(0, false);
        } else {
            syncSelectionState();
        }
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public ComboBox removeItem(int index) {
        if (index < 0 || index >= items.size()) return this;
        int oldSelection = selectedIndex;
        items.remove(index);
        richItems.remove(index);
        rebuildOptions();
        if (items.isEmpty()) {
            setSelectedIndex(-1, oldSelection != -1);
        } else if (oldSelection == index) {
            setSelectedIndex(Math.min(index, items.size() - 1), true);
        } else if (index < oldSelection) {
            selectedIndex = oldSelection - 1;
            syncSelectionState();
        } else {
            syncSelectionState();
        }
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public int itemCount() {
        return items.size();
    }

    public int selectedIndex() {
        return selectedIndex;
    }

    public String selectedItem() {
        return selectedIndex >= 0 && selectedIndex < items.size() ? items.get(selectedIndex) : "";
    }

    public RichText selectedRichItem() {
        return selectedIndex >= 0 && selectedIndex < richItems.size() ? richItems.get(selectedIndex) : RichText.plain("");
    }

    public ComboBox selectedIndex(int index) {
        setSelectedIndex(index, true);
        return this;
    }

    @XmlAttribute(value = "selectedIndex", category = "Behavior", defaultValue = "-1", description = "Initial selected option index without emitting change events during XML load.")
    public ComboBox silentSelectedIndex(int index) {
        setSelectedIndex(index, false);
        return this;
    }

    public boolean opened() {
        return opened;
    }

    public ComboBox open() {
        return opened(true);
    }

    public ComboBox close() {
        return opened(false);
    }

    public ComboBox toggle() {
        return opened(!opened);
    }

    @XmlAttribute(value = "opened", category = "Behavior", defaultValue = "false", description = "Whether the dropdown list starts open.")
    public ComboBox opened(boolean opened) {
        if (this.opened == opened) return this;
        this.opened = opened;
        syncDropDownAttachment();
        syncDropDownVisibility();
        updateHeaderText();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public DropDownMode dropDownMode() {
        return dropDownMode;
    }

    @XmlAttribute(value = "dropDownMode", category = "Behavior", defaultValue = "overlay", description = "Whether dropdown options are shown inline or in an overlay popup.")
    public ComboBox dropDownMode(DropDownMode dropDownMode) {
        DropDownMode normalized = dropDownMode == null ? DropDownMode.INLINE : dropDownMode;
        if (this.dropDownMode == normalized) return this;
        this.dropDownMode = normalized;
        syncDropDownAttachment();
        syncDropDownVisibility();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public OverlayLayer overlayLayer() {
        return explicitOverlayLayer != null ? explicitOverlayLayer : findTopmostOverlayLayer();
    }

    public ComboBox overlayLayer(OverlayLayer overlayLayer) {
        if (this.explicitOverlayLayer == overlayLayer) return this;
        detachFromOverlay();
        this.explicitOverlayLayer = overlayLayer;
        syncDropDownAttachment();
        syncDropDownVisibility();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public ComboBox useOverlay() {
        return dropDownMode(DropDownMode.OVERLAY);
    }

    public ComboBox useOverlay(OverlayLayer overlayLayer) {
        overlayLayer(overlayLayer);
        return dropDownMode(DropDownMode.OVERLAY);
    }

    public Popup dropDownPopup() {
        return dropDownPopup;
    }

    public float dropDownWidth() {
        return dropDownWidth;
    }

    @XmlAttribute(value = "dropDownWidth", category = "Layout", defaultValue = "0", description = "Explicit dropdown width; 0 uses automatic sizing.")
    public ComboBox dropDownWidth(float width) {
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

    @XmlAttribute(value = "dropDownMatchesWidgetWidth", category = "Layout", defaultValue = "false", description = "Whether dropdown width follows the combo box width.")
    public ComboBox dropDownMatchesWidgetWidth(boolean match) {
        if (dropDownMatchesWidgetWidth == match && (match || dropDownWidth <= 0.0f)) return this;
        dropDownMatchesWidgetWidth = match;
        if (match) {
            dropDownWidth = 0.0f;
        }
        syncDropDownSize();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public ComboBox dropDownSameWidth() {
        return dropDownMatchesWidgetWidth(true);
    }

    public ComboBox autoDropDownWidth() {
        boolean changed = dropDownWidth > 0.0f || dropDownMatchesWidgetWidth;
        dropDownWidth = 0.0f;
        dropDownMatchesWidgetWidth = false;
        if (changed) {
            syncDropDownSize();
            invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        }
        return this;
    }

    public float optionRowHeight() {
        return optionRowHeight;
    }

    @XmlAttribute(value = "optionRowHeight", category = "Layout", defaultValue = "20", description = "Height of each option row in UI pixels.")
    public ComboBox optionRowHeight(float height) {
        float normalized = Float.isFinite(height) && height > 0.0f ? height : OPTION_HEIGHT;
        if (optionRowHeight == normalized) return this;
        optionRowHeight = normalized;
        optionsScroll.scrollStep(optionRowHeight);
        for (ToggleButton option : optionButtons) {
            option.layout(style -> style.size(LayoutConstraints.AUTO, optionRowHeight).flexGrow(0).flexShrink(0.0f));
        }
        syncDropDownSize();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public int maxVisibleOptions() {
        return maxVisibleOptions;
    }

    @XmlAttribute(value = "maxVisibleOptions", category = "Layout", defaultValue = "6", description = "Maximum visible options before dropdown scrolling is used; 0 disables the cap.")
    public ComboBox maxVisibleOptions(int count) {
        int normalized = Math.max(0, count);
        if (maxVisibleOptions == normalized) return this;
        maxVisibleOptions = normalized;
        syncDropDownSize();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public OverlayLayer attachedOverlayLayer() {
        return attachedOverlayLayer;
    }

    public String placeholder() {
        return placeholder;
    }

    @XmlAttribute(value = "placeholder", category = "Content", defaultValue = "Select...", description = "Header text shown when no item is selected.")
    public ComboBox placeholder(String placeholder) {
        String normalized = normalize(placeholder);
        if (Objects.equals(this.placeholder, normalized)) return this;
        this.placeholder = normalized;
        this.richPlaceholder = RichText.plain(normalized);
        updateHeaderText();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public RichText richPlaceholder() {
        return richPlaceholder;
    }

    public ComboBox richPlaceholder(RichText placeholder) {
        RichText normalized = placeholder == null ? RichText.plain("") : placeholder;
        if (Objects.equals(this.richPlaceholder, normalized)) return this;
        this.richPlaceholder = normalized;
        this.placeholder = normalized.plainText();
        updateHeaderText();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public Button headerButton() {
        return headerButton;
    }

    public Box optionsHost() {
        return optionsHost;
    }

    public ScrollView optionsScroll() {
        return optionsScroll;
    }

    public ToggleButton optionButton(int index) {
        return optionButtons.get(index);
    }

    public EventSubscription onSelectionChanged(EventListener<? super SelectionChangedEvent> listener) {
        return on(SelectionChangedEvent.TYPE, listener);
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
            } else if (key.keyCode() == KeyCodes.UP) {
                selectRelative(-1);
                event.cancel();
            } else if (key.keyCode() == KeyCodes.DOWN) {
                selectRelative(1);
                event.cancel();
            }
        }
    }

    @Override
    public void arrange(RectView bounds) {
        super.arrange(bounds);
        syncDropDownSize();
    }

    private void selectRelative(int delta) {
        if (items.isEmpty()) return;
        int base = selectedIndex < 0 ? 0 : selectedIndex;
        setSelectedIndex(Math.max(0, Math.min(items.size() - 1, base + delta)), true);
    }

    private void setSelectedIndex(int index, boolean emitChange) {
        int normalized = items.isEmpty() ? -1 : Math.max(0, Math.min(index, items.size() - 1));
        if (selectedIndex == normalized) {
            syncSelectionState();
            return;
        }
        int oldSelection = selectedIndex;
        selectedIndex = normalized;
        syncSelectionState();
        updateHeaderText();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        if (emitChange) {
            emit(new SelectionChangedEvent(this, selectionList(oldSelection), selectionList(selectedIndex)));
        }
    }

    private void rebuildOptions() {
        optionsList.clearChildren();
        optionButtons.clear();
        for (int index = 0; index < items.size(); index++) {
            final int itemIndex = index;
            ToggleButton option = new ToggleButton(richItems.get(index));
            option.layout(style -> style.size(LayoutConstraints.AUTO, optionRowHeight).flexGrow(0).flexShrink(0.0f));
            option.onClick(event -> {
                selectedIndex(itemIndex);
                close();
            });
            optionButtons.add(option);
            optionsList.addChild(option);
        }
        optionsScroll.scrollTo(0.0f, 0.0f);
        syncDropDownSize();
    }

    private void syncSelectionState() {
        for (int i = 0; i < optionButtons.size(); i++) {
            optionButtons.get(i).silentChecked(i == selectedIndex);
        }
    }

    private void updateHeaderText() {
        RichText value = selectedItem().isEmpty() ? richPlaceholder : selectedRichItem();
        headerButton.richText(value.append(RichText.plain(" ?")));
    }

    private void addItemInternal(RichText item) {
        RichText normalized = item == null ? RichText.plain("") : item;
        richItems.add(normalized);
        items.add(normalized.plainText());
    }

    private void syncDropDownAttachment() {
        syncDropDownSize();
        if (dropDownMode == DropDownMode.INLINE) {
            detachFromOverlay();
            clearPopupContent();
            if (optionsHost.parent() != this) {
                super.addChild(optionsHost);
            }
            return;
        }

        detachOptionsFromInlineLayout();

        OverlayLayer targetOverlay = overlayLayer();
        if (targetOverlay != null) {
            if (dropDownPopup.content() != optionsHost) {
                dropDownPopup.content(optionsHost);
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
        if (dropDownMode == DropDownMode.OVERLAY) {
            optionsHost.visibility(attachedOverlayLayer != null ? Visibility.VISIBLE : Visibility.COLLAPSED);
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
            optionsHost.visibility(opened ? Visibility.VISIBLE : Visibility.COLLAPSED);
        }
    }

    private void detachOptionsFromInlineLayout() {
        if (optionsHost.parent() == this) {
            super.removeChild(optionsHost);
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
        float resolvedContentHeight = resolvedDropDownContentHeight();
        float width = resolvedWidth > 0.0f ? resolvedWidth : LayoutConstraints.AUTO;
        float height = resolvedContentHeight > 0.0f ? resolvedContentHeight + OPTIONS_INSET * 2.0f : LayoutConstraints.AUTO;
        float scrollWidth = resolvedWidth > OPTIONS_INSET * 2.0f ? resolvedWidth - OPTIONS_INSET * 2.0f : LayoutConstraints.AUTO;
        float scrollHeight = resolvedContentHeight > 0.0f ? resolvedContentHeight : LayoutConstraints.AUTO;
        Overflow verticalOverflow = maxVisibleOptions <= 0
                ? Overflow.AUTO
                : shouldScrollDropDown() ? Overflow.SCROLL : Overflow.HIDDEN;

        optionsHost.layout(style -> style.size(width, height).flexGrow(0).flexShrink(0.0f));
        optionsScroll.layout(style -> style
                .size(scrollWidth, scrollHeight)
                .margin(OPTIONS_INSET)
                .overflowX(Overflow.HIDDEN)
                .overflowY(verticalOverflow)
                .flexGrow(0)
                .flexShrink(0.0f));
    }

    private float resolvedDropDownContentHeight() {
        if (maxVisibleOptions <= 0 || items.isEmpty()) {
            return 0.0f;
        }
        int visibleOptions = Math.min(items.size(), maxVisibleOptions);
        return optionRowHeight * visibleOptions
                + optionsList.spacing() * Math.max(0, visibleOptions - 1);
    }

    private boolean shouldScrollDropDown() {
        return maxVisibleOptions > 0 && items.size() > maxVisibleOptions;
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

    private static List<Integer> selectionList(int index) {
        return index < 0 ? List.of() : List.of(index);
    }

    private static String normalize(String value) {
        return value == null ? "" : value;
    }

    public enum DropDownMode {
        INLINE,
        OVERLAY
    }
}
