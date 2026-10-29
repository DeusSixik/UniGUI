package dev.sixik.unigui.cleanroom.backend;

import net.minecraft.client.Minecraft;
import org.lwjgl.input.Mouse;

import java.lang.ref.WeakReference;

/**
 * Tracks the cursor once per rendered frame, so hover and drag follow the mouse at the display frame rate.
 *
 * <p>Minecraft 1.12.2 calls {@code GuiScreen.handleMouseInput()} from {@code runTick()}, that is 20 times per second,
 * no matter how fast frames are drawn. Pointer motion taken from those events makes hover, dragging and splitters
 * advance in 50 ms steps. In addition the buffered events are older than the last frame, so replaying them moves the
 * cursor backwards for one frame. {@link Mouse#getX()} / {@link Mouse#getY()} are the live position (vanilla uses them
 * for the {@code drawScreen} arguments), so the UI polls them every frame instead and reports only real changes.
 * Presses, releases and the wheel stay event driven because none of them may be lost or merged.</p>
 *
 * <p>Coordinates are the screen's GUI units with the origin at the top left, as fractions (not rounded like the
 * integer {@code drawScreen} arguments). Render thread only; not thread safe.</p>
 */
final class CleanroomPointer {
    private WeakReference<Object> owner = new WeakReference<>(null);
    private float lastX = Float.NaN;
    private float lastY = Float.NaN;
    private float x;
    private float y;

    /**
     * Reads the live cursor and decides whether the owner has to be told about it.
     *
     * @param owner     object the position is reported to (screen or layer set); a different owner than last time
     *                  always counts as a change, so a freshly opened screen gets its initial hover
     * @param guiWidth  width of the owner's GUI in GUI units
     * @param guiHeight height of the owner's GUI in GUI units
     * @return {@code true} if the position (or the owner) changed since the previous call; read it with
     * {@link #x()} and {@link #y()}
     */
    boolean poll(Object owner, int guiWidth, int guiHeight) {
        Minecraft minecraft = Minecraft.getMinecraft();
        return update(owner, guiX(Mouse.getX(), guiWidth, minecraft.displayWidth),
                guiY(Mouse.getY(), guiHeight, minecraft.displayHeight));
    }

    /**
     * Records a position without touching LWJGL; the testable core of {@link #poll}.
     *
     * @return {@code true} if the position or owner differs from the previous call
     */
    boolean update(Object owner, float newX, float newY) {
        boolean sameOwner = this.owner.get() == owner;
        x = newX;
        y = newY;
        if (sameOwner && newX == lastX && newY == lastY) return false;
        this.owner = new WeakReference<>(owner);
        lastX = newX;
        lastY = newY;
        return true;
    }

    /**
     * @return x of the last polled position in GUI units
     */
    float x() {
        return x;
    }

    /**
     * @return y of the last polled position in GUI units
     */
    float y() {
        return y;
    }

    /**
     * Converts a window x (pixels from the left) into GUI units.
     *
     * @param rawX         {@code Mouse.getX()}
     * @param guiWidth     GUI width in GUI units
     * @param displayWidth framebuffer width in pixels
     * @return {@code rawX * guiWidth / displayWidth}
     */
    static float guiX(int rawX, int guiWidth, int displayWidth) {
        return rawX * guiWidth / (float) Math.max(1, displayWidth);
    }

    /**
     * Converts a window y (pixels from the bottom, as LWJGL reports it) into GUI units from the top.
     *
     * @param rawY          {@code Mouse.getY()}
     * @param guiHeight     GUI height in GUI units
     * @param displayHeight framebuffer height in pixels
     * @return {@code guiHeight - rawY * guiHeight / displayHeight - 1}
     */
    static float guiY(int rawY, int guiHeight, int displayHeight) {
        return guiHeight - rawY * guiHeight / (float) Math.max(1, displayHeight) - 1.0f;
    }
}
