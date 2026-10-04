package dev.sixik.unigui.impl.layout.v3;

import dev.sixik.unigui.api.layout.v3.LayoutNode;
import dev.sixik.unigui.api.layout.v3.LayoutNodeId;
import dev.sixik.unigui.api.layout.v3.LayoutStyleMapper;
import dev.sixik.unigui.api.layout.v3.LayoutStyleSnapshot;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.impl.layout.flex.FlexSolver;
import dev.sixik.unigui.impl.layout.flex.FlexStyleViews;
import dev.sixik.unigui.impl.widget.WidgetBase;

import java.util.Objects;

/** Преобразует текущее дерево виджетов в нейтральное к бэкенду дерево Layout V3. */
public final class LayoutTreeBuilder {
    public LayoutNode build(Widget root) {
        return build(root, "root");
    }

    private LayoutNode build(Widget widget, String path) {
        Objects.requireNonNull(widget, "widget");
        LayoutStyleSnapshot snapshot = widget instanceof WidgetBase base
                ? LayoutStyleMapper.from(base.layoutStyle())
                : LayoutStyleMapper.from(widget.layoutConstraints());
        LayoutNode.Builder builder = LayoutNode.builder(LayoutNodeId.of(path))
                .debugName(widget.getClass().getSimpleName())
                .style(snapshot)
                .baseline(widget.contentBaseline())
                .measure(context -> {
                    widget.measure(context);
                    return widget.desiredSize();
                });
        if (FlexSolver.needsIntrinsicContent(FlexStyleViews.of(snapshot), true)
                || FlexSolver.needsIntrinsicContent(FlexStyleViews.of(snapshot), false)) {
            builder.minContentSize(widget.minContentSize());
            builder.maxContentSize(widget.maxContentSize());
        }

        int index = 0;
        for (Widget child : widget.children()) {
            if (child.visibility() == Visibility.COLLAPSED) {
                index++;
                continue;
            }
            builder.child(build(child, path + "/" + index));
            index++;
        }
        return builder.build();
    }
}
