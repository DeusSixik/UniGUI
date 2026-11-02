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
import dev.sixik.unigui.api.event.Event;
import dev.sixik.unigui.api.event.EventPhase;
import dev.sixik.unigui.api.event.EventListener;
import dev.sixik.unigui.api.event.EventSubscription;
import dev.sixik.unigui.api.event.KeyPressedEvent;
import dev.sixik.unigui.api.input.KeyCodes;
import dev.sixik.unigui.api.event.SelectionChangedEvent;
import dev.sixik.unigui.api.layout.LayoutConstraints;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.layout.Overflow;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.text.RichText;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.api.xml.XmlAttribute;
import dev.sixik.unigui.api.xml.XmlWidgetName;
import dev.sixik.unigui.impl.text.TextEngine;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import dev.sixik.unigui.widgets.containers.HBox;
import dev.sixik.unigui.widgets.containers.LinearBox;
import dev.sixik.unigui.widgets.containers.PanelWidget;
import dev.sixik.unigui.widgets.containers.ScrollView;
import dev.sixik.unigui.widgets.containers.StackPanel;
import dev.sixik.unigui.widgets.core.Orientation;
import dev.sixik.unigui.widgets.interaction.ToggleButton;

@AiDoc(summary = "Tabbed navigation container with a scrollable header bar and switched content area.")
@WikiPage(
        title = "TabControl",
        slug = "tab-control",
        category = "<key:navigation.category>",
        summary = "<key:tab.control.summary>"
)
@WikiSection(
        title = "<key:wiki.section.overview.title>",
        markdown = "<key:tab.control.section.overview.body>",
        order = 10
)
@WikiExample(
        title = "<key:tab.control.example.basic.title>",
        code = "TabControl tabs = new TabControl();\ntabs.addTab(\"Editor\", new PanelWidget());\ntabs.addTab(\"Console\", new PanelWidget());\ntabs.selectTab(0);"
)
@WikiLink(
        label = "PageView",
        target = "page:page-view",
        description = "<key:page.view.summary>"
)
@XmlWidgetName("TabControl")
public class TabControl extends LinearBox {
    public static final String STYLE_TYPE = dev.sixik.unigui.api.style.StyleIds.Widget.TAB_CONTROL;

    private static final float TAB_HEIGHT = 22.0f;
    private static final float TAB_HEADER_HEIGHT = 36.0f;
    private static final float TAB_TEXT_PADDING_X = 10.0f;
    private static final float TAB_TEXT_WIDTH_SAFETY = 10.0f;

    private final HBox tabHeader = new HBox();
    private final ScrollView tabHeaderScroll = new ScrollView(tabHeader);
    private final StackPanel contentHost = new StackPanel();
    private final List<Tab> tabs = new ObjectArrayList<>();
    private int selectedIndex = -1;

        @AiDoc(summary = "Constructs a new TabControl instance.")
    @WikiMethod(summary = "<key:tab.control.method.ctorDefault>")
    public TabControl() {
        super(Orientation.VERTICAL);
        spacing(4.0f);
        focusable(true);

        tabHeader.spacing(4.0f);
        tabHeader.layout(style -> style.size(LayoutConstraints.AUTO, TAB_HEIGHT).flexGrow(0).flexShrink(0.0f));
        tabHeaderScroll.layout(style -> style
                .size(LayoutConstraints.AUTO, TAB_HEADER_HEIGHT)
                .overflowX(Overflow.AUTO)
                .overflowY(Overflow.HIDDEN)
                .flexGrow(0)
                .flexShrink(0.0f));
        contentHost.layout(style -> style.size(LayoutConstraints.AUTO, LayoutConstraints.AUTO).flexGrow(1).flexShrink(1.0f));

        super.addChild(tabHeaderScroll);
        super.addChild(contentHost);
    }

        @AiDoc(summary = "Returns an unmodifiable list of tab descriptors.")
    @WikiMethod(summary = "<key:tab.control.method.tabs>")
    @WikiReturn("<key:tab.control.return.tabs>")
    public List<Tab> tabs() {
        return Collections.unmodifiableList(tabs);
    }

        @AiDoc(summary = "Returns the total number of tabs.")
    @WikiMethod(summary = "<key:tab.control.method.tabCount>")
    @WikiReturn("<key:tab.control.return.tabCount>")
    public int tabCount() {
        return tabs.size();
    }

        @AiDoc(summary = "Returns the 0-based index of the currently selected tab, or -1 if none.")
    @WikiMethod(summary = "<key:tab.control.method.selectedIndexGet>")
    @WikiReturn("<key:tab.control.return.selectedIndex>")
    public int selectedIndex() {
        return selectedIndex;
    }

        @AiDoc(summary = "Returns the currently selected Tab descriptor, or null if none.")
    @WikiMethod(summary = "<key:tab.control.method.selectedTab>")
    @WikiReturn("<key:tab.control.return.selectedTab>")
    public Tab selectedTab() {
        return selectedIndex >= 0 && selectedIndex < tabs.size() ? tabs.get(selectedIndex) : null;
    }

