package dev.sixik.unigui.backend.shared;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/** Platform-independent registry for overlay regions that block pointer interaction below them. */
public final class PointerBlockers<S> {
    @FunctionalInterface
    public interface Blocker<S> {
        boolean blocksPointer(S screen, double mouseX, double mouseY);
    }

    private final List<Blocker<S>> blockers = new CopyOnWriteArrayList<>();

    public AutoCloseable add(Blocker<S> blocker) {
        Objects.requireNonNull(blocker, "blocker");
        blockers.add(blocker);
        return () -> blockers.remove(blocker);
    }

    public boolean blocks(S screen, double mouseX, double mouseY) {
        if (screen == null) return false;
        for (Blocker<S> blocker : blockers) {
            if (blocker.blocksPointer(screen, mouseX, mouseY)) return true;
        }
        return false;
    }

    public void clear() {
        blockers.clear();
    }
}
