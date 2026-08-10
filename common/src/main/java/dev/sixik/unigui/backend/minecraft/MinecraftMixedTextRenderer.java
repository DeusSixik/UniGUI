package dev.sixik.unigui.backend.minecraft;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.math.Transform;
import dev.sixik.unigui.api.render.DrawCommand;
import dev.sixik.unigui.api.render.DrawCommandType;
import dev.sixik.unigui.api.text.FontFace;
import dev.sixik.unigui.api.text.FontMetrics;
import dev.sixik.unigui.api.text.RichText;
import dev.sixik.unigui.api.text.TextRun;
import dev.sixik.unigui.impl.text.SdfGlyphProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

/** Preserves rich-text ordering when Minecraft and SDF faces share one text command. */
final class MinecraftMixedTextRenderer {
    private static final float VANILLA_BASE_SIZE = 9.0f;

    private final Minecraft minecraft;
    private final MinecraftSdfTextRenderer sdfRenderer;

    MinecraftMixedTextRenderer(Minecraft minecraft, MinecraftSdfTextRenderer sdfRenderer) {
        this.minecraft = minecraft;
        this.sdfRenderer = sdfRenderer;
    }

    boolean render(GuiGraphics graphics, List<DrawCommand> commands, PoseStack pose,
                   boolean renderingToPremultipliedTarget) {
        if (graphics == null || commands == null || commands.isEmpty() || pose == null) return false;
        if (!containsMinecraftFace(commands)) return false;

        List<DrawCommand> pendingSdf = new ArrayList<>();
        for (DrawCommand command : commands) {
            if (!hasText(command)) continue;
            renderCommand(graphics, command, richText(command), pendingSdf, renderingToPremultipliedTarget);
            flushSdf(graphics, pendingSdf, renderingToPremultipliedTarget);
        }
        return true;
    }

    private boolean containsMinecraftFace(List<DrawCommand> commands) {
        boolean foundMinecraft = false;
        for (DrawCommand command : commands) {
            if (!hasText(command)) continue;
            for (TextRun run : richText(command).runs()) {
                FontFace face = resolvedFace(run);
                if (face instanceof MinecraftFontFace) {
                    foundMinecraft = true;
                } else if (!(face instanceof SdfGlyphProvider)) {
                    return false;
                }
            }
        }
        return foundMinecraft;
    }

    private void renderCommand(GuiGraphics graphics, DrawCommand command, RichText text,
                               List<DrawCommand> pendingSdf, boolean renderingToPremultipliedTarget) {
        RectView bounds = command.bounds();
        List<LineInfo> lines = lineInfo(text);
        float penX = bounds.x();
        float lineTop = bounds.y();
        int lineIndex = 0;
        float baseline = lineTop + lines.get(0).ascent;

        for (TextRun run : text.runs()) {
            FontFace face = resolvedFace(run);
            FontMetrics metrics = face.metrics(run.pixelSize());
            StringBuilder segment = new StringBuilder();
            String value = run.text();
            for (int index = 0; index < value.length(); ) {
                int codePoint = value.codePointAt(index);
                index += Character.charCount(codePoint);
                if (codePoint == '\n') {
                    penX = renderSegment(graphics, command, run, face, metrics,
                            segment, penX, baseline, pendingSdf, renderingToPremultipliedTarget);
                    segment.setLength(0);
                    penX = bounds.x();
                    lineTop += lines.get(lineIndex).height;
                    lineIndex = Math.min(lineIndex + 1, lines.size() - 1);
                    baseline = lineTop + lines.get(lineIndex).ascent;
                } else {
                    segment.appendCodePoint(codePoint);
                }
            }
            renderSegment(graphics, command, run, face, metrics,
                    segment, penX, baseline, pendingSdf, renderingToPremultipliedTarget);
            penX += measure(face, segment, run.pixelSize());
        }
    }

    private float renderSegment(GuiGraphics graphics, DrawCommand command, TextRun run,
                                FontFace face, FontMetrics metrics, StringBuilder segment,
                                float x, float baseline, List<DrawCommand> pendingSdf,
                                boolean renderingToPremultipliedTarget) {
        if (segment.isEmpty()) return x;
        String value = segment.toString();
        float width = measure(face, segment, run.pixelSize());
        float top = baseline - metrics.ascent();
        if (face instanceof MinecraftFontFace minecraftFace) {
            flushSdf(graphics, pendingSdf, renderingToPremultipliedTarget);
            drawVanilla(graphics, command, value, minecraftFace, run.pixelSize(), run.color(), x, top);
        } else {
            Transform segmentTransform = command.transform().copy();
            segmentTransform.pivot().set(
                    command.bounds().x() + command.transform().pivot().x() - x,
                    command.bounds().y() + command.transform().pivot().y() - top);
            pendingSdf.add(new DrawCommand(DrawCommandType.TEXT)
                    .richText(RichText.of(value, face, run.pixelSize(), run.color()))
                    .bounds(new MutableRect(x, top, width, metrics.lineHeight()))
                    .paint(command.paint())
                    .transform(segmentTransform));
        }
        return x + width;
    }

