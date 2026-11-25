package dev.sixik.unigui.backend.minecraft_impl;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import dev.sixik.unigui.api.backend.UniGuiLogger;
import dev.sixik.unigui.api.backend.UniGuiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;

import java.io.File;
import java.io.IOException;

/**
 * Отладочный захват кадра UniGUI для Minecraft 26.1 (только для разработки).
 *
 * <p>Включается системным свойством {@code -Dunigui.debug.capture=<demo>}, где {@code <demo>} —
 * {@code demo} (главное демо) или {@code complex}. Без свойства класс ничего не делает.</p>
 *
 * <p>Сценарий: на титульном экране открывается демо; через {@link #SETTLE_FRAMES} кадров в
 * {@code <gameDir>/unigui-capture} сохраняются все страницы слоя UniGUI, переносимые на экран в этом кадре
 * ({@code layer_*.png}, с альфой), а в следующем кадре — итоговая картинка окна ({@code screen.png});
 * после записи файлов игра закрывается. Так по файлам видно, теряется изображение при отрисовке в слой или при
 * переносе слоя на экран.</p>
 */
public final class UiDebugCapture {
    private static final UniGuiLogger LOGGER = UniGuiRuntime.logger(UiDebugCapture.class);
    private static final String MODE = System.getProperty("unigui.debug.capture", "");
    /** Включён ли захват. */
    static final boolean ENABLED = !MODE.isEmpty();
    private static final int TITLE_FRAMES = 40;
    /** Перезагрузка ресурсов короткая: экран загрузки снимается раньше, пока она идёт. */
    private static final int SETTLE_FRAMES = "loading".equals(MODE) ? 6 : 60;
    /** Кадр, на котором выполняется клик из {@code unigui.debug.click}. */
    private static final int CLICK_FRAME = 20;
    /** Необязательный клик {@code "x,y"} в единицах GUI, например чтобы открыть всплывающее окно. */
    private static final String CLICK = System.getProperty("unigui.debug.click", "");
    /** Необязательное нажатие клавиши (GLFW-код) на том же кадре, что и клик. */
    private static final int KEY = Integer.getInteger("unigui.debug.key", -1);
    /** Сколько кадров замерять время extract экрана перед захватом (0 — без замера). */
    private static final int BENCH_FRAMES = Integer.getInteger("unigui.debug.bench", 0);
    private static long[] benchSamples;
    private static int benchCount;
    private static long extractStart;

    private static int titleFrames;
    private static int screenFrames;
    private static boolean opened;
    private static boolean captureLayers;
    private static boolean finished;
    private static int layerIndex;
    private static int pending;

    private UiDebugCapture() {
    }

