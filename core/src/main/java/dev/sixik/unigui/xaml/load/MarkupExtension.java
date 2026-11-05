package dev.sixik.unigui.xaml.load;

import dev.sixik.unigui.xaml.UiValue;

/**
 * Расширение разметки вида {@code {Name arg, Key=value}}: вычисляет значение свойства.
 *
 * <p>Расширение получает целевое свойство и само решает, как его заполнить: подставить текст
 * ({@code StaticResource}), создать подписку ({@code Binding}) и т. п. Бросать следует
 * {@link IllegalArgumentException} с понятным сообщением: загрузчик добавит к нему имя свойства и позицию.</p>
 */
@FunctionalInterface
public interface MarkupExtension {
    /**
     * Применяет расширение к свойству.
     *
     * @param context контекст загрузки
     * @param use     использование расширения с аргументами
     * @param target  свойство, в которое нужно записать результат
     * @throws IllegalArgumentException если аргументы некорректны или значение вычислить нельзя
     */
    void apply(ExtensionContext context, UiValue.Extension use, PropertyTarget target);
}
