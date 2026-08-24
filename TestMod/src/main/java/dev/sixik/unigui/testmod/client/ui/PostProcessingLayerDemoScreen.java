package dev.sixik.unigui.testmod.client.ui;

import dev.sixik.unigui.api.core.FrameContext;
import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.core.UnityLikeUIScaleProvider;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.layout.LayoutSize;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.posteffect.UiPostEffectChain;
import dev.sixik.unigui.api.posteffect.UiPostEffectPass;
import dev.sixik.unigui.api.posteffect.UiPostEffectUniforms;
import dev.sixik.unigui.api.render.DrawPoint;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.render.UiRenderPolicy;
import dev.sixik.unigui.api.render.VectorPath;
import dev.sixik.unigui.api.render.shaders.ShaderHandle;
import dev.sixik.unigui.api.text.RichText;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.backend.minecraft.MinecraftClipboardService;
import dev.sixik.unigui.backend.minecraft.MinecraftFonts;
import dev.sixik.unigui.backend.minecraft.MinecraftWidgetScreen;
import dev.sixik.unigui.impl.core.DefaultUIContext;
import dev.sixik.unigui.impl.widget.WidgetBase;
import dev.sixik.unigui.widgets.containers.PanelWidget;
import dev.sixik.unigui.widgets.effects.PostProcessingLayer;
import dev.sixik.unigui.widgets.feedback.OverlayLayer;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * TestMod-пример для проверки {@link PostProcessingLayer}.
 *
 * <p>Экран намеренно показывает две одинаковые карточки: левая рендерится через
 * {@code PostProcessingLayer}, правая — напрямую. Если слой неверно смещает draw-list,
 * clipping или mesh/path-команды, это будет видно по разъезжающимся маркерам.</p>
 */
public final class PostProcessingLayerDemoScreen {
    private static final MutableColor WHITE = MutableColor.fromHex("#EAF7FF");
    private static final MutableColor MUTED = MutableColor.rgba(0.64f, 0.75f, 0.84f, 0.82f);
    private static final MutableColor CYAN = MutableColor.fromHex("#5EE7FF");
    private static final MutableColor AMBER = MutableColor.fromHex("#F7C45A");
    private static final MutableColor RED = MutableColor.fromHex("#FF667A");
    private static final MutableColor PANEL = MutableColor.rgba(0.025f, 0.040f, 0.070f, 0.96f);
    private static final MutableColor PANEL_2 = MutableColor.rgba(0.040f, 0.070f, 0.105f, 0.94f);
    private static final MutableColor BACKGROUND_TOP = MutableColor.fromHex("#07111F");
    private static final MutableColor BACKGROUND_BOTTOM = MutableColor.fromHex("#151927");

    private static final ShaderHandle LAYER_CRT_SHADER = ShaderHandle.fragmentSource(
            "unigui_testmod:post_processing_layer_demo_crt",
            """
            #version 150

            in vec2 uv;
            out vec4 FragColor;

            uniform sampler2D SourceTexture;
            uniform vec2 SourceSize;
            uniform float SourceFlipY;
            uniform float Time;
            uniform float waveAmount;
            uniform float scanlineAmount;
            uniform float chromaAmount;

            vec2 textureUv(vec2 sourceUv) {
                vec2 result = clamp(sourceUv, vec2(0.0), vec2(1.0));
                if (SourceFlipY > 0.5) {
                    result.y = 1.0 - result.y;
                }
                return result;
            }

            vec4 sampleSource(vec2 sourceUv) {
                return texture(SourceTexture, textureUv(sourceUv));
            }

            void main() {
                vec2 sourceUv = uv * 0.5 + 0.5;
                float wave = sin(sourceUv.y * 36.0 + Time * 2.2) * waveAmount;
                sourceUv.x += wave;

                vec2 chroma = vec2(chromaAmount, 0.0);
                vec4 base = sampleSource(sourceUv);
                vec3 rgb = vec3(
                        sampleSource(sourceUv + chroma).r,
                        base.g,
                        sampleSource(sourceUv - chroma).b);

                float scan = 0.5 + 0.5 * sin(sourceUv.y * SourceSize.y * 3.14159);
                rgb *= 1.0 - scanlineAmount * smoothstep(0.50, 1.0, scan);

                float vignette = smoothstep(1.25, 0.20, length(sourceUv * 2.0 - 1.0));
                rgb *= mix(0.72, 1.04, vignette);
                rgb += vec3(0.00, 0.035, 0.055) * base.a;

                FragColor = vec4(rgb, base.a);
            }
            """);

    private PostProcessingLayerDemoScreen() {
    }

