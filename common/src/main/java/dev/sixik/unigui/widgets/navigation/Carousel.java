package dev.sixik.unigui.widgets.navigation;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.event.EventListener;
import dev.sixik.unigui.api.event.EventSubscription;
import dev.sixik.unigui.api.event.SelectionChangedEvent;
import dev.sixik.unigui.api.layout.LayoutConstraints;
import dev.sixik.unigui.api.widget.Widget;

import java.util.List;
import dev.sixik.unigui.widgets.containers.HBox;
import dev.sixik.unigui.widgets.containers.LinearBox;
import dev.sixik.unigui.widgets.core.Orientation;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.widgets.interaction.Button;

public class Carousel extends LinearBox {
    private final HBox header = new HBox();
    private final Button previous = new Button("<");
    private final Label indicator = new Label("0 / 0");
    private final Button next = new Button(">");
    private final PageView pageView = new PageView();

    public Carousel() {
        super(Orientation.VERTICAL);
        spacing(5.0f);
        header.spacing(4.0f);
        previous.layout(style -> style.size(24.0f, 22.0f).flexGrow(0).flexShrink(0.0f));
        next.layout(style -> style.size(24.0f, 22.0f).flexGrow(0).flexShrink(0.0f));
        indicator.layout(style -> style.size(LayoutConstraints.AUTO, 20.0f).flexGrow(1).flexShrink(1.0f));
        previous.onClick(event -> selectRelative(-1));
        next.onClick(event -> selectRelative(1));
        pageView.onSelectionChanged(event -> {
            updateIndicator();
            emit(new SelectionChangedEvent(this, event.oldSelection(), event.newSelection()));
        });
        header.addChild(previous);
        header.addChild(indicator);
        header.addChild(next);
        pageView.layout(style -> style.flexGrow(1).flexShrink(1.0f));
        addChild(header);
        addChild(pageView);
        updateIndicator();
    }

    public Carousel addPage(Widget page) {
        pageView.addPage(page);
        updateIndicator();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public List<Widget> pages() {
        return pageView.pages();
    }

    public PageView pageView() {
        return pageView;
    }

    public int selectedIndex() {
        return pageView.selectedIndex();
    }

    public Carousel selectedIndex(int selectedIndex) {
        pageView.selectedIndex(selectedIndex);
        updateIndicator();
        return this;
    }

    public Carousel silentSelectedIndex(int selectedIndex) {
        pageView.silentSelectedIndex(selectedIndex);
        updateIndicator();
        return this;
    }

    public Carousel selectRelative(int delta) {
        if (pageView.pageCount() == 0) return this;
        pageView.selectedIndex((selectedIndex() + delta + pageView.pageCount()) % pageView.pageCount());
        updateIndicator();
        return this;
    }

    public HBox header() {
        return header;
    }

    public Button previousButton() {
        return previous;
    }

    public Button nextButton() {
        return next;
    }

    public Label indicator() {
        return indicator;
    }

    public EventSubscription onSelectionChanged(EventListener<? super SelectionChangedEvent> listener) {
        return on(SelectionChangedEvent.TYPE, listener);
    }

    private void updateIndicator() {
        indicator.text(pageView.pageCount() == 0 ? "0 / 0" : (selectedIndex() + 1) + " / " + pageView.pageCount());
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
    }
}
