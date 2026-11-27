package dev.sixik.unigui.api.text;

import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.render.SimpleTextureHandle;
import dev.sixik.unigui.impl.text.TextEngine;
import dev.sixik.unigui.widgets.display.Label;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Пересборка RichText без потери inline-контента: mapTextRuns, withFont, slice, TextWidget.font и отладка маркеров. */
class RichTextRestyleTest {
    private static final float EPS = 0.001f;

    @AfterEach
    void cleanup() {
        InlineMarkers.unregister("key");
        InlineMarkers.unresolvedListener(null);
    }

    private static RichText withIcon() {
        return RichText.builder()
                .color(new MutableColor(1, 0, 0, 1)).append("A")
                .icon("i", new SimpleTextureHandle("t", 8, 8), 8.0f)
                .append("B")
                .build();
    }

    @Test
    void mapTextRunsKeepsInlineSpans() {
        RichText source = withIcon();
        RichText mapped = source.mapTextRuns(run -> run.withText(run.text().toLowerCase()));
        assertEquals(3, mapped.spans().size());
        assertSame(source.spans().get(1), mapped.spans().get(1));
        assertEquals("a" + InlineContentSpan.DEFAULT_FALLBACK_TEXT + "b", mapped.plainText());
        assertSame(source, source.mapTextRuns(run -> run), "an identity mapping returns the same instance");
    }

    @Test
    void withFontKeepsColorAndIcons() {
        FontFace face = Fonts.defaultFace();
        RichText styled = withIcon().withFont(face);
        assertTrue(styled.hasInlineContent());
        for (TextRun run : styled.textRuns()) {
            assertSame(face, run.font());
            assertNotNull(run.color());
        }
    }

    @Test
    void measuringWithDefaultFaceCountsIcons() {
        RichText text = withIcon();
        float withoutIcon = TextEngine.measureLineWidth(RichText.plain("AB"), null);
        assertEquals(withoutIcon + 8.0f, TextEngine.measureLineWidth(text, null), EPS);
    }

    @Test
    void partialInlineSliceKeepsNeighbourStyle() {
        InlineContentSpan wide = InlineContentSpan.of("w", 8, 8, null).withFallbackText("[icon]");
        RichText text = RichText.builder().size(14.0f).append("x").inline(wide).build();
        RichText slice = text.slice(1, 3);
        TextRun degraded = assertInstanceOf(TextRun.class, slice.spans().get(0));
        assertEquals("[i", degraded.text());
        assertEquals(14.0f, degraded.pixelSize(), EPS);
    }

    @Test
    void textWidgetFontKeepsMarkersAndSource() {
        InlineMarkers.register("key", (argument, size) -> InlineContentSpan.sized("key:" + argument, size, null));
        Label label = new Label("Hold {key:jump}");
        label.font(Fonts.defaultFace(), 20.0f);
        assertEquals("Hold {key:jump}", label.text(), "text() keeps the marker source");
        assertTrue(label.richText().hasInlineContent());
        InlineContentSpan key = (InlineContentSpan) label.richText().spans().get(1);
        assertEquals(20.0f, key.width(), EPS, "em icons follow the new font size");
        assertEquals(20.0f, label.richText().textRuns().get(0).pixelSize(), EPS);
    }

    @Test
    void unresolvedMarkersAreReported() {
        InlineMarkers.register("key", (argument, size) -> {
            if (argument.equals("boom")) throw new IllegalStateException("boom");
            return null;
        });
        List<InlineMarkers.UnresolvedReason> reasons = new ArrayList<>();
        InlineMarkers.unresolvedListener((marker, reason, failure) -> reasons.add(reason));

        RichText text = RichText.resolve("{kye:a} {key:missing} {key:boom} {key:} {plain}");
        assertEquals("{kye:a} {key:missing} {key:boom} {key:} {plain}", text.plainText());
        assertEquals(List.of(
                InlineMarkers.UnresolvedReason.UNKNOWN_TYPE,
                InlineMarkers.UnresolvedReason.HANDLER_RETURNED_NULL,
                InlineMarkers.UnresolvedReason.HANDLER_FAILED,
                InlineMarkers.UnresolvedReason.EMPTY_ARGUMENT), reasons);
    }
}
