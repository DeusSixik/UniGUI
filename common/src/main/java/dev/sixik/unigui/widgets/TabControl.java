package dev.sixik.unigui.widgets;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.event.EventListener;
import dev.sixik.unigui.api.event.EventSubscription;
import dev.sixik.unigui.api.event.SelectionChangedEvent;
import dev.sixik.unigui.api.layout.LayoutConstraints;
import dev.sixik.unigui.api.text.RichText;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.widget.Widget;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class TabControl extends LinearBox {
    private static final float TAB_HEIGHT = 22.0f;

    private final HBox tabHeader = new HBox();
    private final StackPanel contentHost = new StackPanel();
    private final List<Tab> tabs = new ArrayList<>();
    private int selectedIndex = -1;

    public TabControl() {
        super(Orientation.VERTICAL);
        spacing(4.0f);

        tabHeader.spacing(4.0f);
        tabHeader.layout(style -> style.size(LayoutConstraints.AUTO, TAB_HEIGHT).flexGrow(0).flexShrink(0.0f));
        contentHost.layout(style -> style.size(LayoutConstraints.AUTO, LayoutConstraints.AUTO).flexGrow(1).flexShrink(1.0f));

        super.addChild(tabHeader);
        super.addChild(contentHost);
    }

    public List<Tab> tabs() {
        return Collections.unmodifiableList(tabs);
    }

    public int tabCount() {
        return tabs.size();
    }

    public int selectedIndex() {
        return selectedIndex;
    }

    public Tab selectedTab() {
        return selectedIndex >= 0 && selectedIndex < tabs.size() ? tabs.get(selectedIndex) : null;
    }

    public Widget selectedContent() {
        Tab tab = selectedTab();
        return tab == null ? null : tab.content();
    }

    public HBox tabHeader() {
        return tabHeader;
    }

    public StackPanel contentHost() {
        return contentHost;
    }

    public ToggleButton tabButton(int index) {
        return tabs.get(index).button();
    }

    public TabControl addTab(String title, Widget content) {
        return insertTab(tabs.size(), title, content);
    }

    public TabControl addTab(RichText title, Widget content) {
        return insertTab(tabs.size(), title, content);
    }

    public TabControl insertTab(int index, String title, Widget content) {
        return insertTab(index, RichText.plain(title), content);
    }

    public TabControl insertTab(int index, RichText title, Widget content) {
        Widget normalizedContent = content == null ? new PanelWidget() : content;
        int insertIndex = Math.max(0, Math.min(index, tabs.size()));
        RichText normalizedTitle = title == null ? RichText.plain("") : title;
        ToggleButton button = new ToggleButton(normalizedTitle);
        button.layout(style -> style.size(LayoutConstraints.AUTO, TAB_HEIGHT).flexGrow(0).flexShrink(0.0f));

        Tab tab = new Tab(normalizedTitle, normalizedContent, button);
        tabs.add(insertIndex, tab);
        rebuildChildren();

        button.onClick(event -> selectTab(tabs.indexOf(tab)));

        if (selectedIndex < 0) {
            setSelectedIndex(insertIndex, false);
        } else if (insertIndex <= selectedIndex) {
            selectedIndex++;
        }
        syncSelectionState();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public TabControl removeTab(int index) {
        if (index < 0 || index >= tabs.size()) return this;
        int oldSelection = selectedIndex;
        tabs.remove(index);
        rebuildChildren();

        if (tabs.isEmpty()) {
            setSelectedIndex(-1, oldSelection != -1);
        } else if (oldSelection == index) {
            setSelectedIndex(Math.min(index, tabs.size() - 1), true);
        } else if (index < oldSelection) {
            selectedIndex = oldSelection - 1;
            syncSelectionState();
        } else {
            syncSelectionState();
        }
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public TabControl clearTabs() {
        if (tabs.isEmpty()) return this;
        tabs.clear();
        rebuildChildren();
        setSelectedIndex(-1, selectedIndex != -1);
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public TabControl selectTab(int index) {
        setSelectedIndex(index, true);
        return this;
    }

    public TabControl silentSelectTab(int index) {
        setSelectedIndex(index, false);
        return this;
    }

    public EventSubscription onSelectionChanged(EventListener<? super SelectionChangedEvent> listener) {
        return on(SelectionChangedEvent.TYPE, listener);
    }

    @Override
    public void addChild(Widget child) {
        addTab("Tab " + (tabs.size() + 1), child);
    }

    @Override
    public void removeChild(Widget child) {
        if (child == null) return;
        for (int i = 0; i < tabs.size(); i++) {
            if (tabs.get(i).content() == child) {
                removeTab(i);
                return;
            }
        }
    }

    @Override
    public void clearChildren() {
        clearTabs();
    }

    private void setSelectedIndex(int index, boolean emitChange) {
        int normalized = tabs.isEmpty() ? -1 : Math.max(0, Math.min(index, tabs.size() - 1));
        if (selectedIndex == normalized) {
            syncSelectionState();
            return;
        }
        int oldSelection = selectedIndex;
        selectedIndex = normalized;
        syncSelectionState();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        if (emitChange) {
            emit(new SelectionChangedEvent(this, selectionList(oldSelection), selectionList(selectedIndex)));
        }
    }

    private void syncSelectionState() {
        for (int i = 0; i < tabs.size(); i++) {
            Tab tab = tabs.get(i);
            boolean selected = i == selectedIndex;
            tab.button().silentChecked(selected);
            tab.slot().visibility(selected ? Visibility.VISIBLE : Visibility.COLLAPSED);
        }
    }

    private void rebuildChildren() {
        tabHeader.clearChildren();
        contentHost.clearChildren();
        for (Tab tab : tabs) {
            tabHeader.addChild(tab.button());
            contentHost.addChild(tab.slot());
        }
    }

    private static List<Integer> selectionList(int index) {
        return index < 0 ? List.of() : List.of(index);
    }

    private static String normalize(String title) {
        return title == null ? "" : title;
    }

    public static final class Tab {
        private String title;
        private RichText richTitle;
        private final Widget content;
        private final StackPanel slot = new StackPanel();
        private final ToggleButton button;

        private Tab(RichText title, Widget content, ToggleButton button) {
            this.richTitle = title == null ? RichText.plain("") : title;
            this.title = this.richTitle.plainText();
            this.content = Objects.requireNonNull(content, "content");
            this.button = Objects.requireNonNull(button, "button");
            this.slot.addChild(content);
        }

        public String title() {
            return title;
        }

        public RichText richTitle() {
            return richTitle;
        }

        public Widget content() {
            return content;
        }

        private StackPanel slot() {
            return slot;
        }

        public ToggleButton button() {
            return button;
        }

        public Tab title(String title) {
            this.title = normalize(title);
            this.richTitle = RichText.plain(this.title);
            button.richText(this.richTitle);
            return this;
        }

        public Tab richTitle(RichText title) {
            this.richTitle = title == null ? RichText.plain("") : title;
            this.title = this.richTitle.plainText();
            button.richText(this.richTitle);
            return this;
        }
    }
}