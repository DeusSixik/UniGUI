package dev.sixik.unigui.api.text;

/**
 * Scope активного {@link InlineContentResolver}.
 *
 * <p>Объект возвращается из {@link InlineContentResolvers#push(InlineContentResolver)} и должен
 * закрываться через try-with-resources. При закрытии восстанавливается предыдущий resolver,
 * поэтому вложенные XML-load или demo-блоки не протекают в соседние виджеты.</p>
 *
 * <pre>{@code
 * try (InlineContentResolverScope ignored = InlineContentResolvers.push(myResolver)) {
 *     Label label = new Label("Cost: {item:minecraft:diamond}");
 * }
 * }</pre>
 */
public final class InlineContentResolverScope implements AutoCloseable {
    private final InlineContentResolver previous;
    private boolean closed;

    InlineContentResolverScope(InlineContentResolver previous) {
        this.previous = previous;
    }

    /**
     * Закрывает scope и восстанавливает resolver, который был активен до {@code push(...)}.
     */
    @Override
    public void close() {
        if (closed) return;
        closed = true;
        InlineContentResolvers.restore(previous);
    }
}
