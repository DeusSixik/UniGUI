package dev.sixik.unigui.widgets;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.event.EventListener;
import dev.sixik.unigui.api.event.EventSubscription;
import dev.sixik.unigui.api.event.SelectionChangedEvent;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.layout.LayoutSize;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.impl.layout.v3.LayoutV3StackAdapter;
import dev.sixik.unigui.impl.widget.WidgetBase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Retained page container that keeps exactly one page visible at a time.
 *
 * <p>Unlike {@link Carousel}, PageView owns only page content and selection state:
 * it does not add previous/next buttons, indicator text, cyclic transition chrome or
 * timer behavior.</p>
 */
public class PageView extends PanelWidget {
    private final List<Widget> pages = new ArrayList<>();
    private int selectedIndex;
    private boolean hasArrangedBounds;

    public PageView addPage(Widget page) {
        return insertPage(pages.size(), page);
    }

    public PageView insertPage(int index, Widget page) {
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

    public PageView removePage(int index) {
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

    public PageView removePage(Widget page) {
        return removePage(pages.indexOf(page));
    }

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

    public List<Widget> pages() {
        return Collections.unmodifiableList(pages);
    }

    public int pageCount() {
        return pages.size();
    }

    public Widget selectedPage() {
        return pages.isEmpty() ? null : pages.get(selectedIndex);
    }

    public int selectedIndex() {
        return selectedIndex;
    }

    public PageView selectedIndex(int selectedIndex) {
        setSelectedIndex(selectedIndex, true);
        return this;
    }

    public PageView silentSelectedIndex(int selectedIndex) {
        setSelectedIndex(selectedIndex, false);
        return this;
    }

    public PageView selectRelative(int delta) {
        if (pages.isEmpty()) return this;
        int next = Math.max(0, Math.min(pages.size() - 1, selectedIndex + delta));
        return selectedIndex(next);
    }

    public EventSubscription onSelectionChanged(EventListener<? super SelectionChangedEvent> listener) {
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
