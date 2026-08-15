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
import dev.sixik.unigui.impl.render.DrawBatch;
import dev.sixik.unigui.impl.text.SdfGlyphProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;

/** Preserves rich-text ordering when Minecraft and SDF faces share one text command. */
final class MinecraftMixedTextRenderer {
    private static final float VANILLA_BASE_SIZE = 9.0f;
    private static final float PIXEL_SNAP_MAX_SIZE = 18.0f;
    private static final float MATRIX_EPSILON = 0.0001f;
    /**
     * Vanilla Font treats colors with the high alpha bits cleared as "alpha not specified"
     * and promotes them to opaque, so skip those startup fade values instead of flashing.
     */
    private static final int MIN_VANILLA_ALPHA_CHANNEL = 4;

    private final Minecraft minecraft;
    private final MinecraftSdfTextRenderer sdfRenderer;

    MinecraftMixedTextRenderer(Minecraft minecraft, MinecraftSdfTextRenderer sdfRenderer) {
        this.minecraft = minecraft;
        this.sdfRenderer = sdfRenderer;
    }

    boolean render(GuiGraphics graphics, DrawBatch batch, PoseStack pose,
                   boolean renderingToPremultipliedTarget) {
        if (graphics == null || batch == null || batch.size() == 0 || pose == null) return false;
        Object[] rawCommands = batch.commandElements();
        int commandCount = batch.size();
        if (!containsMinecraftFace(rawCommands, commandCount)) return false;

        ObjectArrayList<DrawCommand> pendingSdf = new ObjectArrayList<>();
        for (int i = 0; i < commandCount; i++) {
            DrawCommand command = (DrawCommand) rawCommands[i];
            if (!hasText(command)) continue;
            renderCommand(graphics, command, richText(command), pendingSdf, renderingToPremultipliedTarget);
            flushSdf(graphics, pendingSdf, renderingToPremultipliedTarget);
        }
        return true;
    }

    private boolean containsMinecraftFace(Object[] rawCommands, int commandCount) {
        boolean foundMinecraft = false;
        for (int i = 0; i < commandCount; i++) {
            DrawCommand command = (DrawCommand) rawCommands[i];
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
                               ObjectArrayList<DrawCommand> pendingSdf, boolean renderingToPremultipliedTarget) {
        if (!visibleAlpha(command.paint().color(), null)) return;
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
            penX += measure(face, segment, run.pixelSize(), run.tracking());
        }
    }

    private float renderSegment(GuiGraphics graphics, DrawCommand command, TextRun run,
                                FontFace face, FontMetrics metrics, StringBuilder segment,
                                float x, float baseline, ObjectArrayList<DrawCommand> pendingSdf,
                                boolean renderingToPremultipliedTarget) {
        if (segment.isEmpty()) return x;
        String value = segment.toString();
        float width = measure(face, segment, run.pixelSize(), run.tracking());
        float top = baseline - metrics.ascent();
        if (face instanceof MinecraftFontFace minecraftFace) {
            flushSdf(graphics, pendingSdf, renderingToPremultipliedTarget);
            drawVanilla(graphics, command, value, minecraftFace, run.pixelSize(), run.tracking(), run.color(), x, top);
        } else {
            Transform segmentTransform = command.transform().copy();
            segmentTransform.pivot().set(
                    command.bounds().x() + command.transform().pivot().x() - x,
                    command.bounds().y() + command.transform().pivot().y() - top);
            pendingSdf.add(new DrawCommand(DrawCommandType.TEXT)
                    .richText(RichText.builder()
                            .font(face)
                            .size(run.pixelSize())
                            .color(run.color())
                            .tracking(run.tracking())
                            .append(value)
                            .build())
                    .bounds(new MutableRect(x, top, width, metrics.lineHeight()))
                    .paint(command.paint())
                    .transformStack(command.transformStack())
                    .transform(segmentTransform));
        }
        return x + width;
    }

    private void flushSdf(GuiGraphics graphics, ObjectArrayList<DrawCommand> pendingSdf) {
        flushSdf(graphics, pendingSdf, false);
    }

