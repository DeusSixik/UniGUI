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

@AiDoc(summary = "Collapsible container widget with interactive title bar header and animated show/hide content host.")
@WikiPage(
        title = "ExpandablePanel",
        slug = "expandable-panel",
        category = "<key:navigation.category>",
        summary = "<key:expandable.panel.summary>"
)
@WikiSection(
        title = "<key:wiki.section.overview.title>",
        markdown = "<key:expandable.panel.section.overview.body>",
        order = 10
)
@WikiExample(
        title = "<key:expandable.panel.example.basic.title>",
        code = "ExpandablePanel panel = new ExpandablePanel(\"Advanced Settings\");\npanel.addContent(settingsBox);"
)
@WikiLink(
        label = "Accordion",
        target = "page:accordion",
        description = "<key:accordion.summary>"
)
@XmlWidgetName("ExpandablePanel")
public class ExpandablePanel extends LinearBox {
    public static final String STYLE_TYPE = dev.sixik.unigui.api.style.StyleIds.Widget.EXPANDABLE_PANEL;

    private static final float HEADER_HEIGHT = 32.0f;

    private final ToggleButton headerButton = new ToggleButton();
    private final VBox contentHost = new VBox();
    private String title = "";
    private RichText richTitle = RichText.plain("");
    private boolean expanded = true;

        @AiDoc(summary = "Constructs an empty ExpandablePanel.")
    @WikiMethod(summary = "<key:expandable.panel.method.ctorDefault>")
    public ExpandablePanel() {
        this("");
    }

        @AiDoc(summary = "Constructs an ExpandablePanel with a plain text title.")
    @WikiMethod(summary = "<key:expandable.panel.method.ctorTitle>")
    public ExpandablePanel(
            @WikiParameter(description = "<key:expandable.panel.param.title>")
            String title
    ) {
        super(Orientation.VERTICAL);
        spacing(2.0f);
        this.title = normalize(title);
        this.richTitle = RichText.resolve(this.title);

        headerButton.layout(style -> style.size(LayoutConstraints.AUTO, HEADER_HEIGHT).flexGrow(0).flexShrink(0.0f));
        headerButton.chevronVisible(true);
        headerButton.radius(4.0f);
        headerButton.silentChecked(expanded);
        headerButton.onCheckedChanged(event -> expanded(event.newValue()));

        contentHost.spacing(4.0f);
        contentHost.layout(style -> style.margin(6.0f, 4.0f));
        contentHost.layout(style -> style.flexGrow(0).flexShrink(0.0f));

        super.addChild(headerButton);
        super.addChild(contentHost);
        updateHeaderText();
    }

        @AiDoc(summary = "Constructs an ExpandablePanel with a RichText formatted title.")
    @WikiMethod(summary = "<key:expandable.panel.method.ctorRichTitle>")
    public ExpandablePanel(
            @WikiParameter(description = "<key:expandable.panel.param.title>")
            RichText title
    ) {
        this(title == null ? "" : title.plainText());
        richTitle(title);
    }

        @AiDoc(summary = "Returns the plain text title string.")
    @WikiMethod(summary = "<key:expandable.panel.method.titleGet>")
    @WikiReturn("<key:expandable.panel.return.title>")
    public String title() {
        return title;
    }

