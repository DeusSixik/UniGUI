package dev.sixik.unigui.widgets.rendering;

import dev.sixik.unigui.api.core.FrameContext;
import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.core.UIContext;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.layout.LayoutSize;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.math.Transform;
import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.render.ImageFit;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.render.RenderBackend;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.render.RenderTarget;
import dev.sixik.unigui.api.render.RenderTargetOptions;
import dev.sixik.unigui.api.render.TextureFilter;
import dev.sixik.unigui.api.render.TextureHandle;
import dev.sixik.unigui.api.render.TexturePlacement;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.impl.render.WidgetTextureRenderer;
import dev.sixik.unigui.impl.widget.WidgetBase;
import dev.sixik.unigui.widgets.containers.StackPanel;
import dev.sixik.unigui.xaml.XamlWidget;

import java.util.Collections;
import java.util.List;

/**
 * Рендерит содержимое в offscreen-текстуру и отдаёт её как обычную {@link TextureHandle}.
 *
 * <p>Содержимое — дочерний виджет ({@link #content(Widget)}), функция отрисовки ({@link #painter(Painter)}) или
 * оба сразу (сначала painter, поверх него дочерний виджет). Текстуру можно показать на месте виджета
 * ({@link #displayed(boolean)}, {@link #fit(ImageFit)}, {@link #filter(TextureFilter)}) или забрать через
 * {@link #texture()} и использовать в другом месте: фон {@code Surface}, картинка, шейдер, материал.</p>
 *
 * <p><b>Размеры.</b> Логический холст — область, в координатах которой рисует содержимое: по умолчанию размер
 * виджета, или {@link #canvasSize(float, float)}. Пиксельный размер текстуры по умолчанию равен
 * {@code холст × pixelScale × resolutionScale}, где {@code pixelScale} — плотность пикселей экрана
 * ({@link RenderContext#pixelScale()}), так что текстура на месте виджета не размыта; {@code resolutionScale}
 * больше {@code 1} даёт суперсэмплинг. {@link #textureSize(int, int)} задаёт размер явно (например 64×64 для
 * пиксель-арта с {@link TextureFilter#NEAREST}); тогда холст рисуется с равномерным масштабом от левого
 * верхнего угла.</p>
 *
 * <p><b>Обновление.</b> {@link UpdateMode#EVERY_FRAME} перерисовывает текстуру каждый кадр (анимации, 3D),
 * {@link UpdateMode#ON_CHANGE} — когда изменились размер, параметры или поддерево содержимого, а также после
 * {@link #requestRender()}; {@link UpdateMode#MANUAL} — только после {@link #requestRender()}. Painter не
 * сообщает об изменениях сам: в режиме {@code ON_CHANGE} после смены его данных нужен
 * {@link #requestRender()}.</p>
 *
 * <p><b>Ввод.</b> Дочерний виджет раскладывается в экранных координатах на месте этого виджета, поэтому
 * клики и наведение доходят до него напрямую, если холст совпадает с размером виджета и текстура показывается
 * растянутой ({@link ImageFit#STRETCH}).</p>
 *
 * <p>Текстура рисуется только во время {@link #render(RenderContext)}: невидимый или не вставленный в дерево
 * виджет её не обновляет. Чтобы текстура обновлялась, но не показывалась, используйте
 * {@link #displayed(boolean) displayed(false)}.</p>
 */
@XamlWidget("RenderToTexture")
public class RenderToTexture extends WidgetBase {
    public static final String STYLE_TYPE = dev.sixik.unigui.api.style.StyleIds.Widget.RENDER_TO_TEXTURE;

    /**
     * Самая большая сторона текстуры, которую виджет создаст.
     */
    public static final int MAX_TEXTURE_SIZE = 8192;

    /**
     * Когда перерисовывать текстуру.
     */
    public enum UpdateMode {
        /**
         * Каждый кадр.
         */
        EVERY_FRAME,
        /**
         * При изменении размера, параметров или поддерева содержимого и после {@link #requestRender()}.
         */
        ON_CHANGE,
        /**
         * Только после {@link #requestRender()} (и при первом показе).
         */
        MANUAL
    }

