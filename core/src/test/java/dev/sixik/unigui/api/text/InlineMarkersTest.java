package dev.sixik.unigui.api.text;

import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.render.DrawCommand;
import dev.sixik.unigui.api.render.DrawCommandType;
import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.render.SimpleTextureHandle;
import dev.sixik.unigui.impl.render.DefaultRenderContext;
import dev.sixik.unigui.impl.text.TextEngine;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Глобальные маркеры, равенство inline-span'ов, em-размеры, отступы, tint и baseline. */
class InlineMarkersTest {
    private static final float EPS = 0.001f;

    private static final InlineMarkerHandler KEYS = (argument, size) ->
            InlineContentSpan.sized("key:" + argument, size,
                    InlineContentRenderer.texture(new SimpleTextureHandle("keys/" + argument, 16, 16)));

    @AfterEach
    void cleanup() {
        InlineMarkers.unregister("key");
    }

    @Test
    void globalMarkersResolveWithoutScope() {
        assertFalse(RichText.resolve("A {key:jump} B").hasInlineContent(), "nothing registered yet");

        InlineMarkers.register("key", KEYS);
        RichText text = RichText.resolve("Hold {key:jump} to fly");
        assertTrue(text.hasInlineContent());
        assertEquals(3, text.spans().size());
        assertEquals("Hold " + InlineContentSpan.DEFAULT_FALLBACK_TEXT + " to fly", text.plainText());
        InlineContentSpan key = (InlineContentSpan) text.spans().get(1);
        assertEquals("key:jump", key.id());
        assertTrue(key.scalesWithText(), "a marker without @size is 1em");
        assertEquals(TextRun.DEFAULT_PIXEL_SIZE, key.width(), EPS);
    }

    @Test
    void unknownMarkersStayLiteralAndEscapeWorks() {
        InlineMarkers.register("key", KEYS);
        RichText text = RichText.resolve("{{x} {nope:a} {key:}");
        assertFalse(text.hasInlineContent());
        assertEquals("{x} {nope:a} {key:}", text.plainText());
    }

    @Test
    void textureScopeFallsBackToGlobalTypes() {
        InlineMarkers.register("key", KEYS);
        RichText text;
        try (var ignored = InlineContentResolvers.push(InlineContentResolvers.textureMarkers(
                (id, width, height) -> new SimpleTextureHandle(id, width, height)))) {
            text = RichText.resolve("{icon:a@9x7}{key:jump}");
        }
        assertEquals(2, text.spans().size());
        InlineContentSpan icon = (InlineContentSpan) text.spans().get(0);
        assertEquals(9.0f, icon.width(), EPS);
        assertEquals(7.0f, icon.height(), EPS);
        assertFalse(icon.scalesWithText());
        assertEquals("key:jump", ((InlineContentSpan) text.spans().get(1)).id());
    }

    @Test
    void plainScopeDisablesGlobalMarkers() {
        InlineMarkers.register("key", KEYS);
        try (var ignored = InlineContentResolvers.push(InlineContentResolvers.plain())) {
            assertFalse(RichText.resolve("{key:jump}").hasInlineContent());
        }
        assertTrue(RichText.resolve("{key:jump}").hasInlineContent());
    }

    @Test
    void invalidSizeSuffixIsPartOfArgument() {
        InlineMarkers.register("key", KEYS);
        InlineContentSpan span = (InlineContentSpan) RichText.resolve("{key:mod:item@meta}").spans().get(0);
        assertEquals("key:mod:item@meta", span.id());
        InlineContentSpan sized = (InlineContentSpan) RichText.resolve("{key:jump@1.5em}").spans().get(0);
        assertEquals(15.0f, sized.width(), EPS);
    }

    @Test
    void resolvedTextIsEqualAcrossCalls() {
        InlineMarkers.register("key", KEYS);
        assertEquals(RichText.resolve("A {key:jump}"), RichText.resolve("A {key:jump}"));
        assertEquals(RichText.resolve("A {key:jump}").hashCode(), RichText.resolve("A {key:jump}").hashCode());
        assertFalse(RichText.resolve("A {key:jump}").equals(RichText.resolve("A {key:sneak}")));
    }

    @Test
    void spansWithoutIdCompareRenderers() {
        InlineContentSpan a = InlineContentSpan.of("", 4, 4, (draw, context) -> { });
        InlineContentSpan b = InlineContentSpan.of("", 4, 4, (draw, context) -> { });
        assertFalse(a.equals(b));
        assertEquals(a, InlineContentSpan.of("", 4, 4, a.renderer()));
    }