    @XmlAttribute(value = "title", category = "Content", defaultValue = "", description = "Panel header title text.")
        @AiDoc(summary = "Sets the plain text title.")
    @WikiMethod(summary = "<key:expandable.panel.method.titleSet>")
    @WikiReturn("<key:expandable.panel.return.this>")
    public ExpandablePanel title(
            @WikiParameter(description = "<key:expandable.panel.param.title>")
            String title
    ) {
        String normalized = normalize(title);
        if (Objects.equals(this.title, normalized)) return this;
        this.title = normalized;
        this.richTitle = RichText.resolve(normalized);
        updateHeaderText();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

        @AiDoc(summary = "Returns the RichText formatted title.")
    @WikiMethod(summary = "<key:expandable.panel.method.richTitleGet>")
    @WikiReturn("<key:expandable.panel.return.richTitle>")
    public RichText richTitle() {
        return richTitle;
    }

        @AiDoc(summary = "Sets the RichText formatted title.")
    @WikiMethod(summary = "<key:expandable.panel.method.richTitleSet>")
    @WikiReturn("<key:expandable.panel.return.this>")
    public ExpandablePanel richTitle(
            @WikiParameter(description = "<key:expandable.panel.param.title>")
            RichText title
    ) {
        RichText normalized = title == null ? RichText.plain("") : title;
        if (Objects.equals(this.richTitle, normalized)) return this;
        this.richTitle = normalized;
        this.title = normalized.plainText();
        updateHeaderText();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

        @AiDoc(summary = "Returns true if the panel content is currently expanded.")
    @WikiMethod(summary = "<key:expandable.panel.method.expandedGet>")
    @WikiReturn("<key:expandable.panel.return.expanded>")
    public boolean expanded() {
        return expanded;
    }

        @AiDoc(summary = "Sets the expanded state and dispatches an expansion event.")
    @WikiMethod(summary = "<key:expandable.panel.method.expandedSet>")
    @WikiReturn("<key:expandable.panel.return.this>")
    public ExpandablePanel expanded(
            @WikiParameter(description = "<key:expandable.panel.param.expanded>")
            boolean expanded
    ) {
        setExpanded(expanded, true);
        return this;
    }

    @XmlAttribute(value = "expanded", category = "Behavior", defaultValue = "true", description = "Initial expanded state without emitting change events during XML load.")
        @AiDoc(summary = "Sets the expanded state without triggering event callbacks.")
    @WikiMethod(summary = "<key:expandable.panel.method.silentExpanded>")
    @WikiReturn("<key:expandable.panel.return.this>")
    public ExpandablePanel silentExpanded(
            @WikiParameter(description = "<key:expandable.panel.param.expanded>")
            boolean expanded
    ) {
        setExpanded(expanded, false);
        return this;
    }

        @AiDoc(summary = "Toggles between expanded and collapsed states.")
    @WikiMethod(summary = "<key:expandable.panel.method.toggleExpanded>")
    @WikiReturn("<key:expandable.panel.return.this>")
    public ExpandablePanel toggleExpanded() {
        return expanded(!expanded);
    }

        @AiDoc(summary = "Returns the header ToggleButton controlling panel expansion.")
    @WikiMethod(summary = "<key:expandable.panel.method.headerButton>")
    @WikiReturn("<key:expandable.panel.return.headerButton>")
    public ToggleButton headerButton() {
        return headerButton;
    }

        @AiDoc(summary = "Returns the vertical container hosting expandable content widgets.")
    @WikiMethod(summary = "<key:expandable.panel.method.contentHost>")
    @WikiReturn("<key:expandable.panel.return.contentHost>")
    public VBox contentHost() {
        return contentHost;
    }

        @AiDoc(summary = "Adds a content widget into the expandable body.")
    @WikiMethod(summary = "<key:expandable.panel.method.addContent>")
    @WikiReturn("<key:expandable.panel.return.this>")
    public ExpandablePanel addContent(
            @WikiParameter(description = "<key:expandable.panel.param.child>")
            Widget child
    ) {
        if (child == null) return this;
        contentHost.addChild(child);
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

        @AiDoc(summary = "Removes a content widget from the expandable body.")
    @WikiMethod(summary = "<key:expandable.panel.method.removeContent>")
    @WikiReturn("<key:expandable.panel.return.this>")
    public ExpandablePanel removeContent(
            @WikiParameter(description = "<key:expandable.panel.param.child>")
            Widget child
    ) {
        if (child == null) return this;
        contentHost.removeChild(child);
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

        @AiDoc(summary = "Clears all content widgets from the expandable body.")
    @WikiMethod(summary = "<key:expandable.panel.method.clearContent>")
    @WikiReturn("<key:expandable.panel.return.this>")
    public ExpandablePanel clearContent() {
        contentHost.clearChildren();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

        @AiDoc(summary = "Registers a listener invoked when the panel expands or collapses.")
    @WikiMethod(summary = "<key:expandable.panel.method.onExpandedChanged>")
    @WikiReturn("<key:expandable.panel.return.subscription>")
    public EventSubscription onExpandedChanged(
            @WikiParameter(description = "<key:expandable.panel.param.listener>")
            EventListener<? super ExpandedChangedEvent> listener
    ) {
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
        headerButton.richText(richTitle);
        headerButton.chevronUp(expanded);
    }

    private static String normalize(String title) {
        return title == null ? "" : title;
    }
}
