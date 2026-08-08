package dev.sixik.unigui.impl.layout.v3;

import dev.sixik.unigui.api.layout.v3.LayoutNode;
import dev.sixik.unigui.api.layout.v3.LayoutNodeId;
import dev.sixik.unigui.api.layout.v3.LayoutStyleMapper;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.impl.widget.WidgetBase;

import java.util.Objects;

/** Converts the current widget tree into a backend-neutral Layout V3 tree. */
public final class LayoutTreeBuilder {
    public LayoutNode build(Widget root) {
        return build(root, "root");
    }

    private LayoutNode build(Widget widget, String path) {
        Objects.requireNonNull(widget, "widget");
        LayoutNode.Builder builder = LayoutNode.builder(LayoutNodeId.of(path))
                .debugName(widget.getClass().getSimpleName())
                .style(widget instanceof WidgetBase base
                        ? LayoutStyleMapper.from(base.layoutStyle())
                        : LayoutStyleMapper.from(widget.layoutConstraints()))
                .measure(context -> {
                    widget.measure(context);
                    return widget.desiredSize();
                });

        int index = 0;
        for (Widget child : widget.children()) {
            if (child.visibility() == Visibility.COLLAPSED) {
                continue;
            }
            builder.child(build(child, path + "/" + index));
            index++;
        }
        return builder.build();
    }
}
