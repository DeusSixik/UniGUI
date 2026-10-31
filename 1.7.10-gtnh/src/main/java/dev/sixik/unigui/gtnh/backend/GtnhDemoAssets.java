package dev.sixik.unigui.gtnh.backend;

import dev.sixik.unigui.api.render.TextureHandle;
import dev.sixik.unigui.api.render.TextureOptions;
import net.minecraft.util.ResourceLocation;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

/**
 * Maps portable demo assets to Gtnh resource-pack textures.
 */
final class GtnhDemoAssets {
    private GtnhDemoAssets() {
    }

    static TextureHandle load(String id, String resourcePath, TextureOptions options) {
        try {
            ResourceLocation location = resourceLocation(resourcePath);
            if (location == null) return null;
            try (InputStream stream = GtnhDemoAssets.class.getClassLoader().getResourceAsStream(resourcePath)) {
                if (stream == null) return null;
                BufferedImage image = ImageIO.read(stream);
                if (image == null) return null;
                return new GtnhTextureHandle(location, image.getWidth(), image.getHeight(), options);
            }
        } catch (IOException | RuntimeException invalidPath) {
            return null;
        }
    }

    static ResourceLocation resourceLocation(String resourcePath) {
        if (resourcePath == null || !resourcePath.startsWith("assets/")) return null;
        String resourceId = resourcePath.substring("assets/".length());
        int separator = resourceId.indexOf('/');
        if (separator <= 0 || separator == resourceId.length() - 1) return null;
        String namespace = resourceId.substring(0, separator);
        if ("unigui_testmod".equals(namespace)) namespace = "unigui";
        return new ResourceLocation(namespace, resourceId.substring(separator + 1));
    }
}
