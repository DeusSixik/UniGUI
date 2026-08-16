package dev.sixik.unigui.api.xml;

import dev.sixik.unigui.api.layout.EdgeInsets;
import dev.sixik.unigui.api.layout.LayoutStyle;
import dev.sixik.unigui.api.layout.SizeValue;
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
        writeFloat(element, "flexGrow", style.flexGrow(), 0.0f);
        writeFloat(element, "flexShrink", style.flexShrink(), 1.0f);
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

        if (widget instanceof Box box) {
            if (box.backgroundVisible()) element.attribute("background", color(box.background()));
            if (box.borderVisible()) {
                element.attribute("border", color(box.borderColor()));
                writeFloat(element, "borderWidth", box.borderWidth(), 1.0f);
            }
            writeFloat(element, "radius", box.radius(), 0.0f);
            writeTexture(element, "backgroundTexture", "backgroundTextureWidth", "backgroundTextureHeight", box.backgroundTexture());
            if (box.backgroundTextureFit() != dev.sixik.unigui.api.render.ImageFit.STRETCH) {
                element.attribute("backgroundTextureFit", enumValue(box.backgroundTextureFit()));
            }
            writeRect(element, "backgroundTextureSource", box.backgroundTextureSource(), 0.0f, 0.0f, 1.0f, 1.0f);
        }

        if (widget instanceof dev.sixik.unigui.widgets.containers.LinearBox linearBox) {
            writeFloat(element, "spacing", linearBox.spacing(), 0.0f);
        } else if (widget instanceof WrapPanel wrapPanel) {
            writeFloat(element, "spacing", wrapPanel.spacing(), 0.0f);
            writeFloat(element, "lineSpacing", wrapPanel.lineSpacing(), 0.0f);
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
