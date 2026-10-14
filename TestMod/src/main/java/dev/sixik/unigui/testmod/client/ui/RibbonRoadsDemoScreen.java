package dev.sixik.unigui.testmod.client.ui;

import dev.sixik.unigui.api.core.UnityLikeUIScaleProvider;
import dev.sixik.unigui.api.debug.DebugFlags;
import dev.sixik.unigui.api.layout.Alignment;
import dev.sixik.unigui.api.layout.Overflow;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.render.DrawPoint;
import dev.sixik.unigui.api.render.TextureHandle;
import dev.sixik.unigui.api.render.TextureOptions;
import dev.sixik.unigui.api.render.TextureWrap;
import dev.sixik.unigui.api.render.UiRenderPolicy;
import dev.sixik.unigui.api.viewport.ViewportPoint;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.backend.minecraft_impl.MinecraftClipboardService;
import dev.sixik.unigui.backend.minecraft_impl.MinecraftTextureHandle;
import dev.sixik.unigui.backend.minecraft_impl.MinecraftWidgetScreen;
import dev.sixik.unigui.backend.minecraft_impl.UniGuiTextures;
import dev.sixik.unigui.impl.core.DefaultUIContext;
import dev.sixik.unigui.widgets.containers.Box;
import dev.sixik.unigui.widgets.containers.HBox;
import dev.sixik.unigui.widgets.containers.StackPanel;
import dev.sixik.unigui.widgets.containers.VBox;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.widgets.feedback.OverlayLayer;
import dev.sixik.unigui.widgets.interaction.Button;
import dev.sixik.unigui.widgets.interaction.Checkbox;
import dev.sixik.unigui.widgets.interaction.Slider;
import dev.sixik.unigui.widgets.world.RibbonLayer;
import dev.sixik.unigui.widgets.world.WorldCanvas;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import com.mojang.blaze3d.platform.NativeImage;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Демо textured ribbon-лент: дороги и кабели на {@link WorldCanvas}.
 *
 * <p>Три {@link RibbonLayer} с кабельными текстурами TestMod: прямая, змейка и
 * замкнутый контур. Панель крутит ширину, длину тайла и miter-лимит; pan/zoom
 * показывают, что ленты живут в мировых координатах.</p>
 */
public final class RibbonRoadsDemoScreen {
    private static final MutableColor BG = MutableColor.rgba(0.014f, 0.018f, 0.028f, 1.0f);
    private static final MutableColor PANEL = MutableColor.rgba(0.050f, 0.064f, 0.088f, 0.96f);
    private static final MutableColor BORDER = MutableColor.rgba(0.220f, 0.400f, 0.580f, 0.88f);
    private static final MutableColor TEXT = MutableColor.rgba(0.820f, 0.900f, 0.960f, 1.0f);
    private static final MutableColor MUTED = MutableColor.rgba(0.500f, 0.600f, 0.700f, 1.0f);

    private static final float WORLD = 2048.0f;

    private RibbonRoadsDemoScreen() {
    }

    public static void open() {
        Minecraft minecraft = Minecraft.getInstance();
        Screen previous = minecraft.screen;

        DefaultUIContext context = new DefaultUIContext(new MinecraftClipboardService());
        context.scaleProvider(new UnityLikeUIScaleProvider()
                .referenceResolution(1920.0f, 1080.0f)
                .matchBalanced()
                .scaleRange(0.75f, 6.0f)
                .userScale(2.0f));
        context.debugFlags(DebugFlags.PROFILER_OVERLAY);

        Runnable[] close = new Runnable[1];
        Widget root = root(() -> close[0].run());
        MinecraftWidgetScreen screen = new MinecraftWidgetScreen(
                Component.literal("Ribbon Roads Demo"), root, context) {
            @Override
            public void onClose() {
                Minecraft.getInstance().setScreen(previous);
            }
        };
        close[0] = screen::onClose;
        screen.useContextScale().scaleWithMinecraftGui(false);
        screen.useSdfDefaultFont();
        screen.renderPolicy(UiRenderPolicy.vsync());
        minecraft.setScreen(screen);
    }

