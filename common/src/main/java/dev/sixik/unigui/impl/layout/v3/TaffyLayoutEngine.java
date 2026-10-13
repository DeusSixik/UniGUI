package dev.sixik.unigui.impl.layout.v3;

/**
 * Backwards-compatible alias for {@link WebLayoutEngine}.
 *
 * @deprecated the old name suggested a real Taffy binding, but the engine is a pure-Java
 * implementation owned by UniGUI. Use {@link WebLayoutEngine} instead.
 */
@Deprecated(forRemoval = false)
public final class TaffyLayoutEngine {
    /**
     * Общий экземпляр движка, см. {@link WebLayoutEngine#INSTANCE}.
     *
     * @deprecated используйте {@link WebLayoutEngine#INSTANCE}.
     */
    @Deprecated(forRemoval = false)
    public static final WebLayoutEngine INSTANCE = WebLayoutEngine.INSTANCE;

    private TaffyLayoutEngine() {
    }
}
