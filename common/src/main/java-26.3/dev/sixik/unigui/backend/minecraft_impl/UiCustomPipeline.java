package dev.sixik.unigui.backend.minecraft_impl;

import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.pipeline.CompiledRenderPipeline;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.pipeline.ShaderSource;
import com.mojang.renderpearl.api.pipeline.ShaderType;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.sixik.unigui.api.backend.UniGuiLogger;
import dev.sixik.unigui.api.backend.UniGuiRuntime;
import dev.sixik.unigui.impl.render.shaders.GlslInterfaceLocations;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Пайплайн с собственным GLSL UniGUI (Minecraft 26.3), один на каждый режим смешивания.
 *
 * <p>Исходник шейдера отдаётся Minecraft через {@link ShaderSource} в {@code GpuDevice.compilePipeline}, файлов
 * в ресурсах нет. На OpenGL Minecraft компилирует его как есть, на Vulkan переводит в SPIR-V (shaderc) и связывает
 * атрибуты, varyings, uniform-блоки и сэмплеры по именам. Поэтому GLSL должен быть {@code #version 330}, а униформы
 * — только в {@code layout(std140)} блоках, объявленных в группах пайплайна (обычные {@code uniform float} Vulkan
 * не принимает); сэмплеры объявляются как {@code UniformType.COMBINED_IMAGE_SAMPLER}.</p>
 *
 * <p>В 26.3 проход принимает только скомпилированный пайплайн, а кэш Minecraft
 * ({@code RenderSystem.getCompiledPipeline}) компилирует из шейдеров ресурсов. Поэтому пайплайн компилируется
 * здесь один раз и хранится вместе с исходным {@link RenderPipeline}; {@link #compiled} находит его при открытии
 * прохода ({@code MinecraftGpuCompat.setPipeline}).</p>
 *
 * <p>После первой ошибки (компиляция или отрисовка) пайплайн отключается до перезапуска, и вызывающий рисует
 * запасным путём.</p>
 */
final class UiCustomPipeline {
    private static final UniGuiLogger LOGGER = UniGuiRuntime.logger(UiCustomPipeline.class);

    private final String name;
    private final ShaderSource source;
    private final Function<MinecraftUiBlend, RenderPipeline> factory;
    private final Map<MinecraftUiBlend, RenderPipeline> pipelines = new EnumMap<>(MinecraftUiBlend.class);
    /**
     * Скомпилированные пайплайны всех экземпляров по исходному пайплайну (только поток рендера).
     */
    private static final Map<RenderPipeline, CompiledRenderPipeline> COMPILED = new IdentityHashMap<>();
    private boolean failed;
    private boolean announced;

    /**
     * @param name     имя для журнала
     * @param shader   идентификатор шейдера в пайплайнах {@code factory} (один для обеих стадий)
     * @param vertex   GLSL вершинной стадии
     * @param fragment GLSL фрагментной стадии
     * @param factory  построение пайплайна для режима смешивания
     */
    UiCustomPipeline(String name, Identifier shader, String vertex, String fragment,
                     Function<MinecraftUiBlend, RenderPipeline> factory) {
        this.name = name;
        // 26.3 компилирует GLSL через SPIR-V и на OpenGL: у входов и выходов нужны явные location.
        GlslInterfaceLocations.Result located = GlslInterfaceLocations.assign(vertex, fragment);
        this.source = new ShaderSource() {
            @Override
            public @Nullable String getShader(Identifier id, ShaderType type) {
                return shader.equals(id) ? (type == ShaderType.VERTEX ? located.vertex() : located.fragment()) : null;
            }

            @Override
            public CachedIncludeSource getInclude(Identifier id) {
                return CachedIncludeSource.createError("UniGUI shader " + shader + " has no includes (" + id + ")");
            }

            @Override
            public void close() {
            }
        };
        this.factory = factory;
    }

    /**
     * @param enabledProperty системное свойство, значение {@code false} которого выключает путь
     * @return {@code true}, если путь не выключен свойством
     */
    static boolean enabled(String enabledProperty) {
        return !"false".equalsIgnoreCase(System.getProperty(enabledProperty));
    }

    /**
     * @return {@code true}, если пайплайн ещё не отказал
     */
    boolean usable() {
        return !failed;
    }

    /**
     * @return скомпилированный пайплайн для режима смешивания или {@code null}, если он недоступен
     */
    @Nullable RenderPipeline get(MinecraftUiBlend blend) {
        if (failed) return null;
        try {
            RenderPipeline pipeline = pipelines.computeIfAbsent(blend, factory);
            CompiledRenderPipeline compiled = COMPILED.get(pipeline);
            if (compiled == null || compiled.isClosed()) {
                // Синхронно на потоке рендера, как и ванильный кэш пайплайнов.
                compiled = RenderSystem.getDevice().compilePipeline(pipeline, source, Runnable::run)
                        .join().finishCompile();
                if (compiled == null) {
                    fail("pipeline " + pipeline.getLocation() + " failed to compile", null);
                    return null;
                }
                COMPILED.put(pipeline, compiled);
            }
            return pipeline;
        } catch (RuntimeException failure) {
            fail("pipeline setup failed", failure);
            return null;
        }
    }

    /**
     * @return скомпилированный пайплайн UniGUI для {@code pipeline} или {@code null}, если это не пайплайн UniGUI
     */
    static @Nullable CompiledRenderPipeline compiled(RenderPipeline pipeline) {
        return COMPILED.get(pipeline);
    }

    /**
     * Releases an evicted variant after queued draws have used its compiled pipelines.
     */
    void releaseAfterFrame() {
        if (pipelines.isEmpty()) return;
        RenderSystem.queueFencedTask(() -> {
            for (RenderPipeline pipeline : pipelines.values()) {
                CompiledRenderPipeline compiled = COMPILED.remove(pipeline);
                if (compiled != null) compiled.close();
            }
            pipelines.clear();
        });
    }

    /**
     * Отмечает успешную отрисовку: при первой пишет в журнал, каким бэкендом рисует путь.
     */
    void drawn() {
        if (announced) return;
        announced = true;
        LOGGER.info("UniGUI {} use RenderPipeline ({} backend)", name,
                RenderSystem.getDevice().getDeviceInfo().backendName());
    }

    /**
     * Отключает пайплайн до перезапуска.
     */
    void fail(String reason, @Nullable Throwable failure) {
        failed = true;
        if (failure == null) {
            LOGGER.error("UniGUI {} pipeline disabled: {}", name, reason);
        } else {
            LOGGER.error("UniGUI " + name + " pipeline disabled: " + reason, failure);
        }
    }

    /**
     * Создаёт неизменяемый вершинный буфер из float-значений.
     */
    static GpuBuffer staticVertices(String label, float... values) {
        ByteBuffer data = MemoryUtil.memAlloc(values.length * Float.BYTES);
        try {
            for (float value : values) data.putFloat(value);
            data.flip();
            return RenderSystem.getDevice().createBuffer(() -> label, GpuBuffer.USAGE_VERTEX, data);
        } finally {
            MemoryUtil.memFree(data);
        }
    }

    /**
     * Переводит вершинный шейдер UniGUI под пайплайн: {@code #version 330} и матрица проекции из блока
     * {@code Projection} (вершины UniGUI уже в координатах цели, ModelView единичная и убирается).
     */
    static String projectionVertex(String source, String owner) {
        String block = "layout(std140) uniform Projection {\n    mat4 ProjMat;\n};";
        String patched = source
                .replace("#version 150", "#version 330")
                .replace("uniform mat4 ModelViewMat;\nuniform mat4 ProjMat;", block)
                .replace("ProjMat * ModelViewMat * ", "ProjMat * ");
        if (!patched.contains(block) || patched.contains("ModelViewMat") || !patched.contains("#version 330")) {
            throw new IllegalStateException(owner + " vertex shader changed; update its pipeline patch");
        }
        return patched;
    }
}
