package dev.sixik.unigui.widgets;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.event.EventListener;
import dev.sixik.unigui.api.event.EventSubscription;
import dev.sixik.unigui.api.event.ExpandedChangedEvent;
import dev.sixik.unigui.api.layout.LayoutConstraints;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.widget.Widget;

import java.util.Objects;

public class ExpandablePanel extends LinearBox {
    private static final float HEADER_HEIGHT = 22.0f;

    private final ToggleButton headerButton = new ToggleButton();
    private final VBox contentHost = new VBox();
    private String title = "";
    private boolean expanded = true;

    public ExpandablePanel() {
        this("");
    }

    public ExpandablePanel(String title) {
        super(Orientation.VERTICAL);
        spacing(2.0f);
        this.title = normalize(title);

        headerButton.preferredSize(LayoutConstraints.AUTO, HEADER_HEIGHT).grow(0.0f);
        headerButton.silentChecked(expanded);
        headerButton.onCheckedChanged(event -> expanded(event.newValue()));

        contentHost.spacing(4.0f);
        contentHost.margin(6.0f, 4.0f);
        contentHost.grow(0.0f);

        super.addChild(headerButton);
        super.addChild(contentHost);
        updateHeaderText();
    }

    public String title() {
        return title;
    }

    public ExpandablePanel title(String title) {
        String normalized = normalize(title);
        if (Objects.equals(this.title, normalized)) return this;
        this.title = normalized;
        updateHeaderText();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public boolean expanded() {
        return expanded;
    }

    public ExpandablePanel expanded(boolean expanded) {
        setExpanded(expanded, true);
        return this;
    }

    public ExpandablePanel silentExpanded(boolean expanded) {
        setExpanded(expanded, false);
        return this;
    }

    public ExpandablePanel toggleExpanded() {
        return expanded(!expanded);
    }

    public ToggleButton headerButton() {
        return headerButton;
    }

    public VBox contentHost() {
        return contentHost;
    }

    public ExpandablePanel addContent(Widget child) {
        if (child == null) return this;
        contentHost.addChild(child);
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public ExpandablePanel removeContent(Widget child) {
        if (child == null) return this;
        contentHost.removeChild(child);
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public ExpandablePanel clearContent() {
        contentHost.clearChildren();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public EventSubscription onExpandedChanged(EventListener<? super ExpandedChangedEvent> listener) {
        return on(ExpandedChangedEvent.TYPE, listener);
    }

    @Override
    public void addChild(Widget child) {
        addContent(child);
    }

    @Override
    public void removeChild(Widget child) {
        removeContent(child);
    }

    @Override
    public void clearChildren() {
        clearContent();
    }

    private void setExpanded(boolean expanded, boolean emitChange) {
        if (this.expanded == expanded) {
            headerButton.silentChecked(expanded);
            contentHost.visibility(expanded ? Visibility.VISIBLE : Visibility.COLLAPSED);
            updateHeaderText();
            return;
        }

        boolean oldValue = this.expanded;
        this.expanded = expanded;
        headerButton.silentChecked(expanded);
        contentHost.visibility(expanded ? Visibility.VISIBLE : Visibility.COLLAPSED);
        updateHeaderText();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);

        if (emitChange) {
            emit(new ExpandedChangedEvent(this, oldValue, expanded));
        }
    }

    private void updateHeaderText() {
        headerButton.text((expanded ? "\u25BE " : "\u25B8 ") + title);
    }

    private static String normalize(String title) {
        return title == null ? "" : title;
    }
}
