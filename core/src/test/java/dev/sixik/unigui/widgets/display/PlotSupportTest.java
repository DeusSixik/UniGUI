package dev.sixik.unigui.widgets.display;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Математика шкалы осей и форматирование подписей.
 */
class PlotSupportTest {
    /**
     * Допуск сравнения float: значения шкалы получаются умножением double на степень десяти и приведением к float,
     * относительная ошибка порядка 1e-7; 1e-5 с запасом покрывает её для значений до 1e3.
     */
    private static final float EPSILON = 1.0e-5f;

    @ParameterizedTest
    @CsvSource({
            // value, round, expected
            "1.0,  true,  1",
            "1.4,  true,  1",
            "1.5,  true,  2",
            "2.9,  true,  2",
            "3.0,  true,  5",
            "6.9,  true,  5",
            "7.0,  true,  10",
            "1.0,  false, 1",
            "1.1,  false, 2",
            "2.0,  false, 2",
            "4.0,  false, 5",
            "5.1,  false, 10",
            "75.0, false, 100",
            "12.5, true,  10",
            "0.25, true,  0.2"
    })
    void niceNumber_variousMantissas_snapsToOneTwoFiveTen(float value, boolean round, float expected) {
        assertEquals(expected, PlotSupport.niceNumber(value, round), Math.max(EPSILON, expected * 1.0e-5f));
    }

    @ParameterizedTest
    @CsvSource({"0", "-3", "NaN", "Infinity"})
    void niceNumber_nonPositiveOrNonFinite_returnsOne(float value) {
        assertEquals(1.0f, PlotSupport.niceNumber(value, true), EPSILON);
    }

    @Test
    void niceScale_zeroToSeventyFive_extendsToEighty() {
        float[] out = new float[3];
        // range = nice(75) = 100; step = nice(100 / 4) = 20; max = ceil(75 / 20) * 20 = 80.
        PlotSupport.niceScale(0.0f, 75.0f, 5, out);
        assertArrayEquals(new float[]{0.0f, 80.0f, 20.0f}, out, EPSILON);
    }

    @Test
    void niceScale_eighteenToSixtyTwo_snapsToTens() {
        float[] out = new float[3];
        PlotSupport.niceScale(18.0f, 62.0f, 5, out);
        assertArrayEquals(new float[]{10.0f, 70.0f, 10.0f}, out, EPSILON);
    }

    @Test
    void niceScale_equalValues_expandsSymmetrically() {
        float[] out = new float[3];
        // Вырожденный диапазон раскрывается на max(1, 10%) в обе стороны: [4, 6], шаг 0.5.
        PlotSupport.niceScale(5.0f, 5.0f, 5, out);
        assertArrayEquals(new float[]{4.0f, 6.0f, 0.5f}, out, EPSILON);
    }

    @Test
    void niceScale_reversedBounds_areSwapped() {
        float[] forward = new float[3];
        float[] reversed = new float[3];
        PlotSupport.niceScale(0.0f, 75.0f, 5, forward);
        PlotSupport.niceScale(75.0f, 0.0f, 5, reversed);
        assertArrayEquals(forward, reversed, EPSILON);
    }

    @Test
    void niceScale_negativeRange_coversBothSidesOfZero() {
        float[] out = new float[3];
        PlotSupport.niceScale(-7.0f, 12.0f, 5, out);
        assertTrue(out[0] <= -7.0f && out[1] >= 12.0f, "scale must contain the data");
        assertEquals(0.0f, out[0] % out[2], EPSILON, "lower bound must be a multiple of the step");
    }

    @Test
    void niceScale_nonFiniteInputs_fallBackToUnitRange() {
        float[] out = new float[3];
        PlotSupport.niceScale(Float.NaN, Float.POSITIVE_INFINITY, 5, out);
        assertEquals(0.0f, out[0], EPSILON);
        assertTrue(out[1] >= 1.0f);
        assertTrue(out[2] > 0.0f);
    }

    @ParameterizedTest
    @CsvSource({
            "3,       3",
            "-4,      -4",
            "2.5,     2.5",
            "2.456,   2.46",
            "0.1,     0.1",
            "1200000, 1200000",
            "-0.001,  0"
    })
    void formatValue_typicalNumbers_trimsTrailingZeros(float value, String expected) {
        assertEquals(expected, PlotSupport.formatValue(value));
    }

    @Test
    void formatValue_nonFinite_usesFloatToString() {
        assertEquals("NaN", PlotSupport.formatValue(Float.NaN));
        assertEquals("Infinity", PlotSupport.formatValue(Float.POSITIVE_INFINITY));
    }

    @ParameterizedTest
    @CsvSource({
            // value, step, expected
            "20,    10,    20",
            "0.5,   0.25,  0.50",
            "0.3,   0.1,   0.3",
            "2.5,   0.5,   2.5",
            "-0.0,  1,     0",
            "-0.001, 0.1,  0.0"
    })
    void formatTick_stepDecidesDecimals(float value, float step, String expected) {
        assertEquals(expected, PlotSupport.formatTick(value, step));
    }

    @ParameterizedTest
    @CsvSource({
            "5,  0, 10, 5",
            "-1, 0, 10, 0",
            "11, 0, 10, 10",
            "5,  8, 3,  8"
    })
    void clamp_valueOutsideRange_isPinned(float value, float min, float max, float expected) {
        // Последний набор: вырожденный отрезок (max < min) возвращает min, чтобы подсказка прижималась влево.
        assertEquals(expected, PlotSupport.clamp(value, min, max), EPSILON);
    }
}
