package dev.sixik.unigui.xaml.format;

import dev.sixik.unigui.xaml.UiDocument;
import dev.sixik.unigui.xaml.UiParseException;

/**
 * Текстовый формат, который умеет превращать исходник в {@link UiDocument} и обратно.
 *
 * <p>Ядро работает только с деревом разметки. XAML — первая реализация ({@link XamlFormat}); Java-билдеры,
 * KubeJS и CraftTweaker подключаются как другие {@code UiFormat}, не меняя схему, загрузчик и редактор.</p>
 */
public interface UiFormat {
    /**
     * Короткий идентификатор формата.
     *
     * @return например {@code "xaml"}
     */
    String id();

    /**
     * Читает документ.
     *
     * @param source  исходный текст
     * @param options настройки чтения
     * @return дерево разметки
     * @throws UiParseException если текст некорректен
     */
    UiDocument read(CharSequence source, ReadOptions options);

    /**
     * Читает документ с настройками по умолчанию.
     *
     * @param source исходный текст
     * @return дерево разметки
     * @throws UiParseException если текст некорректен
     */
    default UiDocument read(CharSequence source) {
        return read(source, ReadOptions.DEFAULT);
    }

    /**
     * Записывает документ.
     *
     * @param document документ
     * @param options  настройки записи
     * @return текст в этом формате
     */
    String write(UiDocument document, WriteOptions options);

    /**
     * Записывает документ с настройками по умолчанию.
     *
     * @param document документ
     * @return текст в этом формате
     */
    default String write(UiDocument document) {
        return write(document, WriteOptions.PRETTY);
    }

    /**
     * Настройки чтения.
     *
     * @param maxDepth максимальная глубина вложенности элементов (защита от переполнения стека)
     */
    record ReadOptions(int maxDepth) {
        /** Значения по умолчанию: глубина 128, как у прежнего загрузчика. */
        public static final ReadOptions DEFAULT = new ReadOptions(128);

        public ReadOptions {
            if (maxDepth < 1) throw new IllegalArgumentException("maxDepth must be positive");
        }
    }

    /**
     * Настройки записи.
     *
     * @param indent         число пробелов на уровень вложенности
     * @param declaration    писать ли {@code <?xml ...?>}
     * @param maxLineLength  длина строки, после которой атрибуты переносятся по одному на строку
     */
    record WriteOptions(int indent, boolean declaration, int maxLineLength) {
        /** Читаемый вывод с отступом 4 и переносом длинных тегов. */
        public static final WriteOptions PRETTY = new WriteOptions(4, false, 100);

        public WriteOptions {
            if (indent < 0) throw new IllegalArgumentException("indent must not be negative");
            if (maxLineLength < 1) throw new IllegalArgumentException("maxLineLength must be positive");
        }
    }
}
