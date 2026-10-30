package dev.sixik.unigui.api.input;

import dev.sixik.unigui.api.core.UIContext;
import dev.sixik.unigui.api.event.KeyPressedEvent;
import dev.sixik.unigui.api.event.KeyReleasedEvent;
import dev.sixik.unigui.api.event.PointerMovedEvent;
import dev.sixik.unigui.api.event.PointerPressedEvent;
import dev.sixik.unigui.api.event.PointerReleasedEvent;
import dev.sixik.unigui.api.event.ScrollEvent;
import dev.sixik.unigui.api.event.TextInputEvent;
import dev.sixik.unigui.api.input.FocusDirection;
import dev.sixik.unigui.api.widget.Widget;

import java.util.Objects;
import java.util.Optional;

/** Routes normalized pointer and keyboard input into one UniGUI widget tree. */
public final class InputHandle {
    private final Widget root;
    private final UIContext context;

    public InputHandle(Widget root, UIContext context) {
        this.root = Objects.requireNonNull(root, "root");
        this.context = Objects.requireNonNull(context, "context");
    }

    public boolean pointerMoved(float x, float y, int pointerId) {
        Widget captured = context.capturedPointer(pointerId);
        if (captured != null) {
            HitTestResult local = context.hitTester().localPoint(root, captured, x, y)
                    .orElse(new HitTestResult(captured, x, y, x, y));
            context.hoverManager().updateHover(captured, x, y, local.localX(), local.localY(), pointerId);
            return context.routedEvents().dispatch(new PointerMovedEvent(captured, x, y,
                    local.localX(), local.localY(), pointerId));
        }

        Optional<HitTestResult> hit = context.hitTester().hitTest(root, x, y);
        if (hit.isEmpty()) {
            context.hoverManager().clearHover();
            return false;
        }
        HitTestResult result = hit.get();
        context.hoverManager().updateHover(result.widget(), x, y, result.localX(), result.localY(), pointerId);
        return context.routedEvents().dispatch(new PointerMovedEvent(result.widget(), x, y,
                result.localX(), result.localY(), pointerId));
    }

    public boolean pointerPressed(float x, float y, int pointerId, PointerButton button) {
        pointerMoved(x, y, pointerId);
        Optional<HitTestResult> hit = context.hitTester().hitTest(root, x, y);
        if (hit.isEmpty()) {
            context.focusManager().clearFocus();
            return false;
        }
        HitTestResult result = hit.get();
        Widget focusTarget = nearestFocusable(result.widget());
        if (focusTarget == null) context.focusManager().clearFocus();
        else context.focusManager().requestFocus(focusTarget);
        return context.routedEvents().dispatch(new PointerPressedEvent(result.widget(), x, y,
                result.localX(), result.localY(), pointerId, button));
    }

    public boolean pointerReleased(float x, float y, int pointerId, PointerButton button) {
        pointerMoved(x, y, pointerId);
        Widget captured = context.capturedPointer(pointerId);
        Optional<HitTestResult> hit = captured == null
                ? context.hitTester().hitTest(root, x, y)
                : context.hitTester().localPoint(root, captured, x, y);
        boolean consumed = false;
        if (hit.isPresent()) {
            HitTestResult result = hit.get();
            consumed = context.routedEvents().dispatch(new PointerReleasedEvent(result.widget(), x, y,
                    result.localX(), result.localY(), pointerId, button));
        }
        if (button == PointerButton.PRIMARY) context.clearPointerCapture(pointerId);
        return consumed;
    }

    public boolean pointerScrolled(float x, float y, float deltaX, float deltaY,
                                   int modifiers, int pointerId) {
        Optional<HitTestResult> hit = context.hitTester().hitTest(root, x, y);
        if (hit.isEmpty()) return false;
        HitTestResult result = hit.get();
        boolean consumed = context.routedEvents().dispatch(new ScrollEvent(result.widget(), x, y,
                result.localX(), result.localY(), deltaX, deltaY, modifiers));
        refreshHover(x, y, pointerId);
        return consumed;
    }

    /**
     * Пересчитывает hover под неподвижным курсором.
     *
     * <p>Скролл двигает контент под мышью без {@code PointerMoved}, поэтому без
     * пересчёта hover (и всё, что от него зависит, например Tooltip) остаётся
     * на элементе, который уехал из-под курсора.</p>
     *
     * @param x координата курсора в root-пространстве
     * @param y координата курсора в root-пространстве
     * @param pointerId идентификатор указателя
     */
    private void refreshHover(float x, float y, int pointerId) {
        if (context.capturedPointer(pointerId) != null) return;
        Optional<HitTestResult> hit = context.hitTester().hitTest(root, x, y);
        if (hit.isEmpty()) {
            context.hoverManager().clearHover();
            return;
        }
        HitTestResult result = hit.get();
        context.hoverManager().updateHover(result.widget(), x, y, result.localX(), result.localY(), pointerId);
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        context.keyboard().press(keyCode);
        Widget focused = context.focusManager().focusedWidget();
        if (focused != null && context.routedEvents().dispatch(new KeyPressedEvent(focused, keyCode, scanCode, modifiers))) {
            return true;
        }
        if (keyCode == KeyCodes.TAB) {
            return KeyModifiers.has(modifiers, KeyModifiers.SHIFT)
                    ? context.focusManager().focusPrevious(root)
                    : context.focusManager().focusNext(root);
        }
        FocusDirection direction = focusDirection(keyCode);
        return direction != null && context.focusManager().focusDirectional(root, direction);
    }

    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        context.keyboard().release(keyCode);
        Widget focused = context.focusManager().focusedWidget();
        return focused != null && context.routedEvents().dispatch(
                new KeyReleasedEvent(focused, keyCode, scanCode, modifiers));
    }

    public boolean textInput(int codePoint, int modifiers) {
        Widget focused = context.focusManager().focusedWidget();
        return focused != null && context.routedEvents().dispatch(new TextInputEvent(focused, codePoint, modifiers));
    }

    /** Dispatches one legacy key callback, which may contain both a key and printable text. */
    public boolean keyTyped(int codePoint, int keyCode, int scanCode, int modifiers) {
        boolean handled = keyCode >= 0 && keyPressed(keyCode, scanCode, modifiers);
        if (codePoint >= 32) {
            handled = textInput(codePoint, modifiers) || handled;
        }
        return handled;
    }

    public void clear() {
        context.hoverManager().clearHover();
        context.focusManager().clearFocus();
        context.keyboard().clear();
        context.clearPointerCapture(0);
    }

    private Widget nearestFocusable(Widget widget) {
        for (Widget current = widget; current != null; current = current.parent()) {
            if (current.enabled() && current.visible() && current.focusable()) return current;
        }
        return null;
    }

    private static FocusDirection focusDirection(int keyCode) {
        return switch (keyCode) {
            case KeyCodes.UP -> FocusDirection.UP;
            case KeyCodes.DOWN -> FocusDirection.DOWN;
            case KeyCodes.LEFT -> FocusDirection.LEFT;
            case KeyCodes.RIGHT -> FocusDirection.RIGHT;
            default -> null;
        };
    }
}
