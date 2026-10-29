package dev.sixik.unigui.api.core;

/**
 * Bit flags, которыми виджет сообщает runtime, какие части состояния нужно пересчитать.
 *
 * <p>Инвалидация разделена по стоимости. Изменение текста или размеров обычно требует layout,
 * изменение цвета - только visual, а обновление texture/atlas - texture path. Флаги можно
 * комбинировать через bitwise OR.</p>
 *
 * <pre>{@code
 * widget.invalidate(InvalidationFlags.LAYOUT | InvalidationFlags.VISUAL);
 * if (InvalidationFlags.has(widget.invalidationFlags(), InvalidationFlags.LAYOUT)) {
 *     // Нужно повторить measure/arrange.
 * }
 * }</pre>
 */
public final class InvalidationFlags {
    /** Нет запрошенных пересчётов. */
    public static final int NONE = 0;

    /** Нужно повторить measure/arrange для виджета или subtree. */
    public static final int LAYOUT = 1;

    /** Нужно пересобрать визуальное представление без обязательного layout. */
    public static final int VISUAL = 1 << 1;

    /** Нужно обновить texture-dependent ресурсы или cached texture state. */
    public static final int TEXTURE = 1 << 2;

    /** Полная инвалидация layout, visual и texture path. */
    public static final int ALL = LAYOUT | VISUAL | TEXTURE;

    private InvalidationFlags() {
    }

    /**
     * Проверяет, установлен ли конкретный флаг в наборе.
     *
     * @param flags набор bit flags
     * @param flag проверяемый флаг или комбинация флагов
     * @return {@code true}, если все биты {@code flag} присутствуют в {@code flags}
     */
    public static boolean has(int flags, int flag) {
        return (flags & flag) == flag;
    }
}