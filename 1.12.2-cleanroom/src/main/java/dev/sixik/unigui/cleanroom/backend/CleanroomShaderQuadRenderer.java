package dev.sixik.unigui.cleanroom.backend;

import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.render.BlendMode;
import dev.sixik.unigui.api.render.DrawCommand;
import dev.sixik.unigui.api.render.TextureHandle;
import dev.sixik.unigui.api.render.TextureOptions;
import dev.sixik.unigui.api.render.shaders.ShaderDrawOptions;
import dev.sixik.unigui.api.render.shaders.ShaderHandle;
import dev.sixik.unigui.api.render.shaders.ShaderProviders;
import dev.sixik.unigui.api.render.shaders.ShaderSource;
import dev.sixik.unigui.api.render.shaders.ShaderUniform;
import dev.sixik.unigui.api.render.shaders.ShaderUniforms;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.util.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Draws {@code SHADER} commands: a user GLSL program over a full-viewport quad, the way the 1.20 backend does.
 *
 * <p>The default vertex shader covers the whole framebuffer; shaders that want a sub-rectangle ship their own vertex
 * shader and place the quad from {@code SquareVertex} and {@code ScreenSize}. Built-in uniforms ({@code ScreenSize},
 * {@code GuiScale}, {@code Time}, {@code SquareVertex}, {@code PoseStack}, {@code SourceTexture} / {@code Texture0},
 * {@code SourceSize}, {@code SourceFlipY}) follow the 1.20 names, so a shader file runs unchanged on both versions.
 * Extra textures bind to units 1.. under the names of {@code DrawCommand.shaderTextures()}.</p>
 *
 * <p>Programs are cached per {@link ShaderHandle} and dropped after a resource reload. A shader that cannot be found
 * or compiled is reported once and skipped afterwards. Everything it changes (program, VAO, buffer, bound textures,
 * samplers, blend) is put back. Render thread only.</p>
 */
final class CleanroomShaderQuadRenderer implements AutoCloseable {
    private static final Logger LOGGER = LogManager.getLogger("unigui");
    private static final long START_NANOS = System.nanoTime();
    private static final String[] ATTRIBUTES = {"Position"};

