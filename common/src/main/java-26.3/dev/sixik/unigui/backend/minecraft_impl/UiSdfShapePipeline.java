package dev.sixik.unigui.backend.minecraft_impl;

import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.vertex.VertexFormat;
import dev.sixik.unigui.impl.render.shaders.SdfShapeShaderSources;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.resources.Identifier;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

/**
 * SDF-фигуры через ванильный {@link RenderPipeline} (Minecraft 26.2): работает и на OpenGL, и на Vulkan.
 *
 * <p>Шейдеры те же, что у прямого GL-пути ({@link SdfShapeShaderSources}), переведённые под пайплайн
 * ({@link UiCustomPipeline#projectionVertex}). Batch рисуется одним instanced-вызовом: привязка 0 — единичный квад
 * (triangle strip из 4 вершин), привязка 1 — по одной записи из {@link SdfShapeShaderSources#INSTANCE_FLOATS}
 * float на фигуру.</p>
 *
 * <p>{@code -Dunigui.sdf.pipeline=false} выключает этот путь (тогда на OpenGL работает прежний прямой GL-путь).</p>
 */
final class UiSdfShapePipeline {
    private static final boolean ENABLED = UiCustomPipeline.enabled("unigui.sdf.pipeline");
    private static final Identifier SHADER = Identifier.fromNamespaceAndPath("unigui", "sdf_shape_instanced");

    /** Единичный квад: {@code Position} в {@code [0, 1]^2}, шаг — вершина. */
    private static final VertexFormat QUAD_FORMAT = VertexFormat.builder(0)
            .addAttribute("Position", GpuFormat.RG32_FLOAT)
            .build();

    /** Данные фигуры, шаг — экземпляр; порядок и размеры совпадают с упаковкой {@code MinecraftSdfShapeRenderer}. */
    private static final VertexFormat INSTANCE_FORMAT = VertexFormat.builder(1)
            .addAttribute("InstancePosition0", GpuFormat.RGB32_FLOAT)
            .addAttribute("InstancePosition1", GpuFormat.RGB32_FLOAT)
            .addAttribute("InstancePosition2", GpuFormat.RGB32_FLOAT)
            .addAttribute("InstancePosition3", GpuFormat.RGB32_FLOAT)
            .addAttribute("InstanceLocalBounds", GpuFormat.RGBA32_FLOAT)
            .addAttribute("InstanceColor", GpuFormat.RGBA32_FLOAT)
            .addAttribute("InstanceSize", GpuFormat.RG32_FLOAT)
            .addAttribute("InstanceLineStart", GpuFormat.RG32_FLOAT)
            .addAttribute("InstanceLineEnd", GpuFormat.RG32_FLOAT)
            .addAttribute("InstanceShape", GpuFormat.RG32_FLOAT)
            .addAttribute("InstanceFlags", GpuFormat.RG32_FLOAT)
            .addAttribute("InstanceEndColor", GpuFormat.RGBA32_FLOAT)
            .build();

    private static final UiCustomPipeline PIPELINE = new UiCustomPipeline("SDF shapes", SHADER,
            UiCustomPipeline.projectionVertex(SdfShapeShaderSources.INSTANCED_VERTEX, "SDF shape"),
            SdfShapeShaderSources.INSTANCED_FRAGMENT.replace("#version 150", "#version 330"),
            UiSdfShapePipeline::build);

    private static GpuBuffer quad;

    private UiSdfShapePipeline() {
    }

    /**
     * @return {@code true}, если путь через пайплайн включён и ещё не отказал
     */
    static boolean available() {
        return ENABLED && PIPELINE.usable();
    }

    /**
     * Рисует упакованные фигуры в текущую цель {@link UiGpu}.
     *
     * @param blend     режим смешивания
     * @param instances данные экземпляров (позиция..limit), {@link SdfShapeShaderSources#INSTANCE_FLOATS} float на фигуру
     * @param count     число фигур
     * @return {@code false}, если путь недоступен или отказал (вызывающий рисует запасным путём)
     */
    static boolean draw(MinecraftUiBlend blend, FloatBuffer instances, int count) {
        if (!available()) return false;
        if (count <= 0) return true;
        RenderPipeline pipeline = PIPELINE.get(blend);
        if (pipeline == null) return false;
        try {
            ByteBuffer bytes = MemoryUtil.memByteBuffer(MemoryUtil.memAddress(instances),
                    instances.remaining() * Float.BYTES);
            GpuBufferSlice instanceSlice = UiGpu.uploadVertices(bytes, 16);
            if (quad == null || quad.isClosed()) {
                // Тот же порядок обхода, что у прямого GL-пути: (0,0) (0,1) (1,0) (1,1).
                quad = UiCustomPipeline.staticVertices("UniGUI SDF shape quad",
                        0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 1.0f, 1.0f);
            }
            GpuBuffer quadBuffer = quad;
            boolean drawn = UiGpu.drawPass(pipeline, pass -> {
                pass.setVertexBuffer(0, quadBuffer.slice());
                pass.setVertexBuffer(1, instanceSlice);
                pass.draw(4, count, 0, 0);
            });
            if (drawn) PIPELINE.drawn();
            return drawn;
        } catch (RuntimeException failure) {
            PIPELINE.fail("draw failed", failure);
            return false;
        }
    }

    private static RenderPipeline build(MinecraftUiBlend blend) {
        BlendFunction function = blend.function();
        return RenderPipeline.builder()
                .withLocation(Identifier.fromNamespaceAndPath("unigui",
                        "pipeline/sdf_shape_" + blend.name().toLowerCase()))
                .withBindGroupLayout(BindGroupLayouts.GLOBALS)
                .withBindGroupLayout(BindGroupLayouts.PROJECTION)
                .withVertexShader(SHADER)
                .withFragmentShader(SHADER)
                .withColorTargetState(function == null ? ColorTargetState.DEFAULT : new ColorTargetState(function))
                .withCull(false)
                .withVertexBinding(0, QUAD_FORMAT)
                .withVertexBinding(1, INSTANCE_FORMAT)
                .withPrimitiveTopology(PrimitiveTopology.TRIANGLE_STRIP)
                .build();
    }
}
