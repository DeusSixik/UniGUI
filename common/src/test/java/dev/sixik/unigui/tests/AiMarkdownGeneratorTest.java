package dev.sixik.unigui.tests;

import dev.sixik.unigui.api.docs.AiDoc;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiMarkdownGeneratorTest {
    @Test
    void renderType_aiAnnotations_createsIndependentCompactCollectionPage() {
        String markdown = AiMarkdownGenerator.renderType(AiOnlyType.class);

        assertTrue(markdown.contains("# `dev.sixik.unigui.tests.AiMarkdownGeneratorTest$AiOnlyType`"));
        assertTrue(markdown.contains("Type guidance."));
        assertTrue(markdown.contains("## `execute(String)`"));
        assertTrue(markdown.contains("Constraints: Main thread only."));
        assertFalse(markdown.contains("title:"));
        assertFalse(markdown.contains("Wiki"));
    }

    @AiDoc(summary = "Type guidance.", useWhen = "An agent needs this API.")
    static class AiOnlyType {
        @AiDoc(summary = "Executes an action.", constraints = "Main thread only.")
        public void execute(String value) {
        }
    }
}
