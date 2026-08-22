package dev.sixik.unigui.widgets.containers;

import dev.sixik.unigui.api.core.FrameContext;
import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.core.UIContext;
import dev.sixik.unigui.api.event.Event;
import dev.sixik.unigui.api.event.EventPhase;
import dev.sixik.unigui.api.event.ScrollEvent;
import dev.sixik.unigui.api.input.KeyModifiers;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.layout.LayoutSize;
import dev.sixik.unigui.api.layout.Overflow;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.api.xml.XmlAttribute;
import dev.sixik.unigui.api.xml.XmlWidgetName;
import dev.sixik.unigui.impl.layout.v3.LayoutV3ScrollAdapter;
import dev.sixik.unigui.impl.widget.WidgetBase;
import dev.sixik.unigui.widgets.core.Orientation;
import dev.sixik.unigui.widgets.interaction.ScrollBar;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Контейнер viewport'а для одного большого content-виджета.
 *
 * <p>{@code ScrollView} измеряет content, вычисляет видимую область и при
 * необходимости показывает горизонтальный/вертикальный {@link ScrollBar}. Оси
 * скролла включаются через {@code layoutStyle().overflowX/Y(...)}: обычно
 * {@link Overflow#AUTO} показывает scrollbar только при переполнении, а
 * {@link Overflow#SCROLL} резервирует его всегда.</p>
 *
 * <p>Если явный {@link #contentSize(float, float)} не задан, scroll extent
 * берётся из measured-size content'а. Позиция скролла всегда зажимается в
 * диапазон {@code 0..maxScrollX/Y}. Колесо мыши скроллит обе оси, а
 * {@code Shift + vertical wheel} переключает вертикальный wheel в горизонтальный
 * скролл, если горизонтальная ось доступна.</p>
 *
 * <pre>{@code
 * ScrollView listViewport = new ScrollView(list)
 *         .scrollStep(18.0f)
 *         .scrollbarGap(2.0f);
 * listViewport.layout(style -> style
 *         .overflowX(Overflow.HIDDEN)
 *         .overflowY(Overflow.AUTO));
 * }</pre>
 *
 * @see ScrollBar
 * @see Overflow
 */
@XmlWidgetName("ScrollView")
public class ScrollView extends WidgetBase {
    public static final String STYLE_TYPE = dev.sixik.unigui.api.style.StyleIds.Widget.SCROLL_VIEW;

    private static final float SCROLLBAR_SIZE = ScrollBar.DEFAULT_SIZE;

    private final MutableColor scrollbarTrackColor = new MutableColor(0.0f, 0.0f, 0.0f, 0.28f);
    private final MutableColor scrollbarThumbColor = new MutableColor(0.25f, 0.78f, 1.0f, 0.75f);
    private final ScrollBar horizontalScrollBar = new ScrollBar().orientation(Orientation.HORIZONTAL);
    private final ScrollBar verticalScrollBar = new ScrollBar().orientation(Orientation.VERTICAL);
    private Widget content;
    private float contentWidth;
    private float contentHeight;
    private float measuredContentWidth;
    private float measuredContentHeight;
    private float scrollX;
    private float scrollY;
    private float scrollStep = 16.0f;
    private float scrollbarGap = ScrollBar.DEFAULT_GAP;
    private boolean scrollingEnabled = true;
    private boolean consumeWheelAtScrollBounds = true;
    private boolean horizontalScrollBarVisible;
    private boolean verticalScrollBarVisible;
    private List<Widget> childrenView = Collections.emptyList();
    private boolean childrenViewDirty = true;
    private boolean childrenViewHasHorizontalScrollBar;
    private boolean childrenViewHasVerticalScrollBar;

    /**
     * Создаёт пустой scroll viewport с {@code overflowX=HIDDEN} и {@code overflowY=AUTO}.
     */
    public ScrollView() {
        layout(style -> style
                .overflowX(Overflow.HIDDEN)
                .overflowY(Overflow.AUTO));
        scrollbarTrackColor.onChanged(() -> {
            horizontalScrollBar.trackColor().set(scrollbarTrackColor);
            verticalScrollBar.trackColor().set(scrollbarTrackColor);
            invalidate(InvalidationFlags.VISUAL);
        });
        scrollbarThumbColor.onChanged(() -> {
            horizontalScrollBar.thumbColor().set(scrollbarThumbColor);
            verticalScrollBar.thumbColor().set(scrollbarThumbColor);
            invalidate(InvalidationFlags.VISUAL);
        });
        horizontalScrollBar.setParentInternal(this);
        verticalScrollBar.setParentInternal(this);
        horizontalScrollBar.trackColor().set(scrollbarTrackColor);
        horizontalScrollBar.thumbColor().set(scrollbarThumbColor);
        verticalScrollBar.trackColor().set(scrollbarTrackColor);
        verticalScrollBar.thumbColor().set(scrollbarThumbColor);
        horizontalScrollBar.onValueChanged(event -> scrollTo(event.newValue(), scrollY));
        verticalScrollBar.onValueChanged(event -> scrollTo(scrollX, event.newValue()));
    }

    /**
     * Создаёт scroll viewport с начальным content-виджетом.
     *
     * @param content content, который будет расположен внутри viewport'а
     */
    public ScrollView(Widget content) {
        this();
        content(content);
    }

    /**
     * Возвращает текущий content-виджет.
     *
     * @return content или {@code null}
     */
    public Widget content() {
        return content;
    }

    /**
     * Заменяет content-виджет внутри scroll viewport'а.
     *
     * <p>У старого content'а сбрасываются parent/UI context, у нового —
     * назначаются текущие parent/UI context. Сам {@code ScrollView} не может
     * быть собственным content'ом.</p>
     *
     * @param content новый content или {@code null}
     * @return этот scroll view для fluent-настройки
     * @throws IllegalArgumentException если передан сам {@code ScrollView}
     */
    public ScrollView content(Widget content) {
        if (this.content == content) return this;
        if (content == this) throw new IllegalArgumentException("ScrollView cannot contain itself");
        detachContent();
        this.content = content;
        attachContent(content);
        markChildrenViewDirty();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    /**
     * Возвращает явно заданную ширину content extent'а.
     *
     * @return ширина content'а; {@code 0} означает использовать measured width
     */
    public float contentWidth() {
        return contentWidth;
    }

    @XmlAttribute(value = "contentWidth", category = "Layout", defaultValue = "0", description = "Explicit scroll content width; 0 uses measured width.")
    public ScrollView contentWidth(float contentWidth) {
        return contentSize(contentWidth, contentHeight);
    }

    /**
     * Возвращает явно заданную высоту content extent'а.
     *
     * @return высота content'а; {@code 0} означает использовать measured height
     */
    public float contentHeight() {
        return contentHeight;
    }

    @XmlAttribute(value = "contentHeight", category = "Layout", defaultValue = "0", description = "Explicit scroll content height; 0 uses measured height.")
    public ScrollView contentHeight(float contentHeight) {
        return contentSize(contentWidth, contentHeight);
    }

    /**
     * Задаёт явный scroll extent content'а.
     *
     * <p>Значения зажимаются к {@code >= 0}. Если размер равен {@code 0}, для
     * соответствующей оси используется measured-size content'а.</p>
     *
     * @param width ширина scroll content'а
     * @param height высота scroll content'а
     * @return этот scroll view для fluent-настройки
     */
    public ScrollView contentSize(float width, float height) {
        float normalizedWidth = Math.max(0.0f, width);
        float normalizedHeight = Math.max(0.0f, height);
        if (contentWidth == normalizedWidth && contentHeight == normalizedHeight) return this;
        contentWidth = normalizedWidth;
        contentHeight = normalizedHeight;
        scrollTo(scrollX, scrollY);
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    /**
     * Возвращает горизонтальную позицию скролла.
     *
     * @return x-offset content'а относительно viewport'а
     */
    public float scrollX() {
        return scrollX;
    }

    /**
     * Возвращает вертикальную позицию скролла.
     *
     * @return y-offset content'а относительно viewport'а
     */
    public float scrollY() {
        return scrollY;
    }

    /**
     * Возвращает множитель wheel/page step'а.
     *
     * @return количество UI-пикселей на единицу scroll delta
     */
    public float scrollStep() {
        return scrollStep;
    }

    /**
     * Задаёт множитель wheel/page step'а.
     *
     * @param scrollStep количество UI-пикселей на единицу scroll delta; минимум {@code 1}
     * @return этот scroll view для fluent-настройки
     */
    @XmlAttribute(value = "scrollStep", category = "Behavior", defaultValue = "16", description = "UI pixels scrolled per wheel/page delta unit.")
    public ScrollView scrollStep(float scrollStep) {
        this.scrollStep = Math.max(1.0f, scrollStep);
        return this;
    }

    /**
     * Возвращает, включён ли пользовательский scrolling.
     *
     * @return {@code true}, если wheel и scrollbar'ы могут менять scroll position
     */
    public boolean scrollingEnabled() {
        return scrollingEnabled;
    }

    /**
     * Включает или выключает пользовательский scrolling.
     *
     * <p>При выключении позиция скролла сбрасывается в {@code 0,0}, а scrollbar'ы
     * скрываются независимо от overflow-настроек.</p>
     *
     * @param scrollingEnabled {@code true}, чтобы разрешить scrolling
     * @return этот scroll view для fluent-настройки
     */
    @XmlAttribute(value = "scrollingEnabled", category = "Behavior", defaultValue = "true", description = "Whether wheel input and scrollbars can change scroll position.")
    public ScrollView scrollingEnabled(boolean scrollingEnabled) {
        if (this.scrollingEnabled == scrollingEnabled) return this;
        this.scrollingEnabled = scrollingEnabled;
        if (!scrollingEnabled) {
            horizontalScrollBarVisible = false;
            verticalScrollBarVisible = false;
            markChildrenViewDirty();
            scrollX = 0.0f;
            scrollY = 0.0f;
            syncScrollBars();
        }
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    /**
     * Выключает пользовательский scrolling.
     *
     * @return этот scroll view для fluent-настройки
     */
    public ScrollView disableScrolling() {
        return scrollingEnabled(false);
    }

    /**
     * Включает пользовательский scrolling.
     *
     * @return этот scroll view для fluent-настройки
     */
    public ScrollView enableScrolling() {
        return scrollingEnabled(true);
    }

    /**
     * Возвращает, будет ли wheel-событие потребляться на границах scroll range.
     *
     * @return {@code true}, если scroll view блокирует propagation wheel'а на своих границах
     */
    public boolean consumeWheelAtScrollBounds() {
        return consumeWheelAtScrollBounds;
    }

    /**
     * Управляет потреблением wheel-событий на границах scroll range.
     *
     * <p>Если включено, scroll view может отменить wheel-событие даже когда
     * позиция уже упёрлась в начало/конец, чтобы родительские scroll areas не
     * начали прокручиваться от того же события.</p>
     *
     * @param consumeWheelAtScrollBounds {@code true}, чтобы потреблять wheel на границах
     * @return этот scroll view для fluent-настройки
     */
    @XmlAttribute(value = "consumeWheelAtScrollBounds", category = "Behavior", defaultValue = "true", description = "Whether wheel events are consumed at scroll range boundaries.")
    public ScrollView consumeWheelAtScrollBounds(boolean consumeWheelAtScrollBounds) {
        this.consumeWheelAtScrollBounds = consumeWheelAtScrollBounds;
        return this;
    }

    /**
     * Возвращает расстояние между content viewport'ом и scrollbar'ами.
     *
     * @return gap в пикселях UI-пространства
     */
    public float scrollbarGap() {
        return scrollbarGap;
    }

    /**
     * Задаёт расстояние между content viewport'ом и scrollbar'ами.
     *
     * @param scrollbarGap gap в пикселях UI-пространства; невалидные значения заменяются дефолтом
     * @return этот scroll view для fluent-настройки
     */
    @XmlAttribute(value = "scrollbarGap", category = "Layout", defaultValue = "2", description = "Gap between viewport content and scrollbar tracks.")
    public ScrollView scrollbarGap(float scrollbarGap) {
        float normalized = Float.isFinite(scrollbarGap) ? Math.max(0.0f, scrollbarGap) : ScrollBar.DEFAULT_GAP;
        if (this.scrollbarGap == normalized) return this;
        this.scrollbarGap = normalized;
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    /**
     * Прокручивает content к абсолютной позиции.
     *
     * <p>Координаты автоматически зажимаются в диапазон
     * {@code 0..maxScrollX/Y}, а scrollbar'ы синхронизируются без обратного
     * value callback'а.</p>
     *
     * @param x желаемый горизонтальный offset
     * @param y желаемый вертикальный offset
     * @return этот scroll view для fluent-настройки
     */
    public ScrollView scrollTo(float x, float y) {
        float clampedX = clamp(x, 0.0f, maxScrollX());
        float clampedY = clamp(y, 0.0f, maxScrollY());
        if (scrollX == clampedX && scrollY == clampedY) return this;
        scrollX = clampedX;
        scrollY = clampedY;
        syncScrollBars();
        arrangeScrolledChildrenNow();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

    /**
     * Прокручивает content относительно текущей позиции.
     *
     * @param dx изменение горизонтального offset'а
     * @param dy изменение вертикального offset'а
     * @return этот scroll view для fluent-настройки
     */
    public ScrollView scrollBy(float dx, float dy) {
        return scrollTo(scrollX + dx, scrollY + dy);
    }

    /**
     * Возвращает максимальный горизонтальный scroll offset.
     *
     * @return {@code 0}, если горизонтальный scrolling выключен или content помещается
     */
    public float maxScrollX() {
        if (!scrollingEnabled || !horizontalScrollingEnabled()) return 0.0f;
        return Math.max(0.0f, effectiveContentWidth() - viewportWidth());
    }

    /**
     * Возвращает максимальный вертикальный scroll offset.
     *
     * @return {@code 0}, если вертикальный scrolling выключен или content помещается
     */
    public float maxScrollY() {
        if (!scrollingEnabled || !verticalScrollingEnabled()) return 0.0f;
        return Math.max(0.0f, effectiveContentHeight() - viewportHeight());
    }

    /**
     * Возвращает live-цвет дорожки scrollbar'ов.
     *
     * @return изменяемый цвет track'а для обеих осей
     */
    public MutableColor scrollbarTrackColor() {
        return scrollbarTrackColor;
    }

    /**
     * Возвращает live-цвет thumb scrollbar'ов.
     *
     * @return изменяемый цвет thumb'а для обеих осей
     */
    public MutableColor scrollbarThumbColor() {
        return scrollbarThumbColor;
    }

    /**
     * Возвращает вертикальный scrollbar.
     *
     * <p>Scrollbar принадлежит этому {@code ScrollView}; его можно стилизовать,
     * но не нужно добавлять в другой контейнер вручную.</p>
     *
     * @return внутренний вертикальный scrollbar
     */
    public ScrollBar verticalScrollBar() {
        return verticalScrollBar;
    }

    /**
     * Возвращает горизонтальный scrollbar.
     *
     * <p>Scrollbar принадлежит этому {@code ScrollView}; его можно стилизовать,
     * но не нужно добавлять в другой контейнер вручную.</p>
     *
     * @return внутренний горизонтальный scrollbar
     */
    public ScrollBar horizontalScrollBar() {
        return horizontalScrollBar;
    }

    @Override
    public void setUiContextInternal(UIContext uiContext) {
        super.setUiContextInternal(uiContext);
        attachContent(content);
        horizontalScrollBar.setParentInternal(this);
        horizontalScrollBar.setUiContextInternal(uiContext);
        verticalScrollBar.setParentInternal(this);
        verticalScrollBar.setUiContextInternal(uiContext);
    }

    /**
     * Возвращает content и видимые scrollbar'ы как дочерние виджеты.
     *
     * <p>Список пересобирается лениво, когда меняется видимость scrollbar'ов
     * или content. Это сохраняет обычную event/navigation модель для внутренних
     * scrollbar'ов.</p>
     *
     * @return неизменяемый список текущих детей scroll view
     */
    @Override
    public List<Widget> children() {
        boolean horizontalVisible = showsHorizontalScrollBar();
        boolean verticalVisible = showsVerticalScrollBar();
        if (childrenViewDirty
                || childrenViewHasHorizontalScrollBar != horizontalVisible
                || childrenViewHasVerticalScrollBar != verticalVisible) {
            if (content == null && !horizontalVisible && !verticalVisible) {
                childrenView = Collections.emptyList();
            } else {
                List<Widget> children = new ObjectArrayList<>(3);
                if (content != null) {
                    children.add(content);
                }
                if (horizontalVisible) {
                    children.add(horizontalScrollBar);
                }
                if (verticalVisible) {
                    children.add(verticalScrollBar);
                }
                childrenView = Collections.unmodifiableList(children);
            }
            childrenViewHasHorizontalScrollBar = horizontalVisible;
            childrenViewHasVerticalScrollBar = verticalVisible;
            childrenViewDirty = false;
        }
        return childrenView;
    }

    @Override
    public void measure(LayoutContext context) {
        if (visibility() == dev.sixik.unigui.api.widget.Visibility.COLLAPSED) {
            setDesiredSize(LayoutSize.ZERO);
            return;
        }
        if (content != null) {
            LayoutV3ScrollAdapter.Extent extent = LayoutV3ScrollAdapter.measureContent(
                    content,
                    context,
                    horizontalScrollingEnabled(),
                    verticalScrollingEnabled());
            measuredContentWidth = extent.contentWidth();
            measuredContentHeight = extent.contentHeight();
            float desiredWidth = contentWidth > 0.0f ? contentWidth : measuredContentWidth;
            float desiredHeight = contentHeight > 0.0f ? contentHeight : measuredContentHeight;
            setDesiredSize(resolveDesiredSize(context, desiredWidth, desiredHeight));
        } else {
            measuredContentWidth = 0.0f;
            measuredContentHeight = 0.0f;
            setDesiredSize(resolveDesiredSize(context, 0.0f, 0.0f));
        }
    }

    @Override
    public void arrange(RectView bounds) {
        super.arrange(bounds);
        updateScrollBarVisibility();
        scrollTo(scrollX, scrollY);
        arrangeContent();
        arrangeScrollBars();
    }

    @Override
    public void tick(FrameContext frame) {
        super.tick(frame);
        if (content != null) {
            content.tick(frame);
        }
        if (showsHorizontalScrollBar()) {
            horizontalScrollBar.tick(frame);
        }
        if (showsVerticalScrollBar()) {
            verticalScrollBar.tick(frame);
        }
    }

    @Override
    public void render(RenderContext context) {
        pushOpacity(context);
        try {
            if (content != null) {
                boolean clipsContent = clipsContent();
                MutableRect viewportBounds = new MutableRect(
                        layoutBounds().x(),
                        layoutBounds().y(),
                        viewportWidth(),
                        viewportHeight());
                if (clipsContent) {
                    context.pushClip(viewportBounds.x(), viewportBounds.y(), viewportBounds.width(), viewportBounds.height());
                }
                RectView previousCullBounds = PanelWidget.pushRenderCullBounds(viewportBounds);
                try {
                    context.pushTextPixelSnap(false);
                    renderChildWithInheritedTransform(context, content);
                } finally {
                    context.popTextPixelSnap();
                    PanelWidget.restoreRenderCullBounds(previousCullBounds);
                    if (clipsContent) {
                        context.popClip();
                    }
                }
            }
            if (showsHorizontalScrollBar()) {
                renderChildWithInheritedTransform(context, horizontalScrollBar);
            }
            if (showsVerticalScrollBar()) {
                renderChildWithInheritedTransform(context, verticalScrollBar);
            }
        } finally {
            popOpacity(context);
        }
    }

    @Override
    public void handle(Event event) {
        super.handle(event);
        if (event.isCancelled()) return;
        if (scrollingEnabled && event instanceof ScrollEvent scroll && scroll.phase() != EventPhase.CAPTURE) {
            float beforeX = scrollX;
            float beforeY = scrollY;
            boolean shiftHorizontal = KeyModifiers.has(scroll.modifiers(), KeyModifiers.SHIFT)
                    && maxScrollX() > 0.0f
                    && scroll.deltaY() != 0.0f;
            if (shiftHorizontal) {
                scrollBy(-scroll.deltaY() * scrollStep, 0.0f);
            } else {
                scrollBy(-scroll.deltaX() * scrollStep, -scroll.deltaY() * scrollStep);
            }
            if (beforeX != scrollX || beforeY != scrollY) {
                syncScrollBars();
                event.cancel();
            } else if (consumeWheelAtScrollBounds && canScrollWheel(scroll)) {
                event.cancel();
            }
        }
    }

    @Override
    public void dispose() {
        if (content != null) {
            content.dispose();
        }
        detachContent();
        content = null;
        horizontalScrollBar.setParentInternal(null);
        horizontalScrollBar.setUiContextInternal(null);
        verticalScrollBar.setParentInternal(null);
        verticalScrollBar.setUiContextInternal(null);
    }

    private void arrangeScrolledChildrenNow() {
        if (layoutBounds().width() <= 0.0f && layoutBounds().height() <= 0.0f) return;
        arrangeContent();
        arrangeScrollBars();
    }

    private void arrangeContent() {
        if (content == null) return;
        StackPanel.arrangeChild(content,
                layoutBounds().x() - scrollX,
                layoutBounds().y() - scrollY,
                effectiveContentWidth(),
                effectiveContentHeight());
    }

    private void arrangeScrollBars() {
        if (showsHorizontalScrollBar()) {
            float horizontalInset = showsVerticalScrollBar() ? scrollbarGap * 0.5f : 0.0f;
            horizontalScrollBar.arrange(new MutableRect(
                    layoutBounds().x() + horizontalInset,
                    layoutBounds().y() + viewportHeight() + scrollbarGap,
                    viewportWidth(),
                    SCROLLBAR_SIZE));
        }
        if (showsVerticalScrollBar()) {
            verticalScrollBar.arrange(new MutableRect(
                    layoutBounds().x() + viewportWidth() + scrollbarGap,
                    layoutBounds().y(),
                    SCROLLBAR_SIZE,
                    viewportHeight()));
        }
        syncScrollBars();
    }

    private void syncScrollBars() {
        horizontalScrollBar
                .range(0.0f, maxScrollX())
                .pageSize(Math.max(1.0f, viewportWidth()))
                .step(scrollStep)
                .silentValue(scrollX);
        verticalScrollBar
                .range(0.0f, maxScrollY())
                .pageSize(Math.max(1.0f, viewportHeight()))
                .step(scrollStep)
                .silentValue(scrollY);
    }

    private void updateScrollBarVisibility() {
        if (!scrollingEnabled) {
            if (horizontalScrollBarVisible || verticalScrollBarVisible) {
                markChildrenViewDirty();
            }
            horizontalScrollBarVisible = false;
            verticalScrollBarVisible = false;
            return;
        }
        boolean horizontal = layoutStyle().overflowX() == Overflow.SCROLL;
        boolean vertical = layoutStyle().overflowY() == Overflow.SCROLL;
        float width = Math.max(0.0f, layoutBounds().width());
        float height = Math.max(0.0f, layoutBounds().height());

        for (int pass = 0; pass < 4; pass++) {
            float candidateWidth = Math.max(0.0f, width - (vertical ? scrollbarReservation() : 0.0f));
            float candidateHeight = Math.max(0.0f, height - (horizontal ? scrollbarReservation() : 0.0f));
            boolean nextHorizontal = layoutStyle().overflowX() == Overflow.SCROLL
                    || (layoutStyle().overflowX() == Overflow.AUTO
                    && rawContentWidth() > candidateWidth);
            boolean nextVertical = layoutStyle().overflowY() == Overflow.SCROLL
                    || (layoutStyle().overflowY() == Overflow.AUTO
                    && rawContentHeight() > candidateHeight);
            if (horizontal == nextHorizontal && vertical == nextVertical) break;
            horizontal = nextHorizontal;
            vertical = nextVertical;
        }

        if (horizontalScrollBarVisible != horizontal || verticalScrollBarVisible != vertical) {
            markChildrenViewDirty();
        }
        horizontalScrollBarVisible = horizontal;
        verticalScrollBarVisible = vertical;
    }

    private float viewportWidth() {
        return Math.max(0.0f, layoutBounds().width()
                - (showsVerticalScrollBar() ? scrollbarReservation() : 0.0f));
    }

    private float viewportHeight() {
        return Math.max(0.0f, layoutBounds().height()
                - (showsHorizontalScrollBar() ? scrollbarReservation() : 0.0f));
    }

    private float scrollbarReservation() {
        return SCROLLBAR_SIZE + scrollbarGap;
    }

    private float effectiveContentWidth() {
        if (!horizontalScrollingEnabled()) {
            return viewportWidth();
        }
        if (contentWidth > 0.0f) {
            return contentWidth;
        }
        return Math.max(measuredContentWidth, viewportWidth());
    }

    private float effectiveContentHeight() {
        if (!verticalScrollingEnabled()) {
            return viewportHeight();
        }
        if (contentHeight > 0.0f) {
            return contentHeight;
        }
        return Math.max(measuredContentHeight, viewportHeight());
    }

    private float rawContentWidth() {
        return contentWidth > 0.0f ? contentWidth : measuredContentWidth;
    }

    private float rawContentHeight() {
        return contentHeight > 0.0f ? contentHeight : measuredContentHeight;
    }

    private boolean horizontalScrollingEnabled() {
        if (!scrollingEnabled) return false;
        Overflow overflow = layoutStyle().overflowX();
        return overflow == Overflow.AUTO || overflow == Overflow.SCROLL;
    }

    private boolean verticalScrollingEnabled() {
        if (!scrollingEnabled) return false;
        Overflow overflow = layoutStyle().overflowY();
        return overflow == Overflow.AUTO || overflow == Overflow.SCROLL;
    }

    private boolean canScrollWheel(ScrollEvent scroll) {
        if (scroll.deltaX() != 0.0f && maxScrollX() > 0.0f) return true;
        if (scroll.deltaY() != 0.0f && maxScrollY() > 0.0f) return true;
        return KeyModifiers.has(scroll.modifiers(), KeyModifiers.SHIFT)
                && scroll.deltaY() != 0.0f
                && maxScrollX() > 0.0f;
    }

    private boolean showsHorizontalScrollBar() {
        return layoutStyle().overflowX() == Overflow.SCROLL
                || (layoutStyle().overflowX() == Overflow.AUTO && horizontalScrollBarVisible);
    }

    private boolean showsVerticalScrollBar() {
        return layoutStyle().overflowY() == Overflow.SCROLL
                || (layoutStyle().overflowY() == Overflow.AUTO && verticalScrollBarVisible);
    }

    private boolean clipsContent() {
        return layoutStyle().overflowX() != Overflow.VISIBLE
                || layoutStyle().overflowY() != Overflow.VISIBLE;
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
        markChildrenViewDirty();
    }

    private void markChildrenViewDirty() {
        childrenViewDirty = true;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
