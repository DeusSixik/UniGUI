package dev.sixik.unigui.cleanroom.backend;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

/**
 * Feeds the current LWJGL mouse and keyboard event to the screen-overlay layers before the vanilla screen sees it.
 *
 * <p>Called from the {@code handleMouseInput} / {@code handleKeyboardInput} hooks, which run once per input event.
 * Native UniGUI screens route input themselves and are skipped here.</p>
 */
public final class CleanroomOverlayInput {
    private CleanroomOverlayInput() {
    }

    /**
     * Offers the current mouse event to the overlay layers.
     *
     * @param screen screen that is about to handle the event
     * @return {@code true} if an overlay consumed the event and the vanilla screen must not see it
     */
    public static boolean handleMouse(GuiScreen screen) {
        CleanroomUiRuntimeBackend backend = CleanroomClientBootstrap.backend();
        if (backend.isNativeScreen(screen)) return false;
        Minecraft minecraft = Minecraft.getMinecraft();
        float mouseX = Mouse.getEventX() * screen.width / (float) Math.max(1, minecraft.displayWidth);
        float mouseY = screen.height - Mouse.getEventY() * screen.height
                / (float) Math.max(1, minecraft.displayHeight) - 1.0f;
        // Pointer motion is polled per frame in CleanroomUiRuntimeBackend.renderScreen, not taken from tick events.
        int button = Mouse.getEventButton();
        if (button != -1 && Mouse.getEventButtonState()) {
            if (backend.screenMouseClicked(screen, mouseX, mouseY, button)) return true;
        } else if (button != -1 && backend.screenMouseReleased(screen, mouseX, mouseY, button)) {
            return true;
        }
        int wheel = Mouse.getEventDWheel();
        if (wheel == 0) return false;
        // Overlay layers already scrolled with this notch in the frame; do not scroll twice and keep vanilla out.
        if (backend.wheelAlreadyDelivered(wheel)) return true;
        return backend.screenMouseScrolled(screen, mouseX, mouseY, wheel / 120.0);
    }

    /**
     * Offers the current keyboard event (press, repeat or release) to the overlay layers.
     *
     * @param screen screen that is about to handle the event
     * @return {@code true} if an overlay consumed the event and the vanilla screen must not see it
     */
    public static boolean handleKeyboard(GuiScreen screen) {
        CleanroomUiRuntimeBackend backend = CleanroomClientBootstrap.backend();
        if (backend.isNativeScreen(screen)) return false;
        int keyCode = Keyboard.getEventKey();
        char character = Keyboard.getEventCharacter();
        if (keyCode == Keyboard.KEY_NONE && character == 0) return false;
        int modifiers = modifiers();
        if (Keyboard.getEventKeyState()) return backend.screenKeyTyped(screen, character, keyCode, modifiers);
        return backend.screenKeyReleased(screen, keyCode, modifiers);
    }

    private static int modifiers() {
        int modifiers = 0;
        if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)) modifiers |= 1;
        if (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL)) modifiers |= 1 << 1;
        if (Keyboard.isKeyDown(Keyboard.KEY_LMENU) || Keyboard.isKeyDown(Keyboard.KEY_RMENU)) modifiers |= 1 << 2;
        return modifiers;
    }
}