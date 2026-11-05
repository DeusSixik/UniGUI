package dev.sixik.unigui.xaml;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Значение свойства в языково-нейтральном дереве разметки.
 *
 * <p>Значение — это либо строковый литерал, либо markup extension ({@code {Binding Path=Health}}), либо
 * вложенные элементы (форма {@code <Button.Content>...</Button.Content>}). Разбор литерала в конкретный тип
 * (число, цвет, enum) здесь не выполняется: это работа схемы, а не формата.</p>
 */
public sealed interface UiValue {
    /**
     * Строковый литерал, как он записан в документе.
     *
     * @param text текст без обрамляющих кавычек и без экранирования формата
     */
    record Literal(String text) implements UiValue {
        public Literal {
            text = text == null ? "" : text;
        }
    }

    /**
     * Markup extension: {@code {Name positional, key=value}}.
     *
     * @param name       имя расширения ({@code Binding}, {@code x:Static})
     * @param positional позиционные аргументы по порядку
     * @param named      именованные аргументы в порядке записи
     */
    record Extension(UiName name, List<UiValue> positional, Map<String, UiValue> named) implements UiValue {
        public Extension {
            if (name == null) throw new IllegalArgumentException("Extension name must not be null");
            positional = positional == null ? List.of() : List.copyOf(positional);
            named = named == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(named));
        }

        /**
         * Возвращает аргумент по имени, а при его отсутствии — позиционный аргумент с указанным индексом.
         *
         * @param key           имя именованного аргумента
         * @param positionalIdx индекс запасного позиционного аргумента
         * @return значение или {@code null}
         */
        public UiValue argument(String key, int positionalIdx) {
            UiValue value = named.get(key);
            if (value != null) return value;
            return positionalIdx >= 0 && positionalIdx < positional.size() ? positional.get(positionalIdx) : null;
        }
    }

    /**
     * Вложенное содержимое property-элемента: узлы и комментарии.
     *
     * @param content упорядоченное содержимое
     */
    record Elements(List<UiContent> content) implements UiValue {
        public Elements {
            content = content == null ? List.of() : List.copyOf(content);
        }
    }
}
