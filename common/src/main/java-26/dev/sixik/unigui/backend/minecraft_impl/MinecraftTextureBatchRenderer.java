package dev.sixik.unigui.backend.minecraft_impl;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.math.Transform;
import dev.sixik.unigui.api.render.DrawCommand;
import dev.sixik.unigui.api.render.DrawCommandType;
import dev.sixik.unigui.api.render.TextureHandle;
import dev.sixik.unigui.api.render.TextureOptions;
import dev.sixik.unigui.impl.render.DrawBatch;
import org.joml.Matrix4f;
import dev.sixik.unigui.api.backend.UniGuiLogger;
import dev.sixik.unigui.api.backend.UniGuiRuntime;

import java.util.Objects;

/** Batches UI images that share one texture into one position-texture-color draw call. */
final class MinecraftTextureBatchRenderer {
    private static final UniGuiLogger LOGGER = UniGuiRuntime.logger(MinecraftTextureBatchRenderer.class);
    private static final float TAU = (float) (Math.PI * 2.0);
    private final UiTextureBinding.Cache textureBindings = new UiTextureBinding.Cache(256);

    boolean render(UniGuiGraphics graphics, DrawBatch batch, boolean renderingToPremultipliedTarget) {
        if (graphics == null || batch == null || batch.size() == 0) return false;
        Object[] rawCommands = batch.commandElements();
        int commandCount = batch.size();
        DrawCommand firstCommand = (DrawCommand) rawCommands[0];
        TextureHandle texture = firstCommand == null ? null : firstCommand.texture();
        if (texture == null) return false;
        for (int i = 0; i < commandCount; i++) {
            DrawCommand command = (DrawCommand) rawCommands[i];
            if (command == null || !isTextureCommand(command.type())
                    || command.texture() == null
                    || !sameTexture(texture, command.texture())) {
                return false;
            }
        }

        UiTextureBinding binding = textureBindings.resolve(texture);
        if (binding == null) return false;

        try {
            Object buffer = MinecraftBufferCompat.begin(UiPrimitive.TRIANGLES, DefaultVertexFormat.POSITION_TEX_COLOR);
            Matrix4f basePose = graphics.pose().last().pose();
            for (int i = 0; i < commandCount; i++) {
                DrawCommand command = (DrawCommand) rawCommands[i];
                Matrix4f matrix = MinecraftTransform.commandMatrix(basePose, command);
                append(buffer, matrix, command, binding.flipY());
            }
            MinecraftBufferCompat.drawTexture(buffer,
                    MinecraftUiBlend.texture(binding.premultipliedAlpha(), renderingToPremultipliedTarget, batch.blendMode()),
                    binding);
            return true;
        } catch (Throwable failure) {
            LOGGER.error("UniGUI texture batch failed; falling back to legacy texture rendering", failure);
            return false;
        }
    }

    private static int append(Object buffer, Matrix4f matrix,
                              DrawCommand command, boolean flipY) {
        if (command.type() == DrawCommandType.TEXTURED_QUAD) {
            return appendTexturedQuad(buffer, matrix, command, flipY);
        }

        RectView bounds = command.bounds();
        float width = bounds.width();
        float height = bounds.height();
        if (Math.abs(width) <= 0.0001f || Math.abs(height) <= 0.0001f) return 0;

        float x1 = Math.min(bounds.x(), bounds.x() + width);
        float y1 = Math.min(bounds.y(), bounds.y() + height);
        float x2 = Math.max(bounds.x(), bounds.x() + width);
        float y2 = Math.max(bounds.y(), bounds.y() + height);
        float radius = clamp(Math.abs(command.radius()), 0.0f,
                Math.min(x2 - x1, y2 - y1) * 0.5f);
        if (radius <= 0.5f) {
            return quad(buffer, matrix, command, flipY, x1, y1, x2, y2);
        }

        int cornerSegments = cornerSegments(radius);
        FloatPoints outline = roundedOutline(x1, y1, x2, y2, radius, cornerSegments);
        float centerX = (x1 + x2) * 0.5f;
        float centerY = (y1 + y2) * 0.5f;
        ColorView tint = command.paint().color();
        int count = outline.size();
        for (int i = 0; i < count; i++) {
            int next = (i + 1) % count;
            vertex(buffer, matrix, command, flipY, centerX, centerY, tint);
            vertex(buffer, matrix, command, flipY, outline.x(i), outline.y(i), tint);
            vertex(buffer, matrix, command, flipY, outline.x(next), outline.y(next), tint);
        }
        return count * 3;
    }

    private static int appendTexturedQuad(Object buffer, Matrix4f matrix,
                                          DrawCommand command, boolean flipY) {
        float minV = Math.min(Math.min(command.quadV1(), command.quadV2()),
                Math.min(command.quadV3(), command.quadV4()));
        float maxV = Math.max(Math.max(command.quadV1(), command.quadV2()),
                Math.max(command.quadV3(), command.quadV4()));

        float v1 = flipY ? minV + maxV - command.quadV1() : command.quadV1();
        float v2 = flipY ? minV + maxV - command.quadV2() : command.quadV2();
        float v3 = flipY ? minV + maxV - command.quadV3() : command.quadV3();
        float v4 = flipY ? minV + maxV - command.quadV4() : command.quadV4();

        quadVertex(buffer, matrix, command.quadX1(), command.quadY1(), command.quadU1(), v1, command);
        quadVertex(buffer, matrix, command.quadX2(), command.quadY2(), command.quadU2(), v2, command);
        quadVertex(buffer, matrix, command.quadX3(), command.quadY3(), command.quadU3(), v3, command);
        quadVertex(buffer, matrix, command.quadX1(), command.quadY1(), command.quadU1(), v1, command);
        quadVertex(buffer, matrix, command.quadX3(), command.quadY3(), command.quadU3(), v3, command);
        quadVertex(buffer, matrix, command.quadX4(), command.quadY4(), command.quadU4(), v4, command);
        return 6;
    }

