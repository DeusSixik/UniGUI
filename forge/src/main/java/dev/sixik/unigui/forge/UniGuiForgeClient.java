package dev.sixik.unigui.forge;

import dev.sixik.unigui.backend.minecraft_impl.custom_renders.MinecraftRendererPlatformHook;
import dev.sixik.unigui.backend.minecraft_impl.ScreenOverlayRender;
import dev.sixik.unigui.forge.custom_renders.ForgeMinecraftRendererPlatformHook;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public final class UniGuiForgeClient {
    private static final Method SCROLL_DELTA = findScrollDeltaMethod();

    private UniGuiForgeClient() {
    }

    public static void init() {
        MinecraftRendererPlatformHook.set(new ForgeMinecraftRendererPlatformHook());
        MinecraftForge.EVENT_BUS.addListener(UniGuiForgeClient::mousePressed);
        MinecraftForge.EVENT_BUS.addListener(UniGuiForgeClient::mouseReleased);
        MinecraftForge.EVENT_BUS.addListener(UniGuiForgeClient::mouseDragged);
        MinecraftForge.EVENT_BUS.addListener(UniGuiForgeClient::mouseScrolled);
        MinecraftForge.EVENT_BUS.addListener(UniGuiForgeClient::keyPressed);
        MinecraftForge.EVENT_BUS.addListener(UniGuiForgeClient::keyReleased);
        MinecraftForge.EVENT_BUS.addListener(UniGuiForgeClient::characterTyped);
    }

    private static void mousePressed(ScreenEvent.MouseButtonPressed.Pre event) {
        if (ScreenOverlayRender.mouseClicked(screen(event), event.getMouseX(), event.getMouseY(),
                event.getButton())) event.setCanceled(true);
    }

    private static void mouseReleased(ScreenEvent.MouseButtonReleased.Pre event) {
        if (ScreenOverlayRender.mouseReleased(screen(event), event.getMouseX(), event.getMouseY(),
                event.getButton())) event.setCanceled(true);
    }

    private static void mouseDragged(ScreenEvent.MouseDragged.Pre event) {
        if (ScreenOverlayRender.mouseDragged(screen(event), event.getMouseX(), event.getMouseY(),
                event.getMouseButton(), event.getDragX(), event.getDragY())) event.setCanceled(true);
    }

    private static void mouseScrolled(ScreenEvent.MouseScrolled.Pre event) {
        if (ScreenOverlayRender.mouseScrolled(screen(event), event.getMouseX(), event.getMouseY(),
                scrollDelta(event))) event.setCanceled(true);
    }

    private static void keyReleased(ScreenEvent.KeyReleased.Pre event) {
        if (ScreenOverlayRender.keyReleased(screen(event), event.getKeyCode(), event.getScanCode(),
                event.getModifiers())) event.setCanceled(true);
    }

    private static void keyPressed(ScreenEvent.KeyPressed.Pre event) {
        if (ScreenOverlayRender.keyPressed(screen(event), event.getKeyCode(), event.getScanCode(),
                event.getModifiers())) event.setCanceled(true);
    }

    private static void characterTyped(ScreenEvent.CharacterTyped.Pre event) {
        if (ScreenOverlayRender.charTyped(screen(event), event.getCodePoint(), event.getModifiers())) {
            event.setCanceled(true);
        }
    }

    private static Screen screen(ScreenEvent event) {
        return (Screen) (Object) event.getScreen();
    }

    private static double scrollDelta(ScreenEvent.MouseScrolled event) {
        try {
            return ((Number) SCROLL_DELTA.invoke(event)).doubleValue();
        } catch (IllegalAccessException | InvocationTargetException exception) {
            throw new IllegalStateException("Failed to read Forge screen scroll delta", exception);
        }
    }

    private static Method findScrollDeltaMethod() {
        for (String name : new String[]{"getScrollDelta", "getScrollDeltaY"}) {
            try {
                return ScreenEvent.MouseScrolled.class.getMethod(name);
            } catch (NoSuchMethodException ignored) {
                // Forge переименовал vertical delta между поддерживаемыми версиями.
            }
        }
        throw new IllegalStateException("Unsupported Forge ScreenEvent.MouseScrolled API");
    }
}
