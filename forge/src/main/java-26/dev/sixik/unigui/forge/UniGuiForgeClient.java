package dev.sixik.unigui.forge;

import dev.sixik.unigui.api.backend.UniGuiRuntime;
import dev.sixik.unigui.backend.minecraft_impl.MinecraftUiRuntimeBackend;
import dev.sixik.unigui.backend.minecraft_impl.ScreenOverlayRender;
import dev.sixik.unigui.backend.minecraft_impl.custom_renders.MinecraftRendererPlatformHook;
import dev.sixik.unigui.forge.custom_renders.ForgeMinecraftRendererPlatformHook;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.listener.Priority;

/**
 * Клиентские хуки UniGUI для Forge 26.1.
 *
 * <p>В Forge 26.1 шина событий EventBus 7: у каждого события своя статическая шина {@code BUS}, а отмена — это
 * {@code true}, возвращённый слушателем ({@link java.util.function.Predicate}), вместо {@code setCanceled}.
 * Слушатели — блочные лямбды с {@code return}: так они подходят только под {@code Predicate} (отменяющий
 * слушатель), а не под {@code Consumer}.
 * Логика та же, что в 1.20/1.21: ввод над экраном сначала получают screen-overlay слои UniGUI.</p>
 */
public final class UniGuiForgeClient {
    private UniGuiForgeClient() {
    }

    public static void init() {
        UniGuiRuntime.install(new MinecraftUiRuntimeBackend());
        MinecraftRendererPlatformHook.set(new ForgeMinecraftRendererPlatformHook());

        ScreenEvent.MouseButtonPressed.Pre.BUS.addListener(event -> {
            return ScreenOverlayRender.mouseClicked(
                event.getScreen(), event.getMouseX(), event.getMouseY(), event.getButton());
        });
        ScreenEvent.MouseButtonReleased.Pre.BUS.addListener(event -> {
            return ScreenOverlayRender.mouseReleased(
                event.getScreen(), event.getMouseX(), event.getMouseY(), event.getButton());
        });
        ScreenEvent.MouseDragged.Pre.BUS.addListener(event -> {
            return ScreenOverlayRender.mouseDragged(
                event.getScreen(), event.getMouseX(), event.getMouseY(), event.getMouseButton(),
                event.getDragX(), event.getDragY());
        });
        ScreenEvent.MouseScrolled.Pre.BUS.addListener(event -> {
            return ScreenOverlayRender.mouseScrolled(
                event.getScreen(), event.getMouseX(), event.getMouseY(), event.getDeltaY());
        });
        ScreenEvent.KeyPressed.Pre.BUS.addListener(Priority.HIGHEST, event -> {
            return ScreenOverlayRender.keyPressed(
                event.getScreen(), event.getKeyCode(), event.getScanCode(), event.getModifiers());
        });
        ScreenEvent.KeyReleased.Pre.BUS.addListener(event -> {
            return ScreenOverlayRender.keyReleased(
                event.getScreen(), event.getKeyCode(), event.getScanCode(), event.getModifiers());
        });
        ScreenEvent.CharacterTyped.Pre.BUS.addListener(Priority.HIGHEST, event -> {
            return ScreenOverlayRender.charTyped(
                event.getScreen(), event.getCodePoint(), 0);
        });
    }
}
