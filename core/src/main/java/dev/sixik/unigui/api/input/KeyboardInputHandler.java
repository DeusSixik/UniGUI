package dev.sixik.unigui.api.input;

/** Optional screen-level keyboard interceptor, invoked before focused widgets receive input. */
@FunctionalInterface
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
