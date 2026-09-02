package dev.sixik.unigui.tests;

import dev.sixik.unigui.api.animation.Anchor;
import dev.sixik.unigui.api.animation.AnimationController;
import dev.sixik.unigui.api.animation.AnimationEasing;
import dev.sixik.unigui.api.animation.Easing;
import dev.sixik.unigui.api.animation.IntTransition;
import dev.sixik.unigui.api.animation.Interpolator;
import dev.sixik.unigui.api.animation.Interpolators;
import dev.sixik.unigui.api.animation.ShakeAnimation;
import dev.sixik.unigui.api.animation.Timeline;
import dev.sixik.unigui.api.animation.TransitionSpec;
import dev.sixik.unigui.api.math.MutableVec2;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.impl.core.QueuedUiDispatcher;
import dev.sixik.unigui.api.animation.Tween;
import dev.sixik.unigui.api.core.FrameContext;
import dev.sixik.unigui.api.core.FramePhase;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.style.MutableStyle;
import dev.sixik.unigui.api.style.StyleKeyRegistry;
import dev.sixik.unigui.api.style.StyleKeys;
import dev.sixik.unigui.api.style.WidgetState;
import dev.sixik.unigui.impl.style.DefaultTheme;
import dev.sixik.unigui.widgets.interaction.Button;

public final class AnimationInterpolationSelfTest {
    private static final float EPSILON = 0.0001f;
    private static final Interpolator<Point> POINTS = (start, end, progress) -> new Point(
            start.x + (end.x - start.x) * progress,
            start.y + (end.y - start.y) * progress);

    public static void main(String[] args) {
        AnimationInterpolationSelfTest test = new AnimationInterpolationSelfTest();
        test.intTransitionRoundsWithoutOverflow();
        test.customTypeUsesTypedInterpolatorAndWriter();
        test.retargetStartsFromCurrentValue();
        test.yoyoReturnsToStart();
        test.discreteInterpolatorSwitchesAtEnd();
        test.customEasingWorksWithAllTransitionTypes();
        test.standardEasingAndCubicBezierAreStable();
        test.backgroundControllerMutationUsesDispatcher();
        test.anchorAndShakeAreReusable();
        test.staggerStartsItemsWithAnOffset();
        test.styleTransitionsAreRegisteredAndDoNotRestart();
        System.out.println("AnimationInterpolationSelfTest passed");
    }

    private void intTransitionRoundsWithoutOverflow() {
        IntTransition rounded = new IntTransition(
                0, 9, TransitionSpec.of(1.0f, AnimationEasing.LINEAR));
        rounded.tick(0.25f);
        expect(rounded.tick(0.25f) == 5, "int transition should round to the nearest integer");

        IntTransition fullRange = new IntTransition(
                Integer.MIN_VALUE,
                Integer.MAX_VALUE,
                TransitionSpec.of(1.0f, AnimationEasing.LINEAR));
        fullRange.tick(0.25f);
        expect(fullRange.tick(0.25f) == 0, "int interpolation should not overflow its delta");
    }

    private void customTypeUsesTypedInterpolatorAndWriter() {
        Holder holder = new Holder(new Point(2.0f, 4.0f));
        Tween<Point> tween = Tween.from(
                holder::value,
                new Point(10.0f, 20.0f),
                TransitionSpec.of(1.0f, AnimationEasing.LINEAR),
                POINTS,
                holder::value);

        tween.tick(0.25f);

        expectClose(holder.value.x, 4.0f, "writer should receive interpolated X");
        expectClose(holder.value.y, 8.0f, "writer should receive interpolated Y");
    }

    private void retargetStartsFromCurrentValue() {
        Tween<Point> tween = new Tween<>(
                new Point(0.0f, 0.0f),
                new Point(10.0f, 20.0f),
                TransitionSpec.of(1.0f, AnimationEasing.LINEAR),
                POINTS);

        tween.tick(0.25f);
        tween.tick(0.25f);
        Point beforeRetarget = tween.currentValue();
        tween.retarget(new Point(20.0f, 40.0f));

        expectClose(tween.currentValue().x, beforeRetarget.x, "retarget should not jump on X");
        expectClose(tween.currentValue().y, beforeRetarget.y, "retarget should not jump on Y");

        tween.tick(0.25f);
        tween.tick(0.25f);
        expectClose(tween.currentValue().x, 12.5f, "retarget should interpolate from current X");
        expectClose(tween.currentValue().y, 25.0f, "retarget should interpolate from current Y");
    }