        @AiDoc(summary = "Returns the content widget of the selected tab, or null if none.")
    @WikiMethod(summary = "<key:tab.control.method.selectedContent>")
    @WikiReturn("<key:tab.control.return.selectedContent>")
    public Widget selectedContent() {
        Tab tab = selectedTab();
        return tab == null ? null : tab.content();
    }

        @AiDoc(summary = "Returns the header container holding tab toggle buttons.")
    @WikiMethod(summary = "<key:tab.control.method.tabHeader>")
    @WikiReturn("<key:tab.control.return.tabHeader>")
    public HBox tabHeader() {
        return tabHeader;
    }

        @AiDoc(summary = "Returns the scroll view wrapping the tab header.")
    @WikiMethod(summary = "<key:tab.control.method.tabHeaderScroll>")
    @WikiReturn("<key:tab.control.return.tabHeaderScroll>")
    public ScrollView tabHeaderScroll() {
        return tabHeaderScroll;
    }

        @AiDoc(summary = "Returns the stack container holding tab page contents.")
    @WikiMethod(summary = "<key:tab.control.method.contentHost>")
    @WikiReturn("<key:tab.control.return.contentHost>")
    public StackPanel contentHost() {
        return contentHost;
    }

        @AiDoc(summary = "Returns the toggle button corresponding to the tab at the given index.")
    @WikiMethod(summary = "<key:tab.control.method.tabButton>")
    @WikiReturn("<key:tab.control.return.tabButton>")
    public ToggleButton tabButton(@WikiParameter(name = "index", description = "<key:tab.control.param.index>") int index) {
        return tabs.get(index).button();
    }

        @AiDoc(summary = "Appends a new tab with plain text title and content widget.")
    @WikiMethod(summary = "<key:tab.control.method.addTabString>")
    @WikiReturn("<key:tab.control.return.self>")
    public TabControl addTab(@WikiParameter(name = "title", description = "<key:tab.control.param.title>") String title, @WikiParameter(name = "content", description = "<key:tab.control.param.content>") Widget content) {
        return insertTab(tabs.size(), title, content);
    }

        @AiDoc(summary = "Appends a new tab with RichText title and content widget.")
    @WikiMethod(summary = "<key:tab.control.method.addTabRich>")
    @WikiReturn("<key:tab.control.return.self>")
    public TabControl addTab(@WikiParameter(name = "title", description = "<key:tab.control.param.title>") RichText title, @WikiParameter(name = "content", description = "<key:tab.control.param.content>") Widget content) {
        return insertTab(tabs.size(), title, content);
    }

        @AiDoc(summary = "Inserts a new tab with plain text title at the specified index.")
    @WikiMethod(summary = "<key:tab.control.method.insertTabString>")
    @WikiReturn("<key:tab.control.return.self>")
    public TabControl insertTab(@WikiParameter(name = "index", description = "<key:tab.control.param.index>") int index, @WikiParameter(name = "title", description = "<key:tab.control.param.title>") String title, @WikiParameter(name = "content", description = "<key:tab.control.param.content>") Widget content) {
        return insertTab(index, RichText.resolve(title), content);
    }

