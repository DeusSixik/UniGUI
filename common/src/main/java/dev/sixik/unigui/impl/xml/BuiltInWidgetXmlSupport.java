package dev.sixik.unigui.impl.xml;

import dev.sixik.unigui.api.layout.Align;
import dev.sixik.unigui.api.layout.Alignment;
import dev.sixik.unigui.api.layout.FlexDirection;
import dev.sixik.unigui.api.layout.FlexWrap;
import dev.sixik.unigui.api.layout.Justify;
import dev.sixik.unigui.api.layout.Overflow;
import dev.sixik.unigui.api.layout.PositionType;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.xml.XmlWidgetAnnotations;
import dev.sixik.unigui.impl.widget.WidgetBase;
import dev.sixik.unigui.widgets.containers.Box;
import dev.sixik.unigui.widgets.display.TextWidget;
import dev.sixik.unigui.widgets.display.TextureWidget;

final class BuiltInWidgetXmlSupport {
    private BuiltInWidgetXmlSupport() {
    }

    static <T extends Box> WidgetXmlType<T> box(WidgetXmlType<T> type) {
        return box(type, Box.class);
    }

    static <T extends Box> WidgetXmlType<T> box(WidgetXmlType<T> type, Class<?> widgetType) {
        return annotated(commonWidget(type), widgetType);
    }

    static <T extends TextWidget> WidgetXmlType<T> textWidget(WidgetXmlType<T> type) {
        return textWidget(type, TextWidget.class);
    }

    static <T extends TextWidget> WidgetXmlType<T> textWidget(WidgetXmlType<T> type, Class<?> widgetType) {
        return annotated(commonWidget(type), widgetType);
    }

    static <T extends TextureWidget> WidgetXmlType<T> textureWidget(WidgetXmlType<T> type) {
        return textureWidget(type, TextureWidget.class);
    }

    static <T extends TextureWidget> WidgetXmlType<T> textureWidget(WidgetXmlType<T> type, Class<?> widgetType) {
        return annotated(commonWidget(type), widgetType);
    }

    static <T extends WidgetBase> WidgetXmlType<T> commonWidget(WidgetXmlType<T> type) {
        return annotated(type
                .attribute("class", XmlValueParsers.STRING, (widget, value) -> {
                })
                .attribute("styleClass", XmlValueParsers.STRING, (widget, value) -> {
                })
                .attribute("enabled", XmlValueParsers.BOOLEAN, WidgetBase::enabled)
                .attribute("visible", XmlValueParsers.BOOLEAN, WidgetBase::visible)
                .attribute("visibility", XmlValueParsers.enumValue(Visibility.class), WidgetBase::visibility)
                .attribute("opacity", XmlValueParsers.FLOAT, WidgetBase::opacity)
                .attribute("rotation", XmlValueParsers.FLOAT, WidgetBase::rotationDegrees)
                .attribute("x", XmlValueParsers.FLOAT, (widget, value) -> widget.transform().position().set(value, widget.transform().position().y()))
                .attribute("y", XmlValueParsers.FLOAT, (widget, value) -> widget.transform().position().set(widget.transform().position().x(), value))
                .attribute("scale", XmlValueParsers.FLOAT, (widget, value) -> widget.transform().scale().set(value, value))
                .attribute("scaleX", XmlValueParsers.FLOAT, (widget, value) -> widget.transform().scale().set(value, widget.transform().scale().y()))
                .attribute("scaleY", XmlValueParsers.FLOAT, (widget, value) -> widget.transform().scale().set(widget.transform().scale().x(), value))
                .attribute("width", XmlValueParsers.SIZE, (widget, value) -> widget.layout(style -> style.width(value)))
                .attribute("height", XmlValueParsers.SIZE, (widget, value) -> widget.layout(style -> style.height(value)))
                .attribute("minWidth", XmlValueParsers.SIZE, (widget, value) -> widget.layout(style -> style.minWidth(value)))
                .attribute("minHeight", XmlValueParsers.SIZE, (widget, value) -> widget.layout(style -> style.minHeight(value)))
                .attribute("maxWidth", XmlValueParsers.SIZE, (widget, value) -> widget.layout(style -> style.maxWidth(value)))
                .attribute("maxHeight", XmlValueParsers.SIZE, (widget, value) -> widget.layout(style -> style.maxHeight(value)))
                .attribute("padding", XmlValueParsers.INSETS, (widget, value) -> widget.layout(style -> style.padding(value)))
                .attribute("margin", XmlValueParsers.INSETS, (widget, value) -> widget.layout(style -> style.margin(value)))
                .attribute("flexGrow", XmlValueParsers.FLOAT, (widget, value) -> widget.layout(style -> style.flexGrow(value)))
                .attribute("flexShrink", XmlValueParsers.FLOAT, (widget, value) -> widget.layout(style -> style.flexShrink(value)))
                .attribute("flexDirection", XmlValueParsers.enumValue(FlexDirection.class), (widget, value) -> widget.layout(style -> style.flexDirection(value)))
                .attribute("flexWrap", XmlValueParsers.enumValue(FlexWrap.class), (widget, value) -> widget.layout(style -> style.flexWrap(value)))
                .attribute("rowGap", XmlValueParsers.FLOAT, (widget, value) -> widget.layout(style -> style.rowGap(value)))
                .attribute("columnGap", XmlValueParsers.FLOAT, (widget, value) -> widget.layout(style -> style.columnGap(value)))
                .attribute("align", XmlValueParsers.enumValue(Alignment.class), (widget, value) -> widget.layout(style -> style.align(value, value)))
                .attribute("alignItems", XmlValueParsers.enumValue(Align.class), (widget, value) -> widget.layout(style -> style.alignItems(value)))
                .attribute("alignSelf", XmlValueParsers.enumValue(Align.class), (widget, value) -> widget.layout(style -> style.alignSelf(value)))
                .attribute("justifyContent", XmlValueParsers.enumValue(Justify.class), (widget, value) -> widget.layout(style -> style.justifyContent(value)))
                .attribute("overflow", XmlValueParsers.enumValue(Overflow.class), (widget, value) -> widget.layout(style -> style.overflow(value)))
                .attribute("overflowX", XmlValueParsers.enumValue(Overflow.class), (widget, value) -> widget.layout(style -> style.overflowX(value)))
                .attribute("overflowY", XmlValueParsers.enumValue(Overflow.class), (widget, value) -> widget.layout(style -> style.overflowY(value)))
                .attribute("position", XmlValueParsers.enumValue(PositionType.class), (widget, value) -> widget.layout(style -> style.position(value)))
                .attribute("left", XmlValueParsers.SIZE, (widget, value) -> widget.layout(style -> style.left(value)))
                .attribute("top", XmlValueParsers.SIZE, (widget, value) -> widget.layout(style -> style.top(value)))
                .attribute("right", XmlValueParsers.SIZE, (widget, value) -> widget.layout(style -> style.right(value)))
                .attribute("bottom", XmlValueParsers.SIZE, (widget, value) -> widget.layout(style -> style.bottom(value))),
                WidgetBase.class);
    }

    static <T extends WidgetBase> WidgetXmlType<T> commonWidget(WidgetXmlType<T> type, Class<?> widgetType) {
        return annotated(commonWidget(type), widgetType);
    }

    static <T extends dev.sixik.unigui.api.widget.Widget> WidgetXmlType<T> annotated(WidgetXmlType<T> type, Class<?> widgetType) {
        return XmlWidgetAnnotations.applyAnnotatedAttributes(type, widgetType);
    }

}
