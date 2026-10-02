package dev.sixik.unigui.api.widget.visual;

import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.render.ImageFit;
import dev.sixik.unigui.api.render.TextureHandle;
import dev.sixik.unigui.api.render.TexturePlacement;
import dev.sixik.unigui.api.render.shaders.ShaderDrawOptions;
import dev.sixik.unigui.api.render.shaders.ShaderHandle;
import dev.sixik.unigui.api.render.shaders.ShaderUniforms;

/**
 * Immutable снимок поверхности виджета для рендера.
 *
 * <p>Единый state для фона и рамки любого виджета. Заменяет разрозненные
 * {@code BoxState} / поля фона в {@code ButtonState} / {@code CheckboxRenderState}
 * и дубли полей фона в {@code TextWidget}. Контент (текст, индикатор, knob)
 * в этот snapshot не входит — его рисуют content-рендеры поверх.</p>
 *
 * @param x X-граница поверхности
 * @param y Y-граница поверхности
 * @param width ширина поверхности
 * @param height высота поверхности
 * @param kind выбранный источник фона
 * @param backgroundVisible рисуется ли фон (kind != NONE)
 * @param background цвет плоской заливки
 * @param backgroundTexture текстура фона
 * @param backgroundTextureTint tint текстуры
 * @param backgroundTexturePlacement рассчитанный placement текстуры
 * @param backgroundTextureFit fit-режим текстуры
 * @param backgroundShader шейдер фона
 * @param shaderUniforms uniforms шейдера
 * @param shaderOptions опции отрисовки шейдера
 * @param radius радиус скругления
 * @param borderVisible рисуется ли рамка
 * @param borderColor цвет рамки
 * @param borderWidth толщина рамки
 */
public record SurfaceSnapshot(
        float x,
        float y,
        float width,
        float height,
        BackgroundKind kind,
        boolean backgroundVisible,
        ColorView background,
        TextureHandle backgroundTexture,
        ColorView backgroundTextureTint,
        TexturePlacement backgroundTexturePlacement,
        ImageFit backgroundTextureFit,
        ShaderHandle backgroundShader,
        ShaderUniforms shaderUniforms,
        ShaderDrawOptions shaderOptions,
        float radius,
        boolean borderVisible,
        ColorView borderColor,
        float borderWidth
) {
    public SurfaceSnapshot {
        kind = kind == null ? BackgroundKind.NONE : kind;
        backgroundVisible = backgroundVisible && kind != BackgroundKind.NONE;
        radius = Math.max(0.0f, radius);
        borderWidth = Math.max(0.0f, borderWidth);
    }

    /** Пустой невидимый snapshot. */
    public static SurfaceSnapshot empty(float x, float y, float width, float height) {
        return new SurfaceSnapshot(x, y, width, height,
                BackgroundKind.NONE, false,
                null, null, null, null, ImageFit.STRETCH,
                null, null, null,
                0.0f, false, null, 0.0f);
    }
}
