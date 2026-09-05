package dev.sixik.unigui.impl.xml;

import dev.sixik.unigui.api.layout.Align;
import dev.sixik.unigui.api.layout.Alignment;
import dev.sixik.unigui.api.layout.FlexDirection;
import dev.sixik.unigui.api.layout.FlexWrap;
import dev.sixik.unigui.api.layout.Justify;
import dev.sixik.unigui.api.layout.Overflow;
import dev.sixik.unigui.api.layout.PositionType;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.xml.XmlAttributeDescriptor;
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
                }, commonAttribute("class"))
                .attribute("styleClass", XmlValueParsers.STRING, (widget, value) -> {
                }, commonAttribute("styleClass"))
                .attribute("enabled", XmlValueParsers.BOOLEAN, WidgetBase::enabled, commonAttribute("enabled"))
                .attribute("visible", XmlValueParsers.BOOLEAN, WidgetBase::visible, commonAttribute("visible"))
                .attribute("visibility", XmlValueParsers.enumValue(Visibility.class), WidgetBase::visibility, commonAttribute("visibility"))
                .attribute("opacity", XmlValueParsers.FLOAT, WidgetBase::opacity, commonAttribute("opacity"))
                .attribute("rotation", XmlValueParsers.FLOAT, WidgetBase::rotationDegrees, commonAttribute("rotation"))
                .attribute("x", XmlValueParsers.FLOAT, (widget, value) -> widget.transform().position().set(value, widget.transform().position().y()), commonAttribute("x"))
                .attribute("y", XmlValueParsers.FLOAT, (widget, value) -> widget.transform().position().set(widget.transform().position().x(), value), commonAttribute("y"))
                .attribute("scale", XmlValueParsers.FLOAT, (widget, value) -> widget.transform().scale().set(value, value), commonAttribute("scale"))
                .attribute("scaleX", XmlValueParsers.FLOAT, (widget, value) -> widget.transform().scale().set(value, widget.transform().scale().y()), commonAttribute("scaleX"))
                .attribute("scaleY", XmlValueParsers.FLOAT, (widget, value) -> widget.transform().scale().set(widget.transform().scale().x(), value), commonAttribute("scaleY"))
                .attribute("width", XmlValueParsers.SIZE, (widget, value) -> widget.layout(style -> style.width(value)), commonAttribute("width"))
                .attribute("height", XmlValueParsers.SIZE, (widget, value) -> widget.layout(style -> style.height(value)), commonAttribute("height"))
                .attribute("minWidth", XmlValueParsers.SIZE, (widget, value) -> widget.layout(style -> style.minWidth(value)), commonAttribute("minWidth"))
                .attribute("minHeight", XmlValueParsers.SIZE, (widget, value) -> widget.layout(style -> style.minHeight(value)), commonAttribute("minHeight"))
                .attribute("maxWidth", XmlValueParsers.SIZE, (widget, value) -> widget.layout(style -> style.maxWidth(value)), commonAttribute("maxWidth"))
                .attribute("maxHeight", XmlValueParsers.SIZE, (widget, value) -> widget.layout(style -> style.maxHeight(value)), commonAttribute("maxHeight"))
                .attribute("padding", XmlValueParsers.INSETS, (widget, value) -> widget.layout(style -> style.padding(value)), commonAttribute("padding"))
                .attribute("margin", XmlValueParsers.INSETS, (widget, value) -> widget.layout(style -> style.margin(value)), commonAttribute("margin"))
                .attribute("flexGrow", XmlValueParsers.FLOAT, (widget, value) -> widget.layout(style -> style.flexGrow(value)), commonAttribute("flexGrow"))
                .attribute("flexShrink", XmlValueParsers.FLOAT, (widget, value) -> widget.layout(style -> style.flexShrink(value)), commonAttribute("flexShrink"))
                .attribute("flexBasis", XmlValueParsers.SIZE, (widget, value) -> widget.layout(style -> style.flexBasis(value)), commonAttribute("flexBasis"))
                .attribute("flexDirection", XmlValueParsers.enumValue(FlexDirection.class), (widget, value) -> widget.layout(style -> style.flexDirection(value)), commonAttribute("flexDirection"))
                .attribute("flexWrap", XmlValueParsers.enumValue(FlexWrap.class), (widget, value) -> widget.layout(style -> style.flexWrap(value)), commonAttribute("flexWrap"))
                .attribute("rowGap", XmlValueParsers.FLOAT, (widget, value) -> widget.layout(style -> style.rowGap(value)), commonAttribute("rowGap"))
                .attribute("columnGap", XmlValueParsers.FLOAT, (widget, value) -> widget.layout(style -> style.columnGap(value)), commonAttribute("columnGap"))
                .attribute("align", XmlValueParsers.enumValue(Alignment.class), (widget, value) -> widget.layout(style -> style.align(value, value)), commonAttribute("align"))
                .attribute("alignItems", XmlValueParsers.enumValue(Align.class), (widget, value) -> widget.layout(style -> style.alignItems(value)), commonAttribute("alignItems"))
                .attribute("alignSelf", XmlValueParsers.enumValue(Align.class), (widget, value) -> widget.layout(style -> style.alignSelf(value)), commonAttribute("alignSelf"))
                .attribute("justifyContent", XmlValueParsers.enumValue(Justify.class), (widget, value) -> widget.layout(style -> style.justifyContent(value)), commonAttribute("justifyContent"))
                .attribute("overflow", XmlValueParsers.enumValue(Overflow.class), (widget, value) -> widget.layout(style -> style.overflow(value)), commonAttribute("overflow"))
                .attribute("overflowX", XmlValueParsers.enumValue(Overflow.class), (widget, value) -> widget.layout(style -> style.overflowX(value)), commonAttribute("overflowX"))
                .attribute("overflowY", XmlValueParsers.enumValue(Overflow.class), (widget, value) -> widget.layout(style -> style.overflowY(value)), commonAttribute("overflowY"))
                .attribute("position", XmlValueParsers.enumValue(PositionType.class), (widget, value) -> widget.layout(style -> style.position(value)), commonAttribute("position"))
                .attribute("left", XmlValueParsers.SIZE, (widget, value) -> widget.layout(style -> style.left(value)), commonAttribute("left"))
                .attribute("top", XmlValueParsers.SIZE, (widget, value) -> widget.layout(style -> style.top(value)), commonAttribute("top"))
                .attribute("right", XmlValueParsers.SIZE, (widget, value) -> widget.layout(style -> style.right(value)), commonAttribute("right"))
                .attribute("bottom", XmlValueParsers.SIZE, (widget, value) -> widget.layout(style -> style.bottom(value)), commonAttribute("bottom")),
                WidgetBase.class);
    }

    static <T extends WidgetBase> WidgetXmlType<T> commonWidget(WidgetXmlType<T> type, Class<?> widgetType) {
        return annotated(commonWidget(type), widgetType);
    }

    static <T extends dev.sixik.unigui.api.widget.Widget> WidgetXmlType<T> annotated(WidgetXmlType<T> type, Class<?> widgetType) {
        return XmlWidgetAnnotations.applyAnnotatedAttributes(type, widgetType);
    }

    private static XmlAttributeDescriptor commonAttribute(String name) {
        return XmlWidgetAnnotations.commonAttributeDescriptor(name).orElse(XmlAttributeDescriptor.of(name));
    }

}
