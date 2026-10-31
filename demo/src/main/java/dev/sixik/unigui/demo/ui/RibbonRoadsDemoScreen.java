package dev.sixik.unigui.demo.ui;

import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.render.DrawPoint;
import dev.sixik.unigui.api.render.TextureHandle;
import dev.sixik.unigui.api.render.TextureOptions;
import dev.sixik.unigui.api.render.TextureWrap;
import dev.sixik.unigui.api.screen.UniScreen;
import dev.sixik.unigui.api.viewport.ViewportPoint;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.widgets.interaction.Checkbox;
import dev.sixik.unigui.widgets.interaction.Slider;
import dev.sixik.unigui.widgets.world.RibbonLayer;
import dev.sixik.unigui.widgets.world.WorldCanvas;

import java.util.ArrayList;
import java.util.List;

import static dev.sixik.unigui.demo.ui.DemoScaffold.TEXT;
import static dev.sixik.unigui.demo.ui.DemoScaffold.checkbox;
import static dev.sixik.unigui.demo.ui.DemoScaffold.fullWidth;
import static dev.sixik.unigui.demo.ui.DemoScaffold.label;
import static dev.sixik.unigui.demo.ui.DemoScaffold.slider;

/**
 * Демо textured ribbon-лент: дороги и кабели на {@link WorldCanvas}.
 *
 * <p>Три {@link RibbonLayer} с кабельными текстурами TestMod: прямая, змейка и
 * замкнутый контур. Панель крутит ширину, длину тайла и miter-лимит; pan/zoom
 * показывают, что ленты живут в мировых координатах.</p>
 */
public final class RibbonRoadsDemoScreen {
    private static final float WORLD = 2048.0f;

    private RibbonRoadsDemoScreen() {
    }

    public static void open() {
        createScreen().openGui();
    }

    public static UniScreen createScreen() {
        return DemoScaffold.create("Ribbon Roads Demo", "RIBBON ROADS",
                "Pan: drag. Zoom: Ctrl+wheel. Ribbons scale with the world.",
                RibbonRoadsDemoScreen::build);
    }

    private static void build(DemoScaffold.DemoShell shell) {
        WorldCanvas canvas = new WorldCanvas()
                .viewport(60.0f, 40.0f, 0.5f)
                .zoomRange(0.1f, 4.0f);
        shell.content(canvas);

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

        Label stats = shell.stats();
        Runnable refresh = () -> stats.text("Zoom " + String.format("%.2f", canvas.viewport().zoom())
                + ", mesh " + highway.cacheRebuilds() + " rebuilds / " + highway.cacheHits() + " hits");
        canvas.onViewportChanged(event -> refresh.run());

        Label widthLabel = fullWidth(label("Width: 1.00x", TEXT), 20.0f);
        Slider widthSlider = slider(0.25f, 3.0f, 1.0f);
        widthSlider.onValueChanged(event -> {
            widthLabel.text("Width: " + String.format("%.2f", widthSlider.value()) + "x");
            highway.width(28.0f * widthSlider.value());
            serpentine.width(18.0f * widthSlider.value());
            ring.width(14.0f * widthSlider.value());
        });

        Label tileLabel = fullWidth(label("Tile length: 1.00x", TEXT), 20.0f);
        Slider tileSlider = slider(0.25f, 4.0f, 1.0f);
        tileSlider.onValueChanged(event -> {
            tileLabel.text("Tile length: " + String.format("%.2f", tileSlider.value()) + "x");
            highway.textureLength(64.0f * tileSlider.value());
            serpentine.textureLength(48.0f * tileSlider.value());
            ring.textureLength(40.0f * tileSlider.value());
        });

        Label miterLabel = fullWidth(label("Miter limit: 4.0", TEXT), 20.0f);
        Slider miterSlider = slider(1.0f, 10.0f, 4.0f);
        miterSlider.onValueChanged(event -> {
            miterLabel.text("Miter limit: " + String.format("%.1f", miterSlider.value()));
            for (RibbonLayer layer : layers) layer.miterLimit(miterSlider.value());
        });

        Checkbox ringCheck = checkbox("Ring closed", true);
        ringCheck.onCheckedChanged(event -> ring.closed(ringCheck.checked()));

        Checkbox flipCheck = checkbox("Flip texture V", true);
        flipCheck.onCheckedChanged(event -> {
            for (RibbonLayer layer : layers) layer.flipV(flipCheck.checked());
        });

        Checkbox jointsCheck = checkbox("Joint dots", false);
        jointsCheck.onCheckedChanged(event -> {
            for (RibbonLayer layer : layers) layer.jointDots(jointsCheck.checked());
        });

        shell.add(widthLabel);
        shell.add(widthSlider);
        shell.add(tileLabel);
        shell.add(tileSlider);
        shell.add(miterLabel);
        shell.add(miterSlider);
        shell.add(ringCheck);
        shell.add(flipCheck);
        shell.add(jointsCheck);
        shell.onRecenter(() -> {
            canvas.viewport(60.0f, 40.0f, 0.5f);
            refresh.run();
        });
        refresh.run();
    }

    private static TextureHandle cable(String file, int width, int height) {
        // REPEAT по V: лента тайлит 1x8 полоску вдоль пути без растяжения.
        TextureOptions repeat = TextureOptions.nearest().wrap(TextureWrap.REPEAT);
        String resource = "assets/unigui_testmod/textures/gui/cables/" + file;
        return DemoAssets.texture("unigui_testmod:dynamic/ribbons/" + file, resource, repeat);
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
}
