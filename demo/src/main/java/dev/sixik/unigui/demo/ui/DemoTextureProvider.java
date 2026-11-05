package dev.sixik.unigui.demo.ui;

import dev.sixik.unigui.api.render.SimpleTextureHandle;
import dev.sixik.unigui.api.render.TextureHandle;
import dev.sixik.unigui.api.render.TextureOptions;

/**
 * Host-supplied texture loader for portable demos.
 *
 * <p>The demo module only knows a stable texture id and a classpath-style resource path.
 * A Minecraft host can turn those values into a native texture; another host can use
 * its own asset system. The default provider returns a one-pixel placeholder so demo
 * widget trees remain constructible in headless tests.</p>
 */
@FunctionalInterface
public interface DemoTextureProvider {
    TextureHandle load(String id, String resourcePath, TextureOptions options);

    /** Returns a provider that creates backend-neutral one-pixel placeholders. */
    static DemoTextureProvider placeholders() {
        return (id, resourcePath, options) -> new SimpleTextureHandle(
                id, 1, 1, null, options == null ? TextureOptions.defaults() : options);
    }
}
