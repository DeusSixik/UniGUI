package dev.sixik.unigui.cleanroom.backend;

import dev.sixik.unigui.backend.shared.KeyRepeat;

import java.util.function.BooleanSupplier;

/** Cleanroom compatibility facade for the shared repeat-state implementation. */
final class CleanroomKeyRepeat {
    interface Repeat {
        boolean enabled();
        void enabled(boolean enabled);
    }

    private final KeyRepeat delegate;

    CleanroomKeyRepeat(Repeat repeat) {
        delegate = new KeyRepeat(new KeyRepeat.Repeat() {
            public boolean enabled() { return repeat.enabled(); }
            public void enabled(boolean enabled) { repeat.enabled(enabled); }
        });
    }

    void update(boolean wanted) { delegate.update(wanted); }
    void release() { delegate.release(); }
    boolean owns() { return delegate.owns(); }

    static Repeat of(BooleanSupplier getter, java.util.function.Consumer<Boolean> setter) {
        KeyRepeat.Repeat repeat = KeyRepeat.of(getter, setter);
        return new Repeat() {
            public boolean enabled() { return repeat.enabled(); }
            public void enabled(boolean enabled) { repeat.enabled(enabled); }
        };
    }
}
