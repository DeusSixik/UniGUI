package dev.sixik.unigui.gtnh.backend;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GtnhResourceListingTest {
    @Test
    void textureIdNeedsNamespaceTexturesFolderAndPngExtension() {
        assertEquals("minecraft:textures/items/diamond.png", GtnhResourceListing.textureId("minecraft/textures/items/diamond.png"));
        assertNull(GtnhResourceListing.textureId("minecraft/models/item/diamond.json"));
        assertNull(GtnhResourceListing.textureId("minecraft/sounds/textures/x.png"));
        assertNull(GtnhResourceListing.textureId("minecraft/textures/items/diamond.png.mcmeta"));
        assertNull(GtnhResourceListing.textureId("textures/items/diamond.png"));
    }

    @Test
    void foldersAreScannedUnderAssets(@TempDir Path dir) throws IOException {
        Path texture = dir.resolve("assets/modid/textures/gui/button.png");
        Files.createDirectories(texture.getParent());
        Files.write(texture, new byte[]{1});
        Path other = dir.resolve("assets/modid/lang/en_us.lang");
        Files.createDirectories(other.getParent());
        Files.write(other, new byte[]{1});
        Files.write(dir.resolve("loose.png"), new byte[]{1});

        Set<String> ids = GtnhResourceListing.texturePaths(dir.toFile());
        assertEquals(Set.of("modid:textures/gui/button.png"), ids);
    }

    @Test
    void archivesAreScannedLikeFolders(@TempDir Path dir) throws IOException {
        File zip = dir.resolve("pack.zip").toFile();
        try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(zip.toPath()))) {
            for (String name : new String[]{"assets/a/textures/x.png", "assets/b/textures/deep/y.png",
                    "assets/a/models/z.json", "pack.mcmeta"}) {
                out.putNextEntry(new ZipEntry(name));
                out.write(1);
                out.closeEntry();
            }
        }
        Set<String> ids = GtnhResourceListing.texturePaths(zip);
        assertEquals(Set.of("a:textures/x.png", "b:textures/deep/y.png"), ids);
    }

    @Test
    void missingRootsAreEmpty() throws IOException {
        assertTrue(GtnhResourceListing.texturePaths(null).isEmpty());
        assertTrue(GtnhResourceListing.texturePaths(new File("does-not-exist-xyz")).isEmpty());
    }
}