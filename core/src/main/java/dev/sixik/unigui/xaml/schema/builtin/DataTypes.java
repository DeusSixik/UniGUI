package dev.sixik.unigui.xaml.schema.builtin;

import dev.sixik.unigui.widgets.data.VirtualListView;
import dev.sixik.unigui.widgets.data.VirtualTableColumn;
import dev.sixik.unigui.widgets.data.VirtualTableView;
import dev.sixik.unigui.xaml.schema.Converters;
import dev.sixik.unigui.xaml.schema.Meta;
import dev.sixik.unigui.xaml.schema.Schema;
import dev.sixik.unigui.xaml.schema.SlotInfo;
import dev.sixik.unigui.xaml.schema.Template;
import dev.sixik.unigui.xaml.schema.TemplateSlot;
import dev.sixik.unigui.xaml.schema.ValueConverter;
import dev.sixik.unigui.xaml.schema.ValueKind;

import java.util.ArrayList;
import java.util.List;

import static dev.sixik.unigui.xaml.schema.builtin.Types.type;

/**
 * Виртуализированные данные: списки и таблицы.
 */
final class DataTypes {
    /**
     * Источник данных нельзя написать текстом: он приходит из привязки или из Java.
     */
    @SuppressWarnings("rawtypes")
    private static final ValueConverter<List> ITEMS = ValueConverter.of(List.class, ValueKind.BINDING_OR_ACTION,
            text -> {
                throw new IllegalArgumentException("items cannot be written as text; bind it with {Binding path}");
            });

    private DataTypes() {
    }

    static void register(Schema schema) {
        schema.define("VirtualListView", () -> type("VirtualListView", VirtualListView.class, VirtualListView::new)
                .describe("Virtual List View", "Data", "Virtualized list or responsive grid for large item sets.")
                .property("items", ITEMS, (view, list) -> VirtualItems.of(view).source(list),
                        Meta.category("Data").kind(ValueKind.BINDING_OR_ACTION).description(
                                "Data source: a list (usually an observable one) bound with {Binding path}. "
                                        + "Each element becomes a row built from ItemTemplate."))
                .bindable("items")
                .slot(SlotInfo.template("ItemTemplate", new TemplateSlot<VirtualListView>() {
                    @Override
                    public void accept(VirtualListView parent, Template template) {
                        VirtualItems.of(parent).template(template);
                    }

                    @Override
                    public Template current(VirtualListView parent) {
                        return VirtualItems.of(parent).template();
                    }
                }, "Row template. Use {Item} or {Item path} to read the current element."))
                // Показанные строки порождены шаблоном и в документ не пишутся: сохраняется сам шаблон.
                .exportsChildren(view -> List.of())
                .build());
        schema.define("VirtualTableView", () -> type("VirtualTableView", VirtualTableView.class, VirtualTableView::new)
                .describe("Virtual Table View", "Data", "Virtualized table for property grids, diagnostics and data views.")
                .property("columns", Converters.STRING, DataTypes::columns,
                        Meta.category("Data").description(
                                "Column definitions separated by '|', ';' or line breaks. Use Header or Header:Width."))
                .reads("columns", table -> {
                    StringBuilder text = new StringBuilder();
                    for (VirtualTableColumn column : table.columns()) {
                        if (text.length() > 0) text.append('|');
                        text.append(column.header()).append(':');
                        float width = column.width();
                        text.append(width == Math.rint(width) ? Long.toString((long) width) : Float.toString(width));
                    }
                    return text.toString();
                })
                .build());
    }

    private static void columns(VirtualTableView table, String value) {
        List<VirtualTableColumn> columns = new ArrayList<>();
        if (value != null && !value.isBlank()) {
            for (String token : value.split("[|;\\r\\n]+")) {
                String definition = token.trim();
                if (definition.isEmpty()) continue;
                int widthSeparator = definition.lastIndexOf(':');
                String header = widthSeparator > 0 ? definition.substring(0, widthSeparator).trim() : definition;
                float width = widthSeparator > 0
                        ? Float.parseFloat(definition.substring(widthSeparator + 1).trim())
                        : 80.0f;
                columns.add(new VirtualTableColumn(header, width));
            }
        }
        table.columns(columns);
    }
}
