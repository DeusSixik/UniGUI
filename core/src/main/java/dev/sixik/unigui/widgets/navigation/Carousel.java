package dev.sixik.unigui.widgets.navigation;


import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiLink;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.event.Event;
import dev.sixik.unigui.api.event.EventListener;
import dev.sixik.unigui.api.event.EventPhase;
import dev.sixik.unigui.api.event.EventSubscription;
import dev.sixik.unigui.api.event.PointerPressedEvent;
import dev.sixik.unigui.api.event.SelectionChangedEvent;
import dev.sixik.unigui.api.input.PointerButton;
import dev.sixik.unigui.api.layout.LayoutConstraints;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.api.xml.XmlAttribute;
import dev.sixik.unigui.api.xml.XmlWidgetName;

import java.util.List;
import dev.sixik.unigui.widgets.containers.HBox;
import dev.sixik.unigui.widgets.containers.LinearBox;
import dev.sixik.unigui.widgets.core.Orientation;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.widgets.interaction.Button;
import dev.sixik.unigui.widgets.WidgetStyles;

/**
 * Карусель поверх {@link PageView}: страницы плюс навигация.
 *
 * <p>Навигация — шевроны штрихами по краям и ряд точек вместо текста
 * {@code 1 / 3}. Сам {@code PageView} хрома не рисует.</p>
 */
@AiDoc(summary = "Rotating carousel container displaying slides with previous/next navigation buttons and dot page indicators.")
@WikiPage(
        title = "Carousel",
        slug = "carousel",
        category = "<key:navigation.category>",
        summary = "<key:carousel.summary>"
)
@WikiSection(
        title = "<key:wiki.section.overview.title>",
        markdown = "<key:carousel.section.overview.body>",
        order = 10
)
@WikiExample(
        title = "<key:carousel.example.basic.title>",
        code = "Carousel carousel = new Carousel();\ncarousel.addPage(slide1).addPage(slide2);"
)
@WikiLink(
        label = "PageView",
        target = "page:page-view",
        description = "<key:page.view.summary>"
)
@XmlWidgetName("Carousel")
public class Carousel extends LinearBox {
    public static final String STYLE_TYPE = dev.sixik.unigui.api.style.StyleIds.Widget.CAROUSEL;

    private static final float NAV_HEIGHT = 28.0f;
    private static final float NAV_BUTTON = 28.0f;
    private static final float DOT = 6.0f;
    private static final float DOT_GAP = 6.0f;
    /** --text-lo #5f6772. */
    private static final MutableColor DOT_IDLE = MutableColor.rgba(0.373f, 0.404f, 0.447f, 1.0f);
    /** --accent #4a9eff. */
    private static final MutableColor DOT_ACTIVE = MutableColor.rgba(0.290f, 0.620f, 1.0f, 1.0f);

    private final HBox footer = new HBox();
    private final NavButton previous = new NavButton(false);
    private final Label indicator = new Label("0 / 0");
    private final DotIndicator dots = new DotIndicator();
    private final NavButton next = new NavButton(true);
    private final PageView pageView = new PageView();

        @AiDoc(summary = "Constructs an empty Carousel with default header controls and dot indicators.")
    @WikiMethod(summary = "<key:carousel.method.ctorDefault>")
    public Carousel() {
        super(Orientation.VERTICAL);
        spacing(6.0f);
        footer.spacing(8.0f);
        previous.layout(style -> style.size(NAV_BUTTON, NAV_HEIGHT).flexGrow(0).flexShrink(0.0f));
        next.layout(style -> style.size(NAV_BUTTON, NAV_HEIGHT).flexGrow(0).flexShrink(0.0f));
        indicator.visibility(dev.sixik.unigui.api.widget.Visibility.COLLAPSED);
        indicator.layout(style -> style.size(0.0f, 0.0f).flexGrow(0).flexShrink(0.0f));
        dots.layout(style -> style.size(LayoutConstraints.AUTO, NAV_HEIGHT).flexGrow(1).flexShrink(1.0f));
        dots.onDotSelected(this::selectedIndex);
        previous.onClick(event -> selectRelative(-1));
        next.onClick(event -> selectRelative(1));
        pageView.onSelectionChanged(event -> {
            updateIndicator();
            emit(new SelectionChangedEvent(this, event.oldSelection(), event.newSelection()));
        });
        footer.addChild(previous);
        footer.addChild(dots);
        footer.addChild(indicator);
        footer.addChild(next);
        pageView.layout(style -> style.flexGrow(1).flexShrink(1.0f));
        addChild(pageView);
        addChild(footer);
        updateIndicator();
    }

