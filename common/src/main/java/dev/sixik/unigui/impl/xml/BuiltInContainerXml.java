package dev.sixik.unigui.impl.xml;

import dev.sixik.unigui.api.xml.XmlPropertyChildDescriptor;
import dev.sixik.unigui.impl.widget.WidgetBase;
import dev.sixik.unigui.widgets.containers.Box;
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

final class BuiltInContainerXml {
    private BuiltInContainerXml() {
    }

    static void register(WidgetXmlRegistry registry) {
        panel(registry.register("Panel", PanelWidget::new));
        panel(BuiltInWidgetXmlSupport.box(registry.register("Box", Box::new)));
        panel(linear(registry.register("VBox", VBox::new)));
        panel(linear(registry.register("HBox", HBox::new)));
        panel(registry.register("StackPanel", StackPanel::new));
        postProcessingLayer(panel(registry.register("PostProcessingLayer", PostProcessingLayer::new)));
        panel(wrapPanel(registry.register("WrapPanel", WrapPanel::new)));
        scrollView(registry.register("ScrollView", ScrollView::new));
        BuiltInWidgetXmlSupport.annotated(panel(registry.register("GridBox", GridBox::new)), GridBox.class);
        BuiltInWidgetXmlSupport.commonWidget(registry.register("SettingRow", SettingRow::new), SettingRow.class);
    }

    private static <T extends PanelWidget> WidgetXmlType<T> panel(WidgetXmlType<T> type) {
        XmlChildPolicy<T> children = (parent, child) -> {
            parent.addChild(child);
            parent.applyQueuedMutations();
        };
        return BuiltInWidgetXmlSupport.commonWidget(type)
                .childPolicy(children)
                .propertyChild("Children", children,
                        XmlPropertyChildDescriptor.of("Children")
                                .category("Content")
                                .description("Child widgets hosted by this container."));
    }

    private static WidgetXmlType<PostProcessingLayer> postProcessingLayer(WidgetXmlType<PostProcessingLayer> type) {
        return BuiltInWidgetXmlSupport.annotated(type, PostProcessingLayer.class);
    }

    private static WidgetXmlType<ScrollView> scrollView(WidgetXmlType<ScrollView> type) {
        XmlChildPolicy<ScrollView> content = (parent, child) -> {
            if (parent.content() != null) {
                throw new IllegalArgumentException("Widget ScrollView can contain only one content child.");
            }
            parent.content(child);
        };
        return BuiltInWidgetXmlSupport.commonWidget(type)
                .attribute("contentWidth", XmlValueParsers.FLOAT, (widget, value) -> widget.contentSize(value, widget.contentHeight()))
                .attribute("contentHeight", XmlValueParsers.FLOAT, (widget, value) -> widget.contentSize(widget.contentWidth(), value))
                .attribute("scrollStep", XmlValueParsers.FLOAT, ScrollView::scrollStep)
                .attribute("scrollbarGap", XmlValueParsers.FLOAT, ScrollView::scrollbarGap)
                .attribute("scrollingEnabled", XmlValueParsers.BOOLEAN, ScrollView::scrollingEnabled)
                .attribute("consumeWheelAtScrollBounds", XmlValueParsers.BOOLEAN, ScrollView::consumeWheelAtScrollBounds)
                .attribute("scrollbarTrackColor", XmlValueParsers.COLOR, (widget, color) -> widget.scrollbarTrackColor().set(color))
                .attribute("scrollbarThumbColor", XmlValueParsers.COLOR, (widget, color) -> widget.scrollbarThumbColor().set(color))
                .childPolicy(content)
                .propertyChild("Content", content,
                        XmlPropertyChildDescriptor.of("Content")
                                .category("Content")
                                .description("Single scrollable content widget.")
                                .singleChildOnly());
    }

    private static <T extends WidgetBase> WidgetXmlType<T> linear(WidgetXmlType<T> type) {
        return BuiltInWidgetXmlSupport.commonWidget(type)
                .attribute("spacing", XmlValueParsers.FLOAT, (widget, value) -> {
                    if (widget instanceof VBox vbox) vbox.spacing(value);
                    if (widget instanceof HBox hbox) hbox.spacing(value);
                });
    }

    private static WidgetXmlType<WrapPanel> wrapPanel(WidgetXmlType<WrapPanel> type) {
        return BuiltInWidgetXmlSupport.commonWidget(type)
                .attribute("orientation", XmlValueParsers.enumValue(Orientation.class), WrapPanel::orientation)
                .attribute("spacing", XmlValueParsers.FLOAT, WrapPanel::spacing)
                .attribute("lineSpacing", XmlValueParsers.FLOAT, WrapPanel::lineSpacing);
    }
}
