package dev.sixik.isf.tests;

import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.widgets.containers.Box;
import dev.sixik.unigui.widgets.containers.VBox;
import dev.sixik.unigui.widgets.display.Label;

/** Воспроизводит переключение страниц рецептов: clearChildren + повторный addChild. */
public final class IsfPageFlipSelfTest {
    public static void main(String[] args) {
        new IsfPageFlipSelfTest().run();
        System.out.println("IsfPageFlipSelfTest passed");
    }

    private void run() {
        VBox area = new VBox();
        area.spacing(2.0f);
        area.layout(style -> style.widthPercent(100.0f).flexGrow(1.0f).flexShrink(1.0f));

        Widget pageOne = recipe();
        Widget pageTwo = recipe();

        // Страница 1: первичный рендер.
        area.addChild(pageOne);
        layout(area);
        check(area.children().contains(pageOne), "first page should be attached");
        check(pageOne.layoutBounds().width() > 0.0f && pageOne.layoutBounds().height() > 0.0f,
                "first page should be arranged");
        check(pageOne.visibility() == Visibility.VISIBLE, "first page should be visible");

        // Вперёд: clearChildren утилизирует pageOne, затем рендер страницы 2.
        area.clearChildren();
        area.addChild(pageTwo);
        layout(area);
        check(area.children().contains(pageTwo), "second page should be attached");
        check(pageTwo.layoutBounds().width() > 0.0f && pageTwo.layoutBounds().height() > 0.0f,
                "second page should be arranged");

        // Назад: повторное добавление pageOne, который уже был в дереве и очищен.
        area.clearChildren();
        area.addChild(pageOne);
        layout(area);
        check(area.children().contains(pageOne), "re-attached page should stay in children");
        check(pageOne.parent() == area, "re-attached page should restore its parent");
        check(pageOne.visibility() == Visibility.VISIBLE,
                "re-attached page should remain visible");
        check(pageOne.layoutBounds().width() > 0.0f && pageOne.layoutBounds().height() > 0.0f,
                "re-attached page should be arranged again after clearChildren");
    }

    private Widget recipe() {
        Box visual = new Box();
        visual.layout(style -> style.position(dev.sixik.unigui.api.layout.PositionType.RELATIVE)
                .width(176.0f).height(92.0f).flexNone());
        Label content = new Label("recipe");
        content.layout(style -> style.width(40.0f).height(14.0f).flexNone());
        visual.addChild(content);
        return visual;
    }

    private static void layout(VBox area) {
        area.applyQueuedMutations();
        area.measure(new LayoutContext(210.0f, 224.0f));
        area.arrange(new MutableRect(0.0f, 0.0f, 210.0f, 224.0f));
        for (Widget child : area.children()) {
            child.measure(new LayoutContext(child.layoutBounds().width(),
                    child.layoutBounds().height()));
            child.arrange(child.layoutBounds());
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
