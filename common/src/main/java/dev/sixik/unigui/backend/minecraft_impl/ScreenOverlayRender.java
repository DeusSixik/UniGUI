package dev.sixik.unigui.backend.minecraft_impl;

import dev.sixik.unigui.api.core.UIContext;
import dev.sixik.unigui.api.widget.Widget;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

/**
 * Регистрирует UniGUI-слои, которые рисуются поверх любого текущего Minecraft Screen.
 *
 * <p>Слой находится поверх содержимого чужого UI. Vanilla tooltip текущего экрана
 * откладываются до завершения overlay-pass и поэтому остаются поверх UniGUI.
 * Через {@link MinecraftRenderLayerRegistration#visibleWhen} можно ограничить показ
 * конкретным классом экрана, например экраном рецептов.</p>
 *
 * <pre>{@code
 * MinecraftRenderLayerRegistration<Screen> overlay = ScreenOverlayRender.register(root);
 * overlay.visibleWhen(screen -> screen instanceof InventoryScreen);
 * }</pre>
 */
@Environment(EnvType.CLIENT)
public final class ScreenOverlayRender {
    private static final MinecraftRenderLayerRegistry<Screen> REGISTRY =
            new MinecraftRenderLayerRegistry<>();
    private static final List<PointerBlocker> POINTER_BLOCKERS = new CopyOnWriteArrayList<>();

    /**
     * Колбэк, сообщающий, что overlay перекрывает курсор и ванильный экран
     * под ним не должен реагировать на мышь (hover, tooltip, клики по слотам).
     */
    public interface PointerBlocker {
        boolean blocksPointer(Screen screen, double mouseX, double mouseY);
    }

    private ScreenOverlayRender() {
    }

    /** Регистрирует блокировщик курсора. @return handle для отмены регистрации */
    public static AutoCloseable addPointerBlocker(PointerBlocker blocker) {
        POINTER_BLOCKERS.add(Objects.requireNonNull(blocker, "blocker"));
        return () -> POINTER_BLOCKERS.remove(blocker);
    }

    /** @return {@code true}, если overlay-слои перекрывают курсор над ванильным экраном */
    public static boolean blocksPointer(Screen screen, double mouseX, double mouseY) {
        if (screen == null) return false;
        for (PointerBlocker blocker : POINTER_BLOCKERS) {
            if (blocker.blocksPointer(screen, mouseX, mouseY)) return true;
        }
        return false;
    }

    public static MinecraftRenderLayerRegistration<Screen> register(Widget root) {
        return register(new MinecraftWidgetRenderLayer(root));
    }

    public static MinecraftRenderLayerRegistration<Screen> register(Widget root, UIContext context) {
        return register(new MinecraftWidgetRenderLayer(root, context));
    }

    public static MinecraftRenderLayerRegistration<Screen> register(MinecraftWidgetRenderLayer layer) {
        return register(layer, screen -> true, 0);
    }

    public static MinecraftRenderLayerRegistration<Screen> register(
            MinecraftWidgetRenderLayer layer,
            Predicate<? super Screen> visibility,
            int priority) {
        return REGISTRY.add(layer, visibility, priority);
    }

    /** Удаляет все screen-overlay слои и освобождает принадлежащие им ресурсы. */
    public static void clear() {
        REGISTRY.clear();
    }

    /** @return {@code true}, если для экрана зарегистрирован хотя бы один видимый слой */
    public static boolean hasVisibleLayer(Screen screen) {
        return screen != null && REGISTRY.hasVisibleEntries(screen);
    }

    /** Внутренняя точка вызова из Screen hook. */
    public static void render(Screen screen, GuiGraphics graphics,
                              int mouseX, int mouseY, float partialTick) {
        if (screen == null) return;
        REGISTRY.render(screen, graphics, screen.width, screen.height,
                mouseX, mouseY, partialTick);
    }

    /** Передаёт движение мыши overlay-слоям для обновления hover-состояния. */
    public static void mouseMoved(Screen screen, double mouseX, double mouseY) {
        if (screen != null) REGISTRY.mouseMoved(screen, mouseX, mouseY);
    }

    /** @return {@code true}, если overlay обработал нажатие мыши */
    public static boolean mouseClicked(Screen screen, double mouseX, double mouseY, int button) {
        return screen != null && REGISTRY.mouseClicked(screen, mouseX, mouseY, button);
    }

    /** @return {@code true}, если overlay обработал отпускание мыши */
    public static boolean mouseReleased(Screen screen, double mouseX, double mouseY, int button) {
        return screen != null && REGISTRY.mouseReleased(screen, mouseX, mouseY, button);
    }

    /** @return {@code true}, если overlay обработал drag мыши */
    public static boolean mouseDragged(Screen screen, double mouseX, double mouseY, int button,
                                       double dragX, double dragY) {
        return screen != null && REGISTRY.mouseDragged(screen, mouseX, mouseY, button, dragX, dragY);
    }

    /** @return {@code true}, если overlay обработал колесо мыши */
    public static boolean mouseScrolled(Screen screen, double mouseX, double mouseY, double delta) {
        return screen != null && REGISTRY.mouseScrolled(screen, mouseX, mouseY, delta);
    }

    /** @return {@code true}, если overlay обработал нажатие клавиши */
    public static boolean keyPressed(Screen screen, int keyCode, int scanCode, int modifiers) {
        return screen != null && REGISTRY.keyPressed(screen, keyCode, scanCode, modifiers);
    }

    /** @return {@code true}, если overlay обработал отпускание клавиши */
    public static boolean keyReleased(Screen screen, int keyCode, int scanCode, int modifiers) {
        return screen != null && REGISTRY.keyReleased(screen, keyCode, scanCode, modifiers);
    }

    /** @return {@code true}, если overlay обработал ввод символа */
    public static boolean charTyped(Screen screen, char codePoint, int modifiers) {
        return screen != null && REGISTRY.charTyped(screen, codePoint, modifiers);
    }
}
