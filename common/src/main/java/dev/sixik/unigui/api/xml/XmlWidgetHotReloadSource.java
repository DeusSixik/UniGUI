package dev.sixik.unigui.api.xml;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * Абстракция источника XML для editor/runtime hot-reload preview.
 *
 * <p>Источник умеет вернуть человекочитаемую подпись, актуальный XML-текст и числовую версию.
 * Preview сравнивает версии и перезагружает XML только когда версия изменилась.</p>
 */
public interface XmlWidgetHotReloadSource {
    /**
     * Возвращает подпись источника для статусов и diagnostics.
     *
     * @return label файла, resource-а или пользовательского источника
     */
    String label();

    /**
     * Читает актуальный XML source.
     *
     * @return XML-текст
     */
    String read();

    /**
     * Возвращает версию источника для обнаружения изменений.
     *
     * @return числовая версия; должна меняться при изменении XML
     */
    long version();

    /**
     * Создаёт произвольный hot-reload source из callbacks.
     *
     * @param label подпись источника; blank значение заменяется дефолтным
     * @param reader callback чтения XML; не может быть {@code null}
     * @param version callback версии; {@code null} заменяется {@link System#nanoTime()}
     * @return hot-reload source
     */
    static XmlWidgetHotReloadSource of(String label, Supplier<String> reader, LongSupplier version) {
        if (reader == null) throw new IllegalArgumentException("XML hot reload source reader must not be null");
        LongSupplier normalizedVersion = version == null ? System::nanoTime : version;
        String normalizedLabel = label == null || label.isBlank() ? "XML source" : label.trim();
        return new XmlWidgetHotReloadSource() {
            @Override
            public String label() {
                return normalizedLabel;
            }

            @Override
            public String read() {
                return reader.get();
            }

            @Override
            public long version() {
                return normalizedVersion.getAsLong();
            }
        };
    }

    /**
     * Создаёт source для файла на диске.
     *
     * <p>Версия строится из last modified time и размера файла.</p>
     *
     * @param path путь к XML-файлу
     * @return file hot-reload source
     */
    static XmlWidgetHotReloadSource path(Path path) {
        if (path == null) throw new IllegalArgumentException("XML hot reload path must not be null");
        Path normalized = path.toAbsolutePath().normalize();
        return of(normalized.toString(),
                () -> readPath(normalized),
                () -> pathVersion(normalized));
    }

    /**
     * Создаёт source для classpath resource.
     *
     * @param resourcePath путь resource-а, с ведущим {@code /} или без него
     * @return resource hot-reload source
     */
    static XmlWidgetHotReloadSource resource(String resourcePath) {
        if (resourcePath == null || resourcePath.isBlank()) {
            throw new IllegalArgumentException("XML hot reload resource path must not be blank");
        }
        String normalized = resourcePath.startsWith("/") ? resourcePath.substring(1) : resourcePath;
        return of(resourcePath,
                () -> readResource(normalized, resourcePath),
                () -> resourceVersion(normalized, resourcePath));
    }

    private static String readPath(Path path) {
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new XmlWidgetLoadException("Cannot read XML hot reload file '" + path + "'.", failure);
        }
    }

    private static long pathVersion(Path path) {
        try {
            if (!Files.exists(path)) return Long.MIN_VALUE;
            long modified = Files.getLastModifiedTime(path).toMillis();
            long size = Files.size(path);
            return modified * 31L + size;
        } catch (IOException failure) {
            throw new XmlWidgetLoadException("Cannot inspect XML hot reload file '" + path + "'.", failure);
        }
    }

    private static String readResource(String normalizedPath, String displayPath) {
        URL url = resourceUrl(normalizedPath, displayPath);
        try (InputStream stream = url.openStream()) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new XmlWidgetLoadException("Cannot read XML hot reload resource '" + displayPath + "'.", failure);
        }
    }

    private static long resourceVersion(String normalizedPath, String displayPath) {
        URL url = resourceUrl(normalizedPath, displayPath);
        try {
            URLConnection connection = url.openConnection();
            long modified = connection.getLastModified();
            long size = connection.getContentLengthLong();
            return modified * 31L + size;
        } catch (IOException failure) {
            throw new XmlWidgetLoadException("Cannot inspect XML hot reload resource '" + displayPath + "'.", failure);
        }
    }

    private static URL resourceUrl(String normalizedPath, String displayPath) {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader == null) loader = XmlWidgetHotReloadSource.class.getClassLoader();
        URL url = loader.getResource(normalizedPath);
        if (url == null && loader != XmlWidgetHotReloadSource.class.getClassLoader()) {
            url = XmlWidgetHotReloadSource.class.getClassLoader().getResource(normalizedPath);
        }
        if (url == null) {
            throw new XmlWidgetLoadException("XML hot reload resource '" + displayPath + "' was not found.");
        }
        return url;
    }
}
