package dev.sixik.unigui.api.core;

public interface UiDispatcher {
    boolean isUiThread();

    void execute(Runnable action);

    void executeNextFrame(Runnable action);

    void drain();
}
