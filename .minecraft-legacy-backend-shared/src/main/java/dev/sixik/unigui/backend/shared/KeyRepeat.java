package dev.sixik.unigui.backend.shared;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/** Coordinates ownership of a process-wide keyboard repeat flag. */
public final class KeyRepeat {
    public interface Repeat {
        boolean enabled();

        void enabled(boolean enabled);
    }

    private final Repeat repeat;
    private boolean ownedByUs;

    public KeyRepeat(Repeat repeat) {
        this.repeat = repeat;
    }

    public void update(boolean wanted) {
        if (wanted) {
            if (!ownedByUs && !repeat.enabled()) {
                repeat.enabled(true);
                ownedByUs = true;
            }
        } else {
            release();
        }
    }

    public void release() {
        if (!ownedByUs) return;
        ownedByUs = false;
        repeat.enabled(false);
    }

    public boolean owns() {
        return ownedByUs;
    }

    public static Repeat of(BooleanSupplier getter, Consumer<Boolean> setter) {
        return new Repeat() {
            @Override
            public boolean enabled() {
                return getter.getAsBoolean();
            }

            @Override
            public void enabled(boolean enabled) {
                setter.accept(enabled);
            }
        };
    }
}
