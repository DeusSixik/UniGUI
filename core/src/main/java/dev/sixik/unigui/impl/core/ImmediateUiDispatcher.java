package dev.sixik.unigui.impl.core;

import dev.sixik.unigui.api.core.UiDispatcher;

public final class ImmediateUiDispatcher implements UiDispatcher {
    public static final ImmediateUiDispatcher INSTANCE = new ImmediateUiDispatcher();

    private ImmediateUiDispatcher() {
    }

    @Override
    public boolean isUiThread() {
        return true;
    }

    @Override
    public void execute(Runnable action) {
        action.run();
    }

    @Override
    public void executeNextFrame(Runnable action) {
        action.run();
    }

    @Override
    public void drain() {
    }
}