    private static Widget root(Runnable closeAction) {
        StackPanel viewport = new StackPanel();
        viewport.layout(style -> style.sizePercent(100.0f, 100.0f).overflow(Overflow.HIDDEN));

        Box background = new Box();
        background.themeEnabled(false);
        background.backgroundVisible(true);
        background.borderVisible(false);
        background.background().set(BG);
        background.layout(style -> style.sizePercent(100.0f, 100.0f)
                .align(Alignment.STRETCH, Alignment.STRETCH));
        viewport.addChild(background);

        WorldCanvas canvas = new WorldCanvas()
                .viewport(60.0f, 40.0f, 0.5f)
                .zoomRange(0.1f, 4.0f);
        canvas.layout(style -> style.sizePercent(100.0f, 100.0f).flexGrow(1.0f).flexShrink(1.0f));

        RibbonLayer highway = new RibbonLayer()
                .texture(cable("cable_blue.png", 1, 8))
                .width(28.0f)
                .textureLength(64.0f)
                .swapUv(true)
                .flipV(true)
                .path(highwayPath());
        RibbonLayer serpentine = new RibbonLayer()
                .texture(cable("cable_green.png", 1, 8))
                .width(18.0f)
                .textureLength(48.0f)
                .swapUv(true)
                .flipV(true)
                .path(serpentinePath());
        RibbonLayer ring = new RibbonLayer()
                .texture(cable("cable_red.png", 1, 8))
                .width(14.0f)
                .textureLength(40.0f)
                .swapUv(true)
                .flipV(true)
                .closed(true)
                .path(ringPath());
        RibbonLayer dirt = new RibbonLayer()
                .texture(null)
                .width(36.0f)
                .path(dirtPath());
        dirt.tint().set(0.42f, 0.30f, 0.18f, 0.85f);
        canvas.addWorldLayer(dirt);
        canvas.addWorldLayer(highway);
        canvas.addWorldLayer(serpentine);
        canvas.addWorldLayer(ring);
        List<RibbonLayer> layers = List.of(highway, serpentine, ring);

        // Низкоуровневый draw.ribbon поверх мира: прицел в центре экрана.
        canvas.addWorldLayer((world, draw) -> {
            float cx = world.layoutBounds().x() + world.layoutBounds().width() * 0.5f;
            float cy = world.layoutBounds().y() + world.layoutBounds().height() * 0.5f;
            float r = 26.0f;
            List<DrawPoint> cross = List.of(
                    new DrawPoint(cx - r, cy), new DrawPoint(cx + r, cy));
            List<DrawPoint> crossV = List.of(
                    new DrawPoint(cx, cy - r), new DrawPoint(cx, cy + r));
            draw.ribbon(null, cross, 3.0f, 8.0f, MutableColor.rgba(1.0f, 1.0f, 1.0f, 0.5f));
            draw.ribbon(null, crossV, 3.0f, 8.0f, MutableColor.rgba(1.0f, 1.0f, 1.0f, 0.5f));
        });

        Label stats = label("", TEXT);
        stats.layout(style -> style.widthPercent(100.0f).height(20.0f)
                .flexGrow(0.0f).flexShrink(0.0f));
        Label hint = label("Pan: drag. Zoom: Ctrl+wheel. Ribbons scale with the world.", MUTED);
        hint.layout(style -> style.widthPercent(100.0f).height(34.0f)
                .flexGrow(0.0f).flexShrink(0.0f));

        Runnable refresh = () -> stats.text("Zoom " + String.format("%.2f", canvas.viewport().zoom()));
        canvas.onViewportChanged(event -> refresh.run());

        Label widthLabel = label("Width: 1.00x", TEXT);
        widthLabel.layout(style -> style.widthPercent(100.0f).height(20.0f)
                .flexGrow(0.0f).flexShrink(0.0f));
        Slider widthSlider = slider(0.25f, 3.0f, 1.0f);
        widthSlider.onValueChanged(event -> {
            widthLabel.text("Width: " + String.format("%.2f", widthSlider.value()) + "x");
            highway.width(28.0f * widthSlider.value());
            serpentine.width(18.0f * widthSlider.value());
            ring.width(14.0f * widthSlider.value());
        });

        Label tileLabel = label("Tile length: 1.00x", TEXT);
        tileLabel.layout(style -> style.widthPercent(100.0f).height(20.0f)
                .flexGrow(0.0f).flexShrink(0.0f));
        Slider tileSlider = slider(0.25f, 4.0f, 1.0f);
        tileSlider.onValueChanged(event -> {
            tileLabel.text("Tile length: " + String.format("%.2f", tileSlider.value()) + "x");
            highway.textureLength(64.0f * tileSlider.value());
            serpentine.textureLength(48.0f * tileSlider.value());
            ring.textureLength(40.0f * tileSlider.value());
        });

        Label miterLabel = label("Miter limit: 4.0", TEXT);
        miterLabel.layout(style -> style.widthPercent(100.0f).height(20.0f)
                .flexGrow(0.0f).flexShrink(0.0f));
        Slider miterSlider = slider(1.0f, 10.0f, 4.0f);
        miterSlider.onValueChanged(event -> {
            miterLabel.text("Miter limit: " + String.format("%.1f", miterSlider.value()));
            for (RibbonLayer layer : layers) layer.miterLimit(miterSlider.value());
        });

        Checkbox ringCheck = new Checkbox("Ring closed");
        ringCheck.checked(true);
        ringCheck.onCheckedChanged(event -> ring.closed(ringCheck.checked()));

        Checkbox flipCheck = new Checkbox("Flip texture V");
        flipCheck.checked(true);
        flipCheck.onCheckedChanged(event -> {
            for (RibbonLayer layer : layers) layer.flipV(flipCheck.checked());
        });

        Button recenter = button("Recenter", () -> {
            canvas.viewport(60.0f, 40.0f, 0.5f);
            refresh.run();
        });
        recenter.layout(style -> style.widthPercent(100.0f).height(28.0f)
                .flexGrow(0.0f).flexShrink(0.0f));
        Button close = button("Close", closeAction);
        close.layout(style -> style.widthPercent(100.0f).height(28.0f)
                .flexGrow(0.0f).flexShrink(0.0f));

        VBox side = new VBox();
        side.spacing(8.0f);
        side.layout(style -> style.width(240.0f).heightPercent(100.0f).padding(10.0f)
                .gap(8.0f).flexGrow(0.0f).flexShrink(0.0f));
        Label title = label("RIBBON ROADS", TEXT);
        title.layout(style -> style.widthPercent(100.0f).height(22.0f)
                .flexGrow(0.0f).flexShrink(0.0f));
        side.addChild(title);
        side.addChild(stats);
        side.addChild(hint);
        side.addChild(widthLabel);
        side.addChild(widthSlider);
        side.addChild(tileLabel);
        side.addChild(tileSlider);
        side.addChild(miterLabel);
        side.addChild(miterSlider);
        side.addChild(ringCheck);
        side.addChild(flipCheck);
        side.addChild(recenter);
        side.addChild(close);

        Box sidePanel = new Box();
        sidePanel.themeEnabled(false);
        sidePanel.backgroundVisible(true);
        sidePanel.borderVisible(true);
        sidePanel.background().set(PANEL);
        sidePanel.borderColor().set(BORDER);
        sidePanel.layout(style -> style.width(260.0f).heightPercent(100.0f)
                .flexGrow(0.0f).flexShrink(0.0f));
        sidePanel.addChild(side);

        HBox body = new HBox();
        body.spacing(10.0f);
        body.layout(style -> style.sizePercent(96.0f, 90.0f)
                .align(Alignment.CENTER, Alignment.CENTER).gap(10.0f));
        body.addChild(canvas);
        body.addChild(sidePanel);
        viewport.addChild(body);

        refresh.run();
        return new OverlayLayer(viewport);
    }

