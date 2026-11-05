package dev.sixik.unigui.xaml;

import dev.sixik.unigui.api.render.SimpleTextureHandle;
import dev.sixik.unigui.api.widget.Widget;
import dev.sixik.unigui.widgets.display.Label;
import dev.sixik.unigui.xaml.component.Args;
import dev.sixik.unigui.xaml.component.Components;
import dev.sixik.unigui.xaml.load.LoadOptions;
import dev.sixik.unigui.xaml.load.LoadedScreen;
import dev.sixik.unigui.xaml.load.XamlLoadException;
import dev.sixik.unigui.xaml.pack.AssetPack;
import dev.sixik.unigui.xaml.pack.AssetPackWriter;
import dev.sixik.unigui.xaml.pack.PackInstallation;
import dev.sixik.unigui.xaml.pack.PackManifest;
import dev.sixik.unigui.xaml.struct.Structs;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class AssetPackTest {
    private static final PackManifest MANIFEST = new PackManifest("mymod:shop", "1.2.0", "Shop UI", "Catalog");

    private final List<PackInstallation> installations = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        installations.forEach(PackInstallation::close);
    }

    private static AssetPackWriter shop() {
        return new AssetPackWriter(MANIFEST)
                .add("structs/line.xaml", """
                        <Struct x:Name="ShopLine" xmlns:x="https://unigui.dev/xaml">
                          <Field Name="product" Type="struct:ShopProduct"/>
                          <Field Name="quantity" Type="int"/>
                        </Struct>""")
                .add("structs/product.xaml", """
                        <Struct x:Name="ShopProduct" xmlns:x="https://unigui.dev/xaml">
                          <Field Name="title" Type="string"/>
                        </Struct>""")
                .add("components/card.xaml", """
                        <Component x:Name="PackCard" xmlns:x="https://unigui.dev/xaml">
                          <Component.Contract><Param Name="Title" Type="string" Default="-"/></Component.Contract>
                          <Label id="label" text="{Param Title}"/>
                        </Component>""")
                .add("screens/main.xaml", "<VBox><PackCard id=\"first\" Title=\"Sword\"/></VBox>")
                .add("textures/icons/sword.png", new byte[]{1, 2, 3});
    }

    private AssetPack pack() {
        return AssetPack.read(shop().toBytes());
    }

    private PackInstallation install(AssetPack pack) {
        PackInstallation installation = pack.install();
        installations.add(installation);
        return installation;
    }

    // ------------------------------------------------------------------ чтение и запись

    @Test
    void aPackSurvivesAWriteAndReadRoundTrip() {
        AssetPack pack = pack();
        assertEquals(MANIFEST, pack.manifest());
        assertEquals(List.of("components/card.xaml", "pack.xaml", "screens/main.xaml", "structs/line.xaml",
                "structs/product.xaml", "textures/icons/sword.png"), pack.names());
        assertTrue(pack.has("textures/icons/sword.png"));
        assertArrayEquals(new byte[]{1, 2, 3}, pack.bytes("textures/icons/sword.png").orElseThrow());
        assertTrue(pack.text("screens/main.xaml").orElseThrow().contains("PackCard"));
        assertEquals(List.of("components/card.xaml"), pack.names("components/"));
        assertTrue(pack.bytes("nothing").isEmpty());
        assertEquals(2, pack.summary().get("structs/"));
    }

    @Test
    void theSameFilesAlwaysGiveTheSameArchive() {
        assertArrayEquals(shop().toBytes(), shop().toBytes());
        // порядок добавления не влияет
        AssetPackWriter reversed = new AssetPackWriter(MANIFEST)
                .add("textures/icons/sword.png", new byte[]{1, 2, 3})
                .add("screens/main.xaml", "<VBox><PackCard id=\"first\" Title=\"Sword\"/></VBox>");
        AssetPackWriter forward = new AssetPackWriter(MANIFEST)
                .add("screens/main.xaml", "<VBox><PackCard id=\"first\" Title=\"Sword\"/></VBox>")
                .add("textures/icons/sword.png", new byte[]{1, 2, 3});
        assertArrayEquals(forward.toBytes(), reversed.toBytes());
    }

    @Test
    void aPackCanBeWrittenToAFile() throws IOException {
        java.nio.file.Path file = java.nio.file.Files.createTempFile("unigui-pack", ".zip");
        try {
            shop().write(file);
            assertEquals(MANIFEST, AssetPack.open(file).manifest());
        } finally {
            java.nio.file.Files.deleteIfExists(file);
        }
    }

    // ------------------------------------------------------------------ установка

    @Test
    void installingRegistersStructsInDependencyOrderAndComponentsUnderThePackId() {
        PackInstallation installation = install(pack());
        assertTrue(installation.clean(), installation.problems().toString());
        assertEquals(List.of("ShopProduct", "ShopLine"), installation.structs(),
                "ShopLine mentions ShopProduct, so it is installed after it even though its file sorts first");
        assertEquals(List.of("mymod:shop/card"), installation.components());

        assertEquals("Sword", ((Label) Xaml.get(Xaml.component("mymod:shop/card")
                .create(Args.of("Title", "Sword")).widget(), "label")).text());
    }

    @Test
    void closingTheInstallationRemovesEverythingItAdded() {
        PackInstallation installation = install(pack());
        assertTrue(Components.find("mymod:shop/card").isPresent());
        assertTrue(Structs.find("ShopProduct").isPresent());
        installation.close();
        assertTrue(Components.find("mymod:shop/card").isEmpty());
        assertTrue(Structs.find("ShopProduct").isEmpty());
        assertThrows(XamlLoadException.class, () -> Xaml.load("<PackCard/>"));
    }

    @Test
    void aBrokenFileDoesNotStopTheRestAndIsReportedByPath() {
        AssetPack pack = AssetPack.read(shop()
                .add("components/broken.xaml", "<Component x:Name=\"Broken\" xmlns:x=\"https://unigui.dev/xaml\"/>")
                .add("structs/bad.xaml", "<Struct/>")
                .toBytes());
        PackInstallation installation = install(pack);
        assertFalse(installation.clean());
        assertEquals(2, installation.problems().size(), installation.problems().toString());
        assertTrue(installation.problems().stream().anyMatch(p -> p.startsWith("components/broken.xaml")));
        assertTrue(installation.problems().stream().anyMatch(p -> p.startsWith("structs/bad.xaml")));
        assertEquals(List.of("mymod:shop/card"), installation.components(), "the healthy component was installed");
    }

    @Test
    void requireCleanRefusesAndUndoesAPartialInstall() {
        AssetPack pack = AssetPack.read(shop().add("components/broken.xaml", "<Nope/>").toBytes());
        IllegalStateException failure = assertThrows(IllegalStateException.class, () -> pack.install().requireClean());
        assertTrue(failure.getMessage().contains("components/broken.xaml"), failure.getMessage());
        assertTrue(Components.find("mymod:shop/card").isEmpty(), "nothing is left behind");
    }

    // ------------------------------------------------------------------ экраны и текстуры

    @Test
    void aPackScreenLoadsAndItsTexturesComeFromThePack() {
        AssetPack pack = pack();
        install(pack);
        List<String> decoded = new ArrayList<>();
        LoadedScreen<Widget> screen = pack.screen("main", LoadOptions.DEFAULT,
                (id, data, width, height, options) -> {
                    decoded.add(id + ":" + data.length);
                    return new SimpleTextureHandle(id, width, height, null, options);
                });
        assertEquals("Sword", ((Label) Xaml.get(screen.root(), "label")).text());

        var resolver = pack.textureResolver((id, data, w, h, o) -> {
            decoded.add(id + ":" + data.length);
            return new SimpleTextureHandle(id, w, h, null, o);
        }, (id, w, h, o) -> new SimpleTextureHandle("fallback:" + id, w, h, null, o));
        resolver.resolve("icons/sword.png", 16, 16, null);
        var outside = resolver.resolve("minecraft:stone", 16, 16, null);
        assertEquals(List.of("icons/sword.png:3"), decoded, "the pack texture was decoded from its bytes");
        assertEquals("fallback:minecraft:stone", ((SimpleTextureHandle) outside).id(), "other ids go to the fallback");
    }

    @Test
    void aMissingScreenListsTheAvailableOnes() {
        XamlLoadException failure = assertThrows(XamlLoadException.class,
                () -> pack().screen("nope", LoadOptions.DEFAULT, null));
        assertTrue(failure.getMessage().contains("screens/main.xaml"), failure.getMessage());
    }

    // ------------------------------------------------------------------ защита и паспорт

    private static byte[] zip(String name, byte[] data) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (ZipOutputStream zip = new ZipOutputStream(out)) {
                zip.putNextEntry(new ZipEntry("pack.xaml"));
                zip.write(MANIFEST.toXaml().getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
                zip.putNextEntry(new ZipEntry(name));
                zip.write(data);
                zip.closeEntry();
            }
            return out.toByteArray();
        } catch (IOException failure) {
            throw new IllegalStateException(failure);
        }
    }

    @Test
    void entriesThatLeaveTheArchiveAreRejected() {
        for (String evil : new String[]{"../evil.xaml", "a/../../evil.xaml", "/abs.xaml", "C:/abs.xaml"}) {
            XamlLoadException failure = assertThrows(XamlLoadException.class,
                    () -> AssetPack.read(zip(evil, new byte[]{1})), evil);
            assertTrue(failure.getMessage().contains(evil.replace("C:/", "C:/")), failure.getMessage());
        }
        assertEquals("a/b.xaml", AssetPack.read(zip("a//./b.xaml", new byte[]{1})).names().get(0), "harmless oddities are normalised");
    }

    @Test
    void anOversizedEntryIsRejectedWhileReading() {
        byte[] huge = new byte[(int) AssetPack.MAX_ENTRY_BYTES + 1];
        XamlLoadException failure = assertThrows(XamlLoadException.class, () -> AssetPack.read(zip("big.bin", huge)));
        assertTrue(failure.getMessage().contains("is larger than"), failure.getMessage());
    }

    @Test
    void theManifestIsRequiredAndValidated() {
        assertThrows(XamlLoadException.class, () -> AssetPack.read(new byte[]{}));
        for (String[] bad : new String[][]{
                {"<Pack Version=\"1\"/>", "requires Id"},
                {"<Pack Id=\"a\"/>", "requires Version"},
                {"<Pack Id=\"Bad Id\" Version=\"1\"/>", "lowercase letters"},
                {"<Pack Id=\"a\" Version=\"\"/>", "version must not be blank"},
                {"<Pack Id=\"a\" Version=\"1\" Colour=\"red\"/>", "Unknown attribute 'Colour'"},
                {"<Other/>", "must start with <Pack>"}}) {
            XamlLoadException failure = assertThrows(XamlLoadException.class, () -> PackManifest.parse(bad[0]), bad[0]);
            assertTrue(failure.getMessage().contains(bad[1]), bad[0] + " -> " + failure.getMessage());
        }
        PackManifest parsed = PackManifest.parse("<Pack Id=\"a:b\" Version=\"2\"/>");
        assertEquals("a:b", parsed.name(), "the name defaults to the id");
    }

    @Test
    void theWriterRefusesBadPathsAndTheManifestFile() {
        AssetPackWriter writer = new AssetPackWriter(MANIFEST);
        assertThrows(XamlLoadException.class, () -> writer.add("../x", "x"));
        assertThrows(IllegalArgumentException.class, () -> writer.add("pack.xaml", "x"));
    }
}
