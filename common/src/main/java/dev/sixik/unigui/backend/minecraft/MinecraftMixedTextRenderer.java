package dev.sixik.unigui.backend.minecraft;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.math.Transform;
import dev.sixik.unigui.api.render.DrawCommand;
import dev.sixik.unigui.api.render.DrawCommandType;
import dev.sixik.unigui.api.render.TransformLayer;
import dev.sixik.unigui.api.text.FontFace;
import dev.sixik.unigui.api.text.FontMetrics;
import dev.sixik.unigui.api.text.LinearGradientTextBrush;
import dev.sixik.unigui.api.text.RichText;
import dev.sixik.unigui.api.text.SolidTextBrush;
import dev.sixik.unigui.api.text.TextBrush;
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
    private static final float VANILLA_BASE_SIZE = TextRun.DEFAULT_PIXEL_SIZE;
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
        return render(graphics, batch, pose, renderingToPremultipliedTarget, 1.0f);
    }

    boolean render(GuiGraphics graphics, DrawBatch batch, PoseStack pose,
                   boolean renderingToPremultipliedTarget, float guiScale) {
        if (graphics == null || batch == null || batch.size() == 0 || pose == null) return false;
        Object[] rawCommands = batch.commandElements();
        int commandCount = batch.size();
        if (!containsMinecraftFace(rawCommands, commandCount)) return false;

        ObjectArrayList<DrawCommand> pendingSdf = new ObjectArrayList<>();
        for (int i = 0; i < commandCount; i++) {
            DrawCommand command = (DrawCommand) rawCommands[i];
            if (!hasText(command)) continue;
            renderCommand(graphics, command, richText(command), pendingSdf, renderingToPremultipliedTarget, guiScale);
            flushSdf(graphics, pendingSdf, renderingToPremultipliedTarget, guiScale);
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
                               ObjectArrayList<DrawCommand> pendingSdf, boolean renderingToPremultipliedTarget,
                               float guiScale) {
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
                            segment, penX, baseline, pendingSdf, renderingToPremultipliedTarget, guiScale);
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
                    segment, penX, baseline, pendingSdf, renderingToPremultipliedTarget, guiScale);
            penX += measure(face, segment, run.pixelSize(), run.tracking());
        }
    }

    private float renderSegment(GuiGraphics graphics, DrawCommand command, TextRun run,
                                FontFace face, FontMetrics metrics, StringBuilder segment,
                                float x, float baseline, ObjectArrayList<DrawCommand> pendingSdf,
                                boolean renderingToPremultipliedTarget, float guiScale) {
        if (segment.isEmpty()) return x;
        String value = segment.toString();
        float width = measure(face, segment, run.pixelSize(), run.tracking());
        float top = baseline - metrics.ascent();
        if (face instanceof MinecraftFontFace minecraftFace) {
            flushSdf(graphics, pendingSdf, renderingToPremultipliedTarget, guiScale);
            drawVanilla(graphics, command, value, minecraftFace, run.pixelSize(), run.tracking(), run.color(),
                    run.brush(), command.bounds(), x, top, guiScale);
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
                            .brush(run.brush())
                            .tracking(run.tracking())
                            .append(value)
                            .build())
                    .bounds(new MutableRect(x, top, width, metrics.lineHeight()))
                    .paint(command.paint())
                    .transformStack(command.transformStack())
                    .transform(segmentTransform)
                    .textPixelSnap(command.textPixelSnap()));
        }
        return x + width;
    }

    private void flushSdf(GuiGraphics graphics, ObjectArrayList<DrawCommand> pendingSdf) {
        flushSdf(graphics, pendingSdf, false, 1.0f);
    }

    private void flushSdf(GuiGraphics graphics, ObjectArrayList<DrawCommand> pendingSdf,
                          boolean renderingToPremultipliedTarget, float guiScale) {
        if (pendingSdf.isEmpty()) return;
        if (!sdfRenderer.render(graphics, pendingSdf, graphics.pose(), renderingToPremultipliedTarget, guiScale)) {
            MinecraftFontFace fallback = MinecraftFonts.defaultFace();
            Object[] rawPendingSdf = pendingSdf.elements();
            for (int i = 0, size = pendingSdf.size(); i < size; i++) {
                DrawCommand command = (DrawCommand) rawPendingSdf[i];
                TextRun run = command.richText().runs().get(0);
                drawVanilla(graphics, command, run.text(), fallback, run.pixelSize(), run.tracking(), run.color(),
                        run.brush(), command.bounds(), command.bounds().x(), command.bounds().y(), guiScale);
            }
        }
        pendingSdf.clear();
    }

    private void drawVanilla(GuiGraphics graphics, DrawCommand command, String text,
                             MinecraftFontFace face, float pixelSize, float tracking, ColorView runColor,
                             TextBrush brush, RectView brushBounds, float x, float y, float guiScale) {
        if (!visibleAlpha(command.paint().color(), runColor, brush)) return;
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
            float drawX = shouldPixelSnap(command, pixelSize) ? snapLocalX(x, matrix, guiScale) : x;
            float drawY = shouldPixelSnap(command, pixelSize) ? snapLocalY(y, matrix, guiScale) : y;
            pose.translate(drawX, drawY, 0.0f);
            pose.scale(scale, scale, 1.0f);
            if (tracking <= 0.0f && !(brush instanceof LinearGradientTextBrush)) {
                int color = argb(command.paint().color(), runColor, brush, brushBounds, x, y + pixelSize * 0.5f);
                Component component = Component.literal(text)
                        .withStyle(style -> style.withFont(face.location()));
                graphics.drawString(minecraft.font, component, 0, 0, color, false);
            } else {
                float localX = 0.0f;
                float penUiX = x;
                int glyphIndex = 0;
                float invScale = scale <= 0.0f ? 1.0f : 1.0f / scale;
                float trackingUi = Math.max(0.0f, tracking) * pixelSize;
                float trackingLocal = trackingUi * invScale;
                for (int index = 0; index < text.length(); ) {
                    int codePoint = text.codePointAt(index);
                    index += Character.charCount(codePoint);
                    if (glyphIndex > 0) {
                        localX += trackingLocal;
                        penUiX += trackingUi;
                    }
                    float advance = Math.max(0.0f, face.advance(codePoint, pixelSize));
                    int color = argb(command.paint().color(), runColor, brush, brushBounds,
                            penUiX + advance * 0.5f, y + pixelSize * 0.5f);
                    String glyph = new String(Character.toChars(codePoint));
                    Component component = Component.literal(glyph)
                            .withStyle(style -> style.withFont(face.location()));
                    graphics.drawString(minecraft.font, component, Math.round(localX), 0, color, false);
                    localX += advance * invScale;
                    penUiX += advance;
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
        return argb(base, run, null, null, 0.0f, 0.0f);
    }

    private static int argb(ColorView base, ColorView run, TextBrush brush, RectView bounds, float x, float y) {
        float baseAlpha = base == null ? 1.0f : clamp01(base.a());
        float runAlpha = run == null ? 1.0f : clamp01(run.a());
        float r;
        float g;
        float b;
        float a = baseAlpha * runAlpha;
        if (brush instanceof SolidTextBrush solid) {
            ColorView color = solid.color();
            r = clamp01(color.r());
            g = clamp01(color.g());
            b = clamp01(color.b());
            a *= clamp01(color.a());
        } else if (brush instanceof LinearGradientTextBrush gradient) {
            float t = gradient.factor(x, y, bounds);
            ColorView start = gradient.startColor();
            ColorView end = gradient.endColor();
            r = lerp(clamp01(start.r()), clamp01(end.r()), t);
            g = lerp(clamp01(start.g()), clamp01(end.g()), t);
            b = lerp(clamp01(start.b()), clamp01(end.b()), t);
            a *= lerp(clamp01(start.a()), clamp01(end.a()), t);
        } else {
            r = (base == null ? 1.0f : clamp01(base.r())) * (run == null ? 1.0f : clamp01(run.r()));
            g = (base == null ? 1.0f : clamp01(base.g())) * (run == null ? 1.0f : clamp01(run.g()));
            b = (base == null ? 1.0f : clamp01(base.b())) * (run == null ? 1.0f : clamp01(run.b()));
        }
        return channel(a) << 24 | channel(r) << 16 | channel(g) << 8 | channel(b);
    }

    private static boolean visibleAlpha(ColorView base, ColorView run) {
        return visibleAlpha(base, run, null);
    }

    private static boolean visibleAlpha(ColorView base, ColorView run, TextBrush brush) {
        float alpha = base == null ? 1.0f : clamp01(base.a());
        alpha *= run == null ? 1.0f : clamp01(run.a());
        if (brush instanceof SolidTextBrush solid) {
            alpha *= clamp01(solid.color().a());
        } else if (brush instanceof LinearGradientTextBrush gradient) {
            alpha *= Math.max(clamp01(gradient.startColor().a()), clamp01(gradient.endColor().a()));
        }
        return channel(alpha) >= MIN_VANILLA_ALPHA_CHANNEL;
    }

    private static float lerp(float from, float to, float t) {
        return from + (to - from) * t;
    }

    private static float positive(float value) {
        return value > 0.0f ? value : TextRun.DEFAULT_PIXEL_SIZE;
    }

    private static boolean shouldPixelSnap(DrawCommand command, float pixelSize) {
        return (command == null || command.textPixelSnap())
                && !hasVisualTransform(command)
                && Float.isFinite(pixelSize)
                && pixelSize > 0.0f
                && pixelSize <= PIXEL_SNAP_MAX_SIZE;
    }

    private static boolean hasVisualTransform(DrawCommand command) {
        if (command == null) return false;
        if (hasVisualTransform(command.transform())) return true;
        Object[] rawLayers = command.transformStackElements();
        for (int i = 0, size = command.transformStackSize(); i < size; i++) {
            Object rawLayer = rawLayers[i];
            if (rawLayer instanceof TransformLayer layer && hasVisualTransform(layer.transform())) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasVisualTransform(Transform transform) {
        if (transform == null) return false;
        return Math.abs(transform.position().x()) > MATRIX_EPSILON
                || Math.abs(transform.position().y()) > MATRIX_EPSILON
                || Math.abs(transform.rotationDegrees()) > MATRIX_EPSILON
                || Math.abs(transform.scale().x() - 1.0f) > MATRIX_EPSILON
                || Math.abs(transform.scale().y() - 1.0f) > MATRIX_EPSILON;
    }

    private static float snapLocalX(float x, Matrix4f matrix, float guiScale) {
        if (matrix == null || Math.abs(matrix.m10()) > MATRIX_EPSILON
                || Math.abs(matrix.m00()) <= MATRIX_EPSILON) {
            return x;
        }
        float scale = sanitizeScale(guiScale);
        float screen = x * matrix.m00() + matrix.m30();
        return (float) (((Math.round(screen * scale) / scale) - matrix.m30()) / matrix.m00());
    }

    private static float snapLocalY(float y, Matrix4f matrix, float guiScale) {
        if (matrix == null || Math.abs(matrix.m01()) > MATRIX_EPSILON
                || Math.abs(matrix.m11()) <= MATRIX_EPSILON) {
            return y;
        }
        float scale = sanitizeScale(guiScale);
        float screen = y * matrix.m11() + matrix.m31();
        return (float) (((Math.round(screen * scale) / scale) - matrix.m31()) / matrix.m11());
    }

    private static float sanitizeScale(float value) {
        return Float.isFinite(value) && value > 0.0f ? value : 1.0f;
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
