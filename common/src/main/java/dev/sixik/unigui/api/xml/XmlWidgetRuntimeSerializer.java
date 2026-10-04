package dev.sixik.unigui.api.xml;

import dev.sixik.unigui.api.layout.Align;
import dev.sixik.unigui.api.layout.AlignContent;
import dev.sixik.unigui.api.layout.AutoMargins;
import dev.sixik.unigui.api.layout.EdgeInsets;
import dev.sixik.unigui.api.layout.FlexDirection;
import dev.sixik.unigui.api.layout.FlexWrap;
import dev.sixik.unigui.api.layout.GridAutoFlow;
import dev.sixik.unigui.api.layout.GridTrack;
import dev.sixik.unigui.api.layout.Justify;
import dev.sixik.unigui.api.layout.LayoutStyle;
import dev.sixik.unigui.api.layout.SizeValue;
import dev.sixik.unigui.widgets.containers.FlexBox;
import dev.sixik.unigui.widgets.containers.GridBox;
import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.render.TextureHandle;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.impl.widget.WidgetBase;
import dev.sixik.unigui.widgets.containers.Box;
import dev.sixik.unigui.widgets.containers.HBox;
import dev.sixik.unigui.widgets.containers.ScrollView;
import dev.sixik.unigui.widgets.containers.StackPanel;
import dev.sixik.unigui.widgets.containers.SurfaceWidget;
import dev.sixik.unigui.widgets.containers.VBox;
import dev.sixik.unigui.widgets.containers.WrapPanel;
import dev.sixik.unigui.widgets.display.ImageView;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.widgets.display.TextBlock;
import dev.sixik.unigui.widgets.display.TextWidget;
import dev.sixik.unigui.widgets.display.TextureWidget;
import dev.sixik.unigui.widgets.feedback.ProgressBar;
import dev.sixik.unigui.widgets.interaction.Button;
import dev.sixik.unigui.widgets.interaction.Slider;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Экспортер live-дерева виджетов обратно в исходный XML по принципу best-effort.
 *
 * <p>Сериализатор восстанавливает полезную структуру для редактора, но не пытается
 * гарантировать полную обратимость runtime-состояния. Например, callback-и, command bindings,
 * runtime-only handles и пользовательские widget state не всегда имеют XML-представление.</p>
 *
 * <p>Неполно поддержанные типы всё равно экспортируются по class simple name, а результат получает
 * diagnostics. Это позволяет сохранить структуру дерева и вручную доработать XML в редакторе.</p>
 */
public final class XmlWidgetRuntimeSerializer {
    private XmlWidgetRuntimeSerializer() {
    }

    /**
     * Создаёт snapshot runtime widget tree как XML document result.
     *
     * @param root root widget live-дерева; не может быть {@code null}
     * @return document result с best-effort XML и diagnostics по неподдержанным типам
     */
    public static XmlWidgetDocumentResult snapshot(Widget root) {
        if (root == null) throw new IllegalArgumentException("XML runtime snapshot root must not be null");
        ArrayList<XmlWidgetDiagnostic> diagnostics = new ArrayList<>();
        XmlWidgetElement element = snapshotWidget(root, diagnostics);
        return new XmlWidgetDocumentResult(XmlWidgetDocument.of(element), diagnostics);
    }

    /**
     * Создаёт XML-документ из runtime widget tree и игнорирует diagnostics.
     *
     * <p>Если нужно показать пользователю предупреждения о неподдержанных типах, используйте
     * {@link #snapshot(Widget)}.</p>
     *
     * @param root root widget live-дерева
     * @return XML document snapshot
     */
    public static XmlWidgetDocument document(Widget root) {
        return snapshot(root).document();
    }

    private static XmlWidgetElement snapshotWidget(Widget widget, List<XmlWidgetDiagnostic> diagnostics) {
        String xmlName = xmlName(widget);
        XmlWidgetElement element = new XmlWidgetElement(xmlName);
        if (!supported(widget)) {
            diagnostics.add(new XmlWidgetDiagnostic("Runtime widget type '" + widget.getClass().getName()
                    + "' does not have a complete XML snapshot exporter."));
        }

        writeCommon(widget, element);
        writeSpecific(widget, element);

        for (Widget child : exportChildren(widget)) {
            element.addElement(snapshotWidget(child, diagnostics));
        }
        return element;
    }

