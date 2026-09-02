package dev.sixik.unigui.tests;

import dev.sixik.unigui.api.animation.AnimationEasing;
import dev.sixik.unigui.api.animation.LayoutTransitionAnimation;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.impl.widget.WidgetBase;

/** Проверяет FLIP-переходы перемещения виджета после layout-изменений. */
public final class LayoutTransitionSelfTest {
    public static void main(String[] args) {
        LayoutTransitionSelfTest test = new LayoutTransitionSelfTest();
        test.layoutTransitionIsOptIn();
        test.layoutTransitionUsesOldPositionAndKeepsBounds();
        test.zeroInitialBoundsStillEstablishFirstLayout();
        test.resizeAloneDoesNotStartTransition();
        test.transitionCanBeRetargetedWithoutJump();
        test.transitionFinishesAtZero();
        System.out.println("LayoutTransitionSelfTest passed");
    }

    private void layoutTransitionIsOptIn() {
        TestWidget widget = new TestWidget();
        widget.arrange(new MutableRect(10.0f, 20.0f, 30.0f, 40.0f));
        widget.arrange(new MutableRect(25.0f, 35.0f, 30.0f, 40.0f));

        expect(widget.layoutTransitionAnimation() == null,
                "layout transition must be disabled by default");
    }

    private void layoutTransitionUsesOldPositionAndKeepsBounds() {
        TestWidget widget = new TestWidget();
        widget.layoutTransition(1.0f, AnimationEasing.LINEAR);
        widget.arrange(new MutableRect(10.0f, 20.0f, 30.0f, 40.0f));
        widget.arrange(new MutableRect(25.0f, 35.0f, 30.0f, 40.0f));

        LayoutTransitionAnimation animation = widget.layoutTransitionAnimation();
        expect(animation != null, "moving an opted-in widget must start FLIP");
        expectClose(-15.0f, animation.offsetX(), "FLIP X offset must be First minus Last");
        expectClose(-15.0f, animation.offsetY(), "FLIP Y offset must be First minus Last");
        expectClose(25.0f, widget.layoutBounds().x(), "FLIP must keep the new X bounds");
        expectClose(35.0f, widget.layoutBounds().y(), "FLIP must keep the new Y bounds");
        expectClose(0.0f, widget.transform().position().x(), "FLIP must not mutate base transform X");
        expectClose(0.0f, widget.transform().position().y(), "FLIP must not mutate base transform Y");
    }

    private void resizeAloneDoesNotStartTransition() {
        TestWidget widget = new TestWidget();
        widget.layoutTransition(1.0f);
        widget.arrange(new MutableRect(10.0f, 20.0f, 30.0f, 40.0f));
        widget.arrange(new MutableRect(10.0f, 20.0f, 50.0f, 60.0f));

        expect(widget.layoutTransitionAnimation() == null,
                "changing only size must not start a Phase 7 FLIP transition");
    }

    private void zeroInitialBoundsStillEstablishFirstLayout() {
        TestWidget widget = new TestWidget();
        widget.layoutTransition(1.0f, AnimationEasing.LINEAR);
        widget.arrange(new MutableRect(0.0f, 0.0f, 0.0f, 0.0f));
        widget.arrange(new MutableRect(20.0f, 30.0f, 10.0f, 10.0f));

        LayoutTransitionAnimation animation = widget.layoutTransitionAnimation();
        expect(animation != null, "a move after zero initial bounds must start FLIP");
        expectClose(-20.0f, animation.offsetX(), "zero initial layout must preserve First X");
        expectClose(-30.0f, animation.offsetY(), "zero initial layout must preserve First Y");
    }

    private void transitionCanBeRetargetedWithoutJump() {
        TestWidget widget = new TestWidget();
        widget.layoutTransition(1.0f, AnimationEasing.LINEAR);
        widget.arrange(new MutableRect(0.0f, 0.0f, 20.0f, 20.0f));
        widget.arrange(new MutableRect(100.0f, 0.0f, 20.0f, 20.0f));

        LayoutTransitionAnimation first = widget.layoutTransitionAnimation();
        first.update(0.25f);
        float visualOffsetBeforeRetarget = first.offsetX();
        widget.arrange(new MutableRect(140.0f, 0.0f, 20.0f, 20.0f));

        LayoutTransitionAnimation second = widget.layoutTransitionAnimation();
        expect(second != first, "a new layout change must replace the old FLIP animation");
        expectClose(visualOffsetBeforeRetarget - 40.0f, second.offsetX(),
                "retarget must preserve the current visual position");
    }

    private void transitionFinishesAtZero() {
        LayoutTransitionAnimation animation = new LayoutTransitionAnimation(
                30.0f, -10.0f, 0.5f, AnimationEasing.LINEAR);
        animation.update(0.25f);
        animation.update(0.25f);

        expect(animation.isFinished(), "FLIP must finish after its duration");
        expectClose(0.0f, animation.offsetX(), "finished FLIP X offset must be zero");
        expectClose(0.0f, animation.offsetY(), "finished FLIP Y offset must be zero");
    }

    private static final class TestWidget extends WidgetBase {
        @Override
        public void measure(LayoutContext context) {
            setDesiredSize(0.0f, 0.0f);
        }

        @Override
        public void render(RenderContext context) {
        }
    }

    private static void expect(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void expectClose(float expected, float actual, String message) {
        if (Math.abs(expected - actual) > 0.0005f) {
            throw new AssertionError(message + ": expected " + expected + ", got " + actual);
        }
    }
}