        @AiDoc(summary = "Inserts a new tab with RichText title at the specified index.")
    @WikiMethod(summary = "<key:tab.control.method.insertTabRich>")
    @WikiReturn("<key:tab.control.return.self>")
    public TabControl insertTab(@WikiParameter(name = "index", description = "<key:tab.control.param.index>") int index, @WikiParameter(name = "title", description = "<key:tab.control.param.title>") RichText title, @WikiParameter(name = "content", description = "<key:tab.control.param.content>") Widget content) {
        Widget normalizedContent = content == null ? new PanelWidget() : content;
        int insertIndex = Math.max(0, Math.min(index, tabs.size()));
        RichText normalizedTitle = title == null ? RichText.plain("") : title;
        ToggleButton button = new ToggleButton(normalizedTitle);
        button.textPaddingX(TAB_TEXT_PADDING_X);
        button.layout(style -> style
                .size(LayoutConstraints.AUTO, TAB_HEIGHT)
                .minWidth(TextEngine.measureLineWidth(normalizedTitle) + TAB_TEXT_PADDING_X * 2.0f + TAB_TEXT_WIDTH_SAFETY)
                .flexGrow(0)
                .flexShrink(0.0f));

        Tab tab = new Tab(normalizedTitle, normalizedContent, button);
        tabs.add(insertIndex, tab);
        tabHeader.insertChild(insertIndex, button);
        contentHost.insertChild(insertIndex, tab.slot());

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

        @AiDoc(summary = "Removes the tab at the specified index.")
    @WikiMethod(summary = "<key:tab.control.method.removeTab>")
    @WikiReturn("<key:tab.control.return.self>")
    public TabControl removeTab(@WikiParameter(name = "index", description = "<key:tab.control.param.index>") int index) {
        if (index < 0 || index >= tabs.size()) return this;
        int oldSelection = selectedIndex;
        Tab removed = tabs.remove(index);
        tabHeader.removeChild(removed.button());
        contentHost.removeChild(removed.slot());

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

        @AiDoc(summary = "Removes all tabs and their content.")
    @WikiMethod(summary = "<key:tab.control.method.clearTabs>")
    @WikiReturn("<key:tab.control.return.self>")
    public TabControl clearTabs() {
        if (tabs.isEmpty()) return this;
        tabs.clear();
        tabHeader.clearChildren();
        contentHost.clearChildren();
        setSelectedIndex(-1, selectedIndex != -1);
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

        @AiDoc(summary = "Selects the tab at the specified index, firing a selection change event.")
    @WikiMethod(summary = "<key:tab.control.method.selectTab>")
    @WikiReturn("<key:tab.control.return.self>")
    public TabControl selectTab(@WikiParameter(name = "index", description = "<key:tab.control.param.index>") int index) {
        setSelectedIndex(index, true);
        return this;
    }

        @AiDoc(summary = "Shifts tab selection by the relative delta offset.")
    @WikiMethod(summary = "<key:tab.control.method.selectRelative>")
    @WikiReturn("<key:tab.control.return.self>")
    public TabControl selectRelative(@WikiParameter(name = "delta", description = "<key:tab.control.param.delta>") int delta) {
        if (tabs.isEmpty()) return this;
        int base = selectedIndex < 0 ? 0 : selectedIndex;
        int next = Math.max(0, Math.min(tabs.size() - 1, base + delta));
        setSelectedIndex(next, true);
        return this;
    }

    @AiDoc(summary = "Silently selects the tab at the given index without emitting events.")
    @WikiMethod(summary = "<key:tab.control.method.silentSelectTab>")
    @WikiReturn("<key:tab.control.return.self>")
    @XmlAttribute(value = "selectedIndex", category = "Behavior", defaultValue = "-1", description = "Initial selected tab index without emitting change events during XML load.")
    public TabControl silentSelectTab(@WikiParameter(name = "index", description = "<key:tab.control.param.index>") int index) {
        setSelectedIndex(index, false);
        return this;
    }

        @AiDoc(summary = "Registers a listener invoked when the selected tab index changes.")
    @WikiMethod(summary = "<key:tab.control.method.onSelectionChanged>")
    @WikiReturn("<key:tab.control.return.subscription>")
    public EventSubscription onSelectionChanged(@WikiParameter(name = "listener", description = "<key:tab.control.param.listener>") EventListener<? super SelectionChangedEvent> listener) {
        return on(SelectionChangedEvent.TYPE, listener);
    }

    @Override
    public void handle(Event event) {
        super.handle(event);
        if (event.isCancelled()) return;
        if (event instanceof KeyPressedEvent key
                && key.phase() == EventPhase.TARGET
                && uiContext() != null
                && uiContext().focusManager().isFocused(this)) {
            if (key.keyCode() == KeyCodes.LEFT) {
                selectRelative(-1);
                event.cancel();
            } else if (key.keyCode() == KeyCodes.RIGHT) {
                selectRelative(1);
                event.cancel();
            }
        }
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
        Tab selectedTab = null;
        for (int i = 0; i < tabs.size(); i++) {
            Tab tab = tabs.get(i);
            boolean selected = i == selectedIndex;
            tab.button().silentChecked(selected);
            tab.slot().visibility(selected ? Visibility.VISIBLE : Visibility.COLLAPSED);
            if (selected) {
                selectedTab = tab;
            }
        }
        arrangeSelectedSlotIfReady(selectedTab);
    }

    private void arrangeSelectedSlotIfReady(Tab selectedTab) {
        if (selectedTab == null) return;
        RectView bounds = contentHost.layoutBounds();
        if (bounds.width() <= 0.0f || bounds.height() <= 0.0f) return;
        selectedTab.slot().measure(new LayoutContext(bounds.width(), bounds.height()));
        selectedTab.slot().arrange(bounds);
    }

    private static List<Integer> selectionList(int index) {
        return index < 0 ? List.of() : List.of(index);
    }

    private static String normalize(String title) {
        return title == null ? "" : title;
    }

    @AiDoc(summary = "Descriptor for an individual tab within a TabControl, managing its title, content, and button.")
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

        @AiDoc(summary = "Returns plain text title of the tab.")
        public String title() {
            return title;
        }

        @AiDoc(summary = "Returns RichText title of the tab.")
        public RichText richTitle() {
            return richTitle;
        }

        @AiDoc(summary = "Returns the content widget of the tab.")
        public Widget content() {
            return content;
        }

        private StackPanel slot() {
            return slot;
        }

        @AiDoc(summary = "Returns the header ToggleButton associated with this tab.")
        public ToggleButton button() {
            return button;
        }

        @AiDoc(summary = "Sets the plain text title of the tab.")
        public Tab title(String title) {
            this.title = normalize(title);
            this.richTitle = RichText.resolve(this.title);
            button.richText(this.richTitle);
            return this;
        }

        @AiDoc(summary = "Sets the RichText title of the tab.")
        public Tab richTitle(RichText title) {
            this.richTitle = title == null ? RichText.plain("") : title;
            this.title = this.richTitle.plainText();
            button.richText(this.richTitle);
            return this;
        }
    }
}
