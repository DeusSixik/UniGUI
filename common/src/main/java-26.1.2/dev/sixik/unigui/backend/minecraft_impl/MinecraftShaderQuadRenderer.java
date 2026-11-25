package dev.sixik.unigui.backend.minecraft_impl;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.render.DrawCommand;
import dev.sixik.unigui.api.render.TextureHandle;
import dev.sixik.unigui.api.render.TextureOptions;
import dev.sixik.unigui.api.render.shaders.ShaderDrawOptions;
import dev.sixik.unigui.api.render.shaders.ShaderHandle;
import dev.sixik.unigui.api.render.shaders.ShaderProviders;
import dev.sixik.unigui.api.render.shaders.ShaderSource;
import dev.sixik.unigui.api.render.shaders.ShaderUniform;
import dev.sixik.unigui.api.render.shaders.ShaderUniforms;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.intellij.lang.annotations.Language;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL20;
import org.lwjgl.system.MemoryStack;
import dev.sixik.unigui.api.backend.UniGuiLogger;
import dev.sixik.unigui.api.backend.UniGuiRuntime;

import java.nio.FloatBuffer;
import java.util.IdentityHashMap;
import java.util.Map;
final class MinecraftShaderQuadRenderer implements AutoCloseable {
    private static final UniGuiLogger LOGGER = UniGuiRuntime.logger(MinecraftShaderQuadRenderer.class);
    private static final int TEXTURE_BINDING_CACHE_SIZE = 500;

    @Language("GLSL")
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

    private static final long START_NANOS = System.nanoTime();

    private final Int2IntMap programs = new Int2IntOpenHashMap();
    private final MinecraftUniformLocationCache uniformLocations = new MinecraftUniformLocationCache();
    private final UiTextureBinding.Cache textureBindings = new UiTextureBinding.Cache(TEXTURE_BINDING_CACHE_SIZE);

    private final MinecraftResourceShaderProvider shaderProvider;

    MinecraftShaderQuadRenderer(Minecraft minecraft) {
        programs.defaultReturnValue(-1);
        shaderProvider = new MinecraftResourceShaderProvider(minecraft);
    }

    boolean render(UniGuiGraphics graphics,
                   Minecraft minecraft,
                   DrawCommand command,
                   boolean renderingToPremultipliedTarget,
                   float screenWidth,
                   float screenHeight,
                   float guiScale) {
        if (graphics == null || minecraft == null || command == null || command.shader() == null) return false;
        int program = program(minecraft, command.shader());
        if (program == 0) return false;

        UiGl.Session session = UiGl.begin();
        try {
            UiGpu.markDirtyAll();
            GL20.glUseProgram(program);
            bindSourceTexture(program, command.texture());
            bindExtraTextures(program, command.shaderTextures());
            uploadBuiltins(program, graphics, command,
                    Math.max(1.0f, screenWidth),
                    Math.max(1.0f, screenHeight),
                    sanitizeScale(guiScale));
            uploadUniforms(program, command.shaderUniforms());

            UiGl.blend(command.shaderOptions().blend()
                    ? MinecraftUiBlend.straight(renderingToPremultipliedTarget)
                    : MinecraftUiBlend.REPLACE);

            Object buffer = MinecraftBufferCompat.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
            MinecraftBufferCompat.vertex(buffer, new Matrix4f(), -1.0f, 1.0f, 0.0f);
            MinecraftBufferCompat.vertex(buffer, new Matrix4f(), -1.0f, -1.0f, 0.0f);
            MinecraftBufferCompat.vertex(buffer, new Matrix4f(), 1.0f, -1.0f, 0.0f);
            MinecraftBufferCompat.vertex(buffer, new Matrix4f(), 1.0f, 1.0f, 0.0f);
            MinecraftBufferCompat.draw(buffer);
            return true;
        } catch (Throwable failure) {
            LOGGER.error("UniGUI shader draw failed for {}", command.shader().id(), failure);
            return false;
        } finally {
            UiGl.end(session);
        }
    }


