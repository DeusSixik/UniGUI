package dev.sixik.isf.client;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;

/** Клиентские hooks и небольшой безопасный recipe browser overlay. */
public final class IsfClient {
    private static final IsfBrowserOverlay OVERLAY = new IsfBrowserOverlay();

    private IsfClient() {
    }

    public static void init() {
        OVERLAY.register();
        MinecraftForge.EVENT_BUS.addListener(IsfClient::keyPressed);
        MinecraftForge.EVENT_BUS.addListener(IsfClient::screenClosed);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, IsfClient::mouseScrolled);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, IsfClient::mousePressed);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, IsfClient::mouseDragged);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, IsfClient::mouseReleased);
    }

    private static void keyPressed(ScreenEvent.KeyPressed.Pre event) {
        if (!(event.getScreen() instanceof AbstractContainerScreen<?>)) return;
        if (event.getKeyCode() == org.lwjgl.glfw.GLFW.GLFW_KEY_A) {
            if (OVERLAY.toggleHoveredBookmark()) event.setCanceled(true);
        }
    }

    private static void screenClosed(ScreenEvent.Closing event) {
        OVERLAY.resetPage();
    }

    private static void mouseScrolled(ScreenEvent.MouseScrolled.Pre event) {
        if (event.isCanceled() || !(event.getScreen() instanceof AbstractContainerScreen<?>)) return;
        if (OVERLAY.scrollItemsAt(event.getMouseX(), event.getMouseY(), event.getScrollDelta())) {
            event.setCanceled(true);
        }
    }

    private static void mousePressed(ScreenEvent.MouseButtonPressed.Pre event) {
        if (event.isCanceled() || !(event.getScreen() instanceof AbstractContainerScreen<?>)) return;
        if (OVERLAY.beginItemScrollBarDrag(event.getMouseX(), event.getMouseY(), event.getButton())) {
            event.setCanceled(true);
        }
    }

    private static void mouseDragged(ScreenEvent.MouseDragged.Pre event) {
        if (event.isCanceled() || !(event.getScreen() instanceof AbstractContainerScreen<?>)) return;
        if (OVERLAY.dragItemScrollBar(event.getMouseY(), event.getMouseButton())) {
            event.setCanceled(true);
        }
    }

    private static void mouseReleased(ScreenEvent.MouseButtonReleased.Pre event) {
        if (event.isCanceled() || !(event.getScreen() instanceof AbstractContainerScreen<?>)) return;
        if (OVERLAY.endItemScrollBarDrag(event.getMouseY(), event.getButton())) {
            event.setCanceled(true);
        }
    }
}
