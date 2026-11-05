package dev.sixik.unigui.api.utils.consumers;

import java.util.Objects;

@FunctionalInterface
public interface Float2Consumer {

    void accept(float value, float value2);

    default Float2Consumer andThen(Float2Consumer after) {
        Objects.requireNonNull(after);
        return (t, t2) -> {
            this.accept(t, t2);
            after.accept(t, t2);
        };
    }
}
