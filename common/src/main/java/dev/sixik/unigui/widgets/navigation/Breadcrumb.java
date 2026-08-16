package dev.sixik.unigui.widgets.navigation;

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

@XmlWidgetName("Breadcrumb")
public class Breadcrumb extends PanelWidget {
    private static final float ITEM_HEIGHT = 20.0f;
    private static final float SEPARATOR_WIDTH = 10.0f;

    private final WrapPanel host = new WrapPanel();
    private final List<BreadcrumbItem> items = new ObjectArrayList<>();
    private final List<Button> itemButtons = new ObjectArrayList<>();
    private String separator = "\u203A";
    private int selectedIndex = -1;

    public Breadcrumb() {
        host.spacing(2.0f);
        host.lineSpacing(2.0f);
        super.addChild(host);
    }

    public WrapPanel host() {
        return host;
    }

    @XmlAttribute(value = "spacing", category = "Layout", defaultValue = "2", description = "Horizontal gap between breadcrumb items and separators.")
    public Breadcrumb spacing(float spacing) {
        host.spacing(spacing);
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    @XmlAttribute(value = "lineSpacing", category = "Layout", defaultValue = "2", description = "Vertical gap between wrapped breadcrumb lines.")
    public Breadcrumb lineSpacing(float lineSpacing) {
        host.lineSpacing(lineSpacing);
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public List<BreadcrumbItem> items() {
        return Collections.unmodifiableList(items);
    }

    public Breadcrumb items(List<String> items) {
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

    public Breadcrumb richItems(List<RichText> items) {
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

    public Breadcrumb breadcrumbItems(List<BreadcrumbItem> items) {
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

    public Breadcrumb addItem(String text) {
        return addItem(new BreadcrumbItem(text));
    }

    public Breadcrumb addItem(RichText text) {
        return addItem(new BreadcrumbItem(text));
    }

    public Breadcrumb addItem(BreadcrumbItem item) {
        if (item == null) return this;
        items.add(item);
        if (selectedIndex < 0) {
            selectedIndex = 0;
        }
        rebuild();
        return this;
    }

    public Breadcrumb removeItem(int index) {
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

    public Breadcrumb clearItems() {
        if (items.isEmpty()) return this;
        items.clear();
        selectedIndex = -1;
        rebuild();
        return this;
    }

    public int itemCount() {
        return items.size();
    }

    public String separator() {
        return separator;
    }

    @XmlAttribute(value = "separator", category = "Content", defaultValue = "\u203A", description = "Text shown between breadcrumb items.")
    public Breadcrumb separator(String separator) {
        String normalized = separator == null ? "" : separator;
        if (this.separator.equals(normalized)) return this;
        this.separator = normalized;
        rebuild();
        return this;
    }

    public int selectedIndex() {
        return selectedIndex;
    }

    public BreadcrumbItem selectedItem() {
        return selectedIndex >= 0 && selectedIndex < items.size() ? items.get(selectedIndex) : null;
    }

    public List<BreadcrumbItem> selectedPath() {
        return selectedIndex < 0 ? List.of() : List.copyOf(items.subList(0, selectedIndex + 1));
    }

    public Button itemButton(int index) {
        return itemButtons.get(index);
    }

    public Breadcrumb selectedIndex(int index) {
        setSelectedIndex(index, true);
        return this;
    }

    @XmlAttribute(value = "selectedIndex", category = "Behavior", defaultValue = "-1", description = "Initial selected breadcrumb item index without emitting change events during XML load.")
    public Breadcrumb silentSelectedIndex(int index) {
        setSelectedIndex(index, false);
        return this;
    }

    public EventSubscription onSelectionChanged(EventListener<? super SelectionChangedEvent> listener) {
        return on(SelectionChangedEvent.TYPE, listener);
    }

    private void rebuild() {
        host.clearChildren();
        itemButtons.clear();
        for (int index = 0; index < items.size(); index++) {
            final int itemIndex = index;
            BreadcrumbItem item = items.get(index);
            Button button = new Button(item.richText());
            button.themeEnabled(false);
            button.enabled(item.enabled());
            button.layout(style -> style.size(LayoutConstraints.AUTO, ITEM_HEIGHT).flexGrow(0).flexShrink(0.0f));
            button.backgroundVisible(true);
            button.borderVisible(false);
            button.radius(3.0f);
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
            MutableColor background = button.background();
            if (selected) {
                background.set(0.16f, 0.23f, 0.32f, 0.96f);
                button.textColor().set(0.88f, 0.94f, 1.0f, 1.0f);
                button.borderVisible(true);
                button.borderColor().set(0.32f, 0.70f, 1.0f, 0.70f);
            } else if (ancestor) {
                background.set(0.08f, 0.12f, 0.16f, 0.82f);
                button.textColor().set(0.70f, 0.86f, 1.0f, 1.0f);
                button.borderVisible(false);
            } else {
                background.set(0.07f, 0.08f, 0.10f, 0.68f);
                button.textColor().set(0.86f, 0.88f, 0.92f, button.enabled() ? 1.0f : 0.50f);
                button.borderVisible(false);
            }
        }
    }

    private static List<Integer> indexList(int index) {
        return index < 0 ? List.of() : List.of(index);
    }
}