    private void bindSourceTexture(int program, TextureHandle texture) {
        if (texture == null) return;
        UiTextureBinding binding = textureBindings.resolve(texture);
        if (binding == null) return;

        UiGl.bindTexture(0, binding.view(), binding.sampler());
        uploadInt(program, "SourceTexture", 0);
        uploadInt(program, "Texture0", 0);
        uploadVec2(program, "SourceSize", Math.max(1.0f, texture.width()), Math.max(1.0f, texture.height()));
        uploadFloat(program, "SourceFlipY", binding.flipY() ? 1.0f : 0.0f);
    }

    private void bindExtraTextures(int program, Map<String, TextureHandle> textures) {
        if (textures == null || textures.isEmpty()) return;
        int unit = 1;
        for (Map.Entry<String, TextureHandle> entry : textures.entrySet()) {
            if (entry == null || entry.getValue() == null) continue;
            String uniform = entry.getKey() == null ? "" : entry.getKey().trim();
            if (uniform.isEmpty()) continue;
            bindTexture(program, uniform, entry.getValue(), unit);
            unit++;
        }
    }

    private void bindTexture(int program, String uniformName, TextureHandle texture, int unit) {
        UiTextureBinding binding = textureBindings.resolve(texture);
        if (binding == null) return;

        UiGl.bindTexture(Math.max(0, unit), binding.view(), binding.sampler());
        uploadInt(program, uniformName, Math.max(0, unit));
    }
    private int program(Minecraft minecraft, ShaderHandle shader) {
        ShaderSource source = ShaderProviders.resolveOrNull(shader, shaderProvider);
        if (source == null || source.fragmentSource().isBlank()) {
            LOGGER.warn("UniGUI shader source not found: {}", shader.id());
            return 0;
        }

        String vertexSource = source.hasVertexSource() ? source.vertexSource() : DEFAULT_VERTEX_SOURCE;
        String fragmentSource = source.fragmentSource();
        int key = source.intId();
        int cached = programs.get(key);
        if (cached != -1) return cached;

        int id = linkProgram(source.id(), vertexSource, fragmentSource);
        programs.put(key, id);
        return id;
    }

    private void uploadBuiltins(int program, UniGuiGraphics graphics, DrawCommand command,
                                float screenWidth, float screenHeight, float guiScale) {
        ShaderDrawOptions options = command.shaderOptions();
        if (!options.builtinUniforms()) return;

        uploadVec2(program, "ScreenSize", screenWidth, screenHeight);
        uploadFloat(program, "GuiScale", guiScale);
        uploadFloat(program, "Time", (System.nanoTime() - START_NANOS) / 1_000_000_000.0f);

        RectView bounds = command.bounds();
        float offset = options.squareVertexOffset();
        float x1 = bounds.x() + offset;
        float y1 = bounds.y() + offset;
        float x2 = bounds.x() + bounds.width() + offset;
        float y2 = bounds.y() + bounds.height() + offset;
        uploadVec4(program, "SquareVertex", x1, y1, x2, y2);

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

    /**
     * Возвращает location uniform'а из cache конкретной OpenGL-программы.
     *
     * <p>Location действителен до удаления или перелинковки программы, поэтому
     * встроенные и пользовательские uniform'ы используют один общий cache.</p>
     */
    private int uniformLocation(int program, String name) {
        return uniformLocations.get(program, name);
    }

    private void uploadFloat(int program, String name, float value) {
        int location = uniformLocation(program, name);
        if (location >= 0) GL20.glUniform1f(location, value);
    }

    private void uploadInt(int program, String name, int value) {
        int location = uniformLocation(program, name);
        if (location >= 0) GL20.glUniform1i(location, value);
    }

    private void uploadVec2(int program, String name, float x, float y) {
        int location = uniformLocation(program, name);
        if (location >= 0) GL20.glUniform2f(location, x, y);
    }

    private void uploadVec3(int program, String name, float x, float y, float z) {
        int location = uniformLocation(program, name);
        if (location >= 0) GL20.glUniform3f(location, x, y, z);
    }

    private void uploadVec4(int program, String name, float x, float y, float z, float w) {
        int location = uniformLocation(program, name);
        if (location >= 0) GL20.glUniform4f(location, x, y, z, w);
    }

    private void uploadMat4(int program, String name, FloatBuffer values) {
        int location = uniformLocation(program, name);
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
        for (int program : programs.values()) {
            if (program != 0) {
                GL20.glDeleteProgram(program);
            }
        }
        programs.clear();
        uniformLocations.clear();
        textureBindings.clear();
    }
}
