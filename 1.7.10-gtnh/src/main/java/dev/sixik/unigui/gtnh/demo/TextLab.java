package dev.sixik.unigui.gtnh.demo;

import dev.sixik.unigui.api.core.InvalidationFlags;
import dev.sixik.unigui.api.layout.Alignment;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.render.RenderContext;
import dev.sixik.unigui.api.screen.UniScreen;
import dev.sixik.unigui.api.text.FontFace;
import dev.sixik.unigui.api.text.FontMetrics;
import dev.sixik.unigui.api.text.LinearGradientTextBrush;
import dev.sixik.unigui.api.text.RichText;
import dev.sixik.unigui.api.widget.Visibility;
import dev.sixik.unigui.gtnh.backend.GtnhClientTasks;
import dev.sixik.unigui.gtnh.backend.GtnhFonts;
import dev.sixik.unigui.gtnh.backend.MinecraftFonts;
import dev.sixik.unigui.impl.core.DefaultUIContext;
import dev.sixik.unigui.impl.input.MemoryClipboardService;
import dev.sixik.unigui.impl.text.DefaultFontRegistry;
import dev.sixik.unigui.impl.text.TextEngine;
import dev.sixik.unigui.impl.widget.WidgetBase;
import dev.sixik.unigui.widgets.containers.StackPanel;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Manual check of the text path ({@code /unigui text}): vanilla metrics, Cyrillic and CJK through the vanilla font,
 * {@code \u00a7} codes, SDF fonts shipped with UniGUI, a system font, runs that mix both renderers, gradients and inline
 * content. Every row names what it should look like.
 */
public final class TextLab {
    private static final float COLUMN_WIDTH = 236.0f;
    private static final MutableColor WHITE = MutableColor.rgba(1.0f, 1.0f, 1.0f, 1.0f);
    private static final MutableColor GRAY = MutableColor.rgba(0.65f, 0.7f, 0.8f, 1.0f);

    private TextLab() {
    }

    /** Queues the screen for the end of the client tick (see {@link GtnhClientTasks}). */
    public static void open() {
        GtnhClientTasks.post(TextLab::openClient);
    }

    private static void openClient() {
        StackPanel root = new StackPanel();
        root.layout(style -> style.sizePercent(100.0f, 100.0f));
        Board board = new Board(rows());
        board.layout(style -> style.sizePercent(100.0f, 100.0f).align(Alignment.START, Alignment.START));
        root.addChild(board);
        new UniScreen("", root, new DefaultUIContext(new MemoryClipboardService())).openGui();
    }

    /** One labelled sample. */
    private record Row(String caption, float height, Sample sample) {
    }

    /** Draws a sample at the given top-left corner. */
    @FunctionalInterface
    private interface Sample {
        void draw(RenderContext context, float x, float y);
    }

