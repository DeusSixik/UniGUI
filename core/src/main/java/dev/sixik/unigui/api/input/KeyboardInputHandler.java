package dev.sixik.unigui.api.input;

import dev.sixik.unigui.api.docs.AiDoc;

/** Optional screen-level keyboard interceptor, invoked before focused widgets receive input. */
@FunctionalInterface
@AiDoc(
        summary = "Screen-level keyboard event interceptor evaluated before passing events to focused widgets.",
        purpose = "Allows handling global screen hotkeys, ESC dismissal, or global shortcuts before child widgets consume them.",
        useWhen = {"Intercepting keyboard input at the UniScreen level."},
        related = {"UniScreen"}
)
public interface KeyboardInputHandler {
    /**
     * Handles a normalized key press.
     *
     * @return {@code true} if the input was consumed
     */
    boolean keyPressed(int keyCode, int scanCode, int modifiers);

    /**
     * Handles a normalized key release.
     *
     * @return {@code true} if the input was consumed
     */
    default boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        return false;
    }

    /**
     * Handles Unicode text input.
     *
     * @return {@code true} if the input was consumed
     */
    default boolean textInput(int codePoint, int modifiers) {
        return false;
    }
}
