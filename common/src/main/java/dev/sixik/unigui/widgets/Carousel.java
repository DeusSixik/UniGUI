package dev.sixik.unigui.widgets;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.layout.LayoutConstraints;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.widget.Widget;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Carousel extends LinearBox {
    private final HBox header = new HBox();
    private final Button previous = new Button("<");
    private final Label indicator = new Label("0 / 0");
    private final Button next = new Button(">");
    private final StackPanel pageHost = new StackPanel();
    private final List<Widget> pages = new ArrayList<>();
    private int selectedIndex;

    public Carousel() {
        super(Orientation.VERTICAL);
        spacing(5.0f);
        header.spacing(4.0f);
        previous.layout(style -> style.size(24.0f, 22.0f).flexGrow(0).flexShrink(0.0f));
        next.layout(style -> style.size(24.0f, 22.0f).flexGrow(0).flexShrink(0.0f));
        indicator.layout(style -> style.size(LayoutConstraints.AUTO, 20.0f).flexGrow(1).flexShrink(1.0f));
        previous.onClick(event -> selectRelative(-1));
        next.onClick(event -> selectRelative(1));
        header.addChild(previous);
        header.addChild(indicator);
        header.addChild(next);
        pageHost.layout(style -> style.flexGrow(1).flexShrink(1.0f));
        addChild(header);
        addChild(pageHost);
        syncPages();
    }

    public Carousel addPage(Widget page) {
        if (page == null) return this;
        pages.add(page);
        pageHost.addChild(page);
        syncPages();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public List<Widget> pages() {
        return Collections.unmodifiableList(pages);
    }

    public int selectedIndex() {
        return selectedIndex;
    }

    public Carousel selectedIndex(int selectedIndex) {
        this.selectedIndex = pages.isEmpty() ? 0 : Math.max(0, Math.min(selectedIndex, pages.size() - 1));
        syncPages();
        return this;
    }

    public Carousel selectRelative(int delta) {
        if (pages.isEmpty()) return this;
        selectedIndex((selectedIndex + delta + pages.size()) % pages.size());
        return this;
    }

    private void syncPages() {
        for (int i = 0; i < pages.size(); i++) {
            if (pages.get(i) instanceof dev.sixik.unigui.impl.widget.WidgetBase base) {
                base.visibility(i == selectedIndex ? Visibility.VISIBLE : Visibility.COLLAPSED);
            }
        }
        indicator.text(pages.isEmpty() ? "0 / 0" : (selectedIndex + 1) + " / " + pages.size());
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
    }
}