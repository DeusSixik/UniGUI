package dev.sixik.unigui.tests;

import dev.sixik.unigui.api.core.FrameContext;
import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.posteffect.UiPostEffectChain;
import dev.sixik.unigui.api.posteffect.UiPostEffectPass;
import dev.sixik.unigui.api.posteffect.UiPostEffectUniforms;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.render.UiRenderPolicy;
import dev.sixik.unigui.api.render.shaders.ShaderHandle;
import dev.sixik.unigui.api.text.RichText;
import dev.sixik.unigui.backend.minecraft.MinecraftClipboardService;
import dev.sixik.unigui.backend.minecraft.MinecraftFonts;
import dev.sixik.unigui.backend.minecraft.MinecraftWidgetScreen;
import dev.sixik.unigui.impl.core.DefaultUIContext;
import dev.sixik.unigui.impl.widget.WidgetBase;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Простой demo-экран старого DOS-терминала с эффектом пузатого CRT-экрана.
 *
 * <p>Класс лежит в tests-пакете и специально не требует XML: это быстрый пример того, как обычный
 * UniGUI widget tree можно отрисовать в screen-level {@link UiPostEffectChain}.</p>
 */
public final class DosTerminalDemo {
    private static final MutableColor BLACK = MutableColor.fromHex("#020704");
    private static final MutableColor PANEL = MutableColor.rgba(0.01f, 0.055f, 0.025f, 0.95f);
    private static final MutableColor PANEL_INNER = MutableColor.rgba(0.0f, 0.11f, 0.04f, 0.30f);
    private static final MutableColor GREEN = MutableColor.fromHex("#5EFF8A");
    private static final MutableColor GREEN_DIM = MutableColor.rgba(0.25f, 1.0f, 0.48f, 0.38f);
    private static final MutableColor GREEN_FAINT = MutableColor.rgba(0.25f, 1.0f, 0.48f, 0.045f);
    private static final MutableColor AMBER = MutableColor.fromHex("#F6C75F");
    private static final MutableColor RED = MutableColor.fromHex("#FF5669");

    private static final ShaderHandle CRT_SCREEN_SHADER = ShaderHandle.fragmentSource(
            "unigui:demo_dos_terminal_crt_screen",
            """
            #version 150

            in vec2 uv;
            out vec4 FragColor;

            uniform sampler2D SourceTexture;
            uniform vec2 SourceSize;
            uniform float SourceFlipY;
            uniform float Time;
            uniform float curvature;
            uniform float chromaAmount;
            uniform float scanlineAmount;
            uniform float sweepAmount;

            float hash(vec2 p) {
                p = fract(p * vec2(123.34, 456.21));
                p += dot(p, p + 45.32);
                return fract(p.x * p.y);
            }

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
                vec2 centered = sourceUv * 2.0 - 1.0;

                float r2 = dot(centered, centered);
                sourceUv += centered * r2 * curvature;
                if (sourceUv.x < 0.0 || sourceUv.x > 1.0 || sourceUv.y < 0.0 || sourceUv.y > 1.0) {
                    FragColor = vec4(0.0, 0.0, 0.0, 1.0);
                    return;
                }

                vec4 base = sampleSource(sourceUv);
                vec3 color = base.rgb;

                vec2 chroma = vec2(chromaAmount, 0.0);
                vec3 chromatic = vec3(
                        sampleSource(sourceUv + chroma).r,
                        color.g,
                        sampleSource(sourceUv - chroma).b);
                color = mix(color, chromatic, 0.12);

                float scan = 0.5 + 0.5 * sin(sourceUv.y * SourceSize.y * 3.14159);
                color *= 1.0 - scanlineAmount * smoothstep(0.58, 1.0, scan);

                float noise = hash(sourceUv * SourceSize.xy + Time * 30.0) - 0.5;
                color += noise * 0.0025;

                float phase = fract(Time * 0.18);
                float active = 1.0 - smoothstep(0.30, 0.42, phase);
                float sweepY = phase / 0.30;
                float sweepCore = active * smoothstep(0.026, 0.0, abs(sourceUv.y - sweepY));
                float sweepGlow = active * smoothstep(0.110, 0.0, abs(sourceUv.y - sweepY)) * 0.32;
                color += vec3(0.020, 0.125, 0.045) * (sweepCore + sweepGlow) * sweepAmount;
                color *= 1.0 + sweepCore * 0.08 * sweepAmount;

                float vignette = smoothstep(1.30, 0.34, length(centered * vec2(0.88, 1.08)));
                color *= 0.88 + vignette * 0.14;

                float phosphor = smoothstep(0.05, 0.42, max(max(color.r, color.g), color.b));
                color = mix(color, color * 1.06 + vec3(0.008, 0.030, 0.014), phosphor * 0.20);

                FragColor = vec4(max(color, vec3(0.0)), base.a);
            }
            """);

