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
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.layout.LayoutSize;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.api.xml.XmlAttribute;
import dev.sixik.unigui.api.xml.XmlWidgetName;
import dev.sixik.unigui.impl.layout.v3.LayoutV3StackAdapter;
import dev.sixik.unigui.impl.widget.WidgetBase;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Collections;
import java.util.List;
import dev.sixik.unigui.widgets.containers.PanelWidget;

/**
 * Retained page container that keeps exactly one page visible at a time.
 *
 * <p>Unlike {@link Carousel}, PageView owns only page content and selection state:
 * it does not add previous/next buttons, indicator text, cyclic transition chrome or
 * timer behavior.</p>
 */
@AiDoc(summary = "Retained page container that keeps exactly one page visible at a time.")
@WikiPage(
        title = "PageView",
        slug = "page-view",
        category = "<key:navigation.category>",
        summary = "<key:page.view.summary>"
)
@WikiSection(
        title = "<key:wiki.section.overview.title>",
        markdown = "<key:page.view.section.overview.body>",
        order = 10
)
@WikiExample(
        title = "<key:page.view.example.basic.title>",
        code = "PageView pageView = new PageView();\npageView.addPage(new Label(\"Page 1\"));\npageView.addPage(new Label(\"Page 2\"));\npageView.selectedIndex(0);"
)
@WikiLink(
        label = "TabControl",
        target = "page:tab-control",
        description = "<key:tab.control.summary>"
)
@XmlWidgetName("PageView")
public class PageView extends PanelWidget {
    public static final String STYLE_TYPE = dev.sixik.unigui.api.style.StyleIds.Widget.PAGE_VIEW;

    private final List<Widget> pages = new ObjectArrayList<>();
    private int selectedIndex;
    private boolean hasArrangedBounds;

        @AiDoc(summary = "Appends a page widget to the end of the page list.")
    @WikiMethod(summary = "<key:page.view.method.addPage>")
    @WikiReturn("<key:page.view.return.self>")
    public PageView addPage(@WikiParameter(name = "page", description = "<key:page.view.param.page>") Widget page) {
        return insertPage(pages.size(), page);
    }