    public static void openGui() {
        DefaultUIContext context = new DefaultUIContext(new MinecraftClipboardService())
                .scaleProvider(new UnityLikeUIScaleProvider()
                        .referenceResolution(1920.0f, 1080.0f)
                        .matchBalanced()
                        .scaleRange(0.70f, 2.50f));

        DemoRoot root = new DemoRoot();
        MinecraftWidgetScreen screen = new MinecraftWidgetScreen(
                Component.literal("UniGUI PostProcessingLayer Demo"),
                new OverlayLayer(root),
                context);
        screen.renderPolicy(UiRenderPolicy.vsync());
        screen.scaleWithMinecraftGui(false);
        Minecraft.getInstance().setScreen(screen);
    }

    public static UiPostEffectChain layerEffect() {
        return UiPostEffectChain.of(List.of(
                UiPostEffectPass.shader(LAYER_CRT_SHADER).uniforms(PostProcessingLayerDemoScreen::configureLayerUniforms)));
    }

    private static void configureLayerUniforms(UiPostEffectUniforms uniforms) {
        uniforms.floatValue("waveAmount", 0.0035f)
                .floatValue("scanlineAmount", 0.060f)
                .floatValue("chromaAmount", 0.0012f);
    }

    private static final class DemoRoot extends PanelWidget {
        private final PostProcessingLayer effectLayer = new PostProcessingLayer();
        private final DemoCard effectCard = new DemoCard("PostProcessingLayer", true);
        private final DemoCard referenceCard = new DemoCard("Direct render reference", false);

        private DemoRoot() {
            effectLayer.postEffect(layerEffect());
            effectLayer.effectScale(1.0f);
            effectLayer.addChild(effectCard);
            effectLayer.applyQueuedMutations();
            addChild(effectLayer);
            addChild(referenceCard);
            applyQueuedMutations();
        }

        @Override
        public void measure(LayoutContext context) {
            if (visibility() == Visibility.COLLAPSED) {
                setDesiredSize(LayoutSize.ZERO);
                return;
            }
            applyQueuedMutations();
            LayoutContext childContext = new LayoutContext(620.0f, 420.0f);
            effectLayer.measure(childContext);
            referenceCard.measure(childContext);
            setDesiredSize(resolveDesiredSize(context, 1280.0f, 720.0f));
        }

        @Override
        public void arrange(RectView bounds) {
            mutableLayoutBounds().set(bounds);
            if (visibility() == Visibility.COLLAPSED) return;
            applyQueuedMutations();
            arrangeCards(bounds);
        }

        @Override
        public void render(RenderContext context) {
            if (context == null || visibility() != Visibility.VISIBLE) return;
            RectView b = layoutBounds();
            arrangeCards(b);

            context.addRectFilledMultiColor(b.x(), b.y(), b.width(), b.height(),
                    BACKGROUND_TOP, BACKGROUND_TOP, BACKGROUND_BOTTOM, BACKGROUND_BOTTOM);
            drawText(context, "POST PROCESSING LAYER TEST", b.x() + 42.0f, b.y() + 30.0f,
                    b.width() - 84.0f, 24.0f, 18.0f, CYAN);
            drawText(context, "Left card is rendered through PostProcessingLayer. Right card is the same widget rendered normally.",
                    b.x() + 42.0f, b.y() + 58.0f, b.width() - 84.0f, 22.0f, 12.0f, MUTED);

            referenceCard.render(context);
            effectLayer.render(context);

            RectView e = effectLayer.layoutBounds();
            context.addRect(e.x() - 4.0f, e.y() - 4.0f, e.width() + 8.0f, e.height() + 8.0f,
                    MutableColor.rgba(0.35f, 0.92f, 1.0f, 0.80f), 1.0f);
            drawText(context, "This label and outer cyan border are outside the post-effect.",
                    e.x(), e.y() + e.height() + 10.0f, e.width(), 20.0f, 11.0f, WHITE);
        }

        private void arrangeCards(RectView bounds) {
            float margin = clamp(bounds.width() * 0.040f, 18.0f, 42.0f);
            float gap = clamp(bounds.width() * 0.026f, 14.0f, 28.0f);
            float header = clamp(bounds.height() * 0.125f, 68.0f, 90.0f);
            float availableW = Math.max(1.0f, bounds.width() - margin * 2.0f);
            float availableH = Math.max(1.0f, bounds.height() - margin * 2.0f - header);
            float cardY = bounds.y() + margin + header;
            float leftX = bounds.x() + margin;

            if (availableW < 720.0f) {
                float cardW = availableW;
                float cardH = Math.max(180.0f, (availableH - gap) * 0.5f);
                effectLayer.arrange(new MutableRect(leftX, cardY, cardW, cardH));
                referenceCard.arrange(new MutableRect(leftX, cardY + cardH + gap, cardW, cardH));
                return;
            }

            float cardW = Math.max(260.0f, (availableW - gap) * 0.5f);
            float cardH = Math.max(240.0f, availableH);
            float rightX = leftX + cardW + gap;

            effectLayer.arrange(new MutableRect(leftX, cardY, cardW, cardH));
            referenceCard.arrange(new MutableRect(rightX, cardY, cardW, cardH));
        }

        private static float clamp(float value, float min, float max) {
            return Math.max(min, Math.min(max, value));
        }
    }

