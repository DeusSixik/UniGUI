package dev.sixik.unigui.api.backend;

import dev.sixik.unigui.api.screen.UniScreen;
import dev.sixik.unigui.api.screen.UniRenderLayer;
import dev.sixik.unigui.api.screen.UniRenderTarget;

import java.util.function.Predicate;

/** Runtime bridge implemented by the active game-version backend. */
public interface UniGuiBackend {
    /** Opens the native screen represented by the supplied UniGUI screen. */
    void openScreen(UniScreen screen);

    /** Closes the native screen represented by the supplied UniGUI screen, if open. */
    default void closeScreen(UniScreen screen) {
    }

    /** Registers a neutral layer at a runtime rendering hook. */
    default LayerHandle registerLayer(UniRenderLayer layer, UniRenderTarget target,
                                      Predicate<Object> visibility, int priority) {
        throw new UnsupportedOperationException("Render layer registration is not supported by this backend");
    }

    interface LayerHandle extends AutoCloseable {
        boolean enabled();
        void enabled(boolean enabled);
        int priority();
        void priority(int priority);
        void visibleWhen(Predicate<Object> visibility);
        boolean closed();
        @Override void close();
    }
}
