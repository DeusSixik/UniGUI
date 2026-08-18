package dev.sixik.unigui.widgets.navigation;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.event.EventListener;
import dev.sixik.unigui.api.event.EventSubscription;
import dev.sixik.unigui.api.event.ExpandedChangedEvent;
import dev.sixik.unigui.api.layout.LayoutConstraints;
import dev.sixik.unigui.api.text.RichText;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.api.xml.XmlAttribute;
import dev.sixik.unigui.api.xml.XmlWidgetName;

import java.util.Objects;
import dev.sixik.unigui.widgets.containers.LinearBox;
import dev.sixik.unigui.widgets.containers.VBox;
import dev.sixik.unigui.widgets.core.Orientation;
import dev.sixik.unigui.widgets.interaction.ToggleButton;

@XmlWidgetName("ExpandablePanel")
public class ExpandablePanel extends LinearBox {
    private static final float HEADER_HEIGHT = 22.0f;

    private final ToggleButton headerButton = new ToggleButton();
    private final VBox contentHost = new VBox();
    private String title = "";
    private RichText richTitle = RichText.plain("");
    private boolean expanded = true;

    public ExpandablePanel() {
        this("");
    }

    public ExpandablePanel(String title) {
        super(Orientation.VERTICAL);
        spacing(2.0f);
        this.title = normalize(title);
        this.richTitle = RichText.resolve(this.title);

        headerButton.layout(style -> style.size(LayoutConstraints.AUTO, HEADER_HEIGHT).flexGrow(0).flexShrink(0.0f));
        headerButton.silentChecked(expanded);
        headerButton.onCheckedChanged(event -> expanded(event.newValue()));

        contentHost.spacing(4.0f);
        contentHost.layout(style -> style.margin(6.0f, 4.0f));
        contentHost.layout(style -> style.flexGrow(0).flexShrink(0.0f));

        super.addChild(headerButton);
        super.addChild(contentHost);
        updateHeaderText();
    }

    public ExpandablePanel(RichText title) {
        this(title == null ? "" : title.plainText());
        richTitle(title);
    }

    public String title() {
        return title;
    }

    @XmlAttribute(value = "title", category = "Content", defaultValue = "", description = "Panel header title text.")
    public ExpandablePanel title(String title) {
        String normalized = normalize(title);
        if (Objects.equals(this.title, normalized)) return this;
        this.title = normalized;
        this.richTitle = RichText.resolve(normalized);
        updateHeaderText();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public RichText richTitle() {
        return richTitle;
    }

    public ExpandablePanel richTitle(RichText title) {
        RichText normalized = title == null ? RichText.plain("") : title;
        if (Objects.equals(this.richTitle, normalized)) return this;
        this.richTitle = normalized;
        this.title = normalized.plainText();
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

    @XmlAttribute(value = "expanded", category = "Behavior", defaultValue = "true", description = "Initial expanded state without emitting change events during XML load.")
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
        headerButton.richText(RichText.plain(expanded ? "\u25BE " : "\u25B8 ").append(richTitle));
    }

    private static String normalize(String title) {
        return title == null ? "" : title;
    }
}
