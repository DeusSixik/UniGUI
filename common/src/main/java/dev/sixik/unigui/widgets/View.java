package dev.sixik.unigui.widgets;

import dev.sixik.unigui.api.layout.LayoutConstraints;
import dev.sixik.unigui.api.widget.Widget;

public class View extends Box {
    private final VBox content = new VBox();
    private final Label title = new Label("");

    public View() {
        backgroundVisible(true);
        borderVisible(true);
        radius(5.0f);
        background().set(0.030f, 0.036f, 0.050f, 0.92f);
        borderColor().set(0.20f, 0.28f, 0.38f, 0.82f);
        content.spacing(6.0f);
        content.margin(8.0f);
        title.preferredSize(LayoutConstraints.AUTO, 18.0f).grow(0.0f);
        content.addChild(title);
        super.addChild(content);
    }

    public View(String title) {
        this();
        title(title);
    }

    public View title(String title) {
        this.title.text(title == null ? "" : title);
        return this;
    }

    public VBox content() {
        return content;
    }

    public View addContent(Widget widget) {
        content.addChild(widget);
        return this;
    }
}
