package dev.sixik.unigui.api.utils.consumers.basic;

import java.util.Objects;

@FunctionalInterface
public interface IntegerConsumer {

    void accept(int value);

    default IntegerConsumer andThen(IntegerConsumer after) {
        Objects.requireNonNull(after);
        return (t) -> {
            this.accept(t);
            after.accept(t);
        };
    }
}
