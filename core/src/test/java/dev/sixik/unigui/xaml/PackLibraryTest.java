package dev.sixik.unigui.xaml;

import dev.sixik.unigui.xaml.component.Components;
import dev.sixik.unigui.xaml.pack.AssetPackWriter;
import dev.sixik.unigui.xaml.pack.PackLibrary;
import dev.sixik.unigui.xaml.pack.PackManifest;
import dev.sixik.unigui.xaml.pack.UiSettings;
import dev.sixik.unigui.xaml.project.Project;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PackLibraryTest {
    @TempDir
    Path temp;

    private final List<AutoCloseable> open = new ArrayList<>();

    @AfterEach
    void closeAll() throws Exception {
        for (AutoCloseable closeable : open) closeable.close();
    }

    private PackLibrary library() {
        return PackLibrary.open(temp.resolve("unigui"));
    }

    private static byte[] pack(String id, String version, String componentName) {
        return new AssetPackWriter(new PackManifest(id, version, id, ""))
                .add("components/" + componentName.toLowerCase() + ".xaml",
                        "<Component x:Name=\"" + componentName + "\" xmlns:x=\"https://unigui.dev/xaml\"><Label text=\"x\"/></Component>")
                .toBytes();
    }

    private void put(String file, byte[] data) throws IOException {
        Files.createDirectories(temp.resolve("unigui/packs"));
        Files.write(temp.resolve("unigui/packs").resolve(file), data);
    }

    @Test
    void theFolderLayoutIsFixedAndCreatedOnFirstWrite() throws IOException {
        PackLibrary library = library();
        assertEquals(temp.resolve("unigui/packs"), library.packs());
        assertEquals(temp.resolve("unigui/projects"), library.projects());
        assertEquals(temp.resolve("unigui/settings.xaml"), library.settingsFile());
        assertFalse(Files.exists(library.root()), "opening does not touch the disk");
        assertEquals(List.of(), library.scan());

        Project project = Project.create(new PackManifest("shop", "1.0", "Shop", ""));
        project.createComponent("Card");
        Path exported = library.export(project);
        assertEquals(temp.resolve("unigui/packs/shop.zip"), exported);
        assertEquals(1, library.scan().size());
        assertEquals("shop", library.scan().get(0).manifest().id());
        assertTrue(library.find("shop").isPresent());
        assertTrue(library.remove("shop"));
        assertFalse(library.remove("shop"));
    }

    @Test
    void installingRegistersEveryPackAndClosingUndoesIt() throws IOException {
        put("a.zip", pack("liba", "1.0", "LibCardA"));
        put("b.zip", pack("libb", "1.0", "LibCardB"));
        PackLibrary.Installed installed = library().install();
        open.add(installed);
        assertEquals(List.of("liba", "libb"), installed.packs());
        assertTrue(installed.clean(), installed.problems().toString());
        assertTrue(Components.find("liba/libcarda").isPresent());
        assertTrue(Components.find("libb/libcardb").isPresent());
        installed.close();
        assertTrue(Components.find("liba/libcarda").isEmpty());
    }

    @Test
    void aBrokenFileIsReportedAndDoesNotStopTheRest() throws IOException {
        put("good.zip", pack("good", "1.0", "GoodCard"));
        put("broken.zip", new byte[]{1, 2, 3});
        PackLibrary.Installed installed = library().install();
        open.add(installed);
        assertEquals(List.of("good"), installed.packs());
        assertFalse(installed.clean());
        assertTrue(installed.problems().get(0).startsWith("broken.zip"), installed.problems().toString());
    }

    @Test
    void theNewerVersionOfTheSameIdWinsAndTheReplacementIsReported() throws IOException {
        put("old.zip", pack("dup", "1.9", "DupCardOld"));
        put("new.zip", pack("dup", "1.10", "DupCardNew"));
        PackLibrary.Installed installed = library().install();
        open.add(installed);
        assertEquals(List.of("dup"), installed.packs());
        assertTrue(Components.find("dup/dupcardnew").isPresent(), "1.10 is newer than 1.9");
        assertTrue(Components.find("dup/dupcardold").isEmpty());
        assertTrue(installed.problems().stream().anyMatch(text -> text.contains("replaces") || text.contains("older")),
                installed.problems().toString());
    }

    @Test
    void aPackBundledInTheModIsReplacedByThePlayersPackWithTheSameIdAndVersion() throws IOException {
        Path resources = temp.resolve("jar");
        Files.createDirectories(resources.resolve("assets/unigui/packs"));
        Files.write(resources.resolve("assets/unigui/packs/default.zip"), pack("base", "1.0", "BundledCard"));
        put("base.zip", pack("base", "1.0", "PlayerCard"));
        try (java.net.URLClassLoader loader = new java.net.URLClassLoader(new java.net.URL[]{resources.toUri().toURL()}, null)) {
            PackLibrary.Installed installed = library().bundled(loader, "assets/unigui/packs/default.zip").install();
            open.add(installed);
            assertEquals(List.of("base"), installed.packs());
            assertTrue(Components.find("base/playercard").isPresent(), "the player's pack wins at equal versions");
            assertTrue(Components.find("base/bundledcard").isEmpty());
        }
    }

    @Test
    void aBundledPackWorksWithoutAnyFolder() throws IOException {
        Path resources = temp.resolve("jar");
        Files.createDirectories(resources.resolve("assets/unigui/packs"));
        Files.write(resources.resolve("assets/unigui/packs/default.zip"), pack("only", "1.0", "OnlyCard"));
        try (java.net.URLClassLoader loader = new java.net.URLClassLoader(new java.net.URL[]{resources.toUri().toURL()}, null)) {
            PackLibrary.Installed installed = library().bundled(loader, "assets/unigui/packs/default.zip")
                    .bundled(loader, "assets/unigui/packs/missing.zip").install();
            open.add(installed);
            assertEquals(List.of("only"), installed.packs());
            assertTrue(installed.problems().stream().anyMatch(text -> text.contains("missing.zip")));
        }
    }

    @Test
    void theLivelibraryReinstallsOnlyWhenTheFolderChanged() throws Exception {
        put("shop.zip", pack("live", "1.0", "LiveCard"));
        PackLibrary.Live live = new PackLibrary.Live(library());
        open.add(live);
        assertTrue(Components.find("live/livecard").isPresent());
        assertFalse(live.poll(), "nothing changed");

        Path file = temp.resolve("unigui/packs/shop.zip");
        Files.write(file, pack("live", "1.1", "LiveCardTwo"));
        Files.setLastModifiedTime(file, FileTime.fromMillis(System.currentTimeMillis() + 5_000));
        assertTrue(live.poll());
        assertTrue(Components.find("live/livecardtwo").isPresent());
        assertTrue(Components.find("live/livecard").isEmpty(), "the previous installation was undone");
        assertFalse(live.poll());

        Files.delete(file);
        assertTrue(live.poll());
        assertTrue(live.installed().packs().isEmpty());
    }

    @Test
    void settingsKeepOnlyWhatWasChangedAndSurviveACorruptFile() throws IOException {
        UiSettings settings = library().settings();
        assertEquals(1.0f, settings.getFloat("scale", 1.0f));
        int[] changes = {0};
        settings.onChanged(() -> changes[0]++);
        settings.set("scale", "1.5").set("editor.theme", "dark & <blue>").set("scale", "1.5");
        assertEquals(2, changes[0], "setting the same value again is not a change");
        settings.save();

        UiSettings again = library().settings();
        assertEquals(1.5f, again.getFloat("scale", 1.0f));
        assertEquals("dark & <blue>", again.get("editor.theme", ""));
        assertEquals(7, again.getInt("missing", 7));
        assertTrue(again.getBoolean("missing", true));
        again.set("flag", "true");
        assertTrue(again.getBoolean("flag", false));
        again.set("flag", null);
        assertFalse(again.has("flag"));

        Files.writeString(library().settingsFile(), "<Settings><Setting Key=");
        UiSettings broken = library().settings();
        assertEquals(1.0f, broken.getFloat("scale", 1.0f), "a corrupt file means defaults");
        assertFalse(broken.problem().isEmpty());
    }
}