    private void flushSdf(GuiGraphics graphics, List<DrawCommand> pendingSdf) {
        flushSdf(graphics, pendingSdf, false);
    }

    private void flushSdf(GuiGraphics graphics, List<DrawCommand> pendingSdf,
                          boolean renderingToPremultipliedTarget) {
        if (pendingSdf.isEmpty()) return;
        if (!sdfRenderer.render(graphics, pendingSdf, graphics.pose(), renderingToPremultipliedTarget)) {
            MinecraftFontFace fallback = MinecraftFonts.defaultFace();
            for (DrawCommand command : pendingSdf) {
                TextRun run = command.richText().runs().get(0);
                drawVanilla(graphics, command, run.text(), fallback, run.pixelSize(), run.color(),
                        command.bounds().x(), command.bounds().y());
            }
        }
        pendingSdf.clear();
    }

    private void drawVanilla(GuiGraphics graphics, DrawCommand command, String text,
                             MinecraftFontFace face, float pixelSize, ColorView runColor,
                             float x, float y) {
        PoseStack pose = graphics.pose();
        graphics.flush();
        boolean depthTest = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        boolean depthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        pose.pushPose();
        try {
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            applyTransform(command.bounds(), command.transform(), pose);
            float scale = Math.max(1.0f, pixelSize) / VANILLA_BASE_SIZE;
            pose.translate(x, y, 0.0f);
            pose.scale(scale, scale, 1.0f);
            Component component = Component.literal(text)
                    .withStyle(style -> style.withFont(face.location()));
            graphics.drawString(minecraft.font, component, 0, 0,
                    argb(command.paint().color(), runColor), false);
            graphics.flush();
        } finally {
            pose.popPose();
            if (depthTest) {
                RenderSystem.enableDepthTest();
            } else {
                RenderSystem.disableDepthTest();
            }
            RenderSystem.depthMask(depthMask);
        }
    }

    private RichText richText(DrawCommand command) {
        return command.richText() == null
                ? RichText.of(command.text(), sdfRenderer.defaultFace(), TextRun.DEFAULT_PIXEL_SIZE)
                : command.richText();
    }

    private static boolean hasText(DrawCommand command) {
        if (command == null) return false;
        if (command.richText() != null) return !command.richText().isEmpty();
        return command.text() != null && !command.text().isEmpty();
    }

    private FontFace resolvedFace(TextRun run) {
        return run.font() == null ? sdfRenderer.defaultFace() : run.font();
    }

    private List<LineInfo> lineInfo(RichText text) {
        List<LineInfo> lines = new ArrayList<>();
        float ascent = 0.0f;
        float height = 0.0f;
        for (TextRun run : text.runs()) {
            FontMetrics metrics = resolvedFace(run).metrics(run.pixelSize());
            ascent = Math.max(ascent, metrics.ascent());
            height = Math.max(height, metrics.lineHeight());
            String value = run.text();
            for (int index = 0; index < value.length(); ) {
                int codePoint = value.codePointAt(index);
                index += Character.charCount(codePoint);
                if (codePoint == '\n') {
                    lines.add(new LineInfo(positive(ascent), positive(height)));
                    ascent = metrics.ascent();
                    height = metrics.lineHeight();
                }
            }
        }
        lines.add(new LineInfo(positive(ascent), positive(height)));
        return lines;
    }

    private static float measure(FontFace face, StringBuilder text, float pixelSize) {
        float width = 0.0f;
        for (int index = 0; index < text.length(); ) {
            int codePoint = text.codePointAt(index);
            index += Character.charCount(codePoint);
            width += Math.max(0.0f, face.advance(codePoint, pixelSize));
        }
        return width;
    }

    private static void applyTransform(RectView bounds, Transform transform, PoseStack pose) {
        float pivotX = bounds.x() + transform.pivot().x();
        float pivotY = bounds.y() + transform.pivot().y();
        pose.translate(transform.position().x(), transform.position().y(), 0.0f);
        pose.translate(pivotX, pivotY, 0.0f);
        pose.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(transform.rotationDegrees()));
        pose.scale(transform.scale().x(), transform.scale().y(), 1.0f);
        pose.translate(-pivotX, -pivotY, 0.0f);
    }

    private static int argb(ColorView base, ColorView run) {
        float r = clamp01(base.r()) * (run == null ? 1.0f : clamp01(run.r()));
        float g = clamp01(base.g()) * (run == null ? 1.0f : clamp01(run.g()));
        float b = clamp01(base.b()) * (run == null ? 1.0f : clamp01(run.b()));
        float a = clamp01(base.a()) * (run == null ? 1.0f : clamp01(run.a()));
        return channel(a) << 24 | channel(r) << 16 | channel(g) << 8 | channel(b);
    }

    private static float positive(float value) {
        return value > 0.0f ? value : TextRun.DEFAULT_PIXEL_SIZE;
    }

    private static float clamp01(float value) {
        return Float.isFinite(value) ? Math.max(0.0f, Math.min(1.0f, value)) : 1.0f;
    }

    private static int channel(float value) {
        return Math.max(0, Math.min(255, Math.round(value * 255.0f)));
    }

    private record LineInfo(float ascent, float height) {
    }
}
