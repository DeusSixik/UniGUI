package dev.sixik.unigui.impl.xml;

import dev.sixik.unigui.api.layout.Align;
import dev.sixik.unigui.api.layout.Alignment;
import dev.sixik.unigui.api.layout.FlexDirection;
import dev.sixik.unigui.api.layout.FlexWrap;
import dev.sixik.unigui.api.layout.Justify;
import dev.sixik.unigui.api.layout.Overflow;
import dev.sixik.unigui.api.layout.PositionType;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.render.ImageFit;
import dev.sixik.unigui.api.render.TextureFilter;
import dev.sixik.unigui.api.render.TextureHandle;
import dev.sixik.unigui.api.render.TextureOptions;
import dev.sixik.unigui.api.render.TextureWrap;
import dev.sixik.unigui.api.text.TextOverflowMode;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.xml.XmlAttributeDescriptor;
import dev.sixik.unigui.impl.widget.WidgetBase;
import dev.sixik.unigui.widgets.containers.Box;
import dev.sixik.unigui.widgets.display.TextWidget;
import dev.sixik.unigui.widgets.display.TextureWidget;

import java.util.function.UnaryOperator;

final class BuiltInWidgetXmlSupport {
    private BuiltInWidgetXmlSupport() {
    }

    static <T extends Box> WidgetXmlType<T> box(WidgetXmlType<T> type) {
        return commonWidget(type)
                .attribute("background", XmlValueParsers.COLOR, (widget, color) -> {
                    widget.background().set(color);
                    widget.backgroundVisible(true);
                })
                .attribute("backgroundVisible", XmlValueParsers.BOOLEAN, Box::backgroundVisible)
                .attribute("backgroundTexture", XmlValueParsers.TEXTURE, Box::backgroundTexture,
                        asset("backgroundTexture", "Background Texture", "",
                                "Texture resource id resolved through XmlWidgetOptions.textureResolver."))
                .attribute("backgroundTextureWidth", BuiltInWidgetXmlSupport::parsePositiveInt,
                        (widget, value) -> widget.backgroundTexture(resizeTexture(widget.backgroundTexture(), value, null)),
                        asset("backgroundTextureWidth", "Background Texture Width", "16",
                                "Source texture width used for contain and cover placement."))
                .attribute("backgroundTextureHeight", BuiltInWidgetXmlSupport::parsePositiveInt,
                        (widget, value) -> widget.backgroundTexture(resizeTexture(widget.backgroundTexture(), null, value)),
                        asset("backgroundTextureHeight", "Background Texture Height", "16",
                                "Source texture height used for contain and cover placement."))
                .attribute("backgroundTextureFit", XmlValueParsers.enumValue(ImageFit.class), Box::backgroundTextureFit,
                        asset("backgroundTextureFit", "Background Texture Fit", "stretch",
                                "Placement mode for the background texture."))
                .attribute("backgroundTextureTint", XmlValueParsers.COLOR, (widget, color) -> widget.backgroundTextureTint().set(color),
                        asset("backgroundTextureTint", "Background Texture Tint", "#FFFFFFFF",
                                "Tint color applied while drawing the background texture."))
                .attribute("backgroundTextureSource", BuiltInWidgetXmlSupport::parseRect,
                        (widget, source) -> widget.backgroundTextureSource(source.x(), source.y(), source.width(), source.height()),
                        asset("backgroundTextureSource", "Background Texture Source", "0 0 1 1",
                                "Normalized UV source rectangle: u v width height."))
                .attribute("backgroundTextureSampling", XmlValueParsers.enumValue(TextureFilter.class),
                        (widget, filter) -> widget.backgroundTexture(textureWithOptions(widget.backgroundTexture(), options -> options.sampling(filter))),
                        asset("backgroundTextureSampling", "Background Texture Sampling", "nearest",
                                "Texture filtering mode used by the renderer backend."))
                .attribute("backgroundTextureWrap", XmlValueParsers.enumValue(TextureWrap.class),
                        (widget, wrap) -> widget.backgroundTexture(textureWithOptions(widget.backgroundTexture(), options -> options.wrap(wrap))),
                        asset("backgroundTextureWrap", "Background Texture Wrap", "clamp-to-edge",
                                "Texture coordinate wrap mode used by the renderer backend."))
                .attribute("backgroundTextureMipmaps", XmlValueParsers.BOOLEAN,
                        (widget, mipmaps) -> widget.backgroundTexture(textureWithOptions(widget.backgroundTexture(), options -> options.mipmaps(mipmaps))),
                        asset("backgroundTextureMipmaps", "Background Texture Mipmaps", "false",
                                "Whether the texture should use mipmapped sampling."))
                .attribute("backgroundTexturePremultipliedAlpha", XmlValueParsers.BOOLEAN,
                        (widget, premultiplied) -> widget.backgroundTexture(textureWithOptions(widget.backgroundTexture(), options -> options.premultipliedAlpha(premultiplied))),
                        asset("backgroundTexturePremultipliedAlpha", "Background Texture Premultiplied Alpha", "false",
                                "Whether the texture color data already uses premultiplied alpha."))
                .attribute("border", XmlValueParsers.COLOR, (widget, color) -> {
                    widget.borderColor().set(color);
                    widget.borderVisible(true);
                })
                .attribute("borderColor", XmlValueParsers.COLOR, (widget, color) -> widget.borderColor().set(color))
                .attribute("borderVisible", XmlValueParsers.BOOLEAN, Box::borderVisible)
                .attribute("borderWidth", XmlValueParsers.FLOAT, Box::borderWidth)
                .attribute("radius", XmlValueParsers.FLOAT, Box::radius)
                .attribute("themeEnabled", XmlValueParsers.BOOLEAN, Box::themeEnabled);
    }