    private static TextureHandle cable(String file, int width, int height) {
        // REPEAT по V: лента тайлит 1x8 полоску вдоль пути без растяжения.
        // Грузим через UniGuiTextures: сырой ResourceLocation без регистрации
        // в TextureManager даёт missing texture.
        TextureHandle cached = UniGuiTextures.get("unigui_testmod:dynamic/ribbons/" + file);
        if (cached != null) return cached;
        TextureOptions repeat = TextureOptions.nearest().wrap(TextureWrap.REPEAT);
        String resource = "assets/unigui_testmod/textures/gui/cables/" + file;
        try (InputStream stream = RibbonRoadsDemoScreen.class.getClassLoader().getResourceAsStream(resource)) {
            if (stream == null) {
                return new MinecraftTextureHandle(
                        ResourceLocation.tryBuild("unigui_testmod", "textures/gui/cables/" + file),
                        width, height, repeat);
            }
            return UniGuiTextures.replace("unigui_testmod:dynamic/ribbons/" + file,
                    NativeImage.read(stream), repeat);
        } catch (IOException | RuntimeException failure) {
            return new MinecraftTextureHandle(
                    ResourceLocation.tryBuild("unigui_testmod", "textures/gui/cables/" + file),
                    width, height, repeat);
        }
    }

    private static List<ViewportPoint> highwayPath() {
        return List.of(
                new ViewportPoint(100.0f, 300.0f),
                new ViewportPoint(700.0f, 300.0f),
                new ViewportPoint(1100.0f, 700.0f),
                new ViewportPoint(1700.0f, 700.0f),
                new ViewportPoint(1900.0f, 1100.0f));
    }

    private static List<ViewportPoint> serpentinePath() {
        List<ViewportPoint> points = new ArrayList<>();
        for (int i = 0; i <= 24; i++) {
            float x = 150.0f + i * 70.0f;
            float y = 1300.0f + (float) Math.sin(i * 0.55f) * 220.0f;
            points.add(new ViewportPoint(x, y));
        }
        return points;
    }

    private static List<ViewportPoint> ringPath() {
        List<ViewportPoint> points = new ArrayList<>();
        float cx = 1500.0f;
        float cy = 1400.0f;
        float rx = 260.0f;
        float ry = 200.0f;
        for (int i = 0; i < 16; i++) {
            float angle = (float) (i / 16.0f * Math.PI * 2.0);
            points.add(new ViewportPoint(
                    cx + (float) Math.cos(angle) * rx,
                    cy + (float) Math.sin(angle) * ry));
        }
        return points;
    }

    private static List<ViewportPoint> dirtPath() {
        return List.of(
                new ViewportPoint(250.0f, 1900.0f),
                new ViewportPoint(900.0f, 1750.0f),
                new ViewportPoint(1300.0f, 1900.0f),
                new ViewportPoint(1900.0f, 1700.0f));
    }

    private static Slider slider(float min, float max, float value) {
        Slider slider = new Slider();
        slider.range(min, max);
        slider.value(value);
        slider.layout(style -> style.widthPercent(100.0f).height(24.0f)
                .flexGrow(0.0f).flexShrink(0.0f));
        return slider;
    }

    private static Label label(String text, MutableColor color) {
        Label label = new Label(text);
        label.color(color);
        return label;
    }

    private static Button button(String text, Runnable action) {
        Button button = new Button(text);
        button.onClick(event -> action.run());
        return button;
    }
}
