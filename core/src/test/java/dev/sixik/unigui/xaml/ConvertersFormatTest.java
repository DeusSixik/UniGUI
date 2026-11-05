package dev.sixik.unigui.xaml;

import dev.sixik.unigui.api.layout.Align;
import dev.sixik.unigui.api.layout.FlexDirection;
import dev.sixik.unigui.api.layout.GridTrack;
import dev.sixik.unigui.api.layout.Overflow;
import dev.sixik.unigui.api.layout.SizeValue;
import dev.sixik.unigui.api.render.TextureWrap;
import dev.sixik.unigui.xaml.schema.Converters;
import dev.sixik.unigui.xaml.schema.LoadContext;
import dev.sixik.unigui.xaml.schema.ValueConverter;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Инвариант обратимости: {@code format(parse(text)) == text} для канонических записей и
 * {@code parse(format(v))} возвращает то же значение.
 */
final class ConvertersFormatTest {
    private static final LoadContext CONTEXT = LoadContext.DEFAULT;

    /** Каноническая запись должна пройти туда-обратно без изменений. */
    private static <V> void canonical(ValueConverter<V> converter, String... texts) {
        for (String text : texts) {
            V value = converter.convert(text, CONTEXT);
            assertEquals(text, converter.format(value), "format(parse(\"" + text + "\"))");
            String again = converter.format(converter.convert(converter.format(value), CONTEXT));
            assertEquals(text, again, "stable on the second pass");
        }
    }

    @Test
    void numbersUseTheShortestExactRepresentation() {
        canonical(Converters.FLOAT, "0", "1", "-1.5", "0.1", "3.14159", "123456.78", "1.0E10", "NaN");
        canonical(Converters.DOUBLE, "0", "2.5", "-3");
        canonical(Converters.INT, "0", "-7", "2147483647");
        for (float value : new float[]{0.1f, 1.0f / 3.0f, 1.0e-7f, 16777216f, 0.85f, Float.MIN_VALUE}) {
            assertEquals(value, Converters.FLOAT.convert(Converters.FLOAT.format(value), CONTEXT),
                    "float " + value + " must survive exactly, got " + Converters.FLOAT.format(value));
        }
    }

    @Test
    void autoFloatKeepsTheAutoMarker() {
        canonical(Converters.FLOAT_OR_AUTO, "auto", "12", "0.5");
        assertTrue(Float.isNaN(Converters.FLOAT_OR_AUTO.convert("auto", CONTEXT)));
    }

    @Test
    void booleansStringsAndColors() {
        canonical(Converters.BOOLEAN, "true", "false");
        canonical(Converters.STRING, "", "plain", "with {braces} and \"quotes\"");
        canonical(Converters.COLOR, "#11223344", "#00000000", "#FFFFFFFF", "#80A0C0CC");
        canonical(Converters.RECT, "0 0 1 1", "0.25 0 0.5 1");
    }

    @Test
    void sizesCoverEveryKeyword() {
        canonical(Converters.SIZE, "auto", "content", "min-content", "max-content", "fit-content(12px)",
                "12", "40%", "12.5%", "0");
        assertEquals(SizeValue.px(12), Converters.SIZE.convert(Converters.SIZE.format(SizeValue.px(12)), CONTEXT));
        assertEquals(SizeValue.percent(40), Converters.SIZE.convert(Converters.SIZE.format(SizeValue.percent(40)), CONTEXT));
    }

    @Test
    void insetsAreWrittenInTheShortestForm() {
        assertEquals("5", Converters.INSETS.format(Converters.INSETS.convert("5", CONTEXT)));
        assertEquals("1 2 3 4", Converters.INSETS.format(Converters.INSETS.convert("1 2 3 4", CONTEXT)));
        assertEquals("1 2 1 2", Converters.INSETS.format(Converters.INSETS.convert("1 2", CONTEXT)),
                "the two-value form expands to the explicit four-value one");
        canonical(Converters.SLICE_BORDERS, "1 2 3 4", "6 6 6 6", "0 0 0 0");
    }

    @Test
    void autoMarginsAndGridTracks() {
        canonical(Converters.AUTO_MARGINS, "none", "all", "horizontal", "vertical", "left", "left top");
        assertEquals("vertical", Converters.AUTO_MARGINS.format(Converters.AUTO_MARGINS.convert("top bottom", CONTEXT)));

        canonical(Converters.GRID_TRACKS, "100 1fr auto 50%", "minmax(100, 1fr) auto", "2fr");
        List<GridTrack> repeated = Converters.GRID_TRACKS.convert("repeat(3, 1fr)", CONTEXT);
        assertEquals("1fr 1fr 1fr", Converters.GRID_TRACKS.format(repeated), "repeat() is expanded when written");
        assertEquals("", Converters.GRID_TRACKS.format(List.of()));
    }

    @Test
    void enumsUseLowerKebabCase() {
        canonical(Converters.enumOf(FlexDirection.class), "column", "row");
        canonical(Converters.enumOf(Align.class), "stretch", "center");
        canonical(Converters.enumOf(Overflow.class), "visible");
        for (TextureWrap wrap : TextureWrap.values()) {
            ValueConverter<TextureWrap> converter = Converters.enumOf(TextureWrap.class);
            assertEquals(wrap, converter.convert(converter.format(wrap), CONTEXT), wrap.name());
        }
        assertEquals("clamp-to-edge", Converters.enumOf(TextureWrap.class).format(TextureWrap.CLAMP_TO_EDGE));
    }

    @Test
    void resourcesRoundTripThroughTheirIds() {
        canonical(Converters.TEXTURE, "test:texture", "minecraft:textures/item/diamond.png");
        canonical(Converters.SHADER, "unigui:glow");
    }

    @Test
    void convertersWithoutAFormatterSayTheyCannotFormat() {
        ValueConverter<String> readOnly = ValueConverter.of(String.class, dev.sixik.unigui.xaml.schema.ValueKind.STRING, v -> v);
        assertFalse(readOnly.canFormat());
        assertThrows(UnsupportedOperationException.class, () -> readOnly.format("x"));
        assertTrue(Converters.FLOAT.canFormat() && Converters.SIZE.canFormat() && Converters.GRID_TRACKS.canFormat());
    }
}