    /**
     * Почему текстура перерисована (или {@link #NONE}, если взята прежняя).
     */
    public enum RenderReason {
        NONE,
        NO_TEXTURE,
        REQUESTED,
        EVERY_FRAME,
        RESIZED,
        TARGET_OPTIONS_CHANGED,
        BACKEND_CHANGED,
        OWN_LAYOUT_DIRTY,
        OWN_TEXTURE_DIRTY,
        CHILD_SUBTREE_DIRTY
    }

    /**
     * Рисует содержимое текстуры.
     */
    @FunctionalInterface
    public interface Painter {
        /**
         * @param context context offscreen-кадра; координаты от {@code (0, 0)} до {@code (width, height)}
         * @param width   ширина логического холста
         * @param height  высота логического холста
         */
        void paint(RenderContext context, float width, float height);
    }

    private final MutableColor tint = new MutableColor(1.0f, 1.0f, 1.0f, 1.0f);
    private final MutableColor clearColor = new MutableColor(0.0f, 0.0f, 0.0f, 0.0f);
    private final Transform contentShift = new Transform();
    /**
     * Переиспользуемый результат расчёта размеров и масштаба цели.
     */
    private final TargetSize measuredTarget = new TargetSize();
    private Widget content;
    private List<Widget> childrenView = Collections.emptyList();
    private Painter painter;
    private UpdateMode updateMode = UpdateMode.EVERY_FRAME;
    private RenderTargetOptions targetOptions = RenderTargetOptions.COLOR;
    private float canvasWidth = Float.NaN;
    private float canvasHeight = Float.NaN;
    private int fixedTextureWidth;
    private int fixedTextureHeight;
    private float resolutionScale = 1.0f;
    private boolean displayed = true;
    private ImageFit fit = ImageFit.STRETCH;
    private TextureFilter filter = TextureFilter.LINEAR;

    private WidgetTextureRenderer textureRenderer;
    private RenderBackend rendererBackend;
    private SampledTexture texture;
    private int textureWidth;
    private int textureHeight;
    private float renderedCanvasWidth;
    private float renderedCanvasHeight;
    /**
     * Масштаб 2D-прохода в последней успешно отрисованной текстуре.
     */
    private float renderedCoordinateScale;
    private boolean renderRequested = true;
    private boolean targetOptionsChanged;
    private boolean backendChanged;
    private long renderCount;
    private RenderReason lastRenderReason = RenderReason.NONE;

    public RenderToTexture() {
        renderCachingEnabled(false);
        tint.onChanged(() -> invalidate(InvalidationFlags.VISUAL));
        clearColor.onChanged(this::requestRender);
    }

    public RenderToTexture(Widget content) {
        this();
        content(content);
    }

    // ------------------------------------------------------------------ content

    public Widget content() {
        return content;
    }

