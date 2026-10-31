package dev.sixik.unigui.widgets.interaction;

import dev.sixik.unigui.api.xml.XmlWidgetName;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;
import java.util.function.Function;
import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;

@AiDoc(
        summary = "Generic dropdown picker binding domain objects to displayed text options.",
        purpose = "Provides strongly typed selection over domain models without losing object references.",
        useWhen = {
                "Selecting complex model objects (players, entities, presets) in dropdowns.",
                "Custom label formatting for domain items."
        },
        criticalRules = {
                "selectedValue() returns null if nothing is selected or list is empty.",
                "labelProvider determines the displayed string for each item."
        },
        related = {"ComboBox", "DropDownBox"})
@WikiPage(
        title = "<key:tree.list.picker.title>",
        slug = "tree-list-picker",
        category = "<key:core.widgets.interaction.category>",
        summary = "<key:tree.list.picker.summary>",
        order = 240)
@WikiSection(
        title = "<key:tree.list.picker.section.overview.title>",
        markdown = "<key:tree.list.picker.section.overview.body>",
        order = 10)
@WikiExample(
        title = "<key:tree.list.picker.example.basic.title>",
        code = "<key:tree.list.picker.example.basic.code>",
        order = 10)
@XmlWidgetName("TreeListPicker")
public final class TreeListPicker<T> extends ComboBox {
    public static final String STYLE_TYPE = dev.sixik.unigui.api.style.StyleIds.Widget.TREE_LIST_PICKER;

    private final List<T> values = new ObjectArrayList<>();
    private Function<T, String> labelProvider = value -> value == null ? "" : value.toString();

    @WikiMethod(
            title = "<key:tree.list.picker.method.values.title>",
            summary = "<key:tree.list.picker.method.values.summary>")
    public TreeListPicker<T> values(List<T> values) {
        this.values.clear();
        if (values != null) this.values.addAll(values);
        refreshItems();
        return this;
    }

    @WikiMethod(
            title = "<key:tree.list.picker.method.label_provider.title>",
            summary = "<key:tree.list.picker.method.label_provider.summary>")
    public TreeListPicker<T> labelProvider(Function<T, String> labelProvider) {
        this.labelProvider = labelProvider == null ? value -> value == null ? "" : value.toString() : labelProvider;
        refreshItems();
        return this;
    }

    @WikiMethod(
            title = "<key:tree.list.picker.method.selected_value.title>",
            summary = "<key:tree.list.picker.method.selected_value.summary>")
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