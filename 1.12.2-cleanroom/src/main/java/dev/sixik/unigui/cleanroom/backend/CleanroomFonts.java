package dev.sixik.unigui.cleanroom.backend;

import dev.sixik.unigui.api.text.FontFace;
import dev.sixik.unigui.api.text.Fonts;
import dev.sixik.unigui.impl.text.DefaultFontRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.client.resources.IResource;
import net.minecraft.util.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Loads TrueType / OpenType fonts from Minecraft resources (mod jars and resource packs) into the UniGUI font
 * registry, where they become SDF faces drawn by the SDF text renderer.
 *
 * <p>A font registered here is read again after every resource reload, so a resource pack that replaces the file
 * takes effect. Faces already held by widgets keep the old glyphs until the widget asks the registry again.</p>
 *
 * <p>Rasterization uses {@code java.awt.Font}; the first face created initializes AWT, which is why the built-in
 * fonts are registered on demand instead of at startup.</p>
 */
public final class CleanroomFonts {
    /** Fonts that ship inside the UniGUI jar ({@code assets/unigui/fonts}). */
    public enum Builtin {
        /** Cuprum Bold, a narrow technical sans. */
        CUPRUM_BOLD("unigui:cuprum_bold", "Cuprum-Bold.ttf"),
        /** Monocraft, a monospaced font in the Minecraft style. */
        MONOCRAFT("unigui:monocraft", "monocraft.ttf"),
        /** Oswald Regular, a condensed grotesque. */
        OSWALD("unigui:oswald", "Oswald-Regular.ttf"),
        /** Perfect DOS VGA 437, a bitmap-style DOS font. */
        PERFECT_DOS_VGA("unigui:perfect_dos_vga", "perfectdosvga437.ttf"),
        /** Play Regular, a rounded display sans. */
        PLAY("unigui:play", "play_regular.ttf");

        private final String id;
        private final ResourceLocation location;

        Builtin(String id, String file) {
            this.id = id;
            this.location = new ResourceLocation("unigui", "fonts/" + file);
        }

        /** @return registry id of the face */
        public String id() {
            return id;
        }

        /** @return resource the face is loaded from */
        public ResourceLocation location() {
            return location;
        }
    }

    private static final Logger LOGGER = LogManager.getLogger("unigui");
    private static final Map<String, ResourceLocation> LOADED = new LinkedHashMap<>();
    private static boolean listening;

    private CleanroomFonts() {
    }

    /**
     * Registers a font from a resource.
     *
     * @param id       registry id, for example {@code "mymod:title"}; also usable from XML
     * @param location resource of a {@code .ttf} / {@code .otf} file, for example
     *                 {@code new ResourceLocation("mymod", "fonts/title.ttf")}
     * @return the registered face
     * @throws IOException if the resource is missing or is not a font
     */
    public static synchronized FontFace register(String id, ResourceLocation location) throws IOException {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(location, "location");
        FontFace face = load(id, location);
        LOADED.put(id, location);
        listenForReloads();
        return face;
    }

    /**
     * Returns a font that ships with UniGUI, registering it on first use.
     *
     * @param font font to get
     * @return the face
     * @throws IllegalStateException if the file cannot be read
     */
    public static synchronized FontFace builtin(Builtin font) {
        Objects.requireNonNull(font, "font");
        FontFace existing = Fonts.global().find(font.id());
        if (existing != null && LOADED.containsKey(font.id())) return existing;
        try {
            return register(font.id(), font.location());
        } catch (IOException e) {
            throw new IllegalStateException("UniGUI font " + font.id() + " could not be loaded", e);
        }
    }

    /**
     * @param id registry id
     * @return the face registered under the id, or {@code null}
     */
    public static FontFace find(String id) {
        return Fonts.global().find(id);
    }

    private static FontFace load(String id, ResourceLocation location) throws IOException {
        IResource resource = Minecraft.getMinecraft().getResourceManager().getResource(location);
        try (InputStream input = resource.getInputStream()) {
            return DefaultFontRegistry.global().register(id, input.readAllBytes());
        } finally {
            resource.close();
        }
    }

    private static void listenForReloads() {
        if (listening) return;
        listening = true;
        if (Minecraft.getMinecraft().getResourceManager() instanceof IReloadableResourceManager manager) {
            manager.registerReloadListener(resourceManager -> reloadAll());
        }
    }

    private static synchronized void reloadAll() {
        for (Map.Entry<String, ResourceLocation> entry : new LinkedHashMap<>(LOADED).entrySet()) {
            try {
                load(entry.getKey(), entry.getValue());
            } catch (IOException | RuntimeException e) {
                // The previous face stays registered; a pack that drops a font must not break the UI.
                LOGGER.warn("UniGUI font {} could not be reloaded from {}", entry.getKey(), entry.getValue(), e);
            }
        }
    }
}