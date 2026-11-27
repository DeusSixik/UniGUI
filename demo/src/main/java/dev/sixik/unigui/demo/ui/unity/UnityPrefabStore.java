package dev.sixik.unigui.demo.ui.unity;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Persists one file per prefab in the demo's Assets/Prefabs folder.
 */
final class UnityPrefabStore {
    /**
     * Default folder relative to the running game's working directory.
     */
    static final Path DEFAULT_DIRECTORY = Path.of("config", "unigui", "unity-demo", "Assets", "Prefabs");
    /**
     * Filename extension shared by project entries and saved template files.
     */
    static final String EXTENSION = ".prefab";
    /**
     * Maximum size in bytes of one serialized prefab.
     */
    private static final long MAX_FILE_SIZE = 8 * 1024 * 1024;
    /**
     * Absolute storage location for this editor.
     */
    private final Path directory;

    /**
     * Attaches to the given folder without creating it until the first write.
     *
     * @param directory destination folder
     */
    UnityPrefabStore(Path directory) {
        this.directory = directory.toAbsolutePath().normalize();
    }

    /**
     * Loads all readable assets and reports any file that could not be decoded.
     *
     * @param onUnreadable receives the filename of each invalid asset
     * @return decoded assets, or an empty list for a new project
     * @throws IOException if the folder cannot be listed
     */
    List<UnityPrefab> load(Consumer<Path> onUnreadable) throws IOException {
        List<UnityPrefab> entries = new ArrayList<>();
        if (Files.notExists(directory)) return entries;
        try (DirectoryStream<Path> files = Files.newDirectoryStream(directory, "*" + EXTENSION)) {
            for (Path file : files) {
                if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) continue;
                try {
                    String filename = file.getFileName().toString();
                    UUID id = UUID.fromString(filename.substring(0, filename.length() - EXTENSION.length()));
                    if (!path(id).equals(file) || Files.size(file) > MAX_FILE_SIZE)
                        throw new IOException("Invalid prefab file");
                    try (var input = new DataInputStream(new BufferedInputStream(Files.newInputStream(file)))) {
                        entries.add(UnityPrefab.read(id, input));
                    }
                } catch (IOException | IllegalArgumentException failure) {
                    onUnreadable.accept(file.getFileName());
                }
            }
        }
        entries.sort(Comparator.comparing(UnityPrefab::name, String.CASE_INSENSITIVE_ORDER).thenComparing(UnityPrefab::id));
        return entries;
    }

    /**
     * Writes a complete new file before replacing any existing version of the asset.
     *
     * @param prefab template to save
     * @throws IOException if the serialized asset is too large or cannot be saved
     */
    void save(UnityPrefab prefab) throws IOException {
        Files.createDirectories(directory);
        Path temporary = Files.createTempFile(directory, "asset-", ".tmp");
        try {
            try (var output = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(temporary)))) {
                prefab.write(output);
            }
            if (Files.size(temporary) > MAX_FILE_SIZE) throw new IOException("Prefab exceeds the file size limit");
            Path target = path(prefab.id());
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException unsupported) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    /**
     * Removes the saved asset associated with the given identity.
     *
     * @param prefab asset to remove
     * @throws IOException if its file cannot be deleted
     */
    void delete(UnityPrefab prefab) throws IOException {
        Files.deleteIfExists(path(prefab.id()));
    }

    /**
     * Finds the storage path by identity, independent of the asset's editable display name.
     *
     * @param id stable prefab identifier
     * @return absolute path to the prefab file
     */
    Path path(UUID id) {
        return directory.resolve(id + EXTENSION);
    }
}
