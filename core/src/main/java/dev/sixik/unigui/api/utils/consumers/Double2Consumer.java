package dev.sixik.unigui.api.utils.consumers;

import java.util.Objects;

@FunctionalInterface
public interface Double2Consumer {

    void accept(double value, double value2);

    default Double2Consumer andThen(Double2Consumer after) {
        Objects.requireNonNull(after);
        return (t, t2) -> {
            this.accept(t, t2);
            after.accept(t, t2);
        };
    }
}
