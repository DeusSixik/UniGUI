package dev.sixik.unigui.tests;

import dev.sixik.unigui.api.core.FrameContext;
import dev.sixik.unigui.api.core.FramePhase;
import dev.sixik.unigui.api.event.EventPhase;
import dev.sixik.unigui.api.event.PointerPressedEvent;
import dev.sixik.unigui.api.event.PointerReleasedEvent;
import dev.sixik.unigui.api.input.PointerButton;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.render.DrawCommand;
import dev.sixik.unigui.api.render.DrawCommandType;
import dev.sixik.unigui.api.render.DrawList;
import dev.sixik.unigui.impl.core.DefaultUIContext;
import dev.sixik.unigui.impl.render.DefaultRenderContext;
import dev.sixik.unigui.widgets.interaction.HoldButton;

public final class HoldButtonSelfTest {
    public static void main(String[] args) {
        new HoldButtonSelfTest().run();
        System.out.println("HoldButtonSelfTest passed");
    }

    private void run() {
        releaseBeforeDurationCancelsWithoutClick();
        completingHoldEmitsHoldCompleteAndClick();
        cancellingHoldCompleteSuppressesClick();
        progressFillFollowsRadiusAndBorderInset();
    }

    private void releaseBeforeDurationCancelsWithoutClick() {
        HoldButton button = fixture();
        Counter counter = new Counter();
        button.onHoldCompleted(event -> counter.completed++);
        button.onClick(event -> counter.clicked++);

        press(button, 1);
        button.tick(frame(0.20f));
        expect(button.holding(), "button should be holding after primary press");
        expect(button.holdProgress() > 0.0f, "hold progress should advance while holding");

        release(button, 1);
        expect(!button.holding(), "release should stop holding");
        expect(button.holdProgress() == 0.0f, "release before completion should reset progress");
        expect(counter.completed == 0, "release before duration should not complete hold");
        expect(counter.clicked == 0, "release before duration should not click");
    }

    private void completingHoldEmitsHoldCompleteAndClick() {
        HoldButton button = fixture();
        Counter counter = new Counter();
        button.onHoldCompleted(event -> counter.completed++);
        button.onClick(event -> counter.clicked++);

        press(button, 2);
        button.tick(frame(0.31f));
        button.tick(frame(0.30f));

        expect(!button.holding(), "completed hold should stop holding");
        expect(button.completed(), "completed hold should set completed state");
        expect(counter.completed == 1, "completed hold should emit HoldCompletedEvent once");
        expect(counter.clicked == 1, "completed hold should emit click once");
        expect(button.holdProgress() == 0.0f, "completed hold should reset visible progress after click");
    }

    private void cancellingHoldCompleteSuppressesClick() {
        HoldButton button = fixture();
        Counter counter = new Counter();
        button.onHoldCompleted(event -> {
            counter.completed++;
            event.cancel();
        });
        button.onClick(event -> counter.clicked++);

        press(button, 3);
        button.tick(frame(0.70f));

        expect(counter.completed == 1, "cancelled completion should still emit HoldCompletedEvent");
        expect(counter.clicked == 0, "cancelled HoldCompletedEvent should suppress click");
    }

    private void progressFillFollowsRadiusAndBorderInset() {
        HoldButton button = fixture();
        button.radius(8.0f);
        button.borderVisible(true);
        button.borderWidth(2.0f);
        // Тема анимирует radius/borderWidth к своим значениям на первом рендере —
        // прогреваем тиками, чтобы ассёртить устоявшийся вид, а не переходный кадр.
        button.render(new DefaultRenderContext(new DrawList()));
        for (int i = 0; i < 5; i++) {
            button.tick(frame(0.5f));
            button.render(new DefaultRenderContext(new DrawList()));
        }
        float settledRadius = button.radius();
        float settledBorder = button.borderWidth();
        press(button, 7);
        button.tick(frame(0.30f));

        DrawList drawList = new DrawList();
        button.render(new DefaultRenderContext(drawList));
        DrawCommand fill = firstFillWithColor(drawList,
                button.holdColor().r(), button.holdColor().g(), button.holdColor().b());
        expect(fill != null, "holding button should render a progress fill");
        expect(fill.type() == DrawCommandType.ROUNDED_RECT,
                "progress fill should follow the button radius instead of a sharp rect");
        float inset = Math.max(0.0f, settledBorder);
        float insetWidth = 120.0f - inset * 2.0f;
        expect(near(fill.bounds().x(), inset) && near(fill.bounds().y(), inset)
                        && near(fill.bounds().height(), 24.0f - inset * 2.0f),
                "progress fill should inset by the border width instead of covering the frame");
        expect(near(fill.bounds().width(), insetWidth * 0.5f),
                "half progress should fill half of the inset width");
        expect(near(fill.radius(), Math.max(0.0f, settledRadius - inset)),
                "progress fill radius should be the button radius minus the border inset");
    }

    private static HoldButton fixture() {
        HoldButton button = new HoldButton("Hold")
                .holdDurationSeconds(0.60f);
        button.setUiContextInternal(new DefaultUIContext());
        button.measure(new LayoutContext(120.0f, 24.0f));
        button.arrange(new MutableRect(0.0f, 0.0f, 120.0f, 24.0f));
        return button;
    }

    private static void press(HoldButton button, int pointerId) {
        button.handle(new PointerPressedEvent(
                button,
                6.0f,
                6.0f,
                6.0f,
                6.0f,
                pointerId,
                PointerButton.PRIMARY).routeTo(button, EventPhase.TARGET));
    }

    private static void release(HoldButton button, int pointerId) {
        button.handle(new PointerReleasedEvent(
                button,
                6.0f,
                6.0f,
                6.0f,
                6.0f,
                pointerId,
                PointerButton.PRIMARY).routeTo(button, EventPhase.TARGET));
    }

    private static FrameContext frame(float deltaSeconds) {
        return new FrameContext(1L, deltaSeconds, 0.0f, FramePhase.ANIMATION);
    }

    private static DrawCommand firstFillWithColor(DrawList drawList, float r, float g, float b) {
        for (DrawCommand command : drawList.commands()) {
            if (command.paint() != null && !command.paint().isStroke()
                    && near(command.paint().color().r(), r)
                    && near(command.paint().color().g(), g)
                    && near(command.paint().color().b(), b)) {
                return command;
            }
        }
        return null;
    }

    private static boolean near(float value, float expected) {
        return Math.abs(value - expected) < 0.01f;
    }

    private static void expect(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static final class Counter {
        private int completed;
        private int clicked;
    }
}
