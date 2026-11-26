package dev.sixik.unigui.xaml.schema.builtin;

import dev.sixik.unigui.api.render.ImageFit;
import dev.sixik.unigui.api.render.RenderTargetOptions;
import dev.sixik.unigui.api.render.TextureFilter;
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
import dev.sixik.unigui.widgets.rendering.RenderToTexture;
import dev.sixik.unigui.widgets.rendering.Scene3D;
import dev.sixik.unigui.xaml.schema.ChildSink;
import dev.sixik.unigui.xaml.schema.Converters;
import dev.sixik.unigui.xaml.schema.Meta;
import dev.sixik.unigui.xaml.schema.Schema;
import dev.sixik.unigui.xaml.schema.SlotInfo;
import dev.sixik.unigui.xaml.schema.TypeInfo;
import dev.sixik.unigui.xaml.schema.ValueKind;

import java.util.Locale;

import static dev.sixik.unigui.xaml.schema.builtin.Types.singleContent;
import static dev.sixik.unigui.xaml.schema.builtin.Types.type;

/**
 * Отображение: текст, изображения, разделители, векторные пути, кэшируемые поддеревья.
 */
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
        schema.define("RenderToTexture", DisplayTypes::renderToTexture);
        schema.define("Scene3D", DisplayTypes::scene3D);
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
                                        .description("Offscreen render target mode: color or colorDepth."))
                        .reads("tint", CachedSubtreeWidget::tint)
                        .exportsChildren(widget -> Types.optional(widget.content())),
                content, "Single widget subtree rendered into the cache.").build();
    }

    private static dev.sixik.unigui.xaml.schema.TypeInfo<RenderToTexture> renderToTexture() {
        ChildSink<RenderToTexture> content = (parent, child) -> {
            if (parent.content() != null) {
                throw new IllegalArgumentException("Widget RenderToTexture can contain only one content child.");
            }
            parent.content(child);
        };
        return singleContent(type("RenderToTexture", RenderToTexture.class, RenderToTexture::new)
                        .describe("Render To Texture", "Display",
                                "Renders its child (or a Java painter) into an offscreen texture that can be shown in place or reused elsewhere.")
                        .property("updateMode", Converters.enumOf(RenderToTexture.UpdateMode.class), RenderToTexture::updateMode,
                                Meta.category("Rendering").displayName("Update Mode").defaultValue("EVERY_FRAME")
                                        .description("When the texture is redrawn: every frame, on change, or only on request."))
                        .property("resolutionScale", Converters.FLOAT, RenderToTexture::resolutionScale,
                                Meta.category("Rendering").displayName("Resolution Scale").defaultValue("1")
                                        .description("Multiplier of the automatic texture size; 2 supersamples."))
                        .property("textureSize", Converters.STRING,
                                (widget, value) -> {
                                    int[] size = sizePair(value, "textureSize");
                                    widget.textureSize(size[0], size[1]);
                                },
                                Meta.category("Rendering").displayName("Texture Size")
                                        .description("Fixed texture size in pixels as 'WxH', for example '64x64'; '0x0' is automatic."))
                        .property("canvasSize", Converters.STRING,
                                (widget, value) -> {
                                    int[] size = sizePair(value, "canvasSize");
                                    if (size[0] == 0 && size[1] == 0) widget.clearCanvasSize();
                                    else widget.canvasSize(size[0], size[1]);
                                },
                                Meta.category("Rendering").displayName("Canvas Size")
                                        .description("Logical size of the content as 'WxH'; by default the widget size."))
                        .property("targetOptions", Converters.STRING,
                                (widget, value) -> widget.targetOptions(renderTargetOptions(value)),
                                Meta.category("Rendering").displayName("Target Options").defaultValue("color")
                                        .kind(ValueKind.ENUM)
                                        .description("Offscreen render target mode: color or colorDepth."))
                        .property("depth", Converters.BOOLEAN, RenderToTexture::depth,
                                Meta.category("Rendering").defaultValue("false")
                                        .description("Whether the offscreen target has a depth buffer (needed for 3D)."))
                        .property("clearColor", Converters.COLOR, (widget, color) -> widget.clearColor().set(color),
                                Meta.category("Rendering").displayName("Clear Color").defaultValue("#00000000")
                                        .description("Color the texture is filled with before drawing."))
                        .property("displayed", Converters.BOOLEAN, RenderToTexture::displayed,
                                Meta.category("Appearance").defaultValue("true")
                                        .description("Whether the texture is drawn in place of the widget."))
                        .property("fit", Converters.enumOf(ImageFit.class), RenderToTexture::fit,
                                Meta.category("Appearance").defaultValue("STRETCH")
                                        .description("How the texture fits the widget bounds."))
                        .property("filter", Converters.enumOf(TextureFilter.class), RenderToTexture::filter,
                                Meta.category("Appearance").defaultValue("LINEAR")
                                        .description("Texture filtering; NEAREST for pixel art."))
                        .property("tint", Converters.COLOR, (widget, color) -> widget.tint().set(color),
                                Meta.category("Appearance").defaultValue("#FFFFFFFF")
                                        .description("Tint applied when drawing the texture."))
                        .reads("tint", RenderToTexture::tint)
                        .reads("clearColor", RenderToTexture::clearColor)
                        .exportsChildren(widget -> Types.optional(widget.content())),
                content, "Single widget rendered into the texture.").build();
    }

    /**
     * Defines camera, rendering and content properties for a 3D scene.
     * @return the Scene3D schema for loading and editor inspection.
     */
    private static TypeInfo<Scene3D> scene3D() {
        ChildSink<Scene3D> overlay = (scene, child) -> {
            if (scene.content() != null) {
                throw new IllegalArgumentException("Scene3D can contain only one UI overlay.");
            }
            scene.content(child);
        };
        return type("Scene3D", Scene3D.class, Scene3D::new)
                .describe("3D Scene", "Display", "Renders a 3D scene with an interactive camera and an optional UI overlay.")
                .property("camera", Converters.enumOf(SceneCamera.class),
                        (scene, mode) -> scene.cameraControls(mode == SceneCamera.ORBIT),
                        scene -> scene.cameraControls() ? SceneCamera.ORBIT : SceneCamera.FIXED,
                        Meta.category("Camera").defaultValue("ORBIT").description("Orbit camera or fixed view."))
                .property("yaw", Converters.FLOAT,
                        (scene, degrees) -> scene.orbit().setAngles(Math.toRadians(degrees), scene.orbit().pitch()),
                        scene -> (float) Math.toDegrees(scene.orbit().yaw()),
                        Meta.category("Camera").defaultValue("0").description("Camera yaw in degrees."))
                .property("pitch", Converters.FLOAT,
                        (scene, degrees) -> scene.orbit().setAngles(scene.orbit().yaw(), Math.toRadians(degrees)),
                        scene -> (float) Math.toDegrees(scene.orbit().pitch()),
                        Meta.category("Camera").defaultValue("0").description("Camera elevation in degrees."))
                .property("distance", Converters.FLOAT,
                        (scene, value) -> scene.orbit().setDistance(value), scene -> scene.orbit().distance(),
                        Meta.category("Camera").defaultValue("5").description("Distance to the origin in scene units."))
                .property("farClip", Converters.FLOAT,
                        (scene, value) -> scene.camera().setClipPlanes(scene.camera().near(), value),
                        scene -> scene.camera().far(), Meta.category("Camera").defaultValue("1000")
                                .description("Far clipping plane in scene units from the camera."))
                .property("maxZoomDistance", Converters.FLOAT, Scene3D::maxZoomDistance, Scene3D::maxZoomDistance,
                        Meta.category("Camera").defaultValue("0")
                                .description("Maximum perspective orbit radius for wheel zoom. Zero uses the far plane."))
                .property("updateMode", Converters.enumOf(RenderToTexture.UpdateMode.class), Scene3D::updateMode,
                        Scene3D::updateMode, Meta.category("Rendering").defaultValue("ON_CHANGE"))
                .property("resolutionScale", Converters.FLOAT, Scene3D::resolutionScale,
                        Scene3D::resolutionScale, Meta.category("Rendering").defaultValue("1"))
                .property("clearColor", Converters.COLOR, (scene, color) -> scene.clearColor().set(color),
                        Scene3D::clearColor, Meta.category("Rendering").defaultValue("#00000000"))
                .property("highlightBlocks", Converters.BOOLEAN, Scene3D::highlightBlocks, Scene3D::highlightBlocks,
                        Meta.category("Appearance").defaultValue("false")
                                .description("Highlights the block beneath the mouse pointer."))
                .property("highlightObjects", Converters.BOOLEAN, Scene3D::highlightObjects, Scene3D::highlightObjects,
                        Meta.category("Appearance").defaultValue("false")
                                .description("Draws a frame around objects such as items and mobs beneath the mouse pointer."))
                .content((scene, child) -> {
                    if (child instanceof Scene3D.NodeContent node) scene.addNodeContent(node);
                    else overlay.add(scene, child);
                })
                .slot(SlotInfo.single("Content", overlay, "One UI widget drawn over the scene."))
                .exportsChildren(Scene3D::xamlChildren)
                .build();
    }

    /** Доступные в разметке режимы управления камерой. */
    private enum SceneCamera {
        /** Камера реагирует на орбитальный ввод. */
        ORBIT,
        /** Камера остаётся на настроенной позиции. */
        FIXED
    }

    private static int[] sizePair(String value, String property) {
        String[] parts = value == null ? new String[0] : value.trim().toLowerCase(Locale.ROOT).split("\\s*[x,]\\s*");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Expected " + property + " as 'WxH', got: " + value);
        }
        try {
            return new int[]{Integer.parseInt(parts[0]), Integer.parseInt(parts[1])};
        } catch (NumberFormatException invalid) {
            throw new IllegalArgumentException("Expected " + property + " as 'WxH', got: " + value, invalid);
        }
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
