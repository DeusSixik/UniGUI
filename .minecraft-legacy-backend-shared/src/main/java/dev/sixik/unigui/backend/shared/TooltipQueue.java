package dev.sixik.unigui.backend.shared;

import java.util.ArrayList;
import java.util.List;

/** Client-thread queue used to replay vanilla tooltip draws after overlay layers. */
public final class TooltipQueue {
    private static final List<Runnable> QUEUE = new ArrayList<>();
    private static boolean deferring;

    private TooltipQueue() {
    }

    public static void begin() {
        QUEUE.clear();
        deferring = true;
    }

    public static void end() {
        deferring = false;
    }

    public static boolean deferring() {
        return deferring;
    }

    public static boolean defer(Runnable draw) {
        if (!deferring) return false;
        QUEUE.add(draw);
        return true;
    }

    public static void flush() {
        if (QUEUE.isEmpty()) return;
        Runnable[] pending = QUEUE.toArray(new Runnable[0]);
        QUEUE.clear();
        for (Runnable draw : pending) draw.run();
    }

    public static void discard() {
        deferring = false;
        QUEUE.clear();
    }
}
