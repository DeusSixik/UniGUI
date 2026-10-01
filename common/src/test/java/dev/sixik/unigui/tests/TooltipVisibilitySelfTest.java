package dev.sixik.unigui.tests;

import dev.sixik.unigui.api.event.EventPhase;
import dev.sixik.unigui.api.event.PointerEnteredEvent;
import dev.sixik.unigui.api.event.PointerExitedEvent;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.widgets.containers.Box;
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

        System.out.println("TooltipVisibilitySelfTest passed");
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