    private static String xmlName(Widget widget) {
        if (widget instanceof VBox) return "VBox";
        if (widget instanceof HBox) return "HBox";
        if (widget instanceof FlexBox) return "FlexBox";
        if (widget instanceof GridBox) return "GridBox";
        if (widget instanceof WrapPanel) return "WrapPanel";
        if (widget instanceof ScrollView) return "ScrollView";
        if (widget instanceof StackPanel) return "StackPanel";
        if (widget instanceof Button) return "Button";
        if (widget instanceof Slider) return "Slider";
        if (widget instanceof ProgressBar) return "ProgressBar";
        if (widget instanceof Label) return "Label";
        if (widget instanceof TextBlock) return "TextBlock";
        if (widget instanceof TextWidget) return "TextWidget";
        if (widget instanceof ImageView) return "ImageView";
        if (widget instanceof TextureWidget) return "TextureWidget";
        if (widget instanceof Box) return "Box";
        return widget.getClass().getSimpleName().isBlank() ? "Widget" : widget.getClass().getSimpleName();
    }

    private static boolean supported(Widget widget) {
        return widget instanceof VBox
                || widget instanceof HBox
                || widget instanceof FlexBox
                || widget instanceof GridBox
                || widget instanceof WrapPanel
                || widget instanceof ScrollView
                || widget instanceof StackPanel
                || widget instanceof Button
                || widget instanceof Slider
                || widget instanceof ProgressBar
                || widget instanceof Label
                || widget instanceof TextBlock
                || widget instanceof TextWidget
                || widget instanceof ImageView
                || widget instanceof TextureWidget
                || widget instanceof Box;
    }

    private static List<Widget> exportChildren(Widget widget) {
        if (widget instanceof ScrollView scroll) {
            return scroll.content() == null ? List.of() : List.of(scroll.content());
        }
        return widget.children();
    }

    private static void writeCommon(Widget widget, XmlWidgetElement element) {
        if (!widget.id().isBlank()) element.attribute("id", widget.id());
        if (!widget.enabled()) element.attribute("enabled", "false");
        if (widget.visibility() != Visibility.VISIBLE) element.attribute("visibility", enumValue(widget.visibility()));

        if (widget instanceof WidgetBase base) {
            writeFloat(element, "opacity", base.opacity(), 1.0f);
            writeFloat(element, "rotation", base.transform().rotationDegrees(), 0.0f);
            writeFloat(element, "x", base.transform().position().x(), 0.0f);
            writeFloat(element, "y", base.transform().position().y(), 0.0f);
            if (base.transform().scale().x() == base.transform().scale().y()) {
                writeFloat(element, "scale", base.transform().scale().x(), 1.0f);
            } else {
                writeFloat(element, "scaleX", base.transform().scale().x(), 1.0f);
                writeFloat(element, "scaleY", base.transform().scale().y(), 1.0f);
            }
            writeLayout(base.layoutStyle(), element);
        }
    }

