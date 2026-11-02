package dev.sixik.unigui.gtnh.backend;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Runs work on the client thread at the end of the current client tick.
 *
 * <p>1.7.10 has no {@code Minecraft.addScheduledTask}. The queue is needed for opening a screen from a chat command:
 * {@code GuiChat.keyTyped} sends the message and <em>then</em> calls {@code displayGuiScreen(null)}, so a screen
 * opened inside the command is closed again in the same call. A task queued by the command runs at
 * {@link TickEvent.ClientTickEvent} {@code END}, after the chat screen has been dismissed.</p>
 *
 * <p>A task that throws is logged and does not stop the others.</p>
 */
public final class GtnhClientTasks {
    private static final Logger LOGGER = LogManager.getLogger("unigui");
    private static final ConcurrentLinkedQueue<Runnable> QUEUE = new ConcurrentLinkedQueue<>();
    private static volatile Thread clientThread;

    private GtnhClientTasks() {
    }

    /**
     * Remembers the calling thread as the client thread; called once during client start-up.
     */
    static void markClientThread() {
        clientThread = Thread.currentThread();
    }

    /**
     * @return {@code true} on the client thread (also before it is known, so code that runs outside the game, for
     * example in tests, behaves as if it were on it)
     */
    public static boolean isClientThread() {
        Thread known = clientThread;
        return known == null || known == Thread.currentThread();
    }

    /**
     * Runs the task now when called on the client thread, otherwise queues it for the end of the client tick. Use it
     * for work that must touch OpenGL, such as texture uploads.
     *
     * @param task work to run on the client thread
     */
    public static void runOnClientThread(Runnable task) {
        if (isClientThread()) task.run();
        else post(task);
    }

    /**
     * Queues a task for the end of the current (or next) client tick.
     *
     * @param task work to run on the client thread
     */
    public static void post(Runnable task) {
        QUEUE.add(java.util.Objects.requireNonNull(task, "task"));
    }

    /**
     * Runs every task queued so far, in order. Tasks queued by a task wait for the next call.
     *
     * @return number of tasks run
     */
    static int drain() {
        int count = QUEUE.size();
        int run = 0;
        for (int i = 0; i < count; i++) {
            Runnable task = QUEUE.poll();
            if (task == null) break;
            run++;
            try {
                task.run();
            } catch (RuntimeException | LinkageError failure) {
                LOGGER.error("UniGUI client task failed", failure);
            }
        }
        return run;
    }

    /**
     * FML event handler that drains the queue every client tick.
     */
    public static final class Pump {
        @SubscribeEvent
        public void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase == TickEvent.Phase.END) drain();
        }
    }
}
