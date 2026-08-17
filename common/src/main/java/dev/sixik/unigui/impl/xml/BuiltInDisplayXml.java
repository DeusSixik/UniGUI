package dev.sixik.unigui.impl.xml;

import dev.sixik.unigui.api.render.RenderTargetOptions;
import dev.sixik.unigui.api.render.VectorPath;
import dev.sixik.unigui.api.xml.XmlAttributeDescriptor;
import dev.sixik.unigui.api.xml.XmlAttributeValueType;
import dev.sixik.unigui.api.xml.XmlPropertyChildDescriptor;
import dev.sixik.unigui.widgets.caching.CachedSubtreeWidget;
import dev.sixik.unigui.widgets.display.Chart;
import dev.sixik.unigui.widgets.display.CanvasWidget;
import dev.sixik.unigui.widgets.core.Orientation;
import dev.sixik.unigui.widgets.display.ImageView;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.widgets.display.Path;
import dev.sixik.unigui.widgets.display.Separator;
import dev.sixik.unigui.widgets.display.Shape;
import dev.sixik.unigui.widgets.display.Sparkline;
import dev.sixik.unigui.widgets.display.Text;
import dev.sixik.unigui.widgets.display.TextBlock;
import dev.sixik.unigui.widgets.display.TextWidget;
import dev.sixik.unigui.widgets.display.TextureWidget;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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
        canvas(registry.register("CanvasWidget", CanvasWidget::new));
        path(registry.register("Path", Path::new));
        cachedSubtree(registry.register("CachedSubtreeWidget", CachedSubtreeWidget::new));
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

    private static WidgetXmlType<CanvasWidget> canvas(WidgetXmlType<CanvasWidget> type) {
        return BuiltInWidgetXmlSupport.commonWidget(type, CanvasWidget.class)
                .describe("Canvas Widget", "Display",
                        "Code-drawn canvas host. XML can size and style it, but draw callbacks are attached from Java.");
    }

    private static WidgetXmlType<Path> path(WidgetXmlType<Path> type) {
        return BuiltInWidgetXmlSupport.commonWidget(type, Path.class)
                .describe("Path", "Display",
                        "Vector path display using a limited XML data syntax: M, L, Q, C and Z absolute commands.")
                .attribute("data", XmlValueParsers.STRING, BuiltInDisplayXml::pathData,
                        XmlAttributeDescriptor.of("data")
                                .displayName("Path Data")
                                .category("Content")
                                .defaultValue("")
                                .valueType(XmlAttributeValueType.STRING)
                                .description("Limited SVG-like path data with absolute M/L/Q/C/Z commands, for example 'M 0 0 L 1 0 L 1 1 Z'."))
                .attribute("color", XmlValueParsers.COLOR, (widget, color) -> widget.color().set(color),
                        XmlAttributeDescriptor.of("color")
                                .category("Appearance")
                                .defaultValue("#FFFFFFFF")
                                .description("Path stroke or fill color."));
    }

    private static WidgetXmlType<CachedSubtreeWidget> cachedSubtree(WidgetXmlType<CachedSubtreeWidget> type) {
        XmlChildPolicy<CachedSubtreeWidget> content = (parent, child) -> {
            if (parent.content() != null) {
                throw new IllegalArgumentException("Widget CachedSubtreeWidget can contain only one content child.");
            }
            parent.content(child);
        };
        return BuiltInWidgetXmlSupport.commonWidget(type, CachedSubtreeWidget.class)
                .describe("Cached Subtree", "Performance",
                        "Advanced opt-in wrapper that renders one child subtree into a cached texture.")
                .attribute("tint", XmlValueParsers.COLOR, (widget, color) -> widget.tint().set(color),
                        XmlAttributeDescriptor.of("tint")
                                .category("Appearance")
                                .defaultValue("#FFFFFFFF")
                                .description("Tint applied when drawing the cached texture."))
                .attribute("targetOptions", XmlValueParsers.STRING, BuiltInDisplayXml::cachedTargetOptions,
                        XmlAttributeDescriptor.of("targetOptions")
                                .displayName("Target Options")
                                .category("Performance")
                                .defaultValue("color")
                                .valueType(XmlAttributeValueType.ENUM)
                                .description("Offscreen render target mode: color or colorDepth."))
                .childPolicy(content)
                .propertyChild("Content", content,
                        XmlPropertyChildDescriptor.of("Content")
                                .category("Content")
                                .description("Single widget subtree rendered into the cache.")
                                .singleChildOnly());
    }

    private static void pathData(Path widget, String value) {
        widget.path().set(parsePathData(value));
    }

    private static VectorPath parsePathData(String value) {
        VectorPath path = new VectorPath();
        List<String> tokens = pathTokens(value);
        if (tokens.isEmpty()) return path;

        int[] index = {0};
        char command = 0;
        while (index[0] < tokens.size()) {
            String token = tokens.get(index[0]);
            if (isCommand(token)) {
                command = parsePathCommand(token);
                index[0]++;
                if (command == 'Z') {
                    path.close();
                    command = 0;
                    continue;
                }
            } else if (command == 0) {
                throw new IllegalArgumentException("Path data must start with a command.");
            }

            switch (command) {
                case 'M' -> {
                    path.moveTo(readPathNumber(tokens, index, command), readPathNumber(tokens, index, command));
                    command = 'L';
                }
                case 'L' -> path.lineTo(readPathNumber(tokens, index, command), readPathNumber(tokens, index, command));
                case 'Q' -> path.quadraticTo(
                        readPathNumber(tokens, index, command),
                        readPathNumber(tokens, index, command),
                        readPathNumber(tokens, index, command),
                        readPathNumber(tokens, index, command));
                case 'C' -> path.cubicTo(
                        readPathNumber(tokens, index, command),
                        readPathNumber(tokens, index, command),
                        readPathNumber(tokens, index, command),
                        readPathNumber(tokens, index, command),
                        readPathNumber(tokens, index, command),
                        readPathNumber(tokens, index, command));
                default -> throw new IllegalArgumentException("Unsupported path command: " + command);
            }
        }
        return path;
    }

    private static List<String> pathTokens(String value) {
        if (value == null || value.isBlank()) return List.of();
        List<String> tokens = new ArrayList<>();
        int index = 0;
        while (index < value.length()) {
            char current = value.charAt(index);
            if (Character.isWhitespace(current) || current == ',' || current == ';' || current == '|') {
                index++;
                continue;
            }
            if (Character.isLetter(current)) {
                tokens.add(String.valueOf(current));
                index++;
                continue;
            }

            int start = index;
            if (current == '+' || current == '-') {
                index++;
            }
            boolean hasDigit = false;
            boolean hasDot = false;
            boolean hasExponent = false;
            while (index < value.length()) {
                char next = value.charAt(index);
                if (Character.isDigit(next)) {
                    hasDigit = true;
                    index++;
                } else if (next == '.' && !hasDot && !hasExponent) {
                    hasDot = true;
                    index++;
                } else if ((next == 'e' || next == 'E') && !hasExponent && hasDigit) {
                    hasExponent = true;
                    index++;
                    if (index < value.length()) {
                        char sign = value.charAt(index);
                        if (sign == '+' || sign == '-') {
                            index++;
                        }
                    }
                } else {
                    break;
                }
            }
            if (!hasDigit) {
                throw new IllegalArgumentException("Expected path number near: " + value.substring(start));
            }
            tokens.add(value.substring(start, index));
        }
        return tokens;
    }

    private static boolean isCommand(String token) {
        return token.length() == 1 && Character.isLetter(token.charAt(0));
    }

    private static char parsePathCommand(String token) {
        char command = Character.toUpperCase(token.charAt(0));
        return switch (command) {
            case 'M', 'L', 'Q', 'C', 'Z' -> command;
            default -> throw new IllegalArgumentException(
                    "Unsupported path command '" + token + "'. Supported commands: M, L, Q, C, Z.");
        };
    }

    private static float readPathNumber(List<String> tokens, int[] index, char command) {
        if (index[0] >= tokens.size() || isCommand(tokens.get(index[0]))) {
            throw new IllegalArgumentException("Missing numeric coordinate for path command " + command + ".");
        }
        String token = tokens.get(index[0]++);
        try {
            return Float.parseFloat(token);
        } catch (NumberFormatException failure) {
            throw new IllegalArgumentException("Expected numeric coordinate for path command " + command + ": " + token, failure);
        }
    }

    private static void cachedTargetOptions(CachedSubtreeWidget widget, String value) {
        widget.targetOptions(parseRenderTargetOptions(value));
    }

    private static RenderTargetOptions parseRenderTargetOptions(String value) {
        String normalized = value == null ? "" : value.trim()
                .replace("-", "_")
                .replace(" ", "_")
                .toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "", "color" -> RenderTargetOptions.COLOR;
            case "color_depth", "colordepth", "depth" -> RenderTargetOptions.COLOR_DEPTH;
            default -> throw new IllegalArgumentException("Unknown render target options: " + value);
        };
    }
}
