package dev.sixik.unigui.widgets.navigation;


import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiLink;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.event.EventListener;
import dev.sixik.unigui.api.event.EventSubscription;
import dev.sixik.unigui.api.event.SelectionChangedEvent;
import dev.sixik.unigui.api.layout.Alignment;
import dev.sixik.unigui.api.layout.LayoutConstraints;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.text.RichText;
import dev.sixik.unigui.api.xml.XmlAttribute;
import dev.sixik.unigui.api.xml.XmlWidgetName;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Collections;
import java.util.List;
import dev.sixik.unigui.widgets.containers.PanelWidget;
import dev.sixik.unigui.widgets.containers.WrapPanel;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.widgets.interaction.Button;
import dev.sixik.unigui.widgets.WidgetStyles;

@AiDoc(summary = "Hierarchical navigation breadcrumb trail displaying sequenced items with separators and selection callbacks.")
@WikiPage(
        title = "Breadcrumb",
        slug = "breadcrumb",
        category = "<key:navigation.category>",
        summary = "<key:breadcrumb.summary>"
)
@WikiSection(
        title = "<key:wiki.section.overview.title>",
        markdown = "<key:breadcrumb.section.overview.body>",
        order = 10
)
@WikiExample(
        title = "<key:breadcrumb.example.basic.title>",
        code = "Breadcrumb breadcrumb = new Breadcrumb();\nbreadcrumb.addItem(\"Home\").addItem(\"Settings\");"
)
@WikiLink(
        label = "BreadcrumbItem",
        target = "page:breadcrumb-item",
        description = "<key:breadcrumb.item.summary>"
)
@XmlWidgetName("Breadcrumb")
public class Breadcrumb extends PanelWidget {
    public static final String STYLE_TYPE = dev.sixik.unigui.api.style.StyleIds.Widget.BREADCRUMB;

    private static final float ITEM_HEIGHT = 20.0f;
    private static final float SEPARATOR_WIDTH = 10.0f;

    private final WrapPanel host = new WrapPanel();
    private final List<BreadcrumbItem> items = new ObjectArrayList<>();
    private final List<Button> itemButtons = new ObjectArrayList<>();
    private String separator = "\u203A";
    private int selectedIndex = -1;

        @AiDoc(summary = "Constructs an empty Breadcrumb navigation bar.")
    @WikiMethod(summary = "<key:breadcrumb.method.ctorDefault>")
    public Breadcrumb() {
        host.spacing(2.0f);
        host.lineSpacing(2.0f);
        super.addChild(host);
    }

        @AiDoc(summary = "Returns the internal WrapPanel container hosting the breadcrumb buttons.")
    @WikiMethod(summary = "<key:breadcrumb.method.host>")
    @WikiReturn("<key:breadcrumb.return.host>")
    public WrapPanel host() {
        return host;
    }

