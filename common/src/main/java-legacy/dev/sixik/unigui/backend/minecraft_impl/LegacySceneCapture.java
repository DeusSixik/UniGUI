package dev.sixik.unigui.backend.minecraft_impl;

import dev.sixik.unigui.api.backend.UniGuiLogger;
import dev.sixik.unigui.api.backend.UniGuiRuntime;
import dev.sixik.unigui.tests.SceneMeshDemo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;

import java.io.IOException;
import java.nio.file.Files;

/**
 * Записывает диагностический снимок ванильной сцены в GL-бэкенде.
 */
public final class LegacySceneCapture {
    /**
     * Диагностика записи файла.
     */
    private static final UniGuiLogger LOGGER = UniGuiRuntime.logger(LegacySceneCapture.class);
    /**
     * Режим захвата, общий с Minecraft 26.x.
     */
    private static final String MODE = System.getProperty("unigui.debug.capture", "");
    /**
     * Распознаёт режимы захвата 3D.
     */
    private static final boolean ENABLED = "scene_blocks".equals(MODE) || "scene".equals(MODE);
    /**
     * Ждёт запуска локального мира, если захват должен включать сущность.
     */
    private static final boolean IN_WORLD = Boolean.getBoolean("unigui.debug.sceneWorld");
    /**
     * Координаты указателя в GUI для диагностического захвата подсветки.
     */
    private static final String HOVER = System.getProperty("unigui.debug.hover", "");
    /**
     * Число отрисованных кадров после открытия демонстрации.
     */
    private static int frames;
    /**
     * Уже поставлено открытие демонстрации на клиентский поток.
     */
    private static boolean opened;

    private LegacySceneCapture() {
    }

    /**
     * Открывает демонстрацию после загрузки ресурсов и снимает её через 60 кадров.
     *
     * @param screen   текущий экран клиента.
     * @param graphics контекст кадра, который нужно отправить на GPU перед снимком.
     */
    public static void afterScreenRender(Screen screen, GuiGraphics graphics) {
        if (!ENABLED || frames >= 60) return;
        Minecraft client = Minecraft.getInstance();
        if (!opened) {
            if (!IN_WORLD && screen instanceof TitleScreen) open(client);
            return;
        }
        if (!(screen instanceof MinecraftWidgetScreen)) return;
        if (++frames == 30 && !HOVER.isEmpty()) {
            String[] coordinates = HOVER.split(",");
            try {
                if (coordinates.length != 2) throw new NumberFormatException();
                screen.mouseMoved(Double.parseDouble(coordinates[0]), Double.parseDouble(coordinates[1]));
            } catch (NumberFormatException failure) {
                LOGGER.warn("unigui.debug.hover must be x,y in GUI coordinates");
            }
        }
        if (frames < 60) return;
        graphics.flush();
        var output = client.gameDirectory.toPath().resolve("unigui-capture").resolve(MODE).resolve("screen.png");
        try (var image = Screenshot.takeScreenshot(client.getMainRenderTarget())) {
            Files.createDirectories(output.getParent());
            image.writeToFile(output);
            LOGGER.info("UniGUI scene capture: wrote {}", output);
        } catch (IOException failure) {
            LOGGER.error("UniGUI scene capture failed", failure);
        }
        client.stop();
    }

    /**
     * Открывает демо после входа в мир, когда модели сущностей можно создать.
     *
     * @param client клиент Minecraft с отрисовываемым HUD.
     */
    public static void afterHudRender(Minecraft client) {
        if (ENABLED && IN_WORLD && client.level != null && client.screen == null) open(client);
    }

    /**
     * Ставит открытие демо в очередь между кадрами.
     *
     * @param client клиент Minecraft.
     */
    private static void open(Minecraft client) {
        if (opened || client.getOverlay() != null) return;
        opened = true;
        client.tell("scene".equals(MODE) ? SceneMeshDemo::openDemo : SceneMeshDemo::openBlocksDemo);
    }
}
