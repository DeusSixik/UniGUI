package dev.sixik.unigui.backend.minecraft;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.render.DrawCommand;
import dev.sixik.unigui.api.render.shaders.ShaderDrawOptions;
import dev.sixik.unigui.api.render.shaders.ShaderHandle;
import dev.sixik.unigui.api.render.shaders.ShaderProviders;
import dev.sixik.unigui.api.render.shaders.ShaderSource;
import dev.sixik.unigui.api.render.shaders.ShaderUniform;
import dev.sixik.unigui.api.render.shaders.ShaderUniforms;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL20;
import org.lwjgl.system.MemoryStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;
final class MinecraftShaderQuadRenderer implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(MinecraftShaderQuadRenderer.class);

    private static final String DEFAULT_VERTEX_SOURCE = """
            #version 150

            in vec3 Position;

            out vec2 screenPos;
            out vec2 guiPos;
            out vec2 uv;

            uniform vec2 ScreenSize;

            void main() {
                gl_Position = vec4(Position.xy, 0.0, 1.0);
                uv = Position.xy;
                screenPos = (Position.xy * 0.5 + 0.5) * ScreenSize;
                guiPos = screenPos;
            }
            """;

    private final Map<String, Program> programs = new HashMap<>();

    boolean render(GuiGraphics graphics,
                   Minecraft minecraft,
                   DrawCommand command,
                   boolean renderingToPremultipliedTarget,
                   int screenWidth,
                   int screenHeight,
                   float guiScale) {
        if (graphics == null || minecraft == null || command == null || command.shader() == null) return false;
        Program program = program(minecraft, command.shader());
        if (program == null || program.id == 0) return false;

        graphics.flush();
        RenderState state = RenderState.capture();
        try {
            GL20.glUseProgram(program.id);
            uploadBuiltins(program.id, graphics, command, Math.max(1, screenWidth), Math.max(1, screenHeight), sanitizeScale(guiScale));
            uploadUniforms(program.id, command.shaderUniforms());

            if (command.shaderOptions().blend()) {
                RenderSystem.enableBlend();
                MinecraftUiBlend.applyStraightAlpha(renderingToPremultipliedTarget);
            } else {
                RenderSystem.disableBlend();
            }
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.disableCull();

            BufferBuilder buffer = Tesselator.getInstance().getBuilder();
            buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
            buffer.vertex(-1.0, 1.0, 0.0).endVertex();
            buffer.vertex(-1.0, -1.0, 0.0).endVertex();
            buffer.vertex(1.0, -1.0, 0.0).endVertex();
            buffer.vertex(1.0, 1.0, 0.0).endVertex();
            BufferUploader.draw(buffer.end());
            return true;
        } catch (Throwable failure) {
            LOGGER.error("UniGUI shader draw failed for {}", command.shader().id(), failure);
            return false;
        } finally {
            state.restore();
        }
    }

    private Program program(Minecraft minecraft, ShaderHandle shader) {
        ShaderSource source = ShaderProviders.resolve(shader, new MinecraftResourceShaderProvider(minecraft)).orElse(null);
        if (source == null || source.fragmentSource().isBlank()) {
            LOGGER.warn("UniGUI shader source not found: {}", shader.id());
            return null;
        }

        String vertexSource = source.hasVertexSource() ? source.vertexSource() : DEFAULT_VERTEX_SOURCE;
        String fragmentSource = source.fragmentSource();
        String key = source.id() + "\n" + vertexSource.hashCode() + "\n" + fragmentSource.hashCode();
        Program cached = programs.get(key);
        if (cached != null) return cached;

        int id = linkProgram(source.id(), vertexSource, fragmentSource);
        Program program = new Program(id);
        programs.put(key, program);
        return program;
    }

    private void uploadBuiltins(int program, GuiGraphics graphics, DrawCommand command,
                                int screenWidth, int screenHeight, float guiScale) {
        ShaderDrawOptions options = command.shaderOptions();
        if (!options.builtinUniforms()) return;

        uploadVec2(program, "ScreenSize", screenWidth, screenHeight);
        uploadFloat(program, "GuiScale", guiScale);

        RectView bounds = command.bounds();
        float offset = options.squareVertexOffset();
        Matrix4f matrix = graphics.pose().last().pose();
        Vector4f point1 = new Vector4f(bounds.x() + offset, bounds.y() + offset, 0.0f, 1.0f).mul(matrix);
        Vector4f point2 = new Vector4f(bounds.x() + bounds.width() + offset,
                bounds.y() + bounds.height() + offset, 0.0f, 1.0f).mul(matrix);
        uploadVec4(program, "SquareVertex", point1.x(), point1.y(), point2.x(), point2.y());

        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer identity = stack.mallocFloat(16);
            new Matrix4f().get(identity);
            uploadMat4(program, "PoseStack", identity);
        }
    }

    private void uploadUniforms(int program, ShaderUniforms uniforms) {
        if (uniforms == null || uniforms.isEmpty()) return;
        for (Map.Entry<String, ShaderUniform> entry : uniforms.values().entrySet()) {
            String name = entry.getKey();
            ShaderUniform uniform = entry.getValue();
            float[] values = uniform.floats();
            switch (uniform.type()) {
                case FLOAT -> uploadFloat(program, name, values.length > 0 ? values[0] : 0.0f);
                case INT -> uploadInt(program, name, uniform.integer());
                case VEC2 -> uploadVec2(program, name,
                        values.length > 0 ? values[0] : 0.0f,
                        values.length > 1 ? values[1] : 0.0f);
                case VEC3 -> uploadVec3(program, name,
                        values.length > 0 ? values[0] : 0.0f,
                        values.length > 1 ? values[1] : 0.0f,
                        values.length > 2 ? values[2] : 0.0f);
                case VEC4 -> uploadVec4(program, name,
                        values.length > 0 ? values[0] : 0.0f,
                        values.length > 1 ? values[1] : 0.0f,
                        values.length > 2 ? values[2] : 0.0f,
                        values.length > 3 ? values[3] : 0.0f);
                case MAT4 -> {
                    if (values.length == 16) {
                        try (MemoryStack stack = MemoryStack.stackPush()) {
                            FloatBuffer buffer = stack.mallocFloat(16);
                            buffer.put(values).flip();
                            uploadMat4(program, name, buffer);
                        }
                    }
                }
            }
        }
    }

    private void uploadFloat(int program, String name, float value) {
        int location = GL20.glGetUniformLocation(program, name);
        if (location >= 0) GL20.glUniform1f(location, value);
    }

    private void uploadInt(int program, String name, int value) {
        int location = GL20.glGetUniformLocation(program, name);
        if (location >= 0) GL20.glUniform1i(location, value);
    }

    private void uploadVec2(int program, String name, float x, float y) {
        int location = GL20.glGetUniformLocation(program, name);
        if (location >= 0) GL20.glUniform2f(location, x, y);
    }

    private void uploadVec3(int program, String name, float x, float y, float z) {
        int location = GL20.glGetUniformLocation(program, name);
        if (location >= 0) GL20.glUniform3f(location, x, y, z);
    }

    private void uploadVec4(int program, String name, float x, float y, float z, float w) {
        int location = GL20.glGetUniformLocation(program, name);
        if (location >= 0) GL20.glUniform4f(location, x, y, z, w);
    }

    private void uploadMat4(int program, String name, FloatBuffer values) {
        int location = GL20.glGetUniformLocation(program, name);
        if (location >= 0) GL20.glUniformMatrix4fv(location, false, values);
    }

    private int linkProgram(String id, String vertexSource, String fragmentSource) {
        int vertex = compileShader(id, GL20.GL_VERTEX_SHADER, vertexSource);
        int fragment = compileShader(id, GL20.GL_FRAGMENT_SHADER, fragmentSource);
        if (vertex == 0 || fragment == 0) {
            if (vertex != 0) GL20.glDeleteShader(vertex);
            if (fragment != 0) GL20.glDeleteShader(fragment);
            return 0;
        }

        int program = GL20.glCreateProgram();
        GL20.glAttachShader(program, vertex);
        GL20.glAttachShader(program, fragment);
        GL20.glBindAttribLocation(program, 0, "Position");
        GL20.glLinkProgram(program);
        GL20.glDeleteShader(vertex);
        GL20.glDeleteShader(fragment);

        if (GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
            LOGGER.error("Failed to link UniGUI shader {}: {}", id, GL20.glGetProgramInfoLog(program));
            GL20.glDeleteProgram(program);
            return 0;
        }
        return program;
    }

    private int compileShader(String id, int type, String source) {
        int shader = GL20.glCreateShader(type);
        GL20.glShaderSource(shader, source);
        GL20.glCompileShader(shader);
        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            LOGGER.error("Failed to compile UniGUI {} shader {}: {}",
                    type == GL20.GL_VERTEX_SHADER ? "vertex" : "fragment",
                    id,
                    GL20.glGetShaderInfoLog(shader));
            GL20.glDeleteShader(shader);
            return 0;
        }
        return shader;
    }

    private static float sanitizeScale(float scale) {
        return Float.isFinite(scale) && scale > 0.0f ? scale : 1.0f;
    }

    @Override
    public void close() {
        for (Program program : programs.values()) {
            if (program.id != 0) {
                GL20.glDeleteProgram(program.id);
            }
        }
        programs.clear();
    }

    private record Program(int id) {
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