        @AiDoc(summary = "Appends a slide page to the carousel.")
    @WikiMethod(summary = "<key:carousel.method.addPage>")
    @WikiReturn("<key:carousel.return.this>")
    public Carousel addPage(
            @WikiParameter(description = "<key:carousel.param.page>")
            Widget page
    ) {
        pageView.addPage(page);
        updateIndicator();
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
        return this;
    }

        @AiDoc(summary = "Returns an unmodifiable list of all slide pages in the carousel.")
    @WikiMethod(summary = "<key:carousel.method.pages>")
    @WikiReturn("<key:carousel.return.pages>")
    public List<Widget> pages() {
        return pageView.pages();
    }

        @AiDoc(summary = "Returns the internal PageView managing slide presentation.")
    @WikiMethod(summary = "<key:carousel.method.pageView>")
    @WikiReturn("<key:carousel.return.pageView>")
    public PageView pageView() {
        return pageView;
    }

        @AiDoc(summary = "Returns the index of the currently active slide.")
    @WikiMethod(summary = "<key:carousel.method.selectedIndexGet>")
    @WikiReturn("<key:carousel.return.index>")
    public int selectedIndex() {
        return pageView.selectedIndex();
    }

        @AiDoc(summary = "Switches to the specified slide index and dispatches a selection event.")
    @WikiMethod(summary = "<key:carousel.method.selectedIndexSet>")
    @WikiReturn("<key:carousel.return.this>")
    public Carousel selectedIndex(
            @WikiParameter(description = "<key:carousel.param.index>")
            int selectedIndex
    ) {
        pageView.selectedIndex(selectedIndex);
        updateIndicator();
        return this;
    }

    @XmlAttribute(value = "selectedIndex", category = "Behavior", defaultValue = "0", description = "Initial selected carousel page without emitting change events during XML load.")
        @AiDoc(summary = "Switches to the specified slide index without triggering event callbacks.")
    @WikiMethod(summary = "<key:carousel.method.silentSelectedIndex>")
    @WikiReturn("<key:carousel.return.this>")
    public Carousel silentSelectedIndex(
            @WikiParameter(description = "<key:carousel.param.index>")
            int selectedIndex
    ) {
        pageView.silentSelectedIndex(selectedIndex);
        updateIndicator();
        return this;
    }

        @AiDoc(summary = "Steps the slide index by the specified relative delta.")
    @WikiMethod(summary = "<key:carousel.method.selectRelative>")
    @WikiReturn("<key:carousel.return.this>")
    public Carousel selectRelative(
            @WikiParameter(description = "<key:carousel.param.delta>")
            int delta
    ) {
        if (pageView.pageCount() == 0) return this;
        pageView.selectedIndex((selectedIndex() + delta + pageView.pageCount()) % pageView.pageCount());
        updateIndicator();
        return this;
    }

        @AiDoc(summary = "Returns the top header container hosting navigation buttons and indicators.")
    @WikiMethod(summary = "<key:carousel.method.header>")
    @WikiReturn("<key:carousel.return.header>")
    public HBox header() {
        return footer;
    }

        @AiDoc(summary = "Returns the navigation button stepping to the previous slide.")
    @WikiMethod(summary = "<key:carousel.method.previousButton>")
    @WikiReturn("<key:carousel.return.button>")
    public Button previousButton() {
        return previous;
    }

        @AiDoc(summary = "Returns the navigation button stepping to the next slide.")
    @WikiMethod(summary = "<key:carousel.method.nextButton>")
    @WikiReturn("<key:carousel.return.button>")
    public Button nextButton() {
        return next;
    }

    /**
     * Возвращает текстовый индикатор {@code 1 / 3}.
     *
     * <p>Метка скрыта: на экране страницы показывает {@link #dots()}. Текст
     * остаётся для контрактов и XML.</p>
     *
     * @return скрытая метка индикатора
     */
        @AiDoc(summary = "Returns the numeric page indicator label.")
    @WikiMethod(summary = "<key:carousel.method.indicator>")
    @WikiReturn("<key:carousel.return.indicator>")
    public Label indicator() {
        return indicator;
    }

    /**
     * Возвращает видимый ряд точек.
     *
     * @return индикатор точками
     */
        @AiDoc(summary = "Returns the DotIndicator widget displaying carousel page dots.")
    @WikiMethod(summary = "<key:carousel.method.dots>")
    @WikiReturn("<key:carousel.return.dots>")
    public DotIndicator dots() {
        return dots;
    }

    private void updateIndicator() {
        indicator.text(pageView.pageCount() == 0 ? "0 / 0" : (selectedIndex() + 1) + " / " + pageView.pageCount());
        dots.sync(pageView.pageCount(), selectedIndex());
        invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
    }