    private static void writeLayout(LayoutStyle style, XmlWidgetElement element) {
        if (style == null) return;
        writeSize(element, "width", style.width());
        writeSize(element, "height", style.height());
        writeSize(element, "minWidth", style.minWidth(), SizeValue.px(0.0f));
        writeSize(element, "minHeight", style.minHeight(), SizeValue.px(0.0f));
        writeSize(element, "maxWidth", style.maxWidth());
        writeSize(element, "maxHeight", style.maxHeight());
        writeInsets(element, "padding", style.padding());
        writeInsets(element, "margin", style.margin());
        if (!style.marginAuto().equals(AutoMargins.NONE)) {
            element.attribute("marginAuto", marginAutoValue(style.marginAuto()));
        }
        writeFloat(element, "flexGrow", style.flexGrow(), 0.0f);
        writeFloat(element, "flexShrink", style.flexShrink(), 0.0f);
        writeSize(element, "flexBasis", style.flexBasis());
        if (style.order() != 0) {
            element.attribute("order", String.valueOf(style.order()));
        }
        if (Float.isFinite(style.aspectRatio()) && style.aspectRatio() > 0.0f) {
            element.attribute("aspectRatio", String.valueOf(style.aspectRatio()));
        }
        writeGridTracks(element, "gridTemplateColumns", style.gridTemplateColumns());
        writeGridTracks(element, "gridTemplateRows", style.gridTemplateRows());
        if (style.gridAutoFlow() != GridAutoFlow.ROW) {
            element.attribute("gridAutoFlow", enumValue(style.gridAutoFlow()));
        }
        writeGridTracks(element, "gridAutoColumns", style.gridAutoColumns());
        writeGridTracks(element, "gridAutoRows", style.gridAutoRows());
        if (style.gridColumnStart() > 0) {
            element.attribute("gridColumn", String.valueOf(style.gridColumnStart()));
        }
        if (style.gridColumnSpan() != 1) {
            element.attribute("gridColumnSpan", String.valueOf(style.gridColumnSpan()));
        }
        if (style.gridRowStart() > 0) {
            element.attribute("gridRow", String.valueOf(style.gridRowStart()));
        }
        if (style.gridRowSpan() != 1) {
            element.attribute("gridRowSpan", String.valueOf(style.gridRowSpan()));
        }
        if (style.zIndex() != 0) {
            element.attribute("zIndex", String.valueOf(style.zIndex()));
        }
        if (style.flexDirection() != FlexDirection.COLUMN) {
            element.attribute("flexDirection", enumValue(style.flexDirection()));
        }
        if (style.flexWrap() != FlexWrap.NOWRAP) {
            element.attribute("flexWrap", enumValue(style.flexWrap()));
        }
        writeFloat(element, "rowGap", style.rowGap(), 0.0f);
        writeFloat(element, "columnGap", style.columnGap(), 0.0f);
        if (style.alignItems() != Align.STRETCH) {
            element.attribute("alignItems", enumValue(style.alignItems()));
        }
        if (style.alignSelf() != Align.AUTO) {
            element.attribute("alignSelf", enumValue(style.alignSelf()));
        }
        if (style.alignContent() != AlignContent.STRETCH) {
            element.attribute("alignContent", enumValue(style.alignContent()));
        }
        if (style.justifyContent() != Justify.START) {
            element.attribute("justifyContent", enumValue(style.justifyContent()));
        }
        if (!style.overflowX().name().equals("VISIBLE")) element.attribute("overflowX", enumValue(style.overflowX()));
        if (!style.overflowY().name().equals("VISIBLE")) element.attribute("overflowY", enumValue(style.overflowY()));
        if (!style.position().name().equals("RELATIVE")) element.attribute("position", enumValue(style.position()));
        writeSize(element, "left", style.left());
        writeSize(element, "top", style.top());
        writeSize(element, "right", style.right());
        writeSize(element, "bottom", style.bottom());
    }