    private void yoyoReturnsToStart() {
        Point start = new Point(3.0f, 6.0f);
        Tween<Point> tween = new Tween<>(
                start,
                new Point(12.0f, 24.0f),
                new TransitionSpec(1.0f, AnimationEasing.LINEAR, 1, true),
                POINTS);

        for (int i = 0; i < 8; i++) tween.tick(0.25f);

        expect(tween.finished(), "finite yoyo tween should finish");
        expect(tween.currentValue() == start, "yoyo with two cycles should return original reference");
    }

    private void discreteInterpolatorSwitchesAtEnd() {
        Tween<String> tween = new Tween<>(
                "offline",
                "online",
                TransitionSpec.of(1.0f, AnimationEasing.LINEAR),
                Interpolators.discrete());

        for (int i = 0; i < 3; i++) tween.tick(0.25f);
        expect("offline".equals(tween.currentValue()), "discrete tween should keep start before end");
        expect("online".equals(tween.tick(0.25f)), "discrete tween should switch at end");
    }

    private void customEasingWorksWithAllTransitionTypes() {
        var cubicIn = (dev.sixik.unigui.api.animation.Easing) progress -> progress * progress * progress;
        TransitionSpec spec = TransitionSpec.of(1.0f, cubicIn);

        IntTransition integer = new IntTransition(0, 100, spec);
        integer.tick(0.25f);
        integer.tick(0.25f);
        expect(integer.currentValue() == 13, "custom easing should affect int transition");

        Tween<Float> decimal = new Tween<>(0.0f, 100.0f, spec, Interpolators.FLOAT);
        decimal.tick(0.25f);
        decimal.tick(0.25f);
        expectClose(decimal.currentValue(), 12.5f, "custom easing should affect generic transition");
    }

    private void standardEasingAndCubicBezierAreStable() {
        expectClose(AnimationEasing.QUAD_IN.apply(0.5f), 0.25f,
                "quad easing should produce the expected midpoint");
        expectClose(AnimationEasing.CUBIC_OUT.apply(0.5f), 0.875f,
                "cubic easing should produce the expected midpoint");
        expectClose(AnimationEasing.EXPO_IN.apply(0.0f), 0.0f,
                "expo in should preserve the start");
        expectClose(AnimationEasing.EXPO_OUT.apply(1.0f), 1.0f,
                "expo out should preserve the end");

        Easing bezier = Easing.cubicBezier(0.25f, 0.1f, 0.25f, 1.0f);
        expectClose(bezier.apply(0.0f), 0.0f,
                "cubic bezier should preserve the start");
        expectClose(bezier.apply(1.0f), 1.0f,
                "cubic bezier should preserve the end");
        expect(bezier.apply(0.5f) > 0.5f,
                "ease-out cubic bezier should advance past the linear midpoint");

        expectThrows(() -> Easing.cubicBezier(-0.1f, 0.0f, 0.5f, 1.0f),
                "cubic bezier should reject an invalid X coordinate");
        expectThrows(() -> Easing.cubicBezier(0.0f, Float.NaN, 1.0f, 1.0f),
                "cubic bezier should reject a non-finite Y coordinate");
    }

    private static void expectThrows(Runnable action, String message) {
        try {
            action.run();
        } catch (IllegalArgumentException expected) {
            return;
        }
        throw new AssertionError(message);
    }
    private void anchorAndShakeAreReusable() {
        RectView bounds = new RectView() {
            @Override public float x() { return 10.0f; }
            @Override public float y() { return 20.0f; }
            @Override public float width() { return 100.0f; }
            @Override public float height() { return 60.0f; }
        };
        MutableVec2 point = Anchor.RIGHT_BOTTOM.resolve(bounds, new MutableVec2());
        expectClose(point.x(), 110.0f, "anchor should resolve absolute X");
        expectClose(point.y(), 80.0f, "anchor should resolve absolute Y");

        ShakeAnimation shake = new ShakeAnimation(10.0f, 4.0f, 1.0f, 1);
        shake.update(0.125f);
        expect(shake.offsetX() > 0.0f, "shake should produce an offset while active");
        for (int i = 0; i < 4; i++) shake.update(0.25f);
        expect(shake.isFinished(), "shake should finish at its duration");
        expectClose(shake.offsetX(), 0.0f, "finished shake should have no residual offset");
    }
    private void staggerStartsItemsWithAnOffset() {
        ProbeAnimation[] animations = new ProbeAnimation[3];
        Timeline timeline = Timeline.stagger(
                java.util.List.of(0, 1, 2),
                index -> animations[index] = new ProbeAnimation(0.1f),
                100.0f);

        timeline.update(0.05f);
        expect(animations[0].updates == 1, "stagger should start the first item immediately");
        expect(animations[1].updates == 0, "stagger should delay the second item");
        expect(animations[2].updates == 0, "stagger should delay the third item");

        timeline.update(0.05f);
        expect(animations[0].isFinished(), "first stagger item should finish independently");
        expect(animations[1].updates == 0, "item at the exact boundary should start on the next step");

        timeline.update(0.05f);
        expect(animations[1].updates == 1, "second stagger item should start after its offset");
        expect(animations[2].updates == 0, "third stagger item should remain delayed");

        timeline.update(0.25f);
        expect(timeline.isFinished(), "stagger should finish after the last item");
    }