    /**
     * Задаёт дочерний виджет, который рисуется в текстуру.
     *
     * @param content виджет или {@code null}
     * @return этот виджет
     */
    public RenderToTexture content(Widget content) {
        if (this.content == content) return this;
        if (content == this) throw new IllegalArgumentException("RenderToTexture cannot contain itself");

        detachContent();
        this.content = content;
        attachContent(content);
        childrenView = content == null ? Collections.emptyList() : Collections.singletonList(content);
        requestRender();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.TEXTURE);
        return this;
    }

    public Painter painter() {
        return painter;
    }

    /**
     * Задаёт функцию отрисовки. Она рисует под дочерним виджетом, если он тоже задан.
     *
     * @param painter функция или {@code null}
     * @return этот виджет
     */
    public RenderToTexture painter(Painter painter) {
        if (this.painter == painter) return this;
        this.painter = painter;
        requestRender();
        return this;
    }

    // ------------------------------------------------------------------ update

    public UpdateMode updateMode() {
        return updateMode;
    }

    public RenderToTexture updateMode(UpdateMode updateMode) {
        this.updateMode = updateMode == null ? UpdateMode.EVERY_FRAME : updateMode;
        return this;
    }

    /**
     * Просит перерисовать текстуру при следующей отрисовке виджета (в любом режиме обновления).
     */
    public void requestRender() {
        renderRequested = true;
        invalidate(InvalidationFlags.TEXTURE | InvalidationFlags.VISUAL);
    }

    // ------------------------------------------------------------------ size

    /**
     * Задаёт логический размер холста. По умолчанию холст равен размеру виджета.
     */
    public RenderToTexture canvasSize(float width, float height) {
        if (!(width > 0.0f) || !(height > 0.0f) || !Float.isFinite(width) || !Float.isFinite(height)) {
            throw new IllegalArgumentException("Canvas size must be positive, got " + width + "x" + height);
        }
        canvasWidth = width;
        canvasHeight = height;
        invalidate(InvalidationFlags.LAYOUT);
        requestRender();
        return this;
    }

    /**
     * Возвращает холст к размеру виджета.
     */
    public RenderToTexture clearCanvasSize() {
        canvasWidth = Float.NaN;
        canvasHeight = Float.NaN;
        invalidate(InvalidationFlags.LAYOUT);
        requestRender();
        return this;
    }

    /**
     * @return {@code true}, если холст задан явно
     */
    public boolean hasCanvasSize() {
        return Float.isFinite(canvasWidth);
    }

    /**
     * Задаёт пиксельный размер текстуры явно. {@code 0} по любой стороне возвращает автоматический размер.
     */
    public RenderToTexture textureSize(int width, int height) {
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("Texture size cannot be negative, got " + width + "x" + height);
        }
        fixedTextureWidth = Math.min(width, MAX_TEXTURE_SIZE);
        fixedTextureHeight = Math.min(height, MAX_TEXTURE_SIZE);
        requestRender();
        return this;
    }

    public float resolutionScale() {
        return resolutionScale;
    }

    /**
     * Множитель автоматического размера текстуры: {@code 2} — суперсэмплинг, {@code 0.5} — половинное
     * разрешение.
     */
    public RenderToTexture resolutionScale(float resolutionScale) {
        if (!(resolutionScale > 0.0f) || !Float.isFinite(resolutionScale)) {
            throw new IllegalArgumentException("Resolution scale must be positive, got " + resolutionScale);
        }
        if (this.resolutionScale == resolutionScale) return this;
        this.resolutionScale = resolutionScale;
        requestRender();
        return this;
    }

    // ------------------------------------------------------------------ target

    public RenderTargetOptions targetOptions() {
        return targetOptions;
    }

    /**
     * Задаёт параметры offscreen-цели, например {@link RenderTargetOptions#COLOR_DEPTH} для 3D.
     */
    public RenderToTexture targetOptions(RenderTargetOptions targetOptions) {
        RenderTargetOptions safeOptions = targetOptions == null ? RenderTargetOptions.COLOR : targetOptions;
        if (safeOptions.equals(this.targetOptions)) return this;
        this.targetOptions = safeOptions;
        resetRenderer();
        targetOptionsChanged = true;
        requestRender();
        return this;
    }

    /**
     * Включает буфер глубины (нужен для 3D).
     */
    public RenderToTexture depth(boolean depth) {
        return targetOptions(depth ? RenderTargetOptions.COLOR_DEPTH : RenderTargetOptions.COLOR);
    }

    /**
     * @return цвет, которым текстура заливается перед отрисовкой (по умолчанию прозрачный)
     */
    public MutableColor clearColor() {
        return clearColor;
    }

    // ------------------------------------------------------------------ display

    public boolean displayed() {
        return displayed;
    }

    /**
     * Показывать ли текстуру на месте виджета. При {@code false} текстура обновляется, но не рисуется.
     */
    public RenderToTexture displayed(boolean displayed) {
        if (this.displayed == displayed) return this;
        this.displayed = displayed;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public ImageFit fit() {
        return fit;
    }

    /**
     * Как текстура вписывается в виджет. {@link ImageFit#NINE_SLICE} не поддерживается и рисуется как
     * {@link ImageFit#STRETCH}.
     */
    public RenderToTexture fit(ImageFit fit) {
        this.fit = fit == null ? ImageFit.STRETCH : fit;
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public TextureFilter filter() {
        return filter;
    }

    /**
     * Фильтрация текстуры при выводе и для {@link #texture()}: {@link TextureFilter#LINEAR} или
     * {@link TextureFilter#NEAREST} для пиксель-арта.
     */
    public RenderToTexture filter(TextureFilter filter) {
        TextureFilter safe = filter == null ? TextureFilter.LINEAR : filter;
        if (this.filter == safe) return this;
        this.filter = safe;
        if (texture != null) texture = new SampledTexture(texture.source(), safe);
        invalidate(InvalidationFlags.VISUAL);
        return this;
    }

    public MutableColor tint() {
        return tint;
    }

    // ------------------------------------------------------------------ result

    /**
     * @return текстура с результатом или {@code null}, пока виджет ни разу не отрисовался
     */
    public TextureHandle texture() {
        return texture;
    }

    /**
     * @return ширина текстуры в пикселях ({@code 0} до первой отрисовки)
     */
    public int textureWidth() {
        return textureWidth;
    }

    /**
     * @return высота текстуры в пикселях ({@code 0} до первой отрисовки)
     */
    public int textureHeight() {
        return textureHeight;
    }

    /**
     * @return сколько раз текстура перерисовывалась
     */
    public long renderCount() {
        return renderCount;
    }

    /**
     * @return причина последней перерисовки
     */
    public RenderReason lastRenderReason() {
        return lastRenderReason;
    }

    // ------------------------------------------------------------------ widget

    @Override
    public void setUiContextInternal(UIContext uiContext) {
        super.setUiContextInternal(uiContext);
        attachContent(content);
    }

    @Override
    public List<Widget> children() {
        return childrenView;
    }

    @Override
    public void measure(LayoutContext context) {
        if (visibility() == Visibility.COLLAPSED) {
            setDesiredSize(LayoutSize.ZERO);
            return;
        }
        float preferredWidth = hasCanvasSize() ? canvasWidth : 0.0f;
        float preferredHeight = hasCanvasSize() ? canvasHeight : 0.0f;
        if (content != null) {
            content.measure(context);
            if (!hasCanvasSize()) {
                preferredWidth = StackPanel.preferredWidth(content, 0.0f);
                preferredHeight = StackPanel.preferredHeight(content, 0.0f);
            }
        }
        setDesiredSize(resolveDesiredSize(context, preferredWidth, preferredHeight));
    }

    @Override
    public void arrange(RectView bounds) {
        super.arrange(bounds);
        if (content != null && content.visibility() != Visibility.COLLAPSED) {
            StackPanel.arrangeChild(content, bounds.x(), bounds.y(),
                    resolvedCanvasWidth(bounds), resolvedCanvasHeight(bounds));
        }
    }

    @Override
    public void tick(FrameContext frame) {
        super.tick(frame);
        if (content != null) content.tick(frame);
        if (updateMode == UpdateMode.EVERY_FRAME) {
            // Кэширующие экраны пересобирают кадр только после invalidation.
            invalidate(InvalidationFlags.TEXTURE | InvalidationFlags.VISUAL);
        }
    }

    @Override
    public void render(RenderContext context) {
        boolean rawPass = hasRawPass();
        if (content == null && painter == null && !rawPass) return;
        pushOpacity(context);
        try {
            RenderBackend backend = context.backend();
            if (backend == null) {
                // Без backend'а текстуру не создать: рисуем содержимое напрямую.
                renderDirect(context);
                return;
            }
            RenderReason reason = updateTexture(context, backend, rawPass);
            afterTextureUpdate(reason);
            DrawScope draw = new DrawScope(context, transform(), layoutBounds());
            if (!renderCustomVisual(draw)) {
                if (displayed) drawTexture(draw);
                renderOverlay(draw, reason);
            }
        } finally {
            popOpacity(context);
        }
    }

    @Override
    public void dispose() {
        resetRenderer();
        if (content != null) content.dispose();
        detachContent();
        content = null;
        painter = null;
        childrenView = Collections.emptyList();
        super.dispose();
    }

    // ------------------------------------------------------------------ hooks for subclasses

    /**
     * Вызывается после каждого решения, перерисовывать ли текстуру.
     *
     * @param reason причина перерисовки или {@link RenderReason#NONE}, если взята прежняя текстура
     */
    protected void afterTextureUpdate(RenderReason reason) {
    }

    /**
     * Рисует поверх текстуры (например, отладочную плашку).
     */
    protected void renderOverlay(DrawScope draw, RenderReason reason) {
    }

    /**
     * Проверяет, нужен ли подклассу сырой проход, в том числе при пустом 2D-содержимом.
     *
     * @return true, если при обновлении текстуры нужно вызвать {@link #renderRawPass}.
     */
    protected boolean hasRawPass() {
        return false;
    }

    /**
     * Рисует в цель перед 2D-командами. При включённом сыром проходе он также очищает цель
     * цветом {@link #clearColor()}, а options должны отключать автоочистку перед 2D.
     *
     * @param backend         текущий бэкенд.
     * @param target          уже выделенная offscreen-цель.
     * @param logicalWidth    логическая ширина холста.
     * @param logicalHeight   логическая высота холста.
     * @param coordinateScale физических пикселей на единицу холста.
     */
    protected void renderRawPass(RenderBackend backend, RenderTarget target, float logicalWidth,
                                 float logicalHeight, float coordinateScale) {
    }

    /**
     * Переиспользуемый результат измерения логического холста и физической цели.
     */
    protected static final class TargetSize {
        /**
         * Ширина холста в единицах UI.
         */
        private float logicalWidth;
        /**
         * Высота холста в единицах UI.
         */
        private float logicalHeight;
        /**
         * Ширина цели в физических пикселях.
         */
        private int width;
        /**
         * Высота цели в физических пикселях.
         */
        private int height;
        /**
         * Число физических пикселей на единицу UI.
         */
        private float coordinateScale;

        /**
         * Создаёт буфер для расчётов {@link RenderToTexture#targetSize(float)}.
         */
        private TargetSize() {
        }

        /**
         * Читает ширину холста.
         *
         * @return ширина в единицах UI.
         */
        public float logicalWidth() {
            return logicalWidth;
        }

        /**
         * Читает высоту холста.
         *
         * @return высота в единицах UI.
         */
        public float logicalHeight() {
            return logicalHeight;
        }

        /**
         * Читает ширину цели.
         *
         * @return ширина в физических пикселях.
         */
        public int width() {
            return width;
        }

        /**
         * Читает высоту цели.
         *
         * @return высота в физических пикселях.
         */
        public int height() {
            return height;
        }

        /**
         * Читает масштаб рендера в цель.
         *
         * @return число физических пикселей на единицу UI.
         */
        public float coordinateScale() {
            return coordinateScale;
        }
    }

    /**
     * Считает размер цели теми же правилами, которыми пользуется offscreen-проход.
     *
     * @param pixelScale коэффициент плотности внешнего контекста.
     * @return внутренний буфер. Значения действительны до следующего измерения.
     */
    protected final TargetSize targetSize(float pixelScale) {
        RectView bounds = layoutBounds();
        float logicalWidth = resolvedCanvasWidth(bounds);
        float logicalHeight = resolvedCanvasHeight(bounds);

        float coordinateScale;
        int width;
        int height;
        if (fixedTextureWidth > 0 && fixedTextureHeight > 0) {
            width = fixedTextureWidth;
            height = fixedTextureHeight;
            coordinateScale = Math.min(width / logicalWidth, height / logicalHeight);
        } else {
            coordinateScale = safeScale(pixelScale) * resolutionScale;
            width = textureSide(logicalWidth * coordinateScale);
            height = textureSide(logicalHeight * coordinateScale);
            // Не больше MAX_TEXTURE_SIZE: масштаб уменьшается вместе с размером.
            float limit = Math.min(width / (logicalWidth * coordinateScale), height / (logicalHeight * coordinateScale));
            if (limit < 1.0f) coordinateScale *= limit;
        }

        measuredTarget.width = width;
        measuredTarget.height = height;
        measuredTarget.logicalWidth = logicalWidth;
        measuredTarget.logicalHeight = logicalHeight;
        measuredTarget.coordinateScale = coordinateScale;
        return measuredTarget;
    }

    // ------------------------------------------------------------------ internals

    private RenderReason updateTexture(RenderContext context, RenderBackend backend, boolean rawPass) {
        WidgetTextureRenderer renderer = rendererFor(backend);
        RectView bounds = layoutBounds();
        TargetSize size = targetSize(context.pixelScale());
        float logicalWidth = size.logicalWidth;
        float logicalHeight = size.logicalHeight;
        float coordinateScale = size.coordinateScale;
        int width = size.width;
        int height = size.height;
        RenderReason reason = renderReason(width, height, logicalWidth, logicalHeight, coordinateScale);
        if (reason == RenderReason.NONE) return reason;

        RenderTarget target = renderer.renderToTarget(width, height, targetOptions, coordinateScale,
                offscreen -> paintContent(offscreen, bounds, logicalWidth, logicalHeight, !rawPass),
                rawPass ? destination -> renderRawPass(backend, destination, logicalWidth, logicalHeight,
                        coordinateScale) : null);
        TextureHandle source = target.colorTexture();
        if (texture == null || texture.source() != source) texture = new SampledTexture(source, filter);
        textureWidth = width;
        textureHeight = height;
        renderedCanvasWidth = logicalWidth;
        renderedCanvasHeight = logicalHeight;
        renderedCoordinateScale = coordinateScale;
        renderRequested = false;
        targetOptionsChanged = false;
        backendChanged = false;
        renderCount++;
        lastRenderReason = reason;
        if (content != null) clearSubtreeInvalidation(content);
        clearInvalidation(InvalidationFlags.LAYOUT | InvalidationFlags.TEXTURE);
        return reason;
    }

    private void paintContent(RenderContext offscreen, RectView bounds, float width, float height, boolean clear) {
        if (clear && clearColor.a() > 0.0f) {
            offscreen.rect(0.0f, 0.0f, width, height, Paint.fill(clearColor));
        }
        if (painter != null) {
            painter.paint(offscreen, width, height);
        }
        if (content != null && content.visibility() != Visibility.COLLAPSED) {
            // Дочерний виджет разложен в экранных координатах (так до него доходит ввод). Внешний слой
            // трансформации сдвигает все его команды, включая clip, mesh и вложенные трансформации, в (0, 0).
            contentShift.position().set(-bounds.x(), -bounds.y());
            offscreen.pushTransform(bounds, contentShift);
            try {
                content.renderCached(offscreen);
            } finally {
                offscreen.popTransform();
            }
        }
    }

    private RenderReason renderReason(int width, int height, float logicalWidth, float logicalHeight,
                                      float coordinateScale) {
        if (backendChanged) return RenderReason.BACKEND_CHANGED;
        if (targetOptionsChanged) return RenderReason.TARGET_OPTIONS_CHANGED;
        if (texture == null) return RenderReason.NO_TEXTURE;
        if (renderRequested) return RenderReason.REQUESTED;
        if (textureWidth != width || textureHeight != height
                || renderedCanvasWidth != logicalWidth || renderedCanvasHeight != logicalHeight
                || renderedCoordinateScale != coordinateScale) {
            return RenderReason.RESIZED;
        }
        if (updateMode == UpdateMode.EVERY_FRAME) return RenderReason.EVERY_FRAME;
        if (updateMode == UpdateMode.MANUAL) return RenderReason.NONE;
        if (InvalidationFlags.has(invalidationFlags(), InvalidationFlags.LAYOUT)) return RenderReason.OWN_LAYOUT_DIRTY;
        if (InvalidationFlags.has(invalidationFlags(), InvalidationFlags.TEXTURE))
            return RenderReason.OWN_TEXTURE_DIRTY;
        if (content != null && content.subtreeInvalidationFlags() != InvalidationFlags.NONE) {
            return RenderReason.CHILD_SUBTREE_DIRTY;
        }
        return RenderReason.NONE;
    }

    private void drawTexture(DrawScope draw) {
        if (texture == null) return;
        RectView bounds = layoutBounds();
        ImageFit placementFit = fit == ImageFit.NINE_SLICE ? ImageFit.STRETCH : fit;
        if (placementFit == ImageFit.STRETCH) {
            draw.texture(texture, bounds.x(), bounds.y(), bounds.width(), bounds.height(), Paint.fill(tint));
            return;
        }
        TexturePlacement placement = TexturePlacement.fit(texture, bounds, placementFit);
        draw.texture(texture, placement, 0.0f, Paint.fill(tint));
    }

    private void renderDirect(RenderContext context) {
        RectView bounds = layoutBounds();
        if (painter != null) {
            contentShift.position().set(bounds.x(), bounds.y());
            context.pushTransform(bounds, contentShift);
            try {
                painter.paint(context, resolvedCanvasWidth(bounds), resolvedCanvasHeight(bounds));
            } finally {
                context.popTransform();
            }
        }
        if (content != null) renderChildWithInheritedTransform(context, content);
    }

    private float resolvedCanvasWidth(RectView bounds) {
        return hasCanvasSize() ? canvasWidth : Math.max(1.0f, Math.abs(bounds.width()));
    }

    private float resolvedCanvasHeight(RectView bounds) {
        return hasCanvasSize() ? canvasHeight : Math.max(1.0f, Math.abs(bounds.height()));
    }

    private WidgetTextureRenderer rendererFor(RenderBackend backend) {
        if (textureRenderer == null || rendererBackend != backend) {
            boolean hadRenderer = textureRenderer != null;
            resetRenderer();
            textureRenderer = new WidgetTextureRenderer(backend);
            rendererBackend = backend;
            backendChanged = hadRenderer;
        }
        return textureRenderer;
    }

    private void resetRenderer() {
        if (textureRenderer != null) {
            textureRenderer.close();
            textureRenderer = null;
        }
        rendererBackend = null;
        texture = null;
        textureWidth = 0;
        textureHeight = 0;
        backendChanged = false;
    }

    private void attachContent(Widget widget) {
        if (widget instanceof WidgetBase base) {
            base.setParentInternal(this);
            base.setUiContextInternal(uiContext());
        }
    }

    private void detachContent() {
        Widget previous = content;
        content = null;
        if (previous instanceof WidgetBase base) {
            base.setParentInternal(null);
            base.setUiContextInternal(null);
        }
        childrenView = Collections.emptyList();
    }

    private static void clearSubtreeInvalidation(Widget widget) {
        if (widget == null) return;
        for (Widget child : widget.children()) {
            clearSubtreeInvalidation(child);
        }
        widget.clearInvalidation(InvalidationFlags.ALL);
    }

    private static int textureSide(float pixels) {
        return Math.max(1, Math.min(MAX_TEXTURE_SIZE, (int) Math.ceil(pixels - 1.0e-3f)));
    }

    private static float safeScale(float scale) {
        return Float.isFinite(scale) && scale > 0.0f ? scale : 1.0f;
    }
}