    private static void writeSpecific(Widget widget, XmlWidgetElement element) {
        if (widget instanceof Button button) {
            if (!button.text().isEmpty()) element.attribute("text", button.text());
        } else if (widget instanceof TextWidget textWidget) {
            if (!textWidget.text().isEmpty()) element.attribute("text", textWidget.text());
            if (!textWidget.wrap()) element.attribute("wrap", "false");
            if (textWidget.overflowMode() != dev.sixik.unigui.api.text.TextOverflowMode.VISIBLE) {
                element.attribute("overflowMode", enumValue(textWidget.overflowMode()));
            }
        }

        if (widget instanceof Slider slider) {
            writeFloat(element, "min", slider.min(), 0.0f);
            writeFloat(element, "max", slider.max(), 1.0f);
            writeFloat(element, "value", slider.value(), 0.0f);
            writeFloat(element, "step", slider.step(), 0.0f);
        } else if (widget instanceof ProgressBar progressBar) {
            writeFloat(element, "min", progressBar.min(), 0.0f);
            writeFloat(element, "max", progressBar.max(), 1.0f);
            writeFloat(element, "value", progressBar.value(), 0.0f);
            if (progressBar.indeterminate()) element.attribute("indeterminate", "true");
        }

        if (widget instanceof TextureWidget textureWidget) {
            writeTexture(element, "texture", "textureWidth", "textureHeight", textureWidget.texture());
            if (textureWidget.fit() != dev.sixik.unigui.api.render.ImageFit.STRETCH) {
                element.attribute("fit", enumValue(textureWidget.fit()));
            }
            writeFloat(element, "radius", textureWidget.radius(), 0.0f);
            writeRect(element, "source", textureWidget.source(), 0.0f, 0.0f, 1.0f, 1.0f);
        }

        if (widget instanceof SurfaceWidget<?> surface) {
            if (surface.backgroundKind() != null) {
                element.attribute("backgroundKind", enumValue(surface.backgroundKind()));
            }
            if (surface.backgroundVisible()) element.attribute("background", color(surface.background()));
            if (surface.borderVisible()) {
                element.attribute("border", color(surface.borderColor()));
                writeFloat(element, "borderWidth", surface.borderWidth(), 1.0f);
            }
            writeFloat(element, "radius", surface.radius(), 0.0f);
            writeTexture(element, "backgroundTexture", "backgroundTextureWidth", "backgroundTextureHeight", surface.backgroundTexture());
            if (surface.backgroundTextureFit() != dev.sixik.unigui.api.render.ImageFit.STRETCH) {
                element.attribute("backgroundTextureFit", enumValue(surface.backgroundTextureFit()));
            }
            writeRect(element, "backgroundTextureSource", surface.backgroundTextureSource(), 0.0f, 0.0f, 1.0f, 1.0f);
            if (surface.backgroundShader() != null) {
                element.attribute("backgroundShader", surface.backgroundShader().id());
            }
        }

        if (widget instanceof dev.sixik.unigui.widgets.containers.LinearBox linearBox) {
            writeFloat(element, "spacing", linearBox.spacing(), 0.0f);
        } else if (widget instanceof WrapPanel wrapPanel) {
            writeFloat(element, "spacing", wrapPanel.spacing(), 0.0f);
            writeFloat(element, "lineSpacing", wrapPanel.lineSpacing(), 0.0f);
        } else if (widget instanceof dev.sixik.unigui.widgets.containers.GridBox gridBox) {
            if (gridBox.columns() != 1) {
                element.attribute("columns", String.valueOf(gridBox.columns()));
            }
            writeFloat(element, "horizontalSpacing", gridBox.horizontalSpacing(), 0.0f);
            writeFloat(element, "verticalSpacing", gridBox.verticalSpacing(), 0.0f);
        } else if (widget instanceof ScrollView scrollView) {
            writeFloat(element, "scrollStep", scrollView.scrollStep(), 16.0f);
            writeFloat(element, "scrollbarGap", scrollView.scrollbarGap(), dev.sixik.unigui.widgets.interaction.ScrollBar.DEFAULT_GAP);
        }
    }

    private static void writeTexture(XmlWidgetElement element, String idName, String widthName, String heightName, TextureHandle texture) {
        if (texture == null || texture.id() == null || texture.id().isBlank()) return;
        element.attribute(idName, texture.id());
        element.attribute(widthName, format(texture.width()));
        element.attribute(heightName, format(texture.height()));
    }

    private static void writeRect(XmlWidgetElement element, String name, RectView rect,
                                  float defaultX, float defaultY, float defaultWidth, float defaultHeight) {
        if (rect == null) return;
        if (near(rect.x(), defaultX) && near(rect.y(), defaultY)
                && near(rect.width(), defaultWidth) && near(rect.height(), defaultHeight)) return;
        element.attribute(name, format(rect.x()) + " " + format(rect.y()) + " "
                + format(rect.width()) + " " + format(rect.height()));
    }

    private static void writeSize(XmlWidgetElement element, String name, SizeValue value) {
        writeSize(element, name, value, SizeValue.auto());
    }

