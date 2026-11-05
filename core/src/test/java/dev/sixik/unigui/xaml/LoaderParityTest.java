package dev.sixik.unigui.xaml;

import org.junit.jupiter.api.Test;


import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Три реальных демо-документа, загруженные новым загрузчиком, должны дать то же дерево виджетов, что и
 * старый (golden снят со старого загрузчика до начала миграции; сам старый загрузчик уже удалён, так что
 * golden-файлы остаются единственным свидетельством эквивалентности).
 */
final class LoaderParityTest {
    private static void assertSameTree(String name) {
        String expected = Golden.read("tree-" + name + ".txt").replace("\r\n", "\n");
        String actual = TreeDump.dump(Xaml.loadResource("assets/unigui/xml/" + name + ".xml"));
        assertEquals(expected, actual, "widget tree of " + name + " differs from the legacy loader");
    }

    @Test
    void overviewMatchesLegacyLoader() {
        assertSameTree("overview");
    }

    @Test
    void demoMatchesLegacyLoader() {
        assertSameTree("xml_demo");
    }

    @Test
    void secondDemoMatchesLegacyLoader() {
        assertSameTree("xml_demo_2");
    }
}