    @Test
    void emSpansFollowTextSize() {
        SimpleTextureHandle texture = new SimpleTextureHandle("t", 8, 8);
        RichText text = RichText.builder().size(18.0f).append("x").icon("i", texture).icon("abs", texture, 8.0f).build();
        InlineContentSpan em = (InlineContentSpan) text.spans().get(1);
        assertEquals(18.0f, em.width(), EPS);

        RichText scaled = text.withTextStyle(6.0f, null, null);
        assertEquals(6.0f, ((InlineContentSpan) scaled.spans().get(1)).width(), EPS);
        assertSame(text.spans().get(2), scaled.spans().get(2), "absolute spans are kept as is");
    }

    @Test
    void appendResolvedUsesBuilderStyle() {
        InlineMarkers.register("key", KEYS);
        RichText text = RichText.builder().size(20.0f).appendResolved("A {key:jump}").build();
        assertEquals(20.0f, ((TextRun) text.spans().get(0)).pixelSize(), EPS);
        assertEquals(20.0f, ((InlineContentSpan) text.spans().get(1)).width(), EPS);
    }

    @Test
    void marginsAddToAdvanceAndOffsetBounds() {
        InlineContentSpan span = InlineContentSpan.of("m", 8, 8, InlineContentRenderer.texture(new SimpleTextureHandle("t", 8, 8)))
                .withMargins(2.0f, 3.0f);
        RichText text = RichText.builder().inline(span).build();
        assertEquals(13.0f, TextEngine.measureLineWidth(text), EPS);

        DefaultRenderContext context = new DefaultRenderContext(new dev.sixik.unigui.api.render.DrawList());
        TextEngine.drawInline(new DrawScope(context, null), text, 10.0f, 0.0f, 100.0f, 12.0f, Paint.fill(new MutableColor(1, 0, 0, 0.5f)));
        DrawCommand texture = only(context, DrawCommandType.TEXTURE);
        assertEquals(12.0f, texture.bounds().x(), EPS);
        assertEquals(8.0f, texture.bounds().width(), EPS);
    }

    @Test
    void untintedTextureKeepsOnlyAlpha() {
        SimpleTextureHandle handle = new SimpleTextureHandle("t", 8, 8);
        RichText plain = RichText.builder().icon("a", handle, 8.0f).build();
        RichText tinted = RichText.builder().icon("a", handle, 8.0f, 8.0f, true).build();
        Paint red = Paint.fill(new MutableColor(1, 0, 0, 0.5f));

        DrawCommand untintedCommand = drawTexture(plain, red);
        assertEquals(1.0f, untintedCommand.paint().color().g(), EPS);
        assertEquals(0.5f, untintedCommand.paint().color().a(), EPS);

        DrawCommand tintedCommand = drawTexture(tinted, red);
        assertEquals(0.0f, tintedCommand.paint().color().g(), EPS);
    }

    @Test
    void baselineAlignmentSitsOnTextBaseline() {
        InlineContentSpan span = InlineContentSpan.of("b", 4, 4, InlineContentRenderer.texture(new SimpleTextureHandle("t", 4, 4)));
        RichText center = RichText.builder().append("Ag").inline(span).build();
        RichText baseline = RichText.builder().append("Ag").inline(span.withAlignment(InlineContentAlignment.BASELINE)).build();

        float centerY = drawTexture(center, Paint.fill(new MutableColor(1, 1, 1, 1))).bounds().y();
        float baselineY = drawTexture(baseline, Paint.fill(new MutableColor(1, 1, 1, 1))).bounds().y();
        assertTrue(baselineY > centerY, "a small icon on the baseline sits lower than a centered one");
        assertTrue(baselineY + 4.0f <= 30.0f);
    }

    @Test
    void inlineSizeParsing() {
        assertEquals(InlineSize.pixels(16, 16), InlineSize.parse("16"));
        assertEquals(InlineSize.pixels(16, 8), InlineSize.parse(" 16 x 8 "));
        assertEquals(InlineSize.ems(1.5f, 1.5f), InlineSize.parse("1.5em"));
        assertEquals(InlineSize.ems(2, 1), InlineSize.parse("2x1em"));
        assertNull(InlineSize.parse("meta"));
        assertNull(InlineSize.parse("0"));
        assertNull(InlineSize.parse(""));
    }

    private static DrawCommand drawTexture(RichText text, Paint paint) {
        DefaultRenderContext context = new DefaultRenderContext(new dev.sixik.unigui.api.render.DrawList());
        TextEngine.drawInline(new DrawScope(context, null), text, 0.0f, 0.0f, 100.0f, 30.0f, paint);
        return only(context, DrawCommandType.TEXTURE);
    }

    private static DrawCommand only(DefaultRenderContext context, DrawCommandType type) {
        List<DrawCommand> result = new ArrayList<>();
        for (DrawCommand command : context.drawList().commands()) {
            if (command.type() == type) result.add(command);
        }
        assertEquals(1, result.size(), "expected one " + type + " command");
        assertNotNull(result.get(0));
        return result.get(0);
    }
}
