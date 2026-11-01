package dev.sixik.unigui.widgets.interaction;

import dev.sixik.unigui.api.event.KeyPressedEvent;
import dev.sixik.unigui.api.event.PointerMovedEvent;
import dev.sixik.unigui.api.event.PointerPressedEvent;
import dev.sixik.unigui.api.event.PointerReleasedEvent;
import dev.sixik.unigui.api.input.KeyCodes;
import dev.sixik.unigui.api.input.PointerButton;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.render.DrawCommand;
import dev.sixik.unigui.api.render.DrawList;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.impl.core.DefaultUIContext;
import dev.sixik.unigui.impl.render.DefaultRenderContext;
import dev.sixik.unigui.widgets.core.Orientation;
import dev.sixik.unigui.widgets.feedback.ProgressBar;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Вертикальные Slider и ProgressBar: размеры, значение от положения указателя, клавиши и отрисовка.
 */
class VerticalRangeControlsTest {
    private static final float EPS = 0.01f;

    private static void layout(Widget widget, float x, float y, float w, float h) {
        widget.measure(new LayoutContext(400.0f, 400.0f));
        widget.arrange(new MutableRect(x, y, w, h));
    }

    private static Slider verticalSlider() {
        Slider slider = new Slider().range(0.0f, 100.0f).orientation(Orientation.VERTICAL);
        layout(slider, 10.0f, 20.0f, 18.0f, 120.0f);
        return slider;
    }

    private static void press(Slider slider, float rootX, float rootY) {
        slider.handle(new PointerPressedEvent(slider, rootX, rootY, rootX - 10.0f, rootY - 20.0f, 0, PointerButton.PRIMARY));
    }

    private static void move(Slider slider, float rootX, float rootY) {
        slider.handle(new PointerMovedEvent(slider, rootX, rootY, rootX - 10.0f, rootY - 20.0f, 0));
    }

    private static void release(Slider slider, float rootX, float rootY) {
        slider.handle(new PointerReleasedEvent(slider, rootX, rootY, rootX - 10.0f, rootY - 20.0f, 0, PointerButton.PRIMARY));
    }

    // ---- размеры ----

    @Test
    void aVerticalSlider_isTallByDefault() {
        Slider slider = new Slider().orientation(Orientation.VERTICAL);

        assertEquals(Slider.DEFAULT_PREFERRED_HEIGHT, slider.preferredWidth(), EPS);
        assertEquals(Slider.DEFAULT_PREFERRED_WIDTH, slider.preferredHeight(), EPS);
    }

    @Test
    void customSizes_surviveAnOrientationChange() {
        Slider slider = new Slider().preferredSize(200.0f, 30.0f).orientation(Orientation.VERTICAL);

        assertEquals(200.0f, slider.preferredWidth(), EPS);
        assertEquals(30.0f, slider.preferredHeight(), EPS);
    }

    @Test
    void switchingBack_restoresTheHorizontalDefaults() {
        Slider slider = new Slider().orientation(Orientation.VERTICAL).orientation(Orientation.HORIZONTAL);

        assertEquals(Slider.DEFAULT_PREFERRED_WIDTH, slider.preferredWidth(), EPS);
        assertEquals(Slider.DEFAULT_PREFERRED_HEIGHT, slider.preferredHeight(), EPS);
    }

    @Test
    void aNullOrientation_meansHorizontal() {
        assertEquals(Orientation.HORIZONTAL, new Slider().orientation(null).orientation());
        assertEquals(Orientation.HORIZONTAL, new ProgressBar().orientation(null).orientation());
    }

    // ---- слайдер: указатель ----

    @Test
    void verticalSlider_topIsMaxAndBottomIsMin() {
        Slider slider = verticalSlider();

        press(slider, 19.0f, 20.0f);
        assertEquals(100.0f, slider.value(), EPS);
        move(slider, 19.0f, 140.0f);
        assertEquals(0.0f, slider.value(), EPS);
        move(slider, 19.0f, 80.0f);
        assertEquals(50.0f, slider.value(), EPS);
    }

    @Test
    void verticalSlider_clampsOutsideTheTrack() {
        Slider slider = verticalSlider();

        press(slider, 19.0f, 80.0f);
        move(slider, 19.0f, -500.0f);
        assertEquals(100.0f, slider.value(), EPS);
        move(slider, 19.0f, 900.0f);
        assertEquals(0.0f, slider.value(), EPS);
    }

