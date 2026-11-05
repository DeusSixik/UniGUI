package dev.sixik.unigui.impl.core;

import dev.sixik.unigui.api.core.UiDispatcher;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class QueuedUiDispatcher implements UiDispatcher {
    private final Thread uiThread;
    private final Queue<Runnable> queue = new ConcurrentLinkedQueue<>();

    public QueuedUiDispatcher() {
        this(Thread.currentThread());
    }

    public QueuedUiDispatcher(Thread uiThread) {
        this.uiThread = uiThread;
    }

    @Override
    public boolean isUiThread() {
        return Thread.currentThread() == uiThread;
    }

    @Override
    public void execute(Runnable action) {
        if (isUiThread()) {
            action.run();
        } else {
            queue.add(action);
        }
    }

    @Override
    public void executeNextFrame(Runnable action) {
        queue.add(action);
    }

    @Override
    public void drain() {
        Runnable action;
        while ((action = queue.poll()) != null) {
            action.run();
        }
    }
}
