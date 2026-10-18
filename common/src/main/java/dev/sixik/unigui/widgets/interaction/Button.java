package dev.sixik.unigui.widgets.interaction;

import dev.sixik.unigui.api.animation.AnimationEasing;
import dev.sixik.unigui.api.animation.TransitionSpec;
import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.core.UIContext;
import dev.sixik.unigui.api.event.ButtonClickEvent;
import dev.sixik.unigui.api.event.EventPhase;
import dev.sixik.unigui.api.event.Event;
import dev.sixik.unigui.api.event.EventListener;
import dev.sixik.unigui.api.event.EventSubscription;
import dev.sixik.unigui.api.event.FocusGainedEvent;
import dev.sixik.unigui.api.event.FocusLostEvent;
import dev.sixik.unigui.api.event.PointerEvent;
import dev.sixik.unigui.api.event.PointerEnteredEvent;
import dev.sixik.unigui.api.event.PointerExitedEvent;
import dev.sixik.unigui.api.event.PointerPressedEvent;
import dev.sixik.unigui.api.event.PointerReleasedEvent;
import dev.sixik.unigui.api.input.PointerButton;
import dev.sixik.unigui.api.input.MouseCursor;
import dev.sixik.unigui.api.layout.Alignment;
import dev.sixik.unigui.api.input.KeyCodes;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.math.ColorView;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.render.ImageFit;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.render.SliceBorders;
import dev.sixik.unigui.api.render.TextureHandle;
import dev.sixik.unigui.api.render.TextureLayer;
import dev.sixik.unigui.api.render.shaders.ShaderDrawOptions;
import dev.sixik.unigui.api.render.shaders.ShaderHandle;
import dev.sixik.unigui.api.render.shaders.ShaderUniforms;
import dev.sixik.unigui.api.text.RichText;
import dev.sixik.unigui.api.style.StyleKeys;
import dev.sixik.unigui.api.style.WidgetState;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.xml.XmlAttribute;
import dev.sixik.unigui.api.xml.XmlWidgetName;
import dev.sixik.unigui.impl.text.TextEngine;
import dev.sixik.unigui.api.style.StyleAnimationIds;
import dev.sixik.unigui.api.style.StyleIds;
import dev.sixik.unigui.api.widget.render.WidgetRole;

import java.util.Objects;
import dev.sixik.unigui.widgets.containers.SurfaceWidget;

/**
 * Интерактивная кнопка с композируемой поверхностью и текстовым контентом.
 *
 * <p>Кнопка больше не наследуется от {@code Box}: поверхность (фон, рамка,
 * радиус) содержится в {@link SurfaceWidget}, а стандартный визуал рисует
 * поверхность плюс текстовый foreground. Источник фона выбирается через
 * {@link BackgroundKind} — цвет, текстура или шейдер переключаются стилем
 * без смены renderer-класса.</p>
 *
 * <p>Порядок отрисовки: custom {@link dev.sixik.unigui.api.widget.render.WidgetRender}
 * (заменяет весь визуал), затем поверхность, затем текст, затем дети.</p>
 */
@XmlWidgetName("Button")
public class Button extends SurfaceWidget<Button> {
    public static final String STYLE_TYPE = StyleIds.Widget.BUTTON;

    public static final class StyleProperties {
        public static final String BACKGROUND_COLOR = StyleIds.Key.BACKGROUND_COLOR;
        public static final String BORDER_COLOR = StyleIds.Key.BORDER_COLOR;
        public static final String BORDER_WIDTH = StyleIds.Key.BORDER_WIDTH;
        public static final String RADIUS = StyleIds.Key.RADIUS;
        public static final String TEXT_COLOR = StyleIds.Key.TEXT_COLOR;
        public static final String TRANSITION_DURATION = StyleIds.Key.TRANSITION_DURATION;
        public static final String TRANSITION_EASING = StyleIds.Key.TRANSITION_EASING;

        private StyleProperties() {
        }
    }

    public static final class AnimationProperties {
        public static final String TEXT_COLOR = StyleAnimationIds.Property.TEXT_COLOR;
        public static final String BACKGROUND_COLOR = StyleAnimationIds.Property.BACKGROUND_COLOR;
        public static final String BORDER_COLOR = StyleAnimationIds.Property.BORDER_COLOR;
        public static final String BORDER_WIDTH = StyleAnimationIds.Property.BORDER_WIDTH;
        public static final String RADIUS = StyleAnimationIds.Property.RADIUS;
        public static final String OPACITY = StyleAnimationIds.Property.OPACITY;
        public static final String SCALE = StyleAnimationIds.Property.SCALE;
        public static final String ROTATION_DEGREES = StyleAnimationIds.Property.ROTATION_DEGREES;
        public static final java.util.List<String> ALL = StyleAnimationIds.Property.BUTTON;

        private AnimationProperties() {
        }
    }

    public static final class AnimationEvents {
        public static final String ON_CLICK = StyleAnimationIds.Event.ON_CLICK;
        public static final String ON_FOCUS = StyleAnimationIds.Event.ON_FOCUS;
        public static final String ON_BLUR = StyleAnimationIds.Event.ON_BLUR;
        public static final String ON_HOVER = StyleAnimationIds.Event.ON_HOVER;
        public static final String ON_HOVER_ENTER = StyleAnimationIds.Event.ON_HOVER_ENTER;
        public static final String ON_HOVER_EXIT = StyleAnimationIds.Event.ON_HOVER_EXIT;
        public static final String ON_PRESS = StyleAnimationIds.Event.ON_PRESS;
        public static final String ON_RELEASE = StyleAnimationIds.Event.ON_RELEASE;
        public static final java.util.List<String> ALL = StyleAnimationIds.Event.BUTTON;