    private static void writeSize(XmlWidgetElement element, String name, SizeValue value, SizeValue defaultValue) {
        SizeValue normalized = value == null ? SizeValue.auto() : value;
        SizeValue defaulted = defaultValue == null ? SizeValue.auto() : defaultValue;
        if (normalized.equals(defaulted) || normalized.isAuto()) return;
        if (normalized.isContent()) {
            element.attribute(name, "content");
            return;
        }
        if (normalized.isMinContent()) {
            element.attribute(name, "min-content");
            return;
        }
        if (normalized.isMaxContent()) {
            element.attribute(name, "max-content");
            return;
        }
        if (normalized.isFitContent()) {
            element.attribute(name, "fit-content(" + format(normalized.value()) + "px)");
            return;
        }
        element.attribute(name, normalized.isPercent() ? format(normalized.value()) + "%" : format(normalized.value()));
    }

    private static void writeInsets(XmlWidgetElement element, String name, EdgeInsets insets) {
        if (insets == null) return;
        if (near(insets.top(), 0.0f) && near(insets.right(), 0.0f)
                && near(insets.bottom(), 0.0f) && near(insets.left(), 0.0f)) return;
        element.attribute(name, format(insets.top()) + " " + format(insets.right()) + " "
                + format(insets.bottom()) + " " + format(insets.left()));
    }

    private static void writeFloat(XmlWidgetElement element, String name, float value, float defaultValue) {
        if (!Float.isFinite(value) || near(value, defaultValue)) return;
        element.attribute(name, format(value));
    }

    private static String enumValue(Enum<?> value) {
        return value.name().toLowerCase(Locale.ROOT).replace('_', '-');
    }

    private static void writeGridTracks(XmlWidgetElement element, String name, java.util.List<GridTrack> tracks) {
        if (tracks == null || tracks.isEmpty()) {
            return;
        }
        StringBuilder builder = new StringBuilder();
        for (GridTrack track : tracks) {
            if (track == null) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(formatTrack(track));
        }
        if (builder.length() > 0) {
            element.attribute(name, builder.toString());
        }
    }

    private static String formatTrack(GridTrack track) {
        if (track instanceof GridTrack.Fixed fixed) {
            return format(fixed.pixels());
        }
        if (track instanceof GridTrack.Percent percent) {
            return format(percent.percent()) + "%";
        }
        if (track instanceof GridTrack.Flex flex) {
            return format(flex.weight()) + "fr";
        }
        if (track instanceof GridTrack.MinMax minmax) {
            return "minmax(" + formatTrack(minmax.min()) + ", " + formatTrack(minmax.max()) + ")";
        }
        return "auto";
    }

    private static String marginAutoValue(AutoMargins margins) {
        if (margins.equals(AutoMargins.ALL)) return "all";
        if (margins.equals(AutoMargins.HORIZONTAL)) return "horizontal";
        if (margins.equals(AutoMargins.VERTICAL)) return "vertical";
        StringBuilder builder = new StringBuilder();
        if (margins.left()) builder.append("left ");
        if (margins.top()) builder.append("top ");
        if (margins.right()) builder.append("right ");
        if (margins.bottom()) builder.append("bottom ");
        return builder.toString().trim();
    }

    private static String color(ColorView color) {
        ColorView safe = color == null ? new dev.sixik.unigui.api.math.MutableColor() : color;
        return "#" + hex(safe.r()) + hex(safe.g()) + hex(safe.b()) + hex(safe.a());
    }

    private static String hex(float value) {
        int channel = Math.max(0, Math.min(255, Math.round(value * 255.0f)));
        String text = Integer.toHexString(channel).toUpperCase(Locale.ROOT);
        return text.length() == 1 ? "0" + text : text;
    }

    private static String format(float value) {
        float rounded = Math.round(value * 1000.0f) / 1000.0f;
        return BigDecimal.valueOf(rounded).stripTrailingZeros().toPlainString();
    }

    private static boolean near(float left, float right) {
        return Math.abs(left - right) < 0.0005f;
    }
}
