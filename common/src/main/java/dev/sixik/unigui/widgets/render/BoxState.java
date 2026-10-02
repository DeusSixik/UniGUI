package dev.sixik.unigui.widgets.render;

import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.render.ImageFit;
import dev.sixik.unigui.api.render.TextureHandle;
import dev.sixik.unigui.api.render.TexturePlacement;
import dev.sixik.unigui.api.render.shaders.ShaderDrawOptions;
import dev.sixik.unigui.api.render.shaders.ShaderHandle;
import dev.sixik.unigui.api.render.shaders.ShaderUniforms;
import dev.sixik.unigui.api.widget.visual.BackgroundKind;
import dev.sixik.unigui.api.widget.visual.SurfaceSnapshot;

/**
 * Снимок поверхности {@code Box} для рендера.
 *
 * <p>Расширен {@link BackgroundKind} и шейдером: стиль может переключать источник
 * фона ({@code background.kind}) без смены renderer-класса. Для нового кода
 * используйте {@link SurfaceSnapshot}; этот record оставлен как совместимый
 * фасад с конвертацией.</p>
 */
public record BoxState(
        float x,
        float y,
        float width,
        float height,
        boolean backgroundVisible,
        ColorView background,
        TextureHandle backgroundTexture,
        ColorView backgroundTextureTint,
        TexturePlacement backgroundTexturePlacement,
        ImageFit backgroundTextureFit,
        float radius,
        boolean borderVisible,
        ColorView borderColor,
        float borderWidth,
        BackgroundKind backgroundKind,
        ShaderHandle backgroundShader,
        ShaderUniforms shaderUniforms,
        ShaderDrawOptions shaderOptions
) {
    public BoxState(
            float x,
            float y,
            float width,
            float height,
            boolean backgroundVisible,
            ColorView background,
            TextureHandle backgroundTexture,
            ColorView backgroundTextureTint,
            TexturePlacement backgroundTexturePlacement,
            ImageFit backgroundTextureFit,
            float radius,
            boolean borderVisible,
            ColorView borderColor,
            float borderWidth) {
        this(x, y, width, height, backgroundVisible, background, backgroundTexture,
                backgroundTextureTint, backgroundTexturePlacement, backgroundTextureFit,
                radius, borderVisible, borderColor, borderWidth,
                backgroundVisible
                        ? (backgroundTexture != null ? BackgroundKind.TEXTURE : BackgroundKind.COLOR)
                        : BackgroundKind.NONE,
                null, null, null);
    }

    /** Конвертирует в единый {@link SurfaceSnapshot}. */
    public SurfaceSnapshot toSurface() {
        return new SurfaceSnapshot(x, y, width, height,
                backgroundKind, backgroundVisible,
                background, backgroundTexture, backgroundTextureTint,
                backgroundTexturePlacement, backgroundTextureFit,
                backgroundShader, shaderUniforms, shaderOptions,
                radius, borderVisible, borderColor, borderWidth);
    }

    /** Создаёт из единого {@link SurfaceSnapshot}. */
    public static BoxState fromSurface(SurfaceSnapshot surface) {
        if (surface == null) return null;
        return new BoxState(surface.x(), surface.y(), surface.width(), surface.height(),
                surface.backgroundVisible(), surface.background(),
                surface.backgroundTexture(), surface.backgroundTextureTint(),
                surface.backgroundTexturePlacement(), surface.backgroundTextureFit(),
                surface.radius(), surface.borderVisible(), surface.borderColor(), surface.borderWidth(),
                surface.kind(), surface.backgroundShader(),
                surface.shaderUniforms(), surface.shaderOptions());
    }
}