        private AnimationEvents() {
        }
    }

    protected static final float DEFAULT_TEXT_PADDING_X = 8.0f;
    protected static final float TEXT_PADDING_X = DEFAULT_TEXT_PADDING_X;
    protected static final float DEFAULT_HEIGHT = 18.0f;
    protected static final float DEFAULT_TEXT_PADDING_Y = (DEFAULT_HEIGHT - TextEngine.LINE_HEIGHT) * 0.5f;
    protected static final float APPROX_CHAR_WIDTH = TextEngine.APPROX_CHAR_WIDTH;
    /** Вынос фокус-кольца за границы кнопки (как у TextInput). */
    protected static final float FOCUS_RING_OUTSET = 2.0f;
    /** Толщина фокус-кольца. */
    protected static final float FOCUS_RING_WIDTH = 2.0f;

    private String text = "";
    private RichText richText = RichText.plain("");
    private final MutableColor textColor = new MutableColor(1.0f, 1.0f, 1.0f, 1.0f);
    private int pressedPointerId = -1;
    private float textPaddingX = DEFAULT_TEXT_PADDING_X;
    private float textPaddingY = DEFAULT_TEXT_PADDING_Y;
    private boolean pressed;
    private boolean focused;
    private boolean focusRingVisible;
    private final MutableColor focusRingColor = new MutableColor(0.29f, 0.62f, 1.0f, 0.30f);
    private boolean chevronVisible;
    private boolean chevronUp;
    private Alignment textAlignment =
            Alignment.CENTER;
    private boolean interactionTransitions;
    private TransitionSpec interactionTransition = TransitionSpec.of(0.10f, AnimationEasing.EASE_OUT);
    private float normalScale = 1.0f;
    private float hoveredScale = 1.025f;
    private float pressedScale = 0.970f;
    private float normalOpacity = 1.0f;
    private float pressedOpacity = 0.88f;
    private float disabledOpacity = 0.55f;
    private boolean customTextColor;
    /**
     * Явные per-state фоны: побеждают тему для своего состояния.
     * Ленивые (null = не задан): цвет отдельно, текстура отдельно,
     * чтобы можно было задать только заливку или только текстуру.
     */
    private MutableColor hoverBackground;
    private TextureLayer hoverTexture;
    /** Шейдер-оверлей hover-состояния; {@code null} отключает его. */
    private ShaderHandle hoverShader;
    /** Uniforms hover-шейдера, переиспользуемые между кадрами. */
    private ShaderUniforms hoverShaderUniforms = ShaderUniforms.empty();
    /** Параметры draw-команды hover-шейдера. */
    private ShaderDrawOptions hoverShaderOptions = ShaderDrawOptions.defaults();
    private MutableColor pressedBackground;
    private TextureLayer pressedTexture;

    public Button() {
        mouseCursor(MouseCursor.POINTER);
        backgroundVisible(true);
        borderVisible(true);
        focusable(true);
        // Только инвалидация: пометка custom ставится в явных сеттерах.
        // Тики theme-анимаций тоже пишут в textColor — по ним нельзя отличать
        // ручной цвет (иначе первое же нажатие с transition навсегда
        // замораживало текст в pressed-цвете).
        textColor.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
        focusRingColor.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
    }

    public Button(String text) {
        this();
        text(text);
    }

    public Button(RichText text) {
        this();
        richText(text);
    }

    public String text() {
        return text;
    }

