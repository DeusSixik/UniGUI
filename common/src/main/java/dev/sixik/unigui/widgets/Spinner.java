package dev.sixik.unigui.widgets;

/** Convenience alias for the default spinner-style loading indicator. */
public final class Spinner extends LoadingIndicator {
    public Spinner() {
        super();
        mode(Mode.SPINNER);
    }
}
