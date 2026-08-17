package dev.sixik.unigui.api.xml.editor;

import dev.sixik.unigui.api.xml.XmlWidgetLoadException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/** Factories for common XML editor document sources. */
public final class XmlEditorDocumentSources {
    private XmlEditorDocumentSources() {
    }

    public static XmlEditorDocumentSource memory(String id, String displayName, String initialText) {
        return new MemorySource(id, displayName, initialText);
    }

    public static XmlEditorDocumentSource file(Path path) {
        return file(path, null);
    }

    public static XmlEditorDocumentSource file(Path path, String displayName) {
        return new FileSource(path, displayName);
    }

    public static XmlEditorDocumentSource resource(String resourcePath) {
        return resource(resourcePath, null, null);
    }

    public static XmlEditorDocumentSource resource(String resourcePath, String displayName, ClassLoader loader) {
        return new ResourceSource(resourcePath, displayName, loader);
    }

    private static String normalize(String value, String fallback) {
        String normalized = value == null ? "" : value.trim();
        return normalized.isEmpty() ? fallback : normalized;
    }

    private static final class MemorySource implements XmlEditorDocumentSource {
        private final String id;
        private final String displayName;
        private String text;

        private MemorySource(String id, String displayName, String initialText) {
            this.id = normalize(id, "memory");
            this.displayName = normalize(displayName, this.id);
            this.text = initialText == null ? "" : initialText;
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public String displayName() {
            return displayName;
        }

        @Override
        public boolean writable() {
            return true;
        }

        @Override
        public String readText() {
            return text;
        }

        @Override
        public void writeText(String text) {
            this.text = text == null ? "" : text;
        }
    }

    private static final class FileSource implements XmlEditorDocumentSource {
        private final Path path;
        private final String displayName;

        private FileSource(Path path, String displayName) {
            this.path = Objects.requireNonNull(path, "path");
            this.displayName = normalize(displayName, path.getFileName() == null ? path.toString() : path.getFileName().toString());
        }

        @Override
        public String id() {
            return path.toAbsolutePath().normalize().toString();
        }

        @Override
        public String displayName() {
            return displayName;
        }

        @Override
        public boolean writable() {
            return true;
        }

        @Override
        public String readText() {
            try {
                return Files.readString(path, StandardCharsets.UTF_8);
            } catch (IOException failure) {
                throw new XmlWidgetLoadException("Cannot read XML editor source '" + path + "'.", failure);
            }
        }

        @Override
        public void writeText(String text) {
            try {
                Path parent = path.toAbsolutePath().getParent();
                if (parent != null) Files.createDirectories(parent);
                Files.writeString(path, text == null ? "" : text, StandardCharsets.UTF_8);
            } catch (IOException failure) {
                throw new XmlWidgetLoadException("Cannot write XML editor source '" + path + "'.", failure);
            }
        }
    }

    private static final class ResourceSource implements XmlEditorDocumentSource {
        private final String resourcePath;
        private final String displayName;
        private final ClassLoader loader;

        private ResourceSource(String resourcePath, String displayName, ClassLoader loader) {
            this.resourcePath = normalize(resourcePath, "");
            if (this.resourcePath.isEmpty()) {
                throw new IllegalArgumentException("XML editor resource path must not be blank");
            }
            this.displayName = normalize(displayName, this.resourcePath);
            ClassLoader contextLoader = Thread.currentThread().getContextClassLoader();
            this.loader = loader != null ? loader : contextLoader != null ? contextLoader : XmlEditorDocumentSources.class.getClassLoader();
        }

        @Override
        public String id() {
            return "resource:" + resourcePath;
        }

        @Override
        public String displayName() {
            return displayName;
        }

        @Override
        public boolean writable() {
            return false;
        }

        @Override
        public String readText() {
            String normalized = resourcePath.startsWith("/") ? resourcePath.substring(1) : resourcePath;
            try (InputStream stream = loader.getResourceAsStream(normalized)) {
                if (stream == null) {
                    throw new XmlWidgetLoadException("XML editor resource '" + resourcePath + "' was not found.");
                }
                return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException failure) {
                throw new XmlWidgetLoadException("Cannot read XML editor resource '" + resourcePath + "'.", failure);
            }
        }

        @Override
        public void writeText(String text) {
            throw new XmlWidgetLoadException("XML editor resource '" + resourcePath + "' is read-only.");
        }
    }
}
