package dev.sixik.unigui.testmod.client;

import dev.sixik.unigui.testmod.client.ui.DominionScreen;
import dev.sixik.unigui.testmod.client.ui.RetroTerminalScreen;
import net.fabricmc.api.ClientModInitializer;

/**
 * Client entrypoint тестового Fabric-мода.
 */
public final class TestModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // UI открывается через кнопки, добавленные mixin'ами на title/pause screen.
    }

    /**
     * Открывает основное UniGUI demo.
     *
     * <p>Вынесено в отдельный метод, чтобы mixin'ы не зависели напрямую от
     * внутренней структуры demo screen.</p>
     */
    public static void openDemo() {
        DominionScreen.openGui();
    }

    public static void openRetroTerminal() {
        RetroTerminalScreen.open();
    }
}
