package dev.sixik.unigui.widgets.navigation;

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

@XmlWidgetName("Accordion")
public class Accordion extends LinearBox {
    private final List<ExpandablePanel> panels = new ObjectArrayList<>();
    private final Map<ExpandablePanel, EventSubscription> subscriptions = new IdentityHashMap<>();
    private boolean singleOpen = true;
    private boolean updating;

    public Accordion() {
        super(Orientation.VERTICAL);
        spacing(4.0f);
    }

    public boolean singleOpen() {
        return singleOpen;
    }

    @XmlAttribute(value = "singleOpen", category = "Behavior", defaultValue = "true", description = "Whether opening one panel automatically collapses the others.")
    public Accordion singleOpen(boolean singleOpen) {
        if (this.singleOpen == singleOpen) return this;
        this.singleOpen = singleOpen;
        if (singleOpen) {
            enforceSingleOpen();
        }
        return this;
    }

    public List<ExpandablePanel> panels() {
        return Collections.unmodifiableList(panels);
    }

    public Accordion addPanel(ExpandablePanel panel) {
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

    public Accordion removePanel(ExpandablePanel panel) {
        if (panel == null || !panels.remove(panel)) return this;
        EventSubscription subscription = subscriptions.remove(panel);
        if (subscription != null) {
            subscription.unsubscribe();
        }
        super.removeChild(panel);
        return this;
    }

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
