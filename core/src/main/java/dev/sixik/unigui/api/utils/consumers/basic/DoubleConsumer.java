package dev.sixik.unigui.api.utils.consumers.basic;

import java.util.Objects;

@FunctionalInterface
public interface DoubleConsumer {

    void accept(double value);

    default DoubleConsumer andThen(DoubleConsumer after) {
        Objects.requireNonNull(after);
        return (t) -> {
            this.accept(t);
            after.accept(t);
        };
    }
}
