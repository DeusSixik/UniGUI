package dev.sixik.unigui.backend.minecraft;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.math.Transform;
import dev.sixik.unigui.api.render.DrawCommand;
import dev.sixik.unigui.api.render.DrawCommandType;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.impl.render.DrawBatch;
import net.minecraft.client.gui.GuiGraphics;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL20;
import org.lwjgl.system.MemoryStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.FloatBuffer;

/**
 * Shader path for common 2D UI primitives.
 *
 * <p>The legacy Minecraft shape renderer approximates curves with geometry, which is visible as stair-stepped
 * rounded corners/circles at normal GUI scale. This renderer draws a single quad per primitive and lets the
 * fragment shader evaluate the shape as an SDF, so edges are anti-aliased in the same spirit as ImGui/Skia.</p>
 */
final class MinecraftSdfShapeRenderer implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(MinecraftSdfShapeRenderer.class);
    private static final float AA_PAD = 1.0f;
    private static final float MIN_STROKE_WIDTH = 0.001f;

    private static final int SHAPE_ROUNDED_RECT = 0;
    private static final int SHAPE_ELLIPSE = 1;
    private static final int SHAPE_LINE = 2;

    private static final String VERTEX_SOURCE = """
            #version 150

            in vec3 Position;
            in vec2 UV0;

            uniform mat4 ModelViewMat;
            uniform mat4 ProjMat;

            out vec2 localPos;

            void main() {
                gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
                localPos = UV0;
            }
            """;

    private static final String FRAGMENT_SOURCE = """
            #version 150

            in vec2 localPos;
            out vec4 fragColor;

            uniform vec4 Color;
            uniform vec2 Size;
            uniform vec2 LineStart;
            uniform vec2 LineEnd;
            uniform float Radius;
            uniform float StrokeWidth;
            uniform int ShapeType;
            uniform int IsStroke;

            float sdRoundedBox(vec2 point, vec2 halfSize, float radius) {
                float safeRadius = clamp(radius, 0.0, min(halfSize.x, halfSize.y));
                vec2 q = abs(point) - halfSize + vec2(safeRadius);
                return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - safeRadius;
            }

            float sdCircle(vec2 point, float radius) {
                return length(point) - max(radius, 0.0001);
            }

            float sdEllipse(vec2 point, vec2 radius) {
                vec2 safeRadius = max(radius, vec2(0.0001));
                vec2 normalized = point / safeRadius;
                return (length(normalized) - 1.0) * min(safeRadius.x, safeRadius.y);
            }

            float sdSegment(vec2 point, vec2 a, vec2 b) {
                vec2 pa = point - a;
                vec2 ba = b - a;
                float denom = max(dot(ba, ba), 0.0001);
                float h = clamp(dot(pa, ba) / denom, 0.0, 1.0);
                return length(pa - ba * h);
            }

            float aaWidth(float distanceToEdge) {
                return max(fwidth(distanceToEdge), 0.0001);
            }

            float edgeCoverage(float distanceToEdge) {
                float aa = aaWidth(distanceToEdge);
                return clamp(0.5 - distanceToEdge / aa, 0.0, 1.0);
            }

            float fillCoverage(float distanceToEdge) {
                return edgeCoverage(distanceToEdge);
            }

            float innerStrokeCoverage(float distanceToEdge, float width) {
                float aa = aaWidth(distanceToEdge);
                float outer = clamp(0.5 - distanceToEdge / aa, 0.0, 1.0);
                float inner = clamp(0.5 - (-distanceToEdge - width) / aa, 0.0, 1.0);
                return outer * inner;
            }

            void main() {
                float distanceToEdge;
                if (ShapeType == 1) {
                    vec2 radius = Size * 0.5;
                    vec2 point = localPos - radius;
                    distanceToEdge = abs(Size.x - Size.y) <= 0.001
                            ? sdCircle(point, radius.x)
                            : sdEllipse(point, radius);
                } else if (ShapeType == 2) {
                    distanceToEdge = sdSegment(localPos, LineStart, LineEnd) - max(StrokeWidth, 0.0001) * 0.5;
                } else {
                    distanceToEdge = sdRoundedBox(localPos - Size * 0.5, Size * 0.5, Radius);
                }

                float coverage = (ShapeType == 2 || IsStroke == 0)
                        ? fillCoverage(distanceToEdge)
                        : innerStrokeCoverage(distanceToEdge, max(StrokeWidth, 0.0001));

                float alpha = Color.a * coverage;
                if (alpha <= 0.001) {
                    discard;
                }

                fragColor = vec4(Color.rgb, alpha);
            }
            """;

    private int program;
    private boolean unavailable;

    boolean shouldRender(DrawBatch batch) {
        if (batch == null || batch.size() == 0) return false;

        boolean needsSdf = false;
        Object[] rawCommands = batch.commandElements();
        for (int i = 0, size = batch.size(); i < size; i++) {
            DrawCommand command = (DrawCommand) rawCommands[i];
            if (command == null || !supports(command.type())) {
                return false;
            }
            needsSdf |= needsSdf(command);
        }
        return needsSdf;
    }

    boolean render(GuiGraphics graphics, DrawBatch batch, boolean renderingToPremultipliedTarget) {
        if (graphics == null || batch == null || batch.size() == 0 || unavailable) return false;
        if (!shouldRender(batch)) return false;

        int activeProgram = program();
        if (activeProgram == 0) return false;

        graphics.flush();
        RenderState state = RenderState.capture();
        try {
            GL20.glUseProgram(activeProgram);
            uploadMat4(activeProgram, "ModelViewMat", RenderSystem.getModelViewMatrix());
            uploadMat4(activeProgram, "ProjMat", RenderSystem.getProjectionMatrix());

            RenderSystem.enableBlend();
            MinecraftUiBlend.applyStraightAlpha(renderingToPremultipliedTarget);
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.disableCull();

            Object[] rawCommands = batch.commandElements();
            for (int i = 0, size = batch.size(); i < size; i++) {
                DrawCommand command = (DrawCommand) rawCommands[i];
                renderCommand(graphics, activeProgram, command);
            }
            return true;
        } catch (Throwable failure) {
            LOGGER.error("UniGUI SDF shape renderer failed; falling back to legacy primitive rendering", failure);
            return false;
        } finally {
            state.restore();
        }
    }

    private void renderCommand(GuiGraphics graphics, int activeProgram, DrawCommand command) {
        switch (command.type()) {
            case RECT -> renderRectLike(graphics, activeProgram, command, 0.0f);
            case ROUNDED_RECT -> renderRectLike(graphics, activeProgram, command, command.radius());
            case CIRCLE -> renderEllipse(graphics, activeProgram, command);
            case LINE -> renderLine(graphics, activeProgram, command);
            default -> {
            }
        }
    }

    private void renderRectLike(GuiGraphics graphics, int activeProgram, DrawCommand command, float requestedRadius) {
        RectView bounds = command.bounds();
        float x1 = Math.min(bounds.x(), bounds.x() + bounds.width());
        float y1 = Math.min(bounds.y(), bounds.y() + bounds.height());
        float x2 = Math.max(bounds.x(), bounds.x() + bounds.width());
        float y2 = Math.max(bounds.y(), bounds.y() + bounds.height());
        float width = x2 - x1;
        float height = y2 - y1;
        if (width <= 0.0f || height <= 0.0f) return;

        float radius = clamp(Math.abs(requestedRadius), 0.0f, Math.min(width, height) * 0.5f);
        uploadCommon(activeProgram, command.paint(), SHAPE_ROUNDED_RECT, width, height, radius,
                positiveThickness(command.paint().strokeWidth()), command.paint().isStroke());
        uploadVec2(activeProgram, "LineStart", 0.0f, 0.0f);
        uploadVec2(activeProgram, "LineEnd", 0.0f, 0.0f);

        float pad = AA_PAD;
        Matrix4f matrix = MinecraftTransform.commandMatrix(graphics.pose().last().pose(), command);
        drawQuad(matrix, x1 - pad, y1 - pad, x2 + pad, y2 + pad,
                -pad, -pad, width + pad, height + pad);
    }

    private void renderEllipse(GuiGraphics graphics, int activeProgram, DrawCommand command) {
        RectView bounds = command.bounds();
        float x1 = Math.min(bounds.x(), bounds.x() + bounds.width());
        float y1 = Math.min(bounds.y(), bounds.y() + bounds.height());
        float x2 = Math.max(bounds.x(), bounds.x() + bounds.width());
        float y2 = Math.max(bounds.y(), bounds.y() + bounds.height());
        float width = x2 - x1;
        float height = y2 - y1;
        if (width <= 0.0f || height <= 0.0f) return;

        uploadCommon(activeProgram, command.paint(), SHAPE_ELLIPSE, width, height, 0.0f,
                positiveThickness(command.paint().strokeWidth()), command.paint().isStroke());
        uploadVec2(activeProgram, "LineStart", 0.0f, 0.0f);
        uploadVec2(activeProgram, "LineEnd", 0.0f, 0.0f);

        float pad = AA_PAD;
        Matrix4f matrix = MinecraftTransform.commandMatrix(graphics.pose().last().pose(), command);
        drawQuad(matrix, x1 - pad, y1 - pad, x2 + pad, y2 + pad,
                -pad, -pad, width + pad, height + pad);
    }

    private void renderLine(GuiGraphics graphics, int activeProgram, DrawCommand command) {
        RectView bounds = command.bounds();
        float dx = bounds.width();
        float dy = bounds.height();
        float thickness = positiveThickness(command.paint().strokeWidth());
        float minX = Math.min(0.0f, dx);
        float minY = Math.min(0.0f, dy);
        float maxX = Math.max(0.0f, dx);
        float maxY = Math.max(0.0f, dy);
        float pad = thickness * 0.5f + AA_PAD;

        uploadCommon(activeProgram, command.paint(), SHAPE_LINE, Math.max(1.0f, maxX - minX),
                Math.max(1.0f, maxY - minY), 0.0f, thickness, false);
        uploadVec2(activeProgram, "LineStart", 0.0f, 0.0f);
        uploadVec2(activeProgram, "LineEnd", dx, dy);

        Matrix4f matrix = MinecraftTransform.commandMatrix(graphics.pose().last().pose(), command);
        drawQuad(matrix,
                bounds.x() + minX - pad,
                bounds.y() + minY - pad,
                bounds.x() + maxX + pad,
                bounds.y() + maxY + pad,
                minX - pad,
                minY - pad,
                maxX + pad,
                maxY + pad);
    }

    private void uploadCommon(int activeProgram, Paint paint, int shapeType, float width, float height,
                              float radius, float strokeWidth, boolean stroke) {
        ColorView color = paint.color();
        uploadVec4(activeProgram, "Color", color.r(), color.g(), color.b(), color.a());
        uploadVec2(activeProgram, "Size", width, height);
        uploadFloat(activeProgram, "Radius", radius);
        uploadFloat(activeProgram, "StrokeWidth", strokeWidth);
        uploadInt(activeProgram, "ShapeType", shapeType);
        uploadInt(activeProgram, "IsStroke", stroke ? 1 : 0);
    }

    private void drawQuad(Matrix4f matrix,
                          float x1, float y1, float x2, float y2,
                          float u1, float v1, float u2, float v2) {
        Object buffer = MinecraftBufferCompat.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        vertex(buffer, matrix, x1, y1, u1, v1);
        vertex(buffer, matrix, x1, y2, u1, v2);
        vertex(buffer, matrix, x2, y2, u2, v2);
        vertex(buffer, matrix, x2, y1, u2, v1);
        MinecraftBufferCompat.draw(buffer);
    }

    private static void vertex(Object buffer, Matrix4f matrix, float x, float y, float u, float v) {
        MinecraftBufferCompat.textureVertex(buffer, matrix, x, y, u, v);
    }

    private int program() {
        if (program != 0 || unavailable) return program;

        int vertex = compileShader(GL20.GL_VERTEX_SHADER, VERTEX_SOURCE);
        int fragment = compileShader(GL20.GL_FRAGMENT_SHADER, FRAGMENT_SOURCE);
        if (vertex == 0 || fragment == 0) {
            if (vertex != 0) GL20.glDeleteShader(vertex);
            if (fragment != 0) GL20.glDeleteShader(fragment);
            unavailable = true;
            return 0;
        }

        int id = GL20.glCreateProgram();
        GL20.glAttachShader(id, vertex);
        GL20.glAttachShader(id, fragment);
        GL20.glBindAttribLocation(id, 0, "Position");
        GL20.glBindAttribLocation(id, 1, "UV0");
        GL20.glLinkProgram(id);
        GL20.glDeleteShader(vertex);
        GL20.glDeleteShader(fragment);

        if (GL20.glGetProgrami(id, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
            LOGGER.error("Failed to link UniGUI SDF shape shader: {}", GL20.glGetProgramInfoLog(id));
            GL20.glDeleteProgram(id);
            unavailable = true;
            return 0;
        }

        program = id;
        return program;
    }

    private int compileShader(int type, String source) {
        int shader = GL20.glCreateShader(type);
        GL20.glShaderSource(shader, source);
        GL20.glCompileShader(shader);
        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            LOGGER.error("Failed to compile UniGUI SDF {} shader: {}",
                    type == GL20.GL_VERTEX_SHADER ? "vertex" : "fragment",
                    GL20.glGetShaderInfoLog(shader));
            GL20.glDeleteShader(shader);
            return 0;
        }
        return shader;
    }

    private static boolean supports(DrawCommandType type) {
        return type == DrawCommandType.RECT
                || type == DrawCommandType.ROUNDED_RECT
                || type == DrawCommandType.LINE
                || type == DrawCommandType.CIRCLE;
    }

    private static boolean needsSdf(DrawCommand command) {
        return switch (command.type()) {
            case ROUNDED_RECT -> Math.abs(command.radius()) > 0.5f || command.paint().isStroke();
            case CIRCLE, LINE -> true;
            case RECT -> command.paint().isStroke()
                    || isFractional(command.bounds().x())
                    || isFractional(command.bounds().y())
                    || isFractional(command.bounds().width())
                    || isFractional(command.bounds().height());
            default -> false;
        };
    }

    private static boolean isFractional(float value) {
        return Math.abs(value - Math.round(value)) > 0.001f;
    }

    private static Matrix4f commandMatrix(Matrix4f basePose, RectView bounds, Transform transform) {
        Matrix4f matrix = new Matrix4f(basePose);
        if (transform == null) return matrix;

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

    private static float positiveThickness(float value) {
        return Float.isFinite(value) ? Math.max(MIN_STROKE_WIDTH, value) : 1.0f;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static void uploadFloat(int activeProgram, String name, float value) {
        int location = GL20.glGetUniformLocation(activeProgram, name);
        if (location >= 0) GL20.glUniform1f(location, value);
    }

    private static void uploadInt(int activeProgram, String name, int value) {
        int location = GL20.glGetUniformLocation(activeProgram, name);
        if (location >= 0) GL20.glUniform1i(location, value);
    }

    private static void uploadVec2(int activeProgram, String name, float x, float y) {
        int location = GL20.glGetUniformLocation(activeProgram, name);
        if (location >= 0) GL20.glUniform2f(location, x, y);
    }

    private static void uploadVec4(int activeProgram, String name, float x, float y, float z, float w) {
        int location = GL20.glGetUniformLocation(activeProgram, name);
        if (location >= 0) GL20.glUniform4f(location, x, y, z, w);
    }

    private static void uploadMat4(int activeProgram, String name, Matrix4f matrix) {
        int location = GL20.glGetUniformLocation(activeProgram, name);
        if (location < 0) return;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer buffer = stack.mallocFloat(16);
            matrix.get(buffer);
            GL20.glUniformMatrix4fv(location, false, buffer);
        }
    }

    @Override
    public void close() {
        if (program != 0) {
            GL20.glDeleteProgram(program);
            program = 0;
        }
        unavailable = false;
    }

    private record RenderState(int program,
                               int activeTexture,
                               int texture,
                               boolean blend,
                               int blendSourceRgb,
                               int blendDestinationRgb,
                               int blendSourceAlpha,
                               int blendDestinationAlpha,
                               boolean depthTest,
                               boolean depthMask,
                               boolean cull) {
        private static RenderState capture() {
            int activeTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
            RenderSystem.activeTexture(GL13.GL_TEXTURE0);
            int texture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
            RenderSystem.activeTexture(activeTexture);
            return new RenderState(
                    GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM),
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
            GL20.glUseProgram(program);
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
