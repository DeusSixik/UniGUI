package dev.sixik.unigui.api.utils.consumers.basic;

import java.util.Objects;

@FunctionalInterface
public interface BooleanConsumer {

    void accept(boolean value);

    default BooleanConsumer andThen(BooleanConsumer after) {
        Objects.requireNonNull(after);
        return (t) -> {
            this.accept(t);
            after.accept(t);
        };
    }
}
