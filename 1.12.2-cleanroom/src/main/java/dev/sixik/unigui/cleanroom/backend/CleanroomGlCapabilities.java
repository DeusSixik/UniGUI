package dev.sixik.unigui.cleanroom.backend;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLCapabilities;

/**
 * Describes which UniGUI render path the current OpenGL context can run.
 *
 * <p>The 1.20 backend renders shapes and text with SDF shaders written for {@code #version 150} (instanced when
 * possible) and falls back to CPU tessellation. 1.12.2 runs on whatever context Cleanroom creates, so the same
 * ladder is selected at runtime from the context version and extensions. The decision itself is the pure function
 * {@link #selectTier}; only {@link #get()} touches OpenGL.</p>
 */
final class CleanroomGlCapabilities {
    private static final Logger LOGGER = LogManager.getLogger("unigui");
    private static CleanroomGlCapabilities instance;

    /** Render path ladder, from most to least capable. */
    enum ShaderTier {
        /** GLSL 1.50 shaders with instanced draws (same sources as the 1.20 backend). */
        INSTANCED,
        /** GLSL 1.50 shaders drawn one quad per command, uniforms per draw. */
        BASIC,
        /** GLSL 1.20 shaders produced by a dialect adapter; no instancing, no {@code flat} varyings. */
        LEGACY,
        /** No usable shader support: vanilla tessellator geometry only. */
        NONE
    }

    private final String version;
    private final String renderer;
    private final int major;
    private final int minor;
    private final boolean instancedArrays;
    private final boolean vertexArrays;
    private final ShaderTier tier;

    private CleanroomGlCapabilities(String version, String renderer, int major, int minor,
                                    boolean instancedArrays, boolean vertexArrays, ShaderTier tier) {
        this.version = version;
        this.renderer = renderer;
        this.major = major;
        this.minor = minor;
        this.instancedArrays = instancedArrays;
        this.vertexArrays = vertexArrays;
        this.tier = tier;
    }

    /**
     * Probes the current context once and caches the result. Must be called on the render thread with a current
     * context; any failure while probing degrades to {@link ShaderTier#NONE}.
     *
     * @return capabilities of the active OpenGL context
     */
    static synchronized CleanroomGlCapabilities get() {
        if (instance != null) return instance;
        CleanroomGlCapabilities probed;
        try {
            String version = GL11.glGetString(GL11.GL_VERSION);
            String renderer = GL11.glGetString(GL11.GL_RENDERER);
            int[] parsed = parseVersion(version);
            GLCapabilities caps = GL.getCapabilities();
            boolean instanced = caps != null && (caps.OpenGL33 || caps.GL_ARB_instanced_arrays);
            boolean vao = caps != null && (caps.OpenGL30 || caps.GL_ARB_vertex_array_object);
            ShaderTier tier = selectTier(parsed[0], parsed[1], instanced, vao,
                    caps != null && caps.OpenGL20);
            probed = new CleanroomGlCapabilities(version, renderer, parsed[0], parsed[1], instanced, vao, tier);
        } catch (Throwable failure) {
            LOGGER.warn("UniGUI could not probe the OpenGL context; shader paths are disabled", failure);
            probed = new CleanroomGlCapabilities("unknown", "unknown", 0, 0, false, false, ShaderTier.NONE);
        }
        instance = probed;
        LOGGER.info("UniGUI OpenGL: version='{}' renderer='{}' parsed={}.{} instancedArrays={} vao={} -> shader tier {}",
                probed.version, probed.renderer, probed.major, probed.minor,
                probed.instancedArrays, probed.vertexArrays, probed.tier);
        return probed;
    }

    /** @return most capable render path this context supports */
    ShaderTier tier() {
        return tier;
    }

    /** @return raw {@code GL_VERSION} string, for diagnostics */
    String version() {
        return version;
    }

    /**
     * Chooses the render path for a context.
     *
     * <p>{@code #version 150} needs GL 3.2. Instancing additionally needs VAOs and either GL 3.3 or
     * {@code ARB_instanced_arrays}. Shader objects (GLSL 1.20 path) need GL 2.0.</p>
     *
     * @param major context major version
     * @param minor context minor version
     * @param instancedArrays {@code glVertexAttribDivisor} is available (GL 3.3 or ARB_instanced_arrays)
     * @param vertexArrays vertex array objects are available
     * @param shaderObjects GL 2.0 shader objects are available
     * @return selected tier
     */
    static ShaderTier selectTier(int major, int minor, boolean instancedArrays, boolean vertexArrays,
                                 boolean shaderObjects) {
        boolean glsl150 = major > 3 || (major == 3 && minor >= 2);
        if (glsl150 && vertexArrays && instancedArrays) return ShaderTier.INSTANCED;
        if (glsl150 && vertexArrays) return ShaderTier.BASIC;
        if (shaderObjects || major >= 2) return ShaderTier.LEGACY;
        return ShaderTier.NONE;
    }

    /**
     * Extracts {@code major.minor} from a {@code GL_VERSION} string such as {@code "4.6.0 NVIDIA 551.23"},
     * {@code "3.2 Mesa 23.1"} or {@code "OpenGL ES 3.0"}.
     *
     * @param version raw version string, may be {@code null}
     * @return two-element array {@code {major, minor}}; {@code {0, 0}} if nothing parseable is found
     */
    static int[] parseVersion(String version) {
        int[] result = {0, 0};
        if (version == null) return result;
        int length = version.length();
        for (int i = 0; i < length; i++) {
            if (!Character.isDigit(version.charAt(i))) continue;
            int majorEnd = i;
            while (majorEnd < length && Character.isDigit(version.charAt(majorEnd))) majorEnd++;
            if (majorEnd >= length - 1 || version.charAt(majorEnd) != '.' || !Character.isDigit(version.charAt(majorEnd + 1))) {
                i = majorEnd;
                continue;
            }
            int minorEnd = majorEnd + 1;
            while (minorEnd < length && Character.isDigit(version.charAt(minorEnd))) minorEnd++;
            try {
                result[0] = Integer.parseInt(version.substring(i, majorEnd));
                result[1] = Integer.parseInt(version.substring(majorEnd + 1, minorEnd));
            } catch (NumberFormatException ignored) {
                result[0] = 0;
                result[1] = 0;
            }
            return result;
        }
        return result;
    }
}
