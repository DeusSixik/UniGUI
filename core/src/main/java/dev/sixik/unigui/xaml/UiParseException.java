package dev.sixik.unigui.xaml;

/**
 * Ошибка чтения документа разметки с позицией в исходном тексте.
 */
public final class UiParseException extends RuntimeException {
    private final SourceLocation location;
    private final String reason;

    /**
     * Создаёт ошибку.
     *
     * @param reason   причина без позиции
     * @param location позиция ошибки
     */
    public UiParseException(String reason, SourceLocation location) {
        super(reason + (location != null && location.known() ? " (at " + location + ")" : ""));
        this.reason = reason;
        this.location = location == null ? SourceLocation.UNKNOWN : location;
    }

    /**
     * Возвращает позицию ошибки.
     *
     * @return позиция или {@link SourceLocation#UNKNOWN}
     */
    public SourceLocation location() {
        return location;
    }

    /**
     * Возвращает причину без приписки о позиции.
     *
     * @return текст причины
     */
    public String reason() {
        return reason;
    }
}
