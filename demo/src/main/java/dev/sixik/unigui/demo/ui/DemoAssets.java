package dev.sixik.unigui.demo.ui;

import dev.sixik.unigui.api.render.TextureHandle;
import dev.sixik.unigui.api.render.TextureOptions;

import java.util.Objects;

/** Installs and invokes the asset provider used by texture-backed portable demos. */
public final class DemoAssets {
    private static volatile DemoTextureProvider provider = DemoTextureProvider.placeholders();

    private DemoAssets() {
    }

    /** Installs a host-specific texture provider; null restores placeholders. */
    public static void provider(DemoTextureProvider next) {
        provider = next == null ? DemoTextureProvider.placeholders() : Objects.requireNonNull(next);
    }

    /** Resolves one demo texture through the current host provider. */
    public static TextureHandle texture(String id, String resourcePath, TextureOptions options) {
        TextureHandle texture = provider.load(id, resourcePath, options);
        return texture == null ? DemoTextureProvider.placeholders().load(id, resourcePath, options) : texture;
    }
}
