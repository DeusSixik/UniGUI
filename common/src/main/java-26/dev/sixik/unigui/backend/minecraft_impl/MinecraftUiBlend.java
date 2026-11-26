package dev.sixik.unigui.backend.minecraft_impl;

import com.mojang.blaze3d.pipeline.BlendFunction;
import dev.sixik.unigui.api.render.BlendMode;

/**
 * Режимы смешивания UniGUI для Minecraft 26.1.
 *
 * <p>В 1.20/1.21 blend выставлялся глобально через {@code RenderSystem.blendFuncSeparate}. В 26.1 он часть
 * {@link com.mojang.blaze3d.pipeline.RenderPipeline}, поэтому здесь режим только выбирается, а пайплайн с нужной
 * {@link BlendFunction} берёт {@link UiPipelines}.</p>
 *
 * <p>В 26.1 UniGUI всегда рисует в текстуры с premultiplied alpha (слои экрана и offscreen-цели), поэтому для
 * обычного (straight) источника используется формула {@code rgb = src.rgb*a + dst.rgb*(1-a)},
 * {@code alpha = src.a + dst.a*(1-src.a)}: результат остаётся premultiplied.</p>
 */
enum MinecraftUiBlend {
    /** Источник со straight alpha. */
    STRAIGHT(BlendFunction.TRANSLUCENT),
    /** Источник с premultiplied alpha. */
    PREMULTIPLIED(BlendFunction.TRANSLUCENT_PREMULTIPLIED_ALPHA),
    /** Аддитивное смешивание: {@code rgb += src.rgb*a}, alpha насыщается. */
    ADDITIVE(MinecraftGpuCompat.ADDITIVE),
    /** Без смешивания: источник заменяет содержимое цели. */
    REPLACE(null);

    private final BlendFunction function;

    MinecraftUiBlend(BlendFunction function) {
        this.function = function;
    }

    /**
     * @return функция смешивания или {@code null}, если смешивание выключено
     */
    BlendFunction function() {
        return function;
    }

    /**
     * Режим для цветной геометрии.
     *
     * @param renderingToPremultipliedTarget сохранено ради совместимости сигнатуры: в 26.1 цель всегда premultiplied
     * @return режим смешивания
     */
    static MinecraftUiBlend straight(boolean renderingToPremultipliedTarget) {
        return STRAIGHT;
    }

    /**
     * Режим для цветной геометрии с учётом {@link BlendMode}.
     */
    static MinecraftUiBlend straight(boolean renderingToPremultipliedTarget, BlendMode blendMode) {
        return blendMode == BlendMode.ADDITIVE ? ADDITIVE : STRAIGHT;
    }

    /**
     * Режим для текстуры.
     *
     * @param premultipliedSource            текстура хранит premultiplied alpha
     * @param renderingToPremultipliedTarget сохранено ради совместимости сигнатуры
     * @return режим смешивания
     */
    static MinecraftUiBlend texture(boolean premultipliedSource, boolean renderingToPremultipliedTarget) {
        return premultipliedSource ? PREMULTIPLIED : STRAIGHT;
    }

    /**
     * Режим для текстуры с учётом {@link BlendMode}.
     */
    static MinecraftUiBlend texture(boolean premultipliedSource, boolean renderingToPremultipliedTarget, BlendMode blendMode) {
        if (blendMode == BlendMode.ADDITIVE) return ADDITIVE;
        return premultipliedSource ? PREMULTIPLIED : STRAIGHT;
    }
}