    private static final class DemoCard extends WidgetBase {
        private final String title;
        private final boolean processed;
        private float timeSeconds;

        private DemoCard(String title, boolean processed) {
            this.title = title;
            this.processed = processed;
        }

        @Override
        public void measure(LayoutContext context) {
            if (visibility() == Visibility.COLLAPSED) {
                setDesiredSize(LayoutSize.ZERO);
                return;
            }
            setDesiredSize(resolveDesiredSize(context, 560.0f, 380.0f));
        }

        @Override
        public void tick(FrameContext frame) {
            super.tick(frame);
            float delta = frame == null || frame.deltaSeconds() <= 0.0f ? 1.0f / 60.0f : frame.deltaSeconds();
            timeSeconds += Math.min(delta, 1.0f / 20.0f);
            invalidate(InvalidationFlags.VISUAL);
        }

        @Override
        public void render(RenderContext context) {
            if (context == null || visibility() != Visibility.VISIBLE) return;
            RectView b = layoutBounds();
            float x = b.x();
            float y = b.y();
            float w = b.width();
            float h = b.height();
            float pad = 24.0f;

            context.addRectFilled(x, y, w, h, 14.0f, PANEL);
            context.addRectFilled(x + pad, y + 74.0f, w - pad * 2.0f, h - 116.0f, 10.0f, PANEL_2);
            context.addRect(x, y, w, h, 14.0f, processed ? CYAN : MUTED, 1.5f);

            drawText(context, title, x + pad, y + 22.0f, w - pad * 2.0f, 24.0f, 16.0f, processed ? CYAN : WHITE);
            drawText(context, processed ? "effectScale=1.0 / subtree render target" : "no effect / direct root draw-list",
                    x + pad, y + 48.0f, w - pad * 2.0f, 20.0f, 11.0f, MUTED);

            drawCoordinateMarkers(context, x + pad, y + 92.0f, w - pad * 2.0f, h - 148.0f);
            drawText(context, "rect + text + line + circle + mesh triangle + vector path",
                    x + pad, y + h - 42.0f, w - pad * 2.0f, 20.0f, 11.0f, WHITE);
        }

        private void drawCoordinateMarkers(RenderContext context, float x, float y, float w, float h) {
            float cx = x + w * 0.50f;
            float cy = y + h * 0.52f;
            float pulse = 0.5f + 0.5f * (float) Math.sin(timeSeconds * 2.0f);
            MutableColor pulseColor = MutableColor.rgba(0.35f, 0.90f, 1.0f, 0.35f + pulse * 0.35f);

            context.addLine(x + 18.0f, y + 18.0f, x + w - 18.0f, y + h - 18.0f, CYAN, 2.0f);
            context.addLine(x + w - 18.0f, y + 18.0f, x + 18.0f, y + h - 18.0f, AMBER, 2.0f);
            context.addCircle(cx, cy, 52.0f + pulse * 8.0f, pulseColor, 40, 2.0f);
            context.addCircleFilled(cx, cy, 5.0f, WHITE, 16);

            DrawPoint p1 = new DrawPoint(x + w * 0.18f, y + h * 0.72f);
            DrawPoint p2 = new DrawPoint(x + w * 0.34f, y + h * 0.34f);
            DrawPoint p3 = new DrawPoint(x + w * 0.47f, y + h * 0.76f);
            context.addTriangleFilled(p1, p2, p3, MutableColor.rgba(1.0f, 0.38f, 0.50f, 0.82f));
            context.addTriangle(p1, p2, p3, RED, 1.5f);

            VectorPath path = new VectorPath()
                    .moveTo(x + w * 0.60f, y + h * 0.72f)
                    .quadraticTo(x + w * 0.70f, y + h * 0.20f, x + w * 0.83f, y + h * 0.46f)
                    .cubicTo(x + w * 0.94f, y + h * 0.68f, x + w * 0.86f, y + h * 0.86f, x + w * 0.68f, y + h * 0.80f);
            context.path(path, x + w * 0.60f, y + h * 0.20f, w * 0.34f, h * 0.66f,
                    Paint.stroke(AMBER, 3.0f));

            context.addRect(x + 14.0f, y + 14.0f, w - 28.0f, h - 28.0f, 8.0f, MutableColor.rgba(0.78f, 0.88f, 1.0f, 0.30f), 1.0f);
            drawText(context, "TOP LEFT", x + 24.0f, y + 24.0f, 100.0f, 18.0f, 11.0f, CYAN);
            drawText(context, "BOTTOM RIGHT", x + w - 138.0f, y + h - 40.0f, 118.0f, 18.0f, 11.0f, AMBER);
        }
    }

    private static void drawText(RenderContext context, String text, float x, float y, float width, float height,
                                 float size, MutableColor color) {
        context.text(RichText.of(text, MinecraftFonts.defaultFace(), size, color),
                x, y, width, height, Paint.fill(color));
    }
}
