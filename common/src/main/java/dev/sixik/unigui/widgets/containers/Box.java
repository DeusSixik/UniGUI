package dev.sixik.unigui.widgets.containers;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.core.UIContext;
import dev.sixik.unigui.api.animation.TransitionSpec;
import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.render.ImageFit;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.render.TextureFilter;
import dev.sixik.unigui.api.render.TextureHandle;
import dev.sixik.unigui.api.render.TexturePlacement;
import dev.sixik.unigui.api.render.TextureWrap;
import dev.sixik.unigui.api.style.Style;
import dev.sixik.unigui.api.style.StyleKey;
import dev.sixik.unigui.api.style.StyleKeys;
import dev.sixik.unigui.api.style.StylePack;
import dev.sixik.unigui.api.style.Theme;
import dev.sixik.unigui.api.style.WidgetState;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.api.widget.skin.WidgetsRender;
import dev.sixik.unigui.api.xml.XmlAttribute;
import dev.sixik.unigui.api.xml.XmlTextureAttributes;
import dev.sixik.unigui.api.xml.XmlWidgetName;
import dev.sixik.unigui.widgets.render.BoxRenderer;
import dev.sixik.unigui.widgets.render.BoxState;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Collections;
import java.util.List;
import dev.sixik.unigui.api.style.StyleAnimationIds;
import dev.sixik.unigui.api.style.StyleIds;

/**
 * Визуальный контейнер с фоном, текстурой, рамкой, радиусом и дочерними виджетами.
 *
 * <p>{@code Box} расширяет {@link PanelWidget}: дети измеряются и рендерятся как
 * у обычной панели, а перед ними отрисовывается собственная визуальная подложка.
 * Подложка может состоять из цвета, texture placement'а, border'а и radius'а.
 * Для простого layout-контейнера без визуальной оболочки используй
 * {@link PanelWidget} или {@link StackPanel}.</p>
 *
 * <p>При включённом {@link #themeEnabled()} значения визуального состояния
 * могут подхватываться из {@link Theme} и local styles родителей. Прямые setter'ы
 * всё ещё можно использовать для ручной настройки или анимаций.</p>
 *
 * <pre>{@code
 * Box card = new Box()
 *         .backgroundVisible(true)
 *         .borderVisible(true)
 *         .radius(4.0f);
 * card.background().set(0.08f, 0.09f, 0.11f, 0.95f);
 * card.addChild(content);
 * }</pre>
 *
 * @see BoxRenderer
 * @see BoxState
 * @see PanelWidget
 */
@XmlWidgetName("Box")
public class Box extends PanelWidget {
    /** Style type id для StylePack selector/binding. */
    public static final String STYLE_TYPE = StyleIds.Widget.BOX;

    /** Style property id, которые понимает стандартный Box RenderPlan. */
    public static final class StyleProperties {
        public static final String BACKGROUND_COLOR = StyleIds.Key.BACKGROUND_COLOR;
        public static final String BACKGROUND_TEXTURE = StyleIds.Key.BACKGROUND_TEXTURE;
        public static final String BACKGROUND_TEXTURE_TINT = StyleIds.Key.BACKGROUND_TEXTURE_TINT;
        public static final String BACKGROUND_TEXTURE_FIT = StyleIds.Key.BACKGROUND_TEXTURE_FIT;
        public static final String BORDER_COLOR = StyleIds.Key.BORDER_COLOR;
        public static final String BORDER_WIDTH = StyleIds.Key.BORDER_WIDTH;
        public static final String RADIUS = StyleIds.Key.RADIUS;

        private StyleProperties() {
        }
    }

    /** Animation property id для Box и его визуальной подложки. */
    public static final class AnimationProperties {
        public static final String BACKGROUND_COLOR = StyleAnimationIds.Property.BACKGROUND_COLOR;
        public static final String BACKGROUND_TEXTURE_TINT = StyleAnimationIds.Property.BACKGROUND_TEXTURE_TINT;
        public static final String BORDER_COLOR = StyleAnimationIds.Property.BORDER_COLOR;
        public static final String BORDER_WIDTH = StyleAnimationIds.Property.BORDER_WIDTH;
        public static final String RADIUS = StyleAnimationIds.Property.RADIUS;
        public static final String OPACITY = StyleAnimationIds.Property.OPACITY;
        public static final String SCALE = StyleAnimationIds.Property.SCALE;
        public static final String ROTATION_DEGREES = StyleAnimationIds.Property.ROTATION_DEGREES;
        public static final java.util.List<String> ALL = StyleAnimationIds.Property.BOX;

