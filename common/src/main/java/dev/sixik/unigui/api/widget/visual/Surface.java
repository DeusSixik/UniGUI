package dev.sixik.unigui.api.widget.visual;

import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.render.ImageFit;
import dev.sixik.unigui.api.render.TextureHandle;
import dev.sixik.unigui.api.render.shaders.ShaderDrawOptions;
import dev.sixik.unigui.api.render.shaders.ShaderHandle;
import dev.sixik.unigui.api.render.shaders.ShaderUniforms;

/**
 * Изменяемая поверхность виджета: фон (цвет / текстура / шейдер) + рамка + радиус.
 *
 * <p>Композируемый блок, заменяющий наследование визуала от {@code Box}.
 * {@code Box}, {@code Button}, {@code TextWidget} и остальные контролы содержат
 * {@code Surface}, а не наследуют и не копируют его поля. Стиль может переключать
 * {@link BackgroundKind} без смены рендера — один и тот же {@link dev.sixik.unigui.api.widget.render.WidgetRender}
 * рисует любой kind.</p>
 */
public final class Surface {
    private final Runnable onChanged;
    private final MutableColor background = new MutableColor(0.0f, 0.0f, 0.0f, 0.0f);
    private final MutableColor backgroundTextureTint = new MutableColor(1.0f, 1.0f, 1.0f, 1.0f);
    private final MutableRect backgroundTextureSource = new MutableRect(0.0f, 0.0f, 1.0f, 1.0f);
    private final MutableColor borderColor = new MutableColor(1.0f, 1.0f, 1.0f, 1.0f);
    private TextureHandle backgroundTexture;
    private ImageFit backgroundTextureFit = ImageFit.STRETCH;
    private ShaderHandle backgroundShader;
    private ShaderUniforms shaderUniforms;
    private ShaderDrawOptions shaderOptions;
    private BackgroundKind backgroundKind;
    private boolean backgroundVisible;
    private boolean borderVisible;
    private float borderWidth = 1.0f;
    private float radius;

    /**
     * Создаёт поверхность с callback инвалидации.
     *
     * @param onChanged вызывается при любом изменении визуала
     */
    public Surface(Runnable onChanged) {
        this.onChanged = onChanged == null ? () -> {
        } : onChanged;
        background.onChanged(this::fireChanged);
        backgroundTextureTint.onChanged(this::fireChanged);
        backgroundTextureSource.onChanged(this::fireChanged);
        borderColor.onChanged(this::fireChanged);
    }

    private void fireChanged() {
        onChanged.run();
    }

    /** @return live-цвет фона */
    public MutableColor background() {
        return background;
    }

    /** @return live tint-цвет текстуры */
    public MutableColor backgroundTextureTint() {
        return backgroundTextureTint;
    }

    /** @return live UV source-rect текстуры */
    public MutableRect backgroundTextureSource() {
        return backgroundTextureSource;
    }

    /** @return live-цвет рамки */
    public MutableColor borderColor() {
        return borderColor;
    }

    /** @return текстура фона или {@code null} */
    public TextureHandle backgroundTexture() {
        return backgroundTexture;
    }

    /** Задаёт текстуру фона. */
    public Surface backgroundTexture(TextureHandle texture) {
        if (this.backgroundTexture == texture) return this;
        this.backgroundTexture = texture;
        fireChanged();
        return this;
    }

    /** @return fit-режим текстуры */
    public ImageFit backgroundTextureFit() {
        return backgroundTextureFit;
    }

    /** Задаёт fit-режим текстуры. */
    public Surface backgroundTextureFit(ImageFit fit) {
        ImageFit effective = fit == null ? ImageFit.STRETCH : fit;
        if (backgroundTextureFit == effective) return this;
        backgroundTextureFit = effective;
        fireChanged();
        return this;
    }

    /** @return шейдер фона или {@code null} */
    public ShaderHandle backgroundShader() {
        return backgroundShader;
    }

    /** Задаёт шейдер фона. */
    public Surface backgroundShader(ShaderHandle shader) {
        if (this.backgroundShader == shader) return this;
        this.backgroundShader = shader;
        fireChanged();
        return this;
    }

    /** @return uniforms шейдера */
    public ShaderUniforms shaderUniforms() {
        return shaderUniforms;
    }

    /** Задаёт uniforms шейдера. */
    public Surface shaderUniforms(ShaderUniforms uniforms) {
        this.shaderUniforms = uniforms;
        fireChanged();
        return this;
    }

    /** @return опции отрисовки шейдера */
    public ShaderDrawOptions shaderOptions() {
        return shaderOptions;
    }

    /** Задаёт опции отрисовки шейдера. */
    public Surface shaderOptions(ShaderDrawOptions options) {
        this.shaderOptions = options;
        fireChanged();
        return this;
    }

    /**
     * @return явный kind фона или {@code null}, если kind выводится автоматически
     */
    public BackgroundKind backgroundKind() {
        return backgroundKind;
    }

    /** Задаёт явный kind фона; {@code null} включает автовывод. */
    public Surface backgroundKind(BackgroundKind kind) {
        if (this.backgroundKind == kind) return this;
        this.backgroundKind = kind;
        fireChanged();
        return this;
    }

    /** @return рисуется ли фон */
    public boolean backgroundVisible() {
        return backgroundVisible;
    }

    /** Включает или выключает фон. */
    public Surface backgroundVisible(boolean visible) {
        if (this.backgroundVisible == visible) return this;
        this.backgroundVisible = visible;
        fireChanged();
        return this;
    }

    /** @return рисуется ли рамка */
    public boolean borderVisible() {
        return borderVisible;
    }

    /** Включает или выключает рамку. */
    public Surface borderVisible(boolean visible) {
        if (this.borderVisible == visible) return this;
        this.borderVisible = visible;
        fireChanged();
        return this;
    }

    /** @return толщина рамки */
    public float borderWidth() {
        return borderWidth;
    }

    /** Задаёт толщину рамки. */
    public Surface borderWidth(float width) {
        if (this.borderWidth == width) return this;
        this.borderWidth = width;
        fireChanged();
        return this;
    }

    /** @return радиус скругления */
    public float radius() {
        return radius;
    }

    /** Задаёт радиус скругления. */
    public Surface radius(float radius) {
        if (this.radius == radius) return this;
        this.radius = radius;
        fireChanged();
        return this;
    }

    /**
     * Вычисляет эффективный kind: явный, либо автовывод по заполненности полей.
     *
     * @return эффективный kind
     */
    public BackgroundKind effectiveKind() {
        if (backgroundKind != null) return backgroundKind;
        if (!backgroundVisible) return BackgroundKind.NONE;
        if (backgroundShader != null) return BackgroundKind.SHADER;
        if (backgroundTexture != null) return BackgroundKind.TEXTURE;
        return BackgroundKind.COLOR;
    }
}
