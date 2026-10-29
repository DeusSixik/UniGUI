package dev.sixik.unigui.api.input;

import dev.sixik.unigui.api.core.UIContext;
import dev.sixik.unigui.api.event.KeyPressedEvent;
import dev.sixik.unigui.api.event.KeyReleasedEvent;
import dev.sixik.unigui.api.event.PointerPressedEvent;
import dev.sixik.unigui.api.event.RoutedEventDispatcher;
import dev.sixik.unigui.api.event.ScrollEvent;
import dev.sixik.unigui.api.event.TextInputEvent;
import dev.sixik.unigui.api.event.WidgetEvent;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.impl.input.DefaultHoverManager;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InputHandleTest {
    @Test
    void keyPressAndRelease_updatesKeyboardAndFocusedWidget() {
        Fixture fixture = new Fixture();
        InputHandle input = fixture.input();

        assertFalse(input.keyPressed(KeyCodes.A, 17, 0));
        assertTrue(fixture.keyboard.isDown(KeyCodes.A));
        assertFalse(input.keyReleased(KeyCodes.A, 17, 0));

        assertTrue(fixture.keyboard.wasPressed(KeyCodes.A));
        assertTrue(fixture.keyboard.wasReleased(KeyCodes.A));
        assertEquals(2, fixture.events.size());
        assertTrue(fixture.events.get(0) instanceof KeyPressedEvent);
        assertTrue(fixture.events.get(1) instanceof KeyReleasedEvent);
    }

    @Test
    void pointerPressAndScroll_hitTargetWithLogicalCoordinates() {
        Fixture fixture = new Fixture();
        InputHandle input = fixture.input();

        assertFalse(input.pointerPressed(14.0f, 9.0f, 0, PointerButton.PRIMARY));
        assertFalse(input.pointerScrolled(14.0f, 9.0f, 0.0f, 1.0f, KeyModifiers.SHIFT, 0));

        assertTrue(fixture.events.get(1) instanceof PointerPressedEvent);
        assertTrue(fixture.events.get(2) instanceof ScrollEvent);
        ScrollEvent scroll = (ScrollEvent) fixture.events.get(2);
        assertEquals(KeyModifiers.SHIFT, scroll.modifiers());
        assertEquals(4.0f, scroll.localX(), 0.0f);
        assertEquals(4.0f, scroll.localY(), 0.0f);
    }

    @Test
    void keyTyped_printableKeyConsumedStillDispatchesText() {
        Fixture fixture = new Fixture();
        fixture.consumeKeyPress = true;

        assertTrue(fixture.input().keyTyped('a', KeyCodes.A, 0, 0));

        assertEquals(2, fixture.events.size());
        assertTrue(fixture.events.get(0) instanceof KeyPressedEvent);
        assertTrue(fixture.events.get(1) instanceof TextInputEvent);
        assertEquals('a', ((TextInputEvent) fixture.events.get(1)).codePoint());
    }

    private static final class Fixture {
        private final List<WidgetEvent> events = new ArrayList<>();
        private final KeyboardState keyboard = new KeyboardState();
        private final Widget target = widget(events);
        private boolean consumeKeyPress;
        private final RoutedEventDispatcher dispatcher = event -> {
            events.add(event);
            return consumeKeyPress && event instanceof KeyPressedEvent;
        };
        private final FocusManager focus = new FocusManager() {
            @Override
            public Widget focusedWidget() {
                return target;
            }
        };
        private final UIContext context = (UIContext) Proxy.newProxyInstance(
                UIContext.class.getClassLoader(), new Class<?>[]{UIContext.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "routedEvents" -> dispatcher;
                    case "keyboard" -> keyboard;
                    case "focusManager" -> focus;
                    case "hoverManager" -> new DefaultHoverManager();
                    case "hitTester" -> (HitTester) (root, x, y) -> Optional.of(new HitTestResult(target, x, y, x - 10.0f, y - 5.0f));
                    case "capturedPointer" -> null;
                    default -> null;
                });

        private InputHandle input() {
            return new InputHandle(target, context);
        }
    }

    private static Widget widget(List<WidgetEvent> events) {
        return (Widget) Proxy.newProxyInstance(Widget.class.getClassLoader(), new Class<?>[]{Widget.class}, (proxy, method, args) -> {
            if (method.getName().equals("handle")) {
                events.add((WidgetEvent) args[0]);
                return null;
            }
            if (method.getName().equals("children")) return List.of();
            if (method.getName().equals("parent")) return null;
            if (method.getName().equals("enabled") || method.getName().equals("visible")) return true;
            if (method.getName().equals("focusable")) return true;
            return null;
        });
    }
}
