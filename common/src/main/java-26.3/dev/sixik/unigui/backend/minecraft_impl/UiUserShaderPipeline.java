package dev.sixik.unigui.backend.minecraft_impl;

import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.pipeline.BindGroupLayout;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.pipeline.UniformType;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.renderpearl.api.textures.GpuSampler;
import com.mojang.renderpearl.api.textures.GpuTexture;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import dev.sixik.unigui.impl.render.shaders.GlslUniformBlocks;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Пользовательские шейдеры UniGUI через ванильный {@link RenderPipeline} (Minecraft 26.2): OpenGL и Vulkan.
 *
 * <p>Шейдер приходит уже сконвертированным ({@link GlslUniformBlocks}): обычные {@code uniform} собраны в один
 * std140-блок, версия не ниже 330. Для каждого шейдера создаётся свой {@link UiCustomPipeline} (свой
 * идентификатор шейдера, пайплайн на режим смешивания), в группы пайплайна добавляются этот блок и все сэмплеры
 * шейдера. Ошибка компиляции отключает только этот шейдер.</p>
 *
 * <p>Геометрия та же, что у прямого GL-пути: полноэкранный квад в NDC ({@code Position} от -1 до 1), позицию на
 * экране шейдер считает сам. Vulkan-бэкенд Minecraft хранит кадр в той же ориентации, что и OpenGL (переворот
 * делается при выводе на экран), поэтому NDC, {@code gl_FragCoord} и UV текстур совпадают с OpenGL.</p>
 *
 * <p>Сэмплеры, для которых нет текстуры, получают прозрачную текстуру 1x1: пайплайны 26.2 требуют, чтобы каждый
 * объявленный сэмплер был привязан. {@code -Dunigui.shader.pipeline=false} выключает этот путь.</p>
 */
final class UiUserShaderPipeline {
    private static final boolean ENABLED = UiCustomPipeline.enabled("unigui.shader.pipeline");
    private static final Matrix4f IDENTITY = new Matrix4f();

    private static final Int2ObjectMap<UiCustomPipeline> PIPELINES = new Int2ObjectOpenHashMap<>();
    private static GpuTexture blank;
    private static GpuTextureView blankView;

    private UiUserShaderPipeline() {
    }

    /**
     * @return {@code true}, если путь через пайплайн включён
     */
    static boolean available() {
        return ENABLED;
    }

    /**
     * Рисует пользовательский шейдер на всю текущую цель {@link UiGpu}.
     *
     * @param key         ключ исходника (одинаковый для одинакового исходника)
     * @param id          идентификатор шейдера UniGUI (для журнала и меток)
     * @param shader      сконвертированный шейдер
     * @param blend       режим смешивания
     * @param uniformData данные uniform-блока (позиция..limit) или {@code null}, если блока нет
     * @param textures    текстуры по имени сэмплера
     * @return {@code false}, если шейдер нельзя нарисовать этим путём (вызывающий рисует запасным путём)
     */
    static boolean draw(int key, String id, GlslUniformBlocks.Result shader, MinecraftUiBlend blend,
                        @Nullable ByteBuffer uniformData, Map<String, UiTextureBinding> textures) {
        if (!ENABLED) return false;
        UiCustomPipeline custom = PIPELINES.computeIfAbsent(key, ignored -> create(key, id, shader));
        RenderPipeline pipeline = custom.get(blend);
        if (pipeline == null) return false;

        List<UiGpu.UniformBind> uniforms = List.of();
        if (shader.hasBlock()) {
            if (uniformData == null) return false;
            GpuBufferSlice slice = UiGpu.uploadUniform(uniformData);
            uniforms = List.of(new UiGpu.UniformBind(shader.blockName(), slice));
        }

        List<UiGpu.TextureBind> binds = new ArrayList<>(shader.samplers().size());
        for (String name : shader.samplers()) {
            UiTextureBinding binding = textures.get(name);
            if (binding != null && binding.view() != null && !binding.view().isClosed()) {
                binds.add(new UiGpu.TextureBind(name, binding.view(), binding.sampler()));
            } else {
                binds.add(new UiGpu.TextureBind(name, blankView(), blankSampler()));
            }
        }

        Object buffer = MinecraftBufferCompat.begin(UiPrimitive.QUADS, DefaultVertexFormat.POSITION);
        MinecraftBufferCompat.vertex(buffer, IDENTITY, -1.0f, 1.0f, 0.0f);
        MinecraftBufferCompat.vertex(buffer, IDENTITY, -1.0f, -1.0f, 0.0f);
        MinecraftBufferCompat.vertex(buffer, IDENTITY, 1.0f, -1.0f, 0.0f);
        MinecraftBufferCompat.vertex(buffer, IDENTITY, 1.0f, 1.0f, 0.0f);
        MeshData mesh = MinecraftBufferCompat.build(buffer);
        UiGpu.drawMesh(pipeline, mesh, binds, uniforms);
        custom.drawn();
        return true;
    }

    private static UiCustomPipeline create(int key, String id, GlslUniformBlocks.Result shader) {
        String suffix = Integer.toHexString(key);
        Identifier shaderId = Identifier.fromNamespaceAndPath("unigui", "user_shader/" + suffix);
        return new UiCustomPipeline("shader " + id, shaderId,
                shader.vertex(), shader.fragment(),
                blend -> build(shaderId, suffix, shader, blend));
    }

    private static RenderPipeline build(Identifier shaderId, String suffix, GlslUniformBlocks.Result shader,
                                        MinecraftUiBlend blend) {
        BlendFunction function = blend.function();
        RenderPipeline.Builder builder = RenderPipeline.builder()
                .withLocation(Identifier.fromNamespaceAndPath("unigui",
                        "pipeline/user_shader_" + suffix + "_" + blend.name().toLowerCase()))
                .withBindGroupLayout(net.minecraft.client.renderer.BindGroupLayouts.GLOBALS)
                .withBindGroupLayout(net.minecraft.client.renderer.BindGroupLayouts.PROJECTION)
                .withVertexShader(shaderId)
                .withFragmentShader(shaderId)
                .withColorTargetState(function == null ? ColorTargetState.DEFAULT : new ColorTargetState(function))
                .withCull(false)
                .withVertexBinding(0, DefaultVertexFormat.POSITION)
                .withPrimitiveTopology(PrimitiveTopology.QUADS);
        if (shader.hasBlock()) {
            builder.withBindGroupLayout(BindGroupLayout.builder()
                    .withUniform(shader.blockName(), UniformType.UNIFORM_BUFFER)
                    .build());
        }
        if (!shader.samplers().isEmpty()) {
            BindGroupLayout.Builder samplers = BindGroupLayout.builder();
            for (String name : shader.samplers())
                samplers.withUniform(name, com.mojang.renderpearl.api.pipeline.UniformType.COMBINED_IMAGE_SAMPLER);
            builder.withBindGroupLayout(samplers.build());
        }
        return builder.build();
    }

    private static GpuTextureView blankView() {
        if (blank == null || blank.isClosed()) {
            blank = MinecraftGpuCompat.createRgbaTexture(() -> "UniGUI blank shader texture",
                    GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_COPY_DST, 1, 1);
            ByteBuffer zero = MemoryUtil.memCalloc(4);
            try {
                RenderSystem.getDevice().createCommandEncoder().writeToTexture(blank, zero, 0, 0, 0, 0, 1, 1);
            } finally {
                MemoryUtil.memFree(zero);
            }
            blankView = RenderSystem.getDevice().createTextureView(blank);
        }
        return blankView;
    }

    private static GpuSampler blankSampler() {
        return RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST);
    }
}
