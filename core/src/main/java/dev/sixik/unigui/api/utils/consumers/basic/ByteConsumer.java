package dev.sixik.unigui.api.utils.consumers.basic;

import java.util.Objects;

@FunctionalInterface
public interface ByteConsumer {

    void accept(byte value);

    default ByteConsumer andThen(ByteConsumer after) {
        Objects.requireNonNull(after);
        return (t) -> {
            this.accept(t);
            after.accept(t);
        };
    }
}
