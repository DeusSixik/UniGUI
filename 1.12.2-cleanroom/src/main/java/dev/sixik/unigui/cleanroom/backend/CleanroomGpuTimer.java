package dev.sixik.unigui.cleanroom.backend;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.ARBTimerQuery;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL33;

/**
 * Measures how long the GPU takes for the UI passes of a frame with a {@code GL_TIME_ELAPSED} query.
 *
 * <p>The query result is read one frame later, when the driver has it ready, so reading never stalls the pipeline;
 * the figure is therefore the previous frame's. Only one query of this kind can be active at a time, so frames of
 * several screens and layers share one timer and a nested {@link #begin()} is refused. When neither OpenGL 3.3 nor
 * {@code ARB_timer_query} is present, or a call fails, the timer switches itself off and the overlay shows
 * "n/a". Client thread only.</p>
 */
final class CleanroomGpuTimer {
    private static final Logger LOGGER = LogManager.getLogger("unigui");
    private static boolean wanted;
    private static boolean unavailable;
    private static boolean active;
    private static boolean inFlight;
    private static boolean useArb;
    private static int query;
    private static float lastMillis = Float.NaN;

    private CleanroomGpuTimer() {
    }

    /**
     * @param on {@code true} while a profiler overlay wants the figure; the timer costs nothing when off
     */
    static void wanted(boolean on) {
        wanted = on;
    }

    /**
     * @return the GPU time of the last finished measurement in milliseconds, or {@code NaN} if there is none
     */
    static float lastMillis() {
        return lastMillis;
    }

    /**
     * Converts a query result.
     *
     * @param nanos elapsed GPU time in nanoseconds
     * @return milliseconds
     */
    static float nanosToMillis(long nanos) {
        return nanos / 1_000_000.0f;
    }

    /**
     * Starts timing.
     *
     * @return {@code true} if a measurement began and {@link #end()} must be called
     */
    static boolean begin() {
        if (!wanted || unavailable || active) return false;
        try {
            poll();
            if (inFlight) return false;
            if (query == 0) {
                if (!supported()) {
                    unavailable = true;
                    return false;
                }
                query = GL15.glGenQueries();
            }
            GL15.glBeginQuery(GL33.GL_TIME_ELAPSED, query);
            active = true;
            return true;
        } catch (RuntimeException | LinkageError failure) {
            disable(failure);
            return false;
        }
    }

    /**
     * Stops the measurement started by a successful {@link #begin()}.
     */
    static void end() {
        if (!active) return;
        active = false;
        try {
            GL15.glEndQuery(GL33.GL_TIME_ELAPSED);
            inFlight = true;
        } catch (RuntimeException | LinkageError failure) {
            disable(failure);
        }
    }

    private static void poll() {
        if (!inFlight) return;
        if (GL15.glGetQueryObjecti(query, GL15.GL_QUERY_RESULT_AVAILABLE) == 0) return;
        long nanos = useArb
                ? ARBTimerQuery.glGetQueryObjecti64(query, GL15.GL_QUERY_RESULT)
                : GL33.glGetQueryObjecti64(query, GL15.GL_QUERY_RESULT);
        lastMillis = nanosToMillis(nanos);
        inFlight = false;
    }

    private static boolean supported() {
        var capabilities = GL.getCapabilities();
        if (capabilities.OpenGL33) {
            useArb = false;
            return true;
        }
        if (capabilities.GL_ARB_timer_query) {
            useArb = true;
            return true;
        }
        return false;
    }

    private static void disable(Throwable failure) {
        unavailable = true;
        active = false;
        inFlight = false;
        lastMillis = Float.NaN;
        LOGGER.warn("UniGUI GPU timer disabled", failure);
    }
}