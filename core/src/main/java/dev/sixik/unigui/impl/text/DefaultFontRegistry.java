package dev.sixik.unigui.impl.text;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;
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
@AiDoc(
        summary = "Standard font registry loading TrueType and Type1 fonts from streams, files, or platform system fonts without Minecraft dependencies.",
        purpose = "Provides platform-independent font loading, caching, and fallback resolution for desktop UI environments or server-side toolkits.",
        useWhen = {
                "Loading custom TrueType (.ttf) or Type1 fonts dynamically at runtime.",
                "Configuring default font faces and system font aliases without accessing Minecraft game assets."
        },
        criticalRules = {
                "Font identifiers are normalized to 'font' if null or blank.",
                "Closing the registry via close() purges all loaded font faces and resets default font state."
        },
        related = {"FontFace", "FontRegistry", "Fonts"}
)
@WikiPage(
        title = "<key:default.font.registry.title>",
        slug = "default-font-registry",
        category = "<key:default.font.registry.category>",
        summary = "<key:default.font.registry.summary>",
        order = 40)
@WikiSection(
        title = "<key:default.font.registry.section.overview.title>",
        markdown = "<key:default.font.registry.section.overview.body>",
        order = 10)
@WikiSection(
        title = "<key:default.font.registry.section.registration.title>",
        markdown = "<key:default.font.registry.section.registration.body>",
        order = 20)
@WikiSection(
        title = "<key:default.font.registry.section.system.title>",
        markdown = "<key:default.font.registry.section.system.body>",
        order = 30)
@WikiSection(
        title = "<key:default.font.registry.section.lifecycle.title>",
        markdown = "<key:default.font.registry.section.lifecycle.body>",
        order = 40)
@WikiExample(
        title = "<key:default.font.registry.example.file.title>",
        code = "<key:default.font.registry.example.file.code>",
        order = 10)
@WikiExample(
        title = "<key:default.font.registry.example.system.title>",
        code = "<key:default.font.registry.example.system.code>",
        order = 20)
public final class DefaultFontRegistry implements FontRegistry, AutoCloseable {
    private static final DefaultFontRegistry GLOBAL = new DefaultFontRegistry();

    private final Map<String, FontFace> faces = new LinkedHashMap<>();
    private FontFace defaultFace;

    @WikiMethod(
            title = "<key:default.font.registry.method.global.title>",
            summary = "<key:default.font.registry.method.global.summary>",
            order = 10)
    @WikiReturn("<key:default.font.registry.method.global.return>")
    public static DefaultFontRegistry global() {
        return GLOBAL;
    }

    @Override
    @WikiMethod(
            title = "<key:default.font.registry.method.register_path.title>",
            summary = "<key:default.font.registry.method.register_path.summary>",
            order = 20)
    @WikiReturn("<key:default.font.registry.method.register_path.return>")
    public FontFace register(
            @WikiParameter(name = "id", description = "<key:default.font.registry.method.register_path.id>")
            String id,
            @WikiParameter(name = "path", description = "<key:default.font.registry.method.register_path.path>")
            Path path) throws IOException {
        Objects.requireNonNull(path, "path");
        try (InputStream input = Files.newInputStream(path)) {
            return register(id, input);
        }
    }

    @Override
    @WikiMethod(
            title = "<key:default.font.registry.method.register_stream.title>",
            summary = "<key:default.font.registry.method.register_stream.summary>",
            order = 30)
    @WikiReturn("<key:default.font.registry.method.register_stream.return>")
    public FontFace register(
            @WikiParameter(name = "id", description = "<key:default.font.registry.method.register_stream.id>")
            String id,
            @WikiParameter(name = "input", description = "<key:default.font.registry.method.register_stream.input>")
            InputStream input) throws IOException {
        Objects.requireNonNull(input, "input");
        return register(id, input.readAllBytes());
    }

    @Override
    @WikiMethod(
            title = "<key:default.font.registry.method.register_bytes.title>",
            summary = "<key:default.font.registry.method.register_bytes.summary>",
            order = 40)
    @WikiReturn("<key:default.font.registry.method.register_bytes.return>")
    public FontFace register(
            @WikiParameter(name = "id", description = "<key:default.font.registry.method.register_bytes.id>")
            String id,
            @WikiParameter(name = "data", description = "<key:default.font.registry.method.register_bytes.data>")
            byte[] data) throws IOException {
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

    @WikiMethod(
            title = "<key:default.font.registry.method.register_system.title>",
            summary = "<key:default.font.registry.method.register_system.summary>",
            order = 50)
    @WikiReturn("<key:default.font.registry.method.register_system.return>")
    public FontFace registerSystem(
            @WikiParameter(name = "id", description = "<key:default.font.registry.method.register_system.id>")
            String id,
            @WikiParameter(name = "family", description = "<key:default.font.registry.method.register_system.family>")
            String family) {
        String normalizedId = normalizeId(id);
        String normalizedFamily = family == null || family.isBlank() ? "SansSerif" : family;
        AwtFontFace face = new AwtFontFace(normalizedId, new Font(normalizedFamily, Font.PLAIN, 16));
        faces.put(normalizedId, face);
        if (defaultFace == null) defaultFace = face;
        return face;
    }

    @Override
    @WikiMethod(
            title = "<key:default.font.registry.method.find.title>",
            summary = "<key:default.font.registry.method.find.summary>",
            order = 60)
    @WikiReturn("<key:default.font.registry.method.find.return>")
    public FontFace find(
            @WikiParameter(name = "id", description = "<key:default.font.registry.method.find.id>")
            String id) {
        return faces.get(id);
    }

    @Override
    @WikiMethod(
            title = "<key:default.font.registry.method.default_face_get.title>",
            summary = "<key:default.font.registry.method.default_face_get.summary>",
            order = 70)
    @WikiReturn("<key:default.font.registry.method.default_face_get.return>")
    public FontFace defaultFace() {
        if (defaultFace == null) defaultFace = registerSystem("default", "SansSerif");
        return defaultFace;
    }

    @Override
    @WikiMethod(
            title = "<key:default.font.registry.method.default_face_set.title>",
            summary = "<key:default.font.registry.method.default_face_set.summary>",
            order = 80)
    public void defaultFace(
            @WikiParameter(name = "face", description = "<key:default.font.registry.method.default_face_set.face>")
            FontFace face) {
        if (face != null) defaultFace = face;
    }

    @Override
    @WikiMethod(
            title = "<key:default.font.registry.method.faces.title>",
            summary = "<key:default.font.registry.method.faces.summary>",
            order = 90)
    @WikiReturn("<key:default.font.registry.method.faces.return>")
    public Map<String, FontFace> faces() {
        return Collections.unmodifiableMap(faces);
    }

    @Override
    @WikiMethod(
            title = "<key:default.font.registry.method.close.title>",
            summary = "<key:default.font.registry.method.close.summary>",
            order = 100)
    public void close() {
        faces.clear();
        defaultFace = null;
    }

    private static String normalizeId(String id) {
        return id == null || id.isBlank() ? "font" : id;
    }
}
