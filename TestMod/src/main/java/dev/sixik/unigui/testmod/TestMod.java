package dev.sixik.unigui.testmod;

import net.fabricmc.api.ModInitializer;

/**
 * Минимальный серверный/common entrypoint тестового мода.
 *
 * <p>Вся полезная логика сейчас находится в client entrypoint и mixin'ах:
 * TestMod нужен только как отдельный Fabric-мод, который зависит от UniGUI и
 * позволяет запускать библиотеку в реальном dev-клиенте.</p>
 */
public final class TestMod implements ModInitializer {
    public static final String MOD_ID = "unigui_testmod";

    @Override
    public void onInitialize() {
        // Пока без общей инициализации: мод нужен как sandbox для ручного UI-тестирования.
    }
}