package dev.sixik.unigui.api.xml;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Вспомогательные методы файлового ввода-вывода для исходных XML-документов редактора виджетов.
 *
 * <p>Все операции используют UTF-8. Ошибки чтения и записи заворачиваются в
 * {@link XmlWidgetLoadException}, чтобы вызывающий код редактора работал с единым типом ошибки.</p>
 */
public final class XmlWidgetDocumentIo {
    private XmlWidgetDocumentIo() {
    }

    /**
     * Загружает XML-файл как строгий document tree.
     *
     * @param path путь к XML-файлу
     * @return parsed document
     */
    public static XmlWidgetDocument load(Path path) {
        return XmlWidgetDocument.parse(read(path));
    }

    /**
     * Загружает XML-файл в editor mode и выполняет validation.
     *
     * @param path путь к XML-файлу
     * @return document result с нефатальными diagnostics
     */
    public static XmlWidgetDocumentResult loadEditor(Path path) {
        return XmlWidgetDocument.parseEditor(read(path));
    }

    /**
     * Сохраняет документ с pretty-настройками по умолчанию.
     *
     * @param path путь назначения
     * @param document документ для сериализации
     * @return путь назначения
     */
    public static Path save(Path path, XmlWidgetDocument document) {
        return save(path, document, XmlWidgetSerializationOptions.PRETTY);
    }

    /**
     * Сохраняет документ с указанными настройками сериализации.
     *
     * <p>Родительские директории создаются автоматически.</p>
     *
     * @param path путь назначения
     * @param document документ для сериализации; не может быть {@code null}
     * @param options настройки serializer-а; {@code null} заменяется pretty defaults
     * @return путь назначения
     */
    public static Path save(Path path, XmlWidgetDocument document, XmlWidgetSerializationOptions options) {
        if (document == null) throw new IllegalArgumentException("XML widget document must not be null");
        write(path, document.toXmlString(options));
        return path;
    }

    private static String read(Path path) {
        if (path == null) throw new IllegalArgumentException("XML widget document path must not be null");
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new XmlWidgetLoadException("Cannot read XML widget document file '" + path + "'.", failure);
        }
    }

    private static void write(Path path, String xml) {
        if (path == null) throw new IllegalArgumentException("XML widget document path must not be null");
        try {
            Path parent = path.toAbsolutePath().getParent();
            if (parent != null) Files.createDirectories(parent);
            Files.writeString(path, xml, StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new XmlWidgetLoadException("Cannot write XML widget document file '" + path + "'.", failure);
        }
    }
}