    @XmlAttribute(value = "text", category = "Content", defaultValue = "", description = "Button label text.")
    public Button text(String text) {
        String normalized = normalize(text);
        RichText normalizedRichText = RichText.resolve(normalized);
        if (Objects.equals(this.richText, normalizedRichText)) return this;
        this.text = normalized;
        this.richText = normalizedRichText;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public RichText richText() {
        return richText;
    }

    public Button richText(RichText richText) {
        RichText normalized = richText == null ? RichText.plain("") : richText;
        if (Objects.equals(this.richText, normalized)) return this;
        this.richText = normalized;
        this.text = normalized.plainText();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public MutableColor textColor() {
        return textColor;
    }

    @XmlAttribute(value = "textColor", category = "Appearance", defaultValue = "#FFFFFFFF", description = "Button text color parsed from XML color syntax.")
    public Button textColor(ColorView color) {
        if (color != null) {
            customTextColor = true;
            textColor.set(color);
        }
        return this;
    }

    public Button textColor(float r, float g, float b, float a) {
        customTextColor = true;
        textColor.set(r, g, b, a);
        return this;
    }

    public boolean hasCustomTextColor() {
        return customTextColor;
    }

    public Button clearCustomTextColor() {
        this.customTextColor = false;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public Button animateTextColor(ColorView color, float durationSeconds) {
        animateColor(textColor, color, durationSeconds);
        return this;
    }

    public Button animateTextColor(ColorView color, TransitionSpec spec) {
        animateColor(textColor, color, spec);
        return this;
    }

    public float textPaddingX() {
        return textPaddingX;
    }

    public Button textPadding(float horizontal, float vertical) {
        return textPaddingX(horizontal).textPaddingY(vertical);
    }

    @XmlAttribute(value = "textPaddingX", category = "Layout", defaultValue = "8", description = "Horizontal padding around button text.")
    public Button textPaddingX(float textPaddingX) {
        float normalized = Float.isFinite(textPaddingX) ? Math.max(0.0f, textPaddingX) : DEFAULT_TEXT_PADDING_X;
        if (this.textPaddingX == normalized) return this;
        this.textPaddingX = normalized;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public float textPaddingY() {
        return textPaddingY;
    }

    @XmlAttribute(value = "textPaddingY", category = "Layout", defaultValue = "4", description = "Vertical padding around button text.")
    public Button textPaddingY(float textPaddingY) {
        float normalized = Float.isFinite(textPaddingY) ? Math.max(0.0f, textPaddingY) : DEFAULT_TEXT_PADDING_Y;
        if (this.textPaddingY == normalized) return this;
        this.textPaddingY = normalized;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    public boolean pressed() {
        return pressed;
    }

    /**
     * Возвращает, владеет ли кнопка клавиатурным фокусом.
     *
     * @return {@code true} после {@code FocusGainedEvent} до {@code FocusLostEvent}
     */
    public boolean focused() {
        return focused;
    }

    /**
     * Возвращает live-цвет внешнего фокус-кольца.
     *
     * <p>Кольцо рисуется самим классом (как у {@code TextInput}), тема только
     * отдаёт цвет через {@code ACCENT_COLOR}: по умолчанию акцент с 30% alpha.
     * Прозрачный цвет гасит кольцо без ломки остального хрома.</p>
     *
     * @return изменяемый цвет кольца вокруг сфокусированной кнопки
     */
    public MutableColor focusRingColor() {
        return focusRingColor;
    }

    /**
     * Возвращает, рисуется ли внешнее фокус-кольцо.
     *
     * <p>По умолчанию выключено: фокус виден по акцентной рамке из темы,
     * кольцо — opt-in для мест, где нужен более заметный индикатор.</p>
     *
     * @return {@code true}, если кольцо рисуется в фокусе
     */
    public boolean focusRingVisible() {
        return focusRingVisible;
    }

    /**
     * Включает или выключает внешнее фокус-кольцо.
     *
     * @param focusRingVisible {@code true}, чтобы рисовать кольцо в фокусе
     * @return эта кнопка для fluent-настройки
     */
    @XmlAttribute(value = "focusRingVisible", category = "Appearance", defaultValue = "false", description = "Whether the button paints an outer halo ring while focused.")
    public Button focusRingVisible(boolean focusRingVisible) {
        if (this.focusRingVisible == focusRingVisible) return this;
        this.focusRingVisible = focusRingVisible;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    /**
     * Возвращает явный фон hover-состояния.
     *
     * <p>Ленивый live-цвет: создаётся при первом обращении, поэтому сам факт
     * вызова геттера включает per-state фон. Заданный цвет побеждает тему
     * для hover-состояния; текстура того же состояния рисуется поверх него.
     * Сброс — {@link #clearHoverBackground()}.</p>
     *
     * @return изменяемый hover-цвет фона
     */
    public MutableColor hoverBackground() {
        if (hoverBackground == null) {
            hoverBackground = new MutableColor(1.0f, 1.0f, 1.0f, 1.0f);
            hoverBackground.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
            invalidate(InvalidationFlags.VISUAL);
        }
        return hoverBackground;
    }

    /**
     * Задаёт фон hover-состояния одним вызовом.
     *
     * @param color новый hover-цвет; {@code null} сбрасывает к теме
     * @return эта кнопка для fluent-настройки
     */
    @XmlAttribute(value = "hoverBackground", category = "Appearance", defaultValue = "", description = "Explicit hover background color; empty means themed.")
    public Button hoverBackground(ColorView color) {
        if (color == null) return clearHoverBackground();
        hoverBackground().set(color);
        return this;
    }

    /**
     * Сбрасывает явный фон hover-состояния: hover снова красится темой.
     *
     * @return эта кнопка для fluent-настройки
     */
    public Button clearHoverBackground() {
        if (hoverBackground == null) return this;
        hoverBackground = null;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    /**
     * Возвращает явный фон pressed-состояния.
     *
     * <p>Семантика как у {@link #hoverBackground()}: ленивый live-цвет,
     * побеждает тему для pressed, сброс — {@link #clearPressedBackground()}.</p>
     *
     * @return изменяемый pressed-цвет фона
     */
    public MutableColor pressedBackground() {
        if (pressedBackground == null) {
            pressedBackground = new MutableColor(1.0f, 1.0f, 1.0f, 1.0f);
            pressedBackground.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
            invalidate(InvalidationFlags.VISUAL);
        }
        return pressedBackground;
    }

    /**
     * Задаёт фон pressed-состояния одним вызовом.
     *
     * @param color новый pressed-цвет; {@code null} сбрасывает к теме
     * @return эта кнопка для fluent-настройки
     */
    @XmlAttribute(value = "pressedBackground", category = "Appearance", defaultValue = "", description = "Explicit pressed background color; empty means themed.")
    public Button pressedBackground(ColorView color) {
        if (color == null) return clearPressedBackground();
        pressedBackground().set(color);
        return this;
    }

    /**
     * Сбрасывает явный фон pressed-состояния: pressed снова красится темой.
     *
     * @return эта кнопка для fluent-настройки
     */
    public Button clearPressedBackground() {
        if (pressedBackground == null) return this;
        pressedBackground = null;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    /**
     * Возвращает явную текстуру hover-состояния.
     *
     * <p>Ленивый слой: создаётся при первом обращении. Пустой слой
     * ({@code null}-текстура) ничего не рисует; заданная текстура рисуется
     * поверх hover-заливки того же состояния. Сброс —
     * {@link #clearHoverTexture()}.</p>
     *
     * @return изменяемый hover-слой
     */
    public TextureLayer hoverTexture() {
        if (hoverTexture == null) {
            hoverTexture = wireStateTexture(new TextureLayer(null));
        }
        return hoverTexture;
    }

    /**
     * Задаёт текстуру hover-состояния одним вызовом.
     *
     * @param texture handle текстуры или {@code null} для сброса к теме
     * @return эта кнопка для fluent-настройки
     */
    @XmlAttribute(value = "hoverTexture", displayName = "Hover Texture", category = "Assets", defaultValue = "", description = "Explicit hover background texture; empty means themed.")
    public Button hoverTexture(TextureHandle texture) {
        if (texture == null) return clearHoverTexture();
        hoverTexture().texture(texture);
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    /**
     * Возвращает шейдер, рисуемый поверх hover-заливки и hover-текстуры.
     *
     * @return handle шейдера или {@code null}
     */
    public ShaderHandle hoverShader() {
        return hoverShader;
    }

    /**
     * Задаёт шейдерный слой для состояния наведения.
     *
     * @param shader шейдер; {@code null} отключает overlay
     * @return эта кнопка для fluent-настройки
     */
    @XmlAttribute(value = "hoverShader", category = "Appearance", defaultValue = "", description = "Ресурс шейдера поверх состояния наведения.")
    public Button hoverShader(ShaderHandle shader) {
        if (Objects.equals(hoverShader, shader)) return this;
        hoverShader = shader;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    /**
     * Сбрасывает шейдерный слой наведения.
     *
     * @return эта кнопка для fluent-настройки
     */
    public Button clearHoverShader() {
        return hoverShader(null);
    }

    /**
     * Возвращает uniforms hover-шейдера.
     *
     * @return изменяемый набор uniforms
     */
    public ShaderUniforms hoverShaderUniforms() {
        return hoverShaderUniforms;
    }

    /**
     * Задаёт uniforms hover-шейдера.
     *
     * @param uniforms набор uniforms или {@code null} для пустого набора
     * @return эта кнопка для fluent-настройки
     */
    public Button hoverShaderUniforms(ShaderUniforms uniforms) {
        hoverShaderUniforms = uniforms == null ? ShaderUniforms.empty() : uniforms;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    /**
     * Возвращает параметры отрисовки hover-шейдера.
     *
     * @return изменяемые параметры draw-команды
     */
    public ShaderDrawOptions hoverShaderOptions() {
        return hoverShaderOptions;
    }

    /**
     * Задаёт параметры отрисовки hover-шейдера.
     *
     * @param options параметры или {@code null} для значений по умолчанию
     * @return эта кнопка для fluent-настройки
     */
    public Button hoverShaderOptions(ShaderDrawOptions options) {
        hoverShaderOptions = options == null ? ShaderDrawOptions.defaults() : options;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    /**
     * Сбрасывает явную текстуру hover-состояния.
     *
     * @return эта кнопка для fluent-настройки
     */
    public Button clearHoverTexture() {
        if (hoverTexture == null) return this;
        unwireStateTexture(hoverTexture);
        hoverTexture = null;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    /**
     * Возвращает явную текстуру pressed-состояния.
     *
     * <p>Семантика как у {@link #hoverTexture()}: ленивый слой поверх
     * pressed-заливки, сброс — {@link #clearPressedTexture()}.</p>
     *
     * @return изменяемый pressed-слой
     */
    public TextureLayer pressedTexture() {
        if (pressedTexture == null) {
            pressedTexture = wireStateTexture(new TextureLayer(null));
        }
        return pressedTexture;
    }

    /**
     * Задаёт текстуру pressed-состояния одним вызовом.
     *
     * @param texture handle текстуры или {@code null} для сброса к теме
     * @return эта кнопка для fluent-настройки
     */
    @XmlAttribute(value = "pressedTexture", displayName = "Pressed Texture", category = "Assets", defaultValue = "", description = "Explicit pressed background texture; empty means themed.")
    public Button pressedTexture(TextureHandle texture) {
        if (texture == null) return clearPressedTexture();
        pressedTexture().texture(texture);
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    /**
     * Сбрасывает явную текстуру pressed-состояния.
     *
     * @return эта кнопка для fluent-настройки
     */
    public Button clearPressedTexture() {
        if (pressedTexture == null) return this;
        unwireStateTexture(pressedTexture);
        pressedTexture = null;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    private TextureLayer wireStateTexture(TextureLayer layer) {
        layer.source().onChanged(() -> invalidate(InvalidationFlags.VISUAL));
        layer.tint().onChanged(() -> invalidate(InvalidationFlags.VISUAL));
        invalidate(InvalidationFlags.VISUAL);
        return layer;
    }

    private void unwireStateTexture(TextureLayer layer) {
        layer.source().onChanged(null);
        layer.tint().onChanged(null);
    }

    /**
     * Возвращает горизонтальное выравнивание текста кнопки.
     *
     * @return выравнивание (default {@code CENTER})
     */
    public Alignment textAlignment() {
        return textAlignment;
    }

    /**
     * Задаёт горизонтальное выравнивание текста кнопки.
     *
     * <p>Нужно строкам списков (опции ComboBox — влево, {@code START}),
     * обычные кнопки остаются центрированными. {@code null} нормализуется
     * в {@code CENTER}.</p>
     *
     * @param textAlignment выравнивание текста
     * @return эта кнопка для fluent-настройки
     */
    public Button textAlignment(Alignment textAlignment) {
        Alignment normalized = textAlignment == null
                ? Alignment.CENTER
                : textAlignment;
        if (this.textAlignment == normalized) return this;
        this.textAlignment = normalized;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    /**
     * Ширина слота trailing-шеврона: глиф 12px + отступ 12px от правого края.
     * Текст при видимом шевроне ужимается левее слота и выравнивается влево.
     */
    public static final float CHEVRON_SLOT_WIDTH = 24.0f;
    /** Размер глифа шеврона в UI-пикселях. */
    public static final float CHEVRON_GLYPH_SIZE = 12.0f;

    /**
     * Возвращает, рисуется ли trailing-шеврон в правом слоте кнопки.
     *
     * @return {@code true}, если шеврон виден (default {@code false})
     */
    public boolean chevronVisible() {
        return chevronVisible;
    }

    /**
     * Показывает или прячет trailing-шеврон: отдельный слот справа через
     * layout (текст ужимается, а не едет вместе с глифом), а не пробел
     * в тексте. Нужно хедерам dropdown/ComboBox.
     *
     * @param chevronVisible {@code true}, чтобы рисовать шеврон справа
     * @return эта кнопка для fluent-настройки
     */
    public Button chevronVisible(boolean chevronVisible) {
        if (this.chevronVisible == chevronVisible) return this;
        this.chevronVisible = chevronVisible;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    /**
     * Возвращает направление шеврона: вверх ({@code true}) или вниз.
     *
     * @return направление шеврона
     */
    public boolean chevronUp() {
        return chevronUp;
    }

    /**
     * Задаёт направление шеврона (вверх — открытый список, вниз — закрытый).
     *
     * @param chevronUp {@code true} для шеврона вверх
     * @return эта кнопка для fluent-настройки
     */
    public Button chevronUp(boolean chevronUp) {
        if (this.chevronUp == chevronUp) return this;
        this.chevronUp = chevronUp;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public boolean interactionTransitions() {
        return interactionTransitions;
    }

    @XmlAttribute(value = "interactionTransitions", category = "Behavior", defaultValue = "false", description = "Enables built-in hover/press transition animation.")
    public Button interactionTransitions(boolean interactionTransitions) {
        if (this.interactionTransitions == interactionTransitions) return this;
        this.interactionTransitions = interactionTransitions;
        applyInteractionTransition();
        return this;
    }

    public Button interactionTransition(TransitionSpec interactionTransition) {
        this.interactionTransition = interactionTransition == null ? TransitionSpec.DEFAULT : interactionTransition;
        return this;
    }

    public Button interactionScales(float normal, float hovered, float pressed) {
        normalScale = sanitizeScale(normal);
        hoveredScale = sanitizeScale(hovered);
        pressedScale = sanitizeScale(pressed);
        applyInteractionTransition();
        return this;
    }

    public Button interactionOpacities(float normal, float pressed, float disabled) {
        normalOpacity = clamp01(normal);
        pressedOpacity = clamp01(pressed);
        disabledOpacity = clamp01(disabled);
        applyInteractionTransition();
        return this;
    }

    public EventSubscription onClick(EventListener<? super ButtonClickEvent> listener) {
        return on(ButtonClickEvent.TYPE, listener);
    }

    @Override
    @XmlAttribute(value = "enabled", category = "Behavior", defaultValue = "true", description = "Whether the widget can be interacted with.")
    public Button enabled(boolean enabled) {
        boolean changed = enabled() != enabled;
        super.enabled(enabled);
        if (changed) {
            applyInteractionTransition();
        }
        return this;
    }

    @Override
    public void measure(LayoutContext context) {
        if (visibility() == Visibility.COLLAPSED) {
            setDesiredSize(0.0f, 0.0f);
            return;
        }
        float textWidth = TextEngine.measureLineWidth(richText);
        float textHeight = intrinsicTextHeight();
        float trailingSlot = chevronVisible ? CHEVRON_SLOT_WIDTH : 0.0f;
        setDesiredSize(resolveDesiredSize(context,
                textWidth + textPaddingX * 2.0f + trailingSlot,
                textHeight + textPaddingY * 2.0f));
    }

    public ButtonClickEvent click() {
        ButtonClickEvent event = new ButtonClickEvent(this);
        UIContext context = uiContext();
        if (context == null) {
            emit(event);
        } else {
            context.routedEvents().dispatch(event);
        }
        return event;
    }

    @Override
    public void handle(Event event) {
        if (visibility() != Visibility.VISIBLE || !enabled()) return;
        boolean hoverChanged = event instanceof PointerEnteredEvent entered && entered.phase() == EventPhase.TARGET
                || event instanceof PointerExitedEvent exited && exited.phase() == EventPhase.TARGET;
        super.handle(event);
        if (event instanceof FocusGainedEvent) {
            setFocused(true);
            return;
        }
        if (event instanceof FocusLostEvent) {
            setFocused(false);
            return;
        }
        if (hoverChanged) {
            applyInteractionTransition();
        }
        if (event instanceof PointerEvent pointerEvent && pointerEvent.phase() == EventPhase.CAPTURE) return;

        // ToggleButton/Checkbox/ToggleSwitch реализуют собственное переключение
        // в наследниках. Обычная кнопка должна активироваться с клавиатуры так же,
        // как при клике мышью.
        if (!(this instanceof ToggleButton)
                && event instanceof dev.sixik.unigui.api.event.KeyPressedEvent key
                && key.phase() == EventPhase.TARGET
                && uiContext() != null
                && uiContext().focusManager().isFocused(this)
                && (key.keyCode() == KeyCodes.SPACE
                || key.keyCode() == KeyCodes.ENTER
                || key.keyCode() == KeyCodes.KEYPAD_ENTER)) {
            click();
            event.cancel();
            return;
        }

        if (event instanceof PointerReleasedEvent pointer && pointer.button() == PointerButton.PRIMARY && ownsPressedPointer(pointer)) {
            boolean wasPressed = pressed;
            boolean releasedInside = pointerInside(pointer);
            releasePressedPointer(pointer.pointerId());
            if (wasPressed && releasedInside && !event.isCancelled()) {
                click();
            }
            if (wasPressed) {
                event.cancel();
            }
            clearPointerFocus();
            return;
        }

        if (event instanceof PointerExitedEvent pointer && ownsPressedPointer(pointer)) {
            releasePressedPointer(pointer.pointerId());
        }

        if (event.isCancelled()) return;

        if (event instanceof PointerPressedEvent pointer && pointer.button() == PointerButton.PRIMARY) {
            pressPointer(pointer.pointerId());
            event.cancel();
        }
    }

    @Override
    protected void renderContent(RenderContext context) {
        applyTheme();
        DrawScope draw = new DrawScope(context, transform(), layoutBounds());
        renderButtonText(draw);
        renderFocusRing(draw);
        super.renderContent(context);
    }

    @Override
    protected void renderSurface(RenderContext context) {
        super.renderSurface(context);
        renderStateBackground(context);
    }

    /**
     * Рисует явный per-state фон поверх базового surface-рендера.
     *
     * <p>Выбор в классе, не в стиле: pressed побеждает hover, hover побеждает
     * normal-базу. Внутри состояния сначала заливка, затем текстура поверх неё
     * (та же композиция, что у базового фона). Пустое состояние (ни цвета, ни
     * текстуры) — no-op, виден themed-базовый фон под ним.</p>
     *
     * @param context текущий render context
     */
    protected void renderStateBackground(RenderContext context) {
        if (context == null || !backgroundVisible()) return;
        MutableColor fill = null;
        TextureLayer texture = null;
        ShaderHandle shader = null;
        if (pressed) {
            fill = pressedBackground;
            texture = pressedTexture;
        } else if (hovered()) {
            fill = hoverBackground;
            texture = hoverTexture;
            shader = hoverShader;
        }
        boolean hasFill = fill != null && fill.a() > 0.0f;
        boolean hasTexture = texture != null && texture.visible() && !texture.isEmpty();
        if (!hasFill && !hasTexture && shader == null) return;
        DrawScope draw = new DrawScope(context, transform(), layoutBounds());
        float x = layoutBounds().x();
        float y = layoutBounds().y();
        float width = layoutBounds().width();
        float height = layoutBounds().height();
        float radius = radius();
        if (hasFill) {
            draw.roundedRectFilled(x, y, width, height, radius, fill);
        }
        if (hasTexture) {
            renderTextureLayer(draw, texture, x, y, width, height, radius);
        }
        if (shader != null) {
            draw.shader(shader, x, y, width, height, hoverShaderUniforms, hoverShaderOptions);
        }
    }

    /**
     * Рисует центрированный текст кнопки.
     *
     * <p>Используется кнопкой и наследниками с тем же текстовым layout
     * (toggle button, tool button). При видимом шевроне текст прижимается
     * влево и ужимается левее шеврон-слота, а сам шеврон рисуется справа.</p>
     *
     * @param draw draw scope виджета
     */
    protected void renderButtonText(DrawScope draw) {
        if (richText == null || richText.isEmpty()) {
            renderChevron(draw);
            return;
        }
        float x = layoutBounds().x();
        float y = layoutBounds().y();
        float width = layoutBounds().width();
        float height = layoutBounds().height();
        float reservedRight = chevronVisible ? CHEVRON_SLOT_WIDTH : 0.0f;
        float contentX = x + textPaddingX;
        float contentWidth = Math.max(0.0f, width - textPaddingX - Math.max(textPaddingX, reservedRight));
        if (contentWidth <= 0.0f) {
            renderChevron(draw);
            return;
        }
        float textWidth = TextEngine.measureLineWidth(draw.context(), richText);
        float textHeight = TextEngine.measureTextHeight(draw.context(), richText);
        float drawWidth = Math.min(contentWidth, Math.max(0.0f, textWidth));
        float drawHeight = Math.min(Math.max(0.0f, height), Math.max(0.0f, textHeight));
        if (drawWidth <= 0.0f || drawHeight <= 0.0f) {
            renderChevron(draw);
            return;
        }
        float drawX;
        if (chevronVisible) {
            drawX = contentX;
        } else if (textAlignment == Alignment.START) {
            drawX = contentX;
        } else if (textAlignment == Alignment.END) {
            drawX = contentX + Math.max(0.0f, contentWidth - drawWidth);
        } else {
            drawX = contentX + Math.max(0.0f, contentWidth - drawWidth) * 0.5f;
        }
        float drawY = y + Math.max(0.0f, height - drawHeight) * 0.5f;
        renderLabel(draw, richText, contentX, y, contentWidth, height,
                drawX, drawY, drawWidth, drawHeight, textColor);
        renderChevron(draw);
    }

    /**
     * Рисует trailing-шеврон в правом слоте кнопки.
     *
     * <p>Шеврон — два stroke-отрезка (уголок вверх/вниз), а не filled
     * треугольник: совпадает с референсом и не зависит от шрифта.
     * No-op, если слот выключен или кнопке не хватило места.</p>
     *
     * @param draw draw scope виджета
     */
    protected void renderChevron(DrawScope draw) {
        if (draw == null || !chevronVisible) return;
        float x = layoutBounds().x();
        float y = layoutBounds().y();
        float width = layoutBounds().width();
        float height = layoutBounds().height();
        if (width < CHEVRON_SLOT_WIDTH || height <= 0.0f) return;
        float centerX = x + width - textPaddingX - CHEVRON_GLYPH_SIZE * 0.5f;
        float centerY = y + height * 0.5f;
        float half = CHEVRON_GLYPH_SIZE * 0.5f - 1.0f;
        float rise = CHEVRON_GLYPH_SIZE * 0.25f;
        Paint paint = Paint.stroke(textColor, 2.0f);
        if (chevronUp) {
            draw.line(centerX - half, centerY + rise, centerX, centerY - rise, paint);
            draw.line(centerX, centerY - rise, centerX + half, centerY + rise, paint);
        } else {
            draw.line(centerX - half, centerY - rise, centerX, centerY + rise, paint);
            draw.line(centerX, centerY + rise, centerX + half, centerY - rise, paint);
        }
    }

    /**
     * Рисует внешнее фокус-кольцо вокруг кнопки.
     *
     * <p>Геометрия — в классе (outset/width, как у {@code TextInput}), цвет —
     * из темы через {@code ACCENT_COLOR}. No-op без фокуса, при выключенном
     * {@link #focusRingVisible()} или прозрачном цвете. Рисуется поверх
     * текста: тонкое кольцо снаружи границ текст не перекрывает.</p>
     *
     * @param draw draw scope виджета
     */
    protected void renderFocusRing(DrawScope draw) {
        if (draw == null || !focused || !focusRingVisible || focusRingColor.a() <= 0.0f) return;
        float x = layoutBounds().x();
        float y = layoutBounds().y();
        float width = layoutBounds().width();
        float height = layoutBounds().height();
        if (width <= 0.0f || height <= 0.0f) return;
        draw.roundedRectStroke(
                x - FOCUS_RING_OUTSET,
                y - FOCUS_RING_OUTSET,
                width + FOCUS_RING_OUTSET * 2.0f,
                height + FOCUS_RING_OUTSET * 2.0f,
                radius() + FOCUS_RING_OUTSET,
                focusRingColor, FOCUS_RING_WIDTH);
    }

    /**
     * Рисует текст с обрезкой по области контента.
     *
     * @param draw draw scope
     * @param text текст
     * @param clipX X области обрезки
     * @param clipY Y области обрезки
     * @param clipWidth ширина области обрезки
     * @param clipHeight высота области обрезки
     * @param drawX X отрисовки текста
     * @param drawY Y отрисовки текста
     * @param drawWidth ширина отрисовки текста
     * @param drawHeight высота отрисовки текста
     * @param color цвет текста
     */
    protected static void renderLabel(DrawScope draw, RichText text,
                                      float clipX, float clipY, float clipWidth, float clipHeight,
                                      float drawX, float drawY, float drawWidth, float drawHeight,
                                      ColorView color) {
        if (draw == null || text == null || text.isEmpty() || color == null
                || clipWidth <= 0.0f || clipHeight <= 0.0f
                || drawWidth <= 0.0f || drawHeight <= 0.0f) {
            return;
        }
        draw.pushTextClip(clipX, clipY, clipWidth, clipHeight);
        try {
            TextEngine.drawInline(draw, text, drawX, drawY, drawWidth, drawHeight, Paint.fill(color));
        } finally {
            draw.popClip();
        }
    }

    /**
     * Рисует квадратный indicator checkbox.
     *
     * @param draw draw scope
     * @param x X индикатора
     * @param y Y индикатора
     * @param size размер индикатора
     * @param innerSize размер внутренней метки
     * @param checked отмечен ли
     * @param indeterminate неопределённое ли состояние
     * @param borderColor цвет рамки
     * @param indicatorColor цвет метки
     */
    protected static void renderCheckIndicator(DrawScope draw, float x, float y, float size, float innerSize,
                                               boolean checked, boolean indeterminate,
                                               ColorView borderColor, ColorView indicatorColor) {
        if (draw == null || size <= 0.0f) return;
        draw.roundedRectStroke(x, y, size, size, 2.0f, borderColor, 1.0f);
        if (indeterminate) {
            float dashWidth = Math.max(1.0f, innerSize);
            float dashHeight = Math.max(1.0f, innerSize * 0.28f);
            float offsetX = Math.max(0.0f, (size - dashWidth) * 0.5f);
            float offsetY = Math.max(0.0f, (size - dashHeight) * 0.5f);
            draw.rectFilled(x + offsetX, y + offsetY, dashWidth, dashHeight, indicatorColor);
        } else if (checked) {
            float inner = Math.max(0.0f, innerSize);
            float offset = Math.max(0.0f, (size - inner) * 0.5f);
            if (inner > 0.0f) {
                draw.rectFilled(x + offset, y + offset, inner, inner, indicatorColor);
            }
        }
    }

    /**
     * Рисует круглый indicator radio button.
     *
     * @param draw draw scope
     * @param x X индикатора
     * @param y Y индикатора
     * @param size размер индикатора
     * @param innerSize размер внутренней точки
     * @param progress прогресс выбора от 0 до 1
     * @param borderColor цвет кольца
     * @param indicatorColor цвет точки
     */
    protected static void renderRadioIndicator(DrawScope draw, float x, float y, float size, float innerSize,
                                               float progress, ColorView borderColor, ColorView indicatorColor) {
        if (draw == null || size <= 0.0f) return;
        draw.circleStroke(x, y, size, size, borderColor, 1.0f);
        float normalizedProgress = Math.max(0.0f, Math.min(1.0f, progress));
        if (normalizedProgress <= 0.0f || innerSize <= 0.0f) return;
        float inner = innerSize * normalizedProgress;
        float offset = Math.max(0.0f, (size - inner) * 0.5f);
        draw.circleFilled(x + offset, y + offset, inner, inner, indicatorColor);
    }

    @Override
    protected WidgetRole renderRole() {
        return WidgetRole.BUTTON;
    }

    private static String normalize(String text) {
        return text == null ? "" : text;
    }

    protected float intrinsicTextHeight() {
        float measured = TextEngine.measureTextHeight(richText);
        return measured > 0.0f ? measured : TextEngine.LINE_HEIGHT;
    }

    @Override
    protected void applyTheme() {
        super.applyTheme();
        ColorView themedAccent = styleValue(StyleKeys.ACCENT_COLOR, styleState(), null);
        if (themedAccent != null) {
            focusRingColor.set(themedAccent.r(), themedAccent.g(), themedAccent.b(), 0.30f);
        }
        if (customTextColor) return;
        ColorView themedText = styleValue(StyleKeys.TEXT_COLOR, styleState(), textColor);
        if (themedText != null) {
            animateColor(textColor, themedText, styleTransition());
        }
    }

    @Override
    protected WidgetState styleState() {
        if (!enabled()) return super.styleState();
        if (pressed) return WidgetState.PRESSED;
        if (focused) return WidgetState.FOCUSED;
        return super.styleState();
    }

    private void setFocused(boolean focused) {
        if (this.focused == focused) return;
        this.focused = focused;
        applyTheme();
        invalidate(InvalidationFlags.VISUAL);
    }

    private void setPressed(boolean pressed) {
        if (this.pressed == pressed) return;
        this.pressed = pressed;
        invalidate(InvalidationFlags.VISUAL);
        applyInteractionTransition();
    }

    private void pressPointer(int pointerId) {
        pressedPointerId = pointerId;
        UIContext context = uiContext();
        if (context != null) {
            context.capturePointer(pointerId, this);
        }
        setPressed(true);
    }

    private void releasePressedPointer(int pointerId) {
        UIContext context = uiContext();
        if (context != null) {
            context.releasePointer(pointerId, this);
        }
        pressedPointerId = -1;
        setPressed(false);
    }

    /**
     * Снимает фокус, который экран временно назначил кнопке для pointer-click.
     * Клавиатурная навигация продолжает управлять фокусом обычным образом.
     */
    private void clearPointerFocus() {
        UIContext context = uiContext();
        if (context != null && context.focusManager().isFocused(this)) {
            context.focusManager().clearFocus();
        }
    }

    private boolean ownsPressedPointer(PointerEvent pointer) {
        return pressed && pointer != null && (pressedPointerId < 0 || pressedPointerId == pointer.pointerId());
    }

    private boolean pointerInside(PointerEvent pointer) {
        if (pointer == null) return false;
        return pointer.localX() >= 0.0f
                && pointer.localY() >= 0.0f
                && pointer.localX() <= layoutBounds().width()
                && pointer.localY() <= layoutBounds().height();
    }

    private void applyInteractionTransition() {
        if (!interactionTransitions) return;
        float targetScale = pressed ? pressedScale : hovered() ? hoveredScale : normalScale;
        float targetOpacity = !enabled() ? disabledOpacity : pressed ? pressedOpacity : normalOpacity;
        animateScale(targetScale, targetScale, interactionTransition);
        animateOpacity(targetOpacity, interactionTransition);
    }

    private static float sanitizeScale(float value) {
        return Float.isFinite(value) ? Math.max(0.01f, value) : 1.0f;
    }

    private static float clamp01(float value) {
        if (!Float.isFinite(value)) return 1.0f;
        return Math.max(0.0f, Math.min(1.0f, value));
    }
}
