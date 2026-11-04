package dev.sixik.unigui.xaml.schema.builtin;

import dev.sixik.unigui.widgets.data.VirtualListView;
import dev.sixik.unigui.widgets.data.VirtualTableColumn;
import dev.sixik.unigui.widgets.data.VirtualTableView;
import dev.sixik.unigui.xaml.schema.Converters;
import dev.sixik.unigui.xaml.schema.Meta;
import dev.sixik.unigui.xaml.schema.Schema;

import java.util.ArrayList;
import java.util.List;

import static dev.sixik.unigui.xaml.schema.builtin.Types.type;

/** Виртуализированные данные: списки и таблицы. */
final class DataTypes {
    private DataTypes() {
    }

    static void register(Schema schema) {
        schema.define("VirtualListView", () -> type("VirtualListView", VirtualListView.class, VirtualListView::new)
                .describe("Virtual List View", "Data", "Virtualized list or responsive grid for large item sets.")
                .build());
        schema.define("VirtualTableView", () -> type("VirtualTableView", VirtualTableView.class, VirtualTableView::new)
                .describe("Virtual Table View", "Data", "Virtualized table for property grids, diagnostics and data views.")
                .property("columns", Converters.STRING, DataTypes::columns,
                        Meta.category("Data").description(
                                "Column definitions separated by '|', ';' or line breaks. Use Header or Header:Width."))
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
