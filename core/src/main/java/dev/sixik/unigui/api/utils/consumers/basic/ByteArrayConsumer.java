package dev.sixik.unigui.api.utils.consumers.basic;

import java.util.Objects;

@FunctionalInterface
public interface ByteArrayConsumer {

    void accept(byte[] value);

    default ByteArrayConsumer andThen(ByteArrayConsumer after) {
        Objects.requireNonNull(after);
        return (t) -> {
            this.accept(t);
            after.accept(t);
        };
    }
}