    static <T extends TextWidget> WidgetXmlType<T> textWidget(WidgetXmlType<T> type) {
        return commonWidget(type)
                .attribute("text", XmlValueParsers.STRING, TextWidget::text)
                .attribute("color", XmlValueParsers.COLOR, (widget, color) -> widget.color().set(color))
                .attribute("wrap", XmlValueParsers.BOOLEAN, TextWidget::wrap)
                .attribute("overflowMode", XmlValueParsers.enumValue(TextOverflowMode.class), TextWidget::overflowMode)
                .attribute("marqueeSpeed", XmlValueParsers.FLOAT, TextWidget::marqueeSpeed)
                .attribute("marqueeGap", XmlValueParsers.FLOAT, TextWidget::marqueeGap);
    }

    static <T extends TextureWidget> WidgetXmlType<T> textureWidget(WidgetXmlType<T> type) {
        return commonWidget(type)
                .attribute("texture", XmlValueParsers.TEXTURE, TextureWidget::texture,
                        asset("texture", "Texture", "",
                                "Texture resource id resolved through XmlWidgetOptions.textureResolver."))
                .attribute("textureWidth", BuiltInWidgetXmlSupport::parsePositiveInt,
                        (widget, value) -> widget.texture(resizeTexture(widget.texture(), value, null)),
                        asset("textureWidth", "Texture Width", "16",
                                "Source texture width used for contain and cover placement."))
                .attribute("textureHeight", BuiltInWidgetXmlSupport::parsePositiveInt,
                        (widget, value) -> widget.texture(resizeTexture(widget.texture(), null, value)),
                        asset("textureHeight", "Texture Height", "16",
                                "Source texture height used for contain and cover placement."))
                .attribute("fit", XmlValueParsers.enumValue(ImageFit.class), TextureWidget::fit,
                        asset("fit", "Fit", "stretch", "Placement mode for the texture."))
                .attribute("tint", XmlValueParsers.COLOR, (widget, color) -> widget.tint().set(color),
                        asset("tint", "Tint", "#FFFFFFFF", "Tint color applied while drawing the texture."))
                .attribute("source", BuiltInWidgetXmlSupport::parseRect,
                        (widget, source) -> widget.source(source.x(), source.y(), source.width(), source.height()),
                        asset("source", "Source", "0 0 1 1", "Normalized UV source rectangle: u v width height."))
                .attribute("radius", XmlValueParsers.FLOAT, TextureWidget::radius)
                .attribute("textureSampling", XmlValueParsers.enumValue(TextureFilter.class),
                        (widget, filter) -> widget.texture(textureWithOptions(widget.texture(), options -> options.sampling(filter))),
                        asset("textureSampling", "Texture Sampling", "nearest", "Texture filtering mode used by the renderer backend."))
                .attribute("textureWrap", XmlValueParsers.enumValue(TextureWrap.class),
                        (widget, wrap) -> widget.texture(textureWithOptions(widget.texture(), options -> options.wrap(wrap))),
                        asset("textureWrap", "Texture Wrap", "clamp-to-edge", "Texture coordinate wrap mode used by the renderer backend."))
                .attribute("textureMipmaps", XmlValueParsers.BOOLEAN,
                        (widget, mipmaps) -> widget.texture(textureWithOptions(widget.texture(), options -> options.mipmaps(mipmaps))),
                        asset("textureMipmaps", "Texture Mipmaps", "false", "Whether the texture should use mipmapped sampling."))
                .attribute("texturePremultipliedAlpha", XmlValueParsers.BOOLEAN,
                        (widget, premultiplied) -> widget.texture(textureWithOptions(widget.texture(), options -> options.premultipliedAlpha(premultiplied))),
                        asset("texturePremultipliedAlpha", "Texture Premultiplied Alpha", "false",
                                "Whether the texture color data already uses premultiplied alpha."));
    }

