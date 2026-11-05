package dev.sixik.unigui.api.utils.consumers;

import java.util.Objects;

@FunctionalInterface
public interface Integer2Consumer {

    void accept(int value, int value2);

    default Integer2Consumer andThen(Integer2Consumer after) {
        Objects.requireNonNull(after);
        return (t, t2) -> {
            this.accept(t, t2);
            after.accept(t, t2);
        };
    }
}
