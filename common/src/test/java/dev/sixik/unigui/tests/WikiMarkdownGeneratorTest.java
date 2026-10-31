package dev.sixik.unigui.tests;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiGif;
import dev.sixik.unigui.api.docs.WikiIcon;
import dev.sixik.unigui.api.docs.WikiLink;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;
import dev.sixik.unigui.api.docs.WikiThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WikiMarkdownGeneratorTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void renderPage_annotatedType_includesPageSectionsMediaExamplesAndMethodsButNotAiDocs() {
        String markdown = WikiMarkdownGenerator.renderPage(SamplePage.class);

        assertTrue(markdown.startsWith("---\ntitle: \"Sample \\\"page\\\"\"\nslug: \"sample-page\""));
        assertTrue(markdown.contains("![Sample icon](<assets/sample.png>)"));
        assertTrue(markdown.contains("![Grid scroll](<assets/grid.gif>)"));
        assertTrue(markdown.indexOf("## Overview") < markdown.indexOf("## Details"));
        assertTrue(markdown.contains("```java\nnew SamplePage();\n```"));
        assertTrue(markdown.contains("## API"));
        assertTrue(markdown.contains("`scrollTo(float)`"));
        assertTrue(markdown.contains("SamplePage::"));
        assertTrue(markdown.contains("| `offset` | `float` | New vertical offset. |"));
        assertTrue(markdown.contains("#### Scroll to the start"));
        assertTrue(markdown.contains("Returns `SamplePage`: this object."));
        assertTrue(markdown.contains("`IllegalArgumentException`: Offset is not finite."));
        assertTrue(markdown.contains("- [Project site](<https://example.com>)"));
        assertTrue(!markdown.contains("AI / API notes"));
        assertTrue(!markdown.contains("Must be arranged before hit testing."));
    }

    @Test
    void renderPage_invalidSlug_rejectsUnsafeOutputPath() {
        assertThrows(IllegalArgumentException.class,
                () -> WikiMarkdownGenerator.renderPage(InvalidSlugPage.class));
    }

    @Test
    void renderPage_unsafeLinkScheme_rejectsJavascriptUrl() {
        assertThrows(IllegalArgumentException.class,
                () -> WikiMarkdownGenerator.renderPage(UnsafeLinkPage.class));
    }

    @Test
    void writePages_localizedKeys_generatesLocaleTreesAndSharedAssets() throws IOException {
        Path languages = temporaryDirectory.resolve("lang");
        Path assets = temporaryDirectory.resolve("assets");
        Path output = temporaryDirectory.resolve("output");
        Files.createDirectories(languages);
        Files.createDirectories(assets.resolve("videos"));
        Files.writeString(assets.resolve("videos/en.gif"), "en");
        Files.writeString(assets.resolve("videos/ru.gif"), "ru");
        Files.writeString(languages.resolve("en.json"), """
                {
                  "wiki.index.title": "Wiki",
                  "wiki.links.title": "Related links",
                  "wiki.api.title": "API",
                  "wiki.api.parameter": "Parameter",
                  "wiki.api.type": "Type",
                  "wiki.api.description": "Description",
                  "wiki.api.returns": "Returns",
                  "wiki.api.throws": "Throws",
                  "page.title": "Button",
                  "page.category": "Widgets/Data",
                  "page.summary": "English summary mentions VectorPath and `VectorPath`.",
                  "page.gif": "assets/videos/en.gif",
                  "page.gif.alt": "Button animation",
                  "page.related": "Related details: <link:path:VectorPath>",
                  "page.link.label": "Details",
                  "page.details.title": "VectorPath"
                }
                """);
        Files.writeString(languages.resolve("ru.json"), """
                {
                  "wiki.index.title": "Вики",
                  "wiki.links.title": "Связанные ссылки",
                  "wiki.api.title": "API",
                  "wiki.api.parameter": "Параметр",
                  "wiki.api.type": "Тип",
                  "wiki.api.description": "Описание",
                  "wiki.api.returns": "Возвращает",
                  "wiki.api.throws": "Исключения",
                  "page.title": "Кнопка",
                  "page.category": "Виджеты/Данные",
                  "page.summary": "Русское описание: Путь вектора и `Путь вектора`.",
                  "page.gif": "assets/videos/ru.gif",
                  "page.gif.alt": "Анимация кнопки",
                  "page.related": "Подробнее: <link:path:VectorPath>",
                  "page.link.label": "Детали",
                  "page.details.title": "Путь вектора"
                }
                """);
        WikiLocalization localization = WikiLocalization.load(languages, warning -> { });

        WikiMarkdownGenerator.writePages(output, List.of(LocalizedPage.class, RelatedPage.class), localization, assets);

        String english = Files.readString(output.resolve("en/widgets/data/button.md"));
        String russian = Files.readString(output.resolve("ru/widgets/data/button.md"));
        assertTrue(english.contains("# Button"));
        assertTrue(english.contains("English summary mentions [VectorPath](<../components/details.md>) and `VectorPath`."));
        assertTrue(english.contains("![Button animation](<../../../assets/videos/en.gif>)"));
        assertTrue(russian.contains("# Кнопка"));
        assertTrue(russian.contains("Русское описание: [Путь вектора](<../components/details.md>) и `Путь вектора`."));
        assertTrue(russian.contains("![Анимация кнопки](<../../../assets/videos/ru.gif>)"));
        assertTrue(english.contains("## Related links\n\n- [Details](<../components/details.md>): Related details: [VectorPath](<../components/details.md>)"));
        assertTrue(russian.contains("## Связанные ссылки\n\n- [Детали](<../components/details.md>): Подробнее: [Путь вектора](<../components/details.md>)"));
        assertTrue(Files.exists(output.resolve("assets/videos/en.gif")));
        assertTrue(Files.exists(output.resolve("assets/videos/ru.gif")));
        assertTrue(Files.exists(output.resolve("en/index.md")));
        assertTrue(Files.exists(output.resolve("ru/index.md")));
        assertTrue(Files.readString(output.resolve("ru/index.md")).contains("## Виджеты / Данные"));
    }

    @Test
    void writePages_autoLinkedSignatureTypes_preservesHeadingAttributeList() throws IOException {
        Path assets = temporaryDirectory.resolve("signature-assets");
        Path output = temporaryDirectory.resolve("signature-output");
        Files.createDirectories(assets);

        WikiMarkdownGenerator.writePages(
                output,
                List.of(SignatureStylePack.class, Style.class, StyleDefinition.class),
                WikiLocalization.literalOnly(),
                assets);

        String markdown = Files.readString(output.resolve("en/api/signature-style-pack.md"));
        assertTrue(markdown.contains("Uses [Style](<style.md>) and [StyleDefinition](<style-definition.md>)."));
        assertTrue(markdown.contains(
                "`put(StyleDefinition)` {: #putstyledefinition data-toc-label=\"put(StyleDefinition)\" }"));
        assertTrue(markdown.contains(
                "`of(String, Style)` {: #ofstring-style data-toc-label=\"of(String, Style)\" }"));
        assertTrue(!markdown.contains("data-toc-label=\"put([StyleDefinition]"));
        assertTrue(!markdown.contains("data-toc-label=\"of(String, [Style]"));
    }

    @Test
    void localization_missingRussianKey_warnsAndFallsBackToEnglish() throws IOException {
        Path languages = temporaryDirectory.resolve("fallback-lang");
        Files.createDirectories(languages);
        Files.writeString(languages.resolve("en.json"), "{\"page.summary\":\"English summary\"}");
        Files.writeString(languages.resolve("ru.json"), "{}");
        List<String> warnings = new ArrayList<>();
        WikiLocalization localization = WikiLocalization.load(languages, warnings::add);

        String value = localization.resolve("<key:page.summary>", "ru", "test.summary");

        assertTrue(value.equals("English summary"));
        assertTrue(warnings.size() == 1 && warnings.get(0).contains("page.summary"));
    }

    @Test
    void localization_keyMissingFromEnglish_failsGeneration() throws IOException {
        Path languages = temporaryDirectory.resolve("invalid-lang");
        Files.createDirectories(languages);
        Files.writeString(languages.resolve("en.json"), "{}");
        Files.writeString(languages.resolve("ru.json"), "{\"only.ru\":\"Есть\"}");
        WikiLocalization localization = WikiLocalization.load(languages, warning -> { });

        assertThrows(WikiLocalization.MissingWikiTranslationException.class,
                () -> localization.resolve("<key:only.ru>", "ru", "test.value"));
    }

    @WikiPage(title = "Sample \"page\"", slug = "sample-page", category = "Widgets", summary = "A sample page.")
    @WikiIcon(src = "assets/sample.png", alt = "Sample icon")
    @WikiGif(src = "assets/grid.gif", alt = "Grid scroll", caption = "Scrolling a grid.")
    @WikiLink(label = "Project site", target = "https://example.com")
    @WikiSection(title = "Details", markdown = "More details.", order = 2)
    @WikiSection(title = "Overview", markdown = "Overview text.", order = 1)
    @WikiExample(title = "Create", code = "new SamplePage();", order = 1)
    @AiDoc(summary = "Represents the sample page.", constraints = "Must be arranged before hit testing.")
    static class SamplePage {
        @AiDoc(summary = "Scrolls the sample.", useWhen = "Move to a known offset.")
        @WikiMethod(title = "Scroll", summary = "Moves the viewport.", order = 10)
        @WikiReturn("this object")
        @WikiThrows(type = IllegalArgumentException.class, description = "Offset is not finite.")
        @WikiExample(title = "Scroll to the start", code = "page.scrollTo(0.0f);")
        public SamplePage scrollTo(@WikiParameter(name = "offset", description = "New vertical offset.") float offset) {
            return this;
        }
    }

    @WikiPage(title = "Invalid", slug = "../outside")
    static class InvalidSlugPage {
    }

    @WikiPage(title = "Unsafe link", slug = "unsafe-link")
    @WikiLink(label = "Run script", target = "javascript:alert(1)")
    static class UnsafeLinkPage {
    }

    @WikiPage(
            title = "<key:page.title>",
            slug = "button",
            category = "<key:page.category>",
            summary = "<key:page.summary>")
    @WikiGif(src = "<key:page.gif>", alt = "<key:page.gif.alt>")
    @WikiLink(label = "<key:page.link.label>", target = "page:details", description = "<key:page.related>")
    static class LocalizedPage {
    }

    @WikiPage(title = "<key:page.details.title>", slug = "details", category = "Widgets/Components")
    static class RelatedPage {
    }

    @WikiPage(
            title = "SignatureStylePack",
            slug = "signature-style-pack",
            summary = "Uses Style and StyleDefinition.")
    static class SignatureStylePack {
        @WikiMethod
        public void put(StyleDefinition definition) {
        }

        @WikiMethod
        public void of(String id, Style style) {
        }
    }

    @WikiPage(title = "Style", slug = "style")
    static class Style {
    }

    @WikiPage(title = "StyleDefinition", slug = "style-definition")
    static class StyleDefinition {
    }
}
