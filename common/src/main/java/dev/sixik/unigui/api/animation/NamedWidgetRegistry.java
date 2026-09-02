package dev.sixik.unigui.api.animation;

import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.widgets.containers.PanelWidget;
import dev.sixik.unigui.widgets.containers.ScrollView;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;

/**
 * Однократно построенный индекс runtime-виджетов по {@link Widget#id()}.
 *
 * <p>Registry использует те же id, что XML-разметка и {@code XMLWidget.getWidget(...)},
 * но storyboard разрешает все цели один раз при создании player, а не обходит дерево на каждом tick.</p>
 */
public final class NamedWidgetRegistry {
    private final Widget root;
    private final Object2ObjectOpenHashMap<String, Widget> widgets = new Object2ObjectOpenHashMap<>();

    public NamedWidgetRegistry(Widget root) {
        if (root == null) throw new IllegalArgumentException("Корневой виджет не должен быть null.");
        this.root = root;
        index(root);
    }

    public static NamedWidgetRegistry from(Widget root) {
        return new NamedWidgetRegistry(root);
    }

    /** Возвращает корневой виджет registry. */
    public Widget root() { return root; }

    /** Возвращает виджет по id или {@code null}. Пустое имя обозначает root. */
    public Widget find(String name) {
        if (name == null || name.isBlank()) return root;
        return widgets.get(name.trim());
    }

    /** Возвращает обязательный виджет по id. */
    public Widget require(String name) {
        Widget widget = find(name);
        if (widget != null) return widget;
        throw new IllegalArgumentException("Storyboard target не найден: " + name);
    }

    public int size() { return widgets.size(); }

    private void index(Widget root) {
        ObjectArrayList<Widget> stack = new ObjectArrayList<>();
        ReferenceOpenHashSet<Widget> visited = new ReferenceOpenHashSet<>();
        stack.push(root);
        while (!stack.isEmpty()) {
            Widget widget = stack.pop();
            if (widget == null || !visited.add(widget)) continue;

            if (widget instanceof PanelWidget panel) panel.applyQueuedMutations();

            String id = widget.id();
            if (id != null && !id.isBlank()) {
                String normalized = id.trim();
                Widget previous = widgets.putIfAbsent(normalized, widget);
                if (previous != null && previous != widget) {
                    throw new IllegalArgumentException("Повторяющийся Widget.id в storyboard registry: " + normalized);
                }
            }

            if (widget instanceof ScrollView scrollView && scrollView.content() != null) {
                stack.push(scrollView.content());
            }
            for (int i = widget.children().size() - 1; i >= 0; i--) {
                Widget child = widget.children().get(i);
                if (child != null) stack.push(child);
            }
        }
    }
}
