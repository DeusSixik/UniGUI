package dev.sixik.unigui.xaml;

import dev.sixik.unigui.api.layout.LayoutStyle;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.impl.widget.WidgetBase;
import dev.sixik.unigui.widgets.containers.ScrollView;
import dev.sixik.unigui.widgets.display.TextWidget;
import dev.sixik.unigui.widgets.interaction.Button;

/**
 * Независимый от загрузчика дамп дерева виджетов: тот же формат, что у {@code golden/tree-*.txt}, снятых со
 * старого загрузчика. Совпадение дампов — прямое доказательство эквивалентности загрузки.
 */
final class TreeDump {
    private TreeDump() {
    }

    static String dump(Widget root) {
        StringBuilder out = new StringBuilder();
        dump(root, 0, out);
        return out.toString();
    }

    private static void dump(Widget widget, int depth, StringBuilder out) {
        out.append("  ".repeat(depth)).append(widget.getClass().getSimpleName());
        if (!widget.id().isBlank()) out.append(" #").append(widget.id());
        if (!widget.enabled()) out.append(" disabled");
        out.append(" vis=").append(widget.visibility());
        if (widget instanceof WidgetBase base) {
            LayoutStyle style = base.layoutStyle();
            out.append(" w=").append(style.width())
                    .append(" h=").append(style.height())
                    .append(" pad=").append(style.padding())
                    .append(" mar=").append(style.margin())
                    .append(" grow=").append(style.flexGrow())
                    .append(" shrink=").append(style.flexShrink())
                    .append(" opacity=").append(base.opacity());
        }
        if (widget instanceof TextWidget text) out.append(" text=\"").append(text.text()).append('"');
        if (widget instanceof Button button) out.append(" button=\"").append(button.text()).append('"');
        out.append('\n');
        if (widget instanceof ScrollView scroll) {
            if (scroll.content() != null) dump(scroll.content(), depth + 1, out);
            return;
        }
        for (Widget child : widget.children()) dump(child, depth + 1, out);
    }
}