    private DosTerminalDemo() {
    }

    /** Открывает demo-экран на клиенте Minecraft. */
    public static void openDemoClient() {
        DefaultUIContext context = new DefaultUIContext(new MinecraftClipboardService());
        MinecraftWidgetScreen screen = new MinecraftWidgetScreen(
                Component.literal("UniGUI DOS Terminal"),
                new TerminalWidget(),
                context);
        screen.renderPolicy(UiRenderPolicy.vsync());
        screen.scaleWithMinecraftGui(true);
        screen.postEffect(crtScreenEffect());
        Minecraft.getInstance().setScreen(screen);
    }

    /** @return screen-level effect для имитации пузатого CRT-монитора */
    public static UiPostEffectChain crtScreenEffect() {
        return UiPostEffectChain.of(List.of(
                UiPostEffectPass.shader(CRT_SCREEN_SHADER).uniforms(DosTerminalDemo::configureCrtUniforms)));
    }

    private static void configureCrtUniforms(UiPostEffectUniforms uniforms) {
        uniforms.floatValue("curvature", 0.032f)
                .floatValue("chromaAmount", 0.00025f)
                .floatValue("scanlineAmount", 0.020f)
                .floatValue("sweepAmount", 1.0f);
    }

    private static final class TerminalWidget extends WidgetBase {
        private static final String[] LINES = new String[]{
                "Microsoft(R) MS-DOS(R) Version 6.22",
                "(C)Copyright Microsoft Corp 1981-1994.",
                "",
                "C:\\UNIGUI>DIR /W",
                "[SYSTEM]   [WIDGETS]  [STYLEPK]  [SHADERS]  TERMINAL.EXE",
                "",
                "C:\\UNIGUI>TYPE STATUS.LOG",
                "BOOT: OK        MEMORY: 640K        GPU: UNKNOWN",
                "STYLEPACK: ONLINE     POSTFX: CRT_SWEEP     INPUT: READY",
                "",
                "C:\\UNIGUI>SCAN /A",
                "SIGNAL NOISE.......... 17%",
                "PHOSPHOR BURN-IN...... NOMINAL",
                "FRAME SYNC............ DRIFTING",
                "",
                "C:\\UNIGUI>_"
        };

        private float timeSeconds;

        @Override
        public void measure(LayoutContext context) {
            setDesiredSize(resolveDesiredSize(context, 640.0f, 360.0f));
        }

        @Override
        public void tick(FrameContext frame) {
            tickAnimations(frame);
            float delta = frame == null || frame.deltaSeconds() <= 0.0f ? 1.0f / 60.0f : frame.deltaSeconds();
            timeSeconds += delta;
            invalidate(InvalidationFlags.VISUAL);
        }

        @Override
        public void render(RenderContext context) {
            RectView b = layoutBounds();
            float x = b.x();
            float y = b.y();
            float w = Math.max(1.0f, b.width());
            float h = Math.max(1.0f, b.height());
            float margin = Math.max(18.0f, Math.min(w, h) * 0.055f);
            float panelX = x + margin;
            float panelY = y + margin;
            float panelW = Math.max(1.0f, w - margin * 2.0f);
            float panelH = Math.max(1.0f, h - margin * 2.0f);
            float pad = Math.max(12.0f, Math.min(panelW, panelH) * 0.038f);
            float textX = panelX + pad;
            float textY = panelY + pad + 10.0f;
            float fontSize = Math.max(12.0f, Math.min(17.0f, panelH / 26.0f));
            float lineHeight = fontSize + 6.0f;

            context.rect(x, y, w, h, Paint.fill(BLACK));
            drawGlow(context, panelX, panelY, panelW, panelH);
            context.roundedRect(panelX, panelY, panelW, panelH, 18.0f, Paint.fill(PANEL));
            context.roundedRect(panelX + 8.0f, panelY + 8.0f, panelW - 16.0f, panelH - 16.0f,
                    12.0f, Paint.stroke(GREEN_DIM, 1.0f));
            context.roundedRect(panelX + 14.0f, panelY + 14.0f, panelW - 28.0f, panelH - 28.0f,
                    8.0f, Paint.fill(PANEL_INNER));

            drawGrid(context, panelX + 16.0f, panelY + 16.0f, panelW - 32.0f, panelH - 32.0f);
            drawScanlines(context, panelX + 16.0f, panelY + 16.0f, panelW - 32.0f, panelH - 32.0f);
            drawSweepBand(context, panelX + 16.0f, panelY + 16.0f, panelW - 32.0f, panelH - 32.0f);
            drawTerminalText(context, textX, textY, panelW - pad * 2.0f, fontSize, lineHeight);
            drawGlass(context, panelX, panelY, panelW, panelH);
        }

