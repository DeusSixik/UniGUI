package dev.sixik.unigui.api.utils.consumers;

import java.util.Objects;

@FunctionalInterface
public interface Integer3Consumer {

    void accept(int value, int value2, int value3);

    default Integer3Consumer andThen(Integer3Consumer after) {
        Objects.requireNonNull(after);
        return (t, t2, t3) -> {
            this.accept(t, t2, t3);
            after.accept(t, t2, t3);
        };
    }
}
