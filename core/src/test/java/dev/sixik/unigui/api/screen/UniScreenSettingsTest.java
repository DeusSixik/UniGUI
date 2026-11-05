package dev.sixik.unigui.api.screen;

import dev.sixik.unigui.api.input.KeyboardInputHandler;
import dev.sixik.unigui.api.text.FontFace;
import dev.sixik.unigui.api.text.FontMetrics;
import dev.sixik.unigui.api.widget.Widget;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UniScreenSettingsTest {
    @Test
    void defaultFont_roundTripsAndCanBeReset() {
        UniScreen screen = new UniScreen(widget());
        FontFace font = new FontFace() {
            @Override
            public String id() {
                return "test";
            }

            @Override
            public FontMetrics metrics(float pixelSize) {
                return new FontMetrics(1.0f, 0.0f, 0.0f, 1.0f);
            }

            @Override
            public float advance(int codePoint, float pixelSize) {
                return 1.0f;
            }
        };

        assertSame(screen, screen.defaultFont(font));
        assertSame(font, screen.defaultFont());
        screen.defaultFont(null);
        assertSame(null, screen.defaultFont());
    }

    @Test
    void keyboardInputHandler_roundTripsAndReceivesNormalizedEvents() {
        UniScreen screen = new UniScreen(widget());
        KeyboardInputHandler handler = (keyCode, scanCode, modifiers) ->
                keyCode == 65 && scanCode == 12 && modifiers == 3;
        screen.keyboardInputHandler(handler);

        assertSame(handler, screen.keyboardInputHandler());
        assertTrue(handler.keyPressed(65, 12, 3));
        assertFalse(handler.keyPressed(66, 12, 3));
        assertFalse(handler.keyReleased(65, 12, 3));
        assertFalse(handler.textInput('a', 0));
    }

    private static Widget widget() {
        return (Widget) Proxy.newProxyInstance(Widget.class.getClassLoader(), new Class<?>[]{Widget.class},
                (proxy, method, args) -> null);
    }
}