        private void drawGlow(RenderContext context, float x, float y, float w, float h) {
            context.roundedRect(x - 14.0f, y - 12.0f, w + 28.0f, h + 24.0f, 22.0f,
                    Paint.fill(MutableColor.rgba(0.06f, 0.55f, 0.18f, 0.12f)));
            context.roundedRect(x - 4.0f, y - 4.0f, w + 8.0f, h + 8.0f, 20.0f,
                    Paint.stroke(MutableColor.rgba(0.20f, 1.0f, 0.48f, 0.24f), 2.0f));
        }

        private void drawGrid(RenderContext context, float x, float y, float w, float h) {
            for (float lineX = x + 28.0f; lineX < x + w; lineX += 28.0f) {
                context.line(lineX, y, lineX, y + h, Paint.stroke(GREEN_FAINT, 1.0f));
            }
            for (float lineY = y + 18.0f; lineY < y + h; lineY += 18.0f) {
                context.line(x, lineY, x + w, lineY, Paint.stroke(GREEN_FAINT, 1.0f));
            }
        }

        private void drawScanlines(RenderContext context, float x, float y, float w, float h) {
            for (float lineY = y; lineY < y + h; lineY += 4.0f) {
                context.rect(x, lineY, w, 1.0f, Paint.fill(MutableColor.rgba(0.0f, 0.0f, 0.0f, 0.20f)));
            }
        }

        private void drawTerminalText(RenderContext context, float x, float y, float width, float fontSize, float lineHeight) {
            context.pushClip(x - 4.0f, y - 6.0f, width + 8.0f, lineHeight * (LINES.length + 1.0f));
            try {
                for (int i = 0; i < LINES.length; i++) {
                    MutableColor color = i == 0 ? AMBER : GREEN;
                    if (LINES[i].contains("DRIFTING")) color = RED;
                    float wobble = (float) Math.sin(timeSeconds * 18.0f + i * 1.7f) * 0.12f;
                    drawText(context, LINES[i], x + wobble, y + i * lineHeight, width, lineHeight, fontSize, color);
                }
                if (((int) (timeSeconds * 2.0f)) % 2 == 0) {
                    float cursorX = x + 81.0f;
                    float cursorY = y + (LINES.length - 1) * lineHeight + fontSize + 1.0f;
                    context.rect(cursorX, cursorY, 8.0f, 2.0f, Paint.fill(GREEN));
                }
            } finally {
                context.popClip();
            }
        }

        private void drawSweepBand(RenderContext context, float x, float y, float w, float h) {
            float cycle = timeSeconds % 5.5f;
            if (cycle > 1.45f) return;

            float t = cycle / 1.45f;
            float bandY = y + t * h;
            context.rect(x, bandY - 10.0f, w, 20.0f,
                    Paint.fill(MutableColor.rgba(0.20f, 1.0f, 0.42f, 0.035f)));
            context.rect(x, bandY - 2.0f, w, 4.0f,
                    Paint.fill(MutableColor.rgba(0.45f, 1.0f, 0.58f, 0.090f)));
            context.rect(x, bandY + 3.0f, w, 1.0f,
                    Paint.fill(MutableColor.rgba(0.95f, 1.0f, 0.70f, 0.055f)));
        }

        private void drawGlass(RenderContext context, float x, float y, float w, float h) {
            context.roundedRect(x + 22.0f, y + 14.0f, w * 0.36f, 10.0f, 5.0f,
                    Paint.fill(MutableColor.rgba(0.65f, 1.0f, 0.75f, 0.08f)));
            context.roundedRect(x + w * 0.63f, y + h - 22.0f, w * 0.20f, 4.0f, 2.0f,
                    Paint.fill(MutableColor.rgba(0.65f, 1.0f, 0.75f, 0.06f)));
        }

        private void drawText(RenderContext context, String text, float x, float y, float width, float height,
                              float fontSize, MutableColor color) {
            RichText richText = RichText.of(text, MinecraftFonts.defaultFace(), fontSize, color);
            context.text(richText, x + 1.0f, y + 1.0f, width, height,
                    Paint.fill(MutableColor.rgba(0.02f, 0.0f, 0.0f, 0.40f)));
            context.text(richText, x, y, width, height, Paint.fill(color));
        }

        private static float noise(int seed, int index) {
            int value = seed * 1103515245 + index * 12345 + 0x2D31;
            value ^= value >>> 16;
            return (value & 0xFFFF) / 65535.0f;
        }
    }
}