package dev.sixik.unigui.tests;

import dev.sixik.unigui.api.event.EventPhase;
import dev.sixik.unigui.api.event.PointerEnteredEvent;
import dev.sixik.unigui.api.layout.LayoutContext;
import dev.sixik.unigui.api.math.MutableRect;
import dev.sixik.unigui.api.render.DrawCommand;
import dev.sixik.unigui.api.render.DrawList;
import dev.sixik.unigui.api.widget.visual.BackgroundKind;
import dev.sixik.unigui.api.widget.visual.Surface;
import dev.sixik.unigui.impl.render.DefaultRenderContext;
import dev.sixik.unigui.widgets.containers.Box;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.widgets.feedback.Tooltip;
import dev.sixik.unigui.widgets.feedback.WindowWidget;
import dev.sixik.unigui.widgets.interaction.Button;
import dev.sixik.unigui.widgets.interaction.Checkbox;

/**
 * Проверяет ядро визуала: композируемый Surface вместо двойных renderer'ов.
 *
 * <p>Покрывает автовывод {@link BackgroundKind}, композицию Button поверх Surface
 * вместо наследования Box и однократную отрисовку фона overlay-виджетов.</p>
 */
public final class SurfaceVisualSelfTest {
    private SurfaceVisualSelfTest() {
    }

    public static void main(String[] args) {
        expect(BackgroundKind.parse("color") == BackgroundKind.COLOR, "parse color");
        expect(BackgroundKind.parse("fill") == BackgroundKind.COLOR, "parse fill alias");
        expect(BackgroundKind.parse("texture") == BackgroundKind.TEXTURE, "parse texture");
        expect(BackgroundKind.parse("image") == BackgroundKind.TEXTURE, "parse image alias");
        expect(BackgroundKind.parse("shader") == BackgroundKind.SHADER, "parse shader");
        expect(BackgroundKind.parse("none") == BackgroundKind.NONE, "parse none");
        expect(BackgroundKind.parse(null) == null, "parse null");
        expect(BackgroundKind.parse("  ") == null, "parse blank");
        expectThrows(() -> BackgroundKind.parse("bogus"), "parse bogus should throw");

        Surface auto = new Surface(null);
        expect(auto.effectiveKind() == BackgroundKind.NONE, "default kind is NONE");
        auto.backgroundVisible(true);
        expect(auto.effectiveKind() == BackgroundKind.COLOR, "visible surface defaults to COLOR");

        Button button = new Button("Test");
        expect(!(((Object) button) instanceof Box), "button no longer inherits Box visual");
        expect(button.surface() != null, "button owns a surface");
        expect(button.backgroundVisible() && button.borderVisible(), "button chrome enabled by default");
        expect(button.renderer() == null, "button has no custom renderer by default");
        expect(button.backgroundKind() == null, "button kind is automatic by default");

        Box box = new Box();
        expect(box.surface() != null, "box owns a surface");
        expect(box.renderer() == null, "box has no custom renderer by default");
        expect(new Checkbox("Check") instanceof Button, "checkbox still is a button");
        expect(!(((Object) new Checkbox("Check")) instanceof Box), "checkbox no longer inherits Box visual");

        // Overlay windows draw their background exactly once through the surface.
        WindowWidget window = new WindowWidget("Title", new Label("Body"));
        window.open();
        window.measure(new LayoutContext(220.0f, 120.0f));
        window.arrange(new MutableRect(0.0f, 0.0f, 220.0f, 120.0f));
        DrawList windowDraw = new DrawList();
        window.render(new DefaultRenderContext(windowDraw));
        expect(countFill(windowDraw, 0.030f, 0.035f, 0.050f, 0.98f) == 1,
                "window background is drawn exactly once by the surface");

        // Tooltip background comes from the surface as well.
        Box anchor = new Box();
        anchor.arrange(new MutableRect(0.0f, 0.0f, 60.0f, 20.0f));
        Tooltip tooltip = new Tooltip(anchor, "Hi");
        anchor.handle(new PointerEnteredEvent(anchor, anchor, EventPhase.TARGET, 0.0f, 0.0f, 0.0f, 0.0f, 0));
        expect(tooltip.showing(), "hovered anchor shows tooltip");
        tooltip.measure(new LayoutContext(220.0f, 120.0f));
        tooltip.arrange(new MutableRect(0.0f, 0.0f, 120.0f, 40.0f));
        DrawList tooltipDraw = new DrawList();
        tooltip.render(new DefaultRenderContext(tooltipDraw));
        expect(countFill(tooltipDraw, 0.02f, 0.025f, 0.035f, 0.94f) == 1,
                "tooltip background is drawn exactly once by the surface");

        System.out.println("SurfaceVisualSelfTest passed");
    }

    private static int countFill(DrawList drawList, float r, float g, float b, float a) {
        int count = 0;
        Object[] commands = drawList.commandElements();
        for (int i = 0, size = drawList.size(); i < size; i++) {
            if (commands[i] instanceof DrawCommand command
                    && command.paint() != null
                    && !command.paint().isStroke()
                    && near(command.paint().color().r(), r)
                    && near(command.paint().color().g(), g)
                    && near(command.paint().color().b(), b)
                    && near(command.paint().color().a(), a)) {
                count++;
            }
        }
        return count;
    }

    private static boolean near(float actual, float expected) {
        return Math.abs(actual - expected) <= 0.01f;
    }

    private static void expect(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void expectThrows(Runnable action, String message) {
        try {
            action.run();
        } catch (IllegalArgumentException expected) {
            return;
        }
        throw new AssertionError(message);
    }
}