        private AnimationProperties() {
        }
    }

    private final MutableColor background = new MutableColor(0.0f, 0.0f, 0.0f, 0.0f);
    private TextureHandle backgroundTexture;
    private final MutableColor backgroundTextureTint = new MutableColor(1.0f, 1.0f, 1.0f, 1.0f);
    private final MutableRect backgroundTextureSource = new MutableRect(0.0f, 0.0f, 1.0f, 1.0f);
    private ImageFit backgroundTextureFit = ImageFit.STRETCH;
    private final MutableColor borderColor = new MutableColor(1.0f, 1.0f, 1.0f, 1.0f);
    private BoxRenderer boxRenderer;
    private boolean backgroundVisible;
    private boolean borderVisible;
    private boolean boxVisualEnabled = true;
    private float borderWidth = 1.0f;
    private float radius;
    private boolean themeEnabled = true;
    private long lastAppliedStyleVersion = Long.MIN_VALUE;
    private long lastAppliedScopeStyleVersion = Long.MIN_VALUE;

    /**
     * Создаёт пустой box и подписывает live-цвета/rect на visual invalidation.
     */
    public Box() {
        background.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
        backgroundTextureTint.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
        backgroundTextureSource.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
        borderColor.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
    }

    /**
     * Возвращает live-цвет фона.
     *
     * @return изменяемый цвет фона
     */
    public MutableColor background() {
        return background;
    }

    @XmlAttribute(value = "background", category = "Appearance", defaultValue = "#00000000", description = "Background color; setting it also enables background rendering.")
    public Box background(ColorView color) {
        background.set(color == null ? new MutableColor(0.0f, 0.0f, 0.0f, 0.0f) : color);
        backgroundVisible(true);
        return this;
    }

    /**
     * Анимирует цвет фона за заданное время.
     *
     * @param color целевой цвет
     * @param durationSeconds длительность анимации в секундах
     * @return этот box для fluent-настройки
     */
    public Box animateBackgroundColor(ColorView color, float durationSeconds) {
        animateColor(background, color, durationSeconds);
        return this;
    }

    /**
     * Анимирует цвет фона по заданной transition-спецификации.
     *
     * @param color целевой цвет
     * @param spec параметры transition'а
     * @return этот box для fluent-настройки
     */
    public Box animateBackgroundColor(ColorView color, TransitionSpec spec) {
        animateColor(background, color, spec);
        return this;
    }

