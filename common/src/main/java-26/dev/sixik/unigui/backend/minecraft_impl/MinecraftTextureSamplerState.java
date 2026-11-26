package dev.sixik.unigui.backend.minecraft_impl;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import dev.sixik.unigui.api.render.TextureFilter;
import dev.sixik.unigui.api.render.TextureOptions;
import dev.sixik.unigui.api.render.TextureWrap;

/**
 * Выбор сэмплера по {@link TextureOptions} для Minecraft 26.1.
 *
 * <p>В 1.20/1.21 параметры фильтрации выставлялись глобально (sampler object или {@code glTexParameter}) и
 * восстанавливались после отрисовки. В 26.1 сэмплер передаётся в каждую привязку текстуры явно, поэтому
 * восстанавливать ничего не нужно: класс только переводит опции UniGUI в {@link GpuSampler} из
 * {@code SamplerCache}.</p>
 *
 * <p>{@code MIRRORED_REPEAT} в 26.1 не поддерживается и заменяется на {@code REPEAT}.</p>
 */
final class MinecraftTextureSamplerState {
    private MinecraftTextureSamplerState() {
    }

    /**
     * Возвращает сэмплер для опций текстуры.
     *
     * @param options  опции текстуры ({@code null} = по умолчанию)
     * @param fallback сэмплер самой текстуры; используется, когда опции не заданы явно
     * @return сэмплер для привязки
     */
    static GpuSampler sampler(TextureOptions options, GpuSampler fallback) {
        TextureOptions normalized = options == null ? TextureOptions.defaults() : options;
        if (normalized.isDefault() && fallback != null) return fallback;
        return RenderSystem.getSamplerCache().getSampler(
                address(normalized.wrapS()),
                address(normalized.wrapT()),
                minFilter(normalized.minFilter()),
                magFilter(normalized.magFilter()),
                usesMipmaps(normalized.minFilter()));
    }

    /**
     * Сэмплер для опций без запасного варианта.
     */
    static GpuSampler sampler(TextureOptions options) {
        return sampler(options, null);
    }

    private static FilterMode minFilter(TextureFilter filter) {
        return switch (filter == null ? TextureFilter.NEAREST : filter) {
            case NEAREST, NEAREST_MIPMAP_NEAREST -> FilterMode.NEAREST;
            case LINEAR, LINEAR_MIPMAP_LINEAR -> FilterMode.LINEAR;
        };
    }

    private static FilterMode magFilter(TextureFilter filter) {
        return minFilter(filter);
    }

    private static boolean usesMipmaps(TextureFilter filter) {
        return filter == TextureFilter.NEAREST_MIPMAP_NEAREST || filter == TextureFilter.LINEAR_MIPMAP_LINEAR;
    }

    private static AddressMode address(TextureWrap wrap) {
        return switch (wrap == null ? TextureWrap.CLAMP_TO_EDGE : wrap) {
            case CLAMP_TO_EDGE -> AddressMode.CLAMP_TO_EDGE;
            case REPEAT, MIRRORED_REPEAT -> AddressMode.REPEAT;
        };
    }
}
