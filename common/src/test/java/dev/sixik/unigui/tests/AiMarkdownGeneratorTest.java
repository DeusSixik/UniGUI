package dev.sixik.unigui.tests;

import dev.sixik.unigui.api.docs.AiDoc;
import dev.sixik.unigui.api.layout.LayoutStyle;
import dev.sixik.unigui.impl.widget.WidgetBase;
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
        assertTrue(markdown.contains("absolute data-item index"));
        assertTrue(markdown.contains("### CORE CONFIGURATION & BUILDER METHODS"));
        assertTrue(markdown.contains("`gridColumns(int)` → VirtualListView (default: `0`)"));
        assertTrue(markdown.contains("`itemHeight(float)` → VirtualListView (default: `18`)"));
        assertTrue(markdown.contains("### SELECTION & NAVIGATION"));
        assertTrue(markdown.contains("`selectedIndices()` → List<Integer>"));
        assertTrue(markdown.contains("### SCROLLING & POSITION"));
        assertTrue(markdown.contains("### EXAMPLES"));
        assertTrue(markdown.contains(".itemFactory(index -> createEntryCard(entries.get(index)))"));
        assertFalse(containsCyrillic(markdown));
    }

    @Test
    void renderType_layoutStyle_generatesEnglishStructuredReference() {
        String markdown = AiMarkdownGenerator.renderType(LayoutStyle.class);

        assertTrue(markdown.contains("# `LayoutStyle`"));
        assertTrue(markdown.contains("Fluent rules used by the layout system"));
        assertTrue(markdown.contains("### SIZE & POSITION"));
        assertTrue(markdown.contains("`fill()` → LayoutStyle"));
        assertTrue(markdown.contains("### FLEX LAYOUT"));
        assertTrue(markdown.contains("`flex(float, float, SizeValue)` → LayoutStyle"));
        assertTrue(markdown.contains("### GRID & ALIGNMENT"));
        assertTrue(markdown.contains("`gridTemplateColumns(List<GridTrack>)` → LayoutStyle"));
        assertTrue(markdown.contains("#### Fixed header and expanding content"));
        assertFalse(containsCyrillic(markdown));
    }

    @Test
    void renderType_widgetBase_generatesEnglishLifecycleAndStyleReference() {
        String markdown = AiMarkdownGenerator.renderType(WidgetBase.class);

        assertTrue(markdown.contains("# `WidgetBase` (implements `Widget`)"));
        assertTrue(markdown.contains("### IDENTITY & STYLE SELECTORS"));
        assertTrue(markdown.contains("`styleId(String)` → WidgetBase"));
        assertTrue(countOccurrences(markdown, "`styleId(String)`") == 1);
        assertTrue(markdown.contains("### TRANSFORMS & ANIMATION"));
        assertTrue(markdown.contains("`animateOpacity(float, TransitionSpec)` → WidgetBase"));
        assertTrue(markdown.contains("### RENDERING & CACHING"));
        assertTrue(markdown.contains("### EVENTS"));
        assertTrue(markdown.contains("### INVALIDATION & LIFECYCLE"));
        assertTrue(markdown.contains("#### Configure a widget"));
        assertFalse(containsCyrillic(markdown));
    }

    private static boolean containsCyrillic(String value) {
        return value.codePoints().anyMatch(codePoint ->
                (codePoint >= 0x0400 && codePoint <= 0x052F)
                        || (codePoint >= 0x2DE0 && codePoint <= 0x2DFF)
                        || (codePoint >= 0xA640 && codePoint <= 0xA69F));
    }

    private static int countOccurrences(String value, String needle) {
        int count = 0;
        int offset = 0;
        while ((offset = value.indexOf(needle, offset)) >= 0) {
            count++;
            offset += needle.length();
        }
        return count;
    }

    @AiDoc(summary = "Type guidance.", useWhen = "An agent needs this API.")
    static class AiOnlyType {
        @AiDoc(summary = "Executes an action.", constraints = "Main thread only.")
        public void execute(String value) {
        }
    }
}
