package dev.sixik.unigui.api.text;

import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.render.SimpleTextureHandle;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** EMPTY, склейка run'ов и стили на участок текста в {@link RichText.Builder}. */
class RichTextBuilderTest {
    private static final float EPS = 0.001f;
    private static final MutableColor RED = new MutableColor(1, 0, 0, 1);

    @Test
    void emptyResultsShareOneInstance() {
        assertSame(RichText.EMPTY, RichText.plain(""));
        assertSame(RichText.EMPTY, RichText.plain(null));
        assertSame(RichText.EMPTY, RichText.ofSpans(List.of()));
        assertSame(RichText.EMPTY, RichText.builder().append("").build());
        assertSame(RichText.EMPTY, RichText.plain("abc").slice(1, 1));
        assertTrue(RichText.EMPTY.isEmpty());
    }

    @Test
    void sameStyleAppendsMerge() {
        RichText text = RichText.builder().append("a").append("b").color(RED).append("c").append("d").build();
        assertEquals(2, text.spans().size());
        assertEquals("ab", text.textRuns().get(0).text());
        assertEquals("cd", text.textRuns().get(1).text());
    }

    @Test
    void inlineSpanBreaksMerging() {
        RichText text = RichText.builder().append("a").icon("i", new SimpleTextureHandle("t", 4, 4), 4).append("b").build();
        assertEquals(3, text.spans().size());
    }

    @Test
    void scopedStyleIsRestored() {
        RichText text = RichText.builder()
                .size(10)
                .append("HP: ")
                .append("20", s -> s.color(RED).size(14))
                .append(" / 20")
                .build();
        List<TextRun> runs = text.textRuns();
        assertEquals(3, runs.size());
        assertNull(runs.get(0).color());
        assertEquals(14.0f, runs.get(1).pixelSize(), EPS);
        assertEquals(1.0f, runs.get(1).color().r(), EPS);
        assertNull(runs.get(2).color(), "style returns to the builder's after the scope");
        assertEquals(10.0f, runs.get(2).pixelSize(), EPS);
    }

    @Test
    void styledBodyAppliesToIconsAndText() {
        RichText text = RichText.builder()
                .styled(s -> s.size(20).color(RED), b -> b.append("x").icon("i", new SimpleTextureHandle("t", 4, 4)))
                .append("y")
                .build();
        assertEquals(20.0f, ((InlineContentSpan) text.spans().get(1)).width(), EPS, "em icon takes the scoped size");
        assertEquals(TextRun.DEFAULT_PIXEL_SIZE, ((TextRun) text.spans().get(2)).pixelSize(), EPS);
    }

    @Test
    void styleIsRestoredWhenBodyThrows() {
        RichText.Builder builder = RichText.builder();
        try {
            builder.styled(s -> s.color(RED), b -> {
                throw new IllegalStateException();
            });
        } catch (IllegalStateException ignored) {
        }
        assertNull(builder.append("z").build().textRuns().get(0).color());
    }
}
