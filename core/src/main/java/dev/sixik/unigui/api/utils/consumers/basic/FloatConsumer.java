package dev.sixik.unigui.api.utils.consumers.basic;

import java.util.Objects;

@FunctionalInterface
public interface FloatConsumer {

    void accept(float value);

    default FloatConsumer andThen(FloatConsumer after) {
        Objects.requireNonNull(after);
        return (t) -> {
            this.accept(t);
            after.accept(t);
        };
    }
}
