package dev.sixik.unigui.tests;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.docs.WikiExample;
import dev.sixik.unigui.api.docs.WikiGif;
import dev.sixik.unigui.api.docs.WikiIcon;
import dev.sixik.unigui.api.docs.WikiMethod;
import dev.sixik.unigui.api.docs.WikiPage;
import dev.sixik.unigui.api.docs.WikiParameter;
import dev.sixik.unigui.api.docs.WikiReturn;
import dev.sixik.unigui.api.docs.WikiSection;
import dev.sixik.unigui.api.docs.WikiThrows;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WikiMarkdownGeneratorTest {
    @Test
    void renderPage_annotatedType_includesPageSectionsMediaExamplesAndMethodsButNotAiDocs() {
        String markdown = WikiMarkdownGenerator.renderPage(SamplePage.class);

        assertTrue(markdown.startsWith("---\ntitle: \"Sample \\\"page\\\"\"\nslug: \"sample-page\""));
        assertTrue(markdown.contains("![Sample icon](<assets/sample.png>)"));
        assertTrue(markdown.contains("![Grid scroll](<assets/grid.gif>)"));
        assertTrue(markdown.indexOf("## Overview") < markdown.indexOf("## Details"));
        assertTrue(markdown.contains("```java\nnew SamplePage();\n```"));
        assertTrue(markdown.contains("## API"));
        assertTrue(markdown.contains("### `scrollTo(float)`"));
        assertTrue(markdown.contains("| `offset` | `float` | New vertical offset. |"));
        assertTrue(markdown.contains("#### Scroll to the start"));
        assertTrue(markdown.contains("Returns `SamplePage`: this object."));
        assertTrue(markdown.contains("`IllegalArgumentException`: Offset is not finite."));
        assertTrue(!markdown.contains("AI / API notes"));
        assertTrue(!markdown.contains("Must be arranged before hit testing."));
    }

    @Test
    void renderPage_invalidSlug_rejectsUnsafeOutputPath() {
        assertThrows(IllegalArgumentException.class,
                () -> WikiMarkdownGenerator.renderPage(InvalidSlugPage.class));
    }

    @WikiPage(title = "Sample \"page\"", slug = "sample-page", category = "Widgets", summary = "A sample page.")
    @WikiIcon(src = "assets/sample.png", alt = "Sample icon")
    @WikiGif(src = "assets/grid.gif", alt = "Grid scroll", caption = "Scrolling a grid.")
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
}
