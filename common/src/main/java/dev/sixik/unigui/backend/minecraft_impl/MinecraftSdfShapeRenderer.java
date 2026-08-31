package dev.sixik.unigui.backend.minecraft_impl;

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
import org.intellij.lang.annotations.Language;import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.ARBInstancedArrays;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;
import org.lwjgl.opengl.GL33;
import org.lwjgl.system.MemoryStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.FloatBuffer;

/**
 * Шейдерный путь рендера распространённых двумерных UI-примитивов.
 *
 * <p>Старый Minecraft-рендерер фигур приближает кривые геометрией, из-за чего скруглённые углы
 * и окружности могут выглядеть ступенчатыми при обычном масштабе GUI. Этот рендерер рисует
 * один quad для каждого примитива, а fragment shader вычисляет форму через SDF и сглаживает
 * её границы.</p>
 */
final class MinecraftSdfShapeRenderer implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(MinecraftSdfShapeRenderer.class);
    private static final float AA_PAD = 1.0f;
    private static final float MIN_STROKE_WIDTH = 0.001f;

    private static final int SHAPE_ROUNDED_RECT = 0;
    private static final int SHAPE_ELLIPSE = 1;
    private static final int SHAPE_LINE = 2;

    @Language("GLSL")
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

    @Language("GLSL")
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

    /**
     * Версия фрагментного шейдера для instanced-пути. Все параметры фигуры
     * приходят из атрибутов экземпляра, поэтому здесь меняются только
     * объявления uniform на входные varyings.
     */
    private static final String INSTANCED_FRAGMENT_SOURCE = FRAGMENT_SOURCE
            .replace("uniform vec4 Color;", "flat in vec4 Color;")
            .replace("uniform vec2 Size;", "flat in vec2 Size;")
            .replace("uniform vec2 LineStart;", "flat in vec2 LineStart;")
            .replace("uniform vec2 LineEnd;", "flat in vec2 LineEnd;")
            .replace("uniform float Radius;", "flat in float Radius;")
            .replace("uniform float StrokeWidth;", "flat in float StrokeWidth;")
            .replace("uniform int ShapeType;", "flat in int ShapeType;")
            .replace("uniform int IsStroke;", "flat in int IsStroke;");

    @Language("GLSL")
    private static final String INSTANCED_VERTEX_SOURCE = """
            #version 150

            in vec2 Position;
            in vec3 InstancePosition0;
            in vec3 InstancePosition1;
            in vec3 InstancePosition2;
            in vec3 InstancePosition3;
            in vec4 InstanceLocalBounds;
            in vec4 InstanceColor;
            in vec2 InstanceSize;
            in vec2 InstanceLineStart;
            in vec2 InstanceLineEnd;
            in vec2 InstanceShape;
            in vec2 InstanceFlags;

            uniform mat4 ModelViewMat;
            uniform mat4 ProjMat;

            out vec2 localPos;
            flat out vec4 Color;
            flat out vec2 Size;
            flat out vec2 LineStart;
            flat out vec2 LineEnd;
            flat out float Radius;
            flat out float StrokeWidth;
            flat out int ShapeType;
            flat out int IsStroke;

            void main() {
                vec3 left = mix(InstancePosition0, InstancePosition1, Position.y);
                vec3 right = mix(InstancePosition3, InstancePosition2, Position.y);
                vec3 position = mix(left, right, Position.x);
                gl_Position = ProjMat * ModelViewMat * vec4(position, 1.0);
                localPos = mix(InstanceLocalBounds.xy, InstanceLocalBounds.zw, Position);
                Color = InstanceColor;
                Size = InstanceSize;
                LineStart = InstanceLineStart;
                LineEnd = InstanceLineEnd;
                Radius = InstanceShape.x;
                StrokeWidth = InstanceShape.y;
                ShapeType = int(InstanceFlags.x + 0.5);
                IsStroke = int(InstanceFlags.y + 0.5);
            }
            """;

    private static final int INSTANCE_FLOATS = 30;
    private static final int INSTANCE_STRIDE_BYTES = INSTANCE_FLOATS * Float.BYTES;
    private static final int INITIAL_INSTANCE_CAPACITY = 256;

    private int program;
    private int instancedProgram;
    private int instanceVertexArray;
    private int instanceVertexBuffer;
    private int instanceBuffer;
    private boolean unavailable;
    private boolean instancedUnavailable;
    private FloatBuffer instanceData = BufferUtils.createFloatBuffer(
            INITIAL_INSTANCE_CAPACITY * INSTANCE_FLOATS);
    private final Vector3f transformedPosition = new Vector3f();
    private boolean runtimeStatsEnabled;
    private long runtimePasses;
    private long runtimeCommands;
    private long runtimeDrawCalls;
    private long runtimeUniformUploads;
    private long runtimeFlushes;
    private long runtimeNanos;
    private final MinecraftUniformLocationCache uniformLocations = new MinecraftUniformLocationCache();

    void runtimeStatsEnabled(boolean enabled) {
        runtimeStatsEnabled = enabled;
    }

    SdfRuntimeStats consumeRuntimeStats() {
        SdfRuntimeStats result = new SdfRuntimeStats(
                runtimePasses, runtimeCommands, runtimeDrawCalls,
                runtimeUniformUploads, runtimeFlushes, runtimeNanos);
        runtimePasses = 0L;
        runtimeCommands = 0L;
        runtimeDrawCalls = 0L;
        runtimeUniformUploads = 0L;
        runtimeFlushes = 0L;
        runtimeNanos = 0L;
        return result;
    }

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

        long startNanos = runtimeStatsEnabled ? System.nanoTime() : 0L;
        if (runtimeStatsEnabled) {
            runtimePasses++;
            runtimeCommands += batch.size();
        }
        graphics.flush();
        if (runtimeStatsEnabled) runtimeFlushes++;
        RenderState state = RenderState.capture();
        try {
            RenderSystem.enableBlend();
            MinecraftUiBlend.applyStraightAlpha(renderingToPremultipliedTarget);
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.disableCull();

            if (!renderInstanced(graphics, batch)) {
                GL20.glUseProgram(activeProgram);
                uploadMat4(activeProgram, "ModelViewMat", RenderSystem.getModelViewMatrix());
                uploadMat4(activeProgram, "ProjMat", RenderSystem.getProjectionMatrix());
                Object[] rawCommands = batch.commandElements();
                for (int i = 0, size = batch.size(); i < size; i++) {
                    DrawCommand command = (DrawCommand) rawCommands[i];
                    renderCommand(graphics, activeProgram, command);
                }
            }
            return true;
        } catch (Throwable failure) {
            LOGGER.error("UniGUI SDF shape renderer failed; falling back to legacy primitive rendering", failure);
            return false;
        } finally {
            state.restore();
            if (runtimeStatsEnabled) runtimeNanos += Math.max(0L, System.nanoTime() - startNanos);
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
        recordDrawCall();
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

    private boolean initializeInstanced() {
        if (instancedProgram != 0) return true;
        if (instancedUnavailable || !supportsInstancing()) return false;

        int vertex = compileShader(GL20.GL_VERTEX_SHADER, INSTANCED_VERTEX_SOURCE);
        int fragment = compileShader(GL20.GL_FRAGMENT_SHADER, INSTANCED_FRAGMENT_SOURCE);
        if (vertex == 0 || fragment == 0) {
            if (vertex != 0) GL20.glDeleteShader(vertex);
            if (fragment != 0) GL20.glDeleteShader(fragment);
            instancedUnavailable = true;
            return false;
        }

        int id = GL20.glCreateProgram();
        GL20.glAttachShader(id, vertex);
        GL20.glAttachShader(id, fragment);
        GL20.glBindAttribLocation(id, 0, "Position");
        GL20.glBindAttribLocation(id, 1, "InstancePosition0");
        GL20.glBindAttribLocation(id, 2, "InstancePosition1");
        GL20.glBindAttribLocation(id, 3, "InstancePosition2");
        GL20.glBindAttribLocation(id, 4, "InstancePosition3");
        GL20.glBindAttribLocation(id, 5, "InstanceLocalBounds");
        GL20.glBindAttribLocation(id, 6, "InstanceColor");
        GL20.glBindAttribLocation(id, 7, "InstanceSize");
        GL20.glBindAttribLocation(id, 8, "InstanceLineStart");
        GL20.glBindAttribLocation(id, 9, "InstanceLineEnd");
        GL20.glBindAttribLocation(id, 10, "InstanceShape");
        GL20.glBindAttribLocation(id, 11, "InstanceFlags");
        GL20.glLinkProgram(id);
        GL20.glDeleteShader(vertex);
        GL20.glDeleteShader(fragment);
        if (GL20.glGetProgrami(id, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
            LOGGER.error("Не удалось связать instanced SDF-шейдер фигур: {}", GL20.glGetProgramInfoLog(id));
            GL20.glDeleteProgram(id);
            instancedUnavailable = true;
            return false;
        }

        int vao = GL30.glGenVertexArrays();
        int vertexBuffer = GL15.glGenBuffers();
        int buffer = GL15.glGenBuffers();
        GL30.glBindVertexArray(vao);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vertexBuffer);
        FloatBuffer quad = BufferUtils.createFloatBuffer(8);
        quad.put(0.0f).put(0.0f);
        quad.put(0.0f).put(1.0f);
        quad.put(1.0f).put(0.0f);
        quad.put(1.0f).put(1.0f).flip();
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, quad, GL15.GL_STATIC_DRAW);
        GL20.glEnableVertexAttribArray(0);
        GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, 2 * Float.BYTES, 0L);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, buffer);
        setupInstanceAttribute(1, 3, 0L);
        setupInstanceAttribute(2, 3, 3L * Float.BYTES);
        setupInstanceAttribute(3, 3, 6L * Float.BYTES);
        setupInstanceAttribute(4, 3, 9L * Float.BYTES);
        setupInstanceAttribute(5, 4, 12L * Float.BYTES);
        setupInstanceAttribute(6, 4, 16L * Float.BYTES);
        setupInstanceAttribute(7, 2, 20L * Float.BYTES);
        setupInstanceAttribute(8, 2, 22L * Float.BYTES);
        setupInstanceAttribute(9, 2, 24L * Float.BYTES);
        setupInstanceAttribute(10, 2, 26L * Float.BYTES);
        setupInstanceAttribute(11, 2, 28L * Float.BYTES);

        GL30.glBindVertexArray(0);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        instancedProgram = id;
        instanceVertexArray = vao;
        instanceVertexBuffer = vertexBuffer;
        instanceBuffer = buffer;
        return true;
    }

    private static void setupInstanceAttribute(int index, int components, long offset) {
        GL20.glEnableVertexAttribArray(index);
        GL20.glVertexAttribPointer(index, components, GL11.GL_FLOAT, false, INSTANCE_STRIDE_BYTES, offset);
        if (GL.getCapabilities().OpenGL33) {
            GL33.glVertexAttribDivisor(index, 1);
        } else {
            ARBInstancedArrays.glVertexAttribDivisorARB(index, 1);
        }
    }

    private static boolean supportsInstancing() {
        if (GL.getCapabilities() == null) return false;
        return GL.getCapabilities().OpenGL31
                && (GL.getCapabilities().OpenGL33 || GL.getCapabilities().GL_ARB_instanced_arrays);
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

    private int uniformLocation(int activeProgram, String name) {
        return uniformLocations.get(activeProgram, name);
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

    private void uploadFloat(int activeProgram, String name, float value) {
        int location = uniformLocation(activeProgram, name);
        if (location >= 0) {
            GL20.glUniform1f(location, value);
            if (runtimeStatsEnabled) runtimeUniformUploads++;
        }
    }

    /**
     * Рисует весь совместимый batch одним instanced-вызовом. Порядок команд
     * сохраняется: экземпляры идут в том же порядке, в котором они записаны
     * в {@link DrawBatch}, а значит прозрачность и перекрытия не меняются.
     */
    private boolean renderInstanced(GuiGraphics graphics, DrawBatch batch) {
        try {
            return renderInstancedPass(graphics, batch);
        } catch (Throwable failure) {
            instancedUnavailable = true;
            GL30.glBindVertexArray(0);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
            LOGGER.warn("Instanced SDF-рендер фигур отключён после ошибки; используется fallback", failure);
            return false;
        }
    }

    private boolean renderInstancedPass(GuiGraphics graphics, DrawBatch batch) {
        if (!initializeInstanced() || batch.size() == 0) return false;

        ensureInstanceCapacity(batch.size());
        instanceData.clear();
        Matrix4f basePose = graphics.pose().last().pose();
        Object[] rawCommands = batch.commandElements();
        int instanceCount = 0;
        for (int i = 0, size = batch.size(); i < size; i++) {
            if (appendInstance(instanceData, basePose, (DrawCommand) rawCommands[i])) {
                instanceCount++;
            }
        }
        if (instanceCount == 0) return true;

        instanceData.flip();
        GL20.glUseProgram(instancedProgram);
        uploadMat4(instancedProgram, "ModelViewMat", RenderSystem.getModelViewMatrix());
        uploadMat4(instancedProgram, "ProjMat", RenderSystem.getProjectionMatrix());
        GL30.glBindVertexArray(instanceVertexArray);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, instanceBuffer);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, instanceData, GL15.GL_STREAM_DRAW);
        GL31.glDrawArraysInstanced(GL11.GL_TRIANGLE_STRIP, 0, 4, instanceCount);
        recordDrawCall();
        return true;
    }

    private boolean appendInstance(FloatBuffer target, Matrix4f basePose, DrawCommand command) {
        RectView bounds = command.bounds();
        float x1 = Math.min(bounds.x(), bounds.x() + bounds.width());
        float y1 = Math.min(bounds.y(), bounds.y() + bounds.height());
        float x2 = Math.max(bounds.x(), bounds.x() + bounds.width());
        float y2 = Math.max(bounds.y(), bounds.y() + bounds.height());
        float width = x2 - x1;
        float height = y2 - y1;
        if (width <= 0.0f || height <= 0.0f) return false;

        int shapeType;
        float radius;
        float lineStartX = 0.0f;
        float lineStartY = 0.0f;
        float lineEndX = 0.0f;
        float lineEndY = 0.0f;
        float quadX1;
        float quadY1;
        float quadX2;
        float quadY2;
        float localX1;
        float localY1;
        float localX2;
        float localY2;
        float strokeWidth;
        boolean stroke = command.paint().isStroke();

        if (command.type() == DrawCommandType.LINE) {
            shapeType = SHAPE_LINE;
            lineEndX = bounds.width();
            lineEndY = bounds.height();
            strokeWidth = positiveThickness(command.paint().strokeWidth());
            float minX = Math.min(0.0f, lineEndX);
            float minY = Math.min(0.0f, lineEndY);
            float maxX = Math.max(0.0f, lineEndX);
            float maxY = Math.max(0.0f, lineEndY);
            float pad = strokeWidth * 0.5f + AA_PAD;
            quadX1 = bounds.x() + minX - pad;
            quadY1 = bounds.y() + minY - pad;
            quadX2 = bounds.x() + maxX + pad;
            quadY2 = bounds.y() + maxY + pad;
            localX1 = minX - pad;
            localY1 = minY - pad;
            localX2 = maxX + pad;
            localY2 = maxY + pad;
            width = Math.max(1.0f, maxX - minX);
            height = Math.max(1.0f, maxY - minY);
            radius = 0.0f;
            stroke = false;
        } else {
            shapeType = command.type() == DrawCommandType.CIRCLE ? SHAPE_ELLIPSE : SHAPE_ROUNDED_RECT;
            radius = command.type() == DrawCommandType.ROUNDED_RECT
                    ? clamp(Math.abs(command.radius()), 0.0f, Math.min(width, height) * 0.5f)
                    : 0.0f;
            strokeWidth = positiveThickness(command.paint().strokeWidth());
            float pad = AA_PAD;
            quadX1 = x1 - pad;
            quadY1 = y1 - pad;
            quadX2 = x2 + pad;
            quadY2 = y2 + pad;
            localX1 = -pad;
            localY1 = -pad;
            localX2 = width + pad;
            localY2 = height + pad;
        }

        Matrix4f matrix = MinecraftTransform.commandMatrix(basePose, command);
        writePosition(target, matrix, quadX1, quadY1);
        writePosition(target, matrix, quadX1, quadY2);
        writePosition(target, matrix, quadX2, quadY2);
        writePosition(target, matrix, quadX2, quadY1);
        target.put(localX1).put(localY1).put(localX2).put(localY2);

        ColorView color = command.paint().color();
        target.put(color.r()).put(color.g()).put(color.b()).put(color.a());
        target.put(width).put(height);
        target.put(lineStartX).put(lineStartY);
        target.put(lineEndX).put(lineEndY);
        target.put(radius).put(strokeWidth);
        target.put(shapeType).put(stroke ? 1.0f : 0.0f);
        return true;
    }

    private void writePosition(FloatBuffer target, Matrix4f matrix, float x, float y) {
        matrix.transformPosition(x, y, 0.0f, transformedPosition);
        target.put(transformedPosition.x()).put(transformedPosition.y()).put(transformedPosition.z());
    }

    private void ensureInstanceCapacity(int commandCount) {
        int required = commandCount * INSTANCE_FLOATS;
        if (required <= instanceData.capacity()) return;
        int capacity = instanceData.capacity();
        while (capacity < required) capacity *= 2;
        instanceData = BufferUtils.createFloatBuffer(capacity);
    }

    private void uploadInt(int activeProgram, String name, int value) {
        int location = uniformLocation(activeProgram, name);
        if (location >= 0) {
            GL20.glUniform1i(location, value);
            if (runtimeStatsEnabled) runtimeUniformUploads++;
        }
    }

    private void uploadVec2(int activeProgram, String name, float x, float y) {
        int location = uniformLocation(activeProgram, name);
        if (location >= 0) {
            GL20.glUniform2f(location, x, y);
            if (runtimeStatsEnabled) runtimeUniformUploads++;
        }
    }

    private void uploadVec4(int activeProgram, String name, float x, float y, float z, float w) {
        int location = uniformLocation(activeProgram, name);
        if (location >= 0) {
            GL20.glUniform4f(location, x, y, z, w);
            if (runtimeStatsEnabled) runtimeUniformUploads++;
        }
    }

    private void uploadMat4(int activeProgram, String name, Matrix4f matrix) {
        int location = uniformLocation(activeProgram, name);
        if (location < 0) return;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer buffer = stack.mallocFloat(16);
            matrix.get(buffer);
            GL20.glUniformMatrix4fv(location, false, buffer);
            if (runtimeStatsEnabled) runtimeUniformUploads++;
        }
    }

    void recordDrawCall() {
        if (runtimeStatsEnabled) runtimeDrawCalls++;
    }

    @Override
    public void close() {
        if (program != 0) {
            GL20.glDeleteProgram(program);
            program = 0;
        }
        if (instancedProgram != 0) {
            GL20.glDeleteProgram(instancedProgram);
            instancedProgram = 0;
        }
        if (instanceBuffer != 0) {
            GL15.glDeleteBuffers(instanceBuffer);
            instanceBuffer = 0;
        }
        if (instanceVertexBuffer != 0) {
            GL15.glDeleteBuffers(instanceVertexBuffer);
            instanceVertexBuffer = 0;
        }
        if (instanceVertexArray != 0) {
            GL30.glDeleteVertexArrays(instanceVertexArray);
            instanceVertexArray = 0;
        }
        uniformLocations.clear();
        unavailable = false;
        instancedUnavailable = false;
    }

    private record RenderState(int program, int vertexArray, int arrayBuffer,
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
                    GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING),
                    GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING),
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
            GL30.glBindVertexArray(vertexArray);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, arrayBuffer);
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

    record SdfRuntimeStats(long passes, long commands, long drawCalls,
                           long uniformUploads, long flushes, long nanos) {
    }
}
