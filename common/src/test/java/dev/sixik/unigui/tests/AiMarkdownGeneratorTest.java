package dev.sixik.unigui.tests;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.widgets.data.VirtualListView;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiMarkdownGeneratorTest {
    @Test
    void renderType_aiAnnotations_createsIndependentCompactCollectionPage() {
        String markdown = AiMarkdownGenerator.renderType(AiOnlyType.class);

        assertTrue(markdown.contains("# `AiOnlyType`"));
        assertTrue(markdown.contains("Type guidance."));
        assertTrue(markdown.contains("- `execute(String)` → void — Executes an action."));
        assertTrue(markdown.contains("- Constraints:\n    - Main thread only."));
        assertFalse(markdown.contains("title:"));
        assertFalse(markdown.contains("Wiki"));
    }

    @Test
    void renderType_virtualListView_includesStructuredRulesMethodsDefaultsAndExample() {
        String markdown = AiMarkdownGenerator.renderType(VirtualListView.class);

        assertTrue(markdown.contains("# `VirtualListView` (extends `WidgetBase`)"));
        assertTrue(markdown.contains("### CRITICAL RULES & GOTCHAS:"));
        assertTrue(markdown.contains("absolute index of the data item" )
                || markdown.contains("абсолютный индекс элемента данных"));
        assertTrue(markdown.contains("### CORE CONFIGURATION & BUILDER METHODS"));
        assertTrue(markdown.contains("`gridColumns(int)` → VirtualListView (default: `0`)"));
        assertTrue(markdown.contains("`itemHeight(float)` → VirtualListView (default: `18`)"));
        assertTrue(markdown.contains("### SELECTION & NAVIGATION"));
        assertTrue(markdown.contains("`selectedIndices()` → List<Integer>"));
        assertTrue(markdown.contains("### SCROLLING & POSITION"));
        assertTrue(markdown.contains("### EXAMPLES"));
        assertTrue(markdown.contains(".itemFactory(index -> createEntryCard(entries.get(index)))"));
    }

    @AiDoc(summary = "Type guidance.", useWhen = "An agent needs this API.")
    static class AiOnlyType {
        @AiDoc(summary = "Executes an action.", constraints = "Main thread only.")
        public void execute(String value) {
        }
    }
}
