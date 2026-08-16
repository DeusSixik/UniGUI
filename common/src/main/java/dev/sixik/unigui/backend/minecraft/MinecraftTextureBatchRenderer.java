package dev.sixik.unigui.backend.minecraft;

import com.mojang.blaze3d.systems.RenderSystem;
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
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/** Batches UI images that share one texture into one position-texture-color draw call. */
final class MinecraftTextureBatchRenderer {
    private static final Logger LOGGER = LoggerFactory.getLogger(MinecraftTextureBatchRenderer.class);
    private static final float TAU = (float) (Math.PI * 2.0);

    boolean render(GuiGraphics graphics, DrawBatch batch, boolean renderingToPremultipliedTarget) {
        if (graphics == null || batch == null || batch.size() == 0) return false;
        Object[] rawCommands = batch.commandElements();
        int commandCount = batch.size();
        DrawCommand firstCommand = (DrawCommand) rawCommands[0];
        TextureHandle texture = firstCommand == null ? null : firstCommand.texture();
        if (texture == null) return false;
        for (int i = 0; i < commandCount; i++) {
            DrawCommand command = (DrawCommand) rawCommands[i];
            if (command == null || command.type() != DrawCommandType.TEXTURE
                    || command.texture() == null
                    || !sameTexture(texture, command.texture())) {
                return false;
            }
        }

        TextureBinding binding = TextureBinding.resolve(texture);
        if (binding == null) return false;

        graphics.flush();
        RenderState state = RenderState.capture();
        MinecraftTextureSamplerState.Scope sampler = null;
        try {
            RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
            binding.bind();
            sampler = MinecraftTextureSamplerState.apply(binding.options());
            RenderSystem.enableBlend();
            MinecraftUiBlend.applyTextureAlpha(binding.premultipliedAlpha(), renderingToPremultipliedTarget, batch.blendMode());
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.disableCull();

            Object buffer = MinecraftBufferCompat.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_TEX_COLOR);
            Matrix4f basePose = graphics.pose().last().pose();
            for (int i = 0; i < commandCount; i++) {
                DrawCommand command = (DrawCommand) rawCommands[i];
                Matrix4f matrix = MinecraftTransform.commandMatrix(basePose, command);
                append(buffer, matrix, command, binding.flipY());
            }
            MinecraftBufferCompat.drawWithShader(buffer);
            return true;
        } catch (Throwable failure) {
            LOGGER.error("UniGUI texture batch failed; falling back to legacy texture rendering", failure);
            return false;
        } finally {
            if (sampler != null) sampler.close();
            state.restore();
        }
    }

    private static int append(Object buffer, Matrix4f matrix,
                              DrawCommand command, boolean flipY) {
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

    private record TextureBinding(Integer textureId, ResourceLocation location, boolean flipY,
                                  boolean premultipliedAlpha, TextureOptions options) {
        private static TextureBinding resolve(TextureHandle texture) {
            TextureOptions options = texture.options() == null ? TextureOptions.defaults() : texture.options();
            Object nativeHandle = texture.nativeHandle();
            if (nativeHandle instanceof MinecraftRenderTarget.ColorTextureHandle colorTexture) {
                return new TextureBinding(colorTexture.textureId(), null, colorTexture.flipY(), true, options);
            }
            if (nativeHandle instanceof Integer textureId) {
                return new TextureBinding(textureId, null, false, options.premultipliedAlpha(), options);
            }
            ResourceLocation location = nativeHandle instanceof ResourceLocation resourceLocation
                    ? resourceLocation
                    : ResourceLocation.tryParse(texture.id());
            return location == null ? null : new TextureBinding(null, location, false, options.premultipliedAlpha(), options);
        }

        private void bind() {
            if (textureId != null) {
                RenderSystem.setShaderTexture(0, textureId);
            } else {
                RenderSystem.setShaderTexture(0, location);
            }
        }
    }

    private record RenderState(int activeTexture, int texture, boolean blend,
                               int blendSourceRgb, int blendDestinationRgb,
                               int blendSourceAlpha, int blendDestinationAlpha,
                               boolean depthTest, boolean depthMask, boolean cull) {
        private static RenderState capture() {
            int activeTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
            RenderSystem.activeTexture(GL13.GL_TEXTURE0);
            int texture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
            RenderSystem.activeTexture(activeTexture);
            return new RenderState(
                    activeTexture,
                    texture,
                    GL11.glIsEnabled(GL11.GL_BLEND),
                    GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB),
                    GL11.glGetInteger(GL14.GL_BLEND_DST_RGB),
                    GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA),
                    GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA),
                    GL11.glIsEnabled(GL11.GL_DEPTH_TEST),
                    GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK),
                    GL11.glIsEnabled(GL11.GL_CULL_FACE));
        }

        private void restore() {
            RenderSystem.activeTexture(GL13.GL_TEXTURE0);
            RenderSystem.bindTexture(texture);
            RenderSystem.blendFuncSeparate(blendSourceRgb, blendDestinationRgb,
                    blendSourceAlpha, blendDestinationAlpha);
            if (blend) RenderSystem.enableBlend();
            else RenderSystem.disableBlend();
            RenderSystem.depthMask(depthMask);
            if (depthTest) RenderSystem.enableDepthTest();
            else RenderSystem.disableDepthTest();
            if (cull) RenderSystem.enableCull();
            else RenderSystem.disableCull();
            RenderSystem.activeTexture(activeTexture);
        }
    }
}