    /**
     * Включает или выключает отрисовку цветового/текстурного фона.
     *
     * @param backgroundVisible {@code true}, чтобы renderer рисовал фон
     * @return этот box для fluent-настройки
     */
    @XmlAttribute(value = "backgroundVisible", category = "Appearance", defaultValue = "false", description = "Whether the box background is rendered.")
    public Box backgroundVisible(boolean backgroundVisible) {
        if (this.backgroundVisible == backgroundVisible) return this;
        this.backgroundVisible = backgroundVisible;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    /**
     * Возвращает, должен ли renderer рисовать фон.
     *
     * @return {@code true}, если фон включён
     */
    public boolean backgroundVisible() {
        return backgroundVisible;
    }

    /**
     * Возвращает, рисует ли Box собственную визуальную подложку.
     *
     * <p>Этот флаг отделяет layout/container часть {@code Box} от старого fallback-рендера.
     * Наследники с собственным renderer'ом выключают его, чтобы не получать лишний фон/рамку от Box.</p>
     *
     * @return {@code true}, если {@link #renderBox(RenderContext)} может рисовать Box visual
     */
    public boolean boxVisualEnabled() {
        return boxVisualEnabled;
    }

    /**
     * Включает или выключает собственный Box visual для этого instance.
     *
     * @param boxVisualEnabled {@code true}, чтобы рисовать Box renderer/RenderPlan/fallback
     * @return этот box для fluent-настройки
     */
    @XmlAttribute(value = "boxVisualEnabled", category = "Appearance", defaultValue = "true", description = "Whether inherited Box visual rendering is enabled.")
    public Box boxVisualEnabled(boolean boxVisualEnabled) {
        if (this.boxVisualEnabled == boxVisualEnabled) return this;
        this.boxVisualEnabled = boxVisualEnabled;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    /**
     * Возвращает renderer, заданный напрямую для этого box'а.
     *
     * @return кастомный renderer или {@code null}, если используется тема/default
     */
    public BoxRenderer boxRenderer() {
        return boxRenderer;
    }

    /**
     * Задаёт renderer визуальной подложки.
     *
     * @param boxRenderer renderer box'а или {@code null} для theme/default renderer'а
     * @return этот box для fluent-настройки
     */
    public Box boxRenderer(BoxRenderer boxRenderer) {
        if (this.boxRenderer == boxRenderer) return this;
        this.boxRenderer = boxRenderer;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    /**
     * Сбрасывает кастомный renderer и снова использует renderer из темы/default.
     *
     * @return этот box для fluent-настройки
     */
    public Box useDefaultBoxRenderer() {
        return boxRenderer(null);
    }

    /**
     * Возвращает текстуру фона.
     *
     * @return handle текстуры или {@code null}, если текстурный фон не задан
     */
    public TextureHandle backgroundTexture() {
        return backgroundTexture;
    }

    /**
     * Задаёт текстуру фона.
     *
     * @param backgroundTexture handle текстуры или {@code null}
     * @return этот box для fluent-настройки
     */
    @XmlAttribute(value = "backgroundTexture", displayName = "Background Texture", category = "Assets", defaultValue = "", description = "Texture resource id resolved through XmlWidgetOptions.textureResolver.")
    public Box backgroundTexture(TextureHandle backgroundTexture) {
        if (this.backgroundTexture == backgroundTexture) return this;
        this.backgroundTexture = backgroundTexture;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    @XmlAttribute(value = "backgroundTextureWidth", displayName = "Background Texture Width", category = "Assets", defaultValue = "16", description = "Source texture width used for contain and cover placement.")
    public Box backgroundTextureWidth(int width) {
        return backgroundTexture(XmlTextureAttributes.resize(backgroundTexture, width, null));
    }

    @XmlAttribute(value = "backgroundTextureHeight", displayName = "Background Texture Height", category = "Assets", defaultValue = "16", description = "Source texture height used for contain and cover placement.")
    public Box backgroundTextureHeight(int height) {
        return backgroundTexture(XmlTextureAttributes.resize(backgroundTexture, null, height));
    }

    @XmlAttribute(value = "backgroundTextureSampling", displayName = "Background Texture Sampling", category = "Assets", defaultValue = "nearest", description = "Texture filtering mode used by the renderer backend.")
    public Box backgroundTextureSampling(TextureFilter filter) {
        return backgroundTexture(XmlTextureAttributes.options(backgroundTexture, options -> options.sampling(filter)));
    }

    @XmlAttribute(value = "backgroundTextureWrap", displayName = "Background Texture Wrap", category = "Assets", defaultValue = "clamp-to-edge", description = "Texture coordinate wrap mode used by the renderer backend.")
    public Box backgroundTextureWrap(TextureWrap wrap) {
        return backgroundTexture(XmlTextureAttributes.options(backgroundTexture, options -> options.wrap(wrap)));
    }

    @XmlAttribute(value = "backgroundTextureMipmaps", displayName = "Background Texture Mipmaps", category = "Assets", defaultValue = "false", description = "Whether the texture should use mipmapped sampling.")
    public Box backgroundTextureMipmaps(boolean mipmaps) {
        return backgroundTexture(XmlTextureAttributes.options(backgroundTexture, options -> options.mipmaps(mipmaps)));
    }

    @XmlAttribute(value = "backgroundTexturePremultipliedAlpha", displayName = "Background Texture Premultiplied Alpha", category = "Assets", defaultValue = "false", description = "Whether the texture color data already uses premultiplied alpha.")
    public Box backgroundTexturePremultipliedAlpha(boolean premultipliedAlpha) {
        return backgroundTexture(XmlTextureAttributes.options(backgroundTexture, options -> options.premultipliedAlpha(premultipliedAlpha)));
    }

    /**
     * Возвращает live tint-цвет фоновой текстуры.
     *
     * @return изменяемый tint-цвет
     */
    public MutableColor backgroundTextureTint() {
        return backgroundTextureTint;
    }

    @XmlAttribute(value = "backgroundTextureTint", displayName = "Background Texture Tint", category = "Assets", defaultValue = "#FFFFFFFF", description = "Tint color applied while drawing the background texture.")
    public Box backgroundTextureTint(ColorView color) {
        if (color != null) backgroundTextureTint.set(color);
        return this;
    }

    /**
     * Анимирует tint фоновой текстуры за заданное время.
     *
     * @param color целевой tint-цвет
     * @param durationSeconds длительность анимации в секундах
     * @return этот box для fluent-настройки
     */
    public Box animateBackgroundTextureTint(ColorView color, float durationSeconds) {
        animateColor(backgroundTextureTint, color, durationSeconds);
        return this;
    }

    /**
     * Анимирует tint фоновой текстуры по заданной transition-спецификации.
     *
     * @param color целевой tint-цвет
     * @param spec параметры transition'а
     * @return этот box для fluent-настройки
     */
    public Box animateBackgroundTextureTint(ColorView color, TransitionSpec spec) {
        animateColor(backgroundTextureTint, color, spec);
        return this;
    }

    /**
     * Возвращает source-rect фоновой текстуры в UV-координатах.
     *
     * @return live rect {@code u/v/width/height}
     */
    public MutableRect backgroundTextureSource() {
        return backgroundTextureSource;
    }

    @XmlAttribute(value = "backgroundTextureSource", displayName = "Background Texture Source", category = "Assets", defaultValue = "0 0 1 1", description = "Normalized UV source rectangle: u v width height.")
    public Box backgroundTextureSource(MutableRect source) {
        backgroundTextureSource.set(source == null ? new MutableRect(0.0f, 0.0f, 1.0f, 1.0f) : source);
        return this;
    }

    /**
     * Задаёт source-rect фоновой текстуры в UV-координатах.
     *
     * @param u левый UV-offset
     * @param v верхний UV-offset
     * @param width ширина UV-области
     * @param height высота UV-области
     * @return этот box для fluent-настройки
     */
    public Box backgroundTextureSource(float u, float v, float width, float height) {
        backgroundTextureSource.set(u, v, width, height);
        return this;
    }

    /**
     * Возвращает способ вписывания фоновой текстуры в bounds box'а.
     *
     * @return режим вписывания текстуры
     */
    public ImageFit backgroundTextureFit() {
        return backgroundTextureFit;
    }

    /**
     * Задаёт способ вписывания фоновой текстуры в bounds box'а.
     *
     * @param fit режим вписывания; {@code null} трактуется как {@link ImageFit#STRETCH}
     * @return этот box для fluent-настройки
     */
    @XmlAttribute(value = "backgroundTextureFit", displayName = "Background Texture Fit", category = "Assets", defaultValue = "stretch", description = "Placement mode for the background texture.")
    public Box backgroundTextureFit(ImageFit fit) {
        ImageFit effectiveFit = fit == null ? ImageFit.STRETCH : fit;
        if (backgroundTextureFit == effectiveFit) return this;
        backgroundTextureFit = effectiveFit;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    /**
     * Возвращает live-цвет рамки.
     *
     * @return изменяемый цвет рамки
     */
    public MutableColor borderColor() {
        return borderColor;
    }

    @XmlAttribute(value = "border", category = "Appearance", defaultValue = "#FFFFFFFF", description = "Border color; setting it also enables border rendering.")
    public Box border(ColorView color) {
        borderColor(color);
        borderVisible(true);
        return this;
    }

    @XmlAttribute(value = "borderColor", category = "Appearance", defaultValue = "#FFFFFFFF", description = "Border color used when border rendering is enabled.")
    public Box borderColor(ColorView color) {
        if (color != null) borderColor.set(color);
        return this;
    }

    /**
     * Анимирует цвет рамки за заданное время.
     *
     * @param color целевой цвет рамки
     * @param durationSeconds длительность анимации в секундах
     * @return этот box для fluent-настройки
     */
    public Box animateBorderColor(ColorView color, float durationSeconds) {
        animateColor(borderColor, color, durationSeconds);
        return this;
    }

    /**
     * Анимирует цвет рамки по заданной transition-спецификации.
     *
     * @param color целевой цвет рамки
     * @param spec параметры transition'а
     * @return этот box для fluent-настройки
     */
    public Box animateBorderColor(ColorView color, TransitionSpec spec) {
        animateColor(borderColor, color, spec);
        return this;
    }

    /**
     * Включает или выключает отрисовку рамки.
     *
     * @param borderVisible {@code true}, чтобы renderer рисовал рамку
     * @return этот box для fluent-настройки
     */
    @XmlAttribute(value = "borderVisible", category = "Appearance", defaultValue = "false", description = "Whether the box border is rendered.")
    public Box borderVisible(boolean borderVisible) {
        if (this.borderVisible == borderVisible) return this;
        this.borderVisible = borderVisible;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    /**
     * Возвращает, должна ли отрисовываться рамка.
     *
     * @return {@code true}, если рамка включена
     */
    public boolean borderVisible() {
        return borderVisible;
    }

    /**
     * Возвращает толщину рамки.
     *
     * @return толщина рамки в пикселях UI-пространства
     */
    public float borderWidth() {
        return borderWidth;
    }

    /**
     * Задаёт толщину рамки.
     *
     * @param borderWidth толщина рамки в пикселях UI-пространства
     * @return этот box для fluent-настройки
     */
    @XmlAttribute(value = "borderWidth", category = "Appearance", defaultValue = "1", description = "Border thickness in UI pixels.")
    public Box borderWidth(float borderWidth) {
        if (this.borderWidth == borderWidth) return this;
        this.borderWidth = borderWidth;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    /**
     * Анимирует толщину рамки за заданное время.
     *
     * @param borderWidth целевая толщина рамки
     * @param durationSeconds длительность анимации в секундах
     * @return этот box для fluent-настройки
     */
    public Box animateBorderWidth(float borderWidth, float durationSeconds) {
        return animateBorderWidth(borderWidth, TransitionSpec.of(durationSeconds));
    }

    /**
     * Анимирует толщину рамки по заданной transition-спецификации.
     *
     * @param borderWidth целевая толщина рамки
     * @param spec параметры transition'а
     * @return этот box для fluent-настройки
     */
    public Box animateBorderWidth(float borderWidth, TransitionSpec spec) {
        animateParameter("Box.borderWidth", this::borderWidth, this::borderWidth, borderWidth, spec);
        return this;
    }

    /**
     * Возвращает радиус скругления box'а.
     *
     * @return радиус скругления в пикселях UI-пространства
     */
    public float radius() {
        return radius;
    }

    /**
     * Задаёт радиус скругления box'а.
     *
     * @param radius радиус скругления в пикселях UI-пространства
     * @return этот box для fluent-настройки
     */
    @XmlAttribute(value = "radius", category = "Appearance", defaultValue = "0", description = "Corner radius in UI pixels.")
    public Box radius(float radius) {
        if (this.radius == radius) return this;
        this.radius = radius;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    /**
     * Анимирует радиус скругления за заданное время.
     *
     * @param radius целевой радиус скругления
     * @param durationSeconds длительность анимации в секундах
     * @return этот box для fluent-настройки
     */
    public Box animateRadius(float radius, float durationSeconds) {
        return animateRadius(radius, TransitionSpec.of(durationSeconds));
    }

    /**
     * Анимирует радиус скругления по заданной transition-спецификации.
     *
     * @param radius целевой радиус скругления
     * @param spec параметры transition'а
     * @return этот box для fluent-настройки
     */
    public Box animateRadius(float radius, TransitionSpec spec) {
        animateParameter("Box.radius", this::radius, this::radius, radius, spec);
        return this;
    }

    /**
     * Возвращает, участвует ли box в theme/style lookup.
     *
     * @return {@code true}, если значения renderer/цветов могут браться из темы
     */
    public boolean themeEnabled() {
        return themeEnabled;
    }

    /**
     * Включает или выключает theme/style lookup для этого box'а.
     *
     * <p>Когда theme отключена, renderer и style values берутся только из
     * локальных полей и fallback-значений.</p>
     *
     * @param themeEnabled {@code true}, чтобы применять theme/local styles
     * @return этот box для fluent-настройки
     */
    @XmlAttribute(value = "themeEnabled", category = "Appearance", defaultValue = "true", description = "Whether theme/style lookup can override box visual values.")
    public Box themeEnabled(boolean themeEnabled) {
        if (this.themeEnabled == themeEnabled) return this;
        this.themeEnabled = themeEnabled;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    @Override
    public void render(RenderContext context) {
        if (visibility() != Visibility.VISIBLE) return;
        pushOpacity(context);
        try {
            renderBox(context);
            renderContent(context);
        } finally {
            popOpacity(context);
        }
    }

    /**
     * Рендерит визуальную подложку box'а перед дочерними виджетами.
     *
     * @param context текущий render context
     */
    protected void renderBox(RenderContext context) {
        if (!boxVisualEnabled) return;
        applyTheme();
        BoxState state = boxState();
        DrawScope draw = new DrawScope(context, transform(), layoutBounds());
        if (boxRenderer != null) {
            boxRenderer.render(draw, state);
            return;
        }
        BoxRenderer styled = styleRendererOverride(BoxRenderer.class);
        if (styled != null) {
            styled.render(draw, state);
            return;
        }
        if (renderStylePlan(context, BoxState.class, state)) return;
        if (hasNonBoxStyleRendererOverride()) return;
        WidgetsRender.box().render(draw, state);
    }

    /**
     * Проверяет, задан ли для текущего widget type style renderer, который не является {@link BoxRenderer}.
     *
     * <p>{@code Box} исторически рисовал fallback-подложку для всех наследников. Это мешает новым
     * StylePack renderer'ам: например, стиль кнопки может указать custom {@code ButtonRenderer}, но
     * унаследованный {@code WidgetsRender.box()} всё равно рисовал старый фон под ним. Если renderer
     * найден на уровне текущего style type, но он не смог разрешиться как {@link BoxRenderer}, значит
     * его должен обработать сам наследник в своём renderContent/renderVisual path, а Box fallback
     * нужно пропустить.</p>
     *
     * @return {@code true}, если унаследованный fallback {@code WidgetsRender.box()} нужно пропустить
     */
    protected boolean hasNonBoxStyleRendererOverride() {
        if (!themeEnabled) return false;
        UIContext context = uiContext();
        Theme theme = context == null ? Theme.EMPTY : context.theme();
        String type = styleType();
        Style themeStyle = theme instanceof StylePack stylePack
                ? stylePack.resolveStyleFor(type, styleId(), styleClasses())
                : theme.styleFor(type);
        Object value = themeStyle.get(StyleKeys.RENDERER, null, null);
        if (!rendererValuePresent(value) && theme instanceof StylePack stylePack) {
            value = stylePack.rendererIdFor(type, styleId(), styleClasses());
        }
        for (Widget current : styleLookupChain()) {
            Style localStyle = current.localStyle(type);
            value = localStyle.get(StyleKeys.RENDERER, null, value);
        }
        return rendererValuePresent(value);
    }

    private static boolean rendererValuePresent(Object value) {
        return value != null && (!(value instanceof String rendererId) || !rendererId.isBlank());
    }

    /**
     * Возвращает renderer, который будет использован на текущем render-проходе.
     *
     * @return локальный, theme или default renderer
     */
    protected BoxRenderer effectiveBoxRenderer() {
        return boxRenderer == null ? styleRenderer(BoxRenderer.class, WidgetsRender.box()) : boxRenderer;
    }

    /**
     * Создаёт immutable snapshot visual-состояния для renderer'а.
     *
     * @return состояние box'а на текущий кадр
     */
    protected BoxState boxState() {
        TexturePlacement placement = backgroundTexture == null
                ? null
                : TexturePlacement.fit(backgroundTexture, backgroundTextureSource, layoutBounds(), backgroundTextureFit);
        return new BoxState(
                layoutBounds().x(),
                layoutBounds().y(),
                layoutBounds().width(),
                layoutBounds().height(),
                backgroundVisible,
                background.copy(),
                backgroundTexture,
                backgroundTextureTint.copy(),
                placement,
                backgroundTextureFit,
                radius,
                borderVisible,
                borderColor.copy(),
                borderWidth);
    }

    /**
     * Рендерит содержимое box'а после собственной подложки.
     *
     * @param context текущий render context
     */
    protected void renderContent(RenderContext context) {
        renderChildren(context);
    }

    /**
     * Применяет theme/local style значения к визуальным полям box'а.
     *
     * <p>Метод вызывается лениво перед render'ом и следит за версиями theme и
     * local style scopes, чтобы invalidate происходил при смене стилей.</p>
     */
    protected void applyTheme() {
        if (!themeEnabled) return;
        UIContext context = uiContext();
        long styleVersion = context == null ? Theme.EMPTY.version() : context.styleVersion();
        long scopeStyleVersion = scopeStyleVersion();
        if ((lastAppliedStyleVersion != Long.MIN_VALUE && lastAppliedStyleVersion != styleVersion)
                || (lastAppliedScopeStyleVersion != Long.MIN_VALUE && lastAppliedScopeStyleVersion != scopeStyleVersion)) {
            invalidate(InvalidationFlags.VISUAL);
        }
        lastAppliedStyleVersion = styleVersion;
        lastAppliedScopeStyleVersion = scopeStyleVersion;

        ColorView themedBackground = styleValue(StyleKeys.BACKGROUND_COLOR, background);
        TextureHandle themedBackgroundTexture = styleValue(StyleKeys.BACKGROUND_TEXTURE, backgroundTexture);
        ColorView themedBackgroundTextureTint = styleValue(StyleKeys.BACKGROUND_TEXTURE_TINT, backgroundTextureTint);
        ImageFit themedBackgroundTextureFit = styleValue(StyleKeys.BACKGROUND_TEXTURE_FIT, backgroundTextureFit);
        ColorView themedBorder = styleValue(StyleKeys.BORDER_COLOR, borderColor);
        Float themedBorderWidth = styleValue(StyleKeys.BORDER_WIDTH, borderWidth);
        Float themedRadius = styleValue(StyleKeys.RADIUS, radius);

        if (themedBackground != null) {
            background.set(themedBackground);
        }
        if (backgroundTexture != themedBackgroundTexture) {
            backgroundTexture = themedBackgroundTexture;
            invalidate(InvalidationFlags.VISUAL);
        }
        if (themedBackgroundTextureTint != null) {
            backgroundTextureTint.set(themedBackgroundTextureTint);
        }
        if (themedBackgroundTextureFit != null && backgroundTextureFit != themedBackgroundTextureFit) {
            backgroundTextureFit = themedBackgroundTextureFit;
            invalidate(InvalidationFlags.VISUAL);
        }
        if (themedBorder != null) {
            borderColor.set(themedBorder);
        }
        if (themedBorderWidth != null && borderWidth != themedBorderWidth) {
            borderWidth = themedBorderWidth;
            invalidate(InvalidationFlags.VISUAL);
        }
        if (themedRadius != null && radius != themedRadius) {
            radius = themedRadius;
            invalidate(InvalidationFlags.VISUAL);
        }
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
     * Возвращает style type, по которому theme ищет значения для этого box'а.
     *
     * @return имя runtime-класса по умолчанию
     */
    protected String styleType() {
        return getClass().getSimpleName();
    }

    @Override
    protected <T> T styleRenderer(Class<T> rendererType, T fallback) {
        return themeEnabled ? super.styleRenderer(rendererType, fallback) : fallback;
    }

    @Override
    protected <T> T styleRendererOverride(Class<T> rendererType) {
        return themeEnabled ? super.styleRendererOverride(rendererType) : null;
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
    protected <T> T styleValue(StyleKey<T> key, T fallback) {
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
    protected <T> T styleValue(StyleKey<T> key, WidgetState state, T fallback) {
        if (!themeEnabled) return fallback;
        UIContext context = uiContext();
        Theme theme = context == null ? Theme.EMPTY : context.theme();
        String type = styleType();
        Style themeStyle = theme instanceof StylePack stylePack
                ? stylePack.resolveStyleFor(type, styleId(), styleClasses())
                : theme.styleFor(type);
        T value = themeStyle.get(key, state, fallback);
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
