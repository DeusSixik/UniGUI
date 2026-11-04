package dev.sixik.unigui.xaml.schema.builtin;

import dev.sixik.unigui.widgets.containers.Box;
import dev.sixik.unigui.widgets.containers.FlexBox;
import dev.sixik.unigui.widgets.containers.GridBox;
import dev.sixik.unigui.widgets.containers.HBox;
import dev.sixik.unigui.widgets.containers.PanelWidget;
import dev.sixik.unigui.widgets.containers.ScrollView;
import dev.sixik.unigui.widgets.containers.SettingRow;
import dev.sixik.unigui.widgets.containers.StackPanel;
import dev.sixik.unigui.widgets.containers.VBox;
import dev.sixik.unigui.widgets.containers.WrapPanel;
import dev.sixik.unigui.widgets.core.Orientation;
import dev.sixik.unigui.widgets.effects.PostProcessingLayer;
import dev.sixik.unigui.xaml.schema.ChildSink;
import dev.sixik.unigui.xaml.schema.Converters;
import dev.sixik.unigui.xaml.schema.Schema;
import dev.sixik.unigui.xaml.schema.SlotInfo;

import static dev.sixik.unigui.xaml.schema.builtin.Types.panel;
import static dev.sixik.unigui.xaml.schema.builtin.Types.type;

/** Контейнеры: Panel, Box, VBox, HBox, FlexBox, GridBox, StackPanel, WrapPanel, ScrollView и родственные. */
final class ContainerTypes {
    private ContainerTypes() {
    }

    static void register(Schema schema) {
        schema.define("Panel", () -> panel(type("Panel", PanelWidget.class, PanelWidget::new)).build());
        schema.define("Box", () -> panel(type("Box", Box.class, Box::new)).build());
        schema.define("VBox", () -> panel(type("VBox", VBox.class, VBox::new))
                .property("spacing", Converters.FLOAT, (widget, value) -> widget.spacing(value)).build());
        schema.define("HBox", () -> panel(type("HBox", HBox.class, HBox::new))
                .property("spacing", Converters.FLOAT, (widget, value) -> widget.spacing(value)).build());
        schema.define("FlexBox", () -> panel(type("FlexBox", FlexBox.class, FlexBox::new)).build());
        schema.define("StackPanel", () -> panel(type("StackPanel", StackPanel.class, StackPanel::new)).build());
        schema.define("PostProcessingLayer",
                () -> panel(type("PostProcessingLayer", PostProcessingLayer.class, PostProcessingLayer::new)).build());
        schema.define("WrapPanel", () -> panel(type("WrapPanel", WrapPanel.class, WrapPanel::new))
                .property("orientation", Converters.enumOf(Orientation.class), WrapPanel::orientation)
                .property("spacing", Converters.FLOAT, WrapPanel::spacing)
                .property("lineSpacing", Converters.FLOAT, WrapPanel::lineSpacing)
                .build());
        schema.define("ScrollView", ContainerTypes::scrollView);
        schema.define("GridBox", () -> panel(type("GridBox", GridBox.class, GridBox::new)).build());
        schema.define("SettingRow", () -> type("SettingRow", SettingRow.class, SettingRow::new).build());
    }

    private static dev.sixik.unigui.xaml.schema.TypeInfo<ScrollView> scrollView() {
        ChildSink<ScrollView> content = (parent, child) -> {
            if (parent.content() != null) {
                throw new IllegalArgumentException("Widget ScrollView can contain only one content child.");
            }
            parent.content(child);
        };
        return type("ScrollView", ScrollView.class, ScrollView::new)
                .property("contentWidth", Converters.FLOAT, (widget, value) -> widget.contentSize(value, widget.contentHeight()))
                .property("contentHeight", Converters.FLOAT, (widget, value) -> widget.contentSize(widget.contentWidth(), value))
                .property("scrollStep", Converters.FLOAT, ScrollView::scrollStep)
                .property("scrollbarGap", Converters.FLOAT, ScrollView::scrollbarGap)
                .property("scrollingEnabled", Converters.BOOLEAN, ScrollView::scrollingEnabled)
                .property("consumeWheelAtScrollBounds", Converters.BOOLEAN, ScrollView::consumeWheelAtScrollBounds)
                .property("scrollbarTrackColor", Converters.COLOR, (widget, color) -> widget.scrollbarTrackColor().set(color))
                .property("scrollbarThumbColor", Converters.COLOR, (widget, color) -> widget.scrollbarThumbColor().set(color))
                .content(content)
                .slot(SlotInfo.single("Content", content, "Single scrollable content widget."))
                .build();
    }
}