    /**
     * Вызывается каждый кадр из extract текущего экрана (или HUD, если экрана нет).
     *
     * @param screen текущий экран или {@code null}
     */
    public static void onScreenExtract(Screen screen) {
        if (!ENABLED) return;
        long elapsed = extractStart != 0L ? System.nanoTime() - extractStart : 0L;
        extractStart = 0L;
        Minecraft minecraft = Minecraft.getInstance();
        if (finished) {
            if (pending == 0) {
                LOGGER.info("UniGUI capture: done, stopping");
                minecraft.stop();
            }
            return;
        }
        if (!opened) {
            // Ждём конца загрузки ресурсов: до неё компоненты предметов не привязаны.
            // В мире (запуск с --quickPlaySingleplayer) демо открывается так же: там уже есть компоненты предметов
            // и сущности. Без мира — с титульного экрана.
            boolean ready = screen instanceof TitleScreen || minecraft.level != null;
            if (ready && minecraft.getOverlay() == null && ++titleFrames > TITLE_FRAMES) {
                opened = true;
                LOGGER.info("UniGUI capture: opening '{}'", MODE);
                if ("layers".equals(MODE)) {
                    // HUD и overlay поверх инвентаря (нужен мир).
                    LOGGER.info("UniGUI capture: {}", dev.sixik.unigui.tests.LayerTestCommands.enable("hud"));
                    LOGGER.info("UniGUI capture: {}", dev.sixik.unigui.tests.LayerTestCommands.enable("overlay"));
                    minecraft.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(minecraft.player));
                } else if ("hud".equals(MODE)) {
                    LOGGER.info("UniGUI capture: {}", dev.sixik.unigui.tests.LayerTestCommands.enable("hud"));
                } else if ("loading".equals(MODE)) {
                    LOGGER.info("UniGUI capture: {}", dev.sixik.unigui.tests.LayerTestCommands.enable("loading"));
                } else if ("complex".equals(MODE)) {
                    dev.sixik.unigui.tests.ComplexUiDemo.openDemo();
                } else {
                    dev.sixik.unigui.tests.UniGuiDemo.openDemo();
                }
            }
            return;
        }
        boolean counted = switch (MODE) {
            case "layers" -> screen != null;
            case "hud" -> screen == null && minecraft.level != null;
            case "loading" -> LoadingScreenRender.isLoading();
            default -> screen instanceof MinecraftWidgetScreen;
        };
        if (!counted) return;
        screenFrames++;
        if (BENCH_FRAMES > 0 && screenFrames > SETTLE_FRAMES / 2) {
            if (benchSamples == null) benchSamples = new long[BENCH_FRAMES];
            if (benchCount < BENCH_FRAMES) {
                benchSamples[benchCount++] = elapsed;
                // Держим захват, пока не набраны кадры замера.
                if (benchCount < BENCH_FRAMES) screenFrames = Math.min(screenFrames, SETTLE_FRAMES - 1);
                else logBench();
            }
        }
        if (screenFrames == CLICK_FRAME && !CLICK.isEmpty()) click(screen);
        if (screenFrames == CLICK_FRAME && KEY >= 0 && screen != null) {
            LOGGER.info("UniGUI capture: key {} -> {}", KEY,
                    screen.keyPressed(new net.minecraft.client.input.KeyEvent(KEY, 0, 0)));
        }
        if (screenFrames == SETTLE_FRAMES) {
            // Слои этого кадра уже нарисованы; страницы снимаются в следующем кадре при переносе.
            captureLayers = true;
        } else if (screenFrames == SETTLE_FRAMES + 1) {
            captureLayers = false;
            // Основная цель сейчас содержит результат предыдущего кадра (того, чьи слои сняты).
            dump(minecraft.getMainRenderTarget().getColorTexture(), "screen", true);
            finished = true;
        }
    }

    /**
     * Сохраняет страницу слоя, если сейчас кадр захвата.
     *
     * @param texture текстура страницы
     * @param note    описание переноса (для лога)
     */
    static void onLayerBlit(GpuTexture texture, String note) {
        if (!ENABLED || !captureLayers) return;
        String name = "layer_" + (layerIndex++);
        LOGGER.info("UniGUI capture: {} <- {}", name, note);
        dump(texture, name, false);
    }

    /**
     * @return {@code true}, если сейчас кадр захвата (для подробного лога)
     */
    static boolean capturing() {
        return ENABLED && captureLayers;
    }

    /**
     * Вызывается в начале extract экрана (для замера).
     */
    public static void beginExtract() {
        if (ENABLED) extractStart = System.nanoTime();
    }

    private static void logBench() {
        long[] sorted = java.util.Arrays.copyOf(benchSamples, benchCount);
        java.util.Arrays.sort(sorted);
        long sum = 0L;
        int over8 = 0;
        for (long value : sorted) {
            sum += value;
            if (value > 8_000_000L) over8++;
        }
        LOGGER.info(String.format(java.util.Locale.ROOT,
                "UniGUI bench: %d frames, avg %.2f ms, p50 %.2f, p95 %.2f, p99 %.2f, max %.2f, >8ms %d",
                benchCount, sum / 1e6 / benchCount, sorted[benchCount / 2] / 1e6,
                sorted[(int) (benchCount * 0.95)] / 1e6, sorted[(int) (benchCount * 0.99)] / 1e6,
                sorted[benchCount - 1] / 1e6, over8));
    }

    private static void click(Screen screen) {
        String[] parts = CLICK.split(",");
        double x = Double.parseDouble(parts[0].trim());
        double y = Double.parseDouble(parts[1].trim());
        MouseButtonEvent event = new MouseButtonEvent(x, y, new MouseButtonInfo(0, 0));
        LOGGER.info("UniGUI capture: click at {},{}", x, y);
        screen.mouseMoved(x, y);
        screen.mouseClicked(event, false);
        screen.mouseReleased(event);
    }

    private static void dump(GpuTexture texture, String name, boolean opaque) {
        if (texture == null) {
            LOGGER.warn("UniGUI capture: {} has no texture", name);
            return;
        }
        File directory = new File(Minecraft.getInstance().gameDirectory, "unigui-capture");
        directory.mkdirs();
        int width = texture.getWidth(0);
        int height = texture.getHeight(0);
        GpuDevice device = RenderSystem.getDevice();
        GpuBuffer buffer = device.createBuffer(() -> "UniGUI capture " + name,
                GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_MAP_READ, (long) width * height * 4L);
        CommandEncoder encoder = device.createCommandEncoder();
        pending++;
        encoder.copyTextureToBuffer(texture, buffer, 0L, () -> {
            try (GpuBuffer.MappedView view = encoder.mapBuffer(buffer, true, false);
                 NativeImage image = new NativeImage(width, height, false)) {
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        int abgr = view.data().getInt((x + y * width) * 4);
                        image.setPixelABGR(x, height - y - 1, opaque ? abgr | 0xFF000000 : abgr);
                    }
                }
                File file = new File(directory, name + ".png");
                image.writeToFile(file);
                LOGGER.info("UniGUI capture: wrote {} ({}x{})", file, width, height);
            } catch (IOException | RuntimeException exception) {
                LOGGER.warn("UniGUI capture: failed to write {}: {}", name, exception.toString());
            } finally {
                buffer.close();
                pending--;
            }
        }, 0);
    }
}