    private void flushSdf(GuiGraphics graphics, ObjectArrayList<DrawCommand> pendingSdf,
                          boolean renderingToPremultipliedTarget) {
        if (pendingSdf.isEmpty()) return;
        if (!sdfRenderer.render(graphics, pendingSdf, graphics.pose(), renderingToPremultipliedTarget)) {
            MinecraftFontFace fallback = MinecraftFonts.defaultFace();
            Object[] rawPendingSdf = pendingSdf.elements();
            for (int i = 0, size = pendingSdf.size(); i < size; i++) {
                DrawCommand command = (DrawCommand) rawPendingSdf[i];
                TextRun run = command.richText().runs().get(0);
                drawVanilla(graphics, command, run.text(), fallback, run.pixelSize(), run.tracking(), run.color(),
                        command.bounds().x(), command.bounds().y());
            }
        }
        pendingSdf.clear();
    }

    private void drawVanilla(GuiGraphics graphics, DrawCommand command, String text,
                             MinecraftFontFace face, float pixelSize, float tracking, ColorView runColor,
                             float x, float y) {
        if (!visibleAlpha(command.paint().color(), runColor)) return;
        PoseStack pose = graphics.pose();
        graphics.flush();
        boolean depthTest = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        boolean depthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        pose.pushPose();
        try {
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            MinecraftTransform.apply(command, pose);
            float scale = Math.max(1.0f, pixelSize) / VANILLA_BASE_SIZE;
            Matrix4f matrix = pose.last().pose();
            float drawX = shouldPixelSnap(command, pixelSize) ? snapLocalX(x, matrix) : x;
            float drawY = shouldPixelSnap(command, pixelSize) ? snapLocalY(y, matrix) : y;
            pose.translate(drawX, drawY, 0.0f);
            pose.scale(scale, scale, 1.0f);
            int color = argb(command.paint().color(), runColor);
            if (tracking <= 0.0f) {
                Component component = Component.literal(text)
                        .withStyle(style -> style.withFont(face.location()));
                graphics.drawString(minecraft.font, component, 0, 0, color, false);
            } else {
                float localX = 0.0f;
                int glyphIndex = 0;
                float invScale = scale <= 0.0f ? 1.0f : 1.0f / scale;
                float trackingLocal = Math.max(0.0f, tracking) * pixelSize * invScale;
                for (int index = 0; index < text.length(); ) {
                    int codePoint = text.codePointAt(index);
                    index += Character.charCount(codePoint);
                    if (glyphIndex > 0) localX += trackingLocal;
                    String glyph = new String(Character.toChars(codePoint));
                    Component component = Component.literal(glyph)
                            .withStyle(style -> style.withFont(face.location()));
                    graphics.drawString(minecraft.font, component, Math.round(localX), 0, color, false);
                    localX += Math.max(0.0f, face.advance(codePoint, pixelSize)) * invScale;
                    glyphIndex++;
                }
            }
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
        List<LineInfo> lines = new ObjectArrayList<>();
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

    private static float measure(FontFace face, StringBuilder text, float pixelSize, float tracking) {
        float width = 0.0f;
        int glyphs = 0;
        for (int index = 0; index < text.length(); ) {
            int codePoint = text.codePointAt(index);
            index += Character.charCount(codePoint);
            if (glyphs > 0) width += Math.max(0.0f, tracking) * pixelSize;
            width += Math.max(0.0f, face.advance(codePoint, pixelSize));
            glyphs++;
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

    private static boolean visibleAlpha(ColorView base, ColorView run) {
        if (base == null) return true;
        float alpha = clamp01(base.a()) * (run == null ? 1.0f : clamp01(run.a()));
        return channel(alpha) >= MIN_VANILLA_ALPHA_CHANNEL;
    }

    private static float positive(float value) {
        return value > 0.0f ? value : TextRun.DEFAULT_PIXEL_SIZE;
    }

    private static boolean shouldPixelSnap(DrawCommand command, float pixelSize) {
        return (command == null || command.textPixelSnap())
                && Float.isFinite(pixelSize)
                && pixelSize > 0.0f
                && pixelSize <= PIXEL_SNAP_MAX_SIZE;
    }

    private static float snapLocalX(float x, Matrix4f matrix) {
        if (matrix == null || Math.abs(matrix.m10()) > MATRIX_EPSILON
                || Math.abs(matrix.m00()) <= MATRIX_EPSILON) {
            return x;
        }
        return (float) ((Math.round(x * matrix.m00() + matrix.m30()) - matrix.m30()) / matrix.m00());
    }

    private static float snapLocalY(float y, Matrix4f matrix) {
        if (matrix == null || Math.abs(matrix.m01()) > MATRIX_EPSILON
                || Math.abs(matrix.m11()) <= MATRIX_EPSILON) {
            return y;
        }
        return (float) ((Math.round(y * matrix.m11() + matrix.m31()) - matrix.m31()) / matrix.m11());
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
