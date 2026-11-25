package dev.sixik.unigui.backend.minecraft_impl;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.BindGroupLayout;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.sixik.unigui.impl.render.shaders.SdfTextShaderSources;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.resources.Identifier;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

/**
 * SDF-текст через ванильный {@link RenderPipeline} (Minecraft 26.2): работает и на OpenGL, и на Vulkan.
 *
 * <p>Шейдеры — {@link SdfTextShaderSources}, переведённые под пайплайн: проекция из блока {@code Projection}, а
 * {@code SdfSpread} становится константой (это всегда {@link SdfTextShaderSources#SPREAD}, отдельный uniform-блок
 * ради одного числа не нужен). Атлас глифов привязывается сэмплером {@code GlyphAtlas}.</p>
 *
 * <p>Каждая страница атласа рисуется одним instanced-вызовом: привязка 0 — единичный квад из двух треугольников,
 * привязка 1 — по {@link SdfTextShaderSources#FLOATS_PER_INSTANCE} float на глиф.</p>
 *
 * <p>{@code -Dunigui.sdf.textPipeline=false} выключает этот путь.</p>
 */
final class UiSdfTextPipeline {
    private static final boolean ENABLED = UiCustomPipeline.enabled("unigui.sdf.textPipeline");
    private static final Identifier SHADER = Identifier.fromNamespaceAndPath("unigui", "sdf_text_instanced");
    private static final BindGroupLayout GLYPH_ATLAS = BindGroupLayout.builder().withSampler("GlyphAtlas").build();

    /** Единичный квад из двух треугольников, тот же порядок, что у прямого GL-пути. */
    private static final VertexFormat QUAD_FORMAT = VertexFormat.builder(0)
            .addAttribute("Position", GpuFormat.RG32_FLOAT)
            .build();

    /** Данные глифа, шаг — экземпляр; порядок совпадает с {@link SdfTextShaderSources#INSTANCE_ATTRIBUTE_NAMES}. */
    private static final VertexFormat INSTANCE_FORMAT = VertexFormat.builder(1)
            .addAttribute("InstancePosition0", GpuFormat.RGB32_FLOAT)
            .addAttribute("InstancePosition1", GpuFormat.RGB32_FLOAT)
            .addAttribute("InstancePosition2", GpuFormat.RGB32_FLOAT)
            .addAttribute("InstancePosition3", GpuFormat.RGB32_FLOAT)
            .addAttribute("InstanceUv", GpuFormat.RGBA32_FLOAT)
            .addAttribute("InstanceColor0", GpuFormat.RGBA32_FLOAT)
            .addAttribute("InstanceColor1", GpuFormat.RGBA32_FLOAT)
            .addAttribute("InstanceColor2", GpuFormat.RGBA32_FLOAT)
            .addAttribute("InstanceColor3", GpuFormat.RGBA32_FLOAT)
            .build();

    private static final UiCustomPipeline PIPELINE = new UiCustomPipeline("SDF text", SHADER,
            UiCustomPipeline.projectionVertex(SdfTextShaderSources.VERTEX, "SDF text"),
            fragment(),
            UiSdfTextPipeline::build);

    private static GpuBuffer quad;

    private UiSdfTextPipeline() {
    }

    /**
     * @return {@code true}, если путь через пайплайн включён и ещё не отказал
     */
    static boolean available() {
        return ENABLED && PIPELINE.usable();
    }

    /**
     * Рисует глифы одной страницы атласа в текущую цель {@link UiGpu}.
     *
     * @param blend     режим смешивания
     * @param atlas     страница атласа
     * @param sampler   сэмплер атласа
     * @param instances данные глифов (позиция..limit)
     * @param count     число глифов
     * @return {@code false}, если путь недоступен или отказал (вызывающий рисует запасным путём)
     */
    static boolean draw(MinecraftUiBlend blend, GpuTextureView atlas, GpuSampler sampler,
                        FloatBuffer instances, int count) {
        if (!available()) return false;
        if (count <= 0) return true;
        RenderPipeline pipeline = PIPELINE.get(blend);
        if (pipeline == null) return false;
        try {
            ByteBuffer bytes = MemoryUtil.memByteBuffer(MemoryUtil.memAddress(instances),
                    instances.remaining() * Float.BYTES);
            GpuBufferSlice instanceSlice = UiGpu.uploadVertices(bytes, 16);
            if (quad == null || quad.isClosed()) {
                quad = UiCustomPipeline.staticVertices("UniGUI SDF text quad",
                        0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f,
                        0.0f, 0.0f, 1.0f, 1.0f, 1.0f, 0.0f);
            }
            GpuBuffer quadBuffer = quad;
            boolean drawn = UiGpu.drawPass(pipeline, pass -> {
                pass.bindTexture("GlyphAtlas", atlas, sampler);
                pass.setVertexBuffer(0, quadBuffer.slice());
                pass.setVertexBuffer(1, instanceSlice);
                pass.draw(6, count, 0, 0);
            });
            if (drawn) PIPELINE.drawn();
            return drawn;
        } catch (RuntimeException failure) {
            PIPELINE.fail("draw failed", failure);
            return false;
        }
    }

    private static String fragment() {
        String patched = SdfTextShaderSources.FRAGMENT
                .replace("#version 150", "#version 330")
                .replace("uniform float SdfSpread;",
                        "const float SdfSpread = " + (float) SdfTextShaderSources.SPREAD + ";");
        if (patched.contains("uniform float") || !patched.contains("#version 330")) {
            throw new IllegalStateException("SDF text fragment shader changed; update UiSdfTextPipeline");
        }
        return patched;
    }

    private static RenderPipeline build(MinecraftUiBlend blend) {
        BlendFunction function = blend.function();
        return RenderPipeline.builder()
                .withLocation(Identifier.fromNamespaceAndPath("unigui",
                        "pipeline/sdf_text_" + blend.name().toLowerCase()))
                .withBindGroupLayout(BindGroupLayouts.GLOBALS)
                .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
                .withBindGroupLayout(GLYPH_ATLAS)
                .withVertexShader(SHADER)
                .withFragmentShader(SHADER)
                .withColorTargetState(function == null ? ColorTargetState.DEFAULT : new ColorTargetState(function))
                .withCull(false)
                .withVertexBinding(0, QUAD_FORMAT)
                .withVertexBinding(1, INSTANCE_FORMAT)
                .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
                .build();
    }
}
