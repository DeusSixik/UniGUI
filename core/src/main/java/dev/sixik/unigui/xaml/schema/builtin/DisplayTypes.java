package dev.sixik.unigui.xaml.schema.builtin;

import dev.sixik.unigui.api.render.RenderTargetOptions;
import dev.sixik.unigui.widgets.caching.CachedSubtreeWidget;
import dev.sixik.unigui.widgets.core.Orientation;
import dev.sixik.unigui.widgets.display.CanvasWidget;
import dev.sixik.unigui.widgets.display.Chart;
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
import dev.sixik.unigui.xaml.schema.ChildSink;
import dev.sixik.unigui.xaml.schema.Converters;
import dev.sixik.unigui.xaml.schema.Meta;
import dev.sixik.unigui.xaml.schema.Schema;
import dev.sixik.unigui.xaml.schema.ValueKind;

import java.util.Locale;

import static dev.sixik.unigui.xaml.schema.builtin.Types.singleContent;
import static dev.sixik.unigui.xaml.schema.builtin.Types.type;

/** Отображение: текст, изображения, разделители, векторные пути, кэшируемые поддеревья. */
final class DisplayTypes {
    private DisplayTypes() {
    }

    static void register(Schema schema) {
        schema.define("TextWidget", () -> type("TextWidget", TextWidget.class, TextWidget::new).build());
        schema.define("Text", () -> type("Text", Text.class, Text::new).build());
        schema.define("TextBlock", () -> type("TextBlock", TextBlock.class, TextBlock::new).build());
        schema.define("Label", () -> type("Label", Label.class, Label::new).build());
        schema.define("TextureWidget", () -> type("TextureWidget", TextureWidget.class, TextureWidget::new).build());
        schema.define("ImageView", () -> type("ImageView", ImageView.class, ImageView::new).build());

        schema.define("Separator", () -> type("Separator", Separator.class, Separator::new)
                .property("color", Converters.COLOR, (widget, color) -> widget.color().set(color))
                .property("orientation", Converters.enumOf(Orientation.class), Separator::orientation)
                .property("thickness", Converters.FLOAT, Separator::thickness)
                .build());
        schema.define("CanvasWidget", () -> type("CanvasWidget", CanvasWidget.class, CanvasWidget::new)
                .describe("Canvas Widget", "Display",
                        "Code-drawn canvas host. XML can size and style it, but draw callbacks are attached from Java.")
                .build());
        schema.define("Path", DisplayTypes::path);
        schema.define("CachedSubtreeWidget", DisplayTypes::cachedSubtree);
        schema.define("Shape", () -> type("Shape", Shape.class, Shape::new).build());
        schema.define("Sparkline", () -> type("Sparkline", Sparkline.class, Sparkline::new).build());
        schema.define("Chart", () -> type("Chart", Chart.class, Chart::new).build());
    }

    private static dev.sixik.unigui.xaml.schema.TypeInfo<Path> path() {
        return type("Path", Path.class, Path::new)
                .describe("Path", "Display",
                        "Vector path display using a limited XML data syntax: M, L, Q, C and Z absolute commands.")
                .property("data", Converters.STRING, (widget, value) -> widget.path().set(PathData.parse(value)),
                        Meta.category("Content").displayName("Path Data").kind(ValueKind.STRING)
                                .description("Limited SVG-like path data with absolute M/L/Q/C/Z commands, for example 'M 0 0 L 1 0 L 1 1 Z'."))
                .property("color", Converters.COLOR, (widget, color) -> widget.color().set(color),
                        Meta.category("Appearance").defaultValue("#FFFFFFFF").description("Path stroke or fill color."))
                .property("stroke", Converters.BOOLEAN, Path::stroke,
                        Meta.category("Appearance").defaultValue("true").description("Whether the path is rendered as a stroke."))
                .property("strokeWidth", Converters.FLOAT, Path::strokeWidth,
                        Meta.category("Appearance").displayName("Stroke Width").defaultValue("1")
                                .description("Path stroke width in logical pixels."))
                .build();
    }

    private static dev.sixik.unigui.xaml.schema.TypeInfo<CachedSubtreeWidget> cachedSubtree() {
        ChildSink<CachedSubtreeWidget> content = (parent, child) -> {
            if (parent.content() != null) {
                throw new IllegalArgumentException("Widget CachedSubtreeWidget can contain only one content child.");
            }
            parent.content(child);
        };
        return singleContent(type("CachedSubtreeWidget", CachedSubtreeWidget.class, CachedSubtreeWidget::new)
                        .describe("Cached Subtree", "Performance",
                                "Advanced opt-in wrapper that renders one child subtree into a cached texture.")
                        .property("tint", Converters.COLOR, (widget, color) -> widget.tint().set(color),
                                Meta.category("Appearance").defaultValue("#FFFFFFFF")
                                        .description("Tint applied when drawing the cached texture."))
                        .property("targetOptions", Converters.STRING,
                                (widget, value) -> widget.targetOptions(renderTargetOptions(value)),
                                Meta.category("Performance").displayName("Target Options").defaultValue("color")
                                        .kind(ValueKind.ENUM)
                                        .description("Offscreen render target mode: color or colorDepth.")),
                content, "Single widget subtree rendered into the cache.").build();
    }

    private static RenderTargetOptions renderTargetOptions(String value) {
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
