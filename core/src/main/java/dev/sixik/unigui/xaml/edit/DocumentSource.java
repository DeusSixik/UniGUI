package dev.sixik.unigui.xaml.edit;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Откуда редактор берёт документ и куда его сохраняет: память, файл, архив ресурсов.
 *
 * <p>Сессия не знает, где лежит документ, и потому одинаково работает с файлом на диске и с документом,
 * живущим только в памяти (например, во вкладке игры без файла).</p>
 */
public interface DocumentSource {
    /**
     * @return стабильный идентификатор источника
     */
    String id();

    /**
     * @return заголовок для вкладки
     */
    String title();

    /**
     * @return текущее сохранённое содержимое
     */
    String readText();

    /**
     * @return {@code true}, если источник принимает запись
     */
    boolean writable();

    /**
     * Сохраняет содержимое.
     *
     * @param text новый текст
     * @throws UnsupportedOperationException если источник только для чтения
     */
    void writeText(String text);

    /**
     * Документ в памяти: запись заменяет хранимый текст.
     *
     * @param id    идентификатор
     * @param title заголовок
     * @param text  начальный текст
     * @return изменяемый источник
     */
    static DocumentSource memory(String id, String title, String text) {
        return new DocumentSource() {
            private String stored = text == null ? "" : text;

            @Override
            public String id() {
                return id;
            }

            @Override
            public String title() {
                return title;
            }

            @Override
            public synchronized String readText() {
                return stored;
            }

            @Override
            public boolean writable() {
                return true;
            }

            @Override
            public synchronized void writeText(String next) {
                stored = next == null ? "" : next;
            }
        };
    }

    /**
     * Файл в кодировке UTF-8.
     *
     * @param path путь к файлу
     * @return источник; чтение и запись выполняются при каждом обращении
     */
    static DocumentSource file(Path path) {
        return new DocumentSource() {
            @Override
            public String id() {
                return path.toAbsolutePath().toString();
            }

            @Override
            public String title() {
                return path.getFileName() == null ? path.toString() : path.getFileName().toString();
            }

            @Override
            public String readText() {
                try {
                    return Files.readString(path, StandardCharsets.UTF_8);
                } catch (IOException failure) {
                    throw new IllegalStateException("Cannot read " + path, failure);
                }
            }

            @Override
            public boolean writable() {
                return !Files.exists(path) || Files.isWritable(path);
            }

            @Override
            public void writeText(String text) {
                try {
                    Files.writeString(path, text == null ? "" : text, StandardCharsets.UTF_8);
                } catch (IOException failure) {
                    throw new IllegalStateException("Cannot write " + path, failure);
                }
            }
        };
    }
}