    private static void quadVertex(Object buffer, Matrix4f matrix, float x, float y,
                                   float u, float v, DrawCommand command) {
        MinecraftBufferCompat.textureColorVertex(buffer, matrix, x, y, u, v,
                argb(command.paint().color()));
    }

    private static int quad(Object buffer, Matrix4f matrix, DrawCommand command,
                            boolean flipY, float x1, float y1, float x2, float y2) {
        ColorView tint = command.paint().color();
        vertex(buffer, matrix, command, flipY, x1, y1, tint);
        vertex(buffer, matrix, command, flipY, x1, y2, tint);
        vertex(buffer, matrix, command, flipY, x2, y2, tint);
        vertex(buffer, matrix, command, flipY, x1, y1, tint);
        vertex(buffer, matrix, command, flipY, x2, y2, tint);
        vertex(buffer, matrix, command, flipY, x2, y1, tint);
        return 6;
    }

    private static void vertex(Object buffer, Matrix4f matrix, DrawCommand command,
                               boolean flipY, float x, float y, ColorView tint) {
        RectView bounds = command.bounds();
        RectView uv = command.uv();
        float horizontal = (x - bounds.x()) / bounds.width();
        float vertical = (y - bounds.y()) / bounds.height();
        float u = uv.x() + uv.width() * horizontal;
        float v = flipY
                ? uv.y() + uv.height() * (1.0f - vertical)
                : uv.y() + uv.height() * vertical;
        MinecraftBufferCompat.textureColorVertex(buffer, matrix, x, y, u, v, argb(tint));
    }

    private static int argb(ColorView color) {
        int a = channel(color.a());
        int r = channel(color.r());
        int g = channel(color.g());
        int b = channel(color.b());
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static FloatPoints roundedOutline(float x1, float y1, float x2, float y2,
                                               float radius, int cornerSegments) {
        FloatPoints outline = new FloatPoints((cornerSegments + 1) * 4);
        appendArc(outline, x1 + radius, y1 + radius, radius,
                (float) Math.PI, (float) (Math.PI * 1.5), cornerSegments);
        appendArc(outline, x2 - radius, y1 + radius, radius,
                (float) (Math.PI * 1.5), TAU, cornerSegments);
        appendArc(outline, x2 - radius, y2 - radius, radius,
                0.0f, (float) (Math.PI * 0.5), cornerSegments);
        appendArc(outline, x1 + radius, y2 - radius, radius,
                (float) (Math.PI * 0.5), (float) Math.PI, cornerSegments);
        return outline;
    }

    private static void appendArc(FloatPoints points, float cx, float cy, float radius,
                                  float start, float end, int segments) {
        for (int i = 0; i <= segments; i++) {
            float angle = start + (end - start) * i / segments;
            points.add(cx + (float) Math.cos(angle) * radius,
                    cy + (float) Math.sin(angle) * radius);
        }
    }

    private static Matrix4f commandMatrix(Matrix4f basePose, RectView bounds, Transform transform) {
        Matrix4f matrix = new Matrix4f(basePose);
        float pivotX = bounds.x() + transform.pivot().x();
        float pivotY = bounds.y() + transform.pivot().y();
        matrix.translate(transform.position().x(), transform.position().y(), 0.0f);
        matrix.translate(pivotX, pivotY, 0.0f);
        if (transform.rotationDegrees() != 0.0f) {
            matrix.rotateZ((float) Math.toRadians(transform.rotationDegrees()));
        }
        matrix.scale(transform.scale().x(), transform.scale().y(), 1.0f);
        matrix.translate(-pivotX, -pivotY, 0.0f);
        return matrix;
    }

    private static int cornerSegments(float radius) {
        return Math.max(3, Math.min(12, Math.round(radius * 0.4f)));
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static int channel(float value) {
        return Math.max(0, Math.min(255, Math.round(value * 255.0f)));
    }

    private static boolean sameTexture(TextureHandle left, TextureHandle right) {
        if (left == right) return true;
        if (left == null || right == null) return false;
        return Objects.equals(left.id(), right.id())
                && Objects.equals(left.options(), right.options());
    }

    private static boolean isTextureCommand(DrawCommandType type) {
        return type == DrawCommandType.TEXTURE || type == DrawCommandType.TEXTURED_QUAD;
    }

    private static final class FloatPoints {
        private float[] values;
        private int size;

        private FloatPoints(int capacity) {
            values = new float[Math.max(4, capacity * 2)];
        }

        private void add(float x, float y) {
            ensure(size + 2);
            values[size++] = x;
            values[size++] = y;
        }

        private int size() {
            return size / 2;
        }

        private float x(int index) {
            return values[index * 2];
        }

        private float y(int index) {
            return values[index * 2 + 1];
        }

        private void ensure(int required) {
            if (required <= values.length) return;
            values = java.util.Arrays.copyOf(values, Math.max(required, values.length * 2));
        }
    }
}
