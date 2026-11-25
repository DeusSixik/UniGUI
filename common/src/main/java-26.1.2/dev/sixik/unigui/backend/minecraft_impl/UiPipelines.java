package dev.sixik.unigui.backend.minecraft_impl;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.resources.Identifier;

import java.util.EnumMap;
import java.util.Map;

/**
 * Пайплайны UniGUI для Minecraft 26.1 на ванильных шейдерах {@code core/position_color} и
 * {@code core/position_tex_color}.
 *
 * <p>Пайплайн в 26.1 фиксирует шейдер, формат вершин, режим примитивов и blend. Поэтому на каждую комбинацию
 * «вид геометрии + режим смешивания + режим примитивов» создаётся свой пайплайн; они кэшируются и живут всё
 * время работы клиента. Глубина выключена, отсечение граней выключено (UI может рисовать треугольники в любом
 * порядке обхода).</p>
 */
final class UiPipelines {
    private static final Map<MinecraftUiBlend, Map<VertexFormat.Mode, RenderPipeline>> COLOR = new EnumMap<>(MinecraftUiBlend.class);
    private static final Map<MinecraftUiBlend, Map<VertexFormat.Mode, RenderPipeline>> TEXTURE = new EnumMap<>(MinecraftUiBlend.class);

    private UiPipelines() {
    }

    /**
     * Пайплайн цветной геометрии ({@code POSITION_COLOR}).
     */
    static RenderPipeline color(MinecraftUiBlend blend, VertexFormat.Mode mode) {
        return COLOR.computeIfAbsent(blend, ignored -> new EnumMap<>(VertexFormat.Mode.class))
                .computeIfAbsent(mode, ignored -> build("color", "core/position_color", false,
                        DefaultVertexFormat.POSITION_COLOR, mode, blend));
    }

    /**
     * Пайплайн текстурированной геометрии ({@code POSITION_TEX_COLOR}, сэмплер {@code Sampler0}).
     */
    static RenderPipeline texture(MinecraftUiBlend blend, VertexFormat.Mode mode) {
        return TEXTURE.computeIfAbsent(blend, ignored -> new EnumMap<>(VertexFormat.Mode.class))
                .computeIfAbsent(mode, ignored -> build("texture", "core/position_tex_color", true,
                        DefaultVertexFormat.POSITION_TEX_COLOR, mode, blend));
    }

    /**
     * Подготовка пайплайна перед отрисовкой. Пайплайны на ванильных шейдерах компилируются самим Minecraft
     * при первом использовании, поэтому сейчас здесь ничего не делается; метод оставлен точкой расширения.
     */
    static void prepare(RenderPipeline pipeline) {
    }

    private static RenderPipeline build(String kind, String shader, boolean textured, VertexFormat format,
                                        VertexFormat.Mode mode, MinecraftUiBlend blend) {
        BlendFunction function = blend.function();
        RenderPipeline.Builder builder = RenderPipeline.builder()
                .withLocation(Identifier.fromNamespaceAndPath("unigui",
                        "pipeline/" + kind + "_" + blend.name().toLowerCase() + "_" + mode.name().toLowerCase()))
                .withVertexShader(shader)
                .withFragmentShader(shader)
                .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
                .withUniform("Projection", UniformType.UNIFORM_BUFFER)
                .withColorTargetState(function == null ? ColorTargetState.DEFAULT : new ColorTargetState(function))
                .withCull(false)
                .withVertexFormat(format, mode);
        if (textured) builder.withSampler("Sampler0");
        return builder.build();
    }
}
