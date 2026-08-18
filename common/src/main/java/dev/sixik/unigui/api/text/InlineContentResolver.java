package dev.sixik.unigui.api.text;

/**
 * Преобразует строку в {@link RichText}, опционально заменяя marker'ы на inline-content.
 *
 * <p>Виджеты не знают конкретный синтаксис marker'ов. Они вызывают {@link RichText#resolve(String)},
 * а активный resolver задаётся внешним кодом через {@link InlineContentResolvers#push(InlineContentResolver)}.
 * Благодаря этому мод или экран может зарегистрировать свой формат, например {@code {item:...}}
 * или {@code {icon:...}}, без хардкода в базовых widgets.</p>
 *
 * @see InlineContentResolvers
 * @see InlineContentSpan
 */
@FunctionalInterface
public interface InlineContentResolver {
    /** Resolver по умолчанию: строка остаётся обычным plain {@link RichText}. */
    InlineContentResolver PLAIN = RichText::plain;

    /**
     * Разбирает строку в rich text.
     *
     * @param text исходная строка; {@code null} трактуется реализацией resolver'а
     * @return rich text с обычными text-run'ами и, при необходимости, inline-span'ами
     */
    RichText resolve(String text);
}
