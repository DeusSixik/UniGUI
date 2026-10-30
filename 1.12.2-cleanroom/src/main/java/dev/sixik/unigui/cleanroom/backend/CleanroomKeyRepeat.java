package dev.sixik.unigui.cleanroom.backend;

import java.util.function.BooleanSupplier;

/**
 * Turns the keyboard auto-repeat of the window on while an overlay text field has focus and gives it back afterwards.
 *
 * <p>1.12.2 delivers held-key repeats only after {@code Keyboard.enableRepeatEvents(true)}; vanilla screens that
 * need it switch it on in {@code initGui} and off in {@code onGuiClosed}. An overlay on top of such a screen must
 * neither disable the repeat the screen asked for nor leave it enabled on a screen that did not.</p>
 */
final class CleanroomKeyRepeat {
    /** Access to the global repeat flag; replaced by a fake in tests. */
    interface Repeat {
        boolean enabled();

        void enabled(boolean enabled);
    }

    private final Repeat repeat;
    private boolean ownedByUs;

    CleanroomKeyRepeat(Repeat repeat) {
        this.repeat = repeat;
    }

    /**
     * Requests or releases repeat events.
     *
     * @param wanted {@code true} while a text field wants held-key repeats
     */
    void update(boolean wanted) {
        if (wanted) {
            if (!ownedByUs && !repeat.enabled()) {
                repeat.enabled(true);
                ownedByUs = true;
            }
        } else {
            release();
        }
    }

    /** Switches repeats back off if this object was the one that switched them on. */
    void release() {
        if (!ownedByUs) return;
        ownedByUs = false;
        repeat.enabled(false);
    }

    /** @return {@code true} while this object holds the repeat flag */
    boolean owns() {
        return ownedByUs;
    }

    /** Convenience for building the global accessor from a getter and a setter. */
    static Repeat of(BooleanSupplier getter, java.util.function.Consumer<Boolean> setter) {
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
