package dev.sixik.unigui.impl.text;

import dev.sixik.unigui.api.text.FontFace;
import dev.sixik.unigui.api.text.FontRegistry;

import java.awt.Font;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Default font registry. Font data is loaded from Java streams/files and does
 * not use Minecraft's font manager or resource manager.
 */
public final class DefaultFontRegistry implements FontRegistry, AutoCloseable {
    private static final DefaultFontRegistry GLOBAL = new DefaultFontRegistry();

    private final Map<String, FontFace> faces = new LinkedHashMap<>();
    private FontFace defaultFace;

    public static DefaultFontRegistry global() {
        return GLOBAL;
    }

    @Override
    public FontFace register(String id, Path path) throws IOException {
        Objects.requireNonNull(path, "path");
        try (InputStream input = Files.newInputStream(path)) {
            return register(id, input);
        }
    }

    @Override
    public FontFace register(String id, InputStream input) throws IOException {
        Objects.requireNonNull(input, "input");
        return register(id, input.readAllBytes());
    }

    @Override
    public FontFace register(String id, byte[] data) throws IOException {
        Objects.requireNonNull(data, "data");
        Font font;
        try (InputStream input = new ByteArrayInputStream(data)) {
            font = Font.createFont(Font.TRUETYPE_FONT, input);
        } catch (Exception first) {
            try (InputStream input = new ByteArrayInputStream(data)) {
                font = Font.createFont(Font.TYPE1_FONT, input);
            } catch (Exception second) {
                IOException failure = new IOException("Unsupported font data", first);
                failure.addSuppressed(second);
                throw failure;
            }
        }

        AwtFontFace face = new AwtFontFace(normalizeId(id), font);
        faces.put(face.id(), face);
        if (defaultFace == null) defaultFace = face;
        return face;
    }

    public FontFace registerSystem(String id, String family) {
        String normalizedId = normalizeId(id);
        String normalizedFamily = family == null || family.isBlank() ? "SansSerif" : family;
        AwtFontFace face = new AwtFontFace(normalizedId, new Font(normalizedFamily, Font.PLAIN, 16));
        faces.put(normalizedId, face);
        if (defaultFace == null) defaultFace = face;
        return face;
    }

    @Override
    public FontFace find(String id) {
        return faces.get(id);
    }

    @Override
    public FontFace defaultFace() {
        if (defaultFace == null) defaultFace = registerSystem("default", "SansSerif");
        return defaultFace;
    }

    @Override
    public void defaultFace(FontFace face) {
        if (face != null) defaultFace = face;
    }

    @Override
    public Map<String, FontFace> faces() {
        return Collections.unmodifiableMap(faces);
    }

    @Override
    public void close() {
        faces.clear();
        defaultFace = null;
    }

    private static String normalizeId(String id) {
        return id == null || id.isBlank() ? "font" : id;
    }
}