    static int parsePositiveInt(String value) {
        int parsed = XmlValueParsers.INT.parse(value);
        if (parsed <= 0) {
            throw new IllegalArgumentException("Expected positive integer, got: " + value);
        }
        return parsed;
    }

    static MutableRect parseRect(String value) {
        if (value == null) throw new IllegalArgumentException("Rect value must not be null");
        String[] parts = value.trim().split("\\s+");
        if (parts.length != 4) {
            throw new IllegalArgumentException("Expected 4 rect values: x y width height, got: " + value);
        }
        return new MutableRect(
                Float.parseFloat(parts[0]),
                Float.parseFloat(parts[1]),
                Float.parseFloat(parts[2]),
                Float.parseFloat(parts[3]));
    }

    static TextureHandle resizeTexture(TextureHandle texture, Integer width, Integer height) {
        if (texture == null) return null;
        int nextWidth = width == null ? texture.width() : Math.max(1, width);
        int nextHeight = height == null ? texture.height() : Math.max(1, height);
        if (texture.width() == nextWidth && texture.height() == nextHeight) return texture;
        return copyTexture(texture, nextWidth, nextHeight, texture.options());
    }

    static TextureHandle textureWithOptions(TextureHandle texture, UnaryOperator<TextureOptions> mutation) {
        if (texture == null) return null;
        TextureOptions options = texture.options() == null ? TextureOptions.defaults() : texture.options();
        TextureOptions nextOptions = mutation == null ? options : mutation.apply(options);
        if (options.equals(nextOptions)) return texture;
        return copyTexture(texture, texture.width(), texture.height(), nextOptions);
    }

    static <T extends WidgetBase> WidgetXmlType<T> commonWidget(WidgetXmlType<T> type) {
        return type
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
                .attribute("bottom", XmlValueParsers.SIZE, (widget, value) -> widget.layout(style -> style.bottom(value)));
    }

    private static TextureHandle copyTexture(TextureHandle texture, int width, int height, TextureOptions options) {
        return XmlValueParsers.resolveTexture(texture.id(), width, height, options);
    }

    private static XmlAttributeDescriptor asset(String name, String displayName, String defaultValue, String description) {
        return XmlAttributeDescriptor.of(name)
                .displayName(displayName)
                .category("Assets")
                .defaultValue(defaultValue)
                .description(description);
    }
}