    @Test
    void verticalSlider_ignoresHorizontalPointerMovement() {
        Slider slider = verticalSlider();
        press(slider, 19.0f, 80.0f);

        move(slider, 400.0f, 80.0f);

        assertEquals(50.0f, slider.value(), EPS);
    }

    @Test
    void dragging_startsOnPressAndEndsOnRelease() {
        Slider slider = verticalSlider();

        press(slider, 19.0f, 50.0f);
        assertTrue(slider.dragging());
        release(slider, 19.0f, 50.0f);

        assertFalse(slider.dragging());
    }

    @Test
    void horizontalSlider_stillReadsTheXAxis() {
        Slider slider = new Slider().range(0.0f, 100.0f);
        layout(slider, 0.0f, 0.0f, 100.0f, 18.0f);

        slider.handle(new PointerPressedEvent(slider, 25.0f, 400.0f, 25.0f, 9.0f, 0, PointerButton.PRIMARY));

        assertEquals(25.0f, slider.value(), EPS);
    }

    // ---- слайдер: клавиши ----

    @Test
    void verticalSlider_upAndDownNudgeTheValue() {
        Slider slider = verticalSlider();
        slider.value(50.0f);
        DefaultUIContext context = new DefaultUIContext();
        slider.setUiContextInternal(context);
        context.focusManager().requestFocus(slider);

        slider.handle(new KeyPressedEvent(slider, KeyCodes.UP, 0, 0));
        assertEquals(55.0f, slider.value(), EPS);
        slider.handle(new KeyPressedEvent(slider, KeyCodes.DOWN, 0, 0));
        slider.handle(new KeyPressedEvent(slider, KeyCodes.DOWN, 0, 0));
        assertEquals(45.0f, slider.value(), EPS);
    }

    @Test
    void horizontalSlider_ignoresUpAndDown() {
        Slider slider = new Slider().range(0.0f, 100.0f).value(50.0f);
        DefaultUIContext context = new DefaultUIContext();
        slider.setUiContextInternal(context);
        context.focusManager().requestFocus(slider);

        slider.handle(new KeyPressedEvent(slider, KeyCodes.UP, 0, 0));

        assertEquals(50.0f, slider.value(), EPS);
    }

    // ---- отрисовка ----

    private static DrawList render(Widget widget) {
        DrawList list = new DrawList();
        widget.render(new DefaultRenderContext(list));
        return list;
    }

    private static boolean hasCommandWithHeight(DrawList list, float height, float bottom) {
        for (DrawCommand command : list.commands()) {
            if (Math.abs(command.bounds().height() - height) < 0.5f
                    && Math.abs(command.bounds().y() + command.bounds().height() - bottom) < 0.5f) {
                return true;
            }
        }
        return false;
    }

    @Test
    void verticalProgressBar_fillsFromTheBottom() {
        ProgressBar bar = new ProgressBar().orientation(Orientation.VERTICAL).range(0.0f, 100.0f).value(25.0f);
        layout(bar, 0.0f, 0.0f, 12.0f, 120.0f);

        DrawList list = render(bar);

        assertTrue(hasCommandWithHeight(list, 30.0f, 120.0f), "25% of 120 is a 30px fill touching the bottom edge");
    }

    @Test
    void horizontalProgressBar_isUnchanged() {
        ProgressBar bar = new ProgressBar().range(0.0f, 100.0f).value(50.0f);
        layout(bar, 0.0f, 0.0f, 120.0f, 12.0f);

        DrawList list = render(bar);

        boolean found = false;
        for (DrawCommand command : list.commands()) {
            if (Math.abs(command.bounds().width() - 60.0f) < 0.5f && Math.abs(command.bounds().height() - 12.0f) < 0.5f) {
                found = true;
            }
        }
        assertTrue(found, "50% of 120 is a 60px wide fill");
    }

    @Test
    void verticalSlider_drawsFillAndKnobAlongTheVerticalAxis() {
        Slider slider = verticalSlider();
        slider.value(50.0f);

        DrawList list = render(slider);

        assertTrue(hasCommandWithHeight(list, 60.0f, 140.0f), "the fill covers the lower half of the track");
    }

    @Test
    void indeterminateVerticalProgress_rendersWithoutError() {
        ProgressBar bar = new ProgressBar().orientation(Orientation.VERTICAL).indeterminate(true);
        layout(bar, 0.0f, 0.0f, 12.0f, 120.0f);

        assertTrue(render(bar).size() > 0);
    }
}