        @AiDoc(summary = "Inserts a page widget at the specified index.")
    @WikiMethod(summary = "<key:page.view.method.insertPage>")
    @WikiReturn("<key:page.view.return.self>")
    public PageView insertPage(@WikiParameter(name = "index", description = "<key:page.view.param.index>") int index, @WikiParameter(name = "page", description = "<key:page.view.param.page>") Widget page) {
        if (page == null) return this;
        int insertIndex = Math.max(0, Math.min(index, pages.size()));
        pages.add(insertIndex, page);
        super.insertChild(insertIndex, page);
        if (pages.size() == 1) {
            selectedIndex = 0;
        } else if (insertIndex <= selectedIndex) {
            selectedIndex++;
        }
        syncPages();
        arrangePagesIfReady();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

        @AiDoc(summary = "Removes the page at the specified index.")
    @WikiMethod(summary = "<key:page.view.method.removePageIndex>")
    @WikiReturn("<key:page.view.return.self>")
    public PageView removePage(@WikiParameter(name = "index", description = "<key:page.view.param.index>") int index) {
        if (index < 0 || index >= pages.size()) return this;
        int oldSelection = selectedIndex;
        Widget removed = pages.remove(index);
        super.removeChild(removed);

        if (pages.isEmpty()) {
            selectedIndex = 0;
            emitSelectionChanged(oldSelection, oldSelection != 0);
        } else if (oldSelection == index) {
            setSelectedIndex(Math.min(index, pages.size() - 1), true, oldSelection);
        } else if (index < oldSelection) {
            selectedIndex = oldSelection - 1;
            syncPages();
        } else {
            syncPages();
        }
        arrangePagesIfReady();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

        @AiDoc(summary = "Removes the specified page widget.")
    @WikiMethod(summary = "<key:page.view.method.removePageWidget>")
    @WikiReturn("<key:page.view.return.self>")
    public PageView removePage(@WikiParameter(name = "page", description = "<key:page.view.param.page>") Widget page) {
        return removePage(pages.indexOf(page));
    }

        @AiDoc(summary = "Removes all pages from the container.")
    @WikiMethod(summary = "<key:page.view.method.clearPages>")
    @WikiReturn("<key:page.view.return.self>")
    public PageView clearPages() {
        if (pages.isEmpty()) return this;
        int oldSelection = selectedIndex;
        pages.clear();
        selectedIndex = 0;
        super.clearChildren();
        emitSelectionChanged(oldSelection, oldSelection != 0);
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

        @AiDoc(summary = "Returns an unmodifiable list of all contained page widgets.")
    @WikiMethod(summary = "<key:page.view.method.pages>")
    @WikiReturn("<key:page.view.return.pages>")
    public List<Widget> pages() {
        return Collections.unmodifiableList(pages);
    }

        @AiDoc(summary = "Returns the total number of pages.")
    @WikiMethod(summary = "<key:page.view.method.pageCount>")
    @WikiReturn("<key:page.view.return.pageCount>")
    public int pageCount() {
        return pages.size();
    }

        @AiDoc(summary = "Returns the currently selected active page widget, or null if none.")
    @WikiMethod(summary = "<key:page.view.method.selectedPage>")
    @WikiReturn("<key:page.view.return.selectedPage>")
    public Widget selectedPage() {
        return pages.isEmpty() ? null : pages.get(selectedIndex);
    }

        @AiDoc(summary = "Returns the 0-based index of the currently selected page.")
    @WikiMethod(summary = "<key:page.view.method.selectedIndexGet>")
    @WikiReturn("<key:page.view.return.selectedIndex>")
    public int selectedIndex() {
        return selectedIndex;
    }

        @AiDoc(summary = "Selects the active page by index, firing a selection change event.")
    @WikiMethod(summary = "<key:page.view.method.selectedIndexSet>")
    @WikiReturn("<key:page.view.return.self>")
    public PageView selectedIndex(@WikiParameter(name = "selectedIndex", description = "<key:page.view.param.selectedIndex>") int selectedIndex) {
        setSelectedIndex(selectedIndex, true);
        return this;
    }

    @AiDoc(summary = "Silently selects a page index without emitting selection change events.")
    @WikiMethod(summary = "<key:page.view.method.silentSelectedIndex>")
    @WikiReturn("<key:page.view.return.self>")
    @XmlAttribute(value = "selectedIndex", category = "Behavior", defaultValue = "0", description = "Initial selected page index without emitting change events during XML load.")
    public PageView silentSelectedIndex(@WikiParameter(name = "selectedIndex", description = "<key:page.view.param.selectedIndex>") int selectedIndex) {
        setSelectedIndex(selectedIndex, false);
        return this;
    }

        @AiDoc(summary = "Shifts current page selection by the specified relative delta offset.")
    @WikiMethod(summary = "<key:page.view.method.selectRelative>")
    @WikiReturn("<key:page.view.return.self>")
    public PageView selectRelative(@WikiParameter(name = "delta", description = "<key:page.view.param.delta>") int delta) {
        if (pages.isEmpty()) return this;
        int next = Math.max(0, Math.min(pages.size() - 1, selectedIndex + delta));
        return selectedIndex(next);
    }

        @AiDoc(summary = "Registers a listener invoked when the selected page index changes.")
    @WikiMethod(summary = "<key:page.view.method.onSelectionChanged>")
    @WikiReturn("<key:page.view.return.subscription>")
    public EventSubscription onSelectionChanged(@WikiParameter(name = "listener", description = "<key:page.view.param.listener>") EventListener<? super SelectionChangedEvent> listener) {
        return on(SelectionChangedEvent.TYPE, listener);
    }

    @Override
    public void addChild(Widget child) {
        addPage(child);
    }

    @Override
    public void insertChild(int index, Widget child) {
        insertPage(index, child);
    }

    @Override
    public void removeChild(Widget child) {
        removePage(child);
    }

    @Override
    public void clearChildren() {
        clearPages();
    }

    @Override
    public void measure(LayoutContext context) {
        if (visibility() == Visibility.COLLAPSED) {
            setDesiredSize(LayoutSize.ZERO);
            return;
        }
        applyQueuedMutations();
        LayoutSize measured = LayoutV3StackAdapter.measure(children(), context, layoutStyle());
        setDesiredSize(resolveDesiredSize(context, measured.width(), measured.height()));
    }

    @Override
    public void arrange(RectView bounds) {
        mutableLayoutBounds().set(bounds);
        hasArrangedBounds = bounds != null && bounds.width() > 0.0f && bounds.height() > 0.0f;
        if (visibility() == Visibility.COLLAPSED) return;
        applyQueuedMutations();
        LayoutV3StackAdapter.arrange(children(), bounds, layoutStyle());
    }

    private void setSelectedIndex(int selectedIndex, boolean emitChange) {
        int normalized = pages.isEmpty() ? 0 : Math.max(0, Math.min(selectedIndex, pages.size() - 1));
        if (this.selectedIndex == normalized) {
            syncPages();
            arrangePagesIfReady();
            return;
        }
        setSelectedIndex(normalized, emitChange, this.selectedIndex);
    }

    private void setSelectedIndex(int selectedIndex, boolean emitChange, int oldSelection) {
        this.selectedIndex = selectedIndex;
        syncPages();
        arrangePagesIfReady();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        emitSelectionChanged(oldSelection, emitChange);
    }

    private void syncPages() {
        for (int i = 0; i < pages.size(); i++) {
            Widget page = pages.get(i);
            if (page instanceof WidgetBase base) {
                base.visibility(i == selectedIndex ? Visibility.VISIBLE : Visibility.COLLAPSED);
            }
        }
    }

    private void arrangePagesIfReady() {
        if (!hasArrangedBounds || visibility() == Visibility.COLLAPSED) return;
        applyQueuedMutations();
        LayoutV3StackAdapter.arrange(children(), layoutBounds(), layoutStyle());
    }

    private void emitSelectionChanged(int oldSelection, boolean emitChange) {
        if (emitChange) {
            emit(new SelectionChangedEvent(this, selectionList(oldSelection, pages.size()), selectionList(selectedIndex, pages.size())));
        }
    }

    private static List<Integer> selectionList(int index, int size) {
        return index < 0 || index >= size ? List.of() : List.of(index);
    }
}
