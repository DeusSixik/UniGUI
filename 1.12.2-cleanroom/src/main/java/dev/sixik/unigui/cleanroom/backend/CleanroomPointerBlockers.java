package dev.sixik.unigui.cleanroom.backend;

import dev.sixik.unigui.backend.shared.PointerBlockers;

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

    private final PointerBlockers<S> delegate = new PointerBlockers<>();

    /**
     * Registers a blocker.
     *
     * @return handle that removes the blocker when closed
     */
    public AutoCloseable add(Blocker<S> blocker) {
        return delegate.add(blocker::blocksPointer);
    }

    /**
     * @return {@code true} if any registered blocker covers the point; always {@code false} without a screen
     */
    public boolean blocks(S screen, double mouseX, double mouseY) {
        return delegate.blocks(screen, mouseX, mouseY);
    }

    /**
     * Removes all blockers.
     */
    public void clear() {
        delegate.clear();
    }
}
