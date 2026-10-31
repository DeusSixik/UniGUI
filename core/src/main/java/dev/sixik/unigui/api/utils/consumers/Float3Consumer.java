package dev.sixik.unigui.api.utils.consumers;

import java.util.Objects;

@FunctionalInterface
public interface Float3Consumer {

    void accept(float value, float value2, float value3);

    default Float3Consumer andThen(Float3Consumer after) {
        Objects.requireNonNull(after);
        return (t, t2, t3) -> {
            this.accept(t, t2, t3);
            after.accept(t, t2, t3);
        };
    }
}
