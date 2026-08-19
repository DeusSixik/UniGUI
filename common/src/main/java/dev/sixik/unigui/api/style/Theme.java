package dev.sixik.unigui.api.style;

/**
 * Источник стилей для дерева виджетов.
 *
 * <p>{@code Theme} отвечает на вопрос: какой {@link Style} использовать для конкретного
 * типа виджета. Простой {@link MutableTheme} работает как карта {@code widgetType -> Style},
 * а {@link StylePack} добавляет selector'ы, style id, class'ы, renderer id и animation preset'ы.</p>
 *
 * @see StyleIds.Widget
 * @see MutableTheme
 * @see StylePack
 */
public interface Theme {
    /** Wildcard-ключ для fallback-стиля, применимого к любому типу виджета. */
    String WILDCARD = "*";

    /** Пустая тема: любой тип виджета получает {@link Style#EMPTY}. */
    Theme EMPTY = widgetType -> Style.EMPTY;

    /**
     * Возвращает версию темы для кэширования style lookup.
     *
     * @return версия темы и вложенных стилей
     */
    default long version() {
        return 0L;
    }

    /**
     * Возвращает стиль для типа виджета.
     *
     * @param widgetType id типа виджета, обычно из {@link StyleIds.Widget}
     * @return стиль для типа или fallback-стиль
     */
    Style styleFor(String widgetType);
}