    @XmlAttribute(value = "spacing", category = "Layout", defaultValue = "2", description = "Horizontal gap between breadcrumb items and separators.")
        @AiDoc(summary = "Sets the horizontal spacing between items and separators.")
    @WikiMethod(summary = "<key:breadcrumb.method.spacing>")
    @WikiReturn("<key:breadcrumb.return.this>")
    public Breadcrumb spacing(
            @WikiParameter(description = "<key:breadcrumb.param.spacing>")
            float spacing
    ) {
        host.spacing(spacing);
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    @XmlAttribute(value = "lineSpacing", category = "Layout", defaultValue = "2", description = "Vertical gap between wrapped breadcrumb lines.")
        @AiDoc(summary = "Sets the vertical line spacing when items wrap to multiple lines.")
    @WikiMethod(summary = "<key:breadcrumb.method.lineSpacing>")
    @WikiReturn("<key:breadcrumb.return.this>")
    public Breadcrumb lineSpacing(
            @WikiParameter(description = "<key:breadcrumb.param.lineSpacing>")
            float lineSpacing
    ) {
        host.lineSpacing(lineSpacing);
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

        @AiDoc(summary = "Returns an unmodifiable list of breadcrumb items.")
    @WikiMethod(summary = "<key:breadcrumb.method.itemsGet>")
    @WikiReturn("<key:breadcrumb.return.items>")
    public List<BreadcrumbItem> items() {
        return Collections.unmodifiableList(items);
    }

        @AiDoc(summary = "Replaces the trail with plain-text items.")
    @WikiMethod(summary = "<key:breadcrumb.method.itemsSet>")
    @WikiReturn("<key:breadcrumb.return.this>")
    public Breadcrumb items(
            @WikiParameter(description = "<key:breadcrumb.param.items>")
            List<String> items
    ) {
        this.items.clear();
        if (items != null) {
            for (String item : items) {
                this.items.add(new BreadcrumbItem(item));
            }
        }
        if (selectedIndex >= this.items.size()) {
            selectedIndex = this.items.isEmpty() ? -1 : this.items.size() - 1;
        }
        if (selectedIndex < 0 && !this.items.isEmpty()) {
            selectedIndex = this.items.size() - 1;
        }
        rebuild();
        return this;
    }

        @AiDoc(summary = "Replaces the trail with RichText formatted items.")
    @WikiMethod(summary = "<key:breadcrumb.method.richItems>")
    @WikiReturn("<key:breadcrumb.return.this>")
    public Breadcrumb richItems(
            @WikiParameter(description = "<key:breadcrumb.param.items>")
            List<RichText> items
    ) {
        this.items.clear();
        if (items != null) {
            for (RichText item : items) {
                this.items.add(new BreadcrumbItem(item));
            }
        }
        if (selectedIndex >= this.items.size()) {
            selectedIndex = this.items.isEmpty() ? -1 : this.items.size() - 1;
        }
        if (selectedIndex < 0 && !this.items.isEmpty()) {
            selectedIndex = this.items.size() - 1;
        }
        rebuild();
        return this;
    }

        @AiDoc(summary = "Replaces the trail with BreadcrumbItem instances.")
    @WikiMethod(summary = "<key:breadcrumb.method.breadcrumbItems>")
    @WikiReturn("<key:breadcrumb.return.this>")
    public Breadcrumb breadcrumbItems(
            @WikiParameter(description = "<key:breadcrumb.param.items>")
            List<BreadcrumbItem> items
    ) {
        this.items.clear();
        if (items != null) {
            this.items.addAll(items);
        }
        if (selectedIndex >= this.items.size()) {
            selectedIndex = this.items.isEmpty() ? -1 : this.items.size() - 1;
        }
        if (selectedIndex < 0 && !this.items.isEmpty()) {
            selectedIndex = this.items.size() - 1;
        }
        rebuild();
        return this;
    }

        @AiDoc(summary = "Appends a plain text item to the trail.")
    @WikiMethod(summary = "<key:breadcrumb.method.addItemText>")
    @WikiReturn("<key:breadcrumb.return.this>")
    public Breadcrumb addItem(
            @WikiParameter(description = "<key:breadcrumb.param.text>")
            String text
    ) {
        return addItem(new BreadcrumbItem(text));
    }

        @AiDoc(summary = "Appends a RichText formatted item to the trail.")
    @WikiMethod(summary = "<key:breadcrumb.method.addItemRich>")
    @WikiReturn("<key:breadcrumb.return.this>")
    public Breadcrumb addItem(
            @WikiParameter(description = "<key:breadcrumb.param.text>")
            RichText text
    ) {
        return addItem(new BreadcrumbItem(text));
    }

        @AiDoc(summary = "Appends a BreadcrumbItem instance to the trail.")
    @WikiMethod(summary = "<key:breadcrumb.method.addItem>")
    @WikiReturn("<key:breadcrumb.return.this>")
    public Breadcrumb addItem(
            @WikiParameter(description = "<key:breadcrumb.param.item>")
            BreadcrumbItem item
    ) {
        if (item == null) return this;
        items.add(item);
        if (selectedIndex < 0) {
            selectedIndex = 0;
        }
        rebuild();
        return this;
    }

        @AiDoc(summary = "Removes the breadcrumb item at the specified index.")
    @WikiMethod(summary = "<key:breadcrumb.method.removeItem>")
    @WikiReturn("<key:breadcrumb.return.this>")
    public Breadcrumb removeItem(
            @WikiParameter(description = "<key:breadcrumb.param.index>")
            int index
    ) {
        if (index < 0 || index >= items.size()) return this;
        int oldSelection = selectedIndex;
        items.remove(index);
        if (items.isEmpty()) {
            selectedIndex = -1;
        } else if (oldSelection >= items.size()) {
            selectedIndex = items.size() - 1;
        } else if (index < oldSelection) {
            selectedIndex = oldSelection - 1;
        }
        rebuild();
        return this;
    }

        @AiDoc(summary = "Clears all items from the breadcrumb trail.")
    @WikiMethod(summary = "<key:breadcrumb.method.clearItems>")
    @WikiReturn("<key:breadcrumb.return.this>")
    public Breadcrumb clearItems() {
        if (items.isEmpty()) return this;
        items.clear();
        selectedIndex = -1;
        rebuild();
        return this;
    }

        @AiDoc(summary = "Returns the total number of items in the breadcrumb trail.")
    @WikiMethod(summary = "<key:breadcrumb.method.itemCount>")
    @WikiReturn("<key:breadcrumb.return.count>")
    public int itemCount() {
        return items.size();
    }

        @AiDoc(summary = "Returns the separator string placed between items.")
    @WikiMethod(summary = "<key:breadcrumb.method.separatorGet>")
    @WikiReturn("<key:breadcrumb.return.separator>")
    public String separator() {
        return separator;
    }

    @XmlAttribute(value = "separator", category = "Content", defaultValue = "\u203A", description = "Text shown between breadcrumb items.")
        @AiDoc(summary = "Sets the separator string displayed between breadcrumb items.")
    @WikiMethod(summary = "<key:breadcrumb.method.separatorSet>")
    @WikiReturn("<key:breadcrumb.return.this>")
    public Breadcrumb separator(
            @WikiParameter(description = "<key:breadcrumb.param.separator>")
            String separator
    ) {
        String normalized = separator == null ? "" : separator;
        if (this.separator.equals(normalized)) return this;
        this.separator = normalized;
        rebuild();
        return this;
    }

        @AiDoc(summary = "Returns the zero-based index of the currently selected breadcrumb item.")
    @WikiMethod(summary = "<key:breadcrumb.method.selectedIndexGet>")
    @WikiReturn("<key:breadcrumb.return.index>")
    public int selectedIndex() {
        return selectedIndex;
    }

        @AiDoc(summary = "Returns the currently selected BreadcrumbItem, or null if none.")
    @WikiMethod(summary = "<key:breadcrumb.method.selectedItem>")
    @WikiReturn("<key:breadcrumb.return.item>")
    public BreadcrumbItem selectedItem() {
        return selectedIndex >= 0 && selectedIndex < items.size() ? items.get(selectedIndex) : null;
    }

        @AiDoc(summary = "Returns a sublist of items from index 0 up to the selected index inclusive.")
    @WikiMethod(summary = "<key:breadcrumb.method.selectedPath>")
    @WikiReturn("<key:breadcrumb.return.items>")
    public List<BreadcrumbItem> selectedPath() {
        return selectedIndex < 0 ? List.of() : List.copyOf(items.subList(0, selectedIndex + 1));
    }

        @AiDoc(summary = "Returns the Button widget representing the item at the specified index.")
    @WikiMethod(summary = "<key:breadcrumb.method.itemButton>")
    @WikiReturn("<key:breadcrumb.return.button>")
    public Button itemButton(
            @WikiParameter(description = "<key:breadcrumb.param.index>")
            int index
    ) {
        return itemButtons.get(index);
    }

        @AiDoc(summary = "Selects the item at the specified index and dispatches a selection changed event.")
    @WikiMethod(summary = "<key:breadcrumb.method.selectedIndexSet>")
    @WikiReturn("<key:breadcrumb.return.this>")
    public Breadcrumb selectedIndex(
            @WikiParameter(description = "<key:breadcrumb.param.index>")
            int index
    ) {
        setSelectedIndex(index, true);
        return this;
    }

    @XmlAttribute(value = "selectedIndex", category = "Behavior", defaultValue = "-1", description = "Initial selected breadcrumb item index without emitting change events during XML load.")
        @AiDoc(summary = "Selects the item at the specified index without firing event callbacks.")
    @WikiMethod(summary = "<key:breadcrumb.method.silentSelectedIndex>")
    @WikiReturn("<key:breadcrumb.return.this>")
    public Breadcrumb silentSelectedIndex(
            @WikiParameter(description = "<key:breadcrumb.param.index>")
            int index
    ) {
        setSelectedIndex(index, false);
        return this;
    }

        @AiDoc(summary = "Registers a listener invoked when the selected breadcrumb step changes.")
    @WikiMethod(summary = "<key:breadcrumb.method.onSelectionChanged>")
    @WikiReturn("<key:breadcrumb.return.subscription>")
    public EventSubscription onSelectionChanged(
            @WikiParameter(description = "<key:breadcrumb.param.listener>")
            EventListener<? super SelectionChangedEvent> listener
    ) {
        return on(SelectionChangedEvent.TYPE, listener);
    }

    private void rebuild() {
        host.clearChildren();
        itemButtons.clear();
        for (int index = 0; index < items.size(); index++) {
            final int itemIndex = index;
            BreadcrumbItem item = items.get(index);
            Button button = new Button(item.richText());
            button.enabled(item.enabled());
            button.style(style -> style.putAll(WidgetStyles.Navigation.BREADCRUMB_ITEM));
            button.layout(style -> style.size(LayoutConstraints.AUTO, ITEM_HEIGHT).flexGrow(0).flexShrink(0.0f));
            button.onClick(event -> selectedIndex(itemIndex));
            itemButtons.add(button);
            host.addChild(button);

            if (index < items.size() - 1 && !separator.isEmpty()) {
                Label label = new Label(separator);
                label.color().set(0.58f, 0.62f, 0.68f, 0.85f);
                label.layout(style -> style.size(SEPARATOR_WIDTH, ITEM_HEIGHT).align(Alignment.CENTER, Alignment.CENTER).flexGrow(0).flexShrink(0.0f));
                host.addChild(label);
            }
        }
        syncVisualState();
        arrangeHostIfReady();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
    }

    private void arrangeHostIfReady() {
        if (layoutBounds().width() <= 0.0f || layoutBounds().height() <= 0.0f) return;
        host.measure(new LayoutContext(layoutBounds().width(), layoutBounds().height()));
        host.arrange(layoutBounds());
    }

    private void setSelectedIndex(int index, boolean emitChange) {
        int normalized = items.isEmpty() ? -1 : Math.max(0, Math.min(index, items.size() - 1));
        if (normalized >= 0 && !items.get(normalized).enabled()) {
            return;
        }
        if (selectedIndex == normalized) {
            syncVisualState();
            return;
        }
        int oldSelection = selectedIndex;
        selectedIndex = normalized;
        syncVisualState();
        invalidate(InvalidationFlags.VISUAL);
        if (emitChange) {
            emit(new SelectionChangedEvent(this, indexList(oldSelection), indexList(selectedIndex)));
        }
    }

    private void syncVisualState() {
        for (int i = 0; i < itemButtons.size(); i++) {
            Button button = itemButtons.get(i);
            boolean selected = i == selectedIndex;
            boolean ancestor = selectedIndex >= 0 && i < selectedIndex;
            if (selected) {
                button.background(MutableColor.rgba(0.16f, 0.23f, 0.32f, 0.96f));
                button.textColor(MutableColor.rgba(0.88f, 0.94f, 1.0f, 1.0f));
                button.borderVisible(true);
                button.borderColor(MutableColor.rgba(0.32f, 0.70f, 1.0f, 0.70f));
            } else if (ancestor) {
                button.background(MutableColor.rgba(0.08f, 0.12f, 0.16f, 0.82f));
                button.textColor(MutableColor.rgba(0.70f, 0.86f, 1.0f, 1.0f));
                button.borderVisible(false);
            } else {
                button.background(MutableColor.rgba(0.07f, 0.08f, 0.10f, 0.68f));
                button.textColor(MutableColor.rgba(0.86f, 0.88f, 0.92f, button.enabled() ? 1.0f : 0.50f));
                button.borderVisible(false);
            }
        }
    }

    private static List<Integer> indexList(int index) {
        return index < 0 ? List.of() : List.of(index);
    }
}
