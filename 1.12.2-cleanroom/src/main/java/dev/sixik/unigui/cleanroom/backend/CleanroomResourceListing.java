package dev.sixik.unigui.cleanroom.backend;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.ResourcePackRepository;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Lists texture files of the game, mods and resource packs.
 *
 * <p>The 1.12.2 resource manager can open a known resource but cannot list resources, so the pickers scan the
 * sources themselves: every folder or archive that holds {@code assets/<namespace>/textures/**.png}. The scan is
 * done once per resource reload and the result is cached.</p>
 */
public final class CleanroomResourceListing {
    private static final Logger LOGGER = LogManager.getLogger("unigui");
    private static List<ResourceLocation> cached = List.of();
    private static int cachedGeneration = -1;

    private CleanroomResourceListing() {
    }

    /**
     * @return all texture resources known to the running game, sorted by id; empty if the scan failed
     */
    public static synchronized List<ResourceLocation> textures() {
        int generation = CleanroomResourceReloads.generation();
        if (generation == cachedGeneration) return cached;
        Set<String> ids = new TreeSet<>();
        for (File root : gameRoots()) {
            try {
                ids.addAll(texturePaths(root));
            } catch (IOException | RuntimeException e) {
                LOGGER.warn("UniGUI could not list textures of {}", root, e);
            }
        }
        List<ResourceLocation> result = new ArrayList<>(ids.size());
        for (String id : ids) result.add(new ResourceLocation(id));
        cached = Collections.unmodifiableList(result);
        cachedGeneration = generation;
        return cached;
    }

    /**
     * Lists the textures of one resource root.
     *
     * @param root a folder or a zip / jar archive with an {@code assets} directory
     * @return ids such as {@code minecraft:textures/items/diamond.png}, in no particular order
     * @throws IOException if the root cannot be read
     */
    public static Set<String> texturePaths(File root) throws IOException {
        Set<String> ids = new LinkedHashSet<>();
        if (root == null || !root.exists()) return ids;
        if (root.isDirectory()) {
            Path assets = root.toPath().resolve("assets");
            if (!Files.isDirectory(assets)) return ids;
            try (Stream<Path> walk = Files.walk(assets)) {
                walk.filter(Files::isRegularFile).forEach(path -> {
                    String relative = assets.relativize(path).toString().replace('\\', '/');
                    addIfTexture(ids, relative);
                });
            }
            return ids;
        }
        try (ZipFile zip = new ZipFile(root)) {
            zip.stream().filter(entry -> !entry.isDirectory()).map(ZipEntry::getName).forEach(name -> {
                if (name.startsWith("assets/")) addIfTexture(ids, name.substring("assets/".length()));
            });
        }
        return ids;
    }

    /**
     * Turns {@code namespace/textures/dir/name.png} into {@code namespace:textures/dir/name.png}.
     *
     * @return the id, or {@code null} if the path is not a texture
     */
    static String textureId(String relative) {
        int slash = relative.indexOf('/');
        if (slash <= 0) return null;
        String path = relative.substring(slash + 1);
        if (!path.startsWith("textures/") || !path.endsWith(".png")) return null;
        return relative.substring(0, slash) + ':' + path;
    }

    private static void addIfTexture(Set<String> ids, String relative) {
        String id = textureId(relative);
        if (id != null) ids.add(id);
    }

    private static Set<File> gameRoots() {
        Set<File> roots = new LinkedHashSet<>();
        try {
            // The game jar (vanilla assets).
            var source = Minecraft.class.getProtectionDomain().getCodeSource();
            if (source != null) roots.add(new File(source.getLocation().toURI()));
        } catch (Exception | LinkageError e) {
            LOGGER.debug("Minecraft code source unavailable", e);
        }
        try {
            for (ModContainer mod : Loader.instance().getModList()) {
                File file = mod.getSource();
                if (file != null) roots.add(file);
            }
        } catch (RuntimeException | LinkageError e) {
            LOGGER.debug("Mod list unavailable", e);
        }
        try {
            ResourcePackRepository repository = Minecraft.getMinecraft().getResourcePackRepository();
            File directory = repository.getDirResourcepacks();
            for (ResourcePackRepository.Entry entry : repository.getRepositoryEntries()) {
                File pack = new File(directory, entry.getResourcePackName());
                if (pack.exists()) roots.add(pack);
            }
        } catch (RuntimeException | LinkageError e) {
            LOGGER.debug("Resource pack list unavailable", e);
        }
        return roots;
    }
}