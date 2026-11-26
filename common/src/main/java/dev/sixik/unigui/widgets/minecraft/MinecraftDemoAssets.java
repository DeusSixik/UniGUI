package dev.sixik.unigui.widgets.minecraft;

import com.mojang.blaze3d.platform.NativeImage;
import dev.sixik.unigui.api.render.TextureHandle;
import dev.sixik.unigui.api.render.TextureOptions;
import dev.sixik.unigui.backend.minecraft_impl.UniGuiTextures;

import java.io.IOException;
import java.io.InputStream;

/**
 * Загрузчик картинок демо-галереи для Minecraft 1.20+ (и 26.1).
 *
 * <p>Модуль demo знает только id текстуры и путь к ресурсу в classpath ({@code assets/...}); ресурсы demo
 * упакованы в jar мода. Без этого загрузчика галерея показывает заглушку (чёрно-розовую клетку), как в
 * headless-тестах.</p>
 */
final class MinecraftDemoAssets {
    private MinecraftDemoAssets() {
    }

    /**
     * Загружает картинку из classpath в текстуру UniGUI (с кешем по id).
     *
     * @param id           id текстуры
     * @param resourcePath путь ресурса в classpath
     * @param options      параметры сэмплирования
     * @return текстура или {@code null}, если ресурс не найден (тогда demo берёт заглушку)
     */
    static TextureHandle load(String id, String resourcePath, TextureOptions options) {
        TextureHandle cached = UniGuiTextures.get(id);
        if (cached != null) return cached;
        try (InputStream stream = MinecraftDemoAssets.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (stream == null) return null;
            return UniGuiTextures.replace(id, NativeImage.read(stream), options);
        } catch (IOException | RuntimeException failure) {
            return null;
        }
    }
}
