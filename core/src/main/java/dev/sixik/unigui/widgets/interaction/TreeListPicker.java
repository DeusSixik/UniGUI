package dev.sixik.unigui.widgets.interaction;

import dev.sixik.unigui.api.xml.XmlWidgetName;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;
import java.util.function.Function;

@XmlWidgetName("TreeListPicker")
public final class TreeListPicker<T> extends ComboBox {
    public static final String STYLE_TYPE = dev.sixik.unigui.api.style.StyleIds.Widget.TREE_LIST_PICKER;

    private final List<T> values = new ObjectArrayList<>();
    private Function<T, String> labelProvider = value -> value == null ? "" : value.toString();

    public TreeListPicker<T> values(List<T> values) {
        this.values.clear();
        if (values != null) this.values.addAll(values);
        refreshItems();
        return this;
    }

    public TreeListPicker<T> labelProvider(Function<T, String> labelProvider) {
        this.labelProvider = labelProvider == null ? value -> value == null ? "" : value.toString() : labelProvider;
        refreshItems();
        return this;
    }

    public T selectedValue() {
        int index = selectedIndex();
        return index >= 0 && index < values.size() ? values.get(index) : null;
    }

    private void refreshItems() {
        List<String> labels = new ObjectArrayList<>(values.size());
        for (T value : values) {
            labels.add(labelProvider.apply(value));
        }
        items(labels);
    }
}
