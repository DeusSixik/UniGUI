package dev.sixik.unigui.impl.text;

import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.math.RectView;
import dev.sixik.unigui.api.render.DrawCommand;
import dev.sixik.unigui.api.render.DrawCommandType;
import dev.sixik.unigui.api.render.DrawList;
import dev.sixik.unigui.api.render.DrawScope;
import dev.sixik.unigui.api.render.Paint;
import dev.sixik.unigui.api.text.RichText;
import dev.sixik.unigui.impl.render.DefaultRenderContext;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Гарантирует, что градиент текста считается по общему extent'у всего текста, а не по bounds
 * отдельной draw-команды (контейнера, строки или chunk'а).
 */
class TextBrushBoundsTest {
    private static final float EPS = 0.001f;
    private static final MutableColor RED = new MutableColor(1, 0, 0, 1);
    private static final MutableColor BLUE = new MutableColor(0, 0, 1, 1);

    private static Paint paint() {
        return Paint.fill(new MutableColor(1, 1, 1, 1));
    }

    private static List<DrawCommand> textCommands(DefaultRenderContext context) {
        List<DrawCommand> result = new ArrayList<>();
        for (DrawCommand command : context.drawList().commands()) {
            if (command.type() == DrawCommandType.TEXT) result.add(command);
        }
        return result;
    }

    @Test
    void plainGradient_usesMeasuredTextExtent_notContainerWidth() {
        DefaultRenderContext context = new DefaultRenderContext(new DrawList());
        RichText text = RichText.gradient("abcd", null, 10.0f, RED, BLUE, 0.0f);

        TextEngine.drawInline(new DrawScope(context, null), text, 10.0f, 20.0f, 200.0f, 12.0f, paint());

        List<DrawCommand> commands = textCommands(context);
        assertEquals(1, commands.size());
        DrawCommand command = commands.get(0);
        assertEquals(200.0f, command.bounds().width(), EPS, "command bounds keep the container width");
        assertTrue(command.hasBrushBounds());
        RectView brush = command.brushBounds();
        assertEquals(10.0f, brush.x(), EPS);
        assertEquals(20.0f, brush.y(), EPS);
        assertEquals(TextEngine.measureLineWidth(text), brush.width(), EPS, "gradient spans the text, not the box");
        assertTrue(brush.width() < 200.0f);
    }

    @Test
    void mixedContent_chunksShareOneGradientRect() {
        DefaultRenderContext context = new DefaultRenderContext(new DrawList());
        RichText text = RichText.builder()
                .linearGradient(RED, BLUE, 0.0f)
                .append("ab")
                .inline("icon", 8.0f, 8.0f, (draw, ctx) -> { })
                .append("cd")
                .build();

        TextEngine.drawInline(new DrawScope(context, null), text, 10.0f, 20.0f, 200.0f, 12.0f, paint());

        List<DrawCommand> commands = textCommands(context);
        assertEquals(2, commands.size(), "text before and after the icon are separate commands");
        RectView first = commands.get(0).brushBounds();
        RectView second = commands.get(1).brushBounds();
        assertTrue(commands.get(0).hasBrushBounds() && commands.get(1).hasBrushBounds());
        assertEquals(first.x(), second.x(), EPS);
        assertEquals(first.width(), second.width(), EPS);
        assertEquals(TextEngine.measureLineWidth(text), first.width(), EPS,
                "shared rect covers text and the inline icon");
        assertTrue(first.width() > commands.get(0).bounds().width(), "wider than a single chunk");
    }

    @Test
    void outerScope_winsOverInnerCalls() {
        DefaultRenderContext context = new DefaultRenderContext(new DrawList());
        RichText line = RichText.gradient("ab", null, 10.0f, RED, BLUE, 90.0f);
        DrawScope draw = new DrawScope(context, null);

        context.pushTextBrushBounds(0.0f, 0.0f, 100.0f, 50.0f);
        try {
            TextEngine.drawInline(draw, line, 5.0f, 5.0f, 40.0f, 10.0f, paint());
            TextEngine.drawInline(draw, line, 5.0f, 25.0f, 40.0f, 10.0f, paint());
        } finally {
            context.popTextBrushBounds();
        }

        List<DrawCommand> commands = textCommands(context);
        assertEquals(2, commands.size());
        for (DrawCommand command : commands) {
            assertEquals(0.0f, command.brushBounds().x(), EPS);
            assertEquals(100.0f, command.brushBounds().width(), EPS);
            assertEquals(50.0f, command.brushBounds().height(), EPS);
        }
        assertNull(context.textBrushBounds(), "scope is closed after pop");
    }

    @Test
    void commandsOutsideScope_andSolidText_keepOwnBounds() {
        DefaultRenderContext context = new DefaultRenderContext(new DrawList());
        DrawScope draw = new DrawScope(context, null);

        TextEngine.drawInline(draw, RichText.plain("solid"), 0.0f, 0.0f, 100.0f, 10.0f, paint());
        context.pushTextBrushBounds(0.0f, 0.0f, 10.0f, 10.0f);
        context.popTextBrushBounds();
        context.text("later", 0.0f, 0.0f, 30.0f, 10.0f, paint());

        for (DrawCommand command : textCommands(context)) {
            assertFalse(command.hasBrushBounds());
            assertEquals(command.bounds().width(), command.brushBounds().width(), EPS);
        }
    }

    @Test
    void brushBounds_survivesCopyAndResets() {
        DrawCommand command = new DrawCommand(DrawCommandType.TEXT).brushBounds(1.0f, 2.0f, 3.0f, 4.0f);
        DrawCommand copy = command.copy();

        assertNotNull(copy.brushBounds());
        assertTrue(copy.hasBrushBounds());
        assertEquals(3.0f, copy.brushBounds().width(), EPS);

        command.brushBounds((RectView) null);
        assertFalse(command.hasBrushBounds());
        assertTrue(copy.hasBrushBounds(), "copy is independent");
    }
}