    private static List<Row> rows() {
        List<Row> rows = new ArrayList<>();
        FontFace vanilla = MinecraftFonts.defaultFace();

        rows.add(new Row("metrics probe: H must sit on the green baseline, g and p hang under it, red = descent",
                24.0f, (context, x, y) -> {
            FontMetrics metrics = vanilla.metrics(9.0f);
            float baseline = y + metrics.ascent();
            context.rect(x, baseline, 120.0f, 1.0f, Paint.fill(MutableColor.rgba(0.2f, 1.0f, 0.3f, 1.0f)));
            context.rect(x, baseline + metrics.descent(), 120.0f, 1.0f,
                    Paint.fill(MutableColor.rgba(1.0f, 0.3f, 0.3f, 1.0f)));
            context.text(RichText.of("HxgpЙ Hxgp", vanilla, 9.0f), x, y, 120.0f, 12.0f, Paint.fill(WHITE));
        }));
        rows.add(text("vanilla 9 px and 18 px (scaled, not blurry)", 30.0f, () ->
                RichText.builder().font(vanilla).size(9.0f).append("The quick brown fox  ")
                        .size(18.0f).append("Hgxp").build()));
        rows.add(text("Cyrillic and CJK through the vanilla unicode pages", 24.0f, () ->
                RichText.of("Съешь ещё этих мягких булок 日本語 中文 한국어", vanilla, 9.0f)));
        rows.add(text("legacy codes: red, green, blue, bold, underline, strike, italic, obfuscated, reset", 24.0f, () ->
                RichText.of("\u00a7cRed \u00a7aGreen \u00a79Blue \u00a7lBold \u00a7nUnder \u00a7mStrike \u00a7oItalic \u00a7kXXXX\u00a7r Plain", vanilla, 9.0f)));
        rows.add(text("legacy codes drawn glyph by glyph (tracking): colour and bold must continue after every glyph", 24.0f,
                () -> RichText.builder().font(vanilla).size(9.0f).tracking(0.2f)
                        .append("\u00a76\u00a7lGold bold \u00a7bAqua spaced").build()));
        rows.add(text("legacy codes over a line break: the second line stays red", 34.0f, () ->
                RichText.of("\u00a7cFirst line\nsecond line", vanilla, 9.0f)));
        rows.add(text("legacy codes with a gradient brush: codes win over the gradient, the rest fades", 24.0f, () ->
                RichText.builder().font(vanilla).size(9.0f)
                        .brush(new LinearGradientTextBrush(MutableColor.rgba(1.0f, 0.4f, 0.2f, 1.0f),
                                MutableColor.rgba(0.2f, 0.6f, 1.0f, 1.0f), 0.0f))
                        .append("Gradient \u00a7aGREEN\u00a7r gradient again").build()));

        for (GtnhFonts.Builtin font : GtnhFonts.Builtin.values()) {
            rows.add(text("SDF " + font.id() + " (shipped with UniGUI), 14 px", 20.0f, () ->
                    RichText.of("Hamburgefonstiv 0123 Привет", GtnhFonts.builtin(font), 14.0f)));
        }
        rows.add(text("SDF system font SansSerif, 14 px", 20.0f, () ->
                RichText.of("Sans Serif 日本語 Привет", DefaultFontRegistry.global()
                        .registerSystem("lab_sans", "SansSerif"), 14.0f)));
        rows.add(text("SDF with legacy codes: colours apply, styles are dropped, codes take no space", 20.0f, () ->
                RichText.of("\u00a7cred \u00a7aSDF \u00a7lnot bold\u00a7r plain", GtnhFonts.builtin(GtnhFonts.Builtin.OSWALD),
                        14.0f)));
        rows.add(text("mixed in one run: vanilla 9 px, SDF 16 px, vanilla again, on one baseline", 24.0f, () ->
                RichText.builder().font(vanilla).size(9.0f).append("vanilla ")
                        .font(GtnhFonts.builtin(GtnhFonts.Builtin.PLAY)).size(16.0f).append("SDF ")
                        .font(vanilla).size(9.0f).append("vanilla again").build()));
        rows.add(text("SDF gradient: orange to blue across the word", 22.0f, () ->
                RichText.builder().font(GtnhFonts.builtin(GtnhFonts.Builtin.CUPRUM_BOLD)).size(18.0f)
                        .brush(new LinearGradientTextBrush(MutableColor.rgba(1.0f, 0.5f, 0.1f, 1.0f),
                                MutableColor.rgba(0.2f, 0.5f, 1.0f, 1.0f), 0.0f))
                        .append("Gradient text").build()));
        rows.add(new Row("inline content: an orange square and a green circle sit inside the line", 22.0f,
                (context, x, y) -> {
                    RichText text = RichText.builder().font(vanilla).size(9.0f).append("before ")
                            .inline("lab:square", 12.0f, 12.0f, (draw, inline) -> draw.rect(inline.x(), inline.y(),
                                    inline.width(), inline.height(), Paint.fill(MutableColor.rgba(1.0f, 0.6f, 0.1f, 1.0f))))
                            .append(" between ")
                            .inline("lab:circle", 12.0f, 12.0f, (draw, inline) -> draw.circle(inline.x(), inline.y(),
                                    inline.width(), inline.height(), Paint.fill(MutableColor.rgba(0.2f, 0.9f, 0.4f, 1.0f))))
                            .append(" after").build();
                    TextEngine.draw(context, text, x, y, COLUMN_WIDTH - 8.0f, 16.0f, Paint.fill(WHITE), null,
                            Alignment.START, Alignment.START);
                }));
        return rows;
    }

    private static Row text(String caption, float height, Supplier<RichText> text) {
        return new Row(caption, height, (context, x, y) ->
                context.text(text.get(), x, y, COLUMN_WIDTH - 8.0f, height, Paint.fill(WHITE)));
    }

    /** Lays the rows out in columns and draws them; a row that throws shows the error instead. */
    private static final class Board extends WidgetBase {
        private final List<Row> rows;

        private Board(List<Row> rows) {
            this.rows = rows;
        }

        @Override
        public void measure(LayoutContext context) {
            setDesiredSize(resolveDesiredSize(context, context.availableWidth(), context.availableHeight()));
        }

        @Override
        public void render(RenderContext context) {
            if (visibility() != Visibility.VISIBLE || context == null) return;
            var bounds = layoutBounds();
            context.rect(bounds.x(), bounds.y(), bounds.width(), bounds.height(),
                    Paint.fill(MutableColor.rgba(0.07f, 0.08f, 0.11f, 1.0f)));
            float x = bounds.x() + 6.0f;
            float y = bounds.y() + 6.0f;
            float limit = bounds.y() + bounds.height() - 6.0f;
            for (Row row : rows) {
                float needed = 12.0f + row.height() + 4.0f;
                if (y + needed > limit) {
                    x += COLUMN_WIDTH + 6.0f;
                    y = bounds.y() + 6.0f;
                }
                context.text(row.caption(), x, y, COLUMN_WIDTH - 8.0f, 10.0f, Paint.fill(GRAY));
                try {
                    row.sample().draw(context, x, y + 11.0f);
                } catch (RuntimeException | LinkageError e) {
                    context.text("error: " + e, x, y + 11.0f, COLUMN_WIDTH - 8.0f, 10.0f,
                            Paint.fill(MutableColor.rgba(1.0f, 0.3f, 0.3f, 1.0f)));
                }
                y += needed;
            }
            invalidate(InvalidationFlags.VISUAL);
        }
    }
}
