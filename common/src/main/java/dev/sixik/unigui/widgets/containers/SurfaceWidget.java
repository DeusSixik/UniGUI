package dev.sixik.unigui.widgets.containers;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.core.UIContext;
import dev.sixik.unigui.api.animation.AnimationEasing;
import dev.sixik.unigui.api.animation.FloatValueReader;
import dev.sixik.unigui.api.animation.FloatValueWriter;
import dev.sixik.unigui.api.animation.TransitionSpec;
import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.render.ImageFit;
import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.render.TextureFilter;
import dev.sixik.unigui.api.render.TextureHandle;
import dev.sixik.unigui.api.render.TextureWrap;
import dev.sixik.unigui.api.render.shaders.ShaderDrawOptions;
import dev.sixik.unigui.api.render.shaders.ShaderHandle;
import dev.sixik.unigui.api.render.shaders.ShaderUniforms;
import dev.sixik.unigui.api.style.Style;
import dev.sixik.unigui.api.style.StyleKey;
import dev.sixik.unigui.api.style.StyleKeys;
import dev.sixik.unigui.api.style.StylePack;
import dev.sixik.unigui.api.style.Theme;
import dev.sixik.unigui.api.style.WidgetState;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.api.widget.visual.BackgroundKind;
import dev.sixik.unigui.api.widget.visual.Surface;
import dev.sixik.unigui.api.widget.visual.SurfaceSnapshot;
import dev.sixik.unigui.api.xml.XmlAttribute;
import dev.sixik.unigui.api.xml.XmlTextureAttributes;
import dev.sixik.unigui.api.xml.XmlWidgetName;
import dev.sixik.unigui.api.widget.render.WidgetRender;
import dev.sixik.unigui.widgets.render.BoxState;
import dev.sixik.unigui.widgets.render.SurfaceRenderers;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Базовый контейнер с композируемой поверхностью: фон, рамка и радиус.
 *
 * <p>Ядро новой визуальной системы. Вместо наследования визуала от {@code Box}
 * каждый виджет <em>содержит</em> {@link Surface}: источник фона выбирается через
 * {@link BackgroundKind} (цвет, текстура или шейдер) и переключается стилем,
 * кодом или XML без смены renderer-класса.</p>
 *
 * <p>Порядок отрисовки: custom {@link WidgetRender} (если задан, заменяет весь визуал),
 * затем {@link #renderSurface(RenderContext)} (хром), затем {@link #renderContent(RenderContext)}
 * (foreground + дети).</p>
 *
 * @param <T> self-тип для fluent-наследования
 * @see Surface
 * @see BackgroundKind
 * @see WidgetRender
 * @see Box
 */
@XmlWidgetName("Surface")
public class SurfaceWidget<T extends SurfaceWidget<T>> extends PanelWidget {
    /** Композируемая поверхность: фон, рамка и радиус. */
    protected final Surface surface = new Surface(() -> invalidate(InvalidationFlags.VISUAL));
    /**
     * Мастер-переключатель отрисовки поверхности. Контролы со своим foreground
     * (слайдеры, текстовые поля) выключают его, чтобы рисовать только контент.
     */
    private boolean boxVisualEnabled = true;
    private boolean themeEnabled = true;
    private long lastAppliedStyleVersion = Long.MIN_VALUE;
    private long lastAppliedScopeStyleVersion = Long.MIN_VALUE;
    private long transitionStyleVersion = Long.MIN_VALUE;
    private long transitionScopeStyleVersion = Long.MIN_VALUE;
    private WidgetState transitionState;
    private TransitionSpec styleTransition = TransitionSpec.INSTANT;
    private final FloatValueReader borderWidthReader = () -> surface.borderWidth();
    private final FloatValueWriter borderWidthWriter = value -> {
        if (surface.borderWidth() == value) return;
        surface.borderWidth(value);
    };
    private final FloatValueReader radiusReader = () -> surface.radius();
    private final FloatValueWriter radiusWriter = value -> {
        if (surface.radius() == value) return;
        surface.radius(value);
    };

    /**
     * Создаёт виджет с пустой композируемой поверхностью.
     */
    protected SurfaceWidget() {
    }

    /**
     * Возвращает self для fluent-наследования.
     *
     * @return этот виджет с точным типом наследника
     */
    @SuppressWarnings("unchecked")
    protected final T self() {
        return (T) this;
    }

    /**
     * Возвращает композируемую поверхность виджета.
     *
     * @return поверхность фона и рамки
     */
    public Surface surface() {
        return surface;
    }

    /**
     * Возвращает live-цвет фона.
     *
     * @return изменяемый цвет фона
     */
    public MutableColor background() {
        return surface.background();
    }

    @XmlAttribute(value = "background", category = "Appearance", defaultValue = "#00000000", description = "Background color; setting it also enables background rendering.")
    public T background(ColorView color) {
        surface.background().set(color == null ? new MutableColor(0.0f, 0.0f, 0.0f, 0.0f) : color);
        backgroundVisible(true);
        return self();
    }

    /**
     * Анимирует цвет фона за заданное время.
     *
     * @param color целевой цвет
     * @param durationSeconds длительность анимации в секундах
     * @return этот виджет для fluent-настройки
     */
    public T animateBackgroundColor(ColorView color, float durationSeconds) {
        animateColor(surface.background(), color, durationSeconds);
        return self();
    }

    /**
     * Анимирует цвет фона по заданной transition-спецификации.
     *
     * @param color целевой цвет
     * @param spec параметры transition'а
     * @return этот виджет для fluent-настройки
     */
    public T animateBackgroundColor(ColorView color, TransitionSpec spec) {
        animateColor(surface.background(), color, spec);
        return self();
    }

    /**
     * Включает или выключает отрисовку фона.
     *
     * @param backgroundVisible {@code true}, чтобы renderer рисовал фон
     * @return этот виджет для fluent-настройки
     */
    @XmlAttribute(value = "backgroundVisible", category = "Appearance", defaultValue = "false", description = "Whether the background is rendered.")
    public T backgroundVisible(boolean backgroundVisible) {
        surface.backgroundVisible(backgroundVisible);
        return self();
    }

    /**
     * Возвращает, должен ли renderer рисовать фон.
     *
     * @return {@code true}, если фон включён
     */
    public boolean backgroundVisible() {
        return surface.backgroundVisible();
    }

    /**
     * Возвращает источник фона поверхности.
     *
     * @return явный kind или {@code null}, если kind выводится автоматически
     */
    public BackgroundKind backgroundKind() {
        return surface.backgroundKind();
    }

    /**
     * Задаёт источник фона: цвет, текстура или шейдер.
     *
     * <pre>{@code
     * widget.backgroundKind(BackgroundKind.TEXTURE).backgroundTexture(texture);
     * widget.backgroundKind(BackgroundKind.SHADER).backgroundShader(shader);
     * }</pre>
     *
     * @param kind источник фона; {@code null} включает автовывод
     * @return этот виджет для fluent-настройки
     */
    @XmlAttribute(value = "backgroundKind", category = "Appearance", defaultValue = "", description = "Background source: none, color, texture or shader. Empty means automatic.")
    public T backgroundKind(BackgroundKind kind) {
        surface.backgroundKind(kind);
        return self();
    }

    /**
     * Возвращает шейдер фона.
     *
     * @return handle шейдера или {@code null}
     */
    public ShaderHandle backgroundShader() {
        return surface.backgroundShader();
    }

    /**
     * Задаёт шейдер фона.
     *
     * @param shader handle шейдера или {@code null}
     * @return этот виджет для fluent-настройки
     */
    @XmlAttribute(value = "backgroundShader", category = "Appearance", defaultValue = "", description = "Shader resource id used as background when background kind is shader.")
    public T backgroundShader(ShaderHandle shader) {
        surface.backgroundShader(shader);
        return self();
    }

    /**
     * Задаёт шейдер фона по resource id.
     *
     * @param shaderId resource id шейдера; пустая строка сбрасывает шейдер
     * @return этот виджет для fluent-настройки
     */
    public T backgroundShader(String shaderId) {
        String normalized = shaderId == null ? "" : shaderId.trim();
        surface.backgroundShader(normalized.isEmpty() ? null : ShaderHandle.resource(normalized));
        return self();
    }

    /**
     * Возвращает uniforms шейдера фона.
     *
     * @return uniforms или {@code null}
     */
    public ShaderUniforms shaderUniforms() {
        return surface.shaderUniforms();
    }

    /**
     * Задаёт uniforms шейдера фона.
     *
     * @param uniforms uniforms шейдера
     * @return этот виджет для fluent-настройки
     */
    public T shaderUniforms(ShaderUniforms uniforms) {
        surface.shaderUniforms(uniforms);
        return self();
    }

    /**
     * Возвращает опции отрисовки шейдера фона.
     *
     * @return опции или {@code null} для defaults
     */
    public ShaderDrawOptions shaderOptions() {
        return surface.shaderOptions();
    }

    /**
     * Задаёт опции отрисовки шейдера фона.
     *
     * @param options опции отрисовки
     * @return этот виджет для fluent-настройки
     */
    public T shaderOptions(ShaderDrawOptions options) {
        surface.shaderOptions(options);
        return self();
    }

    /**
     * Возвращает текстуру фона.
     *
     * @return handle текстуры или {@code null}
     */
    public TextureHandle backgroundTexture() {
        return surface.backgroundTexture();
    }

    /**
     * Задаёт текстуру фона.
     *
     * @param backgroundTexture handle текстуры или {@code null}
     * @return этот виджет для fluent-настройки
     */
    @XmlAttribute(value = "backgroundTexture", displayName = "Background Texture", category = "Assets", defaultValue = "", description = "Texture resource id resolved through XmlWidgetOptions.textureResolver.")
    public T backgroundTexture(TextureHandle backgroundTexture) {
        surface.backgroundTexture(backgroundTexture);
        return self();
    }

    @XmlAttribute(value = "backgroundTextureWidth", displayName = "Background Texture Width", category = "Assets", defaultValue = "16", description = "Source texture width used for contain and cover placement.")
    public T backgroundTextureWidth(int width) {
        return backgroundTexture(XmlTextureAttributes.resize(surface.backgroundTexture(), width, null));
    }

    @XmlAttribute(value = "backgroundTextureHeight", displayName = "Background Texture Height", category = "Assets", defaultValue = "16", description = "Source texture height used for contain and cover placement.")
    public T backgroundTextureHeight(int height) {
        return backgroundTexture(XmlTextureAttributes.resize(surface.backgroundTexture(), null, height));
    }

    @XmlAttribute(value = "backgroundTextureSampling", displayName = "Background Texture Sampling", category = "Assets", defaultValue = "nearest", description = "Texture filtering mode used by the renderer backend.")
    public T backgroundTextureSampling(TextureFilter filter) {
        return backgroundTexture(XmlTextureAttributes.options(surface.backgroundTexture(), options -> options.sampling(filter)));
    }

    @XmlAttribute(value = "backgroundTextureWrap", displayName = "Background Texture Wrap", category = "Assets", defaultValue = "clamp-to-edge", description = "Texture coordinate wrap mode used by the renderer backend.")
    public T backgroundTextureWrap(TextureWrap wrap) {
        return backgroundTexture(XmlTextureAttributes.options(surface.backgroundTexture(), options -> options.wrap(wrap)));
    }

    @XmlAttribute(value = "backgroundTextureMipmaps", displayName = "Background Texture Mipmaps", category = "Assets", defaultValue = "false", description = "Whether the texture should use mipmapped sampling.")
    public T backgroundTextureMipmaps(boolean mipmaps) {
        return backgroundTexture(XmlTextureAttributes.options(surface.backgroundTexture(), options -> options.mipmaps(mipmaps)));
    }

    @XmlAttribute(value = "backgroundTexturePremultipliedAlpha", displayName = "Background Texture Premultiplied Alpha", category = "Assets", defaultValue = "false", description = "Whether the texture color data already uses premultiplied alpha.")
    public T backgroundTexturePremultipliedAlpha(boolean premultipliedAlpha) {
        return backgroundTexture(XmlTextureAttributes.options(surface.backgroundTexture(), options -> options.premultipliedAlpha(premultipliedAlpha)));
    }

    /**
     * Возвращает live tint-цвет фоновой текстуры.
     *
     * @return изменяемый tint-цвет
     */
    public MutableColor backgroundTextureTint() {
        return surface.backgroundTextureTint();
    }

    @XmlAttribute(value = "backgroundTextureTint", displayName = "Background Texture Tint", category = "Assets", defaultValue = "#FFFFFFFF", description = "Tint color applied while drawing the background texture.")
    public T backgroundTextureTint(ColorView color) {
        if (color != null) surface.backgroundTextureTint().set(color);
        return self();
    }

    /**
     * Анимирует tint фоновой текстуры за заданное время.
     *
     * @param color целевой tint-цвет
     * @param durationSeconds длительность анимации в секундах
     * @return этот виджет для fluent-настройки
     */
    public T animateBackgroundTextureTint(ColorView color, float durationSeconds) {
        animateColor(surface.backgroundTextureTint(), color, durationSeconds);
        return self();
    }

    /**
     * Анимирует tint фоновой текстуры по заданной transition-спецификации.
     *
     * @param color целевой tint-цвет
     * @param spec параметры transition'а
     * @return этот виджет для fluent-настройки
     */
    public T animateBackgroundTextureTint(ColorView color, TransitionSpec spec) {
        animateColor(surface.backgroundTextureTint(), color, spec);
        return self();
    }

    /**
     * Возвращает source-rect фоновой текстуры в UV-координатах.
     *
     * @return live rect {@code u/v/width/height}
     */
    public MutableRect backgroundTextureSource() {
        return surface.backgroundTextureSource();
    }

    @XmlAttribute(value = "backgroundTextureSource", displayName = "Background Texture Source", category = "Assets", defaultValue = "0 0 1 1", description = "Normalized UV source rectangle: u v width height.")
    public T backgroundTextureSource(MutableRect source) {
        surface.backgroundTextureSource().set(source == null ? new MutableRect(0.0f, 0.0f, 1.0f, 1.0f) : source);
        return self();
    }

    /**
     * Задаёт source-rect фоновой текстуры в UV-координатах.
     *
     * @return этот виджет для fluent-настройки
     */
    public T backgroundTextureSource(float u, float v, float width, float height) {
        surface.backgroundTextureSource().set(u, v, width, height);
        return self();
    }

    /**
     * Возвращает способ вписывания фоновой текстуры в bounds виджета.
     *
     * @return режим вписывания текстуры
     */
    public ImageFit backgroundTextureFit() {
        return surface.backgroundTextureFit();
    }

    /**
     * Задаёт способ вписывания фоновой текстуры в bounds виджета.
     *
     * @param fit режим вписывания; {@code null} трактуется как {@link ImageFit#STRETCH}
     * @return этот виджет для fluent-настройки
     */
    @XmlAttribute(value = "backgroundTextureFit", displayName = "Background Texture Fit", category = "Assets", defaultValue = "stretch", description = "Placement mode for the background texture.")
    public T backgroundTextureFit(ImageFit fit) {
        surface.backgroundTextureFit(fit);
        return self();
    }

    /**
     * Возвращает live-цвет рамки.
     *
     * @return изменяемый цвет рамки
     */
    public MutableColor borderColor() {
        return surface.borderColor();
    }

    @XmlAttribute(value = "border", category = "Appearance", defaultValue = "#FFFFFFFF", description = "Border color; setting it also enables border rendering.")
    public T border(ColorView color) {
        borderColor(color);
        borderVisible(true);
        return self();
    }

    @XmlAttribute(value = "borderColor", category = "Appearance", defaultValue = "#FFFFFFFF", description = "Border color used when border rendering is enabled.")
    public T borderColor(ColorView color) {
        if (color != null) surface.borderColor().set(color);
        return self();
    }

    /**
     * Анимирует цвет рамки за заданное время.
     *
     * @param color целевой цвет рамки
     * @param durationSeconds длительность анимации в секундах
     * @return этот виджет для fluent-настройки
     */
    public T animateBorderColor(ColorView color, float durationSeconds) {
        animateColor(surface.borderColor(), color, durationSeconds);
        return self();
    }

    /**
     * Анимирует цвет рамки по заданной transition-спецификации.
     *
     * @param color целевой цвет рамки
     * @param spec параметры transition'а
     * @return этот виджет для fluent-настройки
     */
    public T animateBorderColor(ColorView color, TransitionSpec spec) {
        animateColor(surface.borderColor(), color, spec);
        return self();
    }

    /**
     * Включает или выключает отрисовку рамки.
     *
     * @param borderVisible {@code true}, чтобы renderer рисовал рамку
     * @return этот виджет для fluent-настройки
     */
    @XmlAttribute(value = "borderVisible", category = "Appearance", defaultValue = "false", description = "Whether the border is rendered.")
    public T borderVisible(boolean borderVisible) {
        surface.borderVisible(borderVisible);
        return self();
    }

    /**
     * Возвращает, должна ли отрисовываться рамка.
     *
     * @return {@code true}, если рамка включена
     */
    public boolean borderVisible() {
        return surface.borderVisible();
    }

    /**
     * Возвращает толщину рамки.
     *
     * @return толщина рамки в пикселях UI-пространства
     */
    public float borderWidth() {
        return surface.borderWidth();
    }

    /**
     * Задаёт толщину рамки.
     *
     * @param borderWidth толщина рамки в пикселях UI-пространства
     * @return этот виджет для fluent-настройки
     */
    @XmlAttribute(value = "borderWidth", category = "Appearance", defaultValue = "1", description = "Border thickness in UI pixels.")
    public T borderWidth(float borderWidth) {
        surface.borderWidth(borderWidth);
        return self();
    }

    /**
     * Анимирует толщину рамки за заданное время.
     *
     * @param borderWidth целевая толщина рамки
     * @param durationSeconds длительность анимации в секундах
     * @return этот виджет для fluent-настройки
     */
    public T animateBorderWidth(float borderWidth, float durationSeconds) {
        return animateBorderWidth(borderWidth, TransitionSpec.of(durationSeconds));
    }

    /**
     * Анимирует толщину рамки по заданной transition-спецификации.
     *
     * @param borderWidth целевая толщина рамки
     * @param spec параметры transition'а
     * @return этот виджет для fluent-настройки
     */
    public T animateBorderWidth(float borderWidth, TransitionSpec spec) {
        animateParameter("Box.borderWidth", borderWidthReader, borderWidthWriter, borderWidth, spec);
        return self();
    }

    /**
     * Возвращает радиус скругления.
     *
     * @return радиус скругления в пикселях UI-пространства
     */
    public float radius() {
        return surface.radius();
    }

    /**
     * Задаёт радиус скругления.
     *
     * @param radius радиус скругления в пикселях UI-пространства
     * @return этот виджет для fluent-настройки
     */
    @XmlAttribute(value = "radius", category = "Appearance", defaultValue = "0", description = "Corner radius in UI pixels.")
    public T radius(float radius) {
        surface.radius(radius);
        return self();
    }

    /**
     * Анимирует радиус скругления за заданное время.
     *
     * @param radius целевой радиус скругления
     * @param durationSeconds длительность анимации в секундах
     * @return этот виджет для fluent-настройки
     */
    public T animateRadius(float radius, float durationSeconds) {
        return animateRadius(radius, TransitionSpec.of(durationSeconds));
    }

    /**
     * Анимирует радиус скругления по заданной transition-спецификации.
     *
     * @param radius целевой радиус скругления
     * @param spec параметры transition'а
     * @return этот виджет для fluent-настройки
     */
    public T animateRadius(float radius, TransitionSpec spec) {
        animateParameter("Box.radius", radiusReader, radiusWriter, radius, spec);
        return self();
    }

    /**
     * Возвращает, рисуется ли собственная поверхность виджета.
     *
     * @return {@code true}, если {@link #renderSurface(RenderContext)} может рисовать
     */
    public boolean boxVisualEnabled() {
        return boxVisualEnabled;
    }

    /**
     * Включает или выключает отрисовку собственной поверхности.
     *
     * @param boxVisualEnabled {@code true}, чтобы рисовать surface renderer/RenderPlan/fallback
     * @return этот виджет для fluent-настройки
     */
    @XmlAttribute(value = "boxVisualEnabled", category = "Appearance", defaultValue = "true", description = "Whether surface rendering is enabled.")
    public T boxVisualEnabled(boolean boxVisualEnabled) {
        if (this.boxVisualEnabled == boxVisualEnabled) return self();
        this.boxVisualEnabled = boxVisualEnabled;
        invalidate(InvalidationFlags.VISUAL);
        return self();
    }

    /**
     * Задаёт custom renderer виджета с точным типом наследника для fluent-цепочек.
     *
     * <p>Custom renderer полностью заменяет стандартный визуал (фон плюс контент).</p>
     *
     * @param renderer custom renderer или {@code null} для стандартного визуала
     * @return этот виджет для fluent-настройки
     */
    @Override
    public T renderer(WidgetRender renderer) {
        super.renderer(renderer);
        return self();
    }

    /**
     * Возвращает, участвует ли виджет в theme/style lookup.
    public boolean themeEnabled() {
        return themeEnabled;
    }

    /**
     * Включает или выключает theme/style lookup для этого виджета.
     *
     * @param themeEnabled {@code true}, чтобы применять theme/local styles
     * @return этот виджет для fluent-настройки
     */
    @XmlAttribute(value = "themeEnabled", category = "Appearance", defaultValue = "true", description = "Whether theme/style lookup can override visual values.")
    public T themeEnabled(boolean themeEnabled) {
        if (this.themeEnabled == themeEnabled) return self();
        this.themeEnabled = themeEnabled;
        transitionState = null;
        transitionStyleVersion = Long.MIN_VALUE;
        transitionScopeStyleVersion = Long.MIN_VALUE;
        invalidate(InvalidationFlags.VISUAL);
        return self();
    }

    @Override
    public void render(RenderContext context) {
        if (visibility() != Visibility.VISIBLE) return;
        pushOpacity(context);
        try {
            DrawScope draw = new DrawScope(context, transform(), layoutBounds());
            if (renderCustomVisual(draw)) {
                renderChildren(context);
                return;
            }
            if (boxVisualEnabled) {
                renderSurface(context);
            }
            renderContent(context);
        } finally {
            popOpacity(context);
        }
    }

    /**
     * Рендерит поверхность виджета перед контентом.
     *
     * <p>Движок хрома: theme/style значения, затем StylePack RenderPlan и только
     * потом default. Custom {@link WidgetRender} обрабатывается раньше в
     * {@link #render(RenderContext)} и полностью заменяет поверхность.</p>
     *
     * @param context текущий render context
     */
    protected void renderSurface(RenderContext context) {
        applyTheme();
        BoxState legacyState = boxState();
        if (renderStylePlan(context, BoxState.class, legacyState)) return;
        SurfaceRenderers.DEFAULT.render(
                new DrawScope(context, transform(), layoutBounds()), this);
    }

    /**
     * Рендерит контент виджета после поверхности.
     *
     * <p>Базовая реализация рисует только детей. Контентные виджеты (кнопки,
     * чекбоксы, слайдеры) переопределяют метод и рисуют свой foreground —
     * без дублирования фона, который уже отрисован {@link #renderSurface}.</p>
     *
     * @param context текущий render context
     */
    protected void renderContent(RenderContext context) {
        renderChildren(context);
    }

    /**
     * Создаёт immutable snapshot поверхности для renderer'а.
     *
     * @return snapshot поверхности на текущий кадр
     */
    public SurfaceSnapshot surfaceSnapshot() {
        return surface.snapshot(layoutBounds());
    }

    /**
     * Создаёт legacy snapshot поверхности для совместимости.
     *
     * @return состояние поверхности на текущий кадр
     */
    protected BoxState boxState() {
        return BoxState.fromSurface(surface.snapshot(layoutBounds()));
    }

    /**
     * Применяет theme/local style значения к поверхности.
     *
     * <p>Метод вызывается лениво перед render'ом и следит за версиями theme и
     * local style scopes, чтобы invalidate происходил при смене стилей.</p>
     */
    protected void applyTheme() {
        if (!themeEnabled) return;
        UIContext context = uiContext();
        long styleVersion = context == null ? Theme.EMPTY.version() : context.styleVersion();
        long scopeStyleVersion = scopeStyleVersion();
        WidgetState state = styleState();
        updateStyleTransition(styleVersion, scopeStyleVersion, state);
        if ((lastAppliedStyleVersion != Long.MIN_VALUE && lastAppliedStyleVersion != styleVersion)
                || (lastAppliedScopeStyleVersion != Long.MIN_VALUE && lastAppliedScopeStyleVersion != scopeStyleVersion)) {
            invalidate(InvalidationFlags.VISUAL);
        }
        lastAppliedStyleVersion = styleVersion;
        lastAppliedScopeStyleVersion = scopeStyleVersion;

        BackgroundKind themedKind = styleValue(StyleKeys.BACKGROUND_KIND, state, surface.backgroundKind());
        if (themedKind != surface.backgroundKind()) {
            surface.backgroundKind(themedKind);
        }
        ColorView themedBackground = styleValue(StyleKeys.BACKGROUND_COLOR, state, surface.background());
        TextureHandle themedBackgroundTexture = styleValue(StyleKeys.BACKGROUND_TEXTURE, state, surface.backgroundTexture());
        ColorView themedBackgroundTextureTint = styleValue(StyleKeys.BACKGROUND_TEXTURE_TINT, state, surface.backgroundTextureTint());
        ImageFit themedBackgroundTextureFit = styleValue(StyleKeys.BACKGROUND_TEXTURE_FIT, state, surface.backgroundTextureFit());
        ShaderHandle themedShader = styleValue(StyleKeys.BACKGROUND_SHADER, state, surface.backgroundShader());
        ColorView themedBorder = styleValue(StyleKeys.BORDER_COLOR, state, surface.borderColor());
        Float themedBorderWidth = styleValue(StyleKeys.BORDER_WIDTH, state, surface.borderWidth());
        Float themedRadius = styleValue(StyleKeys.RADIUS, state, surface.radius());

        if (themedBackground != null) {
            animateColor(surface.background(), themedBackground, styleTransition);
        }
        if (surface.backgroundTexture() != themedBackgroundTexture) {
            surface.backgroundTexture(themedBackgroundTexture);
        }
        if (themedBackgroundTextureTint != null) {
            animateColor(surface.backgroundTextureTint(), themedBackgroundTextureTint, styleTransition);
        }
        if (themedBackgroundTextureFit != null && surface.backgroundTextureFit() != themedBackgroundTextureFit) {
            surface.backgroundTextureFit(themedBackgroundTextureFit);
        }
        if (themedShader != surface.backgroundShader()) {
            surface.backgroundShader(themedShader);
        }
        if (themedBorder != null) {
            animateColor(surface.borderColor(), themedBorder, styleTransition);
        }
        if (themedBorderWidth != null) {
            animateParameter(StyleKeys.BORDER_WIDTH, borderWidthReader, borderWidthWriter,
                    themedBorderWidth, styleTransition);
        }
        if (themedRadius != null) {
            animateParameter(StyleKeys.RADIUS, radiusReader, radiusWriter, themedRadius, styleTransition);
        }
    }

    /**
     * Возвращает transition, выбранный для текущего изменения состояния или темы.
     *
     * @return transition автоматического применения style-свойств
     */
    protected final TransitionSpec styleTransition() {
        return styleTransition;
    }

    private void updateStyleTransition(long styleVersion, long scopeStyleVersion, WidgetState state) {
        if (transitionStyleVersion == styleVersion
                && transitionScopeStyleVersion == scopeStyleVersion
                && transitionState == state) {
            return;
        }

        boolean firstApplication = transitionState == null;
        Float duration = styleValue(StyleKeys.TRANSITION_DURATION, state, 0.0f);
        AnimationEasing easing = styleValue(StyleKeys.TRANSITION_EASING, state, AnimationEasing.EASE_OUT);
        float normalizedDuration = duration == null || !Float.isFinite(duration)
                ? 0.0f
                : Math.max(0.0f, duration);
        styleTransition = firstApplication || normalizedDuration <= 0.0f
                ? TransitionSpec.INSTANT
                : new TransitionSpec(normalizedDuration, easing == null ? AnimationEasing.EASE_OUT : easing);
        transitionStyleVersion = styleVersion;
        transitionScopeStyleVersion = scopeStyleVersion;
        transitionState = state;
    }

    /**
     * Возвращает состояние виджета для style lookup.
     *
     * @return disabled, hovered или normal
     */
    protected WidgetState styleState() {
        if (!enabled()) return WidgetState.DISABLED;
        return hovered() ? WidgetState.HOVERED : WidgetState.NORMAL;
    }

    /**
     * Возвращает style type, по которому theme ищет значения для виджета.
     *
     * @return имя runtime-класса по умолчанию
     */
    protected String styleType() {
        return getClass().getSimpleName();
    }

    @Override
    protected WidgetRender styleRenderOverride() {
        return themeEnabled ? super.styleRenderOverride() : null;
    }

    @Override
    protected boolean stylePlansEnabled() {
        return themeEnabled;
    }

    /**
     * Ищет style value в theme и local style scopes.
     *
     * @param key ключ style-значения
     * @param fallback значение, если style ничего не задал
     * @return найденное или fallback-значение
     */
    protected <S> S styleValue(StyleKey<S> key, S fallback) {
        return styleValue(key, styleState(), fallback);
    }

    /**
     * Ищет style value для конкретного {@link WidgetState}.
     *
     * @param key ключ style-значения
     * @param state состояние виджета для style lookup
     * @param fallback значение, если style ничего не задал
     * @return найденное или fallback-значение
     */
    protected <S> S styleValue(StyleKey<S> key, WidgetState state, S fallback) {
        if (!themeEnabled) return fallback;
        UIContext context = uiContext();
        Theme theme = context == null ? Theme.EMPTY : context.theme();
        String type = styleType();
        Style themeStyle = theme instanceof StylePack stylePack
                ? stylePack.resolveStyleFor(type, styleId(), styleClasses())
                : theme.styleFor(type);
        S value = themeStyle.get(key, state, fallback);
        for (Widget current : styleLookupChain()) {
            Style localStyle = current.localStyle(type);
            value = localStyle.get(key, state, value);
        }
        return value;
    }

    private long scopeStyleVersion() {
        long version = 0L;
        String type = styleType();
        for (Widget current : styleLookupChain()) {
            version += current.localStyle(type).version();
        }
        return version;
    }

    private List<Widget> styleLookupChain() {
        List<Widget> chain = new ObjectArrayList<>();
        Widget current = this;
        while (current != null) {
            chain.add(current);
            if (current != this && current.styleScope()) {
                break;
            }
            current = current.parent();
        }
        Collections.reverse(chain);
        return chain;
    }
}
