package dev.sixik.unigui.cleanroom.backend;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Registry of callbacks that say "an overlay covers the cursor here, the vanilla screen below must not react".
 *
 * <p>The vanilla container screen derives slot hover, tooltips and slot clicks from the cursor position, so
 * cancelling mouse events alone is not enough: the slot below an overlay panel would still highlight.</p>
 *
 * @param <S> screen type; {@code GuiScreen} in the game, anything in tests
 */
public final class CleanroomPointerBlockers<S> {
    /**
     * Decides whether the pointer at the given screen position is covered by an overlay.
     */
    @FunctionalInterface
    public interface Blocker<S> {
        /**
         * @param screen screen below the overlay
         * @param mouseX pointer X in scaled GUI pixels of that screen
         * @param mouseY pointer Y in scaled GUI pixels of that screen
         * @return {@code true} if the overlay covers the point
         */
        boolean blocksPointer(S screen, double mouseX, double mouseY);
    }

    private final List<Blocker<S>> blockers = new CopyOnWriteArrayList<>();

    /**
     * Registers a blocker.
     *
     * @return handle that removes the blocker when closed
     */
    public AutoCloseable add(Blocker<S> blocker) {
        Objects.requireNonNull(blocker, "blocker");
        blockers.add(blocker);
        return () -> blockers.remove(blocker);
    }

    /**
     * @return {@code true} if any registered blocker covers the point; always {@code false} without a screen
     */
    public boolean blocks(S screen, double mouseX, double mouseY) {
        if (screen == null) return false;
        for (Blocker<S> blocker : blockers) {
            if (blocker.blocksPointer(screen, mouseX, mouseY)) return true;
        }
        return false;
    }

    /**
     * Removes all blockers.
     */
    public void clear() {
        blockers.clear();
    }
}
