package dev.sixik.unigui.api.xml;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Вспомогательные методы файлового ввода-вывода для исходных XML-документов редактора виджетов. */
public final class XmlWidgetDocumentIo {
    private XmlWidgetDocumentIo() {
    }

    public static XmlWidgetDocument load(Path path) {
        return XmlWidgetDocument.parse(read(path));
    }

    public static XmlWidgetDocumentResult loadEditor(Path path) {
        return XmlWidgetDocument.parseEditor(read(path));
    }

    public static Path save(Path path, XmlWidgetDocument document) {
        return save(path, document, XmlWidgetSerializationOptions.PRETTY);
    }

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
