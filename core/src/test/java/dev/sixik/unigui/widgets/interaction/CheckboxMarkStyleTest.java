package dev.sixik.unigui.widgets.interaction;

import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.math.MutableColor;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.render.DrawCommand;
import dev.sixik.unigui.api.render.DrawCommandType;
import dev.sixik.unigui.api.render.DrawList;
import dev.sixik.unigui.api.style.CheckboxMarkShape;
import dev.sixik.unigui.api.style.StyleKeyRegistry;
import dev.sixik.unigui.api.style.StyleKeys;
import dev.sixik.unigui.api.widget.CheckboxState;
import dev.sixik.unigui.impl.render.DefaultRenderContext;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Цвет и форма метки Checkbox через style-ключи.
 */
class CheckboxMarkStyleTest {
    private static final MutableColor WHITE = MutableColor.rgba(1.0f, 1.0f, 1.0f, 1.0f);
    private static final MutableColor RED = MutableColor.rgba(1.0f, 0.0f, 0.0f, 1.0f);

    private static DrawList render(CheckboxMarkShape shape, MutableColor color, CheckboxState state) {
        Checkbox checkbox = new Checkbox().checkAnimation(0.0f);
        checkbox.style(style -> {
            if (shape != null) style.put(StyleKeys.CHECKBOX_MARK_SHAPE, shape);
            if (color != null) style.put(StyleKeys.CHECKBOX_MARK_COLOR, color);
        });
        checkbox.silentState(state);
        checkbox.measure(new LayoutContext(200.0f, 40.0f));
        checkbox.arrange(new MutableRect(0.0f, 0.0f, 12.0f, 12.0f));
        DrawList list = new DrawList();
        checkbox.render(new DefaultRenderContext(list));
        return list;
    }

    private static List<DrawCommand> of(DrawList list, DrawCommandType type) {
        List<DrawCommand> found = new ArrayList<>();
        for (DrawCommand command : list.commands()) {
            if (command.type() == type) found.add(command);
        }
        return found;
    }

    private static boolean isWhite(DrawCommand command) {
        var c = command.paint().color();
        return c.r() > 0.99f && c.g() > 0.99f && c.b() > 0.99f;
    }

    @Test
    void theKeysAreRegisteredForEditors() {
        StyleKeyRegistry registry = StyleKeyRegistry.builtIns();

        assertTrue(registry.descriptor(StyleKeys.CHECKBOX_MARK_COLOR).isPresent());
        assertEquals(CheckboxMarkShape.CHECK,
                registry.descriptor(StyleKeys.CHECKBOX_MARK_SHAPE).orElseThrow().codec().parse("check"));
    }

    @Test
    void theDefaultShapeIsStillADot() {
        DrawList list = render(null, null, CheckboxState.CHECKED);

        assertEquals(1, of(list, DrawCommandType.CIRCLE).size());
        assertTrue(of(list, DrawCommandType.LINE).isEmpty());
    }

    @Test
    void aCheckedDot_takesTheMarkColor() {
        DrawList list = render(CheckboxMarkShape.DOT, WHITE, CheckboxState.CHECKED);

        List<DrawCommand> dots = of(list, DrawCommandType.CIRCLE);
        assertEquals(1, dots.size());
        assertTrue(isWhite(dots.get(0)));
    }

    @Test
    void aCheck_isTwoWhiteLines() {
        DrawList list = render(CheckboxMarkShape.CHECK, WHITE, CheckboxState.CHECKED);

        List<DrawCommand> lines = of(list, DrawCommandType.LINE);
        assertEquals(2, lines.size());
        assertTrue(lines.stream().allMatch(CheckboxMarkStyleTest::isWhite));
        assertTrue(of(list, DrawCommandType.CIRCLE).isEmpty());
    }

    @Test
    void aCross_isTwoLines() {
        DrawList list = render(CheckboxMarkShape.CROSS, RED, CheckboxState.CHECKED);

        assertEquals(2, of(list, DrawCommandType.LINE).size());
    }

    @Test
    void aSquare_isAFilledRectangleOfTheCheckSize() {
        DrawList list = render(CheckboxMarkShape.SQUARE, WHITE, CheckboxState.CHECKED);

        boolean found = of(list, DrawCommandType.RECT).stream()
                .anyMatch(c -> Math.abs(c.bounds().width() - 6.0f) < 0.01f && isWhite(c));
        assertTrue(found, "a 6x6 white square");
    }

    @Test
    void anUncheckedBox_drawsNoMark() {
        DrawList list = render(CheckboxMarkShape.CHECK, WHITE, CheckboxState.UNCHECKED);

        assertTrue(of(list, DrawCommandType.LINE).isEmpty());
        assertTrue(of(list, DrawCommandType.CIRCLE).isEmpty());
    }

    @Test
    void anIndeterminateBox_staysADashWhateverTheShape() {
        DrawList list = render(CheckboxMarkShape.CHECK, WHITE, CheckboxState.INDETERMINATE);

        assertTrue(of(list, DrawCommandType.LINE).isEmpty());
        assertTrue(of(list, DrawCommandType.RECT).stream().anyMatch(CheckboxMarkStyleTest::isWhite));
    }
}
