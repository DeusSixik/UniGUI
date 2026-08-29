package dev.sixik.unigui.testmod.client;

import dev.sixik.unigui.testmod.client.ui.DominionScreen;
import dev.sixik.unigui.testmod.client.ui.LevelMapScreen;
import dev.sixik.unigui.testmod.client.ui.PlugSocketMinigameScreen;
import dev.sixik.unigui.testmod.client.ui.RetroTerminalScreen;
import dev.sixik.unigui.testmod.client.ui.SpannerRhythmMinigameScreen;
import dev.sixik.unigui.testmod.client.ui.SyncBatteryMinigameScreen;
import dev.sixik.unigui.testmod.client.ui.TutorialScreen;
import dev.sixik.unigui.testmod.client.ui.WrenchNutMinigameScreen;
import dev.sixik.unigui.testmod.client.ui.WireConnectionMinigameScreen;
import net.fabricmc.api.ClientModInitializer;

/**
 * Client entrypoint С‚РµСЃС‚РѕРІРѕРіРѕ Fabric-РјРѕРґР°.
 */
public final class TestModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // UI РѕС‚РєСЂС‹РІР°РµС‚СЃСЏ С‡РµСЂРµР· РєРЅРѕРїРєРё, РґРѕР±Р°РІР»РµРЅРЅС‹Рµ mixin'Р°РјРё РЅР° title/pause screen.
    }

    /**
     * РћС‚РєСЂС‹РІР°РµС‚ РѕСЃРЅРѕРІРЅРѕРµ UniGUI demo.
     *
     * <p>Р’С‹РЅРµСЃРµРЅРѕ РІ РѕС‚РґРµР»СЊРЅС‹Р№ РјРµС‚РѕРґ, С‡С‚РѕР±С‹ mixin'С‹ РЅРµ Р·Р°РІРёСЃРµР»Рё РЅР°РїСЂСЏРјСѓСЋ РѕС‚
     * РІРЅСѓС‚СЂРµРЅРЅРµР№ СЃС‚СЂСѓРєС‚СѓСЂС‹ demo screen.</p>
     */
    public static void openDemo() {
        DominionScreen.openGui();
    }

    public static void openRetroTerminal() {
        RetroTerminalScreen.open();
    }

    public static void openLevelMap() {
        LevelMapScreen.open();
    }

    public static void openPlugSocketMinigame() {
        PlugSocketMinigameScreen.open();
    }

    public static void openWrenchNutMinigame() {
        WrenchNutMinigameScreen.open();
    }

    public static void openWireConnectionMinigame() {
        WireConnectionMinigameScreen.open();
    }

    public static void openSyncBatteryMinigame() {
        SyncBatteryMinigameScreen.open();
    }

    public static void openSpannerRhythmMinigame() {
        SpannerRhythmMinigameScreen.open();
    }

    public static void openTutorial() {
        TutorialScreen.open();
    }
}
