package dev.sixik.unigui.impl.xml;

import dev.sixik.unigui.widgets.display.Chart;
import dev.sixik.unigui.widgets.core.Orientation;
import dev.sixik.unigui.widgets.display.ImageView;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.widgets.display.Separator;
import dev.sixik.unigui.widgets.display.Shape;
import dev.sixik.unigui.widgets.display.Sparkline;
import dev.sixik.unigui.widgets.display.Text;
import dev.sixik.unigui.widgets.display.TextBlock;
import dev.sixik.unigui.widgets.display.TextWidget;
import dev.sixik.unigui.widgets.display.TextureWidget;

final class BuiltInDisplayXml {
    private BuiltInDisplayXml() {
    }

    static void register(WidgetXmlRegistry registry) {
        BuiltInWidgetXmlSupport.textWidget(registry.register("TextWidget", TextWidget::new), TextWidget.class);
        BuiltInWidgetXmlSupport.textWidget(registry.register("Text", Text::new), Text.class);
        BuiltInWidgetXmlSupport.textWidget(registry.register("TextBlock", TextBlock::new), TextBlock.class);
        BuiltInWidgetXmlSupport.textWidget(registry.register("Label", Label::new), Label.class);
        BuiltInWidgetXmlSupport.textureWidget(registry.register("TextureWidget", TextureWidget::new), TextureWidget.class);
        BuiltInWidgetXmlSupport.textureWidget(registry.register("ImageView", ImageView::new), ImageView.class);

        separator(registry.register("Separator", Separator::new));
        BuiltInWidgetXmlSupport.commonWidget(registry.register("Shape", Shape::new), Shape.class);
        BuiltInWidgetXmlSupport.commonWidget(registry.register("Sparkline", Sparkline::new), Sparkline.class);
        BuiltInWidgetXmlSupport.commonWidget(registry.register("Chart", Chart::new), Chart.class);
    }

    private static WidgetXmlType<Separator> separator(WidgetXmlType<Separator> type) {
        return BuiltInWidgetXmlSupport.commonWidget(type)
                .attribute("color", XmlValueParsers.COLOR, (widget, color) -> widget.color().set(color))
                .attribute("orientation", XmlValueParsers.enumValue(Orientation.class), Separator::orientation)
                .attribute("thickness", XmlValueParsers.FLOAT, Separator::thickness);
    }
}
