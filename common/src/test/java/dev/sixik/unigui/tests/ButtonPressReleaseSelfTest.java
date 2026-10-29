package dev.sixik.unigui.tests;

import dev.sixik.unigui.api.event.PointerPressedEvent;
import dev.sixik.unigui.api.input.PointerButton;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.impl.core.DefaultUIContext;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.widgets.interaction.Button;

/**
 * Кнопка, ставшая скрытой или выключенной в нажатом состоянии, должна сама отжаться и отпустить захват
 * указателя: событие отпускания до неё уже не дойдёт.
 */
public final class ButtonPressReleaseSelfTest {
    public static void main(String[] args) {
        new ButtonPressReleaseSelfTest().run();
        System.out.println("ButtonPressReleaseSelfTest passed");
    }

    private void run() {
        DefaultUIContext uiContext = new DefaultUIContext();

        Button hidden = pressed(uiContext, "A");
        expect(hidden.pressed() && uiContext.capturedPointer(0) == hidden,
                "Primary press should press the button and capture the pointer");
        hidden.visibility(Visibility.HIDDEN);
        expect(!hidden.pressed() && uiContext.capturedPointer(0) == null,
                "A button hidden while pressed should release itself and the pointer capture");

        Button collapsed = pressed(uiContext, "B");
        collapsed.visibility(Visibility.COLLAPSED);
        expect(!collapsed.pressed() && uiContext.capturedPointer(0) == null,
                "A button collapsed while pressed should release itself and the pointer capture");

        Button disabled = pressed(uiContext, "C");
        disabled.enabled(false);
        expect(!disabled.pressed() && uiContext.capturedPointer(0) == null,
                "A button disabled while pressed should release itself and the pointer capture");

        // Показ обратно не оставляет кнопку нажатой и позволяет нажать её заново.
        hidden.visibility(Visibility.VISIBLE);
        expect(!hidden.pressed(), "A re-shown button should not stay pressed");
        hidden.handle(new PointerPressedEvent(hidden, 5.0f, 5.0f, 5.0f, 5.0f, 0, PointerButton.PRIMARY));
        expect(hidden.pressed(), "A re-shown button should be pressable again");
        hidden.visibility(Visibility.HIDDEN);

        // Скрытие простаивающей кнопки ничего не снимает у других.
        Button holder = pressed(uiContext, "D");
        Button idle = new Button("E");
        idle.setUiContextInternal(uiContext);
        idle.visibility(Visibility.HIDDEN);
        expect(holder.pressed() && uiContext.capturedPointer(0) == holder,
                "Hiding an idle button must not release another button's pointer capture");
        holder.visibility(Visibility.HIDDEN);
    }

    private static Button pressed(DefaultUIContext uiContext, String text) {
        Button button = new Button(text);
        button.setUiContextInternal(uiContext);
        button.arrange(new MutableRect(0.0f, 0.0f, 40.0f, 20.0f));
        button.handle(new PointerPressedEvent(button, 5.0f, 5.0f, 5.0f, 5.0f, 0, PointerButton.PRIMARY));
        return button;
    }

    private static void expect(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}