    private void styleTransitionsAreRegisteredAndDoNotRestart() {
        StyleKeyRegistry registry = StyleKeyRegistry.builtIns();
        expect(registry.descriptor(StyleKeys.TRANSITION_DURATION).isPresent(),
                "transition duration should be registered");
        expect(registry.descriptor(StyleKeys.TRANSITION_EASING)
                        .map(descriptor -> descriptor.parse("cubic-out"))
                        .orElse(null) == AnimationEasing.CUBIC_OUT,
                "transition easing codec should parse style values");
        expectClose(DefaultTheme.INSTANCE.styleFor(Button.STYLE_TYPE)
                        .get(StyleKeys.TRANSITION_DURATION, WidgetState.NORMAL, 0.0f),
                0.12f,
                "default button style should provide a declarative transition");

        MutableStyle style = new MutableStyle()
                .put(StyleKeys.BACKGROUND_COLOR, new MutableColor(0.0f, 0.0f, 0.0f, 1.0f))
                .put(StyleKeys.BACKGROUND_COLOR, WidgetState.HOVERED,
                        new MutableColor(1.0f, 1.0f, 1.0f, 1.0f))
                .put(StyleKeys.TRANSITION_DURATION, 1.0f)
                .put(StyleKeys.TRANSITION_EASING, AnimationEasing.LINEAR);
        StyleTransitionProbeButton button = new StyleTransitionProbeButton();
        button.localStyle(Button.STYLE_TYPE, style);

        button.applyResolvedTheme();
        expectClose(button.background().r(), 0.0f,
                "first style application should be immediate");

        button.state(WidgetState.HOVERED);
        button.applyResolvedTheme();
        button.tick(animationFrame(0, 0.25f));
        expectClose(button.background().r(), 0.25f,
                "hover style should start an automatic transition");

        button.applyResolvedTheme();
        button.tick(animationFrame(1, 0.25f));
        expectClose(button.background().r(), 0.5f,
                "reapplying the same style target should not restart its transition");
    }

    private static FrameContext animationFrame(long index, float deltaSeconds) {
        return new FrameContext(index, deltaSeconds, 0.0f, FramePhase.ANIMATION);
    }
    private void backgroundControllerMutationUsesDispatcher() {
        QueuedUiDispatcher dispatcher = new QueuedUiDispatcher(Thread.currentThread());
        AnimationController controller = new AnimationController(dispatcher);
        Thread worker = new Thread(() -> controller.play(
                "background",
                new IntTransition(0, 1, TransitionSpec.INSTANT)));
        worker.start();
        try {
            worker.join();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AssertionError("worker thread was interrupted", exception);
        }

        expect(controller.size() == 0,
                "background mutation should wait for the UI dispatcher");
        dispatcher.drain();
        expect(controller.size() == 1,
                "dispatcher should apply the queued animation mutation");
        controller.stop("background");
        expect(controller.size() == 0,
                "stop should remove the animation on the UI thread");
    }
    private static void expectClose(float actual, float expected, String message) {
        expect(Math.abs(actual - expected) <= EPSILON, message + ": " + actual + " != " + expected);
    }

    private static void expect(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static final class ProbeAnimation implements dev.sixik.unigui.api.animation.PlayableAnimation {
        private final float duration;
        private float elapsed;
        private int updates;

        private ProbeAnimation(float duration) { this.duration = duration; }

        @Override
        public void update(float deltaSeconds) {
            updates++;
            elapsed += deltaSeconds;
        }

        @Override
        public boolean isFinished() { return elapsed >= duration; }

        @Override
        public void cancel() { elapsed = duration; }
    }

    private static final class StyleTransitionProbeButton extends Button {
        private WidgetState state = WidgetState.NORMAL;

        private void state(WidgetState state) {
            this.state = state;
        }

        private void applyResolvedTheme() {
            applyTheme();
        }

        @Override
        protected WidgetState styleState() {
            return state;
        }

        @Override
        protected String styleType() {
            return Button.STYLE_TYPE;
        }
    }
    private static final class Point {
        private final float x;
        private final float y;

        private Point(float x, float y) {
            this.x = x;
            this.y = y;
        }
    }

    private static final class Holder {
        private Point value;

        private Holder(Point value) { this.value = value; }

        private Point value() { return value; }

        private void value(Point value) { this.value = value; }
    }
}
