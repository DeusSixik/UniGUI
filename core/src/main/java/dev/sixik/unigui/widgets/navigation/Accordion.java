package dev.sixik.unigui.widgets.navigation;


import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiLink;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;

import dev.sixik.unigui.api.event.EventSubscription;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.api.xml.XmlAttribute;
import dev.sixik.unigui.api.xml.XmlWidgetName;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import dev.sixik.unigui.widgets.containers.LinearBox;
import dev.sixik.unigui.widgets.core.Orientation;

@AiDoc(summary = "Vertical accordion container managing a stack of collapsible ExpandablePanel sections with optional single-panel open constraint.")
@WikiPage(
        title = "Accordion",
        slug = "accordion",
        category = "<key:navigation.category>",
        summary = "<key:accordion.summary>"
)
@WikiSection(
        title = "<key:wiki.section.overview.title>",
        markdown = "<key:accordion.section.overview.body>",
        order = 10
)
@WikiExample(
        title = "<key:accordion.example.basic.title>",
        code = "Accordion accordion = new Accordion();\naccordion.singleOpen(true);\naccordion.addPanel(new ExpandablePanel(\"General\"));"
)
@WikiLink(
        label = "ExpandablePanel",
        target = "page:expandable-panel",
        description = "<key:expandable.panel.summary>"
)
@XmlWidgetName("Accordion")
public class Accordion extends LinearBox {
    public static final String STYLE_TYPE = dev.sixik.unigui.api.style.StyleIds.Widget.ACCORDION;

    private final List<ExpandablePanel> panels = new ObjectArrayList<>();
    private final Map<ExpandablePanel, EventSubscription> subscriptions = new IdentityHashMap<>();
    private boolean singleOpen = true;
    private boolean updating;

        @AiDoc(summary = "Constructs an empty Accordion container.")
    @WikiMethod(summary = "<key:accordion.method.ctorDefault>")
    public Accordion() {
        super(Orientation.VERTICAL);
        spacing(4.0f);
    }

        @AiDoc(summary = "Returns true if opening one panel automatically closes all other panels.")
    @WikiMethod(summary = "<key:accordion.method.singleOpenGet>")
    @WikiReturn("<key:accordion.return.singleOpen>")
    public boolean singleOpen() {
        return singleOpen;
    }

    @XmlAttribute(value = "singleOpen", category = "Behavior", defaultValue = "true", description = "Whether opening one panel automatically collapses the others.")
        @AiDoc(summary = "Sets whether only a single panel may be expanded simultaneously.")
    @WikiMethod(summary = "<key:accordion.method.singleOpenSet>")
    @WikiReturn("<key:accordion.return.this>")
    public Accordion singleOpen(
            @WikiParameter(description = "<key:accordion.param.singleOpen>")
            boolean singleOpen
    ) {
        if (this.singleOpen == singleOpen) return this;
        this.singleOpen = singleOpen;
        if (singleOpen) {
            enforceSingleOpen();
        }
        return this;
    }

        @AiDoc(summary = "Returns an unmodifiable list of expandable panels hosted in this accordion.")
    @WikiMethod(summary = "<key:accordion.method.panels>")
    @WikiReturn("<key:accordion.return.panels>")
    public List<ExpandablePanel> panels() {
        return Collections.unmodifiableList(panels);
    }

        @AiDoc(summary = "Appends an ExpandablePanel to this accordion.")
    @WikiMethod(summary = "<key:accordion.method.addPanel>")
    @WikiReturn("<key:accordion.return.this>")
    public Accordion addPanel(
            @WikiParameter(description = "<key:accordion.param.panel>")
            ExpandablePanel panel
    ) {
        if (panel == null || panels.contains(panel)) return this;
        panels.add(panel);
        subscriptions.put(panel, panel.onExpandedChanged(event -> {
            if (singleOpen && event.newValue() && !updating) {
                collapseOthers(panel);
            }
        }));
        super.addChild(panel);
        if (singleOpen && panel.expanded()) {
            collapseOthers(panel);
        }
        return this;
    }

        @AiDoc(summary = "Removes an ExpandablePanel from this accordion.")
    @WikiMethod(summary = "<key:accordion.method.removePanel>")
    @WikiReturn("<key:accordion.return.this>")
    public Accordion removePanel(
            @WikiParameter(description = "<key:accordion.param.panel>")
            ExpandablePanel panel
    ) {
        if (panel == null || !panels.remove(panel)) return this;
        EventSubscription subscription = subscriptions.remove(panel);
        if (subscription != null) {
            subscription.unsubscribe();
        }
        super.removeChild(panel);
        return this;
    }

        @AiDoc(summary = "Removes all panels from this accordion.")
    @WikiMethod(summary = "<key:accordion.method.clearPanels>")
    @WikiReturn("<key:accordion.return.this>")
    public Accordion clearPanels() {
        for (EventSubscription subscription : subscriptions.values()) {
            subscription.unsubscribe();
        }
        subscriptions.clear();
        panels.clear();
        super.clearChildren();
        return this;
    }

    @Override
    public void addChild(Widget child) {
        if (child instanceof ExpandablePanel panel) {
            addPanel(panel);
        } else {
            super.addChild(child);
        }
    }

    @Override
    public void removeChild(Widget child) {
        if (child instanceof ExpandablePanel panel) {
            removePanel(panel);
        } else {
            super.removeChild(child);
        }
    }

    @Override
    public void clearChildren() {
        clearPanels();
    }

    private void enforceSingleOpen() {
        ExpandablePanel open = null;
        for (ExpandablePanel panel : panels) {
            if (panel.expanded()) {
                open = panel;
                break;
            }
        }
        if (open != null) {
            collapseOthers(open);
        }
    }

    private void collapseOthers(ExpandablePanel keepOpen) {
        updating = true;
        try {
            for (ExpandablePanel panel : panels) {
                if (panel != keepOpen) {
                    panel.silentExpanded(false);
                }
            }
        } finally {
            updating = false;
        }
    }
}