        @AiDoc(summary = "Registers a listener invoked when the active carousel page changes.")
    @WikiMethod(summary = "<key:carousel.method.onSelectionChanged>")
    @WikiReturn("<key:carousel.return.subscription>")
    public EventSubscription onSelectionChanged(
            @WikiParameter(description = "<key:carousel.param.listener>")
            EventListener<? super SelectionChangedEvent> listener
    ) {
        return on(SelectionChangedEvent.TYPE, listener);
    }

    /**
     * Обычная кнопка, у которой вместо текста `<` / `>` рисуется шеврон.
     *
     * <p>Тема, фон и рамка остаются кнопки. Глиф — два stroke-отрезка,
     * как disclosure в {@code TreeView}: центр {@code (cx, cy)},
     * полуширина {@code 2}, полувысота {@code 4}. Вперёд:
     * <pre>
     *   (cx - 2, cy - 4) → (cx + 2, cy) → (cx - 2, cy + 4)
     * </pre>
     * Назад — зеркально.</p>
     */
    private static final class NavButton extends Button {
        private final boolean forward;

        private NavButton(boolean forward) {
            super("");
            this.forward = forward;
        }

        @Override
        protected void renderContent(RenderContext context) {
            DrawScope draw = new DrawScope(context, transform(), layoutBounds());
            float centerX = layoutBounds().x() + layoutBounds().width() * 0.5f;
            float centerY = layoutBounds().y() + layoutBounds().height() * 0.5f;
            Paint paint = Paint.stroke(DOT_IDLE, 1.5f);
            if (forward) {
                draw.line(centerX - 2.0f, centerY - 4.0f, centerX + 2.0f, centerY, paint);
                draw.line(centerX + 2.0f, centerY, centerX - 2.0f, centerY + 4.0f, paint);
            } else {
                draw.line(centerX + 2.0f, centerY - 4.0f, centerX - 2.0f, centerY, paint);
                draw.line(centerX - 2.0f, centerY, centerX + 2.0f, centerY + 4.0f, paint);
            }
            super.renderContent(context);
        }
    }

    /**
     * Ряд точек: активная страница — акцент, остальные — приглушённые.
     */
    public static final class DotIndicator extends WidgetBaseIndicator {
        private int count;
        private int active;
        private java.util.function.IntConsumer dotSelected;

        private void onDotSelected(java.util.function.IntConsumer dotSelected) {
            this.dotSelected = dotSelected;
        }

        @Override
        public void handle(Event event) {
            super.handle(event);
            if (!(event instanceof PointerPressedEvent pointer)
                    || pointer.phase() == EventPhase.CAPTURE
                    || pointer.button() != PointerButton.PRIMARY
                    || count <= 0) {
                return;
            }
            float width = count * DOT + (count - 1) * DOT_GAP;
            float start = (layoutBounds().width() - width) * 0.5f;
            int index = (int) ((pointer.localX() - start) / (DOT + DOT_GAP));
            if (index >= 0 && index < count && dotSelected != null) {
                dotSelected.accept(index);
                event.cancel();
            }
        }

        private void sync(int count, int active) {
            this.count = Math.max(0, count);
            this.active = count == 0 ? 0 : Math.max(0, Math.min(active, count - 1));
            invalidate(InvalidationFlags.VISUAL);
        }

        @Override
        protected void renderContent(RenderContext context) {
            if (count <= 0) return;
            DrawScope draw = new DrawScope(context, transform(), layoutBounds());
            float width = Math.max(0.0f, count * DOT + (count - 1) * DOT_GAP);
            float x = layoutBounds().x() + (layoutBounds().width() - width) * 0.5f;
            float y = layoutBounds().y() + (layoutBounds().height() - DOT) * 0.5f;
            for (int i = 0; i < count; i++) {
                draw.circleFilled(x + i * (DOT + DOT_GAP), y, DOT, DOT, i == active ? DOT_ACTIVE : DOT_IDLE);
            }
        }
    }

    /**
     * База индикатора: {@link dev.sixik.unigui.widgets.containers.SurfaceWidget}
     * без собственной поверхности, чтобы точки не получали фон кнопки.
     */
    private abstract static class WidgetBaseIndicator extends dev.sixik.unigui.widgets.containers.SurfaceWidget<WidgetBaseIndicator> {
        private WidgetBaseIndicator() {
            themeEnabled(false);
            boxVisualEnabled(false);
            applyComponentStyleDefaults(WidgetStyles.Surface.UNSURFACED);
        }
    }
}
