package dev.sixik.unigui.cleanroom.demo;

import dev.sixik.unigui.api.core.FrameContext;
import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.layout.Alignment;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.render.DrawList;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.render.RenderTargetOptions;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.impl.render.DefaultRenderContext;
import dev.sixik.unigui.impl.render.WidgetTextureRenderer;
import dev.sixik.unigui.impl.widget.WidgetBase;
import dev.sixik.unigui.api.layout.LayoutConstraints;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.render.TextureFilter;
import dev.sixik.unigui.api.render.TextureHandle;
import dev.sixik.unigui.api.render.TextureOptions;
import dev.sixik.unigui.api.render.TextureWrap;
import dev.sixik.unigui.api.screen.UniScreen;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.cleanroom.backend.CleanroomDynamicTextures;
import dev.sixik.unigui.cleanroom.backend.CleanroomTextureHandle;
import dev.sixik.unigui.impl.core.DefaultUIContext;
import dev.sixik.unigui.impl.input.MemoryClipboardService;
import dev.sixik.unigui.widgets.containers.HBox;
import dev.sixik.unigui.widgets.containers.StackPanel;
import dev.sixik.unigui.widgets.containers.VBox;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.widgets.display.TextureWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;

import java.nio.ByteBuffer;

/**
 * Manual check of the 1.12.2 texture path ({@code /unigui textures}): dynamic textures, nearest and linear sampling,
 * wrap modes, mipmaps, rounded corners, tint and a vanilla resource. Every tile says what it should look like.
 */
public final class TextureLab {
    private static final String CHECKER_ID = "unigui:dynamic/lab_checker";
    private static final String NOISE_ID = "unigui:dynamic/lab_noise";
    private static final float TILE = 96.0f;

    private TextureLab() {
    }

    /** Queues the screen on the client thread. */
    public static void open() {
        Minecraft.getMinecraft().addScheduledTask(TextureLab::openClient);
    }

    private static void openClient() {
        UniScreen screen = new UniScreen("UniGUI Texture Lab", create(),
                new DefaultUIContext(new MemoryClipboardService()));
        screen.scaleWithGameGui(true);
        screen.openGui();
    }

    private static Widget create() {
        TextureHandle checker = CleanroomDynamicTextures.replace(CHECKER_ID, 8, 8, checkerPixels(8), TextureOptions.nearest());
        TextureHandle noise = CleanroomDynamicTextures.replace(NOISE_ID, 64, 64, noisePixels(64),
                TextureOptions.linear().minFilter(TextureFilter.LINEAR_MIPMAP_LINEAR).mipmaps(true));
        CleanroomTextureHandle diamond = new CleanroomTextureHandle(
                new ResourceLocation("minecraft", "textures/items/diamond.png"), 16, 16);

        StackPanel root = new StackPanel();
        root.layout(style -> style.sizePercent(100.0f, 100.0f));

        VBox content = new VBox();
        content.spacing(10.0f);
        content.layout(style -> style.margin(12.0f).align(Alignment.CENTER, Alignment.CENTER));
        content.addChild(caption("Texture lab: each tile states the expected result"));

        HBox top = row();
        top.addChild(tile(new TextureWidget(checker), "nearest: hard 8x8 squares"));
        top.addChild(tile(new TextureWidget(((CleanroomTextureHandle) checker).withOptions(TextureOptions.linear())),
                "linear: smooth blend"));
        top.addChild(tile(new TextureWidget(((CleanroomTextureHandle) checker)
                        .withOptions(TextureOptions.nearest().wrap(TextureWrap.REPEAT))).source(0.0f, 0.0f, 3.0f, 3.0f),
                "repeat: 3 x 3 copies"));
        top.addChild(tile(new TextureWidget(((CleanroomTextureHandle) checker)
                        .withOptions(TextureOptions.nearest().wrap(TextureWrap.MIRRORED_REPEAT)))
                        .source(0.0f, 0.0f, 2.0f, 2.0f),
                "mirrored: 2 x 2, flipped"));
        content.addChild(top);

        HBox bottom = row();
        bottom.addChild(tile(new TextureWidget(diamond).radius(28.0f), "vanilla diamond, radius 28"));
        bottom.addChild(tile(new TextureWidget(diamond).tint(MutableColor.rgba(1.0f, 0.45f, 0.45f, 0.7f)),
                "tint: red, 70% alpha"));
        bottom.addChild(tile(new TextureWidget(noise), "mipmaps: 64 px noise, smooth"));
        bottom.addChild(tile(new TextureWidget(((CleanroomTextureHandle) noise).withOptions(TextureOptions.nearest())),
                "nearest: same noise, grainy"));
        bottom.addChild(tile(new TargetTile(), "render target: drawn offscreen each frame"));
        content.addChild(bottom);

        root.addChild(content);
        return root;
    }