    /** The 1.20 default vertex shader: one quad over the whole framebuffer, {@code uv} in -1..1. */
    static final String DEFAULT_VERTEX_SOURCE = """
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

    private final CleanroomResourceShaderProvider provider = new CleanroomResourceShaderProvider();
    private final Map<ShaderHandle, CleanroomShaderProgram> programs = new HashMap<>();
    private final Set<ShaderHandle> failed = new HashSet<>();
    private final CleanroomGlState.ShaderBindings bindings = new CleanroomGlState.ShaderBindings();
    private final CleanroomSamplerState sampler = new CleanroomSamplerState();
    private final FloatBuffer matrix = BufferUtils.createFloatBuffer(16);
    private int seenReloads = Integer.MIN_VALUE;
    private int vao;
    private int vbo;
    private boolean quadReady;
    private boolean unavailableReported;

    /**
     * @return {@code true} if this GL context can run the shaders (GLSL 1.50 with vertex arrays)
     */
    static boolean supported() {
        CleanroomGlCapabilities.ShaderTier tier = CleanroomGlCapabilities.get().tier();
        return tier == CleanroomGlCapabilities.ShaderTier.INSTANCED
                || tier == CleanroomGlCapabilities.ShaderTier.BASIC;
    }

    /**
     * Draws one shader command.
     *
     * @param command command of type {@code SHADER}
     * @param screenWidth viewport width in draw-list units (feeds {@code ScreenSize})
     * @param screenHeight viewport height in draw-list units
     * @param guiScale physical pixels per draw-list unit (feeds {@code GuiScale})
     * @return {@code true} if something was drawn
     */
    boolean render(DrawCommand command, float screenWidth, float screenHeight, float guiScale) {
        if (command == null || command.shader() == null) return false;
        if (!supported()) {
            if (!unavailableReported) {
                unavailableReported = true;
                LOGGER.warn("UniGUI shader commands are skipped: this OpenGL context has no GLSL 1.50 support");
            }
            return false;
        }
        CleanroomShaderProgram program = program(command.shader());
        if (program == null) return false;

        Minecraft minecraft = Minecraft.getMinecraft();
        int activeUnit = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        int samplerDepth = sampler.depth();
        int extraUnits = 0;
        bindings.capture();
        try {
            if (!prepareQuad()) return false;
            program.use();
            GlStateManager.setActiveTexture(GL13.GL_TEXTURE0);
            bindSource(program, minecraft, command.texture());
            extraUnits = bindExtras(program, minecraft, command.shaderTextures());
            uploadBuiltins(program, command, screenWidth, screenHeight, guiScale);
            uploadUniforms(program, command.shaderUniforms());

            if (command.shaderOptions().blend()) {
                GlStateManager.enableBlend();
                CleanroomGlState.blend(BlendMode.NORMAL);
            } else {
                GlStateManager.disableBlend();
            }
            GL30.glBindVertexArray(vao);
            GL11.glDrawArrays(GL11.GL_TRIANGLE_FAN, 0, 4);
            return true;
        } catch (Throwable failure) {
            LOGGER.error("UniGUI shader draw failed for {}", command.shader().id(), failure);
            return false;
        } finally {
            sampler.restoreTo(samplerDepth);
            for (int unit = 1; unit <= extraUnits; unit++) {
                GlStateManager.setActiveTexture(GL13.GL_TEXTURE0 + unit);
                GlStateManager.bindTexture(0);
            }
            GlStateManager.setActiveTexture(activeUnit);
            bindings.restore();
            GlStateManager.enableBlend();
            CleanroomGlState.blend(BlendMode.NORMAL);
            CleanroomGlState.checkError("UniGUI shader " + command.shader().id());
        }
    }

    private CleanroomShaderProgram program(ShaderHandle handle) {
        int reloads = CleanroomResourceReloads.generation();
        if (reloads != seenReloads) {
            seenReloads = reloads;
            releasePrograms();
        }
        CleanroomShaderProgram cached = programs.get(handle);
        if (cached != null) return cached;
        if (failed.contains(handle)) return null;

        ShaderSource source = ShaderProviders.resolveOrNull(handle, provider);
        if (source == null || source.fragmentSource().isBlank()) {
            LOGGER.warn("UniGUI shader source not found: {}", handle.id());
            failed.add(handle);
            return null;
        }
        String vertex = source.hasVertexSource() ? source.vertexSource() : DEFAULT_VERTEX_SOURCE;
        CleanroomShaderProgram program = CleanroomShaderProgram.link(source.id(), vertex, source.fragmentSource(),
                ATTRIBUTES);
        if (program == null) {
            failed.add(handle);
            return null;
        }
        programs.put(handle, program);
        return program;
    }

    private boolean prepareQuad() {
        if (quadReady) return true;
        vao = GL30.glGenVertexArrays();
        vbo = GL15.glGenBuffers();
        GL30.glBindVertexArray(vao);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        FloatBuffer corners = BufferUtils.createFloatBuffer(12);
        corners.put(-1.0f).put(1.0f).put(0.0f)
                .put(-1.0f).put(-1.0f).put(0.0f)
                .put(1.0f).put(-1.0f).put(0.0f)
                .put(1.0f).put(1.0f).put(0.0f).flip();
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, corners, GL15.GL_STATIC_DRAW);
        GL20.glEnableVertexAttribArray(0);
        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 3 * Float.BYTES, 0L);
        quadReady = true;
        return true;
    }

    // ---- textures -----------------------------------------------------------------------------------------------

    private void bindSource(CleanroomShaderProgram program, Minecraft minecraft, TextureHandle texture) {
        if (texture == null) return;
        int flip = bindTexture(minecraft, 0, texture);
        if (flip < 0) return;
        program.set("SourceTexture", 0);
        program.set("Texture0", 0);
        program.set("SourceSize", Math.max(1.0f, texture.width()), Math.max(1.0f, texture.height()));
        program.set("SourceFlipY", (float) flip);
    }

    private int bindExtras(CleanroomShaderProgram program, Minecraft minecraft, Map<String, TextureHandle> textures) {
        if (textures == null || textures.isEmpty()) return 0;
        int unit = 1;
        for (Map.Entry<String, TextureHandle> entry : textures.entrySet()) {
            if (entry == null || entry.getValue() == null || entry.getKey() == null) continue;
            String uniform = entry.getKey().trim();
            if (uniform.isEmpty()) continue;
            if (bindTexture(minecraft, unit, entry.getValue()) < 0) continue;
            program.set(uniform, unit);
            unit++;
        }
        return unit - 1;
    }

    /**
     * Binds a texture on a unit and applies its sampling options.
     *
     * @return {@code 1} if the texture must be sampled upside down (render target), {@code 0} if not, {@code -1} if
     *         the texture could not be bound
     */
    private int bindTexture(Minecraft minecraft, int unit, TextureHandle texture) {
        GlStateManager.setActiveTexture(GL13.GL_TEXTURE0 + unit);
        TextureOptions options = texture.options() == null ? TextureOptions.defaults() : texture.options();
        Object nativeHandle = texture.nativeHandle();
        int flip = 0;
        if (nativeHandle instanceof CleanroomRenderTarget.ColorTextureHandle target) {
            if (target.isDisposed()) return -1;
            GlStateManager.bindTexture(target.textureId());
            flip = target.flipY() ? 1 : 0;
        } else if (nativeHandle instanceof Integer glName) {
            GlStateManager.bindTexture(glName);
        } else {
            ResourceLocation location = CleanroomTextures.location(texture);
            minecraft.getTextureManager().bindTexture(location);
            if (options.mipmaps()) {
                ITextureObject object = minecraft.getTextureManager().getTexture(location);
                if (object != null && CleanroomTextures.markMipmapped(object.getGlTextureId())) {
                    CleanroomSamplerState.generateMipmaps();
                }
            }
        }
        sampler.apply(unit, options);
        return flip;
    }

    // ---- uniforms -----------------------------------------------------------------------------------------------

    private void uploadBuiltins(CleanroomShaderProgram program, DrawCommand command,
                                float screenWidth, float screenHeight, float guiScale) {
        ShaderDrawOptions options = command.shaderOptions();
        if (!options.builtinUniforms()) return;
        program.set("ScreenSize", Math.max(1.0f, screenWidth), Math.max(1.0f, screenHeight));
        program.set("GuiScale", Float.isFinite(guiScale) && guiScale > 0.0f ? guiScale : 1.0f);
        program.set("Time", (System.nanoTime() - START_NANOS) / 1_000_000_000.0f);

        RectView bounds = command.bounds();
        float offset = options.squareVertexOffset();
        program.set("SquareVertex", bounds.x() + offset, bounds.y() + offset,
                bounds.x() + bounds.width() + offset, bounds.y() + bounds.height() + offset);

        matrix.clear();
        for (int i = 0; i < 16; i++) matrix.put(i % 5 == 0 ? 1.0f : 0.0f);
        matrix.flip();
        program.setMatrix("PoseStack", matrix);
    }

    private void uploadUniforms(CleanroomShaderProgram program, ShaderUniforms uniforms) {
        if (uniforms == null || uniforms.isEmpty()) return;
        for (Map.Entry<String, ShaderUniform> entry : uniforms.values().entrySet()) {
            String name = entry.getKey();
            ShaderUniform uniform = entry.getValue();
            float[] v = uniform.floats();
            switch (uniform.type()) {
                case FLOAT -> program.set(name, component(v, 0));
                case INT -> program.set(name, uniform.integer());
                case VEC2 -> program.set(name, component(v, 0), component(v, 1));
                case VEC3 -> program.set(name, component(v, 0), component(v, 1), component(v, 2));
                case VEC4 -> program.set(name, component(v, 0), component(v, 1), component(v, 2), component(v, 3));
                case MAT4 -> {
                    if (v.length == 16) {
                        matrix.clear();
                        matrix.put(v).flip();
                        program.setMatrix(name, matrix);
                    }
                }
            }
        }
    }

    /**
     * @return {@code values[index]}, or {@code 0} if the array is shorter
     */
    static float component(float[] values, int index) {
        return values != null && index < values.length ? values[index] : 0.0f;
    }

    private void releasePrograms() {
        for (CleanroomShaderProgram program : programs.values()) program.close();
        programs.clear();
        failed.clear();
    }

    @Override
    public void close() {
        releasePrograms();
        if (quadReady) {
            GL30.glDeleteVertexArrays(vao);
            GL15.glDeleteBuffers(vbo);
            quadReady = false;
        }
    }
}
