package dev.sixik.unigui.impl.xml;

import dev.sixik.unigui.api.xml.XmlAttributeDescriptor;
import dev.sixik.unigui.widgets.data.VirtualListView;
import dev.sixik.unigui.widgets.data.VirtualTableColumn;
import dev.sixik.unigui.widgets.data.VirtualTableView;

import java.util.ArrayList;
import java.util.List;

final class BuiltInDataXml {
    private BuiltInDataXml() {
    }

    static void register(WidgetXmlRegistry registry) {
        BuiltInWidgetXmlSupport.commonWidget(registry.register("VirtualListView", VirtualListView::new), VirtualListView.class)
                .describe("Virtual List View", "Data", "Virtualized list for large item sets.");
        BuiltInWidgetXmlSupport.commonWidget(registry.register("VirtualTableView", VirtualTableView::new), VirtualTableView.class)
                .describe("Virtual Table View", "Data", "Virtualized table for property grids, diagnostics and data views.")
                .attribute("columns", XmlValueParsers.STRING, BuiltInDataXml::columns,
                        XmlAttributeDescriptor.of("columns")
                                .category("Data")
                                .defaultValue("")
                                .description("Column definitions separated by '|', ';' or line breaks. Use Header or Header:Width."));
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
