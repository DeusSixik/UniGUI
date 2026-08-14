package dev.sixik.unigui.tests;

import dev.sixik.unigui.api.core.FrameContext;
import dev.sixik.unigui.api.core.FramePhase;
import dev.sixik.unigui.api.event.EventPhase;
import dev.sixik.unigui.api.event.PointerPressedEvent;
import dev.sixik.unigui.api.event.PointerReleasedEvent;
import dev.sixik.unigui.api.input.PointerButton;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.impl.core.DefaultUIContext;
import dev.sixik.unigui.widgets.HoldButton;

public final class HoldButtonSelfTest {
    public static void main(String[] args) {
        new HoldButtonSelfTest().run();
        System.out.println("HoldButtonSelfTest passed");
    }

    private void run() {
        releaseBeforeDurationCancelsWithoutClick();
        completingHoldEmitsHoldCompleteAndClick();
        cancellingHoldCompleteSuppressesClick();
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