    /**
     * Draws a small scene into an offscreen render target every frame and shows it as an image. Expected: a blue
     * rounded square with an orange disc, a white diagonal, a rotating line and the text "FBO", sharp and upright.
     */
    private static final class TargetTile extends WidgetBase {
        private static final int SIZE = 128;
        private final DrawList sceneList = new DrawList();
        private WidgetTextureRenderer renderer;
        private float phase;

        @Override
        public void measure(LayoutContext context) {
            setDesiredSize(resolveDesiredSize(context, TILE, TILE));
        }

        @Override
        public void tick(FrameContext frame) {
            super.tick(frame);
            phase += frame == null ? 1.0f / 60.0f : Math.max(0.0f, frame.deltaSeconds());
            invalidate(InvalidationFlags.VISUAL);
        }

        @Override
        public void render(RenderContext context) {
            if (visibility() != Visibility.VISIBLE || context == null || context.backend() == null) return;
            if (renderer == null) renderer = new WidgetTextureRenderer(context.backend());

            sceneList.clear();
            DefaultRenderContext scene = new DefaultRenderContext(sceneList).backend(context.backend());
            scene.roundedRect(0.0f, 0.0f, SIZE, SIZE, 20.0f, Paint.fill(MutableColor.rgba(0.15f, 0.35f, 0.85f, 1.0f)));
            scene.circle(14.0f, 14.0f, 56.0f, 56.0f, Paint.fill(MutableColor.rgba(1.0f, 0.6f, 0.15f, 1.0f)));
            scene.line(0.0f, 0.0f, SIZE, SIZE, Paint.stroke(MutableColor.rgba(1.0f, 1.0f, 1.0f, 1.0f), 2.0f));
            float cx = SIZE * 0.5f;
            float cy = SIZE * 0.5f;
            scene.line(cx, cy, cx + (float) Math.cos(phase * 2.0f) * 50.0f, cy + (float) Math.sin(phase * 2.0f) * 50.0f,
                    Paint.stroke(MutableColor.rgba(0.2f, 1.0f, 0.4f, 1.0f), 3.0f));
            scene.text("FBO", 40.0f, 100.0f, 60.0f, 14.0f, Paint.fill(MutableColor.rgba(1.0f, 1.0f, 1.0f, 1.0f)));

            var texture = renderer.renderDrawListToTexture(sceneList, SIZE, SIZE, RenderTargetOptions.COLOR);
            var bounds = layoutBounds();
            context.texture(texture, bounds.x(), bounds.y(), bounds.width(), bounds.height(),
                    Paint.fill(MutableColor.rgba(1.0f, 1.0f, 1.0f, 1.0f)));
        }
    }

    private static HBox row() {
        HBox row = new HBox();
        row.spacing(10.0f);
        return row;
    }

    private static Widget tile(WidgetBase image, String text) {
        image.layout(style -> style.size(TILE, TILE).flexGrow(0.0f).flexShrink(0.0f));
        VBox tile = new VBox();
        tile.spacing(4.0f);
        tile.layout(style -> style.size(LayoutConstraints.AUTO, LayoutConstraints.AUTO));
        tile.addChild(image);
        tile.addChild(caption(text));
        return tile;
    }

    private static Label caption(String text) {
        Label label = new Label(text);
        label.color(MutableColor.rgba(0.85f, 0.9f, 0.95f, 1.0f));
        return label;
    }

    /** Red and blue squares, fully opaque. */
    private static ByteBuffer checkerPixels(int size) {
        ByteBuffer rgba = ByteBuffer.allocate(size * size * 4);
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                boolean red = ((x + y) & 1) == 0;
                rgba.put((byte) (red ? 230 : 40)).put((byte) 60).put((byte) (red ? 40 : 230)).put((byte) 255);
            }
        }
        rgba.flip();
        return rgba;
    }

    /** Deterministic grey noise: enough high-frequency detail that a missing mipmap chain is visible. */
    private static ByteBuffer noisePixels(int size) {
        ByteBuffer rgba = ByteBuffer.allocate(size * size * 4);
        int seed = 12345;
        for (int i = 0; i < size * size; i++) {
            seed = seed * 1103515245 + 12345;
            int value = (seed >>> 16) & 0xFF;
            rgba.put((byte) value).put((byte) value).put((byte) value).put((byte) 255);
        }
        rgba.flip();
        return rgba;
    }
}
