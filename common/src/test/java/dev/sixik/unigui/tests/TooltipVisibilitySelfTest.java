package dev.sixik.unigui.tests;

import dev.sixik.unigui.api.core.FrameContext;
import dev.sixik.unigui.api.core.FramePhase;
import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.event.EventPhase;
import dev.sixik.unigui.api.event.PointerEnteredEvent;
import dev.sixik.unigui.api.event.PointerExitedEvent;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.impl.widget.WidgetBase;
import dev.sixik.unigui.widgets.containers.Box;
import dev.sixik.unigui.widgets.containers.ScrollView;
import dev.sixik.unigui.widgets.containers.VBox;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.widgets.feedback.OverlayLayer;
import dev.sixik.unigui.widgets.feedback.Tooltip;

/**
 * Тултип не должен зависать над скрытым якорем: при сворачивании якоря
 * PointerExited не прилетает и флаг hovered остаётся, поэтому showing()
 * дополнительно требует видимости якоря и всех его предков.
 */
public final class TooltipVisibilitySelfTest {
    public static void main(String[] args) {
        Box anchor = new Box();
        Tooltip tooltip = new Tooltip(anchor, "hint");
        hover(anchor, true);
        check(tooltip.showing(), "hovered visible anchor shows tooltip");

        // Кейс из поиска: фильтр свернул клетку под курсором — тултип гаснет,
        // хотя флаг hovered остался (exit не было).
        anchor.visibility(Visibility.COLLAPSED);
        check(anchor.hovered(), "hover flag sticks without exit event");
        check(!tooltip.showing(), "collapsed anchor hides tooltip");

        // Фильтр снят: якорь снова виден под курсором — тултип возвращается сам.
        anchor.visibility(Visibility.VISIBLE);
        check(tooltip.showing(), "tooltip returns when anchor visible again");

        // Скрыт предок (панель целиком) — тултип тоже гаснет.
        Box panel = new Box();
        panel.addChild(anchor);
        panel.applyQueuedMutations();
        panel.visibility(Visibility.COLLAPSED);
        check(!tooltip.showing(), "collapsed ancestor hides tooltip");
        panel.visibility(Visibility.VISIBLE);
        check(tooltip.showing(), "tooltip returns with ancestor");

        // Обычное уведение мыши по-прежнему прячет.
        hover(anchor, false);
        check(!tooltip.showing(), "pointer exit hides tooltip");

        // Скролл двигает якорь через arrange без layout: тултип обязан
        // последовать за ним уже в tick, иначе текст нового hovered-якоря
        // рисуется на старой позиции (баг ISF-каталога).
        followsAnchorWithoutLayout();

        System.out.println("TooltipVisibilitySelfTest passed");
    }

    private static void followsAnchorWithoutLayout() {
        VBox content = new VBox();
        for (int i = 0; i < 20; i++) {
            Label row = new Label("row " + i);
            row.layout(style -> style.size(120.0f, 18.0f).flexGrow(0).flexShrink(0.0f));
            content.addChild(row);
        }
        content.applyQueuedMutations();
        ScrollView scroll = new ScrollView(content);
        Widget anchor = content.children().get(5);
        Tooltip tooltip = new Tooltip(anchor, "hint");
        OverlayLayer root = new OverlayLayer(scroll);
        root.addOverlay(tooltip);
        root.applyQueuedMutations();
        root.measure(new LayoutContext(400.0f, 300.0f));
        root.arrange(new MutableRect(0.0f, 0.0f, 400.0f, 300.0f));
        float before = tooltip.layoutBounds().y();
        clearDirect(root);

        // Прод-сценарий: скролл двигает якорь без layout, затем кадр тикает дерево.
        scroll.scrollBy(0.0f, 24.0f);
        root.tick(new FrameContext(1L, 1.0f / 60.0f, 0.0f, FramePhase.ANIMATION));

        float moved = tooltip.layoutBounds().y() - before;
        check(Math.abs(moved - -24.0f) < 0.001f,
                "tooltip must follow scrolled anchor without layout, moved " + moved);
        check(!InvalidationFlags.has(tooltip.invalidationFlags(), InvalidationFlags.LAYOUT),
                "tooltip follow must not request layout");
    }

    private static void clearDirect(WidgetBase widget) {
        widget.clearInvalidationDirect(InvalidationFlags.ALL);
        for (Widget child : widget.children()) {
            if (child instanceof WidgetBase base) {
                clearDirect(base);
            }
        }
    }

    private static RectView copy(RectView bounds) {
        return new MutableRect(bounds.x(), bounds.y(), bounds.width(), bounds.height());
    }

    private static void hover(Box anchor, boolean inside) {
        if (inside) {
            anchor.handle(new PointerEnteredEvent(anchor, anchor, EventPhase.TARGET, 0.0f, 0.0f, 0.0f, 0.0f, 0));
        } else {
            anchor.handle(new PointerExitedEvent(anchor, anchor, EventPhase.TARGET, 0.0f, 0.0f, 0.0f, 0.0f, 0));
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
